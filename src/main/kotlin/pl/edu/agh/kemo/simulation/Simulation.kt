package pl.edu.agh.kemo.simulation

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.analysis.collector.Observations
import org.moeaframework.core.Settings
import org.moeaframework.core.fitness.CrowdingDistanceFitnessEvaluator
import org.moeaframework.core.fitness.FitnessBasedArchive
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.spi.ProblemFactory
import pl.edu.agh.kemo.algorithm.KemoAlgorithmsProvider
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.tools.algorithmVariants
import pl.edu.agh.kemo.tools.loggerFor
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


val SIMULATION_EXECUTOR: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

val SIMULATION_DISPATCHER =
    SIMULATION_EXECUTOR.asCoroutineDispatcher()

abstract class Simulation(
    protected val repetitions: Int = 10,
    protected val problems: List<String>,
    protected val algorithms: List<String>,
    protected val hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator>,
    private val startRunNo: Int
) {
    private val log = loggerFor<Simulation>()

    init {
        with(AlgorithmFactory.getInstance()) {
            addProvider(HGSProvider())
            addProvider(KemoAlgorithmsProvider())
        }
    }

    fun run() {
        val algorithmTimes = ConcurrentHashMap<String, Long>()

        runBlocking {
        for (problemName in problems) {
            val problemKey = "core.indicator.hypervolume_refpt.$problemName"

            val problem = ProblemFactory.getInstance().getProblem(problemName)
            val referencePoint = (0 until problem.numberOfObjectives).map { 50.0 }.toDoubleArray()

            Settings.PROPERTIES.setDoubleArray(problemKey, referencePoint)

            for (algorithmName in algorithms) {
                log.info("Processing... $algorithmName, $problemName")

                val algorithmVariants = hgsTypes.algorithmVariants(algorithmName)


                    for (runNo in startRunNo until (startRunNo + repetitions)) {
                        for (algorithmVariant in algorithmVariants) {
                            launch(SIMULATION_DISPATCHER) {
                                log.info("Processing... $algorithmVariant")
                                val accumulator = runAlgorithmVariant(
                                    problemName,
                                    algorithmVariant,
                                    runNo,
                                    algorithmTimes
                                )
                                accumulator.saveCSV(algorithmVariant, problemName, runNo)
                            }
                        }
                    }
                }
            }

            algorithmTimes.forEach { (algorithm, elapsedTime) ->
                log.info("$algorithm elapsed time = $elapsedTime")
            }
        }
    }

    private fun runAlgorithmVariant(
        problemName: String,
        algorithmVariant: String,
        runNo: Int,
        algorithmTimes: MutableMap<String, Long>
    ): Observations {
        val instrumenter = Instrumenter()
            .withProblem(problemName)
            .also { configureInstrumenter(it) }
            .also { if (QualityIndicator.IGD in metrics) it.attachInvertedGenerationalDistanceCollector() }
            .also { if (QualityIndicator.HYPERVOLUME in metrics) it.attachHypervolumeCollector() }
            .also { if (QualityIndicator.SPACING in metrics) it.attachSpacingCollector() }

        val runTime = measureTimeMillis {
            val population = Executor()
                .withProblem(problemName)
                .withAlgorithm(algorithmVariant)
                .withProperty("populationSize", 600)
                .withProperty("updateUtility", 50)
                .withInstrumenter(instrumenter)
                .also { configureExecutor(problemName, algorithmVariant, runNo, it) }
                .run()
            print(population.size())
            val finalResult = FitnessBasedArchive(CrowdingDistanceFitnessEvaluator(), 100)
            finalResult.addAll(population)
            finalResult.save(algorithmVariant, problemName, runNo)
        }

        algorithmTimes[algorithmVariant] = (algorithmTimes[algorithmVariant] ?: 0) + runTime
        return instrumenter.observations
    }

    abstract fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter

    abstract fun configureExecutor(problem: String, algorithm: String, runNo: Int, executor: Executor): Executor
}

enum class QualityIndicator(val fullName: String, val shortName: String, val type: BestMetricType) {
    HYPERVOLUME("Hypervolume", "hv", BestMetricType.MAX),
    IGD("InvertedGenerationalDistance", "igd", BestMetricType.MIN),
    SPACING("Spacing", "spacing", BestMetricType.MIN)
}

enum class BestMetricType {
    MIN, MAX
}

fun String.toQualityIndicator(): QualityIndicator = QualityIndicator.values()
    .find { it.fullName == this } ?: throw IllegalArgumentException("Unsupported metric: $this")

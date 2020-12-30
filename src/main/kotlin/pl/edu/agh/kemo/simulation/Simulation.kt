package pl.edu.agh.kemo.simulation

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.core.Settings
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.spi.ProblemFactory
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.tools.algorithmVariants
import pl.edu.agh.kemo.tools.average
import pl.edu.agh.kemo.tools.loggerFor
import java.util.EnumSet
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

    fun run() {
        AlgorithmFactory.getInstance().addProvider(HGSProvider())

        val algorithmTimes = mutableMapOf<String, Long>()

        for (problemName in problems) {
            val problemKey = "core.indicator.hypervolume_refpt.$problemName"

            val problem = ProblemFactory.getInstance().getProblem(problemName)
            val referencePoint = (0 until problem.numberOfObjectives).map { 50.0 }.toDoubleArray()

            Settings.PROPERTIES.setDoubleArray(problemKey, referencePoint)

            val algorithmsAccumulators = mutableMapOf<String, Accumulator>()

            for (algorithmName in algorithms) {
                log.info("Processing... $algorithmName, $problemName")

                val algorithmVariants = hgsTypes.algorithmVariants(algorithmName)
                val resultAccumulators = algorithmVariants.associateWith { mutableListOf<Accumulator>() }

                runBlocking {
                    for (runNo in startRunNo until (startRunNo + repetitions)) {
                        for (algorithmVariant in algorithmVariants) {
                            launch(SIMULATION_DISPATCHER) {
                                log.info("Processing... $algorithmVariant")
                                runAlgorithmVariant(
                                    problemName,
                                    algorithmVariant,
                                    runNo,
                                    algorithmTimes,
                                    resultAccumulators)
                            }
                        }
                    }
                }
                updateStatistics(resultAccumulators, algorithmsAccumulators, problemName)
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
        algorithmTimes: MutableMap<String, Long>,
        resultAccumulators: Map<String, MutableList<Accumulator>>
    ) {
        val instrumenter = Instrumenter()
            .withProblem(problemName)
            .also { configureInstrumenter(it) }
            .also { if (QualityIndicator.IGD in metrics) it.attachInvertedGenerationalDistanceCollector() }
            .also { if (QualityIndicator.HYPERVOLUME in metrics) it.attachHypervolumeCollector() }
            .also { if (QualityIndicator.SPACING in metrics) it.attachSpacingCollector() }

        val runTime = measureTimeMillis {
            val population = Executor().withProblem(problemName)
                .withAlgorithm(algorithmVariant)
                .withProperty("populationSize", 64)
                .withInstrumenter(instrumenter)
                .also { configureExecutor(problemName, algorithmVariant, runNo, it) }
                .run()
            population.save(algorithmVariant, problemName, runNo)
        }

        algorithmTimes[algorithmVariant] = (algorithmTimes[algorithmVariant] ?: 0) + runTime
        resultAccumulators[algorithmVariant]?.add(instrumenter.lastAccumulator)
    }

    private fun updateStatistics(
        resultAccumulators: Map<String, MutableList<Accumulator>>,
        algorithmsAccumulators: MutableMap<String, Accumulator>,
        problemName: String,
    ) {
        saveMetrics(resultAccumulators, problemName, startRunNo)

        val averageAccumulators = resultAccumulators.mapValues { it.value.average() }
        algorithmsAccumulators.putAll(averageAccumulators)

        averageAccumulators.forEach { (variant, accumulator) ->
            log.debug("$problemName : $variant:")
            log.debug("Average accumulator for variant: $variant")
            log.debug(accumulator.toCSV())
        }
    }

    abstract fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter

    abstract fun configureExecutor(problem: String, algorithm: String, runNo: Int, executor: Executor): Executor
}

enum class QualityIndicator(val fullName: String, val shortName: String) {
    HYPERVOLUME("Hypervolume", "hv"),
    IGD("InvertedGenerationalDistance", "igd"),
    SPACING("Spacing", "spacing")
}

fun String.toQualityIndicator(): QualityIndicator = QualityIndicator.values()
    .find { it.fullName == this } ?: throw IllegalArgumentException("Unsupported metric: $this")
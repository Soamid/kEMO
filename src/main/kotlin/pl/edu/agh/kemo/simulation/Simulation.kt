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
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.algorithm.cartesian
import pl.edu.agh.kemo.algorithm.isHgs
import pl.edu.agh.kemo.algorithm.toInfoString
import pl.edu.agh.kemo.tools.algorithmVariants
import pl.edu.agh.kemo.tools.loggerFor
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

const val DEFAULT_REPETITIONS = 10

const val DEFAULT_POPULATION_SIZE = 64

val SIMULATION_EXECUTOR: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

val SIMULATION_DISPATCHER =
    SIMULATION_EXECUTOR.asCoroutineDispatcher()

abstract class Simulation(
    protected val repetitions: Int = DEFAULT_REPETITIONS,
    protected val populationSize: Int = DEFAULT_POPULATION_SIZE,
    protected val problems: List<String>,
    protected val algorithms: List<String>,
    protected val hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator>,
    private val startRunNo: Int,
    private val propertiesSets: TypedProperties? = null
) {
    private val log = loggerFor<Simulation>()

    fun run() {
        AlgorithmFactory.getInstance().addProvider(HGSProvider())

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
                            forEachPropertySet(algorithmVariant) { propertySet ->
                                launch(SIMULATION_DISPATCHER) {
                                    val variantName = getSimulationName(propertySet, algorithmVariant)
                                    log.info("Processing... $variantName")
                                    val accumulator = runAlgorithmVariant(
                                        problemName,
                                        algorithmVariant,
                                        variantName,
                                        runNo,
                                        algorithmTimes,
                                        propertySet
                                    )
                                    accumulator.saveCSV(variantName, problemName, runNo)
                                }
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

    private fun forEachPropertySet(algorithmVariant: String, runner: (propertySet: TypedProperties?) -> Unit) {
        if(algorithmVariant.isHgs() && propertiesSets != null) {
            propertiesSets.cartesian().forEach { runner(it) }
        }
        else {
            runner(null)
        }
    }

    private fun runAlgorithmVariant(
        problemName: String,
        algorithmVariant: String,
        variantName: String,
        runNo: Int,
        algorithmTimes: MutableMap<String, Long>,
        propertySet: TypedProperties?
    ): Accumulator {
        val instrumenter = Instrumenter()
            .withProblem(problemName)
            .also { configureInstrumenter(it) }
            .also { if (QualityIndicator.IGD in metrics) it.attachInvertedGenerationalDistanceCollector() }
            .also { if (QualityIndicator.HYPERVOLUME in metrics) it.attachHypervolumeCollector() }
            .also { if (QualityIndicator.SPACING in metrics) it.attachSpacingCollector() }

        val runTime = measureTimeMillis {
            val population = Executor()
                .apply { if(propertySet != null) withProperties(propertySet.properties) }
                .withProblem(problemName)
                .withAlgorithm(algorithmVariant)
                .withProperty("populationSize", populationSize)
                .withInstrumenter(instrumenter)
                .also { configureExecutor(problemName, variantName, runNo, it) }
                .run()
            population.save(variantName, problemName, runNo)
        }

        algorithmTimes[variantName] = (algorithmTimes[variantName] ?: 0) + runTime
        return instrumenter.lastAccumulator
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

fun getSimulationName(
    propertySet: TypedProperties?,
    algorithmVariant: String
) = if (propertySet != null) "${algorithmVariant}+${propertySet.toInfoString()}" else algorithmVariant


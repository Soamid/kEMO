package pl.edu.agh.kemo.simulation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.moeaframework.Analyzer
import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.core.Settings
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.spi.ProblemFactory
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.tools.WinnerCounter
import pl.edu.agh.kemo.tools.average
import pl.edu.agh.kemo.tools.loggerFor
import toExistingFilepath
import java.util.EnumSet
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

abstract class Simulation(
    protected val repetitions: Int = 10,
    protected val problems: List<String>,
    protected val algorithms: List<String>,
    protected val hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator>
) {
    private val log = loggerFor<Simulation>()

    private val winnerCounter = WinnerCounter()

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

                val hgsAlgorithms = hgsTypes.map { "${it.shortName}+$algorithmName" }
                val algorithmVariants = hgsAlgorithms + algorithmName
                val resultAccumulators = algorithmVariants.associateWith { mutableListOf<Accumulator>() }

                val analyzer = Analyzer()
                    .withProblem(problem)
                    .also { if (QualityIndicator.IGD in metrics) it.includeInvertedGenerationalDistance() }
                    .also { if (QualityIndicator.HYPERVOLUME in metrics) it.includeHypervolume() }
                    .also { if (QualityIndicator.SPACING in metrics) it.includeSpacing() }
                    .showStatisticalSignificance()

                runBlocking {
                    for (runNo in 1..repetitions) {
                        for (algorithmVariant in algorithmVariants) {
                            launch(simulationDispatcher()) {
                                log.info("Processing... $algorithmVariant")
                                runAlgorithmVariant(
                                    problemName,
                                    algorithmVariant,
                                    runNo,
                                    algorithmTimes,
                                    resultAccumulators,
                                    analyzer
                                )
                            }
                        }
                    }
                }
                val averageAccumulators =
                    updateStatistics(resultAccumulators, algorithmsAccumulators, problemName, analyzer)
                saveAlgorithmPlots(averageAccumulators, algorithmName, problemName)

                analyzer.printAnalysis()
                saveSummaryPlots(algorithmsAccumulators, problemName)
            }

            log.info("## GLOBAL SUMMARY")
            winnerCounter.printSummary()

            algorithmTimes.forEach { algorithm, elapsedTime ->
                log.info("$algorithm elapsed time = $elapsedTime")
            }
        }
    }

    private fun simulationDispatcher() =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()

    private fun runAlgorithmVariant(
        problemName: String,
        algorithmVariant: String,
        runNo: Int,
        algorithmTimes: MutableMap<String, Long>,
        resultAccumulators: Map<String, MutableList<Accumulator>>,
        analyzer: Analyzer
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
            analyzer.add(algorithmVariant, population)
        }

        algorithmTimes[algorithmVariant] = (algorithmTimes[algorithmVariant] ?: 0) + runTime
        resultAccumulators[algorithmVariant]?.add(instrumenter.lastAccumulator)
    }


    private fun updateStatistics(
        resultAccumulators: Map<String, MutableList<Accumulator>>,
        algorithmsAccumulators: MutableMap<String, Accumulator>,
        problemName: String,
        analyzer: Analyzer
    ): Map<String, Accumulator> {
        val averageAccumulators = resultAccumulators.mapValues { it.value.average() }
        algorithmsAccumulators.putAll(averageAccumulators)

        averageAccumulators.forEach { (variant, accumulator) ->
            log.debug("$problemName : $variant:")
            log.debug("Average accumulator for variant: $variant")
            log.debug(accumulator.toCSV())

            winnerCounter.update(accumulator, variant, problemName, analyzer)
        }
        return averageAccumulators
    }

    private fun saveAlgorithmPlots(
        averageAccumulators: Map<String, Accumulator>,
        algorithmName: String,
        problemName: String
    ) {
        metrics
            .forEach { metric ->
                val plot = Plot().apply {
                    averageAccumulators.forEach { (variant, accumulator) ->
                        add(variant, accumulator, metric.fullName)
                    }
                    setTitle(algorithmName)
                }
                plot.save("plots/$algorithmName/${problemName}_${metric.shortName}.png".toExistingFilepath())
            }
    }

    private fun saveSummaryPlots(
        algorithmsAccumulators: MutableMap<String, Accumulator>,
        problemName: String
    ) {
        for (metric in metrics) {
            val summaryPlot = Plot().apply {
                algorithmsAccumulators.forEach {
                    add(it.key, it.value, metric.fullName)
                }
                setTitle("$problemName (${metric.fullName})")
            }
            summaryPlot.save("plots/summary/${problemName}_${metric.shortName}.png".toExistingFilepath())
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
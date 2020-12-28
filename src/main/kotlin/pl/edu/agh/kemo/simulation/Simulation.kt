package pl.edu.agh.kemo.simulation

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
import kotlin.system.measureTimeMillis

abstract class Simulation(
    protected val repetitions: Int = 10,
    protected val problems: List<String>,
    protected val algorithms: List<String>,
    protected val hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator> = EnumSet.allOf(QualityIndicator::class.java)
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

                for (runNo in 1..repetitions) {

                    for (algorithmVariant in algorithmVariants) {
                        log.info("Processing... $algorithmVariant")
                        runAlgorithmVariant(problemName, algorithmVariant, algorithmTimes, resultAccumulators)
                    }
                    val averageAccumulators = updateStatistics(resultAccumulators, algorithmsAccumulators, problemName)
                    saveAlgorithmPlots(averageAccumulators, algorithmName, problemName)
                }
                saveSummaryPlots(algorithmsAccumulators, problemName)
            }

            log.info("## GLOBAL SUMMARY")
            winnerCounter.printSummary()

            algorithmTimes.forEach { algorithm, elapsedTime ->
                log.info("$algorithm elapsed time = $elapsedTime")
            }
        }
    }

    private fun runAlgorithmVariant(
        problemName: String,
        algorithmVariant: String,
        algorithmTimes: MutableMap<String, Long>,
        resultAccumulators: Map<String, MutableList<Accumulator>>
    ) {
        val instrumenter = Instrumenter()
            .withProblem(problemName)
            .attachHypervolumeCollector()
            .attachInvertedGenerationalDistanceCollector()
            .attachSpacingCollector()
            .also { configureInstrumenter(it) }


        val runTime = measureTimeMillis {
            Executor().withProblem(problemName)
                .withAlgorithm(algorithmVariant)
                .withProperty("populationSize", 64)
                .withInstrumenter(instrumenter)
                .also { configureExecutor(it) }
                .run()
        }

        algorithmTimes[algorithmVariant] = (algorithmTimes[algorithmVariant] ?: 0) + runTime
        resultAccumulators[algorithmVariant]?.add(instrumenter.lastAccumulator)
    }

    private fun updateStatistics(
        resultAccumulators: Map<String, MutableList<Accumulator>>,
        algorithmsAccumulators: MutableMap<String, Accumulator>,
        problemName: String
    ): Map<String, Accumulator> {
        val averageAccumulators = resultAccumulators.mapValues { it.value.average() }
        algorithmsAccumulators.putAll(averageAccumulators)

        averageAccumulators.forEach { (variant, accumulator) ->
            log.debug("$problemName : $variant:")
            log.debug("Average accumulator for variant: $variant")
            log.debug(accumulator.toCSV())

            winnerCounter.update(accumulator, variant, problemName)
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

    abstract fun configureExecutor(executor: Executor): Executor
}

enum class QualityIndicator(val fullName: String, val shortName: String) {
    HYPERVOLUME("Hypervolume", "hv"),
    IGD("InvertedGenerationalDistance", "igd"),
    SPACING("Spacing", "spacing")
}

fun String.toQualityIndicator(): QualityIndicator = QualityIndicator.values()
    .find { it.fullName == this } ?: throw IllegalArgumentException("Unsupported metric: $this")
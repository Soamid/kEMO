package pl.edu.agh.kemo.simulation

import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.problem.StandardProblems
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.tools.algorithmVariants
import pl.edu.agh.kemo.tools.average
import java.util.EnumSet

class PlotGenerator(
    private val problems: List<String>,
    private val algorithms: List<String>,
    private val hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator>,
    private val runRange: IntRange
) {

    val allAlgorithmVariants = algorithms.flatMap {  hgsTypes.algorithmVariants(it) }

    fun saveAlgorithmPlots() {
        for (problem in problems) {
            for (algorithm in algorithms) {
                val algorithmVariants = hgsTypes.algorithmVariants(algorithm)
                val accumulators =
                    algorithmVariants.associateWith { accumulatorsFromCSV(problem, it, runRange).average() }
                saveAlgorithmPlots(accumulators, algorithm, problem)
            }
        }
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
                plot.save("plots/$algorithmName/${problemName}_${metric.shortName}.png".toExistingFile())
            }
    }

    fun saveSummaryPlots() {
        val algorithmVariants = hgsTypes.algorithmVariants(algorithms)
        for (problem in problems) {
            for (algorithm in algorithms) {
                val accumulators =
                    algorithmVariants.associateWith { accumulatorsFromCSV(problem, it, runRange).average() }
                saveSummaryPlots(accumulators, problem)
            }
        }
    }

    private fun saveSummaryPlots(
        algorithmsAccumulators: Map<String, Accumulator>,
        problemName: String
    ) {
        for (metric in metrics) {
            val summaryPlot = Plot().apply {
                algorithmsAccumulators.forEach {
                    add(it.key, it.value, metric.fullName)
                }
                setTitle("$problemName (${metric.fullName})")
            }
            summaryPlot.save("plots/summary/${problemName}_${metric.shortName}.png".toExistingFile())
        }
    }

    fun savePopulationPlots() {
        for(problemName in problems) {
            val paretoFront = StandardProblems().getReferenceSet(problemName)

            val populations = loadPopulations(problemName, allAlgorithmVariants, runRange)

            val populationPlot = Plot().apply {
                populations.entries.forEach { (algorithm, populations) ->
                    add(algorithm, populations[0])
                }
                add("referenceSet", paretoFront)
            }
            populationPlot.save("plots/fronts/${problemName}.png".toExistingFile())
        }
    }
}
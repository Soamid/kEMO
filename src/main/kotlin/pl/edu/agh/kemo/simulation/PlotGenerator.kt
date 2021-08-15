package pl.edu.agh.kemo.simulation

import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.problem.StandardProblems
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.algorithm.cartesian
import pl.edu.agh.kemo.algorithm.isHgs
import pl.edu.agh.kemo.tools.algorithmVariants
import pl.edu.agh.kemo.tools.average
import java.util.EnumSet

class PlotGenerator(
    private val problems: List<String>,
    algorithms: List<String>,
    hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator>,
    private val runRange: IntRange,
    propertiesSets: TypedProperties? = null
) : ResultsProcessor(algorithms, hgsTypes, propertiesSets) {

    fun saveAlgorithmPlots() {
        for (problem in problems) {
            for (algorithm in algorithms) {
                val variantNames = getVariants(algorithm)
                val accumulators = variantNames.associateWith { accumulatorsFromCSV(problem, it, runRange).average() }
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
        for (problem in problems) {
            for (algorithm in allAlgorithmVariants) {
                val accumulators =
                    allAlgorithmVariants.associateWith { accumulatorsFromCSV(problem, it, runRange).average() }
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
        for (problemName in problems) {
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
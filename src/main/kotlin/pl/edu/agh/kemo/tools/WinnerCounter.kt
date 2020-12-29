package pl.edu.agh.kemo.tools

import org.moeaframework.AlgorithmStats
import org.moeaframework.analysis.collector.Accumulator
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.toQualityIndicator
import kotlin.math.max
import kotlin.math.min


data class MetricEntry(val metric: String, val runNo: Int)

data class WinningAlgorithm(val algorithm: String, val metricValue: Double)

class WinnerCounter {
    val log = loggerFor<WinnerCounter>()
    val allWinnersData = mutableMapOf<MetricEntry, MutableMap<String, WinningAlgorithm>>()

    val significantWinnersData = mutableMapOf<MetricEntry, MutableMap<String, WinningAlgorithm>>()

    fun update(
        resultsAccumulator: Accumulator,
        algorithm: String,
        problem: String,
        finalEpochAlgorithmResults: Map<String, AlgorithmStats>
    ) {
        resultsAccumulator.keySet()
            .filter { isMetricValid(it) }
            .forEach { metric ->
                val samplesCount = resultsAccumulator.size(metric)

                // we take last but one result in order to compare results not exceeding the budget
                val metricValue = (resultsAccumulator[metric, samplesCount - 2] as Number).toDouble()

                val metricEntry = MetricEntry(metric, 0)
                val algorithmsMap = allWinnersData.getOrPut(metricEntry, { mutableMapOf() })
                val significantAlgorithmsMap =
                    significantWinnersData.getOrPut(metricEntry, { mutableMapOf() })

                val winningAlgorithm = algorithmsMap[problem]
                log.debug("updating for $algorithm, $metric value = $metricValue, current = $winningAlgorithm")
                if (winningAlgorithm == null || bestMetricValue(
                        metric,
                        winningAlgorithm.metricValue,
                        metricValue
                    ) == metricValue
                ) {
                    algorithmsMap[problem] = WinningAlgorithm(algorithm, metricValue)

                    if (isStatisticallySignificant(finalEpochAlgorithmResults, algorithm, metric)) {
                        significantAlgorithmsMap[problem] = WinningAlgorithm(algorithm, metricValue)
                    }
                }
            }
    }

    private fun isMetricValid(metric: String) = !metric.endsWith("_error") && metric != "NFE"

    private fun isStatisticallySignificant(
        finalEpochAlgorithmResults: Map<String, AlgorithmStats>,
        algorithm: String,
        metric: String?
    ): Boolean {
        val indifferentAlgorithms = finalEpochAlgorithmResults[algorithm]?.get(metric)?.indifferentAlgorithms
        return indifferentAlgorithms?.isEmpty()
            ?: throw IllegalArgumentException("No such metric or algorithm: $algorithm, $metric")
        // indifferentAlgorithms.size < analyzer.analysis.algorithms.size
    }

    fun printSummary() {
        println("All winners data:")
        printSummary(allWinnersData)

        println("Statistically significant winners data:")
        printSummary(significantWinnersData)
    }

    private fun printSummary(winnersData: Map<MetricEntry, MutableMap<String, WinningAlgorithm>>) {
        winnersData.keys.forEach { metricEntry ->
            println("${metricEntry.metric}, run=${metricEntry.runNo}")
            winnersData[metricEntry]?.let { problemMap ->
                val winnersString = problemMap.values
                    .groupBy { it.algorithm }
                    .entries.sortedByDescending { it.value.size }
                    .joinToString(separator = ",") { "${it.key} (${it.value.size})" }
                println("\t" + winnersString)
            }
        }
    }
}

fun bestMetricValue(metric: String, value1: Double, value2: Double): Double {
    if (metric == "NFE") {
        return max(value1, value2)
    }

    return when (metric.toQualityIndicator()) {
        QualityIndicator.HYPERVOLUME -> max(value1, value2)
        QualityIndicator.IGD -> min(value1, value2)
        QualityIndicator.SPACING -> min(value1, value2)
    }
}
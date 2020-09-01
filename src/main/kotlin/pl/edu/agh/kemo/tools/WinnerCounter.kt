package pl.edu.agh.kemo.tools

import org.moeaframework.analysis.collector.Accumulator
import java.lang.IllegalArgumentException
import kotlin.math.max
import kotlin.math.min


data class MetricEntry(val metric: String, val runNo: Int)

data class WinningAlgorithm(val algorithm: String, val metricValue: Double)

class WinnerCounter {
    val log = loggerFor<WinnerCounter>()
    val winnersData = mutableMapOf<MetricEntry, MutableMap<String, WinningAlgorithm>>()

    fun update(resultsAccumulator: Accumulator, algorithm: String, problem: String) {
        resultsAccumulator.keySet().forEach { metric ->
            val samplesCount = resultsAccumulator.size(metric)
            val metricValue = (resultsAccumulator[metric, samplesCount - 1] as Number).toDouble()

            val metricEntry = MetricEntry(metric, 0)
            val algorithmsMap = winnersData.getOrPut(metricEntry, { mutableMapOf() })

            val winningAlgorithm = algorithmsMap[problem]
            log.debug("updating for $algorithm, $metric value = $metricValue, current = $winningAlgorithm")
            if (winningAlgorithm == null || bestMetricValue(
                    metric,
                    winningAlgorithm.metricValue,
                    metricValue
                ) == metricValue
            ) {
                algorithmsMap[problem] = WinningAlgorithm(algorithm, metricValue)
            }
        }
    }

    fun printSummary() {
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

fun bestMetricValue(metric: String, value1: Double, value2: Double): Double = when (metric) {
    "Hypervolume" -> max(value1, value2)
    "InvertedGenerationalDistance" -> min(value1, value2)
    "NFE" -> max(value1, value2)
    else -> throw IllegalArgumentException("Unknown metric: $metric")
}
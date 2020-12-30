package pl.edu.agh.kemo.tools

import org.moeaframework.IndicatorResult
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.accumulatorsFromCSV
import java.lang.IllegalStateException

fun printMetricsComparisonTable(
    problems: List<String>,
    algorithmVariants: List<String>,
    runRange: IntRange,
    metric: QualityIndicator
) {
    val winnersCounter = mutableMapOf<String, Int>()

    val columnsCount = algorithmVariants.size * 4
    val tableBegin = """\begin{tabular}{|l|*{$columnsCount}{r|}}"""

    val metricHeader = """\multicolumn{${columnsCount + 1}}{|c|}{${metric.fullName}}\\"""

    val algorithmsHeader = algorithmVariants.joinToString(
        separator = " & ",
        prefix = " & ",
        postfix = """\\"""
    ) { """\multicolumn{4}{|c|}{$it}""" }

    val statsHeader = algorithmVariants.joinToString(
        separator = " & ",
        prefix = "Problem & ",
        postfix = """\\"""
    ) { """Min & Mean & Max & Error""" }

    val rows = problems.map { problem ->

        val indicatorResults = algorithmVariants.associateWith { algorithmVariant ->
            accumulatorsFromCSV(problem, algorithmVariant, runRange)
                .map { it[metric.fullName, it.size(metric.fullName) - 1] as Double }
                .let { IndicatorResult(metric.fullName, it.toDoubleArray()) }
        }

        val bestAverage = indicatorResults.bestAlgorithm(metric) { it.average }
        val bestMin = indicatorResults.bestAlgorithm(metric) { it.min }
        val bestMax = indicatorResults.bestAlgorithm(metric) { it.max }

        updateBest(winnersCounter, bestMin, "MIN")
        updateBest(winnersCounter, bestAverage, "AVG")
        updateBest(winnersCounter, bestMax, "MAX")

        algorithmVariants
            .joinToString(
                separator = " & ",
                prefix = "$problem & ",
                postfix = """\\"""
            ) { algorithm ->
                val result = indicatorResults[algorithm] ?: throw IllegalStateException()
                val min = result.min.roundedString(algorithm, bestMin)
                val max = result.max.roundedString(algorithm, bestMax)
                val average = result.average.roundedString(algorithm, bestAverage)
                val error = result.stdev.roundedString()
                "$min & $average & $max & $error"
            }
    }

    val bestMinWins = bestWinner(winnersCounter, "MIN")
    val bestAvgWins = bestWinner(winnersCounter, "AVG")
    val bestMaxWins = bestWinner(winnersCounter, "MAX")

    val summaryWins = algorithmVariants.flatMap {
        listOf(
            formatWinner(winnersCounter, it, "MIN", bestMinWins),
            formatWinner(winnersCounter, it, "AVG", bestAvgWins),
            formatWinner(winnersCounter, it, "MAX", bestMaxWins),
            ""
        )
    }.joinToString(separator = " & ", prefix = "Wins & ", postfix = """\\""")

    println(tableBegin)
    println("""\hline""")
    println(metricHeader)
    println("""\hline""")
    println(algorithmsHeader)
    println("""\hline""")
    println(statsHeader)
    println("""\hline""")
    rows.forEach { println(it) }
    println("""\hline""")
    println(summaryWins)
    println("""\hline""")
    println("""\end{tabular}""")

    println()
}

private fun formatWinner(
    winnersCounter: MutableMap<String, Int>,
    algorithm: String,
    stat: String,
    bestStatValue: Int
): String {
    val winCount = winnersCounter.getOrDefault(algorithm + stat, 0)
    if (bestStatValue == winCount) {
        return """\textbf{$winCount}"""
    }
    return winCount.toString()
}

private fun bestWinner(winnersCounter: MutableMap<String, Int>, stat: String) =
    winnersCounter.filter { (case, _) -> case.endsWith(stat) }
        .map { (_, winCount) -> winCount }
        .maxOrNull() ?: 0

private fun updateBest(
    winnersCounter: MutableMap<String, Int>,
    winner: String,
    statName: String
) {
    val currentCounter = winnersCounter.getOrDefault(winner + statName, 0)
    winnersCounter[winner + statName] = currentCounter + 1
}

private fun Map<String, IndicatorResult>.bestAlgorithm(
    metric: QualityIndicator,
    statExtractor: (IndicatorResult) -> Double
): String {
    return map { (algorithm, results) -> Pair(algorithm, statExtractor(results)) }
        .reduce { leftResult, rightResult -> bestMetricValue(metric, leftResult, rightResult) }
        .let { (algorithm, _) -> algorithm }
}

private fun bestMetricValue(
    metric: QualityIndicator,
    leftResult: Pair<String, Double>,
    rightResult: Pair<String, Double>
): Pair<String, Double> {
    val (_, leftIndicator) = leftResult
    val (_, rightIndicator) = rightResult

    if (bestMetricValue(
            metric.fullName,
            leftIndicator,
            rightIndicator
        ) == leftIndicator
    ) {
        return leftResult
    }
    return rightResult
}

private fun Double.roundedString(algorithm: String? = null, bestAlgorithm: String? = null): String {
    val formattedValue = "%.5f".format(this)
    if (bestAlgorithm != null && algorithm == bestAlgorithm) {
        return """\textbf{$formattedValue}"""
    }
    return formattedValue
}
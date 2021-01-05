package pl.edu.agh.kemo.tools

import org.moeaframework.AlgorithmStats
import org.moeaframework.IndicatorResult
import pl.edu.agh.kemo.algorithm.isHgs
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.accumulatorsFromCSV
import pl.edu.agh.kemo.simulation.calculateStatisticalSignificance
import java.lang.IllegalStateException

fun printMetricsComparisonTable(
    problems: List<String>,
    algorithmVariants: List<String>,
    runRange: IntRange,
    metric: QualityIndicator
) {
    val winnersCounter = WinnersCounter()
    val softWinnersCounter = WinnersCounter(winnerType = WinnersCounter.WinnerType.SOFT)
    val strongWinnersCounter = WinnersCounter(winnerType = WinnersCounter.WinnerType.STRONG)

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
                .let { indicatorResult -> AlgorithmStats(algorithmVariant).apply { add(indicatorResult) } }
        }
        calculateStatisticalSignificance(metric, indicatorResults)

        winnersCounter.update(indicatorResults, metric)
        softWinnersCounter.update(indicatorResults, metric)
        strongWinnersCounter.update(indicatorResults, metric)

        algorithmVariants
            .joinToString(
                separator = " & ",
                prefix = "$problem & ",
                postfix = """\\"""
            ) { algorithm ->
                val result = indicatorResults[algorithm]?.get(metric.fullName) ?: throw IllegalStateException()
                val min = result.min.roundedString(algorithm, winnersCounter.bestMin, result.stdev)
                val average = result.average.roundedString(algorithm, winnersCounter.bestAverage, result.stdev)
                val max = result.max.roundedString(algorithm, winnersCounter.bestMax, result.stdev)
                val error = result.stdev.roundedString(algorithm, winnersCounter.bestError, result.stdev)
                "$min & $average & $max & $error"
            }
    }

    val summaryWins = getSummaryWinsRow(algorithmVariants, winnersCounter, label = "All Wins")
    val summarySoftWins = getSummaryWinsRow(algorithmVariants, strongWinnersCounter, label = "Soft wins")
    val summaryStrongWins = getSummaryWinsRow(algorithmVariants, strongWinnersCounter, label = "Strong wins")

    println("""\resizebox{\textwidth}{!}{""")
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
    println(summarySoftWins)
    println("""\hline""")
    println(summaryStrongWins)
    println("""\hline""")
    println("""\end{tabular}""")
    println("}")

    println()
}

private fun getSummaryWinsRow(
    algorithmVariants: List<String>,
    winnersCounter: WinnersCounter,
    label: String
) = algorithmVariants.flatMap {
    listOf(
        winnersCounter.formatWinner(it, WinnersCounter.Stat.MIN),
        winnersCounter.formatWinner(it, WinnersCounter.Stat.AVERAGE),
        winnersCounter.formatWinner(it, WinnersCounter.Stat.MAX),
        winnersCounter.formatWinner(it, WinnersCounter.Stat.ERROR)
    )
}.joinToString(separator = " & ", prefix = "$label & ", postfix = """\\""")

class WinnersCounter(val winnerType: WinnerType = WinnerType.WEAK) {

    private val counter = mutableMapOf<String, Int>()

    var bestAverage: String? = null
    var bestMin: String? = null
    var bestMax: String? = null
    var bestError: String? = null

    fun update(indicatorResults: Map<String, AlgorithmStats>, metric: QualityIndicator) {
        bestMin = indicatorResults.bestAlgorithm(metric) { it.min }
        bestAverage = indicatorResults.bestAlgorithm(metric) { it.average }
        bestMax = indicatorResults.bestAlgorithm(metric) { it.max }
        bestError = indicatorResults.bestAlgorithmError(metric)

        updateBest(bestMin, Stat.MIN)
        updateBest(bestAverage, Stat.AVERAGE)
        updateBest(bestMax, Stat.MAX)
        updateBest(bestError, Stat.ERROR)
    }

    private fun updateBest(
        winner: String?,
        stat: Stat
    ) {
        if (winner != null) {
            val currentCounter = counter.getOrDefault(winner + stat.name, 0)
            counter[winner + stat] = currentCounter + 1
        }
    }

    private fun Map<String, AlgorithmStats>.bestAlgorithm(
        metric: QualityIndicator,
        statExtractor: (IndicatorResult) -> Double
    ): String? {
        val (bestAlgorithm, _) = map { (algorithm, results) ->
            Pair(
                algorithm,
                statExtractor(results.get(metric.fullName))
            )
        }.reduce { leftResult, rightResult -> bestMetricValue(metric, leftResult, rightResult) }
        return processBestAlgorithm(bestAlgorithm, metric, this)
    }

    private fun processBestAlgorithm(
        bestAlgorithm: String,
        metric: QualityIndicator,
        algorithmsStats: Map<String, AlgorithmStats>
    ): String? {
        val indifferentAlgorithms = algorithmsStats[bestAlgorithm]
            ?.get(metric.fullName)
            ?.indifferentAlgorithms
            ?: throw IllegalStateException()

        return when (winnerType) {
            WinnerType.WEAK -> bestAlgorithm
            WinnerType.SOFT -> if (indifferentAlgorithms.isEmpty() || isIndifferentToAnotherHGS(
                    bestAlgorithm,
                    indifferentAlgorithms
                ) || isBareDifferentFromAtLeastOneAlgorithm(bestAlgorithm, indifferentAlgorithms, algorithmsStats.keys)
            ) bestAlgorithm else null
            WinnerType.STRONG -> {
                if (indifferentAlgorithms.isEmpty()) bestAlgorithm else null
            }
        }
    }

    private fun isIndifferentToAnotherHGS(
        bestAlgorithm: String,
        indifferentAlgorithms: List<String>
    ) = bestAlgorithm.isHgs() && indifferentAlgorithms.all { it.isHgs() }

    private fun isBareDifferentFromAtLeastOneAlgorithm(
        bestAlgorithm: String,
        indifferentAlgorithms: List<String>,
        allAlgorithms: Set<String>
    ) = !bestAlgorithm.isHgs() && indifferentAlgorithms.size < allAlgorithms.size - 1

    private fun Map<String, AlgorithmStats>.bestAlgorithmError(metric: QualityIndicator): String {
        return map { (algorithm, results) ->
            Pair(
                algorithm,
                results[metric.fullName].stdev
            )
        }.reduce { leftResult, rightResult -> if (leftResult.second < rightResult.second) leftResult else rightResult }
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

    fun bestWinner(stat: Stat) =
        counter.filter { (case, _) -> case.endsWith(stat.name) }
            .map { (_, winCount) -> winCount }
            .maxOrNull() ?: 0

    fun formatWinner(
        algorithm: String,
        stat: Stat,
    ): String {
        val winCount = counter.getOrDefault(algorithm + stat.name, 0)
        if (bestWinner(stat) == winCount) {
            return """\textbf{$winCount}"""
        }
        return winCount.toString()
    }

    enum class Stat { MIN, MAX, AVERAGE, ERROR }

    enum class WinnerType { WEAK, SOFT, STRONG }
}

private fun Double.roundedString(algorithm: String? = null, bestAlgorithm: String? = null, error: Double): String {
    val errorString = "%.10f".format(error)
    val (decimal, fractional) = errorString.split(",")
    val decimalPartLength = if (decimal == "0") 0 else decimal.length
    val nonZeroErrorDigitIndex = fractional.indexOfFirst { it != '0' }
    val precision = maxOf(nonZeroErrorDigitIndex + 2 - decimalPartLength, 0)
    val formattedValue = "%.${precision}f".format(this)
    if (bestAlgorithm != null && algorithm == bestAlgorithm) {
        return """\textbf{$formattedValue}"""
    }
    return formattedValue
}
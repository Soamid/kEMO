package pl.edu.agh.kemo.tools

import org.moeaframework.AlgorithmStats
import org.moeaframework.IndicatorResult
import pl.edu.agh.kemo.algorithm.cutHgs
import pl.edu.agh.kemo.algorithm.isHgs
import pl.edu.agh.kemo.algorithm.toHgsType
import pl.edu.agh.kemo.simulation.BestMetricType
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.accumulatorsFromCSV
import pl.edu.agh.kemo.simulation.calculateStatisticalSignificance
import pl.edu.agh.kemo.simulation.toQualityIndicator
import java.text.DecimalFormat


enum class Stat(val label: String) { MIN("Min"), MAX("Max"), AVERAGE("Average"), ERROR("Error"), IMRPROVEMENT("Impr") }

fun printMetricsComparisonTable(
    problems: List<String>,
    algorithmVariants: List<String>,
    runRange: IntRange,
    metric: QualityIndicator,
    statistics: List<Stat>
) {
    val winnersCounter = WinnersCounter()
    val softWinnersCounter = WinnersCounter(winnerType = WinnersCounter.WinnerType.SOFT)
    val strongWinnersCounter = WinnersCounter(winnerType = WinnersCounter.WinnerType.STRONG)

    val columnsCount = algorithmVariants.size * statistics.size
    val tableBegin = """\begin{tabular}{|l|*{$columnsCount}{r|}}"""

    val metricHeader = """\multicolumn{${columnsCount + 1}}{|c|}{${metric.fullName}}\\"""

    val algorithmsHeader = algorithmVariants.joinToString(
        separator = " & ",
        prefix = " & ",
        postfix = """\\"""
    ) { """\multicolumn{${statistics.size}}{|c|}{${it.toHgsType()?.displayName ?: it}}""" }

    val statsHeader = algorithmVariants.joinToString(
        separator = " & ",
        prefix = "Problem & ",
        postfix = """\\"""
    ) { statistics.joinToString(separator = " & ") { it.label } }

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

                val min = if (Stat.MIN in statistics) result.min.roundedString(
                    algorithm,
                    winnersCounter.bestMin,
                    strongWinnersCounter.bestMin,
                    result.stdev
                ) else null
                val average = if (Stat.AVERAGE in statistics) result.average.roundedString(
                    algorithm,
                    winnersCounter.bestAverage,
                    strongWinnersCounter.bestAverage,
                    result.stdev
                ) else null
                val max = if (Stat.MAX in statistics) result.max.roundedString(
                    algorithm,
                    winnersCounter.bestMax,
                    strongWinnersCounter.bestMax,
                    result.stdev
                ) else null
                val error = if (Stat.ERROR in statistics) result.stdev.roundedString(
                    algorithm,
                    winnersCounter.bestError,
                    null,
                    result.stdev
                ) else null

                val improvement = if (Stat.IMRPROVEMENT in statistics) improvement(algorithm, result, indicatorResults)
                    ?.formattedImprovement()
                else null

                listOfNotNull(min, average, max, error, improvement)
                    .joinToString(separator = " & ")
            }
    }

    val summaryWins = getSummaryWinsRow(algorithmVariants, winnersCounter, label = "Weak wins", statistics)
    val summarySoftWins = getSummaryWinsRow(algorithmVariants, softWinnersCounter, label = "Local wins", statistics)
    val summaryStrongWins =
        getSummaryWinsRow(algorithmVariants, strongWinnersCounter, label = "Global wins", statistics)

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
    label: String,
    statistics: List<Stat>
) = algorithmVariants.flatMap { algorithm ->
    statistics.map { winnersCounter.formatWinner(algorithm, it) }

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
            return when (winnerType) {
                WinnerType.WEAK -> """\textbf{$winCount}"""
                WinnerType.SOFT -> """\textbf{$winCount}"""
                WinnerType.STRONG -> """\textcolor{agh_red}{\textbf{$winCount}}"""
            }
        }
        return winCount.toString()
    }

    enum class WinnerType { WEAK, SOFT, STRONG }
}

private fun improvement(
    algorithm: String,
    result: IndicatorResult,
    indicatorResults: Map<String, AlgorithmStats>
): Double {
    return if (algorithm.isHgs()) {
        val bareAlgo = algorithm.cutHgs()
        val bareResult = indicatorResults[bareAlgo]?.get(result.indicator) ?: throw IllegalStateException()
//
//        var worst = indicatorResults.values.map { it.get(result.indicator).max }.maxOrNull()
//            ?: throw IllegalStateException()
//        var best = indicatorResults.values.map { it.get(result.indicator).min }.minOrNull()
//            ?: throw IllegalStateException()
//
//        if( result.indicator.toQualityIndicator().type == BestMetricType.MAX) {
//            worst = best.also { best = worst }
//        }
//
//        val spread = best - worst
//
//        return (result.average - worst) / spread - (bareResult.average - worst) / spread
        return (result.average - bareResult.average) / bareResult.average * if (result.indicator.toQualityIndicator().type == BestMetricType.MAX) 1 else -1
    } else {
        0.0
    }
}

private fun Double.formattedImprovement() = String.format("%.2f", this * 100.0) + "\\%"


private fun Double.roundedString(
    algorithm: String? = null,
    bestAlgorithm: String? = null,
    strongBestAlgorithm: String? = null,
    error: Double
): String {
    val errorString = "%.10f".format(error)
    val (decimal, fractional) = errorString.split(getSystemDecimalSeparator())
    val decimalPartLength = if (decimal == "0") 0 else decimal.length
    val nonZeroErrorDigitIndex = fractional.indexOfFirst { it != '0' }
    val precision = maxOf(nonZeroErrorDigitIndex + 2 - decimalPartLength, 0)
    val formattedValue = "%.${precision}f".format(this)

    if (strongBestAlgorithm != null && algorithm == strongBestAlgorithm) {
        return """\textcolor{agh_red}{\textbf{$formattedValue}}"""
    }
    if (bestAlgorithm != null && algorithm == bestAlgorithm) {
        return """\textbf{$formattedValue}"""
    }
    return formattedValue
}

fun getSystemDecimalSeparator(): Char {
    val format = DecimalFormat.getInstance() as DecimalFormat
    val symbols = format.decimalFormatSymbols
    return symbols.decimalSeparator
}
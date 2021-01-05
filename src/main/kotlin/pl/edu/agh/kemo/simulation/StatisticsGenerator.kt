package pl.edu.agh.kemo.simulation

import me.tongfei.progressbar.ProgressBar
import org.moeaframework.AlgorithmStats
import org.moeaframework.IndicatorResult
import org.moeaframework.util.statistics.KruskalWallisTest
import org.moeaframework.util.statistics.MannWhitneyUTest
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.tools.Stat
import pl.edu.agh.kemo.tools.WinnerCounter
import pl.edu.agh.kemo.tools.algorithmVariants
import pl.edu.agh.kemo.tools.average
import pl.edu.agh.kemo.tools.printMetricsComparisonTable
import java.util.EnumSet

const val SIGNIFICANCE_LEVEL = 0.05

class StatisticsGenerator(
    private val problems: List<String>,
    algorithms: List<String>,
    hgsTypes: EnumSet<HGSType>,
    private val metrics: EnumSet<QualityIndicator>,
    private val runRange: IntRange
) {

    private val algorithmVariants = hgsTypes.algorithmVariants(algorithms)

    fun showStatistics() {
        for (problem in problems) {
            val analyzer = calculateAlgorithmResults(problem)
            analyzer.forEach { (algorithm, results) ->
                println("$problem::$algorithm")
                results.print(System.out)
            }
        }
    }

    fun showWinners() {
        val winnerCounter = WinnerCounter()

        val progressBar = ProgressBar("Calculating winners", (problems.size * algorithmVariants.size).toLong())
        progressBar.use {
            for (problem in problems) {
                val algorithmResults = calculateAlgorithmResults(problem)
                for (algorithmVariant in algorithmVariants) {
                    val accumulatorsFromCSV = accumulatorsFromCSV(problem, algorithmVariant, runRange)
                    val averageAccumulator = accumulatorsFromCSV.average()
                    winnerCounter.update(averageAccumulator, algorithmVariant, problem, algorithmResults)
                    progressBar.step()
                }
            }
        }
        winnerCounter.printSummary()
    }

    fun printLatexComparisonTable(statistics : List<Stat>) {
        for (metric in metrics) {
            printMetricsComparisonTable(problems, algorithmVariants, runRange, metric, statistics)
        }
    }

    private fun calculateAlgorithmResults(problem: String): Map<String, AlgorithmStats> {
        val algorithmResults = mutableMapOf<String, AlgorithmStats>()
        for (algorithmVariant in algorithmVariants) {
            val accumulatorsFromCSV = accumulatorsFromCSV(problem, algorithmVariant, runRange)
            val algorithmResult = AlgorithmStats(algorithmVariant).apply {
                val metricsMap = mutableMapOf<String, MutableList<Double>>()
                for (accumulator in accumulatorsFromCSV) {
                    for (metric in metrics) {
                        val value = accumulator.get(metric.fullName, accumulator.size(metric.fullName) - 1)
                        metricsMap.getOrPut(metric.fullName, { mutableListOf() }).add(value as Double)
                    }
                }
                metricsMap.forEach { (metric, values) ->
                    add(
                        IndicatorResult(
                            metric,
                            values.toDoubleArray()
                        )
                    )
                }
            }
            algorithmResults[algorithmVariant] = algorithmResult
        }
        calculateStatisticalSignificance(algorithmResults)
        return algorithmResults
    }

    private fun calculateStatisticalSignificance(analyzerResults: Map<String, AlgorithmStats>) {
        for (metric in metrics) {
            calculateStatisticalSignificance(metric, analyzerResults)
        }
    }
}

fun calculateStatisticalSignificance(
    metric: QualityIndicator,
    analyzerResults: Map<String, AlgorithmStats>
) {
    val indicatorName = metric.fullName

    val algorithmVariants = analyzerResults.keys.toList()
    val kwTest = KruskalWallisTest(algorithmVariants.size)

    for (index in algorithmVariants.indices) {
        val algorithm = algorithmVariants[index]
        analyzerResults[algorithm]
            ?.get(indicatorName)
            ?.values
            ?.let { kwTest.addAll(it, index) }
    }

    if (!kwTest.test(SIGNIFICANCE_LEVEL)) {
        for (i in 0 until algorithmVariants.size - 1) {
            for (j in algorithmVariants.indices) {
                analyzerResults[algorithmVariants[i]]
                    ?.get(indicatorName)
                    ?.addIndifferentAlgorithm(
                        algorithmVariants[j]
                    )
                analyzerResults[algorithmVariants[j]]
                    ?.get(indicatorName)
                    ?.addIndifferentAlgorithm(
                        algorithmVariants[i]
                    )
            }
        }
    } else {
        for (i in 0 until algorithmVariants.size - 1) {
            for (j in i + 1 until algorithmVariants.size) {
                val mwTest = MannWhitneyUTest()

                mwTest.addAll(
                    analyzerResults[algorithmVariants[i]]
                        ?.get(indicatorName)?.values, 0
                )
                mwTest.addAll(
                    analyzerResults[algorithmVariants[j]]
                        ?.get(indicatorName)?.values, 1
                )

                if (!mwTest.test(SIGNIFICANCE_LEVEL)) {
                    analyzerResults[algorithmVariants[i]]
                        ?.get(indicatorName)
                        ?.addIndifferentAlgorithm(
                            algorithmVariants[j]
                        )
                    analyzerResults[algorithmVariants[j]]
                        ?.get(indicatorName)
                        ?.addIndifferentAlgorithm(
                            algorithmVariants[i]
                        )
                }
            }
        }
    }
}
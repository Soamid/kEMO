package pl.edu.agh.kemo.simulation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.PopulationIO
import toExistingFilepath
import java.io.File

fun saveMetrics(
    resultAccumulators: Map<String, MutableList<Accumulator>>,
    problemName: String,
    startRunNo: Int
) {
    resultAccumulators.entries
        .forEach { (algorithm, accumulators) ->
            accumulators.onEachIndexed { index, accumulator ->
                accumulator.saveCSV(metricsPath(algorithm, problemName, startRunNo + index).toExistingFilepath())
            }
        }
}

private fun metricsPath(
    algorithm: String,
    problemName: String,
    runNo: Int
) = "results/$algorithm/${problemName}_metrics_${runNo}.csv"

fun Population.save(algorithmName: String, problemName: String, runNo: Int) {
    PopulationIO.write(populationPath(algorithmName, problemName, runNo).toExistingFilepath(), this)
}

private fun populationPath(algorithmName: String, problemName: String, runNo: Int) =
    "results/$algorithmName/${problemName}_population_${runNo}.csv"

fun loadPopulations(
    problemName: String,
    algorithmNames: List<String>,
    runRange: IntRange
): Map<String, List<NondominatedPopulation>> = algorithmNames.associateWith { algorithmName ->
    runRange.map { runNo -> populationPath(algorithmName, problemName, runNo) }
        .map { PopulationIO.read(File(it)) }
        .map { NondominatedPopulation(it) }
}

fun accumulatorsFromCSV(
    problemName: String,
    algorithmName: String,
    runRange: IntRange
) : List<Accumulator> {
    return runRange.map { runNo -> metricsPath(algorithmName, problemName, runNo) }
        .map { csvReader().readAllWithHeader(File(it)) }
        .map { rows -> Accumulator().apply {
            rows.flatMap { it.entries }
                .forEach { (metric, value) -> add(metric.trim(), value.toDouble()) }
        }}
}



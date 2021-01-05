package pl.edu.agh.kemo.simulation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.PopulationIO
import pl.edu.agh.kemo.tools.toTrimmedCSV
import java.io.File

const val RESULTS_PATH = "results_global_archive"

fun saveMetrics(
    resultAccumulators: Map<String, MutableList<Accumulator>>,
    problemName: String,
    startRunNo: Int
) {
    resultAccumulators.entries
        .forEach { (algorithm, accumulators) ->
            accumulators.onEachIndexed { index, accumulator ->
                metricsPath(algorithm, problemName, startRunNo + index).toExistingFile()
                    .writeText(accumulator.toTrimmedCSV())
            }
        }
}

fun Accumulator.saveCSV(algorithm: String, problemName: String, runNo: Int) {
    metricsPath(algorithm, problemName, runNo).toExistingFile()
        .writeText(toTrimmedCSV())
}

fun String.toExistingFile(): File = File(this).apply { parentFile.mkdirs() }

private fun metricsPath(
    algorithm: String,
    problemName: String,
    runNo: Int
) = "$RESULTS_PATH/$algorithm/${problemName}_metrics_${runNo}.csv"

fun Population.save(algorithmName: String, problemName: String, runNo: Int) {
    PopulationIO.write(populationPath(algorithmName, problemName, runNo).toExistingFile(), this)
}

private fun populationPath(algorithmName: String, problemName: String, runNo: Int) =
    "$RESULTS_PATH/$algorithmName/${problemName}_population_${runNo}.csv"

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
): List<Accumulator> {
    return runRange.map { runNo -> metricsPath(algorithmName, problemName, runNo) }
        .map { csvReader().readAllWithHeader(File(it)) }
        .map { rows ->
            Accumulator().apply {
                rows.flatMap { it.entries }
                    .forEach { (metric, value) -> add(metric.trim(), value.toDouble()) }
            }
        }
}



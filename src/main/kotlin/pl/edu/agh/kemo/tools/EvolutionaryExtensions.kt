package pl.edu.agh.kemo.tools

import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.text.StringEscapeUtils
import org.moeaframework.analysis.collector.Observation
import org.moeaframework.analysis.collector.Observations
import org.moeaframework.core.Population
import org.moeaframework.core.Settings
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.RealVariable
import pl.edu.agh.kemo.algorithm.Node
import kotlin.math.pow
import kotlin.math.sqrt

fun <T> Iterable<T>.sample(amount: Int): List<T> = shuffled()
    .subList(0, amount)


fun Solution.variables(): List<RealVariable> =
    (0 until numberOfVariables).map { i -> getVariable(i) as RealVariable }


fun Population.average(): List<RealVariable>? {
    val solutionsVariables = map { solution -> solution.variables() }

    if (solutionsVariables.isNotEmpty()) {
        val variablesCount = solutionsVariables[0].size

        return (0 until variablesCount)
            .map { solutionIndex ->
                val representant = solutionsVariables[0][solutionIndex]
                val lowerBound = representant.lowerBound
                val upperBound = representant.upperBound
                val averageValue = solutionsVariables.map { variables -> variables[solutionIndex].value }
                    .average()
                    .let { minOf(maxOf(it, lowerBound), upperBound) }
                RealVariable(averageValue, lowerBound, upperBound)
            }
    }
    return null
}

fun List<RealVariable>.toRealMatrix(): RealMatrix =
    MatrixUtils.createRealMatrix(arrayOf(map { it.value }.toDoubleArray()))


fun List<RealVariable>.redundant(otherSolution: List<RealVariable>, minDistance: Double): Boolean {
    val solutionMatrix = toRealMatrix()
    val otherSolutionMatrix = otherSolution.toRealMatrix()

    val distance = solutionMatrix.subtract(otherSolutionMatrix).frobeniusNorm
//    println("distance: $distance, comparing to: $minDistance redundant: ${distance < minDistance}")
    return distance < minDistance
}

fun List<Node>.countAlive(): Int {
    return asSequence()
        .filter { it.alive }
        .count()
}


fun List<Observations>.average(): Observations {
    val meanAccumulator = Observations()
    if (!isEmpty()) {
        val sampleAccumulator = get(0)

        val nfeSteps = sampleAccumulator.map { it.nfe }.sorted()

        sampleAccumulator.keys().asSequence()
            .forEach { metric ->
                val results =
                    map { accumulator ->
                        accumulator.map { it[metric] as Number }
                    }
                val minSamplesCount = results.minOfOrNull { it.size }

                val runResults = (0 until minSamplesCount!!)
                    .map { runNo ->
                    val singleSampleResults = results.asSequence()
                        .filter { it.size > runNo }
                        .map { it[runNo].toDouble() }
                    when (metric) {
                        "NFE" -> Result(singleSampleResults.maxOrNull() ?: 0.0, 0.0)
                        else -> {
                            Result(singleSampleResults.average(), singleSampleResults.standardDev())
                        }
                    }
                }
                runResults.forEach {
                    meanAccumulator.add(metric, it.average)
                    meanAccumulator.add("${metric}_error", it.error)
                }
            }
    }
    return meanAccumulator
}

fun Observations.add(metric: String?, value: Double, nfe : Int = 0) {
    val observation = Observation(nfe)
    observation[metric] = value
    add(observation)
}

data class Result(val average: Double, val error: Double)

fun Sequence<Double>.standardDev(): Double {
    val mean = average()
    return fold(0.0, { accumulator, next -> accumulator + (next - mean).pow(2.0) })
        .let {
            sqrt(it / count())
        }
}

/**
 * Implementation copied from Accumulator#toCSV() method with separator " ," replaced with ","
 */
fun Observations.toTrimmedCSV(): String {
    val sb = StringBuilder()
    var firstValue = true

    // determine the ordering of the fields
    val fields: MutableSet<String> = LinkedHashSet()
    fields.add("NFE")
    if ("Elapsed Time" in keys()) {
        fields.add("Elapsed Time")
    }
    fields.addAll(keys())

    // create the header
    for (field in fields) {
        if (!firstValue) {
            sb.append(",")
        }
        sb.append(StringEscapeUtils.escapeCsv(field))
        firstValue = false
    }

    // create the data
    for (nfe in map { it.nfe }) {
        sb.append(Settings.NEW_LINE)
        firstValue = true
        for (field in fields) {
            if (!firstValue) {
                sb.append(",")
            }
            sb.append(StringEscapeUtils.escapeCsv(at(nfe)[field].toString()))
            firstValue = false
        }
    }
    return sb.toString()
}

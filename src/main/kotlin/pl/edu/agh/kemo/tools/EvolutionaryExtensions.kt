package pl.edu.agh.kemo.tools

import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.core.Population
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.RealVariable
import pl.edu.agh.kemo.algorithm.Node
import pl.edu.agh.kemo.algorithm.ParallelNode

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


fun List<Accumulator>.average(): Accumulator {
    val meanAccumulator = Accumulator()
    if (!isEmpty()) {
        val sampleAccumulator = get(0)
        val metrics = sampleAccumulator.keySet()

        metrics.asSequence()
            .forEach { metric ->
                val results =
                    map { accumulator ->
                    val samplesCount = if (metric in accumulator.keySet()) accumulator.size(metric) else 0
                    (0 until samplesCount).map { accumulator[metric, it] as Number }
                }
                val minSamplesCount = results.map { it.size }.min()

                val runResults = (0 until minSamplesCount!!).map { runNo ->
                    val singleSampleResults = results.asSequence()
                        .filter { it.size > runNo }
                        .map { it[runNo].toDouble() }
                    when (metric) {
                        "NFE" -> singleSampleResults.max()
                        else -> singleSampleResults.average()
                    }
                }
                runResults.forEach { meanAccumulator.add(metric, it) }
            }
    }
    return meanAccumulator
}

fun Accumulator.minSize() : Int =
    keySet().asSequence()
        .map { size(it) }
        .min()
        ?: 0

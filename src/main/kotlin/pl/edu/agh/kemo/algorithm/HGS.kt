package pl.edu.agh.kemo.algorithm

import org.apache.commons.math3.linear.MatrixUtils
import org.moeaframework.core.Algorithm
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.variable.RealVariable
import java.lang.Math.abs

typealias Driver = (Problem, Population, mutationEta: Double, mutationRate: Double) -> Algorithm

data class HGSConfiguration(
    val population: Population,
    val fitnessErrors: List<Double>,
    val costModifiers: List<Double>,
    val mutationEtas: List<Double>,
    val crossoverEtas: List<Double>,
    val referencePoint: List<Double>,
    val minProgressRatios: List<Double>,
    val comparisonMultipliers: List<Double>,
    val subPopulationSizes: List<Int>,
    val metaepochLength: Int,
    val maxLevel: Int,
    val maxSproutsCount: Int,
    val sproutiveness: Int
)

class HGS(private val problem: Problem, val driver: Driver? = null, private val parameters: HGSConfiguration) {

    private val minDistances: List<Double>

    init {
        val cornersDistance = calculateCornersDistance()
        minDistances = parameters.comparisonMultipliers.map { it * cornersDistance }
        println(cornersDistance)
        println(minDistances)
    }

    private fun calculateCornersDistance(): Double {
        val problemSample = problem.newSolution()
        val corners = IntRange(0, problemSample.numberOfVariables - 1)
            .map { i ->
                val dim = problemSample.getVariable(i) as RealVariable
                abs(dim.lowerBound - dim.upperBound)
            }
        val cornersMatrix = MatrixUtils.createRealMatrix(arrayOf(corners.toDoubleArray()))
        return cornersMatrix.frobeniusNorm
    }
}
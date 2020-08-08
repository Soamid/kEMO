package pl.edu.agh.kemo.algorithm

import org.apache.commons.math3.linear.MatrixUtils
import org.moeaframework.algorithm.AbstractAlgorithm
import org.moeaframework.core.Algorithm
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.variable.RealVariable
import java.lang.Math.abs

typealias Driver = (Problem, Population, mutationEta: Double, mutationRate: Double) -> Algorithm

data class HGSConfiguration(
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

class HGS(
    problem: Problem,
    val driver: Driver? = null,
    val population: Population,
    private val parameters: HGSConfiguration
) :
    AbstractAlgorithm(problem) {

    private val minDistances: List<Double>

    private val nodes: List<Node>

    private val levelNodes: Map<Int, List<Node>>

    private var root: Node? = null

    init {
        val cornersDistance = calculateCornersDistance()
        minDistances = parameters.comparisonMultipliers.map { it * cornersDistance }
        nodes = mutableListOf()
        levelNodes = (0 until parameters.maxLevel).associateWith { mutableListOf<Node>() }

        println(cornersDistance)
        println(minDistances)
    }

    override fun initialize() {
//        this.root = Node(problem, driver, 0,  )

        super.initialize()
    }

    private fun calculateCornersDistance(): Double {
        val problemSample = problem.newSolution()
        val corners = (0 until problemSample.numberOfVariables)
            .map { i ->
                val dim = problemSample.getVariable(i) as RealVariable
                abs(dim.lowerBound - dim.upperBound)
            }
        val cornersMatrix = MatrixUtils.createRealMatrix(arrayOf(corners.toDoubleArray()))
        return cornersMatrix.frobeniusNorm
    }

    override fun getResult(): NondominatedPopulation {
        TODO("Not yet implemented")
    }

    override fun iterate() {
        TODO("Not yet implemented")
    }

}
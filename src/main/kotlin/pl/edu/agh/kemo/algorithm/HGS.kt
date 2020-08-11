package pl.edu.agh.kemo.algorithm

import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.util.Precision
import org.moeaframework.algorithm.AbstractAlgorithm
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.variable.RealVariable
import pl.edu.agh.kemo.tools.sample
import java.lang.Math.abs
import kotlin.math.pow

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
    val driver: DriverBuilder<*>,
    val population: Population,
    private val parameters: HGSConfiguration
) :
    AbstractAlgorithm(problem) {

    private val minDistances: List<Double>

    private val nodes: MutableList<Node>

    private val levelNodes: Map<Int, MutableList<Node>>

    private val root: Node

    init {
        val cornersDistance = calculateCornersDistance()
        minDistances = parameters.comparisonMultipliers.map { it * cornersDistance }
        nodes = mutableListOf()
        levelNodes = (0 until parameters.maxLevel).associateWith { mutableListOf<Node>() }

        this.root = createRoot(problem)

        println(cornersDistance)
        println(minDistances)
    }

    private fun createRoot(problem: Problem): Node {
        val rootPopulation = Population().apply {
            addAll(population.sample(parameters.subPopulationSizes[0]))
        }
        return Node(problem, driver, 0, rootPopulation, parameters)
    }

    override fun initialize() {
        nodes.add(root)
        levelNodes[0]?.add(root)
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
        printStatus()
        runMetaepoch()
        trimSprouts()
        releaseNewSprouts()
        reviveRoot()
    }

    private fun printStatus() {
        println("all nodes: ${nodes.size}, alive: ${nodes.count { it.alive }}, ripe:  ${nodes.count { it.ripe }}")
        levelNodes.forEach {
            println(
                "level ${it.key}, " +
                        "alive: ${it.value.count { it.alive }}, " +
                        "ripe:  ${it.value.count { it.ripe }}"
            )
        }
    }

    private fun runMetaepoch() {
        numberOfEvaluations += levelNodes.keys.asSequence()
            .map { level ->
                levelNodes[level]?.asSequence()
                    ?.map { it.runMetaepoch() }
                    ?.map { calculateCost(level, it) }
                    ?.count()
                    ?: 0
            }.count()
    }

    private fun calculateCost(nodeLevel: Int, driverCost: Int): Int =
        (parameters.costModifiers[nodeLevel] * driverCost).toInt()

    private fun trimSprouts() {
        (parameters.maxLevel - 1 downTo 0).asSequence()
            .map { levelNodes[it] }
            .filterNotNull()
            .forEach { nodes ->
                trimNotProgressing(nodes)
                trimRedundant(nodes)
            }
    }

    private fun trimNotProgressing(nodes: List<Node>) {
        nodes.asSequence()
            .filter { it.alive }
            .filter { isNotProgressing(it) }
            .forEach {
                it.alive = false
                it.ripe = true
                it.recalculateCenter()
                println("KILLED not progressing: $it lvl=${it.level}")
            }
    }

    private fun isNotProgressing(node: Node): Boolean =
        node.previousHypervolume?.let {
            val progressRatio = parameters.minProgressRatios[node.level] / 2.0.pow(node.level)
            return it > 0 && node.hypervolume / ((it + Precision.EPSILON) - 1.0) < progressRatio
        } ?: false

    private fun trimRedundant(nodes: List<Node>) {

    }

    private fun releaseNewSprouts() {
        TODO("Not yet implemented")
    }

    private fun reviveRoot() {
        TODO("Not yet implemented")
    }
}
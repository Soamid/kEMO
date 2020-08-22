package pl.edu.agh.kemo.algorithm

import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.util.Precision
import org.moeaframework.algorithm.AbstractAlgorithm
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.variable.RealVariable
import pl.edu.agh.kemo.tools.countAlive
import pl.edu.agh.kemo.tools.loggerFor
import pl.edu.agh.kemo.tools.redundant
import pl.edu.agh.kemo.tools.sample
import java.lang.Math.abs
import kotlin.math.pow

data class HGSConfiguration(
    val fitnessErrors: List<Double>,
    val costModifiers: List<Double>,
    val mutationEtas: List<Double>,
    val mutationRates: List<Double>,
    val crossoverEtas: List<Double>,
    val crossoverRates: List<Double>,
    val referencePoint: List<Double>,
    val initialMinProgressRatios: List<Double>,
    val comparisonMultipliers: List<Double>,
    val subPopulationSizes: List<Int>,
    val metaepochLength: Int,
    val maxLevel: Int,
    val maxSproutsCount: Int,
    val sproutiveness: Int
)

class HGS(
    problem: Problem,
    val driverBuilder: DriverBuilder<*>,
    val population: Population,
    private val parameters: HGSConfiguration
) :
    AbstractAlgorithm(problem) {

    val levelNodes: Map<Int, List<Node>>

    val minDistances: List<Double>

    private val minProgressRatios: MutableList<Double>

    private val nodes: MutableList<Node>

    private val root: Node

    private var metaepochNumber = 1

    private var redundantKills = 0

    private val log = loggerFor<HGS>()

    init {
        val cornersDistance = calculateCornersDistance()
        minDistances = parameters.comparisonMultipliers.map { it * cornersDistance }
        minProgressRatios = parameters.initialMinProgressRatios.toMutableList()
        nodes = mutableListOf()
        levelNodes = (0..parameters.maxLevel).associateWith { mutableListOf<Node>() }

        this.root = createRoot()
    }

    private fun createRoot(): Node {
        val rootPopulation = Population().apply {
            addAll(population.sample(parameters.subPopulationSizes[0]))
        }
        return registerNode(rootPopulation, 0)
    }

    fun registerNode(sproutPopulation: Population, level: Int): Node {
        val node = Node(
            problem = problem,
            driverBuilder = driverBuilder,
            level = level,
            parameters = parameters,
            population = sproutPopulation
        )
        nodes.add(node)
        (levelNodes[level] as MutableList).add(node)
        return node
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

    override fun getResult(): NondominatedPopulation =
        nodes.asSequence()
            .map { it.population.toList() }
            .reduce { acc, subPopulation -> acc + subPopulation }
            .let { NondominatedPopulation(it) }

    override fun iterate() {
        printStatus()
        runMetaepoch()
        trimSprouts()
        releaseNewSprouts()
        reviveRoot()
        metaepochNumber++
    }

    private fun printStatus() {
        log.debug("SUMMARY #$metaepochNumber")
        log.debug("all nodes: ${nodes.size}, alive: ${nodes.count { it.alive }}, ripe:  ${nodes.count { it.ripe }}")
        levelNodes.forEach {
            log.debug(
                "level ${it.key}, " +
                        "alive: ${it.value.count { it.alive }}, " +
                        "ripe:  ${it.value.count { it.ripe }}"
            )
        }
        log.debug("current cost: ${getNumberOfEvaluations()}")
        log.debug("redundant kills: $redundantKills")
    }

    private fun runMetaepoch() {
        numberOfEvaluations = levelNodes.keys.asSequence()
            .map { level ->
                levelNodes[level]?.asSequence()
                    ?.map { it.runMetaepoch() }
                    ?.map { calculateCost(level, it) }
                    ?.sum()
                    ?: 0
            }.sum()
    }

    private fun calculateCost(nodeLevel: Int, driverCost: Int): Int =
        (parameters.costModifiers[nodeLevel] * driverCost).toInt()

    private fun trimSprouts() {
        (parameters.maxLevel downTo 0).asSequence()
            .map { levelNodes[it] }
            .filterNotNull()
            .filter { it.isNotEmpty() }
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
                log.debug("KILLED not progressing: $it lvl=${it.level}")
            }
    }

    private fun isNotProgressing(node: Node): Boolean =
        node.previousHypervolume?.let {
            val progressRatio = minProgressRatios[node.level] / 2.0.pow(node.level)
            return it > 0 && (node.hypervolume / (it + Precision.EPSILON) - 1.0) < progressRatio
        } ?: false

    private fun trimRedundant(nodes: List<Node>) {
        val aliveNodes = nodes.filter { it.alive }
        val deadNodes = nodes.filter { !it.alive && it.ripe }
        val processed = mutableListOf<Node>()

        for (sprout in aliveNodes) {
            val toCompare = deadNodes + processed
            sprout.recalculateCenter()

            sprout.center?.let { center ->
                if (isRedundant(toCompare, center, sprout.level)) {
                    sprout.alive = false
                    log.debug("KILLED redundant: $sprout lvl=${sprout.level}")
                    redundantKills++
                }
            }
            processed.add(sprout)
        }
    }

    private fun isRedundant(
        toCompare: List<Node>,
        center: List<RealVariable>,
        level: Int
    ): Boolean {
        return toCompare.asSequence()
            .map { it.center }
            .filterNotNull()
            .find { it.redundant(center, minDistances[level]) } != null
    }

    private fun releaseNewSprouts() {
        root.releaseSprouts(this)
    }

    private fun reviveRoot() {
        if (nodes.countAlive() == 0) {
            nodes.asSequence()
                .filter { it.ripe }
                .forEach { ripeNode ->
                    ripeNode.alive = true
                    ripeNode.ripe = false
                }
            (0..parameters.maxLevel)
                .forEach { minProgressRatios[it] = minProgressRatios[it] / 2 }
        }
    }
}
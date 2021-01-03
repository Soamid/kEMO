package pl.edu.agh.kemo.algorithm

import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.util.Precision
import org.moeaframework.algorithm.AbstractAlgorithm
import org.moeaframework.analysis.sensitivity.EpsilonHelper
import org.moeaframework.core.EpsilonBoxDominanceArchive
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.fitness.CrowdingDistanceFitnessEvaluator
import org.moeaframework.core.fitness.FitnessBasedArchive
import org.moeaframework.core.spi.ProblemFactory
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
    val mantissaBits: List<Int>,
    val metaepochLength: Int,
    val maxLevel: Int,
    val maxSproutsCount: Int,
    val sproutiveness: Int
)

fun interface NodeFactory {
    fun createNode(
        problem: Problem,
        driverBuilder: DriverBuilder<*>,
        level: Int,
        population: Population,
        problemReferenceSet: NondominatedPopulation,
        parameters: HGSConfiguration
    ): Node
}

class DefaultNodeFactory : NodeFactory {
    override fun createNode(
        problem: Problem,
        driverBuilder: DriverBuilder<*>,
        level: Int,
        population: Population,
        problemReferenceSet: NondominatedPopulation,
        parameters: HGSConfiguration
    ): Node = Node(problem, driverBuilder, level, population, problemReferenceSet, parameters)
}

open class HGS(
    problem: Problem,
    val driverBuilder: DriverBuilder<*>,
    val population: Population,
    protected val parameters: HGSConfiguration,
    private val nodeFactory: NodeFactory = DefaultNodeFactory(),
    protected val budget: Int? = null
) :
    AbstractAlgorithm(problem) {

    val levelNodes: Map<Int, List<Node>>

    val minDistances: List<Double>

    protected val minProgressRatios: MutableList<Double>

    protected val nodes: MutableList<Node>

    protected val root: Node

    protected var metaepochNumber = 1

    protected var redundantKills = 0

    private val log = loggerFor<HGS>()

    protected val problemReferenceSet: NondominatedPopulation

    protected var finalizedPopulations: MutableMap<Node, List<Solution>> = mutableMapOf()

    init {
        val cornersDistance = calculateCornersDistance()
        problemReferenceSet = ProblemFactory.getInstance().getReferenceSet(problem.name)
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
        if (level > 0 && levelNodes[level].isNullOrEmpty()) {
            log.info("$level: $numberOfEvaluations")
        }
        val node = createNode(level, sproutPopulation)
        nodes.add(node)
        (levelNodes[level] as MutableList).add(node)
        return node
    }

    protected open fun createNode(
        level: Int,
        sproutPopulation: Population
    ): Node = nodeFactory.createNode(
        problem = problem,
        problemReferenceSet = problemReferenceSet,
        driverBuilder = driverBuilder,
        level = level,
        parameters = parameters,
        population = sproutPopulation
    )

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
//        printStatus()
        val pop = finalizedPopulations.values
            .reduce { acc, subPopulation -> acc + subPopulation }
//               .let { createEpsilonBoxDominanceArchive(it) }
            .let { createFitnessBasedArchive(it) }
//        log.info("Pop size: ${pop.size()}")
        return pop
    }

    private fun createEpsilonBoxDominanceArchive(it: List<Solution>) =
        EpsilonBoxDominanceArchive(EpsilonHelper.getEpsilon(problem), it)

    private fun createFitnessBasedArchive(it: List<Solution>) =
        FitnessBasedArchive(
            CrowdingDistanceFitnessEvaluator(), when (problem.numberOfObjectives) {
                2 -> 100
                3 -> 150
                5 -> 800
                else -> 100
            }, it
        )

    override fun iterate() {
//        printStatus()
        finalizedPopulations = nodes.associateWith { it.finalizedPopulation.toList() }.toMutableMap()
        if (runMetaepoch()) {
            trimSprouts()
            releaseNewSprouts()
            reviveRoot()
            metaepochNumber++
        }
    }

    private fun printStatus() {
        log.info("SUMMARY #$metaepochNumber ($numberOfEvaluations evaluations)")
        log.info("all nodes: ${nodes.size}, alive: ${nodes.count { it.alive }}, ripe:  ${nodes.count { it.ripe }}")
        levelNodes.forEach {
            log.info(
                "level ${it.key}, " +
                        "alive: ${it.value.count { it.alive }}, " +
                        "ripe:  ${it.value.count { it.ripe }}"
            )
        }
        log.info("current cost: ${getNumberOfEvaluations()}")
        log.info("redundant kills: $redundantKills")
    }

    protected open fun runMetaepoch(): Boolean {
        for (level in levelNodes.keys.asSequence()) {
            levelNodes[level]?.forEach {
                val epochCost = it.runMetaepoch()
                numberOfEvaluations += calculateCost(level, epochCost)
                if (isBudgetMet()) {
                    return false
                }
                finalizedPopulations[it] = it.finalizedPopulation.toList()
            }
        }
        return true
    }

    protected fun isBudgetMet() = budget != null && numberOfEvaluations > budget

    protected fun calculateCost(nodeLevel: Int, driverCost: Int): Int =
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
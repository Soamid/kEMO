package pl.edu.agh.kemo.algorithm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.fitness.FitnessBasedArchive

open class ParallelHGS(
    problem: Problem,
    driverBuilder: DriverBuilder<*>,
    population: Population,
    parameters: HGSConfiguration,
    nodeFactory: NodeFactory = DefaultNodeFactory(),
    budget: Int? = null
) :
    HGS(problem, driverBuilder, population, parameters, nodeFactory, budget) {

    private val archive: FitnessBasedArchive = createFitnessBasedArchive(null)

    override fun runMetaepoch(): Boolean {
        return runBlocking {
            val nodeCosts = levelNodes.values.flatten()
                .filter { it.alive }
                .map { node ->
                    async(Dispatchers.Default) {
                        val cost = node.runMetaepoch()
                        Pair(node, calculateCost(node.level, cost))
                    }
                }
            val successfulNodes = nodeCosts.asFlow()
                .map { it.await() }
                .takeWhile { !isBudgetMet() }
                .onEach { (_, cost) -> numberOfEvaluations += cost }
                .onEach { (node, _) -> updateFinalizedPopulations(node) }

            return@runBlocking successfulNodes.count() == nodes.size
        }
    }

    override fun getResult(): NondominatedPopulation {
        return archive
    }

    protected open fun updateFinalizedPopulations(node: Node) {
        val solutions = node.finalizedPopulation.toList()
        finalizedPopulations[node] = solutions
        archive.addAll(solutions)
    }
}
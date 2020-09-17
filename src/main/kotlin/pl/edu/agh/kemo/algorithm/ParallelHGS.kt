package pl.edu.agh.kemo.algorithm

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.moeaframework.core.Population
import org.moeaframework.core.Problem

class ParallelHGS(
    problem: Problem,
    driverBuilder: DriverBuilder<*>,
    population: Population,
    parameters: HGSConfiguration
) :
    HGS(problem, driverBuilder, population, parameters) {

    override fun runMetaepoch() {
        runBlocking {
            val costs = levelNodes.values.flatten()
                .map { node ->
                    async(Dispatchers.Default) {
                        val cost = node.runMetaepoch()
                        calculateCost(node.level, cost)
                    }
                }
            numberOfEvaluations = costs.map { it.await() }.sum()

//            numberOfEvaluations = levelNodes.values.flatten().asFlow()
//                .flowOn(Dispatchers.Default)
//                .map {
//                    val cost = it.runMetaepoch()
//                    calculateCost(it.level, cost)
//                }.reduce(Int::plus)
        }
    }

    override fun createNode(
        level: Int,
        sproutPopulation: Population
    ): ParallelNode {
        return ParallelNode(
            problem = problem,
            problemReferenceSet = problemReferenceSet,
            driverBuilder = driverBuilder,
            level = level,
            parameters = parameters,
            population = sproutPopulation
        )
    }
}
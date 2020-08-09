package pl.edu.agh.kemo.algorithm

import org.moeaframework.core.Algorithm
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.indicator.Hypervolume

class Node(
    private val problem: Problem,
    driverBuilder: DriverBuilder<*>,
    private val level: Int,
    private var population: Population,
    private val parameters: HGSConfiguration
) {
    var alive: Boolean = true
    var ripe: Boolean = false
    var delegates: List<Solution> = listOf()

    private var previousHypervolume: Double = 0.0
    private var hypervolume: Double = 0.0
    private var relativeHypervolume : Double? = null

    val driver: Driver<*> =
        driverBuilder.create(problem, population, parameters.mutationEtas[level], parameters.crossoverEtas[level])


    fun runMetaepoch(): Int {
        println("population at the beginning: ${population.size()}")
        val costBeforeEpoch = driver.numberOfEvaluations
        repeat(parameters.metaepochLength) {
            driver.step()
        }
        val metaepochCost = driver.numberOfEvaluations - costBeforeEpoch
        val nondominatedPopulation = driver.result
        population = nondominatedPopulation // TODO ensure it's ok to reduce population here
        delegates = driver.nominateDelegates()

        updateDominatedHypervolume(nondominatedPopulation)

        return metaepochCost
    }

    private fun updateDominatedHypervolume(nondominatedPopulation: NondominatedPopulation) {
        previousHypervolume = hypervolume

        println("population: ${population.size()}")

        val resultHypervolume = Hypervolume(problem, nondominatedPopulation).run {
            evaluate(nondominatedPopulation)
        }
        relativeHypervolume?.let {
            hypervolume = resultHypervolume - it
        }
        if(relativeHypervolume == null) {
            relativeHypervolume = resultHypervolume
        }
        println("hypervolume relative: $relativeHypervolume")

    }
}
package kemo.driver

import org.moeaframework.algorithm.NSGAII
import org.moeaframework.algorithm.StandardAlgorithmsWithInjectedPopulation
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.NondominatedSorting
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.operator.InjectedInitialization
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class NSGAIIDriverBuilder : DriverBuilder<NSGAII> {

    override fun create(
        problem: Problem,
        population: Population,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double
    ): Driver<NSGAII> {

        val standardAlgorithms =
            StandardAlgorithmsWithInjectedPopulation(
                InjectedInitialization(problem, population.size(), population.toList())
            )

        val properties = Properties().apply {
            setProperty("populationSize", population.size().toString())
            setProperty("pm.distributionIndex", mutationEta.toString())
            setProperty("pm.rate", mutationRate.toString())
            setProperty("sbx.distributionIndex", crossoverEta.toString())
            setProperty("sbx.rate", crossoverRate.toString())
        }
        return NSGAIIDriver(standardAlgorithms.getAlgorithm("NSGAII", properties, problem) as NSGAII)
    }
}

class NSGAIIDriver(algorithm: NSGAII) : Driver<NSGAII>(algorithm) {

    override fun nominateDelegates(): List<Solution> =
        algorithm.population
            .filter { it.getAttribute(NondominatedSorting.RANK_ATTRIBUTE) as Int == 0 }

    override fun getPopulation(): Population = algorithm.population

    override fun getArchive(): NondominatedPopulation = algorithm.archive
}
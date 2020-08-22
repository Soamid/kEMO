package kemo.driver

import org.moeaframework.algorithm.NSGAII
import org.moeaframework.core.NondominatedSorting
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class NSGAIIDriverBuilder : DriverBuilder<NSGAII> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: Properties
    ): Driver<NSGAII> {
         properties.apply {
            setProperty("pm.distributionIndex", mutationEta.toString())
            setProperty("pm.rate", mutationRate.toString())
            setProperty("sbx.distributionIndex", crossoverEta.toString())
            setProperty("sbx.rate", crossoverRate.toString())
        }
        return NSGAIIDriver(algorithmProvider.getAlgorithm("NSGAII", properties, problem) as NSGAII)
    }
}

class NSGAIIDriver(algorithm: NSGAII) : Driver<NSGAII>(algorithm) {

    override fun nominateDelegates(): List<Solution> =
        algorithm.population
            .filter { it.getAttribute(NondominatedSorting.RANK_ATTRIBUTE) as Int == 0 }

    override fun getPopulation(): Population = algorithm.population
}
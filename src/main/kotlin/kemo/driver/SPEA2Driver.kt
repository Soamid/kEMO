package kemo.driver

import org.moeaframework.algorithm.NSGAII
import org.moeaframework.algorithm.SPEA2
import org.moeaframework.core.NondominatedSorting
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties


class SPEA2DriverBuilder : DriverBuilder<SPEA2> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: Properties,
        mantissaBits: Int
    ): Driver<SPEA2> {
        properties.apply {
            setProperty("pm.distributionIndex", mutationEta.toString())
            setProperty("pm.rate", mutationRate.toString())
            setProperty("sbx.distributionIndex", crossoverEta.toString())
            setProperty("sbx.rate", crossoverRate.toString())
        }
        return SPEA2Driver(algorithmProvider.getAlgorithm("SPEA2", properties, problem) as SPEA2, mantissaBits)
    }
}

class SPEA2Driver(algorithm: SPEA2, mantissaBits: Int) : Driver<SPEA2>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> =
        algorithm.population.toList()

    override fun getPopulation(): Population = algorithm.population
}
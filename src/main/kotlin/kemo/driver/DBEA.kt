package kemo.driver

import org.moeaframework.algorithm.DBEA
import org.moeaframework.algorithm.NSGAII
import org.moeaframework.core.NondominatedSorting
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class DBEADriverBuilder : DriverBuilder<DBEA> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<DBEA> {
        properties.apply {
            setDouble("pm.distributionIndex", mutationEta)
            setDouble("pm.rate", mutationRate)
            setDouble("sbx.distributionIndex", crossoverEta)
            setDouble("sbx.rate", crossoverRate)
        }
        return DBEADriver(algorithmProvider.getAlgorithm("DBEA", properties, problem) as DBEA, mantissaBits)
    }
}

class DBEADriver(algorithm: DBEA, mantissaBits: Int) : Driver<DBEA>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> =
        algorithm.result.toList()

    override fun getPopulation(): Population = algorithm.population
}

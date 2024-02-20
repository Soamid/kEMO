package kemo.driver

import org.moeaframework.algorithm.IBEA
import org.moeaframework.core.NondominatedSorting
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class IBEADriverBuilder : DriverBuilder<IBEA> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<IBEA> {
        properties.apply {
            setDouble("pm.distributionIndex", mutationEta)
            setDouble("pm.rate", mutationRate)
            setDouble("sbx.distributionIndex", crossoverEta)
            setDouble("sbx.rate", crossoverRate)
        }
        return IBEADriver(algorithmProvider.getAlgorithm("IBEA", properties, problem) as IBEA, mantissaBits)
    }
}

class IBEADriver(algorithm: IBEA, mantissaBits: Int) : Driver<IBEA>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> = algorithm.result.toList()

    override fun getPopulation(): Population = algorithm.population
}

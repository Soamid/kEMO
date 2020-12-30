package kemo.driver

import org.moeaframework.algorithm.CMAES
import org.moeaframework.algorithm.IBEA
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class MOCMAESDriverBuilder : DriverBuilder<CMAES> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: Properties,
        mantissaBits: Int
    ): Driver<CMAES> {
        properties.apply {
            setProperty("pm.distributionIndex", mutationEta.toString())
            setProperty("pm.rate", mutationRate.toString())
            setProperty("sbx.distributionIndex", crossoverEta.toString())
            setProperty("sbx.rate", crossoverRate.toString())
        }
        return CMAESDriver(algorithmProvider.getAlgorithm("MO-CMA-ES", properties, problem) as CMAES, mantissaBits)
    }
}

class CMAESDriver(algorithm: CMAES, mantissaBits: Int) : Driver<CMAES>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> = algorithm.result.toList()

    override fun getPopulation(): Population = algorithm.result
}

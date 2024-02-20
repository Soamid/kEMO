package kemo.driver

import org.moeaframework.algorithm.CMAES
import org.moeaframework.algorithm.IBEA
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.lang.reflect.Array.setDouble
import java.util.Properties

class MOCMAESDriverBuilder : DriverBuilder<CMAES> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<CMAES> {
        properties.apply {
            setDouble("pm.distributionIndex", mutationEta)
            setDouble("pm.rate", mutationRate)
            setDouble("sbx.distributionIndex", crossoverEta)
            setDouble("sbx.rate", crossoverRate)
        }
        return CMAESDriver(algorithmProvider.getAlgorithm("MO-CMA-ES", properties, problem) as CMAES, mantissaBits)
    }
}

class CMAESDriver(algorithm: CMAES, mantissaBits: Int) : Driver<CMAES>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> = algorithm.result.toList()

    override fun getPopulation(): Population = algorithm.result
}

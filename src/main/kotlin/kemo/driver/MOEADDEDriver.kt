package kemo.driver

import org.moeaframework.algorithm.jmetal.adapters.JMetalAlgorithmAdapter
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import org.uma.jmetal.solution.doublesolution.DoubleSolution
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder

class MOEADDEDriverBuilder : DriverBuilder<JMetalAlgorithmAdapter<DoubleSolution>> {
    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<JMetalAlgorithmAdapter<DoubleSolution>> {
        properties.apply {
            setDouble("pm.distributionIndex", mutationEta)
            setDouble("pm.rate", mutationRate)
            setDouble("sbx.distributionIndex", crossoverEta)
            setDouble("sbx.rate", crossoverRate)
        }
        return MOEADDEDriver(
            algorithmProvider.getAlgorithm(
                "MOEAD-JMetal",
                properties,
                problem
            ) as JMetalAlgorithmAdapter<DoubleSolution>, mantissaBits
        )
    }
}

class MOEADDEDriver(algorithm: JMetalAlgorithmAdapter<DoubleSolution>, mantissaBits: Int) :
    Driver<JMetalAlgorithmAdapter<DoubleSolution>>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> =
        getPopulation().toList()

    override fun getPopulation(): Population = algorithm.result
}
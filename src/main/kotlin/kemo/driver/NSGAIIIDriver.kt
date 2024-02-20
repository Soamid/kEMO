package kemo.driver

import org.moeaframework.algorithm.NSGAII
import org.moeaframework.core.Problem
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class NSGAIIIDriverBuilder : DriverBuilder<NSGAII> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<NSGAII> {
        properties.apply {
            setDouble("pm.distributionIndex", mutationEta)
            setDouble("pm.rate", mutationRate)
            setDouble("sbx.distributionIndex", crossoverEta)
            setDouble("sbx.rate", crossoverRate)
        }
        return NSGAIIDriver(algorithmProvider.getAlgorithm("NSGAIII", properties, problem) as NSGAII, mantissaBits)
    }
}


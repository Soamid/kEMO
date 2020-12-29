package kemo.driver

import org.moeaframework.algorithm.NSGAII
import org.moeaframework.core.Problem
import org.moeaframework.core.spi.AlgorithmProvider
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
        properties: Properties,
        mantissaBits: Int
    ): Driver<NSGAII> {
        properties.apply {
            setProperty("pm.distributionIndex", mutationEta.toString())
            setProperty("pm.rate", mutationRate.toString())
            setProperty("sbx.distributionIndex", crossoverEta.toString())
            setProperty("sbx.rate", crossoverRate.toString())
        }
        return NSGAIIDriver(algorithmProvider.getAlgorithm("NSGAIII", properties, problem) as NSGAII, mantissaBits)
    }
}


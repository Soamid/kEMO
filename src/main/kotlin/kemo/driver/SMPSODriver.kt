package kemo.driver

import org.moeaframework.algorithm.pso.initialized.SMPSO
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class SMPSODriverBuilder: DriverBuilder<SMPSO> {
    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<SMPSO> {
        properties.apply {
            setDouble("mutationProbability", mutationRate)
            setDouble("perturbationIndex", mutationEta)
        }
        return SMPSODriver(algorithmProvider.getAlgorithm("SMPSO", properties, problem) as SMPSO, mantissaBits)
    }
}

class SMPSODriver(algorithm: SMPSO, mantissaBits: Int) : Driver<SMPSO>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> = algorithm.leaders

    override fun getPopulation(): Population = Population(algorithm.particles)
}

package kemo.driver

import org.moeaframework.algorithm.pso.initialized.OMOPSO
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class OMOPSODriverBuilder: DriverBuilder<OMOPSO> {
    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: Properties,
        mantissaBits: Int
    ): Driver<OMOPSO> {
        properties.apply {
            setProperty("mutationProbability", mutationRate.toString())
            setProperty("perturbationIndex", mutationEta.toString())
        }
        return OMOPSODriver(algorithmProvider.getAlgorithm("OMOPSO", properties, problem) as OMOPSO, mantissaBits)
    }
}

class OMOPSODriver(algorithm: OMOPSO, mantissaBits: Int) : Driver<OMOPSO>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> = algorithm.result.toList()

    override fun getPopulation(): Population = Population(algorithm.particles)
}
package kemo.driver

import org.moeaframework.algorithm.MOEAD
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.Driver
import pl.edu.agh.kemo.algorithm.DriverBuilder
import java.util.Properties

class MOEADDriverBuilder : DriverBuilder<MOEAD> {

    override fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<MOEAD> {
        properties.apply {
            setDouble("pm.distributionIndex", mutationEta)
            setDouble("pm.rate", mutationRate)
            setDouble("sbx.distributionIndex", crossoverEta)
            setDouble("sbx.rate", crossoverRate)
        }
        return MOEADDriver(algorithmProvider.getAlgorithm("MOEAD", properties, problem) as MOEAD, mantissaBits)
    }
}

class MOEADDriver(algorithm: MOEAD, mantissaBits: Int) : Driver<MOEAD>(algorithm, mantissaBits) {

    override fun nominateDelegates(): List<Solution> =
        getPopulation().toList()

    override fun getPopulation(): Population = algorithm.population
}

private inline val MOEAD.population: Population
    get() = javaClass.getDeclaredField("population").let {
        it.isAccessible = true
        val values = it.get(this) as List<Any>
        val indClazz = Class.forName("org.moeaframework.algorithm.MOEAD\$Individual")
        val method = indClazz.getMethod("getSolution")
        method.isAccessible = true
        return@let Population(values.map { ind -> method.invoke(ind) as Solution })
    }

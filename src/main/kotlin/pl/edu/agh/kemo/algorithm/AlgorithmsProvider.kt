package pl.edu.agh.kemo.algorithm

import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm
import org.moeaframework.algorithm.DefaultAlgorithms
import org.moeaframework.algorithm.pso.initialized.OMOPSO
import org.moeaframework.core.Algorithm
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.initialization.InjectedInitialization
import org.moeaframework.util.TypedProperties

class AlgorithmsProvider(private val population: Population) : DefaultAlgorithms() {

    init {
        register( { properties, problem ->
            OMOPSO(
                problem,
                getMaxIterations(properties),
                InjectedInitialization(problem, population.toList())
            )
        }, "OMOPSO")
    }

    override fun getAlgorithm(name: String?, properties: TypedProperties?, problem: Problem?): Algorithm {
        val algorithm = super.getAlgorithm(name, properties, problem)

        if(algorithm is AbstractEvolutionaryAlgorithm) {
            algorithm.initialization = InjectedInitialization(problem, population.toList())
        }
        return algorithm
    }

    private fun getMaxIterations(properties: TypedProperties): Int {
        return if (properties.contains("maxIterations")) {
            properties.getInt("maxIterations")
        } else {
            val maxEvaluations = properties.getInt("maxEvaluations", 25000)
            val populationSize = properties.getInt("populationSize", properties.getInt("swarmSize", 100))
            maxEvaluations / populationSize
        }
    }
}

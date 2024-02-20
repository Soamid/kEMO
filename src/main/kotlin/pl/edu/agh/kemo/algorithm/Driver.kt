package pl.edu.agh.kemo.algorithm

import org.moeaframework.algorithm.AbstractAlgorithm
import org.moeaframework.algorithm.DefaultAlgorithms
import org.moeaframework.algorithm.StandardAlgorithmsWithInjectedPopulation
import org.moeaframework.core.Algorithm
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.initialization.InjectedInitialization
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.tools.trimMantissa
import pl.edu.agh.kemo.tools.variables
import java.io.NotSerializableException
import java.io.Serializable
import java.util.Properties


interface DriverBuilder<A : AbstractAlgorithm> {

    fun create(
        problem: Problem, population: Population, mutationEta: Double, mutationRate: Double, crossoverEta: Double,
        crossoverRate: Double, mantissaBits: Int
    ): Driver<A> {
        val properties = TypedProperties().apply {
            setInt("populationSize", population.size())
        }
        val algorithmProvider = object : DefaultAlgorithms() {
            override fun getAlgorithm(name: String?, properties: TypedProperties?, problem: Problem?): Algorithm {
                val algorithm = super.getAlgorithm(name, properties, problem)
                algorithm.set
                return algorithm
            }
        }
            StandardAlgorithmsWithInjectedPopulation(
                InjectedInitialization(problem, population.toList())
            )
        return create(
            problem,
            algorithmProvider,
            mutationEta,
            mutationRate,
            crossoverEta,
            crossoverRate,
            properties,
            mantissaBits
        )
    }

    fun create(
        problem: Problem,
        algorithmProvider: AlgorithmProvider,
        mutationEta: Double,
        mutationRate: Double,
        crossoverEta: Double,
        crossoverRate: Double,
        properties: TypedProperties,
        mantissaBits: Int
    ): Driver<A>
}


abstract class Driver<A : AbstractAlgorithm>(
    protected val algorithm: A,
    private val mantissaBits: Int
) : Algorithm {

    override fun getProblem(): Problem = algorithm.problem

    override fun getResult(): NondominatedPopulation = algorithm.result

    override fun step() {
//        if (algorithm.isInitialized) {
//            getPopulation().forEach { solution ->
//                solution.variables().forEach { variable ->
//                    variable.value = variable.value.trimMantissa(mantissaBits)
//                }
//            }
//        }
        algorithm.step()
    }

    override fun evaluate(solution: Solution?) = algorithm.evaluate(solution)

    override fun getNumberOfEvaluations(): Int = algorithm.numberOfEvaluations

    override fun isTerminated(): Boolean = algorithm.isTerminated

    override fun terminate() = algorithm.terminate()

    @Throws(NotSerializableException::class)
    override fun getState(): Serializable? = algorithm.state

    @Throws(NotSerializableException::class)
    override fun setState(state: Any?) = algorithm.setState(state)

    abstract fun nominateDelegates(): List<Solution>

    abstract fun getPopulation(): Population
}

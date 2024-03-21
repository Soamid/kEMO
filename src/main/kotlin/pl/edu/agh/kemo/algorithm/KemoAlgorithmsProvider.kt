package pl.edu.agh.kemo.algorithm

import org.apache.commons.lang3.reflect.MethodUtils
import org.apache.commons.lang3.reflect.TypeUtils
import org.apache.commons.text.WordUtils
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm
import org.moeaframework.algorithm.DefaultAlgorithms
import org.moeaframework.algorithm.MOEAD
import org.moeaframework.algorithm.jmetal.JMetalAlgorithms
import org.moeaframework.algorithm.jmetal.JMetalFactory
import org.moeaframework.algorithm.jmetal.adapters.BinaryProblemAdapter
import org.moeaframework.algorithm.jmetal.adapters.DoubleProblemAdapter
import org.moeaframework.algorithm.jmetal.adapters.JMetalAlgorithmAdapter
import org.moeaframework.algorithm.jmetal.adapters.PermutationProblemAdapter
import org.moeaframework.algorithm.jmetal.adapters.ProblemAdapter
import org.moeaframework.algorithm.pso.initialized.OMOPSO
import org.moeaframework.core.Algorithm
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.initialization.InjectedInitialization
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.core.variable.BinaryVariable
import org.moeaframework.core.variable.Permutation
import org.moeaframework.core.variable.RealVariable
import org.moeaframework.problem.ProblemException
import org.moeaframework.util.TypedProperties
import org.uma.jmetal.algorithm.AlgorithmBuilder
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder
import org.uma.jmetal.operator.mutation.MutationOperator
import org.uma.jmetal.solution.Solution
import org.uma.jmetal.solution.doublesolution.DoubleSolution
import org.uma.jmetal.util.errorchecking.JMetalException
import java.util.Arrays

class KemoAlgorithmsProvider(private val population: Population? = null) : AlgorithmProvider() {

    private val defaultAlgorithms: DefaultAlgorithms = object : DefaultAlgorithms() {
        init {
            if (population != null) {
                register({ properties, problem ->
                    OMOPSO(
                        problem,
                        getMaxIterations(properties),
                        InjectedInitialization(problem, population.toList())
                    )
                }, "OMOPSO")
            }
        }
    }

    private val jmetalAlgorithms: JMetalAlgorithms = JMetalAlgorithms()


    override fun getAlgorithm(name: String?, properties: TypedProperties, problem: Problem): Algorithm {
        val algorithm = if (name == "MOEAD-JMetal") {
            newMOEAD(properties, problem)
        } else defaultAlgorithms.getAlgorithm(name, properties, problem)
            ?: jmetalAlgorithms.getAlgorithm(name, properties, problem)

        if (population != null) {
            injectPopulation(algorithm, problem, name, population)
        }
        return algorithm ?: throw IllegalArgumentException("No algorithm found: $name")
    }

    private fun injectPopulation(
        algorithm: Algorithm?,
        problem: Problem,
        name: String?,
        population: Population
    ) {
        if (algorithm is AbstractEvolutionaryAlgorithm) {
            algorithm.initialization = InjectedInitialization(problem, population.toList())
        } else if (algorithm is MOEAD) {
            algorithm.initialization = InjectedInitialization(problem, population.toList())
        }
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

    @Throws(JMetalException::class)
    private fun newMOEAD(properties: TypedProperties, problem: Problem): Algorithm {
        val adapter: DoubleProblemAdapter = createDoubleProblemAdapter(problem)
        val crossover = JMetalFactory.getInstance().createDifferentialEvolution(adapter, properties)
        val mutation = JMetalFactory.getInstance().createMutationOperator(adapter, properties)
        val variant = MOEADBuilder.Variant.valueOf(properties.getString("variant", "MOEAD"))
        val builder = MOEADBuilder(adapter, variant).setCrossover(crossover)
            .setMutation(mutation as MutationOperator<DoubleSolution>?)
        loadProperties(properties, builder)
        builder.setDataDirectory("moead_weights")
        return JMetalAlgorithmAdapter(builder.build(), properties, adapter)
    }

    private fun createDoubleProblemAdapter(problem: Problem): DoubleProblemAdapter {
        val adapter: ProblemAdapter<*> = createProblemAdapter(problem) as? DoubleProblemAdapter
            ?: throw JMetalException("algorithm only supports problems with real decision variables")
        return adapter as DoubleProblemAdapter
    }

    private fun createProblemAdapter(problem: Problem): ProblemAdapter<out Solution<*>?>? {
        val types: MutableSet<Class<*>> = HashSet()
        val schema = problem.newSolution()
        for (i in 0 until schema.numberOfVariables) {
            types.add(schema.getVariable(i).javaClass)
        }
        if (types.isEmpty()) {
            throw ProblemException(problem, "Problem has no defined types")
        }
        if (types.size > 1) {
            throw ProblemException(
                problem, "Problem has multiple types defined, expected only one: " +
                        Arrays.toString(types.toTypedArray())
            )
        }
        val type = types.iterator().next()
        return if (RealVariable::class.java.isAssignableFrom(type)) {
            DoubleProblemAdapter(problem)
        } else if (BinaryVariable::class.java.isAssignableFrom(type)) {
            BinaryProblemAdapter(problem)
        } else if (Permutation::class.java.isAssignableFrom(type)) {
            PermutationProblemAdapter(problem)
        } else {
            throw ProblemException(
                problem, "Problems with type " + type.simpleName +
                        " are not currently supported by JMetal"
            )
        }
    }

    private fun loadProperties(properties: TypedProperties, builder: AlgorithmBuilder<*>) {
        val type: Class<*> = builder.javaClass
        for (method in type.methods) {
            if (method.name.startsWith("set") && method.parameterCount == 1
            ) {
                val methodName = method.name
                val property = WordUtils.uncapitalize(methodName.substring(3))
                val propertyType = method.parameterTypes[0]
                try {
                    if (TypeUtils.isAssignable(propertyType, Int::class.javaPrimitiveType) && properties.contains(
                            property
                        )
                    ) {
                        val value = properties.getInt(property, -1)
                        MethodUtils.invokeMethod(builder, methodName, value)
                    } else if (TypeUtils.isAssignable(
                            propertyType,
                            Double::class.javaPrimitiveType
                        ) && properties.contains(property)
                    ) {
                        val value = properties.getDouble(property, -1.0)
                        MethodUtils.invokeMethod(builder, methodName, value)
                    } else if (propertyType.isEnum && properties.contains(property)) {
                        val value = properties.getString(property, null)
                        MethodUtils.invokeStaticMethod(propertyType, "valueOf", value)
                    } else if (property == "maxIterations") {
                        val value = getMaxIterations(properties)
                        MethodUtils.invokeMethod(builder, methodName, value)
                    }
                } catch (e: Exception) {
                    System.err.println("Failed to set property $property")
                    e.printStackTrace()
                }
            }
        }
    }
}

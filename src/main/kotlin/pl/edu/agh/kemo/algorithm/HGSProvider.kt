package pl.edu.agh.kemo.algorithm

import kemo.driver.NSGAIIDriverBuilder
import kemo.driver.OMOPSODriverBuilder
import org.moeaframework.core.Algorithm
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.operator.RandomInitialization
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import java.lang.IllegalArgumentException
import java.util.Properties

class HGSProvider : AlgorithmProvider() {

    val driversMapping = mapOf(
        "NSGAII" to ::NSGAIIDriverBuilder,
        "OMOPSO" to ::OMOPSODriverBuilder
    )

    override fun getAlgorithm(name: String, properties: Properties, problem: Problem): Algorithm? {
        val typedProperties = TypedProperties(properties)
        val numberOfVariables = problem.numberOfVariables

        if(name.startsWith("HGS")) {
            val driverName = name.substringAfter('+')
            val driverProvider = driversMapping[driverName]
                ?: throw IllegalArgumentException("Unknown driver: $driverName")
            val hgsConfig = HGSConfiguration(
//                costModifiers = listOf(1.0, 1.0, 1.0),
//                fitnessErrors = listOf(0.0, 0.0, 0.0),
                costModifiers = listOf(0.1, 0.5, 1.0),
                fitnessErrors = listOf(0.1, 0.01, 0.0),
                comparisonMultipliers = listOf(1.0, 0.08, 0.020),
                maxLevel = 2,
                maxSproutsCount = 16,
                metaepochLength = 5,
//                initialMinProgressRatios = listOf(0.1, 0.0001, 0.001),
                initialMinProgressRatios = listOf(0.0, 0.00001, 0.0001),
                crossoverEtas = listOf(15.0, 20.0, 25.0),
                crossoverRates = listOf(0.9, 0.9, 0.9),
                mutationEtas = listOf(10.0, 12.0, 15.0),
                mutationRates = createMutationRates(numberOfVariables),
                referencePoint = listOf(), // TODO move to pl.edu.agh.kemo.algorithm.HGS init and calculate?
                sproutiveness = 3,
                subPopulationSizes = listOf(64, 20, 10)
            )
            val populationSize = typedProperties.getDouble("populationSize", 100.0).toInt()
            val population = RandomInitialization(problem, populationSize)
                .run { initialize() }
                .let { Population(it) }
            return HGS(
                population = population,
                driverBuilder = driverProvider(),
                problem = problem,
                parameters = hgsConfig
            )
        }
        return null
    }

    private fun createMutationRates(numberOfVariables: Int) : List<Double> {
        return (0..2).map { 1.0 / numberOfVariables }
    }
}
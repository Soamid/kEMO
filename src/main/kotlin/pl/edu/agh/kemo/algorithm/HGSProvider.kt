package pl.edu.agh.kemo.algorithm

import kemo.driver.NSGAIIDriverBuilder
import org.moeaframework.core.Algorithm
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.operator.RandomInitialization
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import java.util.Properties

class HGSProvider : AlgorithmProvider() {
    override fun getAlgorithm(name: String, properties: Properties, problem: Problem): Algorithm? {
        val typedProperties = TypedProperties(properties)
        val numberOfVariables = problem.numberOfVariables
        if (name.equals("HGS")) {
            val hgsConfig = HGSConfiguration(
                comparisonMultipliers = listOf(1.0, 0.08, 0.020),
                costModifiers = listOf(1.0, 1.0, 1.0),
                crossoverEtas = listOf(15.0, 20.0, 25.0),
                fitnessErrors = listOf(0.0, 0.0, 0.0),
                maxLevel = 2,
                maxSproutsCount = 16,
                metaepochLength = 5,
                minProgressRatios = listOf(0.0, 0.00001, 0.0001),
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
                driverBuilder = NSGAIIDriverBuilder(),
                problem = problem ?: throw IllegalArgumentException("No problem provided"),
                parameters = hgsConfig
            )
        }
        return null
    }

    private fun createMutationRates(numberOfVariables: Int) : List<Double> {
        return (0..2).map { 1.0 / numberOfVariables }
    }
}
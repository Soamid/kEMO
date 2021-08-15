package pl.edu.agh.kemo.algorithm

import kemo.driver.DBEADriverBuilder
import kemo.driver.IBEADriverBuilder
import kemo.driver.MOCMAESDriverBuilder
import kemo.driver.MOEADDriverBuilder
import kemo.driver.NSGAIIDriverBuilder
import kemo.driver.NSGAIIIDriverBuilder
import kemo.driver.OMOPSODriverBuilder
import kemo.driver.SMPSODriverBuilder
import kemo.driver.SPEA2DriverBuilder
import org.moeaframework.Executor
import org.moeaframework.core.Algorithm
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.operator.RandomInitialization
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.util.TypedProperties
import java.util.Properties

val MAX_EVALUATIONS_PROPERTY = "maxEvaluations"

enum class HGSType(val shortName: String, val displayName: String) {

    CLASSIC("HGS", "MO-mHGS"),
    PARALLEL("PHGS", "MO-EHGS"),
    HOPSO("HOPSO", "MO-$\\epislon$-EHGS");
}

enum class ProgressIndicatorType {
    HYPERVOLUME, IGD
}

fun TypedProperties.setProgressIndicatorTypes(vararg indicatorTypes: ProgressIndicatorType) =
    setStringArray("progressIndicator", indicatorTypes.map { it.toString() }.toTypedArray())

fun TypedProperties.setProgressIndicatorType(indicatorType: ProgressIndicatorType) =
    setString("progressIndicator", indicatorType.toString())

fun TypedProperties.getProgressIndicatorType(defaultValue: ProgressIndicatorType): ProgressIndicatorType =
    getString("progressIndicator", defaultValue.toString())
        .let { ProgressIndicatorType.valueOf(it) }

fun TypedProperties.getProgressIndicatorTypes(defaultValues: Array<ProgressIndicatorType>): List<ProgressIndicatorType> =
    getStringArray("progressIndicator", defaultValues.map { it.toString() }.toTypedArray())
        .map { ProgressIndicatorType.valueOf(it) }

fun Executor.withProgressIndicatorType(progressIndicatorType: ProgressIndicatorType) =
    withProperty("progressIndicator", progressIndicatorType.toString())

fun String.toHgsType() = HGSType.values().find { startsWith(it.shortName) }

fun String.isHgs() = HGSType.values().any { startsWith(it.shortName) }

fun String.cutHgs() = split("+").let { it[1] }

data class PropertyValue(val key: String, val value: Any)

fun TypedProperties.cartesian(): List<TypedProperties> {
    val setsOfProperties = properties.keys.asSequence()
        .map { it.toString() }
        .filter { getStringArray(it, null) != null }
        .map { key -> getStringArray(key, null).map { PropertyValue(key, it) }.toSet() }
        .toSet()
    return cartesianProduct(setsOfProperties).map { properties ->
         TypedProperties().apply {
             properties.forEach { fillByKey(it) }
         }
    }
}

fun <T> cartesianProduct(sets: Set<Set<T>>): Set<List<T>> =
    sets.fold(listOf(listOf<T>())) { acc, set ->
        acc.flatMap { list -> set.map { element -> list + element } }
    }.toSet()


fun TypedProperties.fillByKey(propertyValue: PropertyValue) = when (propertyValue.key) {
    "progressIndicator" -> setProgressIndicatorType(ProgressIndicatorType.valueOf(propertyValue.value.toString()))
    else -> setString(propertyValue.key, propertyValue.value.toString())
}.let { this }

fun TypedProperties.toInfoString() = properties.entries.joinToString { "(${it.key}=${it.value})" }

class HGSProvider : AlgorithmProvider() {

    val driversMapping = mapOf(
        "NSGAII" to ::NSGAIIDriverBuilder,
        "NSGAIII" to ::NSGAIIIDriverBuilder,
        "SMPSO" to ::SMPSODriverBuilder,
        "SPEA2" to ::SPEA2DriverBuilder,
        "IBEA" to ::IBEADriverBuilder,
        "MOEAD" to ::MOEADDriverBuilder,
        "DBEA" to ::DBEADriverBuilder,
        "OMOPSO" to ::OMOPSODriverBuilder,
        "MO-CMA-ES" to ::MOCMAESDriverBuilder
    )

    override fun getAlgorithm(name: String, properties: Properties, problem: Problem): Algorithm? {
        val typedProperties = TypedProperties(properties)
        val numberOfVariables = problem.numberOfVariables

        if (name.isHGSName()) {
            val driverName = name.substringAfter('+')
            val driverProvider = driversMapping[driverName]
                ?: throw IllegalArgumentException("Unknown driver: $driverName")
            val hgsConfig = HGSConfiguration(
                costModifiers = listOf(1.0, 1.0, 1.0),
                fitnessErrors = listOf(0.0, 0.0, 0.0),
//                costModifiers = listOf(0.1, 0.5, 1.0),
//                fitnessErrors = listOf(0.1, 0.01, 0.0),
                comparisonMultipliers = listOf(1.0, 0.08, 0.020),
                maxLevel = 2,
                maxSproutsCount = 16,
                metaepochLength = 5,
                initialMinProgressRatios = listOf(0.0, 0.00001, 0.0001),
                crossoverEtas = listOf(15.0, 20.0, 25.0),
                crossoverRates = listOf(0.9, 0.9, 0.9),
                mutationEtas = listOf(10.0, 12.0, 15.0),
                mutationRates = createMutationRates(numberOfVariables),
                mantissaBits = listOf(4, 16, 64),
                referencePoint = listOf(), // TODO move to pl.edu.agh.kemo.algorithm.HGS init and calculate?
                sproutiveness = 3,
                subPopulationSizes = listOf(64, 20, 10),
                progressIndicatorType = typedProperties.getProgressIndicatorType(defaultValue = ProgressIndicatorType.HYPERVOLUME)
            )
            val populationSize = typedProperties.getDouble("populationSize", 100.0).toInt()
            val population = RandomInitialization(problem, populationSize)
                .run { initialize() }
                .let { Population(it) }

            return when {
                name.startsWith(HGSType.CLASSIC) -> HGS(
                    population = population,
                    driverBuilder = driverProvider(),
                    problem = problem,
                    parameters = hgsConfig,
                    budget = typedProperties.getInt(MAX_EVALUATIONS_PROPERTY, Int.MAX_VALUE)
                )
                name.startsWith(HGSType.PARALLEL) -> ParallelHGS(
                    population = population,
                    driverBuilder = driverProvider(),
                    problem = problem,
                    parameters = hgsConfig,
                    budget = typedProperties.getInt(MAX_EVALUATIONS_PROPERTY, Int.MAX_VALUE)
                )
                name.startsWith(HGSType.HOPSO) -> ParallelHGS(
                    population = population,
                    driverBuilder = driverProvider(),
                    problem = problem,
                    parameters = hgsConfig,
                    nodeFactory = HopsoNodeFactory(),
                    budget = typedProperties.getInt(MAX_EVALUATIONS_PROPERTY, Int.MAX_VALUE)
                )
                else -> throw IllegalArgumentException("No such algorithm: $name")
            }
        }
        return null
    }

    private fun String.isHGSName(): Boolean = HGSType.values()
        .map { it.shortName }
        .any { startsWith(it) }

    private fun String.startsWith(hgsType: HGSType): Boolean = startsWith(hgsType.shortName)

    private fun createMutationRates(numberOfVariables: Int): List<Double> {
        return (0..2).map { 1.0 / numberOfVariables }
    }
}
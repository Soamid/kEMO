import org.moeaframework.algorithm.NSGAII
import org.moeaframework.core.*
import org.moeaframework.core.operator.RandomInitialization
import org.moeaframework.core.operator.TournamentSelection
import org.moeaframework.core.operator.real.SBX
import org.moeaframework.problem.ZDT.ZDT1
import pl.edu.agh.kemo.algorithm.HGS
import pl.edu.agh.kemo.algorithm.HGSConfiguration

fun main() {
    println("Hey ho Kotlin!")
    val zdT1 = ZDT1(2)
    val algorithm = NSGAII(
        zdT1, NondominatedSortingPopulation(),
        EpsilonBoxDominanceArchive(0.01), TournamentSelection(), SBX(0.5, 0.1),
        RandomInitialization(zdT1, 10)
    )

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
        referencePoint = listOf(), // TODO move to pl.edu.agh.kemo.algorithm.HGS init and calculate?
        sproutiveness = 3,
        subPopulationSizes = listOf(64, 20, 10)
    )
    val hgs = HGS(population = Population(), problem = ZDT1(), parameters = hgsConfig)
}
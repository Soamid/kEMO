import org.moeaframework.Executor
import org.moeaframework.algorithm.NSGAII
import org.moeaframework.core.*
import org.moeaframework.core.operator.RandomInitialization
import org.moeaframework.core.operator.TournamentSelection
import org.moeaframework.core.operator.real.SBX
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.problem.ZDT.ZDT1
import pl.edu.agh.kemo.algorithm.HGS
import pl.edu.agh.kemo.algorithm.HGSConfiguration
import pl.edu.agh.kemo.algorithm.HGSProvider

fun main() {
    println("Hey ho Kotlin!")
//    val zdT1 = ZDT1(2)
//    val algorithm = NSGAII(
//        zdT1, NondominatedSortingPopulation(),
//        EpsilonBoxDominanceArchive(0.01), TournamentSelection(), SBX(0.5, 0.1),
//        RandomInitialization(zdT1, 10)
//    )

    AlgorithmFactory.getInstance().addProvider(HGSProvider())

    Executor().withProblem("zdt1")
        .withAlgorithm("HGS")
        .withMaxEvaluations(10)
        .run()
}
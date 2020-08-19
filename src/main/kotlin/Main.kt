import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.spi.ProblemFactory
import pl.edu.agh.kemo.algorithm.HGSProvider


fun main() {
    println("Hey ho Kotlin!")
//    val zdT1 = ZDT1(2)
//    val algorithm = NSGAII(
//        zdT1, NondominatedSortingPopulation(),
//        EpsilonBoxDominanceArchive(0.01), TournamentSelection(), SBX(0.5, 0.1),
//        RandomInitialization(zdT1, 10)
//    )

    for (problemName in listOf("zdt1", "zdt2", "zdt3", "zdt4", "zdt6")) {

        val referenceSet = ProblemFactory.getInstance().getReferenceSet(problemName)

        AlgorithmFactory.getInstance().addProvider(HGSProvider())

        val instrumenter1 = Instrumenter()
            .withProblem(problemName)
            .attachHypervolumeCollector()
            .attachInvertedGenerationalDistanceCollector()
            .withFrequency(100)

        val resultPopulation = Executor().withProblem(problemName)
            .withAlgorithm("HGS")
            .withMaxEvaluations(4500)
            .withInstrumenter(instrumenter1)
            .runSeeds(10)

        val instrumenter2 = Instrumenter()
            .withProblem(problemName)
            .attachHypervolumeCollector()
            .attachInvertedGenerationalDistanceCollector()
            .withFrequency(100)

        val resultPopulation2 = Executor().withProblem(problemName)
            .withAlgorithm("NSGAII")
            .withProperty("populationSize", 64)
            .withMaxEvaluations(4500)
            .withInstrumenter(instrumenter2)
            .runSeeds(10)

//    Plot()
////        .add("original", PopulationIO.read(File("fronts/zdt1.moea")))
//        .add("4500", resultPopulation)
//        .setTitle("ZDT1")
//        .show()
        Plot()
            .add("hgs", instrumenter1.lastAccumulator, "Hypervolume")
            .add("nsgaii", instrumenter2.lastAccumulator, "Hypervolume")
            .setTitle("hgs+nsgaii")
            .save("plots/${problemName}_hv.png")
        Plot()
            .add("hgs", instrumenter1.lastAccumulator, "InvertedGenerationalDistance")
            .add("nsgaii", instrumenter2.lastAccumulator, "InvertedGenerationalDistance")
            .setTitle("hgs+nsgaii")
            .save("plots/${problemName}_igd.png")
    }
}
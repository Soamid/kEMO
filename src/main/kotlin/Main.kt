import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.core.spi.AlgorithmFactory
import pl.edu.agh.kemo.algorithm.HGSProvider
import java.io.File

fun String.toExistingFilepath() : File = File(this).apply { parentFile.mkdirs() }

fun main() {
    val budget = 4500
    val repetitions = 1
    val samplingFrequency = 100
    for(algorithmName in listOf("OMOPSO")) {
        for (problemName in listOf("zdt1")) {

            val hgsName = "HGS+$algorithmName"

            AlgorithmFactory.getInstance().addProvider(HGSProvider())

            val instrumenter1 = Instrumenter()
                .withProblem(problemName)
                .attachHypervolumeCollector()
                .attachInvertedGenerationalDistanceCollector()
                .withFrequency(samplingFrequency)

            val resultPopulation = Executor().withProblem(problemName)
                .withAlgorithm(hgsName)
                .withMaxEvaluations(budget)
                .withInstrumenter(instrumenter1)
                .runSeeds(repetitions)

            val instrumenter2 = Instrumenter()
                .withProblem(problemName)
                .attachHypervolumeCollector()
                .attachInvertedGenerationalDistanceCollector()
                .withFrequency(samplingFrequency)

            val resultPopulation2 = Executor().withProblem(problemName)
                .withAlgorithm(algorithmName)
                .withProperty("populationSize", 64)
                .withMaxEvaluations(budget)
                .withInstrumenter(instrumenter2)
                .runSeeds(repetitions)

            Plot()
                .add(hgsName, instrumenter1.lastAccumulator, "Hypervolume")
                .add(algorithmName, instrumenter2.lastAccumulator, "Hypervolume")
                .setTitle(hgsName)
                .save("plots/$algorithmName/${problemName}_hv.png".toExistingFilepath())
            Plot()
                .add("hgs", instrumenter1.lastAccumulator, "InvertedGenerationalDistance")
                .add("nsgaii", instrumenter2.lastAccumulator, "InvertedGenerationalDistance")
                .setTitle("hgs+nsgaii")
                .save("plots/$algorithmName/${problemName}_igd.png".toExistingFilepath())
        }
    }
}
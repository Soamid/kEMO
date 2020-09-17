import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.algorithm.PeriodicAction
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.core.Settings
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.spi.ProblemFactory
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.tools.WinnerCounter
import pl.edu.agh.kemo.tools.average
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

fun String.toExistingFilepath(): File = File(this).apply { parentFile.mkdirs() }

fun main() {
    val budget = 4500
    val repetitions = 3
    val samplingFrequency = 2
    val winnerCounter = WinnerCounter()
    val algorithmsAccumulators = mutableMapOf<String, Accumulator>()

    var hgsElapsedTime = 0L
    var bareElapsedTime = 0L

    for (problemName in listOf(
        "zdt1" , "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
    )) {
        val problemKey = "core.indicator.hypervolume_refpt.$problemName"

        val problem = ProblemFactory.getInstance().getProblem(problemName)
        val referencePoint = (0 until problem.numberOfObjectives).map { 50.0 }.toDoubleArray()

        Settings.PROPERTIES.setDoubleArray(problemKey, referencePoint)

        for (algorithmName in listOf("NSGAII")) {
            println("Processing... $algorithmName, $problemName")
            val hgsResultAccumulators = mutableListOf<Accumulator>()
            val bareResultAccumulators = mutableListOf<Accumulator>()

            val hgsName = "PHGS+$algorithmName"

            for (runNo in 1..repetitions) {

                AlgorithmFactory.getInstance().addProvider(HGSProvider())

                val hgsInstrumenter = Instrumenter()
                    .withProblem(problemName)
                    .attachHypervolumeCollector()
                    .attachInvertedGenerationalDistanceCollector()
                    .withFrequency(samplingFrequency)
                    .withFrequencyType(PeriodicAction.FrequencyType.STEPS)

                hgsElapsedTime += measureTimeMillis {
                    Executor().withProblem(problemName)
                        .withAlgorithm(hgsName)
                        .withMaxEvaluations(budget)
                        .withInstrumenter(hgsInstrumenter)
                        .run()
                }

                val bareInstrumenter = Instrumenter()
                    .withProblem(problemName)
                    .attachHypervolumeCollector()
                    .attachInvertedGenerationalDistanceCollector()
                    .withFrequency(samplingFrequency)
                    .withFrequencyType(PeriodicAction.FrequencyType.STEPS)

                bareElapsedTime += measureTimeMillis {
                    Executor().withProblem(problemName)
                        .withAlgorithm(algorithmName)
                        .withProperty("populationSize", 64)
                        .withMaxEvaluations(budget)
                        .withInstrumenter(bareInstrumenter)
                        .run()
                }

                hgsResultAccumulators.add(hgsInstrumenter.lastAccumulator)
                bareResultAccumulators.add(bareInstrumenter.lastAccumulator)
            }

            val averageHgsAccumulator = hgsResultAccumulators.average()
            val averageBareAccumulator = bareResultAccumulators.average()

            hgsResultAccumulators.forEach {
                println("Accumulator for run #${hgsResultAccumulators.indexOf(it)}")
                println(it.toCSV())
            }

            algorithmsAccumulators[algorithmName] = averageBareAccumulator
            algorithmsAccumulators[hgsName] = averageHgsAccumulator


            winnerCounter.update(averageBareAccumulator, algorithmName, problemName)
            winnerCounter.update(averageHgsAccumulator, hgsName, problemName)

            Plot()
                .add(hgsName, averageHgsAccumulator, "Hypervolume")
                .add(algorithmName, averageBareAccumulator, "Hypervolume")
                .setTitle(hgsName)
                .save("plots/$algorithmName/${problemName}_hv.png".toExistingFilepath())
            Plot()
                .add("hgs", averageHgsAccumulator, "InvertedGenerationalDistance")
                .add("nsgaii", averageBareAccumulator, "InvertedGenerationalDistance")
                .setTitle("hgs+nsgaii")
                .save("plots/$algorithmName/${problemName}_igd.png".toExistingFilepath())
        }

        val summaryHvPlot = Plot()
        val summaryIgdPlot = Plot()

        algorithmsAccumulators.forEach {
            summaryHvPlot.add(it.key, it.value, "Hypervolume")
            summaryIgdPlot.add(it.key, it.value, "InvertedGenerationalDistance")
        }
        summaryHvPlot.setTitle("$problemName (Hypervolume)")
            .save("plots/summary/${problemName}_hv.png".toExistingFilepath())
        summaryIgdPlot.setTitle("$problemName (IGD)")
            .save("plots/summary/${problemName}_igd.png".toExistingFilepath())

    }
    winnerCounter.printSummary()
    println("Bare elapsed time = $bareElapsedTime")
    println("HGS elapsed time = $hgsElapsedTime")
}


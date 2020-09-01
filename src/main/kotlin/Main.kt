import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.algorithm.PeriodicAction
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.core.spi.AlgorithmFactory
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.tools.WinnerCounter
import pl.edu.agh.kemo.tools.average
import java.io.File

fun String.toExistingFilepath(): File = File(this).apply { parentFile.mkdirs() }

fun main() {
    val budget = 4500
    val repetitions = 1
    val samplingFrequency = 2
    val winnerCounter = WinnerCounter()
    val algorithmsAccumulators = mutableMapOf<String, Accumulator>()
    for (problemName in listOf(
        "zdt1", "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
    )) {
        for (algorithmName in listOf("OMOPSO", "NSGAII")) {
            println("Processing... $algorithmName, $problemName")
            val hgsResultAccumulators = mutableListOf<Accumulator>()
            val bareResultAccumulators = mutableListOf<Accumulator>()

            val hgsName = "HGS+$algorithmName"

            for (runNo in 1..repetitions) {

                AlgorithmFactory.getInstance().addProvider(HGSProvider())

                val hgsInstrumenter = Instrumenter()
                    .withProblem(problemName)
                    .attachHypervolumeCollector()
                    .attachInvertedGenerationalDistanceCollector()
                    .withFrequency(samplingFrequency)
                    .withFrequencyType(PeriodicAction.FrequencyType.STEPS)

                val hgsResults = Executor().withProblem(problemName)
                    .withAlgorithm(hgsName)
                    .withMaxEvaluations(budget)
                    .withInstrumenter(hgsInstrumenter)
                    .run()

                val bareInstrumenter = Instrumenter()
                    .withProblem(problemName)
                    .attachHypervolumeCollector()
                    .attachInvertedGenerationalDistanceCollector()
                    .withFrequency(samplingFrequency)
                    .withFrequencyType(PeriodicAction.FrequencyType.STEPS)

                val bareAlgorithmResults = Executor().withProblem(problemName)
                    .withAlgorithm(algorithmName)
                    .withProperty("populationSize", 64)
                    .withMaxEvaluations(budget)
                    .withInstrumenter(bareInstrumenter)
                    .run()

                hgsResultAccumulators.add(hgsInstrumenter.lastAccumulator)
                bareResultAccumulators.add(bareInstrumenter.lastAccumulator)
            }

            val averageHgsAccumulator = hgsResultAccumulators.average()
            val averageBareAccumulator = bareResultAccumulators.average()

            hgsResultAccumulators.forEach {
                println("Accumulator for run #${hgsResultAccumulators.indexOf(it)}")
                println(it.toCSV())}

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
}


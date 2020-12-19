package pl.edu.agh.kemo.simulation

import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.algorithm.PeriodicAction
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.analysis.plot.Plot
import org.moeaframework.core.Settings
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.spi.ProblemFactory
import pl.edu.agh.kemo.algorithm.HGSProvider
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.tools.WinnerCounter
import pl.edu.agh.kemo.tools.average
import pl.edu.agh.kemo.tools.loggerFor
import toExistingFilepath
import java.util.EnumSet
import kotlin.system.measureTimeMillis

abstract class Simulation(
    protected val repetitions : Int = 10,
    protected val problems : List<String>,
    protected val algorithms : List<String>,
    protected val hgsTypes : EnumSet<HGSType>
) {
    private val log = loggerFor<Simulation>()

    private val winnerCounter = WinnerCounter()

    fun run() {
        AlgorithmFactory.getInstance().addProvider(HGSProvider())

        val algorithmTimes = mutableMapOf<String, Long>()

        for (problemName in problems) {
            val problemKey = "core.indicator.hypervolume_refpt.$problemName"

            val problem = ProblemFactory.getInstance().getProblem(problemName)
            val referencePoint = (0 until problem.numberOfObjectives).map { 50.0 }.toDoubleArray()

            Settings.PROPERTIES.setDoubleArray(problemKey, referencePoint)

            val algorithmsAccumulators = mutableMapOf<String, Accumulator>()

            for (algorithmName in algorithms) {
                println("Processing... $algorithmName, $problemName")

                val hgsAlgorithms = hgsTypes.map { "${it.shortName}+$algorithmName" }

                val algorithmVariants = hgsAlgorithms + algorithmName

                val resultAccumulators = algorithmVariants.associateWith { mutableListOf<Accumulator>() }

                for (runNo in 1..repetitions) {

                    for (algorithmVariant in algorithmVariants) {
                        val instrumenter = Instrumenter()
                            .withProblem(problemName)
                            .attachHypervolumeCollector()
                            .attachInvertedGenerationalDistanceCollector()
                            .also { configureInstrumenter(it) }


                        val runTime = measureTimeMillis {
                            Executor().withProblem(problemName)
                                .withAlgorithm(algorithmVariant)
                                .withProperty("populationSize", 64)
                                .withInstrumenter(instrumenter)
                                .also { configureExecutor(it) }
                                .run()
                        }

                        algorithmTimes[algorithmVariant] = (algorithmTimes[algorithmVariant] ?: 0) + runTime
                        resultAccumulators[algorithmVariant]?.add(instrumenter.lastAccumulator)
                    }

                    val averageAccumulators = resultAccumulators.mapValues { it.value.average() }
                    algorithmsAccumulators.putAll(averageAccumulators)

                    resultAccumulators.forEach { (variant, accumulators) ->
                        log.debug("$problemName : $variant:")
                        accumulators.forEach {
                            log.debug("Accumulator for run #${accumulators.indexOf(it)}")
                            log.debug(it.toCSV())
                        }
                    }
                    averageAccumulators.forEach { (variant, accumulator) ->
                        winnerCounter.update(accumulator, variant, problemName)
                    }

                    listOf("InvertedGenerationalDistance", "Hypervolume")
                        .forEach { metric ->
                            val plot = Plot().apply {
                                averageAccumulators.forEach { (variant, accumulator) ->
                                    add(variant, accumulator, metric)
                                }
                                setTitle(algorithmName)
                            }
                            plot.save("plots/$algorithmName/${problemName}_$metric.png".toExistingFilepath())
                        }
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

            log.info("## GLOBAL SUMMARY")
            winnerCounter.printSummary()

            algorithmTimes.forEach { algorithm, elapsedTime ->
                log.info("$algorithm elapsed time = $elapsedTime")
            }
        }
    }

    abstract fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter

    abstract fun configureExecutor(executor: Executor) : Executor
}
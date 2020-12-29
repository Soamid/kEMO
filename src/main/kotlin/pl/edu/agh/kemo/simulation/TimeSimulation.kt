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
import toExistingFilepath
import java.util.EnumSet
import kotlin.system.measureTimeMillis

class TimeSimulation(
    private val maxTime: Long = 1_000L,
    private val samplingFrequency: Int = 100,
    repetitions: Int = 10,
    problems: List<String>,
    algorithms: List<String>,
    hgsTypes: EnumSet<HGSType>,
    metrics: EnumSet<QualityIndicator> = EnumSet.allOf(QualityIndicator::class.java),
    startRunNo: Int = 0
) : Simulation(repetitions, problems, algorithms, hgsTypes, metrics, startRunNo) {

    override fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter {
        return instrumenter.withFrequency(samplingFrequency)
            .withFrequencyType(PeriodicAction.FrequencyType.EVALUATIONS)
    }

    override fun configureExecutor(problem: String, algorithm: String, runNo: Int, executor: Executor): Executor {
        return executor.withMaxTime(maxTime)
    }
}
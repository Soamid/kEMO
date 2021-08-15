package pl.edu.agh.kemo.simulation

import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.algorithm.PeriodicAction
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.HGSType
import java.util.EnumSet

class TimeSimulation(
    private val maxTime: Long = 1_000L,
    private val samplingFrequency: Int = 100,
    repetitions: Int = 10,
    populationSize: Int = DEFAULT_POPULATION_SIZE,
    problems: List<String>,
    algorithms: List<String>,
    hgsTypes: EnumSet<HGSType>,
    metrics: EnumSet<QualityIndicator> = EnumSet.allOf(QualityIndicator::class.java),
    startRunNo: Int = 0,
    propertiesSets: TypedProperties? = null
) : Simulation(repetitions, populationSize, problems, algorithms, hgsTypes, metrics, startRunNo, propertiesSets) {

    override fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter {
        return instrumenter.withFrequency(samplingFrequency)
            .withFrequencyType(PeriodicAction.FrequencyType.EVALUATIONS)
    }

    override fun configureExecutor(problem: String, algorithm: String, runNo: Int, executor: Executor): Executor {
        return executor.withMaxTime(maxTime)
    }
}
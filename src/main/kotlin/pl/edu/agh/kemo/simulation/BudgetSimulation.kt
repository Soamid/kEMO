package pl.edu.agh.kemo.simulation

import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.algorithm.PeriodicAction
import pl.edu.agh.kemo.algorithm.HGSType
import java.util.EnumSet

class BudgetSimulation(
    private val budget: Int = 4500,
    private val samplingFrequency: Int = 100,
    repetitions: Int = 10,
    problems: List<String>,
    algorithms: List<String>,
    hgsTypes: EnumSet<HGSType>
) : Simulation(repetitions, problems, algorithms, hgsTypes) {

    override fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter {
        return instrumenter.withFrequency(samplingFrequency)
            .withFrequencyType(PeriodicAction.FrequencyType.EVALUATIONS)
    }

    override fun configureExecutor(executor: Executor): Executor {
        return executor.withMaxEvaluations(budget)
    }
}
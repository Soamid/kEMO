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
    hgsTypes: EnumSet<HGSType>,
    metrics: EnumSet<QualityIndicator> = EnumSet.allOf(QualityIndicator::class.java),
    startRunNo: Int = 0
) : Simulation(repetitions, problems, algorithms, hgsTypes, metrics, startRunNo) {

    override fun configureInstrumenter(instrumenter: Instrumenter): Instrumenter {
        return instrumenter.withFrequency(samplingFrequency)
            .withFrequencyType(PeriodicAction.FrequencyType.EVALUATIONS)
    }

    override fun configureExecutor(problem: String, algorithm: String, runNo: Int, executor: Executor): Executor {
        val taskName = "$problem::$algorithm ($runNo)"
        return executor.withMaxEvaluations(budget)
            .withProgressListener(ProgressDisplay(taskName, budget.toLong()) { event -> event.currentNFE.toLong() })
    }
}
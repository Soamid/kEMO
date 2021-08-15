package pl.edu.agh.kemo.simulation

import org.moeaframework.Executor
import org.moeaframework.Instrumenter
import org.moeaframework.algorithm.PeriodicAction
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.HGSType
import java.util.EnumSet

class BudgetSimulation(
    private val budget: Int = 4500,
    private val samplingFrequency: Int = 100,
    repetitions: Int = DEFAULT_REPETITIONS,
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

    override fun configureExecutor(problem: String, variantName: String, runNo: Int, executor: Executor): Executor {
        val taskName = "$problem::$variantName ($runNo)"
        return executor.withMaxEvaluations(budget)
            .withProgressListener(ProgressDisplay(taskName, budget.toLong()) { event -> event.currentNFE.toLong() })
    }
}
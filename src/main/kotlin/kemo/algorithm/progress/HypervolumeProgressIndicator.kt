package kemo.algorithm.progress

import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Problem
import org.moeaframework.core.indicator.Hypervolume

class HypervolumeProgressIndicator(val problem: Problem, val referenceSet: NondominatedPopulation) : ProgressIndicator {

    private var relativeHypervolume: Double? = null

    override var previousValue: Double? = null
        private set

    override var currentValue: Double = 0.0
        private set


    override fun updateProgress(population: NondominatedPopulation) {
        previousValue = currentValue
//        val minPoint = (0 until problem.numberOfObjectives).map { 0.0 }.toDoubleArray()
        val resultHypervolume = Hypervolume(problem, referenceSet).run {
            evaluate(population)
        }
        relativeHypervolume?.let {
            currentValue = resultHypervolume - it
        }
        if (relativeHypervolume == null) {
            relativeHypervolume = resultHypervolume
        }
    }
}
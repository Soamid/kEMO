package kemo.algorithm.progress

import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Problem
import org.moeaframework.core.indicator.Hypervolume

class HypervolumeProgressIndicator( problem: Problem,  referenceSet: NondominatedPopulation)
    : AbstractProgressIndicator(0.0) {

    private val hypervolume = Hypervolume(problem, referenceSet)

    private var relativeHypervolume: Double? = null

    override fun updateProgress(population: NondominatedPopulation) {
        previousValue = currentValue
//        val minPoint = (0 until problem.numberOfObjectives).map { 0.0 }.toDoubleArray()
        val resultHypervolume = hypervolume.evaluate(population)

        relativeHypervolume?.let {
            currentValue = resultHypervolume - it
        }
        if (relativeHypervolume == null) {
            relativeHypervolume = resultHypervolume
        }
    }
}
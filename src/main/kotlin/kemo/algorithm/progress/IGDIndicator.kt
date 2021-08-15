package kemo.algorithm.progress

import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Problem
import org.moeaframework.core.indicator.InvertedGenerationalDistance

class IGDIndicator(problem: Problem, referenceSet: NondominatedPopulation) :
    AbstractProgressIndicator(Double.MAX_VALUE) {

    private val igd = InvertedGenerationalDistance(problem, referenceSet)

    override fun updateProgress(population: NondominatedPopulation) {
        previousValue = currentValue
        currentValue = igd.evaluate(population)
    }
}
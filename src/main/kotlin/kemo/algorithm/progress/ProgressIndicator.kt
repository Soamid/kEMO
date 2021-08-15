package kemo.algorithm.progress

import org.moeaframework.core.NondominatedPopulation
import pl.edu.agh.kemo.algorithm.Node
import pl.edu.agh.kemo.algorithm.ProgressIndicatorType

interface ProgressIndicator {

    val previousValue: Double?

    val currentValue: Double

    fun updateProgress(population: NondominatedPopulation)

    companion object {

        fun forNode(node: Node, type: ProgressIndicatorType) : ProgressIndicator = when (type) {
            ProgressIndicatorType.HYPERVOLUME -> HypervolumeProgressIndicator(node.problem, node.problemReferenceSet)
            ProgressIndicatorType.IGD -> IGDIndicator(node.problem, node.problemReferenceSet)
        }
    }
}
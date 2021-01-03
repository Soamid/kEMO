package pl.edu.agh.kemo.algorithm

import org.moeaframework.analysis.sensitivity.EpsilonHelper
import org.moeaframework.core.EpsilonBoxDominanceArchive
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem

class HopsoNodeFactory : NodeFactory {
    override fun createNode(
        problem: Problem,
        driverBuilder: DriverBuilder<*>,
        level: Int,
        population: Population,
        problemReferenceSet: NondominatedPopulation,
        parameters: HGSConfiguration
    ): Node = HopsoNode(problem, driverBuilder, level, population, problemReferenceSet, parameters)
}

class HopsoNode(
    problem: Problem,
    driverBuilder: DriverBuilder<*>,
    level: Int,
    population: Population,
    problemReferenceSet: NondominatedPopulation,
    parameters: HGSConfiguration
) : Node(problem, driverBuilder, level, population, problemReferenceSet, parameters) {

    private val archive: EpsilonBoxDominanceArchive =
        EpsilonBoxDominanceArchive(EpsilonHelper.getEpsilon(problem))

    override val finalizedPopulation: Population
        get() = archive

    override fun runMetaepoch(): Int {
        val epochCost = super.runMetaepoch()
        archive.addAll(delegates)
        delegates = archive.toList()
        return epochCost
    }
}
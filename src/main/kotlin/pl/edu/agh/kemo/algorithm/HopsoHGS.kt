package pl.edu.agh.kemo.algorithm

import org.moeaframework.core.EpsilonBoxDominanceArchive
import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.fitness.CrowdingDistanceFitnessEvaluator
import org.moeaframework.core.fitness.FitnessBasedArchive

class HopsoHGS(
    problem: Problem,
    driverBuilder: DriverBuilder<*>,
    population: Population,
    parameters: HGSConfiguration,
    budget: Int? = null
) :
    ParallelHGS(problem, driverBuilder, population, parameters, HopsoNodeFactory(), budget) {

    private val archive: FitnessBasedArchive =
        FitnessBasedArchive(CrowdingDistanceFitnessEvaluator(), population.size())  //EpsilonBoxDominanceArchive(0.0075)


    override fun updateFinalizedPopulations(node: Node) {
        super.updateFinalizedPopulations(node)
        archive.addAll(node.finalizedPopulation)
    }

//    override fun getResult(): NondominatedPopulation = archive
}
package pl.edu.agh.kemo.algorithm

import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem

class ParallelNode(
    problem: Problem,
    driverBuilder: DriverBuilder<*>,
    level: Int,
    population: Population,
    problemReferenceSet: NondominatedPopulation,
    parameters: HGSConfiguration
) : Node(problem, driverBuilder, level, population, problemReferenceSet, parameters) {


}
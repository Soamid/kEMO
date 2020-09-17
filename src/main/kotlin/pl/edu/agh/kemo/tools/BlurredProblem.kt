package pl.edu.agh.kemo.tools

import org.moeaframework.core.PRNG
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution

class BlurredProblem(val innerProblem: Problem, private val error: Double) : Problem {

    override fun getName(): String = innerProblem.name

    override fun getNumberOfVariables(): Int = innerProblem.numberOfVariables

    override fun getNumberOfObjectives(): Int = innerProblem.numberOfObjectives

    override fun getNumberOfConstraints(): Int = innerProblem.numberOfConstraints

    override fun evaluate(solution: Solution?) {
        innerProblem.evaluate(solution)
        solution?.objectives
            ?.map { PRNG.nextGaussian(it, error) }
            ?.let { newObjectives -> solution.objectives = newObjectives.toDoubleArray() }
    }

    override fun newSolution(): Solution = innerProblem.newSolution()

    override fun close() = innerProblem.close()
}
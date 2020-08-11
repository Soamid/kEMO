package pl.edu.agh.kemo.tools

import org.moeaframework.core.Population
import org.moeaframework.core.variable.RealVariable

fun <T> Iterable<T>.sample(amount : Int) : List<T> = shuffled()
    .subList(0, amount)



fun Population.mean(): List<RealVariable>? {
    val solutionsVariables = map { solution ->
        (0 until solution.numberOfVariables).map { i -> solution.getVariable(i) as RealVariable }
    }

    if (solutionsVariables.isNotEmpty()) {
        val variablesCount = solutionsVariables[0].size

        return (0 until variablesCount)
            .map { solutionIndex ->
                val representant = solutionsVariables[0][solutionIndex]
                val lowerBound = representant.lowerBound
                val upperBound = representant.upperBound
                val averageValue = solutionsVariables.map { variables -> variables[solutionIndex].value }
                    .average()
                RealVariable(averageValue, lowerBound, upperBound)
            }
    }
    return null
}
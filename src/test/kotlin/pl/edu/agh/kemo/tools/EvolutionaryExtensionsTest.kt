package pl.edu.agh.kemo.tools

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.moeaframework.analysis.collector.Observations
import org.moeaframework.core.Population
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.RealVariable

class EvolutionaryExtensionsTest : StringSpec({

    "choose random elements from a list" {
        // given
        val elements = listOf(1, 2, 3, 4, 5)

        // when
        val sampled = elements.sample(3)

        // then
        println(sampled)
        sampled.size shouldBe 3
        elements shouldContainAll sampled
    }

    "attempt to sample more elements than size of a a list should end with an error" {
        // given
        val elements = listOf(1, 2, 3, 4, 5)

        // when list is sampled with more than 5 elements
        // then
        shouldThrow<IndexOutOfBoundsException> { elements.sample(10) }
    }

    "calculate mean value of population" {
        // given
        val lowerBound = 0.0
        val upperBound = 7.0

        val solution1 = createSolution(listOf(1.0, 2.0, 3.0), lowerBound, upperBound)
        val solution2 = createSolution(listOf(4.0, 5.0, 6.0), lowerBound, upperBound)

        val population = Population(listOf(solution1, solution2))

        // when
        val meanSolution = population.average()

        // then
        meanSolution shouldNotBe null
        meanSolution?.shouldHaveSize(3)

        meanSolution?.get(0)?.value shouldBe 2.5
        meanSolution?.get(1)?.value shouldBe 3.5
        meanSolution?.get(2)?.value shouldBe 4.5

        meanSolution?.forAll {
            it.lowerBound shouldBe lowerBound

            it.upperBound shouldBe upperBound
        }
    }

    "redundant variables check (with respect to min distance" {
        // given
        val lowerBound = 0.0
        val upperBound = 7.0

        val variables1 = createVariables(listOf(1.0, 2.0, 3.0), lowerBound, upperBound)
        val variables2 = createVariables(listOf(4.0, 5.0, 6.0), lowerBound, upperBound)

        // when distance is ~5.1961
        val notRedundantDistance = variables1.redundant(variables2, 5.0)
        val redundantDistance = variables1.redundant(variables2, 5.2)

        // then
        notRedundantDistance shouldBe false
        redundantDistance shouldBe true
    }


    "mean Accumulator value is calculated correctly (preserving min NFE metric)" {
        // given
        val accumulator1 =
            createAccumulator(
                mapOf(
                    "Hypervolume" to listOf(1.0, 2.0, 3.0),
                    "IGD" to listOf(4.0, 5.0, 6.0),
                ),
                nfeSequence = listOf(1001, 2000, 3002)
            )
        val accumulator2 =
            createAccumulator(
                mapOf(
                    "Hypervolume" to listOf(4.0, 5.0, 6.0),
                    "IGD" to listOf(7.0, 8.0, 10.0)
                ),
                nfeSequence = listOf(1000, 2005, 3000)
            )

        val accumulators = listOf(accumulator1, accumulator2)

        // when
        val meanAccumulator = accumulators.average()

        // then
        meanAccumulator.size() shouldBe 3
        meanAccumulator.at(1000)["Hypervolume"] shouldBe 2.5
        meanAccumulator.at(2000)["Hypervolume"] shouldBe 3.5
        meanAccumulator.at(3000)["Hypervolume"] shouldBe 4.5

        meanAccumulator.at(1000)["IGD"] shouldBe 5.5
        meanAccumulator.at(2000)["IGD"] shouldBe 6.5
        meanAccumulator.at(3000)["IGD"] shouldBe 8
    }
})

fun createAccumulator(resultsData: Map<String, List<Double>>, nfeSequence: List<Int>): Observations {
    val accumulator = Observations()
    resultsData.keys.forEach { metric ->
        (0 until resultsData[metric]!!.size).forEach {
            accumulator.add(
                metric,
                resultsData[metric]!![it],
                nfeSequence[it]
            )
        }
    }
    return accumulator
}

fun createSolution(values: List<Double>, lowerBound: Double, upperBound: Double): Solution =
    createVariables(values, lowerBound, upperBound)
        .let { variables ->
            Solution(3, 0).apply {
                variables.indices.forEach { setVariable(it, variables[it]) }
            }
        }

private fun createVariables(
    values: List<Double>,
    lowerBound: Double,
    upperBound: Double
) = values.map { RealVariable(it, lowerBound, upperBound) }


package pl.edu.agh.kemo.tools

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.moeaframework.analysis.collector.Accumulator
import org.moeaframework.core.Population
import org.moeaframework.core.Solution
import org.moeaframework.core.variable.RealVariable
import java.lang.IndexOutOfBoundsException

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
                    "Hypervolume" to listOf(1.0, 2.0, 3.0, 4.0),
                    "IGD" to listOf(4.0, 5.0, 6.0),
                    "NFE" to listOf(1001.0, 2000.0, 3002.0)
                )
            )
        val accumulator2 =
            createAccumulator(
                mapOf(
                    "Hypervolume" to listOf(4.0, 5.0, 6.0),
                    "IGD" to listOf(7.0, 8.0, 10.0),
                    "NFE" to listOf(1000.0, 2005.0, 3000.0)
                )
            )

        val accumulators = listOf(accumulator1, accumulator2)

        // when
        val meanAccumulator = accumulators.average()

        // then
        meanAccumulator.size("Hypervolume") shouldBe 3
        meanAccumulator["Hypervolume", 0] shouldBe 2.5
        meanAccumulator["Hypervolume", 1] shouldBe 3.5
        meanAccumulator["Hypervolume", 2] shouldBe 4.5

        meanAccumulator.size("IGD") shouldBe 3
        meanAccumulator["IGD", 0] shouldBe 5.5
        meanAccumulator["IGD", 1] shouldBe 6.5
        meanAccumulator["IGD", 2] shouldBe 8

        meanAccumulator.size("NFE") shouldBe 3
        meanAccumulator["NFE", 0] shouldBe 1000.0
        meanAccumulator["NFE", 1] shouldBe 2000.0
        meanAccumulator["NFE", 2] shouldBe 3000.0
    }
})

fun createAccumulator(resultsData: Map<String, List<Double>>): Accumulator {
    val accumulator = Accumulator()
    resultsData.keys.forEach { metric ->
        resultsData[metric]?.forEach { accumulator.add(metric, it) }
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


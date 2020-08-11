package pl.edu.agh.kemo.tools

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
        val meanSolution = population.mean()

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
})

fun createSolution(values: List<Double>, lowerBound: Double, upperBound: Double): Solution =
    values.map { RealVariable(it, lowerBound, upperBound) }
        .let { variables ->
            Solution(3, 0).apply {
                variables.indices.forEach { setVariable(it, variables[it]) }
            }
        }


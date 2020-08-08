package pl.edu.agh.kemo.tools

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.lang.IndexOutOfBoundsException

class RandomExtensionsKtTest : StringSpec({

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
        shouldThrow<IndexOutOfBoundsException> { elements.sample(10)  }
    }
})

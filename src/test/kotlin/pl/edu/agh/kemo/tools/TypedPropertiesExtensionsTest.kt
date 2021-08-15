package pl.edu.agh.kemo.tools

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.moeaframework.util.TypedProperties
import pl.edu.agh.kemo.algorithm.cartesian

class TypedPropertiesExtensionsTest : StringSpec({

    "cartesian product of typed properties sets can be created" {
        // given
        val typedProperties = TypedProperties()
        typedProperties.setStringArray("key1", arrayOf("1", "2", "3"))
        typedProperties.setStringArray("key2", arrayOf("a", "b", "c"))
        typedProperties.setStringArray("key3", arrayOf("!", "?"))

        // when
        val cartesianProductProperties = typedProperties.cartesian()

        // then
        cartesianProductProperties.forEach {
            println("NEW SET:")
            it.properties.entries.forEach { println("${it.key} : ${it.value}") }
        }
        cartesianProductProperties.size shouldBe 18
    }
})
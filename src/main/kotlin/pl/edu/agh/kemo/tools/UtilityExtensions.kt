package pl.edu.agh.kemo.tools

import org.moeaframework.Analyzer
import org.slf4j.LoggerFactory
import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.simulation.QualityIndicator
import java.util.EnumSet

inline fun <reified T : Any> loggerFor() = LoggerFactory.getLogger(T::class.java)

fun <K, V> Map<K, List<V>>.pairs(): List<Pair<K, V>> = flatMap { (key, values) -> values.map { Pair(key, it) } }

fun Double.trimMantissa(trimOffset: Int): Double {
    val bits = toBits().toString(2)
    val digitsOffset = 11
    val erasingSymbol = if (this > 0) "0" else "1"
    val trimOffsetFromStart = digitsOffset + trimOffset
    if (trimOffsetFromStart < bits.length) {
        val newBits =
            bits.replaceRange(trimOffsetFromStart, bits.length, erasingSymbol.repeat(bits.length - trimOffsetFromStart))
        return Double.fromBits(newBits.toLong(2))
    }
    return this
}

fun EnumSet<HGSType>.algorithmVariants(baseAlgorithms: List<String>): List<String> =
    baseAlgorithms.flatMap { algorithmVariants(it) }

fun EnumSet<HGSType>.algorithmVariants(baseAlgorithm: String): List<String> {
    val hgsAlgorithms = map { "${it.shortName}+$baseAlgorithm" }
    return hgsAlgorithms + baseAlgorithm
}

fun Analyzer.withMetrics(metrics: EnumSet<QualityIndicator>): Analyzer {
    metrics.forEach {
        when (it) {
            QualityIndicator.HYPERVOLUME -> includeHypervolume()
            QualityIndicator.IGD -> includeInvertedGenerationalDistance()
            QualityIndicator.SPACING -> includeSpacing()
        }
    }
    return this
}
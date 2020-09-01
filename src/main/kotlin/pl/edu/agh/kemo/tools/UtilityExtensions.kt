package pl.edu.agh.kemo.tools

import org.slf4j.LoggerFactory

inline fun <reified T : Any> loggerFor() = LoggerFactory.getLogger(T::class.java)


fun Double.trimMantissa(trimOffset: Int): Double {
    val bits = toBits().toString(2)
    val digitsOffset = 11
    val erasingSymbol = if (this > 0) "0" else "1"
    val trimOffsetFromStart = digitsOffset + trimOffset
    if(trimOffsetFromStart < bits.length) {
        val newBits =
            bits.replaceRange(trimOffsetFromStart, bits.length, erasingSymbol.repeat(bits.length - trimOffsetFromStart))
        return Double.fromBits(newBits.toLong(2))
    }
    return this
}
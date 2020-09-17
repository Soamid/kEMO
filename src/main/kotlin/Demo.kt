import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.system.measureTimeMillis


suspend fun performRequest(request: Int): String {
    println("start performing")
    delay(1000) // imitate long-running asynchronous work
    return "response $request"
}

fun main() = runBlocking { // this: CoroutineScope
    val c = AtomicLong()

    val time = measureTimeMillis {
        coroutineScope {
            val jobs = (1..4).map {
                launch {
                    println("hehe $it")
                    for (i in 1..10) {
                        val number =  calculateBigPrime(i, it)
                        println("$it : calculated prime $i : ${number}")
                        if (i == 4) {
                            cancel()
                        }
                    }
                    println("koniec $it")
                }
            }
            println("poza")
            jobs.forEach { it.join() }
        }
    }
    println(time)
}

suspend fun calculateBigPrime(primeNr: Int, coroutineNr: Int): BigInteger? {
    hehe()
    val veryBig = BigInteger(2000, Random())
    return veryBig.nextProbablePrime()
}

suspend fun hehe() {

}
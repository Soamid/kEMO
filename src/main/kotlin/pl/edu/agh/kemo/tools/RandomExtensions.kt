package pl.edu.agh.kemo.tools

fun <T> List<T>.sample(amount : Int) : List<T> =  (0 until size)
    .shuffled().subList(0, amount)
    .map { get(it) }
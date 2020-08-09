package pl.edu.agh.kemo.tools

fun <T> Iterable<T>.sample(amount : Int) : List<T> = shuffled()
    .subList(0, amount)
package pl.edu.agh.kemo.simulation

import pl.edu.agh.kemo.algorithm.HGSType

abstract class Simulation(
    val repetitions : Int = 10,
    val problems : List<String>,
    val algorithms : List<String>,
    val hgsType : HGSType
) {
    abstract fun run()
}
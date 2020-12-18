import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.simulation.TimeSimulation
import java.io.File

fun String.toExistingFilepath(): File = File(this).apply { parentFile.mkdirs() }

fun main() {
//    BudgetSimulation(
//        algorithms = listOf("OMOPSO"),
//        problems = listOf(
//            "zdt1", "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
//        ),
//        budget = 4500,
//        hgsType = HGSType.CLASSIC
//    ).run()

    TimeSimulation(
        algorithms = listOf("OMOPSO"),
        problems = listOf(
            "zdt1", "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
        ),
        maxTime = 1_000L,
        hgsType = HGSType.PARALLEL
    ).run()
}


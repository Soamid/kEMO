import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.simulation.BudgetSimulation
import pl.edu.agh.kemo.simulation.TimeSimulation
import java.io.File
import java.util.EnumSet

fun String.toExistingFilepath(): File = File(this).apply { parentFile.mkdirs() }

fun main() {
    BudgetSimulation(
        algorithms = listOf("NSGAII"),
        problems = listOf(
            "zdt1", "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
        ),
        budget = 50000,
        hgsTypes = EnumSet.of(HGSType.PARALLEL, HGSType.HOPSO),
        repetitions = 10
    ).run()

//    TimeSimulation(
//        algorithms = listOf("NSGAII"),
//        problems = listOf(
//            "zdt1", "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
//        ),
//        maxTime = 1_000L,
//        hgsTypes =  EnumSet.of(HGSType.CLASSIC, HGSType.PARALLEL)
//    ).run()
}


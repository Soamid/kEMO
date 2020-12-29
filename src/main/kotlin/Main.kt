import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.simulation.BudgetSimulation
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.TimeSimulation
import java.io.File
import java.util.EnumSet

fun String.toExistingFilepath(): File = File(this).apply { parentFile.mkdirs() }

fun main() {
    BudgetSimulation(
        algorithms = listOf("NSGAII", "SPEA2"),
        problems = listOf(
            "zdt1",
            "zdt2",
            "zdt3",
            "zdt4",
            "zdt6",
            "UF1",
            "UF2",
            "UF3",
            "UF4",
            "UF5",
            "UF6" //,"UF7", "UF8", "UF9"
//            "UF11",
//            "UF12",
//            "UF13"
        ),
        budget = 300_000,
        samplingFrequency = 1000,
        hgsTypes = EnumSet.of(HGSType.PARALLEL),
        metrics = EnumSet.of(QualityIndicator.IGD, QualityIndicator.SPACING),
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


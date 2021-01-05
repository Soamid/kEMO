import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.simulation.BudgetSimulation
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.SIMULATION_EXECUTOR
import pl.edu.agh.kemo.simulation.Simulation
import pl.edu.agh.kemo.simulation.StatisticsGenerator
import pl.edu.agh.kemo.simulation.TimeSimulation
import java.io.File
import java.util.EnumSet

fun main() {
    BudgetSimulation(
        algorithms = listOf("NSGAIII"),
        problems = listOf(
//            "zdt1",
//            "zdt2",
//            "zdt3",
//            "zdt4",
//            "zdt6",
//            "UF1",
//            "UF2",
//            "UF3",
//            "UF4",
//            "UF5",
//            "UF6" ,
//            "UF7",
            "UF8",
            "UF9",
            "UF10",
            "UF11",
//            "UF12",
//            "UF13"
//            "DTLZ1-3"
        ),
        budget = 300_000,
        samplingFrequency = 1000,
        hgsTypes = EnumSet.of(HGSType.CLASSIC),
        metrics = EnumSet.of(QualityIndicator.IGD, QualityIndicator.SPACING, QualityIndicator.HYPERVOLUME),
        repetitions = 30,
        startRunNo = 0
    ).run()

//    TimeSimulation(
//        algorithms = listOf("NSGAII"),
//        problems = listOf(
//            "zdt1", "zdt2", "zdt3", "zdt4", "zdt6", "UF1", "UF2", "UF3", "UF4", "UF5", "UF6" //,"UF7", "UF9"
//        ),
//        maxTime = 5_000L,
//        hgsTypes = EnumSet.of(HGSType.CLASSIC, HGSType.PARALLEL, HGSType.HOPSO),
//        repetitions = 1,
//    ).run()

    SIMULATION_EXECUTOR.shutdown()

}


import pl.edu.agh.kemo.algorithm.HGSType
import pl.edu.agh.kemo.simulation.PlotGenerator
import pl.edu.agh.kemo.simulation.QualityIndicator
import pl.edu.agh.kemo.simulation.StatisticsGenerator
import java.util.EnumSet

fun main() {
    PlotGenerator(
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
//            "UF6",
//            "UF7",
            "UF8",
            "UF9",
            "UF11",
//            "UF12",
//            "UF13"
        ),
        hgsTypes = EnumSet.of(HGSType.PARALLEL, HGSType.HOPSO),
        metrics = EnumSet.of(QualityIndicator.IGD, QualityIndicator.SPACING, QualityIndicator.HYPERVOLUME),
        runRange = 0..0
    )
        .apply {
//            saveAlgorithmPlots()
//            saveSummaryPlots()
            savePopulationPlots()
        }
}
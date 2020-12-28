package pl.edu.agh.kemo.algorithm

import org.moeaframework.core.NondominatedPopulation
import org.moeaframework.core.Population
import org.moeaframework.core.Problem
import org.moeaframework.core.Solution
import org.moeaframework.core.indicator.Hypervolume
import org.moeaframework.core.operator.real.PM
import org.moeaframework.core.variable.RealVariable
import pl.edu.agh.kemo.tools.BlurredProblem
import pl.edu.agh.kemo.tools.average
import pl.edu.agh.kemo.tools.countAlive
import pl.edu.agh.kemo.tools.loggerFor
import pl.edu.agh.kemo.tools.redundant
import pl.edu.agh.kemo.tools.variables

open class Node(
    problem: Problem,
    driverBuilder: DriverBuilder<*>,
    val level: Int,
    var population: Population,
    private val problemReferenceSet: NondominatedPopulation,
    private val parameters: HGSConfiguration
) {
    var alive: Boolean = true
    var ripe: Boolean = false

    val sprouts: List<Node> = mutableListOf()

    val driver: Driver<*>

    var center: List<RealVariable>? = null
        private set

    var delegates: List<Solution> = listOf()
        protected set

    var previousHypervolume: Double? = null
        private set

    var hypervolume: Double = 0.0
        private set

    open val finalizedPopulation: Population
        get() = population

    private val problem: BlurredProblem

    private var relativeHypervolume: Double? = null

    private val log = loggerFor<Node>()

    private var previousNumberOfEvaluations: Int = 0

    init {
        this.problem = BlurredProblem(problem, parameters.fitnessErrors[level])
        this.driver =
            driverBuilder.create(
                this.problem,
                population,
                mutationEta = parameters.mutationEtas[level],
                mutationRate = parameters.mutationRates[level],
                crossoverEta = parameters.crossoverEtas[level],
                crossoverRate = parameters.crossoverRates[level],
                mantissaBits = parameters.mantissaBits[level]
            )
        recalculateCenter()
    }

    open fun runMetaepoch(): Int {
        repeat(parameters.metaepochLength) {
            driver.step()
        }
        population = driver.getPopulation()
        delegates = driver.nominateDelegates()

        updateDominatedHypervolume(NondominatedPopulation(population))
        recalculateCenter()

        val epochCost = driver.numberOfEvaluations - previousNumberOfEvaluations
        previousNumberOfEvaluations = driver.numberOfEvaluations
        return epochCost
    }

    private fun updateDominatedHypervolume(nondominatedPopulation: NondominatedPopulation) {
        previousHypervolume = hypervolume
//        val minPoint = (0 until problem.numberOfObjectives).map { 0.0 }.toDoubleArray()
        val resultHypervolume = Hypervolume(problem.innerProblem, problemReferenceSet).run {
            evaluate(nondominatedPopulation)
        }
        relativeHypervolume?.let {
            hypervolume = resultHypervolume - it
            if (hypervolume > 0.0) {
//                Plot()
//                    .add("current pop", nondominatedPopulation)
//                    .add("reference set", referenceSet)
//                    .showDialog()
            }
        }
        if (relativeHypervolume == null) {
            relativeHypervolume = resultHypervolume
        }
    }

    fun recalculateCenter() {
        center = population.average()
    }

    fun releaseSprouts(hgs: HGS) {
        if (ripe) {
            sprouts.forEach { it.releaseSprouts(hgs) }

            if (level < parameters.maxLevel && sprouts.countAlive() < parameters.maxSproutsCount) {
                var releasedSprouts = 0

                for (delegate in delegates) { // TODO should delegates be shuffled before each sprouting?
                    if (releasedSprouts >= parameters.sproutiveness || sprouts.countAlive() >= parameters.maxSproutsCount) {
                        break
                    }

                    if (delegateNotRedundant(hgs, delegate)) {
                        val sproutPopulation = populationFromDelegate(
                            delegate = delegate,
                            size = parameters.subPopulationSizes[level + 1],
                            mutationEta = parameters.mutationEtas[level + 1],
                            mutationRate = parameters.mutationRates[level + 1]
                        )
                        val sprout = hgs.registerNode(sproutPopulation, level + 1)
                        (sprouts as MutableList).add(sprout)
                        releasedSprouts++
                    }
                }
            }
        }
    }

    private fun delegateNotRedundant(hgs: HGS, delegate: Solution): Boolean =
        hgs.levelNodes[level + 1]?.asSequence()
            ?.filter { !it.population.isEmpty }
            ?.map { it.center }
            ?.filterNotNull()
            ?.all { nodeCenter -> !delegate.variables().redundant(nodeCenter, hgs.minDistances[level + 1]) }
            ?: false

    private fun populationFromDelegate(
        delegate: Solution,
        size: Int,
        mutationEta: Double,
        mutationRate: Double
    ): Population {
        val mutation = PM(mutationRate, mutationEta)
        return (0 until size)
            .map { mutation.evolve(arrayOf(delegate))[0] }
            .let { Population(it) }
    }
}


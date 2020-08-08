package pl.edu.agh.kemo.algorithm

import org.moeaframework.core.Population
import org.moeaframework.core.Problem

class Node(
    private val problem: Problem,
    private val driver: Driver?,
    private val level: Int,
    private val population: Population,
    private val parameters: HGSConfiguration
) {
}
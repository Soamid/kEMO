package kemo.algorithm.progress

abstract class AbstractProgressIndicator(initalValue: Double) : ProgressIndicator {

    override var previousValue: Double? = null
        protected set

    override var currentValue: Double = initalValue
        protected set
}
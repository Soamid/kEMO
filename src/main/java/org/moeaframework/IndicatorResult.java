package org.moeaframework;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Min;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inner class for storing the results for a single performance indicator.
 */
public class IndicatorResult {

    /**
     * The name of the indicator.
     */
    private final String indicator;

    /**
     * The computed indicator values.
     */
    private final double[] values;

    /**
     * A list of algorithms whose performance with respect to this
     * indicator are statistically similar to the current algorithm.
     */
    private List<String> indifferentAlgorithms;

    /**
     * The indicator value of the aggregate Pareto set, or {@code null}
     * if the aggregate value was not computed.
     */
    private Double aggregateValue;

    /**
     * Constructs a new object for storing the results for a single
     * indicator.
     *
     * @param indicator the name of the indicator
     * @param values    the computed indicator values
     */
    public IndicatorResult(String indicator, double[] values) {
        super();
        this.indicator = indicator;
        this.values = values;

        indifferentAlgorithms = new ArrayList<String>();
    }

    /**
     * Returns the computed indicator values.
     *
     * @return the indicator values
     */
    public double[] getValues() {
        return values.clone();
    }

    /**
     * Returns the minimum indicator value.
     *
     * @return the minimum indicator value
     */
    public double getMin() {
        return getStatistic(new Min());
    }

    /**
     * Returns the median indicator value.
     *
     * @return the median indicator value
     */
    public double getMedian() {
        return getStatistic(new Median());
    }

    /**
     * Returns the maximum indicator value.
     *
     * @return the maximum indicator value
     */
    public double getMax() {
        return getStatistic(new Max());
    }

    public double getAverage() {
        return getStatistic(new Mean());
    }

    public double getStdev() {
        return getStatistic(new StandardDeviation());
    }

    /**
     * Computes and returns the value of the given univariate statistic.
     *
     * @param statistic the univariate statistic to compute
     * @return the computed value of the statistic
     */
    public double getStatistic(UnivariateStatistic statistic) {
        return statistic.evaluate(values);
    }

    /**
     * Returns the number of samples.
     *
     * @return the number of samples
     */
    public int getCount() {
        return values.length;
    }

    /**
     * Returns a list of algorithms whose performance with respect to this
     * indicator are statistically similar to the current algorithm.  This
     * list will only be populated if
     * {@link Analyzer#showStatisticalSignificance()} is invoked.
     *
     * @return a list of algorithms with statistically similar performance
     */
    public List<String> getIndifferentAlgorithms() {
        return new ArrayList<String>(indifferentAlgorithms);
    }

    /**
     * Adds an algorithm with statistically similar performance to the
     * current algorithm.
     *
     * @param algorithm the algorithm with statistically similar performance
     */
    public void addIndifferentAlgorithm(String algorithm) {
        indifferentAlgorithms.add(algorithm);
    }

    /**
     * Returns the indicator value of the aggregate Pareto set, or
     * {@code null} if the aggregate value was not computed.  This value
     * is only computed if {@link Analyzer#showAggregate()} is invoked.
     *
     * @return the aggregate indicator value; or {@code null} if not
     * computed
     */
    public Double getAggregateValue() {
        return aggregateValue;
    }

    /**
     * Sets the indicator value of the aggregate Pareto set.
     *
     * @param aggregateValue the aggregate indicator value
     */
    void setAggregateValue(Double aggregateValue) {
        this.aggregateValue = aggregateValue;
    }

    /**
     * Returns the indicator name.
     *
     * @return the indicator name
     */
    public String getIndicator() {
        return indicator;
    }

    void print(PrintStream ps) {
        print(ps, Collections.emptyList());
    }

    /**
     * Prints the results to the given stream.
     *
     * @param ps         the stream where the results are printed
     * @param statistics
     */
    void print(PrintStream ps, List<UnivariateStatistic> statistics) {
        double[] values = getValues();

        ps.print("    ");
        ps.print(getIndicator());
        ps.print(": ");

        if (values.length == 0) {
            ps.print("null");
        } else if (values.length == 1) {
            ps.print(values[0]);
        } else {
            ps.println();

            ps.print("        Aggregate: ");
            ps.println(getAggregateValue());

            if (statistics.isEmpty()) {
                ps.print("        Min: ");
                ps.println(getMin());
                ps.print("        Median: ");
                ps.println(getMedian());
                ps.print("        Max: ");
                ps.println(getMax());
                ps.print("        Average: ");
                ps.println(getAverage());
                ps.print("        Stdev: ");
                ps.println(getStdev());

            } else {
                for (UnivariateStatistic statistic : statistics) {
                    ps.print("        ");
                    ps.print(statistic.getClass().getSimpleName());
                    ps.print(": ");
                    ps.println(getStatistic(statistic));
                }
            }

            ps.print("        Count: ");
            ps.print(getCount());

            ps.println();
            ps.print("        Indifferent: ");
            ps.print(getIndifferentAlgorithms());

        }

        ps.println();
    }
}

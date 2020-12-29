package org.moeaframework;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class AlgorithmStats {

    /**
     * The name of the algorithm.
     */
    private final String algorithm;

    /**
     * The results for each indicator.
     */
    private final List<IndicatorResult> indicatorResults;

    /**
     * Constructs a new, empty object for storing the results of a single
     * algorithm.
     *
     * @param algorithm the algorithm name
     */
    public AlgorithmStats(String algorithm) {
        super();
        this.algorithm = algorithm;

        indicatorResults = new ArrayList<>();
    }

    /**
     * Returns the name of the algorithm.
     *
     * @return the algorithm name
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the names of the indicators contained within these results.
     *
     * @return a list of the indicator names
     */
    public List<String> getIndicators() {
        List<String> indicators = new ArrayList<String>();

        for (IndicatorResult result : indicatorResults) {
            indicators.add(result.getIndicator());
        }

        return indicators;
    }

    /**
     * Returns the results for the given indicator.
     *
     * @param indicator the indicator name
     * @return the results for the given indicator
     */
    public IndicatorResult get(String indicator) {
        for (IndicatorResult result : indicatorResults) {
            if (result.getIndicator().equals(indicator)) {
                return result;
            }
        }

        return null;
    }

    /**
     * Adds the results for a single indicator.  The name of the indicator
     * should be unique.
     *
     * @param result the indicator result
     */
    public void add(IndicatorResult result) {
        indicatorResults.add(result);
    }

    /**
     * Prints the results to the given stream.
     *
     * @param ps the stream where the results are printed
     */
    public void print(PrintStream ps) {
        ps.print(getAlgorithm());
        ps.println(':');

        for (IndicatorResult indicatorResult : indicatorResults) {
            indicatorResult.print(ps);
        }
    }

}

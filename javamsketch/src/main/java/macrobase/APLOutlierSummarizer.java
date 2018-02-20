package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanation;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.GlobalRatioMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.SupportMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.AttributeEncoder;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Summarizer that works over both cube and row-based labeled ratio-based
 * outlier summarization.
 */
public class APLOutlierSummarizer extends APLSummarizer {
    private Logger log = LoggerFactory.getLogger("APLOutlierSummarizer");
    private String countColumn = null;
    private boolean onlyUseSupport = false;
    private macrobase.APrioriLinear aplKernel;

    public long aplTime = 0;
    public long mergeTime = 0;
    public long queryTime = 0;

    public List<Integer> o1results;
    public List<int[]> encoded;
    public AttributeEncoder encoder;

    @Override
    public List<String> getAggregateNames() {
        return Arrays.asList("Outliers", "Count");
    }

    @Override
    public double[][] getAggregateColumns(DataFrame input) {
        double[] outlierCol = input.getDoubleColumnByName(outlierColumn);
        double[] countCol = processCountCol(input, countColumn,  outlierCol.length);

        double[][] aggregateColumns = new double[2][];
        aggregateColumns[0] = outlierCol;
        aggregateColumns[1] = countCol;

        return aggregateColumns;
    }

    @Override
    public List<QualityMetric> getQualityMetricList() {
        List<QualityMetric> qualityMetricList = new ArrayList<>();
        qualityMetricList.add(
                new SupportMetric(0)
        );
        if (!onlyUseSupport) {
            qualityMetricList.add(
                    new GlobalRatioMetric(0, 1)
            );
        }
        return qualityMetricList;
    }

    @Override
    public List<Double> getThresholds() {
        if (onlyUseSupport) {
            return Collections.singletonList(minOutlierSupport);
        } else {
            return Arrays.asList(minOutlierSupport, minRatioMetric);
        }
    }

    @Override
    public double getNumberOutliers(double[][] aggregates) {
        double count = 0.0;
        double[] outlierCount = aggregates[0];
        for (int i = 0; i < outlierCount.length; i++) {
            count += outlierCount[i];
        }
        return count;
    }

    public void process(DataFrame input) throws Exception {
        encoder = new AttributeEncoder();
        encoder.setColumnNames(attributes);
        long startTime = System.currentTimeMillis();
        encoded = encoder.encodeAttributes(
                input.getStringColsByName(attributes)
        );
        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("Encoded in: {}", elapsed);
        log.debug("Encoded Categories: {}", encoder.getNextKey());

        thresholds = getThresholds();
        qualityMetricList = getQualityMetricList();
        aplKernel = new APrioriLinear(
                qualityMetricList,
                thresholds
        );
        aplKernel.setDoContainment(doContainment);

        double[][] aggregateColumns = getAggregateColumns(input);
        List<String> aggregateNames = getAggregateNames();
        Map<String, int[]> aggregationOps = getAggregationOps();
        long start = System.nanoTime();
        List<APLExplanationResult> aplResults = aplKernel.explain(encoded, aggregateColumns, aggregationOps);
        aplTime += System.nanoTime() - start;
        mergeTime += aplKernel.mergeTime;
        queryTime += aplKernel.queryTime;
        o1results = aplKernel.o1results;
        numOutliers = (long)getNumberOutliers(aggregateColumns);

        explanation = new APLExplanation(
                encoder,
                numEvents,
                numOutliers,
                aggregateNames,
                qualityMetricList,
                thresholds,
                aplResults
        );
    }

    public String getCountColumn() {
        return countColumn;
    }
    public void setCountColumn(String countColumn) {
        this.countColumn = countColumn;
    }
    public double getMinRatioMetric() {
        return minRatioMetric;
    }
    public void onlyUseSupport(boolean onlyUseSupport) { this.onlyUseSupport = onlyUseSupport; }

    public void resetTime() {
        this.aplTime = 0;
        this.mergeTime = 0;
        this.queryTime = 0;
    }
}

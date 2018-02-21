package macrobase;

import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanation;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.AttributeEncoder;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Summarizer that works over both cube and row-based labeled ratio-based
 * outlier summarization.
 */
public class APLMomentSummarizer extends APLSummarizer {
    private Logger log = LoggerFactory.getLogger("APLMomentSummarizer");
    private String minColumn = null;
    private String maxColumn = null;
    private List<String> momentColumns;
    private List<String> logMomentColumns;
    private double percentile;
    private boolean[] useStages;
    private boolean verbose;
    private APrioriLinear aplKernel;
    private boolean useSupport;
    private boolean useGlobalRatio;

    public AttributeEncoder encoder;
    public List<Integer> o1results;
    public List<int[]> encoded;

    public long aplTime = 0;
    public long mergeTime = 0;
    public long queryTime = 0;

    @Override
    public List<String> getAggregateNames() {
        ArrayList<String> aggregateNames = new ArrayList<>();
        aggregateNames.add("Minimum");
        aggregateNames.add("Maximum");
        aggregateNames.addAll(momentColumns);
        aggregateNames.addAll(logMomentColumns);
        return aggregateNames;
    }

    @Override
    public double[][] getAggregateColumns(DataFrame input) {
        double[][] aggregateColumns = new double[2+momentColumns.size()+logMomentColumns.size()][];
        int curCol = 0;
        aggregateColumns[curCol++] = input.getDoubleColumnByName(minColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(maxColumn);
        for (int i = 0; i < momentColumns.size(); i++) {
            aggregateColumns[curCol++] = input.getDoubleColumnByName(momentColumns.get(i));
        }
        for (int i = 0; i < logMomentColumns.size(); i++) {
            aggregateColumns[curCol++] = input.getDoubleColumnByName(logMomentColumns.get(i));
        }

        processCountCol(input, momentColumns.get(0), aggregateColumns[0].length);
        return aggregateColumns;
    }

    @Override
    public Map<String, int[]> getAggregationOps() {
        Map<String, int[]> aggregationOps = new HashMap<>();
        aggregationOps.put("add", IntStream.range(2, 2+momentColumns.size()+logMomentColumns.size()).toArray());
        aggregationOps.put("min", new int[]{0});
        aggregationOps.put("max", new int[]{1});
        return aggregationOps;
    }

    @Override
    public List<QualityMetric> getQualityMetricList() {
        List<QualityMetric> qualityMetricList = new ArrayList<>();
        if (useSupport) {
            EstimatedSupportMetric metric = new EstimatedSupportMetric(0, 1, 2, 2+momentColumns.size(),
                    (100.0 - percentile) / 100.0, 1e-10, true);
            metric.setCascadeStages(useStages);
            metric.setVerbose(verbose);
            qualityMetricList.add(metric);
        }
        if (useGlobalRatio) {
            EstimatedGlobalRatioMetric metric = new EstimatedGlobalRatioMetric(0, 1, 2, 2+momentColumns.size(),
                    (100.0 - percentile) / 100.0, 1e-10, true);
            metric.setCascadeStages(useStages);
            metric.setVerbose(verbose);
            qualityMetricList.add(metric);
        }
        return qualityMetricList;
    }

    @Override
    public List<Double> getThresholds() {
        List<Double> thresholds = new ArrayList<>();
        if (useSupport) {
            thresholds.add(minOutlierSupport);
        } else {
            thresholds.add(minRatioMetric);
        }
        return thresholds;
    }

    @Override
    public double getNumberOutliers(double[][] aggregates) {
        double count = 0.0;
        double[] counts = aggregates[2];
        for (int i = 0; i < counts.length; i++) {
            count += counts[i];
        }
        return count * percentile / 100.0;
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

    public String getMinColumn() {
        return minColumn;
    }
    public void setMinColumn(String minColumn) {
        this.minColumn = minColumn;
    }
    public String getMaxColumn() {
        return maxColumn;
    }
    public void setMaxColumn(String maxColumn) {
        this.maxColumn = maxColumn;
    }
    public List<String> getMomentColumns() {
        return momentColumns;
    }
    public void setMomentColumns(List<String> momentColumns) {
        this.momentColumns = momentColumns;
    }
    public void setLogMomentColumns(List<String> logMomentColumns) {
        this.logMomentColumns = logMomentColumns;
    }
    public void setPercentile(double percentile) {
        this.percentile = percentile;
    }
    public double getMinRatioMetric() {
        return minRatioMetric;
    }
    public void setUseStages(boolean[] useStages) { this.useStages = useStages; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
    public void setUseSupport(boolean useSupport) { this.useSupport = useSupport; }
    public void setUseGlobalRatio(boolean useGlobalRatio) { this.useGlobalRatio = useGlobalRatio; }

    public void resetTime() {
        this.aplTime = 0;
        this.mergeTime = 0;
        this.queryTime = 0;
    }
}

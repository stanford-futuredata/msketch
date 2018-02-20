package macrobase;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLExplanationResult;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.APLSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.aplinear.metrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.AttributeEncoder;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sketches.YahooSketch;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Summarizer that works over both cube and row-based labeled ratio-based
 * outlier summarization.
 */
public class APLYahooSummarizer {
    private Logger log = LoggerFactory.getLogger("APLYahooSummarizer");
    private double percentile;
    public List<SketchQualityMetric> qualityMetricList;
    boolean doContainment = true;
    protected double minOutlierSupport = 0.1;
    protected double minRatioMetric = 10.0;
    AttributeEncoder encoder;
    protected List<String> attributes = new ArrayList<>();
    public List<APLSketchExplanationResult> aplResults;
    private boolean useSupport;
    private boolean useGlobalRatio;

    public long aplTime = 0;
    public long mergeTime = 0;
    public long queryTime = 0;

    public List<String> getAggregateNames() {
        ArrayList<String> aggregateNames = new ArrayList<>();
        aggregateNames.add("Yahoo");
        return aggregateNames;
    }

    public static YahooSketch[][] getAggregateColumns(String inputFile) throws Exception {
        FileInputStream fis = new FileInputStream(inputFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        List<byte[]> byteArrays = (List<byte[]>) ois.readObject();
        ois.close();

        YahooSketch[] sketches = new YahooSketch[byteArrays.size()];
        for (int i = 0; i < byteArrays.size(); i++) {
            byte[] bytes = byteArrays.get(i);
            UpdateDoublesSketch s = (UpdateDoublesSketch)UpdateDoublesSketch.wrap(Memory.wrap(bytes));
            sketches[i] = new YahooSketch(s);
        }

        YahooSketch[][] aggregateColumns = new YahooSketch[1][];
        aggregateColumns[0] = sketches;

//        processCountCol(input, momentColumns.get(0), aggregateColumns[0].length);
        return aggregateColumns;
    }

    public List<SketchQualityMetric> getQualityMetricList() {
        List<SketchQualityMetric> qualityMetricList = new ArrayList<>();

        if (useSupport) {
            SketchSupportMetric metric = new SketchSupportMetric((100.0 - percentile) / 100.0, 1e-4, true);
            qualityMetricList.add(metric);
        }
        if (useGlobalRatio) {
            SketchGlobalRatioMetric metric = new SketchGlobalRatioMetric((100.0 - percentile) / 100.0, 1e-4, true);
            qualityMetricList.add(metric);
        }

        return qualityMetricList;
    }

    public List<Double> getThresholds() {
        List<Double> thresholds = new ArrayList<>();
        if (useSupport) {
            thresholds.add(minOutlierSupport);
        } else {
            thresholds.add(minRatioMetric);
        }
        return thresholds;
    }

//    public double getNumberOutliers(double[][] aggregates) {
//        double count = 0.0;
//        double[] counts = aggregates[4];
//        for (int i = 0; i < counts.length; i++) {
//            count += counts[i];
//        }
//        return count * percentile / 100.0;
//    }

    public void process(DataFrame input, YahooSketch[][] aggregateColumns) throws Exception {
        encoder = new AttributeEncoder();
        encoder.setColumnNames(attributes);
        long startTime = System.currentTimeMillis();
        List<int[]> encoded = encoder.encodeAttributes(
                input.getStringColsByName(attributes)
        );
        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("Encoded in: {}", elapsed);
        log.debug("Encoded Categories: {}", encoder.getNextKey());

        List<Double> thresholds = getThresholds();
        qualityMetricList = getQualityMetricList();
        SketchAPrioriLinearSimple aplKernel = new SketchAPrioriLinearSimple(
                qualityMetricList,
                thresholds
        );
        aplKernel.setDoContainment(doContainment);

        List<String> aggregateNames = getAggregateNames();
        long start = System.nanoTime();
        aplResults = aplKernel.explain(encoded, aggregateColumns);
        aplTime += System.nanoTime() - start;
        mergeTime += aplKernel.mergeTime;
        queryTime += aplKernel.queryTime;
//        long numOutliers = (long)getNumberOutliers(aggregateColumns);

//        explanation = new APLExplanation(
//                encoder,
//                numEvents,
//                numOutliers,
//                aggregateNames,
//                qualityMetricList,
//                thresholds,
//                aplResults
//        );
    }

    public void setPercentile(double percentile) {
        this.percentile = percentile;
    }
    public void setMinSupport(double minSupport) { this.minOutlierSupport = minSupport; }
    public void setMinRatioMetric(double minRatio) { this.minRatioMetric = minRatio; }
    public void setAttributes(List<String> attributes) { this.attributes = attributes; }
    public void setDoContainment(boolean doContainment) { this.doContainment = doContainment; }
    public void setUseSupport(boolean useSupport) { this.useSupport = useSupport; }
    public void setUseGlobalRatio(boolean useGlobalRatio) { this.useGlobalRatio = useGlobalRatio; }

    public void resetTime() {
        this.aplTime = 0;
        this.mergeTime = 0;
        this.queryTime = 0;
    }
}

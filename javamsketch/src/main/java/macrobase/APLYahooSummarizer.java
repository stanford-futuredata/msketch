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
    public List<SketchSupportMetric> supportMetricList;
    boolean doContainment = true;
    protected double minOutlierSupport = 0.1;
    AttributeEncoder encoder;
    protected List<String> attributes = new ArrayList<>();
    List<APLSketchExplanationResult> aplResults;

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

    public List<SketchSupportMetric> getSupportMetricList() {
        List<SketchSupportMetric> qualityMetricList = new ArrayList<>();

        SketchSupportMetric metric = new SketchSupportMetric((100.0 - percentile) / 100.0, 1e-4, true);
        qualityMetricList.add(metric);

        return qualityMetricList;
    }

    public List<Double> getThresholds() {
        return Arrays.asList(minOutlierSupport);
    }

//    public double getNumberOutliers(double[][] aggregates) {
//        double count = 0.0;
//        double[] counts = aggregates[4];
//        for (int i = 0; i < counts.length; i++) {
//            count += counts[i];
//        }
//        return count * percentile / 100.0;
//    }

    public void process(DataFrame input, String fileName) throws Exception {
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
        supportMetricList = getSupportMetricList();
        SketchAPrioriLinear aplKernel = new SketchAPrioriLinear(
                supportMetricList,
                thresholds
        );
        aplKernel.setDoContainment(doContainment);

        YahooSketch[][] aggregateColumns = getAggregateColumns(fileName);
        List<String> aggregateNames = getAggregateNames();
        aplResults = aplKernel.explain(encoded, aggregateColumns);
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
    public void setAttributes(List<String> attributes) { this.attributes = attributes; }
    public void setDoContainment(boolean doContainment) { this.doContainment = doContainment; }
}

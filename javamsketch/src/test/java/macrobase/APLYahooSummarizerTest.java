package macrobase;

import data.TestDataSource;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import edu.stanford.futuredata.macrobase.datamodel.Schema;
import edu.stanford.futuredata.macrobase.ingest.CSVDataFrameParser;
import io.DataGrouper;
import io.SeqDataGrouper;
import org.junit.Test;
import sketches.QuantileSketch;
import sketches.QuantileUtil;
import sketches.YahooSketch;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class APLYahooSummarizerTest {
    @Test
    public void testSample() throws Exception {
        List<String> requiredColumns = Arrays.asList("location", "version");
        Map<String, Schema.ColType> colTypes = new HashMap<>();
        CSVDataFrameParser loader = new CSVDataFrameParser("src/test/resources/sample_grouped.csv", requiredColumns);
        loader.setColumnTypes(colTypes);
        DataFrame df = loader.load();

        APLYahooSummarizer summ = new APLYahooSummarizer();
        summ.setMinSupport(0.2);
        summ.setAttributes(requiredColumns);
        summ.setPercentile(1.0);
        summ.setDoContainment(true);

        summ.process(df, "src/test/resources/sample_yahoo");
        List<APLSketchExplanationResult> aplResults = summ.aplResults;
        SketchSupportMetric metric = summ.supportMetricList.get(0);

//        System.out.println(aplResults.size());
//        System.out.println(metric.cutoff);
//        for (APLSketchExplanationResult r : aplResults) {
//            System.out.println(r.prettyPrint(summ.encoder, requiredColumns));
//        }

        assertEquals(4, aplResults.size());
        assertEquals(46.5, metric.cutoff, 0.5);
    }
}
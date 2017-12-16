import io.CSVOutput;
import io.DataSource;
import io.SimpleCSVDataSource;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccuracyBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private Map<String, List<Double>> methods;
    private List<Double> quantiles;

    public AccuracyBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        methods = conf.get("methods");
        quantiles = conf.get("quantiles");
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        AccuracyBench bench = new AccuracyBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.writeAllResults(results, bench.testName);
    }

    public List<Map<String, String>> run() throws Exception {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        double[] data = source.get();
        List<Map<String, String>> results = new ArrayList<>();

        int m = quantiles.size();
        double[] trueQuantiles = new double[m];
        Percentile truePercentileCalc = new Percentile()
                .withEstimationType(Percentile.EstimationType.R_1);
        truePercentileCalc.setData(data);
        for (int i = 0; i < m; i++) {
            trueQuantiles[i] = truePercentileCalc.evaluate(quantiles.get(i)*100);
        }

        for (String sketchName : methods.keySet()) {
            System.out.println("Benchmarking: "+sketchName);
            List<Double> sizeParams = methods.get(sketchName);
            for (double sParam : sizeParams) {
                QuantileSketch curSketch = SketchLoader.load(sketchName);
                curSketch.setCalcError(true);
                curSketch.setSizeParam(sParam);
                curSketch.initialize();

                long startTime = System.nanoTime();
                curSketch.add(data);
                long endTime = System.nanoTime();
                long trainTime = endTime - startTime;

                startTime = System.nanoTime();
                double[] qs = curSketch.getQuantiles(quantiles);
                endTime = System.nanoTime();
                long queryTime = endTime - startTime;
                double[] errors = curSketch.getErrors();

                for (int i = 0 ; i < qs.length; i++) {
                    double curP = quantiles.get(i);
                    double curQ = qs[i];
                    double curError = errors[i];

                    Map<String, String> curResults = new HashMap<>();
                    curResults.put("dataset", fileName);
                    curResults.put("q", String.format("%f", curP));
                    curResults.put("quantile_true", String.format("%f", trueQuantiles[i]));
                    curResults.put("quantile_estimate", String.format("%f", curQ));
                    curResults.put("bound_size", String.format("%f", curError));
                    curResults.put("sketch", curSketch.getName());
                    curResults.put("space", String.format("%d",curSketch.getSize()));
                    curResults.put("size_param", String.format("%.2f",sParam));
                    curResults.put("train_time", String.format("%d", trainTime));
                    curResults.put("query_time", String.format("%d", queryTime));
                    curResults.put("n", String.format("%d", data.length));
                    results.add(curResults);
                }
            }
        }

        return results;
    }
}

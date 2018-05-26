import io.CSVOutput;
import io.DataSource;
import io.SimpleCSVDataSource;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscreteBench {
    private String testName;
    private boolean verbose;

    private int numSolveTrials;
    private int totalSize;
    private List<Integer> cardinalities;
    private Map<String, List<Double>> methods;
    private List<Double> quantiles;

    public DiscreteBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        verbose = conf.get("verbose", false);

        numSolveTrials = conf.get("numSolveTrials", 10);
        cardinalities = conf.get("cardinalities");
        totalSize = conf.get("totalSize");
        methods = conf.get("methods");
        quantiles = conf.get("quantiles");
    }

    public List<Map<String, String>> run() throws Exception {
        List<Map<String, String>> results = new ArrayList<>();
        int numTests = cardinalities.size();
        for (int di = 0; di < numTests; di++) {
            int curN = cardinalities.get(di);
            double[] data = new double[totalSize];
            for (int i = 0; i < totalSize; i++) {
                data[i] = (double) (i % curN) / (curN - 1);
            }

            for (String sketchName : methods.keySet()) {
                List<Double> sizeParams = methods.get(sketchName);
                for (double sParam : sizeParams) {
                    System.gc();
                    System.out.println(sketchName + "@" + (int) sParam);
                    QuantileSketch curSketch = SketchLoader.load(sketchName);
                    curSketch.setVerbose(verbose);
                    curSketch.setSizeParam(sParam);
                    curSketch.initialize();

                    long startTime = System.nanoTime();
                    curSketch.add(data);
                    long endTime = System.nanoTime();
                    long trainTime = endTime - startTime;
                    System.out.println("Trained Sketch");

                    startTime = System.nanoTime();
                    double[] qs = new double[0];
                    for (int curSolveTrial = 0; curSolveTrial < numSolveTrials; curSolveTrial++) {
                        qs = curSketch.getQuantiles(quantiles);
                    }
                    endTime = System.nanoTime();
                    long queryTime = (endTime - startTime) / numSolveTrials;

                    for (int i = 0; i < qs.length; i++) {
                        double curP = quantiles.get(i);
                        double curQ = qs[i];

                        Map<String, String> curResults = new HashMap<>();
                        curResults.put("sketch", curSketch.getName());
                        curResults.put("cardinality", String.format("%d", curN));
                        curResults.put("q", String.format("%f", curP));
                        curResults.put("quantile_estimate", Double.toString(curQ));
                        curResults.put("space", String.format("%d", curSketch.getSize()));
                        curResults.put("size_param", String.format("%.2f", sParam));
                        curResults.put("train_time", String.format("%d", trainTime));
                        curResults.put("query_time", String.format("%d", queryTime));
                        curResults.put("n", String.format("%d", data.length));
                        results.add(curResults);
                    }
                }
            }

        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        DiscreteBench bench = new DiscreteBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(false);
        output.writeAllResults(results, bench.testName);
    }
}

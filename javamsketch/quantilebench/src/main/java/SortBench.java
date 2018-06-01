import io.CSVOutput;
import io.DataSource;
import io.SimpleCSVDataSource;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.*;

public class SortBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private List<String> methods;
    private List<Double> sizeParams;
    private int numTrials;

    private boolean verbose = false;

    public SortBench(String confFile) throws IOException{
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        numTrials = conf.get("numTrials");
        methods = conf.get("methods");
        sizeParams = conf.get("sizeParams");

        verbose = conf.get("verbose", false);
    }

    public static void main(String[] args) throws Exception {
        String confFile = args[0];
        SortBench bench = new SortBench(confFile);
//        System.in.read();

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(false);
        output.writeAllResults(results, bench.testName);
    }

    public List<Map<String, String>> run() throws Exception {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        long startTime = System.currentTimeMillis();
        double[] data = source.get();
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;
        System.out.println("Loaded Data in: "+loadTime);
        List<Map<String, String>> results = new ArrayList<>();

        for (int methodIdx = 0 ; methodIdx < methods.size(); methodIdx++) {
            String sketchName = methods.get(methodIdx);
            double sizeParam = sizeParams.get(methodIdx);
            for (int curTrial = 0; curTrial < numTrials; curTrial++) {
                System.gc();
                System.out.println(sketchName + ":" + curTrial);
                startTime = System.nanoTime();
                if (sketchName.equals("sort")) {
                    double[] sortedData = data.clone();
                    Arrays.sort(sortedData);
                }
                else if (sketchName.equals("select")) {
                    Percentile p = new Percentile();
                    p.setData(data);
                    p.evaluate(50.0);
                } else {
                    QuantileSketch s = SketchLoader.load(sketchName);
                    s.setSizeParam(sizeParam);
                    s.initialize();
                    s.add(data);
                    double[] ps = s.getQuantiles(Arrays.asList(.5));
                }
                endTime = System.nanoTime();
                long trainTime = endTime - startTime;
                if (verbose) {
                    System.out.println("Trained Sketch");
                }
                Map<String, String> curResults = new HashMap<>();
                curResults.put("dataset", fileName);
                curResults.put("sketch", sketchName);
                curResults.put("trial", String.format("%d", curTrial));
                curResults.put("train_time", String.format("%d", trainTime));
                curResults.put("n", String.format("%d", data.length));
                results.add(curResults);
            }
        }

        return results;
    }
}

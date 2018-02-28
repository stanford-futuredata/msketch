import io.CSVOutput;
import io.DataSource;
import io.SimpleCSVDataSource;
import msketch.ChebyshevMomentSolver2;
import msketch.MathUtil;
import msketch.data.*;
import org.apache.commons.math3.distribution.NormalDistribution;
import sketches.QuantileSketch;
import sketches.SketchLoader;

import java.io.IOException;
import java.util.*;

public class OutlierBench {
    private String testName;
    private String fileName;
    private int columnIdx;
    private boolean verbose;

    private Map<String, List<Double>> methods;
    private int numSolveTrials;
    private List<Double> distances;
    private List<Double> fractions;
    private double scaleFactor;
    private List<Double> quantiles;

    public OutlierBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        fileName = conf.get("fileName");
        columnIdx = conf.get("columnIdx");
        verbose = conf.get("verbose", false);

        methods = conf.get("methods");
        numSolveTrials = conf.get("numSolveTrials");
        distances = conf.get("distances");
        fractions = conf.get("fractions");
        scaleFactor = conf.get("scaleFactor");
        quantiles = conf.get("quantiles");
    }

    public List<Map<String, String>> run() throws Exception {
        DataSource source = new SimpleCSVDataSource(fileName, columnIdx);
        long startTime = System.currentTimeMillis();
        double[] data = source.get();
        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;
        System.out.println("Loaded Data in: "+loadTime);
        List<Map<String, String>> results = new ArrayList<>();

        int numTests = distances.size();
        for (int di = 0; di < numTests; di++) {
            double curDistance = distances.get(di);
            double curFraction = fractions.get(di);

            int numOutliers = (int)(curFraction * data.length);
//            double[] outliers = new double[numOutliers];
//            for (int i = 0; i < numOutliers; i++) {
//                outliers[i] = data[i]*scaleFactor + curDistance;
//            }
            double[] combinedData = new double[numOutliers + data.length];
            System.arraycopy(data, 0, combinedData, 0, data.length);
            for (int i = 0; i < numOutliers; i++) {
                combinedData[data.length+i] = data[i]*scaleFactor + curDistance;
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

                    startTime = System.nanoTime();
                    curSketch.add(combinedData);
                    endTime = System.nanoTime();
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
                        curResults.put("dataset", fileName);
                        curResults.put("sketch", curSketch.getName());
                        curResults.put("distance", String.format("%f",curDistance));
                        curResults.put("fraction", String.format("%f",curFraction));
                        curResults.put("scaleFactor", String.format("%f",scaleFactor));
                        curResults.put("q", String.format("%f", curP));
                        curResults.put("quantile_estimate", String.format("%f", curQ));
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
        OutlierBench bench = new OutlierBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(false);
        output.writeAllResults(results, bench.testName);
    }
}

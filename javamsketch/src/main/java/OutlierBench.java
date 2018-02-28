import io.CSVOutput;
import msketch.ChebyshevMomentSolver2;
import msketch.MathUtil;
import msketch.data.*;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.IOException;
import java.util.*;

public class OutlierBench {
    private String testName;
    private boolean verbose;
    private int numSolveTrials;
    private List<Double> distances;
    private List<Double> fractions;
    private List<Double> quantiles;

    public OutlierBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        verbose = conf.get("verbose", false);
        numSolveTrials = conf.get("numSolveTrials");
        distances = conf.get("distances");
        fractions = conf.get("fractions");
        quantiles = conf.get("quantiles");
    }

    public List<Map<String, String>> run() {
        ArrayList<Map<String, String>> results = new ArrayList<>();

        MomentData m = new GaussianData();
        int k = 11;

        int numQuantiles = quantiles.size();
        double[] psArray = new double[numQuantiles];
        for (int i = 0; i < numQuantiles; i++) {
            psArray[i] = quantiles.get(i);
        }
        double r = 10.0;

        int numTests = distances.size();
        for (int di = 0; di < numTests; di++) {
            double curDistance = distances.get(di);
            double curFraction = fractions.get(di);
            double aMin = m.getMin();
            double aMax = Math.max(m.getMax(), (m.getMax())/r+curDistance);
            double[] powerSums = m.getPowerSums(k);
            double numPoints = powerSums[0];
            double[] logSums = m.getLogSums(1);

            double[] outlierPowerSums = MathUtil.shiftPowerSum(powerSums, r, -curDistance*r);
            for (int i = 0; i < powerSums.length; i++) {
                powerSums[i] += outlierPowerSums[i] * curFraction;
            }

            double[] qs = new double[1];
            long startTime = 0, endTime = 0, timePerTrial = 0;
            for (int warmUp = 0; warmUp < 2; warmUp++) {
                startTime = System.nanoTime();
                for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                    ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                            aMin, aMax, powerSums,
                            0, 1, logSums
                    );
                    solver.setHessianType(0);
                    solver.setVerbose(verbose);
                    solver.solve(1e-9);
                    qs = solver.estimateQuantiles(psArray);
                }
                endTime = System.nanoTime();
                timePerTrial = (endTime - startTime) / numSolveTrials;
            }
            results.addAll(genResultMaps(
                    k,
                    curDistance,
                    curFraction,
                    psArray,
                    qs,
                    timePerTrial,
                    (long) powerSums[0]
            ));
        }
        return results;
    }

    private List<Map<String, String>> genResultMaps(
            int k,
            double distance,
            double fraction,
            double[] ps,
            double[] qEstimates,
            long queryTime,
            long n
    ) {
        List<Map<String,String>> results = new ArrayList<>();
        for (int i = 0; i < ps.length; i++) {
            Map<String, String> curResults = new HashMap<>();
            curResults.put("dataset", "gaussian");
            curResults.put("distance", String.format("%f",distance));
            curResults.put("fraction", String.format("%f",fraction));
            curResults.put("size_param", String.format("%d", k));
            curResults.put("q", String.format("%f", ps[i]));
            curResults.put("quantile_estimate", String.format("%f", qEstimates[i]));
            curResults.put("query_time", String.format("%d", queryTime));
            curResults.put("n", String.format("%d", n));
            results.add(curResults);
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

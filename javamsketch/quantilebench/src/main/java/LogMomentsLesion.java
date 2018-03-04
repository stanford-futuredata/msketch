import io.CSVOutput;
import msolver.ChebyshevMomentSolver2;
import msolver.data.*;

import java.io.IOException;
import java.util.*;

public class LogMomentsLesion {
    private String testName;
    private String fileName;
    private String datasetName;
    private int numSolveTrials;
    private boolean verbose;
    private List<Integer> ks;

    public LogMomentsLesion(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        verbose = conf.get("verbose", false);
        numSolveTrials = conf.get("numSolveTrials");

        ks = conf.get("ks");
    }

    public List<Map<String, String>> run() {
        ArrayList<Map<String, String>> results = new ArrayList<>();

        List<String> datasetNames = Arrays.asList("milan", "exponential", "occupancy", "retail");
        List<MomentData> datasets = Arrays.asList(
                new MilanData(),
                new ExponentialData(),
                new OccupancyData(),
                new RetailQuantityData()
        );
        List<Double> ps = new ArrayList<>();
        int numQuantiles = 21;
        for (int i = 0; i < numQuantiles; i++) {
            double curValue = (double)i/(numQuantiles-1);
            ps.add(curValue);
        }
        ps.set(0, 0.01);
        ps.set(20, 0.99);
        double[] psArray = new double[numQuantiles];
        for (int i = 0; i < numQuantiles; i++) {
            psArray[i] = ps.get(i);
        }

        for (int di = 0; di < datasetNames.size(); di++) {
            String dname = datasetNames.get(di);
            MomentData mData = datasets.get(di);
            for (int ki = 0; ki < ks.size(); ki++) {
                int k = ks.get(ki);
                System.out.println(dname+":"+k);
                int k2 = (k+1)/2;
                double[] qs = new double[1];

                long startTime = 0, endTime = 0, timePerTrial = 0;

                for (int warmUp = 0; warmUp < 2; warmUp++) {
                    startTime = System.nanoTime();
                    for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                                mData.getMin(), mData.getMax(), mData.getPowerSums(k2),
                                mData.getLogMin(), mData.getLogMax(), mData.getLogSums(k2)
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
                        dname,
                        "cmoments_l",
                        k,
                        psArray,
                        qs,
                        timePerTrial,
                        (long) mData.getPowerSums(1)[0]
                ));

                for (int warmUp = 0; warmUp < 2; warmUp++) {
                    startTime = System.nanoTime();
                    for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                        ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                                mData.getMin(), mData.getMax(), mData.getPowerSums(k),
                                mData.getLogMin(), mData.getLogMax(), mData.getLogSums(1)
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
                        dname,
                        "cmoments_nl",
                        k,
                        psArray,
                        qs,
                        timePerTrial,
                        (long) mData.getPowerSums(1)[0]
                ));

            }
        }
        return results;
    }

    private List<Map<String, String>> genResultMaps(
            String dataset,
            String sketch,
            int k,
            double[] ps,
            double[] qEstimates,
            long queryTime,
            long n
    ) {
        List<Map<String,String>> results = new ArrayList<>();
        for (int i = 0; i < ps.length; i++) {
            Map<String, String> curResults = new HashMap<>();
            curResults.put("dataset", dataset);
            curResults.put("sketch", sketch);
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
        LogMomentsLesion bench = new LogMomentsLesion(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(false);
        output.writeAllResults(results, bench.testName);
    }
}

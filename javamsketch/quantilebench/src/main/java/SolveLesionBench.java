import io.CSVOutput;
import msolver.ChebyshevMomentSolver2;
import msolver.MnatSolver;
import msolver.data.ExponentialData;
import msolver.data.HepData;
import msolver.data.MilanData;
import msolver.data.MomentData;

import java.io.IOException;
import java.util.*;

public class SolveLesionBench {
    private String testName;
    private boolean verbose;
    private int k;

    public SolveLesionBench(String confFile) throws IOException {
        RunConfig conf = RunConfig.fromJsonFile(confFile);
        testName = conf.get("testName");
        verbose = conf.get("verbose", false);
        k = conf.get("k");
    }

    public List<Map<String, String>> run() {
        ArrayList<Map<String, String>> results = new ArrayList<>();

        List<String> datasetNames = Arrays.asList("milan", "exponential", "hepmass");
        List<MomentData> datasets = Arrays.asList(
                new MilanData(),
                new ExponentialData(),
                new HepData()
        );
        List<Boolean> useStandardBasis = Arrays.asList(false, true, true);

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

        int numSolveTrials = 1;
        for (int di = 0; di < datasetNames.size(); di++) {
            String dname = datasetNames.get(di);
            System.out.println(dname);
            MomentData mData = datasets.get(di);
            int ka = k;
            int kb = k;
            if (useStandardBasis.get(di)) {
                kb = 1;
            } else {
                ka = 1;
            }
            double[] qs = new double[1];

            long startTime=0, endTime=0, timePerTrial=0;

            System.out.println("clenshaw");
            numSolveTrials = 1000;
//            numSolveTrials = 1;
            for (int warmUp = 0; warmUp < 2; warmUp++) {
                startTime = System.nanoTime();
                for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                    ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                            mData.getMin(), mData.getMax(), mData.getPowerSums(ka),
                            mData.getLogMin(), mData.getLogMax(), mData.getLogSums(kb)
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
                    "clenshaw",
                    psArray,
                    qs,
                    timePerTrial,
                    (long)mData.getPowerSums(1)[0]
            ));

            System.out.println("newton");
            numSolveTrials = 100;
//            numSolveTrials = 1;
            for (int warmUp = 0; warmUp < 2; warmUp++) {
                startTime = System.nanoTime();
                for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                    ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                            mData.getMin(), mData.getMax(), mData.getPowerSums(ka),
                            mData.getLogMin(), mData.getLogMax(), mData.getLogSums(kb)
                    );
                    solver.setHessianType(2);
                    solver.setVerbose(verbose);
                    solver.solve(1e-9);
                    qs = solver.estimateQuantiles(psArray);
                }
                endTime = System.nanoTime();
                timePerTrial = (endTime - startTime) / numSolveTrials;
            }
            results.addAll(genResultMaps(
                    dname,
                    "newton",
                    psArray,
                    qs,
                    timePerTrial,
                    (long)mData.getPowerSums(1)[0]
            ));

            System.out.println("bfgs");
            numSolveTrials = 100;
//            numSolveTrials = 1;
            for (int warmUp = 0; warmUp < 2; warmUp++) {
                startTime = System.nanoTime();
                for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                    ChebyshevMomentSolver2 solver = ChebyshevMomentSolver2.fromPowerSums(
                            mData.getMin(), mData.getMax(), mData.getPowerSums(ka),
                            mData.getLogMin(), mData.getLogMax(), mData.getLogSums(kb)
                    );
                    solver.setHessianType(3);
                    solver.setSolverType(1);
                    solver.setVerbose(verbose);
                    solver.solve(1e-9);
                    qs = solver.estimateQuantiles(psArray);
                }
                endTime = System.nanoTime();
                timePerTrial = (endTime - startTime) / numSolveTrials;
            }
            results.addAll(genResultMaps(
                    dname,
                    "bfgs",
                    psArray,
                    qs,
                    timePerTrial,
                    (long)mData.getPowerSums(1)[0]
            ));


            System.out.println("mnat");
            numSolveTrials = 20000;
            for (int warmUp = 0; warmUp < 2; warmUp++) {
                if (useStandardBasis.get(di)) {
                    startTime = System.nanoTime();
                    for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                        qs = MnatSolver.estimateQuantiles(
                                mData.getMin(), mData.getMax(), mData.getPowerSums(k), ps
                        );
                    }
                    endTime = System.nanoTime();
                    timePerTrial = (endTime - startTime) / numSolveTrials;
                } else {
                    startTime = System.nanoTime();
                    for (int curTrial = 0; curTrial < numSolveTrials; curTrial++) {
                        qs = MnatSolver.estimateQuantiles(
                                mData.getLogMin(), mData.getLogMax(), mData.getLogSums(k), ps
                        );
                    }
                    endTime = System.nanoTime();
                    timePerTrial = (endTime - startTime) / numSolveTrials;
                    for (int i = 0; i < qs.length; i++) {
                        qs[i] = Math.exp(qs[i]);
                    }
                }
            }
            results.addAll(genResultMaps(
                    dname,
                    "mnat",
                    psArray,
                    qs,
                    timePerTrial,
                    (long)mData.getPowerSums(1)[0]
            ));
        }
        return results;
    }

    private List<Map<String, String>> genResultMaps(
            String dataset,
            String sketch,
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
        SolveLesionBench bench = new SolveLesionBench(confFile);

        List<Map<String, String>> results = bench.run();
        CSVOutput output = new CSVOutput();
        output.setAddTimeStamp(false);
        output.writeAllResults(results, bench.testName);
    }
}

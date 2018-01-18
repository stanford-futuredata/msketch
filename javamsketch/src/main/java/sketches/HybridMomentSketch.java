package sketches;

import msketch.BoundSolver;
import msketch.ChebyshevMomentSolver;
import msketch.MathUtil;
import msketch.MnatSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tracks both the moments and the log-moments to better support
 * lognormal, pareto, etc... distributions
 */
public class HybridMomentSketch implements QuantileSketch{
    private int k = 5;
    private double tolerance = 1e-10;
    private boolean verbose = false;

    private double min;
    private double max;
    private double logMin;
    private double logMax;
    // Stores the normal moments and the log moments
    private double[] totalSums;

    private boolean usedLog;
    private double[] errors;

    @Override
    public String getName() {
        return "hmoments";
    }

    public HybridMomentSketch(double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public int getSize() {
        return (Double.BYTES)*(4+totalSums.length);
    }

    @Override
    public double getSizeParam() {
        return k;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.k = (int)sizeParam;
    }

    @Override
    public void setVerbose(boolean flag) {
        verbose = flag;
    }

    @Override
    public void initialize() {
        this.min = Double.MAX_VALUE;
        this.max = -Double.MAX_VALUE;
        this.logMin = Double.MAX_VALUE;
        this.logMax = -Double.MAX_VALUE;
        this.totalSums = new double[2*k];
    }

    @Override
    public void setCalcError(boolean flag) {
        return;
    }

    @Override
    public void add(double[] data) {
        for (double x: data) {
            if (x < this.min) {
                this.min = x;
            } else if (x > this.max) {
                this.max = x;
            }
            double[] localSums = this.totalSums;
            localSums[0]++;
            double curPow = 1.0;
            int numPows = k;
            for (int i = 1; i < numPows; i++) {
                curPow *= x;
                localSums[i] += curPow;
            }

            if (x > 0.0) {
                double logX = Math.log(x);
                if (logX < this.logMin) {
                    this.logMin = logX;
                } else if (logX > this.logMax) {
                    this.logMax = logX;
                }
                localSums[numPows]++;
                curPow = 1.0;
                for (int i = 1; i < numPows; i++) {
                    curPow *= logX;
                    localSums[numPows+i] += curPow;
                }
            }
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches) {
        double mMin = this.min;
        double mMax = this.max;
        double mLogMin = this.logMin;
        double mLogMax = this.logMax;
        double[] mSums = this.totalSums;
        final int k2 = this.k * 2;

        for (QuantileSketch s : sketches) {
            HybridMomentSketch ms = (HybridMomentSketch) s;
            if (ms.min < mMin) {
                mMin = ms.min;
            }
            if (ms.max > mMax) {
                mMax = ms.max;
            }
            if (ms.logMin < mLogMin) {
                mLogMin = ms.logMin;
            }
            if (ms.logMax > mLogMax) {
                mLogMax = ms.logMax;
            }
            for (int i = 0; i < k2; i++) {
                mSums[i] += ms.totalSums[i];
            }
        }
        this.min = mMin;
        this.max = mMax;
        this.logMin = mLogMin;
        this.logMax = mLogMax;
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        double[] powerSums = Arrays.copyOfRange(totalSums, 0, k);
        double[] logSums = Arrays.copyOfRange(totalSums, k, 2*k);
        int m = ps.size();

        ChebyshevMomentSolver solver = ChebyshevMomentSolver.fromPowerSums(
                min, max, powerSums
        );
        solver.setVerbose(verbose);
        solver.solve(tolerance);
        boolean powConverged = solver.isConverged();
        double[] powQuantiles = new double[m];
        double[] powErrors = new double[m];
        double powAvgError = 1;
        if (powConverged) {
            for (int i = 0; i < m; i++) {
                double p = ps.get(i);
                if (p <= 0.0) {
                    powQuantiles[i] = min;
                } else if (p >= 1.0) {
                    powQuantiles[i] = max;
                } else {
                    powQuantiles[i] = solver.estimateQuantile(p, min, max);
                }
            }
            if (verbose) {
                System.out.println("==Power==");
                System.out.println("Quantiles: "+Arrays.toString(powQuantiles));
            }
            System.out.println(min);
            System.out.println(max);
            BoundSolver powBoundSolver = new BoundSolver(powerSums, min, max);
            for (int i = 0; i < m; i++) {
                double p = ps.get(i);
                if (p <= 0.0) {
                    powErrors[i] = 0.0;
                } else if (p >= 1.0) {
                    powErrors[i] = 0.0;
                } else {
                    powErrors[i] = powBoundSolver.quantileError(powQuantiles[i], p);
                }
            }
            powAvgError = MathUtil.arrayMean(powErrors);
            if (verbose) {
                System.out.println("Avg Error: "+ powAvgError);
                System.out.println(Arrays.toString(powErrors));
            }
        }

        // Deal with Logs
        solver = ChebyshevMomentSolver.fromPowerSums(
                logMin, logMax, logSums
        );
        solver.setVerbose(verbose);
        solver.solve(tolerance);
        boolean logConverged = solver.isConverged();
        double[] logQuantiles = new double[m];
        double[] logErrors = new double[m];
        double logAvgError = 1;
        if (logConverged) {
            if (verbose) {
                System.out.println("==Log==");
                System.out.println("Quantiles: "+Arrays.toString(logQuantiles));
            }
            double totalCount = powerSums[0];
            double logCount = logSums[0];
            double logMissingFrac = (totalCount - logCount) / totalCount;

            BoundSolver logBoundSolver = new BoundSolver(logSums, logMin, logMax);

            for (int i = 0; i < m; i++) {
                double p = ps.get(i) - logMissingFrac;
                if (p <= 0.0) {
                    logQuantiles[i] = Math.exp(logMin);
                    logErrors[i] = Math.abs(p);
                } else if (p >= 1.0) {
                    logQuantiles[i] = max;
                    logErrors[i] = 0.0;
                } else {
                    double curLogQuantile = solver.estimateQuantile(p, logMin, logMax);
                    logQuantiles[i] = Math.exp(curLogQuantile);
                    logErrors[i] = logBoundSolver.quantileError(curLogQuantile, p);
                }
            }

            logAvgError = MathUtil.arrayMean(logErrors);
            if (verbose) {
                System.out.println("Avg Error: " + logAvgError + " Omitted: "+logMissingFrac);
                System.out.println(Arrays.toString(logErrors));
            }
        }

        if (powConverged && logConverged) {
            usedLog = (logAvgError < powAvgError);
        } else if (!powConverged && logConverged) {
            usedLog = true;
        } else if (powConverged && !logConverged) {
            usedLog = false;
        } else {
            usedLog = false;
            throw new Exception("Did not converge");
        }

        if (usedLog) {
            errors = logErrors;
            return logQuantiles;
        } else {
            errors = powErrors;
            return powQuantiles;
        }
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}

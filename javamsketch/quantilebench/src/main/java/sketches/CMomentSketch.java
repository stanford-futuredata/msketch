package sketches;

import msolver.ChebyshevMomentSolver2;
import msolver.MathUtil;
import msolver.SimpleBoundSolver;
import scala.xml.PrettyPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Tracks both the moments and the log-moments and solves for both
 * simultaneously when possible.
 */
public class CMomentSketch implements QuantileSketch{
    private int ka = 5;
    private int kb = 5;
    private double tolerance = 1e-10;
    private boolean verbose = false;

    private double min;
    private double max;
    private double logMin;
    private double logMax;
    // Stores the normal moments and the log moments
    private double[] totalSums;

    private boolean errorBounds = false;
    private double[] errors;

    @Override
    public String getName() {
        return "cmoments";
    }

    public CMomentSketch(double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public int getSize() {
        return (Double.BYTES)*(2+1+totalSums.length);
    }

    @Override
    public double getSizeParam() {
        return ka;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.ka = (int)sizeParam;
        this.kb = ka;
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
        this.totalSums = new double[ka+kb];
    }

    @Override
    public void setCalcError(boolean flag) {
        errorBounds = flag;
        return;
    }

    public void setStats(
            double min,
            double max,
            double logMin,
            double logMax,
            double[] powerSums,
            double[] logSums
    ) {
        ka = powerSums.length;
        kb = logSums.length;
        int l = powerSums.length + logSums.length;
        totalSums = new double[l];
        for (int i = 0; i < ka; i++) {
            totalSums[i] = powerSums[i];
        }
        for (int i = 0; i < kb; i++) {
            totalSums[powerSums.length + i] = logSums[i];
        }
        this.min = min;
        this.max = max;
        this.logMin = logMin;
        this.logMax = logMax;
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
            for (int i = 1; i < ka; i++) {
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
                localSums[ka]++;
                curPow = 1.0;
                for (int i = 1; i < kb; i++) {
                    curPow *= logX;
                    localSums[ka+i] += curPow;
                }
            }
        }
    }

    @Override
    public QuantileSketch merge(ArrayList<QuantileSketch> sketches, int startIndex, int endIndex) {
        double mMin = this.min;
        double mMax = this.max;
        double mLogMin = this.logMin;
        double mLogMax = this.logMax;
        double[] mSums = this.totalSums;
        final int l = this.totalSums.length;

        for (int i = startIndex; i < endIndex; i++) {
            CMomentSketch ms = (CMomentSketch) sketches.get(i);
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
            for (int j = 0; j < l; i++) {
                mSums[j] += ms.totalSums[j];
            }
        }
        this.min = mMin;
        this.max = mMax;
        this.logMin = mLogMin;
        this.logMax = mLogMax;
        return this;
    }

    @Override
    public QuantileSketch parallelMerge(ArrayList<QuantileSketch> sketches, int numThreads) {
        int numSketches = sketches.size();
        final CountDownLatch doneSignal = new CountDownLatch(numThreads);
        ArrayList<QuantileSketch> mergedSketches = new ArrayList<QuantileSketch>(numThreads);
        for (int threadNum = 0; threadNum < numThreads; threadNum++) {
            final int curThreadNum = threadNum;
            final int startIndex = (numSketches * threadNum) / numThreads;
            final int endIndex = (numSketches * (threadNum + 1)) / numThreads;
            Runnable ParallelMergeRunnable = () -> {
                CMomentSketch ms = new CMomentSketch(this.tolerance);
                ms.setSizeParam(this.getSizeParam());
                ms.initialize();
                mergedSketches.set(curThreadNum, ms.merge(sketches, startIndex, endIndex));
                doneSignal.countDown();
            };
            Thread ParallelMergeThread = new Thread(ParallelMergeRunnable);
            ParallelMergeThread.start();
        }
        try {
            doneSignal.await();
        } catch (InterruptedException ex) {ex.printStackTrace();}

        return merge(mergedSketches, 0, mergedSketches.size());
    }


    @Override
    public double[] getQuantiles(List<Double> pList) throws Exception {
        double[] powerSums = Arrays.copyOfRange(totalSums, 0, ka);
        double[] logSums = Arrays.copyOfRange(totalSums, ka, ka+kb);

        ChebyshevMomentSolver2 solver;
        boolean useStandardBasis = true;
        if (min > 0) {
            solver = ChebyshevMomentSolver2.fromPowerSums(
                    min, max, powerSums,
                    logMin, logMax, logSums
            );
            useStandardBasis = solver.isUseStandardBasis();
        } else {
            useStandardBasis = true;
            logSums = new double[1];
            solver = ChebyshevMomentSolver2.fromPowerSums(
                    min, max, powerSums,
                    logMin, logMax, logSums
            );
        }
        solver.setVerbose(verbose);
        solver.solve(tolerance);
        int m = pList.size();
        double[] ps = MathUtil.listToArray(pList);
        double[] quantiles = solver.estimateQuantiles(ps);

        errors = new double[m];
        if (errorBounds) {
            if (useStandardBasis) {
                double[] moments = MathUtil.powerSumsToMoments(powerSums);
                SimpleBoundSolver boundSolver = new SimpleBoundSolver(solver.getNumNormalPowers());
                double[] boundSizes = boundSolver.solveBounds(moments, quantiles);
                errors = boundSolver.getMaxErrors(moments, quantiles, ps, boundSizes);
            } else {
                double[] logQuantiles = new double[m];
                for (int i = 0; i < m; i++) {
                    logQuantiles[i] = Math.log(quantiles[i]);
                }
                double[] moments = MathUtil.powerSumsToMoments(logSums);
                SimpleBoundSolver boundSolver = new SimpleBoundSolver(solver.getNumNormalPowers());
                double[] boundSizes = boundSolver.solveBounds(moments, logQuantiles);
                errors = boundSolver.getMaxErrors(moments, logQuantiles, ps, boundSizes);
            }
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}

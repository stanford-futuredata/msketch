package sketches;

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ReservoirSamplingSketch implements QuantileSketch {
    private int size;
    private int numProcessed;
    private double[] reservoir;
    private double[] errors;
    private Random r;

    public ReservoirSamplingSketch() { r = new Random(); }

    @Override
    public String getName() {
        return "reservoir_sampling";
    }

    @Override
    public int getSize() {
        return (Long.BYTES + this.size * Double.BYTES);
    }

    @Override
    public double getSizeParam() {
        return size;
    }

    @Override
    public void setSizeParam(double sizeParam) {
        this.size = (int)sizeParam;
    }

    @Override
    public void setCalcError(boolean flag) {
        return;
    }

    @Override
    public void initialize() {
        this.reservoir = new double[size];
    }

    @Override
    // Assumes add is only used one time
    public void add(double[] data) {
        numProcessed = data.length;

        if (size > data.length) {
            System.arraycopy(data, 0, reservoir, 0, data.length);
            return;
        }

        System.arraycopy(data, 0, reservoir, 0, size);

        for (int i = size; i < data.length; i++) {
            int j = r.nextInt(i + 1);
            if (j < size) {
                reservoir[j] = data[i];
            }
        }
    }

    @Override
    public QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex) {
        double[] randomDoubles = new double[size];
        for (int i = 0; i < size; i++) {
            randomDoubles[i] = r.nextDouble();
        }
        Arrays.sort(randomDoubles);

        int totalNumProcessed = 0;
        int maxNumProcessed = 0;
        ReservoirSamplingSketch rss;
        for (QuantileSketch s : sketches) {
            rss = (ReservoirSamplingSketch)s;
            totalNumProcessed += rss.numProcessed;
            maxNumProcessed = Math.max(maxNumProcessed, rss.numProcessed);
        }

        int idx = 0;
        int reservoirIdx = 0;
        double threshold = 0.0;
        int[] range = new int[maxNumProcessed];
        for (int sketchIndex = startIndex; sketchIndex < endIndex; sketchIndex++) {
            rss = (ReservoirSamplingSketch) sketches.get(sketchIndex);
            threshold += (double)rss.numProcessed / totalNumProcessed;
            int sampleSize = 0;
            while (idx < size && randomDoubles[idx] < threshold) {
                idx++;
                sampleSize++;
            }

            int rangeSize = Math.min(rss.size, rss.numProcessed);

            if (rangeSize < sampleSize) {
                System.arraycopy(rss.reservoir, 0, reservoir, reservoirIdx, rangeSize);
                reservoirIdx += rangeSize;
                continue;
            }

            // Fisher-Yates to sample the sketch's reservoir
            for (int i = 0; i < rangeSize; i++) {
                range[i] = i;
            }
            for (int i = 0; i < sampleSize; i++) {
                int j = r.nextInt(rangeSize - i) + i;
                int temp = range[j];
                range[j] = range[i];
                range[i] = temp;
            }

//            // Reservoir to sample the sketch's reservoir
//            for (int i = 0; i < sampleSize; i++) {
//                range[i] = i;
//            }
//            Random rand = new Random();
//            for (int i = sampleSize; i < rangeSize; i++) {
//                int j = rand.nextInt(i+1);
//                if (j < sampleSize) {
//                    range[j] = i;
//                }
//            }

            for (int i = 0; i < sampleSize; i++) {
                reservoir[reservoirIdx++] = rss.reservoir[range[i]];
            }
        }

        numProcessed = reservoirIdx;
        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> ps) throws Exception {
        int m = ps.size();
        double[] quantiles = QuantileUtil.getTrueQuantiles(ps, reservoir);

        errors = new double[m];
        for (int i = 0; i < m; i++) {
            double p = ps.get(i);
            errors[i] = 2.5*FastMath.sqrt(p * (1-p) / this.size);
        }
        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }


    /* Experimental code */

    // http://www.cs.umd.edu/~samir/498/vitter.pdf
    private void addVitter(double[] data) {
        numProcessed = data.length;

        if (size > data.length) {
            System.arraycopy(data, 0, reservoir, 0, data.length);
            return;
        }

        System.arraycopy(data, 0, reservoir, 0, size);

        int t = size;
        int s;
        while (t < data.length) {
            if (t <= 22 * size) {
                s = algorithmX(t);
            } else {
                s = algorithmZ(t);
            }

            t += s;
            if (t >= data.length) {
                break;
            }
            int toReplace = r.nextInt(size);
            reservoir[toReplace] = data[t];
            t += 1;
        }
    }

    private int algorithmX(int t) {
        double v = r.nextDouble();
        int s = 0;
        t += 1;
        double quot = (double)(t - size) / t;
        while (quot > v) {
            s += 1;
            t += 1;
            quot *= (double)(t - size) / t;
        }
        return s;
    }

    private int algorithmZ(int t) {
        int s;
        double w = Math.exp(-Math.log(r.nextDouble()) / size);  // TODO: this should be a global var
        double term = (double)t - size + 1;
        while (true) {
            double u = r.nextDouble();
            double x = t * (w - 1.0);
            s = (int) x;
            double tmp = (t + 1) / term;
            double lhs = Math.exp(Math.log(u * tmp * tmp * (term + s) / (t + s)) / size);
            double rhs = term * (t + x) / (t * (term + s));
            if (lhs <= rhs) {
                w = rhs / lhs;
                break;
            }
            double y = (((u * (t + 1)) / term) * (t + s + 1)) / (t + x);
            double denom;
            double numer_lim;
            if (size < s) {
                denom = t;
                numer_lim = term + s;
            } else {
                denom = t - size + s;
                numer_lim = t + 1;
            }

            for (long numer = t + s; numer >= numer_lim; numer -= 1) {
                y *= numer / denom;
                denom -= 1;
            }
            w = Math.exp(-Math.log(r.nextDouble()) / size);
            if (Math.exp(Math.log(y) / size) <= (t + x) / t) {
                break;
            }
        }
        return s;
    }

    // Weighted reservoir sampling
    public QuantileSketch slowMerge(ArrayList<QuantileSketch> sketches) {
        double weight = 0.0;

        // Fill the reservoir
        int sketchIdx = 0;
        int dataIdx = 0;
        int lengthToCopy = 0;
        ReservoirSamplingSketch s = (ReservoirSamplingSketch) sketches.get(0);
        for (; sketchIdx < sketches.size(); sketchIdx++) {
            s = (ReservoirSamplingSketch)sketches.get(sketchIdx);
            lengthToCopy = Math.min(size - numProcessed, s.numProcessed);
            System.arraycopy(s.reservoir, 0, reservoir, numProcessed, lengthToCopy);
            weight += (double)Math.min(s.size, s.numProcessed) * lengthToCopy / (s.numProcessed * size);
            numProcessed += lengthToCopy;
            if (numProcessed == size) {
                break;
            }
        }

        // Everything fits inside the reservoir
        if (sketchIdx == sketches.size()) {
            return this;
        }

        // Finish processing the current sketch
        if (lengthToCopy < Math.min(s.size, s.numProcessed)) {
            dataIdx = lengthToCopy;
            double sketchWeight = Math.min(s.size, s.numProcessed) / (s.numProcessed);
            for (; dataIdx < s.numProcessed; dataIdx++) {
                weight += sketchWeight / size;
                double p = sketchWeight / weight;
                double j = r.nextDouble();
                if (j <= p) {
                    reservoir[r.nextInt(size)] = s.reservoir[dataIdx];
                }
            }
        }
        sketchIdx++;

        for (; sketchIdx < sketches.size(); sketchIdx++) {
            s = (ReservoirSamplingSketch)sketches.get(sketchIdx);
            double sketchWeight = Math.min(s.size, s.numProcessed) / (s.numProcessed);
            for (dataIdx = 0; dataIdx < s.numProcessed; dataIdx++) {
                weight += sketchWeight / size;
                double p = sketchWeight / weight;
                double j = r.nextDouble();
                if (j <= p) {
                    reservoir[r.nextInt(size)] = s.reservoir[dataIdx];
                }
            }
        }

        return this;
    }
}

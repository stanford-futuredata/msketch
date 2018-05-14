package sketches;

import msolver.ChebyshevMomentSolver2;
import msolver.MathUtil;
import msolver.SimpleBoundSolver;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Integer.MAX_VALUE;

/**
 * Tracks both the moments and the log-moments and solves for both
 * simultaneously when possible.
 */
public class RandomSketch implements QuantileSketch{
    private double sizeParam;  // inverse of epsilon
    private int h;  // height
    private int b;  // num buffers
    private int s;  // buffer size
    private boolean verbose = false;

    static double delta = 0.1;

    private int activeLevel;
    private ArrayList<ArrayList<Double>> freeBuffers;
    private HashMap<Integer, ArrayList<ArrayList<Double>>> usedBuffers;
    private ArrayList<Double> curBuffer;
    private ArrayList<Double> tmpBuffer;  // for merging
    private HashMap<Integer, ArrayList<Double>> orphanBuffers;

    private int nextToSample;
    private int sampleBlockLength;
    private int sampleBlockSeen;

    private ArrayList<QuantileEntry> quantileEntries;
    private boolean quantileEntriesUpdated;

    private boolean errorBounds = false;
    private double[] errors;

    private class QuantileEntry {
        double value;
        double quantile;
        double weight;

        public QuantileEntry(double value, double quantile, double weight) {
            this.value = value;
            this.quantile = quantile;
            this.weight = weight;
        }
    }

    private class CompareByValue implements Comparator<QuantileEntry> {
        public int compare(QuantileEntry a, QuantileEntry b) {
            return Double.compare(a.value, b.value);
        }
    }

    private class CompareByQuantile implements Comparator<QuantileEntry> {
        public int compare(QuantileEntry a, QuantileEntry b) {
            return Double.compare(a.quantile, b.quantile);
        }
    }

    @Override
    public String getName() {
        return "random";
    }

    @Override
    public int getSize() {
        return (Double.BYTES) * (b * s);
    }

    @Override
    public double getSizeParam() {
        return sizeParam;
    }

    @Override
    public void setSizeParam(double sizeParam) { this.sizeParam = sizeParam; }

    @Override
    public void setVerbose(boolean flag) {
        verbose = flag;
    }

    @Override
    public void initialize() {
        // s and h copied from https://github.com/coolwanglu/quantile-alg/blob/master/random.h
        this.s = (int) Math.ceil(sizeParam * Math.sqrt(Math.log(1/delta) / Math.log(2)));
        this.h = (int) Math.ceil(Math.log(sizeParam * Math.sqrt(Math.log(1/delta) / Math.log(2)) * 5.0/12.0) / Math.log(2));
        this.b = h + 1;
        this.activeLevel = 0;
        this.freeBuffers = new ArrayList<>();
        this.usedBuffers = new HashMap<>();
        this.orphanBuffers = new HashMap<>();
        for (int i = 0; i < b; i++) {
            freeBuffers.add(new ArrayList<>(s));
        }
        this.curBuffer = freeBuffers.remove(freeBuffers.size() - 1);
        this.tmpBuffer = new ArrayList<>(s);
        this.quantileEntriesUpdated = false;
    }

    @Override
    public void setCalcError(boolean flag) {
        errorBounds = flag;
        return;
    }

    @Override
    public void add(double[] data) {
        for (double x: data) {
            // check if the value should be sampled
            if (!shouldSampleNext()) {
                continue;
            }

            curBuffer.add(x);
            if (curBuffer.size() == s) {
                Collections.sort(curBuffer);
                insertBuffer(curBuffer, activeLevel);
                if (freeBuffers.size() == 0) {
                    collapseForAdding();
                }
                curBuffer = freeBuffers.remove(freeBuffers.size() - 1);
            }
        }

        quantileEntriesUpdated = false;
    }

    private boolean shouldSampleNext() {
        if (activeLevel == 0) {
            return true;
        }

        boolean shouldSampleNext = (sampleBlockSeen == nextToSample);
        sampleBlockSeen++;
        if (sampleBlockSeen == sampleBlockLength) {
            sampleBlockSeen = 0;
            nextToSample = ThreadLocalRandom.current().nextInt(sampleBlockLength);
        }

        return shouldSampleNext;
    }

    private void insertBuffer(ArrayList<Double> curBuffer, int level) {
        if (usedBuffers.containsKey(level)) {
            usedBuffers.get(level).add(curBuffer);
        } else {
            ArrayList<ArrayList<Double>> usedBuffersInLevel = new ArrayList<>();
            usedBuffersInLevel.add(curBuffer);
            usedBuffers.put(level, usedBuffersInLevel);
        }
    }

    // Assumes buffers one and two are sorted
    private void mergeBuffers(ArrayList<Double> bufferOne, ArrayList<Double> bufferTwo, ArrayList<Double> target) {
        boolean use = ThreadLocalRandom.current().nextBoolean();
        int i = 0;
        int j = 0;
        while (true) {
            if (bufferOne.get(i) < bufferTwo.get(j)) {
                if ((use = !use)) {
                    target.add(bufferOne.get(i));
                }
                i++;
                if (i == bufferOne.size()) {
                    while (j != bufferTwo.size()) {
                        if ((use = !use)) {
                            target.add(bufferTwo.get(j));
                        }
                        j++;
                    }
                    break;
                }
            } else {
                if ((use = !use)) {
                    target.add(bufferTwo.get(j));
                }
                j++;
                if (j == bufferTwo.size()) {
                    while (i != bufferOne.size()) {
                        if ((use = !use)) {
                            target.add(bufferOne.get(i));
                        }
                        i++;
                    }
                    break;
                }
            }
        }
    }

    private void collapse(boolean merging) {
        ArrayList<ArrayList<Double>> usedBuffersInLevel = usedBuffers.get(activeLevel);
        if (usedBuffersInLevel.size() == 1) {  // should only hit for merging
            orphanBuffers.put(activeLevel, usedBuffersInLevel.get(0));
            usedBuffers.remove(activeLevel);
            activeLevel = Collections.min(this.usedBuffers.keySet());
            sampleBlockLength = (int) Math.pow(2, activeLevel);
        } else {
            ArrayList<Double> bufferOne = usedBuffersInLevel.remove(usedBuffersInLevel.size() - 1);
            ArrayList<Double> bufferTwo = usedBuffersInLevel.remove(usedBuffersInLevel.size() - 1);

            // Merge the buffers: bufferOne becomes the merged buffer, bufferTwo becomes free
//        ArrayList<Double> mergedBuffer = bufferOne;
//        mergedBuffer.addAll(bufferTwo);
//        Collections.sort(mergedBuffer);
//        int offset = rand.nextInt(2);
//        for (int i = 0; i < s; i++) {
//            mergedBuffer.set(i, mergedBuffer.get(2*i + offset));
//        }
//        mergedBuffer.subList(s, 2*s).clear();
//        insertBuffer(mergedBuffer,activeLevel + 1);
//        bufferTwo.clear();
//        freeBuffers.add(bufferTwo);

            mergeBuffers(bufferOne, bufferTwo, tmpBuffer);
            insertBuffer(tmpBuffer, activeLevel + 1);
            bufferOne.clear();
            tmpBuffer = bufferOne;

            if (!merging) {
                bufferTwo.clear();
                freeBuffers.add(bufferTwo);
            }

            // If all buffers in the active level have been merged, then the active level increases
            if (usedBuffersInLevel.size() == 0) {
                usedBuffers.remove(activeLevel);
                activeLevel++;
                sampleBlockLength = (int) Math.pow(2, activeLevel);
            }
        }
    }

    private void collapseForMerging() {
        collapse(true);
    }

    private void collapseForAdding() {
        collapse(false);
    }

    private void constructQuantileEntries() {
        quantileEntries = new ArrayList<>();

        double totalWeight = 0.0;
        for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> level : usedBuffers.entrySet()) {
            double weight = Math.pow(2, level.getKey());
            for (ArrayList<Double> buffer : level.getValue()) {
                for (double value : buffer) {
                    QuantileEntry entry = new QuantileEntry(value, 0.0, weight);
                    quantileEntries.add(entry);
                }
            }
            totalWeight += level.getValue().size() * s * weight;
        }

        double curWeight = Math.pow(2, activeLevel);
        for (double value : curBuffer) {
            QuantileEntry entry = new QuantileEntry(value, 0.0, curWeight);
            quantileEntries.add(entry);
        }
        totalWeight += curBuffer.size() * curWeight;

        Collections.sort(quantileEntries, new CompareByValue());

        // Compute correct quantiles
        double curQuantile = 0.0;
        for (QuantileEntry entry : quantileEntries) {
            entry.quantile = curQuantile;
            curQuantile += entry.weight / totalWeight;
        }
    }

    // Returns number of new buffers
    private int collapsePartialBuffer(ArrayList<Double> buffer, int level) {
        int numNewBuffers = 0;
        int blockLength = (int) Math.pow(2, activeLevel - level);
        if (blockLength <= 1) {
            if (buffer.size() < s - curBuffer.size()) {
                curBuffer.addAll(buffer);
            } else {
                int j = 0;
                for (; j < s - curBuffer.size(); j++) {
                    curBuffer.add(buffer.get(j));
                }
                Collections.sort(curBuffer);
                insertBuffer(curBuffer, activeLevel);
                numNewBuffers++;
                curBuffer = new ArrayList<>();
                for (; j < buffer.size(); j++) {
                    curBuffer.add(buffer.get(j));
                }
            }
        } else {
            int numSamples = buffer.size() / blockLength;
            for (int j = 0; j < numSamples; j++) {
                int toSample = ThreadLocalRandom.current().nextInt(blockLength);
                curBuffer.add(buffer.get(j * blockLength + toSample));
                if (curBuffer.size() == s) {
                    Collections.sort(curBuffer);
                    insertBuffer(curBuffer, activeLevel);
                    numNewBuffers++;
                    curBuffer = new ArrayList<>();
                }
            }
            int toSample = ThreadLocalRandom.current().nextInt(blockLength);
            if (numSamples * blockLength + toSample < buffer.size()) {
                curBuffer.add(buffer.get(numSamples * blockLength + toSample));
                if (curBuffer.size() == s) {
                    Collections.sort(curBuffer);
                    insertBuffer(curBuffer, activeLevel);
                    numNewBuffers++;
                    curBuffer = new ArrayList<>();
                }
            }
        }
        return numNewBuffers;
    }

    @Override
    public QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex) {
        orphanBuffers.clear();
        int numBuffers = 0;
        activeLevel = MAX_VALUE;

        // Insert all buffers into the tree
        for (int i = startIndex; i < endIndex; i++) {
            RandomSketch rs = (RandomSketch) sketches.get(i);
            for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> entry : rs.usedBuffers.entrySet()) {
                int level = entry.getKey();
                numBuffers += entry.getValue().size();
                if (!usedBuffers.containsKey(level)) {
                    usedBuffers.put(level, new ArrayList<>());
                    if (level < activeLevel) {
                        activeLevel = level;
                    }
                }
                for (ArrayList<Double> buffer : entry.getValue()) {
                    usedBuffers.get(level).add(new ArrayList<>(buffer));
                }
            }
        }

        // Merge until b buffers remain
        for (; numBuffers > b; numBuffers--) {
            collapseForMerging();
        }

        // Edge case: no full buffers were inserted into the hierarchy
        // TODO: how to choose active level? This works for our benchmarks because
        // activeLevel will be similar across merged sketches, but might not work
        // in practice.
        if (numBuffers == 0) {
            activeLevel = -1;
            for (int i = startIndex; i < endIndex; i++) {
                RandomSketch rs = (RandomSketch) sketches.get(i);
                if (rs.activeLevel > activeLevel) {
                    activeLevel = rs.activeLevel;
                }
            }
        }

        // Incorporate curBuffer and orphan buffers
        int numNewBuffers = 0;
        for (int i = startIndex; i < endIndex; i++) {
            RandomSketch rs = (RandomSketch) sketches.get(i);
            numNewBuffers += collapsePartialBuffer(rs.curBuffer, rs.activeLevel);
        }
        for (Map.Entry<Integer, ArrayList<Double>> entry : orphanBuffers.entrySet()) {
            numNewBuffers += collapsePartialBuffer(entry.getValue(), entry.getKey());
        }
        numBuffers += numNewBuffers;

        for (; numBuffers > b; numBuffers--) {
            collapseForMerging();
        }

        // Fill up freeBuffers
        numBuffers += freeBuffers.size();
        if (numBuffers < b) {
            for (; numBuffers < b; numBuffers++) {
                freeBuffers.add(new ArrayList<>(s));
            }
        }

        return this;
    }

    @Override
    public double[] getQuantiles(List<Double> pList) throws Exception {
        if (!quantileEntriesUpdated) {
            constructQuantileEntries();
            quantileEntriesUpdated = true;
        }

        int m = pList.size();
        double[] quantiles = new double[m];
        for (int i = 0; i < m; i++) {
            double p = pList.get(i);
            QuantileEntry dummy = new QuantileEntry(0.0, p, 0);
            int quantileEntryIdx = Collections.binarySearch(quantileEntries, dummy, new CompareByQuantile());
            if (quantileEntryIdx >= 0) {
                quantiles[i] = quantileEntries.get(quantileEntryIdx).value;
            } else {
                if (quantileEntryIdx == -1) {
                    // less than smallest value
                    quantiles[i] = quantileEntries.get(0).quantile;
                } else if (quantileEntryIdx == -quantileEntries.size()-1) {
                    // greater than largest value
                    quantiles[i] = quantileEntries.get(quantileEntries.size()-1).quantile;
                } else {
                    // linearly interpolate between the closest quantile entries.
                    QuantileEntry lowerEntry = quantileEntries.get(-quantileEntryIdx - 2);
                    QuantileEntry higherEntry = quantileEntries.get(-quantileEntryIdx - 1);
                    double quantileDiff = higherEntry.quantile - lowerEntry.quantile;
                    quantiles[i] = (p - lowerEntry.quantile) / quantileDiff * lowerEntry.value
                            + (higherEntry.quantile - p) / quantileDiff * higherEntry.value;
                }
            }
        }

        errors = new double[m];
        for (int i = 0; i < m; i++) {
            errors[i] = 1.0/sizeParam;
        }

        return quantiles;
    }

    @Override
    public double[] getErrors() {
        return errors;
    }
}

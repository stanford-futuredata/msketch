package sketches;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracks both the moments and the log-moments and solves for both
 * simultaneously when possible.
 */
public class RandomSketch implements QuantileSketch{
    private double sizeParam;  // inverse of epsilon
    private int h;  // height
    public int b;  // num buffers
    public int s;  // buffer size
    private boolean verbose = false;

    static double delta = 0.1;

    public int activeLevel;
    private ArrayList<ArrayList<Double>> freeBuffers;
    public HashMap<Integer, ArrayList<ArrayList<Double>>> usedBuffers;
    public ArrayList<Double> curBuffer;
    private ArrayList<Double> tmpBuffer;  // for merging
    private HashMap<Integer, ArrayList<ArrayList<Double>>> partialBuffers;
    private double totalWeight;
    private ThreadLocalRandom rand;

    private int nextToSample;
    private int sampleBlockLength;
    private int nextSampleBlockLength;
    private int sampleBlockSeen;

    private ArrayList<QuantileEntry> quantileEntries;

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

        @Override
        public String toString() {
            return String.format("%.4f: %s", quantile, value);
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
        int numUsedBuffers = 0;
        for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> level : usedBuffers.entrySet()) {
            numUsedBuffers += level.getValue().size();
        }
        return (Double.BYTES) * (numUsedBuffers * s + curBuffer.size());
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
//        this.s = (int) Math.ceil(sizeParam * Math.sqrt(Math.log(sizeParam) / Math.log(2)));
//        this.h = (int) Math.ceil(Math.log(sizeParam) / Math.log(2));
        this.b = h + 1;
        this.activeLevel = 0;
        this.freeBuffers = new ArrayList<>();
        this.usedBuffers = new HashMap<>();
        this.partialBuffers = new HashMap<>();
        for (int i = 0; i < b; i++) {
            freeBuffers.add(new ArrayList<>(s));
        }
        this.curBuffer = freeBuffers.remove(freeBuffers.size() - 1);
        this.tmpBuffer = new ArrayList<>(s);
        this.totalWeight = 0;
        this.rand = ThreadLocalRandom.current();
        this.nextToSample = rand.nextInt(2);  // will first be used when activeLevel = 1
        this.sampleBlockLength = 2;  // will first be used when activeLevel = 1
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
            if (activeLevel == 0) {
                totalWeight++;
            } else {
                totalWeight += sampleBlockLength;
            }
            if (curBuffer.size() == s) {
                Collections.sort(curBuffer);
                insertBuffer(curBuffer, activeLevel);
                if (freeBuffers.size() == 0) {
                    collapseForAdding();
                }
                curBuffer = freeBuffers.remove(freeBuffers.size() - 1);
            }
        }
    }

    private boolean shouldSampleNext() {
        if (activeLevel == 0) {
            return true;
        }

        boolean shouldSampleNext = (sampleBlockSeen == nextToSample);
        sampleBlockSeen++;
        if (sampleBlockSeen == sampleBlockLength) {
            sampleBlockSeen = 0;
            sampleBlockLength = nextSampleBlockLength;
            nextToSample = rand.nextInt(sampleBlockLength);
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
    public void mergeBuffers(ArrayList<Double> bufferOne, ArrayList<Double> bufferTwo, ArrayList<Double> target) {
        boolean use = rand.nextBoolean();
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
        if (usedBuffersInLevel.size() == 1) {  // should rarely happen
            return;
        } else {
            ArrayList<Double> bufferOne = usedBuffersInLevel.remove(usedBuffersInLevel.size() - 1);
            ArrayList<Double> bufferTwo = usedBuffersInLevel.remove(usedBuffersInLevel.size() - 1);

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
                nextSampleBlockLength = (int) Math.pow(2, activeLevel);
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

        // Process buffers in hierarchy
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

        // Process elements in curBuffer
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
            curQuantile += 0.5 * entry.weight / totalWeight;
            entry.quantile = curQuantile;
            curQuantile += 0.5 * entry.weight / totalWeight;
        }
    }

    private int collapseLevel(ArrayList<ArrayList<Double>> buffers, int level) {
        int numNewBuffers = 0;
        int blockLength = (int) Math.pow(2, activeLevel - level);
        int toSample = rand.nextInt(blockLength);
        int seenSoFar = 0;
        for (ArrayList<Double> buffer : buffers) {
            int curIdx = 0;
            while (true) {
                if (toSample >= seenSoFar && toSample - seenSoFar < buffer.size() - curIdx) {
                    curBuffer.add(buffer.get(toSample - seenSoFar + curIdx));
                    if (curBuffer.size() == s) {
                        Collections.sort(curBuffer);
                        insertBuffer(curBuffer, activeLevel);
                        numNewBuffers++;
                        curBuffer = new ArrayList<>();
                    }
                }
                if (blockLength - seenSoFar < buffer.size() - curIdx) {
                    toSample = rand.nextInt(blockLength);
                    curIdx += (blockLength - seenSoFar);
                    seenSoFar = 0;
                } else {
                    seenSoFar += buffer.size() - curIdx;
                    break;
                }
            }
        }
        return numNewBuffers;
    }

    @Override
    public QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            RandomSketch rs = (RandomSketch) sketches.get(i);
            update(rs);
        }
        return this;
    }

    public int numUsedBuffers() {
        int numUsedBuffers = 0;
        for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> entry : usedBuffers.entrySet()) {
            numUsedBuffers += entry.getValue().size();
        }
        return numUsedBuffers;
    }

    private void update(RandomSketch sketch) {
        totalWeight += sketch.totalWeight;
        int curBufferLevel = activeLevel;
        activeLevel = Math.max((int) (Math.log(totalWeight / ((b-1) * s)) / Math.log(2)), 0);

        if (curBufferLevel < activeLevel) {
            if (!usedBuffers.containsKey(curBufferLevel)) {
                usedBuffers.put(curBufferLevel, new ArrayList<>());
            }
            usedBuffers.get(curBufferLevel).add(curBuffer);
            curBuffer = new ArrayList<>(s);
        }

        // Insert all buffers into the tree
        int numBuffers = 0;

        // Process current hierarchy
        ArrayList<Integer> toRemove = new ArrayList<>();
        for (int level : usedBuffers.keySet()) {
            if (level >= activeLevel) {
                numBuffers += usedBuffers.get(level).size();
            } else {
                toRemove.add(level);
                numBuffers += collapseLevel(usedBuffers.get(level), level);
            }
        }
        usedBuffers.keySet().removeAll(toRemove);

        // Process new sketch
        if (sketch.usedBuffers.size() == 0) {
            // Not very efficient, but this case should not happen in practice if sizeParam is reasonable.
            ArrayList<ArrayList<Double>> level = new ArrayList<>();
            level.add(sketch.curBuffer);
            collapseLevel(level, sketch.activeLevel);
        } else {
            for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> entry : sketch.usedBuffers.entrySet()) {
                int level = entry.getKey();
                if (level >= activeLevel) {
                    numBuffers += entry.getValue().size();
                    if (!usedBuffers.containsKey(level)) {
                        usedBuffers.put(level, new ArrayList<>());
                    }
                    for (ArrayList<Double> buffer : entry.getValue()) {
                        usedBuffers.get(level).add(new ArrayList<>(buffer));
                    }
                    if (level == sketch.activeLevel) {
                        ArrayList<Double> buffer = sketch.curBuffer;
                        for (int i = 0; i < buffer.size(); i++) {
                            curBuffer.add(buffer.get(i));
                            if (curBuffer.size() == s) {
                                Collections.sort(curBuffer);
                                insertBuffer(curBuffer, activeLevel);
                                numBuffers++;
                                curBuffer = new ArrayList<>();
                            }
                        }
                    }
                } else {
                    ArrayList<ArrayList<Double>> buffers = entry.getValue();
                    if (level == sketch.activeLevel) {
                        buffers.add(sketch.curBuffer);
                        numBuffers += collapseLevel(entry.getValue(), level);
                        buffers.remove(buffers.size() - 1);
                    } else {
                        numBuffers += collapseLevel(entry.getValue(), level);
                    }
                }
            }
        }

        for (; numBuffers > b - 1; numBuffers--) {
            collapseForMerging();
        }

        // Fill up freeBuffers
        numBuffers += freeBuffers.size();
        for (; numBuffers > b - 1; numBuffers--) {
            freeBuffers.remove(freeBuffers.size() - 1);
        }
        for (; numBuffers < b - 1; numBuffers++) {
            freeBuffers.add(new ArrayList<>(s));
        }
    }

    @Override
    public double[] getQuantiles(List<Double> pList) {
        constructQuantileEntries();

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
                    quantiles[i] = quantileEntries.get(0).value;
                } else if (quantileEntryIdx == -quantileEntries.size()-1) {
                    // greater than largest value
                    quantiles[i] = quantileEntries.get(quantileEntries.size()-1).value;
                } else {
                    // linearly interpolate between the closest quantile entries.
                    QuantileEntry lowerEntry = quantileEntries.get(-quantileEntryIdx - 2);
                    QuantileEntry higherEntry = quantileEntries.get(-quantileEntryIdx - 1);
                    if (lowerEntry.value == higherEntry.value) {
                        quantiles[i] = lowerEntry.value;
                    } else {
                        double quantileDiff = higherEntry.quantile - lowerEntry.quantile;
                        quantiles[i] = (p - lowerEntry.quantile) / quantileDiff * lowerEntry.value
                                + (higherEntry.quantile - p) / quantileDiff * higherEntry.value;
                    }
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


    /* Deprecated code: merging all sketches at once. */
//    // Collapses a partially-filled buffers. Returns number of new buffers
//    private int collapsePartialBuffers() {
//        int numNewBuffers = 0;
//        for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> entry : partialBuffers.entrySet()) {
//            int level = entry.getKey();
//            int blockLength = (int) Math.pow(2, activeLevel - level);
//            int toSample = rand.nextInt(blockLength);
//            int seenSoFar = 0;
//            for (ArrayList<Double> buffer : entry.getValue()) {
//                int curIdx = 0;
//                while (true) {
//                    if (toSample >= seenSoFar && toSample - seenSoFar < buffer.size() - curIdx) {
//                        curBuffer.add(buffer.get(toSample - seenSoFar + curIdx));
//                        if (curBuffer.size() == s) {
//                            Collections.sort(curBuffer);
//                            insertBuffer(curBuffer, activeLevel);
//                            numNewBuffers++;
//                            curBuffer = new ArrayList<>();
//                        }
//                    }
//                    if (blockLength - seenSoFar < buffer.size() - curIdx) {
//                        toSample = rand.nextInt(blockLength);
//                        curIdx += (blockLength - seenSoFar);
//                        seenSoFar = 0;
//                    } else {
//                        seenSoFar += buffer.size() - curIdx;
//                        break;
//                    }
//                }
//            }
//        }
//        return numNewBuffers;
//    }

//    @Override
//    public QuantileSketch merge(List<QuantileSketch> sketches, int startIndex, int endIndex) {
//        partialBuffers.clear();
//
//        for (int i = startIndex; i < endIndex; i++) {
//            RandomSketch rs = (RandomSketch) sketches.get(i);
//            totalWeight += rs.totalWeight;
//        }
//
//        activeLevel = (int) (Math.log(totalWeight / ((b-1) * s)) / Math.log(2));
//
//        // Insert all buffers into the tree
//        int numBuffers = 0;
//        for (int i = startIndex; i < endIndex; i++) {
//            RandomSketch rs = (RandomSketch) sketches.get(i);
//            for (Map.Entry<Integer, ArrayList<ArrayList<Double>>> entry : rs.usedBuffers.entrySet()) {
//                int level = entry.getKey();
//                if (level >= activeLevel) {
//                    numBuffers += entry.getValue().size();
//                    if (!usedBuffers.containsKey(level)) {
//                        usedBuffers.put(level, new ArrayList<>());
//                    }
//                    for (ArrayList<Double> buffer : entry.getValue()) {
//                        usedBuffers.get(level).add(new ArrayList<>(buffer));
//                    }
//                } else {
//                    if (!partialBuffers.containsKey(level)) {
//                        partialBuffers.put(level, new ArrayList<>());
//                    }
//                    for (ArrayList<Double> buffer : entry.getValue()) {
//                        partialBuffers.get(level).add(new ArrayList<>(buffer));
//                    }
//                }
//            }
//            if (!partialBuffers.containsKey(rs.activeLevel)) {
//                partialBuffers.put(rs.activeLevel, new ArrayList<>());
//            }
//            partialBuffers.get(rs.activeLevel).add(new ArrayList<>(rs.curBuffer));
//        }
//
//        numBuffers += collapsePartialBuffers();
//
//        for (; numBuffers > b - 1; numBuffers--) {
//            collapseForMerging();
//        }
//
//        // Fill up freeBuffers
//        freeBuffers.clear();
//        for (; numBuffers < b - 1; numBuffers++) {
//            freeBuffers.add(new ArrayList<>(s));
//        }
//
//        return this;
//    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractTDigest extends TDigest {
    final Random gen = new Random();
    boolean recordAllData = false;

    public AbstractTDigest() {
    }

    static double weightedAverage(double x1, double w1, double x2, double w2) {
        return x1 <= x2 ? weightedAverageSorted(x1, w1, x2, w2) : weightedAverageSorted(x2, w2, x1, w1);
    }

    private static double weightedAverageSorted(double x1, double w1, double x2, double w2) {
        assert x1 <= x2;

        double x = (x1 * w1 + x2 * w2) / (w1 + w2);
        return Math.max(x1, Math.min(x, x2));
    }

    static double interpolate(double x, double x0, double x1) {
        return (x - x0) / (x1 - x0);
    }

    static void encode(ByteBuffer buf, int n) {
        int k = 0;

        do {
            if (n >= 0 && n <= 127) {
                buf.put((byte)n);
                return;
            }

            byte b = (byte)(128 | 127 & n);
            buf.put(b);
            n >>>= 7;
            ++k;
        } while(k < 6);

        throw new IllegalStateException("Size is implausibly large");
    }

    static int decode(ByteBuffer buf) {
        int v = buf.get();
        int z = 127 & v;

        for(int shift = 7; (v & 128) != 0; shift += 7) {
            if (shift > 28) {
                throw new IllegalStateException("Shift too large in decode");
            }

            v = buf.get();
            z += (v & 127) << shift;
        }

        return z;
    }

    abstract void add(double var1, int var3, Centroid var4);

    static double quantile(double index, double previousIndex, double nextIndex, double previousMean, double nextMean) {
        double delta = nextIndex - previousIndex;
        double previousWeight = (nextIndex - index) / delta;
        double nextWeight = (index - previousIndex) / delta;
        return previousMean * previousWeight + nextMean * nextWeight;
    }

    public TDigest recordAllData() {
        this.recordAllData = true;
        return this;
    }

    public boolean isRecording() {
        return this.recordAllData;
    }

    public void add(double x) {
        this.add(x, 1);
    }

    public void add(TDigest other) {
        long start = System.nanoTime();
        List<Centroid> tmp = new ArrayList();
        Iterator var3 = other.centroids().iterator();

        Centroid centroid;
        while(var3.hasNext()) {
            centroid = (Centroid)var3.next();
            tmp.add(centroid);
        }
        insertionTime = (System.nanoTime() - start) / 1.e6;

        start = System.nanoTime();
        Collections.shuffle(tmp, ThreadLocalRandom.current());
        shuffleTime += (System.nanoTime() - start) / 1.e6;
        var3 = tmp.iterator();

        start = System.nanoTime();
        while(var3.hasNext()) {
            centroid = (Centroid)var3.next();
            this.add(centroid.mean(), centroid.count(), centroid);
        }
        mergeTime += (System.nanoTime() - start) / 1.e6;
    }

    protected Centroid createCentroid(double mean, int id) {
        return new Centroid(mean, id, this.recordAllData);
    }
}

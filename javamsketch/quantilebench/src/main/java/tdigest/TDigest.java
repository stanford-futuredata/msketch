//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

public abstract class TDigest implements Serializable {
    double min = 1.0D / 0.0;
    double max = -1.0D / 0.0;

    public double compressionTime = 0.0;
    public double mergeTime = 0.0;

    public TDigest() {
    }

    public static TDigest createMergingDigest(double compression) {
        return new MergingDigest(compression);
    }

    public static TDigest createAvlTreeDigest(double compression) {
        return new AVLTreeDigest(compression);
    }

    public static TDigest createDigest(double compression) {
        return createMergingDigest(compression);
    }

    public abstract void add(double var1, int var3);

    final void checkValue(double x) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("Cannot add NaN");
        }
    }

    public abstract void add(List<? extends TDigest> var1);

    public abstract void compress();

    public abstract long size();

    public abstract double cdf(double var1);

    public abstract double quantile(double var1);

    public abstract Collection<Centroid> centroids();

    public abstract double compression();

    public abstract int byteSize();

    public abstract int smallByteSize();

    public abstract void asBytes(ByteBuffer var1);

    public abstract void asSmallBytes(ByteBuffer var1);

    public abstract TDigest recordAllData();

    public abstract boolean isRecording();

    public abstract void add(double var1);

    public abstract void add(TDigest var1);

    public abstract int centroidCount();

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    void setMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }
}

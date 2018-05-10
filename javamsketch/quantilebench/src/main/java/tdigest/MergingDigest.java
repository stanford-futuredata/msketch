//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import com.tdunning.math.stats.Sort;

import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MergingDigest extends AbstractTDigest {
    private final double compression;
    private int lastUsedCell;
    private double totalWeight;
    private final double[] weight;
    private final double[] mean;
    private List<List<Double>> data;
    private double unmergedWeight;
    private int tempUsed;
    private final double[] tempWeight;
    private final double[] tempMean;
    private List<List<Double>> tempData;
    private final int[] order;
    private static boolean usePieceWiseApproximation = true;
    private static boolean useWeightLimit = true;

    public MergingDigest(double compression) {
        this(compression, -1);
    }

    public MergingDigest(double compression, int bufferSize) {
        this(compression, bufferSize, -1);
    }

    public MergingDigest(double compression, int bufferSize, int size) {
        this.totalWeight = 0.0D;
        this.data = null;
        this.unmergedWeight = 0.0D;
        this.tempUsed = 0;
        this.tempData = null;
        if (size == -1) {
            size = (int)(2.0D * Math.ceil(compression));
            if (useWeightLimit) {
                size += 10;
            }
        }

        if (bufferSize == -1) {
            bufferSize = (int)(5.0D * Math.ceil(compression));
        }

        this.compression = compression;
        this.weight = new double[size];
        this.mean = new double[size];
        this.tempWeight = new double[bufferSize];
        this.tempMean = new double[bufferSize];
        this.order = new int[bufferSize];
        this.lastUsedCell = 0;
    }

    public TDigest recordAllData() {
        super.recordAllData();
        this.data = new ArrayList();
        this.tempData = new ArrayList();
        return this;
    }

    void add(double x, int w, Centroid base) {
        this.add(x, w, base.data());
    }

    public void add(double x, int w) {
        this.add(x, w, (List)null);
    }

    private void add(double x, int w, List<Double> history) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("Cannot add NaN to t-digest");
        } else {
            if (this.tempUsed >= this.tempWeight.length - this.lastUsedCell - 1) {
                this.mergeNewValues();
            }

            int where = this.tempUsed++;
            this.tempWeight[where] = (double)w;
            this.tempMean[where] = x;
            this.unmergedWeight += (double)w;
            if (this.data != null) {
                if (this.tempData == null) {
                    this.tempData = new ArrayList();
                }

                while(this.tempData.size() <= where) {
                    this.tempData.add(new ArrayList());
                }

                if (history == null) {
                    history = Collections.singletonList(x);
                }

                ((List)this.tempData.get(where)).addAll(history);
            }

        }
    }

    private void add(double[] m, double[] w, int count, List<List<Double>> data) {
        if (m.length != w.length) {
            throw new IllegalArgumentException("Arrays not same length");
        } else {
            if (m.length < count + this.lastUsedCell) {
                double[] m1 = new double[count + this.lastUsedCell];
                System.arraycopy(m, 0, m1, 0, count);
                m = m1;
                double[] w1 = new double[count + this.lastUsedCell];
                System.arraycopy(w, 0, w1, 0, count);
                w = w1;
            }

            double total = 0.0D;

            for(int i = 0; i < count; ++i) {
                total += w[i];
            }

            this.merge(m, w, count, data, (int[])null, total);
        }
    }

    public void add(List<? extends TDigest> others) {
        if (others.size() != 0) {
            int size = this.lastUsedCell;

            TDigest other;
            for(Iterator var3 = others.iterator(); var3.hasNext(); size += other.centroidCount()) {
                other = (TDigest)var3.next();
                other.compress();
            }

            double[] m = new double[size];
            double[] w = new double[size];
            ArrayList data;
            if (this.recordAllData) {
                data = new ArrayList();
            } else {
                data = null;
            }

            int offset = 0;
            Iterator var7 = others.iterator();

            while(true) {
                while(var7.hasNext()) {
                    other = (TDigest)var7.next();
                    if (other instanceof MergingDigest) {
                        MergingDigest md = (MergingDigest)other;
                        System.arraycopy(md.mean, 0, m, offset, md.lastUsedCell);
                        System.arraycopy(md.weight, 0, w, offset, md.lastUsedCell);
                        if (data != null) {
                            Iterator var15 = other.centroids().iterator();

                            while(var15.hasNext()) {
                                Centroid centroid = (Centroid)var15.next();
                                data.add(centroid.data());
                            }
                        }

                        offset += md.lastUsedCell;
                    } else {
                        for(Iterator var9 = other.centroids().iterator(); var9.hasNext(); ++offset) {
                            Centroid centroid = (Centroid)var9.next();
                            m[offset] = centroid.mean();
                            w[offset] = (double)centroid.count();
                            if (this.recordAllData) {
                                assert data != null;

                                data.add(centroid.data());
                            }
                        }
                    }
                }

                this.add(m, w, size, data);
                return;
            }
        }
    }

    private void mergeNewValues() {
        if (this.unmergedWeight > 0.0D) {
            this.merge(this.tempMean, this.tempWeight, this.tempUsed, this.tempData, this.order, this.unmergedWeight);
            this.tempUsed = 0;
            this.unmergedWeight = 0.0D;
            if (this.data != null) {
                this.tempData = new ArrayList();
            }
        }

    }

    private void merge(double[] incomingMean, double[] incomingWeight, int incomingCount, List<List<Double>> incomingData, int[] incomingOrder, double unmergedWeight) {
        System.arraycopy(this.mean, 0, incomingMean, incomingCount, this.lastUsedCell);
        System.arraycopy(this.weight, 0, incomingWeight, incomingCount, this.lastUsedCell);
        incomingCount += this.lastUsedCell;
        if (incomingData != null) {
            for(int i = 0; i < this.lastUsedCell; ++i) {
                assert this.data != null;

                incomingData.add(this.data.get(i));
            }

            this.data = new ArrayList();
        }

        if (incomingOrder == null) {
            incomingOrder = new int[incomingCount];
        }

        Sort.sort(incomingOrder, incomingMean, incomingCount);
        this.totalWeight += unmergedWeight;
        double normalizer = this.compression / (3.141592653589793D * this.totalWeight);

        assert incomingCount > 0;

        this.lastUsedCell = 0;
        this.mean[this.lastUsedCell] = incomingMean[incomingOrder[0]];
        this.weight[this.lastUsedCell] = incomingWeight[incomingOrder[0]];
        double wSoFar = 0.0D;
        if (this.data != null) {
            assert incomingData != null;

            this.data.add(incomingData.get(incomingOrder[0]));
        }

        double k1 = 0.0D;
        double wLimit = this.totalWeight * this.integratedQ(k1 + 1.0D);

        for(int i = 1; i < incomingCount; ++i) {
            int ix = incomingOrder[i];
            double proposedWeight = this.weight[this.lastUsedCell] + incomingWeight[ix];
            double projectedW = wSoFar + proposedWeight;
            boolean addThis;
            if (!useWeightLimit) {
                addThis = projectedW <= wLimit;
            } else {
                double z = proposedWeight * normalizer;
                double q0 = wSoFar / this.totalWeight;
                double q2 = (wSoFar + proposedWeight) / this.totalWeight;
                addThis = z * z <= q0 * (1.0D - q0) && z * z <= q2 * (1.0D - q2);
            }

            if (!addThis) {
                wSoFar += this.weight[this.lastUsedCell];
                if (!useWeightLimit) {
                    k1 = this.integratedLocation(wSoFar / this.totalWeight);
                    wLimit = this.totalWeight * this.integratedQ(k1 + 1.0D);
                }

                ++this.lastUsedCell;
                this.mean[this.lastUsedCell] = incomingMean[ix];
                this.weight[this.lastUsedCell] = incomingWeight[ix];
                incomingWeight[ix] = 0.0D;
                if (this.data != null) {
                    assert incomingData != null;

                    assert this.data.size() == this.lastUsedCell;

                    this.data.add(incomingData.get(ix));
                }
            } else {
                this.weight[this.lastUsedCell] += incomingWeight[ix];
                this.mean[this.lastUsedCell] += (incomingMean[ix] - this.mean[this.lastUsedCell]) * incomingWeight[ix] / this.weight[this.lastUsedCell];
                incomingWeight[ix] = 0.0D;
                if (this.data != null) {
                    while(this.data.size() <= this.lastUsedCell) {
                        this.data.add(new ArrayList());
                    }

                    assert incomingData != null;

                    assert this.data.get(this.lastUsedCell) != incomingData.get(ix);

                    ((List)this.data.get(this.lastUsedCell)).addAll((Collection)incomingData.get(ix));
                }
            }
        }

        ++this.lastUsedCell;
        double sum = 0.0D;

        for(int i = 0; i < this.lastUsedCell; ++i) {
            sum += this.weight[i];
        }

        assert sum == this.totalWeight;

        if (this.totalWeight > 0.0D) {
            this.min = Math.min(this.min, this.mean[0]);
            this.max = Math.max(this.max, this.mean[this.lastUsedCell - 1]);
        }

    }

    int checkWeights() {
        return this.checkWeights(this.weight, this.totalWeight, this.lastUsedCell);
    }

    private int checkWeights(double[] w, double total, int last) {
        int badCount = 0;
        int n = last;
        if (w[last] > 0.0D) {
            n = last + 1;
        }

        double k1 = 0.0D;
        double q = 0.0D;
        double left = 0.0D;
        String header = "\n";

        for(int i = 0; i < n; ++i) {
            double dq = w[i] / total;
            double k2 = this.integratedLocation(q + dq);
            q += dq / 2.0D;
            if (k2 - k1 > 1.0D && w[i] != 1.0D) {
                System.out.printf("%sOversize centroid at %d, k0=%.2f, k1=%.2f, dk=%.2f, w=%.2f, q=%.4f, dq=%.4f, left=%.1f, current=%.2f maxw=%.2f\n", header, i, k1, k2, k2 - k1, w[i], q, dq, left, w[i], 3.141592653589793D * total / this.compression * Math.sqrt(q * (1.0D - q)));
                header = "";
                ++badCount;
            }

            if (k2 - k1 > 4.0D && w[i] != 1.0D) {
                throw new IllegalStateException(String.format("Egregiously oversized centroid at %d, k0=%.2f, k1=%.2f, dk=%.2f, w=%.2f, q=%.4f, dq=%.4f, left=%.1f, current=%.2f, maxw=%.2f\n", i, k1, k2, k2 - k1, w[i], q, dq, left, w[i], 3.141592653589793D * total / this.compression * Math.sqrt(q * (1.0D - q))));
            }

            q += dq / 2.0D;
            left += w[i];
            k1 = k2;
        }

        return badCount;
    }

    private double integratedLocation(double q) {
        return this.compression * (asinApproximation(2.0D * q - 1.0D) + 1.5707963267948966D) / 3.141592653589793D;
    }

    private double integratedQ(double k) {
        return (Math.sin(Math.min(k, this.compression) * 3.141592653589793D / this.compression - 1.5707963267948966D) + 1.0D) / 2.0D;
    }

    static double asinApproximation(double x) {
        if (usePieceWiseApproximation) {
            if (x < 0.0D) {
                return -asinApproximation(-x);
            } else {
                double c0High = 0.1D;
                double c1High = 0.55D;
                double c2Low = 0.5D;
                double c2High = 0.8D;
                double c3Low = 0.75D;
                double c3High = 0.9D;
                double c4Low = 0.87D;
                if (x > c3High) {
                    return Math.asin(x);
                } else {
                    double[] m0 = new double[]{0.2955302411D, 1.2221903614D, 0.1488583743D, 0.2422015816D, -0.3688700895D, 0.0733398445D};
                    double[] m1 = new double[]{-0.043099192D, 0.959403575D, -0.0362312299D, 0.1204623351D, 0.045702962D, -0.0026025285D};
                    double[] m2 = new double[]{-0.034873933724D, 1.054796752703D, -0.194127063385D, 0.283963735636D, 0.023800124916D, -8.72727381E-4D};
                    double[] m3 = new double[]{-0.37588391875D, 2.61991859025D, -2.48835406886D, 1.48605387425D, 0.00857627492D, -1.5802871E-4D};
                    double[] vars = new double[]{1.0D, x, x * x, x * x * x, 1.0D / (1.0D - x), 1.0D / (1.0D - x) / (1.0D - x)};
                    double x0 = bound((c0High - x) / c0High);
                    double x1 = bound((c1High - x) / (c1High - c2Low));
                    double x2 = bound((c2High - x) / (c2High - c3Low));
                    double x3 = bound((c3High - x) / (c3High - c4Low));
                    double mix1 = (1.0D - x0) * x1;
                    double mix2 = (1.0D - x1) * x2;
                    double mix3 = (1.0D - x2) * x3;
                    double mix4 = 1.0D - x3;
                    double r = 0.0D;
                    if (x0 > 0.0D) {
                        r += x0 * eval(m0, vars);
                    }

                    if (mix1 > 0.0D) {
                        r += mix1 * eval(m1, vars);
                    }

                    if (mix2 > 0.0D) {
                        r += mix2 * eval(m2, vars);
                    }

                    if (mix3 > 0.0D) {
                        r += mix3 * eval(m3, vars);
                    }

                    if (mix4 > 0.0D) {
                        r += mix4 * Math.asin(x);
                    }

                    return r;
                }
            }
        } else {
            return Math.asin(x);
        }
    }

    private static double eval(double[] model, double[] vars) {
        double r = 0.0D;

        for(int i = 0; i < model.length; ++i) {
            r += model[i] * vars[i];
        }

        return r;
    }

    private static double bound(double v) {
        if (v <= 0.0D) {
            return 0.0D;
        } else {
            return v >= 1.0D ? 1.0D : v;
        }
    }

    public void compress() {
        this.mergeNewValues();
    }

    public long size() {
        return (long)(this.totalWeight + this.unmergedWeight);
    }

    public double cdf(double x) {
        this.mergeNewValues();
        if (this.lastUsedCell == 0) {
            return 0.0D / 0.0;
        } else if (this.lastUsedCell == 1) {
            double width = this.max - this.min;
            if (x < this.min) {
                return 0.0D;
            } else if (x > this.max) {
                return 1.0D;
            } else {
                return x - this.min <= width ? 0.5D : (x - this.min) / (this.max - this.min);
            }
        } else {
            int n = this.lastUsedCell;
            if (x <= this.min) {
                return 0.0D;
            } else if (x >= this.max) {
                return 1.0D;
            } else if (x <= this.mean[0]) {
                return this.mean[0] - this.min > 0.0D ? (x - this.min) / (this.mean[0] - this.min) * this.weight[0] / this.totalWeight / 2.0D : 0.0D;
            } else {
                assert x > this.mean[0];

                if (x >= this.mean[n - 1]) {
                    return this.max - this.mean[n - 1] > 0.0D ? 1.0D - (this.max - x) / (this.max - this.mean[n - 1]) * this.weight[n - 1] / this.totalWeight / 2.0D : 1.0D;
                } else {
                    assert x < this.mean[n - 1];

                    double weightSoFar = this.weight[0] / 2.0D;

                    for(int it = 0; it < n; ++it) {
                        if (this.mean[it] == x) {
                            while(it < n && this.mean[it + 1] == x) {
                                weightSoFar += this.weight[it] + this.weight[it + 1];
                                ++it;
                            }

                            return (weightSoFar + weightSoFar) / 2.0D / this.totalWeight;
                        }

                        if (this.mean[it] <= x && this.mean[it + 1] > x) {
                            double dw;
                            if (this.mean[it + 1] - this.mean[it] > 0.0D) {
                                dw = (this.weight[it] + this.weight[it + 1]) / 2.0D;
                                return (weightSoFar + dw * (x - this.mean[it]) / (this.mean[it + 1] - this.mean[it])) / this.totalWeight;
                            }

                            dw = (this.weight[it] + this.weight[it + 1]) / 2.0D;
                            return weightSoFar + dw / this.totalWeight;
                        }

                        weightSoFar += (this.weight[it] + this.weight[it + 1]) / 2.0D;
                    }

                    throw new IllegalStateException("Can't happen ... loop fell through");
                }
            }
        }
    }

    public double quantile(double q) {
        if (q >= 0.0D && q <= 1.0D) {
            this.mergeNewValues();
            if (this.lastUsedCell == 0 && this.weight[this.lastUsedCell] == 0.0D) {
                return 0.0D / 0.0;
            } else if (this.lastUsedCell == 0) {
                return this.mean[0];
            } else {
                int n = this.lastUsedCell;
                double index = q * this.totalWeight;
                if (index < this.weight[0] / 2.0D) {
                    assert this.weight[0] > 0.0D;

                    return this.min + 2.0D * index / this.weight[0] * (this.mean[0] - this.min);
                } else {
                    double weightSoFar = this.weight[0] / 2.0D;

                    for(int i = 0; i < n - 1; ++i) {
                        double dw = (this.weight[i] + this.weight[i + 1]) / 2.0D;
                        if (weightSoFar + dw > index) {
                            double z1 = index - weightSoFar;
                            double z2 = weightSoFar + dw - index;
                            return weightedAverage(this.mean[i], z2, this.mean[i + 1], z1);
                        }

                        weightSoFar += dw;
                    }

                    assert index <= this.totalWeight;

                    assert index >= this.totalWeight - this.weight[n - 1] / 2.0D;

                    double z1 = index - this.totalWeight - this.weight[n - 1] / 2.0D;
                    double z2 = this.weight[n - 1] / 2.0D - z1;
                    return weightedAverage(this.mean[n - 1], z1, this.max, z2);
                }
            }
        } else {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }
    }

    public int centroidCount() {
        return this.lastUsedCell;
    }

    public Collection<Centroid> centroids() {
        this.compress();
        return new AbstractCollection<Centroid>() {
            public Iterator<Centroid> iterator() {
                return new Iterator<Centroid>() {
                    int i = 0;

                    public boolean hasNext() {
                        return this.i < MergingDigest.this.lastUsedCell;
                    }

                    public Centroid next() {
                        Centroid rc = new Centroid(MergingDigest.this.mean[this.i], (int)MergingDigest.this.weight[this.i], MergingDigest.this.data != null ? (List)MergingDigest.this.data.get(this.i) : null);
                        ++this.i;
                        return rc;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Default operation");
                    }
                };
            }

            public int size() {
                return MergingDigest.this.lastUsedCell;
            }
        };
    }

    public double compression() {
        return this.compression;
    }

    public int byteSize() {
        this.compress();
        return this.lastUsedCell * 16 + 32;
    }

    public int smallByteSize() {
        this.compress();
        return this.lastUsedCell * 8 + 30;
    }

    public void asBytes(ByteBuffer buf) {
        this.compress();
        buf.putInt(MergingDigest.Encoding.VERBOSE_ENCODING.code);
        buf.putDouble(this.min);
        buf.putDouble(this.max);
        buf.putDouble(this.compression);
        buf.putInt(this.lastUsedCell);

        for(int i = 0; i < this.lastUsedCell; ++i) {
            buf.putDouble(this.weight[i]);
            buf.putDouble(this.mean[i]);
        }

    }

    public void asSmallBytes(ByteBuffer buf) {
        this.compress();
        buf.putInt(MergingDigest.Encoding.SMALL_ENCODING.code);
        buf.putDouble(this.min);
        buf.putDouble(this.max);
        buf.putFloat((float)this.compression);
        buf.putShort((short)this.mean.length);
        buf.putShort((short)this.tempMean.length);
        buf.putShort((short)this.lastUsedCell);

        for(int i = 0; i < this.lastUsedCell; ++i) {
            buf.putFloat((float)this.weight[i]);
            buf.putFloat((float)this.mean[i]);
        }

    }

    public static MergingDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        double min;
        double max;
        double compression;
        if (encoding == MergingDigest.Encoding.VERBOSE_ENCODING.code) {
            min = buf.getDouble();
            max = buf.getDouble();
            compression = buf.getDouble();
            int n = buf.getInt();
            MergingDigest r = new MergingDigest(compression);
            r.setMinMax(min, max);
            r.lastUsedCell = n;

            for(int i = 0; i < n; ++i) {
                r.weight[i] = buf.getDouble();
                r.mean[i] = buf.getDouble();
                r.totalWeight += r.weight[i];
            }

            return r;
        } else if (encoding != MergingDigest.Encoding.SMALL_ENCODING.code) {
            throw new IllegalStateException("Invalid format for serialized histogram");
        } else {
            min = buf.getDouble();
            max = buf.getDouble();
            compression = (double)buf.getFloat();
            int n = buf.getShort();
            int bufferSize = buf.getShort();
            MergingDigest r = new MergingDigest(compression, bufferSize, n);
            r.setMinMax(min, max);
            r.lastUsedCell = buf.getShort();

            for(int i = 0; i < r.lastUsedCell; ++i) {
                r.weight[i] = (double)buf.getFloat();
                r.mean[i] = (double)buf.getFloat();
                r.totalWeight += r.weight[i];
            }

            return r;
        }
    }

    public static enum Encoding {
        VERBOSE_ENCODING(1),
        SMALL_ENCODING(2);

        private final int code;

        private Encoding(int code) {
            this.code = code;
        }
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AVLTreeDigest extends AbstractTDigest {
    private final double compression;
    private AVLGroupTree summary;
    private long count = 0L;
    private static final int VERBOSE_ENCODING = 1;
    private static final int SMALL_ENCODING = 2;

    public AVLTreeDigest(double compression) {
        this.compression = compression;
        this.summary = new AVLGroupTree(false);
    }

    public TDigest recordAllData() {
        if (this.summary.size() != 0) {
            throw new IllegalStateException("Can only ask to record added data on an empty summary");
        } else {
            this.summary = new AVLGroupTree(true);
            return super.recordAllData();
        }
    }

    public int centroidCount() {
        return this.summary.size();
    }

    void add(double x, int w, Centroid base) {
        if (x == base.mean() && w == base.count()) {
            this.add(x, w, base.data());
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void add(double x, int w) {
        this.add(x, w, (List)null);
    }

    public void add(List<? extends TDigest> others) {
        Iterator var2 = others.iterator();

        while(var2.hasNext()) {
            TDigest other = (TDigest)var2.next();
            this.setMinMax(Math.min(this.min, other.getMin()), Math.max(this.max, other.getMax()));
            Iterator var4 = other.centroids().iterator();

            while(var4.hasNext()) {
                Centroid centroid = (Centroid)var4.next();
                this.add(centroid.mean(), centroid.count(), this.recordAllData ? centroid.data() : null);
            }
        }

    }

    public void add(double x, int w, List<Double> data) {
        this.checkValue(x);
        if (x < this.min) {
            this.min = x;
        }

        if (x > this.max) {
            this.max = x;
        }

        int start = this.summary.floor(x);
        if (start == 0) {
            start = this.summary.first();
        }

        if (start == 0) {
            assert this.summary.size() == 0;

            this.summary.add(x, w, data);
            this.count = (long)w;
        } else {
            double minDistance = 1.7976931348623157E308D;
            int lastNeighbor = 0;

            int closest;
            for(closest = start; closest != 0; closest = this.summary.next(closest)) {
                double z = Math.abs(this.summary.mean(closest) - x);
                if (z < minDistance) {
                    start = closest;
                    minDistance = z;
                } else if (z > minDistance) {
                    lastNeighbor = closest;
                    break;
                }
            }

            closest = 0;
            long sum = this.summary.headSum(start);
            double n = 0.0D;

            for(int neighbor = start; neighbor != lastNeighbor; neighbor = this.summary.next(neighbor)) {
                assert minDistance == Math.abs(this.summary.mean(neighbor) - x);

                double q = this.count == 1L ? 0.5D : ((double)sum + (double)(this.summary.count(neighbor) - 1) / 2.0D) / (double)(this.count - 1L);
                double k = (double)(4L * this.count) * q * (1.0D - q) / this.compression;
                if ((double)(this.summary.count(neighbor) + w) <= k) {
                    ++n;
                    if (ThreadLocalRandom.current().nextDouble() < 1.0D / n) {
                        closest = neighbor;
                    }
                }

                sum += (long)this.summary.count(neighbor);
            }

            if (closest == 0) {
                this.summary.add(x, w, data);
            } else {
                double centroid = this.summary.mean(closest);
                int count = this.summary.count(closest);
                List<Double> d = this.summary.data(closest);
                if (d != null) {
                    if (w == 1) {
                        d.add(x);
                    } else {
                        d.addAll(data);
                    }
                }

                centroid = weightedAverage(centroid, (double)count, x, (double)w);
                count += w;
                this.summary.update(closest, centroid, count, d);
            }

            this.count += (long)w;
            if ((double)this.summary.size() > 20.0D * this.compression) {
                this.compress();
            }
        }

    }

    public void compress() {
        long start = System.nanoTime();
        if (this.summary.size() > 1) {
            AVLGroupTree centroids = this.summary;
            this.summary = new AVLGroupTree(this.recordAllData);
            int[] nodes = new int[centroids.size()];
            nodes[0] = centroids.first();

            int i;
            for(i = 1; i < nodes.length; ++i) {
                nodes[i] = centroids.next(nodes[i - 1]);

                assert nodes[i] != 0;
            }

            assert centroids.next(nodes[nodes.length - 1]) == 0;

            int other;
            int tmp;
            for(i = centroids.size() - 1; i > 0; --i) {
                other = ThreadLocalRandom.current().nextInt(i + 1);
                tmp = nodes[other];
                nodes[other] = nodes[i];
                nodes[i] = tmp;
            }

            int[] var7 = nodes;
            other = nodes.length;

            for(tmp = 0; tmp < other; ++tmp) {
                int node = var7[tmp];
                this.add(centroids.mean(node), centroids.count(node), centroids.data(node));
            }

        }
        this.compressionTime += (System.nanoTime() - start) / 1.e6;
    }

    public long size() {
        return this.count;
    }

    public double cdf(double x) {
        AVLGroupTree values = this.summary;
        if (values.size() == 0) {
            return 0.0D / 0.0;
        } else if (values.size() == 1) {
            return x < values.mean(values.first()) ? 0.0D : 1.0D;
        } else {
            double r = 0.0D;
            Iterator<Centroid> it = values.iterator();
            Centroid a = (Centroid)it.next();
            Centroid b = (Centroid)it.next();
            double left = (b.mean() - a.mean()) / 2.0D;

            double right;
            for(right = left; it.hasNext(); right = (b.mean() - a.mean()) / 2.0D) {
                if (x < a.mean() + right) {
                    double value = (r + (double)a.count() * interpolate(x, a.mean() - left, a.mean() + right)) / (double)this.count;
                    return value > 0.0D ? value : 0.0D;
                }

                r += (double)a.count();
                a = b;
                left = right;
                b = (Centroid)it.next();
            }

            if (x < a.mean() + right) {
                return (r + (double)a.count() * interpolate(x, a.mean() - left, a.mean() + right)) / (double)this.count;
            } else {
                return 1.0D;
            }
        }
    }

    public double quantile(double q) {
        if (q >= 0.0D && q <= 1.0D) {
            AVLGroupTree values = this.summary;
            if (values.size() == 0) {
                return 0.0D / 0.0;
            } else if (values.size() == 1) {
                return ((Centroid)values.iterator().next()).mean();
            } else {
                double index = q * (double)this.count;
                int currentNode = values.first();
                int currentWeight = values.count(currentNode);
                double weightSoFar = (double)currentWeight / 2.0D;
                if (index < weightSoFar) {
                    return (this.min * index + values.mean(currentNode) * (weightSoFar - index)) / weightSoFar;
                } else {
                    for(int i = 0; i < values.size() - 1; ++i) {
                        int nextNode = values.next(currentNode);
                        int nextWeight = values.count(nextNode);
                        double dw = (double)(currentWeight + nextWeight) / 2.0D;
                        if (weightSoFar + dw > index) {
                            double z1 = index - weightSoFar;
                            double z2 = weightSoFar + dw - index;
                            return weightedAverage(values.mean(currentNode), z2, values.mean(nextNode), z1);
                        }

                        weightSoFar += dw;
                        currentNode = nextNode;
                        currentWeight = nextWeight;
                    }

                    double z1 = index - weightSoFar;
                    double z2 = (double)currentWeight / 2.0D - z1;
                    return weightedAverage(values.mean(currentNode), z2, this.max, z1);
                }
            }
        } else {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }
    }

    public Collection<Centroid> centroids() {
        return Collections.unmodifiableCollection(this.summary);
    }

    public double compression() {
        return this.compression;
    }

    public int byteSize() {
        return 32 + this.summary.size() * 12;
    }

    public int smallByteSize() {
        int bound = this.byteSize();
        ByteBuffer buf = ByteBuffer.allocate(bound);
        this.asSmallBytes(buf);
        return buf.position();
    }

    public void asBytes(ByteBuffer buf) {
        buf.putInt(1);
        buf.putDouble(this.min);
        buf.putDouble(this.max);
        buf.putDouble((double)((float)this.compression()));
        buf.putInt(this.summary.size());
        Iterator var2 = this.summary.iterator();

        Centroid centroid;
        while(var2.hasNext()) {
            centroid = (Centroid)var2.next();
            buf.putDouble(centroid.mean());
        }

        var2 = this.summary.iterator();

        while(var2.hasNext()) {
            centroid = (Centroid)var2.next();
            buf.putInt(centroid.count());
        }

    }

    public void asSmallBytes(ByteBuffer buf) {
        buf.putInt(2);
        buf.putDouble(this.min);
        buf.putDouble(this.max);
        buf.putDouble(this.compression());
        buf.putInt(this.summary.size());
        double x = 0.0D;
        Iterator var4 = this.summary.iterator();

        Centroid centroid;
        while(var4.hasNext()) {
            centroid = (Centroid)var4.next();
            double delta = centroid.mean() - x;
            x = centroid.mean();
            buf.putFloat((float)delta);
        }

        var4 = this.summary.iterator();

        while(var4.hasNext()) {
            centroid = (Centroid)var4.next();
            int n = centroid.count();
            encode(buf, n);
        }

    }

    public static AVLTreeDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        double min;
        double max;
        double compression;
        AVLTreeDigest r;
        int n;
        double[] means;
        if (encoding == 1) {
            min = buf.getDouble();
            max = buf.getDouble();
            compression = buf.getDouble();
            r = new AVLTreeDigest(compression);
            r.setMinMax(min, max);
            n = buf.getInt();
            means = new double[n];

            int i;
            for(i = 0; i < n; ++i) {
                means[i] = buf.getDouble();
            }

            for(i = 0; i < n; ++i) {
                r.add(means[i], buf.getInt());
            }

            return r;
        } else if (encoding != 2) {
            throw new IllegalStateException("Invalid format for serialized histogram");
        } else {
            min = buf.getDouble();
            max = buf.getDouble();
            compression = buf.getDouble();
            r = new AVLTreeDigest(compression);
            r.setMinMax(min, max);
            n = buf.getInt();
            means = new double[n];
            double x = 0.0D;

            int i;
            for(i = 0; i < n; ++i) {
                double delta = (double)buf.getFloat();
                x += delta;
                means[i] = x;
            }

            for(i = 0; i < n; ++i) {
                int z = decode(buf);
                r.add(means[i], z);
            }

            return r;
        }
    }
}

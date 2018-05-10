//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Centroid implements Comparable<Centroid>, Serializable {
    private static final AtomicInteger uniqueCount = new AtomicInteger(1);
    private double centroid;
    private int count;
    private transient int id;
    private List<Double> actualData;

    private Centroid(boolean record) {
        this.centroid = 0.0D;
        this.count = 0;
        this.actualData = null;
        this.id = uniqueCount.getAndIncrement();
        if (record) {
            this.actualData = new ArrayList();
        }

    }

    public Centroid(double x) {
        this(false);
        this.start(x, 1, uniqueCount.getAndIncrement());
    }

    public Centroid(double x, int w) {
        this(false);
        this.start(x, w, uniqueCount.getAndIncrement());
    }

    public Centroid(double x, int w, int id) {
        this(false);
        this.start(x, w, id);
    }

    public Centroid(double x, int id, boolean record) {
        this(record);
        this.start(x, 1, id);
    }

    Centroid(double x, int w, List<Double> data) {
        this(x, w);
        this.actualData = data;
    }

    private void start(double x, int w, int id) {
        this.id = id;
        this.add(x, w);
    }

    public void add(double x, int w) {
        if (this.actualData != null) {
            this.actualData.add(x);
        }

        this.count += w;
        this.centroid += (double)w * (x - this.centroid) / (double)this.count;
    }

    public double mean() {
        return this.centroid;
    }

    public int count() {
        return this.count;
    }

    public int id() {
        return this.id;
    }

    public String toString() {
        return "Centroid{centroid=" + this.centroid + ", count=" + this.count + '}';
    }

    public int hashCode() {
        return this.id;
    }

    public int compareTo(Centroid o) {
        int r = Double.compare(this.centroid, o.centroid);
        if (r == 0) {
            r = this.id - o.id;
        }

        return r;
    }

    public List<Double> data() {
        return this.actualData;
    }

    public void insertData(double x) {
        if (this.actualData == null) {
            this.actualData = new ArrayList();
        }

        this.actualData.add(x);
    }

    public static Centroid createWeighted(double x, int w, Iterable<? extends Double> data) {
        Centroid r = new Centroid(data != null);
        r.add(x, w, data);
        return r;
    }

    public void add(double x, int w, Iterable<? extends Double> data) {
        if (this.actualData != null) {
            if (data != null) {
                Iterator var5 = data.iterator();

                while(var5.hasNext()) {
                    Double old = (Double)var5.next();
                    this.actualData.add(old);
                }
            } else {
                this.actualData.add(x);
            }
        }

        this.centroid = AbstractTDigest.weightedAverage(this.centroid, (double)this.count, x, (double)w);
        this.count += w;
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.id = uniqueCount.getAndIncrement();
    }
}

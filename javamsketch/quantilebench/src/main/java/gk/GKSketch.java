package gk;

import java.util.ArrayList;
import java.util.Arrays;

public class GKSketch {
    private double eps;
    private int nSize;
    private ArrayList<Tuple> tuples;

    public class Tuple implements Comparable<Tuple>{
        private double v;
        int g, del;
        public Tuple(double v, int g, int del) {
            this.v = v;
            this.g = g;
            this.del = del;
        }

        public Tuple copy() {
            return new Tuple(v, g, del);
        }

        @Override
        public int compareTo(Tuple o) {
            int ret = Double.compare(v, o.v);
            return ret;
        }

        @Override
        public String toString() {
            return String.format("%3g[%d,%d]", v, g, del);
        }
    }
    public GKSketch(double eps) {
        this.eps = eps;
        this.tuples = new ArrayList<>((int)(1.0/eps));
    }
    public ArrayList<Tuple> getTuples() {
        return tuples;
    }


    public void mergeInternal(int numAdded, ArrayList<Tuple> addTuples) {
        int oldNSize = nSize;
        nSize += numAdded;
        int i1 = 0;
        int n1 = tuples.size();
        int i2 = 0;
        int n2 = addTuples.size();

        ArrayList<Tuple> newTuples = new ArrayList<>(2*n2);
        int i = 0;
        Tuple tLast = null, tNext;
        while (i1 < n1 || i2 < n2) {
            if (i > 0) {
                tLast = newTuples.get(i-1);
            }
            if (i1 == n1) {
                Tuple t2 = addTuples.get(i2);
                i2++;
                tNext = t2.copy();
            } else if (i2 == n2) {
                Tuple t1 = tuples.get(i1);
                i1++;
                tNext = t1.copy();
            } else {
                Tuple t1 = tuples.get(i1);
                Tuple t2 = addTuples.get(i2);
                if (t1.v <= t2.v) {
                    tNext = t1.copy();
                    tNext.del += (t2.g + t2.del - 1);
                    i1++;
                } else {
                    tNext = t2.copy();
                    tNext.del += (t1.g + t1.del - 1);
                    i2++;
                }
            }

            // Collapse tuples greedily as we merge the two lists
            if ((i > 1) && tLast.g + tNext.g + tNext.del <= 2.0*eps*nSize) {
                tNext.g += tLast.g;
                newTuples.set(i-1, tNext);
            } else {
                newTuples.add(tNext);
                i++;
            }
        }
        tuples = newTuples;
    }

    public void merge(GKSketch other) {
        mergeInternal(other.nSize, other.tuples);
    }

    public void add(double[] xValsRaw) {
        double[] xVals = xValsRaw.clone();
        Arrays.sort(xVals);

        ArrayList<Tuple> addTuples = new ArrayList<>();
        for (double x : xVals) {
            Tuple t = new Tuple(x, 1, 0);
            addTuples.add(t);
        }

        mergeInternal(xVals.length, addTuples);
    }

    public double quantile(double p) {
        if (tuples.isEmpty()) {
            return 0.0;
        }

        int minTargetRank = (int)((p - eps) * nSize);
        int maxTargetRank = (int)((p + eps) * nSize);

        int n = tuples.size();
        int minRank=0, maxRank=0;
        for (int i = 0; i < n; i++) {
            Tuple curTuple = tuples.get(i);
            minRank += curTuple.g;
            maxRank = minRank + curTuple.del;
            if ((minRank >= minTargetRank) && (maxRank <= maxTargetRank)) {
                return curTuple.v;
            }
        }
        return tuples.get(n-1).v;
    }

    @Override
    public String toString() {
        return tuples.toString();
    }
}

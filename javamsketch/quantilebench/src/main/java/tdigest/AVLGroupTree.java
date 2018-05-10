//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

final class AVLGroupTree extends AbstractCollection<Centroid> implements Serializable {
    private double centroid;
    private int count;
    private List<Double> data;
    private double[] centroids;
    private int[] counts;
    private List<Double>[] datas;
    private int[] aggregatedCounts;
    private final IntAVLTree tree;

    AVLGroupTree() {
        this(false);
    }

    AVLGroupTree(boolean record) {
        this.tree = new IntAVLTree() {
            protected void resize(int newCapacity) {
                super.resize(newCapacity);
                AVLGroupTree.this.centroids = Arrays.copyOf(AVLGroupTree.this.centroids, newCapacity);
                AVLGroupTree.this.counts = Arrays.copyOf(AVLGroupTree.this.counts, newCapacity);
                AVLGroupTree.this.aggregatedCounts = Arrays.copyOf(AVLGroupTree.this.aggregatedCounts, newCapacity);
                if (AVLGroupTree.this.datas != null) {
                    AVLGroupTree.this.datas = (List[])Arrays.copyOf(AVLGroupTree.this.datas, newCapacity);
                }

            }

            protected void merge(int node) {
                throw new UnsupportedOperationException();
            }

            protected void copy(int node) {
                AVLGroupTree.this.centroids[node] = AVLGroupTree.this.centroid;
                AVLGroupTree.this.counts[node] = AVLGroupTree.this.count;
                if (AVLGroupTree.this.datas != null) {
                    if (AVLGroupTree.this.data == null) {
                        if (AVLGroupTree.this.count != 1) {
                            throw new IllegalStateException();
                        }

                        AVLGroupTree.this.data = new ArrayList();
                        AVLGroupTree.this.data.add(AVLGroupTree.this.centroid);
                    }

                    AVLGroupTree.this.datas[node] = AVLGroupTree.this.data;
                }

            }

            protected int compare(int node) {
                return AVLGroupTree.this.centroid < AVLGroupTree.this.centroids[node] ? -1 : 1;
            }

            protected void fixAggregates(int node) {
                super.fixAggregates(node);
                AVLGroupTree.this.aggregatedCounts[node] = AVLGroupTree.this.counts[node] + AVLGroupTree.this.aggregatedCounts[this.left(node)] + AVLGroupTree.this.aggregatedCounts[this.right(node)];
            }
        };
        this.centroids = new double[this.tree.capacity()];
        this.counts = new int[this.tree.capacity()];
        this.aggregatedCounts = new int[this.tree.capacity()];
        if (record) {
            List<Double>[] datas = new List[this.tree.capacity()];
            this.datas = datas;
        }

    }

    public int size() {
        return this.tree.size();
    }

    public int prev(int node) {
        return this.tree.prev(node);
    }

    public int next(int node) {
        return this.tree.next(node);
    }

    public double mean(int node) {
        return this.centroids[node];
    }

    public int count(int node) {
        return this.counts[node];
    }

    public List<Double> data(int node) {
        return this.datas == null ? null : this.datas[node];
    }

    public void add(double centroid, int count, List<Double> data) {
        this.centroid = centroid;
        this.count = count;
        this.data = data;
        this.tree.add();
    }

    public boolean add(Centroid centroid) {
        this.add(centroid.mean(), centroid.count(), centroid.data());
        return true;
    }

    public void update(int node, double centroid, int count, List<Double> data) {
        this.centroid = centroid;
        this.count = count;
        this.data = data;
        this.tree.update(node);
    }

    public int floor(double centroid) {
        int floor = 0;
        int node = this.tree.root();

        while(node != 0) {
            int cmp = Double.compare(centroid, this.mean(node));
            if (cmp <= 0) {
                node = this.tree.left(node);
            } else {
                floor = node;
                node = this.tree.right(node);
            }
        }

        return floor;
    }

    public int floorSum(long sum) {
        int floor = 0;
        int node = this.tree.root();

        while(node != 0) {
            int left = this.tree.left(node);
            long leftCount = (long)this.aggregatedCounts[left];
            if (leftCount <= sum) {
                floor = node;
                sum -= leftCount + (long)this.count(node);
                node = this.tree.right(node);
            } else {
                node = this.tree.left(node);
            }
        }

        return floor;
    }

    public int first() {
        return this.tree.first(this.tree.root());
    }

    public long headSum(int node) {
        int left = this.tree.left(node);
        long sum = (long)this.aggregatedCounts[left];
        int n = node;

        for(int p = this.tree.parent(node); p != 0; p = this.tree.parent(p)) {
            if (n == this.tree.right(p)) {
                int leftP = this.tree.left(p);
                sum += (long)(this.counts[p] + this.aggregatedCounts[leftP]);
            }

            n = p;
        }

        return sum;
    }

    public Iterator<Centroid> iterator() {
        return this.iterator(this.first());
    }

    private Iterator<Centroid> iterator(final int startNode) {
        return new Iterator<Centroid>() {
            int nextNode = startNode;

            public boolean hasNext() {
                return this.nextNode != 0;
            }

            public Centroid next() {
                Centroid next = new Centroid(AVLGroupTree.this.mean(this.nextNode), AVLGroupTree.this.count(this.nextNode));
                List<Double> data = AVLGroupTree.this.data(this.nextNode);
                if (data != null) {
                    Iterator var3 = data.iterator();

                    while(var3.hasNext()) {
                        Double x = (Double)var3.next();
                        next.insertData(x.doubleValue());
                    }
                }

                this.nextNode = AVLGroupTree.this.tree.next(this.nextNode);
                return next;
            }

            public void remove() {
                throw new UnsupportedOperationException("Read-only iterator");
            }
        };
    }

    public int sum() {
        return this.aggregatedCounts[this.tree.root()];
    }

    void checkBalance() {
        this.tree.checkBalance(this.tree.root());
    }

    void checkAggregates() {
        this.checkAggregates(this.tree.root());
    }

    private void checkAggregates(int node) {
        assert this.aggregatedCounts[node] == this.counts[node] + this.aggregatedCounts[this.tree.left(node)] + this.aggregatedCounts[this.tree.right(node)];

        if (node != 0) {
            this.checkAggregates(this.tree.left(node));
            this.checkAggregates(this.tree.right(node));
        }

    }
}

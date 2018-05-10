//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tdigest;

import java.io.Serializable;
import java.util.Arrays;

abstract class IntAVLTree implements Serializable {
    protected static final int NIL = 0;
    private final IntAVLTree.NodeAllocator nodeAllocator;
    private int root;
    private int[] parent;
    private int[] left;
    private int[] right;
    private byte[] depth;

    static int oversize(int size) {
        return size + (size >>> 3);
    }

    IntAVLTree(int initialCapacity) {
        this.nodeAllocator = new IntAVLTree.NodeAllocator();
        this.root = 0;
        this.parent = new int[initialCapacity];
        this.left = new int[initialCapacity];
        this.right = new int[initialCapacity];
        this.depth = new byte[initialCapacity];
    }

    IntAVLTree() {
        this(16);
    }

    public int root() {
        return this.root;
    }

    public int capacity() {
        return this.parent.length;
    }

    protected void resize(int newCapacity) {
        this.parent = Arrays.copyOf(this.parent, newCapacity);
        this.left = Arrays.copyOf(this.left, newCapacity);
        this.right = Arrays.copyOf(this.right, newCapacity);
        this.depth = Arrays.copyOf(this.depth, newCapacity);
    }

    public int size() {
        return this.nodeAllocator.size();
    }

    public int parent(int node) {
        return this.parent[node];
    }

    public int left(int node) {
        return this.left[node];
    }

    public int right(int node) {
        return this.right[node];
    }

    public int depth(int node) {
        return this.depth[node];
    }

    public int first(int node) {
        if (node == 0) {
            return 0;
        } else {
            while(true) {
                int left = this.left(node);
                if (left == 0) {
                    return node;
                }

                node = left;
            }
        }
    }

    public int last(int node) {
        while(true) {
            int right = this.right(node);
            if (right == 0) {
                return node;
            }

            node = right;
        }
    }

    public final int next(int node) {
        int right = this.right(node);
        if (right != 0) {
            return this.first(right);
        } else {
            int parent;
            for(parent = this.parent(node); parent != 0 && node == this.right(parent); parent = this.parent(parent)) {
                node = parent;
            }

            return parent;
        }
    }

    public final int prev(int node) {
        int left = this.left(node);
        if (left != 0) {
            return this.last(left);
        } else {
            int parent;
            for(parent = this.parent(node); parent != 0 && node == this.left(parent); parent = this.parent(parent)) {
                node = parent;
            }

            return parent;
        }
    }

    protected abstract int compare(int var1);

    protected abstract void copy(int var1);

    protected abstract void merge(int var1);

    public boolean add() {
        if (this.root == 0) {
            this.root = this.nodeAllocator.newNode();
            this.copy(this.root);
            this.fixAggregates(this.root);
            return true;
        } else {
            int node = this.root;

            assert this.parent(this.root) == 0;

            int parent;
            int cmp;
            do {
                cmp = this.compare(node);
                if (cmp < 0) {
                    parent = node;
                    node = this.left(node);
                } else {
                    if (cmp <= 0) {
                        this.merge(node);
                        return false;
                    }

                    parent = node;
                    node = this.right(node);
                }
            } while(node != 0);

            node = this.nodeAllocator.newNode();
            if (node >= this.capacity()) {
                this.resize(oversize(node + 1));
            }

            this.copy(node);
            this.parent(node, parent);
            if (cmp < 0) {
                this.left(parent, node);
            } else {
                assert cmp > 0;

                this.right(parent, node);
            }

            this.rebalance(node);
            return true;
        }
    }

    public int find() {
        int node = this.root;

        while(node != 0) {
            int cmp = this.compare(node);
            if (cmp < 0) {
                node = this.left(node);
            } else {
                if (cmp <= 0) {
                    return node;
                }

                node = this.right(node);
            }
        }

        return 0;
    }

    public void update(int node) {
        int prev = this.prev(node);
        int next = this.next(node);
        if (prev != 0 && this.compare(prev) <= 0 || next != 0 && this.compare(next) >= 0) {
            this.remove(node);
            this.add();
        } else {
            this.copy(node);

            for(int n = node; n != 0; n = this.parent(n)) {
                this.fixAggregates(n);
            }
        }

    }

    public void remove(int node) {
        if (node == 0) {
            throw new IllegalArgumentException();
        } else {
            int parent;
            if (this.left(node) != 0 && this.right(node) != 0) {
                parent = this.next(node);

                assert parent != 0;

                this.swap(node, parent);
            }

            assert this.left(node) == 0 || this.right(node) == 0;

            parent = this.parent(node);
            int child = this.left(node);
            if (child == 0) {
                child = this.right(node);
            }

            if (child == 0) {
                if (node == this.root) {
                    assert this.size() == 1 : this.size();

                    this.root = 0;
                } else if (node == this.left(parent)) {
                    this.left(parent, 0);
                } else {
                    assert node == this.right(parent);

                    this.right(parent, 0);
                }
            } else {
                if (node == this.root) {
                    assert this.size() == 2;

                    this.root = child;
                } else if (node == this.left(parent)) {
                    this.left(parent, child);
                } else {
                    assert node == this.right(parent);

                    this.right(parent, child);
                }

                this.parent(child, parent);
            }

            this.release(node);
            this.rebalance(parent);
        }
    }

    private void release(int node) {
        this.left(node, 0);
        this.right(node, 0);
        this.parent(node, 0);
        this.nodeAllocator.release(node);
    }

    private void swap(int node1, int node2) {
        int parent1 = this.parent(node1);
        int parent2 = this.parent(node2);
        if (parent1 != 0) {
            if (node1 == this.left(parent1)) {
                this.left(parent1, node2);
            } else {
                assert node1 == this.right(parent1);

                this.right(parent1, node2);
            }
        } else {
            assert this.root == node1;

            this.root = node2;
        }

        if (parent2 != 0) {
            if (node2 == this.left(parent2)) {
                this.left(parent2, node1);
            } else {
                assert node2 == this.right(parent2);

                this.right(parent2, node1);
            }
        } else {
            assert this.root == node2;

            this.root = node1;
        }

        this.parent(node1, parent2);
        this.parent(node2, parent1);
        int left1 = this.left(node1);
        int left2 = this.left(node2);
        this.left(node1, left2);
        if (left2 != 0) {
            this.parent(left2, node1);
        }

        this.left(node2, left1);
        if (left1 != 0) {
            this.parent(left1, node2);
        }

        int right1 = this.right(node1);
        int right2 = this.right(node2);
        this.right(node1, right2);
        if (right2 != 0) {
            this.parent(right2, node1);
        }

        this.right(node2, right1);
        if (right1 != 0) {
            this.parent(right1, node2);
        }

        int depth1 = this.depth(node1);
        int depth2 = this.depth(node2);
        this.depth(node1, depth2);
        this.depth(node2, depth1);
    }

    private int balanceFactor(int node) {
        return this.depth(this.left(node)) - this.depth(this.right(node));
    }

    private void rebalance(int node) {
        int p;
        for(int n = node; n != 0; n = p) {
            p = this.parent(n);
            this.fixAggregates(n);
            switch(this.balanceFactor(n)) {
                case -2:
                    int right = this.right(n);
                    if (this.balanceFactor(right) == 1) {
                        this.rotateRight(right);
                    }

                    this.rotateLeft(n);
                case -1:
                case 0:
                case 1:
                    break;
                case 2:
                    int left = this.left(n);
                    if (this.balanceFactor(left) == -1) {
                        this.rotateLeft(left);
                    }

                    this.rotateRight(n);
                    break;
                default:
                    throw new AssertionError();
            }
        }

    }

    protected void fixAggregates(int node) {
        this.depth(node, 1 + Math.max(this.depth(this.left(node)), this.depth(this.right(node))));
    }

    private void rotateLeft(int n) {
        int r = this.right(n);
        int lr = this.left(r);
        this.right(n, lr);
        if (lr != 0) {
            this.parent(lr, n);
        }

        int p = this.parent(n);
        this.parent(r, p);
        if (p == 0) {
            this.root = r;
        } else if (this.left(p) == n) {
            this.left(p, r);
        } else {
            assert this.right(p) == n;

            this.right(p, r);
        }

        this.left(r, n);
        this.parent(n, r);
        this.fixAggregates(n);
        this.fixAggregates(this.parent(n));
    }

    private void rotateRight(int n) {
        int l = this.left(n);
        int rl = this.right(l);
        this.left(n, rl);
        if (rl != 0) {
            this.parent(rl, n);
        }

        int p = this.parent(n);
        this.parent(l, p);
        if (p == 0) {
            this.root = l;
        } else if (this.right(p) == n) {
            this.right(p, l);
        } else {
            assert this.left(p) == n;

            this.left(p, l);
        }

        this.right(l, n);
        this.parent(n, l);
        this.fixAggregates(n);
        this.fixAggregates(this.parent(n));
    }

    private void parent(int node, int parent) {
        assert node != 0;

        this.parent[node] = parent;
    }

    private void left(int node, int left) {
        assert node != 0;

        this.left[node] = left;
    }

    private void right(int node, int right) {
        assert node != 0;

        this.right[node] = right;
    }

    private void depth(int node, int depth) {
        assert node != 0;

        assert depth >= 0 && depth <= 127;

        this.depth[node] = (byte)depth;
    }

    void checkBalance(int node) {
        if (node == 0) {
            assert this.depth(node) == 0;
        } else {
            assert this.depth(node) == 1 + Math.max(this.depth(this.left(node)), this.depth(this.right(node)));

            assert Math.abs(this.depth(this.left(node)) - this.depth(this.right(node))) <= 1;

            this.checkBalance(this.left(node));
            this.checkBalance(this.right(node));
        }

    }

    private static class NodeAllocator implements Serializable {
        private int nextNode = 1;
        private final IntAVLTree.IntStack releasedNodes = new IntAVLTree.IntStack();

        NodeAllocator() {
        }

        int newNode() {
            return this.releasedNodes.size() > 0 ? this.releasedNodes.pop() : this.nextNode++;
        }

        void release(int node) {
            assert node < this.nextNode;

            this.releasedNodes.push(node);
        }

        int size() {
            return this.nextNode - this.releasedNodes.size() - 1;
        }
    }

    private static class IntStack implements Serializable {
        private int[] stack = new int[0];
        private int size = 0;

        IntStack() {
        }

        int size() {
            return this.size;
        }

        int pop() {
            return this.stack[--this.size];
        }

        void push(int v) {
            if (this.size >= this.stack.length) {
                int newLength = IntAVLTree.oversize(this.size + 1);
                this.stack = Arrays.copyOf(this.stack, newLength);
            }

            this.stack[this.size++] = v;
        }
    }
}

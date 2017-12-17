import numpy as np

p_gen = np.polynomial.chebyshev.Chebyshev


class CSketch:
    def __init__(self, k=4):
        self.k = k
        self.min = None
        self.max = None
        self.m = np.zeros(self.k, dtype="float64")

    def train(self, data):
        self.min = np.min(data)
        self.max = np.max(data)

        ndata = data - self.min
        ndata = ndata / np.max(ndata)
        ndata = (ndata * 2) - 1

        for i in range(self.k):
            p = p_gen.basis(deg=i)
            self.m[i] = np.mean(p(ndata))

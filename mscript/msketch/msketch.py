import numpy as np
import scipy.special

class MSketch:
    def __init__(self, k=4):
        self.k = k
        self.m = np.zeros(self.k, dtype="float64")
        self.min = None
        self.max = None

    def sketch(self, data):
        self.min = np.min(data)
        self.max = np.max(data)
        self.m = np.array([
            np.sum(data**i) for i in range(self.k)
        ])

    def normalize_moments(self):
        d = self.max - self.min
        k = self.k
        m = self.m
        
        mu2 = np.zeros(k)
        apows = np.power(-self.min, np.arange(0,k))
        mu2[0] = m[0]
        for i in range(1,k):
            combs = scipy.special.comb(i, np.arange(0,i+1))
            mu2[i] = np.sum(combs * m[0:i+1][::-1] * apows[0:i+1]) / (d**(i))

        mu2 /= m[0]
        return mu2
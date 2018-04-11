import numpy as np


class PointSolver:
    def __init__(self, n_mus, verbose=False):
        self.n = int((n_mus + 1)/2)
        n = self.n
        self.pmus = None
        self.mmat = np.zeros(shape=(n, n))
        self.locs = None
        self.weights = None
        self.is_discrete = False

        self.verbose = verbose

    def solve(self, pmus: np.ndarray):
        self.pmus = pmus / pmus[0]
        n = self.n

        for i in range(n):
            for j in range(n):
                self.mmat[i, j] = self.pmus[i + j]

        self.is_discrete = False
        n_points = n-1
        cur_det = 1.0
        for i in range(1, n):
            new_det = np.linalg.det(self.mmat[:i,:i])
            ratio = new_det / cur_det
            cur_det = new_det
            if self.verbose:
                print("eig ratio: {}".format(ratio))
            if ratio < 1e-10:
                n_points = i-1
                self.is_discrete = True
                break

        n = n_points + 1
        short_mat = self.mmat[:n - 1, :n]
        coeffs = np.array([
            (-1) ** i * np.linalg.det(np.delete(short_mat, i, 1))
            for i in range(n)
        ])
        self.locs = np.roots(coeffs[::-1])
        A = np.vstack([self.locs ** i for i in range(n_points)])
        b = self.pmus[:n_points]
        self.weights = np.linalg.solve(A, b)

        i_order = np.argsort(self.locs)
        self.locs = self.locs[i_order]
        self.weights = self.weights[i_order]
        if self.verbose:
            print(self.locs, self.weights)
        return self.locs, self.weights

    def get_quantile(self, p:float) -> float:
        n = len(self.weights)
        if p <= 0.0:
            return self.locs[0]
        if p >= 1.0:
            return self.locs[-1]
        total = 0.0
        for i in range(n):
            total += self.weights[i]
            if total > p:
                return self.locs[i]
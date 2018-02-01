from typing import Iterable, Callable, Sequence
import math
import numpy as np
import scipy
import scipy.integrate


class Solver:
    def __init__(
            self,
            funs: Sequence[Callable[[float], float]],
            mus: np.ndarray,
            range_min: float,
            range_max: float
    ):
        self.k = len(funs)
        self.funs = funs
        self.mus = mus
        self.range_min = range_min
        self.range_max = range_max
        self.cs = np.zeros(self.k)
        self.verbose = False

    def set_cs(self, cs: np.ndarray):
        self.cs = cs

    def set_verbose(self, flag: bool):
        self.verbose = flag

    def pdf(self, x: float):
        f_vals = np.array([
            f(x) for f in self.funs
        ])
        return math.exp(self.cs.dot(f_vals))

    def calc_moment(
            self,
            weight: Callable[[float], float]
    ) -> float:
        r, _ = scipy.integrate.quad(
            lambda x: weight(x) * self.pdf(x),
            self.range_min,
            self.range_max
        )
        return r

    def P(self) -> float:
        m0 = self.calc_moment(lambda x: 1.0)
        return m0 - self.mus.dot(self.cs)

    def dP(self) -> np.ndarray:
        results = np.zeros(self.k)
        for i in range(self.k):
            results[i] = self.calc_moment(self.funs[i]) - self.mus[i]
        return results

    def H(self) -> np.ndarray:
        Hmat = np.zeros(shape=(self.k, self.k))
        for i in range(self.k):
            for j in range(self.k):
                Hmat[i,j] = self.calc_moment(
                    lambda x: self.funs[i](x) * self.funs[j](x)
                )
        return Hmat

    def solve(
            self,
            num_steps = 10
    ):
        for step_num in range(num_steps):
            Hmat = self.H()
            dPmat = self.dP()
            step = scipy.linalg.solve(
                Hmat,
                -dPmat,
                assume_a="pos"
            )
            self.set_cs(self.cs + step)
            if self.verbose:
                print("STEP: {}".format(step_num))
                print(repr(self.cs))
                print(dPmat)

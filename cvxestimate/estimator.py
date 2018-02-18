import numpy as np


class Estimator:
    def __init__(self, k):
        self.a_min = 0.0
        self.a_max = 0.0
        self.k = k
        self.moments = np.zeros(k)

    def set_statistics(
            self,
            a_min: float,
            a_max: float,
            moments: float,
    ):
        self.a_min = a_min
        self.a_max = a_max
        self.moments = moments

    def solve(self):
        raise NotImplemented()

    def estimate(self, p: float):
        raise NotImplemented()
import numpy as np
import math
import scipy.stats

import estimator

class GaussEstimator(estimator.Estimator):
    def __init__(self,k):
        super().__init__(k)
        self.mu = 0
        self.std = 1

    def solve(self):
        self.mu = self.moments[1]
        self.std = math.sqrt(self.moments[2] - self.mu*self.mu)
        # xs = np.linspace(0,1,1000)
        # values = scipy.stats.norm.pdf(
        #     xs, loc=self.mu, scale=self.std
        # )
        # return values

    def estimate(self, p: float):
        xloc = scipy.stats.norm.ppf(
            p, loc=self.mu, scale=self.std
        )
        return xloc*(self.a_max-self.a_min) + self.a_min
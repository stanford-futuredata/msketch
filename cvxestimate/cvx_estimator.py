import cvxpy as cvx
import numpy as np

import estimator


class CvxEstimator(estimator.Estimator):
    def __init__(
            self,
            k:int,
            resolution=1000,
            solver="maxent"
    ):
        super().__init__(k)
        self.resolution = resolution
        self.locs = np.linspace(0, 1, self.resolution)
        m_list = []
        for i in range(k):
            m_list.append(
                self.locs**i
            )
        self.loc_moments = np.array(m_list)
        self.solver = solver
        self.values = None

    def solve(self):
        if self.solver == "lp":
            xsol = np.linalg.lstsq(
                self.loc_moments,
                self.moments
            )[0]
            self.values = xsol
        else:
            # Moment values of the boundaries
            Xs = cvx.Variable(self.resolution)
            constraints = [
                Xs >= 0,
                Xs <= 1.0,
                self.loc_moments * Xs == self.moments
            ]
            if self.solver == "mindensity":
                o = cvx.Minimize(cvx.max_entries(Xs))
            else:
                o = cvx.Maximize(cvx.sum_entries(cvx.entr(Xs)))
            prob = cvx.Problem(o, constraints)
            sol = prob.solve(solver=cvx.ECOS)
            self.values = Xs.value
        return self.values * 1000

    def estimate(self, p: float):
        running_sum = 0
        excess_fraction = 0.5
        for i, val in enumerate(self.values):
            val = float(val)
            running_sum += val
            if running_sum >= p:
                excess_fraction = (running_sum - p) / val
                break

        best_est = (1-excess_fraction)*self.locs[i] + excess_fraction*self.locs[i+1]
        return best_est*(self.a_max-self.a_min) + self.a_min

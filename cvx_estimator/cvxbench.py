import argparse
import json
import sys
import time
import matplotlib.pyplot as plt

import numpy as np
import cvxpy as cvx
import scipy

class CvxEstimator:
    def __init__(
            self,
            a_min: float,
            a_max: float,
            moments,
            resolution=1000,
    ):
        self.k = len(moments)
        self.resolution = resolution

        self.min = a_min
        self.max = a_max
        self.locs = np.linspace(self.min, self.max, resolution)

        self.data_moments = np.array(moments)
        # Moment values of the boundaries
        self.loc_moments = []
        for i in range(self.k):
            self.loc_moments.append(
                self.locs**i
            )
        self.loc_moments = np.array(self.loc_moments)

        Xs = cvx.Variable(resolution)
        self.Xs = Xs
        self.constraints = [
            Xs >= 0,
            Xs <= 1.0,
            self.loc_moments * Xs == self.data_moments
        ]

    def solve_maxent(self):
        prob = cvx.Problem(
            cvx.Maximize(cvx.sum_entries(cvx.entr(
                self.Xs
            ))),
            self.constraints
        )
        sol = prob.solve(solver=cvx.ECOS)
        values = self.Xs.value
        return values

    def solve_lp(self):
        prob = cvx.Problem(
            # cvx.Maximize(cvx.sum_entries(cvx.entr(
            #     self.Xs
            # ))),
            # cvx.Maximize(0),
            cvx.Minimize(cvx.max_entries(self.Xs)),
            self.constraints
        )
        sol = prob.solve(solver=cvx.ECOS,verbose=True)
        values = self.Xs.value
        return values


    def est_quantile_from_values(self, values, quantile):
        running_sum = 0
        excess_fraction = 0.5
        for i, val in enumerate(values):
            val = float(val)
            running_sum += val
            if running_sum >= quantile:
                excess_fraction = (running_sum - quantile) / val
                break

        best_est = (1-excess_fraction)*self.locs[i] + excess_fraction*self.locs[i+1]
        return best_est


parser = argparse.ArgumentParser(description='Moment Quantile Sketch Solver')
parser.add_argument("min", type=float)
parser.add_argument("max", type=float)
parser.add_argument("moments")
parser.add_argument("query", type=float)
parser.add_argument("--resolution", type=int, default=1000)
parser.add_argument("--point", action="store_true", default=False)

def test_lp_np():
    amin = 0
    amax = 1
    moments = np.array([
1.0,
 0.32486496131339415,
 0.13670550089511058,
 0.069534547548459308,
 0.039711253391653267,
 0.024290415008215556,
 0.015536344694360594,
 0.010274031144859938,
 0.0069864133677746019    ])
    xs = np.linspace(amin, amax, 1000)
    A = np.vstack([
        xs ** i for i in range(len(moments))
    ])
    xsol = scipy.optimize.lsq_linear(A, moments, (0,1)).x
    # xsol = np.linalg.lstsq(A, moments)[0]

    e = CvxEstimator(
        a_min=amin,
        a_max=amax,
        moments=moments,
        resolution=1000
    )
    values = e.solve_maxent()

    plt.figure()
    plt.plot(xs, xsol, label="np")
    plt.plot(xs, values, label="maxent")
    plt.legend()
    plt.show()

    return xsol

def test_lp_acc():
    amin = 0
    amax = 1
    moments = [
    1.0,
 0.16697417054330901,
 0.062865664670922625,
 0.03300482612534044,
 0.020461720216716581,
 0.014046570116995484,
 0.010356997858492523]
    e = CvxEstimator(
        a_min=amin,
        a_max=amax,
        moments=moments,
        resolution=1000
    )
    values = e.solve_lp()
    return values


def test_solve_time():
 #    amin = 412.75
 #    amax = 2076.5
 #    moments = [1.0,
 # 690.55327624143035,
 # 573705.35417805763,
 # 579773848.78093076,
 # 692768038763.12378,
 # 936229045768396.75,
 # 1.378198547307115e+18]
    amin = 0
    amax = 1
    moments = [
    1.0,
     0.16697417054330901,
     0.062865664670922625,
     0.03300482612534044,
     0.020461720216716581,
     0.014046570116995484,
     0.010356997858492523]
    e = CvxEstimator(
        a_min=amin,
        a_max=amax,
        moments=moments,
        resolution=1000
    )
    num_iters = 1
    start_time = time.time()
    for i in range(num_iters):
        values = e.solve_maxent()
    plt.figure()
    plt.plot(values)
    plt.show()
    end_time = time.time()
    print((end_time - start_time)/num_iters)
    print(e.est_quantile_from_values(values, .1))
    print(e.est_quantile_from_values(values, .5))
    print(e.est_quantile_from_values(values, .9))


def main():
    args = parser.parse_args()
    moments = json.loads(args.moments)

    e = CvxEstimator(
        a_min=args.min,
        a_max=args.max,
        moments=moments,
        resolution=args.resolution,
    )
    v = args.query
    if not args.point:
        lb, ub = e.solve_quantile(v)
        print("{},{}".format(lb, ub))
    else:
        result, q_l, q_u = e.solve_quantile_maxent(v)
        print("{},{},{}".format(result, q_l, q_u))
    return 0


if __name__ == "__main__":
    sys.exit(test_lp_np())
    # sys.exit(main())
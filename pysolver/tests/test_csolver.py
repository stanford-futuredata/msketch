import math
import numpy as np
import unittest
import pandas as pd
import os

import csolver
import preprocess

from numpy.polynomial.chebyshev import Chebyshev
cb = Chebyshev.basis


class TestSolver(unittest.TestCase):
    def test_lowprec(self):
        n = 10000
        k = 10
        xs = np.concatenate([
            np.random.normal(loc=2, scale=1, size=n),
            np.random.normal(loc=6, scale=.5, size=n),
            np.random.normal(loc=10, scale=1, size=n)
        ]) + 50

        shifter = preprocess.Shifter(compress=False)
        shifter.set_xs(xs, k)
        cxs = shifter.cxs
        cheby_moments = np.array([
            np.mean(cb(i)(cxs)) for i in range(k)
        ])
        print(cheby_moments)

        power_sums = np.array([
            np.sum(xs ** i) for i in range(k)
        ])
        rounded_sums = power_sums
        rounded_sums = np.array([float("{:.15g}".format(x)) for x in power_sums])
        shifter.set_powers(rounded_sums, amin=np.min(xs), amax=np.max(xs))
        c_moments = shifter.get_mus()

        print(c_moments)

        s = csolver.CSolver(k, n=256, verbose=True)
        s.solve(c_moments)
        print(np.percentile(xs, 50))
        print(shifter.invert(s.get_quantile(.5)))

    def test_uniform(self):
        n = 10000
        k = 10
        xs = np.linspace(-1, 1, n)
        d_mus = np.array([
            np.mean(cb(i)(xs)) for i in range(k)
        ])

        power_sums = np.array([
            np.sum(xs**i) for i in range(k)
        ])
        print(power_sums)
        rounded_sums = [float("{:.3g}".format(x)) for x in power_sums]
        print(rounded_sums)
        amin = -1
        amax = 1
        c_moments = preprocess.power_sum_to_cheby(
            rounded_sums,
            amin=amin,
            amax=amax,
        )

        s = csolver.CSolver(k, n=128, verbose=True)
        s.solve(c_moments)
        print(s.get_quantile(.5))
        self.assertAlmostEqual(np.percentile(xs, 50), s.get_quantile(.5), 3)
        self.assertAlmostEqual(np.percentile(xs, 10), s.get_quantile(.1), 3)

    def test_lognorm(self):
        n = 10000
        k = 10
        xs = np.random.normal(loc=1, scale=2, size=n)
        xs = np.exp(xs)

        shifter = preprocess.Shifter(xs, compress=True)
        cxs = shifter.get_cxs()
        d_mus = np.array([
            np.mean(cb(i)(cxs)) for i in range(k)
        ])
        s = csolver.CSolver(k, n=128, verbose=False)
        s.solve(d_mus)

        p49 = np.percentile(xs, 49)
        p51 = np.percentile(xs, 51)
        p50_est = shifter.invert(s.get_quantile(.5))
        self.assertTrue(
            p49 < p50_est
        )
        self.assertTrue(
            p51 > p50_est
        )

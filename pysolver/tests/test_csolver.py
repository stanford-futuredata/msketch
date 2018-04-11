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
    def test_uniform(self):
        n = 10000
        k = 10
        xs = np.linspace(-1, 1, n)
        d_mus = np.array([
            np.mean(cb(i)(xs)) for i in range(k)
        ])
        s = csolver.CSolver(k, n=128, verbose=False)
        s.solve(d_mus)
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

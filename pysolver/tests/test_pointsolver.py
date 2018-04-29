import numpy as np
import unittest

import preprocess
import pointsolver

from numpy.polynomial.chebyshev import Chebyshev
cb = Chebyshev.basis


class TestPointSolver(unittest.TestCase):
    def test_check_discrete(self):
        k = 11
        xs = np.array([100.0, 200.0, 300.0, 400.0, 200.0]).astype(float)
        processor = preprocess.Shifter(compress=False)
        processor.set_xs(xs, k=k)
        pmus = processor.get_pmus()

        ps = pointsolver.PointSolver(len(pmus))
        locs, weights = ps.solve(pmus)
        self.assertEqual(4, len(locs))
        self.assertTrue(ps.is_discrete)
        self.assertAlmostEqual(200.0, processor.invert(ps.get_quantile(.5)), 4)
        self.assertAlmostEqual(300.0, processor.invert(ps.get_quantile(.7)), 4)

    def test_continuous(self):
        k = 11
        xs = np.linspace(0, 100, 1000)
        processor = preprocess.Shifter(compress=False)
        processor.set_xs(xs, k=k)
        pmus = processor.get_pmus()

        ps = pointsolver.PointSolver(len(pmus))
        locs, weights = ps.solve(pmus)
        print(locs)
        print(weights)
        self.assertFalse(ps.is_discrete)
        p50 = processor.invert(ps.get_quantile(.5))
        self.assertTrue(p50 > 40)
        self.assertTrue(p50 < 60)

    def test_exponential(self):
        k = 11
        xs = np.random.exponential(scale=1, size=100000)
        processor = preprocess.Shifter(compress=False)
        processor.set_xs(xs, k=k)
        pmus = processor.get_pmus()

        ps = pointsolver.PointSolver(len(pmus))
        locs, weights = ps.solve(pmus)
        print(np.min(xs), np.max(xs))
        print(repr(processor.invert(locs)))
        print(repr(weights))

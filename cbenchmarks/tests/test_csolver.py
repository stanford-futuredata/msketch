import os
print(os.getcwd())
import unittest
import numpy as np
import benchmarks.csolver as csolver


class TestCSolver(unittest.TestCase):
    def test_simple(self):
        ms = np.array([1.0, 0.0, -1.0/3])
        lambdas, steps = csolver.solve(ms)
        self.assertLessEqual(steps, 5)
        self.assertAlmostEqual(0.0, lambdas[1], 10)
        self.assertAlmostEqual(0.0, lambdas[2], 10)

        f = csolver.gen_pdf(lambdas)
        q = csolver.quantile(f, .5)
        self.assertAlmostEqual(f(0), .5, 10)
        self.assertAlmostEqual(0.0, q, 10)

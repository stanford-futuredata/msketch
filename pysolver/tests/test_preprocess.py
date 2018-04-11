import math
import numpy as np
import unittest

import preprocess

from numpy.polynomial.chebyshev import Chebyshev
cb = Chebyshev.basis


class TestSolver(unittest.TestCase):
    def test_shift_powers(self):
        n = 10000
        k = 10
        xs = np.linspace(0, 100, n)

        amin = np.min(xs)
        amax = np.max(xs)
        xr = (amax - amin)/2
        xc = (amax + amin)/2

        power_sums = np.array([np.sum(xs**i) for i in range(k)])
        s_sums = preprocess.shift_power_sum(
            power_sums,
            xr,
            xc
        )
        s_sums /= s_sums[0]
        self.assertAlmostEqual(1, s_sums[0], 10)
        self.assertAlmostEqual(0, s_sums[1], 3)
        self.assertAlmostEqual(1.0/3, s_sums[2], 3)

        c_moments = preprocess.power_sum_to_cheby(
            power_sums,
            amin=amin,
            amax=amax,
        )
        self.assertAlmostEqual(-1.0/3, c_moments[2], 3)

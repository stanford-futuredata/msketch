import math
import numpy as np
import unittest
import lsolver.basis


class TestBasis(unittest.TestCase):
    def test_cheb_log_basis(self):
        funs = lsolver.basis.get_cheb_log_basis(
            2,
            0, 1,
            -2, 2
        )
        self.assertEqual(1, funs[0](.5))
        self.assertAlmostEqual(0.0, funs[1](.5), 10)
        self.assertAlmostEqual(1, funs[4](math.exp(2)), 10)
        self.assertAlmostEqual(-1, funs[4](1), 10)

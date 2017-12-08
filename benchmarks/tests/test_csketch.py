import unittest
import numpy as np
from benchmarks.csketch import CSketch


class TestCSketch(unittest.TestCase):
    def test_simple(self):
        data = np.linspace(0, 100, 10000)
        cs = CSketch(5)
        cs.train(data)
        self.assertAlmostEqual(1.0, cs.m[0], 3)
        self.assertAlmostEqual(0.0, cs.m[1], 3)
        self.assertAlmostEqual(-.3333, cs.m[2], 3)

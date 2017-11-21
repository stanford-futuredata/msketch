import unittest
import numpy as np

from .context import msketch

class TestMSketch(unittest.TestCase):
    def test_msketch(self):
        data = np.linspace(-10,20,1000)
        ms = msketch.msketch.MSketch(5)
        ms.sketch(data)
        self.assertEqual(1000, ms.m[0])
        n_mus = ms.normalize_moments()
        self.assertEqual(.5, n_mus[1], 1e-7)
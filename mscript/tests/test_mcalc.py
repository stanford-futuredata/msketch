import unittest
import numpy as np
import scipy

from .context import msketch

from msketch.msketch import MSketch
from msketch.mcalc import MCalc

class TestMCalc(unittest.TestCase):
    def test_mcalc(self):
        data = np.linspace(0,100,1000)
        ms = MSketch(7)
        ms.sketch(data)
        mc = MCalc(ms)
        mc.calc()
        self.assertAlmostEqual(50.0, mc.quantile(.5), places=2)
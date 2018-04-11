import numpy as np
import unittest

import msolver


class TestMSolver(unittest.TestCase):
    def test_check_discrete(self):
        k = 11
        xs = np.array([100.0, 200.0, 300.0, 400.0, 200.0]).astype(float)

        axs = np.arcsinh(xs)
        psums = np.array([np.sum(axs**i) for i in range(k)])
        amin, amax = np.min(axs), np.max(axs)
        ms = msolver.MSolver(k, integer=False, verbose=False)
        ms.solve(psums, amin=amin, amax=amax)
        p50 = ms.get_quantile(.5)
        p70 = ms.get_quantile(.7)
        self.assertAlmostEqual(200, p50, 4)
        self.assertAlmostEqual(300, p70, 4)

    def test_large_discrete(self):
        k = 11
        xs = np.arange(10, 30).astype(float)

        axs = np.arcsinh(xs)
        psums = np.array([np.sum(axs ** i) for i in range(k)])
        amin, amax = np.min(axs), np.max(axs)
        ms = msolver.MSolver(k, integer=False, verbose=False)
        ms.solve(psums, amin=amin, amax=amax)
        p50 = ms.get_quantile(.5)
        self.assertTrue(p50 > 19)
        self.assertTrue(p50 < 21)

    def test_continuous(self):
        k = 11
        xs = np.linspace(0, 100, 10000)

        axs = np.arcsinh(xs)
        psums = np.array([np.sum(axs**i) for i in range(k)])
        amin, amax = np.min(axs), np.max(axs)
        ms = msolver.MSolver(k, integer=False, verbose=False, compressed=True)
        ms.solve(psums, amin=amin, amax=amax)
        p50 = ms.get_quantile(.5)
        self.assertTrue(p50 > 49)
        self.assertTrue(p50 < 51)


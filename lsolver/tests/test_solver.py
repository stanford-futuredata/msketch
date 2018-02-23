import math
import numpy as np
import unittest
import lsolver.solver
import lsolver.basis
import pandas as pd
import os

class TestSolver(unittest.TestCase):
    def test_calc_moment(self):
        solver = lsolver.solver.Solver(
            [
                lambda x: 1,
                lambda x: x
            ],
            np.array([0.0, 0.0]),
            0, 1
        )
        cs = np.array([0, 1])
        solver.set_cs(cs)
        m0 = solver.calc_moment(lambda x: 1)
        self.assertAlmostEqual(math.e - 1, m0, 10)
        m1 = solver.calc_moment(lambda x: x)
        self.assertAlmostEqual(1.0, m1, 10)

    def test_solve_simple(self):
        funs = [
            lambda x: 1,
            lambda x: x,
            lambda x: x**2,
            lambda x: math.log(x),
            lambda x: math.log(x)**2
        ]
        cs = np.array([0,-1,0,1,0])
        mus = np.array([ 0.99945093,  1.99446088,  5.93798369,  0.42183849,  0.81948816])
        solver = lsolver.solver.Solver(
            funs,
            mus,
            0.01, 10
        )
        # solver.set_verbose(True)
        solver.solve(10)
        for i in range(len(cs)):
            self.assertAlmostEqual(cs[i], solver.cs[i], 5)

    def test_shuttle(self):
        df = pd.read_csv("sampledata/shuttle.csv")
        xs = df["0"].values.astype(float)
        logxs = np.log(xs)
        a = np.min(xs)
        b = np.max(xs)
        log_a = np.min(logxs)
        log_b = np.max(logxs)
        funs = lsolver.basis.get_cheb_log_basis(
            2, a, b, log_a, log_b
        )
        mus = np.array([
            np.mean(f(xs)) for f in funs
        ])

        solver = lsolver.solver.Solver(
            funs,
            mus,
            a, b
        )
        solver.set_verbose(True)
        solver.solve(20)

        print(solver.cs)



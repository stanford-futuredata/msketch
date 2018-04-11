import numpy as np

import csolver
import pointsolver
import preprocess


class MSolver:
    def __init__(
            self,
            k: int,
            compressed: bool = True,
            integer: bool = False,
            verbose: bool = False
    ):
        self.k = k
        self.integer = integer
        self.csolver = csolver.CSolver(
            k, n=64, n_steps=15, verbose=verbose
        )
        self.psolver = pointsolver.PointSolver(
            n_mus = k, verbose=verbose
        )
        self.shifter = preprocess.Shifter(compress=compressed, integral=integer)

    def solve(self, psums: np.ndarray, amin: float, amax: float):
        self.shifter.set_powers(psums, amin=amin, amax=amax)
        pmus = self.shifter.get_pmus()
        cmus = self.shifter.get_mus()
        # print("est cmus: {}".format(cmus))
        self.psolver.solve(pmus)
        if not self.psolver.is_discrete:
            self.csolver.solve(d_mus = cmus)

    def get_quantile(self, p:float) -> float:
        if self.psolver.is_discrete:
            raw_q = self.psolver.get_quantile(p)
        else:
            raw_q = self.csolver.get_quantile(p)
        return self.shifter.invert(raw_q)

from typing import Iterable, Callable, Sequence
import math
import numpy as np
import scipy
import scipy.integrate
import scipy.fftpack
import scipy.optimize

from numpy.polynomial.chebyshev import Chebyshev
cb = Chebyshev.basis


def fit_cheby(fvals: np.ndarray) -> np.ndarray:
    n = len(fvals)
    cs = scipy.fftpack.dct(fvals, type=1)
    cs /= n
    cs[0] /= 2
    return cs


class CSolver:
    def __init__(
            self,
            k: int,
            n: int = 128,
            n_steps: int = 15,
            grad_tol: float = 1e-8,
            verbose : bool=False
    ):
        self.k = k
        self.n = n
        self.xrs = np.cos(np.arange(n+1) * math.pi / n)
        xrs_moments = [
            cb(i)(self.xrs)
            for i in range(k)
        ]
        self.G = np.vstack(xrs_moments)
        self.verbose = verbose
        self.n_steps = n_steps
        self.grad_tol = grad_tol
        self.lambd = None
        self.f_poly = None

    def solve(
            self,
            d_mus:np.ndarray,
            lambd=None,
    ):
        n = self.n
        k = self.k

        if lambd is None:
            lambd = np.zeros(self.k)

        H = np.zeros(shape=(k, k))
        fpoly = None

        for i_step in range(self.n_steps):
            if fpoly is None:
                fvals = np.exp(lambd.dot(self.G))
                cs = fit_cheby(fvals)
                fpoly = Chebyshev(cs)

            e_mu = np.array([
                (fpoly*cb(i)).integ(lbnd=-1)(1)
                for i in range(2*k)
            ])
            grad = e_mu[:k] - d_mus
            grad_norm = np.linalg.norm(grad)
            Pval_old = e_mu[0] - lambd.dot(d_mus)
            if self.verbose:
                print("Step: {}, Grad: {}, P: {}".format(
                    i_step, grad_norm, Pval_old
                ))
            if grad_norm < self.grad_tol:
                break

            for i in range(k):
                for j in range(k):
                    H[i, j] = (e_mu[i+j] + e_mu[abs(i-j)]) / 2
            step = -np.linalg.solve(H, grad)
            dfdx = step.dot(grad)

            stepScaleFactor = 1.0
            newX = lambd + stepScaleFactor * step
            alpha = .3
            beta = .25

            while True:
                fvals = np.exp(newX.dot(self.G))
                cs = fit_cheby(fvals)
                fpoly = np.polynomial.chebyshev.Chebyshev(cs)

                Pval_new = fpoly.integ(lbnd=-1)(1) - newX.dot(d_mus)
                delta_change = Pval_old + alpha * stepScaleFactor * dfdx - Pval_new
                if delta_change > -1e-6 or stepScaleFactor < 1e-3:
                    break
                else:
                    stepScaleFactor *= beta
                if self.verbose:
                    print("step: {}, delta: {}".format(stepScaleFactor, delta_change))
                newX = lambd + stepScaleFactor * step

            lambd = newX

        self.lambd = lambd
        self.f_poly = self.get_fpoly(lambd)
        self.cdf_poly = self.f_poly.integ(lbnd=-1)

    def get_fpoly(self, lambd: np.ndarray):
        n = 256
        cs = None
        while n < 10000:
            xrs = np.cos(np.arange(n + 1) * math.pi / n)
            epoly = np.polynomial.chebyshev.Chebyshev(lambd)
            f = np.exp(epoly(xrs))
            cs = scipy.fftpack.dct(f, type=1)
            eps = np.max(np.abs(cs[-3:]))
            if eps < 1e-6:
                cs *= 1 / (n - 1)
                cs[0] /= 2
                break
            else:
                n *= 2
        if self.verbose:
            print("Final Poly has degree: {}".format(n))
        fpoly = np.polynomial.chebyshev.Chebyshev(cs)
        return fpoly

    def get_pdf(self) -> Chebyshev:
        return self.f_poly

    def get_cdf(self) -> Chebyshev:
        return self.cdf_poly

    def get_quantile(self, p: float) -> float:
        pmin = self.cdf_poly(-1)
        pmax = self.cdf_poly(1)
        # Compensate for slightly un-normalized cdfs
        padj = p*(pmax - pmin) + pmin
        if padj <= pmin:
            return -1
        if padj >= pmax:
            return 1
        res = scipy.optimize.brentq(
            lambda x: self.cdf_poly(x) - padj,
            -1,
            1
        )
        return res

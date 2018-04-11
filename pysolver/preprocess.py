import numpy as np
import scipy.special

from numpy.polynomial.chebyshev import Chebyshev
cb = Chebyshev.basis


def shift_power_sum(
        power_sums: np.ndarray,
        xr: float,
        xc: float
) -> np.ndarray:
    k = len(power_sums) - 1
    nxc_powers = np.power(-xc, np.arange(0, k+1))
    r_neg_powers = np.power(1.0/xr, np.arange(0, k+1))
    scaled_power_sums = np.zeros(k+1)
    for i in range(k+1):
        scaled_power_sums[i] = r_neg_powers[i] * (
            np.sum(
                scipy.special.comb(i, np.arange(0,i+1))
                * nxc_powers[:i+1][::-1]
                * power_sums[:i+1]
            )
        )
    return scaled_power_sums


def power_sum_to_cheby(
        power_sums: np.ndarray,
        amin: float,
        amax: float
) -> np.ndarray:
    k = len(power_sums) - 1
    xr = (amax - amin) / 2
    xc = (amax + amin) / 2
    scaled_power_sums = shift_power_sum(power_sums, xr=xr, xc=xc)
    cheby_moments = [
        np.polynomial.chebyshev.cheb2poly([0]*(i-1)+[1]).dot(
            scaled_power_sums[:i]
        ) / scaled_power_sums[0]
        for i in range(1,k+2)
    ]
    return np.array(cheby_moments)


class Shifter:
    def __init__(
            self,
            compress=False,
            integral=False
    ):
        self.compress = compress
        self.integral = integral

        self.xc = None
        self.xr = None
        self.cxs = None
        self.d_mus = None
        self.d_pmus = None

    def set_xs(self, xs: np.ndarray, k: int):
        if self.compress:
            xs = np.arcsinh(xs)
        amin = np.min(xs)
        amax = np.max(xs)
        self.xc = (amin + amax)/2
        self.xr = (amax - amin)/2
        self.cxs = (xs - self.xc) / self.xr
        self.d_mus = np.array([
            np.mean(cb(i)(self.cxs)) for i in range(k)
        ])
        self.d_pmus = np.array([
            np.mean(self.cxs**i) for i in range(k)
        ])

    def set_powers(
            self,
            power_sums: np.ndarray,
            amin: float,
            amax: float,
    ):
        self.xc = (amin + amax) / 2
        self.xr = (amax - amin) / 2
        self.cxs = None
        self.d_mus = power_sum_to_cheby(power_sums, amin=amin, amax=amax)
        self.d_pmus = shift_power_sum(power_sums, xc=self.xc, xr=self.xr)
        self.d_pmus /= self.d_pmus[0]

    def get_pmus(self):
        return self.d_pmus

    def get_mus(self):
        return self.d_mus

    def invert(self, s_vals):
        x_vals = s_vals * self.xr + self.xc
        if self.compress:
            x_vals = np.sinh(x_vals)
        if self.integral:
            x_vals = np.around(x_vals)
        return x_vals

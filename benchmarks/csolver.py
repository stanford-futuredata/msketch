import ctypes as C
from typing import Tuple

import numpy as np
import scipy.integrate
import scipy.optimize
import math

p_gen = np.polynomial.chebyshev.Chebyshev

class GSL_VEC(C.Structure):
    _fields_ = [
        ("size", C.c_size_t),
        ("stride", C.c_size_t),
        ("data", C.c_void_p),
        ("block", C.c_void_p),
        ("owner", C.c_int)
    ]
def gsl_from_np(a):
    assert(a.dtype == "float64")
    return GSL_VEC(
        size=len(a),
        stride=int(a.strides[0]/8),
        data=a.ctypes.data,
        block=a.ctypes.data,
        owner=0
    )
mlib = np.ctypeslib.load_library("libmlib", "./mlib")
mlib.c_solve.argtypes = [
    C.POINTER(GSL_VEC),
    C.POINTER(GSL_VEC),
    C.c_double
]
mlib.c_solve.restype = C.c_int


def solve(
        ms: np.ndarray,
        tol: float = 1e-9
) -> Tuple[np.ndarray, int]:
    k = len(ms)
    ms_gsl = gsl_from_np(ms)
    coeffs = np.zeros(k, dtype="float64")
    ms_coeffs = gsl_from_np(coeffs)
    steps = mlib.c_solve(
        C.byref(ms_gsl),
        C.byref(ms_coeffs),
        tol
    )
    return coeffs, steps


def gen_pdf(lambdas):
    p = p_gen(lambdas)
    def f(x):
        return np.exp(-p(x))
    return f


def quantile(f, p, tol=1e-8, fmin=-1, fmax=1):
    def c(x):
        y, err = scipy.integrate.quad(f, fmin, x, epsabs=tol, epsrel=tol)
        return y - p

    val = scipy.optimize.brentq(
        f=c,
        a=fmin,
        b=fmax,
        xtol=tol,
    )
    return val


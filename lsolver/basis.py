from typing import Callable, Sequence
from numpy.polynomial.chebyshev import Chebyshev
import numpy as np
import math


def create_log_cheby(i, log_a, log_b):
    cb = Chebyshev.basis(i, domain=[log_a, log_b])
    return lambda x: cb(np.log(x))

def create_exp_cheby(i, exp_a, exp_b):
    cb = Chebyshev.basis(i, domain=[exp_a, exp_b])
    return lambda x: cb(np.exp(x))

def get_cheb_exp_basis(
        k: int,
        exp_k: int,
        a: float,
        b: float,
        exp_a: float,
        exp_b: float
):
    funs = [lambda x: np.full_like(x, 1.0)]
    for i in range(1,k+1):
        funs.append(Chebyshev.basis(i, domain=[a,b]))
    for i in range(1,exp_k+1):
        funs.append(create_exp_cheby(i, exp_a, exp_b))
    return funs


def get_cheb_log_basis(
        k: int,
        log_k: int,
        a: float,
        b: float,
        log_a: float,
        log_b: float
) -> Sequence[Callable[[float], float]]:
    funs = [lambda x: np.full_like(x, 1.0)]
    for i in range(1,k+1):
        funs.append(Chebyshev.basis(i, domain=[a,b]))
    for i in range(1,log_k+1):
        funs.append(create_log_cheby(i, log_a, log_b))
    return funs

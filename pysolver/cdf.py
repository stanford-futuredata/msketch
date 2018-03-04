from typing import Callable
import scipy.integrate

import numpy as np

def pdf_to_cdf(
        pdf: Callable[[float], float],
        a: float
) -> Callable[[float], float]:
    def cdf(x: float) -> float:
        r,_=scipy.integrate.quad(pdf, a, x)
        return r
    return cdf
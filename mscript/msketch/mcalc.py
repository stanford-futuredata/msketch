import ctypes as C
import math
import numpy as np
import scipy.optimize
import os

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
mlib.e_pdf.argtypes = [
    C.c_double, C.POINTER(GSL_VEC)
]
mlib.e_pdf.restype = C.c_double
mlib.e_moments.argtypes = [
    C.POINTER(GSL_VEC), C.POINTER(GSL_VEC), C.c_double
]
mlib.e_moments.restype = None

class MCalc:
    def __init__(self, msketch):
        self.ms = msketch
        self.d = self.ms.max - self.ms.min
        self.n_mus = None
        self.lambdas = None
    
    def calc(self):
        self.n_mus = self.ms.normalize_moments()
        self.solve_lambda()
    
    def solve_lambda(self, verbose=False):
        k = self.ms.k
        e_mus = np.zeros(shape=k, dtype="float64")
        e_mus_gsl = gsl_from_np(e_mus)
        e_mus_p = C.byref(e_mus_gsl)
        d_mus = self.n_mus

        def fdf(l):
            mlib.e_moments(
                C.byref(gsl_from_np(l)),
                e_mus_p,
                1.0e-10
            )
            f = (e_mus[0] - 1) + l.dot(d_mus)
            df = d_mus - e_mus
            return f,df
        
        res = scipy.optimize.minimize(
            fun=fdf,
            x0=np.zeros(k),
            tol=1e-9,
            jac=True,
            options={
                "disp": verbose,
                "gtol": 1e-9
            }
        )
        self.lambdas = res.x
        
    def scale_back(self, x):
        return x * self.d + self.ms.min
    
    def quantile(self, p):
        l = self.lambdas
        k = len(l)
        def f(x):
            return math.exp(-sum([l[i]*x**i for i in range(k)]))
        def c(x):
            y,err = scipy.integrate.quad(f, 0, x, epsabs=1e-8, epsrel=1e-8)
            return y - p
        val = scipy.optimize.brentq(
            f=c,
            a=0.0,
            b=1.0,
            xtol=1e-8,
        )
        return self.scale_back(val)
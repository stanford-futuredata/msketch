import numpy as np
import pandas as pd
import lsolver.solver
import lsolver.basis
import lsolver.cdf
import math
import matplotlib.pyplot as plt

# cs = np.array([56.04418812,331.05966675,-18.73521171,-330.93959537,-47.378093])

# df = pd.read_csv("../datasets/shuttle.csv")
# xs = df["0"].values.astype(float)

# df = pd.read_csv("../datasets/occupancy_2.csv")
# xs = df["CO2"].values.astype(float)

def milan_compare():
    df = pd.read_csv("../datasets/internet-mi-2013-11-sampled.csv")
    xs = df["internet"].values.astype(float)
    logxs = np.log(xs)
    a = np.min(xs)
    b = np.max(xs)
    log_a = np.min(logxs)
    log_b = np.max(logxs)

    funs = lsolver.basis.get_cheb_log_basis(
        4, 0, a, b, log_a, log_b
    )
    mus = np.array([
        np.mean(f(xs)) for f in funs
    ])
    solver0 = lsolver.solver.Solver(
        funs,
        mus,
        a, b
    )
    solver0.set_verbose(True)
    solver0.cs = np.array([
    -16.728496, -8.39528612, 3.2848766, 4.90346965, 6.69105508
    ])
    # solver0.solve(15)

    funs = lsolver.basis.get_cheb_log_basis(
        0, 2, a, b, log_a, log_b
    )
    mus = np.array([
        np.mean(f(xs)) for f in funs
    ])
    solver1 = lsolver.solver.Solver(
        funs,
        mus,
        a, b
    )
    solver1.cs = np.array([
    -2.36835198,-8.20091853,-2.16677827
    ])

    funs = lsolver.basis.get_cheb_log_basis(
        6, 2, a, b, log_a, log_b
    )
    mus = np.array([
        np.mean(f(xs)) for f in funs
    ])
    solver2 = lsolver.solver.Solver(
        funs,
        mus,
        a, b
    )
    solver2.solve(20)
    # solver2.cs = np.array([
    # -13.53540031,-17.78237269,-10.65605666,-7.4026135, -2.80701528,
    #   -8.35571714, -1.47808286
    # ])

    cdf0 = lsolver.cdf.pdf_to_cdf(lambda x: solver0.pdf(x), a)
    cdf1 = lsolver.cdf.pdf_to_cdf(lambda x: solver1.pdf(x), a)
    cdf2 = lsolver.cdf.pdf_to_cdf(lambda x: solver2.pdf(x), a)

    for t in [0.01, .1, 1, 10, 100, 500, 1000, 2000]:
        print("t: {}".format(t))
        print(len(xs[xs < t]) / len(xs))
        print("powers: {}".format(cdf0(t)))
        print("logs: {}".format(cdf1(t)))
        print("both: {}".format(cdf2(t)))


def milan_log():
    df = pd.read_csv("../datasets/internet-mi-2013-11-sampled.csv")
    xs = df["internet"].values.astype(float)
    logxs = np.log(xs)
    a = np.min(xs)
    b = np.max(xs)
    log_a = np.min(logxs)
    log_b = np.max(logxs)

    funs = lsolver.basis.get_cheb_exp_basis(
        6, 4, log_a, log_b, a, b
    )
    mus = np.array([
        np.mean(f(logxs)) for f in funs
    ])
    solver0 = lsolver.solver.Solver(
        funs,
        mus,
        log_a, log_b
    )
    solver0.set_verbose(True)
    solver0.cs = np.array([ 71.61007875, -44.25589953, -55.80201029, -26.49969624,
       -25.40508705,  -7.42522493,  -6.34688246,  89.80248677,
       -16.37731821,   1.17612908,  -3.21962964])

    funs = lsolver.basis.get_cheb_exp_basis(
        6, 0, log_a, log_b, a, b
    )
    mus = np.array([
        np.mean(f(logxs)) for f in funs
    ])
    solver1 = lsolver.solver.Solver(
        funs,
        mus,
        log_a, log_b
    )
    solver1.set_verbose(True)
    solver1.cs = np.array([-6.47241602, -1.1129685 , -4.90454222, -3.09602966, -1.39421737,
       -1.7362422 , -0.54497085])
    # solver1.solve(15)

    cdf0 = lsolver.cdf.pdf_to_cdf(lambda x: solver0.pdf(x), log_a)
    cdf1 = lsolver.cdf.pdf_to_cdf(lambda x: solver1.pdf(x), log_a)

    for t in [0.01, .1, 1, 10, 100, 500, 1000, 2000]:
        print("t: {}".format(t))
        print(len(xs[xs < t]) / len(xs))
        print("p4l6: {}".format(cdf0(math.log(t))))
        print("l6: {}".format(cdf1(math.log(t))))

    xs = np.linspace(log_a, log_b, 1000)
    ys0 = [solver0.pdf(x) for x in xs]
    ys1 = [solver1.pdf(x) for x in xs]
    plt.figure()
    plt.plot(xs, ys0, label="p4l6")
    plt.plot(xs, ys1, label="l6")
    plt.hist(logxs, density=True, bins=100)
    plt.legend()
    plt.show()

    # solver0.solve(15)


def main():
    print("hello")
    milan_log()

if __name__ == "__main__":
    main()
# lpxs = np.linspace(log_a, log_b, 1000)
# ys1 = [solver1.pdf(math.exp(x))*math.exp(x) for x in lpxs]
# ys2 = [solver2.pdf(math.exp(x))*math.exp(x) for x in lpxs]

# xs = np.linspace(a, b, 1000)
# ys1 = [solver1.pdf(x) for x in xs]
# ys2 = [solver2.pdf(x) for x in xs]
# plt.figure()
# plt.plot(xs, ys1, label="no log")
# plt.plot(xs, ys2, label="with log")
# plt.hist(xs, density=True, bins=100, label="density")
# plt.legend()
# plt.show()

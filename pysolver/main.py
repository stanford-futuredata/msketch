import numpy as np
import pandas as pd

import msolver
import preprocess
from numpy.polynomial.chebyshev import Chebyshev
cb = Chebyshev.basis


def calc_errors(ps, q_ests, xs: np.ndarray):
    n = len(ps)
    idxs_l = np.searchsorted(xs, q_ests, side="left")
    idxs_r = np.searchsorted(xs, q_ests, side="right")
    ps_l = idxs_l / (len(xs)-1)
    ps_r = idxs_r / (len(xs)-1)
    delta_l = np.maximum(0, ps_l - ps)
    delta_r = np.maximum(0, ps - ps_r)
    return np.maximum(delta_l, delta_r)


def main():
    dataset_map = {
        "skype": {
            "path": "skype_startup.csv",
            "colname": "Startup_Duration",
            "is_integral": True,
        },
        "sdkstats": {
            "path": "records_received.csv",
            "colname": "records_received_count",
            "is_integral": True
        },
        "sentiment": {
            "path": "sentiment.csv",
            "colname": "Sentiment",
            "is_integral": False,
        },
        "apilogs": {
            "path": "api_logs.csv",
            "colname": "x",
            "is_integral": True,
        },
        "batchprocess": {
            "path": "batch_processed.csv",
            "colname": "AverageMessageDelayMillis",
            "is_integral": True,
        },
        "cube2star": {
            "path": "cube2-star-flat.csv",
            "colname": "x",
            "is_integral": False
        },
        "cube2nostar": {
            "path": "cube2-nonstar-flat.csv",
            "colname": "x",
            "is_integral": False
        },
        "cube31": {
            "path": "cube31-raw.csv",
            "colname": "AvgMuxToOrcaDelayMillis",
            "is_integral": True
        }

    }
    pathprefix = "/Users/egan/Documents/Projects/datasets/msolver2/cleaned/"
    dnames = [
        "skype", "sdkstats",
        "sentiment", "apilogs", "batchprocess",
        "cube2star", "cube2nostar", "cube31"
    ]
    ks = [5, 11, 15]

    dnames = ["cube31"]
    ks = [11]
    ps = np.array([0.001, .01, .1, .5, .9, .99, 0.999])

    result_rows = []
    for di in range(len(dnames)):
        dname = dnames[di]
        print(dname)
        dmap = dataset_map[dname]
        df = pd.read_csv(
            pathprefix+dmap["path"]
        )
        xvals = np.sort(df[dmap["colname"]])
        axs = np.arcsinh(xvals)
        amin = np.min(axs)
        amax = np.max(axs)
        q_true = np.percentile(xvals, ps*100, interpolation="nearest")

        for k in ks:
            print("k: {}".format(k))

            # ts = preprocess.Shifter(compress=True, integral=False)
            # ts.set_xs(xvals, k=k)
            # true_cmus = ts.get_mus()
            # print("true cmus: {}".format(true_cmus))

            axs_powers = np.array([np.sum(axs**i) for i in range(k)])

            s = msolver.MSolver(
                k=k,
                compressed=True,
                integer=dmap["is_integral"],
                verbose=True
            )
            s.solve(psums=axs_powers, amin=amin, amax=amax)
            q_ests = np.array([s.get_quantile(p) for p in ps])
            errors = calc_errors(ps, q_ests, xvals)
            import matplotlib.pyplot as plt
            plt.figure()
            xs = np.linspace(-1, 1, 1000)
            ys = s.csolver.f_poly(xs)
            plt.plot(xs, ys)
            plt.show()
            print(q_true)
            print(q_ests)
            print(errors)
            for pi in range(len(ps)):
                cur_result = {
                    "dname": dname,
                    "k": k,
                    "p": ps[pi],
                    "q_true": q_true[pi],
                    "q_est": q_ests[pi],
                    "error": errors[pi]
                }
                result_rows.append(cur_result)

    df = pd.DataFrame(result_rows)
    df.to_csv("results.csv", index=False)


if __name__ == "__main__":
    main()

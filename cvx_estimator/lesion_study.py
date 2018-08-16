import milan
import exponential
import hepmass
import estimator
import cvx_estimator
import gauss_estimator
import time
import pandas as pd
import numpy
import math

def main():
    ps = numpy.linspace(0, 1, 21)
    ps[0] = 0.01
    ps[-1] = 0.99
    k = 7
    datasets = {
        "milan": milan.data,
        "exponential": exponential.data,
        "hepmass": hepmass.data
    }
    isLog = {
        "milan": True,
        "exponential": False,
        "hepmass": False
    }
    solvers = {
        "lp": cvx_estimator.CvxEstimator(k,1000,solver="lp"),
        "maxent": cvx_estimator.CvxEstimator(k,1000,solver="maxent"),
        "mindensity": cvx_estimator.CvxEstimator(k,1000,solver="mindensity"),
        "gaussian": gauss_estimator.GaussEstimator(k),
    }
    results = []
    for dname in datasets:
        print(dname)
        data = datasets[dname]
        distributions = {}
        num_trials = {
            "lp": 500,
            "maxent": 10,
            "mindensity": 50,
            "gaussian": 1000,
        }
        # num_trials = {
        #     "lp": 10,
        #     "maxent": 1,
        #     "mindensity": 1,
        #     "gaussian": 1,
        # }

        for sname in solvers:
            print(sname)
            e = solvers[sname]
            if isLog[dname]:
                e.set_statistics(
                    data["ranges"][2],
                    data["ranges"][3],
                    data["sLogMoments"][:k]
                )
            else:
                e.set_statistics(
                    data["ranges"][0],
                    data["ranges"][1],
                    data["sMoments"][:k]
                )
            distributions[sname] = e.solve()

            start_time = time.time()
            for i in range(num_trials[sname]):
                e.solve()
            end_time = time.time()

            for p in ps:
                q_est = e.estimate(p)
                if isLog[dname]:
                    q_est = math.exp(q_est)
                results.append({
                    "dataset": dname,
                    "size_param": k,
                    "sketch": sname,
                    "query_time": ((end_time-start_time)/num_trials[sname]) * 1e9,
                    "q": "{0:.3g}".format(p),
                    "quantile_estimate": q_est
                })

    pd.DataFrame(results).to_csv("lesion_results.csv", index=False)

    # import matplotlib.pyplot as plt
    # import numpy as np
    # plt.figure()
    # xs = np.linspace(0, 1, 1000)
    # for sname in solvers:
    #     plt.plot(xs, distributions[sname], label=sname)
    # plt.legend()
    # plt.show()


if __name__ == "__main__":
    main()
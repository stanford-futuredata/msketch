#include <stdio.h>
#include <sys/time.h>
#include <gsl/gsl_vector_double.h>
#include "mlib.h"
#include "util.h"

int benchmark_solve() {
    double d_moments[8] = {1.        , -0.57071218, -0.22603818,  0.68115364, -0.60778341,
                           0.17844626,  0.22018082, -0.29730968};
    gsl_vector *d_mus = gsl_vector_alloc(8);
    gsl_vector *l_init = gsl_vector_alloc(8);
    for (size_t i =0; i < 8; i++) {
        gsl_vector_set(d_mus, i, d_moments[i]);
        gsl_vector_set(l_init, i, 0.0);
    }

    struct timeval start_t, end_t;
    int num_trials = 4000;

    gettimeofday(&start_t, NULL);
    int steps;
    for (int i = 0; i < num_trials; i++) {
        for (size_t i=0; i < 8; i++) {
            gsl_vector_set(l_init, i, 0.0);
        }
        steps = c_solve(d_mus, l_init, 1.0e-8);
    }
    gettimeofday(&end_t, NULL);

    double tot_time = (end_t.tv_sec - start_t.tv_sec) +
                      (end_t.tv_usec - start_t.tv_usec) * 1e-6;
    double op_time = tot_time / num_trials;
    printf("%g seconds\n", op_time);
    printf("%d steps\n", steps);
    print_vec(l_init);
    return 0;
}

int main() {
    benchmark_solve();
}

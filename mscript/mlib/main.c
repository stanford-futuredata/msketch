#include <stdio.h>
#include <sys/time.h>
#include <gsl/gsl_vector_double.h>
#include "mlib.h"
#include "mopt.h"

int test_lib() {
    printf("Testing Lib\n");
    const int k = 3;
    double l_values[3] = {1.0, 2.0, 3.0};
    gsl_vector *l = gsl_vector_alloc(k);
    for (size_t i=0; i < k; i++) {
        gsl_vector_set(l, i, l_values[i]);
    }
    gsl_vector *mu = gsl_vector_alloc(2*k);

    double val = e_pdf(.5, l);
    double expected = 0.06392786;
    printf("PDF %f = %f \n", expected, val);

    e_moments(l, mu, 1e-8);
    double e_moments[6] = {0.10852667, 0.02472456, 0.00943313, 0.004684, 0.0027421, 0.0017955};
    printf("Moments: ");
    for (size_t i=0; i < mu->size; i++) {
        printf("%f=%f,", e_moments[i], gsl_vector_get(mu, i));
    }
    return 0;
}

int c_solve() {
    size_t k = 9;
    double mus[9] = {
            1.        ,  0.21464391,  0.06138914,  0.02388256,  0.01212886,
            0.00737966,  0.00498471,  0.00357036,  0.00264872
    };
    double lambd[9] = {
            2.99311969,  -65.87230371,  298.47541976, -460.92050211,
            237.38141984, 0, 0, 0, 0
    };
    gsl_vector *v = gsl_vector_alloc(k);
    for (size_t i=0; i < k; i++) {
        gsl_vector_set(v, i, lambd[i]);
    }

    struct timeval start_t, end_t;
    int num_trials = 1;

    double f = 0.0;
    gettimeofday(&start_t, NULL);
    for (int i = 0; i < num_trials; i++) {
        e_opt(
                mus,
                v,
                1e-9,
                1e-6,
                20000
        );
    }
    gettimeofday(&end_t, NULL);

    double tot_time = (end_t.tv_sec - start_t.tv_sec) +
            (end_t.tv_usec - start_t.tv_usec) * 1e-6;
    double op_time = tot_time / num_trials;
    printf("%f\n", f);
    for (size_t i = 0; i < k; i++) {
        printf("%f,", gsl_vector_get(v, i));
    }
    printf("\n");
    printf("%g seconds\n", op_time);

    gsl_vector_free(v);
    return 0;
}

int main() {
    test_lib();
}

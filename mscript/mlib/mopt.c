#include <gsl/gsl_multimin.h>
#include <gsl/gsl_integration.h>
#include "mopt.h"

void e_opt(
        double* d_mus,
        const gsl_vector *lambd_out,
        double int_tol,
        double mu_tol,
        int max_iter
) {
    size_t k = lambd_out->size;

    e_fdf_params p = {
            d_mus,
            int_tol
    };

    gsl_multimin_function_fdf F;
    F.n = k;
    F.f = &e_f;
    F.df = &e_df;
    F.fdf = &e_fdf;
    F.params = &p;

    const gsl_multimin_fdfminimizer_type *T = gsl_multimin_fdfminimizer_vector_bfgs2;
    gsl_multimin_fdfminimizer *s = gsl_multimin_fdfminimizer_alloc (T, k);
    gsl_multimin_fdfminimizer_set(s, &F, lambd_out, 0.01, 0.01);

    int iter = 0;
    int status;
    do {
        iter++;
        status = gsl_multimin_fdfminimizer_iterate (s);

        if (status)
            break;

        status = gsl_multimin_test_gradient (s->gradient, mu_tol);

        printf("%5zd", iter);
        for (size_t i =0; i < k; i++) {
            printf(" %.5f", gsl_vector_get(s->x, i));
        }
        printf("%10.5f\n", s->f);

    } while (status == GSL_CONTINUE && iter < max_iter);
    gsl_vector_memcpy(lambd_out, s->x);

    gsl_multimin_fdfminimizer_free (s);
}

void e_fdf(
        const gsl_vector *lambd,
        void *params_raw,
        double *f,
        gsl_vector *df
) {
    e_fdf_params *params = (e_fdf_params*)params_raw;
    size_t k = lambd->size;

    gsl_vector *e_mus = gsl_vector_alloc(k);
    e_moments(lambd, e_mus, params->int_tol);

    double *d_mus = params->d_mus;
    double z = gsl_vector_get(e_mus, 0);
    double f_2 = 0.0;
    for (size_t i = 0; i < k; i++) {
        f_2 += gsl_vector_get(lambd, i) * d_mus[i];
        if (df) {
            gsl_vector_set(df, i, d_mus[i] - gsl_vector_get(e_mus, i));
        }
    }
    if (f) {
        *f = (z - 1) + f_2;
    }

    gsl_vector_free(e_mus);
    return;
}

double e_f (const gsl_vector * x, void * params) {
    double ret_val = 0.0;
    e_fdf(
            x, params, &ret_val, NULL
    );
    return ret_val;
}

void e_df (const gsl_vector * x, void * params, gsl_vector * g) {
    e_fdf(
            x, params, NULL, g
    );
}

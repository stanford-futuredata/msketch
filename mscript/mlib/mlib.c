#include "mlib.h"

#include <math.h>
#include <gsl/gsl_math.h>
#include <gsl/gsl_integration.h>

size_t w_limit = 50;


double e_pdf(
        double x,
        const gsl_vector *lambd
) {
    double x_pow = 1.0;
    double poly_sum = 0.0;
    size_t k = lambd->size;
    for (size_t i = 0; i < k; i++) {
        poly_sum += x_pow * gsl_vector_get(lambd, i);
        x_pow *= x;
    }
    return exp(-poly_sum);
}

void e_moments(
        const gsl_vector *lambd,
        gsl_vector *mus_out,
        const double int_tol
) {
    gsl_integration_workspace *w = gsl_integration_workspace_alloc(w_limit);
    gsl_integration_qaws_table *t = gsl_integration_qaws_table_alloc(0, 0, 0, 0);

    size_t mu_k = mus_out->size;

    gsl_function F;
    F.function = (double (*)(double, void *)) &e_pdf;
    F.params = (void *) lambd;

    int ret_code = 0;
    double abserr = 0.0;
    for (size_t i = 0; i < mu_k; i++) {
        gsl_integration_qaws_table_set(t, i, 0 , 0, 0);
        ret_code |= gsl_integration_qaws(
                &F,
                0.0, // min
                1.0, // max
                t,
                0.0, // epsabs
                int_tol,
                w_limit,
                w,
                gsl_vector_ptr(mus_out, i),
                &abserr
        );
    }

    gsl_integration_qaws_table_free(t);
    gsl_integration_workspace_free(w);
}

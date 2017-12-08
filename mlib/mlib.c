#include "mlib.h"

#include <math.h>
#include <gsl/gsl_math.h>
#include <gsl/gsl_integration.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_linalg.h>
#include <stdbool.h>

#include "util.h"

size_t w_limit = 100;

double c_poly(
        double x,
        const gsl_vector *lambd
) {
    size_t k = lambd->size;
    double sum = 0.0;
    double ts0 = 1.0;
    double ts1 = x;

    sum += gsl_vector_get(lambd, 0);
    if (k > 0) {
        sum += gsl_vector_get(lambd, 1) * x;
        for (size_t i = 2; i < k; i++) {
            double tt1 = ts1;
            ts1 = 2*x*ts1 - ts0;
            ts0 = tt1;
            sum += gsl_vector_get(lambd, i) * ts1;
        }
    }
    return sum;
}

double c_poly_2(
        double x,
        const gsl_vector *lambd
) {
    size_t k = lambd->size-1;
    double bs2 = 0.0;
    double bs1 = 0.0;
    double bs0;
    for (size_t i=0; i<k; i++) {
        bs0 = gsl_vector_get(lambd, k-i) + 2*x*bs1 - bs2;
        bs2 = bs1;
        bs1 = bs0;
    }
    return gsl_vector_get(lambd, 0) + x*bs1 - bs2;
}


double c_monomial(
        double x,
        int k
) {
    if (k == 0) {
        return 1.0;
    } else if (k == 1) {
        return x;
    } else {
        double ts0 = 1.0;
        double ts1 = x;
        double tt1 = 0.0;
        for (int i = 2; i <= k; i++) {
            tt1 = ts1;
            ts1 = 2*x*ts1 - ts0;
            ts0 = tt1;
        }
        return ts1;
    }
}

double c_pdf(
        double x,
        const gsl_vector *lambd
) {
    return exp(-c_poly(x, lambd));
}

double c_weighted_pdf(
        double x,
        struct c_pdf_params *params
) {
    double p1 = c_monomial(x, params->m_idx);
    double p2 = exp(-c_poly(x, params->lambd));
    return p1 * p2;
}

void c_moments(
        const gsl_vector *lambd,
        gsl_vector *mus_out,
        const double int_tol
) {
    gsl_integration_workspace *w = gsl_integration_workspace_alloc(w_limit);

    size_t mu_k = mus_out->size;
    struct c_pdf_params params;
    params.m_idx = 0;
    params.lambd = lambd;

    gsl_function F;
    F.function = (double (*)(double, void *)) &c_weighted_pdf;
    F.params = (void *) &params;

    int ret_code = 0;
    double abserr = 0.0;
    for (int i = 0; i < mu_k; i++) {
        params.m_idx = i;
        ret_code |= gsl_integration_qag(
                &F,
                -1.0, // min
                1.0, // max
                int_tol, // epsabs
                0.0,
                w_limit,
                5,
                w,
                gsl_vector_ptr(mus_out, (size_t)i),
                &abserr
        );
    }

    gsl_integration_workspace_free(w);
}


int c_solve(
        const gsl_vector *d_mus,
        gsl_vector *lambd,
        double m_tol
) {
    size_t k = d_mus->size;
    gsl_vector *e_mus = gsl_vector_alloc(2*k);
    gsl_vector *dl = gsl_vector_alloc(k);
    gsl_matrix *hh = gsl_matrix_alloc(k, k);

    size_t max_iter = 100;
    for (size_t step = 0; step < max_iter; step++) {
        c_moments(lambd, e_mus, m_tol/4);

        bool within_tol = true;
        for (size_t i=0; i < k; i++) {
            double m_del = gsl_vector_get(d_mus, i) - gsl_vector_get(e_mus, i);
            gsl_vector_set(dl,i,m_del);
            if (fabs(m_del) > m_tol) {
                within_tol = false;
            }
        }
        if (within_tol) {
            return step;
        }

        for (size_t i=0; i < k; i++) {
            for (size_t j=0; j < k; j++) {
                double val = .5*(gsl_vector_get(e_mus, i+j) + gsl_vector_get(e_mus, (size_t)abs(i-j)));
                gsl_matrix_set(hh, i, j, val);
            }
        }

        gsl_linalg_HH_svx(hh, dl);

        gsl_vector_sub(lambd, dl);
        if (step > max_iter) {
            return -step;
        }
    }
}
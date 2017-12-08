#include <gsl/gsl_vector.h>

#ifndef MOMENTS_LIBRARY_H
#define MOMENTS_LIBRARY_H

// Public API

double c_pdf(
        double x,
        const gsl_vector *lambd
);
void c_moments(
        const gsl_vector *lambd,
        gsl_vector *mus_out,
        double int_tol
);
int c_solve(
        const gsl_vector *d_mus,
        gsl_vector *lambd,
        double m_tol
);

// Helper Functions

double c_monomial(
        double x,
        int k
);
double c_poly(
        double x,
        const gsl_vector *lambd
);
struct c_pdf_params {
    int m_idx;
    const gsl_vector *lambd;
};
double c_weighted_pdf(
        double x,
        struct c_pdf_params *params
);
double c_poly_2(
        double x,
        const gsl_vector *lambd
);

#endif
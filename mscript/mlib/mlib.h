#include <gsl/gsl_vector.h>

#ifndef MOMENTS_LIBRARY_H
#define MOMENTS_LIBRARY_H

double e_pdf(
        double x,
        const gsl_vector *lambd
);
void e_moments(
        const gsl_vector *lambd,
        gsl_vector *mus_out,
        double int_tol
);

#endif
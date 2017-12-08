#include <gsl/gsl_vector.h>

#include "mlib.h"

#ifndef MOMENTS_MOPT_H
#define MOMENTS_MOPT_H

void e_opt(
        double* d_mus,
        const gsl_vector *lambd_out,
        double int_tol,
        double mu_tol,
        int max_iter
);

typedef struct e_fdf_params {
    double *d_mus;
    double int_tol;
} e_fdf_params;
void e_fdf(
        const gsl_vector *lambd,
        void *params,
        double *f,
        gsl_vector *df
);
double e_f (const gsl_vector * x, void * params);
void e_df (const gsl_vector * x, void * params, gsl_vector * g);


#endif //MOMENTS_MOPT_H


#ifndef MOMENTS_UTIL_H_H
#define MOMENTS_UTIL_H_H

#include <gsl/gsl_vector.h>

void print_vec(
        gsl_vector *vec
);
void set_vec(
        gsl_vector *out,
        double *in,
        size_t k
);

void print_vec(
        gsl_vector *vec
) {
    size_t k = vec->size;
    for (size_t i=0; i < k; i++) {
        printf("%2.6e,", gsl_vector_get(vec, i));
    }
    printf("\n");
}

void set_vec(
        gsl_vector *out,
        double *in,
        const size_t k
) {
    for (size_t i = 0; i < k; i++) {
        gsl_vector_set(out, i, in[i]);
    }
}


#endif //MOMENTS_UTIL_H_H

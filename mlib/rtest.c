#include <math.h>
#include <stdio.h>

#include "mlib.h"
#include "util.h"
#include "greatest.h"

gsl_vector * gen_basic_lambda() {
    const int k = 3;
    double c_values[3] = {1.0, 2.0, 3.0};
    gsl_vector *lambd = gsl_vector_alloc(k);
    set_vec(lambd, c_values, k);
    return lambd;
}

TEST cheby_poly(void) {
    gsl_vector *lambd = gen_basic_lambda();
    double pval = c_poly(.7, lambd);
    ASSERT_IN_RANGE(2.34, pval, 1e-11);

    pval = c_monomial(-.1, 3);
    ASSERT_IN_RANGE(0.296, pval, 1e-11);

    gsl_vector_free(lambd);
    PASS();
}

TEST cheby_moments(void) {
    gsl_vector *lambd = gen_basic_lambda();

    const int mu_k = 4;
    gsl_vector *mu = gsl_vector_alloc(2 * mu_k);
    c_moments(lambd, mu, 1e-11);

    double m_values[8] = {
            6.303954641290793, -1.0395877292934701,
            -4.9297352972133845, 2.0119170973456093,
            2.458369282294647, -1.5127916121976486,
            -0.84272224125321182, 0.73491729283435847
    };
    for (size_t i = 0; i < 8; i++) {
        ASSERT_IN_RANGE(m_values[i], gsl_vector_get(mu, i), 1e-11);
    }

    gsl_vector_free(lambd);
    PASS();
}

static greatest_test_res check_solver(
        const size_t k,
        double *mus,
        const double *true_lambda
) {
    gsl_vector *mu = gsl_vector_alloc(k);
    gsl_vector *lambd = gsl_vector_alloc(k);
    for (size_t i = 0; i < k; i++) {
        gsl_vector_set(lambd, i, 0.0);
        gsl_vector_set(mu, i, mus[i]);
    }

    c_solve(mu, lambd, 1.0e-10);
    for(size_t i = 0; i < k; i++) {
        ASSERT_IN_RANGE(true_lambda[i], gsl_vector_get(lambd, i), 1e-7);
    }
    gsl_vector_free(lambd);
    gsl_vector_free(mu);
    PASS();
}

TEST solve_uniform(void) {
    const size_t k = 7;
    double m_values[7] = {1.0, 0, -1.0/3, 0, -1.0/15, 0, -1.0/35};
    double l_values[7] = {0.0, 0, 0, 0, 0, 0, 0};
    l_values[0] = log(2.0);
    CHECK_CALL(check_solver(
            k, m_values, l_values
    ));

    gsl_vector *l = gsl_vector_alloc(k);
    set_vec(l, l_values, k);
    for (double x=-1.0; x <= 1.0; x+=.1) {
        double fval = c_pdf(x, l);
        ASSERT_IN_RANGE(0.5, fval, 1e-10);
    }
    PASS();
}

GREATEST_MAIN_DEFS();

int main(int argc, char **argv) {
    GREATEST_MAIN_BEGIN();
    RUN_TEST(cheby_poly);
    RUN_TEST(cheby_moments);
    RUN_TEST(solve_uniform);
    GREATEST_MAIN_END();
    return 0;
}


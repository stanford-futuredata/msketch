#include "../include/catch.hh"
#include "../src/Chebyshev.h"

#include <iostream>

TEST_CASE("construct_cheby") {
    double x;
    Chebyshev p = Chebyshev::basis(2);
    x = p.eval(1);
    CHECK(x == Approx(1).epsilon(1e-10));
}
#include "Chebyshev.h"

Chebyshev::Chebyshev(const std::vector<double>& coef): coef(coef) {}

Chebyshev::Chebyshev(std::vector<double>&& coef): coef(coef) {}

double Chebyshev::eval(double x) {
        double bk0=0, bk1=0, bk2=0;
        size_t n = coef.size();
        for (size_t i = n-1; i > 0; i--) {
            bk2=bk1;
            bk1=bk0;
            bk0=coef[i] + 2*x*bk1 - bk2;
        }
        return coef[0] + x*bk0 - bk1;
}

Chebyshev Chebyshev::basis(int n) {
    std::vector<double> coef(n+1);
    coef[n] = 1.0;
    return Chebyshev(coef);
}


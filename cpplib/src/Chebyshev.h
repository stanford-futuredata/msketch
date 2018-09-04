#ifndef CPPLIB_CHEBYSHEV_H
#define CPPLIB_CHEBYSHEV_H

#include <vector>

class Chebyshev {
public:
    Chebyshev() = delete;
    explicit Chebyshev(const std::vector<double>& coef);
    explicit Chebyshev(std::vector<double>&& coef);
    static Chebyshev basis(int n);

    double eval(double x);

    std::vector<double>& getCoefficients() {
        return this->coef;
    }

private:
    std::vector<double> coef;
};


#endif //CPPLIB_CHEBYSHEV_H

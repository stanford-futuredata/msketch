#include "../include/library.h"

#include <iostream>
#include <Eigen/Core>
#include <unsupported/Eigen/FFT>

void hello() {
    std::cout << "Hello, World!" << std::endl;

    size_t size = 5;

    Eigen::VectorXd u = Eigen::VectorXd::Zero(size);
    std::cout << u.transpose() << std::endl;


}
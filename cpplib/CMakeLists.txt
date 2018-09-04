cmake_minimum_required(VERSION 3.2)

project(cpplib)

set(CMAKE_CXX_STANDARD 14)

set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -ffast-math -march=native -DHAVE_INLINE -DGSL_RANGE_CHECK_OFF")


find_package(Eigen3 3.3 REQUIRED NO_MODULE)
include_directories(${EIGEN3_INCLUDE_DIR})

find_package(GSL 2.5 REQUIRED)
include_directories(${GSL_INCLUDE_DIRS})

add_library(cpplib SHARED src/library.cpp src/Chebyshev.cpp)

add_executable(test test/test.cpp test/test_simple.cpp test/test_cheby.cpp)
target_link_libraries(test cpplib Eigen3::Eigen GSL::gsl)

add_executable (main src/main.cpp)
target_link_libraries (main cpplib Eigen3::Eigen GSL::gsl)
#ifndef __EIGEN_TYPES_H__
#define __EIGEN_TYPES_H__

#ifdef random
#undef random
#endif
#include <Eigen/Eigen>

namespace hrp{
    typedef Eigen::Vector2d Vector2;
    typedef Eigen::Vector3d Vector3;
    typedef Eigen::Matrix3d Matrix33;
    typedef Eigen::MatrixXd dmatrix;
    typedef Eigen::VectorXd dvector;
    typedef Eigen::VectorXi ivector;
    typedef Eigen::Matrix<double, 6,1> dvector6;
    typedef Eigen::Quaternion<double> dquaternion;
};

#include <iostream>
inline std::ostream& operator<<(std::ostream& out, hrp::dmatrix &a) {
    const int c = a.rows();
    const int n = a.cols();

    for(int i = 0; i < c; i++){
        out << "      :";
        for(int j = 0; j < n; j++){
            out << " " << std::setw(7) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << (a)(i,j);
        }
        out << std::endl;
    }
}

inline std::ostream& operator<<(std::ostream& out, hrp::dvector &a) {
    const int n = a.size();

    for(int i = 0; i < n; i++){
        out << std::setw(7) << std::setiosflags(std::ios::fixed) << std::setprecision(4) << a(i) << " ";
    }
    out << std::endl;
}

#endif

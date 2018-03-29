#include <gtest/gtest.h>
#include "Eigen3d.h"

using namespace hrp;

#if __cplusplus >= 201103L
using std::isnan;
#endif

TEST(Eigen3d, omegaFromRot)
{
    Matrix33 m;
    Vector3 v;

    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);

    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0+2.0e-12;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);

    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0+2.0e-6;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);

    m = rotFromRpy(0, 0, 0);
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);

    for (int i = 1; i <= 16; i++) {
        for (int j = 1; j <= 16; j++) {
            for (int k = 1; k <= 16; k++) {
                Vector3 rpy(pow(0.1, i), pow(0.1, j), pow(0.1, k));
                m = rotFromRpy(rpy);
                v = omegaFromRot(m);
                EXPECT_FALSE(isnan(v[0]));
                EXPECT_FALSE(isnan(v[1]));
                EXPECT_FALSE(isnan(v[2]));
            }
        }
    }

    for (int i = 0; i <= 180; i++) {
        for (int j = 0; j <= 180; j++) {
            for (int k = 0; k <= 180; k++) {
                Vector3 rpy(i*M_PI/180, j*M_PI/180, k*M_PI/180);
                m = rotFromRpy(rpy);
                v = omegaFromRot(m);
                EXPECT_FALSE(isnan(v[0]));
                EXPECT_FALSE(isnan(v[1]));
                EXPECT_FALSE(isnan(v[2]));
            }
        }
    }
}

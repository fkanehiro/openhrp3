#include <gtest/gtest.h>
#include "Eigen3d.h"

using namespace hrp;

TEST(TestEigen3d, test1)
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
}

/*
TEST(TestEigen3d, test2)
{
    Matrix33 m;
    Vector3 v;
    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0-1.0e-12;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);
}

TEST(TestEigen3d, test3)
{
    Matrix33 m;
    Vector3 v;
    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0+1.0e-12;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);
}

TEST(TestEigen3d, test4)
{
    Matrix33 m;
    Vector3 v;
    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0-1.0e-6;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);
}

TEST(TestEigen3d, test5)
{
    Matrix33 m;
    Vector3 v;
    m << 1.0,   0,   0,
           0, 1.0,   0,
           0,   0, 1.0+1.0e-6;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);
}
*/

TEST(TestEigen3d, test6)
{
    Matrix33 m;
    Vector3 v;
    m <<-1.0,   0,   0,
           0, 1.0,   0,
           0,   0,-1.0;
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], M_PI);
    EXPECT_EQ(v[2], 0);
}

/*
TEST(TestEigen3d, test7)
{
    Matrix33 m;
    Vector3 v;
    m <<-1.0,   0,   0,
           0, 1.0,   0,
           0,   0,-1.0-::std::numeric_limits<double>::epsilon();
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);
}

TEST(TestEigen3d, test8)
{
    Matrix33 m;
    Vector3 v;
    m <<-1.0,   0,   0,
           0, 1.0,   0,
           0,   0,-1.0+::std::numeric_limits<double>::epsilon();
    v = omegaFromRot(m);
    EXPECT_EQ(v[0], 0);
    EXPECT_EQ(v[1], 0);
    EXPECT_EQ(v[2], 0);
}

TEST(TestEigen3d, test9)
{
    Matrix33 m;
    Vector3 v;
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
}
*/

TEST(TestEigen3d, test10)
{
    Matrix33 m;
    Vector3 v;
    for (int i = 0; i <= 90; i++) {
      for (int j = 0; j <= 90; j++) {
        for (int k = 0; k <= 90; k++) {
          double r = i*M_PI/180;
          double p = j*M_PI/180;
          double y = k*M_PI/180;
          m = rotFromRpy(r, p, y);
          v = omegaFromRot(m);
          EXPECT_FALSE(isnan(v[0]));
          EXPECT_FALSE(isnan(v[1]));
          EXPECT_FALSE(isnan(v[2]));
        }
      }
    }
}

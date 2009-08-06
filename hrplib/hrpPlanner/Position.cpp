#include "Position.h"

#ifndef M_PI
#define M_PI 3.14159
#endif

using namespace PathEngine;

double Position::weightX_ = 1.0;
double Position::weightY_ = 1.0;
double Position::weightTh_ = 1.0;

double Position::maxX_ =  2.0;
double Position::maxY_ =  2.0;
double Position::minX_ = -2.0;
double Position::minY_ = -2.0;

Position::Position() {
  x_ = y_ = theta_ = 0;
}

Position::Position(double x, double y, double theta) {
  x_ = x;
  y_ = y;
  theta_ = theta_limit(theta);
}


Position::Position(const Position& pos) {
  x_ = pos.getX();
  y_ = pos.getY();
  theta_ = pos.getTheta();
}

Position Position::random()
{
  double dx = maxX_ - minX_;
  double dy = maxY_ - minY_;
  double x = (rand()/(double)RAND_MAX) * dx + minX_;
  double y = (rand()/(double)RAND_MAX) * dy + minY_;
  double theta = (rand()/(double)RAND_MAX) * 2 * M_PI;
  return Position(x,y,theta);
}

bool Position::isValid() const
{
  if (x_ > maxX_ || x_ < minX_) return false;
  if (y_ > maxY_ || y_ < minY_) return false;
  if (theta_ >= 2*M_PI || theta_ < 0) return false;
  return true;
}

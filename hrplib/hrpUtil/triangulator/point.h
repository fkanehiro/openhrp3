#ifndef __POINT_H__
#define __POINT_H__

#include "vector.h"


namespace geometry
{

template<typename Scalar, size_t Dim>
class Point: public Vector<Scalar,Dim>
{
public:
  Point(): Vector<Scalar,Dim>() {}
  Point(Scalar  s): Vector<Scalar,Dim>(s) {}
  Point(Scalar *a): Vector<Scalar,Dim>(a) {}

#if 0  // intentionally omitted
  Point(const Point& that) {..}
  void operator=(const Point& that) {..}
#endif

};//class Point


template<typename Scalar, size_t Dim>
Point<Scalar,Dim> operator-(const Point<Scalar,Dim>& p)
{
  Point<Scalar,Dim> q = p; q.negate(); return q;
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> operator+(const Point<Scalar,Dim>& p, const Vector<Scalar,Dim>& v)
{
  Point<Scalar,Dim> q = p; q += v; return q;
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> operator*(const Point<Scalar,Dim>& p, Scalar s)
{ 
  Point<Scalar,Dim> q = p; q *= s; return q;
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> operator/(const Point<Scalar,Dim>& p, Scalar s)
{
  Point<Scalar,Dim> q = p; q /= s; return q;
}


template<typename Scalar, size_t Dim> inline
Scalar distance(const Point<Scalar,Dim>& p, const Point<Scalar,Dim>& q) { return (p - q).norm(); }


template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> cyclic_cross_sum(const std::vector< Point<Scalar,Dim> >& points)
{
  typedef Vector<Scalar,Dim> V;
  typedef Point<Scalar,Dim> P;
  V ccs;
  int size = points.size();
  for (int i = 0; i < size; ++i) {
    P a = points[i];
    P b = points[(i + 1) % size]; 	// taken circularly
    ccs += cross(a,b);
  }
  return ccs;
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> cyclic_cross_sum(const Point<Scalar,Dim>& a, const Point<Scalar,Dim>& b, const Point<Scalar,Dim>& c)
{
  //return (cross(a,b) + cross(b,c) + cross(c,a)); 	// definition-faithful
  return cross(b - a, c - a); 				// more efficient
}


template<typename Scalar, size_t Dim> inline
bool colinear(const Point<Scalar,Dim>& p, const Point<Scalar,Dim>& q, const Point<Scalar,Dim>& r)
{
  return parallel<Scalar,Dim>(q - p, r - p);
}

template<typename Scalar, size_t Dim>
bool coplanar(const Point<Scalar,Dim>& p, const Point<Scalar,Dim>& q, const Point<Scalar,Dim>& r, const Point<Scalar,Dim>& s)
{
  Vector<Scalar,Dim> m = cross(q - p, s - p), n = cross(s - r, q - r);
  return parallel<Scalar,Dim>(m,n);
}


}//namespace geometry

#endif //__POINT_H__


#ifndef __LINE_H__
#define __LINE_H__

#include <stdexcept>

#include "point.h"


namespace geometry
{

class ParallelException: public std::runtime_error 
{
public:
  ParallelException(const char* what): std::runtime_error(what) {}
};//class ParallelException

class IntersectionException: public std::runtime_error 
{
public:
  IntersectionException(const char* what): std::runtime_error(what) {}
};//class IntersectionException


template<typename Scalar, size_t Dim>
class Line
{
typedef Vector<Scalar,Dim> VectorType;
typedef Point<Scalar,Dim>  PointType;

public:
  Line(): _ogn(), _dtn() {}

  Line(PointType origin, VectorType direction): _ogn(origin), _dtn(direction) {}

#if 0  // intentionally omitted
  Line(const Line& that): Line(that.origin(), that.direction()) {}
  void operator=(const Line& that) { _ogn = that._ogn; _dtn = that._dtn; }
#endif

  inline
  PointType& origin() { return _ogn; }
  
  inline
  const PointType& origin() const { return _ogn; }

  inline
  VectorType& direction() { return _dtn; }

  inline
  const VectorType& direction() const { return _dtn; }

  PointType point(Scalar s) const { return PointType(_ogn + (_dtn * s)); }

  Scalar coord(PointType p) const
  {
    if (!parallel(p - _ogn, _dtn)) { throw *(new std::invalid_argument("Line::coord(p): p is not on this line")); }
    return measure(p);
  }

  Scalar measure(PointType p) const
  {
    return dot(p - _ogn, _dtn) / _dtn.norm2();
  }

  PointType project(PointType p) const { return point(measure(p)); }

  bool contains(PointType p) const { return parallel(p - _ogn, _dtn); }

private:
  PointType  _ogn;
  VectorType _dtn;

};//class Line


template<typename Scalar, size_t Dim> inline
bool operator==(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b)
{
  return parallel(a.direction(), b.direction()) && a.contains(b.origin());
}

template<typename Scalar, size_t Dim> inline
bool operator!=(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b) { return !(a == b); }

template<typename Scalar, size_t Dim> inline
bool parallel(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b) 
{ 
  return parallel(a.direction(), b.direction()); 
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b) 
{
  return orthogonal(a.direction(), b.direction()); 
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> an_orthogonal_vector_of(const Vector<Scalar,Dim>& v)
{
  Vector<Scalar,Dim> w((Scalar)0.0); w[0] = -v[1]; w[1] = v[0]; return w;
}

template<typename Scalar, size_t Dim>
void get_intersection_coords(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b, Scalar& sa, Scalar& sb)
{
  if (parallel(a,b)) { throw *(new ParallelException("get_intersection_coords(Line& a, Line& b): a and b are parallel")); }
  typedef Vector<Scalar,Dim> V;
  V va = an_orthogonal_vector_of(a.direction());
  V vb = an_orthogonal_vector_of(b.direction());
  sa = dot(b.origin() - a.origin(), vb) / dot(a.direction(), vb);
  sb = dot(a.origin() - b.origin(), va) / dot(b.direction(), va);
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> intersection(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b)
{
  if (parallel(a,b)) { 
    if (a.contains(b.origin())) { // a == b
      throw *(new ParallelException("intersection(Line& a, Line& b): a and b are equal"));
    } else {
      throw *(new ParallelException("intersection(Line& a, Line& b): a and b are parallel")); 
    }
  }
  Scalar sa,sb;
  get_intersection_coords(a,b, sa,sb);
  Point<Scalar,Dim> pa = a.point(sa), pb = b.point(sb);
  if (pa != pb) { throw *(new IntersectionException("intersection(Line& a, Line& b): a and b don't intersect")); }
  return pa;
}

template<typename Scalar, size_t Dim>
bool intersecting(const Line<Scalar,Dim>& a, const Line<Scalar,Dim>& b)
{
  if (parallel(a,b)) { return a.contains(b.origin()); }
  Scalar sa,sb;
  get_intersection_coords(a,b, sa,sb);
  Point<Scalar,Dim> pa = a.point(sa), pb = b.point(sb);
  return (pa == pb);
}

}//namespace geometry

#endif //__LINE_H__


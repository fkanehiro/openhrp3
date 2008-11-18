#ifndef __SEGMENT_H__
#define __SEGMENT_H__

#include "line.h"


namespace geometry
{

template<typename Scalar, size_t Dim>
class Segment
{
typedef Vector<Scalar,Dim> VectorType;
typedef Point<Scalar,Dim>  PointType;
typedef Line<Scalar,Dim>   LineType;

public:
  Segment() {}
  Segment(PointType p, PointType q) { _p[0] = p; _p[1] = q; }
  Segment(PointType points[2]) { _p[0] = points[0]; _p[1] = points[1]; }

#if 0  // intentionally omitted
  Segment(const Segment& that) {..}
  void operator=(const Segment& that) {..}
#endif

  void operator=(const PointType points[2]) { _p[0] = points[0]; _p[1] = points[1]; }

  inline 
  PointType* points() { return _p; }
  inline
  const PointType* points() const { return _p; }

  inline
  operator const PointType*() const { return _p; }

  inline
  PointType* begin() { return _p; }
  inline
  PointType* end()   { return _p + 2; }

  inline
  const PointType* begin() const { return _p; }
  inline
  const PointType* end()   const { return _p + 2; }

  inline
  PointType& operator[](int index) { return _p[index]; }

  inline
  const PointType& operator[](int index) const { return _p[index]; }

  inline
  Scalar length() const { return distance(_p[0], _p[1]); }

  inline
  PointType origin() const { return _p[0]; }

  inline
  VectorType direction() const { return (_p[1] - _p[0]); }

  inline
  bool intersects(const LineType& line) const { return intersecting(*this, line); }

  inline
  bool intersects(const Segment& that) const { return intersecting(*this, that); }

  bool contains(const PointType& p) const
  {
    LineType line = this->as_Line();
    if (!line.contains(p)) { return false; }
    return coord_valid(line.coord(p));
  }

  inline
  bool contains(const Segment& that) const { return contains(that[0]) && contains(that[1]); }

  inline
  bool contained_in(const LineType& line) const { return line.contains(_p[0]) && line.contains(_p[1]); }

  inline
  bool contained_in(const Segment& that) const { return that.contains(*this); }

  inline
  bool touches(const PointType& p) const { return (p == _p[0]) || (p == _p[1]); }

  bool touches(const LineType& line) const
  {
    bool b0 = line.contains(_p[0]);
    bool b1 = line.contains(_p[1]);
    return (b0 || b1) && !(b0 && b1);
  }

  bool touches(const Segment& that) const
  {
    bool b0 = that.contains(_p[0]);
    bool b1 = that.contains(_p[1]);
    return (b0 || b1) && !(b0 && b1);
  }

  inline
  LineType as_Line() const { return LineType(origin(), direction()); }

  inline
  static bool coord_valid(Scalar s) { return ((Scalar)0.0 <= s) && (s <= (Scalar)1.0); }

private:
  PointType _p[2];

};//class Segment


template<typename Scalar, size_t Dim>
bool operator==(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b)
{
  return ((a[0] == b[0]) && (a[1] == b[1])) || ((a[0] == b[1]) && (a[1] == b[0]));
}

template<typename Scalar, size_t Dim> inline
bool operator!=(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b) { return !(a == b); }


template<typename Scalar, size_t Dim> inline
bool parallel(const Segment<Scalar,Dim>& sgmt, const Line<Scalar,Dim>& line)
{
  return parallel(sgmt.direction(), line.direction());
}

template<typename Scalar, size_t Dim> inline
bool parallel(const Line<Scalar,Dim>& line, const Segment<Scalar,Dim>& sgmt)
{
  return parallel(sgmt, line);
}

template<typename Scalar, size_t Dim> inline
bool parallel(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b)
{
  return parallel(a.direction(), b.direction());
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Segment<Scalar,Dim>& sgmt, const Line<Scalar,Dim>& line)
{
  return orthogonal(sgmt.direction(), line.direction());
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Line<Scalar,Dim>& line, const Segment<Scalar,Dim>& sgmt)
{
  return orthogonal(sgmt, line);
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b)
{
  return orthogonal(a.direction(), b.direction());
}


template<typename Scalar, size_t Dim>
Point<Scalar,Dim> intersection(const Segment<Scalar,Dim>& sgmt, const Line<Scalar,Dim>& line)
{
  if (sgmt.contained_in(line)) {
    throw *(new ParallelException("intersection(Segment& sgmt, Line& line): sgmt contained in line"));
  }
  if (parallel(sgmt, line)) {
    throw *(new ParallelException("intersection(Segment& sgmt, Line& line): sgmt and line are parallel"));
  }
  Line<Scalar,Dim> line0 = sgmt.as_Line();
  Scalar c0, c;
  get_intersection_coords(line0, line, c0, c);
  Point<Scalar,Dim> p0 = line0.point(c0), p = line.point(c);
  if (p0 != p) {
    throw *(new IntersectionException("intersection(Segment& sgmt, Line& line): sgmt(as Line) and line don't intersect"));
  }
  if (!Segment<Scalar,Dim>::coord_valid(c0)) {
    throw *(new IntersectionException("intersection(Segment& sgmt, Line& line): sgmt and line don't intersect"));
  }
  return p0;
}

template<typename Scalar, size_t Dim> inline
Point<Scalar,Dim> intersection(const Line<Scalar,Dim>& line, const Segment<Scalar,Dim>& sgmt)
{
  return intersection(sgmt, line);
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> intersection(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b)
{
  if (parallel(a,b)) {
    if (a == b) { throw *(new ParallelException("intersection(Segment& a,Segment& b): a and b are equal")); }
    if (a.contains(b)) { throw *(new ParallelException("intersection(Segment& a,Segment& b): a contains b")); }
    if (b.contains(a)) { throw *(new ParallelException("intersection(Segment& a,Segment& b): b contains a")); }
    throw *(new ParallelException("intersection(Segment& a,Segment& b): a and b are parallel")); 
  }
  Line<Scalar,Dim> la = a.as_Line(), lb = b.as_Line();
  Scalar sa,sb;
  get_intersection_coords(la,lb, sa,sb);
  Point<Scalar,Dim> pa = la.point(sa), pb = lb.point(sb);
  if (pa != pb) {
    throw *(new IntersectionException("intersection(Segment& a, Segment& b): a(as Line) and b(as Line) don't intersect"));
  }
  if (!Segment<Scalar,Dim>::coord_valid(sa)) {
    throw *(new IntersectionException("intersection(Segment& a, Segment& b): a and b(as Line) don't intersect"));
  }
  if (!Segment<Scalar,Dim>::coord_valid(sb)) {
    throw *(new IntersectionException("intersection(Segment& a, Segment& b): a(as Line) and b don't intersect"));
  }
  return pa;
}

template<typename Scalar, size_t Dim>
bool intersecting(const Segment<Scalar,Dim>& sgmt, const Line<Scalar,Dim>& line)
{
  Line<Scalar,Dim> line0 = sgmt.as_Line();
  if (line0 == line) { return true; }
  if (parallel(line0, line)) { return false; }
  Scalar c0, c;
  get_intersection_coords(line0, line, c0, c);
  return Segment<Scalar,Dim>::coord_valid(c0) && (line0.point(c0) == line.point(c));
}

template<typename Scalar, size_t Dim> inline
bool intersecting(const Line<Scalar,Dim>& line, const Segment<Scalar,Dim>& sgmt)
{
  return intersecting(sgmt, line);
}

template<typename Scalar, size_t Dim>
bool intersecting(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b)
{
  typedef Line<Scalar,Dim> LineType;
  typedef Segment<Scalar,Dim> SegmentType;
  if (parallel(a,b))
  {
    return a.contains(b[0]) || a.contains(b[1]) || b.contains(a[0]) || b.contains(a[1]);
  }
  LineType la = a.as_Line(), lb = b.as_Line();
  if (!intersecting(la,lb)) { return false; }
  Scalar sa,sb;
  get_intersection_coords(la,lb, sa,sb);
  return SegmentType::coord_valid(sa) && SegmentType::coord_valid(sb) && (la.point(sa) == lb.point(sb));
}


template<typename Scalar, size_t Dim> inline
bool touching(const Segment<Scalar,Dim>& s, const Point<Scalar,Dim>& p) { return s.touches(p); }

template<typename Scalar, size_t Dim> inline
bool touching(const Point<Scalar,Dim> p, const Segment<Scalar,Dim>& s) { return touching(s,p); }

template<typename Scalar, size_t Dim> inline
bool touching(const Segment<Scalar,Dim>& sgmt, const Line<Scalar,Dim>& line) { return sgmt.touches(line); }

template<typename Scalar, size_t Dim> inline
bool touching(const Line<Scalar,Dim>& line, const Segment<Scalar,Dim>& sgmt) { return touching(sgmt, line); }

template<typename Scalar, size_t Dim> inline
bool touching(const Segment<Scalar,Dim>& a, const Segment<Scalar,Dim>& b)
{
  return a.touches(b) || b.touches(a);
}


}//namespace geometry

#endif //__SEGMENT_H__


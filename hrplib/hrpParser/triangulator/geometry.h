#ifndef OPENHRP_GEOMETRY_H_INCLUDED
#define OPENHRP_GEOMETRY_H_INCLUDED

#include <cmath>
#include <algorithm>
#include <functional>
#include <numeric>
#include <vector>
#include <map>
#include <stdexcept>


namespace openhrp {

//region gutil
namespace geometry {

template<typename Predicate, typename Iterator>
bool some_true(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (predicate(*p)) { return true; }
  }
  return false;
}

template<typename Predicate, typename Iterator>
bool some_false(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (!predicate(*p)) { return true; }
  }
  return false;
}

template<typename Predicate, typename Iterator>
bool all_false(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (predicate(*p)) { return false; }
  }
  return true;
}

template<typename Predicate, typename Iterator>
bool all_true(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (!predicate(*p)) { return false; }
  }
  return true;
}

enum CompareMode {Lenient, Strict};

template<typename T>
bool ascending(const T& a, const T& b, const T& c, CompareMode mode)
{
  return (mode == Strict) ? ((a < b) && (b < c)) : ((a <= b) && (b <= c));
}

template<typename T>
bool descending(const T& a, const T& b, const T& c, CompareMode mode)
{
  return ascending(c, b, a, mode);
}

template<typename T>
int sign(const T& t)
{
  if (t < (T)0) { return -1; }
  if (t > (T)0) { return  1; }
  return 0;
}

}//namespace geometry
//endregion gutil


//region vector
namespace geometry {

template<typename Scalar, size_t Dim>
class Vector
{
public:
  Vector(): _dim(Dim) { std::fill(_data, _data + Dim, (Scalar)0.0); }
  Vector(Scalar  s): _dim(Dim) { std::fill(_data, _data + Dim, s); }
  Vector(Scalar* a): _dim(Dim) { std::copy(a, a + Dim, _data); }

#if 0  // intentionally omitted
  Vector(const Vector& that) {..}
  Vector& operator=(const Vector& that) {..}
#endif

  inline
  size_t dimension() const { return _dim; } 

  inline
  Scalar* data() { return _data; }

  inline
  const Scalar* data() const { return _data; }

  inline
  operator Scalar*() { return _data; }

  inline
  operator const Scalar*() const { return _data; }

  inline
  Scalar& operator[](int index) { return _data[index]; }

  inline 
  const Scalar& operator[](int index) const { return _data[index]; }

  inline
  Scalar* begin() { return _data; }
  inline
  Scalar* end()   { return _data + Dim; }

  inline
  const Scalar* begin() const { return _data; }
  inline
  const Scalar* end()   const { return _data + Dim; }

  void shift(int d)
  {
    if (d < 0) { d = Dim - (-d % Dim); }
    d %= Dim;
    if (d == 0) { return; }
    std::rotate(begin(), end() - d, end());
  }

  inline
  void negate() 
  { 
    std::transform(begin(), end(), begin(), std::negate<Scalar>()); 
  }

  inline
  void operator+=(Vector that)
  {
    std::transform(begin(), end(), that.begin(), begin(), std::plus<Scalar>());
  }

  inline
  void operator-=(Vector that)
  {
    std::transform(begin(), end(), that.begin(), begin(), std::minus<Scalar>());
  }

  inline
  void operator*=(Scalar s) 
  {
    std::transform(begin(), end(), begin(), std::bind2nd(std::multiplies<Scalar>(), s));
  }

  inline
  void operator/=(Scalar s)
  {
    std::transform(begin(), end(), begin(), std::bind2nd(std::divides<Scalar>(), s));
  }

  Scalar norm1() const
  {
    Scalar data[Dim];
    std::transform(begin(), end(), data, abs);
    return sum(data);
  }

  inline
  Scalar norm2() const { return dot(_data, _data); }

  inline
  Scalar norm() const { return std::sqrt(norm2()); }

  static Scalar AlmostZero;

  inline
  static bool is_zero(Scalar s) { return abs(s) <= AlmostZero; }

  inline
  static bool is_zero(const Vector& v) { return is_zero(v.norm1()); }

protected:
  inline
  static Scalar abs(Scalar s) { return (s >= (Scalar)0.0) ? s : -s; }

  inline
  static Scalar sum(Scalar a[Dim]) { return std::accumulate(a, a + Dim, (Scalar)0.0); }

  inline
  static Scalar dot(const Scalar a[Dim], const Scalar b[Dim]) 
  { 
    return std::inner_product(a, a + Dim, b, (Scalar)0.0);
  }

private:
  size_t _dim;
  Scalar _data[Dim];

};//class Vector


template<typename Scalar, size_t Dim>
Scalar Vector<Scalar,Dim>::AlmostZero = (Scalar)1.0e-10;

template<typename Scalar, size_t Dim> inline
bool operator==(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b) 
{
  return (&a == &b) || Vector<Scalar,Dim>::is_zero((a - b).norm1());
}

template<typename Scalar, size_t Dim> inline
bool operator!=(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b) { return !(a == b); }

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator-(const Vector<Scalar,Dim>& v)
{
  Vector<Scalar,Dim> w = v; w.negate(); return w;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator+(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  Vector<Scalar,Dim> c = a; c += b; return c;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator-(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  Vector<Scalar,Dim> c = a; c -= b; return c;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator*(const Vector<Scalar,Dim>& v, Scalar s) 
{ 
  Vector<Scalar,Dim> w = v; w *= s; return w;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator/(const Vector<Scalar,Dim>& v, Scalar s)
{
  Vector<Scalar,Dim> w = v; w /= s; return w;
}

template<typename Scalar, size_t Dim>
Scalar dot(const Vector<Scalar,Dim>& ca, const Vector<Scalar,Dim>& cb)
{
  typedef Vector<Scalar,Dim> VectorType;
  VectorType& a = const_cast<VectorType&>(ca);
  VectorType& b = const_cast<VectorType&>(cb);
  return std::inner_product(a.begin(), a.end(), b.begin(), (Scalar)0.0);
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> cross(const Vector<Scalar,Dim>& ca, const Vector<Scalar,Dim>& cb)
{
  typedef Vector<Scalar,Dim> VectorType;
  VectorType& a = const_cast<VectorType&>(ca);
  VectorType& b = const_cast<VectorType&>(cb);
  Scalar c[Dim];
  for (int i = 0; i < Dim; i++) {
    int m = (i + 1) % Dim;
    int n = (i + 2) % Dim;
    c[i] = (a[m] * b[n]) - (a[n] * b[m]);
  }
  return Vector<Scalar,Dim>(c);
}

template<typename Scalar, size_t Dim> inline
bool parallel(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  return Vector<Scalar,Dim>::is_zero(cross(a,b));
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{ 
  return Vector<Scalar,Dim>::is_zero(dot(a,b));
}

}//namespace geometry
//endregion vector


//region point
namespace geometry {

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
//endregion point


//region line
namespace geometry {

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
//endregion line


//region segment
namespace geometry {

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
//endregion segment


//region triangle
namespace geometry {

template<typename Scalar, size_t Dim>
class Triangle
{
public:
  typedef Vector<Scalar,Dim>  VectorType;
  typedef Point<Scalar,Dim>   PointType;
  typedef Line<Scalar,Dim>    LineType;
  typedef Segment<Scalar,Dim> SegmentType;

  class Frame
  {
  public:
    Frame(const PointType* vertices, const SegmentType* edges)
    {
      for (int i = 0; i < 3; i++) { _axes[i] = axis(vertices[i], edges[i]); }
    }

    inline
    const LineType* axes() const { return _axes; }

    inline
    const LineType& axis(int index) { return _axes[index]; }

    inline
    const LineType& operator[](int index) { return _axes[index]; }

    void get_measures(const PointType& p, Scalar measures[3]) const
    {
      for (int i = 0; i < 3; i++) { measures[i] = _axes[i].measure(p); }
    }

  protected:
    static LineType axis(const PointType& vertex, const SegmentType& edge)
    {
      LineType el = edge.as_Line();
      PointType p = el.project(vertex);
      return LineType(p, vertex - p);
    }

  private:
    LineType _axes[3];

  };//class Frame

public:
  Triangle()
  {
    PointType zero((Scalar)0.0);
    PointType vertices[] = {zero, zero, zero};
    *this = vertices;
  }

  Triangle(PointType vertices[3]) { *this = vertices; }

  Triangle(PointType p, PointType q, PointType r) { PointType vertices[] = {p,q,r}; *this = vertices; }

#if 0 // intentionally omitted
  Triangle(const Triangle& that) {..}
  const Triangle& operator=(const Triangle& that) {..}
#endif

  Triangle& operator=(PointType vertices[3])
  {
    for (int i = 0; i < 3; i++) {
      _v[i] = vertices[i];
      _e[i] = SegmentType(vertices[(i+1) % 3], vertices[(i+2) % 3]);
    }
    return *this;
  }

  inline
  const PointType* vertices() const { return _v; }

  inline
  const PointType& vertex(int index) const { return _v[index]; }

  inline
  const PointType& operator[](int index) const { return _v[index]; }

  inline
  const SegmentType* edges() const { return _e; }

  inline
  const SegmentType& edge(int index) const { return _e[index]; }

  inline
  VectorType normal() const { return cross(_v[1] - _v[0], _v[2] - _v[0]); }

  inline
  bool intersects(const LineType& line) const { return intersecting(*this, line); }

  inline
  bool intersects(const SegmentType& sgmt) const { return intersecting(*this, sgmt); }

  bool contains(const PointType& p) const
  {
    Frame f(_v, _e);
    Scalar m[3];
    f.get_measures(p, m);
    for (int i = 0; i < 3; i++) {
      if (!ascending((Scalar)0.0, m[i], (Scalar)1.0, Lenient)) { return false; }
    }
    return true;
  }

  bool contains(const SegmentType& csgmt) const
  {
    SegmentType& sgmt = const_cast<SegmentType&>(csgmt);
    return contains(sgmt[0]) && contains(sgmt[1]);
  }

  bool contains(const Triangle& cthat) const
  {
    //return all_true(ContainsPoint(), that.vertices().begin(), that.vertices().end());
    Triangle& that = const_cast<Triangle&>(cthat);
    for (int i = 0; i < 3; i++) {
      if (!contains(that.vertex(i))) { return false; }
    }
    return true;
  }

  //bool contained_in(const PlaneType& plane) const {}

  inline
  bool contained_in(const Triangle& that) const { return that.contains(*this); }

  bool touches(const PointType& p) const
  {
    //return some_true(TouchingPoint(p), _e, _e + 3);
    for (int i = 0; i < 3; i++) {
      if (_e[i].touches(p)) { return true; }
    }
    return false;
  }

  bool touches(const LineType& line) const
  {
    //return some_true(bind(touching, _1, line), _e, _e + 3);
    for (int i = 0; i < 3; i++) {
      if (touching(_e[i], line)) { return true; }
    }
    return false;
  }

  bool touches(const SegmentType& sgmt) const
  {
    //return some_true(TouchingSegment(sgmt), _e, _e + 3);
    for (int i = 0; i < 3; i++) {
      if (touching(_e[i], sgmt)) { return true; }
    }
    return false;
  }

  //bool touches(const Triangle& that) const { return false; }

private:
  PointType 	_v[3]; //vertices
  SegmentType 	_e[3]; //edges

};//class Triangle


template<typename Scalar, size_t Dim>
bool operator==(const Triangle<Scalar,Dim>& a, const Triangle<Scalar,Dim>& b)
{
  return ((a[0] == b[0]) && (a[1] == b[1]) && (a[2] == b[2]))
       || ((a[0] == b[1]) && (a[1] == b[2]) && (a[2] == b[0]))
       || ((a[0] == b[2]) && (a[1] == b[0]) && (a[2] == b[1]));
}

template<typename Scalar, size_t Dim> inline
bool operator!=(const Triangle<Scalar,Dim>& a, const Triangle<Scalar,Dim>& b) { return !(a == b); }


template<typename Scalar, size_t Dim> inline
bool parallel(const Triangle<Scalar,Dim>& tg, const Line<Scalar,Dim>& line)
{
  return orthogonal(tg.normal(), line);
}

template<typename Scalar, size_t Dim> inline
bool parallel(const Triangle<Scalar,Dim>& tg, const Segment<Scalar,Dim>& sgmt)
{
  return orthogonal(tg.normal(), sgmt);
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Triangle<Scalar,Dim>& tg, const Line<Scalar,Dim>& line)
{
  return parallel(tg.normal(), line);
}
template<typename Scalar, size_t Dim> inline
bool orthogonal(const Triangle<Scalar,Dim>& tg, const Segment<Scalar,Dim>& sgmt)
{
  return parallel(tg.normal(), sgmt);
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> intersection(const Triangle<Scalar,Dim>& tg, const Line<Scalar,Dim>& line)
{
  throw *(new std::runtime_error("intersection(Triangle&, Line&): N.Y.I."));
}

template<typename Scalar, size_t Dim>
Point<Scalar,Dim> intersection(const Triangle<Scalar,Dim>& tg, const Segment<Scalar,Dim>& sgmt)
{
  throw *(new std::runtime_error("intersection(Triangle&, Segment&): N.Y.I."));
}

template<typename Scalar, size_t Dim> inline
bool intersecting(const Triangle<Scalar,Dim>& tg, const Line<Scalar,Dim>& line)
{
  return tg.intersects(line);
}

template<typename Scalar, size_t Dim> inline
bool intersecting(const Triangle<Scalar,Dim>& tg, const Segment<Scalar,Dim>& sgmt) { return tg.intersects(sgmt); }


template<typename Scalar, size_t Dim>
bool intersecting(const Triangle<Scalar,Dim>& a, const Triangle<Scalar,Dim>& b)
{
  throw *(new std::runtime_error("intersecting(Triangle&, Triangle&): N.Y.I."));
}

template<typename Scalar, size_t Dim> inline
bool touching(const Triangle<Scalar,Dim>& tg, const Point<Scalar,Dim>& pt) { return tg.touches(pt); }

template<typename Scalar, size_t Dim> inline
bool touching(const Triangle<Scalar,Dim>& tg, const Line<Scalar,Dim>& line) { return tg.touches(line); }

template<typename Scalar, size_t Dim> inline
bool touching(const Triangle<Scalar,Dim>& tg, const Segment<Scalar,Dim>& sgmt) { return tg.touches(sgmt); }

template<typename Scalar, size_t Dim> inline
bool touching(const Triangle<Scalar,Dim>& a, const Triangle<Scalar,Dim>& b) { return a.touches(b) || b.touches(a); }

}//namespace geometry
//endregion triangle


//region polygon
namespace geometry {

template<typename Scalar>
class Vertex
{
public:
  typedef Vector<Scalar,3> VectorType;
  typedef Point<Scalar,3> PointType;
  typedef Segment<Scalar,3> SegmentType;
  typedef Triangle<Scalar,3> TriangleType;

  typedef std::unary_function<const Vertex&, bool> Predicate;

  class AnyVertex: Predicate
  {
  public:
    AnyVertex() {}
    inline
    bool operator()(const Vertex& v) { return true; }
  };//class AnyVertex

  class WithPoint: Predicate
  {
  public:
    WithPoint(const PointType& target): _target(target) {}
    inline
    bool operator()(const Vertex& vtx) { return (vtx.point() == _target); }
  private:
    PointType _target;
  };//class WithPoint

  class WithConvexity: Predicate
  {
  public:
    WithConvexity(const VectorType& normal, int value): _normal(normal), _value(value) {}
    inline
    bool operator()(const Vertex& vtx) { return (vtx.convexity(_normal) == _value); }
  private:
    VectorType _normal;
    int _value;
  };//class WithConvexity

public:
  Vertex(): _point(), _prev(NULL), _next(NULL) {}

  Vertex(const PointType& p): _point(p), _prev(NULL), _next(NULL) {}

  inline
  PointType& point() { return _point; }

  inline
  const PointType& point() const { return _point; }

  inline
  operator PointType() { return _point; }

  Vertex& operator=(const PointType& p) { _point = p; return *this; }

  inline
  Vertex* prev() { return _prev; }
  inline
  Vertex* next() { return _next; }

  inline
  const Vertex* prev() const { return _prev; }
  inline
  const Vertex* next() const { return _next; }

  void get_incident_vertices(PointType vertices[2]) const
  { 
    check_link("Vertex::get_incident_vertices(): null link");
    vertices[0] = _prev->_point;
    vertices[1] = _next->_point;
  }

  void get_incident_edges(SegmentType edges[2]) const
  {
    check_link("Vertex::get_incident_edges(): null link");
    edges[0] = SegmentType(_prev->_point, _point);
    edges[1] = SegmentType(_point, _next->_point);
  }

  int convexity(const VectorType& normal) const
  {
    check_link("Vertex::convexity(): null link");
    VectorType pv = _point - _prev->_point;
    VectorType nv = _next->_point - _point;
    return sign(dot(normal, cross(pv,nv)));
  }

  //double angle() {}

  TriangleType triangle() const
  {
    check_link("Vertex::triangle(): null link");
    return TriangleType(_prev->_point, _point, _next->_point);
  }

  static void link(Vertex& a, Vertex& b) { a._next = &b; b._prev = &a; }
  static void link(Vertex* a, Vertex* b) { a->_next = b; b->_prev = a; }

  static void link(Vertex& a, Vertex& b, Vertex& c) { link(a,b); link(b,c); }
  static void link(Vertex* a, Vertex* b, Vertex* c) { link(a,b); link(b,c); }

protected:
  void check_link(const char* errmsg) const
  {
    if (!(_prev && _next)) { throw *(new std::runtime_error(errmsg)); }
  }

private:
  PointType _point;
  Vertex* _prev;
  Vertex* _next;

};//class Vertex


template<typename Scalar>
class Polygon
{
public:
  typedef Vector<Scalar,3> VectorType;
  typedef Point<Scalar,3> PointType;
  typedef Vertex<Scalar> VertexType;

public:
  Polygon(const VectorType& normal): _normal(normal), _size(0), _head(NULL) {}

  ~Polygon() { try { clear(); } catch(...) {} }

  inline
  VectorType& normal() { return _normal; }

  inline
  const VectorType& normal() const { return _normal; }

  inline
  size_t size() const { return _size; }

  template<typename Predicate>
  size_t count(Predicate predicate) const
  {
    if (_size == 0) { return 0; }
    size_t c = 0;
    VertexType* p = _head;
    while (true) {
      if (predicate(*p)) { c++; }
      p = p->next();
      if (p == _head) { break; }
    }
    return c;
  }

  template<typename Predicate>
  void detect(Predicate predicate, std::vector<int>& indices) const
  {
    if (_size == 0) { return; }
    VertexType* p = _head;
    int index = 0;
    while (true) {
      if (predicate(*p)) { indices.push_back(index); }
      p = p->next();
      if (p == _head) { break; }
      index++;
    }
  }

  void collect(std::vector<VertexType*>& vertices) const // { collect(AnyVertex(), vertices); }
  {
    if (_size == 0) { return; }
    VertexType* p = _head;
    while (true) {
      vertices.push_back(p);
      p = p->next();
      if (p == _head) { break; }
    }
  }

  template<typename Predicate>
  void collect(Predicate predicate, std::vector<VertexType*>& vertices) const
  {
    if (_size == 0) { return; }
    VertexType* p = _head;
    while (true) {
      if (predicate(*p)) { vertices.push_back(p); }
      p = p->next();
      if (p == _head) { break; }
    }
  }

  inline
  VertexType* head() { return _head; }
  inline
  VertexType* tail() { return _head->prev(); }

  inline
  const VertexType* head() const { return _head; }
  inline
  const VertexType* tail() const { return _head->prev(); }

  inline
  bool contains(const VertexType& v) const { return contains(&v); }

  bool contains(const VertexType* vp) const
  {
    if (_size == 0) { return false; }
    VertexType* p = _head;
    while (true) {
      if (p == vp) { return true; }
      p = p->next();
      if (p == _head) { break; }
    }
    return false;
  }

  void clear()
  {
    if (_size == 0) { return; }
    VertexType* p = _head;
    while (true) {
      VertexType* q = p->next();
      delete p;
      if (q == _head) { break; }
      p = q;
    }
    _head = NULL;
    _size = 0;
  }

  void add(const PointType& p)
  {
    VertexType* vp = new VertexType(p);
    if (!vp) { throw *(new std::runtime_error("Polygon::add(Point): failed in newing Vertex")); }
    if (_size == 0) {
      VertexType::link(vp,vp);
      _head = vp;
      _size = 1;
    } else {
      VertexType::link(_head->prev(), vp, _head);
      _size++;
    }
  }

  void remove(VertexType& v) { remove(&v); }

  void remove(VertexType* vp) { if (contains(vp)) { _remove(vp); } }

  template<typename Predicate>
  void remove(Predicate predicate)
  {
    if (_size == 0) { return; }
    VertexType* p = _head;
    while (_size > 0) {
      VertexType* q = p->next();
      if (predicate(*p)) { _remove(p); }
      p = q;
      if (p == _head) { break; }
    }
  }

  void remove(const PointType& p) { remove(VertexType::WithPoint(p)); }

  void collect_convex(std::vector<VertexType*>& vertices) const
  {
    typedef typename VertexType::WithConvexity WithConvexity;
    collect(WithConvexity(_normal, 1), vertices);
  }

  void collect_nonconvex(std::vector<VertexType*>& vertices) const
  {
    typedef typename VertexType::WithConvexity WithConvexity;
    collect(WithConvexity(_normal, -1), vertices);
  }

protected:
  // void _remove(VertexType* vp): low-level method, assumes this->contains(vp)
  void _remove(VertexType* vp)
  {
    if (_size == 0) { return; }
    if (_size == 1) {
      _head = NULL;
      _size = 0;
    } else {
      VertexType::link(vp->prev(), vp->next());
      if (vp == _head) { _head = vp->next(); }
      _size--;
    }
    delete vp;
  }

private:
  size_t _size;
  VertexType* _head;
  VectorType _normal;

};//class Polygon

}//namespace geometry
//endregion polygon


//region ear
namespace geometry {

template<typename Scalar>
class IsEar: public Vertex<Scalar>::Predicate
{
public:
  typedef Vertex<Scalar> VertexType;
  typedef Polygon<Scalar> PolygonType;

  typedef typename VertexType::VectorType VectorType;
  typedef typename VertexType::PointType PointType;
  typedef typename VertexType::TriangleType TriangleType;

  typedef VertexType* VertexPtr;
  typedef std::vector<VertexPtr> VtxPtrList;

  class WithConvexity: VertexType::Predicate
  {
  public:
    WithConvexity(const VectorType& normal, int value): _normal(normal), _value(value) {}
    bool operator()(const VertexType& vtx)
    {  
      return (vtx.convexity(_normal) == _value);
    }
  private:
    VectorType _normal;
    int _value;
  };//class WithConvexity

public:
  IsEar(PolygonType& polygon): _pgn(polygon) {}

#ifdef DEBUG
  bool operator()(const VertexType& vertex)
  {
    VertexType& vtx = const_cast<VertexType&>(vertex);
    std::clog << "IsEar::operator()(Vertex).." << std::endl;
    std::clog << "  candidate vertex: " << vtx << std::endl;
    if (vtx.convexity(_pgn.normal()) != 1) { 
      std::clog << "  this is non-convex.." << std::endl;
      return false;
    }
    TriangleType candidate_triangle = vtx.triangle();
    std::clog << "  candidate triangle: " << candidate_triangle << std::endl;
    VtxPtrList penetrable;
    get_penetrable_vertices(&vtx, penetrable);
    std::clog << "  penetrable vertices: " << penetrable << std::endl;
    for (int i = 0; i < penetrable.size(); i++) {
      VertexType& pv = *(penetrable[i]);
      PointType pp = pv.point();
      std::clog << "    checking penetration by " << pp << " .. ";
      bool penetrated = candidate_triangle.contains(pp);
      std::clog << (penetrated ? "NG" : "OK") << std::endl;
      if (penetrated) { return false; }
    }
    std::clog << "  ** this IS an ear!" << std::endl;
    return true;
  }
#else
  bool operator()(const VertexType& vertex)
  {
    VertexType& vtx = const_cast<VertexType&>(vertex);
    if (vtx.convexity(_pgn.normal()) != 1) { return false; }
    TriangleType candidate_triangle = vtx.triangle();
    VtxPtrList penetrable;
    get_penetrable_vertices(&vtx, penetrable);
    for (int i = 0; i < penetrable.size(); i++) {
      VertexType& pv = *(penetrable[i]);
      PointType pp = pv.point();
      bool penetrated = candidate_triangle.contains(pp);
      if (penetrated) { return false; }
    }
    return true;
  }
#endif

protected:
  void get_penetrable_vertices(VertexPtr candidate, VtxPtrList& penetrable)
  {
    _pgn.collect(WithConvexity(_pgn.normal(), -1), penetrable);
    remove(penetrable, candidate->prev());
    remove(penetrable, candidate);
    remove(penetrable, candidate->next());
  }

  static void remove(VtxPtrList& list, VertexPtr elem)
  {
    list.erase(std::remove(list.begin(), list.end(), elem), list.end());
  }

private:
  PolygonType& _pgn;

};//class IsEar


template<typename Scalar>
void triangulate(Polygon<Scalar>& pgn, std::vector<Triangle<Scalar,3> >& mesh)
{
  typedef Vertex<Scalar> Vtx;
  if (pgn.size() < 3) { return; }
  while (pgn.size() > 3) {
    std::vector<Vtx*> ears;
    pgn.collect(IsEar<double>(pgn), ears);
    if (ears.empty()) { throw *(new std::runtime_error("triangulate(Polygon): no ear detected")); }
    Vtx* ear = ears[0]; // or what? here we can make a choice a little more elaborate.
    mesh.push_back(ear->triangle());
    pgn.remove(ear);
  }
  mesh.push_back(pgn.head()->triangle());
}

}//namespace geometry
//endregion ear


}//namespace openhrp

#endif //OPENHRP_GEOMETRY_H_INCLUDED


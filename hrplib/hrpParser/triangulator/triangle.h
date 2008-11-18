#ifndef __TRIANGLE_H__
#define __TRIANGLE_H__

#include "segment.h"


namespace geometry
{

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
  VectorType cyclic_cross_sum() const { return cyclic_cross_sum(_v[0],_v[1],_v[2]); }

  inline 
  VectorType area_vector() const { return (cyclic_cross_sum() / (Scalar)2.0); }

  inline 
  VectorType normal() const
  {
    VectorType ccs = cyclic_cross_sum();
    return (VectorType::is_zero(ccs) ? VectorType() : (ccs / ccs.norm()));
  }

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

protected:
  static VectorType cyclic_cross_sum(const PointType& a, const PointType& b, const PointType& c)
  {
    //return (cross(a,b) + cross(b,c) + cross(c,a)); 	// definition-faithful
    return cross(b - a, c - a); 			// more efficient
  }

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

#endif //__TRIANGLE_H__


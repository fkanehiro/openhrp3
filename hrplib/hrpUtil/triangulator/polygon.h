#ifndef __POLYGON_H__
#define __POLYGON_H__

#include <functional>
#include <vector>

#include "triangle.h"

namespace geometry 
{

enum Convexity {Convex, Flat, Concave};


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

  class WithZeroNormal: Predicate
  {
  public:
    WithZeroNormal(Scalar almost_zero): _almost_zero(almost_zero) {}
    inline
    bool operator()(const Vertex& vtx) { return (vtx.triangle().normal().norm() < _almost_zero); }
  private:
    Scalar _almost_zero;
  };//class WithZeroNormal

  class WithPositiveNormal: Predicate
  {
  public:
    WithPositiveNormal(const VectorType& normal): _normal(normal) {}
    inline
    bool operator()(const Vertex& vtx) { return (dot(vtx.triangle().normal(), _normal) > (Scalar)0.0); }
  private:
    VectorType _normal;
  };//class WithPositiveNormal

  class WithNegativeNormal: Predicate
  {
  public:
    WithNegativeNormal(const VectorType& normal): _normal(normal) {}
    inline 
    bool operator()(const Vertex& vtx) { return (dot(vtx.triangle().normal(), _normal) < (Scalar)0.0); }
  private:
    VectorType _normal;
  };//class WithNegativeNormal

#if 0
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
#endif

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

  Convexity convexity(const VectorType& positive_ccs) const
  {
    check_link("Vertex::convexity(): null link");
    VectorType vccs = triangle().cyclic_cross_sum();
    if (VectorType::is_zero(vccs)) { return Flat; }
    return ((dot(vccs, positive_ccs) > (Scalar)0.0) ? Convex : Concave);
  }

  //double angle() {} 	// to-do

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
    if (!(_prev && _next)) { throw std::runtime_error(errmsg); }
    //if (!(_prev && _next)) { throw *(new std::runtime_error(errmsg)); }
  }

private:
  PointType _point;
  Vertex* _prev;
  Vertex* _next;

};//class Vertex


template<typename Scalar>
class VertexInfo
{
public:
  typedef Point<Scalar,3> PointType;

public:
  VertexInfo(): point(), convexity(Flat) {}
  VertexInfo(const PointType& _point, Convexity _convexity): point(_point), convexity(_convexity) {}

public:
  PointType point;
  Convexity convexity;
};//class VertexInfo


template<typename Scalar>
class Polygon
{
public:
  typedef Vector<Scalar,3> VectorType;
  typedef Point<Scalar,3> PointType;
  typedef Vertex<Scalar> VertexType;
  typedef VertexInfo<Scalar> VertexInfoType;

public:
  Polygon(): _size(0), _head(NULL) {}

  ~Polygon() { try { clear(); } catch(...) {} }

  VectorType cyclic_cross_sum() const { return cyclic_cross_sum(collect_points()); }

  VectorType area_vector() const { return (cyclic_cross_sum() / (Scalar)2.0); }

  inline
  VectorType normal() const { VectorType ccs = cyclic_cross_sum(); return (ccs / ccs.norm()); }

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

  template<typename Predicate>
  std::vector<int> detect(Predicate predicate) const
  {
    std::vector<int> indices;
    if (_size == 0) { return indices; }
    VertexType* p = _head;
    int index = 0;
    while (true) {
      if (predicate(*p)) { indices.push_back(index); }
      p = p->next();
      if (p == _head) { break; }
      index++;
    }
    return indices;
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

  std::vector<VertexType*> collect_vertices() const 
  {
    std::vector<VertexType*> vertices;
    if (_size == 0) { return vertices; }
    VertexType* p = _head;
    while (true) {
      vertices.push_back(p);
      p = p->next();
      if (p == _head) { break; }
    }
    return vertices;
  }

  std::vector<PointType> collect_points() const { return point_list(collect_vertices()); }

  std::vector<VertexInfoType> collect_vertex_info() const
  {
    std::vector<VertexInfoType> infolist;
    std::vector<VertexType*> vertices = collect_vertices();
    VectorType pccs = cyclic_cross_sum(vertices);
    for (int i = 0; i < vertices.size(); ++i) {
      PointType point = vertices[i]->point();
      VectorType vccs = vertices[i]->triangle().cyclic_cross_sum();
      Convexity convexity = get_convexity(vccs, pccs);
      infolist.push_back(VertexInfoType(point, convexity));
    }
    return infolist;
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

  template<typename Predicate>
  std::vector<VertexType*> collect_vertices(Predicate predicate) const
  {
    std::vector<VertexType*> vertices;
    if (_size == 0) { return vertices; }
    VertexType* p = _head;
    while (true) {
      if (predicate(*p)) { vertices.push_back(p); }
      p = p->next();
      if (p == _head) { break; }
    }
    return vertices;
  }

  template<typename Predicate>
  VertexType* find_vertices(Predicate predicate) const
  {
    VertexType* p = 0;
    if (_size == 0) { return p; }
    p = _head;
    while (true) {
      if (predicate(*p)) { return p; }
      p = p->next();
      if (p == _head) { break; }
    }
    return p;
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

  void remove(const PointType& p) 
  { 
    typedef typename VertexType::WithPoint WithPoint;
    remove(WithPoint(p));
  }

  size_t count_convex() const
  {
    typedef typename VertexType::WithPositiveNormal WithPositiveNormal;
    return count(WithPositiveNormal(normal()));
  }

  size_t count_concave() const
  {
    typedef typename VertexType::WithNegativeNormal WithNegativeNormal;
    return count(WithNegativeNormal(normal()));
  }

  std::vector<VertexType*> collect_convex_vertices() const
  {
    typedef typename VertexType::WithPositiveNormal WithPositiveNormal;
    return collect_vertices(WithPositiveNormal(normal()));
  }

  std::vector<VertexType*> collect_concave_vertices() const
  {
    typedef typename VertexType::WithNegativeNormal WithNegativeNormal;
    return collect_vertices(WithNegativeNormal(normal()));
  }

  std::vector<PointType> collect_convex_points() const
  {
    return point_list(collect_convex_vertices());
  }

  std::vector<PointType> collect_concave_points() const
  {
    return point_list(collect_concave_vertices());
  }

#if 0
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
#endif

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

  static VectorType cyclic_cross_sum(const std::vector<VertexType*>& vertices)
  {
    //return cyclic_cross_sum(point_list(vertices));
    std::vector<PointType> points = point_list(vertices);
    return cyclic_cross_sum(points);
  }

  static std::vector<PointType> point_list(const std::vector<VertexType*>& vertices)
  {
    std::vector<PointType> points;
    for (int i = 0; i < vertices.size(); ++i) {
      points.push_back(vertices[i]->point());
    }
    return points;
  }

  //borrowed from "point.h"
  static VectorType cyclic_cross_sum(const std::vector<PointType>& points)
  {
    VectorType ccs;
    int size = points.size();
    for (int i = 0; i < size; ++i) {
      PointType a = points[i];
      PointType b = points[(i + 1) % size]; 	//taken circularly
      ccs += cross(a,b);
    }
    return ccs;
  }

  inline
  static Convexity get_convexity(const VectorType& vertex_ccs, const VectorType& polygon_ccs)
  {
    if (VectorType::is_zero(vertex_ccs)) { return Flat; }
    return ((dot(vertex_ccs, polygon_ccs) > (Scalar)0.0) ? Convex : Concave);
  }

private:
  size_t _size;
  VertexType* _head;

};//class Polygon


}//namespace geometry

#endif //__POLYGON_H__


#ifndef __TRIANGULATOR_H__
#define __TRIANGULATOR_H__

#include <vector>
#include <map>

#include <boost/array.hpp>

#include "ear.h"


namespace hrp {

template<typename Scalar>
class PolygonTriangulator
{
public:
  typedef boost::array<Scalar, 3> BA3;
  typedef std::vector<BA3> BA3List;

  typedef int Index;
  typedef std::vector<int> IndexList;
  typedef std::map<BA3, int> Point2IndexMap;

  typedef geometry::Vector<Scalar,3> VectorType;
  typedef geometry::Point<Scalar,3> PointType;
  typedef geometry::Triangle<Scalar,3> TriangleType;
  typedef geometry::Polygon<Scalar> PolygonType;

  typedef std::vector<PointType> PointList;
  typedef std::vector<TriangleType> TriangleList;

public:
  PolygonTriangulator(const BA3List& base_points)
  {
    copy(base_points, _points);
    build(_point2index, base_points);
  }

  void operator()(IndexList& polygon_indices, IndexList& mesh_indices, IndexList& remove_indices)
  {
    PolygonType polygon;
    for (int i = 0; i < polygon_indices.size(); i++) {
      polygon.add(_points[polygon_indices[i]]);
    }

    TriangleList mesh;
    PointList removePoint;
    geometry::triangulate(polygon, mesh, removePoint);

    for (int i = 0; i < mesh.size(); i++) {
      int triad[3];
      lookup_index(mesh[i], triad);
      add_all(mesh_indices, triad);
    }
    for(int i=0; i<removePoint.size(); i++){
        remove_indices.push_back(index_of(removePoint[i]));
    }
  }

  void operator()(IndexList& polygon_indices, IndexList& mesh_indices){
    IndexList remove_indices;
    this->operator ()(polygon_indices, mesh_indices, remove_indices);
  }

protected:
  void build(Point2IndexMap& m, const BA3List& points)
  {
    for (int i = 0; i < points.size(); i++) { m[points[i]] = i; }
  }

  Index index_of(const PointType& p) { return _point2index[BA(p)]; }

  void lookup_index(TriangleType& tg, Index indices[3])
  {
    for (int i = 0; i < 3; i++) { indices[i] = index_of(tg.vertex(i)); }
  }

  void add_all(IndexList& indices, Index triad[3])
  {
    for (int i = 0; i < 3; i++) { indices.push_back(triad[i]); }
  }

  static BA3 BA(Scalar x, Scalar y, Scalar z) { BA3 ba = {x,y,z}; return ba; }

  static BA3 BA(const PointType& p) { return BA(p[0], p[1], p[2]); }

  static VectorType V(const BA3& ba)
  {
    Scalar a[3]; std::copy(ba.begin(), ba.end(), a); return VectorType(a);
  }

  static PointType P(const BA3& ba)
  {
    Scalar a[3]; std::copy(ba.begin(), ba.end(), a); return PointType(a);
  }

  static void copy(const BA3List& bal, PointList& lst)
  {
    for (int i = 0; i < bal.size(); i++) { lst.push_back(P(bal[i])); }
  }

private:
  PointList  _points; 	// base points

  Point2IndexMap _point2index;

};//class PolygonTriangulator

}//namespace hrp


#endif //__TRIANGULATOR_H__


#ifndef __EAR_H__
#define __EAR_H__

#include "polygon.h"


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

public:
  IsEar(PolygonType& polygon): _pgn(polygon), _ccs(polygon.cyclic_cross_sum()) {}

#ifdef DEBUG_EAR
  bool operator()(const VertexType& cvtx)
  {
    VertexType& vtx = const_cast<VertexType&>(cvtx);
    std::clog << "IsEar::operator()(Vertex).." << std::endl;
    std::clog << "  candidate vertex: " << vtx << std::endl;
    if (vtx.convexity(_ccs) != Convex) { 
      std::clog << "  this is non-convex.." << std::endl;
      return false;
    }
    TriangleType candidate_triangle = vtx.triangle();
    std::clog << "  candidate triangle: " << candidate_triangle << std::endl;
    VtxPtrList penetrable = get_penetrable_vertices(&vtx);
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
  bool operator()(const VertexType& cvtx)
  {
    VertexType& vtx = const_cast<VertexType&>(cvtx);
    if (vtx.convexity(_ccs) != Convex) { return false; }
    TriangleType candidate_triangle = vtx.triangle();
    VtxPtrList penetrable = get_penetrable_vertices(&vtx);
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
  VtxPtrList get_penetrable_vertices(VertexPtr candidate) const
  {
    VtxPtrList penetrable = _pgn.collect_concave_vertices();
    if (penetrable.empty()) { return penetrable; }
    remove(penetrable, candidate->prev());
    remove(penetrable, candidate);
    remove(penetrable, candidate->next());
    return penetrable;
  }

  static void remove(VtxPtrList& list, VertexPtr elem)
  {
    list.erase(std::remove(list.begin(), list.end(), elem), list.end());
  }

  PolygonType& _pgn;
  VectorType   _ccs;

};//class IsEar

template<typename Scalar>
class IsFlatPoint: IsEar<Scalar>
{
public:
    IsFlatPoint(PolygonType& polygon): IsEar(polygon) {}

    bool operator()(const VertexType& cvtx)
  {
    VertexType& vtx = const_cast<VertexType&>(cvtx);
    if (vtx.convexity(_ccs) != Flat) return false;
    else return true;
  }
};


template<typename Scalar>
void triangulate(Polygon<Scalar>& pgn, std::vector< Triangle<Scalar,3> >& mesh, std::vector<Point<Scalar,3>>& removePoint )
{
  typedef Vertex<Scalar> Vtx;
  if (pgn.size() < 3) { return; }
  IsFlatPoint<Scalar> isFlatPoint(pgn);
  std::vector<Vtx*> removeVertex =pgn.collect_vertices(isFlatPoint);
  for(int i=0; i<removeVertex.size(); i++){
      removePoint.push_back(removeVertex[i]->point());   
      pgn.remove(removeVertex[i]);
  }
  while (pgn.size() > 3) {
    Vtx* ear = pgn.find_vertices(IsEar<Scalar>(pgn));
    if (ear==0) { 
        std::cout << "triangulate(Polygon): no ear detected" << std::endl;
        break;
    }
    mesh.push_back(ear->triangle());
    pgn.remove(ear);
  }
  mesh.push_back(pgn.head()->triangle());
}


}//namespace geometry

#endif //__EAR_H__


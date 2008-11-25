#include "DistFuncs.h"
#if 1
std::ostream &operator<<(std::ostream &ost, const Point& p)
{
    ost << "(" << p.x << ", " << p.y << ", " << p.z << ")";
    return ost;
} 

#endif

/**
 * @brief compute the minimum distance and the closest points between two line segments
 * 
 * This function is implemented referring the following webpage
 * http://www.softsurfer.com/Archive/algorithm_0106/algorithm_0106.htm
 * @param u0 one of end points of the first line segment
 * @param u1 the other end point of the first line segment
 * @param v0 one of end points of the second line segment
 * @param v1 the other end point of the second line segment
 * @param cp0 the closest point on the first line segment 
 * @param cp1 the closest point on the second line segment
 * @return the minimum distance
 */
inline float SegSegDist(const Point& u0, const Point& u1,
                 const Point& v0, const Point& v1,
                 Point& cp0, Point& cp1)
{
    Point    u = u1 - u0;
    Point    v = v1 - v0;
    Point    w = u0 - v0;
    float    a = u|u;        // always >= 0
    float    b = u|v;
    float    c = v|v;        // always >= 0
    float    d = u|w;
    float    e = v|w;
    float    D = a*c - b*b;       // always >= 0
    float    sc, sN, sD = D;      // sc = sN / sD, default sD = D >= 0
    float    tc, tN, tD = D;      // tc = tN / tD, default tD = D >= 0

    // compute the line parameters of the two closest points
#define EPS 1e-8
    if (D < EPS) { // the lines are almost parallel
        sN = 0.0;        // force using point P0 on segment S1
        sD = 1.0;        // to prevent possible division by 0.0 later
        tN = e;
        tD = c;
    }
    else {                // get the closest points on the infinite lines
        sN = (b*e - c*d);
        tN = (a*e - b*d);
        if (sN < 0.0) {       // sc < 0 => the s=0 edge is visible
            sN = 0.0;
            tN = e;
            tD = c;
        }
        else if (sN > sD) {  // sc > 1 => the s=1 edge is visible
            sN = sD;
            tN = e + b;
            tD = c;
        }
    }

    if (tN < 0.0) {           // tc < 0 => the t=0 edge is visible
        tN = 0.0;
        // recompute sc for this edge
        if (-d < 0.0)
            sN = 0.0;
        else if (-d > a)
            sN = sD;
        else {
            sN = -d;
            sD = a;
        }
    }
    else if (tN > tD) {      // tc > 1 => the t=1 edge is visible
        tN = tD;
        // recompute sc for this edge
        if ((-d + b) < 0.0)
            sN = 0;
        else if ((-d + b) > a)
            sN = sD;
        else {
            sN = (-d + b);
            sD = a;
        }
    }
    // finally do the division to get sc and tc
    sc = (fabsf(sN) < EPS ? 0.0 : sN / sD);
    tc = (fabsf(tN) < EPS ? 0.0 : tN / tD);

    cp0 = u0 + sc * u;
    cp1 = v0 + tc * v;

    // get the difference of the two closest points
    Point dP = cp0 - cp1; 

    return dP.Magnitude();   // return the closest distance
}

/**
 * @brief compute signed distance between a point and a plane
 * @param P a point
 * @param pointOnPolane a point on the plane
 * @param n normal vector of the plane
 * @param cp the closest point on the plane from P
 */
inline float PointPlaneDist(const Point& P, const Point& pointOnPlane, const Point& n,
                     Point& cp)
{
    Point v = P - pointOnPlane;
    float l = v|n;
    cp = P-l*n;
    return l;
}

/**
 * @brief check whether a point is in Voroni region of a face
 * @param p a point to be tested
 * @param vertices vertices of the triangle
 * @param outers normal vectors of edges
 * @return true if the point is in the Voronoi region, false otherwise
 */
inline bool PointFaceAppTest(const Point& P, const Point** vertices,
                             const Point* outers)
{
    Point v;
    for (unsigned int i=0; i<3; i++){
        v = P - *vertices[i];
        if ((v|outers[i])>0) return false;
    }
    return true;
}

/**
 * @brief check whether a point is in Voroni region of a face
 * @param p a point to be tested
 * @param vertices vertices of the triangle
 * @param edges edges of the triangle
 * @param n normal vector of the triangle
 * @return true if the point is in the Voronoi region, false otherwise
 */
inline bool PointFaceAppTest(const Point& P, const Point** vertices, 
                             const Point* edges, const Point& n)
{
    Point outers[3];
    for (unsigned int i=0; i<3; i++){
        outers[i] = edges[i]^n;
    }
    return PointFaceAppTest(P, vertices, outers);
}

/**
 * @brief check whether a point is in Voroni region of a face
 * @param p a point to be tested
 * @param vertices vertices of the triangle
 * @return true if the point is in the Voronoi region, false otherwise
 */
inline bool PointFaceAppTest(const Point& P, const Point** vertices)
{
    Point edges[3];
    for (unsigned int i=0; i<3; i++){
        edges[i] = *vertices[(i+1)%3]-*vertices[i];
    }
    Point n = edges[0]^edges[1];
    return PointFaceAppTest(P, vertices, edges, n);
}

// see DistFuncs.h
float TriTriDist(const Point& U0, const Point& U1, const Point& U2,
                 const Point& V0, const Point& V1, const Point& V2,
                 Point& cp0, Point& cp1)
{
    const Point* uvertices[] = {&U0, &U1, &U2};
    const Point* vvertices[] = {&V0, &V1, &V2};
    float min_d, d;
    Point p0, p1;

    // set initial values
    cp0 = U0;
    cp1 = V0;
    min_d = (cp0-V0).Magnitude();

    // vertex-vertex, vertex-edge, edge-edge
    for (unsigned int i=0; i<3; i++){
        for (unsigned int j=0; j<3; j++){
            d = SegSegDist(*uvertices[i], *uvertices[(i+1)%3],
                           *vvertices[j], *vvertices[(j+1)%3],
                           p0, p1);
            if (d < min_d){
                min_d = d;
                cp0 = p0;
                cp1 = p1;
            }
        }
    }

    Point uedges[3], vedges[3];
    for (unsigned int i=0; i<3; i++){
        uedges[i] = *uvertices[(i+1)%3] - *uvertices[i];
        vedges[i] = *vvertices[(i+1)%3] - *vvertices[i];
    }

    Point un = uedges[0]^uedges[1];
    un.Normalize();
    Point vn = vedges[0]^vedges[1];
    vn.Normalize();

    Point uouters[3], vouters[3];
    for (unsigned int i=0; i<3; i++){
        uouters[i] = uedges[i]^un;
        vouters[i] = vedges[i]^vn;
    }

    // vertex-face, edge-face, face-face
    for (unsigned int i=0; i<3; i++){
        if (PointFaceAppTest(*uvertices[i], vvertices, vouters)){
            d = fabsf(PointPlaneDist(*uvertices[i], *vvertices[0], vn, p1));
            if (d < min_d){
                min_d = d;
                cp0 = *uvertices[i];
                cp1 = p1;
            }
        }
    }
    for (unsigned int i=0; i<3; i++){
        if (PointFaceAppTest(*vvertices[i], uvertices, uouters)){
            d = fabsf(PointPlaneDist(*vvertices[i], *uvertices[0], un, p0));
            if (d < min_d){
                min_d = d;
                cp0 = p0;
                cp1 = *vvertices[i];
            }
        }
    }

    return min_d;
}

#if 1

int main()
{
    Point p0(0,0,0), p1(2,0,0), p2(1, 1, -1), p3(1, 1, 1);
    Point cp1, cp2;
    float d;

    d= SegSegDist(p0, p1, p2, p3, cp1, cp2);
    std::cout << "test1 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;

    Point p4(0,1,0), p5(2,1,0);
    d= SegSegDist(p0, p1, p4, p5, cp1, cp2);
    std::cout << "test2 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;

    d= SegSegDist(p0, p1, p0, p1, cp1, cp2);
    std::cout << "test3 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;

    Point p6(0, 2, 0), p7(-2, 1, 0);
    d = TriTriDist(p0, p1, p5, p4, p6, p7, cp1, cp2);
    std::cout << "test4 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;

    Point p8(3, -1, 1), p9(3, 2, 1), p10(-3, -1, 1);
    d = TriTriDist(p0, p1, p5, p8, p9, p10, cp1, cp2);
    std::cout << "test5 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;

    d = TriTriDist(p0, p1, p2, p8, p9, p10, cp1, cp2);
    std::cout << "test6 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;
    std::cout << "answer: d = 1, cp1 = (0, 0, 0), cp2 = (0, 0, 1)" 
              << std::endl; 

    d = TriTriDist(p2, p0, p1, p8, p9, p10, cp1, cp2);
    std::cout << "test7 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;
    std::cout << "answer: d = 1, cp1 = (0, 0, 0), cp2 = (0, 0, 1)" 
              << std::endl; 

    d = TriTriDist(p1, p2, p0, p8, p9, p10, cp1, cp2);
    std::cout << "test8 : d = " << d << ", cp1 = " << cp1 << ", cp2 = " << cp2 
              << std::endl;
    std::cout << "answer: d = 1, cp1 = (2, 0, 0), cp2 = (2, 0, 1)" 
              << std::endl; 

    return 0;
}
#endif

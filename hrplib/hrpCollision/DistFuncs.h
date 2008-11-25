#include "Opcode/Opcode.h"

/**
 * @brief compute the minimum distance and the closest points between two triangles
 * @param U0 the first vertex of the first triangle
 * @param U1 the second vertex of the first triangle
 * @param U2 the third vertex of the first triangle
 * @param V0 the first vertex of the second triangle
 * @param V1 the second vertex of the second triangle
 * @param V2 the third vertex of the second triangle
 * @parma cp0 the closest point on the first triangle
 * @parma cp1 the closest point on the second triangle
 * @return the minimum distance
 */
float TriTriDist(const Point& U0, const Point& U1, const Point& U2,
                 const Point& V0, const Point& V1, const Point& V2,
                 Point& cp0, Point& cp1);



#if 1
#include <iostream>
std::ostream &operator<<(std::ostream &ost, const Point& p);
#endif

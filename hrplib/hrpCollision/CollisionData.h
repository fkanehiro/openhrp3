/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

#ifndef HRP_COLLISION_COLLISION_DATA_H_INCLUDED
#define HRP_COLLISION_COLLISION_DATA_H_INCLUDED

#include <boost/numeric/ublas/vector.hpp>
#include <boost/numeric/ublas/io.hpp>

typedef boost::numeric::ublas::bounded_vector<double,3> dvector3;


// this is for the client
class collision_data
{
public:
  int id1;
  int id2;

  int num_of_i_points;
  dvector3 i_points[4];
  int i_point_new[4];

  dvector3 n_vector;
  double depth;

  dvector3 n; // normal vector of triangle id1
  dvector3 m; // normal vector of triangle id2
  int c_type; // c_type=1 for vertex-face contact, c_type=2 for edge-edge contact

};

extern  int cdBoxTestsCount;
extern  int cdTriTestsCount;
extern  int cdContactsCount;

extern  collision_data *cdContact;


#endif

// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
#include <algorithm>
#include <iostream>
#include "quaternion.h"

using namespace OpenHRP;

const double EPSILON = 5e-8; // logs show that max value tends to be around 2.0e-13 - hit exception

//////////////////////////////////////////////////////////////////////
//
// Note:    the input quaternion need not be of norm 1 for the
// following function
//
// Following functions from boost::quaternion example HS03.hpp 
// roughly converted to use OpenHRP types dvector3 and dquaternion
//
// should change HSO3.hpp to use expression templates.
//

matrix33 OpenHRP::rotFromQuaternion(const dquaternion& q)
{
    using    ::std::numeric_limits;
  
    double    a = q.R_component_1(); // w
    double    b = q.R_component_2(); // x
    double    c = q.R_component_3(); // y
    double    d = q.R_component_4(); // z
    
    double    aa = a*a;
    double    ab = a*b;
    double    ac = a*c;
    double    ad = a*d;
    double    bb = b*b;
    double    bc = b*c;
    double    bd = b*d;
    double    cc = c*c;
    double    cd = c*d;
    double    dd = d*d;
    
    double    norme_carre = aa+bb+cc+dd;
    
    //if (norme_carre <= numeric_limits<double>::epsilon()) {
    if (norme_carre <= EPSILON) {
	
	std::string            error_reporting("Argument to quaternion_to_R3_rotation is too small!");
	std::underflow_error   bad_argument(error_reporting);
      
	throw(bad_argument);
    }

    matrix33 rot;
    rot = (aa+bb-cc-dd) / norme_carre, 2.0*(-ad+bc) / norme_carre, 2.0*(ac+bd) / norme_carre,
	2.0*(ad+bc) / norme_carre, (aa-bb+cc-dd) / norme_carre, 2.0*(-ab+cd) / norme_carre,
	2.0*(-ac+bd) / norme_carre, 2.0*(ab+cd) / norme_carre, (aa-bb-cc+dd) / norme_carre;
    return rot;
}


namespace
{
    void    find_invariant_vector( matrix33 const & rot,
				   double & x,
				   double & y,
				   double & z)
    {
	using    ::std::sqrt;
        
	using    ::std::numeric_limits;
        
	double    b11 = rot(0,0) - 1.0;
	double    b12 = rot(0,1);
	double    b13 = rot(0,2);
	double    b21 = rot(1,0);
	double    b22 = rot(1,1) - 1.0;
	double    b23 = rot(1,2);
	double    b31 = rot(2,0);
	double    b32 = rot(2,1);
	double    b33 = rot(2,2) - 1.0;
        
	double    minors[9] =
	    {
		b11*b22-b12*b21,
		b11*b23-b13*b21,
		b12*b23-b13*b22,
		b11*b32-b12*b31,
		b11*b33-b13*b31,
		b12*b33-b13*b32,
		b21*b32-b22*b31,
		b21*b33-b23*b31,
		b22*b33-b23*b32
	    };
        
	double *        where = ::std::max_element(minors, minors+9);
        
	double          det = *where;
        
	//if    (det <= numeric_limits<double>::epsilon())
	if    (det <= EPSILON)
	    {
		::std::string            error_reporting("Underflow error in find_invariant_vector!");
		::std::underflow_error   processing_error(error_reporting);
		
		throw(processing_error);
	    }
        
	switch    (where-minors)
	    {
	    case 0:
                
		z = 1.0;
                
		x = (-b13*b22+b12*b23)/det;
		y = (-b11*b23+b13*b21)/det;
		
		break;
                
	    case 1:
                
		y = 1.0;
                
		x = (-b12*b23+b13*b22)/det;
		z = (-b11*b22+b12*b21)/det;
		
		break;
                
	    case 2:
		
		x = 1.0;
                
		y = (-b11*b23+b13*b21)/det;
		z = (-b12*b21+b11*b22)/det;
                
		break;
                
	    case 3:
                
		z = 1.0;
                
		x = (-b13*b32+b12*b33)/det;
		y = (-b11*b33+b13*b31)/det;
                
		break;
                
	    case 4:
                
		y = 1.0;
                
		x = (-b12*b33+b13*b32)/det;
		z = (-b11*b32+b12*b31)/det;
                
		break;
                
	    case 5:
                
		x = 1.0;
		
		y = (-b11*b33+b13*b31)/det;
		z = (-b12*b31+b11*b32)/det;
                
		break;
                
	    case 6:
		
		z = 1.0;
                
		x = (-b23*b32+b22*b33)/det;
		y = (-b21*b33+b23*b31)/det;
                
		break;
                
	    case 7:
                
		y = 1.0;
                
		x = (-b22*b33+b23*b32)/det;
		z = (-b21*b32+b22*b31)/det;
                
		break;
                
	    case 8:
                
		x = 1.0;
                
		y = (-b21*b33+b23*b31)/det;
		z = (-b22*b31+b21*b32)/det;
                
		break;
                
	    default:
                
		::std::string
		      error_reporting
		      ("Impossible condition in find_invariant_vector");
	    
	    ::std::logic_error   processing_error(error_reporting);
	    
	    throw(processing_error);
	    
	    break;
	    }
        
	double    vecnorm = sqrt(x*x+y*y+z*z);
        
	//if    (vecnorm <= numeric_limits<double>::epsilon())
	if    (vecnorm <= EPSILON)
	    {
		::std::string            
		      error_reporting
		      ("Overflow error in find_invariant_vector!");
		::std::overflow_error    
		      processing_error(error_reporting);
		
		throw(processing_error);
	    }
        
	x /= vecnorm;
	y /= vecnorm;
	z /= vecnorm;
    }
    
    
    void    find_orthogonal_vector( double x,
				    double y,
				    double z,
				    double & u,
				    double & v,
				    double & w)
    {
	using    std::abs;
	using    std::sqrt;
        
	using    std::numeric_limits;
        
	double   vecnormsqr = x*x+y*y+z*z;
	
	//if    (vecnormsqr <= numeric_limits<double>::epsilon())
	if    (vecnormsqr <= EPSILON)
	    {
		std::string            error_reporting("Underflow error in find_orthogonal_vector!");
		std::underflow_error   processing_error(error_reporting);
		
		throw(processing_error);
	    }
        
	double        lambda;
        
	double        components[3] =
	    {
		abs(x),
		abs(y),
		abs(z)
	    };
        
	double *    where = std::min_element(components, components+3);
        
	switch    (where-components)
	    {
	    case 0:
                
	      //if    (*where <= numeric_limits<double>::epsilon())
	      if    (*where <= EPSILON)
		{
			v =
			    w = 0.0;
			u = 1.0;
		    }
		else
		    {
			lambda = -x/vecnormsqr;
			
			u = 1.0 + lambda*x;
			v = lambda*y;
			w = lambda*z;
		    }
                
		break;
                
	    case 1:
                
	      //if    (*where <= numeric_limits<double>::epsilon())	      
	      if    (*where <= EPSILON)
		    {
			u =
			    w = 0.0;
			v = 1.0;
		    }
		else
		    {
			lambda = -y/vecnormsqr;
                    
			u = lambda*x;
			v = 1.0 + lambda*y;
			w = lambda*z;
		    }
                
		break;
                
	    case 2:
                
	      //if    (*where <= numeric_limits<double>::epsilon())
	      if    (*where <= EPSILON)
		    {
			u =
			    v = 0.0;
			w = 1.0;
		    }
		else
		    {
			lambda = -z/vecnormsqr;
                    
			u = lambda*x;
			v = lambda*y;
			w = 1.0 + lambda*z;
		    }
                
		break;
                
	    default:
                
		std::string        error_reporting("Impossible condition in find_invariant_vector");
		std::logic_error   processing_error(error_reporting);
                
		throw(processing_error);
                
		break;
	    }
        
	double    vecnorm = sqrt(u*u+v*v+w*w);
        
	//if    (vecnorm <= numeric_limits<double>::epsilon())
	if    (vecnorm <= EPSILON)
	    {
		std::string            error_reporting("Underflow error in find_orthogonal_vector!");
		std::underflow_error   processing_error(error_reporting);
            
		throw(processing_error);
	    }
        
	u /= vecnorm;
	v /= vecnorm;
	w /= vecnorm;
    }
    
    
    // Note:    we want [[v, v, w], [r, s, t], [x, y, z]] to be a direct orthogonal basis
    //            of R^3. It might not be orthonormal, however, and we do not check if the
    //            two input vectors are colinear or not.
    
    void    find_vector_for_BOD(double x,
				double y,
				double z,
				double u, 
				double v,
				double w,
				double & r,
				double & s,
				double & t)
    {
	r = +y*w-z*v;
	s = -x*w+z*u;
	t = +x*v-y*u;
    }
}

inline bool is_33_rotation_matrix(matrix33 const & mat)
{
    using    std::abs;
    using    std::numeric_limits;
#if 0    
    return    
	(
	 !(
	   (abs(mat(0,0)*mat(0,0)+mat(1,0)*mat(1,0)+mat(2,0)*mat(2,0) - 1.0) > EPSILON)
	   (abs(mat(0,0)*mat(0,1)+mat(1,0)*mat(1,1)+mat(2,0)*mat(2,1) - 0.0) > EPSILON)||
	   (abs(mat(0,0)*mat(0,2)+mat(1,0)*mat(1,2)+mat(2,0)*mat(2,2) - 0.0) > EPSILON)||
	   (abs(mat(0,1)*mat(0,1)+mat(1,1)*mat(1,1)+mat(2,1)*mat(2,1) - 1.0) > EPSILON)||
	   (abs(mat(0,1)*mat(0,2)+mat(1,1)*mat(1,2)+mat(2,1)*mat(2,2) - 0.0) > EPSILON)||
	   (abs(mat(0,2)*mat(0,2)+mat(1,2)*mat(1,2)+mat(2,2)*mat(2,2) - 1.0) > EPSILON)
	   )
	 );
#else

    //double c = 10.0*numeric_limits<double>::epsilon();
    double c1 = (abs(mat(0,0)*mat(0,0)+mat(1,0)*mat(1,0)+mat(2,0)*mat(2,0) - 1.0));
    double c2 = (abs(mat(0,0)*mat(0,1)+mat(1,0)*mat(1,1)+mat(2,0)*mat(2,1) - 0.0));
    double c3 = (abs(mat(0,0)*mat(0,2)+mat(1,0)*mat(1,2)+mat(2,0)*mat(2,2) - 0.0));
    double c4 = (abs(mat(0,1)*mat(0,1)+mat(1,1)*mat(1,1)+mat(2,1)*mat(2,1) - 1.0));
    double c5 = (abs(mat(0,1)*mat(0,2)+mat(1,1)*mat(1,2)+mat(2,1)*mat(2,2) - 0.0));
    double c6 = (abs(mat(0,2)*mat(0,2)+mat(1,2)*mat(1,2)+mat(2,2)*mat(2,2) - 1.0));
    return    
#if 0
	(
	 !( (c1 > 100.0*numeric_limits<double>::epsilon())||
	    (c2 > 100.0*numeric_limits<double>::epsilon())||
	    (c3 > 100.0*numeric_limits<double>::epsilon())||
	    (c4 > 100.0*numeric_limits<double>::epsilon())||
	    (c5 > 100.0*numeric_limits<double>::epsilon())||
	    (c6 > 100.0*numeric_limits<double>::epsilon())
	    )
	 );
#else
	(
	 !( 
	   (c1 > EPSILON)||(c2 > EPSILON)||(c3 > EPSILON)||
	   (c4 > EPSILON)||(c5 > EPSILON)||(c6 > EPSILON)
	   )
	 );
#endif
#endif
}


dquaternion OpenHRP::quaternionFromRot( const matrix33 & rot,
					const dquaternion * hint)
{
    using    boost::math::abs;
    
    using    std::abs;
    using    std::sqrt;
		
    using    std::numeric_limits;

    if    (!is_33_rotation_matrix(rot))
	{
	    //std::string        error_reporting("Argument to R3_rotation_to_quaternion is not an R^3 rotation matrix!");
	    std::cerr << "Argument to R3_rotation_to_quaternion is not an R^3 rotation matrix!\n";
	    std::cerr << (abs(rot(0,0)*rot(0,0)+rot(1,0)*rot(1,0)+rot(2,0)*rot(2,0) - 1.0)) << " ";
	    std::cerr << (abs(rot(0,0)*rot(0,1)+rot(1,0)*rot(1,1)+rot(2,0)*rot(2,1) - 0.0)) << " ";
	    std::cerr << (abs(rot(0,0)*rot(0,2)+rot(1,0)*rot(1,2)+rot(2,0)*rot(2,2) - 0.0)) << " ";
	    std::cerr << (abs(rot(0,1)*rot(0,1)+rot(1,1)*rot(1,1)+rot(2,1)*rot(2,1) - 1.0)) << " ";
	    std::cerr << (abs(rot(0,1)*rot(0,2)+rot(1,1)*rot(1,2)+rot(2,1)*rot(2,2) - 0.0)) << " ";
	    std::cerr << (abs(rot(0,2)*rot(0,2)+rot(1,2)*rot(1,2)+rot(2,2)*rot(2,2) - 1.0)) << std::endl;
	    //std::range_error   bad_argument(error_reporting);
	    //throw(bad_argument);
	}

    dquaternion    q;
#if 0    
    if(
       (abs(rot(0,0) - 1.0) <= numeric_limits<double>::epsilon())&&
       (abs(rot(1,1) - 1.0) <= numeric_limits<double>::epsilon())&&
       (abs(rot(2,2) - 1.0) <= numeric_limits<double>::epsilon())
       )
	q = dquaternion(1);
#else
    if(
       (abs(rot(0,0) - 1.0) <= EPSILON)&&
       (abs(rot(1,1) - 1.0) <= EPSILON)&&
       (abs(rot(2,2) - 1.0) <= EPSILON)
       )
	q = dquaternion(1);
#endif
    else
	{
	    double    cos_theta = (rot(0,0)+rot(1,1)+rot(2,2)-1.0)/2.0;
	    double    stuff = (cos_theta+1.0)/2.0;
	    double    cos_theta_sur_2 = sqrt(stuff);
	    double    sin_theta_sur_2 = sqrt(1-stuff);
        
	    double    x;
	    double    y;
	    double    z;
        
	    find_invariant_vector(rot, x, y, z);
        
	    double    u;
	    double    v;
	    double    w;
        
	    find_orthogonal_vector(x, y, z, u, v, w);
        
	    double    r;
	    double    s;
	    double    t;
        
	    find_vector_for_BOD(x, y, z, u, v, w, r, s, t);
        
	    double    ru = rot(0,0)*u+rot(0,1)*v+rot(0,2)*w;
	    double    rv = rot(1,0)*u+rot(1,1)*v+rot(1,2)*w;
	    double    rw = rot(2,0)*u+rot(2,1)*v+rot(2,2)*w;
        
	    double    angle_sign_determinator = r*ru+s*rv+t*rw;
        
	    //if        (angle_sign_determinator > +numeric_limits<double>::epsilon())
	    if        (angle_sign_determinator > +EPSILON)

		q = dquaternion(cos_theta_sur_2, +x*sin_theta_sur_2, 
				+y*sin_theta_sur_2, +z*sin_theta_sur_2);

	    //else if    (angle_sign_determinator < -numeric_limits<double>::epsilon())
	    else if    (angle_sign_determinator < -EPSILON)

		q = dquaternion(cos_theta_sur_2, -x*sin_theta_sur_2, 
				-y*sin_theta_sur_2, -z*sin_theta_sur_2);

	    else
		{
		    double    desambiguator = u*ru+v*rv+w*rw;
	  
		    if    (desambiguator >= 1.0)

			q = dquaternion(0, +x, +y, +z);

		    else

			q = dquaternion(0, -x, -y, -z);

		}
	}
    
    if    ((hint != 0) && (abs(*hint+q) < abs(*hint-q)))

	return(-q);

    
    return(q);
}
// 
// Above from Boost Quaternion Sample Implementation
// Should be changed to use expression templates.
//
////////////////////////////////////////////////////////////////////

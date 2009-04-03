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

/**
   \file
   \brief Implementations of the LinkPath and JointPath classes
   \author Shin'ichiro Nakaoka
*/
  

#include "LinkPath.h"

#include <algorithm>
#include "Link.h"
#include "MatrixSolvers.h"


using namespace std;
using namespace hrp;
using namespace tvmet;


LinkPath::LinkPath()
{

}


LinkPath::LinkPath(Link* root, Link* end)
{
    find(root, end);
}


LinkPath::LinkPath(Link* end)
{
    findPathFromRoot(end);
}


bool LinkPath::find(Link* root, Link* end)
{
    links.clear();
    numUpwardConnections = 0;
	bool found = findPathSub(root, 0, end, false);
	if(!found){
		links.clear();
	}
	return found;
}


bool LinkPath::findPathSub(Link* link, Link* prev, Link* end, bool isUpward)
{
    links.push_back(link);
    if(isUpward){
        ++numUpwardConnections;
    }
    
    if(link == end){
        return true;
    }

    for(Link* child = link->child; child; child = child->sibling){
        if(child != prev){
            if(findPathSub(child, link, end, false)){
                return true;
            }
        }
    }

    Link* parent = link->parent;
    if(parent && parent != prev){
        if(findPathSub(parent, link, end, true)){
            return true;
        }
    }

    links.pop_back();
    if(isUpward){
        --numUpwardConnections;
    }

    return false;
}


void LinkPath::findPathFromRoot(Link* end)
{
    links.clear();
    numUpwardConnections = 0;
    findPathFromRootSub(end);
    std::reverse(links.begin(), links.end());
}


void LinkPath::findPathFromRootSub(Link* link)
{
    links.push_back(link);
    if(link->parent){
        findPathFromRootSub(link->parent);
    }
}


JointPath::JointPath()
{
	initialize();
}

JointPath::JointPath(Link* root, Link* end) : 
	LinkPath(root, end), 
	joints(links.size())
{
	initialize();
    extractJoints();
}


JointPath::JointPath(Link* end) :
	LinkPath(end), 
	joints(links.size())
{
	initialize();
    extractJoints();
}


void JointPath::initialize()
{
	maxIkErrorSqr = 1.0e-6 * 1.0e-6;
}
	

JointPath::~JointPath()
{

}


bool JointPath::find(Link* root, Link* end)
{
    if(LinkPath::find(root, end)){
        extractJoints();
    }
	onJointPathUpdated();

	return (!joints.empty());
}


bool JointPath::findPathFromRoot(Link* end)
{
    LinkPath::findPathFromRoot(end);
	extractJoints();
	onJointPathUpdated();
	
    return !joints.empty();
}


void JointPath::extractJoints()
{
    numUpwardJointConnections = numUpwardConnections;

    int n = links.size();
    if(n <= 1){
        joints.clear();
    } else {
        int i = 0;
        if(isDownward(i)){
            i++;
        }
        joints.resize(n); // reserve size n buffer
        joints.clear();
        int m = n - 1;
        while(i < m){
            Link* link = links[i];
            if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
                joints.push_back(link);
            } else if(!isDownward(i)){
                --numUpwardJointConnections;
            }
            ++i;
        }
        if(isDownward(m-1)){
			Link* link = links[m];
			if(link->jointType == Link::ROTATIONAL_JOINT || link->jointType == Link::SLIDE_JOINT){
				joints.push_back(link);
			}
        }
    }
}


void JointPath::onJointPathUpdated()
{

}


void JointPath::setMaxIKError(double e)
{
  maxIkErrorSqr = e * e;
}


void JointPath::calcJacobian(dmatrix& out_J) const
{
	const int n = joints.size();
	out_J.resize(6, n, false);
	
	if(n > 0){
		
		Link* targetLink = LinkPath::endLink();
		
		for(int i=0; i < n; ++i){
			
			Link* link = joints[i];
			
			switch(link->jointType){
				
			case Link::ROTATIONAL_JOINT:
				{
					vector3 omega(link->R * link->a);
					vector3 arm(targetLink->p - link->p);
					if(!isJointDownward(i)){
						omega *= -1.0;
                    } 
					vector3 dp(cross(omega, arm));
					setVector3(dp,    out_J, 0, i);
					setVector3(omega, out_J, 3, i);
				}
				break;
				
			case Link::SLIDE_JOINT:
				{
					vector3 dp(link->R * link->d);
					if(!isJointDownward(i)){
						dp *= -1.0;
					}
					setVector3(dp, out_J, 0, i);
					out_J(3, i) = 0.0;
					out_J(4, i) = 0.0;
					out_J(5, i) = 0.0;
				}
				break;
				
			default:
				for(int j=0; j < 6; ++j){
					out_J(j, i) = 0.0;
				}
			}
		}
	}
}


bool JointPath::calcInverseKinematics
(const vector3& base_p, const matrix33& base_R, const vector3& end_p, const matrix33& end_R)
{
	Link* baseLink = LinkPath::rootLink();
	baseLink->p = base_p;
	baseLink->R = base_R;
	calcForwardKinematics();
	return calcInverseKinematics(end_p, end_R);
}


bool JointPath::calcInverseKinematics(const vector3& end_p, const matrix33& end_R0)
{
    static const int MAX_IK_ITERATION = 50;
	static const double LAMBDA = 0.9;
    
    if(joints.empty()){
		return false;
    }
    
    const int n = numJoints();

	if(n < 6){
		return false;
	}

    Link* target = LinkPath::endLink();
	matrix33 end_R(end_R0 * trans(target->Rs));

    std::vector<double> qorg(n);
    for(int i=0; i < n; ++i){
		qorg[i] = joints[i]->q;
    }

    dmatrix J(6, n);
	dvector dq(n);
    dvector v(6);

	bool converged = false;

    for(int i=0; i < MAX_IK_ITERATION; i++){
	
		calcJacobian(J);
	
		vector3 dp(end_p - target->p);
        vector3 omega(target->R * omegaFromRot(matrix33(trans(target->R) * end_R)));

        double errsqr = dot(dp, dp) + dot(omega, omega);
        if(errsqr < maxIkErrorSqr){
			converged = true;
            break;
        }

		setVector3(dp   , v, 0);
		setVector3(omega, v, 3);
		
		if(n == 6){ 
			solveLinearEquationLU(J, v, dq);
		} else {
			solveLinearEquationSVD(J, v, dq);  // dq = pseudoInverse(J) * v
		}
		
		for(int j=0; j < n; ++j){
			joints[j]->q += LAMBDA * dq(j);
		}

		calcForwardKinematics();
    }

	if(!converged){
		for(int i=0; i < n; ++i){
			joints[i]->q = qorg[i];
		}
		calcForwardKinematics();
	}
    
    return converged;
}


bool JointPath::hasAnalyticalIK()
{
	return false;
}


std::ostream& operator<<(std::ostream& os, JointPath& path)
{
    path.putInformation(os);
    return os;
}


void JointPath::putInformation(std::ostream& os) const
{
    int n = numJoints();
    for(int i=0; i < n; ++i){
        Link* link = joint(i);
        os << link->name;
        if(i != n){
			os << (isDownward(i) ? " => " : " <= ");
        }
    }
    os << std::endl;
}

/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */
/**
   \file
   \author Shin'ichiro Nakaoka
*/

#include "ForwardDynamicsABM.h"
#include "Body.h"
#include "Link.h"
#include "LinkTraverse.h"
#include "Sensor.h"
#include <hrpUtil/uBlasCommonTypes.h>
#include <boost/numeric/ublas/triangular.hpp>
#include <boost/numeric/ublas/lu.hpp>


using namespace hrp;
using namespace std;
using namespace tvmet;
using namespace boost::numeric;


static const bool debugMode = false;
static const bool rootAttitudeNormalizationEnabled = false;


ForwardDynamicsABM::ForwardDynamicsABM(BodyPtr body) :
	ForwardDynamics(body),
	q0(body->numLinks()),
	dq0(body->numLinks()),
	dq(body->numLinks()),
	ddq(body->numLinks())
{

}


ForwardDynamicsABM::~ForwardDynamicsABM()
{

}


void ForwardDynamicsABM::initialize()
{
    initializeSensors();
    calcABMFirstHalf();
}


inline void ForwardDynamicsABM::calcABMFirstHalf()
{
	calcABMPhase1();
	calcABMPhase2Part1();
}


inline void ForwardDynamicsABM::calcABMLastHalf()
{
	calcABMPhase2Part2();
	calcABMPhase3();
}


void ForwardDynamicsABM::calcNextState()
{
	switch(integrationMode){

	case EULER_METHOD:
		calcMotionWithEulerMethod();
		break;
		
	case RUNGEKUTTA_METHOD:
		calcMotionWithRungeKuttaMethod();
		break;
	}

	if(rootAttitudeNormalizationEnabled){
		Link* root = body->rootLink();
		Vector3 x0;
		getVector3(x0, root->R, 0, 0);
		Vector3 y0;
		getVector3(y0, root->R, 0, 1);
		Vector3 x(normalize(x0));
		Vector3 z(normalize((cross(x, y0))));
		Vector3 y(cross(z, x));
		setVector3(x, root->R, 0, 0);
		setVector3(y, root->R, 0, 1);
		setVector3(z, root->R, 0, 2);
	}

	calcABMFirstHalf();

    if(sensorsEnabled){
        updateSensorsFinal();
    }

    body->setVirtualJointForces();
}


void ForwardDynamicsABM::calcMotionWithEulerMethod()
{
    calcABMLastHalf();

    if(sensorsEnabled){
        updateForceSensors();
    }

    Link* root = body->rootLink();

    if(root->jointType != Link::FIXED_JOINT){
        Vector3 p;
        Matrix33 R;
        SE3exp(p, R, root->p, root->R, root->w, root->vo, timeStep);
        root->p = p;
        root->R = R;

        root->vo += root->dvo * timeStep;
        root->w  += root->dw  * timeStep;
    }

    int n = body->numLinks();
    for(int i=1; i < n; ++i){
        Link* link = body->link(i);
        link->q  += link->dq  * timeStep;
        link->dq += link->ddq * timeStep;
    }
}


void ForwardDynamicsABM::integrateRungeKuttaOneStep(double r, double dt)
{
    Link* root = body->rootLink();

    if(root->jointType != Link::FIXED_JOINT){

        SE3exp(root->p, root->R, p0, R0, root->w, root->vo, dt);
        root->vo = vo0 + root->dvo * dt;
        root->w  = w0  + root->dw  * dt;

        vo  += r * root->vo;
        w   += r * root->w;
        dvo += r * root->dvo;
        dw  += r * root->dw;
    }

    int n = body->numLinks();
    for(int i=1; i < n; ++i){

        Link* link = body->link(i);

        link->q  =  q0[i] + dt * link->dq;
        link->dq = dq0[i] + dt * link->ddq;

        dq[i]  += r * link->dq;
        ddq[i] += r * link->ddq;
    }
}


void ForwardDynamicsABM::calcMotionWithRungeKuttaMethod()
{
    Link* root = body->rootLink();

    if(root->jointType != Link::FIXED_JOINT){
        p0  = root->p;
        R0  = root->R;
        vo0 = root->vo;
        w0  = root->w;
    }

    vo = 0;
    w = 0;
    dvo = 0;
    dw = 0;

    int n = body->numLinks();
    for(int i=1; i < n; ++i){
        Link* link = body->link(i);
        q0[i]  = link->q;
        dq0[i] = link->dq;
        dq[i]  = 0.0;
        ddq[i] = 0.0;
    }

    calcABMLastHalf();

    if(sensorsEnabled){
        updateForceSensors();
    }

    integrateRungeKuttaOneStep(1.0 / 6.0, timeStep / 2.0);
    calcABMPhase1();
    calcABMPhase2();
    calcABMPhase3();

    integrateRungeKuttaOneStep(2.0 / 6.0, timeStep / 2.0);
    calcABMPhase1();
    calcABMPhase2();
    calcABMPhase3();

    integrateRungeKuttaOneStep(2.0 / 6.0, timeStep);
    calcABMPhase1();
    calcABMPhase2();
    calcABMPhase3();

    if(root->jointType != Link::FIXED_JOINT){
        SE3exp(root->p, root->R, p0, R0, w0, vo0, timeStep);
        root->vo = vo0 + (dvo + root->dvo / 6.0) * timeStep;
        root->w  = w0  + (dw  + root->dw  / 6.0) * timeStep;
    }

    for(int i=1; i < n; ++i){
        Link* link = body->link(i);
        link->q  =  q0[i] + ( dq[i] + link->dq  / 6.0) * timeStep;
        link->dq = dq0[i] + (ddq[i] + link->ddq / 6.0) * timeStep;
    }
}


void ForwardDynamicsABM::calcABMPhase1()
{
    const LinkTraverse& traverse = body->linkTraverse();
    int n = traverse.numLinks();

    for(int i=0; i < n; ++i){
        Link* link = traverse[i];
        Link* parent = link->parent;

        if(parent){
            switch(link->jointType){
                
            case Link::ROTATIONAL_JOINT:
                link->R = parent->R * rodrigues(link->a, link->q);
                link->p = parent->R * link->b + parent->p;
                link->sw = parent->R * link->a;
                link->sv = cross(link->p, link->sw);
                link->w = link->dq * link->sw + parent->w;
                break;
                
            case Link::SLIDE_JOINT:
                link->p = parent->R * (link->b + link->q * link->d) + parent->p;
                link->R = parent->R;
                link->sw = 0.0;
                link->sv = parent->R * link->d;
                link->w = parent->w;
                break;
                
            case Link::FIXED_JOINT:
            default:
                link->p = parent->R * link->b + parent->p;
                link->R = parent->R;
                link->w = parent->w;
                link->vo = parent->vo;
                link->sw = 0.0;
                link->sv = 0.0;
                link->cv = 0.0;
                link->cw = 0.0;
                goto COMMON_CALCS_FOR_ALL_JOINT_TYPES;
            }
            
            // Common for ROTATE and SLIDE
            link->vo = link->dq * link->sv + parent->vo;
            Vector3 dsv(cross(parent->w, link->sv) + cross(parent->vo, link->sw));
            Vector3 dsw(cross(parent->w, link->sw));
            link->cv = link->dq * dsv;
            link->cw = link->dq * dsw;
        }
        
	COMMON_CALCS_FOR_ALL_JOINT_TYPES:

        link->v = link->vo + cross(link->w, link->p);
	
        link->wc = link->R * link->c + link->p;
        
        // compute I^s (Eq.(6.24) of Kajita's textbook))
        Matrix33 Iw(Matrix33(link->R * link->I)  * trans(link->R));
        
        Matrix33 c_hat(hat(link->wc));
        link->Iww = link->m * (c_hat * trans(c_hat)) + Iw;
        
        link->Ivv =
            link->m, 0.0,     0.0,
            0.0,     link->m, 0.0,
            0.0,     0.0,     link->m;
        
        link->Iwv = link->m * c_hat;
        
        // compute P and L (Eq.(6.25) of Kajita's textbook)
        Vector3 P(link->m * (link->vo + cross(link->w, link->wc)));
        Vector3 L(link->Iww * link->w + link->m * cross(link->wc, link->vo));
        
        link->pf = cross(link->w, P);
        link->ptau = cross(link->vo, P) + cross(link->w, L);
        
        Vector3 fg(-link->m * g);
        Vector3 tg(cross(link->wc, fg));
        
        link->pf -= fg;
        link->ptau -= tg;
    }
}


void ForwardDynamicsABM::calcABMPhase2()
{
    const LinkTraverse& traverse = body->linkTraverse();
    int n = traverse.numLinks();

    for(int i = n-1; i >= 0; --i){
        Link* link = traverse[i];

        link->pf   -= link->fext;
        link->ptau -= link->tauext;

        // compute articulated inertia (Eq.(6.48) of Kajita's textbook)
        for(Link* child = link->child; child; child = child->sibling){

            if(child->jointType == Link::FIXED_JOINT){
                link->Ivv += child->Ivv;
                link->Iwv += child->Iwv;
                link->Iww += child->Iww;

            }else{
                Vector3 hhv_dd(child->hhv / child->dd);
                link->Ivv += child->Ivv - VVt_prod(child->hhv, hhv_dd);
                link->Iwv += child->Iwv - VVt_prod(child->hhw, hhv_dd);
                link->Iww += child->Iww - VVt_prod(child->hhw, Vector3(child->hhw / child->dd));
            }

            link->pf   += child->Ivv * child->cv + trans(child->Iwv) * child->cw + child->pf;
            link->ptau += child->Iwv * child->cv + child->Iww * child->cw + child->ptau;

            if(child->jointType != Link::FIXED_JOINT){  
                double uu_dd = child->uu / child->dd;
                link->pf   += uu_dd * child->hhv;
                link->ptau += uu_dd * child->hhw;
            }
        }

        if(i > 0){
            if(link->jointType != Link::FIXED_JOINT){
                // hh = Ia * s
                link->hhv = link->Ivv * link->sv + trans(link->Iwv) * link->sw;
                link->hhw = link->Iwv * link->sv + link->Iww * link->sw;
                // dd = Ia * s * s^T
                link->dd = dot(link->sv, link->hhv) + dot(link->sw, link->hhw) + link->Jm2;
                // uu = u - hh^T*c + s^T*pp
                link->uu = link->u - (dot(link->hhv, link->cv) + dot(link->hhw, link->cw)
                                      + dot(link->sv, link->pf) + dot(link->sw, link->ptau));
            }
        }
    }
}


// A part of phase 2 (inbound loop) that can be calculated before external forces are given
void ForwardDynamicsABM::calcABMPhase2Part1()
{
    const LinkTraverse& traverse = body->linkTraverse();
    int n = traverse.numLinks();

    for(int i = n-1; i >= 0; --i){
        Link* link = traverse[i];

        for(Link* child = link->child; child; child = child->sibling){

            if(child->jointType == Link::FIXED_JOINT){
                link->Ivv += child->Ivv;
                link->Iwv += child->Iwv;
                link->Iww += child->Iww;

            }else{
                Vector3 hhv_dd(child->hhv / child->dd);
                link->Ivv += child->Ivv - VVt_prod(child->hhv, hhv_dd);
                link->Iwv += child->Iwv - VVt_prod(child->hhw, hhv_dd);
                link->Iww += child->Iww - VVt_prod(child->hhw, Vector3(child->hhw / child->dd));
            }

            link->pf   += child->Ivv * child->cv + trans(child->Iwv) * child->cw;
            link->ptau += child->Iwv * child->cv + child->Iww * child->cw;
        }

        if(i > 0){
            if(link->jointType != Link::FIXED_JOINT){
                link->hhv = link->Ivv * link->sv + trans(link->Iwv) * link->sw;
                link->hhw = link->Iwv * link->sv + link->Iww * link->sw;
                link->dd  = dot(link->sv, link->hhv) + dot(link->sw, link->hhw) + link->Jm2;
                link->uu  = - (dot(link->hhv, link->cv) + dot(link->hhw, link->cw));
            }
        }
    }
}


// A remaining part of phase 2 that requires external forces
void ForwardDynamicsABM::calcABMPhase2Part2()
{
    const LinkTraverse& traverse = body->linkTraverse();
    int n = traverse.numLinks();

    for(int i = n-1; i >= 0; --i){
        Link* link = traverse[i];

        link->pf   -= link->fext;
        link->ptau -= link->tauext;

        for(Link* child = link->child; child; child = child->sibling){
            link->pf   += child->pf;
            link->ptau += child->ptau;

            if(child->jointType != Link::FIXED_JOINT){  
                double uu_dd = child->uu / child->dd;
                link->pf   += uu_dd * child->hhv;
                link->ptau += uu_dd * child->hhw;
            }
        }

        if(i > 0){
            if(link->jointType != Link::FIXED_JOINT)
                link->uu += link->u - (dot(link->sv, link->pf) + dot(link->sw, link->ptau));
        }
    }
}


void ForwardDynamicsABM::calcABMPhase3()
{
    const LinkTraverse& traverse = body->linkTraverse();

    Link* root = traverse[0];

    if(root->jointType == Link::FREE_JOINT){

        // - | Ivv  trans(Iwv) | * | dvo | = | pf   |
        //   | Iwv     Iww     |   | dw  |   | ptau |

        ublas::bounded_matrix<double, 6, 6, ublas::column_major> Ia;
        setMatrix33(root->Ivv, Ia, 0, 0);
        setTransMatrix33(root->Iwv, Ia, 0, 3);
        setMatrix33(root->Iwv, Ia, 3, 0);
        setMatrix33(root->Iww, Ia, 3, 3);
        
        ublas::bounded_vector<double, 6> p;
        setVector3(root->pf, p, 0);
        setVector3(root->ptau, p, 3);
        p *= -1.0;
        
        ublas::permutation_matrix<std::size_t> pm(6);
        ublas::lu_factorize(Ia, pm);
        ublas::lu_substitute(Ia, pm, p);

        getVector3(root->dvo, p, 0);
        getVector3(root->dw, p, 3);

    } else {
        root->dvo = 0.0;
        root->dw = 0.0;
    }

    int n = traverse.numLinks();
    for(int i=1; i < n; ++i){
        Link* link = traverse[i];
        Link* parent = link->parent;
        if(link->jointType != Link::FIXED_JOINT){
            link->ddq = (link->uu - (dot(link->hhv, parent->dvo) + dot(link->hhw, parent->dw))) / link->dd;
            link->dvo = parent->dvo + link->cv + link->sv * link->ddq;
            link->dw  = parent->dw  + link->cw + link->sw * link->ddq;
        }else{
            link->ddq = 0.0;
            link->dvo = parent->dvo;
            link->dw  = parent->dw; 
        }
    }
}


void ForwardDynamicsABM::updateForceSensors()
{
    int n = body->numSensors(Sensor::FORCE);
    for(int i=0; i < n; ++i){
        updateForceSensor(body->sensor<ForceSensor>(i));
    }
}


void ForwardDynamicsABM::updateForceSensor(ForceSensor* sensor)
{
	Link* link = sensor->link;

	//    | f   | = | Ivv  trans(Iwv) | * | dvo | + | pf   |
	//    | tau |   | Iwv     Iww     |   | dw  |   | ptau |
	
	Vector3 f  (-(link->Ivv * link->dvo + trans(link->Iwv) * link->dw + link->pf));
	Vector3 tau(-(link->Iwv * link->dvo + link->Iww * link->dw + link->ptau));

	Matrix33 sensorR(link->R * sensor->localR);
	Vector3 fs(trans(sensorR) * f);
	Vector3 sensorPos(link->p + link->R * sensor->localPos);
	Vector3 ts(trans(sensorR) * (tau - cross(sensorPos, f)));

	sensor->f   = fs;
	sensor->tau = ts;

	if(debugMode){
		cout << "sensor " << sensor->name << ": ";
		cout << "f = " << f;
		cout << "R = " << sensorR;
		cout << "sensor->f = " << sensor->f;
		cout << endl;
	}
}



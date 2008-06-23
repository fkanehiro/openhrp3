// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/**
   \file
   \author S.NAKAOKA
*/

#include <boost/numeric/ublas/triangular.hpp>
#include <boost/numeric/ublas/lu.hpp>

#include "Body.h"
#include "Link.h"
#include "LinkTraverse.h"
#include "Sensor.h"
#include "ForwardDynamicsCBM.h"

using namespace OpenHRP;
using namespace std;
using namespace tvmet;
using namespace boost::numeric;


static const bool CALC_ALL_JOINT_TORQUES = false;
static const bool ROOT_ATT_NORMALIZATION_ENABLED = false;

static const bool debugMode = false;

#include <boost/format.hpp>

template<class TMatrix>
static void putMatrix(TMatrix& M, char* name)
{
	if(M.size2() == 1){
		std::cout << "Vector " << name << M << std::endl;
	} else {
		std::cout << "Matrix " << name << ": \n";
		for(size_t i=0; i < M.size1(); i++){
			for(size_t j=0; j < M.size2(); j++){
				std::cout << boost::format(" %6.3f ") % M(i, j);
			}
			std::cout << std::endl;
		}
	}
}

template<class TVector>
static void putVector(TVector& M, char* name)
{
	std::cout << "Vector " << name << M << std::endl;
}


ForwardDynamicsMM::ForwardDynamicsMM(BodyPtr body) :
    ForwardDynamics(body)
{

}


ForwardDynamicsMM::~ForwardDynamicsMM()
{

}


void ForwardDynamicsMM::initialize()
{
	rootDof = (body->rootLink()->jointType == Link::FREE_JOINT) ? 6 : 0;

	torqueModeJoints.clear();
	highGainModeJoints.clear();

	int numLinks = body->numLinks();

	for(int i=1; i < numLinks; ++i){
		Link* link = body->link(i);
		int jointType = link->jointType;
		if(jointType == Link::ROTATIONAL_JOINT || jointType == Link::SLIDE_JOINT){
			if(link->isHighGainMode){
				highGainModeJoints.push_back(link);
			} else {
				torqueModeJoints.push_back(link);
			}
		}
	}

	int n = rootDof + torqueModeJoints.size();
	int m = highGainModeJoints.size();

	isNoUnknownAccelMode = (n == 0);

	M11.resize(n, n);
	M12.resize(n, m);
	b1. resize(n, 1);
	c1. resize(n);

	qGiven.  resize(m);
	dqGiven. resize(m);
	ddqGiven.resize(m, 1);

	qGivenPrev. resize(m);
	dqGivenPrev.resize(m);

	ddqorg.resize(numLinks);
	uorg.  resize(numLinks);

	calcPositionAndVelocityFK();

	if(!isNoUnknownAccelMode){
		calcMassMatrix();
	}

	ddqGivenCopied = false;

	initializeSensors();

	if(integrationMode == RUNGEKUTTA_METHOD){

		q0. resize(numLinks);
		dq0.resize(numLinks);
		dq. resize(numLinks);
		ddq.resize(numLinks);

		preserveHighGainModeJointState();
	}
}


inline void ForwardDynamicsMM::solveUnknownAccels()
{
	initializeAccelSolver();
	solveUnknownAccels(fextTotal, tauextTotal);
}


inline void ForwardDynamicsMM::calcAccelFKandForceSensorValues()
{
	vector3 f, tau;
	calcAccelFKandForceSensorValues(body->rootLink(), f, tau);
}


void ForwardDynamicsMM::calcNextState()
{
	if(isNoUnknownAccelMode){

		calcPositionAndVelocityFK();

	} else {

		switch(integrationMode){

		case EULER_METHOD:
			calcMotionWithEulerMethod();
			break;
			
		case RUNGEKUTTA_METHOD:
			calcMotionWithRungeKuttaMethod();
			break;
		}
		
		if(ROOT_ATT_NORMALIZATION_ENABLED && rootDof){
			Link* root = body->rootLink();
			vector3 x0;
			getVector3(x0, root->R, 0, 0);
			vector3 y0;
			getVector3(y0, root->R, 0, 1);
			vector3 x(normalize(x0));
			vector3 z(normalize((cross(x, y0))));
			vector3 y(cross(z, x));
			setVector3(x, root->R, 0, 0);
			setVector3(y, root->R, 0, 1);
			setVector3(z, root->R, 0, 2);
		}
	}
		
	if(sensorsEnabled){
		updateSensorsFinal();
	}
	
	body->setVirtualJointForces();

	ddqGivenCopied = false;
}


void ForwardDynamicsMM::calcMotionWithEulerMethod()
{
	sumExternalForces();
	solveUnknownAccels();
	calcAccelFKandForceSensorValues();

    Link* root = body->rootLink();

    if(rootDof){
        vector3  p;
        matrix33 R;
        SE3exp(p, R, root->p, root->R, root->w, root->vo, timeStep);
        root->p = p;
        root->R = R;

        root->vo += root->dvo * timeStep;
		root->w  += root->dw  * timeStep;
    }

	int n = torqueModeJoints.size();
	for(int i=0; i < n; ++i){
		Link* link = torqueModeJoints[i];
		link->q  += link->dq  * timeStep;
        link->dq += link->ddq * timeStep;
	}

	calcPositionAndVelocityFK();
	calcMassMatrix();
}


void ForwardDynamicsMM::calcMotionWithRungeKuttaMethod()
{
	int numHighGainJoints = highGainModeJoints.size();

	for(int i=0; i < numHighGainJoints; ++i){
		Link* link = highGainModeJoints[i];
		qGiven [i] = link->q;
		dqGiven[i] = link->dq;
		link->q  = qGivenPrev[i];
		link->dq = dqGivenPrev[i];
	}

    Link* root = body->rootLink();

    if(rootDof){
		p0  = root->p;
		R0  = root->R;
		vo0 = root->vo;
		w0  = root->w;
    }

    vo  = 0;
    w   = 0;
    dvo = 0;
    dw  = 0;

	int numLinks = body->numLinks();

	for(int i=1; i < numLinks; ++i){
		Link* link = body->link(i);
        q0 [i] = link->q;
		dq0[i] = link->dq;
		dq [i] = 0.0;
		ddq[i] = 0.0;
	}

	sumExternalForces();

	solveUnknownAccels();
	calcAccelFKandForceSensorValues();

    integrateRungeKuttaOneStep(1.0 / 6.0, timeStep / 2.0);

	calcPositionAndVelocityFK();
	calcMassMatrix();
	solveUnknownAccels();

    integrateRungeKuttaOneStep(2.0 / 6.0, timeStep / 2.0);

	calcPositionAndVelocityFK();
	calcMassMatrix();
	solveUnknownAccels();

    integrateRungeKuttaOneStep(2.0 / 6.0, timeStep);

	calcPositionAndVelocityFK();
	calcMassMatrix();
	solveUnknownAccels();

    if(rootDof){
        SE3exp(root->p, root->R, p0, R0, w0, vo0, timeStep);
        root->vo = vo0 + (dvo + root->dvo / 6.0) * timeStep;
		root->w  = w0  + (dw  + root->dw  / 6.0) * timeStep;
    }

    for(size_t i=0; i < torqueModeJoints.size(); ++i){
		Link* link = torqueModeJoints[i];
		int index = link->index;
		link->q  = q0 [index] + (dq [index] + link->dq  / 6.0) * timeStep;
		link->dq = dq0[index] + (ddq[index] + link->ddq / 6.0) * timeStep;
    }
	for(size_t i=0; i < highGainModeJoints.size(); ++i){
		Link* link = highGainModeJoints[i];
		link->q  = qGiven [i];
		link->dq = dqGiven[i];
	}

	calcPositionAndVelocityFK();
	calcMassMatrix();

	preserveHighGainModeJointState();
}


void ForwardDynamicsMM::integrateRungeKuttaOneStep(double r, double dt)
{
    Link* root = body->rootLink();

    if(rootDof){
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
		link->q  = q0 [i] + dt * link->dq;
		link->dq = dq0[i] + dt * link->ddq;
		dq [i] += r * link->dq;
		ddq[i] += r * link->ddq;
	}
}


void ForwardDynamicsMM::preserveHighGainModeJointState()
{
	for(size_t i=0; i < highGainModeJoints.size(); ++i){
		Link* link = highGainModeJoints[i];
		qGivenPrev [i] = link->q;
		dqGivenPrev[i] = link->dq;
	}
}


void ForwardDynamicsMM::calcPositionAndVelocityFK()
{
    const LinkTraverse& traverse = body->linkTraverse();
    int n = traverse.numLinks();

	Link* root = traverse[0];
	root_w_x_v = cross(root->w, vector3(root->vo + cross(root->w, root->p)));

    for(int i=0; i < n; ++i){
        Link* link = traverse[i];
        Link* parent = link->parent;

		if(parent){

			switch(link->jointType){

			case Link::SLIDE_JOINT:
				link->p  = parent->R * (link->b + link->q * link->d) + parent->p;
				link->R  = parent->R;
				link->sw = 0.0;
				link->sv = parent->R * link->d;
				link->w  = parent->w;
				break;

			case Link::ROTATIONAL_JOINT:
			default:
				link->R  = parent->R * rodrigues(link->a, link->q);
				link->p  = parent->R * link->b + parent->p;
				link->sw = parent->R * link->a;
				link->sv = cross(link->p, link->sw);
				link->w  = link->dq * link->sw + parent->w;
				break;
			}

			link->vo = link->dq * link->sv + parent->vo;

			vector3 dsv(cross(parent->w, link->sv) + cross(parent->vo, link->sw));
			vector3 dsw(cross(parent->w, link->sw));
			link->cv = link->dq * dsv;
			link->cw = link->dq * dsw;
		}

		/// \todo remove this  equation
		link->v = link->vo + cross(link->w, link->p);

		link->wc = link->R * link->c + link->p;
		matrix33 Iw(matrix33(link->R * link->I)  * trans(link->R));
        matrix33 c_hat(hat(link->wc));
		link->Iww = link->m * (c_hat * trans(c_hat)) + Iw;
		link->Iwv = link->m * c_hat;

		vector3 P(link->m * (link->vo + cross(link->w, link->wc)));
		vector3 L(link->Iww * link->w + link->m * cross(link->wc, link->vo));

		link->pf   = cross(link->w,  P);
		link->ptau = cross(link->vo, P) + cross(link->w, L);
	}
}


/**
   calculate the mass matrix using the unit vector method
   \todo replace the unit vector method here with
   a more efficient method that only requires O(n) computation time
*/
void ForwardDynamicsMM::calcMassMatrix()
{
	Link* root = body->rootLink();
	int numLinks = body->numLinks();

	// preserve and clear the joint accelerations
	for(int i=1; i < numLinks; ++i){
		Link* link = body->link(i);
		ddqorg[i] = link->ddq;
		uorg  [i] = link->u;
		link->ddq = 0.0;
	}

	// preserve and clear the root link acceleration
	dvoorg = root->dvo;
	dworg  = root->dw;
	root->dvo = g - root_w_x_v;   // dv = g, dw = 0
	root->dw  = 0.0;
	
	setColumnOfMassMatrix(b1, 0);

	if(rootDof){
		for(int i=0; i < 3; ++i){
			root->dvo[i] += 1.0;
			setColumnOfMassMatrix(M11, i);
			root->dvo[i] -= 1.0;
		}
		for(int i=0; i < 3; ++i){
			root->dw[i] = 1.0;
			vector3 dw_x_p = cross(root->dw, root->p);
			root->dvo -= dw_x_p;
			setColumnOfMassMatrix(M11, i + 3);
			root->dvo += dw_x_p;
			root->dw[i] = 0.0;
		}
	}

	for(size_t i=0; i < torqueModeJoints.size(); ++i){
		Link* link = torqueModeJoints[i];
		link->ddq = 1.0;
		int j = i + 6;
		setColumnOfMassMatrix(M11, j);
		M11(j, j) += link->Jm2; // motor inertia
		link->ddq = 0.0;
    }
    for(size_t i=0; i < highGainModeJoints.size(); ++i){
		Link* link = highGainModeJoints[i];
		link->ddq = 1.0;
		setColumnOfMassMatrix(M12, i);
		link->ddq = 0.0;
    }

	// subtract the constant term
	ublas::matrix_column<dmatrix> vb1(b1, 0);
	for(size_t i=0; i < M11.size2(); ++i){
		ublas::matrix_column<dmatrix>(M11, i) -= vb1;
	}
	for(size_t i=0; i < M12.size2(); ++i){
		ublas::matrix_column<dmatrix>(M12, i) -= vb1;
	}

	for(int i=1; i < numLinks; ++i){
		Link* link = body->link(i);
		link->ddq = ddqorg[i];
		link->u   = uorg  [i];
	}
	root->dvo = dvoorg;
	root->dw  = dworg;

	accelSolverInitialized = false;
}


void ForwardDynamicsMM::setColumnOfMassMatrix(dmatrix& M, int column)
{
    vector3 f;
	vector3 tau;
    Link* root = body->rootLink();
    calcInverseDynamics(root, f, tau);

	if(rootDof){
		tau -= cross(root->p, f);
		setVector3(f,   M, 0, column);
		setVector3(tau, M, 3, column);
	}

	for(size_t i=0; i < torqueModeJoints.size(); ++i){
		M(i + 6, column) = torqueModeJoints[i]->u;
    }
}


void ForwardDynamicsMM::calcInverseDynamics(Link* link, vector3& out_f, vector3& out_tau)
{
    Link* parent = link->parent;
    if(parent){
		link->dvo = parent->dvo + link->cv + link->sv * link->ddq;
		link->dw  = parent->dw  + link->cw + link->sw * link->ddq;
    }

    out_f = link->pf;
    out_tau = link->ptau;

    if(link->child){
		vector3 f_c;
		vector3 tau_c;
		calcInverseDynamics(link->child, f_c, tau_c);
		out_f += f_c;
		out_tau += tau_c;
    }

    out_f   += link->m   * link->dvo + trans(link->Iwv) * link->dw;
    out_tau += link->Iwv * link->dvo + link->Iww        * link->dw;

    link->u = dot(link->sv, out_f) + dot(link->sw, out_tau);

    if(link->sibling){
		vector3 f_s;
		vector3 tau_s;
		calcInverseDynamics(link->sibling, f_s, tau_s);
		out_f += f_s;
		out_tau += tau_s;
    }
}


void ForwardDynamicsMM::sumExternalForces()
{
	fextTotal   = 0.0;
	tauextTotal = 0.0;

    int n = body->numLinks();
    for(int i=0; i < n; ++i){
		Link* link = body->link(i);
		fextTotal   += link->fext;
		tauextTotal += link->tauext;
    }

	tauextTotal -= cross(body->rootLink()->p, fextTotal);
}


void ForwardDynamicsMM::initializeAccelSolver()
{
	if(!accelSolverInitialized){

		if(!ddqGivenCopied){
			for(size_t i=0; i < highGainModeJoints.size(); ++i){
				ddqGiven(i,0) = highGainModeJoints[i]->ddq;
			}
			ddqGivenCopied = true;
		}

		b1 += prod(M12, ddqGiven);

		accelSolverInitialized = true;
	}
}


void ForwardDynamicsMM::solveUnknownAccels(const vector3& fext, const vector3& tauext)
{
	setVector3(fext,   c1, 0);
	setVector3(tauext, c1, 3);

	for(size_t i=0; i < torqueModeJoints.size(); ++i){
		c1(i + 6) = torqueModeJoints[i]->u;
	}

	c1 -= ublas::matrix_column<dmatrix>(b1, 0);

	dmatrix M = M11;
	ublas::permutation_matrix<std::size_t> pm(c1.size());
	ublas::lu_factorize(M, pm);
	ublas::lu_substitute(M, pm, c1);

	if(rootDof){
		Link* root = body->rootLink();
		getVector3(root->dw,  c1, 3);
		vector3 dv;
		getVector3(dv, c1, 0);
		root->dvo = dv - cross(root->dw, root->p) - root_w_x_v;
	}

	for(size_t i=0; i < torqueModeJoints.size(); ++i){
		Link* link = torqueModeJoints[i];
		link->ddq = c1(i + 6);
	}
}


void ForwardDynamicsMM::calcAccelFKandForceSensorValues(Link* link, vector3& out_f, vector3& out_tau)
{
    Link* parent = link->parent;

    if(parent){
		link->dvo = parent->dvo + link->cv + link->sv * link->ddq;
		link->dw  = parent->dw  + link->cw + link->sw * link->ddq;
    }

    out_f   = link->pf;
    out_tau = link->ptau;

	for(Link* child = link->child; child; child = child->sibling){
		vector3 f, tau;
		calcAccelFKandForceSensorValues(child, f, tau);
		out_f   += f;
		out_tau += tau;
    }

	ForceSensorInfo& info = forceSensorInfo[link->index];

	if(CALC_ALL_JOINT_TORQUES || info.hasSensorsAbove){

        vector3 fg(-link->m * g);
        vector3 tg(cross(link->wc, fg));

		out_f   -= fg;
		out_tau -= tg;

		out_f   -= link->fext;
		out_tau -= link->tauext;

		out_f   += link->m   * link->dvo + trans(link->Iwv) * link->dw;
		out_tau += link->Iwv * link->dvo + link->Iww        * link->dw;

		if(CALC_ALL_JOINT_TORQUES && link->isHighGainMode){
			link->u = dot(link->sv, out_f) + dot(link->sw, out_tau);
		}

		if(info.sensor){
			ForceSensor* sensor = info.sensor;
			matrix33 sensorR  (link->R * sensor->localR);
			vector3  sensorPos(link->p + link->R * sensor->localPos);
			vector3 f(-out_f);
			sensor->f   = trans(sensorR) * f;
			sensor->tau = trans(sensorR) * (-out_tau - cross(sensorPos, f));
		}
	}
}


void ForwardDynamicsMM::initializeSensors()
{
	ForwardDynamics::initializeSensors();

	int n = body->numLinks();

	forceSensorInfo.resize(n);

	for(int i=0; i < n; ++i){
		forceSensorInfo[i].sensor = 0;
		forceSensorInfo[i].hasSensorsAbove = false;
	}

	if(sensorsEnabled){

		int numForceSensors = body->numSensors(Sensor::FORCE);
		for(int i=0; i < numForceSensors; ++i){
			ForceSensor* sensor = body->sensor<ForceSensor>(i);
			forceSensorInfo[sensor->link->index].sensor = sensor;
		}

		updateForceSensorInfo(body->rootLink(), false);
	}
}


void ForwardDynamicsMM::updateForceSensorInfo(Link* link, bool hasSensorsAbove)
{
	ForceSensorInfo& info = forceSensorInfo[link->index];
	hasSensorsAbove |= (info.sensor != 0);
	info.hasSensorsAbove = hasSensorsAbove;

	for(Link* child = link->child; child; child = child->sibling){
		updateForceSensorInfo(child, hasSensorsAbove);
    }
}

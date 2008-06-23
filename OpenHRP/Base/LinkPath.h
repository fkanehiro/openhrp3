// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-

/** \file
    \brief The header file of the LinkPath and JointPath classes
    \author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_LINK_PATH_H_INCLUDED
#define OPENHRP_LINK_PATH_H_INCLUDED

#include <vector>
#include <ostream>
#include <boost/shared_ptr.hpp>

#include "tvmet3d.h"
#include "ublasCommonTypes.h"
#include "LinkTraverse.h"

#include "hrpModelExportDef.h"


namespace OpenHRP {

	class HRPMODEL_EXPORT LinkPath : public LinkTraverse
	{
	public:
		LinkPath();
		LinkPath(Link* root, Link* end);
		/// set path from the root link
		LinkPath(Link* end);
		
		/**
		   true when the path is not empty
		*/
		inline operator bool() const {
			return !links.empty();
		}

		inline Link* endLink() const {
			return links.back();
		}
		
		bool find(Link* root, Link* end);
		void findPathFromRoot(Link* end);
		
	private:
		bool findPathSub(Link* link, Link* prev, Link* end, bool isForwardDirection);
		void findPathFromRootSub(Link* link);
	};


	class HRPMODEL_EXPORT JointPath : public LinkPath
    {
    public:
		
		JointPath();
		JointPath(Link* root, Link* end);
		/// set path from the root link
		JointPath(Link* end);
		virtual ~JointPath();
		
		bool find(Link* root, Link* end);
		bool findPathFromRoot(Link* end);
		
		inline int numJoints() const {
			return joints.size();
		}
		
		inline Link* joint(int index) const {
			return joints[index];
		}
		
		inline bool isJointDownward(int index) const {
			return (index >= numUpwardJointConnections);
		}
		
		void setMaxIKError(double e);
		
		void calcJacobian(dmatrix& out_J) const;
		
		inline dmatrix Jacobian() const {
			dmatrix J;
			calcJacobian(J);
			return J;
		}
		
		virtual bool calcInverseKinematics
		(const vector3& from_p, const matrix33& from_R, const vector3& to_p, const matrix33& to_R);
		
		virtual bool calcInverseKinematics(const vector3& to_p, const matrix33& to_R);

		virtual bool hasAnalyticalIK();
		
		/**
		   @deprecated use operator<<
		*/
		void putInformation(std::ostream& os) const;
		
    protected:
		
		virtual void onJointPathUpdated();
		
		double maxIkErrorSqr;
		
    private:
		
		void initialize();
		void extractJoints();
		
		std::vector<Link*> joints;
		int numUpwardJointConnections;
    };

	typedef boost::shared_ptr<JointPath> JointPathPtr;
	
};


HRPMODEL_EXPORT std::ostream& operator<<(std::ostream& os, OpenHRP::LinkTraverse& traverse);


#endif

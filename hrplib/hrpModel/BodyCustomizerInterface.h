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
   \brief The definitions of the body customizer interface for increasing binary compatibility
   \author Shin'ichiro Nakaoka
*/

#ifndef HRPMODEL_BODY_CUSTOMIZER_INTERFACE_H_INCLUDED
#define HRPMODEL_BODY_CUSTOMIZER_INTERFACE_H_INCLUDED

#include "Config.h"
#include <string>
#include <hrpUtil/Tvmet3d.h>

namespace hrp {

    typedef void* BodyHandle;
    typedef void* BodyCustomizerHandle;

    typedef int         (*BodyGetLinkIndexFromNameFunc) (BodyHandle bodyHandle, const char* linkName);
    typedef const char* (*BodyGetLinkNameFunc)          (BodyHandle bodyHandle, int linkIndex);
    typedef double*     (*BodyGetLinkDoubleValuePtrFunc)(BodyHandle bodyHandle, int linkIndex);

    static const int BODY_INTERFACE_VERSION = 1;

    struct BodyInterface
    {
        int version;
		
        BodyGetLinkIndexFromNameFunc   getLinkIndexFromName;
        BodyGetLinkNameFunc            getLinkName;
        BodyGetLinkDoubleValuePtrFunc  getJointValuePtr;
        BodyGetLinkDoubleValuePtrFunc  getJointVelocityPtr;
        BodyGetLinkDoubleValuePtrFunc  getJointForcePtr;
    };
    
    typedef const char** (*BodyCustomizerGetTargetModelNamesFunc)();
    typedef BodyCustomizerHandle (*BodyCustomizerCreateFunc)(BodyHandle bodyHandle, const char* modelName);
	
    typedef void (*BodyCustomizerDestroyFunc)              (BodyCustomizerHandle customizerHandle);
    typedef int  (*BodyCustomizerInitializeAnalyticIkFunc) (BodyCustomizerHandle customizerHandle, int baseLinkIndex, int targetLinkIndex);

    /*
      p and R are based on the coordinate of a base link
    */
    typedef bool (*BodyCustomizerCalcAnalyticIkFunc)       (BodyCustomizerHandle customizerHandle, int ikPathId, const Vector3& p, const Matrix33& R);
	
    typedef void (*BodyCustomizerSetVirtualJointForcesFunc)(BodyCustomizerHandle customizerHandle);
	

    static const int BODY_CUSTOMIZER_INTERFACE_VERSION = 1;

    struct BodyCustomizerInterface
    {
        int version;

        BodyCustomizerGetTargetModelNamesFunc getTargetModelNames;
        BodyCustomizerCreateFunc create;
        BodyCustomizerDestroyFunc destroy;
        BodyCustomizerInitializeAnalyticIkFunc initializeAnalyticIk;
        BodyCustomizerCalcAnalyticIkFunc calcAnalyticIk;
        BodyCustomizerSetVirtualJointForcesFunc setVirtualJointForces;
    };

    typedef BodyCustomizerInterface* (*GetBodyCustomizerInterfaceFunc)(BodyInterface* bodyInterface);

    HRPMODEL_API int loadBodyCustomizers(const std::string pathString, BodyInterface* bodyInterface);
    HRPMODEL_API int loadBodyCustomizers(const std::string pathString);
    HRPMODEL_API int loadBodyCustomizers(BodyInterface* bodyInterface);
    HRPMODEL_API int loadBodyCustomizers();
    
    HRPMODEL_API BodyCustomizerInterface* findBodyCustomizer(std::string modelName);

}
    
#endif

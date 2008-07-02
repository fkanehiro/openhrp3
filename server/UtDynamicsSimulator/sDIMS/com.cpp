/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * handle.cpp
 * Create: Katsu Yamane, 03.07.10
 */

#include "ik.h"

int IKCom::calc_jacobian()
{
	if(n_const == 0) return 0;
	int n_dof = ik->NumDOF();
	fMat Jtemp;
	J.resize(n_const, n_dof);
	Jtemp.resize(3, n_dof);
	Jtemp.zero();
	ik->ComJacobian(Jtemp, cur_com, charname);
	int i, j, count = 0;
	for(i=0; i<3; i++)
	{
		if(const_index[i] == IK::HAVE_CONSTRAINT)
		{
			for(j=0; j<n_dof; j++)
				J(count, j) = Jtemp(i, j);
			count++;
		}
	}
	return 0;
}

int IKCom::calc_feedback()
{
	int i, count = 0;
	fVec3 dp;
	dp.sub(des_com, cur_com);
	dp *= gain;
	for(i=0; i<3; i++)
	{
		if(const_index[i] == IK::HAVE_CONSTRAINT)
		{
			fb(count) = dp(i);
			count++;
		}
	}
	return 0;
}


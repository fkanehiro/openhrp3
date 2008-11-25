/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */

package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxServerManagerViewPart extends GrxBaseViewPart {
    public void createPartControl(Composite parent) {
        createView( GrxServerManagerView.class, "Server Manager", this, parent );
    }
}

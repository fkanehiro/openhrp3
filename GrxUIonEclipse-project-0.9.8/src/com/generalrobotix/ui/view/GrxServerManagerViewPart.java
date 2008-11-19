/*
 *  GrxProcessManagerView.java
 *
 *  Copyright (C) 2008 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxServerManagerViewPart extends GrxBaseViewPart {
    public void createPartControl(Composite parent) {
        createView( GrxServerManagerView.class, "Server Manager", this, parent );
    }
}

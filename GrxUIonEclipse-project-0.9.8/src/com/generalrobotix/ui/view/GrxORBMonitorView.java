/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/*
 *  GrxORBMonitorView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxORBMonitor;

@SuppressWarnings("serial")
public class GrxORBMonitorView extends GrxBaseView {
    public static final String TITLE = "NameService Monitor";
    private GrxORBMonitor      monitor_ = null;
    
    public GrxORBMonitorView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);

        monitor_ = new GrxORBMonitor(composite_, SWT.NULL);
        setScrollMinSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    public String getNSHost() {
        return monitor_.getNSHost();
    }

    public String getNSPort() {
        return monitor_.getNSPort();
    }
}

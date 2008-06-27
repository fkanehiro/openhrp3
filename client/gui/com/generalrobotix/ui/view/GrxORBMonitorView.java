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

import java.awt.BorderLayout;
import javax.swing.*;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.GrxORBMonitor;

@SuppressWarnings("serial")
public class GrxORBMonitorView extends GrxBaseView {
    public static final String TITLE = "NameService Monitor";
	private GrxORBMonitor monitor_;

	public GrxORBMonitorView(String name, GrxPluginManager manager) {
		super(name, manager);
		getContentPane().setLayout(new BorderLayout());
		monitor_ = new GrxORBMonitor();
		getContentPane().add(monitor_);
	}

	public void restoreProperties() {
		monitor_.setHosts(new String[]{"localhost", getStr("nsHost", "")});
	}
}

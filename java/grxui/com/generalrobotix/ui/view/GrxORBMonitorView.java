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
    private String nsHost_;
    private int    nsPort_;

    public GrxORBMonitorView(String name, GrxPluginManager manager) {
        super(name, manager);
        getContentPane().setLayout(new BorderLayout());
        nsHost_ = System.getenv("NS_HOST");
        if (nsHost_ == null) {
            nsHost_ = "localhost";
        }
        try {
            nsPort_ = Integer.parseInt(System.getenv("NS_PORT"));
        } catch (Exception e) {
            nsPort_ = 2809;
        }
        monitor_ = new GrxORBMonitor(nsHost_, Integer.toString(nsPort_));
        getContentPane().add(monitor_);
    }

    public void restoreProperties() {
        if (nsHost_.equals("localhost")) {
            monitor_.setHosts(new String[]{"localhost"});
        } else {
            monitor_.setHosts(new String[]{nsHost_, "localhost"});
        }
        monitor_.setPort(Integer.toString(nsPort_));
    }
}

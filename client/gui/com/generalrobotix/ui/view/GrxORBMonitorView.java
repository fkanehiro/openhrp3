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

import javax.swing.JPanel;

import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxORBMonitor;

@SuppressWarnings("serial")
public class GrxORBMonitorView extends GrxBaseView {
    public static final String TITLE = "NameService Monitor";

	public GrxORBMonitorView(String name, GrxPluginManager manager) {
		super(name, manager);

		GrxORBMonitor monitor = new GrxORBMonitor();
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(monitor);
	}
}

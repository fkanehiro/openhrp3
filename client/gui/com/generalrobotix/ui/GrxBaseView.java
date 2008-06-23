/*
 *  GrxBaseView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.awt.Dimension;
import java.util.List;

import javax.swing.*;

public abstract class GrxBaseView extends GrxBasePlugin implements GrxBaseController {
	private JPanel contentPane_ = null;
	private JComponent toolBar_ = null;
	private static Dimension defaultButtonSize_ = new Dimension(27, 27);
	public boolean isScrollable_ = true;
	
	public double min, max, now;
	int view_state_ = GRX_VIEW_SLEEP;
	static final int GRX_VIEW_SETUP   = 0;
	static final int GRX_VIEW_ACTIVE  = 1;
	static final int GRX_VIEW_CLEANUP = 2;
	static final int GRX_VIEW_SLEEP   = 3;

	public GrxBaseView(String name, GrxPluginManager manager) {
		super(name, manager);
		//menuPath_ = new String[]{"View"};
	}

	public void setName(String name) {
		super.setName(name);
		getContentPane().setName(name);
	}

	public final JPanel getContentPane() {
		if (contentPane_ == null) {
			contentPane_ = new JPanel();
			contentPane_.setName(getName());
		}
		return contentPane_;
	};

	public void setToolBar(JComponent toolBar) {
		toolBar_ = toolBar;
	};

	public JComponent getToolBar() {
		return toolBar_;
	};

	public void setToolBarVisible(boolean visible) {
		if (toolBar_ != null)
			toolBar_.isVisible();
	}

	public boolean isToolBarVisible() {
		return toolBar_ != null || toolBar_.isVisible();
	}

	public static Dimension getDefaultButtonSize() {
		return defaultButtonSize_;
	}

	public void start() {
		if (view_state_ == GRX_VIEW_SLEEP)
			view_state_ = GRX_VIEW_SETUP;
	}

	public void stop() {
		if (view_state_ == GRX_VIEW_ACTIVE)
			view_state_ = GRX_VIEW_CLEANUP;
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList){}
	public boolean setup(List<GrxBaseItem> itemList){return true;}
	public void control(List<GrxBaseItem> itemList){}
	public boolean cleanup(List<GrxBaseItem> itemList){return true;}

	public boolean isRunning() {
		return (view_state_ == GRX_VIEW_ACTIVE);
	}

	public boolean isSleeping() {
		return (view_state_ == GRX_VIEW_SLEEP);
	}
}

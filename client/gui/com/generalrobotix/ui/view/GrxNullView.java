/*
 *  GrxNullView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.*;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxNullView extends GrxBaseView {
    public static final String TITLE = "Null";

	private JLabel label_ = new JLabel("Null View");
	private JToggleButton button_ = new JToggleButton("Hello");

	public GrxNullView(String name, GrxPluginManager manager) {
		super(name, manager);

		getContentPane().add(label_);

		button_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (button_.isSelected())
					label_.setText("Hello World!!");
				else
					label_.setText("Null View");
			}
		});

		JToolBar toolbar = new JToolBar();
		toolbar.add(button_);
		setToolBar(toolbar);
	}
	
	public void restoreProperties() {
		super.restoreProperties();
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void control(List<GrxBaseItem> itemList) {

	}

	public boolean cleanup(List<GrxBaseItem> itemList) {
		return true;
	}
}

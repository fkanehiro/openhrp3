/*
 *  GrxGraphItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import javax.swing.ImageIcon;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxGraphItem extends GrxBaseItem {
	public static final String TITLE = "Graph Contents";

	public GrxGraphItem(String name, GrxPluginManager manager) {
		super(name, manager);
		setExclusive(true);
		setIcon(new ImageIcon(getClass().getResource("/resources/images/graph.png")));
	}
	
	public boolean create() {
		return true;
	}
}

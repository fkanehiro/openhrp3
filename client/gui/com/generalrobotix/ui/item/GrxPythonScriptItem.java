/*
 *  GrxPythonScriptItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxPythonScriptItem extends GrxTextItem {
	public static final String TITLE = "Python Script";
	public static final String DEFAULT_DIR = "script";
	public static final String FILE_EXTENSION = "py";
	
	public GrxPythonScriptItem(String name, GrxPluginManager manager) {
		super(name, manager);
		setExclusive(true);
	}
}

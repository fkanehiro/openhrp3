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
	public static final String DEFAULT_DIR = "/../script";
	public static final String FILE_EXTENSION = "py";
	
	public GrxPythonScriptItem(String name, GrxPluginManager manager) {
		super(name, manager);
		setExclusive(true);
	}
}

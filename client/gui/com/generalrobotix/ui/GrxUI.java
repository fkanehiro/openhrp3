/*
 *  GrxUI.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.awt.Font;

public class GrxUI {
	public static void main(String args[]) {
		boolean isDebug = System.getProperty("DEBUG", "OFF").toUpperCase().equals("ON");
		com.generalrobotix.ui.util.GrxDebugUtil.setDebugFlag(isDebug);
		java.util.Locale locale = new java.util.Locale("en", "US");
		//java.util.Locale locale = new java.util.Locale("ja", "JP");
		java.util.Locale.setDefault(locale);
		com.generalrobotix.ui.util.GrxGuiUtil.setWholeFont(new Font("Sans-serif", Font.TRUETYPE_FONT, 12));
		
		GrxPluginManager manager = new GrxPluginManager();
		GrxUIFrame frame = new GrxUIFrame(manager);
		manager.setFrame(frame);
		manager.start();
	}
}

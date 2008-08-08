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
 *  GrxDebugUtil.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  2004/04/19
 */
package com.generalrobotix.ui.util;

import java.util.logging.*;

public class GrxDebugUtil {
	private static boolean isDebugging_ = false;
	
	public static void setDebugFlag(boolean flag){
		isDebugging_ = flag;		
		Logger logger = Logger.getLogger("");
		if (flag) {
			System.out.println("debug on (message logging level : INFO)");
			logger.setLevel(Level.INFO);
		} else {
			System.out.println("message logging level : SEVERE");
			logger.setLevel(Level.SEVERE);
		}
	}
	
	public static void print(String s){
		if (isDebugging_ == true)
			System.out.print(s);
	}
	
	public static void println(String s){
		if (isDebugging_ == true)
			System.out.println(s);
	}

	public static void printErr(String s) {
		if (isDebugging_ == true)
			System.err.println(s);
	}

	public static void printErr(String s,Exception e) {
		if (isDebugging_ == true){
			System.err.println(s);
			e.printStackTrace();
		}
	}

  public static boolean isDebugging() {
    return isDebugging_;
  }
}

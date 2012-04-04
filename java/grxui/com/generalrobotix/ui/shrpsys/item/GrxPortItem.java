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
 *  GrxPortItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.item;

import java.io.IOException;

public interface GrxPortItem {
	public short open();
	public void close() throws IOException;
	public void println(String msg);
	public void write(String msg);
	public void write(byte[] b);
	public boolean ready() throws IOException;
	public String readLine() throws IOException;
	public int read(char[] ret) throws IOException;
	public void setEnabled(boolean b);
	public boolean isEnabled();
	public boolean isConnected();
}

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
 *  GrxBaseController.java 
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui;

import java.util.List;

public interface GrxBaseController {
  public void start();
  public void stop();
  public void itemSelectionChanged(List<GrxBaseItem> itemList);
  public boolean setup(List<GrxBaseItem> itemList);
  public void control(List<GrxBaseItem> itemList);
  public boolean cleanup(List<GrxBaseItem> itemList);
}

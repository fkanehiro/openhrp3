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

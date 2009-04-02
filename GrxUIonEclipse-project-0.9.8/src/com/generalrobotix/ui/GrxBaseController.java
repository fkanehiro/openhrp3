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

/**
 * @brief
 *
 */
public interface GrxBaseController {
	/**
	 * @brief
	 */
	public void start();
  
	/**
	 * @brief
	 */
	public void stop();

	/**
	 * @brief
	 * @param itemList
	 */
	public void itemSelectionChanged(List<GrxBaseItem> itemList);

	/**
	 * @brief
	 * @param itemList
	 * @return
	 */
	public boolean setup(List<GrxBaseItem> itemList);

	/**
	 * @brief
	 * @param itemList
	 */
	public void control(List<GrxBaseItem> itemList);

	/**
	 * @brief
	 * @param itemList
	 * @return
	 */
	public boolean cleanup(List<GrxBaseItem> itemList);
}

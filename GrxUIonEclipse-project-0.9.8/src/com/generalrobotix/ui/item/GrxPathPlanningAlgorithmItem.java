/*
 *  GrxPathPlanningAlgorithmItem.java
 *
 *  Copyright (C) 2007 s-cubed, Inc.
 *  All Rights Reserved
 *
 *  @author keisuke kobayashi (s-cubed, Inc.)
 */
 
package com.generalrobotix.ui.item;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
/**
 * @brief 
 */
public class GrxPathPlanningAlgorithmItem extends GrxBaseItem {

	public static final String TITLE = "PPAlgorithm";
	public static final String FILE_EXTENSION = "ppa";

	public GrxPathPlanningAlgorithmItem(String name, GrxPluginManager manager) {
		super(name, manager);

		setExclusive(true);

		setProperty("rebuildRoadmap", "true");
		setProperty("carpetZ", "0.01");
		setProperty("tolerance", "0.0");
	}

	public boolean create() {
		return true;
	}
	
	public boolean propertyChanged(String key, String value){
		if (super.propertyChanged(key, value)){
			
		}else{
			setProperty(key, value);
			return true;
		}
		return false;
	}
}

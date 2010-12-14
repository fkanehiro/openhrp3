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
 *  GrxHwclItem.java
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  @author Shin'ichiro Nakaoka (AIST)
 */

package com.generalrobotix.ui.item;

import jp.go.aist.hrp.simulator.HwcInfo;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxHwcItem extends GrxShapeTransformItem{
	//HwcInfo info_;
	
	/**
     * @constructor
     * @param info Hwc information retrieved through ModelLoader
     */
	protected GrxHwcItem(String name, GrxPluginManager manager, GrxModelItem model, HwcInfo info) {
		super(name, manager, model);
		//info_ = info;
		
    	setProperty("id", String.valueOf(info.id));
    	translation(info.translation);
    	rotation(info.rotation);
    	setURL(info.url);

    	setIcon("camera.png");
    	
    	int n = info.shapeIndices.length;
        for(int i=0; i<n; i++)
        	addTransformedShapeIndex(info.shapeIndices[i]);
        buildShapeTransforms(info.inlinedShapeTransformMatrices);

    }
}
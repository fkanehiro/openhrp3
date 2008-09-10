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
 *  GrxModelItem.java
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  @author Shin'ichiro Nakaoka (AIST)
 */

package com.generalrobotix.ui.item;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

/**
 * @brief sensor
 */
@SuppressWarnings("serial")
public class GrxShapeItem extends GrxBaseItem{

	private String name_;
	/*
    final public double[] translation;
    final public double[] rotation;
    */
    final public GrxLinkItem parent_;

    /**
     * @brief get name
     * @return name of sensor
     */
    public String name(){
    	return name_;
    }
    
    /**
     * @brief constructor
     * @param info SensorInfo retrieved through ModelLoader
     * @param parentLink link to which this sensor is attached
     */
    public GrxShapeItem(String name, GrxPluginManager manager, GrxLinkItem parentLink) {
    	super(name, manager);

    	/*
        translation = info.translation;
        rotation = info.rotation;
		*/
        parent_ = parentLink;
    }


}

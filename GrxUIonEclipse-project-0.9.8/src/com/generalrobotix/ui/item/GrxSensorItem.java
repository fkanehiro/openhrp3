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

import jp.go.aist.hrp.simulator.SensorInfo;
import jp.go.aist.hrp.simulator.CameraPackage.CameraParameter;
import jp.go.aist.hrp.simulator.CameraPackage.CameraType;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.tdview.Manipulatable;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

/**
 * @brief sensor
 */
public class GrxSensorItem extends GrxBaseItem implements  Comparable {
	
	SensorInfo info_;
    final public double[] translation;
    final public double[] rotation;
    final public float[] maxValue;
    final public GrxLinkItem parent_;

    /**
     * @brief get name
     * @return name of sensor
     */
    public String name(){
    	return info_.name;
    }
    
    /**
     * @brief get type of sensor
     * @return type of sensor
     */
    public String type(){
    	return info_.type;
    }
    
    /**
     * @brief get id of sensor
     * @return id of sensor
     */
    public int id(){
    	return info_.id;
    }
    /**
     * @brief constructor
     * @param info SensorInfo retrieved through ModelLoader
     * @param parentLink link to which this sensor is attached
     */
    public GrxSensorItem(String name, GrxPluginManager manager, SensorInfo info, GrxLinkItem parentLink) {
    	super(name, manager);
    	info_ = info;

        translation = info.translation;
        rotation = info.rotation;
        maxValue = info.specValues;
        parent_ = parentLink;

        if (info.type.equals("Vision")) {
            CameraParameter prm = new CameraParameter();
            prm.defName = new String(info.name);
            prm.sensorName = new String(info.name);
            prm.sensorId = info.id;
            
            prm.frontClipDistance = (float)info.specValues[0];
            prm.backClipDistance = (float)info.specValues[1];
            prm.fieldOfView = (float)info.specValues[2];
            try {
                prm.type = CameraType.from_int((int)info.specValues[3]);
            } catch (Exception e) {
                prm.type = CameraType.NONE;
            }
            prm.width  = (int)info.specValues[4];
            prm.height = (int)info.specValues[5];
            boolean offScreen = false;
            Camera_impl camera = new Camera_impl(prm, offScreen);
            parentLink.cameras_.add(camera);
        }
        setIcon("camera.png");
    
    }

    public int compareTo(Object o) {
        if (o instanceof GrxSensorItem) {
            GrxSensorItem s = (GrxSensorItem) o;
            if (getOrder(type()) < getOrder(s.type())) 
                return -1;
            else{
                if (id() < s.id())
                    return -1;
            }
        }
        return 1;
    }

    /**
     * @brief get sensor type as integer value
     * @param type sensor type
     * @return 
     */
    private int getOrder(String type) {
        if (type.equals("Force")) 
            return 0;
        else if (type.equals("RateGyro")) 
            return 1;
        else if (type.equals("Acceleration")) 
            return 2;
        else if (type.equals("Vision")) 
            return 3;
        else
            return -1;

    }

}

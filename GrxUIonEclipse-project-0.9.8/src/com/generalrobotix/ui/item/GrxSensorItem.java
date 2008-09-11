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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import jp.go.aist.hrp.simulator.SensorInfo;
import jp.go.aist.hrp.simulator.CameraPackage.CameraParameter;
import jp.go.aist.hrp.simulator.CameraPackage.CameraType;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

/**
 * @brief sensor
 */
@SuppressWarnings("serial")
public class GrxSensorItem extends GrxBaseItem implements  Comparable {
	
	SensorInfo info_;
    public GrxLinkItem parent_;
	public Camera_impl camera_;

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
     * @brief delete this sensor
     */
    public void delete() {
    	System.out.println("GrxSensorItem.delete() is called.");
    	super.delete();
    	parent_.removeSensor(this);
    }
    
    /**
     * @brief constructor
     * @param info SensorInfo retrieved through ModelLoader
     * @param parentLink link to which this sensor is attached
     */
    public GrxSensorItem(String name, GrxPluginManager manager, SensorInfo info) {
    	super(name, manager);
    	
		getMenu().clear();
		
		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return "rename";
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, null,
						"Input new name.", getName(),null);
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return "delete";
			}
			public void run(){
				if( MessageDialog.openQuestion( null, "delete item",
						"Are you sure to delete " + getName() + " ?") )
					delete();
			}
		};
		setMenuItem(item);
    	
    	info_ = info;
    	
    	setProperty("type", info_.type);
    	setProperty("id", String.valueOf(info_.id));
    	setDblAry("translation", info_.translation);
    	setDblAry("rotation", info_.rotation);

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
            camera_ = new Camera_impl(prm, offScreen);
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

    /** 
     * @brief get translation relative to the parent link
     * @return translation
     */
	public double[] translation() {
		return info_.translation;
	}

    /** 
     * @brief get rotation relative to the parent link
     * @return rotation
     */
	public double[] rotation() {
		return info_.rotation;
	}
	
    /**
     * @brief properties are set to robot
     */
    public void propertyChanged() {
    	super.propertyChanged();
    	info_.translation = getDblAry("translation",null);
    	info_.rotation = getDblAry("rotation", null);
    }
}

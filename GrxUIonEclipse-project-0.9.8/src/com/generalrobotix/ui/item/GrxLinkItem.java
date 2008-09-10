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

import java.util.Vector;

import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;

import jp.go.aist.hrp.simulator.LinkInfo;
import jp.go.aist.hrp.simulator.SensorInfo;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

@SuppressWarnings("serial")
public class GrxLinkItem extends GrxBaseItem{

	private LinkInfo info_;

    final public double[]	translation_;
    final public double[]	rotation_;
    final public double[]	ulimit_;
    final public double[]	llimit_;
    final public double[]	uvlimit_;
    final public double[]	lvlimit_;
    
    public Vector<GrxLinkItem> children_;
    
    final public Vector<GrxSensorItem> sensors_;
    public Vector<Camera_impl> cameras_;
    
    public Vector<Short>	shapeIndices;
    public Vector<GrxShapeItem> shapes_;
    
    public double  jointValue_;
    public TransformGroup tg_;

    /**
     * @brief get inertia matrix
     * @return inertia matrix
     */
    public double [] inertia(){
    	return info_.inertia;
    }
    
    /**
     * @brief get relative position of center of mass
     * @return position of center of mass relative to coordinates of this link
     */
    public double [] centerOfMass(){
    	return info_.centerOfMass;
    }
    /**
     * @brief get axis of joint
     * @return axis of joint
     */
    public double[] jointAxis(){
    	return info_.jointAxis;
    }
    
    /**
     * @brief get joint id
     * @return joint id
     */
    public int jointId(){
    	return info_.jointId;
    }
    
    /**
     * @brief get index of parent link in lInfo_
     * @return index of parent link
     */
    public short parentIndex() {
    	return info_.parentIndex;
    }
    
    /**
     * @brief get indices of child links in lInfo_
     * @return indices of child links
     */
    public short[] childIndices() {
    	return info_.childIndices;
    }

    /**
     * @brief get mass of link
     * @return mass of link
     */
    public double mass() {
    	return info_.mass;
    }

    /**
     * @brief get type of joint
     * @return type of joint
     */
    public String jointType() {
    	return info_.jointType;
    }
    
    /**
     * @constructor
     * @param info link information retrieved through ModelLoader
     */
	protected GrxLinkItem(String name, GrxPluginManager manager, LinkInfo info) {
		super(name, manager);

    	info_ = info;

        translation_    = info.translation;
        
        rotation_ = new double[9];
        Matrix3d R = new Matrix3d();
        R.set(new AxisAngle4d(info.rotation));
        for(int row=0; row < 3; ++row){
            for(int col=0; col < 3; ++col){
                rotation_[row * 3 + col] = R.getElement(row, col);
            }
        }
        
        if (info.ulimit == null || info.ulimit.length == 0) {
            ulimit_ = new double[]{0.0};
        } else {
            ulimit_  = info.ulimit;
        }
        
        if (info.llimit == null || info.llimit.length == 0){
            llimit_ = new double[]{0.0};
        } else {
            llimit_ = info.llimit;
        }

        uvlimit_ = info.uvlimit;
        lvlimit_ = info.lvlimit;
        
        children_ = new Vector<GrxLinkItem>();

        cameras_ = new Vector<Camera_impl>();
        sensors_ = new Vector<GrxSensorItem>();
        SensorInfo[] sinfo = info.sensors;
        for (int i=0; i<sinfo.length; i++) {
        	GrxSensorItem sensor = new GrxSensorItem(sinfo[i].name, manager, sinfo[i], this);
            sensors_.add(sensor);
            // TODO : sensorMap_ must be updated later
            /*
            List<GrxSensorItem> l = sensorMap_.get(sensor.type());
            if (l == null) {
                l = new ArrayList<SensorInfoLocal>();
                sensorMap_.put(sensor.type(), l);
            }
            l.add(sensors.get(i));
            */
	
        }
        
        shapes_ = new Vector<GrxShapeItem>();
        
        jointValue_ = 0.0;
        
        setIcon("joint.png");
    }
}

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

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import jp.go.aist.hrp.simulator.LinkInfo;
import jp.go.aist.hrp.simulator.SensorInfo;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

@SuppressWarnings("serial")
public class GrxLinkItem extends GrxBaseItem{

	private LinkInfo info_;

    final public double[]	ulimit_;
    final public double[]	llimit_;
    final public double[]	uvlimit_;
    final public double[]	lvlimit_;
    
    public GrxLinkItem parent_;
    public Vector<GrxLinkItem> children_;
    final public Vector<GrxSensorItem> sensors_;
    public Vector<Camera_impl> cameras_;
    public Vector<GrxShapeItem> shapes_;
    
    private double  jointValue_;
    public TransformGroup tg_;
    public BranchGroup bg_;

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
     * @brief delete this item and children
     */
    public void delete() {
    	// delete children first
    	while (children_.size() > 0){
    		children_.get(0).delete();
    	}
    	while (sensors_.size() > 0){
    		sensors_.get(0).delete();
    	}
    	while (shapes_.size() > 0){
    		shapes_.get(0).delete();
    	}
    	// delete this link
    	
    	// TODO : implement delete this link
    	super.delete();
    	if (parent_ != null){
    		parent_.removeLink(this);
    	}
    }
    
    /**
     * @brief create and add a new link as a child
     * @param name name of the new link
     */
    public void addLink(String name){
    	System.out.println("GrxLinkItem.addLink("+name+") is called");
    	// TODO : implement
    }
    
    /**
     * @brief add a link as a child
     * @param link link
     */
    public void addLink(GrxLinkItem link){
    	children_.add(link);
    	link.parent_ = this;
    }
    
    /**
     * @brief remove a link from children
     * @param link link
     */
    public void removeLink(GrxLinkItem link){
    	children_.remove(link);
    	bg_.detach();
    }
    
    /**
     * @brief create and add a new sensor as a child
     * @param name name of the new sensor
     */
    public void addSensor(String name){
    	System.out.println("GrxLinkItem.addSensor("+name+") is called");
    	// TODO : implement
    }
    
    /**
     * @brief add a sensor as a child
     * @param sensor sensor
     */
    public void addSensor(GrxSensorItem sensor){
    	sensors_.add(sensor);
    	if (sensor.camera_ != null){
    		cameras_.add(sensor.camera_);
    	}
    }
    
    public void removeSensor(GrxSensorItem sensor){
    	sensors_.remove(sensor);
    	if (sensor.camera_ != null){
    		cameras_.remove(sensor.camera_);
    		// TODO : GrxModelItem.cameraList_ must be updated
    	}
    }
    /**
     * @brief create and add a new shape as a child
     * @param url URL of the file where shape is described
     */
    public void addShape(String url){
    	System.out.println("GrxLinkItem.addShape("+url+") is called");
    	// TODO : implement
    }

    /**
     * @brief add a shape as a child
     * @param shape shape
     */
    public void addShape(GrxShapeItem shape){
    	shapes_.add(shape);
    	shape.parent_ = this;
    	tg_.addChild(shape.bg_);
    }
    
    /**
     * @brief remove a shape from children
     * @param shape shape
     */
    public void removeShape(GrxShapeItem shape){
    	shapes_.remove(shape);
    }
    
    /**
     * @brief set new joint value
     * @param jv joint value
     */
    public void jointValue(double jv){
    	jointValue_ = jv;
    	setDbl("angle", jointValue_);
    }
    
    /**
     * @brief get current joint value
     * @return joint value
     */
    public double jointValue(){
    	return jointValue_;
    }
    
    /**
     * @brief properties are set to robot
     */
    public void propertyChanged() {
    	super.propertyChanged();
    	jointValue_ = getDbl("angle", 0.0);
    	info_.translation = getDblAry("translation",null);
    	info_.rotation = getDblAry("rotation", null);
    	calcForwardKinematics();
    }
    
    /**
     * @brief get translation relative to the parent joint
     * @return translation
     */
    public double[] translation(){
    	return info_.translation;
    }
    
    /**
     * @brief get axis and angle relative to the parent link
     * @return axis and angle
     */
    public double[] rotation(){
    	return info_.rotation;
    }

    /**
     * @brief compute forward kinematics
     */
    public void calcForwardKinematics(){
    	Transform3D t3dp = new Transform3D();
    	Transform3D t3d = new Transform3D();
    	Vector3d v3d = new Vector3d();
    	Vector3d v3d2 = new Vector3d();
    	Matrix3d m3d = new Matrix3d();
    	Matrix3d m3d2 = new Matrix3d();
    	AxisAngle4d a4d = new AxisAngle4d();
    	if (parent_ != null){
    		parent_.tg_.getTransform(t3dp);
            v3d.set(translation());
            if (jointType().equals("rotate") || jointType().equals("fixed")) {
                t3d.setTranslation(v3d);
                m3d.set(new AxisAngle4d(rotation()));
                a4d.set(jointAxis()[0], jointAxis()[1], jointAxis()[2], jointValue());
                m3d2.set(a4d);
                m3d.mul(m3d2);
                t3d.setRotation(m3d);
            } else if(jointType().equals("slide")) {
                v3d2.set(jointAxis()[0], jointAxis()[1], jointAxis()[2]);
                v3d2.scale(jointValue());
                v3d.add(v3d2);
                t3d.setTranslation(v3d);
                m3d.set(new AxisAngle4d(rotation()));
                m3d.mul(m3d);
                t3d.setRotation(m3d);
            }
            t3dp.mul(t3d);
            tg_.setTransform(t3dp);
        }
        for (int i=0; i<children_.size(); i++){
        	children_.get(i).calcForwardKinematics();
        }

    }
    
    /**
     * @constructor
     * @param info link information retrieved through ModelLoader
     */
	protected GrxLinkItem(String name, GrxPluginManager manager, LinkInfo info) {
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
						"Are you sure to delete " + getName() + " and its children ?") )
					delete();
			}
		};
		setMenuItem(item);

		// menu item : add joint
		item = new Action(){
				public String getText(){
					return "add joint";
				}
				public void run(){
					InputDialog dialog = new InputDialog( null, null,
							"Input name of new joint", null,null);
					if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
						addLink( dialog.getValue() );
				}
		};
		setMenuItem(item);
	
		// menu item : add sensor
		item = new Action(){
				public String getText(){
					return "add sensor";
				}
				public void run(){
					InputDialog dialog = new InputDialog( null, null,
							"Input name of new sensor", null,null);
					if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
						addSensor( dialog.getValue() );
				}
		};
		setMenuItem(item);
	
		// menu item : add shape
		item = new Action(){
				public String getText(){
					return "add shape";
				}
				public void run(){
					InputDialog dialog = new InputDialog( null, null,
							"Input name of new shape", null,null);
					if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
						addShape( dialog.getValue() );
				}
		};
		setMenuItem(item);

    	info_ = info;

    	setDblAry("translation", info_.translation);
    	setDblAry("rotation", info_.rotation);
    	setDblAry("centerOfMass", info_.centerOfMass);
    	setDblAry("inertia", info_.inertia);
    	setDblAry("jointAxis", info_.jointAxis);
        setProperty("jointType", info_.jointType);
        setDbl("mass", info_.mass);
        setDblAry("ulimit", info_.ulimit);
        setDblAry("llimit", info_.llimit);
        setDblAry("uvlimit", info_.uvlimit);
        setDblAry("lvlimit", info_.lvlimit);
        setProperty("jointId", String.valueOf(info_.jointId));
        
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
        	GrxSensorItem sensor = new GrxSensorItem(sinfo[i].name, manager, sinfo[i]);
        	addSensor(sensor);
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
        
        jointValue(0);
        
        tg_ = new TransformGroup();
        bg_ = new BranchGroup();
        bg_.setCapability(BranchGroup.ALLOW_DETACH);
        bg_.addChild(tg_);
        
        setIcon("joint.png");
    }
}

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

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.media.j3d.Appearance;import javax.media.j3d.BadTransformException;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.HwcInfo;
import jp.go.aist.hrp.simulator.LinkInfo;
import jp.go.aist.hrp.simulator.SensorInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.SegmentInfo;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.util.AxisAngle4d;
import com.generalrobotix.ui.util.CalcInertiaUtil;
import com.generalrobotix.ui.util.GrxShapeUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.OrderedHashMap;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.picking.PickTool;


@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxLinkItem extends GrxTransformItem{
	private double[] translation_ = {0,0,0};		// parent link local 
	private double[] rotation_ = {0,1,0,0};
    public short	jointId_;
    public String jointType_;
    public double jointValue_;
    public double[] jointAxis_;
    public double[] ulimit_;
    public double[] llimit_;
    private double[] uvlimit_;
    private double[] lvlimit_;
    public double	  linkMass_; 
    private double[] linkCenterOfMass_;
    private double[] linkInertia_;
    public double 	rotorInertia_; 
    public double		rotorResistor_;
    public double		gearRatio_;
    public double		torqueConst_;  
    public double		encoderPulse_;  

    public short	  	parentIndex_;  
    public short[]	childIndices_; ///< 子リンクインデックス列  
    private short AABBmaxNum_;

    // display
    private Switch switchCom_;
    private TransformGroup tgCom_;
    private Switch switchAxis_;
    private Switch switchAABB_;

    public Transform3D absTransform(){
    	Transform3D t3d = new Transform3D();
    	tg_.getTransform(t3d);
    	return t3d;
    }
    
    public double[] localTranslation(){
    	return translation_;
    }
    
    public double[] localRotation(){
    	return rotation_;
    }

    public void absTransform(double[] p, double[] R){
    	Transform3D t3d = new Transform3D();
        tg_.getTransform(t3d);        
        t3d.setTranslation(new Vector3d(p));
        AxisAngle4d a4d = new AxisAngle4d();
		a4d.setMatrix(new Matrix3d(R));
		double[] newrot = new double[4];
		a4d.get(newrot);
        t3d.setRotation(new AxisAngle4d(newrot));
        tg_.setTransform(t3d);
    }
    
    public boolean localTranslation(double[] pos){
    	if (pos == null || pos.length != 3) return false;
    	translation_ = pos;
        setDblAry("translation", pos, 4);
        if (model_ != null && parent_ != null) model_.notifyModified();
        return true;
    }
    
    public boolean localTranslation(String value){
    	double [] pos = getDblAry(value);
    	if (localTranslation(pos)){
            return true;
    	}else{
    		return false;
    	}
    }
    
    public boolean localRotation(double[] rot){
    	if (rot == null)	return false;
    	if(rot.length == 9){
    		AxisAngle4d a4d = new AxisAngle4d();
    		a4d.setMatrix(new Matrix3d(rot));
    		rot = new double[4];
    		a4d.get(rot);
    	}
    	if(rot.length == 4){ 
    		rotation_ = rot;
    		setDblAry("rotation", rot, 4);
    		if (model_ != null && parent_ != null) model_.notifyModified();
    		return true;
    	}
    	return false;
    }
    
    public boolean localRotation(String value){
    	double [] rot = getDblAry(value);
    	if (localRotation(rot)){
            return true;
    	}else{
    		return false;
    	}
    }
 
    /**
     * @brief set new joint value
     * @param jv joint value
     */
    public void jointValue(double jv){
    	jointValue_ = jv;
    	setDbl("angle", jointValue_); //$NON-NLS-1$
    }
    
    /**
     * @brief set joint value from string
     * @param value joint value
     * @return true if set successfully, false otherwise
     */
    public boolean jointValue(String value){
		Double a = getDbl(value);
		if (a != null){
			jointValue(a);
			return true;
		}else{
			return false;
		}
    }
    
    /**
     * @brief set inertia matrix
     * @param newI inertia matrix(length=9)
     * @return true if set successfully, false otherwise
     */
    public boolean inertia(double [] newI){
    	if (newI != null && newI.length == 9){
    		linkInertia_ = newI;
    		setDblAry("momentsOfInertia", linkInertia_); //$NON-NLS-1$
    		_updateScaleOfBall();
    		if (model_ != null) model_.notifyModified();
    		return true;
       	}
    	return false;
    }
    
    public void inertia(String i){
    	double [] mi = getDblAry(i);
    	if(mi != null)
    		inertia(mi);
    }
    
    /**
     * @brief set CoM position
     * @param com CoM position(length=3)
     * @return true if set successfully, false otherwise
     */
    public boolean centerOfMass(double [] com){
		if (com != null && com.length==3){
        	linkCenterOfMass_ = com;
        	setDblAry("centerOfMass", com); //$NON-NLS-1$
    		if (model_ != null) model_.notifyModified();
        	Transform3D t3d = new Transform3D();
        	tgCom_.getTransform(t3d);
        	t3d.setTranslation(new Vector3d(linkCenterOfMass_));
        	tgCom_.setTransform(t3d);
        	return true;
		}else{
			return false;
		}
    }
    
    /**
     * @brief set CoM position from string
     * @param value space separated array of double(length=3)
     */
    public void centerOfMass(String value){
		double [] com = getDblAry(value);
		if(com != null)
			centerOfMass(com);
    }
    
    /**
     * @brief set joint axis
     * @param axis axis of this joint. it must be one of "X", "Y" and "Z"
     */
    public void jointAxis(double[] newAxis){
    	if(jointType_.equals("fixed")||jointType_.equals("free"))
    		return;
    	if (newAxis != null && newAxis.length == 3){
    		jointAxis_ = newAxis;
    		setDblAry("jointAxis", newAxis); //$NON-NLS-1$
    		updateAxis();
    		if (model_ != null) model_.notifyModified();
    	}  	
    }

    void jointAxis(String axis){
    	double[] newAxis = getDblAry(axis);
    	if(newAxis != null)
    		jointAxis(newAxis);
    }
 
    /**
     * @brief set joint id
     * @return joint id
     */
    public void jointId(short id){
    	jointId_ = id;
		setShort("jointId", id); //$NON-NLS-1$
		if (model_ != null) model_.notifyModified();
    }
    
    /**
     * set joint id from string
     * @param value string
     */
    public void jointId(String value){
    	Short id = getShort(value);
    	if (id != null && id != jointId_){
    		jointId(id);
    	}
    }

    /**
     * @brief set mass from string
     * @param value mass
     */
    public void mass(double m){
    	linkMass_ = m;
        setDbl("mass", linkMass_); //$NON-NLS-1$
        _updateScaleOfBall();
		if (model_ != null) model_.notifyModified();
    }
    
    public boolean mass(String value){
    	Double a = getDbl(value);
		if (a != null){
			mass(a);
			return true;
		}else{
			return false;
		}
    }
    
    /**
     * @brief set joint type
     * @param type type of this joint. It must be one of "fixed", "free", "rotate" and "slide"
     */
    public void jointType(String type){
		if (type.equals("fixed")||type.equals("rotate")||type.equals("free")||type.equals("slide"))
	    	jointType_ = type;
		else
			jointType_ = "free";
		setProperty("jointType", type); //$NON-NLS-1$
		if(type.equals("fixed")||type.equals("free"))
			setProperty("jointAxis", "---");
		else
			if(getProperty("jointAxis")== null || getProperty("jointAxis").equals("---"))
				jointAxis("0.0 0.0 1.0");
		if (model_ != null) model_.notifyModified();		
    }

    public void gearRatio(double r){
    	gearRatio_ = r;
    	setDbl("gearRatio", r);
		if (model_ != null) model_.notifyModified();
    }
    
    public void gearRatio(String g){
    	Double gr = getDbl(g);
    	if(gr != null)
    		gearRatio(gr);
    }
    
    public void encoderPulse(double e){
    	encoderPulse_ = e;
    	setDbl("encoderPulse", e);
    	if (model_ != null) model_.notifyModified();
    }

    public void encoderPulse(String e){
    	Double ep = getDbl(e);
    	if(ep != null)
    		encoderPulse(ep);
    }
    
    public void rotorInertia(double r){
    	rotorInertia_ = r;
    	setDbl("rotorInertia", r);
    	if (model_ != null) model_.notifyModified();
    }

    public void rotorResistor(double r){
    	rotorResistor_ = r;
    	setDbl("rotorResistor", r);
    	if (model_ != null) model_.notifyModified();
    }

    public void rotorResistor(String r){
    	Double rr = getDbl(r);
    	if(rr != null)
    		rotorResistor(rr);
    }
    
    public void torqueConst(double t){
    	torqueConst_ = t;
    	setDbl("torqueConst", t);
    	if (model_ != null) model_.notifyModified();
    }

    public void torqueConst(String t){
    	Double tc = getDbl(t);
    	if(tc != null)
    		torqueConst(tc);
    }
    
    public void ulimit(double[] u){
    	ulimit_ = u;
    	setDblAry("ulimit", u);
    	if (model_ != null) model_.notifyModified();
    }

    public void ulimit(String u){
    	double[] ulimit = getDblAry(u);
    	if(ulimit != null)
    		ulimit(ulimit);
    }
    
    public void llimit(double[] l){
    	llimit_ = l;
    	setDblAry("llimit", l);
    	if (model_ != null) model_.notifyModified();
    }

    public void llimit(String l){
    	double[] llimit = getDblAry(l);
    	if(llimit != null)
    		llimit(llimit);
    }
    
    public void uvlimit(double[] uv){
    	uvlimit_ = uv;
    	setDblAry("uvlimit", uv);
    	if (model_ != null) model_.notifyModified();
    }
    
    public void uvlimit(String uv){
    	double[] uvlimit = getDblAry(uv);
    	if(uvlimit != null)
    		uvlimit(uvlimit);
    }
    
    public void lvlimit(double[] lv){
    	lvlimit_ = lv;
    	setDblAry("lvlimit", lv);
    	if (model_ != null) model_.notifyModified();
    }
    
    public void lvlimit(String lv){
    	double[] lvlimit = getDblAry(lv);
    	if(lvlimit != null)
    		lvlimit(lvlimit);
    }
        
    /**
     * @brief create and add a new link as a child
     * @param name name of the new link
     */
    public void addLink(String name){
    	System.out.println("GrxLinkItem.addLink("+name+") is called"); //$NON-NLS-1$ //$NON-NLS-2$
    	try{
    		GrxLinkItem newLink = new GrxLinkItem(name, manager_, model_);
    		addLink(newLink);
        	System.out.println("GrxLinkItem.addLink("+name+") is done"); //$NON-NLS-1$ //$NON-NLS-2$
        	manager_.itemChange(newLink, GrxPluginManager.ADD_ITEM);
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    	//manager_.reselectItems();
    }

    /**
     * @brief add a child link
     * @param child child link
     */
    public void addLink(GrxLinkItem child){
    	children_.add(child);
    	child.parent_ = this;
    	model_.bgRoot_.addChild(child.bg_);
    	child.calcForwardKinematics();
    }

    /**
     * @brief create and add a new sensor as a child
     * @param name name of the new sensor
     */
    public void addSensor(String name){
    	try{
	    	GrxSensorItem sensor = new GrxSensorItem(name, manager_, model_, null);
	    	addSensor(sensor);
	    	manager_.itemChange(sensor, GrxPluginManager.ADD_ITEM);
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }

    /**
     * @brief create and add a new sensor as a child
     * @param name name of the new sensor
     */
    public void addSegment(String name){
    	try{
	    	GrxSegmentItem segment = new GrxSegmentItem(name, manager_, model_, null, null);
	    	addChild(segment);
	    	 manager_.itemChange(segment, GrxPluginManager.ADD_ITEM);
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }

    /**
     * @brief add a sensor as a child
     * @param sensor sensor
     */
    public void addSensor(GrxSensorItem sensor){
    	addChild(sensor);
        // TODO : GrxModelItem.sensorMap_ and GrxModelItem.cameraList_ must be updated
    }

    public void removeSensor(GrxSensorItem sensor){
    	removeChild(sensor);
    	if (sensor.isCamera()){
    		// TODO : GrxModelItem.sensorMap_ and GrxModelItem.cameraList_ must be updated
    	}
    }
    
    /**
     * @brief load and add a new robot as a child
     * @param f file name of the new robot
     */
    public void addRobot(File f){
    	model_.addRobot(f, this);
    }
    
    /**
     * @brief compute CoM in global frame
     * @return computed CoM
     */
    public Vector3d absCoM(){
        Vector3d absCom = new Vector3d(linkCenterOfMass_);
        return transformV3(absCom);
    }
       
    private void _updateScaleOfBall(){
		Matrix3d I = new Matrix3d(linkInertia_);
		double m = linkMass_;
		Vector3d scale = CalcInertiaUtil.calcScale(I, m);
		Transform3D t3d = new Transform3D();
		tgCom_.getTransform(t3d);
		t3d.setScale(scale);
		try{
			tgCom_.setTransform(t3d);
		}catch(BadTransformException ex){
			System.out.println("BadTransformException in _updateScaleOfBall"); //$NON-NLS-1$
		}
    }
    
    /**
     * @brief check validity of new value of property and update if valid
     * @param property name of property
     * @param value value of property
     * @return true if checked(even if value is not used), false otherwise
     */
    public boolean propertyChanged(String property, String value) {
    	if (property.equals("name")){ //$NON-NLS-1$
			rename(value);
    	}else if (property.equals("angle")){ //$NON-NLS-1$
    		if (jointValue(value)){
    			model_.updateInitialJointValue(this);
        		calcForwardKinematics();
    		}
    	}else if(property.equals("translation")){ //$NON-NLS-1$
    		if (localTranslation(value)){
    			model_.updateInitialTransformRoot();
            	calcForwardKinematics();
    		}
    	}else if(property.equals("rotation")){ //$NON-NLS-1$
    		if (localRotation(value)){
    			model_.updateInitialTransformRoot();
            	calcForwardKinematics();
    		}
    	}else if(property.equals("jointAxis")){ //$NON-NLS-1$
    		jointAxis(value);
    	}else if(property.equals("jointType")){ //$NON-NLS-1$
    		jointType(value);
    	}else if(property.equals("jointId")){ //$NON-NLS-1$
    		jointId(value);
    	}else if(property.equals("ulimit")){ //$NON-NLS-1$
    		ulimit(value);
    	}else if(property.equals("llimit")){ //$NON-NLS-1$
    		llimit(value);
    	}else if(property.equals("uvlimit")){ //$NON-NLS-1$
    		uvlimit(value);
    	}else if(property.equals("lvlimit")){ //$NON-NLS-1$
    		lvlimit(value);
    	}else if(property.equals("torqueConst")){ //$NON-NLS-1$
    		torqueConst(value);
    	}else if(property.equals("rotorResistor")){ //$NON-NLS-1$
    		rotorResistor(value);
    	}else if(property.equals("encoderPulse")){ //$NON-NLS-1$
    		encoderPulse(value);
    	}else if(property.equals("gearRatio")){ //$NON-NLS-1$
    		gearRatio(value);
    	}else if(property.equals("centerOfMass")){ //$NON-NLS-1$
    		centerOfMass(value);
    	}else if(property.equals("mass")){ //$NON-NLS-1$
    		mass(value);
    	}else if(property.equals("momentsOfInertia")){ //$NON-NLS-1$
    		inertia(value);
    	}else if (property.equals("tolerance")){ //$NON-NLS-1$
    		Double tr = getDbl(value);
    		if (tr != null){
    			setProperty("tolerance", value); //$NON-NLS-1$
    		}
    	}else if (property.equals("NumOfAABB")){
    		try{
	    		int depth = Integer.parseInt(value);
	    		if(depth<AABBmaxNum_){
	    			setProperty("NumOfAABB", value);
	    			model_.setProperty(getName()+".NumOfAABB", value);
	    			model_.makeAABBforSameUrlModels();
	    		}else if(AABBmaxNum_==0){
	    			setProperty("NumOfAABB", "no shape Data");
	    		}
    		}catch(NumberFormatException e){
    			System.out.println("input number");
    			//TODO
    			return false;
    		}
    	}else if( property.equals("mode")){
    		setProperty("mode", value);
    		model_.setProperty(getName()+".mode", value);
    	}else if( property.equals("jointVelocity")){
    		setProperty("jointVelocity", value);
    		model_.setProperty(getName()+".jointVelocity", value);
    	}else {
    		return false;
    	}
    	return true;
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
    		t3dp = ((GrxLinkItem)parent_).absTransform();
            v3d.set(localTranslation());
            if (jointType_.equals("rotate")) { //$NON-NLS-1$ //$NON-NLS-2$
                t3d.setTranslation(v3d);
                m3d.set(new AxisAngle4d(localRotation()));
                a4d.set(jointAxis_[0], jointAxis_[1], jointAxis_[2], jointValue_);
                m3d2.set(a4d);
                m3d.mul(m3d2);
                t3d.setRotation(m3d);
            } else if(jointType_.equals("slide")) { //$NON-NLS-1$
                v3d2.set(jointAxis_[0], jointAxis_[1], jointAxis_[2]);
                v3d2.scale(jointValue_);
                v3d.add(v3d2);
                t3d.setTranslation(v3d);
                m3d.set(new AxisAngle4d(localRotation()));
                t3d.setRotation(m3d);
            }else if(jointType_.equals("free") || jointType_.equals("fixed") ){
            	t3d.setTranslation(v3d);
            	m3d.set(new AxisAngle4d(localRotation()));
            	t3d.setRotation(m3d);
            }
            t3dp.mul(t3d);
            tg_.setTransform(t3dp);
        }else{
        	v3d.set(localTranslation());
        	t3d.setTranslation(v3d);
        	t3d.setRotation(new AxisAngle4d(localRotation()));
        	tg_.setTransform(t3d);
        }
        for (int i=0; i<children_.size(); i++){
        	if (children_.get(i) instanceof GrxLinkItem){
        		GrxLinkItem link = (GrxLinkItem)children_.get(i);
        		link.calcForwardKinematics();
        	}
        }

    }

    protected GrxLinkItem(String name, GrxPluginManager manager, GrxModelItem model){
    	super(name, manager, model);
    	_init();
		jointValue(0);
        localTranslation(new double[]{0.0, 0.0, 0.0});
        localRotation(new double[]{0.0, 0.0, 1.0, 0.0});
        centerOfMass(new double[]{0.0, 0.0, 0.0});
        inertia(new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0});
        jointType("rotate");
        jointAxis(new double[]{0.0, 0.0, 1.0});
        mass(1.0);
        ulimit(new double[]{0,0});
        llimit(new double[]{0,0});
        uvlimit(new double[]{0,0});
        lvlimit(new double[]{0,0});
        gearRatio(1.0);
        torqueConst(1.0);
        rotorInertia(1.0);
        rotorResistor(1.0);
        encoderPulse(1.0);
        jointId((short)-1);
        parentIndex_ = 0;  
        childIndices_ = null;  
        AABBmaxNum_ = 0;
    }
	/**
     * @constructor
     * @param info link information retrieved through ModelLoader
     */
	protected GrxLinkItem(String name, GrxPluginManager manager, GrxModelItem model, LinkInfo info) {
		super(name, manager, model);
		_init();
		jointValue(0);
        localTranslation(info.translation);
        localRotation(info.rotation);
        centerOfMass(info.centerOfMass);
        inertia(info.inertia);
        jointType(info.jointType);
        jointAxis(info.jointAxis);
        mass(info.mass);
        if (info.ulimit == null || info.ulimit.length == 0)
        	ulimit(new double[]{0,0});
        else
        	ulimit(info.ulimit);
        if (info.llimit == null || info.llimit.length == 0) 
        	llimit(new double[]{0,0});
        else
        	llimit(info.llimit);
        uvlimit(info.uvlimit);
        lvlimit(info.lvlimit);
        gearRatio(info.gearRatio);
        torqueConst(info.torqueConst);
        rotorInertia(info.rotorInertia);
        rotorResistor(info.rotorResistor);
        encoderPulse(info.encoderPulse);
        jointId(info.jointId);

        SensorInfo[] sinfo = info.sensors;
        if (sinfo != null){
            for (int i=0; i<sinfo.length; i++) {
            	GrxSensorItem sensor = new GrxSensorItem(sinfo[i].name, manager_, model_, sinfo[i]);
            	manager_.itemChange(sensor, GrxPluginManager.ADD_ITEM);
            	addSensor(sensor);
            }
        }
        
        HwcInfo[] hinfo = info.hwcs;
        if (hinfo != null){
        	for (int i=0; i<hinfo.length; i++) {
        		GrxHwcItem hwc = new GrxHwcItem(hinfo[i].name, manager_, model_, hinfo[i]);
        		manager_.itemChange(hwc, GrxPluginManager.ADD_ITEM);
        		addChild(hwc);
        	}
        }
        
        SegmentInfo[] segmentInfo = info.segments;
        if (segmentInfo != null){
        	for (int i=0; i<segmentInfo.length; i++) {
        		GrxSegmentItem segment = new GrxSegmentItem(segmentInfo[i].name, manager_, model_, info, segmentInfo[i]);
        		manager_.itemChange(segment, GrxPluginManager.ADD_ITEM);
        		addChild(segment);
        	}
        }
        parentIndex_ = info.parentIndex;  
        childIndices_ = info.childIndices;  
        AABBmaxNum_ = info.AABBmaxNum;
    }

	/**
	 * @brief initialize menu
	 */
	private void _initMenu(){
		getMenu().clear();

		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.rename"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxLinkItem.dialog.message.rename"), getName(),null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.delete"); //$NON-NLS-1$
			}
			public void run(){
                String mes = MessageBundle.get("GrxLinkItem.dialog.message.delete"); //$NON-NLS-1$
                mes = NLS.bind(mes, new String[]{getName()});
                if(parent_ == null){ 	// can't delete root link 
                	MessageDialog.openInformation(null, MessageBundle.get("GrxLinkItem.dialog.title.delete0"), 
                			MessageBundle.get("GrxLinkItem.dialog.message.rootLinkDelete"));
                	return;
                }
				if( MessageDialog.openQuestion( null, MessageBundle.get("GrxLinkItem.dialog.title.delete0"), //$NON-NLS-1$
						mes) )
					delete();
			}
		};
		setMenuItem(item);

		// menu item : add joint
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.addJoint"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxLinkItem.dialog.message.jointName"), null,null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					addLink( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// menu item : add sensor
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.addSensor"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxLinkItem.dialog.message.sensorName"), null,null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					addSensor( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// menu item : add segment
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.addSegment"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxLinkItem.dialog.message.segmentName"), null,null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					addSegment( dialog.getValue() );
			}
		};
		setMenuItem(item);
		
		// menu item : add robot
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.addRobot"); //$NON-NLS-1$
			}
			public void run(){
				FileDialog fdlg = new FileDialog(GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);
                String[] fe = { "*.wrl" };
                fdlg.setFilterExtensions(fe);
                String fPath = fdlg.open();
                if (fPath != null) {
                    File f = new File(fPath);
					addRobot( f );
                }
			}
		};
		setMenuItem(item);

        /* diable copy and paste menus until they are implemented
        // menu item : copy
        item = new Action(){
            public String getText(){
                return "copy";
            }
            public void run(){
                GrxDebugUtil.println("GrxModelItem.GrxLinkItem copy Action");
                manager_.setSelectedGrxBaseItemList();
            }
        };
        setMenuItem(item);

        // menu item : paste
        item = new Action(){
            public String getText(){
                return "paste";
            }
            public void run(){
            }
        };
		setMenuItem(item);
		*/

	}
	/**
	 * @brief common initialization
	 */
	private void _init(){
		_initMenu();
		
		model_.addLink(this);

		setDbl("tolerance", 0.0); //$NON-NLS-1$
        setDbl("jointVelocity", 0.0);
		
		// CoM display
		// 0.01 is default scale of ellipsoid
        switchCom_ = GrxShapeUtil.createBall(0.01, new Color3f(1.0f, 0.5f, 0.5f), 0.5f);
        tgCom_ = (TransformGroup)switchCom_.getChild(0);
        tg_.addChild(switchCom_);

        Transform3D tr = new Transform3D();
        tr.setIdentity();
        tg_.setTransform(tr);

        SceneGraphModifier modifier = SceneGraphModifier.getInstance();
        Vector3d jointAxis = new Vector3d(0.0, 0.0, 1.0);
        switchAxis_ = SceneGraphModifier._makeSwitchNode(modifier._makeAxisLine(jointAxis));
        tg_.addChild(switchAxis_);

        setIcon("joint.png"); //$NON-NLS-1$

        Map<String, Object> userData = new Hashtable<String, Object>();
        userData.put("linkInfo", this); //$NON-NLS-1$
        userData.put("object", model_); //$NON-NLS-1$
        tg_.setUserData(userData);
        tg_.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        
        switchAABB_ = SceneGraphModifier._makeSwitchNode();
        tg_.addChild(switchAABB_);
        setProperty("NumOfAABB","original data"); //String.valueOf(AABBmaxDepth_));
        if(model_.getProperty(getName()+".NumOfAABB")!=null)
        	model_.remove(getName()+".NumOfAABB");
	}
	
	private void updateAxis(){
		try{
			Shape3D shapeNode = (Shape3D)switchAxis_.getChild(0);
			Geometry gm = (Geometry)shapeNode.getGeometry(0);
			SceneGraphModifier modifier = SceneGraphModifier.getInstance();
			Point3f[] p3fW = modifier.makeAxisPoints(new Vector3d(jointAxis_));
			if (gm instanceof LineArray){
				LineArray la = (LineArray)gm;
				la.setCoordinates(0, p3fW);
			}
		}catch(Exception ex){
        	ex.printStackTrace();
        }
	}
	
	
    /**
     * @brief Override clone method
     * @return GrxLinkItem
     */
	public GrxLinkItem clone(){
		GrxLinkItem ret = (GrxLinkItem)super.clone();
/*    	
	Deep copy suspension list
*/
		
		return ret;
	}

	/**
	 * @brief set new position and rotation in global frame
	 * @param pos new position
	 * @param rot new rotation
	 */
	public void setTransform(Vector3d pos, Matrix3d rot) {
		if (parent_ != null) return;	// root only
		
    	if (pos != null){
    		double[] newpos = new double[3];
    		pos.get(newpos);
    		localTranslation(newpos);
    	}
    	if (rot != null){
    		AxisAngle4d a4d = new AxisAngle4d();
    		a4d.setMatrix(rot);
    		double[] newrot = new double[4];
    		a4d.get(newrot);
    		localRotation(newrot);
    	}
    	if (pos != null || rot != null)	calcForwardKinematics();
	}

	/**
	 * @brief set new position and rotation in global frame
	 * @param tform new transform
	 */
	public void setTransform(Transform3D trans) {
		Vector3d pos = new Vector3d();
		Matrix3d rot = new Matrix3d();
		trans.get(rot, pos);
		setTransform(pos, rot);
	}

	/**
	 * @brief limit joint value within limits recursively
	 */
	public void setJointValuesWithinLimit() {
        if (llimit_ != null && ulimit_ != null && llimit_[0] < ulimit_[0]) {
            if (jointValue_ < llimit_[0])
                jointValue(llimit_[0]);
            else if (ulimit_[0] < jointValue_)
                jointValue(ulimit_[0]);
        }
        for (int i=0; i<children_.size(); i++){
        	if (children_.get(i) instanceof GrxLinkItem){
        		GrxLinkItem link = (GrxLinkItem)children_.get(i);
        		link.setJointValuesWithinLimit();
        	}
        }
	}
    
    /**
     * @brief set/unset fucus on this item
     * 
     * When this item is focused, some geometries are displayed
     * @param b true to fucus, false to unfocus
     */
    public void setFocused(boolean b){
    	//if (b!=isSelected()) System.out.println("GrxLinkItem.setFocused("+getName()+" of "+model_.getName()+", flag = "+b+")");
    	if (b){
    		resizeBoundingBox();
    		if (jointType_.equals("rotate") || jointType_.equals("slide")) { //$NON-NLS-1$ //$NON-NLS-2$
    			updateAxis();
    			switchAxis_.setWhichChild(Switch.CHILD_ALL);
    		}else
    			switchAxis_.setWhichChild(Switch.CHILD_NONE);
    	}else{
            switchAxis_.setWhichChild(Switch.CHILD_NONE);
    	}
    	super.setFocused(b);
    	switchCom_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
    }

	/**
	 * delete this link and children
	 */
	public void delete(){
		super.delete();
		model_.removeLink(this);
	}
	
	public void setColor(java.awt.Color color){
		for (int i=0; i<children_.size(); i++){
			if (children_.get(i) instanceof GrxShapeItem){
				GrxShapeItem shape = (GrxShapeItem)children_.get(i);
				shape.setColor(color);
			}else if (children_.get(i) instanceof GrxSensorItem){
				for (int j=0; j<children_.get(i).children_.size(); j++){
					GrxShapeItem shape = (GrxShapeItem)children_.get(i).children_.get(j);
					shape.setColor(color);
				}
			}
		}
    }
	
	public void restoreColor(){
		for (int i=0; i<children_.size(); i++){
			if (children_.get(i) instanceof GrxShapeItem){
				GrxShapeItem shape = (GrxShapeItem)children_.get(i);
				shape.restoreColor();
			}else if (children_.get(i) instanceof GrxSensorItem){
				for (int j=0; j<children_.get(i).children_.size(); j++){
					GrxShapeItem shape = (GrxShapeItem)children_.get(i).children_.get(j);
					shape.restoreColor();
				}
			}
		}
    }
	
	public void setVisibleAABB(boolean b){ 
		switchAABB_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
	}
	
	public void clearAABB(){
		switchAABB_.removeAllChildren();
	}
	
	public void makeAABB(ShapeInfo shape, double[] T){	
		TransformGroup tg = new TransformGroup();
		tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
	    tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
	    tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
	    tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		Transform3D t3d = new Transform3D();
	    tg.getTransform(t3d);
	    Vector3d v = new Vector3d(T[3], T[7], T[11]);
	    t3d.setTranslation(v);
	    tg.setTransform(t3d);
	    Appearance appearance = new Appearance();
	    PolygonAttributes pa = new PolygonAttributes();
	    pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	    appearance.setPolygonAttributes(pa);
	    
	    GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
        int numVertices = shape.vertices.length / 3;
        Point3f[] vertices = new Point3f[numVertices];
        for(int i=0; i < numVertices; ++i){
            vertices[i] = new Point3f(shape.vertices[i*3], shape.vertices[i*3+1], shape.vertices[i*3+2]);
        }
        geometryInfo.setCoordinates(vertices);
        geometryInfo.setCoordinateIndices(shape.triangles);
        NormalGenerator ng = new NormalGenerator();
        ng.generateNormals(geometryInfo);

        Shape3D shape3D = new Shape3D(geometryInfo.getGeometryArray());
        shape3D.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        shape3D.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        shape3D.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
        shape3D.setCapability(GeometryArray.ALLOW_COUNT_READ);
        PickTool.setCapabilities(shape3D, PickTool.INTERSECT_FULL);
        shape3D.setAppearance(appearance);
 	    
	    tg.addChild(shape3D);
	    BranchGroup bg = new BranchGroup();
	    bg.setCapability(BranchGroup.ALLOW_DETACH);
	    bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	    bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	    bg.addChild(tg);
	    switchAABB_.addChild(bg);		

	}

	/**
	 * @brief rename this Link
	 * @param newName new name
	 */
	public void rename(String newName) {
    	String oldName = getName();
    	setName(newName);
    	if (model_ != null) model_.notifyModified();
        OrderedHashMap mcoll = manager_.pluginMap_.get(GrxCollisionPairItem.class);
        if(mcoll != null)
        {
        	String modelName = model().getName();
        	Iterator<?> it = mcoll.values().iterator();
        	while(it.hasNext())
        	{
        		GrxCollisionPairItem ci = (GrxCollisionPairItem)it.next();
        		if(modelName.equals(ci.getProperty("objectName1")) && oldName.equals(ci.getProperty("jointName1")))
        			ci.setProperty("jointName1", newName);

        		if(modelName.equals(ci.getProperty("objectName2")) && oldName.equals(ci.getProperty("jointName2")))
        			ci.setProperty("jointName2", newName);
        	}
        }
	}
    
    @Override
    public ValueEditType GetValueEditType(String key) {
        if(key.equals("jointType"))
        {
            return new ValueEditCombo(jointTypeComboItem_);
        }else if(key.equals("mode")){
            return new ValueEditCombo(modeComboItem_);
        }else if( key.equals("mass") || key.equals("centerOfMass") || key.equals("momentsOfInertia") ){
        	return null;
        }
        return super.GetValueEditType(key);
    }

    public void modifyMass(){
    	double w = 0.0;
    	for(int i=0; i<children_.size(); i++){
    		if(children_.get(i) instanceof GrxSegmentItem){
    			GrxSegmentItem segment = (GrxSegmentItem)children_.get(i);
    			w += segment.mass_;
    		}
    	}
    	mass(w);
    	modifyCenterOfMass();
    	modifyInertia();
    }
    
	public void modifyCenterOfMass() {
		double[] w = {0.0, 0.0, 0.0};
    	for(int i=0; i<children_.size(); i++){
    		if(children_.get(i) instanceof GrxSegmentItem){
    			GrxSegmentItem segment = (GrxSegmentItem)children_.get(i);
    			Vector3d com = segment.transformV3(new Vector3d(segment.centerOfMass_));
    			w[0] += segment.mass_ * com.x;
    			w[1] += segment.mass_ * com.y;
    			w[2] += segment.mass_ * com.z;
    			
    		}
    	}
    	for(int j=0; j<3; j++){
    		if(linkMass_==0.0)
    			w[j] = 0.0;
    		else
    			w[j] /= linkMass_;
    	}
    	centerOfMass(w);
    	modifyInertia();
    	model_.updateCoM();
	}

	public void modifyInertia() {
		double[] w = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		for(int i=0; i<children_.size(); i++){
    		if(children_.get(i) instanceof GrxSegmentItem){
    			GrxSegmentItem segment = (GrxSegmentItem)children_.get(i);
    			Matrix3d I = new Matrix3d(segment.momentOfInertia_);
    			Matrix3d R = new Matrix3d(); 
    			segment.getTransform().get(R);
    			Matrix3d W = new Matrix3d(); 
    			W.mul(R,I);
    			I.mulTransposeRight(W,R);   			
    			
    			Vector3d com = segment.transformV3(new Vector3d(segment.centerOfMass_));
    		    double x = com.x - linkCenterOfMass_[0];
   		        double y = com.y - linkCenterOfMass_[1];
   		        double z = com.z - linkCenterOfMass_[2];
   		        double m = segment.mass_;
   		        w[0] += I.m00 +  m * (y*y + z*z);
   		        w[1] += I.m01 - m * x * y;
   		        w[2] += I.m02 - m * x * z;
   		        w[3] += I.m10 - m * y * x;
   		        w[4] += I.m11 + m * (z*z + x*x);
   		        w[5] += I.m12 - m * y * z;
   		        w[6] += I.m20 - m * z * x;
   		        w[7] += I.m21 - m * z * y;
   		        w[8] += I.m22 + m * (x*x + y*y);
    		}
		}
		inertia(w);
	}
}

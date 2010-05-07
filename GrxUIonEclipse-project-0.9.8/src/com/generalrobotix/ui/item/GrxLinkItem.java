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
import java.util.List;
import java.util.Map;

import javax.media.j3d.Appearance;import javax.media.j3d.BadTransformException;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.BodyInfo;
import jp.go.aist.hrp.simulator.DblArray3SequenceHolder;
import jp.go.aist.hrp.simulator.HwcInfo;
import jp.go.aist.hrp.simulator.LinkInfo;
import jp.go.aist.hrp.simulator.SensorInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.osgi.util.NLS;

import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;
import com.generalrobotix.ui.util.AxisAngle4d;
import com.generalrobotix.ui.util.GrxShapeUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.OrderedHashMap;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.picking.PickTool;


@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxLinkItem extends GrxTransformItem{

	private LinkInfo info_;
	
    // display
    private Switch switchCom_;
    private TransformGroup tgCom_;
    private Switch switchBb_;
    private Switch switchAxis_;
    private Switch switchAABB_;

    private double  jointValue_;
    
    private int AABBmaxNum_;

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

    public double gearRatio(){
    	return info_.gearRatio;
    }

    public double encoderPulse(){
    	return info_.encoderPulse;
    }

    public double rotorInertia(){
    	return info_.rotorInertia;
    }

    public double rotorResistor(){
    	return info_.rotorResistor;
    }

    public double torqueConst(){
    	return info_.torqueConst;
    }

    public double [] ulimit(){
    	return info_.ulimit;
    }

    public double [] llimit(){
    	return info_.llimit;
    }

    public double [] uvlimit(){
    	return info_.uvlimit;
    }

    public double [] lvlimit(){
    	return info_.lvlimit;
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
	    	SensorInfo info = new SensorInfo();
	    	info.id = -1;
	    	info.type = new String("Force"); //$NON-NLS-1$
	    	info.translation = new double[]{0.0, 0.0, 0.0};
	    	info.rotation = new double[]{0.0, 0.0, 1.0, 0.0};
	    	info.specValues = new float[]{-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f};
	    	GrxSensorItem sensor = new GrxSensorItem(name, manager_, model_, info);
	    	addSensor(sensor);
	    	 manager_.itemChange(sensor, GrxPluginManager.ADD_ITEM);
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    	//manager_.reselectItems();
    }

    /**
     * @brief add child under this link
     * @param child child
     */
    public void addShape(GrxTransformItem child){
    	super.addChild(child);
    	resizeBoundingBox();
    }
    
    /**
     * @brief read shape from VRML97 and add
     * @param fPath URL of VRML file
     */
    public void addShape(String fPath){
    	super.addShape(fPath);
    	resizeBoundingBox();
    }
    
    public void addPrimitiveShape(String name){
    	super.addPrimitiveShape(name);
    	resizeBoundingBox();
    }
    
    /**
     * @brief remove child
     */
    public void removeChild(GrxTransformItem child){
    	super.removeChild(child);
    	resizeBoundingBox();
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
    
    public void addHwc(GrxHwcItem hwc){
    	addChild(hwc);
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
     * @brief set new joint value
     * @param jv joint value
     */
    public void jointValue(double jv){
    	jointValue_ = jv;
    	setDbl("angle", jointValue_); //$NON-NLS-1$
    }

    /**
     * @brief set new joint values recursively
     * @param values new joint values
     */
	public void jointValue(double[] values) {
		if (jointId() >= 0 && jointId() < values.length){
			jointValue(values[jointId()]);
		}
		for (int i=0; i<children_.size(); i++){
			if (children_.get(i) instanceof GrxLinkItem){
				GrxLinkItem link = (GrxLinkItem)children_.get(i);
				link.jointValue(values);
			}
		}
	}

    /**
     * @brief get current joint value
     * @return joint value
     */
    public double jointValue(){
    	return jointValue_;
    }

    /**
     * @brief compute CoM in global frame
     * @return computed CoM
     */
    public Vector3d absCoM(){
        Vector3d absCom = new Vector3d();
        Vector3d p = new Vector3d();
        Transform3D t3d = new Transform3D();
        absCom.set(centerOfMass());
        tg_.getTransform(t3d);
        t3d.transform(absCom);
        t3d.get(p);
        absCom.add(p);
        return absCom;
    }
    
    /**
     * @brief set joint type
     * @param type type of this joint. It must be one of "fixed", "free", "rotate" and "slide"
     */
    void jointType(String type){
		if (type.equals("fixed")||type.equals("rotate")||type.equals("free")||type.equals("slide")){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	    	info_.jointType = type;
	    	setProperty("jointType", type); //$NON-NLS-1$
	    	if(type.equals("fixed")||type.equals("free"))
	    		setProperty("jointAxis", "---");
	    	else
	    		if(getProperty("jointAxis").equals("---"))
	    			jointAxis("0.0 0.0 1.0");
    		if (model_ != null) model_.notifyModified();
		}
    }
    /**
     * @brief set joint axis
     * @param axis axis of this joint. it must be one of "X", "Y" and "Z"
     */
    void jointAxis(double[] newAxis){
    	if(info_.jointType.equals("fixed")||info_.jointType.equals("free"))
    		return;
    	if (newAxis != null && newAxis.length == 3){
    		info_.jointAxis = newAxis;
    		setDblAry("jointAxis", newAxis); //$NON-NLS-1$
    		resizeBoundingBox();
    		if (model_ != null) model_.notifyModified();
    	}  	
    }

    void jointAxis(String axis){
    	double[] newAxis = getDblAry(axis);
    	jointAxis(newAxis);
    }
    
    /**
     * set joint id from string
     * @param value string
     */
    void jointId(String value){
    	Short id = getShort(value);
    	if (id != null && id != info_.jointId){
    		info_.jointId = id;
    		setShort("jointId", id); //$NON-NLS-1$
    		if (model_ != null) model_.notifyModified();
    	}
    }
    
    /**
     * @brief set new translation
     * @param pos new translation
     * @return true if new translation is set successfully, false otherwise
     */
    public boolean translation(double[] pos){
    	if (super.translation(pos)){
        	info_.translation = pos;
        	return true;
    	}else{
    		return false;
    	}
    }

    /**
     * @brief set new rotation
     * @param rot new rotation(axis and angle, length=4)
     * @return true if set successfully, false otherwise
     */
    public boolean rotation(double[] rot){
    	if (super.rotation(rot)){
        	info_.rotation = rot;
        	return true;
    	}else{
    		return false;
    	}
    }

    /**
     * @brief set CoM position from string
     * @param value space separated array of double(length=3)
     */
    public void CoM(String value){
		double [] com = getDblAry(value);
		CoM(com);
    }
    
    /**
     * @brief set CoM position
     * @param com CoM position(length=3)
     * @return true if set successfully, false otherwise
     */
    public boolean CoM(double [] com){
		if (com != null && com.length==3){
        	info_.centerOfMass = com;
        	setDblAry("centerOfMass", com); //$NON-NLS-1$
    		if (model_ != null) model_.notifyModified();
        	Transform3D t3d = new Transform3D();
        	tgCom_.getTransform(t3d);
        	t3d.setTranslation(new Vector3d(centerOfMass()));
        	tgCom_.setTransform(t3d);
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
    		info_.inertia = newI;
    		setDblAry("momentsOfInertia", info_.inertia); //$NON-NLS-1$
    		_updateScaleOfBall();
    		if (model_ != null) model_.notifyModified();
    		return true;
       	}
    	return false;
    }
    
    /**
     * @brief set mass from string
     * @param value mass
     */
    public void mass(String value){
    	try{
    		double m = Double.parseDouble(value);
    		mass(m);
    	}catch(Exception ex){
    		
    	}
    }
    
    public void mass(double m){
    	info_.mass = m;
        setDbl("mass", info_.mass); //$NON-NLS-1$
        _updateScaleOfBall();
		if (model_ != null) model_.notifyModified();
    }
    
    private void _updateScaleOfBall(){
		Matrix3d I = new Matrix3d(inertia());
		double m = mass();
		Matrix3d R = new Matrix3d();
		Matrix3d II = new Matrix3d();
		if (diagonalize(I,R,II)){
			Transform3D t3d = new Transform3D();
			tgCom_.getTransform(t3d);
			double sum = II.m00+II.m11+II.m22;
			Vector3d sv = new Vector3d(
					m*Math.sqrt(sum/II.m00),
					m*Math.sqrt(sum/II.m11),
					m*Math.sqrt(sum/II.m22));
			t3d.setScale(sv);
			try{
				tgCom_.setTransform(t3d);
			}catch(BadTransformException ex){
				System.out.println("BadTransformException in _updateScaleOfBall"); //$NON-NLS-1$
				System.out.println("I = ("+II.m00+", "+II.m11+", "+II.m22+"), mass = "+m); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}else{
			System.out.println("diagonalization failed"); //$NON-NLS-1$
		}

    }
    /**
     * @brief check validity of new value of property and update if valid
     * @param property name of property
     * @param value value of property
     * @return true if checked(even if value is not used), false otherwise
     */
    public boolean propertyChanged(String property, String value) {
    	if (super.propertyChanged(property, value)){
    	}else if (property.equals("angle")){ //$NON-NLS-1$
    		if (jointValue(value)){
    			model_.updateInitialJointValue(this);
        		calcForwardKinematics();
    		}
    	}else if(property.equals("translation")){ //$NON-NLS-1$
    		if (translation(value)){
    			model_.updateInitialTransformRoot();
            	calcForwardKinematics();
    		}
    	}else if(property.equals("rotation")){ //$NON-NLS-1$
    		if (rotation(value)){
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
    		double[] limit = getDblAry(value);
    		if (limit != null){
    			info_.ulimit = limit;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("vlimit")){ //$NON-NLS-1$
    		double[] limit = getDblAry(value);
    		if (limit != null){
    			info_.llimit = limit;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("uvlimit")){ //$NON-NLS-1$
    		double[] limit = getDblAry(value);
    		if (limit != null){
    			info_.uvlimit = limit;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("lvlimit")){ //$NON-NLS-1$
    		double[] limit = getDblAry(value);
    		if (limit != null){
    			info_.lvlimit = limit;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("torqueConst")){ //$NON-NLS-1$
    		Double tc = getDbl(value);
    		if (tc != null){
        		info_.torqueConst = tc;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("rotorResistor")){ //$NON-NLS-1$
    		Double rr = getDbl(value);
    		if (rr != null){
        		info_.rotorResistor = rr;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("encoderPulse")){ //$NON-NLS-1$
    		Double tc = getDbl(value);
    		if (tc != null){
        		info_.torqueConst = tc;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("gearRatio")){ //$NON-NLS-1$
    		Double gr = getDbl(value);
    		if (gr != null){
        		info_.gearRatio = gr;
        		setProperty(property, value);
        		if (model_ != null) model_.notifyModified();
    		}
    	}else if(property.equals("centerOfMass")){ //$NON-NLS-1$
    		CoM(value);
    	}else if(property.equals("mass")){ //$NON-NLS-1$
    		mass(value);
    	}else if(property.equals("momentsOfInertia")){ //$NON-NLS-1$
    		double [] I = getDblAry(value);
    		inertia(I);
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
            if (jointType().equals("rotate")) { //$NON-NLS-1$ //$NON-NLS-2$
                t3d.setTranslation(v3d);
                m3d.set(new AxisAngle4d(rotation()));
                a4d.set(jointAxis()[0], jointAxis()[1], jointAxis()[2], jointValue());
                m3d2.set(a4d);
                m3d.mul(m3d2);
                t3d.setRotation(m3d);
            } else if(jointType().equals("slide")) { //$NON-NLS-1$
                v3d2.set(jointAxis()[0], jointAxis()[1], jointAxis()[2]);
                v3d2.scale(jointValue());
                v3d.add(v3d2);
                t3d.setTranslation(v3d);
                m3d.set(new AxisAngle4d(rotation()));
                m3d.mul(m3d);
                t3d.setRotation(m3d);
            }else if(jointType().equals("free") || jointType().equals("fixed") ){
            	t3d.setTranslation(v3d);
            	m3d.set(new AxisAngle4d(rotation()));
            	t3d.setRotation(m3d);
            }
            t3dp.mul(t3d);
            tg_.setTransform(t3dp);
        }else{
        	v3d.set(translation());
        	t3d.setTranslation(v3d);
        	t3d.setRotation(new AxisAngle4d(rotation()));
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
		info_ = new LinkInfo();
		info_.translation = new double[]{0.0, 0.0, 0.0};
		info_.rotation = new double[]{0.0, 0.0, 1.0, 0.0};
		info_.centerOfMass = new double[]{0.0, 0.0, 0.0};
		info_.inertia = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
		info_.mass = 1.0;
		info_.jointAxis = new double[]{0.0, 0.0, 1.0};
		info_.jointId = -1;
		info_.gearRatio = 1.0;
		info_.jointType = new String("rotate"); //$NON-NLS-1$
		info_.encoderPulse = 1.0;
		info_.torqueConst = 1.0;
		info_.ulimit = new double[]{};
		info_.llimit = new double[]{};
		info_.uvlimit = new double[]{};
		info_.lvlimit = new double[]{};
    	_init();
    }
	/**
     * @constructor
     * @param info link information retrieved through ModelLoader
     */
	protected GrxLinkItem(String name, GrxPluginManager manager, GrxModelItem model, LinkInfo info) {
		super(name, manager, model);
		info_ = info;
		_init();
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

		// menu item : add shape
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.VRML97"); //$NON-NLS-1$
			}
			public void run(){
				FileDialog fdlg = new FileDialog( GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);
				fdlg.setFilterExtensions(new String[]{"*.wrl"});
				fdlg.setFilterPath(getDefaultDir().getAbsolutePath());
				String fPath = fdlg.open();
				System.out.println("fPath = "+fPath); //$NON-NLS-1$
				if( fPath != null ) {
					addShape( fPath );
					setDefaultDirectory(new File(fPath).getParent());
				}
			}
		};
        setMenuItem(item);
        
        // menu item : add primitive shape
        MenuManager subMenu= new MenuManager(MessageBundle.get("GrxLinkItem.menu.primitiveShape")); //$NON-NLS-1$
        setSubMenu(subMenu);      
        item = new Action(){
			public String getText(){
				return "Box"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Box"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		item = new Action(){
			public String getText(){
				return "Cone"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Cone"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		item = new Action(){
			public String getText(){
				return "Cylinder"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Cylinder"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		item = new Action(){
			public String getText(){
				return "Sphere"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Sphere"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		setSubMenu(subMenu);
		
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
		
		// CoM display
		// 0.01 is default scale of ellipsoid
        switchCom_ = GrxShapeUtil.createBall(0.01, new Color3f(1.0f, 1.0f, 0.0f), 0.5f);
        tgCom_ = (TransformGroup)switchCom_.getChild(0);
        tg_.addChild(switchCom_);

        Transform3D tr = new Transform3D();
        tr.setIdentity();
        tg_.setTransform(tr);
        
        SceneGraphModifier modifier = SceneGraphModifier.getInstance();

        modifier.init_ = true;
        modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
        modifier._calcUpperLower(tg_, tr);
        
        Color3f color = new Color3f(1.0f, 0.0f, 0.0f);
        switchBb_ =  SceneGraphModifier._makeSwitchNode(modifier._makeBoundingBox(color));
        tg_.addChild(switchBb_);

        Vector3d jointAxis = new Vector3d(jointAxis());
        switchAxis_ = SceneGraphModifier._makeSwitchNode(modifier._makeAxisLine(jointAxis));
        tg_.addChild(switchAxis_);

        setIcon("joint.png"); //$NON-NLS-1$

        jointValue(0);
        translation(info_.translation);
        rotation(info_.rotation);
        CoM(info_.centerOfMass);
        inertia(info_.inertia);
        jointAxis(info_.jointAxis);
        jointType(info_.jointType);
        mass(info_.mass);
        setDblAry("ulimit", info_.ulimit); //$NON-NLS-1$
        setDblAry("llimit", info_.llimit); //$NON-NLS-1$
        setDblAry("uvlimit", info_.uvlimit); //$NON-NLS-1$
        setDblAry("lvlimit", info_.lvlimit); //$NON-NLS-1$
        setDbl("gearRatio", info_.gearRatio); //$NON-NLS-1$
        setDbl("torqueConst", info_.torqueConst); //$NON-NLS-1$
        setDbl("rotorInertia", info_.rotorInertia); //$NON-NLS-1$
        setDbl("rotorResistor", info_.rotorResistor); //$NON-NLS-1$
        setDbl("encoderPulse", info_.encoderPulse); //$NON-NLS-1$
        setDbl("jointVelocity", 0.0);
        setProperty("jointId", String.valueOf(info_.jointId)); //$NON-NLS-1$

        if (info_.ulimit == null || info_.ulimit.length == 0) {
            info_.ulimit = new double[]{0.0};
        }

        if (info_.llimit == null || info_.llimit.length == 0){
            info_.llimit = new double[]{0.0};
        }

        SensorInfo[] sinfo = info_.sensors;
        if (sinfo != null){
            for (int i=0; i<sinfo.length; i++) {
            	GrxSensorItem sensor = new GrxSensorItem(sinfo[i].name, manager_, model_, sinfo[i]);
            	addSensor(sensor);
            }
        }
        
        HwcInfo[] hinfo = info_.hwcs;
        if (hinfo != null){
        	for (int i=0; i<hinfo.length; i++) {
        		GrxHwcItem hwc = new GrxHwcItem(hinfo[i].name, manager_, model_, hinfo[i]);
        		addHwc(hwc);
        	}
        }
        
        Map<String, Object> userData = new Hashtable<String, Object>();
        userData.put("linkInfo", this); //$NON-NLS-1$
        userData.put("object", model_); //$NON-NLS-1$
        userData.put("boundingBoxSwitch", switchBb_); //$NON-NLS-1$
        tg_.setUserData(userData);
        tg_.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        
        switchAABB_ = SceneGraphModifier._makeSwitchNode();
        tg_.addChild(switchAABB_);
        AABBmaxNum_ = info_.AABBmaxNum;
        setProperty("NumOfAABB","original data"); //String.valueOf(AABBmaxDepth_));
        if(model_.getProperty(getName()+".NumOfAABB")!=null)
        	model_.remove(getName()+".NumOfAABB");
	}

	/**
	 * @brief resize bounding box and axis line which are displayed when this joint is selected
	 */
	private void resizeBoundingBox(){
		Transform3D trorg = new Transform3D();
		tg_.getTransform(trorg);
        try{
		Transform3D tr = new Transform3D();
        tg_.setTransform(tr);
        
        SceneGraphModifier modifier = SceneGraphModifier.getInstance();
 
        modifier.init_ = true;
        modifier.mode_ = SceneGraphModifier.RESIZE_BOUNDS;
        modifier._calcUpperLower(tg_, tr);
        
    	Shape3D shapeNode = (Shape3D)switchBb_.getChild(0);
    	Geometry gm = (Geometry)shapeNode.getGeometry(0);

    	Point3f[] p3fW = modifier._makePoints();
    	if (gm instanceof QuadArray) {
    		QuadArray qa = (QuadArray) gm;
    		qa.setCoordinates(0, p3fW);
		}

    	shapeNode = (Shape3D)switchAxis_.getChild(0);
    	gm = (Geometry)shapeNode.getGeometry(0);
    	
    	p3fW = modifier.makeAxisPoints(new Vector3d(jointAxis()));
    	if (gm instanceof LineArray){
    		LineArray la = (LineArray)gm;
    		la.setCoordinates(0, p3fW);
    	}
        }catch(Exception ex){
        	ex.printStackTrace();
        }
        tg_.setTransform(trorg);
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
		if (parent_ != null) return;
		
    	if (pos != null){
    		double[] newpos = new double[3];
    		pos.get(newpos);
    		translation(newpos);
    	}
    	if (rot != null){
    		AxisAngle4d a4d = new AxisAngle4d();
    		a4d.setMatrix(rot);
    		double[] newrot = new double[4];
    		a4d.get(newrot);
    		rotation(newrot);
    	}
    	if (pos != null || rot != null)	calcForwardKinematics();
	}

	/**
	 * @brief set new position and rotation in global frame
	 * @param pos new position(length = 3)
	 * @param rot new rotation(length = 9)
	 */
	public void setTransform(double[] pos, double[] rot) {
		Vector3d v3d = new Vector3d(pos);
		Matrix3d m3d = new Matrix3d(rot);
		setTransform(v3d, m3d);
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
        if (llimit() != null && ulimit() != null && llimit()[0] < ulimit()[0]) {
            if (jointValue() < llimit()[0])
                jointValue(llimit()[0]);
            else if (ulimit()[0] < jointValue())
                jointValue(ulimit()[0]);
        }
        for (int i=0; i<children_.size(); i++){
        	if (children_.get(i) instanceof GrxLinkItem){
        		GrxLinkItem link = (GrxLinkItem)children_.get(i);
        		link.setJointValuesWithinLimit();
        	}
        }
	}

	/**
	 * @brief make CoM ball visible/invisible
	 * @param b true to make visible, false otherwise
	 */
    public void setVisibleCoM(boolean b) {
        switchCom_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
    }
    
    /**
     * @brief set/unset fucus on this item
     * 
     * When this item is focused, some geometries are displayed
     * @param b true to fucus, false to unfocus
     */
    public void setFocused(boolean b){
    	//if (b!=isSelected()) System.out.println("GrxLinkItem.setFocused("+getName()+" of "+model_.getName()+", flag = "+b+")");
    	super.setFocused(b);
    	setVisibleCoM(b);
    	if (b){
    		resizeBoundingBox();
			switchBb_.setWhichChild(Switch.CHILD_ALL);
    		if (jointType().equals("rotate") || jointType().equals("slide")) { //$NON-NLS-1$ //$NON-NLS-2$
    			switchAxis_.setWhichChild(Switch.CHILD_ALL);
    		}
    	}else{
            switchBb_.setWhichChild(Switch.CHILD_NONE);
            switchAxis_.setWhichChild(Switch.CHILD_NONE);
    	}
    }

    /**
     * @brief diagonalize symmetric matrix
     * @param a symmetric matrix
     * @param U orthogonal matrix
     * @param W diagonal matrix
     * @return true if diagonalized successfully, false otherwise
     */
    static boolean diagonalize(Matrix3d a, Matrix3d U, Matrix3d W){
    	int i=0,j=0,l,m,p,q,count;
    	double max,theta;
    	Matrix3d oldU = new Matrix3d();
    	Matrix3d newW = new Matrix3d();
    	W.set(a);

    	//計算結果としてだされた直行行列を格納するための配列を単位行列に初期化しておく。
    	for(p=0;p<3;p++) {
    		for(q=0;q<3;q++) {
    			if(p==q){
    				U.setElement(p, q, 1.0);
    			}else{
    				U.setElement(p, q, 0.0);
    			}
    		}
    	}

    	for(count=0;count<=10000;count++) {

    		//配列olduは新たな対角化計算を行う前にかけてきた直行行列を保持する。
    		for(p=0;p<3;p++) {
    			for(q=0;q<3;q++) {
    				oldU.setElement(p, q, U.getElement(p, q));
    			}
    		}
    		//非対角要素の中から絶対値の最大のものを見つける
    		max=0.0;
    		for(p=0;p<3;p++) {
    			for(q=0;q<3;q++) {
    				if(max<Math.abs(W.getElement(p, q)) && p!=q) {
    					max=Math.abs(W.getElement(p, q));
    					//その最大のものの成分の行と列にあたる数を記憶しておく。
    					i=p;
    					j=q;
    				}
    			}
    		}
    		/*先ほど選んだ最大のものが指定の値より小さければ対角化終了*/
    		if(max < 1.0e-10) {
    			break;
    		}
    		/*条件によってシータの値を決める*/
    		if(W.getElement(i,i)==W.getElement(j,j)){
    			theta=Math.PI/4.0;
    		}else{
    			theta=Math.atan(-2*W.getElement(i,j)/(W.getElement(i,i)-W.getElement(j,j)))/2.0;
    		}

    		//ここでこのときに実対称行列にかける個々の直行行列uが決まるが 特にここでの計算の意味はない。(する必要はない。)*/
    		double sth = Math.sin(theta);
    		double cth = Math.cos(theta);

    		/*ここでいままで実対称行列にかけてきた直行行列を配列Uに入れる。*/
    		for(p=0;p<3;p++) {
    			U.setElement(p,i,oldU.getElement(p,i)*cth-oldU.getElement(p,j)*sth);
    			U.setElement(p,j,oldU.getElement(p,i)*sth+oldU.getElement(p,j)*cth);
    		}

    		//対角化計算によってでた新たな実対称行列の成分を配列newaに入れる。
    		newW.setElement(i,i,W.getElement(i,i)*cth*cth
    				+W.getElement(j,j)*sth*sth-2.0*W.getElement(i,j)*sth*cth);
    		newW.setElement(j, j, W.getElement(i,i)*sth*sth
    				+W.getElement(j,j)*cth*cth+2.0*W.getElement(i,j)*sth*cth);
    		newW.setElement(i,j,0.0);
    		newW.setElement(j,i,0.0);
    		for(l=0;l<3;l++) {
    			if(l!=i && l!=j) {
    				newW.setElement(i,l,W.getElement(i,l)*cth-W.getElement(j,l)*sth);
    				newW.setElement(l,i,newW.getElement(i,l));
    				newW.setElement(j,l,W.getElement(i,l)*sth+W.getElement(j,l)*cth);
    				newW.setElement(l,j,newW.getElement(j,l));
    			}
    		}
    		for(l=0;l<3;l++) {
    			for(m=0;m<3;m++) {
    				if(l!=i && l!=j && m!=i && m!=j) newW.setElement(l, m, W.getElement(l,m));
    			}
    		}

    		//次の対角化計算を行う行列の成分を配列aへ上書きする。
    		W.set(newW);

    	}
    	if(count==10000) {
    		System.out.println("対角化するためにはまだ作業を繰り返す必要があります"); //$NON-NLS-1$
    		return false;
    	}else{
    		return true;
    	}
    }

	/**
	 * delete this link and children
	 */
	public void delete(){
		super.delete();
		model_.removeLink(this);
		manager_.itemChange(this, GrxPluginManager.REMOVE_ITEM);
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
    	
		super.rename(newName);

        OrderedHashMap mcoll = manager_.pluginMap_.get(GrxCollisionPairItem.class);
        if(mcoll != null)
        {
        	String modelName = model().getName();
        	Iterator it = mcoll.values().iterator();
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
        }
        return super.GetValueEditType(key);
    }
}

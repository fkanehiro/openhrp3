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

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import jp.go.aist.hrp.simulator.LinkInfo;
import jp.go.aist.hrp.simulator.SensorInfo;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;
import com.generalrobotix.ui.view.vsensor.Camera_impl;


@SuppressWarnings("serial")
public class GrxLinkItem extends GrxTransformItem{

	private LinkInfo info_;

    // TODO remove cameras_
    public Vector<Camera_impl> cameras_;

    private double  jointValue_;

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

    public double rotorResister(){
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
    	System.out.println("GrxLinkItem.addLink("+name+") is called");
    	try{
    		GrxLinkItem newLink = new GrxLinkItem(name, manager_);
    		addLink(newLink);
        	System.out.println("GrxLinkItem.addLink("+name+") is done");
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    	manager_.reselectItems();
    }

    /**
     * @brief add a child link
     * @param child child link
     */
    public void addLink(GrxLinkItem child){
    	children_.add(child);
    	child.parent_ = this;
    	BranchGroup bg = (BranchGroup)bg_.getParent();
    	bg.addChild(child.bg_);
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
	    	info.type = new String("Force");
	    	info.translation = new double[]{0.0, 0.0, 0.0};
	    	info.rotation = new double[]{0.0, 0.0, 1.0, 0.0};
	    	info.specValues = new float[]{-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f};
	    	GrxSensorItem sensor = new GrxSensorItem(name, manager_, info);
	    	addSensor(sensor);
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    	manager_.reselectItems();
    }

    /**
     * @brief add child under this link
     * @param child child
     */
    public void addShape(GrxTransformItem child){
    	super.addChild(child);
    	rebuildBoundingBox();
    }
    
    /**
     * @brief remove child
     */
    public void removeChild(GrxTransformItem child){
    	super.removeChild(child);
    	rebuildBoundingBox();
    }

    /**
     * @brief add a sensor as a child
     * @param sensor sensor
     */
    public void addSensor(GrxSensorItem sensor){
    	addChild(sensor);
    	if (sensor.camera_ != null){
    		cameras_.add(sensor.camera_);
    	}
    }

    public void removeSensor(GrxSensorItem sensor){
    	removeChild(sensor);
    	if (sensor.camera_ != null){
    		cameras_.remove(sensor.camera_);
    		// TODO : GrxModelItem.cameraList_ must be updated
    	}
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
     * @brief properties are set to robot
     */
    public void propertyChanged() {
    	//System.out.println("GrxLinkItem::propertyChanged()");
    	super.propertyChanged();
    	jointValue_ = getDbl("angle", 0.0);
    	info_.translation = getDblAry("translation",null);
    	info_.rotation = getDblAry("rotation", null);
    	info_.jointAxis = getDblAry("jointAxis", null);
    	// TODO rebuild axis shape
    	info_.jointType = getStr("jointType", null);
    	calcForwardKinematics();
    	// joint properties
    	info_.jointId = getShort("jointId", null);
    	info_.ulimit = getDblAry("ulimit", null);
    	info_.llimit = getDblAry("llimit", null);
    	info_.uvlimit = getDblAry("uvlimit", null);
    	info_.lvlimit = getDblAry("lvlimit", null);
    	// motor & gear properties
    	info_.torqueConst = getDbl("torqueConst", null);
    	info_.rotorResistor = getDbl("rotorResistor", null);
    	info_.encoderPulse = getDbl("encoderPulse", null);
    	info_.gearRatio = getDbl("gearRatio", null);
    	// mass properties
    	info_.centerOfMass = getDblAry("centerOfMass", null);
    	info_.mass = getDbl("mass", null);
    	info_.inertia = getDblAry("inertia", null);
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

    protected GrxLinkItem(String name, GrxPluginManager manager){
    	super(name, manager);
		info_ = new LinkInfo();
		info_.translation = new double[]{0.0, 0.0, 0.0};
		info_.rotation = new double[]{0.0, 0.0, 1.0, 0.0};
		info_.centerOfMass = new double[]{0.0, 0.0, 0.0};
		info_.inertia = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		info_.jointAxis = new double[]{0.0, 0.0, 1.0};
		info_.jointId = -1;
		info_.gearRatio = 1.0;
		info_.jointType = new String("rotate");
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
	protected GrxLinkItem(String name, GrxPluginManager manager, LinkInfo info) {
		super(name, manager);
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
				return "add shape from VRML97";
			}
			public void run(){
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				FileDialog fdlg = new FileDialog( window.getShell(), SWT.OPEN);
				String fPath = fdlg.open();
				System.out.println("fPath = "+fPath);
				if( fPath != null ) {
					addShape( fPath );
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
		
        cameras_ = new Vector<Camera_impl>();

        jointValue(0);

        setIcon("joint.png");

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
        setDbl("gearRatio", info_.gearRatio);
        setDbl("torqueConst", info_.torqueConst);
        setDbl("rotorInertia", info_.rotorInertia);
        setDbl("rotorResistor", info_.rotorResistor);
        setDbl("encoderPulse", info_.encoderPulse);
        setProperty("jointId", String.valueOf(info_.jointId));

        if (info_.ulimit == null || info_.ulimit.length == 0) {
            info_.ulimit = new double[]{0.0};
        }

        if (info_.llimit == null || info_.llimit.length == 0){
            info_.llimit = new double[]{0.0};
        }

        SensorInfo[] sinfo = info_.sensors;
        if (sinfo != null){
            for (int i=0; i<sinfo.length; i++) {
            	GrxSensorItem sensor = new GrxSensorItem(sinfo[i].name, manager_, sinfo[i]);
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
        }
        Map<String, Object> userData = new Hashtable<String, Object>();
        userData.put("linkInfo", this);
        tg_.setUserData(userData);
        tg_.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        
        Transform3D tr = new Transform3D();
        tr.setIdentity();
        tg_.setTransform(tr);
        
        SceneGraphModifier modifier = SceneGraphModifier.getInstance();

        modifier.init_ = true;
        modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
        modifier._calcUpperLower(tg_, tr);
        
        Color3f color = new Color3f(1.0f, 0.0f, 0.0f);
        Switch bbSwitch =  modifier._makeSwitchNode(modifier._makeBoundingBox(color));
        tg_.addChild(bbSwitch);
        userData.put("boundingBoxSwitch", bbSwitch);

        Vector3d jointAxis = new Vector3d(jointAxis());
        if (jointAxis != null) {
            Switch axisSwitch = modifier._makeSwitchNode(modifier._makeAxisLine(jointAxis));
            tg_.addChild(axisSwitch);
            userData.put("axisLineSwitch", axisSwitch);
        }
	}

	/**
	 * @brief rebuild bounding box which is displayed when this joint is selected
	 */
	private void rebuildBoundingBox(){
        Transform3D tr = new Transform3D();
        Transform3D org = new Transform3D();
        tr.setIdentity();
        tg_.getTransform(org);
        tg_.setTransform(tr);
        
        SceneGraphModifier modifier = SceneGraphModifier.getInstance();
        Hashtable<String, Object> userData = SceneGraphModifier.getHashtableFromTG(tg_);
 
        modifier.init_ = true;
        modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
        modifier._calcUpperLower(tg_, tr);
        
        Switch bbSwitch =  (Switch)userData.get("boundingBoxSwith");
    	Shape3D shapeNode = (Shape3D)bbSwitch.getChild(0);
    	Geometry gm = (Geometry)shapeNode.getGeometry(0);

    	Point3f[] p3fW = modifier._makePoints();
    	if (gm instanceof QuadArray) {
    		QuadArray qa = (QuadArray) gm;
    		qa.setCoordinates(0, p3fW);
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
	 * @brief set new position and rotation
	 * @param pos new position
	 * @param rot new rotation
	 */
	public void setTransform(Vector3d pos, Matrix3d rot) {
    	if (pos != null){
    		double[] newpos = new double[3];
    		pos.get(newpos);
        	setDblAry("translation", newpos);
    	}
    	if (rot != null){
    		AxisAngle4d a4d = new AxisAngle4d();
    		a4d.set(rot);
    		double[] newrot = new double[4];
    		a4d.get(newrot);
    		setDblAry("rotation", newrot);
    	}
    	if (pos != null || rot != null)	propertyChanged();
	}

	/**
	 * @brief set new position and rotation in global frame
	 * @param pos new position(length = 3)
	 * @param rot new rotation(length = 9)
	 */
	public void setTransform(double[] pos, double[] rot) {
		Transform3D t3d = new Transform3D();
		Vector3d v3d = new Vector3d(pos);
		Matrix3d m3d = new Matrix3d(rot);
		t3d.setTranslation(v3d);
		t3d.setRotation(m3d);
		tg_.setTransform(t3d);
	}

	/**
	 * @brief set new position and rotation
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

	
}

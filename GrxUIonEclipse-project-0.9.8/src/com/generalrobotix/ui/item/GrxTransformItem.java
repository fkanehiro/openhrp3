/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */


package com.generalrobotix.ui.item;

import java.util.List;
import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;

import javax.media.j3d.BadTransformException;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import com.generalrobotix.ui.util.AxisAngle4d;

import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.ModelLoader;
import jp.go.aist.hrp.simulator.ModelLoaderHelper;
import jp.go.aist.hrp.simulator.ModelLoaderPackage.ModelLoaderException;
import jp.go.aist.hrp.simulator.SceneInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;

import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxShapeUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;

/**
 * @brief item which have a transformation
 */
@SuppressWarnings("serial")
public class GrxTransformItem extends GrxBaseItem {
	public TransformGroup tg_;	
	public BranchGroup bg_;
	
	public GrxTransformItem parent_;
	public Vector<GrxTransformItem> children_;
	
	private Switch switchAxes_;
	private Switch switchBb_;
	protected GrxModelItem model_;

    /**
     * @brief constructor
     * @name name of this item
     * @manager PluginManager
     */
    public GrxTransformItem(String name, GrxPluginManager manager, GrxModelItem model) {
    	super(name, manager);
    	model_ = model;
    	
        tg_ = new TransformGroup();
        tg_.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        tg_.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        switchAxes_ = GrxShapeUtil.createAxes();
        tg_.addChild(switchAxes_);
        
        bg_ = new BranchGroup();
        bg_.setCapability(BranchGroup.ALLOW_DETACH);
        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        
        bg_.addChild(tg_);

        createBoundingBox();
        
        children_ = new Vector<GrxTransformItem>();   

    }

    /**
     * @brief add a child
     * @param child child
     */
    public void addChild(GrxTransformItem child){
    	children_.add(child);
    	child.parent_ = this;
    	tg_.addChild(child.bg_);
    	resizeBoundingBox();
    }

    /**
     * @brief remove a child from children
     * @param child child
     */
    public void removeChild(GrxTransformItem child){
    	children_.remove(child);
    	child.bg_.detach();
    	resizeBoundingBox();
    }

    /**
     * @brief delete this item and children
     */
    public void delete() {
    	try{
    	// delete children first
    	while (children_.size() > 0){
    		children_.get(0).delete();
    	}

    	if (parent_ != null){
    		parent_.removeChild(this);
    		// I don't know why the following line is required.
    		// But without the line, this transform is moved to the origin.
			model_.calcForwardKinematics();
    	}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
		manager_.itemChange(this, GrxPluginManager.REMOVE_ITEM);
    }

    /**
     * @brief get model item to which this item belongs
     * @return model item
     */
	public GrxModelItem model() {
		return model_;
	}
	
	public void setFocused(boolean b){
		super.setFocused(b);
		switchAxes_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
		switchBb_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
	}
	
	private void createBoundingBox(){
		 SceneGraphModifier modifier = SceneGraphModifier.getInstance();
		 modifier.init_ = true;
	     modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
	     Color3f color = new Color3f(1.0f, 0.0f, 0.0f);
	     switchBb_ =  SceneGraphModifier._makeSwitchNode(modifier._makeBoundingBox(color));
	     tg_.addChild(switchBb_);
	}
	
	protected void resizeBoundingBox(){
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
	
	    }catch(Exception ex){
	        	ex.printStackTrace();

	    }
	    tg_.setTransform(trorg);
    }
	
	public void gatherSensors(String type, List<GrxSensorItem> sensors){
		if (this instanceof GrxSensorItem){
			GrxSensorItem sensor = (GrxSensorItem)this;
			if (sensor.type_.equals(type)){
				sensors.add(sensor);
			}
		}
		for (int i=0; i<children_.size(); i++){
			children_.get(i).gatherSensors(type, sensors);
		}
	}
	
	// (Vector3)v = (Matrix4d)tg_*(Vector3d)v 
	public Vector3d transformV3  (Vector3d v){
		Transform3D t3d = new Transform3D();
		Vector3d p = new Vector3d();
		Vector3d _v = new Vector3d(v);
        tg_.getTransform(t3d);
        t3d.transform(v, _v);
        t3d.get(p);
        _v.add(p);
        return _v;
	}
	
}

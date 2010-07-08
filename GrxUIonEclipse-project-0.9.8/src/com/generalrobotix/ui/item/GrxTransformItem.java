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
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import com.generalrobotix.ui.util.AxisAngle4d;
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
    }

    /**
     * @brief remove a child from children
     * @param child child
     */
    public void removeChild(GrxTransformItem child){
    	children_.remove(child);
    	child.bg_.detach();
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

    	super.delete();
    	if (parent_ != null){
    		parent_.removeChild(this);
    		manager_.itemChange(this, GrxPluginManager.REMOVE_ITEM);
    		// I don't know why the following line is required.
    		// But without the line, this transform is moved to the origin.
			model_.calcForwardKinematics();
    	}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }

    /**
     * @brief set new translation from string
     * @param value string of space separated array of double(length=3)
     * @return true if set successfully, false otherwise
     */
    public boolean translation(String value){
    	double [] pos = getDblAry(value);
    	if (translation(pos)){
            return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * @brief set new translation to TransformGroup
     * @param pos new translation(length=3)
     * @return true if new translation is set successfully, false otherwise
     */
    public boolean translation(double[] pos){
    	if (pos == null || pos.length != 3) return false;
        Transform3D t3d = new Transform3D();
        tg_.getTransform(t3d);
        Vector3d v = new Vector3d(pos);
        t3d.setTranslation(v);
        setDblAry("translation", pos, 4);
        if (model_ != null && parent_ != null) model_.notifyModified();
        try{
        	tg_.setTransform(t3d);
        }catch(BadTransformException e){
        	System.out.println("Invalid translation:"+v+" is applied to "+getName());
        	return false;
        }
        return true;
    }

    /**
     * @brief set new rotation from string
     * @param value string of space separated array of double(length=4)
     * @return true if set successfully, false otherwise
     */
    public boolean rotation(String value){
    	double [] rot = getDblAry(value);
    	if (rotation(rot)){
            return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * @breif set new rotation to TransformGroup
     * @param rot new rotation(axis and angle, length=4)
	 * @return true if new rotation is set successfully, false otherwise
     */
    public boolean rotation(double[] rot){
    	if (rot == null || rot.length != 4) return false;
        Transform3D t3d = new Transform3D();
        tg_.getTransform(t3d);
        t3d.setRotation(new AxisAngle4d(rot));
        tg_.setTransform(t3d);
        setDblAry("rotation", rot, 4);
        if (model_ != null && parent_ != null) model_.notifyModified();
        return true;
    }

    /**
     * @brief create and add a new shape as a child
     * @param url URL of the file where shape is described
     */
    public void addShape(String fPath){
		if( fPath != null ) {
            try {
                ModelLoader mloader = ModelLoaderHelper.narrow(
                        GrxCorbaUtil.getReference("ModelLoader"));
                    
                SceneInfo sInfo = mloader.loadSceneInfo(fPath);
                int n=children_.size();
                GrxShapeItem shapeItem = new GrxShapeItem(getName()+"_shape_"+n, manager_, model_);
                shapeItem.loadnewInlineShape(sInfo);
                shapeItem.setURL(fPath);
                addChild(shapeItem);
                
            	//manager_.reselectItems();
                manager_.itemChange(shapeItem, GrxPluginManager.ADD_ITEM);
            } catch(ModelLoaderException me){
                MessageDialog.openError(GrxUIPerspectiveFactory.getCurrentShell(),
                                        MessageBundle.get("GrxModelItem.dialog.title.error"), //$NON-NLS-1$
                                        MessageBundle.get("GrxModelItem.dialog.message.loadSceneError") +"\n" + //$NON-NLS-1$ //$NON-NLS-2$
                                        fPath + "\n\n" + me.description); //$NON-NLS-1$
                System.out.println("Failed to load scene info:" + fPath);
                me.printStackTrace();
            } catch(Exception ex){
                System.out.println("Failed to load scene info:" + fPath);
                ex.printStackTrace();
            }
		}

    }
    
    public void addPrimitiveShape(String name){
    	int n=children_.size();
    	int type;
    	if(name.equals("Box"))
    		type = ShapePrimitiveType._SP_BOX;
    	else if(name.equals("Cone"))
    		type = ShapePrimitiveType._SP_CONE;
    	else if(name.equals("Cylinder"))
    		type = ShapePrimitiveType._SP_CYLINDER;
    	else if(name.equals("Sphere"))
    		type = ShapePrimitiveType._SP_SPHERE;
    	else
    		type = -1;
        GrxShapeItem shapeItem = new GrxShapeItem(getName()+"_"+name+"_"+n, manager_, model_);
        shapeItem.createnewPrimitiveShape(type);
    	addChild(shapeItem);
    	//manager_.reselectItems();
        manager_.itemChange(shapeItem, GrxPluginManager.ADD_ITEM);
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
	}
	
	public void gatherSensors(String type, List<GrxSensorItem> sensors){
		if (this instanceof GrxSensorItem){
			GrxSensorItem sensor = (GrxSensorItem)this;
			if (sensor.type().equals(type)){
				sensors.add(sensor);
			}
		}
		for (int i=0; i<children_.size(); i++){
			children_.get(i).gatherSensors(type, sensors);
		}
	}
}

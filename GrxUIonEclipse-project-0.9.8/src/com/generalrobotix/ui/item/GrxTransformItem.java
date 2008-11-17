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

import java.util.Vector;

import javax.media.j3d.BadTransformException;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.AppearanceInfo;
import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ModelLoader;
import jp.go.aist.hrp.simulator.ModelLoaderHelper;
import jp.go.aist.hrp.simulator.SceneInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.TextureInfo;
import jp.go.aist.hrp.simulator.TransformedShapeIndex;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxCorbaUtil;

/**
 * @brief item which have a transformation
 */
@SuppressWarnings("serial")
public class GrxTransformItem extends GrxBaseItem {
	public TransformGroup tg_;
	public BranchGroup bg_;
	
	public GrxTransformItem parent_;
	public Vector<GrxTransformItem> children_;
	
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
        tg_.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        tg_.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        bg_ = new BranchGroup();
        bg_.setCapability(BranchGroup.ALLOW_DETACH);
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
    	// delete children first
    	while (children_.size() > 0){
    		children_.get(0).delete();
    	}

    	// TODO : implement delete this link
    	super.delete();
    	if (parent_ != null){
    		parent_.removeChild(this);
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
        	//System.out.println("Invalid translation:"+v+" is applied to "+getName());
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
                ShapeInfo[] shapes = sInfo.shapes();
                AppearanceInfo[] appearances = sInfo.appearances();
                MaterialInfo[] materials = sInfo.materials();
                TextureInfo[] textures = sInfo.textures();
                TransformedShapeIndex[] tsiDim = sInfo.shapeIndices();
                
                for (int i=0; i<tsiDim.length; i++){
                    TransformedShapeIndex tsi = tsiDim[i];
                    int shapeIndex = tsi.shapeIndex;
                    ShapeInfo shapeInfo = shapes[shapeIndex];
                    AppearanceInfo appearanceInfo = null;
                    MaterialInfo materialInfo = null;
                    TextureInfo textureInfo = null;
                    if (shapeInfo.appearanceIndex >= 0){
                        appearanceInfo = appearances[shapeInfo.appearanceIndex];
                        if (appearanceInfo.materialIndex >= 0){
                            materialInfo = materials[appearanceInfo.materialIndex];
                        }
                        if (appearanceInfo.textureIndex >= 0){
                        	textureInfo = textures[appearanceInfo.textureIndex];
                        }
                    }
                    GrxShapeItem shape = new GrxShapeItem(getName()+"_shape_"+i, manager_, model_, tsi.transformMatrix,
							shapeInfo, appearanceInfo, materialInfo, textureInfo);
                    shape.setURL(fPath);
					addChild(shape);
                }
            	manager_.reselectItems();
            } catch(Exception ex){
                System.out.println("Failed to load scene info:" + fPath);
                ex.printStackTrace();
            }
		}

    }

    /**
     * @brief get model item to which this item belongs
     * @return model item
     */
	public GrxModelItem model() {
		return model_;
	}

}

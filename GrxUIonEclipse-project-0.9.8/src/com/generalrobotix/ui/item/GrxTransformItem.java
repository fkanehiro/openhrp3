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

import javax.media.j3d.BranchGroup;
import javax.media.j3d.TransformGroup;

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

    /**
     * @brief constructor
     * @name name of this item
     * @manager PluginManager
     */
    public GrxTransformItem(String name, GrxPluginManager manager) {
    	super(name, manager);
    	
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
                
                System.out.println("tsiDim.length = "+tsiDim.length);
                System.out.println("shapes.length = "+shapes.length);
                System.out.println("appearances.length = "+appearances.length);
                System.out.println("textures.length = "+textures.length);
                System.out.println("materials.length = "+materials.length);
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
                    GrxShapeItem shape = new GrxShapeItem(getName()+"_shape_"+i, manager_, tsi.transformMatrix,
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
}

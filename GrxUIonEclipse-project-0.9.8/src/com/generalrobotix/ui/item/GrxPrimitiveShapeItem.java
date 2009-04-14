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

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;

import jp.go.aist.hrp.simulator.AppearanceInfo;
import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;
import jp.go.aist.hrp.simulator.TextureInfo;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;

@SuppressWarnings("serial")

public class GrxPrimitiveShapeItem extends GrxShapeItem{
       
	private Primitive primitive;
	private int primitiveType;
	private BranchGroup bg_;
	private int primitiveFlag;
	
   	public GrxPrimitiveShapeItem(int type, String name, GrxPluginManager manager, GrxModelItem model) {
    	super(name, manager, model);
    	
    	shapes_ = new ShapeInfo[1];
    	shapes_[0] = new ShapeInfo();
    	appearances_ = new AppearanceInfo[1];
    	appearances_[0] = new AppearanceInfo();
        materials_ = new MaterialInfo[1];
        materials_[0] = new MaterialInfo();
        textures_ = new TextureInfo[1];
        
    	Transform3D t3d = new Transform3D();
    	tg_.setTransform(t3d);
    	
    	bg_ = new BranchGroup();
    	bg_.setCapability(BranchGroup.ALLOW_DETACH);
    	bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        
        Appearance appearance = createAppearance(); 
        materials_[0].diffuseColor = new float[3];
        materials_[0].diffuseColor[0] = materials_[0].diffuseColor[1] = materials_[0].diffuseColor[2] =0.8f;
        materials_[0].emissiveColor = new float[3];
        materials_[0].emissiveColor[0] = materials_[0].emissiveColor[1] = materials_[0].emissiveColor[2] =0.0f;
        materials_[0].specularColor = new float[3];
        materials_[0].specularColor[0] = materials_[0].specularColor[1] = materials_[0].specularColor[2] =0.0f;
        materials_[0].ambientIntensity = 0.2f;
        materials_[0].shininess = 0.2f;
        materials_[0].transparency = 0.0f;
        setMaterial(appearance, materials_[0]);
                  
        primitiveFlag = Primitive.GEOMETRY_NOT_SHARED | Primitive.GENERATE_NORMALS;
    	
    	primitiveType = type;
    	switch(type){
    	case ShapePrimitiveType._SP_BOX :
	    	float[] size=new float[3];
	    	size[0] = size[1] = size[2] = 1.0f;
	    	primitive = new Box(size[0]/2, size[1]/2, size[2]/2, primitiveFlag, appearance);
	    	shapes_[0].primitiveType = ShapePrimitiveType.SP_BOX;
			shapes_[0].primitiveParameters = size;
			setFltAry("size", size);
			break;
    	case ShapePrimitiveType._SP_CONE :
    		float bottomRadius = 1.0f;
    		float height = 2.0f;
    		primitive = new Cone(bottomRadius, height, primitiveFlag, appearance);
    		shapes_[0].primitiveType = ShapePrimitiveType.SP_CONE;
    		float[] param = new float[4];
    		param[0] = bottomRadius;
    		param[1] = height;
    		param[2] = 1;   
    		param[3] = 1;	
			shapes_[0].primitiveParameters = param;
			setFlt("bottomRadius", bottomRadius);
			setFlt("height", height);
			setProperty("side","true");
			setProperty("bottom","true");
    		break;
    	case ShapePrimitiveType._SP_CYLINDER :
    		float radius = 1.0f;
    		height = 2.0f;
    		primitive = new Cylinder(radius, height, primitiveFlag, appearance);
    		shapes_[0].primitiveType = ShapePrimitiveType.SP_CYLINDER;
    		param = new float[5];
    		param[0] = radius;
    		param[1] = height;
    		param[2] = 1;   
    		param[3] = 1;	
    		param[4] = 1;
			shapes_[0].primitiveParameters = param;
			setFlt("radius", radius);
			setFlt("height", height);
			setProperty("side","true");
			setProperty("bottom","true");
			setProperty("top","true");
    		break;
    	case ShapePrimitiveType._SP_SPHERE :
    		radius = 1.0f;
    		primitive = new Sphere(radius, primitiveFlag, appearance);
    		shapes_[0].primitiveType = ShapePrimitiveType.SP_SPHERE;
    		param = new float[1];
    		param[0] = radius;
    		shapes_[0].primitiveParameters = param;
			setFlt("radius", radius);
			break;
    	default :
    		break;	
    	}
		
    	bg_.addChild(primitive);
    	tg_.addChild(bg_);

    	initialize(t3d);
    	setFltAry("diffuseColor", materials_[0].diffuseColor);
    
   	}
   	
   	public boolean propertyChanged(String property, String value) {
    	if (super.propertyChanged(property, value)){
    	}else if(property.equals("size")){
    		size(value);
    	}else if(property.equals("bottomRadius") || property.equals("height") || property.equals("radius") ||
    			property.equals("side") || property.equals("bottom") || property.equals("top")){
    		if(primitiveType==ShapePrimitiveType._SP_CONE)
    			coneSize(property, value);
    		else if(primitiveType==ShapePrimitiveType._SP_CYLINDER)
    			cylinderSize(property, value);
    		else 
    			sphereSize(value);
    	}else if(property.equals("diffuseColor")){
    		diffuseColor(value);
    	}else {
    		return false;
    	}
    	return true;
    }
   	
   	void size(float[] newSize){
   		if (newSize != null && newSize.length == 3){
	    	setFltAry("size", newSize);
	    	Appearance appearance = primitive.getAppearance();
	    	tg_.removeChild(bg_);
	    	bg_ = new BranchGroup();
	       	bg_.setCapability(BranchGroup.ALLOW_DETACH);
	       	bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	       	primitive = new Box(newSize[0]/2, newSize[1]/2, newSize[2]/2, primitiveFlag, appearance);
	       	bg_.addChild(primitive);
	       	tg_.addChild(bg_);
	    	resizeBoundingBox();
	    	if (model_ != null) model_.notifyModified();
	    	shapes_[0].primitiveParameters = newSize;
	    }  	
    }

    void size(String size){
    	float[] newSize = getFltAry(size);
    	size(newSize);
    }
    
    void coneSize(String property, String size){
    	float bottomRadius = ((Cone)primitive).getRadius();
    	float height = ((Cone)primitive).getHeight();
    	boolean side = isTrue("side");
    	boolean bottom = isTrue("bottom");
    	if(property.equals("bottomRadius")){
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		bottomRadius = newsize;
    	}else if(property.equals("height")){
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		height = newsize;
    	}else if(property.equals("side")){
    		boolean flg = size.equals("true");
    		setBool(property, flg);
    		side = flg;
    	}else if(property.equals("bottom")){
    		boolean flg = size.equals("true");
    		setBool(property, flg);
    		bottom = flg;
    	}
    	Appearance appearance = primitive.getAppearance();
	    tg_.removeChild(bg_);
	    bg_ = new BranchGroup();
	    bg_.setCapability(BranchGroup.ALLOW_DETACH);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	    primitive = new Cone( bottomRadius, height, primitiveFlag, appearance);   
	    if(!bottom)
	    	primitive.removeChild(primitive.getShape(Cone.CAP));
	    if(!side)
	    	primitive.removeChild(primitive.getShape(Cone.BODY));
	    bg_.addChild(primitive);
	    tg_.addChild(bg_);
	    resizeBoundingBox();
	    if (model_ != null) model_.notifyModified();
	    float[] param = new float[4];
	    param[0] = bottomRadius;
		param[1] = height;
		param[2] = bottom ? 1: 0;   
		param[3] = side? 1 : 0;	
		shapes_[0].primitiveParameters = param;
    }
    
    void cylinderSize(String property, String size){
    	float radius = ((Cylinder)primitive).getRadius();
    	float height = ((Cylinder)primitive).getHeight();
    	boolean side = isTrue("side");
    	boolean bottom = isTrue("bottom");
    	boolean top = isTrue("top");
    	if(property.equals("radius")){
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		radius = newsize;
    	}else if(property.equals("height")){
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		height = newsize;
    	}else if(property.equals("side")){
    		boolean flg = size.equals("true");
    		setBool(property, flg);
    		side = flg;
    	}else if(property.equals("bottom")){
    		boolean flg = size.equals("true");
    		setBool(property, flg);
    		bottom = flg;
    	}else if(property.equals("top")){
    		boolean flg = size.equals("true");
    		setBool(property, flg);
    		top = flg;
    	}
    	
    	Appearance appearance = primitive.getAppearance();
	    tg_.removeChild(bg_);
	    bg_ = new BranchGroup();
	    bg_.setCapability(BranchGroup.ALLOW_DETACH);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	    primitive = new Cylinder( radius, height, primitiveFlag, appearance);
	    bg_.addChild(primitive);
	    if(!bottom)
	    	primitive.removeChild(primitive.getShape(Cylinder.BOTTOM));
	    if(!top)
	    	primitive.removeChild(primitive.getShape(Cylinder.TOP));
	    if(!side)
	    	primitive.removeChild(primitive.getShape(Cylinder.BODY));
	    tg_.addChild(bg_);
	    resizeBoundingBox();
	    if (model_ != null) model_.notifyModified();
	    float[] param = new float[5];
	    param[0] = radius;
		param[1] = height;
		param[2] = top ? 1 : 0;   
		param[3] = bottom? 1 : 0;	
		param[4] = side? 1 : 0;
		shapes_[0].primitiveParameters = param;
    }
    
    void sphereSize(float[] newSize){
   		if (newSize != null && newSize.length == 1){
	    	setFlt("radius", newSize[0]);
	    	Appearance appearance = primitive.getAppearance();
	    	tg_.removeChild(bg_);
	    	bg_ = new BranchGroup();
	       	bg_.setCapability(BranchGroup.ALLOW_DETACH);
	       	bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	       	primitive = new Sphere(newSize[0], primitiveFlag, appearance);
	       	bg_.addChild(primitive);
	       	tg_.addChild(bg_);
	    	resizeBoundingBox();
	    	if (model_ != null) model_.notifyModified();
	    	shapes_[0].primitiveParameters = newSize;
	    }  	
    }

    void sphereSize(String size){
    	float[] newSize = getFltAry(size);
    	sphereSize(newSize);
    }
    
    void diffuseColor(float[] newValue){
    	if (newValue != null && newValue.length == 3){
    		Appearance appearance = primitive.getAppearance();
    	    setFltAry("diffuseColor", newValue);
    		materials_[0].diffuseColor = newValue;
    		setMaterial(appearance, materials_[0]);
    		if (model_ != null) model_.notifyModified();
    	}
    }
    
    void diffuseColor(String value){
    	float[] newValue = getFltAry(value);
    	diffuseColor(newValue);
    }
    
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
	
	    }catch(Exception ex){
	        	ex.printStackTrace();

	    }
	    tg_.setTransform(trorg);
    }
}
   	
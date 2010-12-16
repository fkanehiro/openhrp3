package com.generalrobotix.ui.item;

import javax.media.j3d.BadTransformException;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.LinkInfo;
import jp.go.aist.hrp.simulator.SegmentInfo;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.CalcInertiaUtil;
import com.generalrobotix.ui.util.GrxShapeUtil;

@SuppressWarnings("serial")
public class GrxSegmentItem extends GrxShapeTransformItem {
	public double mass_;
	public double[] centerOfMass_;
	public double[] momentOfInertia_;
	// display
    private Switch switchCom_;
    private TransformGroup tgCom_;
	
	public GrxSegmentItem(String name, GrxPluginManager manager_, GrxModelItem model_, LinkInfo linkInfo, SegmentInfo segmentInfo) {
		super(name, manager_, model_);
		setIcon("segment1.png");
		switchCom_ = GrxShapeUtil.createBall(0.01, new Color3f(1.0f, 0.5f, 0.25f), 0.5f);
	    tgCom_ = (TransformGroup)switchCom_.getChild(0);
	    tg_.addChild(switchCom_);
	    
	    if(segmentInfo != null){
			transform(segmentInfo.transformMatrix);
	        centerOfMass(segmentInfo.centerOfMass);  
	        momentOfInertia(segmentInfo.inertia);
	        mass(segmentInfo.mass);     
	        if(linkInfo != null){
		        int n = segmentInfo.shapeIndices.length;
		        for(int i=0; i<n; i++)
		        	addTransformedShapeIndex(linkInfo.shapeIndices[segmentInfo.shapeIndices[i]]);
		        buildShapeTransforms(linkInfo.inlinedShapeTransformMatrices);
	        }
	    }else{
	    	transform( new double[]{	1.0, 0.0, 0.0, 0.0, 
					  					0.0, 1.0, 0.0, 0.0,
					  					0.0, 0.0, 1.0, 0.0 });
	        centerOfMass(new double[]{0.0, 0.0, 0.0});  
	        momentOfInertia(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0});
	        mass(0.0);
	    }
        initMenu();
	}

	private void mass(double mass){
		mass_ = mass;
        setDbl("mass", mass);
        _updateScaleOfBall();
        if (model_ != null)		model_.notifyModified();
	}
	
	private void momentOfInertia(double[] inertia){
		if(inertia == null || inertia.length != 9) return;
		momentOfInertia_ = inertia;
		setDblAry("momentsOfInertia", inertia);
        _updateScaleOfBall();
        if (model_ != null)		model_.notifyModified();
	}
	
	private void centerOfMass(double[] centerOfMass){
		if (centerOfMass == null || centerOfMass.length != 3) return;
		centerOfMass_ = centerOfMass;
		setDblAry("centerOfMass", centerOfMass);
		Transform3D t3d = new Transform3D();
    	tgCom_.getTransform(t3d);
    	t3d.setTranslation(new Vector3d(centerOfMass_));
    	tgCom_.setTransform(t3d);
    	if (model_ != null)		model_.notifyModified();
	}
       
    public boolean propertyChanged(String property, String value) {
    	if (property.equals("name")){ //$NON-NLS-1$
			rename(value);
    	}else if(property.equals("translation")){ //$NON-NLS-1$
    		translation(value);
    		((GrxLinkItem)parent_).modifyCenterOfMass();
    	}else if(property.equals("rotation")){ //$NON-NLS-1$
    		rotation(value);
    		((GrxLinkItem)parent_).modifyCenterOfMass();
    	}else if(property.equals("centerOfMass")){ //$NON-NLS-1$
    		centerOfMass(getDblAry(value));
    		((GrxLinkItem)parent_).modifyCenterOfMass();
    	}else if(property.equals("momentsOfInertia")){ //$NON-NLS-1$
    		momentOfInertia(getDblAry(value));
    		((GrxLinkItem)parent_).modifyInertia();
        }else if(property.equals("mass")){ //$NON-NLS-1$
    		mass(getDbl(value));
    		((GrxLinkItem)parent_).modifyMass();
    	}else{
    		return false;
    	}
    	return true;
    }
 
    public void setFocused(boolean b){
    	if (b)
    		resizeBoundingBox();
    	super.setFocused(b);
    	switchCom_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
    }
    
    private void _updateScaleOfBall(){
		Matrix3d I = new Matrix3d(momentOfInertia_);
		Vector3d scale = CalcInertiaUtil.calcScale(I, mass_);
		Transform3D t3d = new Transform3D();
		tgCom_.getTransform(t3d);
		t3d.setScale(scale);
		try{
			tgCom_.setTransform(t3d);
		}catch(BadTransformException ex){
			System.out.println("BadTransformException in _updateScaleOfBall"); //$NON-NLS-1$
		}
    }
}

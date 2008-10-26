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


import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import jp.go.aist.hrp.simulator.AppearanceInfo;
import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ModelLoader;
import jp.go.aist.hrp.simulator.ModelLoaderHelper;
import jp.go.aist.hrp.simulator.SceneInfo;
import jp.go.aist.hrp.simulator.SensorInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.TextureInfo;
import jp.go.aist.hrp.simulator.TransformedShapeIndex;
import jp.go.aist.hrp.simulator.CameraPackage.CameraParameter;
import jp.go.aist.hrp.simulator.CameraPackage.CameraType;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

/**
 * @brief sensor
 */
@SuppressWarnings({ "serial", "unchecked" })
public class GrxSensorItem extends GrxTransformItem implements  Comparable {

	SensorInfo info_;
	public Camera_impl camera_;

    /**
     * @brief get type of sensor
     * @return type of sensor
     */
    public String type(){
    	return info_.type;
    }

    /**
     * @brief get id of sensor
     * @return id of sensor
     */
    public int id(){
    	return info_.id;
    }

    /**
     * @brief constructor
     * @param info SensorInfo retrieved through ModelLoader
     * @param parentLink link to which this sensor is attached
     */
    public GrxSensorItem(String name, GrxPluginManager manager, SensorInfo info) {
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
						"Are you sure to delete " + getName() + " ?") )
					delete();
			}
		};
		setMenuItem(item);

		/* disable copy and paste menus until they are implemented
        // menu item : copy
        item = new Action(){
            public String getText(){
                return "copy";
            }
            public void run(){
                GrxDebugUtil.println("GrxModelItem.GrxSensorItem copy Action");
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
				addShape(fPath);
			}
		};
        setMenuItem(item);

    	info_ = info;

    	setProperty("type", info_.type);
    	setProperty("id", String.valueOf(info_.id));
    	setDblAry("translation", info_.translation);
    	setDblAry("rotation", info_.rotation);

        setIcon("camera.png");

        updateTransformGroup();

        if (info.type.equals("Vision")) {
            CameraParameter prm = new CameraParameter();
            prm.defName = new String(info.name);
            prm.sensorName = new String(info.name);
            prm.sensorId = info.id;

            prm.frontClipDistance = (float)info.specValues[0];
            prm.backClipDistance = (float)info.specValues[1];
            prm.fieldOfView = (float)info.specValues[2];
            try {
                prm.type = CameraType.from_int((int)info.specValues[3]);
            } catch (Exception e) {
                prm.type = CameraType.NONE;
            }
            prm.width  = (int)info.specValues[4];
            prm.height = (int)info.specValues[5];
            prm.frameRate = (float)info.specValues[6];
            
            setDbl("frontClipDistance", prm.frontClipDistance);
            setDbl("backClipDistance",  prm.backClipDistance);
            setDbl("fieldOfView",       prm.fieldOfView);
            if (prm.type == CameraType.NONE){
            	setProperty("cameraType", "NONE");
            }else if (prm.type == CameraType.COLOR){
            	setProperty("cameraType", "COLOR");
            }else if (prm.type == CameraType.MONO){
            	setProperty("cameraType", "MONO");
            }else if (prm.type == CameraType.COLOR_DEPTH){
            	setProperty("cameraType", "COLOR_DEPTH");
            }else if (prm.type == CameraType.MONO_DEPTH){
            	setProperty("cameraType", "MONO_DEPTH");
            }else if (prm.type == CameraType.DEPTH){
            	setProperty("cameraType", "DEPTH");
            }
            setInt("width",             prm.width);
            setInt("height",            prm.height);
            setDbl("frameRate",         prm.frameRate);
            boolean offScreen = false;
            camera_ = new Camera_impl(prm, offScreen);

            tg_.addChild(camera_.getBranchGroup());
        }else if(info.type.equals("RateGyro")){
        	float[] max = new float[3];
        	max[0] = info.specValues[0];
        	max[1] = info.specValues[1];
        	max[2] = info.specValues[2];
        	setFltAry("maxAngularVelocity", max);
        }else if(info.type.equals("Acceleration")){
        	float[] max = new float[3];
        	max[0] = info.specValues[0];
        	max[1] = info.specValues[1];
        	max[2] = info.specValues[2];
        	setFltAry("maxAcceleration", max);
        }else if(info.type.equals("Force")){
        	float[] maxf = new float[3];
        	maxf[0] = info.specValues[0];
        	maxf[1] = info.specValues[1];
        	maxf[2] = info.specValues[2];
        	float[] maxt = new float[3];
        	maxt[0] = info.specValues[3];
        	maxt[1] = info.specValues[4];
        	maxt[2] = info.specValues[5];
        	setFltAry("maxForce", maxf);
        	setFltAry("maxTorque", maxt);
        }

    }

    public void updateTransformGroup(){
        Transform3D t3d = new Transform3D();
        t3d.setTranslation(new Vector3d(info_.translation));
        t3d.setRotation(new AxisAngle4d(info_.rotation));
        tg_.setTransform(t3d);
    }

    public int compareTo(Object o) {
        if (o instanceof GrxSensorItem) {
            GrxSensorItem s = (GrxSensorItem) o;
            if (getOrder(type()) < getOrder(s.type()))
                return -1;
            else{
                if (id() < s.id())
                    return -1;
            }
        }
        return 1;
    }

    /**
     * @brief get sensor type as integer value
     * @param type sensor type
     * @return
     */
    private int getOrder(String type) {
        if (type.equals("Force"))
            return 0;
        else if (type.equals("RateGyro"))
            return 1;
        else if (type.equals("Acceleration"))
            return 2;
        else if (type.equals("Vision"))
            return 3;
        else
            return -1;

    }

    /**
     * @brief get translation relative to the parent link
     * @return translation
     */
	public double[] translation() {
		return info_.translation;
	}

    /**
     * @brief get rotation relative to the parent link
     * @return rotation
     */
	public double[] rotation() {
		return info_.rotation;
	}

    /**
     * @brief properties are set to robot
     */
    public void propertyChanged() {
    	System.out.println("GrxSensorItem.propertyChanged()");
    	super.propertyChanged();
    	info_.translation = getDblAry("translation",null);
    	info_.rotation = getDblAry("rotation", null);
    	updateTransformGroup();
    	info_.id = getShort("id", null);
    	info_.type = getStr("type", null);
    	if (info_.type.equals("Vision")){
    		if (info_.specValues == null || info_.specValues.length != 7){
    			info_.specValues = new float[7];
    			_removeSensorSpecificProperties();
    			setProperty("frontClipDistance", "0.01");
    			setProperty("backClipDistance", "10.0");
    			setProperty("fieldOfView", "0.0785398");
    			setProperty("cameraType", "COLOR");
    			setProperty("width", "320");
    			setProperty("height", "240");
    			setProperty("frameRate", "30.0");
    		}
			info_.specValues[0] = getFlt("frontClipDistance", null);
			info_.specValues[1] = getFlt("backClipDistance", null);
			info_.specValues[2] = getFlt("fieldOfView", null);
			if (getStr("cameraType", "NONE").equals("NONE")){
				info_.specValues[3] = CameraType._NONE;
			}else if(getStr("cameraType", "NONE").equals("COLOR")){
				info_.specValues[3] = CameraType._COLOR;
			}else if(getStr("cameraType", "NONE").equals("MONO")){
				info_.specValues[3] = CameraType._MONO;
			}else if(getStr("cameraType", "NONE").equals("DEPTH")){
				info_.specValues[3] = CameraType._DEPTH;
			}else if(getStr("cameraType", "NONE").equals("COLOR_DEPTH")){
				info_.specValues[3] = CameraType._COLOR_DEPTH;
			}else if(getStr("cameraType", "NONE").equals("MONO_DEPTH")){
				info_.specValues[3] = CameraType._MONO_DEPTH;
			}else{
				setProperty("cameraType", "NONE");
			}
			info_.specValues[4] = getInt("width", null);
			info_.specValues[5] = getInt("height", null);
			info_.specValues[6] = getFlt("frameRate", null);
    	}else if(info_.type.equals("RateGyro")){
    		if (info_.specValues == null || info_.specValues.length != 3 || getProperty("maxAngularVelocity")==null){
    			info_.specValues = new float[]{-1.0f, -1.0f, -1.0f};
    			_removeSensorSpecificProperties();
    			setFltAry("maxAngularVelocity", info_.specValues);
    		}
    		info_.specValues = getFltAry("maxAngularVelocity", null);
    	}else if(info_.type.equals("Acceleration")){
    		if (info_.specValues == null || info_.specValues.length != 3 || getProperty("maxAcceleration")==null){
    			info_.specValues = new float[]{-1.0f, -1.0f, -1.0f};
    			_removeSensorSpecificProperties();
    			setFltAry("maxAcceleration", info_.specValues);
    		}
    		info_.specValues = getFltAry("maxAcceleration", null);
    	}else if(info_.type.equals("Force")){
    		if (info_.specValues == null || info_.specValues.length != 6){
    			info_.specValues = new float[]{-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f};
    			_removeSensorSpecificProperties();
    			setFltAry("maxForce", new float[]{-1.0f, -1.0f, -1.0f});
    			setFltAry("maxTorque", new float[]{-1.0f, -1.0f, -1.0f});
    		}
    		float[] maxf = getFltAry("maxForce", null);
    		float[] maxt = getFltAry("maxTorque", null);
    		info_.specValues[0] = maxf[0];
    		info_.specValues[1] = maxf[1];
    		info_.specValues[2] = maxf[2];
    		info_.specValues[3] = maxt[0];
    		info_.specValues[4] = maxt[1];
    		info_.specValues[5] = maxt[2];
    	}else{
    		System.out.println("GrxSensorItem.propertyChanged() : unknown sensor type : "+info_.type);
    	}
    }
    
    private void _removeSensorSpecificProperties() {
		// properties of ForceSensor
		remove("maxForce");
		remove("maxTorque");
		// property of Gyro
		remove("maxAngularVelocity");
		// property of AccelerationSensor
		remove("maxAcceleration");
		// property of VisionSensor
		remove("frontClipDistance");
		remove("backClipDistance");
		remove("fieldOfView");
		remove("width");
		remove("height");
		remove("frameRate");
		remove("cameraType");
	}

	/**
     * @brief Override clone method
     * @return GrxSensorItem
     */
	public GrxSensorItem clone(){
		GrxSensorItem ret = (GrxSensorItem)super.clone();
/*    	
	Deep copy suspension list
*/
		
		return ret;
	}
    
}

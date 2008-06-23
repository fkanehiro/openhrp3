package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

import jp.go.aist.hrp.simulator.CameraPackage.CameraType;

/**
 * VisionSensorInfoFactory.java class
 * VisionSensorÍÑ¡£
 * @author F.Kanehiro
 * @version 1.0 (2004/03/25)
 */
public class VisionSensorInfoFactory
    extends SensorInfoFactory
{
    public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene)
    {
        info.maxValue_ = new double[6];
        info.maxValue_[0] = ProtoFieldGettor.getDoubleValue(defName, scene, "frontClipDistance", 0.01);
        info.maxValue_[1] = ProtoFieldGettor.getDoubleValue(defName, scene, "backClipDistance", 10.0);
        info.maxValue_[2] = ProtoFieldGettor.getDoubleValue(defName, scene, "fieldOfView", 0.785398);

	   	String type = ProtoFieldGettor.getStringValue(defName, scene, "type", "NONE");
		if (type.equals("NONE")) 
			info.maxValue_[3] = CameraType._NONE;
		else if (type.equals("COLOR")) 
			info.maxValue_[3] = CameraType._COLOR;
		else if (type.equals("MONO")) 
			info.maxValue_[3] = CameraType._MONO;
		else if (type.equals("DEPTH")) 
			info.maxValue_[3] = CameraType._DEPTH;
		else if (type.equals("COLOR_DEPTH")) 
			info.maxValue_[3] = CameraType._COLOR_DEPTH;
		else if (type.equals("MONO_DEPTH")) 
			info.maxValue_[3] = CameraType._MONO_DEPTH;

        info.maxValue_[4] = (double)ProtoFieldGettor.getIntValue(defName, scene, "width",  320);
        info.maxValue_[5] = (double)ProtoFieldGettor.getIntValue(defName, scene, "height", 240);
    }

    public SensorType getType(){
        return SensorType.VISION_SENSOR;
    }
}

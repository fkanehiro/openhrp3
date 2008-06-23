package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * AccererationSensorInfoFactoryclass
 * AccererationSensorÍÑ¡£
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/02/25)
 */
public class AccelerationSensorInfoFactory
    extends SensorInfoFactory
{
    public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene)
    {
        info.maxValue_ = new double[3];
        
        double[] def = {-1,-1,-1};
        double[] temp;
        
        temp = ProtoFieldGettor.getVectorValue(defName, scene, "maxAcceleration",def );
        
        info.maxValue_[0] = temp[0];
        info.maxValue_[1] = temp[1];
        info.maxValue_[2] = temp[2];
        
    }
    public SensorType getType(){
        return SensorType.ACCELERATION_SENSOR;
    }
}

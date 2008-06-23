package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * GyroSensorInfoFactory.java class
 * GyroSensorÍÑ¡£
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/02/25)
 */
public class GyroSensorInfoFactory
    extends SensorInfoFactory
{
    public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene)
    {
        info.maxValue_ = new double[3];
        
        double[] def = {-1,-1,-1};
        double[] temp;
        
        temp = ProtoFieldGettor.getVectorValue(defName, scene, "maxAngularVelocity",def );
        
        info.maxValue_[0] = (double)temp[0];
        info.maxValue_[1] = (double)temp[1];
        info.maxValue_[2] = (double)temp[2];
        
    }
    public SensorType getType(){
        return SensorType.RATE_GYRO;
    }
}

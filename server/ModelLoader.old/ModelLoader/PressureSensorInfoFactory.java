package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * PressureSensorInfoFactory
 * PressureSensorÍÑ¡£
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/02/25)
 */
public class PressureSensorInfoFactory
    extends SensorInfoFactory
{
    public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene)
    {
        info.maxValue_ = new double[1];
        
        double temp;
        
        temp = ProtoFieldGettor.getDoubleValue(defName, scene, "maxPressure",-1);
        
        info.maxValue_[0] = (double)temp;
        
    }
    public SensorType getType(){
        return SensorType.PRESSURE_SENSOR;
    }
}

package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * TorqueSensorInfoFactory class
 * TorqueSensorÍÑ¡£
 */
public class TorqueSensorInfoFactory
    extends SensorInfoFactory
{
    public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene)
    {
    }
    public SensorType getType(){
        return SensorType.TORQUE_SENSOR;
    }
}

package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * PhotoInterrupterSensorInfoFactory
 * PhotoInterrupterÍÑ¡£
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/02/25)
 */
public class PhotoInterrupterSensorInfoFactory
    extends SensorInfoFactory
{
    public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene)
    {
	double[] def = {0,0,0};
	
	info.translation_ = ProtoFieldGettor.getVectorValue(defName, scene, "transmitter", def );
	info.rotation_ = ProtoFieldGettor.getVectorValue(defName, scene, "receiver", def );

	/*
	  double[] rp = {0,0,0};
	  double[] ra = {0,0,0,0,0,0,0,0,0};
	  
	  rp = ProtoFieldGettor.getVectorValue(defName, scene, "transmitter", def );
	  ra = ProtoFieldGettor.getVectorValue(defName, scene, "receiver", def );
	  
	  Quat4d quat4d = new Quat4d();
	  quat4d.set(ra);
	  
	  info.relPosAtt_.px = rp[0];
	  info.relPosAtt_.py = rp[1];
	  info.relPosAtt_.pz = rp[2];
	  
	  info.relPosAtt_.qx = quat4d.x;
	  info.relPosAtt_.qy = quat4d.y;
	  info.relPosAtt_.qz = quat4d.z;
	  info.relPosAtt_.qw = quat4d.w;
	*/
	}
    public SensorType getType(){
        return SensorType.PHOTO_INTERRUPTER;
    }
}

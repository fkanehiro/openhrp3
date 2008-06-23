package jp.go.aist.hrp.simulator;

import javax.vecmath.*;
// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * SensorInfoFactory class
 * SensorInfoを生成するクラス。
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/02/25)
 */
public abstract class SensorInfoFactory
{
    //デフォルト値
    private double[] relPos_ = {0,0,0};
    
    public SensorInfo_impl createSensorInfo(String defName, VrmlSceneEx scene){
        SensorInfo_impl info = new SensorInfo_impl();
        info.name_ = defName;
        
        info.type_ = getType();
        
        info.id_ = ProtoFieldGettor.getIntValue(defName,scene,"sensorId",-1);
        
        //set translation value.
        info.translation_ = ProtoFieldGettor.getVectorValue(defName, scene, "translation",relPos_);
        
        //set rotation value.
        double[] defAxis = {0,0,1,0};
        AxisAngle4d axisangle4d = new AxisAngle4d(ProtoFieldGettor.getRotationValue(defName, scene, "rotation",defAxis));

	Matrix4d matrix4d = new Matrix4d();
        matrix4d.set(axisangle4d);
	
        info.rotation_[0] = matrix4d.m00;
        info.rotation_[1] = matrix4d.m01;
        info.rotation_[2] = matrix4d.m02;
        info.rotation_[3] = matrix4d.m10;
        info.rotation_[4] = matrix4d.m11;
	info.rotation_[5] = matrix4d.m12;
        info.rotation_[6] = matrix4d.m20;
        info.rotation_[7] = matrix4d.m21;
        info.rotation_[8] = matrix4d.m22;

	/*
        Quat4d quat4d = new Quat4d();
        quat4d.set(axisangle4d);
        info.relPosAtt_.px = info.translation_[0];
	info.relPosAtt_.py = info.translation_[1];
	info.relPosAtt_.pz = info.translation_[2];
	info.relPosAtt_.qx = quat4d.x;
	info.relPosAtt_.qy = quat4d.y;
	info.relPosAtt_.qz = quat4d.z;
	info.relPosAtt_.qw = quat4d.w;
	*/
        
        //set each value.
        setParam(info,defName,scene);
        
        return info;
    }
    
    abstract public void setParam(SensorInfo_impl info,String defName, VrmlSceneEx scene);
    abstract public SensorType getType();
}

/**
 * JointRecover.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.tdview;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.*;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.*;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxModelItem.LinkInfoLocal;

/**
 * InvKinemaResolver
 *
 */
public class InvKinemaResolver {
    private DynamicsSimulator integrator_;
    private GrxPluginManager manager_;
    private GrxModelItem robot_;
    private LinkInfoLocal from_;
    private LinkInfoLocal to_;
    private Transform3D trFrom_;
    private LinkPosition tr;

    /**
     * コンストラクタ
     *
     * 設定に必要な情報をこのクラスに格納する
     * @param   integrator
     * @param   world
     */
    public InvKinemaResolver(GrxPluginManager manager) {
        manager_ = manager;
        trFrom_ = new Transform3D();
        tr = new LinkPosition();
        tr.p = new double[3];
        tr.R = new double[9];
    }

    public void setDynamicsSimulator(DynamicsSimulator integrator) {
        integrator_ = integrator;
    }

    /**
     * setFromJoint
     *
     * 移動原点になる Joint の名前を格納する
     * @param   objectName
     * @param   jointName
     */
    public boolean setFromJoint(String objectName,String jointName) {
        if (robot_ == null) {
            robot_ = (GrxModelItem)manager_.getItem(GrxModelItem.class, objectName);
        } else if(!robot_.getName().equals(objectName)) {
            to_ = null;
            robot_ = (GrxModelItem)manager_.getItem(GrxModelItem.class, objectName);
        }
        
        from_ = (LinkInfoLocal)robot_.getLinkInfo(jointName);
        
        // fromジョイントのグローバル座標での位置姿勢を保持
        TransformGroup tg = from_.tg;
        Transform3D tr = new Transform3D();
        tg.getTransform(tr);
        tg.getLocalToVworld(trFrom_);
        trFrom_.mul(tr);
        //tg.getTransform(trFrom_);

        return true;
    }

    /**
     * setToJoint
     *
     * 実際にマウスの動きに合わせて動く Joint の名前を格納する
     * @param   objectName
     * @param   jointName
     */
    public boolean setToJoint(String objectName,String jointName) {
        if (robot_ == null) {
            robot_ = (GrxModelItem)manager_.getItem(GrxModelItem.class, objectName);
        } else if(!robot_.getName().equals(objectName)) {
            return false;
        }
        
        to_ = (LinkInfoLocal)robot_.getLinkInfo(jointName);
        
        return true;
    }

    /**
     * setLinkStatus
     *
     * Debugようにこのメソッドを叩くと現在のRobotの状態をIntegratorに通知する
     * @param   objectName
     */
    private void _setLinkStatus(String objectName) {
        if(robot_ == null) 
            robot_ = (GrxModelItem)manager_.getItem(GrxModelItem.class, objectName);
        
        TransformGroup tg = robot_.getTransformGroupRoot();
        Transform3D t3d = new Transform3D();
        tg.getTransform(t3d);
        Vector3d pos = new Vector3d();
        Matrix3d mat = new Matrix3d();
        t3d.get(mat, pos);
        
        double[] value = new double[12];
        pos.get(value);
        value[3] = mat.m00;
        value[4] = mat.m01;
        value[5] = mat.m02;
        value[6] = mat.m10;
        value[7] = mat.m11;
        value[8] = mat.m12;
        value[9] = mat.m20;
        value[10]= mat.m21;
        value[11]= mat.m22;

        integrator_.setCharacterLinkData(
            objectName, robot_.lInfo_[0].name, LinkDataType.ABS_TRANSFORM, value
        );
        integrator_.setCharacterAllLinkData(
            objectName, LinkDataType.JOINT_VALUE, robot_.getJointValues()
        );
        integrator_.calcCharacterForwardKinematics(objectName);
    }

    /**
     * resolve
     *
     *    マウスの動きから生成された T3D を逆運動学サーバを使って
     *    各ジョイントの動きに直し設定する
     *  @param   transform
     *  @return
     */
    public boolean resolve(Transform3D transform) {
        _setLinkStatus(robot_.getName());
        
        Matrix3d m3d = new Matrix3d();
        Vector3d v3d = new Vector3d();
        transform.get(m3d, v3d);
        
        v3d.get(tr.p);
        for (int i=0; i<3; i++) {
        	for (int j=0; j<3; j++) {
        		tr.R[3*i+j] = m3d.getElement(i,j);
        	}
        }
        try {
			if (robot_ == null || from_ == null || to_ == null)
				return false;

        	if (!integrator_.calcCharacterInverseKinematics(robot_.getName(), from_.name, to_.name, tr)) {
        		System.out.println("ik failed.");
        		robot_.calcForwardKinematics();
            	return false;
        	};
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        DblSequenceHolder v = new DblSequenceHolder();
        integrator_.getCharacterAllLinkData(robot_.getName(), LinkDataType.JOINT_VALUE, v);
        robot_.setJointValues(v.value);
        robot_.setJointValuesWithinLimit();
        robot_.updateInitialJointValues();
        _setRootJoint(robot_);
        robot_.calcForwardKinematics();
        
        return true;
    }
    
    private void _setRootJoint(GrxModelItem robot) {
        Transform3D t3d = new Transform3D();
        from_.tg.getTransform(t3d);
        
        Transform3D t3dFromNew = new Transform3D();
        from_.tg.getLocalToVworld(t3dFromNew);
        t3dFromNew.mul(t3d);
        t3dFromNew.invert();
        
        Transform3D t3dRoot = new Transform3D();
        robot.getTransformGroupRoot().getTransform(t3dRoot);
        t3d.mul(trFrom_, t3dFromNew);
        t3d.mul(t3dRoot);
        
        Matrix3d rot = new Matrix3d();
        Vector3d pos = new Vector3d();
        t3d.get(rot, pos);
        robot.setTransformRoot(pos, rot);
        robot.updateInitialTransformRoot();
       /* 
        AxisAngle4d  a4d = new AxisAngle4d();
        a4d.set(rot);
        robot.setProperty(robot_.lInfo_[0].name+".rotation", +a4d.angle+" "+a4d.x+" "+a4d.y+" "+a4d.z);
        robot.setProperty(robot_.lInfo_[0].name+".translation", pos.x+" "+pos.y+" "+pos.z);*/
    }
}

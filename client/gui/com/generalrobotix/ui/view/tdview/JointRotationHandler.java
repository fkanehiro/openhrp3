/**
 * JointRotationHandler.java
 *
 * @author  Kernel, Inc.
 * @version  (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import javax.vecmath.*;


import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxModelItem.LinkInfoLocal;
import com.sun.j3d.utils.picking.*;

class JointRotationHandler extends OperationHandler {
    //--------------------------------------------------------------------
    // 定数
    private static final int MODE_NONE     = 0;
    private static final int DISK_MODE     = 1;
    private static final int CYLINDER_MODE = 2;
    private static final float FACTOR = 0.004f;

    // 物体のVectorと視点のVectorがこの角度以下の時CYLINDER_MODEに移行する
    private static final double THRESHOLD = 0.262f;

    //--------------------------------------------------------------------
    // インスタンス変数
    private int mode_;
    private TransformGroup tgTarget_;
    private Switch bbSwitch_;
    private Switch axisSwitch_;
    private Point prevPoint_ = new Point();
    private double angle_;
    private boolean isPicked_;
    private Vector3d vectorCylinder_;

    // DISK モードの時に使用する
    //protected Point3f point000 = new Point3f(0,0,0);
    private Point3d pointTarget_;

    //--------------------------------------------------------------------
    // BehaviorHandlerの実装
    public void processPicking(MouseEvent evt, BehaviorInfo info) {
        prevPoint_.x = evt.getPoint().x;
        prevPoint_.y = evt.getPoint().y;

        isPicked_ = false;

        try {
            info.pickCanvas.setShapeLocation(prevPoint_.x, prevPoint_.y);
            PickResult pickResult = info.pickCanvas.pickClosest();
            if (pickResult == null) {
                //_disableBoundingBox();
                return;
            }
            TransformGroup tg = (TransformGroup)pickResult.getNode(
                PickResult.TRANSFORM_GROUP
            );
            if (tg == null) {
                //_disableBoundingBox();
                return;
            }

            if (tg != tgTarget_) {
                if (!_enableBoundingBox(tg, info)) {
                    return;
                }
            }
           // Point3d startPoint = info.pickCanvas.getStartPosition();
           // PickIntersection intersection =
           //     pickResult.getClosestIntersection(startPoint);
        } catch (CapabilityNotSetException ex) {
            // もう出ることはないと思うが、読み込むモデルによっては
            // 出るかもしれないので、スタックトレースは表示する。
            ex.printStackTrace();
            _disableBoundingBox();
        }

        isPicked_ = true;
        //evt.consume();
    }

    public void processStartDrag(MouseEvent evt, BehaviorInfo info) {
        if (bbSwitch_ == null || !isPicked_) {
            mode_ = MODE_NONE;
            return;
        }
        //Press されたら視点と物体の角度を取得し現在のモードを判定する
         // ターゲット座標系からワールド座標系への変換
        Transform3D target2vw = new Transform3D();
        Transform3D l2vw = new Transform3D();
        Transform3D tr = new Transform3D();
        tgTarget_.getLocalToVworld(l2vw);
        tgTarget_.getTransform(tr);
        target2vw.mul(l2vw, tr);

        // ターゲットの原点のワールド座標系での座標を求める。
        pointTarget_ = new Point3d();
        target2vw.transform(pointTarget_);

        // ワールド座標系から視点座標系への変換
        Transform3D vw2view = new Transform3D();
        tr = new Transform3D();
        l2vw = new Transform3D();
        TransformGroup tgView = info.drawable.getTransformGroupRoot();
        tgView.getLocalToVworld(l2vw);
        tgView.getTransform(tr);
        vw2view.mul(l2vw, tr);
        vw2view.invert();
        
        // (0,0,0) (0,0,1) の point を作成し、上で作成した
        // vw2view target2vw を使って変換する
        Point3f point000 = new Point3f(0,0,0);
        //軸情報取り出す

        Hashtable ht = (Hashtable)tgTarget_.getUserData();
        LinkInfoLocal l = (LinkInfoLocal)ht.get("linkInfo");
  
        vw2view.mul(target2vw);
        vw2view.transform(point000);
        Vector3d vectorView = new Vector3d(point000);
        vectorCylinder_ = new Vector3d(l.jointAxis);
        vw2view.transform(vectorCylinder_);
        
        // 二つの Vector で angle の角度を得て diskAngle と
        // 比較しモードを設定
        double angle = vectorView.angle(vectorCylinder_);
        
        if(angle == Double.NaN) {
            System.err.println("無効な値が入りました");
        }
        // 角度が 90 以上の時のことも考えて if で分ける
        if (angle > Math.PI / 2.0) {
            // 両端に DISK_MODE の状態になる範囲があるので大きい
            // ほうの範囲を小さいほうへ移動する計算
            angle = Math.PI - angle;
        }
        
        if (angle < THRESHOLD) {
            mode_ = DISK_MODE;
        } else {
            mode_ = CYLINDER_MODE;
        }
    }

    public void processDragOperation(MouseEvent evt, BehaviorInfo info) {
//        if (bbSwitch_ == null) {
//            return;
//        }

        Vector2d mouseMove = new Vector2d(
            FACTOR * (evt.getPoint().getX() - prevPoint_.getX()),
            FACTOR * (evt.getPoint().getY() - prevPoint_.getY())
        );

        angle_ = 0.0;  // 移動量を定義するためのラジアン
        // 現在のモードに応じてマウスの動きを回転に直す
        switch (mode_) {
        case DISK_MODE:
            Point2d pointMouseOnPlane = new Point2d();
            //Point3d pointTemp = new Point3d(point000);
            Point3d pointTemp = new Point3d(pointTarget_);

            Canvas3D canvas = info.pickCanvas.getCanvas();
            Transform3D vw2imagePlate = new Transform3D();
            canvas.getVworldToImagePlate(vw2imagePlate);
            vw2imagePlate.transform(pointTemp);
            canvas.getPixelLocationFromImagePlate(pointTemp, pointMouseOnPlane);

            Vector2d prev = new Vector2d(
                prevPoint_.getX() - pointMouseOnPlane.x,
                prevPoint_.getY() - pointMouseOnPlane.y
            );

            Vector2d current = new Vector2d(
                evt.getPoint().getX() - pointMouseOnPlane.x,
                evt.getPoint().getY() - pointMouseOnPlane.y
            );

            // 画面上の座標は下に行くほど y の値が逆転していくので
            // y の値を逆転させておく。
            angle_ = prev.angle(current);

            Vector3d cross = new Vector3d();
            cross.cross(vectorCylinder_, new Vector3d(prev.x, prev.y, 0.0));

            if (mouseMove.dot(new Vector2d(cross.x, cross.y)) > 0.0) {
                angle_ = - angle_;
            }
            // 画面のどちらが我がシリンダーの表面を向いているかで
            // 動作が反転する
/*
            if (vectorCylinder_.z < 0) {
                angle_ = - angle_;
            }
*/
            break;
        case CYLINDER_MODE:
            // x,y 平面において軸方向と直行する方向のマウスの
            // 動作だけをシリンダーの回転にする
            // fDotProduct <- 内積 InnerProduct ともいう
            // 半時計回りに 90 度回転するので 4x4 の変換行列を
            // かましてやった値をつかって直行するベクトルを出す
            // マウスに対する回転量を DISK_MODE の二倍にする
            mouseMove.x *= -2.0;
            mouseMove.y *= 2.0;

            //vectorCylinder_.normalize();
            Vector2d vectorCylinder =
                new Vector2d(- vectorCylinder_.y, vectorCylinder_.x);

            // このベクトルと直行する Vector を求める
            vectorCylinder.normalize();
            angle_ = vectorCylinder.dot(mouseMove);
            break;
        case MODE_NONE:
            return;
        }

        prevPoint_.x = evt.getPoint().x;
        prevPoint_.y = evt.getPoint().y;
        _jointAngleChanged(info);
        
        evt.consume();
    }

    public void processReleased(MouseEvent evt, BehaviorInfo info) {
//        if (bbSwitch_ != null) {
            evt.consume();
//        }
    }

    public void processTimerOperation(BehaviorInfo info) {}

    //--------------------------------------------------------------------
    // OperationHandlerの実装
    public void disableHandler() {
        _disableBoundingBox();
    }

    public void setPickTarget(TransformGroup tg, BehaviorInfo info) {
        if (tg != tgTarget_) {
            _enableBoundingBox(tg, info);
        }
    }

    //--------------------------------------------------------------------
    // プライベートメソッド
    private void _disableBoundingBox() {
        if (bbSwitch_ != null) {
            bbSwitch_.setWhichChild(Switch.CHILD_NONE);
            axisSwitch_.setWhichChild(Switch.CHILD_NONE);
            tgTarget_ = null;
            bbSwitch_ = null;
            axisSwitch_ = null;
        }
    }

    private boolean _enableBoundingBox(TransformGroup tg, BehaviorInfo info) {
        Hashtable ht = SceneGraphModifier.getHashtableFromTG(tg);
        String objectName = (String)ht.get("objectName");
        GrxModelItem robot = (GrxModelItem)info.getManipulatable(objectName);
        LinkInfoLocal l = (LinkInfoLocal)ht.get("linkInfo");
        if (l == null)
        	return false;
        
        if (l.jointType.equals("rotate") || l.jointType.equals("slide")) {
            _disableBoundingBox();
            robot.activeLinkInfo_ = l;
            tgTarget_ = l.tg;
            bbSwitch_ = (Switch)ht.get("boundingBoxSwitch");
            axisSwitch_ = (Switch)ht.get("axisLineSwitch");
            bbSwitch_.setWhichChild(Switch.CHILD_ALL);
            axisSwitch_.setWhichChild(Switch.CHILD_ALL);
            return true;
        }
        return false;               	
    }

    private void _jointAngleChanged(BehaviorInfo info) {
        try {
            Hashtable ht = SceneGraphModifier.getHashtableFromTG(tgTarget_);
            GrxModelItem model = (GrxModelItem)ht.get("object");
            String jname = (String)ht.get("controllableJoint");
            double a = model.getJointValue(jname) +angle_;
            model.setJointValue(jname, a);
            model.setJointValuesWithinLimit();
            model.updateInitialJointValue(jname);
            model.calcForwardKinematics();
        } catch (Exception e) { 
        	e.printStackTrace();
        }
    }
}

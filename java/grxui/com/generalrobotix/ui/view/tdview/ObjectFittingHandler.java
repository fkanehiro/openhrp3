/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * ObjectFittingHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import javax.vecmath.*;


import com.generalrobotix.ui.item.GrxModelItem;
import com.sun.j3d.utils.picking.*;

class ObjectFittingHandler extends OperationHandler {
    //--------------------------------------------------------------------
    // 定数
    public static final int FITTING_FROM  = 1;
    public static final int FITTING_TO   = 2;

    //--------------------------------------------------------------------
    // インスタンス変数
    private FittingInfo fittingInfoFrom_;
    private FittingInfo fittingInfoTo_;

    private int mode_;
    private boolean bHaveFirst = false;
    private boolean bHaveSecond = false;
    private Point3f intersectVW_;
    private Point3d[] verticesVW_;

    //--------------------------------------------------------------------
    // コンストラクタ
    public ObjectFittingHandler () {
        fittingInfoFrom_ = new FittingInfo(
            new Color3f(1.0f,0.0f,0.0f),
            new Color3f(0.0f,1.0f,0.0f),
            false
        );

        fittingInfoTo_ = new FittingInfo(
            new Color3f(0.0f,0.0f,1.0f),
            new Color3f(0.0f,1.0f,0.0f),
            true
        );
    }

    //--------------------------------------------------------------------
    // 公開メソッド
    void setFittingMode(int mode) {
        mode_ = mode;
    }

    /**
     *  二つのオブジェクトを結合します
     */
    public boolean fit(BehaviorInfo info) {
        if (!bHaveSecond) {
            System.err.println("二番目の TG が選択されていません");
            return false;
        }
        if (!bHaveFirst) {
            System.err.println("一番目の TG が選択されていません");
            return false;
        }

        // すでに二つの物体が選択されていたらそれをくっつける

        // 二つのベクトルの Angle を求め、二つのベクトルの外積方向を
        // 軸にその angle 回転させる
        // その情報を t3dRotate へ入れる
        Vector3f v3fCross = new Vector3f();
        Transform3D t3dRotate = new Transform3D();
        Vector3f v3fNormal = fittingInfoFrom_.getNormalVector();
        Vector3f v3fNormalSecond = fittingInfoTo_.getNormalVector();
        v3fCross.cross(v3fNormal,v3fNormalSecond);
        // 同方向かどうかのテスト部分
        if (v3fNormal.angle(v3fNormalSecond) == 0.0f) {
            // 角度が 0 度の時の対策。もし要素のすべての符号が
            // いっしょなら同じ方向のベクトルだとみなす
            if (_isSameSignVector3f(v3fNormal, v3fNormalSecond)) {
                // v3fCross に外積が入らないので AxisAngle は使えない
                Vector3f vecXAxis =
                    new Vector3f(new Point3f(1.0f,0.0f,0.0f));
                Vector3f vecYAxis =
                    new Vector3f(new Point3f(0.0f,1.0f,0.0f));
                if (v3fNormal.x != -1.0d) {
                    v3fCross.cross(vecXAxis,v3fNormal);
                    t3dRotate.set(new AxisAngle4f(v3fCross,(float)Math.PI));
                } else {
                    v3fCross.cross(vecYAxis,v3fNormal);
                    t3dRotate.set(new AxisAngle4f(v3fCross,(float)Math.PI));
                }
            }
        } else {
            t3dRotate.set(
                new AxisAngle4f(
                    v3fCross,
                    (float)Math.PI + v3fNormal.angle(v3fNormalSecond)
                )
            );
        }

        // 現在の Transform3D を生成し t3dCur へ入れる
        Transform3D t3dCur = new Transform3D();
        TransformGroup tgCur = new TransformGroup();
        Hashtable hashTable =
            SceneGraphModifier.getHashtableFromTG(
                fittingInfoFrom_.getTransformGroup()
            );

        String strObjectName = (String)hashTable.get("objectName");
        Manipulatable element = info.getManipulatable(strObjectName);
        if (element == null) return false;
        tgCur = element.getTransformGroupRoot();
        tgCur.getTransform(t3dCur);

        // 移動させる側の法線のスタートポイントへ回転処理をかけた場合の
        // 回転後の位置を p3dIntersect へ入れる
        Point3f p3dIntersect = fittingInfoFrom_.getIntersectPoint();
        Point3f p3dIntersectSecond = fittingInfoTo_.getIntersectPoint();
        t3dRotate.transform(p3dIntersect);
        // 移動させる処理
        Point3d p3dMove = new Point3d();
        p3dMove.x = p3dIntersectSecond.x - p3dIntersect.x;
        p3dMove.y = p3dIntersectSecond.y - p3dIntersect.y;
        p3dMove.z = p3dIntersectSecond.z - p3dIntersect.z;
        Vector3f v3fTranslate = new Vector3f(p3dMove);
        Transform3D t3dTranslate = new Transform3D();

        // Report された TG のぶんまでの LocalToVworld ぶんを逆変換する
        Transform3D t3dLocalToVworld = new Transform3D();
        Transform3D t3dCurrent = new Transform3D();
        tgCur.getLocalToVworld(t3dLocalToVworld);
        tgCur.getTransform(t3dCurrent);

        t3dLocalToVworld.mul(t3dCurrent);
        t3dLocalToVworld.invert();

        t3dTranslate.set(v3fTranslate);

        // Behavior に変更を任せる
        // 実際に Transform3D を multiply していく
        t3dRotate.mul(t3dCur);
        t3dTranslate.mul(t3dRotate);

        AxisAngle4f axis = new AxisAngle4f();
        Quat4f quat = new Quat4f();
        t3dTranslate.get(quat);
        t3dTranslate.get(v3fTranslate);
        axis.set(quat);

        tgCur.setTransform(t3dTranslate);

        // 別クラスに出す場合は broadcast する
        _transformChanged(info, tgCur);
        // この Behavior で TG を変更する場合

        // 処理が終わったら二つの表示用の線分を見えなくする
        fittingInfoFrom_.removeForDisplay();
        fittingInfoTo_.removeForDisplay();

        // くっつける処理が終わったら元の何も選択されていない状態に戻す
        bHaveFirst = false;
        bHaveSecond = false;
        fittingInfoFrom_.setTransformGroup(null);
        fittingInfoTo_.setTransformGroup(null);
        return true;
    }
 
    //--------------------------------------------------------------------
    // BehaviorHandlerの実装
    public void processPicking(MouseEvent evt, BehaviorInfo info) {
        Point mouse = evt.getPoint();

        try {
            info.pickCanvas.setShapeLocation(mouse.x, mouse.y);
            PickResult pickResult = info.pickCanvas.pickClosest();
            if (pickResult == null) {
                return;
            }

            TransformGroup tg =
                (TransformGroup)pickResult.getNode(PickResult.TRANSFORM_GROUP);
            //System.out.println(pickResult.getSceneGraphPath());            
            Point3d startPoint = info.pickCanvas.getStartPosition();
            PickIntersection intersection =
                        pickResult.getClosestIntersection(startPoint);
            intersectVW_ =
                new Point3f(intersection.getPointCoordinatesVW());
            verticesVW_ = intersection.getPrimitiveCoordinatesVW();
            _enableIndicator(tg, info);
        } catch (CapabilityNotSetException ex) {
            // もう出ることはないと思うが、読み込むモデルによっては
            // 出るかもしれないので、スタックトレースは表示する。
            ex.printStackTrace();
            _disableIndicator();
        }
    }

    public void processStartDrag(MouseEvent evt, BehaviorInfo info) {}
    public void processDragOperation(MouseEvent evt, BehaviorInfo info) {}
    public void processReleased(MouseEvent evt, BehaviorInfo info) {}
    public void processTimerOperation(BehaviorInfo info) {}

    //--------------------------------------------------------------------
    // OperationHandlerの実装
    public void disableHandler() {
        _disableIndicator();
    }

    public void setPickTarget(TransformGroup tg, BehaviorInfo info) {}

    //--------------------------------------------------------------------
    // プライベートメソッド
    private void _enableIndicator(TransformGroup tg, BehaviorInfo info) {
        // ObjectTranslationBehavior と ObjectRotationBehavior の場合は
        // BoundingBox の方向が pick されたのか
        // 判定して Behavior にモードを設定します

        String strFirst = null;
        String strSecond = null;
        Hashtable tgInfo;
        switch (mode_) {
        case FITTING_FROM:
            if (tg == fittingInfoFrom_.getArrowTransformGroup()) {
                bHaveFirst = false;
                fittingInfoFrom_.setTransformGroup(null);
                fittingInfoFrom_.removeForDisplay();
                return;
            }

            tgInfo = SceneGraphModifier.getHashtableFromTG(tg);
            if (tgInfo == null) return;
            strFirst = (String)tgInfo.get("objectName");

            tgInfo = SceneGraphModifier.getHashtableFromTG(
                fittingInfoTo_.getTransformGroup()
            );
            if (tgInfo != null) {
                strSecond = (String)tgInfo.get("objectName");
            }

            if (strFirst.equals(strSecond)) return;

            fittingInfoFrom_.setPickable(false);
            fittingInfoFrom_.setIntersectPoint(intersectVW_);
            fittingInfoFrom_.setPrimitiveCoordinates(verticesVW_);
            fittingInfoFrom_.setTransformGroup(tg);

            // 選択された面の情報を表示する
            fittingInfoFrom_.addForDisplay();

            bHaveFirst = true;
            break;
        case FITTING_TO:
            if (tg == fittingInfoTo_.getArrowTransformGroup()) {
                bHaveSecond = false;
                fittingInfoTo_.setTransformGroup(null);
                fittingInfoTo_.removeForDisplay();
                return;
            }

            tgInfo = SceneGraphModifier.getHashtableFromTG(tg);
            if (tgInfo == null) 
            	return;
            strSecond = (String)tgInfo.get("objectName");

            tgInfo = SceneGraphModifier.getHashtableFromTG(
                fittingInfoFrom_.getTransformGroup()
            );
            if (tgInfo != null) {
                strFirst = (String)tgInfo.get("objectName");
            }
            if (strSecond.equals(strFirst)) 
            	return;

            fittingInfoTo_.setPickable(false);
            fittingInfoTo_.setIntersectPoint(intersectVW_);
            fittingInfoTo_.setPrimitiveCoordinates(verticesVW_);
            fittingInfoTo_.setTransformGroup(tg);

            fittingInfoTo_.addForDisplay();

            bHaveSecond = true;
            break;
        }
    }

    private void _disableIndicator() {
        if (bHaveFirst) {
            bHaveFirst = false;
            fittingInfoFrom_.removeForDisplay();
            fittingInfoFrom_.setTransformGroup(null);
        }

        if (bHaveSecond) {
            bHaveSecond = false;
            fittingInfoTo_.removeForDisplay();
            fittingInfoTo_.setTransformGroup(null);
        }
    }

    private boolean _isSameSign(float x, float y) {
       return (((x >= 0) || (y >= 0)) && ((x <= 0) || (y <= 0)));
    }

    private boolean _isSameSignVector3f(Vector3f v1, Vector3f v2) {
       return (
           _isSameSign(v1.x, v2.x) &&
           _isSameSign(v1.y, v2.y) &&
           _isSameSign(v1.z, v2.z)
       );
    }

    private void _transformChanged(BehaviorInfo info, TransformGroup tg) {
        Hashtable hashtable = SceneGraphModifier.getHashtableFromTG(tg);
        GrxModelItem model = (GrxModelItem)hashtable.get("object");
        if (model == null) {
            System.out.println("no manipulatable.");
            return;
        }
        model.calcForwardKinematics();
        model.updateInitialTransformRoot();
    }
}

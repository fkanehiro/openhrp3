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
 * RoomViewHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.picking.*;

class RoomViewHandler extends ViewHandler {
    //--------------------------------------------------------------------
    // 定数
    private static final float ROTATION_FACTOR = 0.02f;
    private static final float TRANSLATION_FACTOR = 0.005f;
    private static final float ZOOM_FACTOR = 0.02f;

    //--------------------------------------------------------------------
    // インスタンス変数
    private Point3f pickPoint_;
    private Point prevPoint_;
    private boolean parallelMode_;
    //private double x_angle;
    //private double y_angle;

    //--------------------------------------------------------------------
    // コンストラクタ
    RoomViewHandler() {
        pickPoint_ = new Point3f();
    }

    //--------------------------------------------------------------------
    // 公開メソッド
    public void setParallelMode(boolean mode) {
        parallelMode_ = mode;
    }

    //--------------------------------------------------------------------
    // BehaviorHandlerの実装
    public void processPicking(MouseEvent evt, BehaviorInfo info) {
        prevPoint_ = new Point(evt.getPoint());

        info.pickCanvas.setShapeLocation(prevPoint_.x, prevPoint_.y);
        try {
            PickResult pickResult = info.pickCanvas.pickClosest();
            if (pickResult != null) {
                Point3d startPoint = info.pickCanvas.getStartPosition();
                PickIntersection intersection =
                    pickResult.getClosestIntersection(startPoint);
                pickPoint_ = new Point3f(intersection.getPointCoordinatesVW());
            }
        } catch (CapabilityNotSetException ex) {
            return;
        }
    }

    public void processDragOperation(MouseEvent evt, BehaviorInfo info) {
        switch (mode_[getMouseButtonMode(evt)]) {
        case ROTATION_MODE:
            if (!parallelMode_) {
                _rotation(evt, info);
            }
            break;
        case TRANSLATION_MODE:
            _translation(evt, info);
            break;
        case ZOOM_MODE:
            _zoom(evt, info);
            break;
        }

        prevPoint_ = new Point(evt.getPoint());
    }

    public void processStartDrag(MouseEvent evt, BehaviorInfo info) {}
    public void processReleased(MouseEvent evt, BehaviorInfo info) {}
    public void processTimerOperation(BehaviorInfo info) {}

    //--------------------------------------------------------------------
    // プライベートメソッド
    private void _rotation(MouseEvent evt, BehaviorInfo info) {
        double dx = ROTATION_FACTOR * (prevPoint_.getX() - evt.getPoint().getX());
        double dy = ROTATION_FACTOR * (prevPoint_.getY() - evt.getPoint().getY());

        // グローバル座標から視点座標への変換をもとめる。
        TransformGroup tgView = info.drawable.getTransformGroupRoot();
        Transform3D trView = new Transform3D();   // 視点の変換
        tgView.getTransform(trView);

        // グローバル座標系から視点座標系への変換
        Transform3D vw2view = new Transform3D();
        tgView.getLocalToVworld(vw2view);
        vw2view.mul(trView);
        vw2view.invert();

        // 視点から見たターゲットポイント
        Point3d pointTarget = new Point3d(pickPoint_);
        vw2view.transform(pointTarget);

        // 視点座標系をターゲットポイントまで移動
        Vector3d ray = new Vector3d(pointTarget);  // 視線ベクトル
        Transform3D trMove = new Transform3D();
        trMove.set(ray);
        trView.mul(trMove);

        // 視点座標系を回転
        Transform3D tr = new Transform3D();
        Vector3d zAxis = new Vector3d(0.0, 0.0, 1.0);
        vw2view.transform(zAxis);
        tr.set(new AxisAngle4d(zAxis, dx));
        trView.mul(tr);

        Vector3d xAxis = new Vector3d(1.0, 0.0, 0.0);
        tr.set(new AxisAngle4d(xAxis, dy));
        trView.mul(tr);

        // 視点座標系をターゲットポイントから離す
        trView.mulInverse(trMove);

        info.drawable.setTransform(trView);
    }

    private void _translation(MouseEvent evt, BehaviorInfo info) {
        double dx = TRANSLATION_FACTOR * (prevPoint_.getX() - evt.getPoint().getX());
        double dy = TRANSLATION_FACTOR * (prevPoint_.getY() - evt.getPoint().getY());

        Transform3D tr = new Transform3D();
        Transform3D trView = new Transform3D();
        TransformGroup tgView = info.drawable.getTransformGroupRoot();
        tgView.getTransform(trView);
        tr.set(new Vector3d(dx, -dy, 0.0f));
        trView.mul(tr);
        info.drawable.setTransform(trView);
    }

    private void _zoom(MouseEvent evt, BehaviorInfo info) {
        double dy = - ZOOM_FACTOR * (prevPoint_.getY() - evt.getPoint().getY());

        Transform3D tr = new Transform3D();
        Transform3D trView = new Transform3D();

        TransformGroup tgView = info.drawable.getTransformGroupRoot();
        tgView.getTransform(trView);
        tr.set(new Vector3d(0.0f, 0.0f, -dy));
        trView.mul(tr);
        info.drawable.setTransform(trView);
    }
}

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
 * ObjectTranslationHandler.java
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


import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.sun.j3d.utils.picking.*;

class ObjectTranslationHandler extends OperationHandler {
    private static final float TRANSLATION_FACTOR = 0.002f;

    private TransformGroup tgTarget_;
    private Switch bbSwitch_;
    private Vector3f norm_;
    private Point prevPoint_ = new Point();
    private boolean isPicked_;

    public void processPicking(MouseEvent evt, BehaviorInfo info) {
        prevPoint_.x = evt.getPoint().x;
        prevPoint_.y = evt.getPoint().y;

        norm_ = null;
        isPicked_ = false;

        try {
            info.pickCanvas.setShapeLocation(prevPoint_.x, prevPoint_.y);
            PickResult pickResult = info.pickCanvas.pickClosest();
            if (pickResult == null) {
                //_disableBoundingBox();
                return;
            }

            TransformGroup tg =
                (TransformGroup)pickResult.getNode(PickResult.TRANSFORM_GROUP);
            if (tg == null) {
                //_disableBoundingBox();
                return;
            }
            
            if (tg != tgTarget_) {
                if (_enableBoundingBox(tg, info)) {
                    isPicked_ = true;
                    //evt.consume();
                } 
            } else {
                Point3d startPoint = info.pickCanvas.getStartPosition();
                PickIntersection intersection =
                    pickResult.getClosestIntersection(startPoint);
                norm_ = new Vector3f(intersection.getPointNormal());
                //System.out.println("norm: " + norm_);
                isPicked_ = true;
                //evt.consume();
            }
        } catch (CapabilityNotSetException ex) {
            // もう出ることはないと思うが、読み込むモデルによっては
            // 出るかもしれないので、スタックトレースは表示する。
            ex.printStackTrace();
            _disableBoundingBox();
        }
    }

    public void processStartDrag(MouseEvent evt, BehaviorInfo info) {
        if (isPicked_) {
            evt.consume();
        }
    }

    public void processDragOperation(MouseEvent evt, BehaviorInfo info) {
        // tgViewからみたマウスの軌跡ベクトルmouseを求める。
        // mouse-(norm_,mouse)norm_ がオブジェクトの移動ベクトル
        if (isPicked_) {
            if (norm_ != null) {
                // ターゲットの座標系から視点座標系への変換を求める。
                Transform3D tr = new Transform3D();
                Transform3D l2vw = new Transform3D();
                Transform3D trTarget2View = new Transform3D();
         
                tgTarget_.getLocalToVworld(l2vw);
                tgTarget_.getTransform(tr);
                trTarget2View.mul(l2vw, tr);
         
                TransformGroup tgView = info.drawable.getTransformGroupRoot();
                tgView.getLocalToVworld(l2vw);
                tgView.getTransform(tr);
                l2vw.mul(tr);
                l2vw.invert();
         
                l2vw.mul(trTarget2View);
                trTarget2View.set(l2vw);
         
                // マウスの動きをターゲットの座標系の動きに変換
                float fdx =
                    TRANSLATION_FACTOR *
                        (float)(evt.getPoint().getX() - prevPoint_.getX());
                float fdy =
                    - TRANSLATION_FACTOR *
                        (float)(evt.getPoint().getY() - prevPoint_.getY());
                Vector3f mouse = new Vector3f(fdx, fdy, 0.0f);
                Vector3f normal = new Vector3f();
         
                trTarget2View.transform(norm_, normal);
         
                float inner = normal.dot(mouse);
                normal.scale(inner);
                mouse.sub(normal);
         
                trTarget2View.invert();
                trTarget2View.transform(mouse);
         
                tr.set(mouse);
                tgTarget_.getTransform(l2vw);
                l2vw.mul(tr);
                GrxLinkItem link = SceneGraphModifier.getLinkFromTG(tgTarget_);
                link.setTransform(l2vw);
                _transformChanged(info);
         
                prevPoint_.x = evt.getPoint().x;
                prevPoint_.y = evt.getPoint().y;
            }
            evt.consume();
        }
    }

    public void processReleased(MouseEvent evt, BehaviorInfo info) {
        if (isPicked_) {
            evt.consume();
        }
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
            tgTarget_ = null;
            bbSwitch_ = null;
            norm_ = null;
        }
    }

    private boolean _enableBoundingBox(TransformGroup tg, BehaviorInfo info) {
        Hashtable<String, Object> hashTable = SceneGraphModifier.getHashtableFromTG(tg);
        if (hashTable == null) 
        	return false;
        
        GrxModelItem model = SceneGraphModifier.getModelFromTG(tg);
        if (model == null) 
        	return false;

        SceneGraphModifier modifier = SceneGraphModifier.getInstance();
        modifier.resizeBounds(model);

        TransformGroup tgTarget = model.getTransformGroupRoot();
        hashTable = SceneGraphModifier.getHashtableFromTG(tgTarget);
        if (hashTable == null) 
        	return false;
        
        Switch sw = (Switch)hashTable.get("fullBoundingBoxSwitch");
        if (sw != null) {
            _disableBoundingBox();
            tgTarget_ = tgTarget;
            bbSwitch_ = sw;
            bbSwitch_.setWhichChild(Switch.CHILD_ALL);
            return true;
        } else {
            return false;
        }
    }

    private void _transformChanged(BehaviorInfo info) {
        GrxModelItem model = SceneGraphModifier.getModelFromTG(tgTarget_);
        if (model == null) {
            System.out.println("no manipulatable.");
            return;
        }
        model.calcForwardKinematics();
        model.updateInitialTransformRoot();
    }
}
/**
 * InvKinemaHandler.java
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
import com.sun.j3d.utils.picking.*;

class InvKinemaHandler extends OperationHandler {
    //--------------------------------------------------------------------
    // 定数
    public static final int FROM_MODE        = 1;
    public static final int ROTATION_MODE    = 2;
    public static final int TRANSLATION_MODE = 3;

    public static final int UNKNOWN = 0;
    public static final int ROTATION_WITH_X = 1;
    public static final int ROTATION_WITH_Y = 2;
    public static final int ROTATION_WITH_Z = 4;
    public static final int AXIS_FLAGS = 7;
    public static final int MINUS = 8;

    public static final int XY_TRANSLATION = 1;
    public static final int YZ_TRANSLATION = 2;
    public static final int ZX_TRANSLATION = 4;

    private static final float ROTATION_FACTOR    = (float)Math.PI / 360.0f;
    private static final float ROTATION_LIMIT     = (float)Math.PI / 360.0f;
    private static final float TRANSLATION_FACTOR = 0.001f;
    private static final float TRANSLATION_LIMIT  = 0.003f;
    
    //--------------------------------------------------------------------
    // インスタンス変数
    protected int rotationMode_ = UNKNOWN;
    protected int translationMode_ = UNKNOWN;
    
    private float limit_;

    /** マウスの動きの中心となる回転軸 */
    protected Vector3f v3fAnotherAxis = new Vector3f();
    protected Vector3f v3fAxisRotate = new Vector3f();
    
    /** 速度のために T3D を保持 */
    //protected Transform3D t3dWorld = new Transform3D();
    //protected Transform3D t3dView = new Transform3D();
    
    /** 回転軸が Canvas 状で、どのような線分に見えるかを示す */
    //protected Vector2f v2fAnotherAxis;
    protected Vector2f v2fDetermine;

    /** マウスの動きを投影する際の一つ目の軸 */
    protected Vector3f v3fAxisFirst = new Vector3f();
    /** マウスの動きを投影する際の二つ目の軸 */
    protected Vector3f v3fAxisSecond = new Vector3f();

    /** カメラ座標の z 軸方向とまったく同じ向きに軸がなってしまった場合に
     *  軸の方向の決定にしよう
     */
    protected Point3f point000 = new Point3f(0,0,0);

    private Vector3f normal_;
    private Point3f intersect_;

    private Switch bbSwitchFrom_;
    private Switch bbSwitchTo_;

    //private boolean bBoundsSecond;

    private TransformGroup tgTarget_;

    private int mode_;
    private Point startPoint_ = new Point();
    private Point point_ = new Point();

    private InvKinemaResolver resolver_;

    private boolean isPicked_;

    //--------------------------------------------------------------------
    // 公開メソッド
    public void setInvKinemaMode(int mode) {
        mode_ = mode;
    }

    public void setInvKinemaResolver(InvKinemaResolver resolver) {
        resolver_ = resolver;
    }

    //--------------------------------------------------------------------
    // BehaviorHandlerの実装
    public void processPicking(MouseEvent evt, BehaviorInfo info) {
        startPoint_.x = evt.getPoint().x;
        startPoint_.y = evt.getPoint().y;

        intersect_ = null;
        normal_ = null;
        tgTarget_ = null;
        isPicked_ = false;

        info.pickCanvas.setShapeLocation(startPoint_.x, startPoint_.y);
        PickResult pickResult = info.pickCanvas.pickClosest();
        if (pickResult == null) 
            return;

        TransformGroup tg = (TransformGroup)pickResult.getNode(PickResult.TRANSFORM_GROUP);
        if (tg == null) 
            return;

        Point3d startPoint = info.pickCanvas.getStartPosition();
        PickIntersection intersection = pickResult.getClosestIntersection(startPoint);
        intersect_ = new Point3f(intersection.getPointCoordinates());
        normal_ = new Vector3f(intersection.getPointNormal());
        if (_setInvKinema(tg, info)) {
            isPicked_ = true;
            // ピックが成功してもconsume()しなければ、ViewHandlerが
            // ピック処理をするので、視点の回転中心が設定される。
            //evt.consume();
        } else {
            intersect_ = null;
	    	normal_ = null;
        }
    }

    public void processStartDrag(MouseEvent evt, BehaviorInfo info) {
        if (isPicked_) {
            switch (mode_) {
            case FROM_MODE:
                break;
            case ROTATION_MODE:
                _decideRotationAxis(evt, info);
                limit_ = ROTATION_LIMIT;
                break;
            case TRANSLATION_MODE:
                _decideTranslationAxis(evt, info);
                limit_ = TRANSLATION_LIMIT;
                break;
            default:
                break;
            }
            evt.consume();
        }
    }

    public void processDragOperation(MouseEvent evt, BehaviorInfo info) {
        if (isPicked_) {
            switch (mode_) {
            case FROM_MODE:
                break;
            case ROTATION_MODE:
            case TRANSLATION_MODE:
                point_.x = evt.getPoint().x;
                point_.y = evt.getPoint().y;
                break;
            default:
                break;
            }
            evt.consume();
        }
    }

    public void processReleased(MouseEvent evt, BehaviorInfo info) {
        if (isPicked_) {
            evt.consume();
        }
    }

    public void processTimerOperation(BehaviorInfo info) {
        switch (mode_) {
        case FROM_MODE:
            break;
	case ROTATION_MODE:
            _rotation(info);
            break;
	case TRANSLATION_MODE:
            _translation(info);
            break;
        default:
            break;
        }
    }

    //--------------------------------------------------------------------
    // OperationHandlerの実装
    public void disableHandler() {
        _disableBoundingBox();
    }

    public void setPickTarget(TransformGroup tg, BehaviorInfo info) {
        switch(mode_) {
        case FROM_MODE:
            _disableBoundingBox();
            _enableBoundingBoxFrom(tg);
            break;
        case ROTATION_MODE:
        case TRANSLATION_MODE:
            if (bbSwitchFrom_ == null) {
                return;
            }

            if (tgTarget_ != tg) {
                _enableBoundingBoxTo(tg);
            }
            break;
        }
    }

    //--------------------------------------------------------------------
    // プライベートメソッド
    private void _disableBoundingBox() {
        if (bbSwitchFrom_ != null) {
            bbSwitchFrom_.setWhichChild(Switch.CHILD_NONE);
        }

        if (bbSwitchTo_ != null) {
            bbSwitchTo_.setWhichChild(Switch.CHILD_NONE);
        }

        tgTarget_ = null;
        bbSwitchFrom_ = null;
        bbSwitchTo_ = null;
    }

    private boolean _setInvKinema(TransformGroup tg, BehaviorInfo info) {
        switch(mode_) {
        case FROM_MODE:
            _disableBoundingBox();
            if (!_enableBoundingBoxFrom(tg)) {
                return false;
            }
            break;
        case ROTATION_MODE:
            if (bbSwitchFrom_ == null) {
                return false;
            }

            if (tgTarget_ != tg) {
                if (!_enableBoundingBoxTo(tg)) {
                    return false;
                }
            }
            _setRotationMode();
            break;
        case TRANSLATION_MODE:
            if (bbSwitchFrom_ == null) {
                return false;
            }

            if (tgTarget_ != tg) {
                if (!_enableBoundingBoxTo(tg)) {
                    return false;
                }
            }
            _setTranslationMode();
            break;
        default:
            return false;
        }
        return true;
    }

    private boolean _enableBoundingBoxFrom(TransformGroup tg) {
        Hashtable hashTable = SceneGraphModifier.getHashtableFromTG(tg);

        bbSwitchFrom_ = (Switch)hashTable.get("boundingBoxSwitch");
        if (bbSwitchFrom_ == null) {
            return false;
        }

        String objectName = (String)hashTable.get("objectName");
        if (objectName == null) {
            return false;
        }
        
        String jointName = (String)hashTable.get("jointName");
        if (jointName == null) {
            return false;
        }
        
        resolver_.setFromJoint(objectName, jointName);
        bbSwitchFrom_.setWhichChild(Switch.CHILD_ALL);
        return true;
     }

    private boolean _enableBoundingBoxTo(TransformGroup tg) {
        Hashtable hashTable = SceneGraphModifier.getHashtableFromTG(tg);
        if (bbSwitchTo_ != null) {
            bbSwitchTo_.setWhichChild(Switch.CHILD_NONE);
        }

        bbSwitchTo_ = (Switch)hashTable.get("boundingBoxSwitch");
     
        // fromジョイントと同じジョイントをPickした場合
        if (bbSwitchFrom_ == bbSwitchTo_) {
            bbSwitchTo_ = null;
            return false;
        }
     
        if (bbSwitchTo_ != null && 
        		resolver_.setToJoint(
                (String)hashTable.get("objectName"),
                (String)hashTable.get("jointName"))) {
            bbSwitchTo_.setWhichChild(Switch.CHILD_ALL);
            tgTarget_ = tg;
            return true;
        } else {
            return false;
        }
    }

    private void _setRotationMode() {
        if (Math.abs(normal_.x) == 1.0f) {
            rotationMode_ = ROTATION_WITH_X;
        } else if(Math.abs(normal_.y) == 1.0f) {
            rotationMode_ = ROTATION_WITH_Y;
        } else if(Math.abs(normal_.z) == 1.0f) {
            rotationMode_ = ROTATION_WITH_Z;
        }
    }

    private void _setTranslationMode() {
        if (Math.abs(normal_.x) == 1.0f) {
            translationMode_ = YZ_TRANSLATION;
        } else if(Math.abs(normal_.y) == 1.0f) {
            translationMode_ = ZX_TRANSLATION;
        } else if (Math.abs(normal_.z) == 1.0f) {
            translationMode_ = XY_TRANSLATION;
        }
    }

    private void _decideTranslationAxis(MouseEvent evt, BehaviorInfo info) {
        // 現在のモードにしたがった内積を取る対象の二軸の設定をします
        switch(translationMode_) {
        case XY_TRANSLATION:
            v3fAxisFirst.set(1.0f,0.0f,0.0f);
            v3fAxisSecond.set(0.0f,1.0f,0.0f);
            break;
        case YZ_TRANSLATION:
            v3fAxisFirst.set(0.0f,1.0f,0.0f);
            v3fAxisSecond.set(0.0f,0.0f,1.0f);
            break;
        case ZX_TRANSLATION:
            v3fAxisFirst.set(0.0f,0.0f,1.0f);
            v3fAxisSecond.set(1.0f,0.0f,0.0f);
            break;
        }

        // 軸を現在のワールド座標へ変換する
        Transform3D t3dLocalToVworld = new Transform3D();
        Transform3D t3dCurrent = new Transform3D();
        Transform3D t3dWorld = new Transform3D();
        Transform3D t3dView = new Transform3D();

        tgTarget_.getLocalToVworld(t3dLocalToVworld);
        tgTarget_.getTransform(t3dCurrent);
        // それぞれの単位ベクトルを transform するとワールド座標
        // におけるそれぞれの軸の方向が得られる。

        // この TG がワールドへの変換に相当する
        t3dLocalToVworld.mul(t3dCurrent);
        t3dWorld.set(t3dLocalToVworld);

        TransformGroup tgView = info.drawable.getTransformGroupRoot();
        tgView.getLocalToVworld(t3dLocalToVworld);
        tgView.getTransform(t3dCurrent);

        // この TG がカメラへの変換に相当する
        t3dLocalToVworld.mul(t3dCurrent);
        t3dView.set(t3dLocalToVworld);
        t3dView.invert();

        t3dView.mul(t3dWorld);
        t3dView.transform(v3fAxisFirst);
        t3dView.transform(v3fAxisSecond);

        point000.set(0f,0f,0f);
        t3dView.transform(point000);
        if (intersect_ != null) {
	    Point3f pointPicked = new Point3f(intersect_);
            t3dView.transform(pointPicked);
        }

        info.setTimerEnabled(true);
    }

    private void _decideRotationAxis(MouseEvent evt, BehaviorInfo info) {
        // 指定された時間が経過しました
        // マウスの前回からの移動量をベクトルに直す
        int dx = evt.getPoint().x - startPoint_.x;
	int dy = evt.getPoint().y - startPoint_.y;

        Point2f pointMouse =
            new Point2f(
	        (float)dx * ROTATION_FACTOR,
	        (float)dy * ROTATION_FACTOR
            );
        Vector2f v2fMouse = new Vector2f(pointMouse);

        v2fMouse.y *= -1.0f;

        // 最初の動きで回転の軸を決定します
        Vector3f v3fAxisFirst = new Vector3f();
        Vector3f v3fAxisSecond = new Vector3f();

        // 軸を現在のワールド座標へ変換する
        Transform3D t3dLocalToVworld = new Transform3D();
        Transform3D t3dCurrent = new Transform3D();
        Transform3D t3dWorld = new Transform3D();
        Transform3D t3dView = new Transform3D();

        tgTarget_.getLocalToVworld(t3dLocalToVworld);
        tgTarget_.getTransform(t3dCurrent);
        // それぞれの単位ベクトルを transform すると
        // ワールド座標におけるそれぞれの軸の方向が得られる。
        t3dLocalToVworld.mul(t3dCurrent);  // ワールドへの変換に相当する
        t3dWorld.set(t3dLocalToVworld);

        TransformGroup tgView = info.drawable.getTransformGroupRoot();
        tgView.getLocalToVworld(t3dLocalToVworld);
        tgView.getTransform(t3dCurrent);
        t3dLocalToVworld.mul(t3dCurrent);  // がカメラへの変換に相当する
        t3dView.set(t3dLocalToVworld);
        t3dView.invert();

        t3dView.mul(t3dWorld);

        switch(rotationMode_ & AXIS_FLAGS) {
        case ROTATION_WITH_X:
            v3fAxisFirst.set(0.0f,1.0f,0.0f);
            v3fAxisSecond.set(0.0f,0.0f,1.0f);
            break;
        case ROTATION_WITH_Y:
            v3fAxisFirst.set(0.0f,0.0f,1.0f);
            v3fAxisSecond.set(1.0f,0.0f,0.0f);
            break;
        case ROTATION_WITH_Z:
            v3fAxisFirst.set(1.0f,0.0f,0.0f);
            v3fAxisSecond.set(0.0f,1.0f,0.0f);
            break;
        }

        t3dView.transform(v3fAxisFirst);
        t3dView.transform(v3fAxisSecond);

        // 移動量を定義するためのラジアン
        float fDotProductFirst = 0.0f;
        float fDotProductSecond = 0.0f;

        // Canvas 平面へ投j影
        Vector2f v2fAxisFirst = new Vector2f(v3fAxisFirst.x, v3fAxisFirst.y);
        Vector2f v2fAxisSecond = new Vector2f(v3fAxisSecond.x, v3fAxisSecond.y);

        v2fAxisFirst.normalize();
        v2fAxisSecond.normalize();

        fDotProductFirst = v2fAxisFirst.dot(v2fMouse);
        fDotProductSecond = v2fAxisSecond.dot(v2fMouse);

        // 以前の回転軸の決定パターン
        switch(rotationMode_ & AXIS_FLAGS) {
        case ROTATION_WITH_X:
            if (Math.abs(fDotProductFirst) < Math.abs(fDotProductSecond)) {
                v3fAnotherAxis = v3fAxisSecond;
                v3fAxisRotate.set(0.0f, 1.0f, 0.0f);
            } else {
                v3fAnotherAxis = v3fAxisFirst;
                v3fAxisRotate.set(0.0f, 0.0f, 1.0f);
            }
            break;
        case ROTATION_WITH_Y:
            if (Math.abs(fDotProductFirst) < Math.abs(fDotProductSecond)) {
                v3fAnotherAxis = v3fAxisSecond;
                v3fAxisRotate.set(0.0f, 0.0f, 1.0f);
            } else {
                v3fAnotherAxis = v3fAxisFirst;
                v3fAxisRotate.set(1.0f, 0.0f, 0.0f);
            }
            break;
        case ROTATION_WITH_Z:
            if (Math.abs(fDotProductFirst) < Math.abs(fDotProductSecond)) {
                v3fAnotherAxis = v3fAxisSecond;
                v3fAxisRotate.set(1.0f, 0.0f, 0.0f);
            } else {
                v3fAnotherAxis = v3fAxisFirst;
                v3fAxisRotate.set(0.0f, 1.0f, 0.0f);
            }
            break;
        }

        Vector3f v3fTemp = new Vector3f();
        v3fTemp.set(v3fAxisRotate);
        t3dView.transform(v3fAxisRotate);

        // 平面状の回転軸ベクトルから -90 度回転した二次元平面状の軸
        // とマウスの動作のベクトルの内積が正だったらそのまま。
        // 負だったら正負を反転して使う。

        Vector2f v2fAxisRotate = new Vector2f(v3fAxisRotate.x, v3fAxisRotate.y);

        //v2fAnotherAxis = new Vector2f(v3fAnotherAxis.x, v3fAnotherAxis.y);

        // 回転軸を -90 回転したもの
        v2fDetermine = new Vector2f(v2fAxisRotate.y, -v2fAxisRotate.x);

        v3fAxisRotate.set(v3fTemp);

        v2fMouse.normalize();
        //float fDetermine = v2fMouse.dot(v2fDetermine);

        info.setTimerEnabled(true);
    }

    private void _translation(BehaviorInfo info) {
        // 指定された時間が経過しました
        // マウスの前回からの移動量をベクトルに直す
        int dx = point_.x - startPoint_.x;
	int dy = point_.y - startPoint_.y;
        // ?_factor でマウスの移動量に対する回転量の倍率を指定
        // マウスの動作においては下がプラスになっているので反転しておく
        Point2f pointMouse =
            new Point2f(
	        (float)dx * TRANSLATION_FACTOR,
	        (float)dy * TRANSLATION_FACTOR
            );
        Vector2f v2fMouse = new Vector2f(pointMouse);

        v2fMouse.y *= -1.0f;

        float fDotProductFirst = 0.0f;  // 移動量を定義するためのラジアン
        float fDotProductSecond = 0.0f;  // 移動量を定義するためのラジアン

        // Canvas 平面へ投影
        Vector2f v2fAxisFirst = new Vector2f(v3fAxisFirst.x, v3fAxisFirst.y);
        Vector2f v2fAxisSecond = new Vector2f(v3fAxisSecond.x, v3fAxisSecond.y);

        // 軸の追加
        if (v2fAxisFirst.length() == 0) {
            if (intersect_ != null) {
	        Point3f pointPicked = new Point3f(intersect_);
                pointPicked.sub(point000);  // View からみた point 000 で割る
                pointPicked.scale(-1.0f);
                v3fAxisFirst.set(pointPicked);
                v2fAxisFirst.set(v3fAxisFirst.x,v3fAxisFirst.y);
            } else {
                // 軸が判定できなかったとき用
                v2fAxisFirst.set(0.0f,1.0f);
            }
        }

        if (v2fAxisSecond.length() == 0) {
            if (intersect_ != null) {
	        Point3f pointPicked = new Point3f(intersect_);
                pointPicked.sub(point000);
                pointPicked.scale(-1.0f);
                v3fAxisSecond.set(pointPicked);
                v2fAxisSecond.set(v3fAxisSecond.x,v3fAxisSecond.y);
            } else {
                // 軸が判定できなかったとき用
                v2fAxisSecond.set(1.0f,0.0f);
            }
        }
        v2fAxisFirst.normalize();
        v2fAxisSecond.normalize();

        fDotProductFirst = v2fAxisFirst.dot(v2fMouse);
        fDotProductSecond = v2fAxisSecond.dot(v2fMouse);

        if (Float.isNaN(fDotProductFirst)) {
            fDotProductFirst = 0;
        }

        if (Float.isNaN(fDotProductSecond)) {
            fDotProductSecond = 0;
        }

        Transform3D t3dCur = new Transform3D();
        Transform3D t3dTranslate = new Transform3D();
        tgTarget_.getTransform(t3dCur);
        Vector3f v3fTranslate = new Vector3f();

        // 閾値チェック
        if (Math.abs(fDotProductFirst) > TRANSLATION_LIMIT) {
            if (fDotProductFirst > TRANSLATION_LIMIT) {
                fDotProductFirst = TRANSLATION_LIMIT;
            } else {
                fDotProductFirst = -TRANSLATION_LIMIT;
            }
        }
        if (Math.abs(fDotProductSecond) > TRANSLATION_LIMIT) {
            if (fDotProductSecond > TRANSLATION_LIMIT) {
                fDotProductSecond = TRANSLATION_LIMIT;
            } else {
                fDotProductSecond = -TRANSLATION_LIMIT;
            }
        }

        switch(translationMode_) {
        case XY_TRANSLATION:
            v3fTranslate.x += fDotProductFirst;
            v3fTranslate.y += fDotProductSecond;
            break;
        case YZ_TRANSLATION:
            v3fTranslate.y += fDotProductFirst;
            v3fTranslate.z += fDotProductSecond;
            break;
        case ZX_TRANSLATION:
            v3fTranslate.z += fDotProductFirst;
            v3fTranslate.x += fDotProductSecond;
            break;
        }
        
        t3dTranslate.set(v3fTranslate);
        t3dCur.mul(t3dTranslate);
        tgTarget_.setTransform(t3dCur);

        _resolve();
        // 変換情報を通知
        //broadcastEvent();
    }

    private void _rotation(BehaviorInfo info) {
        int dx = point_.x - startPoint_.x;
	int dy = point_.y - startPoint_.y;

        Point2f pointMouse = new Point2f(
            (float)dx * ROTATION_FACTOR,
            (float)dy * ROTATION_FACTOR
        );
        Vector2f v2fMouse = new Vector2f(pointMouse);
        // Swing だと不要？
        v2fMouse.y *= -1.0f;
        float fDotProductRotate = v2fDetermine.dot(v2fMouse);

        Transform3D t3dCur = new Transform3D();
        Transform3D t3dRotate = new Transform3D();
        tgTarget_.getTransform(t3dCur);

        // 閾値チェック
        if (Math.abs(fDotProductRotate) > ROTATION_LIMIT) {
            if (fDotProductRotate > ROTATION_LIMIT) {
                fDotProductRotate = ROTATION_LIMIT;
            }
        } else {
            fDotProductRotate = - ROTATION_LIMIT;
        }

        AxisAngle4f axis =
                new AxisAngle4f(v3fAxisRotate, fDotProductRotate);
        t3dRotate.set(axis);
        t3dCur.mul(t3dRotate);
        tgTarget_.setTransform(t3dCur);

        _resolve();
        // 変換情報を通知
        //broadcastEvent();
    }

    private void _resolve() {
        Transform3D t3dCurrent = new Transform3D();
        tgTarget_.getTransform(t3dCurrent);
        Transform3D t3dLocalToVworld = new Transform3D();
        tgTarget_.getLocalToVworld(t3dLocalToVworld);
        Transform3D t3dCur = new Transform3D();
        tgTarget_.getTransform(t3dCur);

        // まず Tn の変換を求める
        // Transform3D 同士の変換を結合する
        t3dLocalToVworld.mul(t3dCur);
        t3dCurrent = t3dLocalToVworld;

        if (resolver_.resolve(t3dCurrent)) {
            // Bounding Box の resize
            // 成功したら limit 値をあげる
            if (mode_ == ROTATION_MODE) {
                limit_ += (float)(Math.PI/1440);
            } else {
                limit_ += 0.0001f;
            }
        } else {
            // 失敗したら limit 値をさげる
            if (mode_ == ROTATION_MODE) {
	        if (limit_ > (float)Math.PI/1440) {
                    limit_ -= Math.PI/1440;
		}
            } else {
	        if (limit_ > 0.0001f) {
                    limit_ -= 0.0001f;
		}
            }
        }
    }   
}

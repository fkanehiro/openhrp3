/**
 * ViewInfo.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.tdview;

import javax.media.j3d.*;
import javax.vecmath.*;

public class ViewInfo {
    /* View のモードを示すフラグ */
    public static final int VIEW_MODE_FIXED = 0;
    public static final int VIEW_MODE_WALK = 1;
    public static final int VIEW_MODE_ROOM = 2;
    public static final int VIEW_MODE_PARALLEL = 4;
    public static final int MODE_MASK =
        VIEW_MODE_WALK | VIEW_MODE_ROOM | VIEW_MODE_PARALLEL;

    public static final int FRONT_VIEW = 8;
    public static final int BACK_VIEW = 16;
    public static final int LEFT_VIEW = 32;
    public static final int RIGHT_VIEW = 64;
    public static final int TOP_VIEW = 128;
    public static final int BOTTOM_VIEW = 256;
    public static final int VIEW_MASK =
      FRONT_VIEW | BACK_VIEW | LEFT_VIEW | RIGHT_VIEW | TOP_VIEW | BOTTOM_VIEW;

    private int mode_;         // モード
    private double distance_;  // 原点からの距離

    protected Transform3D transform_;

    public double fieldOfView;
    public double frontClipDistance;
    public double backClipDistance;

    private ViewModeChangeListener listener_;

    /**
     * コンストラクタ
     * @param   mode
     * @param   dist
     */
    public ViewInfo(int mode, double dist) {
        transform_ = new Transform3D();
        setDistance(dist);
        setViewMode(mode);
        setDirection(mode);
    }

    /**
     * ビューモード設定
     * @param   mode
     */
    public void setViewMode(int mode) {
        if ((mode_ & VIEW_MASK) == VIEW_MODE_FIXED) return;
        mode_ = (mode & MODE_MASK) | (mode_ & VIEW_MASK);
        if (listener_ != null) {
            listener_.viewModeChanged(mode);
        }
    }

    /**
     *
     * @param   dist
     */
    public void setDistance(double dist) {
        distance_ = dist;
    }

    /**
     *
     * @param   dir
     */
    public void setDirection(int dir) {
        // VIEW_MODE_PARALELL以外のモードではセットできない
        //if ((mode_ & MODE_MASK) != VIEW_MODE_PARALLEL) return;
        mode_ = (mode_ & MODE_MASK) | (dir & VIEW_MASK);
        Matrix3d rot = new Matrix3d();
        Vector3d pos = new Vector3d();
        switch (mode_ & VIEW_MASK) {
        case FRONT_VIEW:
            pos.set(new double[]{distance_, 0.0, 0.0});
            rot.set(
                new Matrix3d(
                    0.0, 0.0, 1.0,
                    1.0, 0.0, 0.0,
                    0.0, 1.0, 0.0
                )
            );
            break;
        case BACK_VIEW:
            pos.set(new double[]{-distance_, 0.0, 0.0});
            rot.set(
                new Matrix3d(
                    0.0, 0.0, -1.0,
                    -1.0, 0.0, 0.0,
                    0.0, 1.0, 0.0
                )
            );
            break;
        case LEFT_VIEW:
            pos.set(new double[]{0.0, distance_, 0.0});
            rot.set(
                new Matrix3d(
                    -1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0,
                    0.0, 1.0, 0.0
                )
            );
            break;
        case RIGHT_VIEW:
            pos.set(new double[]{0.0, -distance_, 0.0});
            rot.set(
                new Matrix3d(
                    1.0, 0.0, 0.0,
                    0.0, 0.0, -1.0,
                    0.0, 1.0, 0.0
                )
            );
            break;
        case TOP_VIEW:
            pos.set(new double[]{0.0, 0.0, distance_});
            rot.set(
                new Matrix3d(
                    0.0, 1.0, 0.0,
                    -1.0, 0.0, 0.0,
                    0.0, 0.0, 1.0
                )
            );
            break;
        case BOTTOM_VIEW:
            pos.set(new double[]{0.0, 0.0, -distance_});
            rot.set(
                new Matrix3d(
                    0.0, 1.0, 0.0,
                    1.0, 0.0, 0.0,
                    0.0, 0.0, -1.0
                )
            );
            break;
        }
        transform_.setTranslation(pos);
        transform_.setRotation(rot);
    }

    /**
     * ビューモード取得
     * @return  ビューモード
     */
    public int getViewMode() {
        return (mode_ & MODE_MASK);
    }

    public int getDirection() {
         return (mode_ & VIEW_MASK);
    }

    /**
     * トランスフォーム取得
     * @param  tr
     */
    public Transform3D getTransform() {
        return transform_;
    }

    public void setTransform(Transform3D transform) {
        transform_.set(transform);
    }

    public void addViewModeChangeListener(ViewModeChangeListener listener) {
        listener_ = ViewModeChangeMulticaster.add(listener_, listener);
    }

    public void removeViewModeChangeListener(ViewModeChangeListener listener) {
        listener_ = ViewModeChangeMulticaster.remove(listener_, listener);
    }
}

/**
 * IseBehavior.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.AWTEvent;
import java.awt.event.*;
import java.util.Enumeration;
import javax.media.j3d.*;
import javax.vecmath.*;

/**
 * Behaviorの実装クラス。
 * 実際の処理はハンドラクラスに委ねられる。
 */
class IseBehavior extends Behavior {
    private static final WakeupCondition WAKEUP_ON_MOUSE_PRESSED =
        new WakeupOr(
            new WakeupCriterion[] {
                new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED)
            }
        );

    private static final WakeupCondition WAKEUP_ON_MOUSE_DRAGGED =
        new WakeupOr(
            new WakeupCriterion[] {
                new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED),
                new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED)
            }
        );

    private static final WakeupCondition WAKEUP_ON_TIMER_EVENT =
        new WakeupOr(
            new WakeupCriterion[] {
                new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED),
                new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED),
                new WakeupOnElapsedTime(10)
            }
        );

    //--------------------------------------------------------------------
    // インスタンス変数
    private BehaviorHandler handler_;
    private WakeupCondition wakeupCondition_;
    private BehaviorInfo info_;
    private boolean isDragging_;

    //--------------------------------------------------------------------
    // コンストラクタ
    public IseBehavior(BehaviorHandler handler) {
        handler_ = handler;
        setSchedulingBounds(
            new BoundingSphere(new Point3d(), Double.POSITIVE_INFINITY)
        );
    }

    //--------------------------------------------------------------------
    // 公開メソッド
    public void initialize() {
        wakeupCondition_ = WAKEUP_ON_MOUSE_PRESSED;
        wakeupOn(wakeupCondition_);
    }

    public void setBehaviorInfo(BehaviorInfo info) {
        info_ = info;
    }

    public BehaviorInfo getBehaviorInfo() { return info_; }

    public void processStimulus(Enumeration criteria) {
        while (criteria.hasMoreElements()) {
            WakeupCriterion wakeup = (WakeupCriterion)criteria.nextElement();
            if (wakeup instanceof WakeupOnAWTEvent) {
                AWTEvent[] event = ((WakeupOnAWTEvent)wakeup).getAWTEvent();
                if (event[0] instanceof MouseEvent) {
                    MouseEvent mouseEvent = (MouseEvent)event[0];
                    _processMouseEvent(mouseEvent);
                }
            } else if (wakeup instanceof WakeupOnElapsedTime) {
                handler_.processTimerOperation(info_);
            }
        }

        wakeupOn(wakeupCondition_);
    }

    private synchronized void _processMouseEvent(MouseEvent evt) {
        //System.out.println("IseBehavior: processMouseEvent()");
        switch (evt.getID()) {
        case MouseEvent.MOUSE_PRESSED:
            //PickingInfo info = _makePickingInfo(evt);
            handler_.processPicking(evt, info_);
            wakeupCondition_ = WAKEUP_ON_MOUSE_DRAGGED;
            break;
        case MouseEvent.MOUSE_RELEASED:
            info_.setTimerEnabled(false);
            handler_.processReleased(evt, info_);
            isDragging_ = false;
            wakeupCondition_ = WAKEUP_ON_MOUSE_PRESSED;
            break;
        case MouseEvent.MOUSE_DRAGGED:
            if (isDragging_) {
                handler_.processDragOperation(evt, info_);
                if (info_.isTimerEnabled()) {
                    wakeupCondition_ = WAKEUP_ON_TIMER_EVENT;
                }
            } else {
                isDragging_ = true;
                handler_.processStartDrag(evt, info_);
            }
            break;
        }
    }
}


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
 * IseBehaviorHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.event.*;
import javax.media.j3d.TransformGroup;

import org.eclipse.swt.widgets.Display;

/**
 * BehaviorHandlerの実装クラス。
 * 実際の処理はViewHandlerクラス、OperationHandlerクラスに委譲される。
 * モードによって、ViewHandler,OperationHandlerの具象クラスを切替える。
 */
class IseBehaviorHandler implements BehaviorHandler {
    //--------------------------------------------------------------------
    // 定数

    // OperationHandlerの具象クラスのインスタンス
    private static final ObjectRotationHandler
        OBJECT_ROTATION_HANDLER = new ObjectRotationHandler();
    private static final ObjectTranslationHandler
        OBJECT_TRANSLATION_HANDLER = new ObjectTranslationHandler();
    private static final JointRotationHandler
        JOINT_ROTATION_HANDLER = new JointRotationHandler();
    private static final ObjectFittingHandler
        OBJECT_FITTING_HANDLER = new ObjectFittingHandler();
    private static final InvKinemaHandler
        INV_KINEMA_HANDLER = new InvKinemaHandler();

    // ViewHandlerの具象クラスのインスタンス
    private static final WalkViewHandler
        WALK_VIEW_HANDLER = new WalkViewHandler();
    private static final RoomViewHandler
        ROOM_VIEW_HANDLER = new RoomViewHandler();

    public static final String
        BUTTON_MODE_ROTATION = "button_mode_rotation";
    public static final String
        BUTTON_MODE_TRANSLATION = "button_mode_translation"; 
    public static final String BUTTON_MODE_ZOOM = "button_mode_zoom";
    public static final String CTRL_PRESSED = "ctrl_pressed";
    public static final String ALT_PRESSED = "alt_pressed";

    private static final int TIMER_MODE_OFF       = 0;
    private static final int TIMER_MODE_OPERATION = 1;
    private static final int TIMER_MODE_VIEW      = 2;

    //--------------------------------------------------------------------
    // インスタンス変数
    private ViewHandler viewHandler_;
    private OperationHandler operationHandler_;
    private BehaviorHandler indicator_;
    private int timerMode_;
    private BehaviorManager bManager_;

    //--------------------------------------------------------------------
    // コンストラクタ
    public IseBehaviorHandler(BehaviorManager bManager) {
        viewHandler_ = ROOM_VIEW_HANDLER;
        ROOM_VIEW_HANDLER.setParallelMode(false);
        bManager_ = bManager;
    }

    //--------------------------------------------------------------------
    // 公開メソッド
    public void setViewIndicator(BehaviorHandler handler) {
        indicator_ = handler;
    }
    
    public void setViewMode(int mode) {
        switch (mode) {
        case BehaviorManager.WALK_VIEW_MODE:
            viewHandler_ = WALK_VIEW_HANDLER;
            break;
        case BehaviorManager.ROOM_VIEW_MODE:
            viewHandler_ = ROOM_VIEW_HANDLER;
            ROOM_VIEW_HANDLER.setParallelMode(false);
            break;
        case BehaviorManager.PARALLEL_VIEW_MODE:
            viewHandler_ = ROOM_VIEW_HANDLER;
            ROOM_VIEW_HANDLER.setParallelMode(true);
            break;
        }
    }

    public void setViewHandlerMode(String str) {
        WALK_VIEW_HANDLER.setMode(str);
        ROOM_VIEW_HANDLER.setMode(str);
    }

    public void setOperationMode(int mode) {
        switch (mode) {
        case BehaviorManager.OPERATION_MODE_NONE:
            if (operationHandler_ != null) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = null;
            break;
        case BehaviorManager.OBJECT_ROTATION_MODE:
            if (operationHandler_ != null) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = OBJECT_ROTATION_HANDLER;
            break;
        case BehaviorManager.OBJECT_TRANSLATION_MODE:
            if (operationHandler_ != null) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = OBJECT_TRANSLATION_HANDLER;
            break;
        case BehaviorManager.JOINT_ROTATION_MODE:
            if (operationHandler_ != null) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = JOINT_ROTATION_HANDLER;
            break;
        case BehaviorManager.FITTING_FROM_MODE:
            if (operationHandler_ != null &&
                operationHandler_ != OBJECT_FITTING_HANDLER
            ) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = OBJECT_FITTING_HANDLER;
            OBJECT_FITTING_HANDLER.setFittingMode(
                ObjectFittingHandler.FITTING_FROM
            );
            break;
        case BehaviorManager.FITTING_TO_MODE:
            if (operationHandler_ != null &&
                operationHandler_ != OBJECT_FITTING_HANDLER
            ) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = OBJECT_FITTING_HANDLER;
            OBJECT_FITTING_HANDLER.setFittingMode(
                ObjectFittingHandler.FITTING_TO
            );
            break;
        case BehaviorManager.INV_KINEMA_FROM_MODE:
            if (operationHandler_ != null &&
                operationHandler_ != INV_KINEMA_HANDLER
            ) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = INV_KINEMA_HANDLER;
            INV_KINEMA_HANDLER.setInvKinemaMode(InvKinemaHandler.FROM_MODE);
            break;
        case BehaviorManager.INV_KINEMA_TRANSLATION_MODE:
            if (operationHandler_ != null &&
                operationHandler_ != INV_KINEMA_HANDLER
            ) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = INV_KINEMA_HANDLER;
            INV_KINEMA_HANDLER.setInvKinemaMode(
                InvKinemaHandler.TRANSLATION_MODE
            );
            break;
        case BehaviorManager.INV_KINEMA_ROTATION_MODE:
            if (operationHandler_ != null &&
                operationHandler_ != INV_KINEMA_HANDLER
            ) {
                operationHandler_.disableHandler();
            }
            operationHandler_ = INV_KINEMA_HANDLER;
            INV_KINEMA_HANDLER.setInvKinemaMode(
                InvKinemaHandler.ROTATION_MODE
            );
            break;
        }
    }

    public void setInvKinemaResolver(InvKinemaResolver resolver) {
        INV_KINEMA_HANDLER.setInvKinemaResolver(resolver);
    }

    private boolean ret_;
    public boolean fit(final BehaviorInfo info) {
        if (operationHandler_ == OBJECT_FITTING_HANDLER) {
         	syncExec(new Runnable(){
            	public void run(){
            		ret_ = OBJECT_FITTING_HANDLER.fit(info);
            	}
            });
        	return ret_;
        } else {
            return false;
        }
    }

    public void setPickTarget(TransformGroup tg, BehaviorInfo info) {
        if (operationHandler_ != null) {
            operationHandler_.setPickTarget(tg, info);
        }
    }

    //--------------------------------------------------------------------
    // BehaviorHandlerの実装
    public void processPicking(final MouseEvent evt, final BehaviorInfo info) {
        if (!evt.isAltDown() && !evt.isMetaDown()) {
            if (operationHandler_ != null) {
            	syncExec(new Runnable(){
                	public void run(){
                		operationHandler_.processPicking(evt, info);
                	}
                });
            }
        }

        if (!evt.isConsumed()) {
            viewHandler_.processPicking(evt, info);
            indicator_.processPicking(evt, info);
        }
    }

    public void processStartDrag(final MouseEvent evt, final BehaviorInfo info) {
        int mode = TIMER_MODE_OFF;
        timerMode_ = TIMER_MODE_OFF;

        if (!evt.isAltDown() && !evt.isMetaDown()) {
            if (operationHandler_ != null) {
            	syncExec(new Runnable(){
                	public void run(){
                		operationHandler_.processStartDrag(evt, info);
                	}
                });
                mode = TIMER_MODE_OPERATION;
            }
        }

        if (!evt.isConsumed()) {
            viewHandler_.processStartDrag(evt, info);
            mode = TIMER_MODE_VIEW;
        }

        if (info.isTimerEnabled()) {
            timerMode_ = mode;
        }
    }

    public void processDragOperation(final MouseEvent evt, final BehaviorInfo info) {
        if (!evt.isAltDown() && !evt.isMetaDown()) {
            if (operationHandler_ != null) {
            	syncExec(new Runnable(){
                	public void run(){
                		operationHandler_.processDragOperation(evt, info);
                	}
                });
            }
        }

        if (!evt.isConsumed()) {
            viewHandler_.processDragOperation(evt, info);
        }
    }

    public void processReleased(final MouseEvent evt, final BehaviorInfo info) {
        if (!evt.isAltDown() && !evt.isMetaDown()) {
            if (operationHandler_ != null) {
            	syncExec(new Runnable(){
                	public void run(){
                		operationHandler_.processReleased(evt, info);
                	}
                });
            }
        }

        if (!evt.isConsumed()) {
            viewHandler_.processReleased(evt, info);
            indicator_.processReleased(evt, info);
        }
    }

    public boolean processTimerOperation(final BehaviorInfo info) {
        switch (timerMode_) {
        case TIMER_MODE_OFF:
            break;
        case TIMER_MODE_OPERATION:
        	if(operationHandler_ instanceof InvKinemaHandler)
            if(!bManager_.initDynamicsSimulator())	
            	return false;
        	syncExec(new Runnable(){
            	public void run(){
            		operationHandler_.processTimerOperation(info);
            	}
            });
            break;
        case TIMER_MODE_VIEW:
            viewHandler_.processTimerOperation(info);
            break;
        }
        return true;
    }
    
    private boolean syncExec(Runnable r){
		Display display = Display.getDefault();
        if ( display!=null && !display.isDisposed()){
            display.syncExec( r );
            return true;
        }else
        	return false;
	}
}

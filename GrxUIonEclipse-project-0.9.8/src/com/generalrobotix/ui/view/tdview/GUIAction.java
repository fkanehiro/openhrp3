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
 * GUIAction.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 27 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.AWTEventMulticaster;
import javax.swing.AbstractAction;

import com.generalrobotix.ui.util.IconProperties;
import com.generalrobotix.ui.util.MessageBundle;



/**
 * 
 */
@SuppressWarnings("serial")
public class GUIAction extends AbstractAction {
    public static final GUIAction EXIT  = new GUIAction("exit");
    public static final GUIAction ABOUT = new GUIAction("about");

    /** 編集 */
    public static final GUIAction UNDO = new GUIAction("undo");

    /** プロジェクト */
    public static final GUIAction NEW_PROJECT     = new GUIAction("newProject");
    public static final GUIAction OPEN_PROJECT    = new GUIAction("openProject");
    public static final GUIAction SAVE_PROJECT    = new GUIAction("saveProject");
    public static final GUIAction SAVE_PROJECT_AS = new GUIAction("saveProjectAs");
    public static final GUIAction OPEN_LOG        = new GUIAction("openLog");
    public static final GUIAction SAVE_LOG        = new GUIAction("saveLog");
    public static final GUIAction SAVE_LOG_AS     = new GUIAction("saveLogAs");
    public static final GUIAction SAVE_CSV        = new GUIAction("saveCSV");

    /** オブジェクトの追加・削除 */
    public static final GUIAction ADD_ROBOT      = new GUIAction("addRobot");
    public static final GUIAction ADD_ENV        = new GUIAction("addEnvironment");
    public static final GUIAction REMOVE_OBJECT  = new GUIAction("removeObject");
    public static final GUIAction SET_CONTROLLER = new GUIAction("setController");

    /** 視点の操作 */
    public static final GUIAction ROOM_VIEW          = new GUIAction("roomView");
    public static final GUIAction WALK_VIEW          = new GUIAction("walkView");
    public static final GUIAction FRONT_VIEW         = new GUIAction("frontView");
    public static final GUIAction BACK_VIEW          = new GUIAction("backView");
    public static final GUIAction LEFT_VIEW          = new GUIAction("leftView");
    public static final GUIAction RIGHT_VIEW         = new GUIAction("rightView");
    public static final GUIAction TOP_VIEW           = new GUIAction("topView");
    public static final GUIAction BOTTOM_VIEW        = new GUIAction("bottomView");
    public static final GUIAction VIEW_PAN_MODE      = new GUIAction("panMode");
    public static final GUIAction VIEW_ZOOM_MODE     = new GUIAction("zoomMode");
    public static final GUIAction VIEW_ROTATION_MODE = new GUIAction("rotationMode");
    public static final GUIAction WIRE_FRAME         = new GUIAction("wireFrame");
    public static final GUIAction BG_COLOR      = new GUIAction("bgColor");
    public static final GUIAction CAPTURE      = new GUIAction("capture");
    
    /** オブジェクトの操作モード */
    public static final GUIAction OBJECT_TRANSLATION = new GUIAction("objectTranslation");
    public static final GUIAction OBJECT_ROTATION = new GUIAction("objectRotation");
    public static final GUIAction JOINT_ROTATION  = new GUIAction("jointRotation");
    public static final GUIAction FITTING_SRC     = new GUIAction("fittingSrc");
    public static final GUIAction FITTING_DEST    = new GUIAction("fittingDest");
    public static final GUIAction DO_FIT          = new GUIAction("doFit");
    public static final GUIAction INV_KINEMA_FROM = new GUIAction("invKinemaFrom");
    public static final GUIAction INV_KINEMA_TRANS = new GUIAction("invKinemaTrans");
    public static final GUIAction INV_KINEMA_ROT  = new GUIAction("invKinemaRot");
    public static final GUIAction OPERATION_DISABLE = new GUIAction("operationDisable");

    /** GUIモード切替え */
    public static final GUIAction START_SIMULATION = new GUIAction("startSimulation");
    public static final GUIAction STOP_SIMULATION = new GUIAction("stopSimulation");
    public static final GUIAction EDIT_MODE       = new GUIAction("editMode");
    public static final GUIAction SPLIT_MODE      = new GUIAction("splitMode");

    /** 再生モード切換え */
    public static final GUIAction PLAY   = new GUIAction("play");
    public static final GUIAction PAUSE  = new GUIAction("pause");
    public static final GUIAction STOP   = new GUIAction("stop");
    public static final GUIAction RECORD = new GUIAction("record");
    public static final GUIAction COLLISION = new GUIAction("collision");
    public static final GUIAction DELETE = new GUIAction("delete");

    /** シミュレーションツールバー */
    public static final GUIAction SLIDER_CHANGED = new GUIAction("sliderChanged");
    public static final GUIAction RATE_CHANGED   = new GUIAction("rateChanged");
    public static final GUIAction SET_IN_POINT   = new GUIAction("setInPoint");
    public static final GUIAction SET_OUT_POINT  = new GUIAction("setOutPoint");
    public static final GUIAction MOVIE_PLAYER   = new GUIAction("moviePlayer");

    //--------------------------------------------------------------------
    // Instance Valiables
    private String command_;
    private ActionListener listener_;

    //--------------------------------------------------------------------
    // Constructor
    public GUIAction(String command) {
/*
        super(
            MessageBundle.get("action.text." + command),
            IconProperties.get("icon." + command)
        );
*/

        command_ = command;
        putValue(SHORT_DESCRIPTION, MessageBundle.get("action.text." + command));
        putValue(SMALL_ICON, IconProperties.get("icon." + command));
    }

    public String getActionCommand() { return command_; }

    public void addActionListener(ActionListener listener) {
        listener_ = AWTEventMulticaster.add(listener_, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listener_ = AWTEventMulticaster.remove(listener_, listener);
    }

    public void actionPerformed(ActionEvent evt) {
        if (listener_ != null) {
            listener_.actionPerformed(
                new ActionEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    command_
                )
            );
        }
    }

    public void fireAction() {
        if (listener_ != null) {
            listener_.actionPerformed(
                new ActionEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    command_
                )
            );
        }
    }

    public String toString() {
        return (String)getValue(SHORT_DESCRIPTION);
    }
}

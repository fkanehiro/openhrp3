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
 * ViewHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.*;
import java.awt.event.*;

abstract class ViewHandler implements BehaviorHandler {
    protected static final int MOUSE_BUTTON_LEFT   = 0;
    protected static final int MOUSE_BUTTON_CENTER = 1;
    protected static final int MOUSE_BUTTON_RIGHT  = 2;

    protected static final int ROTATION_MODE    = 1;
    protected static final int TRANSLATION_MODE = 2;
    protected static final int ZOOM_MODE        = 3;

    protected int[] mode_;
    protected Map<String, int[]> modeMap_;

    ViewHandler() {
        modeMap_ = new HashMap<String, int[]>();

        modeMap_.put(
            "default_mode",
            new int[] { ROTATION_MODE, ZOOM_MODE, TRANSLATION_MODE }
        );

        modeMap_.put(
            "button_mode_rotation",
            new int[] { ROTATION_MODE, ZOOM_MODE, TRANSLATION_MODE }
        );

        modeMap_.put(
            "button_mode_translation",
            new int[] { TRANSLATION_MODE, ZOOM_MODE, TRANSLATION_MODE }
        );

        modeMap_.put(
            "button_mode_zoom",
            new int[] { ZOOM_MODE, ZOOM_MODE, TRANSLATION_MODE }
        );

        modeMap_.put(
            "ctrl_pressed",
            new int[] { ZOOM_MODE, ZOOM_MODE, ZOOM_MODE }
        );

        modeMap_.put(
            "alt_pressed",
            new int[] { TRANSLATION_MODE, TRANSLATION_MODE, TRANSLATION_MODE }
        );

        setMode("default_mode");
    }

    void setMode(String mode) {
        mode_ = (int[])modeMap_.get(mode);
    }

    protected int getMouseButtonMode(MouseEvent evt) {
        // マウスイベントからマウスボタンの識別子に変換。
/*
        switch (evt.getModifiers()) {
        case MouseEvent.BUTTON1_MASK:
            return MOUSE_BUTTON_LEFT;
        case MouseEvent.BUTTON2_MASK:
            return MOUSE_BUTTON_CENTER;
        case MouseEvent.BUTTON3_MASK:
            return MOUSE_BUTTON_RIGHT;
        default:
            return 3;
            //return MOUSE_BUTTON_RIGHT;
        }
*/

        if (evt.isMetaDown()) {
            return MOUSE_BUTTON_RIGHT;
        } else if (evt.isAltDown()) {
            return MOUSE_BUTTON_CENTER;
        } else {
            return MOUSE_BUTTON_LEFT;
        }
    }
}

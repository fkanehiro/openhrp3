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
 * GUIStatus.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 28 2001)
 */

package com.generalrobotix.ui.view.graph;

/**
 * GUIのモード管理を行うクラス。
 *
 * @history  1.0 (Wed Nov 28 2001)
 */
public class GUIStatus {
    //--------------------------------------------------------------------
    // 定数
    public static final int EDIT_MODE     = 1;
    public static final int EXEC_MODE     = 2;
    public static final int PLAYBACK_MODE = 3;

    private static int mode_;

    public static int getMode() {
        return mode_;
    }

    static void setMode(int mode) {
        mode_ = mode;
    }
}

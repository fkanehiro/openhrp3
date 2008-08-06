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

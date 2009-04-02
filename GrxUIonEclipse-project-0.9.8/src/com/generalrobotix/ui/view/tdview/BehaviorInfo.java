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
 * BehaviorInfo.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */
 
package com.generalrobotix.ui.view.tdview;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.sun.j3d.utils.picking.PickCanvas;

/**
 * BehaviorHandlerに情報を伝えるためのクラス
 */
class BehaviorInfo {
    //--------------------------------------------------------------------
    // インスタンス変数
    private boolean timerEnabled_;
    public GrxPluginManager manager_;

    final PickCanvas pickCanvas;
    //final TransformGroup tgView;
    final ThreeDDrawable drawable;

    BehaviorInfo(
        GrxPluginManager manager,
        PickCanvas pickCanvas,
        ThreeDDrawable drawable
        //TransformGroup tgView
    ) {
        manager_ = manager;
        this.pickCanvas = pickCanvas;
        this.drawable = drawable;
        //this.tgView = tgView;
    }

    void setTimerEnabled(boolean enabled) {
        timerEnabled_ = enabled;
    }

    boolean isTimerEnabled() {
        return timerEnabled_;
    }

    Manipulatable getManipulatable(String name) {
    	return (Manipulatable) manager_.<GrxModelItem>getSelectedItem(GrxModelItem.class, name);
        /*
        SimulationNode node = world_.getChild(name);
        if (node instanceof Manipulatable) {
            return (Manipulatable)node;
        } else {
            return null;
        }
        */
    }
}

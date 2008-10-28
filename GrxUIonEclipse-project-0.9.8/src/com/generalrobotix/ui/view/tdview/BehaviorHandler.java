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
 * BehaviorHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.event.*;

/**
 * ビヘイビアのためのイベントハンドラのインターフェース
 * IseBehaviorクラスから呼び出される。
 */
public interface BehaviorHandler {
    public void processPicking(MouseEvent evt, BehaviorInfo info);
    public void processStartDrag(MouseEvent evt, BehaviorInfo info);
    public void processDragOperation(MouseEvent evt, BehaviorInfo info);
    public void processReleased(MouseEvent evt, BehaviorInfo info);
    public void processTimerOperation(BehaviorInfo info);
}


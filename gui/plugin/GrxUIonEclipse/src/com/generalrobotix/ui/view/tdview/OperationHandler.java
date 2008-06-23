/**
 * OperationHandler.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import javax.media.j3d.TransformGroup;

abstract class OperationHandler implements BehaviorHandler {
    public abstract void disableHandler();
    public abstract void setPickTarget(TransformGroup tg, BehaviorInfo info);
}


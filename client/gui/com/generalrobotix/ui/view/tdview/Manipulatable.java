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
 * @(#)Manipulatable.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Jul 13 2001)
 */

package com.generalrobotix.ui.view.tdview;
import javax.media.j3d.*;

public interface Manipulatable {
    /**
     * TransformGroupの参照取得
     *
     * @return       シーングラフの参照
     */
    public TransformGroup getTransformGroupRoot();
    //public void changeAttribute(String name, String value);
}

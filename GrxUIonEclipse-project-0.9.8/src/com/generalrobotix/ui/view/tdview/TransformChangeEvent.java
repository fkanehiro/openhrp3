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
 * TransformChangeEvent.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.EventObject;
import javax.media.j3d.*;

@SuppressWarnings("serial")
public class TransformChangeEvent extends EventObject
{

    /**
     */
    // 内部に保持する TransformGroup
    private TransformGroup tgChanged = null;

    /**
     * コンストラクタ
     * @param  objSource
     * @param  tgChanged
     */
    public TransformChangeEvent(Object objSource,TransformGroup tgChanged)
    {
        super(objSource);
        this.tgChanged = tgChanged;
    }

    /**
     * トランスフォームグループ取得
     * @return  トランスフォームグループ
     */
    public TransformGroup getTransformGroup()
    {
        return tgChanged;
    }
}

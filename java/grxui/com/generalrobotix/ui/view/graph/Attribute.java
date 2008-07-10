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
 * Attribute.java
 *
 * @author  Kernel, Inc.
 * @version  1.0  (Mon Sep 17 2001)
 */

package com.generalrobotix.ui.view.graph;

public class Attribute {
    public static final int EDITABLE             = 0x0001;
    public static final int VISIBLE              = 0x0002;
    public static final int SAVE_REQUIRED        = 0x0004;
    public static final int RECORDABLE           = 0x0008;
    public static final int RECORD_REQUIRED      = 0x0010;
    public static final int MUST_RECORD          = 0x0020;
    public static final int RECORD_FLAG_LOCKED   = 0x0040;
    public static final int SAVE_FROM_WORLD      = 0x0080;
    public static final int SAVE_FLAGS_REQUIRED   = 0x0100;//¡úÀÆÆ£ÄÉ²Ã(2003/6/7)

    public final StringExchangeable  value;
    public int flag;

    public Attribute(StringExchangeable value, int flag) {
        this.value = value;
        this.flag = flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}

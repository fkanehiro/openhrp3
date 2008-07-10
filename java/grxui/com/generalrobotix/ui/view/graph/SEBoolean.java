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
 * SEFloat.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

public class SEBoolean extends SEEnumeration {
    Boolean value_;
    static String[] __s = {"false", "true"};
    
    
    public SEBoolean(String s) {
        super(__s, 0);
        fromString(s);
    };

    public SEBoolean() {
        super(__s, 0);
        value_ = new Boolean(false);
    };

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEBoolean(boolean value) {
        super(__s, _getSelect(value));
        value_ = new Boolean(value);
    };


    private static int _getSelect(boolean b) {
        return b ? 1 : 0;
    }

    /**
     * String値を取得
     *
     * @return   String値
     */
    public String toString() {
        return value_.toString();
    }

    /**
     * String値からObjectを取得
     *
     * @param  str  String値
     * @return      Object値
     */
    public Object fromString(String str) {
        value_ = Boolean.valueOf(str);
        selection_ = _getSelect(value_.booleanValue());
        return (Object)value_;
    }

    /**
     * Object値の設定
     *
     * @param  value  Object値
     */
    public void setValue(Object value) {
        value_ = (Boolean)value;
        selection_ = _getSelect(value_.booleanValue());
    }

    /**
     * String値の設定
     *
     * @param  str  String値
     */
    public void setValue(String str) {
        value_ = Boolean.valueOf(str);
        selection_ = _getSelect(value_.booleanValue());
    }

    /**
     * Object値の取得
     *
     * @return  Object値
     */
    public Object getValue() {
        return (Object)value_;
    }

    /**
     * boolean値の取得
     *
     * @return  boolean値
     */
    public boolean booleanValue() {
        return value_.booleanValue();
    }
};

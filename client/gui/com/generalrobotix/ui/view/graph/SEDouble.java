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
 * SEDouble.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.graph;

import java.text.DecimalFormat;

public class SEDouble implements StringExchangeable {
    Double value_;
    DecimalFormat df_;
    boolean isNaN_;

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEDouble(double value) {
        value_ = new Double(value);
        df_ = new DecimalFormat("0.####");
    };

    public SEDouble(String value) {
        if (value.equals("NaN") || value.equals("")) {
            isNaN_ = true;
            value_ = new Double(Double.NaN);
        } else {
            value_ = new Double(value);
        }
        df_ = new DecimalFormat("0.####");
    }

    /**
     * String値を取得
     *
     * @return  String値
     */
    public String toString() {
        if (isNaN_) {
            return "";
        } else {
            return df_.format(value_.doubleValue());
        }
    }

    /**
     * String値からObjectを取得
     * @param    str  String値
     * @return        Object値
     */
    public Object fromString(String str) {
        if (str.equals("NaN") || str.equals("")) {
            isNaN_ = true;
            value_ = new Double(Double.NaN);
        } else {
            value_ = new Double(str);
        }
        return (Object)value_;
    }

    /**
     * Object値の設定
     * @param    value  Object値
     */
    public void setValue(Object value) {
        value_ = (Double)value;
    }

    /**
     * String値の設定
     * @param    String値
     */
    public void setValue(String str) {
        value_ = new Double(str);
    }

    public void setValue(double value) {
        value_ = new Double(value);
    }

    /**
     * Object値の取得
     * @return Object値
     */
    public Object getValue() {
        return (Object)value_;
    }

    public double doubleValue() {
        return value_.doubleValue();
    }

    public float floatValue() {
        return value_.floatValue();
    }
}

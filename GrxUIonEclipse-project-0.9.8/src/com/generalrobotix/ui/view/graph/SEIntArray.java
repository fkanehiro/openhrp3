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
 * SEIntArray.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

import java.util.StringTokenizer;

public class SEIntArray implements StringExchangeable {
    Integer value_[];

    /**
     * コンストラクタ
     *
     * @param   size    サイズ
     */
    public SEIntArray(int size) {
        value_ = new Integer[size];
        for (int i = 0; i < size; i ++) {
            value_[i] = new Integer(0);
        }
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEIntArray(Integer value[]) {
        value_ = value;
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEIntArray(int value[]) {
        value_ = new Integer[value.length];
        for (int i = 0; i < value.length; i ++) {
            value_[i] = new Integer(value[i]);
        }
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEIntArray(String value) {
        StringTokenizer tokenizer = new StringTokenizer(value);
        value_ = new Integer[tokenizer.countTokens()];
        for (int i = 0; tokenizer.hasMoreTokens(); i ++) {
            value_[i] = new Integer(tokenizer.nextToken());
        }
    }

    public int size() {
        if (value_ == null) { return 0; }
        return value_.length;
    }


    /**
     * String値を取得
     *
     * @return   String値
     */
    public String toString() {
        String str;

        if (value_.length == 0) return new String();
        str = value_[0].toString();
        for (int i = 1; i < value_.length; i ++) {
            str += " " + value_[i].toString();
        }
        return str;
    }

    /**
     * String値からObjectを取得
     *
     * @param  str  String値
     * @return      Object値
     */
    public Object fromString(String str) {
        StringTokenizer token = new StringTokenizer(str);

        for (int i = 0; i < value_.length; i ++) {
            if (token.hasMoreTokens()) {
                value_[i] = new Integer(token.nextToken());
            } else {
                break;
            }
        }
        return (Object)value_;
    }

    /**
     * Object値の設定
     *
     * @param  value  Object値
     */
    public void setValue(Object value) {
        value_ = (Integer[])value;
    }

    /**
     * String値の設定
     *
     * @param  str  String値
     */
    public void setValue(String str) {
        StringTokenizer token = new StringTokenizer(str);

        for (int i = 0; i < value_.length; i ++) {
            if (token.hasMoreTokens()) {
                value_[i] = new Integer(token.nextToken());
            } else {
                break;
            }
        }
    }

    /**
     * Object値の取得
     *
     * @return  Object値
     */
    public Object getValue() {
        return (Object)value_;
    }
};

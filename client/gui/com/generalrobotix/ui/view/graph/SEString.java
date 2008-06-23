/**
 * SEString.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

public class SEString implements StringExchangeable {
    String value_;

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEString(String value) {
        value_ = value;
    };

    /**
     * String値を取得
     *
     * @return   String値
     */
    public String toString() {
        return value_;
    }

    /**
     * String値からObject値を取得
     *
     * @param  str  String値
     * @return      Object値
     */
    public Object fromString(String str) {
        value_ = str;
        return (Object)value_;
    }

    /**
     * Object値の設定
     *
     * @param  value  Object値
     */
    public void setValue(Object value) {
        value_ = (String)value;
    }

    /**
     * String値の設定
     *
     * @param  str  String値
     */
    public void setValue(String str) {
        value_ = str;
    }

    /**
     * Object値の取得
     *
     * @return    Object値
     */
    public Object getValue() {
        return (Object)value_;
    }
};

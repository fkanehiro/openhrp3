/**
 * SEInteger.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

public class SEInteger implements StringExchangeable {
    Integer value_;

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEInteger(int value) {
        value_ = new Integer(value);
    };

    public SEInteger(String value) {
        fromString(value);
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
        value_ = Integer.decode(str);
        return (Object)value_;
    }

    /**
     * Object値の設定
     *
     * @param  value  Object値
     */
    public void setValue(Object value) {
        value_ = (Integer)value;
    }

    /**
     * String値の設定
     *
     * @param  str  String値
     */
    public void setValue(String str) {
        value_ = Integer.decode(str);
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
     * int値の取得
     *
     * @return  int値
     */
    public int intValue() {
        return value_.intValue();
    }
};

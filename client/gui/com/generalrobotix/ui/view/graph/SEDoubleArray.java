/**
 * SEDoubleArray.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.graph;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

public class SEDoubleArray implements StringExchangeable {
    Double value_[];
    DecimalFormat df_;
    boolean isNaN_;

    /**
     * コンストラクタ
     *
     * @param   size    サイズ
     */
    public SEDoubleArray(int size) {
        value_ = new Double[size];
        for (int i = 0; i < size; i ++) {
            value_[i] = new Double(0.0);
        }
        df_ = new DecimalFormat("0.####");
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEDoubleArray(Double value[]) {
        value_ = value;
        df_ = new DecimalFormat("0.####");

        if (!_isValid(value_)) {
           throw new StringExchangeException();
        }
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEDoubleArray(double value[]) {
        value_ = new Double[value.length];
        for (int i = 0; i < value.length; i ++) {
            value_[i] = new Double(value[i]);
        }
        df_ = new DecimalFormat("0.####");
        if (!_isValid(value_)) {
           throw new StringExchangeException();
        }
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEDoubleArray(float value[]) {
        value_ = new Double[value.length];
        for (int i = 0; i < value.length; i ++) {
            value_[i] = new Double((double)value[i]);
        }
        df_ = new DecimalFormat("0.####");
        if (!_isValid(value_)) {
            throw new StringExchangeException();
        }
    }

    /**
     * コンストラクタ
     *
     * @param   value
     */
    public SEDoubleArray(String value) {
        StringTokenizer token = new StringTokenizer(value);

        value_ = new Double[token.countTokens()];
        for (int i = 0; i < value_.length; i ++) {
            String str = token.nextToken();
            if (str.equals("NaN") || str.equals("")) {
                isNaN_ = true;
                value_[i] = new Double(Double.NaN);
            } else {
                value_[i] = new Double(str);
                if (value_[i].isInfinite()) {
                   throw new StringExchangeException();
                }
            }
        }
        df_ = new DecimalFormat("0.####");
    }

    /**
     * String値を取得
     *
     * @return   String値
     */
    public String toString() {
        if (isNaN_) { return ""; }

        StringBuffer strBuf = new StringBuffer();

        if (value_.length == 0) return strBuf.toString();
        strBuf.append(df_.format(value_[0]));
        for (int i = 1; i < value_.length; i ++) {
            strBuf.append(" ");
            strBuf.append(df_.format(value_[i]));
        }
        return strBuf.toString();
    }

    /**
     * String値からObjectを取得
     *
     * @param    String値
     * @return   Object値
     */
    public Object fromString(String str) {
        setValue(str);
        return (Object)value_;
    }

    /**
     * Object値の設定
     *
     * @param  value  Object値
     */
    public void setValue(Object value) {
        isNaN_ = false;

        if (_isValid((Double[])value)) { 
            value_ = (Double[])value;
        } else {
            throw new StringExchangeException();
        }
    }

    public void setValue(double[] value) {
        isNaN_ = false;

        for (int i = 0; i < value.length; i ++) {
            if (Double.isNaN(value[i])) {
                isNaN_ = true;
            } else if (Double.isInfinite(value[i])) { 
                throw new StringExchangeException();
            }
            value_[i] = new Double(value[i]);
        }
    }

    /**
     * String値の設定
     *
     * @param  str  String値
     */
    public void setValue(String str) {
        if (str.equals("")) {
            isNaN_ = true;
            for (int i = 0; i < value_.length; i ++) {
                value_[i] = new Double(Double.NaN);
            }
            return;
        }

        StringTokenizer token = new StringTokenizer(str);

        isNaN_ = false;
        for (int i = 0; i < value_.length; i ++) {
            if (token.hasMoreTokens()) {
                String value = token.nextToken();
                if (value.equals("NaN")) {
                    isNaN_ = true;
                    value_[i] = new Double(Double.NaN);
                } else {
                    value_[i] = new Double(value);
                    if (value_[i].isInfinite()) {
                        throw new StringExchangeException();
                    }
                }
            } else {
                throw new StringExchangeException();
            }
        }
    }

    public void setValue(int index, double value) {
        value_[index] = new Double(value);
    }

    /**
     * 配列の個数を取得
     *
     * @return  配列の個数
     */
    public int size() {
        return value_.length;
    }

    /**
     * 配列要素の取得
     *
     * @return  Object値
     */
    public Object getValue() {
        return (Object)value_;
    }

    /**
     * 配列要素の取得
     *
     * @param   index  インデックス
     * @return  Double値
     */
    public Double getValue(int index) {
        if (value_.length <= index) return null;
        return value_[index];
    }

    /**
     * 配列要素の取得
     *
     * @param   index  インデックス
     * @return  double値
     */
    public double doubleValue(int index) {
        if (value_.length <= index) return 0.0;
        return value_[index].doubleValue();
    }

    /**
     * 配列要素の取得
     *
     * @param   index  インデックス
     * @return  float値
     */
    public float floatValue(int index) {
        if (value_.length <= index) return 0.0f;
        return value_[index].floatValue();
    }

    /**
     * 配列要素の取得
     *
     * @return  Double値
     */
    public Double[] getDoubleArray() {
        return value_;
    }

    public void get(double[] value) {
        for (int i = 0; i < value_.length; i ++) {
            value[i] = value_[i].doubleValue();
        }
    }

    public void get(float[] value) {
        for (int i = 0; i < value_.length; i ++) {
            value[i] = value_[i].floatValue();
        }
    }

    private boolean _isValid(Double[] value) {
        for (int i = 0; i < value.length; i ++) {
            if (value[i].isNaN()) {
                isNaN_ = true;
                return true; 
            } else if (value[i].isInfinite()) {
                return false;
            }
        }
        return true;
    }
}

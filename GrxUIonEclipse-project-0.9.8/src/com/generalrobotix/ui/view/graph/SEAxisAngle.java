/**
 * SEAxisAngle
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/29)
 */

package com.generalrobotix.ui.view.graph;

import java.util.StringTokenizer;
import java.text.DecimalFormat;
import javax.vecmath.*;

public class SEAxisAngle implements StringExchangeable {
    AxisAngle4d value_;
    DecimalFormat df_;
    boolean isNaN_;

    public SEAxisAngle() {
        value_ = new AxisAngle4d();
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(AxisAngle4d value) {
        value_ = new AxisAngle4d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(Double[] value) {
        value_ = new AxisAngle4d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(double[] value) {
        value_ = new AxisAngle4d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(float[] value) {
        value_ = new AxisAngle4d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(double x, double y, double z, double angle) {
        value_ = new AxisAngle4d();
        setValue(x, y, z, angle);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(Quat4d value) {
        value_ = new AxisAngle4d();
        value_.set(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(String value) {
        value_ = new AxisAngle4d();
        fromString(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SEAxisAngle(DecimalFormat df) {
        df_ = df;
        value_ = new AxisAngle4d();
    }

    public void setValue(Double value[]) {
        if (value.length != 4) throw new StringExchangeException();
        isNaN_ = false;
        for (int i = 0; i < 4; i ++) {
            if (value[i].isNaN()) {
                isNaN_ = true;
            } else if (value[i].isInfinite()) {
                throw new StringExchangeException();
            }
        }

        value_.set(
            value[0].doubleValue(),
            value[1].doubleValue(),
            value[2].doubleValue(),
            value[3].doubleValue()
        );

        Quat4d quat = new Quat4d();
        quat.set(value_);
        value_.set(quat);
    }

    public void setValue(double value[]) {
        if (value.length != 4) throw new StringExchangeException();
        isNaN_ = false;
        for (int i = 0; i < 4; i ++) {
            if (Double.isNaN(value[i])) {
                isNaN_ = true;
            } else if (Double.isInfinite(value[i])) {
                throw new StringExchangeException();
            }
        }

        value_.set(value);

        Quat4d quat = new Quat4d();
        quat.set(value_);
        value_.set(quat);
    }

    public void setValue(float value[]) {
        if (value.length != 4) throw new StringExchangeException();
        isNaN_ = false;
        for (int i = 0; i < 4; i ++) {
            if (Double.isNaN(value[i])) {
                isNaN_ = true;
            } else if (Double.isInfinite(value[i])) {
                throw new StringExchangeException();
            }
        }
        value_.set(
            (double)value[0],
            (double)value[1],
            (double)value[2],
            (double)value[3]
        );

        Quat4d quat = new Quat4d();
        quat.set(value_);
        value_.set(quat);
    }

    public void setValue(Quat4d value) {
        isNaN_ = false;
        value_.set(value);
    }

    public String toString() {
        if (isNaN_) { return ""; }

        StringBuffer strBuf = new StringBuffer();

        strBuf.append(df_.format(value_.x));
        strBuf.append(" ");
        strBuf.append(df_.format(value_.y));
        strBuf.append(" ");
        strBuf.append(df_.format(value_.z));
        strBuf.append(" ");
        strBuf.append(df_.format(value_.angle));

        return strBuf.toString();
    }

    public Object fromString(String str) {
        if (str.equals("")) {
            isNaN_ = true;
            return null;
        }

        StringTokenizer token = new StringTokenizer(str);

        Double[] doubleArray = new Double[4];

        isNaN_ = false;

        for (int i = 0; i < 4; i ++) {
            if (token.hasMoreTokens()) {
                String value = token.nextToken();
                if (value.equals("NaN")) {
                    isNaN_ = true;
                    doubleArray[i] = new Double(Double.NaN);
                } else {
                    try {
                        doubleArray[i] = new Double(value);
                    } catch (NumberFormatException ex) {
                        throw new StringExchangeException();
                    }

                    if (doubleArray[i].isNaN()) {
                        isNaN_ = true;
                    } else if (doubleArray[i].isInfinite()) {
                        throw new StringExchangeException(); 
                    }
                }
            } else {
                throw new StringExchangeException();
            }
        }

        if (token.hasMoreTokens()) {
            throw new StringExchangeException();
        }

        value_.set(
            doubleArray[0].doubleValue(),
            doubleArray[1].doubleValue(),
            doubleArray[2].doubleValue(),
            doubleArray[3].doubleValue()
        );

        Quat4d quat = new Quat4d();
        quat.set(value_);
        value_.set(quat);

        return (Object)value_;
    }

    public void setValue(Object value) {
        Quat4d quat = new Quat4d();
        quat.set((AxisAngle4d)value);
        value_.set(quat);
    }

    public void setValue(double x, double y, double z, double angle) {
        value_.set(x, y, z, angle);
        Quat4d quat = new Quat4d();
        quat.set(value_);
        value_.set(quat);
    }

    public void setValue(String str) {
        fromString(str);
    }

    public Object getValue() {
        return (Object)value_;
    }

    public double getX() {
        return value_.x;
    }
 
    public double getY() {
        return value_.y;
    }

    public double getZ() {
        return value_.z;
    }

    public double getAngle() {
        return value_.angle;
    }

    public boolean isNaN() {
        return isNaN_;
    }
}

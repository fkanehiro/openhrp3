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
 * SETranslation
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/29)
 */

package com.generalrobotix.ui.view.graph;

import java.util.StringTokenizer;
import java.text.DecimalFormat;
import javax.vecmath.*;

public class SETranslation implements StringExchangeable {
    Vector3d value_;
    DecimalFormat df_;
    boolean isNaN_;

    public SETranslation() {
        value_ = new Vector3d();
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(Vector3d value) {
        value_ = new Vector3d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(Double[] value) {
        value_ = new Vector3d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(double[] value) {
        value_ = new Vector3d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(float[] value) {
        value_ = new Vector3d();
        setValue(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(double x, double y, double z) {
        value_ = new Vector3d();
        setValue(x, y, z);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(String value) {
        value_ = new Vector3d();
        fromString(value);
        df_ = new DecimalFormat("0.####");  // default format
    }

    public SETranslation(DecimalFormat df) {
        df_ = df;
        value_ = new Vector3d();
    }

    public void setValue(Double value[]) {
        if (value.length != 3) throw new StringExchangeException();
        isNaN_ = false;
        for (int i = 0; i < 3; i ++) {
            if (value[i].isNaN()) {
                isNaN_ = true;
            } else if (value[i].isInfinite()) {
                throw new StringExchangeException();
            }
        }

        value_.set(
            value[0].doubleValue(),
            value[1].doubleValue(),
            value[2].doubleValue()
        );
    }

    public void setValue(double value[]) {
        if (value.length != 3) throw new StringExchangeException();
        isNaN_ = false;
        for (int i = 0; i < 3; i ++) {
            if (Double.isNaN(value[i])) {
                isNaN_ = true;
            } else if (Double.isInfinite(value[i])) {
                throw new StringExchangeException();
            }
        }

        value_.set(value);
    }

    public void setValue(float value[]) {
        if (value.length != 3) throw new StringExchangeException();
        isNaN_ = false;
        for (int i = 0; i < 3; i ++) {
            if (Double.isNaN(value[i])) {
                isNaN_ = true;
            } else if (Double.isInfinite(value[i])) {
                throw new StringExchangeException();
            }
        }
        value_.set((double)value[0], (double)value[1], (double)value[2]);
    }

    public void setValue(double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)){
	  isNaN_ = true;
	}else{
	  isNaN_ = false;
	}
        value_.set(x, y, z);
    }

    public String toString() {
        if (isNaN_) { return ""; }
        StringBuffer strBuf = new StringBuffer();

        strBuf.append(df_.format(value_.x));
        strBuf.append(" ");
        strBuf.append(df_.format(value_.y));
        strBuf.append(" ");
        strBuf.append(df_.format(value_.z));

        return strBuf.toString();
    }

    public Object fromString(String str) {
        if (str.equals("")) {
            isNaN_ = true;
            return null;
        }

        StringTokenizer token = new StringTokenizer(str);

        Double[] doubleArray = new Double[3];
        isNaN_ = false;

        for (int i = 0; i < 3; i ++) {
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
            doubleArray[2].doubleValue()
        );

        return (Object)value_;
    }

    public void setValue(Object value) {
        value_.set((Vector3d)value);
    }

    public void setValue(Vector3d value) {
        value_.set(value);
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

    public boolean isNaN() { return isNaN_; }
}

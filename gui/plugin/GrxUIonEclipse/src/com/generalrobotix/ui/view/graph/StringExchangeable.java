/**
 * StringExchangeable.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

public interface StringExchangeable {
    public String toString();
    public Object fromString(String str);
    public void setValue(Object value);
    public void setValue(String str);
    public Object getValue();
};

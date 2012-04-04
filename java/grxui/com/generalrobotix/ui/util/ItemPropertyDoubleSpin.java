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
 * SEDoubleTextWithSpin.java
 *
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.view.graph.SEDouble;
import com.generalrobotix.ui.view.graph.SpinControl;
import com.generalrobotix.ui.view.graph.SpinListener;

@SuppressWarnings("serial")
public class ItemPropertyDoubleSpin extends JPanel implements SpinListener {
    private SEDouble value_;
    private double max_;
    private double min_;
    private double step_;
    private JTextField text_;
    
    private GrxBaseItem item_;
    private String key_;
    
    public ItemPropertyDoubleSpin(double min,double max,double step) {
        value_ = new SEDouble(0);
        
        max_ = max;
        min_ = min;
        step_ = step;
        SpinControl spin = new SpinControl(this);
        text_ = new JTextField();
        text_.addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   setValue(text_.getText());
               }
            }
        );
        text_.addFocusListener(
            new FocusListener() {
                public void focusGained(FocusEvent e) {
                }
                public void focusLost(FocusEvent e) {
                   setValue(text_.getText());
                }
            }
        );

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER,text_);
        add(BorderLayout.EAST,spin);
        
        setValue((max+min)/2);
    }

    public void up() {
        setValue(text_.getText());
        double v = value_.doubleValue() + step_;
        setValue(v);
    }

    public void down() {
        setValue(text_.getText());
        double v = value_.doubleValue() - step_;
        setValue(v);
    }
    
    public double getValue() {
        setValue(text_.getText());
        return value_.doubleValue();
    }
    
    public void setValue(String s) {
    	double v = 0;
    	try {
    		v = new SEDouble(s).doubleValue();
    	} catch (Exception e) {
    		v = value_.doubleValue();
    	}
        
        setValue(v);
    }
    
    public void setValue(double v) {
        if (isOk(v)) {
            value_.setValue(new Double(v));
        }

        text_.setText(value_.toString());

        if (item_ != null && key_ != null)
            item_.setDbl(key_, v);
    }

    public boolean isOk(double v) {
        return (min_ <= v && v<=max_);
    }

    public void setEnabled(boolean flag) {
        super.setEnabled(flag);
        Component[] cmps = getComponents();
        for (int i = 0; i < cmps.length; i++) {
            cmps[i].setEnabled(flag);
        }
    }
    
    public void setItem(GrxBaseItem item, String key) {
        item_ = item;
    	key_ = key;
    }
}

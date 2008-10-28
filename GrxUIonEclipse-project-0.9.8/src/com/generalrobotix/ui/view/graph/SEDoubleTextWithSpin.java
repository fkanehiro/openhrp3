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
package com.generalrobotix.ui.view.graph;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class SEDoubleTextWithSpin extends JPanel implements SpinListener {
    SEDouble value_;
    double max_;
    double min_;
    double step_;
    JTextField text_;
    
    public SEDoubleTextWithSpin(double min,double max,double step) {
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
    
    public SEDouble getValue() {
        setValue(text_.getText());
        return value_;
    }
    
    public void setValue(String s) {
        double v = new SEDouble(s).doubleValue();
        setValue(v);
    }
    
    public void setValue(double v) {
        if (isOk(v)) {
            value_.setValue(new Double(v));
        }

        text_.setText(value_.toString());
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
}

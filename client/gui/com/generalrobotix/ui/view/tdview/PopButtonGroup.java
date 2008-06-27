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
 * PopButtonGroup.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Thu Dec 06 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * 選択されているボタンをもう一度押すとボタンがポップするボタングループ
 */
public class PopButtonGroup extends ButtonGroup implements ActionListener {
    //--------------------------------------------------------------------
    // Instance variables
    private AbstractButton prevButton_;
    private JToggleButton hidden_;

    //--------------------------------------------------------------------
    // Constructor
    public PopButtonGroup() {
        hidden_ = new JToggleButton();
        hidden_.setVisible(false);
        super.add(hidden_);
    }

    //--------------------------------------------------------------------
    // Public methods
    public void addActionListener(ActionListener listener) {
        hidden_.addActionListener(listener);
    }

    public void removeActionListener(ActionListener listener) {
        hidden_.removeActionListener(listener);
    }

    public void selectNone() {
        hidden_.setSelected(true);
    }

    //--------------------------------------------------------------------
    // Override
    public void add(AbstractButton button) {
        button.addActionListener(this);
        super.add(button);
    }

    public void remove(AbstractButton button) {
        button.removeActionListener(this);
        super.remove(button);
    }

    //--------------------------------------------------------------------
    // Implementation of ActionListener
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == prevButton_) {
            //hidden_.setSelected(true);
            SwingUtilities.invokeLater(
                new Runnable() {
                     public void run() {
                         hidden_.doClick();
                         prevButton_ = hidden_;
                     }
                }
            );
        } else {
            prevButton_ = (AbstractButton)evt.getSource();
        }
    }
}

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
 * ComboBoxDialog.java
 *
 * @author  Kernel, Inc.
 * @version  2.0 (Fri Dec 07 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import javax.swing.*;


@SuppressWarnings("serial")
public class ComboBoxDialog extends AlertBox {
    JComboBox combo_;

    public ComboBoxDialog(Frame owner, String caption, String message, Object[] item) {
        super(owner, caption, JOptionPane.QUESTION_MESSAGE, OK_CANCEL_TYPE);
        setTitle("InputDialog");
        setCaption(caption);
        setMessage(message);
        _init(item);
    }

    private void _init(Object[] item) {
        combo_ = new JComboBox(item);
        setInputAreaWidth(300);
        addInputComponent(
            null,
            combo_,
            ModalDialog.MULTILINE_CAPTION,
            true
        );
    }

    public Object showComboBoxDialog() {
        int res = showModalDialog();
        if (res == ModalDialog.OK_BUTTON) {
            return combo_.getSelectedItem();
        }
        return null;
    }

}

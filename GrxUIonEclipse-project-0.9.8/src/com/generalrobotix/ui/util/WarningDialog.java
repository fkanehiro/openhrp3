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
 * WarningDialog.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import javax.swing.*;


/**
 * 警告ダイアログ
 */
@SuppressWarnings("serial")
public class WarningDialog extends AlertBox {
    public WarningDialog(Frame owner, String caption, String message) {
        super(owner, caption, JOptionPane.WARNING_MESSAGE, OK_TYPE);
        setTitle("Warning");
        setCaption(caption);
        setMessage(message);
    }

    public WarningDialog(Dialog owner, String caption, String message) {
        super(owner, caption, JOptionPane.WARNING_MESSAGE, OK_TYPE);
        setTitle("Warning");
        setCaption(caption);
        setMessage(message);
    }
}

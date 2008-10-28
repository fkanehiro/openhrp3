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
 * ConfirmDialog.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import javax.swing.*;


/**
 * 確認ダイアログボックス
 */
@SuppressWarnings("serial")
public class ConfirmDialog extends AlertBox {
    public ConfirmDialog(Frame owner, String caption, String message) {
        super(owner, caption, JOptionPane.QUESTION_MESSAGE, YES_NO_TYPE);
        setTitle(MessageBundle.get("dialog.comfirm.title"));
        setCaption(caption);
        setMessage(message);
    }

    public ConfirmDialog(Frame owner, String caption, String message, int type) {
        super(owner, caption, JOptionPane.QUESTION_MESSAGE, type);
        setTitle(MessageBundle.get("dialog.comfirm.title"));
        setCaption(caption);
        setMessage(message);
    }
}

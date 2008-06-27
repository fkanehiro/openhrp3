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
 * MessageDialog.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import javax.swing.*;


/**
 * メッセージボックス
 */
@SuppressWarnings("serial")
public class MessageDialog extends AlertBox {
    public MessageDialog(Frame owner, String caption, String message) {
        super(owner, caption, JOptionPane.INFORMATION_MESSAGE, OK_TYPE);
        setTitle(MessageBundle.get("dialog.message.title"));
        setCaption(caption);
        setMessage(message);
    }

    public MessageDialog(Dialog owner, String caption, String message) {
        super(owner, caption, JOptionPane.INFORMATION_MESSAGE, OK_TYPE);
        setTitle(MessageBundle.get("dialog.message.title"));
        setCaption(caption);
        setMessage(message);
    }

    public MessageDialog(Frame owner, String caption, String message, int type) {
        super(owner, caption, JOptionPane.INFORMATION_MESSAGE, type);
        setTitle(MessageBundle.get("dialog.message.title"));
        setCaption(caption);
        setMessage(message);
    }

    public MessageDialog(Dialog owner, String caption, String message, int type) {
        super(owner, caption, JOptionPane.INFORMATION_MESSAGE, type);
        setTitle(MessageBundle.get("dialog.message.title"));
        setCaption(caption);
        setMessage(message);
    }
}

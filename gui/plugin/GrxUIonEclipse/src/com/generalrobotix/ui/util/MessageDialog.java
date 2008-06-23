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

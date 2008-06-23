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

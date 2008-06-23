/**
 * ErrorDialog.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Sun Dec 09 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import javax.swing.*;


/**
 * エラーダイアログ
 */
@SuppressWarnings("serial")
public class ErrorDialog extends AlertBox {
    public ErrorDialog(Frame owner, String caption, String message) {
        super(owner, caption, JOptionPane.ERROR_MESSAGE, OK_BUTTON);
        setTitle(MessageBundle.get("dialog.error.title"));
        setCaption(caption);
        setMessage(message);
    }

    public ErrorDialog(Dialog owner, String caption, String message) {
        super(owner, caption, JOptionPane.ERROR_MESSAGE, OK_BUTTON);
        setTitle(MessageBundle.get("dialog.error.title"));
        setCaption(caption);
        setMessage(message);
    }
}

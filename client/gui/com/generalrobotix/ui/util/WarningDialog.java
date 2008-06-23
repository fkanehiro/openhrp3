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

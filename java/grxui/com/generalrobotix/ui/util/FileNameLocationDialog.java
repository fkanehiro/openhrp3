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
 * FileNameLocationDialog.java
 *
 * @author  Kernel, Inc.
 * @version  2.0 (Sat Dec 08 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import javax.swing.*;



@SuppressWarnings("serial")
public class FileNameLocationDialog extends ModalDialog {
    private JTextField nameField_;
    private FileInput fileInput_;

    public FileNameLocationDialog(Frame owner, String title, String message, String[] suffix,
				  String path) {
        super(owner, title, message, OK_CANCEL_TYPE);

        nameField_ = new JTextField();
        fileInput_ = new FileInput(suffix, path);

        addInputComponent(MessageBundle.get("dialog.file.objectname"), nameField_, MULTILINE_CAPTION, true);
        addInputComponent(MessageBundle.get("dialog.file.filename"), fileInput_, MULTILINE_CAPTION, true);
        setInputAreaWidth(480);
    }

     public void setText(String text) {
         fileInput_.setText(text);
     }

     public String getFileName() {
         return fileInput_.getFileName();
     }

     public void setNameFieldText(String text) {
         nameField_.setText(text);
     }

     public String getName() {
         return nameField_.getText();
     }
}

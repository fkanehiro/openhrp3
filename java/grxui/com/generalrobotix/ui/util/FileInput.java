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
 * FileInput.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.filechooser.*;



@SuppressWarnings("serial")
public class FileInput extends JPanel {
    private JTextField text_;
    private String[] fileFilter_;
    private String basedir_;

    static {
        UIManager.put("FileChooser.cancelButtonText",            MessageBundle.get("FileChooser.cancelButtonText"));
        UIManager.put("FileChooser.cancelButtonToolTipText",     MessageBundle.get("FileChooser.cancelButtonToolTipText"));
        UIManager.put("FileChooser.acceptAllFileFilterText",     MessageBundle.get("FileChooser.acceptAllFileFilterText"));
        UIManager.put("FileChooser.filesOfTypeLabelText",        MessageBundle.get("FileChooser.filesOfTypeLabelText"));
        UIManager.put("FileChooser.fileNameLabelText",           MessageBundle.get("FileChooser.fileNameLabelText"));
        UIManager.put("FileChooser.lookInLabelText",             MessageBundle.get("FileChooser.lookInLabelText"));
        UIManager.put("FileChooser.upFolderToolTipText",         MessageBundle.get("FileChooser.upFolderToolTipText"));
        UIManager.put("FileChooser.homeFolderToolTipText",       MessageBundle.get("FileChooser.homeFolderToolTipText"));
        UIManager.put("FileChooser.newFolderToolTipText",        MessageBundle.get("FileChooser.newFolderToolTipText"));
        UIManager.put("FileChooser.listViewButtonToolTipText",   MessageBundle.get("FileChooser.listViewButtonToolTipText"));
        UIManager.put("FileChooser.detailsViewButtonToolTipText",MessageBundle.get("FileChooser.detailsViewButtonToolTipText"));
    }                                                            
                                                                 
    public FileInput(String[] filter, String dir) {
        fileFilter_ = filter;
        text_ = new JTextField();
	basedir_ = dir;
        JButton button = new JButton(MessageBundle.get("dialog.fileinput.filechoose"));
        button.addActionListener(
            new ActionListener() { 
                public void actionPerformed(ActionEvent evt) {
                    JFileChooser chooser = new JFileChooser(basedir_);
                    for (int i = 0; i < fileFilter_.length; i ++) {
                        chooser.addChoosableFileFilter(createFilter(fileFilter_[i]));
                    }
                    
                    //chooser.setControlButtonsAreShown(false);
                    chooser.setApproveButtonMnemonic('o');
                    chooser.setApproveButtonToolTipText(MessageBundle.get("dialog.fileopen.title"));
                    int result = chooser.showDialog(FileInput.this, MessageBundle.get("dialog.fileopen.title"));
                    if (result == JFileChooser.APPROVE_OPTION) {
                        text_.setText(chooser.getSelectedFile().getPath());
                    }
                }
            }
        );

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(text_);
        add(Box.createHorizontalStrut(12));
        add(button);
    }

    public String getFileName() {
        return text_.getText().trim();
    }

    public void setText(String text) {
        text_.setText(text);
    }

    public FileFilter createFilter(final String filter) {
        return new FileFilter() {
            public boolean accept(java.io.File fileobj ) {
                if (fileobj.isDirectory()) {
                    return true;
                }

                String extension = "";

                if (fileobj.getPath().lastIndexOf('.') > 0) {
                    extension =
                        fileobj.getPath().substring(
                            fileobj.getPath().lastIndexOf('.') + 1
                        ).toLowerCase();
                }

                return extension.equals(filter);

                /*
                if (extension != "") {
                    return extension.equals(filter);
                } else {
                    return fileobj.isDirectory();
                }
                */
            }

            public String getDescription() {
                return filter + " files (*." + filter + ")";
            }
        };
    }

}

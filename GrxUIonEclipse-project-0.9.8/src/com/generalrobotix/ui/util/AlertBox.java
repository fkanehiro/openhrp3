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
 * AlertBox.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public abstract class AlertBox extends ModalDialog {
    //--------------------------------------------------------------------
    protected Object[] options_;
    protected JLabel caption_;
    //protected JLabel message_;
    JTextArea message_;
    protected String title_;

    private int messageType_;
    private JDialog dialog_;

    //--------------------------------------------------------------------
    public AlertBox(Frame owner, String title, int messageType, int buttonType) {
        owner_ = owner;
        _init(title, messageType, buttonType);
    }
    public AlertBox(Dialog owner, String title, int messageType, int buttonType) {
        owner_ = owner;
        _init(title, messageType, buttonType);
    }

    private void _init(String title, int messageType, int buttonType) {
        title_ = title;
        messageType_ = messageType;

        caption_ = new JLabel("");
        //caption_.setForeground(Color.black);
        //caption_.setBorder(new EmptyBorder(12, 12, 1, 0));

        message_ = new JTextArea();
        message_.setOpaque(false);
        message_.setEditable(false);
        message_.setForeground(caption_.getForeground());
        //message_ = new JLabel("");
        //message_.setBorder(new EmptyBorder(12, 12, 1, 0));

        inputArea_ = new JPanel();
        inputArea_.setLayout(new GridBagLayout());
        //inputArea_.setBorder(new EmptyBorder(0, 0, 0, 0));
        gbc_ = new GridBagConstraints();

        listener_ = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                InnerButton button = (InnerButton)evt.getSource();
                buttonType_ = button.getButtonType();
                buttonCommand_ = evt.getActionCommand();
                SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            dispose();
                        }
                    }
                );
            }
        };

        InnerButton okButton;
        switch (buttonType) {
        case NONE_TYPE:
            break;
        case OK_TYPE:
            okButton = new InnerButton(OK_BUTTON);
            options_ = new Object[] { okButton };
            //getRootPane().setDefaultButton(okButton);
            break;
        case OK_CANCEL_TYPE:
            okButton = new InnerButton(OK_BUTTON);
            options_ = new Object[] { okButton, new InnerButton(CANCEL_BUTTON) };
            //getRootPane().setDefaultButton(okButton);
            break;
        case YES_NO_TYPE:
            options_ = new Object[] {
                new InnerButton(YES_BUTTON),
                new InnerButton(NO_BUTTON)
            };
            break;
        case YES_NO_CANCEL_TYPE:
            options_ = new Object[] {
                new InnerButton(YES_BUTTON),
                new InnerButton(NO_BUTTON),
                new InnerButton(CANCEL_BUTTON)
            };
            break;
        }

        mainPanel_ = new JPanel();
        mainPanel_.setLayout(new BorderLayout());
        mainPanel_.add(caption_, BorderLayout.NORTH);
        mainPanel_.add(message_, BorderLayout.CENTER);
        mainPanel_.add(inputArea_, BorderLayout.SOUTH);
    }

    public void show() {
        JOptionPane optionPane = new JOptionPane(
            mainPanel_,
            messageType_,
            JOptionPane.DEFAULT_OPTION,
            null,
            options_,
            options_[0]
        );

        dialog_ = optionPane.createDialog(owner_, title_);
        dialog_.setVisible(true);
    }
    
    public void dispose() {
        dialog_.dispose();
    }
    public void setCaption(String caption) {
        caption_.setText(caption);
    }
    public void setTitle(String title) {
        title_ = title;
    }
    public void setMessage(String message) {
        message_.setText(message);
    }

}

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
 * ModalDialog.java
 *
 * @author  Kernel, Inc.
 * @version 1.0 (Wed Nov 21 2001)
 */

package com.generalrobotix.ui.util;

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;


/**
 * モーダルダイアログのスーパークラス
 */
@SuppressWarnings("serial")
public class ModalDialog extends JDialog {
    //--------------------------------------------------------------------
    // 定数

    // ボタンタイプ
    public static final int NONE_TYPE          = 0;
    public static final int OK_TYPE            = 1;
    public static final int OK_CANCEL_TYPE     = 2;
    public static final int YES_NO_TYPE        = 3;
    public static final int YES_NO_CANCEL_TYPE = 4;

    // キャプションの配置
    public static final int INLINE_CAPTION     = 1;
    public static final int MULTILINE_CAPTION  = 2;

    // ボタンの識別子
    public static final int CUSTOM_BUTTON = 0;
    public static final int OK_BUTTON     = 1;
    public static final int CANCEL_BUTTON = 2;
    public static final int YES_BUTTON    = 3;
    public static final int NO_BUTTON     = 4;
    public static final int CLOSE_BUTTON  = 5;

    // コンポーネント配置のための定数
    private static final int BUTTON_GAP  =  5;
    private static final int CAPTION_GAP = 11;

    //--------------------------------------------------------------------
    // インスタンス変数
    private int width_;

    protected int buttonType_;
    protected String buttonCommand_;
    protected boolean closeButtonEnabled_ = true;

    protected ActionListener listener_;
    protected Window owner_;
    protected JPanel inputArea_;
    protected JPanel buttonArea_;
    protected JPanel mainPanel_;
    protected GridBagConstraints gbc_;

    //--------------------------------------------------------------------
    // コンストラクタ
    public ModalDialog() {}

    public ModalDialog(
        Frame owner,
        String title,
        String message,
        int buttonType
    ) {
        super(owner, title, true);
        owner_ = owner;
        _init(title, message, buttonType);
    }

    public ModalDialog(
        Dialog owner,
        String title,
        String message,
        int buttonType
    ) {
        super(owner, title, true);
        owner_ = owner;
        _init(title, message, buttonType);
    }

    private void _init(String title, String message, int buttonType) {
        JLabel messageLabel = new JLabel(message);
        //messageLabel.setForeground(Color.black);

        inputArea_ = new JPanel();
        inputArea_.setLayout(new GridBagLayout());
        inputArea_.setBorder(new EmptyBorder(6, 12, 0, 0));
        gbc_ = new GridBagConstraints();

        listener_ = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                InnerButton button = (InnerButton)evt.getSource();
                buttonType_ = button.getButtonType();
                buttonCommand_ = evt.getActionCommand();
                SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            ModalDialog.this.dispose();
                        }
                    }
                );
            }
        };

        buttonArea_ = new JPanel();
        buttonArea_.setBorder(new EmptyBorder(0, 0, 11, 11));
        buttonArea_.setLayout(new BoxLayout(buttonArea_, BoxLayout.X_AXIS));
        buttonArea_.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonArea_.add(Box.createHorizontalGlue());

        InnerButton okButton;
        switch (buttonType) {
        case NONE_TYPE:
            break;
        case OK_TYPE:
            okButton = new InnerButton(OK_BUTTON);
            getRootPane().setDefaultButton(okButton);
            buttonArea_.add(okButton);
            break;
        case OK_CANCEL_TYPE:
            okButton = new InnerButton(OK_BUTTON);
            getRootPane().setDefaultButton(okButton);
            buttonArea_.add(okButton);
            buttonArea_.add(Box.createHorizontalStrut(BUTTON_GAP));
            buttonArea_.add(new InnerButton(CANCEL_BUTTON));
            break;
        case YES_NO_TYPE:
            buttonArea_.add(new InnerButton(YES_BUTTON));
            buttonArea_.add(Box.createHorizontalStrut(BUTTON_GAP));
            buttonArea_.add(new InnerButton(NO_BUTTON));
            break;
        case YES_NO_CANCEL_TYPE:
            buttonArea_.add(new InnerButton(YES_BUTTON));
            buttonArea_.add(Box.createHorizontalStrut(BUTTON_GAP));
            buttonArea_.add(new InnerButton(NO_BUTTON));
            buttonArea_.add(Box.createHorizontalStrut(BUTTON_GAP));
            buttonArea_.add(new InnerButton(CANCEL_BUTTON));
            break;
        }

        mainPanel_ = new JPanel();
        mainPanel_.setLayout(new BorderLayout());
        mainPanel_.setBorder(new EmptyBorder(12, 12, 17, 11));
        mainPanel_.add(messageLabel, BorderLayout.NORTH);
        mainPanel_.add(inputArea_, BorderLayout.CENTER);

        getContentPane().add(mainPanel_, BorderLayout.CENTER);
        getContentPane().add(buttonArea_, BorderLayout.SOUTH);
        setResizable(false);
    }

    //--------------------------------------------------------------------
    // 公開メソッド
    public void setInputAreaWidth(int width) {
        width_ = width;
        gbc_.gridwidth = GridBagConstraints.REMAINDER;
        gbc_.fill = GridBagConstraints.HORIZONTAL;
        inputArea_.add(Box.createHorizontalStrut(width_), gbc_);
        
    }

    public int getInputAreaWidth() {
        return width_;
    }

    /** 入力コンポーネントの追加 */
    public void addInputComponent(
        String caption,
        Component component,
        int location,
        boolean fill
    ) {
        gbc_.gridwidth = GridBagConstraints.REMAINDER;
        gbc_.fill = GridBagConstraints.HORIZONTAL;
        inputArea_.add(Box.createVerticalStrut(11), gbc_);

        gbc_.anchor = GridBagConstraints.NORTHWEST;
        gbc_.fill = GridBagConstraints.NONE;

        JLabel label;

        if (caption == null || caption.equals("")) {
            label = new JLabel("");
        } else {
            label = new JLabel(caption + " :");
        }

        switch (location) {
        case INLINE_CAPTION:
            gbc_.gridwidth = 2;
            inputArea_.add(label, gbc_);
            gbc_.gridwidth = 1;
            inputArea_.add(Box.createHorizontalStrut(CAPTION_GAP));
            break;
        case MULTILINE_CAPTION:
            gbc_.gridwidth = GridBagConstraints.REMAINDER;
            inputArea_.add(label, gbc_);

            gbc_.gridwidth = GridBagConstraints.REMAINDER;
            gbc_.fill = GridBagConstraints.HORIZONTAL;
            inputArea_.add(Box.createVerticalStrut(6), gbc_);

            gbc_.gridwidth = 1;
            inputArea_.add(Box.createHorizontalStrut(12));
            break;
        default:
            return;
        }

        gbc_.anchor = GridBagConstraints.NORTHWEST;
        gbc_.gridwidth = GridBagConstraints.REMAINDER;
        if (fill) {
            gbc_.fill = GridBagConstraints.HORIZONTAL;
        } else {
            gbc_.fill = GridBagConstraints.NONE;
        }

        inputArea_.add(component, gbc_);
    }

    public void addButton(String caption) {
        buttonArea_.add(Box.createHorizontalStrut(BUTTON_GAP));
        InnerButton button = new InnerButton(CUSTOM_BUTTON);
        button.setCaption(caption);
        buttonArea_.add(button);
    }

    //戻り値として押されたボタンの識別子を返す。
    public int showModalDialog() {
        setModal(true);
        pack();
        setLocationRelativeTo(owner_);
        setVisible(true);
        return buttonType_;
    }

    public void showNoWait() {
        setModal(false);
        owner_.setEnabled(false);
        pack();
        setLocationRelativeTo(owner_);
        setVisible(true);
    }

    public void dispose() {
        super.dispose();
        owner_.setEnabled(true);
    }

    public void addActionListener(ActionListener listener) {
        listener_ = AWTEventMulticaster.add(listener_, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listener_ = AWTEventMulticaster.remove(listener_, listener);
    }

    public void setCloseButtonEnabled(boolean enabled) {
        closeButtonEnabled_ = enabled;
    }

    public void processWindowEvent(WindowEvent evt) {
        if (closeButtonEnabled_) {
            super.processWindowEvent(evt);
        }
    }

    //--------------------------------------------------------------------
    // インナークラス
    protected class InnerButton extends JButton {
        private int type_;
  
        public InnerButton(int type) {
            type_ = type;
            switch (type_) {
            case CUSTOM_BUTTON:
                break;
            case OK_BUTTON:
                setCaption(MessageBundle.get("dialog.okButton"));
                break;
            case CANCEL_BUTTON:
                setCaption(MessageBundle.get("dialog.cancelButton"));
                break;
            case YES_BUTTON:
                setCaption(MessageBundle.get("dialog.yesButton"));
                break;
            case NO_BUTTON:
                setCaption(MessageBundle.get("dialog.noButton"));
                break;
            }

            this.addActionListener(ModalDialog.this.listener_);
        }

        void setCaption(String caption) {
            setText(caption);
            setActionCommand(caption);
        }

        public int getButtonType() { return type_; }
    }
}

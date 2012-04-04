/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/*
 *  GrxProcessingWindow.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro kawasumi (GeneralRobotix, Inc)
 */

package com.generalrobotix.ui.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.lang.Thread;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

//import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.SynchronizedAccessor;

@SuppressWarnings("serial")
public class GrxProcessingWindow extends JDialog{
    private List<ImageIcon> robotIcons = new ArrayList<ImageIcon>();
    private JLabel labelIcon = new JLabel();
    private JTextArea area = new JTextArea();
    private int waitCount = 0;
    private static int SLEEP_MSEC_ = 100;
    private java.util.Timer timer_ = null;
    private TimerTask task_ = null;
    private SynchronizedAccessor<Runnable> runnableVisible_;
    private SynchronizedAccessor<Runnable> runnableMessage_;
    private SynchronizedAccessor<Boolean> isProcessing_ = new SynchronizedAccessor<Boolean>(false);
    private SynchronizedAccessor<String> message_ = new SynchronizedAccessor<String>("");

    public GrxProcessingWindow(Frame owner, boolean modal)
            throws HeadlessException {
        super(owner, modal);
        setSize(new Dimension(400, 150));
        setPreferredSize(new Dimension(400, 150));
        robotIcons
                .add(new ImageIcon(java.awt.Toolkit.getDefaultToolkit()
                        .getImage(
                                getClass().getResource(
                                        "/resources/images/grxrobot.png"))));
        for (int i = 1; i < 14; i++)
            robotIcons
                    .add(new ImageIcon(java.awt.Toolkit.getDefaultToolkit()
                            .getImage(
                                    getClass().getResource(
                                            "/resources/images/grxrobot" + i
                                                    + ".png"))));
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setTitle("Processing...");
        JPanel cPane = (JPanel) getContentPane();
        cPane.setLayout(new BoxLayout(cPane, BoxLayout.X_AXIS));

        labelIcon.setIcon(robotIcons.get(0));
        labelIcon.setAlignmentY(JLabel.CENTER_ALIGNMENT);

        area.setEditable(false);
        area.setBackground(cPane.getBackground());

        cPane.add(Box.createHorizontalStrut(30));
        cPane.add(labelIcon);
        cPane.add(Box.createHorizontalStrut(30));
        cPane.add(area);
        cPane.add(Box.createHorizontalStrut(30));

        setAlwaysOnTop(true);
    }

    public void initInvoker() {
        runnableVisible_ = new SynchronizedAccessor<Runnable>(new Runnable() {
            public void run() {
                doSetVisible();
            }
        });

        runnableMessage_ = new SynchronizedAccessor<Runnable>(new Runnable() {
            public void run() {
                doSetMessage();
            }
        });
    }

    public void setVisible(boolean b) {
        if (isProcessing_.get() != b) {
            isProcessing_.set(b);
            SwingUtilities.invokeLater(runnableVisible_.get());
            try {
                Thread.sleep(SLEEP_MSEC_);
            } catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }

    public void setMessage(String message) {
        if (message != null) {
            message_.set("\n\n\n" + message);
            SwingUtilities.invokeLater(runnableMessage_.get());
            try {
                Thread.sleep(SLEEP_MSEC_);
            } catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }

    private synchronized void doSetVisible() {
        if (isProcessing_.get()) {
            _showDialog();
        } else {
            if (timer_ != null) {
                timer_.cancel();
                timer_.purge();
                timer_ = null;
            }
            super.setVisible(false);
        }
    }

    private synchronized void doSetMessage() {
        area.setText(message_.get());
    }

    private void _showDialog() {
        setSize(new Dimension(400, 150));
        setLocationRelativeTo(getOwner());

        if (isModal()) {
            Thread t = new Thread() {
                public void run() {
                    setVisible(true);
                }
            };
            SwingUtilities.invokeLater(t);
            t.start();
        } else {
            super.setVisible(true);
        }
        SwingUtilities.invokeLater(task_ = new TimerTask() {
            public void run() {
                if (isProcessing_.get())
                    iconAnimation();
            }
        });

        timer_ = new java.util.Timer();
        timer_.scheduleAtFixedRate(task_, 0, SLEEP_MSEC_);
    }

    private void iconAnimation() {
        labelIcon.setVisible(false);
        labelIcon.setIcon(robotIcons.get(waitCount % robotIcons.size()));
        labelIcon.setVisible(true);
        waitCount++;
    }
    
/*
// * #290 Debugging method
    private void _DbgPrintThread(String strMessage){
        GrxDebugUtil.setDebugFlag(true);
        Thread refThread = Thread.currentThread();
        Long threadID = refThread.getId();
        String threadName = refThread.getName();
        GrxDebugUtil.println("threadID = " + threadID.toString() + "  threadName = " + threadName + 
                "  isProcessing_ = " + isProcessing_.get().toString() );
        if (timer_ != null && task_ != null ){
            GrxDebugUtil.println("timer_ = " + timer_.toString() + "  task_ = " + task_.toString() );
        }
        GrxDebugUtil.println(strMessage);
        GrxDebugUtil.setDebugFlag(false);
    }
*/    
}

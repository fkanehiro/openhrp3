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
 *  GrxORBMonitor.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  2004/03/16
 */
package com.generalrobotix.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxORBMonitor extends Composite {
    private MessageBox dialog_     = null;

    private Label      labelNs     = null;
    private Text       multiText   = null;
    private Text       textNsHost_ = null;
    private Text       textNsPort_ = null;

    public GrxORBMonitor(Composite parent, int style) {
        super(parent, style);
        this.setLayout(new GridLayout(1, false));

        Composite northPane = new Composite(this, SWT.NONE);
        northPane.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        GridLayout gridLayout = new GridLayout(5, false);
        gridLayout.marginWidth = 5;
        gridLayout.horizontalSpacing = 5;
        northPane.setLayout(gridLayout);

        Button button = new Button(northPane, SWT.PUSH);
        button.setText(MessageBundle.get("GrxORBMonitor.button.update")); //$NON-NLS-1$
        GridData btnGridData = new GridData();
        btnGridData.widthHint = 80;
        btnGridData.heightHint = 26;
        button.setLayoutData(btnGridData);
        button.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                update();
            }
        });

        new Label(northPane, SWT.NONE).setText(MessageBundle.get("GrxORBMonitor.label.host")); //$NON-NLS-1$

        textNsHost_ = new Text(northPane, SWT.SINGLE | SWT.BORDER);
        textNsHost_.setText(GrxCorbaUtil.nsHost());
        GridData textGridData = new GridData();
        textGridData.widthHint = 100;
        textNsHost_.setLayoutData(textGridData);

        new Label(northPane, SWT.NONE).setText(MessageBundle.get("GrxORBMonitor.label.port")); //$NON-NLS-1$

        textNsPort_ = new Text(northPane, SWT.SINGLE | SWT.BORDER);
        Integer nsPort = new Integer(GrxCorbaUtil.nsPort());
        textNsPort_.setText(nsPort.toString());
        textNsPort_.setLayoutData(textGridData);

        multiText = new Text(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        multiText.setEditable(false);
        multiText.setLayoutData(new GridData(GridData.FILL_BOTH));

        labelNs = new Label(this, SWT.SHADOW_NONE);
        labelNs.setText(MessageBundle.get("GrxORBMonitor.label.NSURL0")); //$NON-NLS-1$
        labelNs.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        setSize(412, 280);
    }

    // public methods --------------------------------------------------------
    //    public static GrxORBMonitor getInstance() {
    //        if (this_ == null){
    //            this_ =  new GrxORBMonitor();
    //        }
    //        return this_;
    //    }

    public void showDialog() {//JFrame frame){
        if (dialog_ == null) {
            dialog_ = new MessageBox(null, SWT.OK);
            //dialog_ = new JDialog(frame,"ORB Monitor",false);
            dialog_.setMessage(MessageBundle.get("GrxORBMonitor.dialog.message.ORB")); //$NON-NLS-1$
            //dialog_.pack();
            //dialog_.setContentPane(this);
            //            dialog_.addWindowListener(new java.awt.event.WindowAdapter() { 
            //                public void windowClosing(java.awt.event.WindowEvent e) {    
            //                    dialog_.setVisible(false);
            //                }
            //            });
        }
        multiText.setText(""); //$NON-NLS-1$
        dialog_.open();
    }

    public void update() {
        String nsHost = textNsHost_.getText();
        String nsPort = textNsPort_.getText();
        if (nsHost == null || nsPort == null) {
            return;
        }
        int nsPortInt = Integer.parseInt(nsPort);
        labelNs.setText(MessageBundle.get("GrxORBMonitor.label.NSURL1") + nsHost + ":" + nsPort + "/NameService"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        String s[] = GrxCorbaUtil.getObjectNameList(nsHost, nsPortInt);
        multiText.setText(""); //$NON-NLS-1$
        if (s == null) {
            return;
        }
        for (int i = 0; i < s.length; i++) {
            String str = null;
            if (GrxCorbaUtil.isConnected(s[i], nsHost, nsPortInt)) {
                str = MessageBundle.get("GrxORBMonitor.text.active") + s[i] + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                GrxDebugUtil.println("[GrxORBMonitor] " + s[i] + " is not in " + nsHost + ":" + nsPortInt + " active"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                str = MessageBundle.get("GrxORBMonitor.text.notActive") + s[i] + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            multiText.append(str);
        }

        GrxDebugUtil.println("ObjectNum:" + s.length); //$NON-NLS-1$
    }

    public String getNSHost() {
        return textNsHost_.getText();
    }

    public String getNSPort() {
        return textNsPort_.getText();
    }
}
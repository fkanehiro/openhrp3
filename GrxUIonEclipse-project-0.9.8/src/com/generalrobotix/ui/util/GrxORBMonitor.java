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

@SuppressWarnings("serial")
public class GrxORBMonitor extends Composite {
    private MessageBox dialog_ = null;
    
    private Text textNsHost = null;
    private Text textNsPort = null;
    private Label labelNs = null;
    private Text multiText = null;

    public GrxORBMonitor(Composite parent,int style) {
        super(parent,style);
        this.setLayout(new GridLayout(1,false));

        Composite northPane = new Composite(this,SWT.NONE);
        northPane.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        GridLayout gridLayout = new GridLayout(5,false);
        gridLayout.marginWidth = 5;
        gridLayout.horizontalSpacing = 5;
        northPane.setLayout(gridLayout);
        
        Button button = new Button(northPane,SWT.PUSH);
        button.setText("update");
        GridData btnGridData = new GridData();
        btnGridData.widthHint = 80;
        btnGridData.heightHint = 26;
        button.setLayoutData(btnGridData);
        button.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                update();
            }
        }
        );
        
        new Label(northPane,SWT.NONE).setText("Host:");
        
        textNsHost = new Text(northPane,SWT.SINGLE|SWT.BORDER);
        textNsHost.setText(GrxCorbaUtil.nsHost());
        GridData textGridData = new GridData();
        textGridData.widthHint = 100;
        textNsHost.setLayoutData(textGridData);
        
        new Label(northPane,SWT.NONE).setText("Port:");
        
        textNsPort = new Text(northPane,SWT.SINGLE|SWT.BORDER);
        Integer nsPort = new Integer(GrxCorbaUtil.nsPort());
        textNsPort.setText(nsPort.toString());
        textNsPort.setLayoutData(textGridData);

        multiText = new Text(this,SWT.MULTI|SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL);
        multiText.setEditable(false);
        multiText.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        labelNs = new Label(this,SWT.SHADOW_NONE);
        labelNs.setText("NS_URL:");
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
    
    public void showDialog(){//JFrame frame){
        if (dialog_ == null){
            dialog_ = new MessageBox(null,SWT.OK);
            //dialog_ = new JDialog(frame,"ORB Monitor",false);
            dialog_.setMessage("ORB Monitor");
            //dialog_.pack();
            //dialog_.setContentPane(this);
//            dialog_.addWindowListener(new java.awt.event.WindowAdapter() { 
//                public void windowClosing(java.awt.event.WindowEvent e) {    
//                    dialog_.setVisible(false);
//                }
//            });
        }
        multiText.setText("");
        dialog_.open();
    }
    public void update(){
        String nsHost = textNsHost.getText();   
        String nsPort = textNsPort.getText();
        if (nsHost == null || nsPort == null) return;
        int nsPortInt = Integer.parseInt(nsPort);
        labelNs.setText("NS_URL: corbaloc:iiop:"+nsHost+":"+nsPort+"/NameService");
        
        String s[] = GrxCorbaUtil.getObjectNameList(nsHost,nsPortInt);
        multiText.setText("");
        if (s==null) return;
        
        for (int i=0;i<s.length;i++){
            String str = null;
            if (GrxCorbaUtil.isConnected(s[i],nsHost,nsPortInt)){
                str = "(    Active    )  "+s[i]+"\n";
            } else {
            	GrxDebugUtil.println("[GrxORBMonitor] "+s[i]+" is not in " +nsHost +":"+ nsPortInt +" active");
                str = "( not Active )  "+s[i]+"\n";
            }
            multiText.append(str);
        }
        
        GrxDebugUtil.println("ObjectNum:"+s.length);
    }
}
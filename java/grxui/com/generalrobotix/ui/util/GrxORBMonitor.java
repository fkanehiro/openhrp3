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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.*;

@SuppressWarnings("serial")
public class GrxORBMonitor extends JPanel 
{
	private static final KeyStroke KS_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0);

	private JComboBox  cmbHost_;
	private JTextField fldHost_;
	private JTextField fldPort_;
	private JTextArea  area_;

    public GrxORBMonitor() {
		super(new BorderLayout());

		JButton btnUpdate = new JButton("update");
		btnUpdate.setPreferredSize(new Dimension(120,20));
		btnUpdate.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});

 		cmbHost_ = new JComboBox(new String[]{"localhost"});
		cmbHost_.setEditable(true);
		cmbHost_.setPreferredSize(new Dimension(80, 20));
		fldHost_ = (JTextField)cmbHost_.getEditor().getEditorComponent();
		fldHost_.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
				if (ks == KS_ENTER)
					update();
			}
		}); 

		fldPort_ = new JTextField("2809");
		fldPort_.setPreferredSize(new Dimension(80,20));
		fldPort_.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
				if (ks == KS_ENTER)
					update();
			}
		}); 

		JPanel pnl = new JPanel();
		pnl.add(new JLabel("Host"));
		pnl.add(cmbHost_);
		pnl.add(new JLabel("Port"));
		pnl.add(fldPort_);
		pnl.add(btnUpdate);
		this.add(pnl, BorderLayout.NORTH);

		area_ = new JTextArea();
		area_.setEditable(false);
		this.add(new JScrollPane(area_), BorderLayout.CENTER);
	}

	public void setHosts(String[] hosts) {
		cmbHost_.removeAllItems();
		for (int i=0; i<hosts.length; i++) 
			cmbHost_.addItem(hosts[i]);
	}
	
	private void update() {
		String host = (String)fldHost_.getText();
		if (host.equals(""))
			host = "localhost";

		int port = 2809;
		try {
			port = Integer.parseInt(fldPort_.getText());
		} catch(Exception e) {
			area_.setText("Can't parse port as integer : "+fldPort_.getText()+" .");
			return;
		}

		boolean b = true;
		for (int i=0; i<cmbHost_.getItemCount(); i++) {
			String val = (String)cmbHost_.getItemAt(i);
			if (host.equals(val)) {
				b = false;
				break;
			}
		}

		area_.setText("accessing "+host+":"+port+" ...");
		String s[] = GrxCorbaUtil.getObjectNameList(host, port);
		if (s == null) {
			area_.setText("Accessing to NameService( "+host+" : "+port+" ) ... failed.");
			return;
		}

		area_.setText("");
		if (b) {
			cmbHost_.addItem(host);
			cmbHost_.setSelectedItem(host);
		}
		
		for (int i=0; i<s.length; i++) {
			if (GrxCorbaUtil.isConnected(s[i], host, port))
				area_.append("(    Active    )  "+s[i]+"\n");
			else
				area_.append("( not Active )  "+s[i]+"\n");
		}
	}
}  

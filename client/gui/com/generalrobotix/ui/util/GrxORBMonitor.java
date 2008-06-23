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

import javax.swing.*;

@SuppressWarnings("serial")
public class GrxORBMonitor extends JPanel {
	private static GrxORBMonitor this_ = null;
    private JDialog dialog_ = null;
    
	private JTextField jTextField = null;
	private JTextField jTextField1 = null;
	private JLabel jLabel2 = null;
	private JPanel jPanel = null;
	private JTextArea jTextArea = null;
	//private JCheckBox checkActive = new JCheckBox("Check Active",true);

    public GrxORBMonitor() {
		super(new BorderLayout());
		// Panel North
		
		jPanel = new JPanel(new GridLayout(2,1));
		jPanel.add(new JLabel("NameService Host"), null);
		jPanel.add(getJTextField(), null);
		jPanel.add(new JLabel("NameService Port"), null);
		jPanel.add(getJTextField1(), null);
		
		JButton jButton = new JButton("update");
		jButton.setPreferredSize(new Dimension(100,26));
		jButton.addActionListener(new java.awt.event.ActionListener() { 
			public void actionPerformed(java.awt.event.ActionEvent e) {
				update();
			}
		});
		JPanel jPanel2 = new JPanel(new BorderLayout());
		jPanel2.add(jButton, BorderLayout.NORTH);
		
		JPanel northPane = new JPanel();
		northPane.add(jButton, null);
		northPane.add(jPanel, null);
		add(northPane, BorderLayout.NORTH);
		
		// Panel Center
		//JPanel jPanel3 = new JPanel();
		//jPanel3.setLayout(new BoxLayout(jPanel3, BoxLayout.Y_AXIS));
		//jPanel3.add(getJLabel2(), null);
		//jPanel3.add(new JScrollPane(getJTextArea()), null);
		add(new JScrollPane(getJTextArea()), BorderLayout.CENTER);
		
		add(getJLabel2(), BorderLayout.SOUTH);
		
		setSize(412, 280);
	}
    
	private JTextField getJTextField() {
		if(jTextField == null) {
			jTextField = new JTextField("localhost");
		}
		return jTextField;
	}
	private JTextField getJTextField1() {
		if(jTextField1 == null) {
			jTextField1 = new JTextField("2809");
		}
		return jTextField1;
	}
	private JLabel getJLabel2() {
		if(jLabel2 == null) {
			jLabel2 = new JLabel("NS_URL:");
			jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
		}
		return jLabel2;
	}
	private JTextArea getJTextArea() {
		if(jTextArea == null) {
			jTextArea = new JTextArea();
			jTextArea.setEditable(false);
		}
		return jTextArea;
	}
	
	// public methods --------------------------------------------------------
	public static GrxORBMonitor getInstance() {
		if (this_ == null){
			this_ =  new GrxORBMonitor();
		}
		return this_;
	}
	public void showDialog(JFrame frame){
		if (dialog_ == null){
			dialog_ = new JDialog(frame,"ORB Monitor",false);
			dialog_.pack();
			//GuiUtil.setWindowConfig("orbmonitor",dialog_,new Dimension(400,400));
			dialog_.setContentPane(this);
			dialog_.addWindowListener(new java.awt.event.WindowAdapter() { 
				public void windowClosing(java.awt.event.WindowEvent e) {    
					dialog_.setVisible(false);
				}
			});
		}
		getJTextArea().setText("");
		dialog_.setVisible(true);
	}
	public void update(){
		String nsHost = getJTextField().getText();   
		String nsPort = getJTextField1().getText();
		if (nsHost == null || nsPort == null) return;
		int nsPortInt = Integer.parseInt(nsPort);
		getJLabel2().setText("NS_URL: corbaloc:iiop:"+nsHost+":"+nsPort+"/NameService");
		
		String s[] = GrxCorbaUtil.getObjectNameList(nsHost,nsPortInt);
		getJTextArea().setText("");
		if (s==null) return;
		
		for (int i=0;i<s.length;i++){
			String str = null;
			if (GrxCorbaUtil.isConnected(s[i],nsHost,nsPortInt)){
				str = "(    Active    )  "+s[i]+"\n";
			} else {
				str = "( not Active )  "+s[i]+"\n";
			}
			jTextArea.append(str);
		}
		
		GrxDebugUtil.println("ObjectNum:"+s.length);
	}
}  //  @jve:visual-info  decl-index=0 visual-constraint="-123,-111"

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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public class GrxProcessingWindow extends JDialog{
	private List<ImageIcon> robotIcons = new ArrayList<ImageIcon>();
	private JLabel labelIcon = new JLabel();
	private JTextArea area = new JTextArea();
	private int waitCount = 0;
	
	public GrxProcessingWindow(Frame owner, boolean modal) throws HeadlessException {
		super(owner, modal);
		setSize(new Dimension(400,150));
		setPreferredSize(new Dimension(400,150));
		robotIcons.add(new ImageIcon(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/images/grxrobot.png"))));
		for (int i = 1; i < 14; i++)
			robotIcons.add(new ImageIcon(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/images/grxrobot"+i+".png"))));
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setTitle("Processing...");
		JPanel cPane = (JPanel)getContentPane();
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
	
	private java.util.Timer timer_;
	private TimerTask task_;
	private boolean isProcessing_ = false;
	
	public void setVisible(boolean b) {
		if (b == isProcessing_)
			return;
		if (b) {
			isProcessing_ = true;
			_showDialog();
		} else  if (isProcessing_) {
			timer_.cancel();
			timer_.purge();
			while (!getOwner().isVisible()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			super.setVisible(false);
			isProcessing_ = false;
		}
	}
	
	public void setMessage(String message) {
		if (message != null) {
			message = "\n\n\n"+message;
			area.setText(message);
		}
	}
	
	private void _showDialog() {
		setSize(new Dimension(400,150));
		setLocationRelativeTo(getOwner());
		while (!getOwner().isVisible()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (isModal()) {
			Thread t = new Thread() {
				public void run() {
					GrxProcessingWindow.super.setVisible(true);
				}
			};
			t.start();
		} else {
			GrxProcessingWindow.super.setVisible(true);
		}
		
		task_ = new TimerTask() {
			public void run() {
				labelIcon.setIcon(robotIcons.get(waitCount % robotIcons.size()));
				waitCount++;
			}
		};
		
		timer_ = new java.util.Timer();
		timer_.scheduleAtFixedRate(task_, 0, 700);
		
		while (!isVisible()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}	
	}
}

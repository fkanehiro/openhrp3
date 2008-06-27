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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class GrxProcessingWindow extends JDialog{
	private List<ImageIcon> robotIcons = new ArrayList<ImageIcon>();
	private JLabel labelIcon = new JLabel();
	private JTextArea area = new JTextArea();
	private int waitCount = 0;
	private java.util.Timer timer_;
	
	public GrxProcessingWindow(Frame owner, boolean modal) throws HeadlessException {
		super(owner, modal);
		setTitle("Processing...");
		setPreferredSize(new Dimension(400,150));
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setAlwaysOnTop(true);

		JPanel cPane = (JPanel)getContentPane();
		
		area.setEditable(false);
		area.setBackground(cPane.getBackground());
		
		robotIcons.add(new ImageIcon(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/images/grxrobot.png"))));
		for (int i = 1; i < 14; i++)
			robotIcons.add(new ImageIcon(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/images/grxrobot"+i+".png"))));
		labelIcon.setIcon(robotIcons.get(0));
		labelIcon.setAlignmentY(JLabel.CENTER_ALIGNMENT);
		
		cPane.setLayout(new BoxLayout(cPane, BoxLayout.X_AXIS));
		cPane.add(Box.createHorizontalStrut(30));
		cPane.add(labelIcon);
		cPane.add(Box.createHorizontalStrut(30));
		cPane.add(area);
		cPane.add(Box.createHorizontalStrut(30));
	}
	
	public void setMessage(String message) {
		if (message != null) {
			area.setText("\n\n\n" + message);
		}
	}
	
	public void setVisible(boolean b) {
		if (!b) {
			super.setVisible(false);
			if (timer_ != null) {
				timer_.cancel();
				timer_.purge();
				timer_ = null;
			}
			return;
		}

		setSize(new Dimension(400,150));
		setLocationRelativeTo(getOwner());

		while (!getOwner().isVisible()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					GrxProcessingWindow.super.setVisible(true);
				}
			}
		);
		
		timer_ = new java.util.Timer();
		TimerTask task_ = new TimerTask() {
			public void run() {
				SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							labelIcon.setIcon(robotIcons.get(waitCount % robotIcons.size()));
							waitCount++;
						}
					}
				);
			}
		};
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

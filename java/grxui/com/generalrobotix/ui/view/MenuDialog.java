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
 *  MenuDialog.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  Created on 2005/01/31
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;

/**
 * Dialog with buttons that is be able to set jython funtion 
 * 
 * @author kawasumi
 * 
 */
@SuppressWarnings("serial")
public class MenuDialog extends JPanel {

	public static final int BOTH = 0;
	public static final int ALWAYS = 1;
	public static final int SEQUENTIAL = 2;

	private JScrollPane sclAlways_;
	private JScrollPane sclSequence_;
	private JPanel    pnlAlways_   = new JPanel();
	private JPanel    pnlSequence_ = new JPanel();
	private JPanel    pnlLowerC_   = new JPanel();
	private ImageIcon icnUp_       = new ImageIcon(getClass().getResource("/resources/images/arrow_up.png"));
	private ImageIcon icnDn_       = new ImageIcon(getClass().getResource("/resources/images/arrow_down.png"));
	private JButton   btnSwitch_   = new JButton(icnUp_);
	private JButton   btnNext_     = new JButton(new ImageIcon(getClass().getResource("/resources/images/playback.png")));
	private JButton   btnPrevious_ = new JButton(new ImageIcon(getClass().getResource("/resources/images/arrow_left.png")));
	private JButton   btnQuit_     = new JButton(new ImageIcon(getClass().getResource("/resources/images/batsu.png")));
	private JButton   btnRetry_    = new JButton(new ImageIcon(getClass().getResource("/resources/images/undo.png")));
	private JLabel 	  lblStep_     = new JLabel("  1 / 1  ");
	private JLabel 	  lblMode_     = new JLabel("Commands Always Enabled");
	private JCheckBox cbxSequential_ = new JCheckBox("Sequential", true);
	private JCheckBox cbxContinuous_ = new JCheckBox("continuous", false);

	private List<JTextField> currentFields_ = new ArrayList<JTextField>();
	private JButton currentGoBtn_;
	
	private String[][] menu_;
	private int showingStage_ = 0;
	private int currentStage_ = 0;
	private String executingCommand_ = null;
	private boolean isWaiting_ = false;
	private String message_;
	private Thread thread_;

	private static final SimpleDateFormat dateFormat_ = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	public MenuDialog(String[][] src) {
		this(src, BOTH);
	}

	public MenuDialog(String[][] src, int initialMode) {
		this(src, initialMode, true, true);
	}

	public MenuDialog(String[][] src, int initialMode, boolean showSwitch, boolean showQuit) {
		menu_ = src;
		isWaiting_ = false;

		pnlAlways_.setLayout(new BoxLayout(pnlAlways_, BoxLayout.Y_AXIS));
		sclAlways_   = new JScrollPane(pnlAlways_);

		pnlSequence_.setLayout(new BoxLayout(pnlSequence_, BoxLayout.Y_AXIS));
		sclSequence_ = new JScrollPane(pnlSequence_);

		//
		btnSwitch_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnSwitch_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnSwitch_.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				if (btnSwitch_.getIcon() == icnUp_) 
					switchPanel(ALWAYS);
				else
					switchPanel(SEQUENTIAL);
			}
		});

		btnPrevious_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnPrevious_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnPrevious_.setToolTipText("show previous menu");
		btnPrevious_.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				showingStage_--;
				updateSequentialMenu();
			}
		});

		btnNext_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnNext_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnNext_.setToolTipText("show next menu");
		btnNext_.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {    
				showingStage_++;
				updateSequentialMenu();
			}
		});

		btnQuit_.setToolTipText("quit this menu");
		btnQuit_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnQuit_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnQuit_.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {    
				exit();
			}
		});	
		btnQuit_.setVisible(showQuit);

		btnRetry_.setToolTipText("retry from first");
		btnRetry_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnRetry_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnRetry_.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {    
				retry();
			}
		});	

		cbxSequential_.setToolTipText("enable sequential execution");
		cbxSequential_.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				if (cbxSequential_.isSelected()) {
					showingStage_ = currentStage_;
					updateSequentialMenu();
					setContinuous(true);
				} else {
					if (executingCommand_ == null)
						GrxGuiUtil.setEnableRecursive(true, pnlSequence_, null);
					setContinuous(false);
				}
			}
		});

		cbxContinuous_.setToolTipText("excecute continuously");

		JPanel pnlLowerL = new JPanel();
		JPanel pnlLowerR = new JPanel();
		pnlLowerL.add(btnSwitch_);
		pnlLowerR.add(btnRetry_);
		pnlLowerR.add(btnQuit_);

		pnlLowerC_.add(btnPrevious_);
		pnlLowerC_.add(lblStep_);
		pnlLowerC_.add(btnNext_);
		pnlLowerC_.add(cbxSequential_);
		pnlLowerC_.add(lblMode_);
		lblMode_.setVisible(false);
		lblMode_.setAlignmentY(JLabel.CENTER_ALIGNMENT);

		JPanel pnlLower = new JPanel(new BorderLayout());
		pnlLower.add(pnlLowerL,  BorderLayout.WEST);
		pnlLower.add(pnlLowerC_, BorderLayout.CENTER);
		pnlLower.add(pnlLowerR,  BorderLayout.EAST);


		this.setLayout(new BorderLayout());
		this.add(sclSequence_, BorderLayout.CENTER);
		this.add(pnlLower, BorderLayout.SOUTH);

		updateAlwaysMenu(); // only call

		refresh();

		if (menu_.length == 1) {
			switchPanel(ALWAYS);
			btnSwitch_.setVisible(false);
		} else {
			switchPanel(initialMode);
			btnSwitch_.setVisible(showSwitch);
		}
	}

	public JPanel getAlwaysPanel() {
		return pnlAlways_;
	}

	private void switchPanel(int mode) {
		this.setVisible(false);
		boolean b = (mode != ALWAYS);

		if (b) {
			btnSwitch_.setIcon(icnUp_);
			btnSwitch_.setToolTipText("show buttons always enabled");
			this.remove(sclAlways_);
			this.add(sclSequence_);

		} else {
			btnSwitch_.setIcon(icnDn_);
			btnSwitch_.setToolTipText("show buttons executed sequentially");
			this.remove(sclSequence_);
			this.add(sclAlways_);
		}

		btnPrevious_.setVisible(b);
		lblStep_.setVisible(b);
		btnNext_.setVisible(b);
		cbxSequential_.setVisible(b);
		btnRetry_.setVisible(b);
		lblMode_.setVisible(!b);

		this.setVisible(true);
	}

	private void updateAlwaysMenu() {
		pnlAlways_.removeAll();
		pnlAlways_.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		pnlAlways_.setAlignmentY(JPanel.CENTER_ALIGNMENT);
		
		if (menu_[0].length < 1) {
			JLabel lbl = new JLabel("There is no buttons.");
			lbl.setFont(new Font("Monospaced", Font.BOLD, 20));
			lbl.setForeground(Color.gray);
			lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			lbl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
			pnlAlways_.add(lbl);
			return;
		} 

		//for (int i=1; i<menu_[0].length; i=i+2) {
			//pnlAlways_.add(new CommandButton(menu_[0][i-1], menu_[0][i], false));
		//}
		for (int i=1; i<menu_[0].length; i=i+2) {
			String m1 = menu_[0][i-1].trim();
			String m2 = menu_[0][i].trim();
			if (m2.equals("#label") || m2.equals("#monitor")) {

				JLabel lbl = new JLabel(m1);
				lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
				lbl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
				pnlAlways_.add(lbl);

			} else {
				JPanel bPnl = new CommandButton(m1, m2, (i<=1));
				bPnl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
				bPnl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
				pnlAlways_.add(bPnl);
			}
		}
	}

	private void updateAlwaysMenuActive(PythonInterpreter interpreter) {

		pnlAlways_.removeAll();
		pnlAlways_.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		pnlAlways_.setAlignmentY(JPanel.CENTER_ALIGNMENT);
		
		if (menu_[0].length < 1) {
			JLabel lbl = new JLabel("There is no buttons.");
			lbl.setFont(new Font("Monospaced", Font.BOLD, 20));
			lbl.setForeground(Color.gray);
			lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			lbl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
			pnlAlways_.add(lbl);
			return;
		} 

		//for (int i=1; i<menu_[0].length; i=i+2) {
			//pnlAlways_.add(new CommandButton(menu_[0][i-1], menu_[0][i], false));
		//}
		for (int i=1; i<menu_[0].length; i=i+2) {
			String m1 = menu_[0][i-1].trim();
			String m2 = menu_[0][i].trim();

			if (m2.equals("#label")) {

				JLabel lbl = new JLabel(m1);
				lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
				lbl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
				pnlAlways_.add(lbl);

			} else if (m2.equals("#monitor")) {

			  	PyObject res = interpreter.eval(m1);
				JLabel lbl = new JLabel(res.toString());
				lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
				lbl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
				pnlAlways_.add(lbl);

			} else {

				JPanel bPnl = new CommandButton(m1, m2, (i<=1));
				bPnl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
				bPnl.setAlignmentY(JLabel.CENTER_ALIGNMENT);
				pnlAlways_.add(bPnl);
			}
		}
		setVisible(false);
		setVisible(true);
	}

	
	private void updateSequentialMenu() {
		pnlSequence_.setVisible(false);

		if (showingStage_ >= menu_.length-1) {
			showingStage_ = menu_.length-1;
			btnNext_.setEnabled(false);
		} else {
			btnNext_.setEnabled(true);
		}
		if (showingStage_ < 2) {
			showingStage_ = 1;
			btnPrevious_.setEnabled(false);
		} else {
			btnPrevious_.setEnabled(true);
		}
		
		currentFields_.clear();
		pnlSequence_.removeAll();

		for (int i=1; i<menu_[showingStage_].length; i=i+2) {

			String m1 = menu_[showingStage_][i-1].trim();
			String m2 = menu_[showingStage_][i].trim();
			if ((m2.equals("#label")) || (m2.equals("#monitor")))
				pnlSequence_.add(new JLabel(m1));
			else
				pnlSequence_.add(new CommandButton(m1, m2, (i<=1)));
		}

		lblStep_.setText(showingStage_+" / "+(menu_.length-1));
		
		if (executingCommand_ != null || 
		    (cbxSequential_.isSelected() && showingStage_ != currentStage_))
			GrxGuiUtil.setEnableRecursive(false, pnlSequence_, null);

		//pnlSequence_.add(javax.swing.Box.createVerticalGlue());
		pnlSequence_.setVisible(true);
	}


	private void updateSequentialMenuActive(PythonInterpreter interpreter) {

		pnlSequence_.setVisible(false);

		if (showingStage_ >= menu_.length-1) {
			showingStage_ = menu_.length-1;
			btnNext_.setEnabled(false);
		} else {
			btnNext_.setEnabled(true);
		}
		if (showingStage_ < 2) {
			showingStage_ = 1;
			btnPrevious_.setEnabled(false);
		} else {
			btnPrevious_.setEnabled(true);
		}
		
		currentFields_.clear();
		pnlSequence_.removeAll();

		for (int i=1; i<menu_[showingStage_].length; i=i+2) {

			String m1 = menu_[showingStage_][i-1].trim();
			String m2 = menu_[showingStage_][i].trim();
			if (m2.equals("#label"))
				pnlSequence_.add(new JLabel(m1));

			else if(m2.equals("#monitor")) {

				PyObject res = interpreter.eval(m1);
				pnlSequence_.add(new JLabel(res.toString()));
			
			} else 
				pnlSequence_.add(new CommandButton(m1, m2, (i<=1)));
		}

		lblStep_.setText(showingStage_+" / "+(menu_.length-1));
		
		if (executingCommand_ != null || 
		    (cbxSequential_.isSelected() && showingStage_ != currentStage_))
			GrxGuiUtil.setEnableRecursive(false, pnlSequence_, null);

		//pnlSequence_.add(javax.swing.Box.createVerticalGlue());
		pnlSequence_.setVisible(true);
	}


 	private class CommandButton extends JPanel {
		private String command_;

		private JButton button_;
		private ArrayList<JTextField> fieldList_ = new ArrayList<JTextField>();

		public CommandButton(String name, String command) {
			this(name, command, false);
		}

		public CommandButton(String name, String command, final boolean goNext) {
			super();

			if (name.trim().equals(""))
				name = "untitled";

			button_ = new JButton(name);
			this.add(button_);

			command_ = command;

			if (goNext) {
				if (command.indexOf("#continuous")!=-1) {
					setContinuous(true);
					this.add(cbxContinuous_);
				} else {
					setContinuous(false);
				}
				currentGoBtn_ = button_;
			}else{
			        currentGoBtn_ = null;
			}

			int idx = -1;
			while ( (idx = command.indexOf("#", idx+1)) != -1 ) {
				char c = command.charAt(idx + 1);
				if (c == 'D' || c == 'I' || c == 'T') {
					JTextField f = new JTextField();
					f.setName("#"+c);
					f.setPreferredSize(new Dimension(60,20));
					fieldList_.add(f);
					this.add(f);
				}
			}

			button_.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {    
					if (executingCommand_ != null) 
						return;

					String com = new String(command_);
					try {
						for (int i=0; i<fieldList_.size(); i++) {
							String type = fieldList_.get(i).getName();
							String argc = fieldList_.get(i).getText();
							if (type.equals("#D"))
								Double.parseDouble(argc);
							else if (type.equals("#I"))
								Integer.parseInt(argc);
 							else if (type.equals("#T"))
								argc = "\'"+argc+"\'";
							else 
								continue;
							com = com.replaceFirst(type, argc);
						}
						
						if (cbxSequential_.isSelected() && goNext) {
							currentStage_++;
						}
						executingCommand_ = com;

						GrxGuiUtil.setEnableRecursive(false, pnlSequence_, null);
						GrxGuiUtil.setEnableRecursive(false, pnlAlways_,   null);

					} catch (NumberFormatException e1) {
						GrxDebugUtil.printErr("MenuDialog: parse error");
					}
				}
			});
		}

		public JButton getButton() {
			return button_;

		}

		public String getCommand() {
			return command_;
		}
	}

	private void retry() {
		int ans = JOptionPane.showConfirmDialog(this,
			"Retry from first ?",
			"Retry", JOptionPane.OK_CANCEL_OPTION);
		if (ans == JOptionPane.OK_OPTION)
			refresh();
	}

	private void refresh(){
		currentStage_ = showingStage_ = 1;
		isWaiting_ = true;
		if (menu_.length > 1)
			updateSequentialMenu();
	}

	private void refreshActive(PythonInterpreter interpreter) {
		currentStage_ = showingStage_ = 1;
		isWaiting_ = true;
		if (menu_.length > 1)
			updateSequentialMenuActive(interpreter);
	}

	public void setMessage(String msg) {
		message_ = msg;
	}

	public void setContinuous(boolean b) {
		cbxContinuous_.setSelected(b);
	}

	public void createThread(final PythonInterpreter interpreter) {
		Thread t = new Thread() {
			public void run() {
				MenuDialog.this.start(interpreter);
			}
		};
		t.start();
	}

	public void start() {
		start(new PythonInterpreter());
	}

	public void start(PythonInterpreter interpreter) {
		if (thread_ != null) {
			JOptionPane.showMessageDialog(this, "This menu already running.");
			return;
		}

		thread_ = Thread.currentThread();

		updateAlwaysMenuActive(interpreter);

		refreshActive(interpreter);

		while (isWaiting_) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {}

			if (executingCommand_ == null) 
				continue;

			try {
				interpreter.exec(executingCommand_);
				updateAlwaysMenuActive(interpreter);
				
			} catch (Exception e) {

				if (e.toString().indexOf("Script Canceled.") > 0) {
					JOptionPane.showMessageDialog(this, "Script Canceled.");
				} else {
					if (e.toString().indexOf("org.omg.CORBA.COMM_FAILURE:") > 0) {
						String err = e.toString().split("org.omg.CORBA.COMM_FAILURE:")[0];
						JOptionPane.showMessageDialog(this, "Communication error occured."+err);
						System.out.println(e.toString());
					}
					System.out.println(e.toString());
				}
				setContinuous(false);
			}
			executingCommand_ = null;
	
			if (!isWaiting_) 
				break;
	
			// For continuous execution
			if (menu_.length > 1) {
				if (cbxSequential_.isSelected()) {
					if (currentStage_ < menu_.length) {
						showingStage_ = currentStage_;
						updateSequentialMenuActive(interpreter);
						if (cbxContinuous_.isSelected() && currentGoBtn_ != null)
							currentGoBtn_.doClick();

					} else {

						showingStage_ = menu_.length;
						updateSequentialMenuActive(interpreter);
					}
				} else {
					updateSequentialMenuActive(interpreter);
				}
			}	
			GrxGuiUtil.setEnableRecursive(true, pnlAlways_, null);

			// Fill text field if there are any message
			if (message_ == null)
				continue;
			int idx = -1;
			while ((idx = message_.indexOf("$", idx+1)) != -1) {
				int idx2 = message_.indexOf("=", idx+1);
				String s = message_.substring(idx+1, idx2);
				int idx3 = message_.indexOf(" ", idx2+1);
				if (idx3 < 0)
					idx3 = message_.length();
				String val = message_.substring(idx2+1, idx3);
				try {
					int i = Integer.parseInt(s.trim());
					currentFields_.get(i).setText(val);
				} catch (Exception e) {
					GrxDebugUtil.printErr("MenuDialog.setMessage():",e);
				}
				idx = idx2;
			}
		}
	}

	public boolean exit() {
		int ans = JOptionPane.showConfirmDialog(this,
			"Are you sure quit menu ?",
			"Quit Menu", JOptionPane.OK_CANCEL_OPTION);
		if (ans != JOptionPane.OK_OPTION)
			return false;

		if (thread_ != null)  {
			try {
				thread_.interrupt();
			} catch (Exception e) {
				GrxDebugUtil.printErr("", e);
			}
			thread_ = null;
		}

		Container c = this.getParent();
		if (c != null)
			c.remove(this);
		isWaiting_ = false;
		return true;
	}
}

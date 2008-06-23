/*
 *  GrxSerialControlView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.shrpsys.item.GrxSerialPortItem;
import com.generalrobotix.ui.util.GrxGuiUtil;

@SuppressWarnings("serial")
public class GrxSerialControlView extends GrxBaseView {
	private GrxSerialPortItem serialPort_ = null;

	private JPanel buttonPanel_ = new JPanel();

	private JTextField commandField_ = new JTextField();

	final String[][] commands = new String[][] {
			{ "Start/Stop", "Start Hrpsys",
					"cd /usr/local/bin/CHOROMET;./sHrpsys.sh script/demo.py",
					"Send Ctrl+C", "^C", "Send Ctrl+Z", "^Z", "Stop Hrpsys",
					"^D" },
			{ "Setup", "Servo On ", "robot.sendMsg(':servo all on')",
					"Servo Off", "robot.sendMsg(':servo all off')",
					"Sensor calib", "sensor.sendMsg(':calib inertia')",
					"Force Sensor calib", "sensor.sendMsg(':calib inertia')",
					"Show Stat", "manager.sendMsg(':stat')" },
			{ "Basic Motion", "Go Initial", "seq.sendMsg(':go-initial')",
					"Go HalfSit", "seq.sendMsg(':go-half-sitting')",
					"Sit Down", "sitdown(seq)", "Stand Up", "standup(seq)",
					"Bow", "bow(seq)", "Look Around", "kyoro(seq)" },
			{ "Demo", "Start(from Chair)", "demo.start()", "Send Return", "",
					"Finish (onto Chair)", "demo.finish()" },
			{ "Taisou", "Taiso(Roll)", "taisou_r(seq)", "Taiso(Pitch)",
					"taisou_p(seq)", "Taiso(Yaw)", "taisou_y(seq)" },
			{ "Unstable", "Punch", "punch2(seq)", "Kick", "maegeri(seq)",
					"One Leg", "oneleg2(seq)", "Neoki1", "demo.neoki1()",
					"Neoki2", "demo.neoki2()", },
			{ "Walking", "Init Walk", "demo.walkinit()", "Step", "demo.step()",
					"Walk Forward", "demo.walkfwd()", "Walk Backward",
					"demo.walkbwd()", "Walk Right", "demo.walkright()",
					"Walk Left", "demo.walkleft()" } };

	@SuppressWarnings("serial")
	class CommandButton extends JButton {
		private CommandButton() {};

		CommandButton(String name, final String com) {
			super(name);
			setToolTipText(com);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (com.equals("^C")) {
						getSerialPort().sendETX();
					} else if (com.equals("^D")) {
						getSerialPort().sendEOT();
					} else if (com.equals("^Z")) {
						getSerialPort().sendSUB();
					} else {
						getSerialPort().write(com);
					}
				}
			});
		}
	}

	public GrxSerialControlView(String name, GrxPluginManager manager) {
		super(name, manager);

		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(buttonPanel_, BorderLayout.CENTER);
		JPanel commandPanel = new JPanel(new BorderLayout());
		commandPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		commandPanel.add(new JLabel("   Command :  "), BorderLayout.WEST);
		commandPanel.add(commandField_, BorderLayout.CENTER);
		contentPane.add(commandPanel, BorderLayout.SOUTH);

		buttonPanel_.setLayout(new BoxLayout(buttonPanel_, BoxLayout.X_AXIS));
		buttonPanel_.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		Dimension maxDim = new Dimension(0, 0);
		for (int i = 0; i < commands.length; i++) {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
			panel.setAlignmentY(JPanel.TOP_ALIGNMENT);
			GrxGuiUtil.setTitleBorder(panel, commands[i][0]);
			buttonPanel_.add(panel);
			buttonPanel_.add(Box.createHorizontalStrut(10));
			for (int j = 1; j < commands[i].length; j = j + 2) {
				CommandButton b = new CommandButton(commands[i][j],
						commands[i][j + 1]);
				b.setAlignmentX(CommandButton.CENTER_ALIGNMENT);
				panel.add(Box.createVerticalStrut(5));
				panel.add(b);
				if (maxDim.width < b.getMaximumSize().width)
					maxDim = b.getMaximumSize();
			}
			panel.add(Box.createVerticalStrut(5));
		}

		for (int i = 0; i < buttonPanel_.getComponentCount(); i++) {
			Container cont = (Container) buttonPanel_.getComponent(i);
			for (int j = 0; j < cont.getComponentCount(); j++) {
				Component comp = cont.getComponent(j);
				if (comp instanceof JButton) {
					comp.setMinimumSize(maxDim);
					comp.setSize(maxDim);
					comp.setPreferredSize(maxDim);
					comp.setMaximumSize(maxDim);
				}
			}
		}

		commandField_.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent arg0) {
				KeyStroke ks = KeyStroke.getKeyStrokeForEvent(arg0);
				String com = commandField_.getText();
				int len = com.length();
				int cp = commandField_.getCaretPosition();

				if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
					commandField_.setEnabled(false);
					List list = manager_
							.getSelectedItemList(GrxSerialPortItem.class);
					if (list.size() > 0) {
						((GrxSerialPortItem) list.get(0)).write(commandField_
								.getText());
					}
					commandField_.setText("");
					commandField_.setEnabled(true);
					commandField_.requestFocus();
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
						|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_K, 0)) {
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)
						|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_J, 0)) {
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_U,
						KeyEvent.CTRL_MASK)) {
					commandField_.setText("");
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_A,
						KeyEvent.CTRL_MASK)) {
					commandField_.setCaretPosition(0);
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_E,
						KeyEvent.CTRL_MASK)) {
					commandField_.setCaretPosition(len);
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_F,
						KeyEvent.CTRL_MASK)
						&& cp < len) {
					commandField_.setCaretPosition(cp + 1);
				} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_B,
						KeyEvent.CTRL_MASK)
						&& cp > 0) {
					commandField_.setCaretPosition(cp - 1);
				}
			}
		});
	}

	public GrxSerialPortItem getSerialPort() {
		if (serialPort_ == null) {
			List list = manager_.getSelectedItemList(GrxSerialPortItem.class);
			serialPort_ = (GrxSerialPortItem) list.get(0);
		}
		return serialPort_;
	}

	public boolean cleanup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void control(List<GrxBaseItem> itemList) {
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(serialPort_)) {
			Iterator<GrxBaseItem> it = itemList.iterator();
			while (it.hasNext()) {
				GrxBaseItem item = it.next();
				if (item instanceof GrxSerialPortItem) {
					serialPort_ = (GrxSerialPortItem) item;
					break;
				}
			}
		}
	}
}

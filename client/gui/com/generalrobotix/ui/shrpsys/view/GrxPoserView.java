/*
 *  GrxPoserView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.*;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.shrpsys.item.GrxChorometStateItem;
import com.generalrobotix.ui.shrpsys.item.GrxPortItem;
import com.generalrobotix.ui.shrpsys.item.GrxSockPortItem;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.item.GrxModelItem;

@SuppressWarnings("serial")
public class GrxPoserView extends GrxBaseView {
	private int INITIAL_POSTURE_ID = 0;
	private int HALFSIT_POSTURE_ID = 1;
	private double[][] CALIB_POSTURE = new double[][] {
			{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, -27, 60, -33, 0, 0, 0, -27, 60, -33, 0, 10, -20, 0, -30, 10, 20, 0, -30 } };

	private DecimalFormat format1 = new DecimalFormat("###0.0 ");

	private boolean sendCommand_ = true;
	private GrxModelItem currentModelItem_ = null;
	private GrxPortItem currentPort_ = null;

	private JPanel[] partPnls_ = null;
	private JPanel westernPanel_ = null;
	private JPanel centralPanel_ = null;
	private JPanel easternPanel_ = null;
	private JPanel commandButtonPanel_ = null;
	private JMenuItem menuItems_[] = null;
	private JSlider resolutionSlider_ = null;
	private Vector<JointSlider> sliders_ = new Vector<JointSlider>();
	private Vector<JointSlider> slidersFocusOrder_ = new Vector<JointSlider>();
	private JRadioButton moveLeftLegOnly_ = new JRadioButton("left", false);
	private JRadioButton moveRightLegOnly_ = new JRadioButton("right", false);
	private JRadioButton moveBothLeg_ = new JRadioButton("both", true);

	private ImageIcon robotIcon = new ImageIcon(java.awt.Toolkit
			.getDefaultToolkit().getImage(
					getClass().getResource("/resources/images/robot.png")));

	public GrxPoserView(String name, GrxPluginManager manager) {
		super(name, manager);
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));
		contentPane.add(westernPanel_ = new JPanel());//,BorderLayout.WEST);
		contentPane.add(Box.createHorizontalStrut(10));
		contentPane.add(centralPanel_ = new JPanel());//,BorderLayout.CENTER);
		contentPane.add(Box.createHorizontalStrut(10));
		contentPane.add(easternPanel_ = new JPanel());//,BorderLayout.EAST);
		westernPanel_.setLayout(new BoxLayout(westernPanel_, BoxLayout.Y_AXIS));
		centralPanel_.setLayout(new BoxLayout(centralPanel_, BoxLayout.Y_AXIS));
		easternPanel_.setLayout(new BoxLayout(easternPanel_, BoxLayout.Y_AXIS));

		//loadJointConfig();

		resolutionSlider_ = new JSlider(0, 3, 0);
		resolutionSlider_.setMaximumSize(new Dimension(200, 50));
		resolutionSlider_.setSnapToTicks(true);
		resolutionSlider_.setPaintTicks(true);
		resolutionSlider_.setPaintLabels(true);
		resolutionSlider_.setMajorTickSpacing(1);
		Hashtable<Integer, JLabel> map = new Hashtable<Integer, JLabel>();
		String[] l = new String[] { "0.1", "0.5", "1.0", "2.0" };
		resolutionSlider_.setMaximum(l.length - 1);
		resolutionSlider_.setValue(l.length / 2);
		for (int i = 0; i < l.length; i++)
			map.put(i, new JLabel(l[i]));
		resolutionSlider_.setLabelTable(map);
		resolutionSlider_.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				int val = ((JSlider) arg0.getSource()).getValue();
				String label = ((JLabel) resolutionSlider_.getLabelTable().get(
						val)).getText();
				for (int i = 0; i < sliders_.size(); i++)
					sliders_.get(i).setResolution(Double.parseDouble(label));
			}
		});

		centralPanel_.add(Box.createVerticalStrut(5));
		centralPanel_.add(new JLabel("Angle Resolution"));
		centralPanel_.add(resolutionSlider_);
		centralPanel_.add(Box.createVerticalStrut(5));
		centralPanel_.add(createCommandButtonPanel());

		Component[][] comps = new Component[][] { contentPane.getComponents(),
				centralPanel_.getComponents(), easternPanel_.getComponents(),
				westernPanel_.getComponents() };
		for (int i = 0; i < comps.length; i++) {
			for (int j = 0; j < comps[i].length; j++) {
				JComponent c = (JComponent) comps[i][j];
				c.setAlignmentY(JPanel.TOP_ALIGNMENT);
				c.setAlignmentX(JPanel.CENTER_ALIGNMENT);
			}
		}
	}

	private JPanel createCommandButtonPanel() {
		if (commandButtonPanel_ != null)
			return commandButtonPanel_;
		commandButtonPanel_ = new JPanel();
		commandButtonPanel_.setLayout(new BoxLayout(commandButtonPanel_,
				BoxLayout.Y_AXIS));
		JButton servoOn = new JButton("Servo On");//new CommandButton("Servo On", "send robot :servo all on");
		servoOn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				for (int i = 0; i < sliders_.size(); i++)
					sliders_.get(i).nameButton_.setSelected(true);
				sendCommand("send robot :servo all on");
			}
		});
		JButton servoOff = new JButton("Servo Off");//new CommandButton("Servo Off","send robot :servo all off");   
		servoOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				for (int i = 0; i < sliders_.size(); i++)
					sliders_.get(i).nameButton_.setSelected(false);
				sendCommand("send robot :servo all off");
			}
		});
		JButton reconnect = new JButton("Reconnect");
		reconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (currentPort_ != null) {
					if (!currentPort_.isConnected()) {
						int ans = JOptionPane.showConfirmDialog(manager_
								.getFrame(), "Reconnect to robot ?",
								"Reconnect", JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, robotIcon);
						if (ans == JOptionPane.OK_OPTION)
							currentPort_.open();
					} else {
						JOptionPane.showMessageDialog(manager_.getFrame(),
								"Already Connected to Robot.");
					}
				} else {
					JOptionPane.showMessageDialog(manager_.getFrame(),
							"No RobotStateItem Selected.");
				}
			}
		});
		JButton goInitial = new SeqCommandButton("Initial",
				CALIB_POSTURE[INITIAL_POSTURE_ID]);
		JButton goHalfSit = new SeqCommandButton("HalfSit",
				CALIB_POSTURE[HALFSIT_POSTURE_ID]);
		JButton initialCalib = new JButton("Calib(Initial)");
		initialCalib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String[] option = new String[] { "OK", "CANCEL" };
				int ans = JOptionPane
						.showOptionDialog(
								manager_.getFrame(),
								"Calibrate joint angles ?\n"
										+ "This make current joint angles to [Initial Posture].",
								"Calibrate Joint Angle",
								JOptionPane.DEFAULT_OPTION,
								JOptionPane.QUESTION_MESSAGE, robotIcon,
								option, option[1]);
				if (ans == JOptionPane.OK_OPTION)
					calibration(INITIAL_POSTURE_ID);
			}
		});
		JButton halfsitCalib = new JButton("Calib(HalfSit)");
		halfsitCalib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String[] option = new String[] { "OK", "CANCEL" };
				int ans = JOptionPane.showOptionDialog(
					manager_.getFrame(),
					"Calibrate joint angles ?\n"
						+ "This make current joint angles to [HalfSit Posture].",
					"Calibrate Joint Angle",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE, robotIcon,
					option, option[1]);
				if (ans == JOptionPane.OK_OPTION)
					calibration(HALFSIT_POSTURE_ID);
			}
		});
		JButton sensorCalib = new JButton("Calib(Sensor)");
		sensorCalib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String[] option = { "OK", "CANCEL", "SKIP" };
				int ans = JOptionPane.showOptionDialog(manager_.getFrame(),
						"Calibrate Force Sensors ?\n"
								+ "Put the robot in the air",
						"Sensor Calibration", JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE, robotIcon, option,
						option[0]);
				if (ans == 0)
					sendCommand("send sensor :calib force");
				else if (ans == 1)
					return;
				ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
						"Calibrate the Gyro and G-sensors ?\n"
								+ "Make the robot not moving.",
						"Sensor Calibration", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, robotIcon);
				if (ans == JOptionPane.OK_OPTION)
					sendCommand("send sensor :calib inertia");
			}
		});
		//  new CommandButton("Calib(Sensor)","send sensor :calib all",
		//  "Calibrate force sensor, gyro and g-sensor ?\n");

		JPanel commandPanel = new JPanel();
		//GuiUtil.setTitleBorder(commandPanel, "Commands");
		commandPanel.setLayout(new GridLayout(4, 2, 3, 3));
		commandPanel.setMaximumSize(new Dimension(200, 110));
		commandPanel.add(servoOn);
		commandPanel.add(servoOff);
		commandPanel.add(goInitial);
		commandPanel.add(initialCalib);
		commandPanel.add(goHalfSit);
		commandPanel.add(halfsitCalib);
		commandPanel.add(reconnect);
		commandPanel.add(sensorCalib);

		JPanel lpPane = new LoadPatPanel();
		lpPane.setMaximumSize(new Dimension(200, 28));

		JPanel moveWaistPanel = new JPanel();
		GrxGuiUtil.setTitleBorder(moveWaistPanel, "Waist Link Control");
		moveWaistPanel.setMaximumSize(new Dimension(200, 250));
		moveWaistPanel.setLayout(new GridLayout(7, 3, 3, 3));
		moveWaistPanel.add(moveRightLegOnly_);
		moveWaistPanel.add(moveBothLeg_);
		moveWaistPanel.add(moveLeftLegOnly_);
		
		moveWaistPanel.add(new MoveWaistButton("UP", new double[]{ 0.0, 0.0,-0.002 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton("DN", new double[]{ 0.0, 0.0, 0.002 }, null, 0.2));
		moveWaistPanel.add(new JLabel(""));

		moveWaistPanel.add(new MoveWaistButton("RB", new double[]{ 0.0014, 0.0014, 0.0 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton(" B", new double[]{ 0.002,  0.0,    0.0 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton("LB", new double[]{ 0.0014,-0.0014, 0.0 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton(" R", new double[]{ 0.0,    0.002,  0.0 }, null, 0.2));
		//moveWaistPanel.add(new AdjustCoMButton("Adjust"));
		moveWaistPanel.add(new JLabel(""));
		moveWaistPanel.add(new MoveWaistButton(" L", new double[]{ 0.0,   -0.002,  0.0 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton("RF", new double[]{-0.0014, 0.0014, 0.0 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton(" F", new double[]{-0.002,  0.0,    0.0 }, null, 0.2));
		moveWaistPanel.add(new MoveWaistButton("LF", new double[]{-0.0014,-0.0014, 0.0 }, null, 0.2));

		moveWaistPanel.add(new MoveWaistButton("R+", null, new double[]{-1.0, 0.0, 0.0 }, 0.2));
		moveWaistPanel.add(new MoveWaistButton("P+", null, new double[]{ 0.0,-1.0, 0.0 }, 0.2));
		moveWaistPanel.add(new MoveWaistButton("Y+", null, new double[]{ 0.0, 0.0,-1.0 }, 0.2));
		moveWaistPanel.add(new MoveWaistButton("R-", null, new double[]{ 1.0, 0.0, 0.0 }, 0.2));
		moveWaistPanel.add(new MoveWaistButton("P-", null, new double[]{ 0.0, 1.0, 0.0 }, 0.2));
		moveWaistPanel.add(new MoveWaistButton("Y-", null, new double[]{ 0.0, 0.0, 1.0 }, 0.2));

		ButtonGroup group = new ButtonGroup();

		group.add(moveRightLegOnly_);
		group.add(moveBothLeg_);
		group.add(moveLeftLegOnly_);
		//JButton testButton = new JButton("send command");

		commandButtonPanel_.add(commandPanel);
		commandButtonPanel_.add(Box.createVerticalStrut(5));
		commandButtonPanel_.add(lpPane);
		commandButtonPanel_.add(Box.createVerticalStrut(5));
		commandButtonPanel_.add(moveWaistPanel);

		if (GrxDebugUtil.isDebugging())
			centralPanel_.add(new HrpsysPrompt());

		GrxGuiUtil.setButtonInsetsRecursive(new Insets(2, 2, 2, 2),
				commandButtonPanel_);
		return commandButtonPanel_;
	}

	public void initView() {
		initPlugin();
	}

	private void initPlugin() {
		sendCommand("send self :load seqplay");
		sendCommand("send self :create seqplay seq");
		sendCommand("send seq  :start");
		sendCommand("send self :load jsrPlugin");
		sendCommand("send self :create jsrPlugin robot");
		sendCommand("send robot :start");
		sendCommand("send self :load sensPlugin");
		sendCommand("send self :create sensPlugin sensor");
		sendCommand("send sensor :start");
	}

	private void sendCommand(String com) {
		if (currentPort_ != null)
			currentPort_.println(com);
	}

	JMenuItem[] getMenuItems() {
		if (menuItems_ == null) {
			menuItems_ = new JMenuItem[2];
			menuItems_[0] = new JMenuItem("Export Pose");
			menuItems_[0].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					File f = new File("pose.out");
					try {
						FileWriter fw = new FileWriter(f);
						fw.append("send seq :joint-angles ");
						for (int i = 0; i < sliders_.size(); i++) {
							fw.append(sliders_.get(i).getDoubleValue() + " ");
						}
						fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			menuItems_[1] = new JMenuItem("Update Connection");
			menuItems_[1].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					currentPort_.open();
				}
			});
		}
		return menuItems_;
	}

	public void loadJointConfig() {
		if (element_ == null)
			return;
		sliders_.clear();
		NodeList list = element_.getElementsByTagName("jointgroup");//GrxXmlUtil.getPropertyElements("jointgroup");
		if (partPnls_ == null || partPnls_.length != list.getLength())
			partPnls_ = new JPanel[list.getLength()];
		int maxNameWidth = 0;
		for (int i = 0; i < list.getLength(); i++) {
			Element e = ((Element) list.item(i));
			//String strId = e.getAttribute("id");
			String pname = e.getAttribute("name");
			//int id = Integer.parseInt(strId);
			if (partPnls_[i] == null) {
				partPnls_[i] = new JPanel();
				partPnls_[i].setLayout(new BoxLayout(partPnls_[i], BoxLayout.Y_AXIS));
				partPnls_[i].setName(pname);
				GrxGuiUtil.setTitleBorder(partPnls_[i], pname);
			} else {
				partPnls_[i].removeAll();
			}
			JPanel pnl = null;
			if (pname.startsWith("R"))
				pnl = westernPanel_;
			else if (pname.startsWith("L"))
				pnl = easternPanel_;
			else
				pnl = centralPanel_;
			pnl.add(partPnls_[i], 0);
			//pnl.add(Box.createVerticalStrut(10),1);

			NodeList jointList = e.getElementsByTagName("joint");
			for (int j = 0; j < jointList.getLength(); j++) {
				Element jElement = (Element) jointList.item(j);
				String jname = jElement.getAttribute("name");
				JointSlider sld = new JointSlider(jname, pname, 90, -90);
				sliders_.add(sld);
				partPnls_[i].add(sld);
				if (maxNameWidth < sld.getLabelWidth())
					maxNameWidth = sld.getLabelWidth();
			}
		}
		for (int i = 0; i < sliders_.size(); i++)
			sliders_.get(i).setLabelWidth(maxNameWidth);

		JPanel[] pnls = new JPanel[] { westernPanel_, centralPanel_, easternPanel_ };
		for (int i = 0; i < pnls.length; i++) {
			for (int j = 0; j < pnls[i].getComponentCount(); j++) {
				Component c = pnls[i].getComponent(j);
				if (c instanceof JPanel) {
					JPanel partPnl = (JPanel) c;
					for (int k = 0; k < partPnl.getComponentCount(); k++) {
						c = partPnl.getComponent(k);
						if (c instanceof JointSlider)
							slidersFocusOrder_.add((JointSlider) c);
					}
				}
			}
		}

		loadOrigin();
	}

	class JointValue {
		int jid = 0;
		int ch = 0;
		double offset = 0.0;
		double minVal = -90.0;
		double maxVal = 90.0;
		double gain = 1.0;
	}

	public void loadOrigin() {
		File f = new File("Property.sav");
		if (f.exists() && f.isFile()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				StreamTokenizer st = new StreamTokenizer(br);
				st.wordChars('_', '_');
				st.whitespaceChars(' ', ' ');
				st.commentChar('#');
				for (int i = 0; i < sliders_.size(); i++) {
					st.nextToken();
					st.nextToken();
					st.nextToken();
					sliders_.get(i).setDoubleValue(st.nval, false);
					sliders_.get(i).setOrigin(0.0);
					st.nextToken();
					sliders_.get(i).setMinimumValue(st.nval);
					st.nextToken();
					sliders_.get(i).setMaximumValue(st.nval);
					st.nextToken();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void calibration(int postureId) {
		String fname = "Property.sav";
		File f = new File(fname);

		JointValue[] jvList = new JointValue[sliders_.size()];
		for (int i = 0; i < jvList.length; i++)
			jvList[i] = new JointValue();

		String afterString = new String();

		if (f.exists() && f.isFile()) {
			String fname2 = fname + "~";//"."+DATEFORMAT1.format(new Date());
			copyFileAs(fname, fname2);
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				StreamTokenizer st = new StreamTokenizer(br);
				st.wordChars('_', '_');
				st.whitespaceChars(' ', ' ');
				st.commentChar('#');
				for (int i = 0; i < jvList.length; i++) {
					st.nextToken();
					jvList[i].jid = (int) st.nval;
					st.nextToken();
					jvList[i].ch = (int) st.nval;
					st.nextToken();
					jvList[i].offset = st.nval;
					st.nextToken();
					jvList[i].minVal = st.nval;
					st.nextToken();
					jvList[i].maxVal = st.nval;
					st.nextToken();
					jvList[i].gain = st.nval;
				}
				while (br.ready())
					afterString += br.readLine() + "\n";
				br.close();
				f.delete();
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			afterString = "#FORCE\n" + "80 0.74 0.72 0.72\n"
					+ "80 0.64 0.64 0.64\n";
		}

		try {
			FileWriter fw = new FileWriter(f);
			for (int i = 0; i < jvList.length; i++) {

				double offset = jvList[i].offset;
				if (sliders_.get(i).nameButton_.isSelected())
					offset += sliders_.get(i).getDoubleValue()
							- CALIB_POSTURE[postureId][i];
				fw.write(jvList[i].jid + "\t" + jvList[i].ch + "\t"
						+ format1.format(offset) + "\t" + jvList[i].minVal
						+ "\t" + jvList[i].maxVal + "\t" + jvList[i].gain
						+ "\n");
			}
			fw.write(afterString);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		sendCommand("send robot :servo all off");
		sendCommand("send robot :reload");
		setJointAngles(CALIB_POSTURE[postureId], 0.005);
		//sendCommand("send seq :go-initial 0.005");
		sendCommand("send seq :wait-interpolation");
		for (int i = 0; i < sliders_.size(); i++) {
			if (sliders_.get(i).nameButton_.isSelected())
				sendCommand("send robot :servo " + sliders_.get(i).getName()
						+ " on");
		}

		for (int i = 0; i < sliders_.size(); i++)
			sliders_.get(i).setOrigin(CALIB_POSTURE[postureId][i]);

		/*double[] angles = new double[sliders_.size()];
		 for (int i=0;i<sliders_.size();i++)
		 angles[i] = 0.0;
		 setSliderAngles(angles);*/
	}

	public void copyFileAs(String from, String to) {
		try {
			FileReader in = new FileReader(new File(from));
			FileWriter out = new FileWriter(new File(to));
			int c;
			while ((c = in.read()) != -1) {
				out.write(c);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * returned radians
	 */
	public double[] getJointAngles() {
		double[] values = new double[sliders_.size()];
		for (int i = 0; i < values.length; i++)
			values[i] = sliders_.get(i).getDoubleValue();
		return values;
	}

	public boolean setJointAngles(double[] angles, double time) {
		String command = "send seq :joint-angles ";
		for (int i = 0; i < angles.length; i++) {
			if (Double.isNaN(angles[i]))
				return false;
			command += angles[i] + " ";
		}
		command += time + "\n";
		//command += "send seq :wait-interpolation\n";
		sendCommand(command);
		for (int i = 0; i < angles.length; i++)
			sliders_.get(i).setDoubleValue(angles[i], false);

		double[] radian = new double[angles.length];
		for (int i = 0; i < radian.length; i++)
			radian[i] = Math.toRadians(angles[i]);

		if (currentModelItem_ != null)
			currentModelItem_.setJointValues(radian);
		return true;
	}

	@SuppressWarnings("serial")
	class JointSlider extends JPanel {
		private JSlider slider_ = null;
		private JTextField valField_ = null;
		private JToggleButton nameButton_ = null;

		private double maxValue_ = 0;
		private double minValue_ = 0;
		private String partName_ = null;
		private double resolution_ = 1.0;
		private Hashtable<Integer, JLabel> labels_ = new Hashtable<Integer, JLabel>();
		private Color orgColor_ = null;
		private int prevVal_ = 0;
		
		private JointSlider() {
		}

		public JointSlider(String jname, String partName, double max, double min) {
			setName(jname);
			partName_ = partName;
			maxValue_ = max;
			minValue_ = min;
			setToolTipText(jname);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			nameButton_ = new JToggleButton(jname);
			nameButton_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					String com = "send robot :servo "
							+ ((JToggleButton) arg0.getSource()).getText();
					if (nameButton_.isSelected())
						com += " on";
					else
						com += " off";
					sendCommand(com);
				}
			});
			nameButton_.setMargin(new Insets(2, 2, 2, 2));
			valField_ = new JTextField("  0.0");
			Dimension d = new Dimension(40, 20);
			valField_.setPreferredSize(d);
			valField_.setMaximumSize(d);
			valField_.setMinimumSize(d);
			valField_.setEditable(false);
			valField_.setHorizontalAlignment(JTextField.TRAILING);
			valField_.setBackground(Color.white);

			slider_ = new JSlider();
			// slider_.setForeground(Color.white);
			// slider_.setBackground(Color.black);
			slider_.setPaintLabels(true);
			slider_.setPaintTicks(true);

			slider_.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					int val = slider_.getValue();
					if (sendCommand_ && val != prevVal_) {
						double value = getDoubleValue();
						valField_.setText(String.valueOf(value));
						sendCommand("send seq :joint-angle " +getName()+" "+value+" 0.005");

						if (currentModelItem_ != null)
							currentModelItem_.setJointValue(getName(), Math.toRadians(value));
					}
					prevVal_ = val;
				}
			});
			setResolution(1.0);
			slider_.setValue(0);
			slider_.addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent arg0) {
					orgColor_ = getBackground();
					slider_.setBackground(Color.lightGray);
				}

				public void focusLost(FocusEvent arg0) {
					slider_.setBackground(orgColor_);
				}
			});
			slider_.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent arg0) {
					KeyStroke ks = KeyStroke.getKeyStrokeForEvent(arg0);
					if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_UP,
							KeyEvent.SHIFT_MASK)
							|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_K, 0)) {
						JSlider js = (JSlider) arg0.getSource();
						int idx = slidersFocusOrder_.indexOf(js.getParent());
						idx = idx > 0 ? idx - 1 : slidersFocusOrder_.size() - 1;
						slidersFocusOrder_.get(idx).grabFocus();
					} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
							KeyEvent.SHIFT_MASK)
							|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_J, 0)) {
						JSlider js = (JSlider) arg0.getSource();
						int idx = slidersFocusOrder_.indexOf(js.getParent());
						idx = idx < slidersFocusOrder_.size() - 1 ? idx + 1 : 0;
						slidersFocusOrder_.get(idx).grabFocus();
					} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
							KeyEvent.SHIFT_MASK)
							|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_H,
									KeyEvent.SHIFT_MASK)) {
						JSlider js = (JSlider) arg0.getSource();
						int idx = slidersFocusOrder_.indexOf(js.getParent())
								- (slidersFocusOrder_.size() / 2);
						if (idx >= 0)
							slidersFocusOrder_.get(idx).grabFocus();
					} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
							KeyEvent.SHIFT_MASK)
							|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_L,
									KeyEvent.SHIFT_MASK)) {
						JSlider js = (JSlider) arg0.getSource();
						int idx = slidersFocusOrder_.indexOf(js.getParent())
								+ (slidersFocusOrder_.size() / 2);
						if (idx < slidersFocusOrder_.size())
							slidersFocusOrder_.get(idx).grabFocus();
					} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_H, 0)) {
						JSlider js = (JSlider) arg0.getSource();
						int val = js.getValue();
						val = val > js.getMinimum() ? val - 1 : val;
						js.setValue(val);
					} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_L, 0)) {
						JSlider js = (JSlider) arg0.getSource();
						int val = js.getValue();
						val = val < js.getMaximum() ? val + 1 : val;
						js.setValue(val);
					}
				}
			});

			if (partName_.startsWith("R")) {
				add(nameButton_);
				add(slider_);
				add(valField_);
			} else {
				add(valField_);
				add(slider_);
				add(nameButton_);
			}
			d = slider_.getPreferredSize();
			d.width = 140;
			slider_.setPreferredSize(d);
		}

		public void grabFocus() {
			slider_.grabFocus();
		}

		public void setResolution(double res) {
			sendCommand_ = false;

			int val = (int) (slider_.getValue() * resolution_ / res);
			resolution_ = res;
			slider_.setMaximum((int) (maxValue_ / resolution_));
			slider_.setMinimum((int) Math.ceil(minValue_ / resolution_));
			slider_.setValue(val);

			labels_.clear();
			if (0 < minValue_ || maxValue_ < 0) {
				slider_.setMajorTickSpacing((int) ((maxValue_ - minValue_) / 4.0 / resolution_));
			} else {
				if (maxValue_ <= -minValue_)
					slider_.setMajorTickSpacing((int) (-minValue_ / 2.0 / resolution_));
				else
					slider_.setMajorTickSpacing((int) (-minValue_ / resolution_));
				slider_.setMinorTickSpacing((int) ((maxValue_ - minValue_) / resolution_));
				labels_.put(0, new JLabel("0"));
			}
			labels_.put((int) (minValue_ / resolution_), new JLabel("" + (int) minValue_));
			labels_.put((int) (maxValue_ / resolution_), new JLabel("" + (int) maxValue_));
			slider_.setLabelTable(labels_);

			sendCommand_ = true;
		}

		public void setDoubleValue(double val, boolean sendCom) {
			sendCommand_ = sendCom;
			BigDecimal bd = null;
			try {
				bd = new BigDecimal(String.valueOf(val));
			} catch (Exception e) {
				GrxDebugUtil.println("GPoserView.setDoubleValue():" + val);
				e.printStackTrace();
				return;
			}
			slider_.setValue((int) Math.rint(val / resolution_));
			valField_.setText(bd.setScale(1, BigDecimal.ROUND_HALF_UP)
					.toString());
			sendCommand_ = true;
		}

		public void setMaximumValue(double val) {
			maxValue_ = val;
			setResolution(resolution_);
		}

		public void setMinimumValue(double val) {
			minValue_ = val;
			setResolution(resolution_);
		}

		public double getDoubleValue() {
			double dbl = slider_.getValue() * resolution_;
			BigDecimal bd = new BigDecimal(String.valueOf(dbl));
			return bd.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
		}

		public int getValue() {
			return slider_.getValue();
		}

		public void setOrigin(double val) {
			double current = getDoubleValue();
			maxValue_ += val - current;
			minValue_ += val - current;
			setResolution(resolution_);
			setDoubleValue(val, false);
		}

		public int getLabelWidth() {
			return nameButton_.getMaximumSize().width;
		}

		public void setLabelWidth(int width) {
			Dimension d = nameButton_.getSize();
			d.width = width;
			nameButton_.setMinimumSize(d);
			nameButton_.setPreferredSize(d);
		}
	}

	class CommandButton extends JButton {
		public CommandButton(String name, final String com) {
			setText(name);
			setToolTipText(com);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					sendCommand(com);
				}
			});
		}

		public CommandButton(String name, final String com,
				final String confirmMsg) {
			setText(name);
			setToolTipText(com);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int ans = JOptionPane.showConfirmDialog(
							manager_.getFrame(), confirmMsg, "",
							JOptionPane.OK_CANCEL_OPTION);
					if (ans == JOptionPane.OK_OPTION)
						sendCommand(com);
				}
			});
		}
	}

	class SeqCommandButton extends JButton {
		public SeqCommandButton(String name, final double[] angles) {
			setText(name);
			String com = "send seq :joint-angles ";
			for (int i = 0; i < angles.length; i++)
				com += angles[i] + " ";
			setToolTipText(com);

			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setJointAngles(angles, 3);
				}
			});
		}
	}

	class MoveWaistButton extends JButton {
		public MoveWaistButton(String name, final double[] dpos,
				final double[] drpy, final double time2move) {
			setText(name);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					setWaistTransform(dpos, drpy, time2move);
					try {
						Thread.sleep((int) time2move * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	class AdjustCoMButton extends JButton {
		public AdjustCoMButton(String name) {
			setText(name);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					Vector3d com = new Vector3d();
					currentModelItem_.getCoM(com);
					double[] dpos = new double[3];
					/*Transform3D t3d = new Transform3D();
					if (!moveLeftLegOnly_.isSelected()) {
						t3d = currentModelItem_.getTransformfromRoot("R_ANKLE_R");
					}*/

					if (!moveRightLegOnly_.isSelected()) {

					}

					setWaistTransform(dpos, null, 1);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	class LoadPatPanel extends JPanel {
		String DEFAULT_TEXT = "pattern file name";

		JTextField field = new JTextField(DEFAULT_TEXT);

		JButton play = new JButton("play");

		JButton load = new JButton("Ref.");

		public LoadPatPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			play.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					File f = new File(field.getText() + ".pos");
					if (f.exists()) {
						int ans = JOptionPane.showConfirmDialog(manager_
								.getFrame(), "Play pattern [" + field.getText()
								+ "] ?\n"
								+ "It takes a few seconds to load pattern.",
								"Play Pattern", JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, robotIcon);
						if (ans == JOptionPane.OK_OPTION)
							sendCommand("send seq :load-pattern "
									+ field.getText() + " 3");
					} else {
						JOptionPane.showMessageDialog(manager_.getFrame(),
								"File not exist!");
						field.setText(DEFAULT_TEXT);
					}
				}
			});
			load.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					File dir = new File("etc");
					if (!dir.exists())
						dir = null;
					JFileChooser fc = GrxGuiUtil.createFileChooser(
							"Choose Pattern File", dir, "pos");
					if (fc.showOpenDialog(manager_.getFrame()) != JFileChooser.APPROVE_OPTION)
						return;
					File f = fc.getSelectedFile();
					String fname = "etc/" + f.getName();
					fname = fname.substring(0, fname.lastIndexOf("."));
					field.setText(fname);
				}
			});
			add(play);
			add(Box.createHorizontalStrut(3));
			add(field);
			add(Box.createHorizontalStrut(3));
			add(load);
		}
	}

	class HrpsysPrompt extends JPanel {
		private JTextField commandField_ = new JTextField();

		HrpsysPrompt() {
			setLayout(new BoxLayout(HrpsysPrompt.this, BoxLayout.X_AXIS));
			add(commandField_);
			commandField_.setMaximumSize(new Dimension(150, 20));
			commandField_.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent arg0) {
					KeyStroke ks = KeyStroke.getKeyStrokeForEvent(arg0);
					String com = commandField_.getText();
					int len = com.length();
					int cp = commandField_.getCaretPosition();

					if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) {
						commandField_.setEnabled(false);
						sendCommand(com);
						commandField_.setEnabled(true);
					} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
							|| ks == KeyStroke.getKeyStroke(KeyEvent.VK_K, 0)) {

					} else if (ks == KeyStroke
							.getKeyStroke(KeyEvent.VK_DOWN, 0)
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
	}

	public void setWaistTransform(double[] dpos, double[] drpy, double time2move) {
		if (currentModelItem_ != null) {
			//Transform3D t3dL = currentModelItem_.getTransformfromRoot("R_ANKLE_R");
			//Transform3D t3dR = currentModelItem_.getTransformfromRoot("L_ANKLE_R");

			double[] rotL = new double[9];
			double[] posL = new double[3];
			double[] thetaL = new double[6];
			double[] rotR = new double[9];
			double[] posR = new double[3];
			double[] thetaR = new double[6];

			//currentModelItem_.getTransformFromRoot("R_ANKLE_R", rotR, posR);
			//currentModelItem_.getTransformFromRoot("L_ANKLE_R", rotL, posL);

			if (dpos != null) {
				for (int i = 0; i < 3; i++) {
					posR[i] = posR[i] + dpos[i];
					posL[i] = posL[i] + dpos[i];
				}
			}

			if (drpy != null) {
				Matrix3d drot = new Matrix3d();
				drot.rotX(Math.toRadians(drpy[0]));
				Matrix3d rotY = new Matrix3d();
				rotY.rotY(Math.toRadians(drpy[1]));
				Matrix3d rotZ = new Matrix3d();
				rotZ.rotZ(Math.toRadians(drpy[2]));
				drot.mul(rotY, drot);
				drot.mul(rotZ, drot);
				Matrix3d matR = new Matrix3d(rotR);
				Matrix3d matL = new Matrix3d(rotL);

				matR.mul(drot, matR);
				matL.mul(drot, matL);

				double[] tempR = new double[3];
				double[] tempL = new double[3];
				for (int i = 0; i < 3; i++) {
					tempR[i] = 0;
					tempL[i] = 0;
					for (int j = 0; j < 3; j++) {
						rotR[i * 3 + j] = matR.getElement(i, j);
						rotL[i * 3 + j] = matL.getElement(i, j);
						//drot.invert();
						tempR[i] += drot.getElement(i, j) * posR[j];
						tempL[i] += drot.getElement(i, j) * posL[j];
					}
				}
				for (int i = 0; i < 3; i++) {
					posR[i] = tempR[i];
					posL[i] = tempL[i];
				}
			}

			double[] ang = getJointAngles();
			if (!moveLeftLegOnly_.isSelected()) {
				GrxKinematicsHRP2m.legIK(GrxKinematicsHRP2m.CHOROMET_RLEG,
						posR, rotR, thetaR);
				for (int i = 0; i < 6; i++)
					ang[i] = Math.toDegrees(thetaR[i]);
			}
			if (!moveRightLegOnly_.isSelected()) {
				GrxKinematicsHRP2m.legIK(GrxKinematicsHRP2m.CHOROMET_LLEG,
						posL, rotL, thetaL);
				for (int i = 0; i < 6; i++)
					ang[i + 6] = Math.toDegrees(thetaL[i]);
			}
			setJointAngles(ang, time2move);
		}
	}
	
	public void restoreProperties() {
		loadJointConfig();
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		Iterator<GrxBaseItem> it = itemList.iterator();
		while (it.hasNext()) {
			GrxBaseItem item = it.next();
			if (item instanceof GrxModelItem) {
				currentModelItem_ = (GrxModelItem) item;
			} else if (item instanceof GrxChorometStateItem) {
				currentPort_ = (GrxSockPortItem) item;
			}
		}
	}
}

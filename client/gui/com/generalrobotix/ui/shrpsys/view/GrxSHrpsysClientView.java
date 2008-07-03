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
 *  GrxSHrpsysClientView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.*;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.shrpsys.item.GrxPortItem;
import com.generalrobotix.ui.shrpsys.item.GrxSockPortItem;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxSHrpsysClientView extends GrxBaseView {
	private DynamicsSimulator dynamics_;
	private GrxWorldStateItem currentItem_;
	private GrxModelItem currentModel_;
	private GrxPortItem currentPort_;
	
	private List<Integer> jointOrder = new ArrayList<Integer>();
	private double[] prevAngles_ = null;
	
	private WorldStateHolder worldStateH_ = new WorldStateHolder();
	
	private Date servoOnTimer_ = null;
	private int SERVO_OVERRUN_TIME = 300000; // [msec]
	
	private String modelName_ = "TRP1";
	private String nsHost_ = "localhost";
	private int nsPort_ = 2809;
	//private Date initialDate_;
	private Date prevDate_;
	private int intervalMS_ = 200;
	private ImageIcon startMonitorIcon_ = new ImageIcon(getClass().getResource(
			"/resources/images/sim_start.png"));
	private ImageIcon stopMonitorIcon_ = new ImageIcon(getClass().getResource(
			"/resources/images/sim_stop.png"));
	private ImageIcon servoOnIcon_ = new ImageIcon(getClass().getResource(
			"/resources/images/robot_servo_start.png"));
	private ImageIcon servoOffIcon_ = new ImageIcon(getClass().getResource(
			"/resources/images/robot_servo_stop.png"));
	private JToggleButton startMon_ = new JToggleButton(startMonitorIcon_);
	private JToggleButton servoOn_ = new JToggleButton(servoOnIcon_);
	private boolean requesting_ = false;

	public GrxSHrpsysClientView(String name, GrxPluginManager manager) {
		super(name, manager);
		isScrollable_ = false;
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		/*  
		 *  CREATE TOOLBAR
		 */
		startMon_.setEnabled(false);
		startMon_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		startMon_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		startMon_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JToggleButton b = (JToggleButton) arg0.getSource();
				if (b.isSelected()) {
					modelName_ = getStr("modelName", modelName_);
					startMonitor();
				} else {
					int ans = JOptionPane.showConfirmDialog(
							manager_.getFrame(), "Are you sure stop monitor ?",
							"Stop Robot State Monitor",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
					if (ans == JOptionPane.OK_OPTION) {
						stopMonitor();
					} else {
						startMon_.setSelected(true);
					}
				}
			}
		});

		servoOn_.setEnabled(false);
		servoOn_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		servoOn_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		servoOn_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (servoOn_.isSelected())
					servoOn();
				else
					servoOff();
			}
		});
		
		JToolBar toolbar = new JToolBar();
		toolbar.add(startMon_);
		toolbar.add(servoOn_);
		setToolBar(toolbar);
	}

	private void startMonitor() {
		try {
			currentModel_ = (GrxModelItem) manager_.getItem(GrxModelItem.class, modelName_);
			if (currentModel_ == null) {
				JOptionPane.showMessageDialog(manager_.getFrame(),
					"Load Model(" + modelName_ + ") first.",
					"Start Robot State Monitor",
					JOptionPane.WARNING_MESSAGE, manager_.ROBOT_ICON);
				stopMonitor();
				return;
			}
			
			jointOrder.clear();
			prevAngles_ = null;
			
			for (int i=0; i<currentModel_.lInfo_.length; i++)
				jointOrder.add(currentModel_.lInfo_[i].jointId);

			setProperty("modelName", modelName_);

			//
			// This is required
			//
			currentPort_ = (GrxPortItem)manager_.getSelectedItem(GrxSockPortItem.class, null);
			if (currentPort_.open() < 0) {
				stopMonitor();
                return;
            }

			//
			// used to get servo state 
			//

			//
			// for go-actual
			// (simply comment out for now)
			// 
			//boolean seqplayFlag = isTrue("loadSeqplay", true);
			//if (seqplayFlag == true) {
			currentPort_.setEnabled(true);
				currentPort_.println("send self :load seqplay");
				currentPort_.println("send self :create seqplay seq");
				currentPort_.println("send seq :start");
				currentPort_.println("send self :load jxcPlugin");
				currentPort_.println("send self :create jxcPlugin robot");
				currentPort_.println("send robot :start");
			//}

			currentItem_.clearLog();
			currentItem_.registerCharacter(modelName_, currentModel_.cInfo_);

			servoOn_.setEnabled(true);
			servoOn_.setSelected(false);
			servoOn_.setIcon(servoOnIcon_);
			servoOn_.setToolTipText("Servo On");

			startMon_.setIcon(stopMonitorIcon_);
			startMon_.setToolTipText("Stop Robot State Monitor");
			startMon_.setSelected(true);
			prevDate_ = new Date();
			initDynamicsSimulator(false);
			start();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(manager_.getFrame(),
					"Couldn't Connect StateProvider(" + nsHost_ + ":" + nsPort_
							+ ":" + modelName_ + ").",
					"Start Robot State Monitor", JOptionPane.WARNING_MESSAGE,
					manager_.ROBOT_ICON);
			stopMonitor();
		}
	}

	private DynamicsSimulator initDynamicsSimulator(boolean update) {
		if (dynamics_ != null && !update) 
			return dynamics_;

		obj = GrxCorbaUtil.getReference("DynamicsSimulatorFactory");
		DynamicsSimulatorFactory dynFactory = DynamicsSimulatorFactoryHelper.narrow(obj);
		if (dynamics_ != null)  {
			try {
				dynamics_.destroy();
			} catch(Exception e) {
				GrxDebugUtil.printErr("", e);
			}
		}
		try {
			dynamics_ = dynFactory.create();
			dynamics_.registerCharacter(robotType_, currentModel_.cInfo_);
			dynamics_.init(0.001, IntegrateMethod.EULER, SensorOption.DISABLE_SENSOR);
		} catch (Exception e) {
			dynamics_ = null;
		}

		return dynamics_;
	}

	private void stopMonitor() {
		startMon_.setIcon(startMonitorIcon_);
		startMon_.setToolTipText("Start Robot State Monitor");
		startMon_.setSelected(false);
		servoOn_.setEnabled(false);
		if (currentPort_ != null) {
			currentPort_.setEnabled(false);
			try {
				currentPort_.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
        currentPort_ = null;
        requesting_ = false;
		stop();
	}

	private void servoOn() {
		int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
				"!! Robot Motion Warning (Servo ON)!!\n"
						+ "Confirm Turn Relay On.\n"
						+ "Then Push [OK] to Servo On.\n", "Servo ON",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				manager_.ROBOT_ICON);
		if (ans == JOptionPane.OK_OPTION) {
			try {
				//currentPort_.println("send seq :go-actual");
				currentPort_.println("send robot :servo all on");
				servoOn_.setIcon(servoOffIcon_);
				servoOn_.setToolTipText("Servo Off");
				servoOnTimer_ = new Date();
				servoOnTimer_.setTime(servoOnTimer_.getTime()+SERVO_OVERRUN_TIME);
			} catch (Exception e) {
				GrxDebugUtil.printErr("got exception during servo on process:", e);
			}
		} else {
			servoOn_.setSelected(false);
		}
	}

	private void servoOff() {
		int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
				"!! Robot Motion Warning (Servo OFF)!!\n\n"
						+ "Push [OK] to stop Servo.\n", "Servo OFF",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				manager_.ROBOT_ICON);
		if (ans == JOptionPane.OK_OPTION) {
			try {
				servoOnTimer_ = null;
				currentPort_.println("send robot :servo all off");
				servoOn_.setIcon(servoOnIcon_);
				servoOn_.setToolTipText("Servo On");
			} catch (Exception e) {
				GrxDebugUtil.printErr("got exception during servo off process:", e);
			}
		} else {
			servoOn_.setSelected(false);
		}
	}

	// this rewrites the currentItem_ to start the control function going.
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(currentItem_)) {
			currentItem_ = (GrxWorldStateItem)manager_.getSelectedItem(GrxWorldStateItem.class, null);
			currentPort_ = (GrxPortItem)manager_.getSelectedItem(GrxSockPortItem.class, null);
			if (currentItem_ == null)
				startMon_.setEnabled(false);
			else
				startMon_.setEnabled(true);
		}
	}
	
	// get the data here and stuff it into currentItem_
	public void control(List<GrxBaseItem> itemList) {
		if (!startMon_.isSelected()) {
			stopMonitor();
			return;
		} 
		if (servoOnTimer_ != null && servoOnTimer_.before(new Date())) {
			JOptionPane.showMessageDialog(manager_.getFrame(), 
				SERVO_OVERRUN_TIME/60000+" minutes passed \nServo Off and take a rest ...", "SERVO ON OVER RUN", 
				JOptionPane.WARNING_MESSAGE, manager_.ROBOT_ICON);
			servoOnTimer_ = null;
		}
	
		if (currentModel_ == null)	
			return;
		
		if (!currentModel_.update_) {
			double[] angles = currentModel_.getJointValues();
			if (prevAngles_ == null)
				prevAngles_ = angles;
			int i=0;
			for (; i<angles.length; i++) {
				if (angles[i] != prevAngles_[i])
					break;
			}
			if (i != angles.length) {
				String com = "send seq :joint-angles ";
				for (i=0; i<angles.length; i++) {
					double a = Math.toDegrees(angles[i]);
					com += Math.abs(a)<0.01 ? "0 " : String.format("%.1f ",a);
				}
				com += "0.02";
				
				currentPort_.println(com);
				prevAngles_ = angles;
			}
			return;
		}
		_receiveData();
			
		Date now = new Date();
		try {
			if (!requesting_ && !currentPort_.ready() &&
				now.getTime() - prevDate_.getTime() > intervalMS_) {
				requesting_ = true;
				boolean b = currentPort_.isEnabled();
				currentPort_.setEnabled(true);
				currentPort_.println("#request data");
				currentPort_.setEnabled(b);
				prevDate_ = now;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	private void _receiveData() {
		try {
			StringBuffer rowData = null;
			while (currentPort_.ready()) {
				if (rowData == null) 
					rowData = new StringBuffer();
				String str = currentPort_.readLine()+"\n";
				rowData.append(str);
			}
			
			if (rowData != null) {
				WorldStateEx wsx = parse(rowData.toString());
			
				double[] val = wsx.get(modelName_).sensorState.q;
				if (val != null && currentItem_ != null) {
					dynamics_.setCharacterAllLinkData(modelName_, LinkDataType.JOINT_VALUE, val);
					dynamics_.calcWorldForwardKinematics();
					dynamics_.getWorldState(worldStateH_);
					worldStateH_.value.time = wsx.time;
					wsx.setWorldState(worldStateH_.value);
					currentItem_.addValue(wsx.time, wsx);
					requesting_ = false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private WorldStateEx parse(String rowData) {
		//System.out.println(rowData);
		WorldStateEx wsx = new WorldStateEx();
		SensorState s = new SensorState();
		wsx.setSensorState(modelName_, s);
		String[] line = rowData.split("\n");
		for (int i = 0; i < line.length; i++) {
			StringTokenizer st = new StringTokenizer(line[i]);
			if (!st.hasMoreTokens())
				continue;
			String tag = st.nextToken();
			if (tag.equals("#count")) {
				st = new StringTokenizer(line[++i]);
				int run_count = Integer.parseInt(st.nextToken());
				wsx.time = 0.02*(double)run_count;
			} else if (tag.equals("#angle")) {
				st.nextToken();
				int n2 = Integer.parseInt(st.nextToken());
				s.q = new double[n2];
				s.u = new double[n2];
				st = new StringTokenizer(line[++i]);
				for (int k = 0; k < n2; k++) {
					try {
						s.q[k] = Double.parseDouble(st.nextToken());
					} catch (Exception e) {
						s.q[k] = Double.NaN;
					}
				}
			} else if (tag.equals("#force")) {
				int n1 = Integer.parseInt(st.nextToken());
				int n2 = Integer.parseInt(st.nextToken());
				s.force = new double[n1][n2];
				double[][] zmp = new double[n1][2];
				for (int j = 0; j < n1; j++) {
					st = new StringTokenizer(line[++i]);
					for (int k = 0; k < n2; k++)
						s.force[j][k] = Double.parseDouble(st.nextToken());
					if (s.force[j][2] > 1.0) {
						zmp[j][0] = 1000 * -s.force[j][4] / s.force[j][2];
						zmp[j][1] = 1000 * s.force[j][3] / s.force[j][2];
					} else
						zmp[j][0] = zmp[j][1] = 0.0;
				}
			} else if (tag.equals("#rate")) {
				int n1 = Integer.parseInt(st.nextToken());
				int n2 = Integer.parseInt(st.nextToken());
				s.rateGyro = new double[n1][3];
				for (int j = 0; j < n1; j++) {
					st = new StringTokenizer(line[++i]);
					for (int k = 0; k < n2; k++)
						s.rateGyro[j][k] = Double.parseDouble(st.nextToken());
				}
			} else if (tag.equals("#accel")) {
				int n1 = Integer.parseInt(st.nextToken());
				int n2 = Integer.parseInt(st.nextToken());
				s.accel = new double[n1][n2];
				for (int j = 0; j < n1; j++) {
					st = new StringTokenizer(line[++i]);
					for (int k = 0; k < n2; k++)
						s.accel[j][k] = Double.parseDouble(st.nextToken());
				}
			} else if (tag.equals("#adval")) {
				//int n1 = Integer.parseInt(st.nextToken());
				int n2 = Integer.parseInt(st.nextToken());
				double[] adval = new double[n2];
				st = new StringTokenizer(line[++i]);
				for (int j = 0; j < n2; j++) {
					adval[j] = Double.parseDouble(st.nextToken());
				}
			}
		}
		return wsx;
	}	
}

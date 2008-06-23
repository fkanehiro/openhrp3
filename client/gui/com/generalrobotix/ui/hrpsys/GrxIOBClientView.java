/*
 *  GrxIOBClientView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.hrpsys;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import org.python.core.PyList;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.*;
import jp.go.aist.hrp.simulator.IoControlPluginPackage.*;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxPythonScriptItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.view.GrxJythonPromptView;
import com.generalrobotix.ui.view.MenuDialog;

@SuppressWarnings("serial")
public class GrxIOBClientView extends GrxBaseView {
	public static final String TITLE = "IOBClient";

	private static final int NOT_CONNECTED = 0;
	private static final int CONNECTING    = 1;
	private static final int CONNECTED     = 2;

	private static final KeyStroke KS_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0);

	private GrxJythonPromptView jythonView_;

	private PluginManager pluginManager_;
	private Plugin   seqplay_;
	private stateProvider provider_;
	private IoControlPlugin ioCtrl_;

	private DynamicsSimulator dynamics_;
	private GrxWorldStateItem currentItem_;
	private GrxModelItem currentModel_;
	
	private WorldStateHolder worldStateH_  = new WorldStateHolder();
	private RobotStateHolder actualStateH_ = new RobotStateHolder();
	private RobotStateHolder refStateH_  = new RobotStateHolder();
	private StatusSeqHolder servoStateH_ = new StatusSeqHolder();
	private StatusSeqHolder calibStateH_ = new StatusSeqHolder();
	
	private String robotType_ = "-----";
	private String nsHost_    = "-----";
	private int    nsPort_    = 2809;
	private int    interval_  = 200;
	private String setupFile_ = "-----";

	private Date initialDate_;
	private Date prevDate_;
	private int  state_       = NOT_CONNECTED;
	private Thread thread_;

	private ImageIcon startMonitorIcon_ = new ImageIcon(getClass().getResource("/resources/images/sim_start.png"));
	private ImageIcon stopMonitorIcon_  = new ImageIcon(getClass().getResource("/resources/images/sim_stop.png"));
	private ImageIcon servoOnIcon_  = new ImageIcon(getClass().getResource("/resources/images/robot_servo_start.png"));
	private ImageIcon servoOffIcon_ = new ImageIcon(getClass().getResource("/resources/images/robot_servo_stop.png"));
	private ImageIcon  lampOnIcon_  = new ImageIcon(getClass().getResource("/resources/images/lamp_on.png"));
	private ImageIcon  lampOffIcon_ = new ImageIcon(getClass().getResource("/resources/images/lamp_off.png"));
	private JToggleButton btnConnect_ = new JToggleButton(startMonitorIcon_);
	private JToggleButton   btnServo_ = new JToggleButton(servoOnIcon_);
	private JButton         btnSetup_ = new JButton("ROBOT SETUP");
	private JLabel       lblLamp_ = new JLabel(lampOffIcon_);
	private JLabel     lblStatus_ = new JLabel("Status");
	private JTextField fldStatus_ = new JTextField("Not Connected.");
	private JPanel propertyPanel_ = new JPanel();

	private ArrayList<SetPropertyPanel> propList_ = new ArrayList<SetPropertyPanel>();

	public GrxIOBClientView(String name, GrxPluginManager manager) {
		super(name, manager);

		/*
		 *  Create main panel
		 */
		JPanel cpane = getContentPane();
		cpane.setLayout(new BorderLayout());
		JPanel layout1 = new JPanel();
		fldStatus_.setEditable(false);
		fldStatus_.setBackground(Color.white);
		fldStatus_.setPreferredSize(new Dimension(100, 28));
		layout1.add(lblStatus_);
		layout1.add(lblLamp_);
		layout1.add(fldStatus_);
		layout1.add(btnConnect_);
		layout1.add(btnSetup_);
		cpane.add(layout1, BorderLayout.NORTH);

		propList_.add(new SetPropertyPanel("Robot Host",  "nsHost", false, nsHost_));
		propList_.add(new SetPropertyPanel("Robot Port",  "nsPort", false, new Integer(nsPort_).toString()));
		propList_.add(new SetPropertyPanel("Robot Name",  "ROBOT",  false, robotType_));
		propList_.add(new SetPropertyPanel("Interval[ms]","interval", true, new Integer(interval_).toString()));
		propList_.add(new SetPropertyPanel("Setup File",  "setupFile", true, setupFile_));
		propertyPanel_.setLayout(new BoxLayout(propertyPanel_, BoxLayout.Y_AXIS));
		propertyPanel_.setBorder(new TitledBorder(new LineBorder(Color.black), "Propety"));
		for (int i=0; i<propList_.size(); i++) 
			propertyPanel_.add(propList_.get(i));
		JPanel layout2 = new JPanel(new BorderLayout());
		layout2.add(propertyPanel_, BorderLayout.NORTH);
		cpane.add(layout2);

		btnConnect_.setEnabled(false);
		btnConnect_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnConnect_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnConnect_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (btnConnect_.isSelected()) {
					getJythonView().restoreProperties();
					startMonitor(true);
				} else {
					int ans = JOptionPane.showConfirmDialog(manager_.getFrame(), 
							"Are you sure stop monitor ?", "Stop Robot State Monitor",
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
					if (ans == JOptionPane.OK_OPTION)
						stopMonitor();
					else
						btnConnect_.setSelected(true);
				}
			}
		});

		btnSetup_.setEnabled(false);
		btnSetup_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setupFile_ = getProperty("setupFile");
				getJythonView().execFile(GrxXmlUtil.expandEnvVal(setupFile_));
			}
		});

		btnServo_.setEnabled(false);
		btnServo_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnServo_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnServo_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btnServo_.isSelected())
					servoOn();
				else
					servoOff();
			}
		});
		
		JToolBar toolbar = new JToolBar();
		toolbar.add(btnServo_);
		setToolBar(toolbar);
	}

	private class SetPropertyPanel extends JPanel {
		private String propName;	
		private boolean isLocal = true;
		private String defaultVal;

		private JLabel    label = new JLabel();
		private JTextField  fld = new JTextField();
		private JButton     set = new JButton("Set");
		private JButton  resume = new JButton("Resume");

		private String    value;

		public SetPropertyPanel(String _title, String _propName, boolean _isLocal, String _defaultVal) {
			super();
			label.setText(_title);
			propName = _propName;
			isLocal = _isLocal;
			defaultVal = _defaultVal;

			label.setPreferredSize(new Dimension(100, 28));
			fld.setPreferredSize(new Dimension(150, 28));
			fld.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
					if (ks == KS_ENTER) {
					 	set(); 
					} else {
						boolean hasChanged = !fld.getText().equals(getValue());
						set.setEnabled(hasChanged);
						resume.setEnabled(hasChanged);
					}
				}
			});

			set.setEnabled(false);
			set.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					set();
				}
			});

			resume.setEnabled(false);
			resume.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					resume();
				}
			});

			this.add(label);
			this.add(fld);
			this.add(Box.createHorizontalGlue());
			this.add(set);
			this.add(resume);
		}

		public void setEditable(boolean isEditable) {
			fld.setEditable(isEditable);
			resume.setEnabled(isEditable);
			set.setEnabled(isEditable);
		}

		public String getValue() {
			String str;
			if (isLocal)
				str = getProperty(propName);
			else
				str = manager_.getProjectProperty(propName);
			if (str == null)
				str = defaultVal;
			return str;
		}

		public void set() {
			String value = fld.getText();
			if (isLocal)
				setProperty(propName, value);
			else
				manager_.setProjectProperty(propName, value);
			set.setEnabled(false);
			resume.setEnabled(false);
		}

		public void resume() {
			fld.setText(getValue());
			set.setEnabled(false);
			resume.setEnabled(false);
		}
	}

	private void startMonitor(final boolean isInteractive) {
		if (thread_ != null) 
			return;

		thread_ = new Thread() {
			public void run() {
				startMonitorCore(isInteractive);	
			}
		};
		thread_.start();
	}
						
	private void startMonitorCore(boolean isInteractive) {
		btnConnect_.setIcon(stopMonitorIcon_);
		btnConnect_.setToolTipText("Stop Robot State Monitor");
		btnConnect_.setSelected(true);

		setConnectionState(CONNECTING);

		GrxGuiUtil.setEnableRecursive(false, propertyPanel_, null);

		for (int i=0; i<propList_.size(); i++)
			propList_.get(i).resume();

		start();

		try {
			// set property for connection
			nsHost_ = manager_.getProjectProperty("nsHost");
			nsPort_ = Integer.parseInt(manager_.getProjectProperty("nsPort"));

			robotType_ = getStr("ROBOT", robotType_);
			interval_  = getInt("interval", 200);

			currentModel_ = (GrxModelItem) manager_.getItem(GrxModelItem.class, robotType_);
			if (currentModel_ == null)
				return;

			// used to create Plugins
			org.omg.CORBA.Object obj = GrxCorbaUtil.getReference("motionsys", nsHost_, nsPort_);
			pluginManager_ = PluginManagerHelper.narrow(obj);
			
			// used to get robot state (joint angles etc.)
			pluginManager_.load("provider");
			provider_ = stateProviderHelper.narrow(pluginManager_.create("provider", "provider", ""));
			provider_.start();

			// used to get servo state 
			pluginManager_.load("humanoid");
			ioCtrl_ = IoControlPluginHelper.narrow(pluginManager_.create("humanoid", robotType_, ""));
			ioCtrl_.start();

			// for go-actual
			boolean seqplayFlag = isTrue("loadSeqplay", true);
			if (seqplayFlag == true) {
				pluginManager_.load("seqplay");
				seqplay_ = pluginManager_.create("seqplay", "seq", "");
				seqplay_.start();
			}

			// to calculate forward-kinematics
			obj = GrxCorbaUtil.getReference("DynamicsSimulatorFactory", "localhost", 2809);
			DynamicsSimulatorFactory dynFactory = DynamicsSimulatorFactoryHelper.narrow(obj);
			if (dynamics_ != null)  {
				try {
					dynamics_.destroy();
				} catch(Exception e) {
					GrxDebugUtil.printErr("", e);
				}
			}
			dynamics_ = dynFactory.create();
			dynamics_.registerCharacter(robotType_, currentModel_.cInfo_);
			dynamics_.init(0.001, IntegrateMethod.EULER, SensorOption.DISABLE_SENSOR);
			
            // initialize logger
			currentItem_.clearLog();
			currentItem_.registerCharacter(robotType_, currentModel_.cInfo_);

			// initialize servo On button
			if (isAnyServoOn()) {
				btnServo_.setSelected(true);
				btnServo_.setIcon(servoOffIcon_);
				btnServo_.setToolTipText("Servo Off");
			} else {
				btnServo_.setSelected(false);
				btnServo_.setIcon(servoOnIcon_);
				btnServo_.setToolTipText("Servo On");
			}
			initialDate_ = prevDate_ = new Date();

			setConnectionState(CONNECTED);
		} catch (Exception e) {
			GrxDebugUtil.printErr("", e);

			if (isInteractive && currentModel_ == null) {
				JOptionPane.showMessageDialog(manager_.getFrame(),
					"Load Model(" + robotType_ + ") first.",
					"Start Robot State Monitor",
					JOptionPane.WARNING_MESSAGE, manager_.ROBOT_ICON);
				stopMonitor();
			} else {
				setConnectionState(CONNECTING);
			}
		} finally {
			thread_ = null;
		}
	}

	private void stopMonitor() {
		try {
			if (thread_ != null)
				thread_.interrupt();
		} catch (Exception e) {
			GrxDebugUtil.printErr("thread to connect is interrupted.", e);	
		}

		try {
			if (thread_!= null) 
				thread_.join();
		} catch (Exception e) {
			GrxDebugUtil.printErr("join .", e);	
		}

		btnConnect_.setIcon(startMonitorIcon_);
		btnConnect_.setToolTipText("Start Robot State Monitor");
		btnConnect_.setSelected(false);

		setConnectionState(NOT_CONNECTED);

		stop();

		thread_ = null;
	}

	private void setConnectionState(int state) {
		switch (state) {
		case NOT_CONNECTED:
			btnSetup_.setEnabled(false);
			btnServo_.setEnabled(false);
			fldStatus_.setText("Not Connected.");
			lblLamp_.setIcon(lampOffIcon_);
			GrxGuiUtil.setEnableRecursive(true, propertyPanel_, null);
			for (int i=0; i<propList_.size(); i++) {
				propList_.get(i).set.setEnabled(false);
				propList_.get(i).resume.setEnabled(false);
			}
			
			break;

		case CONNECTING:
			btnSetup_.setEnabled(false);
			btnServo_.setEnabled(false);
			fldStatus_.setText("Connecting ...");
			GrxGuiUtil.setEnableRecursive(false, propertyPanel_, null);
			break;

		case CONNECTED:
			btnSetup_.setEnabled(true);
			btnServo_.setEnabled(true);
			fldStatus_.setText("Connected.");
			lblLamp_.setIcon(lampOnIcon_);
			break;
		}
		state_ = state;
	}

	// this rewrites the currentItem_ to start the control function going.
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(currentItem_)) {
			Iterator<GrxBaseItem> it = itemList.iterator();
			currentItem_ = null;
			while (it.hasNext()) {
				GrxBaseItem item = it.next();
				if (item instanceof GrxWorldStateItem)
					currentItem_ = (GrxWorldStateItem) item;
			}
			if (currentItem_ == null) {
				btnConnect_.setEnabled(false);
			} else {
				btnConnect_.setEnabled(true);
			}
		}

		if (!itemList.contains(currentModel_)) {
			if (state_ == CONNECTED)
				setConnectionState(CONNECTING);
		}
	}

	public void restoreProperties() {
		super.restoreProperties();

		startMonitor(false);
	}

	// get the data here and stuff it into currentItem_
	public void control(List<GrxBaseItem> itemList) {
		//if (currentItem_ == null || !btnConnect_.isSelected()) {
		if (currentItem_ == null) {
			stopMonitor();
			return;
		}

		Date now = new Date();
		long time = now.getTime();
		if (prevDate_ != null)
			time -= prevDate_.getTime();

		if (state_ == CONNECTING) {
			if (time > 1000) {
				prevDate_ = now;
				if (lblLamp_.getIcon() == lampOffIcon_)
					lblLamp_.setIcon(lampOnIcon_);
				else
					lblLamp_.setIcon(lampOffIcon_);
				startMonitor(false);
			}
			return;
		}

		if (state_ == CONNECTED && time > interval_) {
			prevDate_ = now;
			try {
				provider_.getActualState(actualStateH_);
				provider_.getReferenceState(refStateH_);
				ioCtrl_.getServoStatus(servoStateH_);
				ioCtrl_.getCalibStatus(calibStateH_); 
				
				dynamics_.setCharacterAllLinkData(robotType_, LinkDataType.JOINT_VALUE, refStateH_.value.angle);
				//actualStateH_.value.angle);
				double[] posatt = new double[12];
				for (int i=0; i<3; i++) 
					posatt[i] = refStateH_.value.basePos[i];
                                       
				for (int i=0; i<9; i++) 
					posatt[i+3] = refStateH_.value.baseAtt[i];
				
				dynamics_.setCharacterLinkData(robotType_, currentModel_.lInfo_[0].name, LinkDataType.ABS_TRANSFORM, posatt);
				dynamics_.calcWorldForwardKinematics();
				dynamics_.getWorldState(worldStateH_);
			} catch (Exception e) {
				GrxDebugUtil.printErr("iobclient got exception:", e);
				setConnectionState(CONNECTING);
				return;
			}
			
			SensorState ss = new SensorState();
			ss.q = actualStateH_.value.angle;
			ss.u = actualStateH_.value.torque;
			ss.force = actualStateH_.value.force;
			ss.accel = actualStateH_.value.accel;
			ss.rateGyro = actualStateH_.value.rateGyro;
			
			WorldStateEx wsx = new WorldStateEx(worldStateH_.value);
			wsx.time = (now.getTime() - initialDate_.getTime()) / 1000.0;
			wsx.collisions = null;
			wsx.setSensorState(robotType_, ss);
			wsx.setTargetState(robotType_, refStateH_.value.angle);
			wsx.setServoState(robotType_, servoStateH_.value);
			wsx.setCalibState(robotType_, calibStateH_.value);
			currentItem_.addValue(wsx.time, wsx);
		}
	}

	private GrxJythonPromptView getJythonView() {
		if (jythonView_ == null) 
			jythonView_= (GrxJythonPromptView)manager_.getView(GrxJythonPromptView.class);

		return jythonView_;
	}

	private void servoOn() {
		int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
				"!! Robot Motion Warning (Servo ON)!!\n" + 
				"Confirm Turn Relay On.\n" + 
				"Then Push [OK] to Servo On.\n", 
				"Servo ON",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				manager_.ROBOT_ICON);
		if (ans == JOptionPane.OK_OPTION) {
			try {
				destroyAllPlugin(false);
				if (seqplay_ != null) 
					seqplay_.sendMsg(":go-actual");

				ioCtrl_.servo("all", SwitchStatus.SWITCH_ON);
				btnServo_.setIcon(servoOffIcon_);
				btnServo_.setToolTipText("Servo Off");
			} catch (Exception e) {
				GrxDebugUtil.printErr("got exception during servo on process:", e);
			}
		} else {
			btnServo_.setSelected(false);
		}
	}

	private void servoOff() {
		int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
				"!! Robot Motion Warning (Servo OFF)!!\n\n" + 
				"Push [OK] to stop Servo.\n", 
				"Servo OFF",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
		if (ans == JOptionPane.OK_OPTION) {
			try {
				ioCtrl_.servo("all", SwitchStatus.SWITCH_OFF);
				btnServo_.setIcon(servoOnIcon_);
				btnServo_.setToolTipText("Servo On");
			} catch (Exception e) {
				GrxDebugUtil.printErr("got exception during servo off process:", e);
			}
		} else {
			btnServo_.setSelected(true);
		}
	}

	public boolean isAnyServoOn() {
		ioCtrl_.getServoStatus(servoStateH_);
		long[] state = servoStateH_.value;
		for (int i=0; i<currentModel_.getDOF(); i++) {
			if (_isSwitchOn(i, state))
				return true;
		}
		return false;
	}

	public boolean isCalibDone(String jname) {
		ioCtrl_.getCalibStatus(calibStateH_);
		long[] state = calibStateH_.value;
		
		if (jname.toLowerCase().equals("all")) {
			for (int i=0; i<currentModel_.getDOF(); i++) {
				if (!_isSwitchOn(i, state))
					return false;
			}
			return true;
		} 

		/*int id = currentModel_.getJointjointId(jname);
		if (id >= 0)
			return _isSwitchOn(id, state);
		else 
			return false;*/
		return false;
	}

	private boolean _isSwitchOn(int ch, long[] state) {
		if (ch >= 0) {
			long a = 1 << (ch % 64);
			if ((state[ch / 64] & a) > 0)
				return true;
		}
		return false;
	}

	public boolean isFloating(double sh) {
		double[][] dat = actualStateH_.value.force;
		double lz = dat[0][2],rz = dat[1][2];
		if (lz<sh && rz<sh)
			return true;
		return false;
	}
	
	public boolean isLanded(double sh) {
		double[][] dat = actualStateH_.value.force;
		double lz = dat[0][2],rz = dat[1][2];
		if (sh<lz && sh<rz)
			return true;
		return false;
	}
	
	public void destroyAllPlugin(boolean isInteractive) {
		try{
			if (isInteractive) {
				int ans = JOptionPane.showConfirmDialog(manager_.getFrame(), 
						"Destroy All Plugins on Robot?", "Destroy All Plugin",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, 
						manager_.ROBOT_ICON);
				if (ans != JOptionPane.OK_OPTION)
					return;
			}

			PluginManager manager = PluginManagerHelper.narrow(GrxCorbaUtil.getReference("motionsys", nsHost_, nsPort_));
			String[] plist = manager.getPluginNames();
			for (int i=0; i<plist.length; i++) {
				try {
					PluginHelper.narrow(GrxCorbaUtil.getReference(plist[plist.length-1-i], nsHost_, nsPort_)).stop();
					String name = plist[plist.length-1-i];
					org.omg.CORBA.Object obj = GrxCorbaUtil.getReference(name, nsHost_, nsPort_);
					if (!obj._is_equivalent(seqplay_) && 
						!obj._is_equivalent(provider_) && 
						!obj._is_equivalent(ioCtrl_))
						manager.sendMsg(":destroy " + name);
				} catch (Exception e) {
					GrxDebugUtil.printErr("destroyAllPlugin:",e);
				}
			}
		} catch (Exception e){
			GrxDebugUtil.printErr("destroyAllPlugin() caught error:", e);
		}
	}
}

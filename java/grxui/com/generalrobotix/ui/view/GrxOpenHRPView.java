// -*- indent-tabs-mode: nil; tab-width: 4; -*-
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
 *  GrxOpenHRPView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import javax.swing.*;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.*;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.util.GrxProcessManager.AProcess;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
import com.generalrobotix.ui.view.simulation.*;

@SuppressWarnings("serial")
public class GrxOpenHRPView extends GrxBaseView {
	public static final String TITLE = "OpenHRP";

	private GrxWorldStateItem currentWorld_;
	private DynamicsSimulator currentDynamics_;
    private List<ControllerAttribute> controllers_ = new ArrayList<ControllerAttribute>();
	private WorldStateHolder stateH_ = new WorldStateHolder();
	private SensorStateHolder cStateH_ = new SensorStateHolder();
	private List<String> robotEntry_ = new ArrayList<String>();
	
	private String nsHost_ = "localhost";
	private int    nsPort_ = 2809;
	private boolean isInteractive_ = true;
	private boolean isExecuting_ = false;
	private boolean isSuspending_ = false;
	private double simTime_ = 0.0;
	private long startTime_ = 0;
	private long suspendedTime_ = 0;
	private boolean isIntegrate_ = true;
    private double stepTime_ = 0.001;
	private double totalTime_ = 20;
	private double logStepTime_ = 0.05;
    private JToggleButton startBtn_ = new JToggleButton();
	private JTabbedPane tabbedPane_ = new JTabbedPane();
	private SimulationParameterPanel simParamPane_ = new SimulationParameterPanel();
	private ControllerPanel controllerPane_ = new ControllerPanel(manager_);
	private CollisionPairPanel collisionPane_ = new CollisionPairPanel(manager_);
	private Thread simThread_;
	private ImageIcon startSimIcon_   = new ImageIcon(getClass().getResource("/resources/images/sim_start.png"));
	private ImageIcon stopSimIcon_    = new ImageIcon(getClass().getResource("/resources/images/sim_stop.png"));
	
	private static final String FORMAT1 = "%8.3f";
	
        private class ControllerAttribute {
		String modelName_;
		Controller controller_;
		double stepTime_;
		int doCount_ = 0;
		boolean doFlag_ = false;
		
		ControllerAttribute(String modelName, Controller controller, double stepTime) {
			modelName_ = modelName;
			controller_ = controller;
			stepTime_ = stepTime;
		}

		private void input(double time) {
			try {
				doFlag_ = false;
				if (doCount_ <= time/stepTime_) {
					doFlag_ = true;
				    doCount_++;
					controller_.input();
				}
			} catch (Exception e) {
				GrxDebugUtil.printErr("Exception in input", e);
			}
		}

		private void control() {
			try {
				if (doFlag_) controller_.control();
			} catch (Exception e) {
				GrxDebugUtil.printErr("Exception in control", e);
			}
		}

		private void output() {
			try {
				if (doFlag_) controller_.output();
			} catch (Exception e) {
				GrxDebugUtil.printErr("Exception in output", e);
			}
		}
	}
	
	public GrxOpenHRPView(String name, GrxPluginManager manager) {
		super(name, manager);

		/* Create content pane */
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(tabbedPane_);
		simParamPane_.setName("simulation");
		controllerPane_.setName("controller");
		collisionPane_.setName("collision");
		simParamPane_.setEnabled(false);
		controllerPane_.setEnabled(false);
		collisionPane_.setEnabled(false);
		tabbedPane_.add(simParamPane_);
		tabbedPane_.add(controllerPane_);
		tabbedPane_.add(collisionPane_);

		/* Create toolbar */
		JToolBar toolbar = new JToolBar();
		toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
		toolbar.add(startBtn_);
		setToolBar(toolbar);
		
		startBtn_.setIcon(startSimIcon_);
		startBtn_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		startBtn_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		startBtn_.setToolTipText("start simulation");
		startBtn_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (startBtn_.isSelected()) {
					startSimulation(true);
				} else {
					isSuspending_ = true;
					int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
						"Simulation suspended.\n"+
						"Click [OK] to finish Simulation.", 
						"Suspend or Finish", 
						JOptionPane.OK_CANCEL_OPTION, 
						JOptionPane.QUESTION_MESSAGE,
						manager_.ROBOT_ICON);
					isSuspending_ = false;
					if (ans == JOptionPane.OK_OPTION)
						stopSimulation();
					else 
						startBtn_.setSelected(true);
				}
			}
		});
	}

	public void restoreProperties() {
		super.restoreProperties();
		nsHost_ = System.getenv("NS_HOST");
		if (nsHost_ == null) {
			nsHost_ = "localhost";
		}

		try {
			nsPort_ = Integer.parseInt(System.getenv("NS_PORT"));
		} catch (Exception e) {
		 	nsPort_ = 2809;
		}
	}
	
	public void startSimulation(boolean isInteractive) {
		if (isExecuting_)
			return;

		if (currentWorld_ == null) {
			stopSimulation();
			return;
		}

		isInteractive_ = isInteractive;
		
		if (isInteractive && currentWorld_.getLogSize() > 0) {
			int ans = JOptionPane.showConfirmDialog(manager_.getFrame(), 
				"The log data will be cleared.\n" +
				"Are you sure to start ?",
				"Start Simulation", 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, 
				manager_.ROBOT_ICON);
			
			if (ans != JOptionPane.OK_OPTION) {
				stopSimulation();
				return;
			}
		}
		
		startBtn_.setIcon(stopSimIcon_);
		startBtn_.setToolTipText("Stop Simulation");
		startBtn_.setSelected(true);
		simParamPane_.setEnabled(false);
		controllerPane_.setEnabled(false);
		collisionPane_.setEnabled(false);

		try {
			List<GrxBaseItem> modelList = manager_.getSelectedItemList(GrxModelItem.class);
			initLogger(modelList);
			if (currentDynamics_ != null) {
				try {
					currentDynamics_.destroy();
				} catch (Exception e) {
					GrxDebugUtil.printErr("", e);
				}
				currentDynamics_ = null;
			}
			currentDynamics_ = initDynamicsSimulator(modelList);

			if (currentDynamics_ == null) {
				stopSimulation();
				JOptionPane.showMessageDialog(manager_.getFrame(), "Failed to initialize DynamicsSimulator.");
				return;
			}
			if (!initController()) {
				stopSimulation();
				JOptionPane.showMessageDialog(manager_.getFrame(), "Failed to initialize Controller.");
				return;
			}
		} catch (Exception e) {
			stopSimulation();
			GrxDebugUtil.printErr("got exception in startSimulation():", e);
			return;
		}

		simThread_ = new Thread() {
			public void run() {
				isExecuting_ = true;
				try {
					Thread.sleep(manager_.getDelay());
					while (isExecuting_) {
						if (isSuspending_) {
							long s = System.currentTimeMillis();
							Thread.sleep(200);
							suspendedTime_ += System.currentTimeMillis() - s;
						} else {
							simLoop(null);
						}
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(manager_.getFrame(),
						"Simulation Interrupted by Exception.", 
						"Simulation Interupted",
						JOptionPane.ERROR_MESSAGE, manager_.ROBOT_ICON);
					stopSimulation();
					GrxDebugUtil.printErr("Simulation Interrupted by Exception:",e);
				}
			}
		};
		simThread_.setPriority(Thread.currentThread().getPriority() + getInt("threadPriority", -1));
		System.out.println("sim.thread priority : "+getInt("threadPriority", -1));
		isIntegrate_ = simParamPane_.isIntegrate();
		totalTime_   = simParamPane_.getTotalTime();
		stepTime_    = simParamPane_.getStepTime();
		logStepTime_ = simParamPane_.getLogStepTime();
		
		simTime_ = 0.0;
		startTime_ = System.currentTimeMillis();
		suspendedTime_ = 0;
		Grx3DView tdview = (Grx3DView)manager_.getView("3DView");
		if (simParamPane_.isViewSimulate()){
			tdview.disableUpdateModel();
			tdview.showViewSimulator();
			GrxLoggerView lgview = (GrxLoggerView)manager_.getView("Logger View");
			lgview.disableControl();
		}

		GrxGraphView graphview = (GrxGraphView)manager_.getView("Graph");
		if(graphview != null)
			graphview.setStepTime((long)(1000000*logStepTime_));

		simThread_.start();
	}

	public void stopSimulation() {
		if (isExecuting_) {
			isExecuting_ = false;
			Grx3DView tdview = (Grx3DView)manager_.getView("3DView");
			tdview.enableUpdateModel();
			GrxLoggerView lgview = (GrxLoggerView)manager_.getView("Logger View");
			lgview.enableControl();
			
			double time = (double)(System.currentTimeMillis() - startTime_ - suspendedTime_)/1000.0;
			String msg = 
				"   (A)Sim. Time = " + String.format(FORMAT1, simTime_) + "[s]\n" +
				"   (B)Real Time = " + String.format(FORMAT1, time) + "[s]\n" +
				"     (B) / (A)  = " + String.format(FORMAT1, time/simTime_);
			try {
				if (Thread.currentThread() != simThread_) {
					//simThread_.interrupt();
					simThread_.join();
				}
			} catch (Exception e) {
				GrxDebugUtil.printErr("stopSimulation:", e);
			}
			System.out.println(getName()+" : "+new Date()+msg.replace(" ", "").replace("\n", " : "));
			if (isInteractive_) {
				JOptionPane.showMessageDialog(manager_.getFrame(), 
					msg, "Simulation Finished", 
					JOptionPane.INFORMATION_MESSAGE,manager_.ROBOT_ICON);
			}

        	if (currentWorld_ != null){
        		currentWorld_.stop();
        		currentWorld_.setPosition(0);
        	}
		}	

		startBtn_.setIcon(startSimIcon_);
		startBtn_.setToolTipText("Start Simulation");
		startBtn_.setSelected(false);
		simParamPane_.setEnabled(true);
		controllerPane_.setEnabled(true);
		collisionPane_.setEnabled(true);
	}
	
	public void waitStopSimulation() {
		try {
			if (Thread.currentThread() != simThread_)
				simThread_.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!isExecuting_) {
			currentWorld_ = (GrxWorldStateItem)manager_.getSelectedItem(GrxWorldStateItem.class, null);
            GrxSimulationItem simItem = (GrxSimulationItem)manager_.getSelectedItem(GrxSimulationItem.class, null);
            if (simItem != null){
                currentWorld_.setDbl("totalTime",
                                     simItem.getDbl("totalTime",com.generalrobotix.ui.item.GrxWorldStateItem.DEFAULT_TOTAL_TIME));
                currentWorld_.setDbl("timeStep",
                                     simItem.getDbl("timeStep",
                                                    com.generalrobotix.ui.item.GrxWorldStateItem.DEFAULT_STEP_TIME));
                currentWorld_.setDbl("gravity", simItem.getDbl("gravity", 9.8));
                //setMethod(item.getProperty("method",METHOD_NAMES[0]));
                //setIntegrate(item.isTrue("integrate", true));
                //setViewSimulate(item.isTrue("viewsimulate", false));
            }
            simParamPane_.updateItem(currentWorld_);
			controllerPane_.updateRobots(itemList);
			collisionPane_.updateCollisionPairs(itemList);
			return;
		}
	}

	private void simLoop(List<GrxBaseItem> itemList) {
		// check time
		if (simTime_ > totalTime_) { // && !extendTime()) {
			stopSimulation();
			return;
		}

		// input
	    for (int i=0; i<controllers_.size(); i++)
	    	controllers_.get(i).input(simTime_);

		// control
		for (int i=0; i<controllers_.size(); i++) 
	    	controllers_.get(i).control();
		
		// integrate or calculate forward kinematics
		if (isIntegrate_) 
			currentDynamics_.stepSimulation();
		else 
			currentDynamics_.calcWorldForwardKinematics();

		// log
		if ((simTime_ % logStepTime_) < stepTime_) {
			currentDynamics_.getWorldState(stateH_);
			WorldStateEx wsx = new WorldStateEx(stateH_.value);
			for (int i=0; i<robotEntry_.size(); i++) {
				String name = robotEntry_.get(i);
				currentDynamics_.getCharacterSensorState(name, cStateH_);
				wsx.setSensorState(name, cStateH_.value);
			}
            if (!isIntegrate_)
                wsx.time = simTime_;
			currentWorld_.addValue(simTime_, wsx);
			if (simParamPane_.isViewSimulate()){
				Grx3DView tdview = (Grx3DView)manager_.getView("3DView");
				tdview.updateModels(wsx);
				tdview.updateViewSimulator(simTime_);
			}
		}	

		// output
		for (int i=0; i<controllers_.size(); i++) 
	    	controllers_.get(i).output();

		simTime_ += stepTime_;
	}
	
	public void shutdown() {
		if (currentDynamics_ != null) {
			try {
				currentDynamics_.destroy();
			} catch (Exception e) {
				GrxDebugUtil.printErr("", e);
			}
			currentDynamics_ = null;
		}
	}
	

	private void initLogger(List<GrxBaseItem> modelList) {
		currentWorld_.clearLog();
		robotEntry_.clear();
		for (int i=0; i<modelList.size(); i++) {
			GrxModelItem model = (GrxModelItem) modelList.get(i);
			if (model.cInfo_ != null) {
				currentWorld_.registerCharacter(model.getName(), model.cInfo_);
				if (model.isRobot()) 
					robotEntry_.add(model.getName());
			}
		}
	}

	private DynamicsSimulator initDynamicsSimulator(List<GrxBaseItem> modelList) {
		DynamicsSimulator dynamics;	
		try {
			org.omg.CORBA.Object obj = GrxCorbaUtil.getReference("DynamicsSimulatorFactory", nsHost_, nsPort_);
			DynamicsSimulatorFactory ifactory = DynamicsSimulatorFactoryHelper.narrow(obj);
			dynamics = ifactory.create();
		} catch (Exception e) {
			GrxDebugUtil.printErr("initDynamicsSimulator: create failed.", e);
			return null;
		}

		try {
			// Register robot to dynamics
			for (int i=0; i<modelList.size(); i++) {
				GrxBaseItem item = modelList.get(i);
				if (item instanceof GrxModelItem) {
					GrxModelItem model = (GrxModelItem) item;
					if (model.cInfo_ != null)
						dynamics.registerCharacter(model.getName(), model.cInfo_);
				}
			}

			// Initialize Simulation
			IntegrateMethod m = IntegrateMethod.RUNGE_KUTTA;
			if (simParamPane_.getMethod().getSelectedIndex() == 1)
				m = IntegrateMethod.EULER;
			dynamics.init(simParamPane_.getStepTime(), m, SensorOption.ENABLE_SENSOR);
			dynamics.setGVector(new double[] { 0.0, 0.0, simParamPane_.getGravity() });

			// Initialize robot state
			for (int i=0; i<modelList.size(); i++) {
				GrxBaseItem item = modelList.get(i);
				if (item instanceof GrxModelItem) {
					GrxModelItem model = (GrxModelItem) modelList.get(i);
					if (model.lInfo_ == null)
						continue;
					String name = model.getName();
					String base = model.lInfo_[0].name; 

					// Set initial robot position and attitude
					dynamics.setCharacterLinkData(
							name, base, LinkDataType.ABS_TRANSFORM, 
							model.getInitialTransformArray(base));

					// Set joint values
					dynamics.setCharacterAllLinkData(
							name, LinkDataType.JOINT_VALUE, 
							model.getInitialJointValues());
			
					// Set joint mode
					JointDriveMode jm = JointDriveMode.TORQUE_MODE;
                	if (isIntegrate_) {
                		double[] jms = model.getInitialJointMode();
						for (int j=0; j<jms.length; j++) {
                  			if (jms[j] > 0) {
								jm = JointDriveMode.HIGH_GAIN_MODE;
								break;
							}
						}
					} else {
						jm = JointDriveMode.HIGH_GAIN_MODE;
					}
					dynamics.setCharacterAllJointModes(name, jm);
				}
			}
			dynamics.calcWorldForwardKinematics();
			
            // Set collision pair 
			List<GrxBaseItem> collisionPair = manager_.getSelectedItemList(GrxCollisionPairItem.class);
			for (int i=0; i<collisionPair.size(); i++) {
				GrxBaseItem pair = collisionPair.get(i);
				dynamics.registerCollisionCheckPair(
						pair.getStr("objectName1", ""), pair.getStr("jointName1", ""), 
						pair.getStr("objectName2", ""), pair.getStr("jointName2", ""), 
						pair.getDbl("staticFriction", 0.5), 
						pair.getDbl("slidingFriction", 0.5),
						pair.getDblAry("springConstant", new double[]{0, 0, 0, 0, 0, 0}), 
						pair.getDblAry("damperConstant", new double[]{0, 0, 0, 0, 0, 0}),
                        0
				); 
			}

			// Initialize server
			dynamics.initSimulation();
			
			stateH_.value = null;
		} catch (Exception e) {
			GrxDebugUtil.printErr("initDynamicsSimulator:", e);
			return null;
		}
		return dynamics;
	}

	private boolean initController() {
		GrxDebugUtil.println("initializing controllers ...");
		for (int i=controllers_.size()-1; i>=0; i--) {
            /*
			try {
				controllers_.get(i).controller_.destroy();
			} catch (Exception e) {}
            */
			controllers_.remove(i);
		}

		List<GrxBaseItem> models = manager_.getSelectedItemList(GrxModelItem.class);
		for (int i=0; i<models.size(); i++) {
			GrxModelItem model = (GrxModelItem) models.get(i);
			if (model.isRobot() && _setupController(model) < 0) 
				return false;
		}
		return true;
	}
	
	private short _setupController(GrxModelItem model) {
		String controllerName = model.getProperty("controller");
		double step = model.getDbl("controlTime", 0.005);
		
		if (controllerName == null || controllerName.equals(""))
			return 0;
		String optionAdd = null;
		if (!simParamPane_.isIntegrate())
			optionAdd = " -nosim";
		
		GrxDebugUtil.println("model name = " + model.getName() + " : controller = " + controllerName + " : cycle time[s] = " + step);
		GrxProcessManagerView pManager = (GrxProcessManagerView)manager_.getView(GrxProcessManagerView.class);

		boolean doRestart = false;
		org.omg.CORBA.Object cobj = GrxCorbaUtil.getReference(controllerName, nsHost_, nsPort_);
		AProcess proc = pManager.processManager.get(controllerName);
		String dir = model.getStr("setupDirectory", "");
		String com = model.getStr("setupCommand", "");
		if (cobj != null) {
			try {
				cobj._non_existent();
				if (isInteractive_ && (!com.equals("") || proc != null)) { // ask only in case being abled to restart process
					int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
						"Controller '"+controllerName+"' may already exist.\n" +
						"Restart it ?","Restart the Controller",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
				
					if (ans == JOptionPane.YES_OPTION)
						doRestart = true;
				}

			} catch (Exception e) {
				cobj = null;
			}
		}

		if (cobj == null || doRestart) {
			if (proc != null)
				proc.stop();

			if (!com.equals("")) {
				com = dir+java.io.File.separator+com;
				ProcessInfo pi = new ProcessInfo();
				pi.id = controllerName;
				pi.dir = dir;
				pi.com.add(com);
				pi.waitCount = 2000;
				pi.nsHost = nsHost_;
				pi.nsPort = nsPort_;
				pi.isCorbaServer = true;
				pi.hasShutdown = true;
				pi.doKillall = false;
				pi.autoStart = false; 
				pi.autoStop = true;
				if (proc != null)
					pManager.processManager.unregister(proc.pi_.id);
				pManager.processManager.register(pi);
				proc = pManager.processManager.get(controllerName);
			}

			if (proc != null) {
				GrxDebugUtil.println("Executing controller process ...");
				GrxDebugUtil.println("dir: " + dir);
				GrxDebugUtil.println("command: " + com);
				proc.start(optionAdd);
			}
		} 

		Date before = new Date();
		int WAIT_COUNT = 4;
		for (int j=0; ; j++) {
			cobj = GrxCorbaUtil.getReference(controllerName, nsHost_, nsPort_);
			if (cobj != null) {
				try {
					Controller controller = ControllerHelper.narrow(cobj);
                    controller.setModelName(model.getName());
                    controller.initialize();
					controller.setDynamicsSimulator(currentDynamics_);

					if (simParamPane_.isViewSimulate()) {
						cobj = GrxCorbaUtil.getReference("ViewSimulator", nsHost_, nsPort_);
						ViewSimulator viewsim = ViewSimulatorHelper.narrow(cobj);
						controller.setViewSimulator(viewsim);
					}
					
					controllers_.add(new ControllerAttribute(model.getName(), controller, step));	
					GrxDebugUtil.println(" connected to the Controller("+controllerName+")\n");
                    controller.setTimeStep(step);
                    controller.initialize();
					controller.start();
					break;
				} catch (Exception e) {
					GrxDebugUtil.printErr("setupController:", e);
				}
			}
			
			if (j > WAIT_COUNT || (new Date().getTime() - before.getTime() > WAIT_COUNT*1000)) {
				GrxDebugUtil.println(" failed to setup controller:"+controllerName);
				int ans = JOptionPane.showConfirmDialog(manager_.getFrame(), 
					"Can't connect the Controller("+controllerName+").\n" +
					"Wait more seconds ?", "Setup Controller",
					JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
				if (ans == JOptionPane.YES_OPTION) {
					before = new Date();
					j=0;
				} else if (ans == JOptionPane.NO_OPTION) {
					break;
				} else {
					return -1;
				}
			} else {
			  try {Thread.sleep(1000);} catch (Exception e) {}
			}
		}
		
		return 1;
	}

	private boolean extendTime() {
		int state = JOptionPane.showConfirmDialog(manager_.getFrame(), 
			"Finish Simulation ?", "Time is up",
			JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE,
			manager_.ROBOT_ICON);
		if (state == JOptionPane.YES_OPTION)
			return false;
		
		String str = null;
		while (true) {
			str = (String) JOptionPane.showInputDialog(manager_.getFrame(),
				"Input time value[s] to extend.", "Extend Time", 
				JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON, null, "5.0");
			if (str == null)
				return false;
			try {
				double d = Double.parseDouble(str);
				if (d > 0) {
					//simParamPane_.setTotalTime(simParamPane_.getTotalTime() + d);
					totalTime_ = totalTime_ + d;
					currentWorld_.extendTime(totalTime_);
					break;
				}
			} catch (NumberFormatException e) {}
		}
		return true;
	}
}

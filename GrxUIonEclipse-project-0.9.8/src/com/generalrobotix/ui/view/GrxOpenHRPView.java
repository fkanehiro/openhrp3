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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import jp.go.aist.hrp.simulator.ClockGenerator;
import jp.go.aist.hrp.simulator.Controller;
import jp.go.aist.hrp.simulator.ControllerHelper;
import jp.go.aist.hrp.simulator.DynamicsSimulator;
import jp.go.aist.hrp.simulator.DynamicsSimulatorFactory;
import jp.go.aist.hrp.simulator.DynamicsSimulatorFactoryHelper;
import jp.go.aist.hrp.simulator.SensorStateHolder;
import jp.go.aist.hrp.simulator.ViewSimulator;
import jp.go.aist.hrp.simulator.ViewSimulatorHelper;
import jp.go.aist.hrp.simulator.WorldStateHolder;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.IntegrateMethod;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.JointDriveMode;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.LinkDataType;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.SensorOption;
import jp.go.aist.hrp.simulator.ClockGeneratorPOA;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;

import RTC.ExtTrigExecutionContextService;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.*;

import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.GrxProcessManager;
import com.generalrobotix.ui.util.GrxProcessManager.AProcess;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
import com.generalrobotix.ui.view.simulation.CollisionPairPanel;
import com.generalrobotix.ui.view.simulation.ControllerPanel;
import com.generalrobotix.ui.view.simulation.SimulationParameterPanel;

@SuppressWarnings("serial")
public class GrxOpenHRPView extends GrxBaseView {
    public static final String TITLE = "OpenHRP";
    private static final int WAIT_COUNT_ = 4;
	private GrxWorldStateItem currentWorld_;
	private List<GrxModelItem> currentModels_;
	private List<GrxCollisionPairItem> currentCollisionPairs_;
	private DynamicsSimulator currentDynamics_;
	private List<ControllerAttribute> controllers_ = new ArrayList<ControllerAttribute>();
	private WorldStateHolder stateH_ = new WorldStateHolder();
	private SensorStateHolder cStateH_ = new SensorStateHolder();
	private List<String> robotEntry_ = new ArrayList<String>();
	private ClockGenerator_impl clockGenerator_ = new ClockGenerator_impl();
	
	private boolean isInteractive_ = true;
	private boolean isExecuting_ = false;
	private boolean isSuspending_ = false;
	private double simulateTime_ = 0;
	private boolean isIntegrate_ = true;
	private double stepTime_ = 0.001;
	private double totalTime_ = 20;
	private double logStepTime_ = 0.05;
	private boolean isSimulatingView_;
	private IAction action_ = null;

	private SimulationParameterPanel simParamPane_;
	private ControllerPanel controllerPane_;
	private CollisionPairPanel collisionPane_;

	private Thread simThread_;
	private static final int interval_ = 100; //[ms]
	private Grx3DView view3D;
	
	private static final String FORMAT1 = "%8.3f";
	private Object lock2_ = new Object();
	
	/**
	 * @brief implementation of ClockGenerator interface
	 */
	private class ClockGenerator_impl extends ClockGeneratorPOA {
		private Vector<ExecutionContext> ecs_;
		private double simTime_ = 0.0;

		/**
		 * @brief This class manages execution timing of execution context
		 */
		private class ExecutionContext {
			public ExtTrigExecutionContextService ec_;
			private double period_;
			private double nextExecutionTime_;
			
			/**
			 * @brief constructor
			 * @param ec execution context
			 * @param period ExtTrigExecutionContextService.tick() is called with this period
			 */
			ExecutionContext(ExtTrigExecutionContextService ec, double period){
				ec_ = ec;
				period_ = period;
				reset();
			}
			
			/**
			 * @param t current time in the simulation world
			 * @return true if executed successfully, false otherwise
			 */
			boolean execute(double t){
				if (t >= nextExecutionTime_){
					try{
						ec_.tick();
						nextExecutionTime_ += period_;
					}catch(Exception ex){
						return false;
					}
				}
				return true;
			}

			/**
			 * @brief reset
			 */
			void reset(){
				nextExecutionTime_ = 0;
			}
		}
		
		/**
		 * @brief constructor
		 */
		ClockGenerator_impl(){
			ecs_ = new Vector<ExecutionContext>();
		}
		
		/**
		 * @brief register an execution context with its execution period
		 * @param ec execution context
		 * @param period execution period
		 */
		public void subscribe(ExtTrigExecutionContextService ec, double period) {
			System.out.println("ClockGenerator::subscribe("+ec+", "+period);
			ecs_.add(new ExecutionContext(ec, period));
		}

		/**
		 * @brief remove execution context from execution list
		 * @param ec execution context
		 */
		public void unsubscribe(ExtTrigExecutionContextService ec) {
			System.out.println("ClockGenerator::unsubscribe("+ec+")");
			for (int i=0; i<ecs_.size(); i++){
				ExecutionContext ec2 = ecs_.get(i);
				if (ec._is_equivalent(ec2.ec_)){
					ecs_.remove(ec2);
					return;
				}
			}
		}
		
		private static final int EXEC = -1;
		private static final int TIMEOVER = 0;
		private static final int STOP = 1;
		private static final int INTERRUPT = 2;
		private int simThreadState_ =  EXEC;
		private Object lock_ = new Object();
		
		public boolean startSimulation(boolean isInteractive) {
			
			if (isExecuting_){
				GrxDebugUtil.println("[HRP]@startSimulation now executing.");
				return false;
			}

			if (currentWorld_ == null) {
				MessageDialog.openError(null, "Failed to start simulation", "There is no WorldState item.");
				GrxDebugUtil.println("[HRP]@startSimulation there is no world.");
				return false;
			}
			
			isInteractive_ = isInteractive;

			if (isInteractive && currentWorld_.getLogSize() > 0) {
	            boolean ans = MessageDialog.openConfirm(getParent().getShell(), "Start Simulation", "The log data will be cleared.\n" + "Are you sure to start ?");
				
				if (ans != true) {
					return false;
				}
			}

			simParamPane_.setEnabled(false);
			controllerPane_.setEnabled(false);
			collisionPane_.setEnabled(false);
			
			currentWorld_.clearLog();
			try {
				if (!initDynamicsSimulator()) {
	                MessageDialog.openInformation(getParent().getShell(),"", "Failed to initialize DynamicsSimulator.");
					return false;
				}
				if (!initController()) {
	                MessageDialog.openInformation(getParent().getShell(), "", "Failed to initialize Controller.");
					return false;
				}
			} catch (Exception e) {
				GrxDebugUtil.printErr("SimulationLoop:", e);
				return false;
			}

			simThread_ = _createSimulationThread();
			isIntegrate_ = simParamPane_.isIntegrate();
			totalTime_   = simParamPane_.getTotalTime();
			stepTime_    = simParamPane_.getStepTime();
			logStepTime_ = simParamPane_.getLogStepTime();
			
			isSimulatingView_ = simParamPane_.isSimulatingView();
			if(isSimulatingView_){
				view3D =  (Grx3DView)manager_.getView( Grx3DView.class );
				if(view3D==null){
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
					IWorkbenchPage page = window.getActivePage();
					try {
						page.showView("com.generalrobotix.ui.view.Grx3DViewPart", null, IWorkbenchPage.VIEW_CREATE);  
					} catch (PartInitException e1) {
						e1.printStackTrace();
					}
					view3D =  (Grx3DView)manager_.getView( Grx3DView.class );
				}
			}
			
			currentWorld_.startSimulation(isSimulatingView_);
			
			// TODO disable "start simulation" button and menu
			_resetClockReceivers();

			simTime_ = 0.0;
			simulateTime_ = 0;
			currentWorld_.init();
			simThreadState_ =  EXEC;
			simThread_.start();
			
			Runnable run = new Runnable(){
				public void run() {
					switch(simThreadState_){
						case TIMEOVER:
							if(extendTime())
								simThreadState_=EXEC;
							else
								simThreadState_=STOP;
							synchronized(lock_){ 
								lock_.notifyAll();
							}
						case EXEC:
							if(isSimulatingView_){
								currentWorld_.setPosition(currentWorld_.getLogSize()-1,view3D);
							}
							Display display = composite_.getDisplay();
					        if ( display!=null && !display.isDisposed()){
					        	display.timerExec(interval_, this);
					        }
					        break;
						case STOP:
							endOfSimulation();
							synchronized(lock2_){ 
								lock2_.notifyAll();
							}
							break;
						case INTERRUPT:
							MessageDialog.openError( GrxUIPerspectiveFactory.getCurrentShell(),
											"Simulation Interrupted", "Simulation Interrupted by Exception.");
							endOfSimulation();
							break;
						default :
							break;
					}	
				}
			};
			Display display = composite_.getDisplay();
	        if ( display!=null && !display.isDisposed()){
	        	display.timerExec(interval_, run);
	        }
	        
			GrxDebugUtil.println("[OpenHRP]@startSimulation Start Thread and end this function.");
			return true;
		}

		String timeMsg_;
		String updateTimeMsg(){
			timeMsg_ = 
				"   (A)Sim. Time = " + String.format(FORMAT1, simTime_) + "[s]\n" +
				"   (B)Real Time = " + String.format(FORMAT1, simulateTime_) + "[s]\n" +
				"     (B) / (A)  = " + String.format(FORMAT1, simulateTime_/simTime_);
			return timeMsg_;
		}


		private Thread _createSimulationThread(){
			Thread thread = new Thread(){
				public void run() {
					isExecuting_ = true;
					long suspendT = 0;
					long startT = System.currentTimeMillis();
					try {
						while (isExecuting_) {
							if (isSuspending_) {
								long s = System.currentTimeMillis();
								Thread.sleep(200);
								suspendT += System.currentTimeMillis() - s;
							} else {
								if (!simulateOneStep()){
									long s = System.currentTimeMillis();
									simThreadState_ = TIMEOVER;
									synchronized(lock_){
											lock_.wait();
									}
									suspendT += System.currentTimeMillis() - s;
									if(simThreadState_==STOP)
										break;
								}
							}
						}
						isExecuting_ = false;
						simulateTime_ += (System.currentTimeMillis() - startT - suspendT)/1000.0;
						
			    		for (ControllerAttribute i: controllers_) {
			    			i.deactive();
			    		}
                        currentWorld_.setLogMenus(true);
			    		simThreadState_ = STOP;

					} catch (Exception e) {
						GrxDebugUtil.printErr("Simulation Interrupted by Exception:",e);
						isExecuting_ = false;
						simThreadState_ = INTERRUPT;
					}
				}
			};

			thread.setPriority(Thread.currentThread().getPriority() - 1);
			return thread;
		}
		/**
		 * reset execution contexts(clock receivers)
		 */
		private void _resetClockReceivers(){
			for (int i=0; i<ecs_.size();){
				ExecutionContext ec = ecs_.get(i);
				try{
					System.out.println(i+": rate = "+ec.ec_.get_rate());
					ec.reset();
					i++;
				}catch(Exception ex){
					ecs_.remove(ec);
				}
			}
		}
		
		public void continueSimulation(){
			simThread_ = _createSimulationThread();
			simThread_.start();
		}

		public void stopSimulation() {
			if (isExecuting_) 
				isExecuting_ = false;
		}
		
		public void endOfSimulation(){
				
				updateTimeMsg();
				System.out.println(new java.util.Date()+timeMsg_.replace(" ", "").replace("\n", " : "));
				if (isInteractive_) {
					isInteractive_ = false;
					execSWT( new Runnable(){
							public void run(){
						        MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),
                                        "Simulation Finished", timeMsg_);
							}
						} ,
						Thread.currentThread() != simThread_
					);
				}
				
				syncExec(new Runnable(){
					public void run() {
						currentWorld_.setPosition(0);
						currentWorld_.stopSimulation();
	                }
	            } );
	        	
			/*
			startBtn_.setIcon(startSimIcon_);
			startBtn_.setToolTipText("Start Simulation");
			startBtn_.setSelected(false);
			*/
			execSWT( new Runnable(){
					public void run(){
						simParamPane_.setEnabled(true);
						controllerPane_.setEnabled(true);
						collisionPane_.setEnabled(true);
						if (action_ != null){
							action_.setToolTipText("Start Simulation");
							action_.setText("Start Simulation");
							action_.setImageDescriptor(Activator.getDefault().getDescriptor("sim_start.png"));
							action_ = null;
						}
					}
				}, 
				Thread.currentThread() != simThread_
			);
		}
		
		/**
		 * @biref simulate one step
		 * @return true if simulatin should be continued, false otherwise
		 */
		private boolean simulateOneStep() {
			if (simTime_ > totalTime_ ) {
				return false;
			}

			// input
		    for (int i = 0; i<controllers_.size(); i++) {
		    	ControllerAttribute attr = 
		    		(ControllerAttribute)controllers_.get(i);
		    	attr.input(simTime_);
		    }
		    
			simTime_ += stepTime_;

			// control
			for (int i = 0; i < controllers_.size(); i++) {
				ControllerAttribute attr = controllers_.get(i);
				attr.control();
			}
			for (int i=0; i<ecs_.size();){
				ExecutionContext ec = ecs_.get(i);
				if (ec.execute(simTime_)){
					i++;
				}else{
					ecs_.remove(ec);
				}
			}
			
			// simulate
			if (isIntegrate_) {
				currentDynamics_.stepSimulation();
			} else {
				currentDynamics_.calcWorldForwardKinematics();
			}
	
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
         
				if (isSimulatingView_){
					view3D._showCollision(wsx.collisions);
					view3D.updateModels(wsx);
					view3D.updateViewSimulator(wsx.time);
				}
			}
			
			// output
			for (int i = 0; i < controllers_.size(); i++) {
				ControllerAttribute attr = controllers_.get(i);
				attr.output();
			}
			return true;
		}
		
	}
	   
	/**
	 * @brief start simulation
	 * @param isInteractive flag to be interactive. If false is given, any dialog boxes are not displayed during this simulation
	 */
	public void startSimulation(boolean isInteractive, IAction action){
		if (clockGenerator_.startSimulation(isInteractive)){
			action_ = action;
			if (action_ != null){
				action_.setToolTipText("Stop Simulation");
				action_.setText("Stop Simulation");
				action_.setImageDescriptor(Activator.getDefault().getDescriptor("sim_stop.png"));
			}
		}
	}

	public void waitStopSimulation() {
		try {
			synchronized(lock2_){ 
				lock2_.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @brief stop simulation
	 */
	public void stopSimulation(){
		clockGenerator_.stopSimulation();
	}
	
	private class ControllerAttribute {
		String modelName_;
		String controllerName_;
		Controller controller_;
		double stepTime_;
		int doCount_ = 0;
		boolean doFlag_ = false;
		ControllerAttribute(String modelName, String controllerName, Controller controller, double stepTime) {
			modelName_ = modelName;
			controllerName_ = controllerName;
			controller_ = controller;
			stepTime_ = stepTime;
		}
		
		private void reset(Controller controller, double stepTime) {
			controller_ = controller;
			stepTime_ = stepTime;
			doCount_ = 0;
			doFlag_ = false;
		}
		
        private void input(double time){
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
        
        private void deactive(){
            try {
            	controller_.stop();
            } catch (Exception e) {
                GrxDebugUtil.printErr("Exception in deactive", e); 
            }
        }
        private void active() {
            try {
            	controller_.initilize();
            } catch (Exception e) {
                GrxDebugUtil.printErr("Exception in active", e); 
            }
        }
	}
	
	public GrxOpenHRPView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager,vp,parent);
		
        TabFolder folder = new TabFolder(composite_,SWT.TOP);
        
        TabItem tabItem = new TabItem(folder,SWT.NONE);
        tabItem.setText("simulation");
        simParamPane_ = new SimulationParameterPanel(folder, SWT.NONE);
        tabItem.setControl(simParamPane_);
        simParamPane_.setEnabled(true);//false
        
        tabItem = new TabItem(folder,SWT.NONE);
        tabItem.setText("controller");
        controllerPane_ = new ControllerPanel(folder, SWT.NONE, manager_);
        tabItem.setControl(controllerPane_);
        controllerPane_.setEnabled(true);//false

        
        tabItem = new TabItem(folder,SWT.NONE);
        tabItem.setText("collision");
        collisionPane_ = new CollisionPairPanel(folder, SWT.NONE, manager_);
        tabItem.setControl(collisionPane_);
        controllerPane_.setEnabled(true);//false
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        
        
        currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
		simParamPane_.updateItem(currentWorld_);
		if(currentWorld_!=null)
			currentWorld_.addObserver(this);
		currentModels_ = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
		controllerPane_.updateRobots(currentModels_);
		currentCollisionPairs_ = manager_.<GrxCollisionPairItem>getSelectedItemList(GrxCollisionPairItem.class);
		collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
		manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
		manager_.registerItemChangeListener(this, GrxModelItem.class);
		manager_.registerItemChangeListener(this, GrxCollisionPairItem.class);
		
		NamingContext rootnc = GrxCorbaUtil.getNamingContext();
	       
		ClockGenerator cg = clockGenerator_._this(manager_.orb_);
		NameComponent[] path = {new NameComponent("ClockGenerator", "")};
	       
		try {
			rootnc.rebind(path, cg);
		} catch (Exception ex) {
			GrxDebugUtil.println("OpenHRPView : failed to bind ClockGenerator to NamingService");
		}
		GrxDebugUtil.println("OpenHRPView : ClockGenerator is successfully registered to NamingService");

	}
		
	void execSWT( Runnable r, boolean execInCurrentThread ){
		if( execInCurrentThread ) {
			r.run();
		}else{
			Display display = Display.getDefault();
			if ( display!=null && !display.isDisposed())
					display.asyncExec(r);
		}
	}

	public void registerItemChange(GrxBaseItem item, int event){
		if(item instanceof GrxWorldStateItem){
			GrxWorldStateItem witem = (GrxWorldStateItem) item;
	    	switch(event){
	    	case GrxPluginManager.SELECTED_ITEM:
	    		if(currentWorld_!=witem){
	    			simParamPane_.updateItem(witem);
	    			currentWorld_ = witem;
	    			currentWorld_.addObserver(this);
	    		}
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentWorld_==witem){
	    			simParamPane_.updateItem(null);
	    			currentWorld_.deleteObserver(this);
	    			currentWorld_ = null;
	    		}
	    		break;
	    	default:
	    		break;
	    	}
		}else if(item instanceof GrxModelItem){
			GrxModelItem mitem = (GrxModelItem)item;
			switch(event){
			case GrxPluginManager.SELECTED_ITEM:
				if(!currentModels_.contains(mitem)){
					currentModels_.add(mitem);
					controllerPane_.updateRobots(currentModels_);
					collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
				}
				break;
			case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentModels_.contains(mitem)){
		    		currentModels_.remove(mitem);
					controllerPane_.updateRobots(currentModels_);
					collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
	    		}
	    		break;
	    	default:
	    		break;	
			}
		}else if(item instanceof GrxCollisionPairItem){
			GrxCollisionPairItem citem = (GrxCollisionPairItem)item;
			switch(event){
			case GrxPluginManager.SELECTED_ITEM:
			case GrxPluginManager.ADD_ITEM:
				if(!currentCollisionPairs_.contains(citem)){
					currentCollisionPairs_.add(citem);
					controllerPane_.updateRobots(currentModels_);
					collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
				}
				break;
			case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentCollisionPairs_.contains(citem)){
		    		currentCollisionPairs_.remove(citem);
					controllerPane_.updateRobots(currentModels_);
					collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
	    		}
	    		break;
	    	default:
	    		break;	
			}
		}
	}
	
	public void update(GrxBasePlugin plugin, Object... arg) {
		if(currentWorld_!=plugin) return;
    	if((String)arg[0]=="PropertyChange")
    		simParamPane_.updateItem(currentWorld_);	
    	return;
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
		
		if(currentWorld_!=null)
    		currentWorld_.deleteObserver(this);
		
		manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
		manager_.removeItemChangeListener(this, GrxModelItem.class);
		manager_.removeItemChangeListener(this, GrxCollisionPairItem.class);
	}
	

	public boolean initDynamicsSimulator() {
		getDynamicsSimulator(true);

		try {
			List<GrxModelItem> modelList = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
			robotEntry_.clear();
			for (int i=0; i<modelList.size(); i++) {
				GrxModelItem model = modelList.get(i);
				if (model.links_ == null)
					continue;

				currentWorld_.registerCharacter(model.getName(), model.getBodyInfo());
				currentDynamics_.registerCharacter(model.getName(), model.getBodyInfo());
				if (model.isRobot()) {
					robotEntry_.add(model.getName());
				}
			}

			IntegrateMethod m = IntegrateMethod.RUNGE_KUTTA;
			if (simParamPane_.getMethod().getSelectedIndex() == 1)
				m = IntegrateMethod.EULER;
			currentDynamics_.init(simParamPane_.getStepTime(), m, SensorOption.ENABLE_SENSOR);

			currentDynamics_.setGVector(new double[] { 0.0, 0.0, simParamPane_.getGravity() });
			
			for (int i=0; i<modelList.size(); i++) {
				GrxModelItem model = modelList.get(i);
				if (model.links_ == null)
					continue;

				// SET INITIAL ROBOT POSITION AND ATTITUDE 				
				GrxLinkItem base = model.rootLink(); 
				currentDynamics_.setCharacterLinkData(
					model.getName(), base.getName(), LinkDataType.ABS_TRANSFORM, 
					model.getInitialTransformArray(base));
				
			
				// SET I/O MODE OF JOINTS
				if (isIntegrate_) {
            		double[] jms = model.getInitialJointMode();
					for (int j=0; j<jms.length; j++) {
						double[] mode = new double[1];
						mode[0] = jms[j];
						currentDynamics_.setCharacterLinkData(model.getName(), model.links_.get(j).getName(), LinkDataType.POSITION_GIVEN, mode );
					}
				} else {
					currentDynamics_.setCharacterAllJointModes(model.getName(), JointDriveMode.HIGH_GAIN_MODE);
				}
				
				
				// SET INITIAL JOINT VALUES
				currentDynamics_.setCharacterAllLinkData(
					model.getName(), LinkDataType.JOINT_VALUE, 
					model.getInitialJointValues());
			}
			currentDynamics_.calcWorldForwardKinematics();
			
            // SET COLLISION PAIR 
			List<GrxBaseItem> collisionPair = manager_.getSelectedItemList(GrxCollisionPairItem.class);
			for (int i=0; i<collisionPair.size(); i++) {
				GrxCollisionPairItem item = (GrxCollisionPairItem) collisionPair.get(i);
				currentDynamics_.registerCollisionCheckPair(
						item.getStr("objectName1", ""), 
						item.getStr("jointName1", ""), 
						item.getStr("objectName2", ""),
						item.getStr("jointName2", ""), 
						item.getDbl("staticFriction", 0.5),
						item.getDbl("slidingFriction", 0.5),
						item.getDblAry("springConstant",new double[]{0.0,0.0,0.0,0.0,0.0,0.0}), 
						item.getDblAry("damperConstant",new double[]{0.0,0.0,0.0,0.0,0.0,0.0}),
						item.getDbl("cullingThresh", 0.01)); 
			}
			currentDynamics_.initSimulation();
			
			stateH_.value = null;
		} catch (Exception e) {
			GrxDebugUtil.printErr("initDynamicsSimulator:", e);
			return false;
		}
		return true;
	}

	public DynamicsSimulator getDynamicsSimulator(boolean update) {
		if (update && currentDynamics_ != null) {
			try {
				currentDynamics_.destroy();
			} catch (Exception e) {
				GrxDebugUtil.printErr("", e);
			}
			currentDynamics_ = null;
		}
		
		if (currentDynamics_ == null) {
			try {
				org.omg.CORBA.Object obj = //process_.get(DynamicsSimulatorID_).getReference();
				GrxCorbaUtil.getReference("DynamicsSimulatorFactory");
				DynamicsSimulatorFactory ifactory = DynamicsSimulatorFactoryHelper.narrow(obj);
				currentDynamics_ = ifactory.create();
				currentDynamics_._non_existent();

			} catch (Exception e) {
				GrxDebugUtil.printErr("getDynamicsSimulator: create failed.");
				e.printStackTrace();
				currentDynamics_ = null;
			}
		}
		return currentDynamics_;
	}

	private boolean initController() {
		boolean ret = true;
		List<String> localStrList = new Vector<String>();
        for (GrxModelItem model: manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class) ) {
            if( model.isRobot() ){
        		if ( _setupController(model, _getControllerFromControllerName(model.getProperty("controller"))) < 0 )
        			ret =  false;
            	localStrList.add( model.getProperty("controller") );
            }
        }
        _refreshControllers( localStrList );
        return ret;
	}
	
	private ControllerAttribute _getController(String localID){
		ControllerAttribute ret = null;
		for (ControllerAttribute i: controllers_) {
			if ( i.modelName_.equals(localID) ){
				ret = i;
				break;
			}
		}
		return ret;
	}

	private ControllerAttribute _getControllerFromControllerName(String controllerName){
		ControllerAttribute ret = null;
		for (ControllerAttribute i: controllers_) {
			if ( i.controllerName_.equals(controllerName) ){
				ret = i;
				break;
			}
		}
		return ret;
	}

	
	private void _refreshControllers(List<String> refStrList){
		Vector<String> localStrVec = new Vector<String>();
		for (ControllerAttribute i: controllers_) {
			int index = refStrList.indexOf(i.controllerName_);
			if ( index < 0 )
				localStrVec.add(i.controllerName_);
		}
		for (String i: localStrVec) {
		    GrxProcessManager  pManager = GrxProcessManager.getInstance();
            AProcess proc = pManager.get(i);
            if( proc != null && proc.stop() ){
            	pManager.unregister(proc.pi_.id);
            	_getControllerFromControllerName(i);
            	int index = controllers_.indexOf( _getControllerFromControllerName(i) );
    			if ( index >= 0 )
    				controllers_.remove(index);
            }
		}
	}
	
	private short _setupController(GrxModelItem model, ControllerAttribute deactivatedController) {
        String controllerName = model.getProperty("controller");
        double step = model.getDbl("controlTime", 0.005);
		
		if (controllerName == null || controllerName.equals(""))
			return 0;
		String optionAdd = null;
		if (!simParamPane_.isIntegrate())
			optionAdd = " -nosim";
		
        GrxDebugUtil.println("model name = " + model.getName() + " : controller = " + controllerName + " : cycle time[s] = " + step);
        GrxProcessManager  pManager = GrxProcessManager.getInstance();

        boolean doRestart = false;
        org.omg.CORBA.Object cobj = GrxCorbaUtil.getReference(controllerName);
        AProcess proc = pManager.get(controllerName);
        String dir = model.getStr("setupDirectory", "");
        String com = model.getStr("setupCommand", "");
        
        if (cobj != null) {
            try {
                cobj._non_existent();
                if (isInteractive_ && (!com.equals("") || proc != null)) { // ask only in case being abled to restart process
                    MessageDialog dialog =new MessageDialog(getParent().getShell(),"Restart the Controller",null,
                        "Controller '"+controllerName+"' may already exist.\n" + "Restart it ?" ,MessageDialog.QUESTION, new String[]{"YES","NO","CANCEL"}, 2);
                    switch( dialog.open() ){
                    case 0: // 0 == "YES"
                    	doRestart = true;
                    	break;
                    case 1: // 1 == "NO"
                    	if(deactivatedController != null)
                    		deactivatedController.active();
                    	break;
                    default:
                    	return -1;
                    }
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
                String osname = System.getProperty("os.name");
                if(osname.indexOf("Windows") >= 0){
                    com = "\"" + com + "\"";
                }
                ProcessInfo pi = new ProcessInfo();
                pi.id = controllerName;
                pi.dir = dir;
                pi.com.add(com);
                pi.waitCount = 2000;
                pi.isCorbaServer = true;
                pi.hasShutdown = true;
                pi.doKillall = false;
                pi.autoStart = false;
                pi.autoStop = true;
                if (proc != null)
                    pManager.unregister(proc.pi_.id);
                pManager.register(pi);
                proc = pManager.get(controllerName);
            }

            if (proc != null) {
                GrxDebugUtil.println("Executing controller process ...");
                GrxDebugUtil.println("dir: " + dir);
                GrxDebugUtil.println("command: " + com);
                proc.start(optionAdd);
            }
        }
		
        Date before = new Date();
        for (int j=0; ; j++) {
            cobj = GrxCorbaUtil.getReference(controllerName);
            if (cobj != null) {
                try {
					Controller controller = ControllerHelper.narrow(cobj);
                    controller.setModelName(model.getName());
                    controller.setDynamicsSimulator(currentDynamics_);
                    controller.initilize();

                    if (simParamPane_.isSimulatingView()) {
                        cobj = GrxCorbaUtil.getReference("ViewSimulator");
                        ViewSimulator viewsim = ViewSimulatorHelper.narrow(cobj);
                        controller.setViewSimulator(viewsim);
                    }
                    ControllerAttribute refAttr = _getControllerFromControllerName(controllerName);
                    if( refAttr == null){
                    	controllers_.add(new ControllerAttribute(model.getName(), controllerName, controller, step));
                    }else{
                    	refAttr.reset(controller, step);
                    }
                    GrxDebugUtil.println(" connected to the Controller("+controllerName+")\n");
                    controller.setTimeStep(step);
                    controller.start();
                    break;
                } catch (Exception e) {
                    GrxDebugUtil.printErr("setupController:", e);
                }
            }

            if (j > WAIT_COUNT_ || (new Date().getTime() - before.getTime() > WAIT_COUNT_*1000)) {
                GrxDebugUtil.println(" failed to setup controller:"+controllerName);
                //タイトル画像をなしにするにはどうすればいいのか？とりあえずnullにしてみた
                MessageDialog dialog = new MessageDialog(getParent().getShell(),"Setup Controller",null,"Can't connect the Controller("+controllerName+").\n" +"Wait more seconds ?",MessageDialog.QUESTION,new String[]{"YES","NO","CANCEL"}, 2);
                int ans = dialog.open();
                if (ans == 0) {
                    before = new Date();
                    j=0;
                } else if (ans == 1) {
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
        
		boolean state = MessageDialog.openQuestion(getParent().getShell(), "Time is up", "Finish Simulation ?");
		if (state == true)
			return false;
		
		String str = null;
		while (true) {
            InputDialog dialog = new InputDialog(getParent().getShell(),"Extend Time","Input time value[s] to extend.","5.0",null);
            int result = dialog.open();
			str = dialog.getValue();
			if (result == InputDialog.CANCEL)
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
	
	private boolean asyncExec(Runnable r){
		Display display = composite_.getDisplay();
        if ( display!=null && !display.isDisposed()){
            display.asyncExec( r );
            return true;
        }else
        	return false;
	}
	
	protected boolean syncExec(Runnable r){
		Display display = composite_.getDisplay();
        if ( display!=null && !display.isDisposed()){
            display.syncExec( r );
            return true;
        }else
        	return false;
	}
	
	public boolean isSimulating(){
		return isExecuting_;
	}
}

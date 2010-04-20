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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import jp.go.aist.hrp.simulator.BodyInfo;
import jp.go.aist.hrp.simulator.ClockGenerator;
import jp.go.aist.hrp.simulator.Controller;
import jp.go.aist.hrp.simulator.ControllerHelper;
import jp.go.aist.hrp.simulator.DynamicsSimulator;
import jp.go.aist.hrp.simulator.DynamicsSimulatorFactory;
import jp.go.aist.hrp.simulator.DynamicsSimulatorFactoryHelper;
import jp.go.aist.hrp.simulator.OnlineViewer;
import jp.go.aist.hrp.simulator.SensorStateHolder;
import jp.go.aist.hrp.simulator.ViewSimulator;
import jp.go.aist.hrp.simulator.ViewSimulatorHelper;
import jp.go.aist.hrp.simulator.WorldStateHolder;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.IntegrateMethod;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.JointDriveMode;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.LinkDataType;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.SensorOption;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;

//import RTC.ExtTrigExecutionContextService;
import com.generalrobotix.ui.depends.rtm.SwitchDependVerClockGenerator;

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
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.GrxProcessManager.AProcess;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
import com.generalrobotix.ui.view.simulation.SimulationParameterPanel;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxOpenHRPView extends GrxBaseView {
    public static final String TITLE = "OpenHRP"; //$NON-NLS-1$
    private static final int WAIT_COUNT_ = 4;
    private GrxWorldStateItem currentWorld_;
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
    private double viewSimulationStep_=0;
    private IAction action_ = null;

    private SimulationParameterPanel simParamPane_;

    private Thread simThread_;
    private static final int interval_ = 10; //[ms]
    private Grx3DView view3D;
    
    private static final String FORMAT1 = "%8.3f"; //$NON-NLS-1$
    private Object lock2_ = new Object();
    
    /**
     * @brief implementation of ClockGenerator interface
     */
    private class ClockGenerator_impl extends SwitchDependVerClockGenerator {
        private double simTime_ = 0.0;

        private static final int EXEC = -1;
        private static final int TIMEOVER = 0;
        private static final int STOP = 1;
        private static final int INTERRUPT = 2;
        private int simThreadState_ =  EXEC;
        private Object lock_ = new Object();
        private Object lock3_ = new Object();
        private boolean viewSimulationUpdate_ = false;
        private WorldStateEx wsx_=null;
        
        public boolean startSimulation(boolean isInteractive) {
            
            if (isExecuting_){
                GrxDebugUtil.println("[HRP]@startSimulation now executing."); //$NON-NLS-1$
                return false;
            }

            if (currentWorld_ == null) {
                MessageDialog.openError(null, MessageBundle.get("GrxOpenHRPView.dialog.title.Fail"), MessageBundle.get("GrxOpenHRPView.dialog.message.noWorldState")); //$NON-NLS-1$ //$NON-NLS-2$
                GrxDebugUtil.println("[HRP]@startSimulation there is no world."); //$NON-NLS-1$
                return false;
            }
            
            isInteractive_ = isInteractive;

            if (isInteractive && currentWorld_.getLogSize() > 0) {
                boolean ans = MessageDialog.openConfirm(getParent().getShell(), MessageBundle.get("GrxOpenHRPView.dialog.title.start"), MessageBundle.get("GrxOpenHRPView.dialog.message.start0") + MessageBundle.get("GrxOpenHRPView.dialog.message.start1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                
                if (ans != true) {
                    return false;
                }
            }
            GrxServerManagerView viewServerManager =  (GrxServerManagerView)manager_.getView( GrxServerManagerView.class );
            if(viewServerManager != null){
                viewServerManager.setEditableHostPortText(false);
            }
            simParamPane_.setEnabled(false);
            
            currentWorld_.clearLog();
            try {
                if (!initDynamicsSimulator()) {
                    MessageDialog.openInformation(getParent().getShell(),"", MessageBundle.get("GrxOpenHRPView.dialog.message.failedInit")); //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                }
                if (!initController()) {
                    MessageDialog.openInformation(getParent().getShell(), "", MessageBundle.get("GrxOpenHRPView.dialog.message.failedController")); //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                }
            } catch (Exception e) {
                GrxDebugUtil.printErr("SimulationLoop:", e); //$NON-NLS-1$
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
                        page.showView("com.generalrobotix.ui.view.Grx3DViewPart", null, IWorkbenchPage.VIEW_CREATE);   //$NON-NLS-1$
                    } catch (PartInitException e1) {
                        e1.printStackTrace();
                    }
                    view3D =  (Grx3DView)manager_.getView( Grx3DView.class );
                }
            }
            
            currentWorld_.startSimulation(isSimulatingView_);
            
            // TODO disable "start simulation" button and menu
            resetClockReceivers();

            simTime_ = 0.0;
            simulateTime_ = 0;
            currentWorld_.init();
            simThreadState_ =  EXEC;
            viewSimulationUpdate_ = false;
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
                            	if(viewSimulationUpdate_){
	                            	view3D._showCollision(wsx_.collisions);
	                                view3D.updateModels(wsx_);
	                                view3D.updateViewSimulator(wsx_.time);
	                                currentWorld_.setPosition(currentWorld_.getLogSize()-1,view3D);
	                                synchronized(lock3_){
		                                viewSimulationUpdate_=false;
		                                lock3_.notify();
	                                }
                            	}
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
                                            MessageBundle.get("GrxOpenHRPView.dialog.title.Interrupt"), MessageBundle.get("GrxOpenHRPView.dialog.message.Interrupt")); //$NON-NLS-1$ //$NON-NLS-2$
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
            
            GrxDebugUtil.println("[OpenHRP]@startSimulation Start Thread and end this function."); //$NON-NLS-1$
            return true;
        }

        String timeMsg_;
        String updateTimeMsg(){
            timeMsg_ = 
                MessageBundle.get("GrxOpenHRPView.dialog.message.simTime0") + String.format(FORMAT1, simTime_) + MessageBundle.get("GrxOpenHRPView.dialog.message.simTime1") + //$NON-NLS-1$ //$NON-NLS-2$
                MessageBundle.get("GrxOpenHRPView.dialog.message.simTime2") + String.format(FORMAT1, simulateTime_) + MessageBundle.get("GrxOpenHRPView.dialog.message.simTime3") + //$NON-NLS-1$ //$NON-NLS-2$
                MessageBundle.get("GrxOpenHRPView.dialog.message.simTime4") + String.format(FORMAT1, simulateTime_/simTime_); //$NON-NLS-1$
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
                                    synchronized(lock_){
                                        simThreadState_ = TIMEOVER;
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
                        GrxDebugUtil.printErr("Simulation Interrupted by Exception:",e); //$NON-NLS-1$
                        isExecuting_ = false;
                        simThreadState_ = INTERRUPT;
                    }
                }
            };

            thread.setPriority(Thread.currentThread().getPriority() - 1);
            return thread;
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
                System.out.println(new java.util.Date()+timeMsg_.replace(" ", "").replace("\n", " : ")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                if (isInteractive_) {
                    isInteractive_ = false;
                    execSWT( new Runnable(){
                            public void run(){
                                MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),
                                        MessageBundle.get("GrxOpenHRPView.dialog.title.finish"), timeMsg_); //$NON-NLS-1$
                            }
                        } ,
                        Thread.currentThread() != simThread_
                    );
                }
                
                syncExec(new Runnable(){
                    public void run() {
                        currentWorld_.stopSimulation();
                        currentWorld_.setPosition(0);
                    }
                } );
                
            /*
            startBtn_.setIcon(startSimIcon_);
            startBtn_.setToolTipText("Start Simulation");
            startBtn_.setSelected(false);
            */
            execSWT( new Runnable(){
                    public void run(){
                        GrxServerManagerView viewServerManager =  (GrxServerManagerView)manager_.getView( GrxServerManagerView.class );
                        if(viewServerManager != null){
                            viewServerManager.setEditableHostPortText(true);
                        }
                        simParamPane_.setEnabled(true);
                        if (action_ != null){
                            action_.setToolTipText(MessageBundle.get("GrxOpenHRPView.text.start")); //$NON-NLS-1$
                            action_.setText(MessageBundle.get("GrxOpenHRPView.menu.start")); //$NON-NLS-1$
                            action_.setImageDescriptor(Activator.getDefault().getDescriptor("sim_start.png")); //$NON-NLS-1$
                            action_ = null;
                        }
                    }
                }, 
                Thread.currentThread() != simThread_
            );
        }
        
        /**
         * @brief simulate one step
         * @return true if simulation should be continued, false otherwise
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
            updateExecutionContext(simTime_);           
            
            // simulate
            if (isIntegrate_) {
                currentDynamics_.stepSimulation();
            } else {
                currentDynamics_.calcWorldForwardKinematics();
            }
    
            // log
            wsx_=null;
            if ((simTime_ % logStepTime_) < stepTime_) {
                currentDynamics_.getWorldState(stateH_);
                wsx_ = new WorldStateEx(stateH_.value);
                for (int i=0; i<robotEntry_.size(); i++) {
                    String name = robotEntry_.get(i);
                    currentDynamics_.getCharacterSensorState(name, cStateH_);
                    wsx_.setSensorState(name, cStateH_.value);
                }
                if (!isIntegrate_)
                    wsx_.time = simTime_;
                currentWorld_.addValue(simTime_, wsx_);
            }
            
            // viewSimlulation update
            if(isSimulatingView_){
            	if ((simTime_ % viewSimulationStep_) < stepTime_) {
            		if(wsx_==null){
            			currentDynamics_.getWorldState(stateH_);
                        wsx_ = new WorldStateEx(stateH_.value);
                        for (int i=0; i<robotEntry_.size(); i++) {
                            String name = robotEntry_.get(i);
                            currentDynamics_.getCharacterSensorState(name, cStateH_);
                            wsx_.setSensorState(name, cStateH_.value);
                        }
                        if (!isIntegrate_)
                            wsx_.time = simTime_;	
            		}
            		synchronized(lock3_){
            			try {
            				viewSimulationUpdate_ = true;
							lock3_.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
            		}
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
        if(action == null)
            action = getStartSimulationAction();

        if (clockGenerator_.startSimulation(isInteractive)){
            action_ = action;
            if (action_ != null){
                action_.setToolTipText(MessageBundle.get("GrxOpenHRPView.text.stop")); //$NON-NLS-1$
                action_.setText(MessageBundle.get("GrxOpenHRPView.menu.stop")); //$NON-NLS-1$
                action_.setImageDescriptor(Activator.getDefault().getDescriptor("sim_stop.png")); //$NON-NLS-1$
            }
        }
    }
   
    private IAction getStartSimulationAction()
    {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        for(IWorkbenchWindow w : windows){
            if(!(w instanceof ApplicationWindow))
                continue;
            ApplicationWindow window = (ApplicationWindow)w;
            ICoolBarManager coolbar = window.getCoolBarManager2();
            if(coolbar == null)
                continue;
            IContributionItem setitem = coolbar.find("com.generalrobotix.ui.actionSet");
            if(setitem != null && setitem instanceof ToolBarContributionItem)
            {
                IToolBarManager toolbar = ((ToolBarContributionItem)setitem).getToolBarManager();
                if(toolbar == null)
                    continue;
                IContributionItem actitem = toolbar.find("com.generalrobotix.ui.actions.StartSimulate");
                if(actitem != null && actitem instanceof ActionContributionItem)
                    return ((ActionContributionItem)actitem).getAction();
            }
        }
        return null;
    }

    public void waitStopSimulation() throws InterruptedException {
        try {
            synchronized(lock2_){ 
                lock2_.wait();
            }
        } catch (InterruptedException e) {
            clockGenerator_.stopSimulation();
            throw e;
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
                GrxDebugUtil.printErr("Exception in input", e);  //$NON-NLS-1$
            }
        }

        private void control() {
            try {
                if (doFlag_) controller_.control();
            } catch (Exception e) {
                GrxDebugUtil.printErr("Exception in control", e);  //$NON-NLS-1$
            }
        }

        private void output() {
            try {
                if (doFlag_) controller_.output();
            } catch (Exception e) {
                GrxDebugUtil.printErr("Exception in output", e);  //$NON-NLS-1$
            }
        }
        
        private void deactive(){
            try {
                controller_.stop();
            } catch (Exception e) {
                GrxDebugUtil.printErr("Exception in deactive", e);  //$NON-NLS-1$
            }
        }
        private void active() {
            try {
                controller_.initialize();
            } catch (Exception e) {
                GrxDebugUtil.printErr("Exception in active", e);  //$NON-NLS-1$
            }
        }
    }
    
    public GrxOpenHRPView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager,vp,parent);
        
        simParamPane_ = new SimulationParameterPanel(composite_, SWT.NONE);
        simParamPane_.setEnabled(true);//false
        
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        
        
        currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
        simParamPane_.updateItem(currentWorld_);
        if(currentWorld_!=null)
            currentWorld_.addObserver(this);
        
        manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
        registerCORBA();
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
        }
    }
    
    public void update(GrxBasePlugin plugin, Object... arg) {
        if(currentWorld_==plugin){
            if((String)arg[0]=="PropertyChange") //$NON-NLS-1$
                simParamPane_.updateItem(currentWorld_);    
        }
    }
    
    public boolean registerCORBA() {
        NamingContext rootnc = GrxCorbaUtil.getNamingContext();
        ClockGenerator cg = clockGenerator_._this(manager_.orb_);
        NameComponent[] path = {new NameComponent("ClockGenerator", "")};
           
        try {
            rootnc.rebind(path, cg);
            GrxDebugUtil.println("OpenHRPView : ClockGenerator is successfully registered to NamingService");
        } catch (Exception ex) {
            GrxDebugUtil.println("OpenHRPView : failed to bind ClockGenerator to NamingService");
        }
        return true;
    }
    
    public void unregisterCORBA() {
        NamingContext rootnc = GrxCorbaUtil.getNamingContext();
        NameComponent[] path = {new NameComponent("ClockGenerator", "")};
        try{
            rootnc.unbind(path);
            GrxDebugUtil.println("OpenHRPView : successfully ClockGenerator unbound to localhost NameService");
        }catch(Exception ex){
            GrxDebugUtil.println("OpenHRPView : failed to unbind ClockGenerator to localhost NameService");
        }
    }
    
    public void shutdown() {
        if (currentDynamics_ != null) {
            try {
                currentDynamics_.destroy();
            } catch (Exception e) {
                GrxDebugUtil.printErr("", e); //$NON-NLS-1$
            }
            currentDynamics_ = null;
        }
        
        if(currentWorld_!=null)
            currentWorld_.deleteObserver(this);
        
        manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
    }
    

    public boolean initDynamicsSimulator() {
        getDynamicsSimulator(true);

        try {
            List<GrxModelItem> modelList = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
            robotEntry_.clear();
            float cameraFrameRate = 1;
            for (int i=0; i<modelList.size(); i++) {
                GrxModelItem model = modelList.get(i);
                if (model.links_ == null)
                    continue;
                if(model.isModified()){
                    MessageDialog.openInformation(null, "", MessageBundle.get("GrxOpenHRPView.dialog.message.saveModel0")+model.getName()+MessageBundle.get("GrxOpenHRPView.dialog.message.saveModel1")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    if(!model.saveAndLoad())
                        return false;
                }
                BodyInfo bodyInfo = model.getBodyInfo();
                if(bodyInfo==null)  return false;
                currentWorld_.registerCharacter(model.getName(), bodyInfo);
                currentDynamics_.registerCharacter(model.getName(), bodyInfo);
                if (model.isRobot()) {
                    robotEntry_.add(model.getName());
                }
                List<Camera_impl> cameraList = model.getCameraSequence();
                for (int j=0; j<cameraList.size(); j++) {
                    Camera_impl camera = cameraList.get(j);
                    float frameRate = camera.getCameraParameter().frameRate;
                    cameraFrameRate = lcm(cameraFrameRate, frameRate);
                }
            }
            viewSimulationStep_ = 1.0d/cameraFrameRate;
            
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
                        item.getStr("objectName1", ""),  //$NON-NLS-1$ //$NON-NLS-2$
                        item.getStr("jointName1", ""),  //$NON-NLS-1$ //$NON-NLS-2$
                        item.getStr("objectName2", ""), //$NON-NLS-1$ //$NON-NLS-2$
                        item.getStr("jointName2", ""),  //$NON-NLS-1$ //$NON-NLS-2$
                        item.getDbl("staticFriction", 0.5), //$NON-NLS-1$
                        item.getDbl("slidingFriction", 0.5), //$NON-NLS-1$
                        item.getDblAry("springConstant",new double[]{0.0,0.0,0.0,0.0,0.0,0.0}),  //$NON-NLS-1$
                        item.getDblAry("damperConstant",new double[]{0.0,0.0,0.0,0.0,0.0,0.0}), //$NON-NLS-1$
                        item.getDbl("cullingThresh", 0.01));  //$NON-NLS-1$
            }
            currentDynamics_.initSimulation();
            
            stateH_.value = null;
        } catch (Exception e) {
            GrxDebugUtil.printErr("initDynamicsSimulator:", e); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    public DynamicsSimulator getDynamicsSimulator(boolean update) {
        if (update && currentDynamics_ != null) {
            try {
                currentDynamics_.destroy();
            } catch (Exception e) {
                GrxDebugUtil.printErr("", e); //$NON-NLS-1$
            }
            currentDynamics_ = null;
        }
        
        if (currentDynamics_ == null) {
            try {
                org.omg.CORBA.Object obj = //process_.get(DynamicsSimulatorID_).getReference();
                GrxCorbaUtil.getReference("DynamicsSimulatorFactory"); //$NON-NLS-1$
                DynamicsSimulatorFactory ifactory = DynamicsSimulatorFactoryHelper.narrow(obj);
                currentDynamics_ = ifactory.create();
                currentDynamics_._non_existent();

            } catch (Exception e) {
                GrxDebugUtil.printErr("getDynamicsSimulator: create failed."); //$NON-NLS-1$
                e.printStackTrace();
                currentDynamics_ = null;
            }
        }
        return currentDynamics_;
    }

    private boolean initController() {
        boolean ret = true;
        List<String> localStrList = new Vector<String>();
        List<GrxModelItem> modelList = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        for (GrxModelItem model : modelList ) {
            if( model.isRobot() ){
                localStrList.add( model.getProperty("controller") ); //$NON-NLS-1$
            }
        }
        _refreshControllers( localStrList );
        for (GrxModelItem model: modelList ) {
            if( model.isRobot() ){
                if ( _setupController(model, _getControllerFromControllerName(model.getProperty("controller"))) < 0 ) //$NON-NLS-1$
                    ret =  false;
            }
        }
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
        String controllerName = model.getProperty("controller"); //$NON-NLS-1$
        double step = model.getDbl("controlTime", 0.005); //$NON-NLS-1$
        
        if (controllerName == null || controllerName.equals("")) //$NON-NLS-1$
            return 0;
        String optionAdd = null;
        if (!simParamPane_.isIntegrate())
            optionAdd = " -nosim"; //$NON-NLS-1$
        
        GrxDebugUtil.println("model name = " + model.getName() + " : controller = " + controllerName + " : cycle time[s] = " + step); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        GrxProcessManager  pManager = GrxProcessManager.getInstance();

        boolean doRestart = false;
        org.omg.CORBA.Object cobj = GrxCorbaUtil.getReference(controllerName);
        AProcess proc = pManager.get(controllerName);
        String dir = model.getStr("setupDirectory", ""); //$NON-NLS-1$ //$NON-NLS-2$
        String com = model.getStr("setupCommand", ""); //$NON-NLS-1$ //$NON-NLS-2$
        
        if (cobj != null) {
            try {
                cobj._non_existent();
                if (isInteractive_ && (!com.equals("") || proc != null)) { // ask only in case being abled to restart process //$NON-NLS-1$
                    MessageDialog dialog =new MessageDialog(getParent().getShell(),MessageBundle.get("GrxOpenHRPView.dialog.title.restartController"),null, //$NON-NLS-1$
                        MessageBundle.get("GrxOpenHRPView.dialog.message.restartController0")+controllerName+MessageBundle.get("GrxOpenHRPView.dialog.message.restartController1") + MessageBundle.get("GrxOpenHRPView.dialog.message.restartController2") ,MessageDialog.QUESTION, new String[]{MessageBundle.get("GrxOpenHRPView.dialog.button.yes"),MessageBundle.get("GrxOpenHRPView.dialog.button.no"),MessageBundle.get("GrxOpenHRPView.dialog.button.cancel")}, 2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
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

            if (!com.equals("")) { //$NON-NLS-1$
                com = dir+java.io.File.separator+com;
                String osname = System.getProperty("os.name"); //$NON-NLS-1$
                if(osname.indexOf("Windows") >= 0){ //$NON-NLS-1$
                    com = "\"" + com + "\""; //$NON-NLS-1$ //$NON-NLS-2$
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
                GrxDebugUtil.println("Executing controller process ..."); //$NON-NLS-1$
                GrxDebugUtil.println("dir: " + dir); //$NON-NLS-1$
                GrxDebugUtil.println("command: " + com); //$NON-NLS-1$
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
                    controller.initialize();

                    if (simParamPane_.isSimulatingView()) {
                        cobj = GrxCorbaUtil.getReference("ViewSimulator"); //$NON-NLS-1$
                        ViewSimulator viewsim = ViewSimulatorHelper.narrow(cobj);
                        controller.setViewSimulator(viewsim);
                    }
                    ControllerAttribute refAttr = _getControllerFromControllerName(controllerName);
                    if( refAttr == null){
                        controllers_.add(new ControllerAttribute(model.getName(), controllerName, controller, step));
                    }else{
                        refAttr.reset(controller, step);
                    }
                    GrxDebugUtil.println(" connected to the Controller("+controllerName+")\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    controller.setTimeStep(step);
                    controller.start();
                    break;
                } catch (Exception e) {
                    GrxDebugUtil.printErr("setupController:", e); //$NON-NLS-1$
                }
            }

            if (j > WAIT_COUNT_ || (new Date().getTime() - before.getTime() > WAIT_COUNT_*1000)) {
                GrxDebugUtil.println(" failed to setup controller:"+controllerName); //$NON-NLS-1$
                //タイトル画像をなしにするにはどうすればいいのか？とりあえずnullにしてみた
                MessageDialog dialog = new MessageDialog(getParent().getShell(),MessageBundle.get("GrxOpenHRPView.dialog.title.setupController"),null,MessageBundle.get("GrxOpenHRPView.dialog.message.setupController0")+controllerName+").\n" +MessageBundle.get("GrxOpenHRPView.dialog.message.setupController1"),MessageDialog.QUESTION,new String[]{MessageBundle.get("GrxOpenHRPView.dialog.button.yes"),MessageBundle.get("GrxOpenHRPView.dialog.button.no"),MessageBundle.get("GrxOpenHRPView.dialog.button.cancel")}, 2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
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
        if(!isInteractive_)
            return false;
        boolean state = MessageDialog.openQuestion(getParent().getShell(), MessageBundle.get("GrxOpenHRPView.dialog.title.timeUp"), MessageBundle.get("GrxOpenHRPView.dialog.message.TimeUp")); //$NON-NLS-1$ //$NON-NLS-2$
        if (state == true)
            return false;
        
        String str = null;
        while (true) {
            InputDialog dialog = new InputDialog(getParent().getShell(),MessageBundle.get("GrxOpenHRPView.dialog.title.ExtendTime"),MessageBundle.get("GrxOpenHRPView.dialog.message.extendTime"),"5.0",null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
    
   
    public int gcd(int a, int b){
    	BigInteger bigA = BigInteger.valueOf(a);
    	BigInteger bigB = BigInteger.valueOf(b);
    	return bigA.gcd(bigB).intValue();
    }
    
    public int lcm(int a, int b){
    	return a*b/gcd(a,b);
    }
    
    public float lcm(float a, float b){
    	return (float)lcm((int)a, (int)b);
    }

}

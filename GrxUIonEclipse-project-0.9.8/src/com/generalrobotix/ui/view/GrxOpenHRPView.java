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

import jp.go.aist.hrp.simulator.DynamicsSimulator;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;

import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.view.simulation.SimulationParameterPanel;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxOpenHRPView extends GrxBaseView {
    public static final String TITLE = "OpenHRP"; //$NON-NLS-1$
    private GrxWorldStateItem currentWorld_;
    private SimulationParameterPanel simParamPane_;

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
        
        GrxSimulationItem simItem = (GrxSimulationItem)manager_.getItem("simulation");
		simItem.addObserver(this);
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
    	if((String)arg[0]=="StartSimulation")
			simParamPane_.setEnabled(false);
        else if((String)arg[0]=="StopSimulation")
			simParamPane_.setEnabled(true);
        if(currentWorld_==plugin){
            if((String)arg[0]=="PropertyChange") //$NON-NLS-1$
                simParamPane_.updateItem(currentWorld_);  
        }
    }
    
    public void shutdown() {
        if(currentWorld_!=null)
            currentWorld_.deleteObserver(this);
        
        manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
        
        GrxSimulationItem simItem = (GrxSimulationItem)manager_.getItem("simulation");
		simItem.deleteObserver(this);
    }
    
    //  Python scriptからの呼び出しのために　public宣言されていたメソッドを残す　　　//
    /**
     * @brief start simulation
     * @param isInteractive flag to be interactive. If false is given, any dialog boxes are not displayed during this simulation
     */
    public void startSimulation(boolean isInteractive, IAction action){
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	simItem.startSimulation(isInteractive);
    }
 
    public void waitStopSimulation() throws InterruptedException {
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	simItem.waitStopSimulation();
    }
    
    /**
     * @brief stop simulation
     */
    public void stopSimulation(){
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	simItem.stopSimulation();
    }
    
    public boolean registerCORBA() {
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	return simItem.registerCORBA();
    }
    
    public void unregisterCORBA() {
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	simItem.unregisterCORBA();
    }
 
    public boolean initDynamicsSimulator() {
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	return simItem.initDynamicsSimulator();
    }

    public DynamicsSimulator getDynamicsSimulator(boolean update) {
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	return simItem.getDynamicsSimulator(update);
    }
  
    public boolean isSimulating(){
    	GrxSimulationItem simItem = (GrxSimulationItem) manager_.getItem("simulation");
    	return simItem.isSimulating();
    }

}

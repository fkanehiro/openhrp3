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
    private GrxSimulationItem simItem_=null;
    private SimulationParameterPanel simParamPane_;

    public GrxOpenHRPView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager,vp,parent);
        
        simParamPane_ = new SimulationParameterPanel(composite_, SWT.NONE);
        simParamPane_.setEnabled(true);//false
        
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
 
        setUp();
        manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
		manager_.registerItemChangeListener(this, GrxSimulationItem.class);
    }    
    
    public void setUp(){
    	if(currentWorld_ != null)
    		currentWorld_.deleteObserver(this);
    	currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
        simParamPane_.updateLogTime(currentWorld_);
        if(currentWorld_!=null)
            currentWorld_.addObserver(this);
        if(simItem_ != null)
        	simItem_.deleteObserver(this);
        simItem_ = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
		simParamPane_.updateItem(simItem_);
		if(simItem_!=null){
			simItem_.addObserver(this);
		}
    }
   
    public void registerItemChange(GrxBaseItem item, int event){
        if(item instanceof GrxWorldStateItem){
            GrxWorldStateItem witem = (GrxWorldStateItem) item;
            switch(event){
            case GrxPluginManager.SELECTED_ITEM:
                if(currentWorld_!=witem){
                    simParamPane_.updateLogTime(witem);
                    currentWorld_ = witem;
                    currentWorld_.addObserver(this);
                }
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                if(currentWorld_==witem){
                    simParamPane_.updateLogTime(null);
                    currentWorld_.deleteObserver(this);
                    currentWorld_ = null;
                }
                break;
            default:
                break;
            }
        }else if(item instanceof GrxSimulationItem){
    		GrxSimulationItem simItem = (GrxSimulationItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(simItem_!=simItem){
    				simParamPane_.updateItem(simItem);
    				simItem_ = simItem;
    				simItem_.addObserver(this);
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(simItem_==simItem){
	    			simParamPane_.updateItem(null);
	    			simItem_.deleteObserver(this);
	    			simItem_ = null;
	    		}
	    		break;
	    	default:
	    		break;
    		}
    	}
    }
    
    public void update(GrxBasePlugin plugin, Object... arg) {
        if(simItem_==plugin){
	    	if((String)arg[0]=="StartSimulation")
                simParamPane_.setEnabled(false);
	        else if((String)arg[0]=="StopSimulation")
                simParamPane_.setEnabled(true);
            else if((String)arg[0]=="PropertyChange") //$NON-NLS-1$
                simParamPane_.updateItem(simItem_); 
        }else if(currentWorld_==plugin){
            if((String)arg[0]=="PropertyChange") //$NON-NLS-1$
                simParamPane_.updateLogTime(currentWorld_);  
        }
    }
    
    public void shutdown() {
        if(currentWorld_!=null)
            currentWorld_.deleteObserver(this);
        
        manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
        
        if(simItem_!=null)
			simItem_.deleteObserver(this);
		manager_.removeItemChangeListener(this, GrxSimulationItem.class);
    }
    
    //  Python scriptからの呼び出しのために　public宣言されていたメソッドを残す　　　//
    /**
     * @brief start simulation
     * @param isInteractive flag to be interactive. If false is given, any dialog boxes are not displayed during this simulation
     */
    public void startSimulation(boolean isInteractive, IAction action){
		simParamPane_.fixParam();
		if(simItem_==null){
			simItem_ = (GrxSimulationItem)manager_.createItem(GrxSimulationItem.class, null);
			simItem_.addObserver(this);
			manager_.itemChange(simItem_, GrxPluginManager.ADD_ITEM);
			manager_.setSelectedItem(simItem_, true);
		}
		simItem_.startSimulation(isInteractive);
    }
 
    public void waitStopSimulation() throws InterruptedException {
    	simItem_.waitStopSimulation();
    }
    
    /**
     * @brief stop simulation
     */
    public void stopSimulation(){
    	simItem_.stopSimulation();
    }
    
    public boolean registerCORBA() {
    	if(simItem_==null){
			simItem_ = (GrxSimulationItem)manager_.createItem(GrxSimulationItem.class, null);
			simItem_.addObserver(this);
			manager_.itemChange(simItem_, GrxPluginManager.ADD_ITEM);
			manager_.setSelectedItem(simItem_, true);
		}
    	return simItem_.registerCORBA();
    }
    
    public void unregisterCORBA() {
    	simItem_.unregisterCORBA();
    }
 
    public boolean initDynamicsSimulator() {
    	if(simItem_==null){
			simItem_ = (GrxSimulationItem)manager_.createItem(GrxSimulationItem.class, null);
			simItem_.addObserver(this);
			manager_.itemChange(simItem_, GrxPluginManager.ADD_ITEM);
			manager_.setSelectedItem(simItem_, true);
		}
    	return simItem_.initDynamicsSimulator();
    }

    public DynamicsSimulator getDynamicsSimulator(boolean update) {
    	if(simItem_==null){
			simItem_ = (GrxSimulationItem)manager_.createItem(GrxSimulationItem.class, null);
			simItem_.addObserver(this);
			manager_.itemChange(simItem_, GrxPluginManager.ADD_ITEM);
			manager_.setSelectedItem(simItem_, true);
		}
    	return simItem_.getDynamicsSimulator(update);
    }
  
    public boolean isSimulating(){
    	if(simItem_!=null)
    		return simItem_.isSimulating();
    	else
    		return false;
    }

    public void fixParam(){
        simParamPane_.fixParam();
    }
}

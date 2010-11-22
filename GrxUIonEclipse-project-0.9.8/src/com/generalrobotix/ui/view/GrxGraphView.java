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
 *  GrxGraphView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxTimeSeriesItem;
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.view.graph.DataItem;
import com.generalrobotix.ui.view.graph.DataItemInfo;
import com.generalrobotix.ui.view.graph.GraphPanel;
import com.generalrobotix.ui.view.graph.Time;
import com.generalrobotix.ui.view.graph.TrendGraph;
import com.generalrobotix.ui.view.graph.TrendGraphModel;

@SuppressWarnings("serial")
public class GrxGraphView extends GrxBaseView {
    public static final String TITLE = "Graph";
	public static final int NumOfGraph = 3;
	
	private GrxWorldStateItem currentWorld_ = null;
    private GrxSimulationItem simItem_ = null;
	private GrxGraphItem currentGraph_ = null;
	private List<GrxModelItem> currentModels_ = new ArrayList<GrxModelItem>();
	private TrendGraphModel graphManager_;
	private GraphPanel gpanel_;
	
	public GrxGraphView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent)  {
		super(name, manager, vp, parent);
		isScrollable_ = false;
		graphManager_ = new TrendGraphModel(NumOfGraph );
		
		gpanel_ = new GraphPanel(manager_, graphManager_, composite_ );
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        graphManager_.setPanel(gpanel_);
        
        setUp();
        manager_.registerItemChangeListener(this, GrxSimulationItem.class);
        manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
        manager_.registerItemChangeListener(this, GrxGraphItem.class);   
        manager_.registerItemChangeListener(this, GrxModelItem.class);
	}

	public void setUp(){
		if(currentGraph_!=null){
        	_updateGraph(null);
        }
        currentGraph_ = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
        if(currentGraph_!=null){
        	_updateGraph(currentGraph_);
        }
		if(currentWorld_!=null){
			currentWorld_.deleteObserver(this);
            currentWorld_.deletePosObserver(this);
		}
		currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
        if(currentWorld_!=null){
        	currentWorld_.addObserver(this);
        	currentWorld_.addPosObserver(this);
        	graphManager_.setWorldState(currentWorld_);
        }else
        	graphManager_.setWorldState(null);
        graphManager_.updateGraph();
        
        if(simItem_!=null)
        	simItem_.deleteObserver(this);
        simItem_ = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
        if(simItem_!=null)
            simItem_.addObserver(this);
        
        currentModels_ = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        gpanel_.setModelList(currentModels_);
	}
	
    public void registerItemChange(GrxBaseItem item, int event){
        if(item instanceof GrxWorldStateItem){
            GrxWorldStateItem worldStateItem = (GrxWorldStateItem) item;
            switch(event){
            case GrxPluginManager.SELECTED_ITEM:
                if(currentWorld_!=worldStateItem){
                    currentWorld_ = worldStateItem;
                    currentWorld_.addObserver(this);
                    currentWorld_.addPosObserver(this);
                    graphManager_.setWorldState(currentWorld_);
                    graphManager_.updateGraph();
                }
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                if(currentWorld_==worldStateItem){
                    currentWorld_.deleteObserver(this);
                    currentWorld_.deletePosObserver(this);
                    currentWorld_ = null;
                    graphManager_.setWorldState(null);
                    graphManager_.updateGraph();
                }
                break;
            default:
                break;
            }
        }else if(item instanceof GrxGraphItem){
            GrxGraphItem graphItem = (GrxGraphItem) item;
            switch(event){
            case GrxPluginManager.SELECTED_ITEM:
                _updateGraph(graphItem);
                currentGraph_ = graphItem;
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                _updateGraph(null);
                currentGraph_ = null;
                break;
            default:
                break;
            }
        }else if(item instanceof GrxModelItem){
            GrxModelItem modelItem = (GrxModelItem) item;
            switch(event){
            case GrxPluginManager.SELECTED_ITEM:
                if(!currentModels_.contains(modelItem)){
                    currentModels_.add(modelItem);
                    gpanel_.setModelList(currentModels_);
                }
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                if(currentModels_.contains(modelItem)){
                    currentModels_.remove(modelItem);
                    gpanel_.setModelList(currentModels_);
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
                    simItem_ = simItem;
                    simItem_.addObserver(this);
                }
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                if(simItem_==simItem){
                    simItem_.deleteObserver(this);
                    simItem_ = null;
                }
                break;
            default:
                break;
            }
        }
    }
	
	
	private void _updateGraph(GrxBasePlugin p) {
		graphManager_.clearDataItem();
	
		if (p == null) {
			gpanel_.setEnabledRangeButton(false);
			graphManager_.updateGraph();
			return;
		}
		
		gpanel_.setEnabledRangeButton(true);
		
		for (int i = 0; i < graphManager_.getNumGraph(); i++) {
			String graphName = "Graph" + i;
			String header = graphName + ".";
			String ditems = p.getStr(header + "dataItems");
			if (ditems == null)
				continue;

			TrendGraph tgraph = graphManager_.getTrendGraph(i);
			List<String> addedList = new ArrayList<String>();
			String[] str = ditems.split(",");
			for (int j = 0; j < str.length; j++) {
				if (str[j].equals(""))
					continue;
				String object= p.getStr(header + str[j] + ".object");
				String node  = p.getStr(header + str[j] + ".node");
				String attr  = p.getStr(header + str[j] + ".attr");
				int index    = p.getInt(header + str[j] + ".index", -1);
				String sColor= p.getStr(header + str[j] + ".color");
				RGB color=null;
				if(sColor!=null)
					color=StringConverter.asRGB(sColor);
				String legend= p.getStr(header + str[j] + ".legend");
				String type = "";
				if(attr.equals("angle"))
					type = "Joint";
				else if (attr.equals("force")) 
					type = "ForceSensor";
				else if (attr.equals("torque"))	
					type = "ForceSensor";
				else if (attr.equals("acceleration"))
					type = "AccelerationSensor";
				else if (attr.equals("angularVelocity"))
					type = "Gyro";

				DataItem ditem = new  DataItem(object, node, attr, index, type);
				if (!addedList.contains(ditem.toString())){
					addedList.add(ditem.toString());
					tgraph.addDataItem(new DataItemInfo(ditem, color, legend));
				}
			}

			double[] vRange = p.getDblAry(header + "vRange", null);
			if (vRange != null) {
				tgraph.setRange(vRange[0], vRange[1]);
			}
		}
		
		double[] timeRange = p.getDblAry("timeRange", new double[]{1.0, 0.8});
		graphManager_.setRangeAndPos(timeRange[0], timeRange[1]);
		
		graphManager_.updateGraph();
	}

	private boolean isSimulation = false;
    public void update(GrxBasePlugin plugin, Object... arg) {
        if(simItem_==plugin){
            if((String)arg[0]=="StartSimulation"){ //$NON-NLS-1$
				double step = currentWorld_.getDbl("logTimeStep", 0.001);
				graphManager_.setStepTime((long)(1000000*step));
				graphManager_.initGetData();
				isSimulation = true;
            }else if((String)arg[0]=="StopSimulation"){
            	isSimulation = false;
            	graphManager_.setTotalTime(currentWorld_.getTime(currentWorld_.getLogSize()-1));
            }
        } else if(currentWorld_==plugin) {
            if((String)arg[0]=="ClearLog"){ //$NON-NLS-1$
            	graphManager_.setWorldState(currentWorld_);
            	graphManager_.updateGraph();
            }else if((String)arg[0]=="LoadLog"){
            	graphManager_.setWorldState(currentWorld_);
            	graphManager_.updateGraph();
            }
        }
    }
    
    public void updatePosition(GrxBasePlugin plugin, Integer arg_pos){
        if(currentWorld_!=plugin) return;

        int pos = arg_pos.intValue();
        WorldStateEx state = currentWorld_.getValue(pos);
        if (state == null){
        	updateGraph(new Time(0));
        }else{
	        Time time_ = new Time();
			time_.set(state.time);
	        updateGraph(time_);
        }
    }

	private void updateGraph(Time time_){
		if(isSimulation)
			graphManager_.setTotalTime(((GrxTimeSeriesItem)currentWorld_).getTime(currentWorld_.getLogSize()-1));
		graphManager_.worldTimeChanged(time_);
	}

    public void shutdown() {
        manager_.removeItemChangeListener(this, GrxGraphItem.class);
        manager_.removeItemChangeListener(this, GrxModelItem.class);
        manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
        manager_.removeItemChangeListener(this, GrxSimulationItem.class);
        if(currentWorld_!=null)
        {
            currentWorld_.deleteObserver(this);
            currentWorld_.deletePosObserver(this);
        }

        if(simItem_!=null)
            simItem_.deleteObserver(this);
    }
    
}

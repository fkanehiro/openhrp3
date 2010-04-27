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
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.view.graph.DataItem;
import com.generalrobotix.ui.view.graph.DataItemInfo;
import com.generalrobotix.ui.view.graph.GUIStatus;
import com.generalrobotix.ui.view.graph.GraphPanel;
import com.generalrobotix.ui.view.graph.Time;
import com.generalrobotix.ui.view.graph.TrendGraph;
import com.generalrobotix.ui.view.graph.TrendGraphManager;

@SuppressWarnings("serial")
public class GrxGraphView extends GrxBaseView {
    public static final String TITLE = "Graph";

	private GrxWorldStateItem currentWorld_ = null;
	private GrxGraphItem currentGraph_ = null;
	private List<GrxModelItem> currentModels_ = new ArrayList<GrxModelItem>();
	private TrendGraphManager graphManager_;
	private GraphPanel gpanel_;
	
	public GrxGraphView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent)  {
		super(name, manager, vp, parent);
		isScrollable_ = false;
		graphManager_ = new TrendGraphManager(3);
		
		gpanel_ = new GraphPanel(manager_, graphManager_, composite_ );
		TrendGraph t = graphManager_.getTrendGraph(0);
		t.getDataItemInfoList();
		graphManager_.setMode(GUIStatus.EXEC_MODE);
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        
        currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
        if(currentWorld_!=null){
            setWorldState(currentWorld_);
        	currentWorld_.addObserver(this);
        }
        manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
        currentGraph_ = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
        if(currentGraph_!=null){
        	_updateGraph(currentGraph_);
        	if(currentWorld_!=null){
        		WorldStateEx state = currentWorld_.getValue();
        		if (state != null)	
        			updateGraph(state);
        	}
        }
        manager_.registerItemChangeListener(this, GrxGraphItem.class);
        currentModels_ = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        gpanel_.setModelList(currentModels_);
        manager_.registerItemChangeListener(this, GrxModelItem.class);
	}

	private void setWorldState(GrxWorldStateItem worldStateItem){
		if(worldStateItem != null){
			graphManager_.setWorldState(worldStateItem);
			try {
				double step = worldStateItem.getDbl("logTimeStep", 0.001);
				graphManager_.trendGraphModel_.setStepTime((long)(1000000*step));
			} catch (Exception e) {
				GrxDebugUtil.printErr("Couldn't parse log step time.", e);
			}
		}else
			graphManager_.setWorldState(null);
	}
	
	public void registerItemChange(GrxBaseItem item, int event){
		if(item instanceof GrxWorldStateItem){
			GrxWorldStateItem worldStateItem = (GrxWorldStateItem) item;
	    	switch(event){
	    	case GrxPluginManager.SELECTED_ITEM:
	    		if(currentWorld_!=worldStateItem){
                    setWorldState(worldStateItem);
	    			currentWorld_ = worldStateItem;
	    			currentWorld_.addObserver(this);
	    		}
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentWorld_==worldStateItem){
	    			currentWorld_.deleteObserver(this);
                    setWorldState(null);
	    			currentWorld_ = null;
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
		}
	}
	
	
	private void _updateGraph(GrxBasePlugin p) {
		for (int i = 0; i < graphManager_.getNumGraph(); i++) {
			TrendGraph t = graphManager_.getTrendGraph(i);
			DataItemInfo[] info = t.getDataItemInfoList();
			for (int j = 0; j < info.length; j++)
				t.removeDataItem(info[j].dataItem);
		}
		
		if (p == null) {
			gpanel_.setEnabledRangeButton(false);
			composite_.redraw(composite_.getLocation().x,composite_.getLocation().y,composite_.getSize().x,composite_.getSize().y,true);	
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

			double[] timeRange = p.getDblAry(header + "timeRange", null);
			if (timeRange != null) {
				tgraph.setTimeRange(timeRange[0]);
				tgraph.setMarkerPos(timeRange[1]);
			}

			double[] vRange = p.getDblAry(header + "vRange", null);
			if (vRange != null) {
				tgraph.setRange(vRange[0], vRange[1]);
			}
		}
		composite_.redraw(composite_.getLocation().x,composite_.getLocation().y,composite_.getSize().x,composite_.getSize().y,true);	
	}

	public void update(GrxBasePlugin plugin, Object... arg) {
		if(currentWorld_!=plugin) return;
		if((String)arg[0]=="PositionChange"){
			int pos = ((Integer)arg[1]).intValue();
			WorldStateEx state = currentWorld_.getValue(pos);
			if (state == null)	return;
			updateGraph(state);
		}
	}
	
	private void updateGraph(WorldStateEx state){
		Time time_ = new Time();
		time_.set(state.time);
		graphManager_.simulationTimeChanged(time_);
		graphManager_.worldTimeChanged(time_);
		
		double ti = time_.getDouble() + graphManager_.getTimeRange();
        if (ti > graphManager_.getTotalTime())
            graphManager_.setTotalTime((long)(ti+5)*1000000);
	}
	
	 public void shutdown() {
		 manager_.removeItemChangeListener(this, GrxGraphItem.class);
		 manager_.removeItemChangeListener(this, GrxModelItem.class);
		 manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
		 if(currentWorld_!=null)
			 currentWorld_.deleteObserver(this);   
	}
}

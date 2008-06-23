/*
 *  GrxGraphView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.view.graph.AttributeInfo;
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
	private TrendGraphManager graphManager_;
	private GraphPanel gpanel_;
	private double prevTime_ = 0.0;

	public GrxGraphView(String name, GrxPluginManager manager) {
		super(name, manager);
		isScrollable_ = false;
		JPanel contentPane = getContentPane();

		contentPane.setLayout(new BorderLayout());
		JPanel combpanel = new JPanel();
		combpanel.setLayout(new FlowLayout());
		combpanel.setBackground(Color.white);

		graphManager_ = new TrendGraphManager(3);
		gpanel_ = new GraphPanel(manager_, graphManager_);
		TrendGraph t = graphManager_.getTrendGraph(0);
		t.getDataItemInfoList();
		contentPane.add(gpanel_, BorderLayout.CENTER);
		graphManager_.setMode(GUIStatus.EXEC_MODE);
	}
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(currentWorld_)) {
			currentWorld_ = (GrxWorldStateItem)manager_.getSelectedItem(GrxWorldStateItem.class, null);
			if (currentWorld_ == null)
				return;

			graphManager_.setLogManager(currentWorld_.logger_);
			try {
				double step = currentWorld_.getDbl("logTimeStep", null);
				graphManager_.trendGraphModel_.setStepTime((long)(1000000*step));
			} catch (Exception e) {
				GrxDebugUtil.printErr("Couldn't parse log step time.", e);
				currentWorld_ = null;
			}
		}
	
		if (!itemList.contains(currentGraph_)) {
			currentGraph_ = (GrxGraphItem)manager_.getSelectedItem(GrxGraphItem.class, null);
			_updateGraph(currentGraph_);
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
			gpanel_.setEnabled(false);
			gpanel_.repaint();	
			return;
		}
		
		gpanel_.setEnabled(true);
		
		for (int i = 0; i < graphManager_.getNumGraph(); i++) {
			String graphName = "Graph" + i;
			String header = graphName + ".";
			String ditems = p.getStr(header + "dataItems");
			if (ditems == null)
				continue;

			TrendGraph tgraph = graphManager_.getTrendGraph(i);

			//String kind = getProperty(header + "dataKind", "");
			List<String> addedList = new ArrayList<String>();
			String[] str = ditems.split(",");
			for (int j = 0; j < str.length; j++) {
				if (str[j].equals(""))
					continue;
				String object= p.getStr(header + str[j] + ".object");
				String node  = p.getStr(header + str[j] + ".node");
				String attr  = p.getStr(header + str[j] + ".attr");
				int index    = p.getInt(header + str[j] + ".index", 0);
				DataItem ditem = new DataItem(object, node, attr, index);
				if (!addedList.contains(ditem.toString()))
					addedList.add(ditem.toString());

				boolean isContains = false;
				DataItemInfo[] info = graphManager_.getTrendGraph(i).getDataItemInfoList();
				for (int k = 0; k < info.length; k++) {
					DataItem d = info[k].dataItem;
					if (d.toString().equals(ditem.toString())) {
						isContains = true;
						break;
					}
				}

				if (isContains)
					continue;

				String type = "Joint";
				int length = 0;
				if (attr.equals("force")) {
					type = "ForceSensor";
					length = 3;
				} else if (attr.equals("torque")) {
					type = "ForceSensor";
					length = 3;
				} else if (attr.equals("acceleration")) {
					type = "AccelerationSensor";
					length = 3;
				} else if (attr.equals("angularVelocity")) {
					type = "Gyro";
					length = 3;
				}

				tgraph.addDataItem(new AttributeInfo(type, object, node, attr, length));
			}

			DataItemInfo[] info = tgraph.getDataItemInfoList();
			for (int k = info.length - 1; k >= 0; k--) {
				DataItem d = info[k].dataItem;
				if (!addedList.contains(d.toString()))
					tgraph.removeDataItem(d);
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
		gpanel_.repaint();	
	}

	private Time time_ = new Time();
	public void control(List<GrxBaseItem> itemList) {
	    if (currentWorld_ == null || !getContentPane().isShowing()) 
	    	return;
	    
		WorldStateEx state = currentWorld_.getValue();
		if (state == null)
			return;
		
		if (state.time - prevTime_ != 0) {
			prevTime_ = state.time;
			time_.set(state.time);
			graphManager_.simulationTimeChanged(time_);
			graphManager_.worldTimeChanged(time_);
			
			double ti = time_.getDouble() + graphManager_.getTimeRange();
	        if (ti > graphManager_.getTotalTime())
	            graphManager_.setTotalTime((long)(ti+5)*1000000);
		}
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		currentWorld_ = null;
		return true;
	}
}

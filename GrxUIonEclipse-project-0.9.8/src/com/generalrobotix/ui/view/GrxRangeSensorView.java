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
 *  GrxRangeSensorView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.go.aist.hrp.simulator.SensorState;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSensorItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.CharacterStateEx;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;

/**
 * @brief Range Sensor output viewer
 */
@SuppressWarnings("serial")
public class GrxRangeSensorView extends GrxBaseView implements PaintListener{
    public static final String TITLE = "Range Sensor View";

    private Composite canvas_;
    private GrxWorldStateItem currentWorld_ = null;
    private GrxModelItem currentModel_ = null;
    private GrxSensorItem currentSensor_ = null;
    private SensorState  currentSensorState_ = null;
    
    private Combo comboModelName_;
    private Combo comboSensorName_;
    private List<GrxModelItem> modelList_;
    private List<GrxSensorItem> sensorList_;
    
    private double drawnTime_ = -1;

    /**
     * @brief constructor
     * @param name
     * @param manager
     * @param vp
     * @param parent
     */
    public GrxRangeSensorView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);
        
        GridLayout layout = new GridLayout(1, false);
        composite_.setLayout(layout);
        
        Composite northPane = new Composite(composite_,SWT.NONE);
        northPane.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout gridLayout = new GridLayout(2,true);
        northPane.setLayout(gridLayout);
        
        comboModelName_ = new Combo(northPane,SWT.READ_ONLY);
        comboModelName_.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelList_ = new ArrayList<GrxModelItem>();
        
        comboModelName_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e) {
                GrxModelItem item = modelList_.get(comboModelName_.getSelectionIndex());
                if (item == null || item == currentModel_)
                    return;

                currentModel_ = item;
                List<GrxSensorItem> sensors = currentModel_.getSensors("Range");
                comboSensorName_.removeAll();
                sensorList_.clear();
                if (sensors != null){
                	for (int i=0; i<sensors.size(); i++){
                		comboSensorName_.add(sensors.get(i).getName());
                		sensorList_.add(sensors.get(i));
                	}
                }
            }
        });

        comboSensorName_ = new Combo(northPane,SWT.READ_ONLY);
        comboSensorName_.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sensorList_ = new ArrayList<GrxSensorItem>();
        
        comboSensorName_.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e){
        		currentSensor_ = sensorList_.get(comboSensorName_.getSelectionIndex());
        	}
        });
        canvas_ = new Canvas(composite_,SWT.NONE);
        canvas_.addPaintListener(this);
        canvas_.setLayoutData(new GridData(GridData.FILL_BOTH));


   }

    /**
     * @brief draw range sensor output if sensor is choosed
     * @param e paint event
     */
    public void paintControl(PaintEvent e) {
    	if (currentSensor_ != null && currentSensorState_ != null){
    		double step = currentSensor_.getDbl("scanStep", 0.1);
    		double maxD = currentSensor_.getDbl("maxDistance", 10.0);
    		if (currentSensor_.id() >= 0 && currentSensor_.id() < currentSensorState_.range.length){
    	    	Rectangle bounds = canvas_.getBounds();
    	    	if(bounds.height < bounds.width) {
    	    		bounds.x = (bounds.width - bounds.height) / 2;
    	    		bounds.width = bounds.height;
    	    	} else {
    	    		bounds.y = (bounds.height - bounds.width) / 2;
    	    		bounds.height = bounds.width;
    	    	}
    	    	// I don't know why "-5" is required. An arc shifts slightly right without it.
    	    	e.gc.drawArc(bounds.x-5,bounds.y-5,bounds.width-1, bounds.height-1,0,360);
    			double [] distances = currentSensorState_.range[currentSensor_.id()];
     			int half = distances.length/2;
    			int x,y,oldx,oldy;
    			int centerx = bounds.x + bounds.width / 2;
    			int centery = bounds.y + bounds.height / 2;
    			oldx = centerx;
    			oldy = centery;
    			double scale = bounds.width/(maxD*2);
    			for (int i=-half; i<=half; i++){
    				x = centerx - (int)(scale*distances[i+half]*Math.sin(step*i));
    				y = centery - (int)(scale*distances[i+half]*Math.cos(step*i));
    				e.gc.drawLine(oldx, oldy, x, y);
    				oldx = x;
    				oldy = y;
    			}
    			e.gc.drawLine(oldx, oldy, centerx, centery);
    		}
    	}
    }

    /**
     * @brief control() method of GrxPlugin. For more details, see documents of parent class
     * @param itemList list of items
     */
    public void control(List<GrxBaseItem> itemList) {
        if (currentWorld_ != null && currentWorld_.getLogSize() > 0) {
            WorldStateEx state = currentWorld_.getValue();
            if (state != null  && currentModel_ != null) {
                CharacterStateEx charStat = state.get(currentModel_.getName());
                if (charStat != null && state.time != drawnTime_) {
                    currentSensorState_ = charStat.sensorState;
                	canvas_.redraw();
                	drawnTime_ = state.time;
                }
            }
        }
    }
    
    /**
     * @brief This method is called when contents of selected items are changed
     * @param itemList list of selected items
     */
    public void itemSelectionChanged(List<GrxBaseItem> itemList) {
        Iterator<GrxBaseItem> it = itemList.iterator();
        comboModelName_.removeAll();
        modelList_.clear();
        currentModel_ = null;
        comboSensorName_.removeAll();
        sensorList_.clear();
        currentSensor_ = null;
        
        while (it.hasNext()) {
        	GrxBaseItem item = it.next();
        	if (item instanceof GrxWorldStateItem){
        		currentWorld_ = (GrxWorldStateItem) item;
        	}else if (item instanceof GrxModelItem && ((GrxModelItem)item).isRobot()) {
        		comboModelName_.add(item.getName());
        		modelList_.add((GrxModelItem)item);
        	}
        }
        canvas_.redraw();
    }
}

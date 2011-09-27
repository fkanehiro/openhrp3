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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
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
        
        comboModelName_.addSelectionListener(new SelectionListener(){
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
                	if(sensors.size()>0){
                		comboSensorName_.select(0);
                		comboSensorName_.notifyListeners(SWT.Selection, null);
                	}
                }else
                	updateCanvas(null);
                	
            }
			public void widgetDefaultSelected(SelectionEvent e) {
			}
        });

        comboSensorName_ = new Combo(northPane,SWT.READ_ONLY);
        comboSensorName_.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sensorList_ = new ArrayList<GrxSensorItem>();
        
        comboSensorName_.addSelectionListener(new SelectionListener(){
        	public void widgetSelected(SelectionEvent e){
        		currentSensor_ = sensorList_.get(comboSensorName_.getSelectionIndex());
        		if(currentWorld_!=null)
        			updateCanvas(currentWorld_.getValue());
        	}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
        });
        canvas_ = new Canvas(composite_,SWT.NONE);
        canvas_.addPaintListener(this);
        canvas_.setLayoutData(new GridData(GridData.FILL_BOTH));

        setUp();
        manager_.registerItemChangeListener(this, GrxModelItem.class);
        manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
   }

    public void setUp(){
    	Iterator<GrxModelItem> it = modelList_.iterator();
    	while(it.hasNext())
    		comboModelName_.remove(it.next().getName());
    	modelList_ = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        it = modelList_.iterator();
	    while(it.hasNext())
	    	comboModelName_.add(it.next().getName());
	    if(comboModelName_.getItemCount()>0){
		   	comboModelName_.select(0);
		   	comboModelName_.notifyListeners(SWT.Selection, null);
	    }
	    	
	   	if(currentWorld_ != null){
	   		currentWorld_.deleteObserver(this);
	   		currentWorld_.deletePosObserver(this);
	   	}
        currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
        if(currentWorld_!=null){
        	currentWorld_.addObserver(this);
        	currentWorld_.addPosObserver(this);
        	updateCanvas(currentWorld_.getValue());
        }else
        	updateCanvas(null);

    }
    
    /**
     * @brief draw range sensor output if sensor is choosed
     * @param e paint event
     */
    public void paintControl(PaintEvent e) {
    	if (currentSensor_ != null && currentSensorState_ != null){
    		double step = currentSensor_.getDbl("scanStep", 0.1);
    		double maxD = currentSensor_.getDbl("maxDistance", 10.0);
    		if (currentSensor_.id_ >= 0 && currentSensor_.id_ < currentSensorState_.range.length){
    	    	Rectangle bounds = canvas_.getBounds();
    	    	if(bounds.height < bounds.width) {
    	    		bounds.x = (bounds.width - bounds.height) / 2;
    	    		bounds.width = bounds.height;
    	    		bounds.y = 0;
    	    	} else {
    	    		bounds.y = (bounds.height - bounds.width) / 2;
    	    		bounds.height = bounds.width;
    	    		bounds.x = 0;
    	    	}
    	    	e.gc.drawArc(bounds.x, bounds.y, bounds.width-1, bounds.height-1,0,360);
    			double [] distances = currentSensorState_.range[currentSensor_.id_];
    			int centerx = bounds.x + bounds.width / 2;
    			int centery = bounds.y + bounds.height / 2;
    			double scale = bounds.width/(maxD*2);
    			e.gc.setBackground(Activator.getDefault().getColor("darkGray"));
    			int stepAngle = (int)(step*57.29579+0.5);
    			int half = (int)(distances.length/2);
    			double startAngle = 1.570796-step*half-step/2;
    			for(int i=0; i<distances.length; i++){
    				double distance = distances[i];
    				if(distance==0)
    					distance = maxD;
    				int iDistance = (int)(scale*distance);
    				e.gc.fillArc(centerx-iDistance, centery-iDistance, iDistance*2, iDistance*2, (int)(startAngle*57.29579), stepAngle);
    				startAngle += step;
    			}
    		}
    	}
    }
    
    public void updatePosition(GrxBasePlugin plugin, Integer arg_pos){
        if(currentWorld_!=plugin) return;

        int pos = arg_pos.intValue();
        WorldStateEx state = currentWorld_.getValue(pos);
        updateCanvas(state);
    }
    
    private void updateCanvas( WorldStateEx state ){
    	if (state != null  && currentModel_ != null) {
    		CharacterStateEx charStat = state.get(currentModel_.getName());
    		if (charStat != null ) {
    			currentSensorState_ = charStat.sensorState;
    			canvas_.redraw();
    		}
    	}else{
    		currentSensorState_ = null;
    		canvas_.redraw();
    	}
    }
    
    public void registerItemChange(GrxBaseItem item, int event){
    	if(item instanceof GrxModelItem){
    		GrxModelItem modelItem = (GrxModelItem) item;
	    	switch(event){
	    	case GrxPluginManager.ADD_ITEM:
	    		if(!modelList_.contains(modelItem)){
	    			modelList_.add(modelItem);
	    			comboModelName_.add(modelItem.getName());
	    			if(currentModel_==null){
	    				comboModelName_.select(0);
	    				comboModelName_.notifyListeners(SWT.Selection, null);
	    			}
	    		}
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    		if(modelList_.contains(modelItem)){
	    			modelList_.remove(modelItem);
	    			comboModelName_.remove(modelItem.getName());
	    			if(currentModel_==modelItem){
	    				if(modelList_.size()>0){
	    					comboModelName_.select(0);
	    					comboModelName_.notifyListeners(SWT.Selection, null);
	    				}else
	    					currentModel_=null;
	    			}
	    		}
	    		break;
	    	default:
	    		break;
	    	}
    	}else if(item instanceof GrxWorldStateItem){
    		GrxWorldStateItem worldStateItem = (GrxWorldStateItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(currentWorld_!=worldStateItem){
    				currentWorld_ = worldStateItem;
    				currentWorld_.addObserver(this);
    				currentWorld_.addPosObserver(this);
    				updateCanvas(currentWorld_.getValue());
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentWorld_==worldStateItem){
	    			currentWorld_.deleteObserver(this);
	    			currentWorld_.deletePosObserver(this);
	    			currentWorld_ = null;
	    			updateCanvas(null);
	    		}
	    		break;
	    	default:
	    		break;
    		}
    	}
    }
    
    public void shutdown() {
        manager_.removeItemChangeListener(this, GrxModelItem.class);
        manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
        if(currentWorld_!=null) {
			 currentWorld_.deleteObserver(this);
			 currentWorld_.deletePosObserver(this);
        }
	}
}

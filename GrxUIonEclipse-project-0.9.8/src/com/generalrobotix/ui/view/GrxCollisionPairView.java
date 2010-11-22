/*
 * Copyright (c) 2010, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * GrxCollisionPairView.java
 *
 *
 * @author  Ksk.Saeki
 * @version 1.0 (2010/02/15)
 */
package com.generalrobotix.ui.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.view.simulation.CollisionPairPanel;

@SuppressWarnings("serial")
public class GrxCollisionPairView extends GrxBaseView {

    private CollisionPairPanel collisionPane_;
    private List<GrxModelItem> currentModels_ = new ArrayList<GrxModelItem>();
    private List<GrxCollisionPairItem> currentCollisionPairs_ = new ArrayList<GrxCollisionPairItem>();;

    public GrxCollisionPairView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);
        collisionPane_ = new CollisionPairPanel(composite_, SWT.NONE, manager_);

        setUp();
        collisionPane_.setEnabled(true);
        
        manager_.registerItemChangeListener(this, GrxModelItem.class);
        manager_.registerItemChangeListener(this, GrxCollisionPairItem.class);
    }
    
    public void setUp(){
    	Iterator<GrxCollisionPairItem> it = currentCollisionPairs_.iterator();
        while(it.hasNext()) {
            it.next().deleteObserver(this);
        }
        currentModels_ = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        currentCollisionPairs_ = manager_.<GrxCollisionPairItem>getSelectedItemList(GrxCollisionPairItem.class);
        collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
        it = currentCollisionPairs_.iterator();
        while(it.hasNext()) {
            it.next().addObserver(this);
        }
    }
    
    public void registerItemChange(GrxBaseItem item, int event){
        if(item instanceof GrxModelItem){
            GrxModelItem mitem = (GrxModelItem)item;
            switch(event){
            case GrxPluginManager.SELECTED_ITEM:
                if(!currentModels_.contains(mitem)){
                    currentModels_.add(mitem);
                    collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
                }
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                if(currentModels_.contains(mitem)){
                    currentModels_.remove(mitem);
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
                    collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
                    citem.addObserver(this);
                }
                break;
            case GrxPluginManager.REMOVE_ITEM:
            case GrxPluginManager.NOTSELECTED_ITEM:
                if(currentCollisionPairs_.contains(citem)){
                    currentCollisionPairs_.remove(citem);
                    collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
                    citem.deleteObserver(this);
                }
                break;
            default:
                break;  
            }
        }
    }

    public void update(GrxBasePlugin plugin, Object... arg) {
        if(currentCollisionPairs_.contains(plugin)){
            if((String)arg[0]=="PropertyChange"){
                collisionPane_.updateCollisionPairs(currentCollisionPairs_, currentModels_);
            }
        }
    }
    
    public void shutdown() {
        Iterator<GrxCollisionPairItem> it = currentCollisionPairs_.iterator();
        while(it.hasNext()) {
            it.next().deleteObserver(this);
        }

        manager_.removeItemChangeListener(this, GrxModelItem.class);
        manager_.removeItemChangeListener(this, GrxCollisionPairItem.class);
    }

    public void updateTableFont(){
        collisionPane_.updateTableFont();
    }
}

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
 *  GrxProcessManagerView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxProcessManager;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.GrxProcessManager.AProcess;

@SuppressWarnings("serial")
public class GrxProcessManagerView extends GrxBaseView
    implements DisposeListener {
    public static final String TITLE          = "Process Manager";
    private StyledText		    outputArea_      = null;
    public GrxProcessManager   processManager_ = null;

    public GrxProcessManagerView(String name, GrxPluginManager manager,
            GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);
        
        initializeComposite();     
        composite_.addDisposeListener(this);
        processManager_ = (GrxProcessManager) manager.getItem("processManager");
        if(processManager_!=null){
        	outputArea_.setText("");
        	String[] processed = processManager_.getOutputBuffer().toString().split("\n", -1);
        	for (int i = 0; i < processed.length; i++) {
        		if(processed[i].startsWith("[")){
        			String id=processed[i].substring(1, processed[i].indexOf(":"));
        			AProcess p =processManager_.get(id);
        			if(p.showOutput())
        				outputArea_.append(processed[i] + "\n");
        		}
        	}
        	processManager_.clearBuffer();
        }
        isScrollable_ = false;

        if(processManager_!=null)
        	processManager_.addObserver(this);
        
    }

    public String[] getMenuPath() {
        return new String[] { "Tools" };
    }
    
    public void initializeComposite() {
        outputArea_ = new StyledText(composite_, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        outputArea_.setEditable(false);

        // 右クリックメニューを自動再生成させる
        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager menu) {
                for (Action action : getOutputMenu()) {
                    menu.add(action);
                }
            }
        });
        outputArea_.setMenu(manager.createContextMenu(outputArea_));
    }
    
    private Vector<Action> getOutputMenu() {
        Vector<Action> vector = new Vector<Action>(size());
        for (int i = 0; i < processManager_.size(); i++) {
            final AProcess p = processManager_.get(i);
            Action action = new Action(p.pi_.id, Action.AS_CHECK_BOX) {
                public void run() {
                    p.setShowOutput(isChecked());
                    String[] processed = processManager_.getOutputBuffer().toString().split("\n"); //$NON-NLS-1$
                    outputArea_.setText(""); //$NON-NLS-1$
                    for (int i = 0; i < processed.length; i++) {
                        for (int j = 0; j < processManager_.size(); j++) {
                            AProcess p2 = processManager_.get(j);
                            if (p2.showOutput() && processed[i].startsWith("[" + p2.pi_.id)) { //$NON-NLS-1$
                                outputArea_.append(processed[i] + "\n"); //$NON-NLS-1$
                            }

                        }
                    }
                }
            };
            action.setChecked(p.showOutput());
            vector.add(action);
        }

        Action actionClearAll = new Action(MessageBundle.get("GrxProcessManager.menu.clearAll1"), Action.AS_PUSH_BUTTON) { //$NON-NLS-1$
            public void run() {
                outputArea_.setText(""); //$NON-NLS-1$
                processManager_.setOutputBuffer(new StringBuffer());
            }
        };
        vector.add(actionClearAll);
        return vector;

    }
    
    public void update(GrxBasePlugin plugin, Object... arg) {
    	if(processManager_!=plugin) return;
    	if((String)arg[0]=="append"){
    		outputArea_.append((String)arg[1]);
    	}else if((String)arg[0]=="setTopIndex"){
    		outputArea_.setTopIndex(outputArea_.getLineCount());
    	}
    }
    
    public void shutdown() {
    	if(processManager_!=null)
        	processManager_.deleteObserver(this);
    }

    public void widgetDisposed(DisposeEvent e){
        processManager_.stopType();
    }
}

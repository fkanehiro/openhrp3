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

import java.util.List;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.util.GrxProcessManager;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

@SuppressWarnings("serial")
public class GrxProcessManagerView extends GrxBaseView
    implements DisposeListener {
    public static final String TITLE          = "Process Manager";

    public GrxProcessManager   processManager = null;

    public GrxProcessManagerView(String name, GrxPluginManager manager,
            GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);
        processManager = GrxProcessManager.getInstance();
        processManager.initializeComposite(composite_);
        composite_.addDisposeListener(this);
        isScrollable_ = false;
        processManager.createThread();
    }

    public String[] getMenuPath() {
        return new String[] { "Tools" };
    }

    // プラグイン初期化時にはビューが無いためプロセス初期化に失敗するため、
    // ビューが初期化されたさいに再度プロセスをリストアする
    public boolean setup(List<GrxBaseItem> itemList) {
        //GrxDebugUtil.println("[ProcessManagerView] restore process");
        //manager_.restoreProcess();
        return true;
    }

    public void shutdown() {
        
    }

    public void widgetDisposed(DisposeEvent e){
        processManager.stopType();
    }
}

/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */

package com.generalrobotix.ui.view;

import java.util.List;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxTextItem;
import com.generalrobotix.ui.util.GrxServerManager;
//import com.generalrobotix.ui.util.GrxXmlUtil;
//import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
//import com.generalrobotix.ui.util.GrxProcessManager;
//import com.generalrobotix.ui.util.GrxXmlUtil;
//import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;


@SuppressWarnings("serial")
public class GrxServerManagerView extends GrxBaseView {
    public static final String TITLE = "Server Manager";

    public GrxServerManagerView(String name, GrxPluginManager manager_,
            GrxBaseViewPart vp, Composite parent) {
        super(name, manager_, vp, parent);
        new GrxServerManager(composite_,SWT.NULL);
        setScrollMinSize();
	}
}

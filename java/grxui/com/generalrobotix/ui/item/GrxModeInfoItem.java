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
 *  GrxModeInfoItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxXmlUtil;

@SuppressWarnings("unchecked")
public class GrxModeInfoItem extends GrxBaseItem {
	public static final String TITLE = "Mode Info";

	public ArrayList<Class<? extends GrxBaseItem>> activeItemClassList_ = new ArrayList<Class<? extends GrxBaseItem>>();
	public ArrayList<Class<? extends GrxBaseView>> activeViewClassList_ = new ArrayList<Class<? extends GrxBaseView>>();

	public GrxModeInfoItem(String name, GrxPluginManager manager) {
		super(name, manager);
		setExclusive(true);
	}

	public void restoreProperties() {
		super.restoreProperties();
		
		NodeList list = element_.getElementsByTagName("item");
		for (int i = 0; i < list.getLength(); i++) {
			Element el = (Element) list.item(i);
			manager_.pluginLoader_.addURL(GrxXmlUtil.expandEnvVal(el.getAttribute("lib")));
			Class cls = manager_.registerPlugin(el.getAttribute("class"));
			if (cls != null && 
					GrxBaseItem.class.isAssignableFrom(cls) &&
					!activeItemClassList_.contains(cls))
				activeItemClassList_.add((Class<? extends GrxBaseItem>)cls);
		}
				
		list = element_.getElementsByTagName("view");
		for (int i = 0; i < list.getLength(); i++) {
			Element el = (Element) list.item(i);
			manager_.pluginLoader_.addURL(GrxXmlUtil.expandEnvVal(el.getAttribute("lib")));
			Class cls = manager_.registerPlugin(el.getAttribute("class"));
			if (cls != null && 
					GrxBaseView.class.isAssignableFrom(cls) &&
					!activeViewClassList_.contains(cls)) {
				activeViewClassList_.add((Class<? extends GrxBaseView>)cls);
				String name = el.getAttribute("name");
				name = name.length()>0 ? name:null;
				manager_.createView((Class<? extends GrxBaseView>)cls, name);
			}
		}
		manager_.setVisibleItem();
	}

	public boolean create() {
		return true;
	}
}

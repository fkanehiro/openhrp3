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
 *  GrxBasePlugin.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.lang.reflect.Field;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.item.GrxProjectItem;
import com.generalrobotix.ui.util.GrxConfigBundle;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxBasePlugin extends GrxConfigBundle {
	private String name_;
	protected GrxPluginManager manager_;
	private String url_;
	private boolean selected_ = false;
	private boolean isExclusive_= false;
	private ImageIcon icon_;
	private JMenu menu_;
	private String[] menuPath_;
	
	private Document doc_;
	protected Element element_;

    protected final static String ITEM_TAG = "item";
    protected final static String VIEW_TAG = "view";
    protected final static String PROPERTY_TAG = "property";
    protected final static String INDENT4 = "    ";
    
	private GrxBasePlugin() {
	};

	protected GrxBasePlugin(String name, GrxPluginManager manager) {
		manager_ = manager;
		name_ = name;

		JMenuItem item = new JMenuItem("restore Properties");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				restoreProperties();
			}
		});
		setMenuItem(item);
	}

	public void restoreProperties() {
		if (element_ == null) {
			return;
		}
		clear();
		NodeList props = element_.getElementsByTagName(PROPERTY_TAG);
		for (int j = 0; j < props.getLength(); j++) {
			Element propEl = (Element) props.item(j);
			String key = propEl.getAttribute("name");
			String val = propEl.getAttribute("value");
			setProperty(key, val);
		}
	}
	
	public Element storeProperties() {
		if (doc_ == null)
			return null;
		
		if (element_ != null) {
			Node n = element_.getParentNode();
			if (n != null)
				n.removeChild(element_);
		}
		
		String tag = (this instanceof GrxBaseItem) ? ITEM_TAG:VIEW_TAG;
		element_ = doc_.createElement(tag); 
		
		element_.setAttribute("class",getClass().getName());
		element_.setAttribute("name", getName());
		element_.setAttribute("select", String.valueOf(isSelected()));
		if (getURL(false) != null)
			element_.setAttribute("url", getURL(false));
		element_.appendChild(doc_.createTextNode("\n"));
		
		Enumeration keys = propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			String val = getProperty(key);
			if (key == null || val == null)
				continue;
				
			Element propEl = doc_.createElement(GrxProjectItem.PROPERTY_TAG);
			propEl.setAttribute("name",  key);
			propEl.setAttribute("value", val);
			
			element_.appendChild(doc_.createTextNode(INDENT4+INDENT4+INDENT4));
			element_.appendChild(propEl);
			element_.appendChild(doc_.createTextNode("\n"));
		}
		element_.appendChild(doc_.createTextNode(INDENT4+INDENT4));	
		
		return element_;
	}

	public void setName(String name) {
		name_ = name;
	}

	public final String getName() {
		return name_;
	}

	public final String toString() {
		return name_;
	}
	
	public void setDocument(Document doc) {
		doc_ = doc;
	}

	public void setElement(Element element) {
		element_ = element;
		doc_ = element.getOwnerDocument();
	}

	public Element getElement() {
		return element_;
	}

	protected void setSelected(boolean b) {
		selected_ = b;
	}

	public boolean isSelected() {
		return selected_;
	}

	public void setExclusive(boolean b) {
		isExclusive_ = b;
	}

	public boolean isExclusive() {
		return isExclusive_;
	}

	protected void setIcon(ImageIcon icon) {
		icon_ = icon;
	}

	public ImageIcon getIcon() {
		return icon_;
	}

	protected void setMenuItem(JComponent c) {
		if (menu_ == null)
			menu_ = new JMenu(name_);
		menu_.add(c);
	}

	public JMenu getMenu() {
		return menu_;
	};

	protected void setMenuPath(String[] path) {
		menuPath_ = path;
	}

	public String[] getMenuPath() {
		return menuPath_;
	};

	public boolean equals(Object obj) {

		return obj != null && this.hashCode() == obj.hashCode()
				&& this.toString().equals(obj.toString());
	}

	public String getURL(boolean expand) {
		if (expand)
			return GrxXmlUtil.expandEnvVal(url_);
		else
			return url_;
	}
	
	public void setURL(String url) {
		url_ =url;
	}
	
    public static Object getField(Class<? extends GrxBasePlugin> cls, String field, Object defaultValue) {
		try {
			Field f = cls.getField(field);
			return (Object)f.get(new Object());
		} catch (Exception e) {
			GrxDebugUtil.println(cls.getName() + ": " + field + " not defined");
		}
		return defaultValue;
    }

	public void propertyChanged() {
		
	}

	public void shutdown() {

	}
}

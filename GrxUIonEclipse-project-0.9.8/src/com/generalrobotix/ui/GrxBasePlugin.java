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
import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.item.GrxProjectItem;
import com.generalrobotix.ui.util.GrxConfigBundle;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
/**
 * @brief
 */
public class GrxBasePlugin extends GrxConfigBundle {
	private String name_;
	protected GrxPluginManager manager_;
	private String url_;
	private boolean selected_ = false;
	private boolean isExclusive_= false;

	private ImageRegistry ireg_;
	private String iconName_;

	private Vector<Action> menu_ = new Vector<Action>();
	private String[] menuPath_;
	
	private Document doc_;
	protected Element element_;

    protected final static String ITEM_TAG = "item";
    protected final static String VIEW_TAG = "view";
    protected final static String PROPERTY_TAG = "property";
    protected final static String INDENT4 = "    ";

    /**
     * @brief constructor
     * @param name name of this plugin
     * @param manager plugin manager
     */
	protected GrxBasePlugin(String name, GrxPluginManager manager) {
		manager_ = manager;
		name_ = name;
		ireg_ = new ImageRegistry();
		// menu item : restore Properties
		Action a = new Action(){
			public String getText(){
				return "restore Properties";
			}
			public void run(){
				restoreProperties();
			}
		};
		setMenuItem(a);
	}

	/**
	 * @brief restore properties. Called by menu item "restore Properties"
	 */
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
	
	/**
	 * @brief store properties
	 * @return
	 */
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

	/**
	 * @brief set name 
	 * @param name name
	 */
	public void setName(String name) {
		name_ = name;
	}

	/**
	 * @brief get name
	 * @return name
	 */
	public final String getName() {
		return name_;
	}

	/**
	 * @brief convert to String. Currently, name is returned.
	 * @return name
	 */
	public final String toString() {
		return name_;
	}
	
	/**
	 * @brief set document
	 * @param doc document
	 */
	public void setDocument(Document doc) {
		doc_ = doc;
	}

	/**
	 * @brief set element
	 * @param element element
	 */
	public void setElement(Element element) {
		element_ = element;
		doc_ = element.getOwnerDocument();
	}

	/**
	 * @brief get element
	 * @return element
	 */
	public Element getElement() {
		return element_;
	}

	/**
	 * @brief set selected flag
	 * @param b flag
	 */
	protected void setSelected(boolean b) {
		selected_ = b;
	}

	/**
	 * @brief check whether this is selected or not
	 * @return true if selected, false otherwise
	 */
	public boolean isSelected() {
		return selected_;
	}

	/**
	 * @brief set exclusive flag
	 * @param b flag
	 */
	public void setExclusive(boolean b) {
		isExclusive_ = b;
	}

	/**
	 * @brief check whether this is exclusive or not
	 * @return true if exclusive, false otherwise
	 */
	public boolean isExclusive() {
		return isExclusive_;
	}

	/**
	 * @brief set icon
	 * @param iconName name of icon
	 */
	protected void setIcon(String iconName) {
		iconName_ = iconName;
		if( ireg_.get( iconName_ ) == null )
			ireg_.put( iconName_, ImageDescriptor.createFromURL( getClass().getResource( "/resources/images/"+iconName_ ) ) );
		//icon_ = icon;
	}

	/**
	 * @brief get icon
	 * @return icon
	 */
	public Image getIcon() {
		return ireg_.get( iconName_ );
	}

	/**
	 * @brief add a menu item
	 * @param a new menu item
	 */
	public void setMenuItem( Action a ) {
		menu_.add(a);
	}
	
	/**
	 * @brief get menu
	 * @return menu
	 */
	public Vector<Action> getMenu() {
		return menu_;
	}
	
	/**
	 * @brief set menu path
	 * @param path menu path
	 */
	protected void setMenuPath(String[] path) {
		menuPath_ = path;
	}

	/**
	 * @brief get menu path
	 * @return menu path
	 */
	public String[] getMenuPath() {
		return menuPath_;
	};

	/**
	 * @brief check whether obj equals to this 
	 * @param obj an object to be checked
	 */
	public boolean equals(Object obj) {

		return obj != null && this.hashCode() == obj.hashCode()
				&& this.toString().equals(obj.toString());
	}

	/**
	 * get url
	 * @param expand if expand is true, expanded url is returned.
	 * @return url
	 */
	public String getURL(boolean expand) {
		if (expand)
			return GrxXmlUtil.expandEnvVal(url_);
		else
			return url_;
	}
	
	/**
	 * @brief set url
	 * @param url
	 */
	public void setURL(String url) {
		url_ =url;
	}
	
	/**
	 * @brief get field
	 * @param cls
	 * @param field
	 * @param defaultValue
	 * @return
	 */
    public static Object getField(Class<? extends GrxBasePlugin> cls, String field, Object defaultValue) {
		try {
			Field f = cls.getField(field);
			return (Object)f.get(new Object());
		} catch (Exception e) {
			GrxDebugUtil.println(cls.getName() + ": " + field + " not defined");
		}
		return defaultValue;
    }

    /**
     * 
     */
	public void propertyChanged() {
		
	}

	/**
	 * 
	 */
	public void shutdown() {

	}
}

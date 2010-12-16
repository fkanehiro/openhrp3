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

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.PreferenceConstants;
import com.generalrobotix.ui.item.GrxProjectItem;
import com.generalrobotix.ui.util.GrxConfigBundle;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial") //$NON-NLS-1$
/**
 * @brief
 */
public class GrxBasePlugin extends GrxConfigBundle {
	private String name_;
	private String oldName_;
	protected GrxPluginManager manager_;
	private String url_;
	private boolean selected_ = true;
	private boolean isExclusive_= false;

	private ImageRegistry ireg_;
	private String iconName_;

	private Vector<Action> menu_ = new Vector<Action>();
	private Vector<MenuManager> subMenu_ = new Vector<MenuManager>();
	private String[] menuPath_;

	protected Document doc_;
	protected Element element_;

    protected final static String ITEM_TAG = "item"; //$NON-NLS-1$
    protected final static String VIEW_TAG = "view"; //$NON-NLS-1$
    protected final static String PROPERTY_TAG = "property"; //$NON-NLS-1$
    protected final static String INDENT4 = "    "; //$NON-NLS-1$
    
    protected static final String[] booleanComboItem_ = new String[] {"true", "false" };
    
    private ArrayList<GrxObserver> observers_ = new ArrayList<GrxObserver>();

    /**
     * @brief constructor
     * @param name name of this plugin
     * @param manager plugin manager
     */
	protected GrxBasePlugin(String name, GrxPluginManager manager) {
		manager_ = manager;
		setName(name);
		ireg_ = new ImageRegistry();
		// menu item : restore Properties
		Action a = new Action(){
			public String getText(){
				return MessageBundle.get("GrxBasePlugin.menu.restoreProperties"); //$NON-NLS-1$
			}
			public void run(){
				restoreProperties();
			}
		};
		setMenuItem(a);

		// menu item : rename
		Action item = new Action(){
				public String getText(){
					return MessageBundle.get("GrxBasePlugin.menu.rename"); //$NON-NLS-1$
				}
				public void run(){
					InputDialog dialog = new InputDialog( null, getText(),
							MessageBundle.get("GrxBasePlugin.dialog.message.input"), getName(),null); //$NON-NLS-1$
					if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
						rename( dialog.getValue() );
				}
			};
		setMenuItem(item);
	}

	/**
	 * @brief restore properties. Called by menu item "restore Properties"
	 */
	public void restoreProperties() {
		if (element_ == null) {
			return;
		}
		clear();
		if(url_!=null)	
			setProperty("url", url_);
		if(name_!=null)
			setProperty("name", name_);
		NodeList props = element_.getElementsByTagName(PROPERTY_TAG);
		for (int j = 0; j < props.getLength(); j++) {
			Element propEl = (Element) props.item(j);
			String key = propEl.getAttribute("name"); //$NON-NLS-1$
			String val = propEl.getAttribute("value"); //$NON-NLS-1$
			if (!propertyChanged(key, val)){
				setProperty(key, val);
			}
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

		element_.setAttribute("class",getClass().getName()); //$NON-NLS-1$
		element_.setAttribute("name", getName()); //$NON-NLS-1$
		element_.setAttribute("select", String.valueOf(isSelected())); //$NON-NLS-1$
		if (url_ != null)
			element_.setAttribute("url", GrxXmlUtil.replaceEnvVal(new File(url_))); //$NON-NLS-1$
		element_.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$

		Enumeration<?> keys = propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			String val = getProperty(key);
			if (key == null || val == null || key.equals("name") || key.equals("url"))
				continue;
			
			if(key.equals("setupDirectory"))
				val = GrxXmlUtil.replaceEnvVal(new File(GrxXmlUtil.expandEnvVal(val)));
			if(key.equals("setupCommand")){
				String s = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.BIN_SFX);
				val = val.replace(s, "$(BIN_SFX)");
			}
			Element propEl = doc_.createElement(GrxProjectItem.PROPERTY_TAG);
			propEl.setAttribute("name",  key); //$NON-NLS-1$
			propEl.setAttribute("value", val); //$NON-NLS-1$

			element_.appendChild(doc_.createTextNode(INDENT4+INDENT4+INDENT4));
			element_.appendChild(propEl);
			element_.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
		}
		element_.appendChild(doc_.createTextNode(INDENT4+INDENT4));

		return element_;
	}

	/**
	 * @brief set name
	 * @param name name
	 */
	public void setName(String name) {
		oldName_ = name_;
		name_ = name;
		setProperty("name", name); //$NON-NLS-1$
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

	public String getOldName(){
		return oldName_;
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
	public void setSelected(boolean b) {
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
			ireg_.put( iconName_, ImageDescriptor.createFromURL( getClass().getResource( "/resources/images/"+iconName_ ) ) ); //$NON-NLS-1$
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
	 * @brief add a submenu
	 * @param a new submenu
	 */
	public void setSubMenu( MenuManager m ) {
		subMenu_.add(m);
	}

	/**
	 * @brief get menu
	 * @return menu
	 */
	public Vector<Action> getMenu() {
		return menu_;
	}

	/**
	 * @brief get subMenu
	 * @return subMenu
	 */
	public Vector<MenuManager> getSubMenu() {
		return subMenu_;
	}
	
	/**
	 * @brief set menu path
	 * @param path menu path
	 */
	protected void setMenuPath(String[] path) {
		menuPath_ = path;
	}

    /**
     * @brief Transfer SWT UI thread
     * @param r Runnable instance
     * @return boolean Syncable current display
     */
    protected boolean syncExec(Runnable r) {
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed()) {
            display.syncExec(r);
            return true;
        } else
        return false;
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

		return this == obj;
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
	 * @brief set URL property
	 * @param url URL to be set
	 */
	public void setURL(String url) {
		url = url.replace('\\','/');
		setProperty("url", url); //$NON-NLS-1$
		url_ =url;
	}

	/**
	 * @brief rename this item
	 * @param newName new name
	 */
	public void rename(String newName) {
		manager_.renamePlugin(this, newName);
	};

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
			//GrxDebugUtil.println(cls.getName() + ": " + field + " not defined");
		}
		return defaultValue;
    }

	/**
	 *
	 */
	public void shutdown() {

	}
	
    /**
    *
    */
	public void unregisterCORBA() {

	}

    /**
    *
    */
    public boolean registerCORBA() {
        return true;
    }
	
    /**
     * @brief Override clone method
     * @return GrxBasePlugin
     */
	public GrxBasePlugin clone(){
		GrxBasePlugin ret = (GrxBasePlugin) super.clone();
		
		
    	ret.setName(name_);
    	ret.setURL(url_);
    	
/*    	
 * 		Deep copy suspension list

	private ImageRegistry ireg_;
	private String iconName_;

	private Vector<Action> menu_ = new Vector<Action>();
	private String[] menuPath_;

	private Document doc_;
	protected Element element_;
*/
    	
    	return ret;
	}

    /**
     * @brief check validity of new value of property and update if valid
     * @param property name of property
     * @param value value of property
     * @return true if checked(even if value is not used), false otherwise
     */
	public boolean propertyChanged(String property, String value) {
		if (property.equals("name")){ //$NON-NLS-1$
			rename(value);
			return true;
		}
		return false;
	}
	

	/**
	 * @brief set property value associated with a keyword
	 * @param key keyword
	 * @param value value of property associated with a keyword
	 */
	public Object setProperty(String key, String value){
		//System.out.println("GrxBasePlugin.setProperty("+key+","+value+")");
		Object o = super.setProperty(key, value);
		notifyObservers("PropertyChange", key, value); //$NON-NLS-1$
		return o;
	}
	
	/**
	 * @brief set/unset focus on this plugin
	 * @param b true to focus, false to unfocus
	 */
	public void setFocused(boolean b){
	}
	
	public void addObserver(GrxObserver v){
		observers_.add(v);
	}
	
	public void deleteObserver(GrxObserver v){
		observers_.remove(v);
	}

	public ArrayList<GrxObserver> getObserver(){
		return observers_;
	}
	
	public void notifyObservers(Object... arg) { 
        ListIterator<GrxObserver> it = observers_.listIterator();
        while (it.hasNext()) {
            GrxObserver observer = it.next();
            observer.update(this, arg);
        }
    }

    /**
     * @brief  Return editing type of the key item
     * @return ValueEditType
     */
    public ValueEditType GetValueEditType(String key) {
        return new ValueEditText();
    }
        
    public class ValueEditType{
        private ValueEditType(){}
    }
    public class ValueEditText extends ValueEditType{
        public ValueEditText(){}
    }
    public class ValueEditCombo extends ValueEditType{
        private String[] items_;
        public ValueEditCombo(String[] items){
            items_ = items;
        }
        public String[] GetItems(){ return items_; }
    }
}

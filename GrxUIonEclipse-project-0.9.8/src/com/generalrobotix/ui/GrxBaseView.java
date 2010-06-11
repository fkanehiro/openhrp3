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
 *  GrxBaseView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.awt.Dimension;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.generalrobotix.ui.item.GrxProjectItem;

/**
 * @brief
 *
 */
@SuppressWarnings("serial")
public class GrxBaseView extends GrxBasePlugin implements GrxItemChangeListener, GrxObserver , GrxPositionObserver{

	private static Dimension defaultButtonSize_ = new Dimension(27, 27);
	public boolean isScrollable_ = true;
	
	public double min, max, now;
	    
	private GrxBaseViewPart vp_;
	private Composite parent_;
    private ScrolledComposite scrollComposite_;
    protected Composite composite_;

    /**
     * @brief constructor
     * @param name name
     * @param manager_ plugin manager
     * @param vp view part
     * @param parent parent composite
     */
	public GrxBaseView( String name, GrxPluginManager manager_, GrxBaseViewPart vp, Composite parent ){
		super( name, manager_ );
		vp_ = vp;
		parent_ = parent;
        
        scrollComposite_ = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        composite_ = new Composite(scrollComposite_, SWT.NONE);
        composite_.setLayout(parent.getLayout());
        scrollComposite_.setExpandHorizontal(true);
        scrollComposite_.setExpandVertical(true);
        scrollComposite_.setContent(composite_);
	}
    
	/**
	 * @brief set name
	 * @name name
	 */
	public void setName(String name) {
		super.setName(name);
	}
	
	/**
	 * @brief get view part
	 * @return view part
	 */
	public GrxBaseViewPart getViewPart(){
		return vp_;
	}

	/**
	 * @brief get parent composite
	 * @return parent composite
	 */
	public Composite getParent(){
		return parent_;
	}

	/**
	 * @brief get composite. SWTのパーツはここで取得できるコンポジット上に設置する。
	 * @return composite
	 */
    public Composite getComposite(){
        return composite_;
    }
    
    /**
     * @brief set scroll minimum size
     */
    public void setScrollMinSize(int width, int height){
        scrollComposite_.setMinSize(composite_.computeSize(width, height));
    }

    /**
     * @brief get default button size
     * @return default button size
     */
	public static Dimension getDefaultButtonSize() {
		return defaultButtonSize_;
	}

	/**
	 * @brief cleanup
	 * @param itemList
	 * @return true if cleanup finished, false otherwise
	 */public boolean cleanup(List<GrxBaseItem> itemList){return true;}

	/**
	 * @brief this method is called by PluginManager when list of items is changed
	 */
	public void itemListChanged() {
	}

	public void focusedItemChanged(GrxBaseItem item) {
	}

	/**
	 * @brief This method is called when a property of plugin is changed
	 */
	public void propertyChanged() {
	}
    
	public void registerItemChange(GrxBaseItem item, int event){
	}

	public void update(GrxBasePlugin plugin, Object... arg) {
	}
    
    public void updatePosition(GrxBasePlugin plugin, Integer pos){
    }
    
    public void updateTableFont(){
    }
    
    public void updateEditerFont(){
    }
    
    public void restoreProperties() {
    	clear();
    	Properties properties = manager_.getViewProperties(getName());
    	if(properties!=null){
    		for (Enumeration<?> e = properties.propertyNames();  e.hasMoreElements(); ){
    			String key = (String) e.nextElement();
    			String val = properties.getProperty(key);
    			if (!propertyChanged(key, val)){
    				setProperty(key, val);
    			}
    		}
    	}
    }
    
	public Element storeProperties() {
		if (doc_ == null)
			return null;

		String tag = VIEW_TAG;
		element_ = doc_.createElement(tag);

		element_.setAttribute("class",getClass().getName()); //$NON-NLS-1$
		element_.setAttribute("name", getName()); //$NON-NLS-1$
		element_.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$

		Enumeration<?> keys = propertyNames();
		boolean hasProperty=false;
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			String val = getProperty(key);
			if (key == null || val == null || key.equals("name"))
				continue;

			hasProperty = true;
			Element propEl = doc_.createElement(GrxProjectItem.PROPERTY_TAG);
			propEl.setAttribute("name",  key); //$NON-NLS-1$
			propEl.setAttribute("value", val); //$NON-NLS-1$

			element_.appendChild(doc_.createTextNode(INDENT4+INDENT4+INDENT4));
			element_.appendChild(propEl);
			element_.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
		}
		element_.appendChild(doc_.createTextNode(INDENT4+INDENT4));
		
		if(hasProperty)
			return element_;
		else
			return null;
	}
}

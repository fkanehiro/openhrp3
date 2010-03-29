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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;

/**
 * @brief
 *
 */
@SuppressWarnings("serial")
public class GrxBaseView extends GrxBasePlugin {

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
}

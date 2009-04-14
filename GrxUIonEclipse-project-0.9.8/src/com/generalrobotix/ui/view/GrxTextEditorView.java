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
 *  GrxTextEditorView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxPythonScriptItem;

@SuppressWarnings("serial")
public class GrxTextEditorView extends GrxBaseView {
	private Text area_;
	private Action save_,saveAs_;

	private GrxPythonScriptItem currentItem_ = null;
	
	public GrxTextEditorView(String name, GrxPluginManager manager_,
			GrxBaseViewPart vp, Composite parent) {
		super(name, manager_, vp, parent);
		area_ = new Text( composite_, SWT.MULTI|SWT.V_SCROLL|SWT.BORDER );

		IToolBarManager toolbar = vp.getViewSite().getActionBars().getToolBarManager();

		save_ = new Action() {
            public void run() {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.save();
					//save_.setEnabled(false);
				}
            }
        };
        save_.setToolTipText( "Save" );
        save_.setImageDescriptor( Activator.getDefault().getDescriptor("save_edit.png") );
        toolbar.add( save_ );

		saveAs_ = new Action() {
            public void run() {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.saveAs();
				}
            }
        };
        saveAs_.setToolTipText( "Save As" );
        saveAs_.setImageDescriptor( Activator.getDefault().getDescriptor("saveas_edit.png") );
        toolbar.add( saveAs_ );
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        
		currentItem_ = manager_.<GrxPythonScriptItem>getSelectedItem(GrxPythonScriptItem.class, null);
		setTextItem(currentItem_);
        manager_.registerItemChangeListener(this, GrxPythonScriptItem.class);
	}

	private void setTextItem(GrxPythonScriptItem item){
		if(item != null){
			area_.setText( (String)item.getValue() );
			area_.setEnabled(true);
			save_.setEnabled(true);
			saveAs_.setEnabled(true);
		}else{
			area_.setText("");
			area_.setEnabled(false);
			save_.setEnabled(false);
			saveAs_.setEnabled(false);
		}
	}
	
	private void _syncCurrentItem() {
		if (currentItem_ != null) {
			currentItem_.setValue(area_.getText());
			currentItem_.setCaretPosition(area_.getCaretPosition());
		}
	}

	public void registerItemChange(GrxBaseItem item, int event){
		GrxPythonScriptItem textItem = (GrxPythonScriptItem)item;
		switch(event){
    	case GrxPluginManager.SELECTED_ITEM:
    		setTextItem(textItem);
			currentItem_ = textItem;
    		break;
    	case GrxPluginManager.REMOVE_ITEM:
    	case GrxPluginManager.NOTSELECTED_ITEM:
    		_syncCurrentItem();
    		if(currentItem_ == textItem){
	    		setTextItem(null);
				currentItem_ = null;
    		}
    		break;
    	default:
    			break;
		}
	}
	
	public void shutdown(){
		manager_.removeItemChangeListener(this, GrxPythonScriptItem.class);
	}
}

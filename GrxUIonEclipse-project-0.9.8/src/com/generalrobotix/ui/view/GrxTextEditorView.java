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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.custom.StyledText;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxPythonScriptItem;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxTextEditorView extends GrxBaseView {
    private Label status_;
	private StyledText area_;
	private Action save_,saveAs_;
	private int caretPos_ = -1;

	private GrxPythonScriptItem currentItem_ = null;
	
	public GrxTextEditorView(String name, GrxPluginManager manager_,
			GrxBaseViewPart vp, Composite parent) {
		super(name, manager_, vp, parent);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		composite_.setLayout(layout);
        
		area_ = new StyledText( composite_, SWT.MULTI|SWT.V_SCROLL|SWT.BORDER );

		area_.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				if(currentItem_!=null)
					currentItem_.setValue(area_.getText());
			}	
		});

		area_.addFocusListener(new FocusListener() { 
			public void focusGained(FocusEvent e) {
				if (currentItem_ == null)
					return;
				if (currentItem_.isModifiedExternally()) {
					boolean state = MessageDialog.openQuestion(getParent().getShell(), MessageBundle.get("GrxTextEditorView.dialog.title.reload"), MessageBundle.get("GrxTextEditorView.dialog.message.reload")); //$NON-NLS-1$ //$NON-NLS-2$
					if (state){
						currentItem_.reload();
						area_.setText(currentItem_.getValue());
					}
				}
			}

			public void focusLost(FocusEvent e) {
			}
		});

        area_.addListener(SWT.Paint, new Listener() {
            public void handleEvent(Event event) {
                setPositionLabel();
            }
          });

        area_.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event event) {
                setPositionLabel();
            }
          });
        
        area_.addListener(SWT.KeyDown, new Listener() {
            public void handleEvent(Event event) {
                setPositionLabel();
            }
          });

        status_ = new Label(composite_, SWT.BORDER);
        
        GridData textData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        area_.setLayoutData(textData);

        GridData statusData = new GridData(GridData.FILL_HORIZONTAL);
        statusData.verticalAlignment = SWT.END;
        status_.setLayoutData(statusData);        


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
	    save_.setToolTipText( MessageBundle.get("GrxTextEditorView.text.save") ); //$NON-NLS-1$
	    save_.setImageDescriptor( Activator.getDefault().getDescriptor("save_edit.png") ); //$NON-NLS-1$
	    toolbar.add( save_ );
	
		saveAs_ = new Action() {
	        public void run() {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.saveAs();
				}
	        }
	    };
	    saveAs_.setToolTipText( MessageBundle.get("GrxTextEditorView.text.saveAs") ); //$NON-NLS-1$
	    saveAs_.setImageDescriptor( Activator.getDefault().getDescriptor("saveas_edit.png") ); //$NON-NLS-1$
	    toolbar.add( saveAs_ );
	    setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
	    
	    setUp();
	    manager_.registerItemChangeListener(this, GrxPythonScriptItem.class);
	    updateEditerFont();
	}
	
	public void setUp(){
		currentItem_ = manager_.<GrxPythonScriptItem>getSelectedItem(GrxPythonScriptItem.class, null);
		setTextItem(currentItem_);
	}

	private void setTextItem(GrxPythonScriptItem item){
		if(item != null){
			area_.setText( (String)item.getValue() );
			area_.setEnabled(true);
			save_.setEnabled(true);
			saveAs_.setEnabled(true);
			setPositionLabel();
		}else{
			area_.setText(""); //$NON-NLS-1$
			area_.setEnabled(false);
			save_.setEnabled(false);
			saveAs_.setEnabled(false);
			status_.setText("");
		}
	}

    private void setPositionLabel(){
        if(caretPos_ == area_.getCaretOffset())
            return;
        caretPos_ = area_.getCaretOffset();
        int y = area_.getLineAtOffset(caretPos_);
        String s = area_.getText();
        int x = 0;
        int ti = caretPos_;
        while(0 < ti){
            --ti;
            char cha = s.charAt(ti); 
            if(cha == '\r' || cha == '\n'){
                break;
            }
            ++x;
        }
        status_.setText(String.format("%6d:%d", y + 1, x + 1));
    }

	public void registerItemChange(GrxBaseItem item, int event){
		GrxPythonScriptItem textItem = (GrxPythonScriptItem)item;
		switch(event){
    	case GrxPluginManager.SELECTED_ITEM:
    		currentItem_ = textItem;
    		setTextItem(textItem);
    		break;
    	case GrxPluginManager.REMOVE_ITEM:
    	case GrxPluginManager.NOTSELECTED_ITEM:
    		if(currentItem_ == textItem){
				currentItem_ = null;
	    		setTextItem(null);
    		}
    		break;
    	default:
    			break;
		}
	}
	
	public void shutdown(){
		manager_.removeItemChangeListener(this, GrxPythonScriptItem.class);
	}
	
    public void updateEditerFont(){
        area_.setFont(Activator.getDefault().getFont("preference_editer"));
    }
}

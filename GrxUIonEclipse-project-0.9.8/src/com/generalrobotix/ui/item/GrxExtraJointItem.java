/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

package com.generalrobotix.ui.item;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;

import jp.go.aist.hrp.simulator.ExtraJointInfo;
import jp.go.aist.hrp.simulator.ExtraJointType;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxBasePlugin.ValueEditCombo;
import com.generalrobotix.ui.GrxBasePlugin.ValueEditType;
import com.generalrobotix.ui.util.MessageBundle;


/**
 * @brief item which have a transformation
 */
@SuppressWarnings("serial")

public class GrxExtraJointItem extends GrxBaseItem {
	public static final String TITLE = "ExtraJoint";
	boolean isModel = true;
	private GrxModelItem model_;
	private GrxModelItem model1_, model2_;
	static final String[] extrajointTypeComboItem_ = new String[] { "piston" };
	
	public  GrxExtraJointItem(String name, GrxPluginManager manager) {
		super(name, manager);
		setIcon("extraJoint.png");
		isModel = false;
		model_ = null;
	}
	
	public GrxExtraJointItem(String name, GrxPluginManager manager, GrxModelItem model, ExtraJointInfo extraJointInfo) {
		super(name, manager);
		model_ = model;
		
		if(extraJointInfo != null){
			setProperty("link1Name", extraJointInfo.link[0]);
			setProperty("link2Name", extraJointInfo.link[1]);
			setDblAry("link1LocalPos", extraJointInfo.point[0], 4);
			setDblAry("link2LocalPos", extraJointInfo.point[1], 4);
			if(extraJointInfo.jointType == ExtraJointType.EJ_PISTON){
				setProperty("jointType", "piston");
			}else if(extraJointInfo.jointType == ExtraJointType.EJ_BALL){
				setProperty("jointType", "ball");
			}
			setDblAry("jointAxis", extraJointInfo.axis, 4);
		}else{
			setProperty("link1Name", "");
			setProperty("link2Name", "");
			setProperty("link1LocalPos", "0.0 0.0 0.0");
			setProperty("link2LocalPos", "0.0 0.0 0.0");
			setProperty("jointType", "piston");
			setProperty("jointAxis", "0 0 1");
		}
    	
		setIcon("extraJoint.png");
    	initMenu();
	}
	
	public void jointAxis(String axis){
    	double[] newAxis = getDblAry(axis);
    	if (newAxis != null && newAxis.length == 3){
    		setDblAry("jointAxis", newAxis, 4); 
       		if (model_ != null) model_.notifyModified();
    	}  
    }
	
	public void link1LocalPos(String pos){
		double[] newPos = getDblAry(pos);
    	if (newPos != null && newPos.length == 3){
    		setDblAry("link1LocalPos", newPos, 4); 
       		if (model_ != null) model_.notifyModified();
    	}  
	}
	
	public void link2LocalPos(String pos){
		double[] newPos = getDblAry(pos);
    	if (newPos != null && newPos.length == 3){
    		setDblAry("link2LocalPos", newPos, 4); 
       		if (model_ != null) model_.notifyModified();
    	}  
	}
	
	public ValueEditType GetValueEditType(String key) {
		if(key.equals("jointType"))
		{
			return new ValueEditCombo(extrajointTypeComboItem_);
		}else if( key.equals("link1Name") || key.equals("link2Name") ){
			String[] linkComboItem_ = null;
			if(isModel){
				if(model_!=null){
					linkComboItem_ = model_.nameToLink_.keySet().toArray(new String[0]);
				}
			}else{
				if( key.equals("link1Name") ){
					if(model1_!=null){
						linkComboItem_ = model1_.nameToLink_.keySet().toArray(new String[0]);
					}
				}
				if( key.equals("link2Name") ){
					if(model2_!=null){
						linkComboItem_ = model2_.nameToLink_.keySet().toArray(new String[0]);
					}
				}
			}
			if(linkComboItem_!=null)
				return new ValueEditCombo(linkComboItem_);
			else
				return super.GetValueEditType(key);
		}else if( key.equals("object1Name") || key.equals("object2Name") ){
			if(!isModel){
				Map<?, ?> m = manager_.pluginMap_.get((GrxModelItem.class));
			    String[] modelComboItem_ = m.keySet().toArray(new String[0]);
				return new ValueEditCombo(modelComboItem_);
			}
		}
		return super.GetValueEditType(key);
	}
	
	private void initMenu(){
		getMenu().clear();

		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.rename"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxLinkItem.dialog.message.rename"), getName(),null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxBaseItem.menu.delete"); //$NON-NLS-1$
			}
			public void run(){
                String mes = MessageBundle.get("GrxBaseItem.dialog.message.delete"); //$NON-NLS-1$
                mes = NLS.bind(mes, new String[]{getName()});
				if( MessageDialog.openQuestion( null, MessageBundle.get("GrxBaseItem.dialog.title.delete"), //$NON-NLS-1$
						mes) )
					delete();
			}
		};
		setMenuItem(item);
	}
	
	public boolean propertyChanged(String property, String value) {
		if (property.equals("name")){ //$NON-NLS-1$
			rename(value);
		}else if(property.equals("link1Name")){
			setProperty("link1Name", value);
			if (model_ != null) model_.notifyModified();
		}else if(property.equals("link2Name")){
			setProperty("link2Name", value);
			if (model_ != null) model_.notifyModified();
		}else if(property.equals("link1LocalPos")){
			link1LocalPos(value);
		}else if(property.equals("link2LocalPos")){
			link2LocalPos(value);
		}else if(property.equals("jointType")){
			setProperty("jointType", value);
			if (model_ != null) model_.notifyModified();
		}else if(property.equals("jointAxis")){
			jointAxis(value);
		}else if(property.equals("object1Name")){
			setProperty("object1Name", value);
			model1_ = (GrxModelItem)(manager_.getItem( GrxModelItem.class, value ));
		}else if(property.equals("object2Name")){
			setProperty("object2Name", value);
			model2_ = (GrxModelItem)(manager_.getItem( GrxModelItem.class, value ));
		}else
			return false;
		return true;
	}
	
	public void delete(){
		if(isModel && model_!=null)
			model_.removeExtraJoint(this);
		super.delete();
	}
	
	public boolean create() {
		setProperty("object1Name", "");
		setProperty("object2Name", "");
		setProperty("link1Name", "");
		setProperty("link2Name", "");
		setProperty("link1LocalPos", "0.0 0.0 0.0");
		setProperty("link2LocalPos", "0.0 0.0 0.0");
		setProperty("jointType", "piston");
		setProperty("jointAxis", "0 0 1");
		return true;
	}
}

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
 *  GrxBaseItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

@SuppressWarnings("serial")

/**
 *
 */
public class GrxBaseItem extends GrxBasePlugin {
	private   Object value_ = null;
	private   File defaultFileDir_;
	protected File file_;
	private   String ext_;
	protected String clipValue_ = "";

	/**
	 * @brief constructor
	 * @param name name
	 * @param manager manager
	 */
	protected GrxBaseItem(String name, GrxPluginManager manager) {
		super(name, manager);
		// delete
		Action item = new Action(){
				public String getText(){
					return "delete";
				}
				public void run(){
					if( MessageDialog.openQuestion( null, "delete item",
							"Are you sure to delete " + GrxBaseItem.this.getName() + " ?") )
						delete();
				}
			};
		setMenuItem(item);
	}

	/**
	 * @brief
	 * @return
	 */
	public boolean create() {
		return true;
	};

	/**
	 *
	 * @param file
	 * @return
	 */
	public boolean load(File file) {
		file_ = file;
		return true;
	};


	/**
	 * @brief delete this item
	 */
	public void delete() {
		//System.out.println("GrxBaseItem.delete("+getName()+") is called");
		manager_.removeItem(this);
	};

	/**
	 * @brief get file extension
	 * @return file extension
	 */
	public String getFileExtention() {
		if (ext_ == null)
			ext_ = (String)GrxBasePlugin.getField(this.getClass(), "FILE_EXTENSION", "");
		return ext_;
	}

	/**
	 * @brief get default directory
	 * @return default directory
	 */
	public File getDefaultDir() {
        if (defaultFileDir_ == null)
            defaultFileDir_ = new File(manager_.getHomePath() + GrxBasePlugin.getField(this.getClass(), "DEFAULT_DIR", ""));
		return defaultFileDir_;
	}

	/**
	 * get value
	 * @return value
	 */
	public Object getValue() {
		return value_;
	}

	/**
	 * set file extension
	 * @param ext file extension
	 */
	protected void setFileExtension(String ext) {
		ext_ = ext;
	}

	/**
	 * set default directory
	 * @param dir new default directory. If dir is invalid, home is set.
	 */
	protected void setDefaultDirectory(String dir) {
        String home = manager_.getHomePath();
        if (home == null)
            home = "";
		defaultFileDir_ = new File(dir);
		if (!defaultFileDir_.isDirectory())
			defaultFileDir_ = new File(home);
	}

	/**
	 * set value
	 * @param o new value
	 */
	public void setValue(Object o) {
		value_ = o;
	}

	/**
	 * choose a file to save
	 * @return file to save
	 */
	public File chooseSaveFile() {
		FileDialog openDialog = new FileDialog(null,SWT.SAVE);
		String openFile = openDialog.open();
		if( openFile != null )
			return new File(openFile);
		else
			return null;
	}

    /**
     * @brief paste object
     */
    public void paste(String clipVal) {
        clipValue_ = clipVal;
    };

    /**
     * @brief Override clone method
     * @return GrxBaseItem
     */
    public GrxBaseItem clone() {
    	GrxBaseItem ret = (GrxBaseItem) super.clone();
    	ret.setValue(value_);
/*    	
 * 		Deep copy suspension list
    	ret.defaultFileDir_ = new File(defaultFileDir_.getPath());
    	ret.file_ = new File(file_.getPath());
    	ret.ext_ = new String(ext_);
    	ret.clipValue_ = new String(clipValue_);
*/
    	return ret;
    }
}

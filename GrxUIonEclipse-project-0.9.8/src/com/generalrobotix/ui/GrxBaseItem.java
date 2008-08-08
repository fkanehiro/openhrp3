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

//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import javax.swing.*;
import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

//import com.generalrobotix.ui.util.GrxGuiUtil;

@SuppressWarnings("serial")
public class GrxBaseItem extends GrxBasePlugin {
    //public final String DEFAULT_DIR = "";
    //public final String FILE_EXTENSION = "";
	private Object value_ = null;
	private   File defaultFileDir_;
	protected File file_;
	private   String ext_;
	
	protected GrxBaseItem(String name, GrxPluginManager manager) {
		super(name, manager);
		/*
		JMenuItem item = new JMenuItem("rename");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String ans = JOptionPane.showInputDialog(manager_.getFrame(),
					"Input new name (without extension).", getName());
				if (ans != null)
					rename(ans);
			}});
		setMenuItem(item);
		item = new JMenuItem("delete");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Object[] options = new Object[] { "OK", "CANCEL" };
				int ans = JOptionPane.showOptionDialog(manager_.getFrame(),
					"Are you sure to delete " + GrxBaseItem.this.getName() + " ?",
					"delete item", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON, options,
					options[1]);
				if (ans == 0) 
					delete();
			}
		});
		setMenuItem(item);
		setMenuItem(new JSeparator());
		setMenuPath(new String[] { "Item" });
		*/
		Action item = new Action(){
				public String getText(){
					return "rename";
				}
				public void run(){
					//String ans = null;
					InputDialog dialog = new InputDialog( null, null,
							"Input new name (without extension).", null,null);
					if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
						rename( dialog.getValue() );
				}
			};
		setMenuItem(item);
		item = new Action(){
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

	public boolean create() {
		return true;
	};

	public boolean load(File file) {
		file_ = file;
		return true;
	};
	
	public void rename(String newName) {
		manager_.renamePlugin(this, newName);
	};

	public void delete() {
		manager_.removeItem(this);
	};

	public String getFileExtention() {
		if (ext_ == null)
			ext_ = (String)GrxBasePlugin.getField(this.getClass(), "FILE_EXTENSION", "");
		return ext_;
	}
	
	public File getDefaultDir() {
        if (defaultFileDir_ == null)
            defaultFileDir_ = new File(manager_.getHomePath() + GrxBasePlugin.getField(this.getClass(), "DEFAULT_DIR", ""));
		return defaultFileDir_;
	}

	public Object getValue() {
		return value_;
	}

	protected void setFileExtension(String ext) {
		ext_ = ext;
	}
	
	protected void setDefaultDirectory(String dir) {
        String home = manager_.getHomePath();
        if (home == null) 
            home = "";
		defaultFileDir_ = new File(dir);
		if (!defaultFileDir_.isDirectory())
			defaultFileDir_ = new File(home);
	}
	
	public void setValue(Object o) {
		value_ = o;
	}
	
	public File chooseSaveFile() {
		/*
		JFileChooser fc = manager_.getFileChooser();
		fc.setDialogTitle("Save Item");
		if (file_ == null)
			fc.setCurrentDirectory(getDefaultDir());
		else  {
			fc.setCurrentDirectory(file_.getParentFile());
			fc.setSelectedFile(file_);		
		}
		
		if (ext_ != null)
			fc.setFileFilter(GrxGuiUtil.createFileFilter(ext_));
			
		if (fc.showSaveDialog(manager_.getFrame()) != JFileChooser.APPROVE_OPTION)
			return null;
		
		return fc.getSelectedFile();
		*/
		FileDialog openDialog = new FileDialog(null,SWT.SAVE);
		String openFile = openDialog.open();
		if( openFile != null )
			return new File(openFile);
		else
			return null;
	}
}

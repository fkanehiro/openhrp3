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
 *  GrxTextItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.undo.UndoManager;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxTextItem extends GrxBaseItem {
	public static final String TITLE = "Text Item";
	public static final String FILE_EXTENSION = "txt";

	private boolean isEdited_ = false;
	private long lastModified_ = 0;
	private int caretPosition_ = 0;
	public UndoManager undo_ = new UndoManager();
	
	public GrxTextItem(String name, GrxPluginManager manager) {
		super(name, manager);
		JMenuItem item = new JMenuItem("save");
		setMenuItem(item);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});

		item = new JMenuItem("save As");
		setMenuItem(item);
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAs();
			}
		});
	}
	
	public boolean create() {
		file_ = new File(getDefaultDir()+File.separator+getName()+"."+getFileExtention());
		setURL(file_.getPath());
		
		isEdited_ = false;
		lastModified_ = file_.lastModified();
		undo_.discardAllEdits();
		caretPosition_ = 0;
		setValue("");
		return true;
	}

	public boolean load(File f) {
		try {
			BufferedReader b = new BufferedReader(new FileReader(f));
			StringBuffer buf = new StringBuffer();
			while (b.ready()) {
				buf.append(b.readLine() + "\n");
			}
			setValue(buf.toString());
		} catch (FileNotFoundException e) {
			GrxDebugUtil.println("TextItem: File Not Found. ("+f.getName()+")");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		file_ = f;
		
		isEdited_ = false;
		lastModified_ = file_.lastModified();
		undo_.discardAllEdits();
		caretPosition_ = 0;
		return true;
	}
	
	public void rename(String newName) {
		String p = file_.getParent();
		super.rename(newName);
		file_ = new File(p+File.separator+getName()+"."+getFileExtention());
		setURL(file_.getPath());
	}

	public boolean save() {
		try {
			GrxDebugUtil.println("savefile:"+file_.getPath());
			if (!file_.exists())
				file_.createNewFile();
			FileWriter fw = new FileWriter(file_);
			fw.write(getValue());
			fw.close();
			isEdited_ = false;
			undo_.discardAllEdits();
			lastModified_ = file_.lastModified();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean saveAs() {
		File f = chooseSaveFile();
		if (f == null || 
			f.isDirectory() ||
			(f.exists() && !file_.equals(f) && 
				JOptionPane.showConfirmDialog( manager_.getFrame(),
				"Overwrite the file ?", "Save File",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)) {
			return false;
		}
        String newName = f.getName().split("[.]")[0];
		manager_.renamePlugin(this, newName);
        if (getName().equals(newName)) {
			file_ = f;
			setURL(f.getPath());
			return save();
        }
        return false;
	}
	
	public String getValue() {
		return (String)super.getValue();
	}
	
	public boolean isEdited() {
		return isEdited_;
	}
	
	public void setEdited() {
		isEdited_ = true;
	}

	public boolean isModifiedExternally() {
		if (lastModified_ < file_.lastModified()) {
			lastModified_ = file_.lastModified();
			return true;
		}
		return false;
	}
	
	public void reload() {
		this.load(file_);
	}
	
	public void setCaretPosition(int pos) {
		caretPosition_ = pos;
	}
	
	public int getCaretPositoin() {
		return caretPosition_;
	}
}

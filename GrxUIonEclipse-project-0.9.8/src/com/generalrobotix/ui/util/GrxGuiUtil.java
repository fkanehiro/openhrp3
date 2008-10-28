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
 *  GrxGuiUtil.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;

public class GrxGuiUtil {
	public static void setTitleBorder(JComponent pane, String title) {
		if (title != null) {
			pane.setBorder(new TitledBorder(new LineBorder(Color.BLACK), title,
					TitledBorder.LEFT, TitledBorder.TOP));
		} else {
			pane.setBorder(new LineBorder(Color.BLACK));
		}
	}

	public static void setWholeFont(Font f){
		FontUIResource fontUIResource = new FontUIResource(f);
		UIDefaults defaultTable = UIManager.getLookAndFeelDefaults();
		Set set = defaultTable.keySet();
		Iterator it = set.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof String) {
				String s = (String) o;
				if (s.endsWith("font") || s.endsWith("Font"))
					UIManager.put(s, fontUIResource);
			}
		}
	}

	public static Frame getParentFrame(Component c) {
		Component parent = c.getParent();
		if (parent == null) {
			return null;
		}
		if (parent instanceof Frame) {
			return (Frame) parent;
		}
		return getParentFrame(parent);
	}

	public static void setButtonInsetsRecursive(Insets insets,
			Container container) {
		Component[] components = container.getComponents();
		for (int i = 0; i < components.length; i++) {
			Component c = components[i];
			if (c instanceof JButton || c instanceof JToggleButton) {
				if (!(c instanceof JCheckBox) && !(c instanceof JRadioButton))
					((AbstractButton) c).setMargin(insets);
			} else if (c instanceof Container)
				setButtonInsetsRecursive(insets, (Container) c);
		}
	}

	public static void setButtonSizeRecursive(Dimension size,
			Container container) {
		Component[] components = container.getComponents();
		for (int i = 0; i < components.length; i++) {
			Component c = components[i];
			if (c instanceof JButton || c instanceof JToggleButton) {
				if (!(c instanceof JCheckBox) && !(c instanceof JRadioButton)) {
					((AbstractButton) c).setPreferredSize(size);
					((AbstractButton) c).setMaximumSize(size);
				}
			} else if (c instanceof Container) {
				setButtonSizeRecursive(size, (Container) c);
			}
		}
	}

	public static JFileChooser createFileChooser(String title, File dir, String filter) {
		return createFileChooser(title, dir, new String[] { filter });
	}

	public static JFileChooser createFileChooser(String title, File dir, String[] filter) {
		JFileChooser jFileChooser = new JFileChooser();
		// title
		if (title != null)
			jFileChooser.setDialogTitle(title);
		// directory
		if (dir == null)
			dir = new File(System.getProperty("user.dir"));
		jFileChooser.setCurrentDirectory(dir);
		// filter
		FileFilter[] ff = jFileChooser.getChoosableFileFilters();
		for (int i = 1; i < ff.length; i++)
			jFileChooser.removeChoosableFileFilter(ff[i]);
		for (int i = 0; i < filter.length; i++) {
			if (filter[i] != null && !filter[i].equals(""))
				jFileChooser.addChoosableFileFilter(createFileFilter(filter[i]));
		}
		return jFileChooser;
	}

	public static FileFilter createFileFilter(final String filter) {
		return new FileFilter() {
			public boolean accept(File f) {
				String ext = "";
				String path = f.getPath();
				int idx = path.lastIndexOf('.');
				if (idx > 0) {
					ext = path.substring(idx + 1).toLowerCase();
					if (ext.equals(filter))
						return true;
				}
				return f.isDirectory();
			}

			public String getDescription() {
				return filter + " files (*." + filter + ")";
			}
		};
	}
	
	public static void setEnableRecursive(boolean e,Container container,HashMap except){
		Component[] components = container.getComponents();
		for (int i=0;i<components.length;i++){
			Component c = components[i];
			if (!(c instanceof JCheckBox) && 
				 (c instanceof JButton || c instanceof JToggleButton)){
				if (except == null || except.get(((AbstractButton)c).getText()) == null)
					c.setEnabled(e);
			} else if (c instanceof JTextField) {
				c.setEnabled(e);
			} else if (c instanceof Container) {
				setEnableRecursive(e,(Container)c,except);
			}
		}
	}
}

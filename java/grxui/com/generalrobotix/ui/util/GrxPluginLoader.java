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
 *  GrxPluginLoader.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.util;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("unchecked")
public class GrxPluginLoader extends URLClassLoader {
	public GrxPluginLoader(String pluginDir, ClassLoader parent) {
		super(new URL[0], parent);
		File pluginDir_ = new File(pluginDir);
		String[] files = pluginDir_.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		if (files != null) {
			for (int i=0; i<files.length; i++)
				addURL(pluginDir_ + "/" + files[i]);
		}
		addURL(pluginDir);
		//addURL("");
	}
	
	public GrxPluginLoader(String pluginDir) {
		this(pluginDir, null);
	}
	
	public void addURL(String path) {
		try {
			File f = new File(path);
			if (f.isFile() || f.isDirectory()) {
				super.addURL(f.toURL());
				System.out.println("classpath added: "+f.toURI().toString());
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public Class loadClass(String cname){
		try {
			return super.loadClass(cname, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public GrxBasePlugin createPlugin(Class cls, String name, GrxPluginManager manager) {
		try {
			Constructor c = cls.getConstructor(new Class[] { String.class, GrxPluginManager.class });
			return (GrxBasePlugin) c.newInstance(new Object[] { name, manager});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

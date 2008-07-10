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
 *  GrxProcessManagerView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import javax.swing.*;
import org.w3c.dom.*;

import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxProcessManager;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

@SuppressWarnings("serial")
public class GrxProcessManagerView extends GrxBaseView {
    public static final String TITLE = "Process Manager";

	public GrxProcessManager processManager = new GrxProcessManager();

	public GrxProcessManagerView(String name, GrxPluginManager manager) {
		super(name, manager);
		isScrollable_ = false;
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JPanel(), BorderLayout.NORTH);
		contentPane.add(new JScrollPane(processManager.getOutputArea()), BorderLayout.CENTER);
		processManager.createThread();
	}
	
	public void loadProcessList(Element root) {
		NodeList processList = root.getElementsByTagName("processmanagerconfig");
		if (processList == null || processList.getLength() == 0)
			return;
		processList = ((Element) processList.item(0)).getElementsByTagName("process");
		String defaultDir = "";
		String defaultNsHost = "localhost";
		int defaultNsPort = 2809;
		int defaultWaitCount = 500;

		for (int i = 0; i < processList.getLength(); i++) {
			Node n = processList.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element e = (Element) n;
			String id = GrxXmlUtil.getString(e, "id", "");
			if (id.equals("")) {
				defaultDir = GrxXmlUtil.getString(e, "dir", defaultDir);
				defaultNsHost = GrxXmlUtil.getString(e, "nshost", defaultNsHost);
				defaultNsPort = GrxXmlUtil.getInteger(e, "nsport", defaultNsPort);
				defaultWaitCount = GrxXmlUtil.getInteger(e, "waitcount", defaultWaitCount);
				break;
			}
		}

		for (int i = 0; i < processList.getLength(); i++) {
			Node n = processList.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element e = (Element) n;
			String id = GrxXmlUtil.getString(e, "id", "");
			if (id == null || id.trim().equals(""))
				continue;

			ProcessInfo pi = new ProcessInfo();
			pi.id = id;
			pi.com.add(GrxXmlUtil.getString(e, "com", ""));
			String str = null;
			for (int j = 0; !(str = GrxXmlUtil.getString(e, "env"+j, "")).equals(""); j++)
				pi.env.add(str);
			pi.dir = GrxXmlUtil.getString(e, "dir", defaultDir);
			pi.waitCount = GrxXmlUtil.getInteger(e, "waitcount",defaultWaitCount);
			pi.nsHost = GrxXmlUtil.getString(e, "nshost", defaultNsHost);
			pi.nsPort = GrxXmlUtil.getInteger(e, "nsport", defaultNsPort);
			pi.isCorbaServer = GrxXmlUtil.getBoolean(e, "iscorbaserver", false);
			pi.hasShutdown = GrxXmlUtil.getBoolean(e, "hasshutdown", false);
			pi.doKillall = GrxXmlUtil.getBoolean(e, "dokillall", false);
			pi.autoStart = GrxXmlUtil.getBoolean(e, "autostart", true);
			pi.autoStop = GrxXmlUtil.getBoolean(e, "autostop", true);
			processManager.register(pi);
		}
		processManager.autoStart();
	}

	public JMenu getMenu() {
		return processManager.getMenu();
	}
	
	public String[] getMenuPath() {
		return new String[]{"Tools"};
	}

	public void shutdown() {
		processManager.autoStop();
	}
}

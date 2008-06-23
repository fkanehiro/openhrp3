/*
 *  GrxSerialPortView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import com.generalrobotix.ui.GrxBaseController;
import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.shrpsys.item.GrxSerialPortItem;
import com.generalrobotix.ui.util.JTextAreaEx;

@SuppressWarnings("serial")
public class GrxSerialPortView extends GrxBaseView implements GrxBaseController {
	private GrxSerialPortItem currentItem_ = null;
	private JTextAreaEx area_ = new JTextAreaEx("");
	private int maxLineNumber_ = 200;

	public GrxSerialPortView(String name, GrxPluginManager manager) {
		super(name, manager);

		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		area_.setEditable(false);
		contentPane.add(area_, BorderLayout.CENTER);
	}

	public boolean cleanup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void control(List<GrxBaseItem> itemList) {
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(currentItem_)) {
			Iterator<GrxBaseItem> it = itemList.iterator();
			currentItem_ = null;
			while (it.hasNext()) {
				GrxBaseItem item = it.next();
				if (item instanceof GrxSerialPortItem) {
					currentItem_ = (GrxSerialPortItem) item;
					break;
				}
			}

			if (currentItem_ != null) {
				try {
					while (currentItem_.ready()) {
						area_.append(currentItem_.readLine());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				int extraLine = area_.getLineCount() - maxLineNumber_;
				int pos = 0;
				for (int i = 0; i < extraLine; i++)
					pos = area_.getText().indexOf("\n", pos + 1);
				if (extraLine > 0)
					area_.setText(area_.getText().substring(pos));
				area_.setCaretPosition(area_.getText().length());
			}
		}
	}
}

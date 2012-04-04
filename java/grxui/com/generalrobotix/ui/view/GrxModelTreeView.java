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
 *  GrxModelTreeView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.util.*;

import javax.swing.*;
import javax.swing.JTree.DynamicUtilTreeNode;
import javax.swing.tree.*;
import javax.media.j3d.*;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.item.GrxModelItem;

@SuppressWarnings("serial")
public class GrxModelTreeView extends GrxBaseView {
    public static final String TITLE = "Model Tree";

	private DefaultMutableTreeNode root_ = new DefaultMutableTreeNode();
	private DefaultTreeModel model_ = new DefaultTreeModel(root_);
	private JTree tree_ = new JTree(model_);
	private GrxModelItem currentModel_;
	
	public GrxModelTreeView(String name, GrxPluginManager manager) {
		super(name, manager);
		root_.setAllowsChildren(true);
		JPanel contentPane = getContentPane();
		contentPane.add(tree_);
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (currentModel_ == null || !currentModel_.isSelected()) {
			root_.removeAllChildren();
			List l = manager_.getSelectedItemList(GrxModelItem.class);
			if (l.size() > 0) {
				currentModel_ = (GrxModelItem)l.get(0);
				root_.setUserObject(currentModel_.getName());
				tree_.setVisible(false);
				traverse(currentModel_.bgRoot_, root_);
				_expandAll();
				tree_.setVisible(true);
			}
		}
	}
	
	void traverse(Node node, DefaultMutableTreeNode tparent) {
		String info = node.getClass().getSimpleName();
		Object udat = node.getUserData();
		if (udat instanceof Map) {
			Object o = ((Map)udat).get("jointName");
			if (o != null)
				info += ":"+o;
		}
		//System.out.println(info);
		if (node instanceof Group) {
			DynamicUtilTreeNode tnode = new DynamicUtilTreeNode(info, new HashMap());
			tnode.setAllowsChildren(true);
			tparent.add(tnode);
			try {
				Enumeration e = ((Group)node).getAllChildren();
				while (e.hasMoreElements()) {
					Node n = (Node)e.nextElement();
					traverse(n, tnode);
				}
			} catch (Exception e) {
				e.printStackTrace();
				info += " --- can't read children";
			}
		} else if (node instanceof Link) {
			DynamicUtilTreeNode tnode = new DynamicUtilTreeNode(info, new HashMap());
			tnode.setAllowsChildren(true);
			tparent.add(tnode);
			Link l = (Link) node;
			SharedGroup sg = l.getSharedGroup();
			for (int i=0; i<sg.numChildren() ; i++) {
				traverse(sg.getChild(i), tnode);
			}
		} else {
			DefaultMutableTreeNode tnode = new DefaultMutableTreeNode(info);
			tnode.setAllowsChildren(true);
			tparent.add(tnode);
		}
	}
	
	private void _expandAll() {
		for (int i = 0; i < tree_.getRowCount(); i++)
			tree_.expandRow(i);
	}
}

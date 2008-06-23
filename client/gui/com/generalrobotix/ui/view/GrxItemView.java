/*
 *  GrxItemView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.generalrobotix.ui.*;

@SuppressWarnings("unchecked")
public class GrxItemView extends GrxBaseView {
    public static final String TITLE = "Item View";

	public JTree tree_ = null;

	public GrxItemView(String name, GrxPluginManager manager) {
		super(name, manager);

		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		contentPane.setBackground(Color.white);

		tree_ = new JTree(manager_.treeModel_);
		tree_.setAlignmentX(JTree.LEFT_ALIGNMENT);
		tree_.setRootVisible(true);
		
		tree_.setEditable(true);
		tree_.setCellEditor(new ItemTreeCellEditor(tree_));
		tree_.setCellRenderer(new ItemTreeCellRenderer());
		tree_.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					tree_.setEditable(false);
					TreePath selPath = tree_.getPathForLocation(e.getX(), e.getY());
					if (selPath == null) {
						tree_.setEditable(true);
						return;
					}
					
					tree_.setSelectionPath(selPath);
					DefaultMutableTreeNode node = 
						(DefaultMutableTreeNode)selPath.getLastPathComponent();
					Object usrObj = node.getUserObject();

					JMenu menu = null;
					if (node.isRoot()) {
						menu = ((GrxBaseItem) usrObj).getMenu();
					} else if (usrObj instanceof Class) {
						menu = manager_.getItemMenu((Class<? extends GrxBaseItem>) usrObj);
					} else if (usrObj instanceof GrxBaseItem) {
						menu = ((GrxBaseItem) usrObj).getMenu();
					} else {
						tree_.setEditable(true);
						return;
					}

					if (menu != null) {
						JPopupMenu pm = menu.getPopupMenu();
						pm.setLightWeightPopupEnabled(false);
						pm.show(e.getComponent(), e.getX(),e.getY());
					}
				}
				tree_.setEditable(true);
			}
		});

		tree_.getModel().addTreeModelListener(new TreeModelListener() {
			public void treeNodesChanged(TreeModelEvent arg0) {}
			public void treeNodesInserted(TreeModelEvent arg0) {}
			public void treeNodesRemoved(TreeModelEvent arg0) {}

			public void treeStructureChanged(TreeModelEvent arg0) {
              SwingUtilities.invokeLater(
                new Runnable() {
                  public void run() {
                    expand();
                  }
                }
              );
			}
		});
		
		contentPane.add(tree_);
	}

	private class ItemTreeCellRenderer implements TreeCellRenderer {
		GrxBasePlugin plugin = null;
		DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
		JPanel panel = new JPanel();
		JCheckBox ckbox = new JCheckBox();
		JLabel label = new JLabel("");
		Color selectionForeground;
		Color selectionBackground;
		Color textForeground;
		Color textBackground;

		public ItemTreeCellRenderer() {
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			label.setFont(new Font("Sans-serif", Font.TRUETYPE_FONT, 12));
			ckbox.setBackground(textBackground);
			panel.add(ckbox);
			panel.add(label);
			selectionForeground = UIManager.getColor("Tree.selectionForeground");
			selectionBackground = UIManager.getColor("Tree.selectionBackground");
			textForeground = UIManager.getColor("Tree.textForeground");
			textBackground = UIManager.getColor("Tree.textBackground");
		}

		public GrxBasePlugin getLeafRenderer() {
			return plugin;
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			Component ret = null;
			if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)value; 
				Object usrObj = node.getUserObject();
				if (usrObj instanceof Class &&
						GrxBasePlugin.class.isAssignableFrom((Class) usrObj)) {
					Class<? extends GrxBasePlugin> cls = (Class<? extends GrxBasePlugin>) usrObj;
					label.setText(manager_.getItemTitle(cls));
					if (expanded)
						label.setIcon(defaultRenderer.getOpenIcon());
					else
						label.setIcon(defaultRenderer.getClosedIcon());
					ckbox.setVisible(false);
					ret = panel;
				} else if (usrObj instanceof GrxBaseItem) {
					plugin = (GrxBasePlugin) usrObj;
					label.setText(plugin.getName());
					if (node.isRoot()) {
						ckbox.setVisible(false);
						if (expanded)
							label.setIcon(defaultRenderer.getOpenIcon());
						else
							label.setIcon(defaultRenderer.getClosedIcon());
					} else {
						ckbox.setSelected(plugin.isSelected());
						ckbox.setVisible(true);
						label.setEnabled(tree.isEnabled());
						Icon icon = plugin.getIcon();
						if (icon == null)
							icon = defaultRenderer.getLeafIcon();
						label.setIcon(icon);
					}
					ret = panel;
				}
			}

			if (ret == null) {
				ret = defaultRenderer.getTreeCellRendererComponent(tree, value,
						selected, expanded, leaf, row, hasFocus);
			} else {
				if (selected) {
					panel.setForeground(selectionForeground);
					panel.setBackground(selectionBackground);
				} else {
					panel.setForeground(textForeground);
					panel.setBackground(textBackground);
				}
			}
			return ret;
		}
	}

	class ItemTreeCellEditor
	    extends AbstractCellEditor implements TreeCellEditor {

		ItemTreeCellRenderer renderer = new ItemTreeCellRenderer();
		ChangeEvent changeEvent = null;
		JTree tree;
		JCheckBox ckbox;

		public ItemTreeCellEditor(JTree tree) {
			this.tree = tree;
		}

		public Object getCellEditorValue() {
			return renderer.getLeafRenderer();
		}

		public boolean isCellEditable(EventObject event) {
			boolean returnValue = false;
			if (event instanceof MouseEvent) {
				MouseEvent mouseEvent = (MouseEvent) event;
				TreePath path = tree.getPathForLocation(mouseEvent.getX(),
						mouseEvent.getY());
				if (path != null) {
					Object node = path.getLastPathComponent();
					if ((node != null)
							&& (node instanceof DefaultMutableTreeNode)) {
						DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
						returnValue = ((treeNode.isLeaf()) && (treeNode.getUserObject() instanceof GrxBaseItem));
					}
				}
			}
			return returnValue;
		}

		public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row) {

			Component editor = renderer.getTreeCellRendererComponent(tree,
					value, true, expanded, leaf, row, true);

			if (editor instanceof JPanel) {
				Component c = ((JPanel) editor).getComponent(0);
				if (c instanceof JCheckBox) {
					ckbox = (JCheckBox) c;
					if (ckbox.getListeners(ItemListener.class).length == 0) {
						ckbox.addItemListener(new ItemListener() {
							public void itemStateChanged(ItemEvent itemEvent) {
								if (stopCellEditing()) {
									fireEditingStopped();
								}
							}
						});
					}
				}
			} else {
				ckbox = null;
			}
			return editor;
		}

		public boolean stopCellEditing() {
			GrxBaseItem item = (GrxBaseItem) renderer.getLeafRenderer();
			manager_.setSelectedItem(item, ckbox.isSelected());
			return true;
		}
	}
	
	public void expand() {
		for (int i = 0; i < tree_.getRowCount(); i++) 
			tree_.expandRow(i);
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		tree_.repaint();
	}
	public boolean setup(List<GrxBaseItem> itemList) {
		expand();
		return true;
	}

	public GrxBasePlugin getFocusedItem() {
		TreePath path = tree_.getSelectionPath();
		if (path != null) {
			Object usrObj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
			if (usrObj instanceof GrxBasePlugin)
				return (GrxBasePlugin)usrObj;
		}

		return null;
	}
}

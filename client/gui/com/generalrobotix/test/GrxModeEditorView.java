/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.test;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModeInfoItem;
import com.generalrobotix.ui.util.TableSorter;

@SuppressWarnings("serial")
public class GrxModeEditorView extends GrxBaseView {
	private JScrollPane sPane_ = null;
	private JTable table_ = null;
	private JMenu menu_ = null;
	
	private DefaultTableModel model_ = null;
	private String[] columnNames_ = { "property", "value" };

	private GrxModeInfoItem currentItem_ = null;

	public GrxModeEditorView(String name, GrxPluginManager manager) {
		super(name, manager);

		JPanel contentPane_ = getContentPane();
		contentPane_.setLayout(new BorderLayout());
		model_ = new DefaultTableModel(columnNames_, 1) {
			public boolean isCellEditable(int row, int column) {
				if (column == 0)
					return false;
				return true;
			}
		};

		TableSorter sorter = new TableSorter(model_);
		table_ = new JTable(sorter);
		sorter.setTableHeader(table_.getTableHeader());

		TableColumn tc = table_.getColumn(columnNames_[0]);
		tc.setPreferredWidth(40);

		sPane_ = new JScrollPane(table_);
		contentPane_.add(sPane_, BorderLayout.CENTER);

		menu_ = new JMenu("Mode Info");
		JMenuItem addItem = new JMenuItem("add Item");
		addItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < model_.getRowCount(); i++) {
					String str = (String) model_.getValueAt(i, 0);
					if (str == null || str.equals("")) {
						model_.setValueAt("item0", 0, 0);
						break;
					} else if (str.startsWith("view")) {
						model_.insertRow(i, new String[] { "item" + i, "" });
						break;
					} else if (i == model_.getRowCount() - 1) {
						model_.insertRow(i + 1, new String[] {
								"item" + (i + 1), "" });
						break;
					}
				}
			}
		});
		JMenuItem addView = new JMenuItem("add View");
		addView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String str = (String) model_.getValueAt(
						model_.getRowCount() - 1, 0);
				if (str == null || str.equals("")) {
					model_.setValueAt("view0", 0, 0);
				} else if (str.startsWith("item")) {
					model_.addRow(new String[] { "view0", "" });
				} else {
					str = str.replace("view", "");
					model_.addRow(new String[] {
							"view" + (Integer.parseInt(str) + 1), "" });
				}
			}
		});
		JMenuItem delete = new JMenuItem("delete");
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < model_.getRowCount(); i++) {
					if (table_.isRowSelected(i) && i > 0) {
						model_.removeRow(i);
						String str = (String) model_.getValueAt(i, 0);
						if (str.startsWith("item")) {

						}
					}
				}
			}
		});
		JMenuItem apply = new JMenuItem("apply");
		apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

			}
		});

		menu_.add(addItem);
		menu_.add(addView);
		menu_.add(delete);
		menu_.add(apply);
		MouseAdapter ma = new MouseAdapter() {
			public void mousePressed(MouseEvent arg0) {
				if (arg0.getButton() == MouseEvent.BUTTON3) {
					menu_.getPopupMenu().show((Component) arg0.getSource(),
							arg0.getX(), arg0.getY());
				}
			}
		};

		contentPane_.addMouseListener(ma);
		sPane_.addMouseListener(ma);
		table_.addMouseListener(ma);
	}

	public void control(List<GrxBaseItem> itemList) {
	}

	public boolean cleanup(List<GrxBaseItem> itemList) {
		return true;
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(currentItem_)) {
			currentItem_ = null;
			Iterator<GrxBaseItem> it = itemList.iterator();
			while (it.hasNext()) {
				currentItem_ = (GrxModeInfoItem) it.next();
				List _itemList = currentItem_.activeItemClassList_;
				//List _viewList = currentItem_.activeViewClassList_;
				if (itemList.size() != table_.getRowCount())
					model_.setRowCount(_itemList.size() + 1);
				for (int i = 0; i < itemList.size(); i++) {
					table_.setValueAt(_itemList.get(i), i, 1);
				}
			}
		}
	}
}

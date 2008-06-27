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
 *  GrxPropertyView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.TableSorter;

@SuppressWarnings("serial")
public class GrxPropertyView extends GrxBaseView {
    public static final String TITLE = "Property";

	private GrxBasePlugin currentPlugin_;
	private GrxItemView itemView_;
	
	private PropertyTableModel dataModel_ = new PropertyTableModel();
	private TableSorter sorter_ = new TableSorter(dataModel_);
	private JTable table_ = new JTable(sorter_);
	private JScrollPane scPane_ = new JScrollPane(table_);
    private int[] sortStatus_;
	
	public GrxPropertyView(String name, GrxPluginManager manager) {
		super(name, manager);
		isScrollable_ = false;
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		contentPane.add(scPane_);
		sorter_.setTableHeader(table_.getTableHeader());
        sortStatus_ = new int[table_.getColumnCount()];
        for (int i=0; i<sortStatus_.length; i++)
        	sortStatus_[i] =TableSorter.NOT_SORTED;
	}
	
	public boolean setup(List<GrxBaseItem> itemList) { 
		itemView_ = (GrxItemView)manager_.getView(GrxItemView.class);
		return true; 
	}
	
	public void control(List<GrxBaseItem> itemList) {
		GrxBasePlugin plugin = itemView_.getFocusedItem();
		if (plugin == null) {
			currentPlugin_ = null;
		} else if (plugin != currentPlugin_) {
			table_.setVisible(false);
			for (int i=0; i<sortStatus_.length; i++) {
				sortStatus_[i] = sorter_.getSortingStatus(i);
				sorter_.setSortingStatus(i, TableSorter.NOT_SORTED);
			}
			currentPlugin_ = plugin;
			dataModel_.entry_ = currentPlugin_.keySet().toArray(new String[0]);
			for (int i=0; i<table_.getColumnCount(); i++) 
				sorter_.setSortingStatus(i, sortStatus_[i]);

			table_.setVisible(true);
		}
	}

	private class PropertyTableModel extends AbstractTableModel {
		private String[] entry_ = new String[0];
        private final String[] clmName_ ={"Name", "Value"};
        
        public String getColumnName(int col) { 
        	return clmName_[col];
        }
        
        public int getColumnCount() { 
        	return clmName_.length; 
       	}
        
        public int getRowCount() { 
        	return entry_.length;
        }
        
        public Object getValueAt(int row, int col) {
            String str = null;
            if (currentPlugin_ != null && 
            		entry_.length > row && 
            		entry_[row] != null) {
                switch (col) {
                case 0:
                	str = entry_[row];
                	break;
                case 1:
                	str = currentPlugin_.getProperty(entry_[row], "");
                	break;
                default:
                	str = "---";
                	break;
                }
                return str;
            }
            	
            return "";
        }
        
        public void setValueAt(Object val, int row, int col) {
        	if (col == 1) {
        		Object key = getValueAt(row, 0);			
	        	currentPlugin_.setProperty((String)key, (String)val);
	        	currentPlugin_.propertyChanged();
        	}
        }
        
        public Class<? extends Object> getColumnClass(int c) {
        	return getValueAt(0, c).getClass();
        }
        
        public boolean isCellEditable(int row, int col) {
        	return (col > 0);
        }
	}
}

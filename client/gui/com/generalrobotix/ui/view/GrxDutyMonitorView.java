/*
 *  GrxDutyMonitorView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxDutyMonitorView extends GrxBaseView {
    public static final String TITLE = "Duty Monitor";

	private JToggleButton updateButton_ = new JToggleButton("update");
	private JLabel label1_ = 
		new JLabel("Assigned Time [s]: "+format1.format(manager_.getDelay()/1000.0)+
		           "    Measured Time [s]: "+format1.format(manager_.now/1000.0));
	private JTable table_;
	private JScrollPane scPane_;
	
	private List<GrxBaseView> list_ = new ArrayList<GrxBaseView>();
	private static DecimalFormat format1 = new DecimalFormat("0.000");
	
	public GrxDutyMonitorView(String name, GrxPluginManager manager) {
		super(name, manager);
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		isScrollable_ = false;
		
	    AbstractTableModel dataModel = new AbstractTableModel() {
	        private final String[] clmName_ ={
	        	"View Name","State","Max [s]","Min [s]","Now [s]"
	        };
            
            public int getColumnCount() { return clmName_.length; }
            public int getRowCount() { return list_.size()+1;}
            public String getColumnName(int col) {
                return clmName_[col];
            }
            public Object getValueAt(int row, int col) {
            	if (row == list_.size()) {
            		if (col == 0)
            			return "TOTAL";
            		else if (col == 4)
            			return format1.format(total_/1000.0); 
            		return "---";
            	}
                GrxBaseView view = list_.get(row);
                String str = null;
                switch (col) {
                case 0:
                	str = view.getName();
                	break;
                case 1:
                	str = "---";
                	break;
                case 2:
                	str = format1.format(view.max/1000.0);
                	break;
                case 3:
                	str = format1.format(view.min/1000.0);
                	break;
                case 4:
                	str = format1.format(view.now/1000.0);
                	break;
                default:
                	str = "---";
                	break;
                }
                return str;
            }
            public Class<? extends Object> getColumnClass(int c) {return getValueAt(0, c).getClass();}
            public boolean isCellEditable(int row, int col) {return false;}
	    };
	    table_  = new JTable(dataModel);
	    scPane_ = new JScrollPane(table_);
		
		contentPane.add(updateButton_);
		contentPane.add(label1_);
		contentPane.add(scPane_);
	}
	
	double total_ = 0.0;
	public void control(List<GrxBaseItem> itemList) {
		if (updateButton_.isSelected()) {
			int size = list_.size();
			List<GrxBaseView> list = manager_.getActiveViewList();
			
		    double total = 0.0;
		    for (int i=0; i<list.size(); i++)
		    	total = list.get(i).now;
		    total_ = total;
		    if (size != list.size()) {
		    	table_.setVisible(false);
		    	list_ = list;
		    	table_.setVisible(true);
		    } else {
		    	list_ = list;
				table_.repaint();
		    }
			label1_.setText(
				"Assigned Time [s]: "+format1.format(manager_.getDelay()/1000.0)+
			    "    Measured Time [s]: "+format1.format(manager_.now/1000.0));
		}
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		return true;
	}
	
	public boolean cleanup(List<GrxBaseItem> itemList) {
		return true;
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
	}
}

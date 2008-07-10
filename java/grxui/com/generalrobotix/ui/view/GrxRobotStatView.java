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
 *  GrxRobotStatView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import jp.go.aist.hrp.simulator.SensorState;
//import jp.go.aist.hrp.simulator.SensorType;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxModelItem.LinkInfoLocal;
import com.generalrobotix.ui.item.GrxWorldStateItem.CharacterStateEx;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;

@SuppressWarnings("serial")
public class GrxRobotStatView extends GrxBaseView {
    public static final String TITLE = "Robot State";

	private static final DecimalFormat FORMAT1 = new DecimalFormat(" 0.0;-0.0");
	private static final DecimalFormat FORMAT2 = new DecimalFormat(" 0.000;-0.000");

	private static final Font MONO_PLAIN_12 = new Font("Monospaced", Font.PLAIN, 12);
	private static final Font MONO_BOLD_12 = new Font("Monospaced", Font.BOLD, 12);
	private static final Font MONO_BOLD_20 = new Font("Monospaced", Font.BOLD, 20);

	private GrxWorldStateItem currentWorld_;
	private GrxModelItem currentModel_;
	private SensorState  currentSensor_;
	private double[]	 currentRefAng_;
	private long[] 	     currentSvStat_;
	private long[] 	     currentCbStat_;
	private List<LinkInfoLocal> jointList_ = new ArrayList<LinkInfoLocal>();
	private String[] forceName_;
	
	private CardLayout clayout_ = new CardLayout();
	private JComboBox comboModelName_ = new JComboBox();
	private UpdatableTableModel[] tableModels_;
	private JTable[] tables_;
	private JScrollPane[] scPanes_;
	private JLabel lblInstruction1_ = new JLabel("Select Model Item on ItemView");
	private JLabel lblInstruction2_ = new JLabel();

	public GrxRobotStatView(String name, GrxPluginManager manager) {
		super(name, manager);
		JPanel contentPane = getContentPane();
		contentPane.setLayout(clayout_);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setAlignmentY(JPanel.TOP_ALIGNMENT);
		mainPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		Dimension d = new Dimension(200, 20);
		comboModelName_.setPreferredSize(d);
		comboModelName_.setMaximumSize(d);
		comboModelName_.setAlignmentY(JComponent.TOP_ALIGNMENT);
		comboModelName_.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		comboModelName_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				forceName_ = null;
				GrxModelItem item = (GrxModelItem)comboModelName_.getSelectedItem();
				if (item == null || item == currentModel_)
					return;

				currentModel_ = item;
				jointList_.clear();
				LinkInfoLocal[] lInfo = currentModel_.lInfo_;
				for (int i = 0; i < lInfo.length; i++) {
					for (int j = 0; j < lInfo.length; j++) {
						if (i == lInfo[j].jointId) {
							jointList_.add(lInfo[j]);
							break;
						}
					}
				}
				_resizeTables();
			}
		});
		mainPanel.add(comboModelName_);

		String[][] header = new String[][] {
				{ "No", "Joint", "Angle", "Target", "Current", "PWR", "SRV", "Pgain", "Dgain" },
				{ "Force", "Fx[N]", "Fy[N]", "Fz[N]", "Mx[Nm]", "My[Nm]", "Mz[Nm]" }, 
				{ "Sensor", "Xaxis", "Yaxis", "Zaxis" } };
		int[][] alignment = new int[][] {
				{ JLabel.LEFT, JLabel.LEFT,  JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.CENTER, JLabel.CENTER, JLabel.RIGHT, JLabel.RIGHT },
				{ JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT },
				{ JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT } };
		int[][] columnSize_ = new int[][] {
				{ 18, 100, 60, 60, 50, 28, 28, 32, 32 },
				{ 102, 51, 50, 51, 50, 51, 50 }, 
				{ 102, 101, 100, 100 } };

		tableModels_ = new UpdatableTableModel[] {
				new TableModelJoint(header[0]), new TableModelForce(header[1]),
				new TableModelSensor(header[2]) };
		tables_ = new JTable[header.length];
		scPanes_ = new JScrollPane[header.length];
		for (int i = 0; i < 3; i++) {
			JTable table = new JTable(tableModels_[i]);
			table.setEnabled(false);
			table.setDefaultRenderer(Object.class, new MyCellRenderer(
					alignment[i]));
			for (int j = 0; j < table.getColumnCount(); j++) {
				TableColumn tc = table.getColumn(table.getColumnName(j));
				tc.setPreferredWidth(columnSize_[i][j]);
			}
			tables_[i] = table;
			scPanes_[i] = new JScrollPane(table);
			scPanes_[i].setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			scPanes_[i].setAlignmentY(JScrollPane.TOP_ALIGNMENT);
			scPanes_[i].setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
			mainPanel.add(scPanes_[i]);
		}
		
		lblInstruction1_.setForeground(Color.gray);
		lblInstruction1_.setFont(MONO_BOLD_20);
		lblInstruction1_.setHorizontalAlignment(JLabel.CENTER);
		
		lblInstruction2_.setForeground(Color.gray);
		lblInstruction2_.setFont(MONO_BOLD_20);
		lblInstruction2_.setHorizontalAlignment(JLabel.CENTER);
		
		contentPane.add(mainPanel,"main");
		clayout_.addLayoutComponent(mainPanel,"main");
		contentPane.add(lblInstruction1_,"instruction1");
		clayout_.addLayoutComponent(lblInstruction1_,"instruction1");
		contentPane.add(lblInstruction1_,"instruction2");
		clayout_.addLayoutComponent(lblInstruction1_,"instruction2");
	}

	public void restoreProperties() {
		super.restoreProperties();
		_resizeTables();
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		Iterator<GrxBaseItem> it = itemList.iterator();
		comboModelName_.removeAllItems();
		while (it.hasNext()) {
			GrxBaseItem item = it.next();
			if (item instanceof GrxWorldStateItem)
				currentWorld_ = (GrxWorldStateItem) item;
			else if (item instanceof GrxModelItem && ((GrxModelItem)item).isRobot()) {
				comboModelName_.addItem(item);
			}
		}
		
		if (comboModelName_.getItemCount() > 1) {
			comboModelName_.setVisible(true);
			clayout_.show(getContentPane(), "main");
		} else if (comboModelName_.getItemCount() == 1) {
			comboModelName_.setVisible(false);
			clayout_.show(getContentPane(), "main");
		} else {
			clayout_.show(getContentPane(), "instruction1");
		}

		_resizeTables();
	}

	public void control(List<GrxBaseItem> itemList) {
		if (currentModel_ == null)
			return;

		if (currentWorld_ != null && currentWorld_.getLogSize() > 0) {
			WorldStateEx state = currentWorld_.getValue();
			if (state != null) {
				CharacterStateEx charStat = state.get(currentModel_.getName());
				if (charStat != null) {
					currentSensor_ = charStat.sensorState;
					currentRefAng_ = charStat.targetState;
					currentSvStat_ = charStat.servoState;
					currentCbStat_ = charStat.calibState;
				}
				
				if (forceName_ == null) {
					//#####[[Changed]] NewModelLoader.IDL
					//forceName_ = currentModel_.getSensorNames(SensorType.FORCE_SENSOR);
					forceName_ = currentModel_.getSensorNames("Force");

					_resizeTables();
				}
			}
		}
		for (int i=0; i < tables_.length; i++)
			tableModels_[i].updateAsNeeded();
	}

	private void _resizeTables() {
		//getContentPane().setVisible(false);
		for (int i = 0; i < tables_.length; i++) {
			if (tables_[i].getRowCount() > 0) {
				Dimension d = tables_[i].getPreferredSize();
				d.height += scPanes_[i].getColumnHeader().getPreferredSize().height + 3;
				scPanes_[i].setPreferredSize(d);
				scPanes_[i].setMaximumSize(new Dimension(1000, d.height));
				scPanes_[i].setVisible(true);
			} else
				scPanes_[i].setVisible(false);
		}
		//getContentPane().setVisible(true);
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		_resizeTables();
		return true;
	}

	class CellState {
		String value = null; 
		Color bgColor = Color.white;
		Color fgColor = Color.black;
		Font font = MONO_PLAIN_12;
	}

	class MyCellRenderer extends JLabel implements TableCellRenderer {
		private int[] columnAlignment_ = null;

		public MyCellRenderer(int[] columnAlignment) {
			super();
			columnAlignment_ = columnAlignment;
			setOpaque(true);
			setBackground(Color.white);
			setForeground(Color.black);
			setFont(MONO_PLAIN_12);
		}

		public Component getTableCellRendererComponent(JTable table,
				Object data, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setHorizontalAlignment(columnAlignment_[column]);
			if (data == null) {
				setText("---");
				setForeground(Color.black);
				setBackground(Color.white);
				setFont(MONO_PLAIN_12);
			} else if (data instanceof String) {
				setText((String) data);
				setForeground(Color.black);
				setBackground(Color.white);
				setFont(MONO_PLAIN_12);
			} else if (data instanceof CellState) {
				CellState state = (CellState) data;
				setText(state.value);
				setForeground(state.fgColor);
				setBackground(state.bgColor);
				setFont(state.font);
			}
			return this;
		}
	}

	class UpdatableTableModel extends DefaultTableModel {
		public UpdatableTableModel(String[] columnName, int rowNum) {
			super(columnName, rowNum);
		}

		public void updateCell(int row, int col) {
			if (getValueAt(row, col) != null)
				fireTableCellUpdated(row, col);
		}

		public void updateRow(int firstRow, int lastRow) {
			fireTableRowsUpdated(firstRow, lastRow);
		}

		public void updateAll() {
			updateRow(0, getRowCount());
		}

		public void updateAsNeeded() {
			for (int i = 0; i < getRowCount(); i++) {
				for (int j = 0; j < getColumnCount(); j++) {
					updateCell(i, j);
				}
			}
		}
	}

	class TableModelJoint extends UpdatableTableModel {
		CellState state_ = new CellState();

		public TableModelJoint(String[] columnName) {
			super(columnName, 0);
		}

		public int getRowCount() {
			return jointList_.size();
		}

		public Object getValueAt(int row, int col) {
			if (row < getRowCount()) {
				state_.value = null;
				if (currentModel_ != null) {
					if (jointList_.get(row) == currentModel_.activeLinkInfo_) {
						state_.value = "---";
						state_.bgColor = Color.yellow;
					} else {
						state_.bgColor = Color.white;
					}
					state_.fgColor = Color.black;
					state_.font = MONO_PLAIN_12;
				}
				switch (col) {
				case 0:
					state_.value = Integer.toString(row);
					if (currentSvStat_ != null && _isSwitchOn(row, currentSvStat_)) {
						state_.fgColor = Color.red;
						state_.font = MONO_BOLD_12;
					//	currentModel_.setJointColor(row, Color.red);
					} else if (currentCbStat_ != null && !_isSwitchOn(row, currentCbStat_)) {
						state_.fgColor = Color.yellow;
						state_.font = MONO_BOLD_12;
					//	currentModel_.setJointColor(row, Color.yellow);
					} else {
					//	currentModel_.setJointColor(row, null);
					}
					return state_;
				case 1:
					if (jointList_.size() <= 0)
						break;
					state_.value = jointList_.get(row).name;
					return state_;
				case 2:
					// if (currentAstate_ != null)
					// return
					// FORMAT1.format(Math.toDegrees(currentAstate_.angle[row]));
					if (jointList_.size() <= 0)
						break;
					LinkInfoLocal li = jointList_.get(row);
					state_.value = FORMAT1.format(Math.toDegrees(li.jointValue));
					//#####[[Changed]] 2008.03.27
					if (li.llimit[0] < li.ulimit[0] && (
							li.jointValue <= li.llimit[0] || li.ulimit[0]  <= li.jointValue)) {
						state_.font = MONO_BOLD_12;
						state_.fgColor = Color.red;
					}
					return state_;
				case 3:
					if (currentRefAng_ == null)
						break;
					state_.value = FORMAT1.format(Math.toDegrees(currentRefAng_[row]));
					return state_;
				case 6:
					if (currentSvStat_ == null)
						break;
					if (_isSwitchOn(row, currentSvStat_)) {
						state_.value = "ON";
						state_.fgColor = Color.red;
						state_.font = MONO_BOLD_12;
					} else
						state_.value = "OFF";
					return state_;
				case 4:
				case 5:
				case 7:
				case 8:
				default:
					break;
				}
				//if (state_.value != null)
				//  return state_;
			}

			return null;
		}
	};

	private boolean _isSwitchOn(int ch, long[] state) {
		long a = 1 << (ch % 64);
		int idx = ch/64;
		if (state.length > idx && (state[idx] & a) > 0)
			return true;
		return false;
	}

	class TableModelForce extends UpdatableTableModel {
		public TableModelForce(String[] columnName) {
			super(columnName, 0);
		}

		public int getRowCount() {
			if (currentSensor_ == null || currentSensor_.force == null)
				return 0;
			return currentSensor_.force.length;
		}

		public Object getValueAt(int row, int col) {
			if (col == 0) {
				if (forceName_ != null)
					return forceName_[row];
			} else if (col < getColumnCount())
				return FORMAT2.format(currentSensor_.force[row][col - 1]);

			return null;
		}
	};

	class TableModelSensor extends UpdatableTableModel {
		public SensorState currentValue;
		
		public TableModelSensor(String[] columnName) {
			super(columnName, 0);
		}

		public int getRowCount() {
			if (currentSensor_ == null || currentSensor_.accel == null
					|| currentSensor_.rateGyro == null)
				return 0;
			return currentSensor_.accel.length + currentSensor_.rateGyro.length;
		}

		public Object getValueAt(int row, int col) {
			if (currentSensor_.accel == null || currentSensor_.accel == null
					|| currentSensor_.rateGyro == null)
				return null;

			int numAccel = currentSensor_.accel.length;
			if (col == 0) {
				if (row < numAccel)
					return "Acc_" + row + "[m/s^2]";
				else
					return "Gyro_" + (row - numAccel) + "[rad/s]";
			} else {
				if (row < numAccel)
					return FORMAT2.format(currentSensor_.accel[row][col - 1]);
				else
					return FORMAT2.format(currentSensor_.rateGyro[row - numAccel][col - 1]);
			}
		}
	}
}

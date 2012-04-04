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
import jp.go.aist.hrp.simulator.LinkPosition;

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
        private LinkPosition[] currentPosition_;
	private GrxModelItem currentModel_;
	private SensorState  currentSensor_;
	private double[]     currentRefAng_;
	private long[] 	     currentSvStat_;
	private int[]		 currentJtStat_;
	private long[] 	     currentCbStat_;
	private double[]	 currentPwStat_;
	private List<LinkInfoLocal> jointList_ = new ArrayList<LinkInfoLocal>();
	private String[] forceName_;
	//private WorldStateEx lastWorldState_ = null;
	private double prevTime_ = -1;
	
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
		    { "No", "Joint", "Angle", "Target", "Torque", "PWR", "SRV", "ARM", "T", "Pgain", "Dgain" },
				{ "Position", "X[m]", "Y[m]", "Z[m]"},
				{ "Force", "Fx[N]", "Fy[N]", "Fz[N]", "Mx[Nm]", "My[Nm]", "Mz[Nm]" }, 
				{ "Sensor", "Xaxis", "Yaxis", "Zaxis" },
				{ "Voltage[V]", "Current[A]"},
				{ "ZMP", "Right X[m]", "Right Y[m]", "Left X[m]", "Left Y[m]" }
		};
		int[][] alignment = new int[][] {
				{ JLabel.LEFT, JLabel.LEFT,  JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.CENTER, JLabel.CENTER, JLabel.CENTER, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT },
				{ JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT},
				{ JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT },
				{ JLabel.LEFT, JLabel.RIGHT, JLabel.RIGHT, JLabel.RIGHT },
				{ JLabel.RIGHT, JLabel.RIGHT},
				{ JLabel.LEFT, JLabel.RIGHT,  JLabel.RIGHT,  JLabel.RIGHT,  JLabel.RIGHT } 
		};
		int[][] columnSize_ = new int[][] {
		                { 18, 100, 60, 60, 50, 28, 28, 28, 28, 32, 32 },
				{ 102, 101, 100, 100 },
				{ 102, 51, 50, 51, 50, 51, 50 }, 
				{ 102, 101, 100, 100 },
				{ 100, 100 },
				{ 60, 70, 70, 70, 70 }
		};

		tableModels_ = new UpdatableTableModel[] {
				new TableModelJoint(header[0]),
				new TableModelPosition(header[1]),
				new TableModelForce(header[2]),
				new TableModelSensor(header[3]),
				new TableModelPower(header[4]),
				new TableModelZmp(header[5]),
		};
		tables_ = new JTable[header.length];
		scPanes_ = new JScrollPane[header.length];
		for (int i = 0; i < 6; i++) {
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
			if (state != null && prevTime_ != state.time) {		//state != lastWorldState_) {
				CharacterStateEx charStat = state.get(currentModel_.getName());
				if (charStat != null) {
				        currentPosition_ = charStat.position;
					currentSensor_ = charStat.sensorState;
					currentRefAng_ = charStat.targetState;
					currentSvStat_ = charStat.servoState;
					currentJtStat_ = charStat.jointState;
					currentCbStat_ = charStat.calibState;
					currentPwStat_ = charStat.powerState;
				}
				
				if (forceName_ == null) {
					forceName_ = currentModel_.getSensorNames("Force");
					_resizeTables();
				}
//				lastWorldState_ = state;
				prevTime_ = state.time;
				for (int i=0; i < tables_.length; i++)
				        tableModels_[i].updateAsNeeded();
			}
		}
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
	        final int CALIB_STATE_MASK = 0x1;
	        final int SERVO_STATE_MASK = 0x2;
		final int POWER_STATE_MASK = 0x4;
		final int SERVO_ALARM_MASK = 0x7fff8;
		final int SERVO_ALARM_SHIFT = 3;
		final int DRIVER_TEMP_MASK = 0xff000000;
		final int DRIVER_TEMP_SHIFT = 24;
		
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
				case 0: // joint id
					state_.value = Integer.toString(row);
					if ((currentSvStat_ != null && _isSwitchOn(row, currentSvStat_))
					    || (currentJtStat_ != null && (currentJtStat_[row]&SERVO_STATE_MASK) != 0)) {
						state_.fgColor = Color.red;
						state_.font = MONO_BOLD_12;
					//	currentModel_.setJointColor(row, Color.red);
					} else if ((currentCbStat_ != null && !_isSwitchOn(row, currentCbStat_))
						   || (currentJtStat_ != null && (currentJtStat_[row]&CALIB_STATE_MASK) == 0)){
						state_.fgColor = Color.yellow;
						state_.font = MONO_BOLD_12;
					//	currentModel_.setJointColor(row, Color.yellow);
					} else {

					//	currentModel_.setJointColor(row, null);
					}
					return state_;
				case 1: // joint name
					if (jointList_.size() <= 0)
						break;
					state_.value = jointList_.get(row).name;
					return state_;
				case 2: // actual angle
					// if (currentAstate_ != null)
					// return
					// FORMAT1.format(Math.toDegrees(currentAstate_.angle[row]));
					if (jointList_.size() <= 0)
						break;
					LinkInfoLocal li = jointList_.get(row);
					state_.value = FORMAT1.format(Math.toDegrees(li.jointValue));
					if (li.llimit.length > 0 
					    && li.ulimit.length > 0
					    && li.llimit[0] < li.ulimit[0]
					    && (
						li.jointValue <= li.llimit[0] || li.ulimit[0]  <= li.jointValue)) {
						state_.font = MONO_BOLD_12;
						state_.fgColor = Color.red;
					}
					return state_;
				case 3: // reference angle
					if (currentRefAng_ == null)
						break;
					state_.value = FORMAT1.format(Math.toDegrees(currentRefAng_[row]));
					return state_;
				case 4: // torque
					if (currentSensor_ == null || currentSensor_.u == null)
						break;
					state_.value = FORMAT1.format(currentSensor_.u[row]);
					return state_;
				case 5: // power status
					if (currentJtStat_ == null) break;
					int power = currentJtStat_[row]&POWER_STATE_MASK;
					if (power != 0){
						state_.value = "ON";
					}else{
						state_.value = "OFF";
					}
					return state_;
				case 6: // servo status
				        if ((currentSvStat_ != null && _isSwitchOn(row, currentSvStat_))
					    || (currentJtStat_ != null && (currentJtStat_[row]&SERVO_STATE_MASK) != 0)){
						state_.value = "ON";
						state_.fgColor = Color.red;
						state_.font = MONO_BOLD_12;
					} else
						state_.value = "OFF";
					return state_;
				case 7: // servo alarm
					if (currentJtStat_ == null) break;
					int alarm = (currentJtStat_[row]&SERVO_ALARM_MASK)>>SERVO_ALARM_SHIFT;
					state_.value = String.format("%03x", alarm);
					if (alarm != 0){
						state_.fgColor = Color.red;
						state_.font = MONO_BOLD_12;
					}else{
						
					}
					return state_;
				case 8: // driver temperature
					if (currentJtStat_ == null) break;
					int temp = (currentJtStat_[row]&DRIVER_TEMP_MASK)>>DRIVER_TEMP_SHIFT;
					state_.value = String.format("%3d", temp);
					if (temp > 60){
						state_.fgColor = Color.red;
						state_.font = MONO_BOLD_12;
					}else{
						
					}
					return state_;
				case 9: // pgain
				case 10: // dgain
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
		long a = 1L << (ch % 64);
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
	class TableModelPosition extends UpdatableTableModel {
		
		public TableModelPosition(String[] columnName) {
			super(columnName, 0);
		}

		public int getRowCount() {
		        if (currentPosition_ == null || currentPosition_.length < 1)
				return 0;
			else
			        return 1;
		}

		public Object getValueAt(int row, int col) {
			if (col == 0) {
			    return "position";
			} else {
			    return FORMAT2.format(currentPosition_[0].p[col-1]);
			}
		}
	}
	class TableModelPower extends UpdatableTableModel {
		public TableModelPower(String[] columnName) {
			super(columnName, 0);
		}

		public int getRowCount() {
			if (currentPwStat_ == null){
				return 0;
			}else{
				return 1;
			}
		}

		public Object getValueAt(int row, int col) {
			if (currentPwStat_ == null) return null;
			if (col == 0){
				return FORMAT1.format(currentPwStat_[0]);
			}else{
				return FORMAT1.format(currentPwStat_[1]);
			}
		}
	};

	class TableModelZmp extends UpdatableTableModel {
		public TableModelZmp(String[] columnName) {
			super(columnName, 0);
		}

		public int getRowCount() {
			if (currentSensor_ == null || currentSensor_.force == null || currentSensor_.force.length < 2)
				return 0;
			return 1;
		}

		public Object getValueAt(int row, int col) {
		    double fz, min_fz = 25.0;
		    switch(col){
		    case 1:
			fz = currentSensor_.force[0][2];
			if (fz > min_fz){
			    return FORMAT2.format(-currentSensor_.force[0][4]/fz);
			}else{
			    return FORMAT2.format(0.0);
			}
		    case 2:
			fz = currentSensor_.force[0][2];
			if (fz > min_fz){
			    return FORMAT2.format(currentSensor_.force[0][3]/currentSensor_.force[0][2]);
			}else{
			    return FORMAT2.format(0.0);
			}
		    case 3:
			fz = currentSensor_.force[1][2];
			if (fz > min_fz){
			    return FORMAT2.format(-currentSensor_.force[1][4]/currentSensor_.force[1][2]);
			}else{
			    return FORMAT2.format(0.0);
			}
		    case 4:
			fz = currentSensor_.force[1][2];
			if (fz > min_fz){
			    return FORMAT2.format(currentSensor_.force[1][3]/currentSensor_.force[1][2]);
			}else{
			    return FORMAT2.format(0.0);
			}
		    default:
			return null;
		    }
		}
	};

}

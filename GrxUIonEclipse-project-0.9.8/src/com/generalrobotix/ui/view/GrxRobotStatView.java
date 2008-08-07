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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.go.aist.hrp.simulator.SensorState;
//import jp.go.aist.hrp.simulator.SensorType;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
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
    
    private Font plain12_;
    private Font bold12_;
    //private Font bold20_;

    private GrxWorldStateItem currentWorld_;
    private GrxModelItem currentModel_;
    private SensorState  currentSensor_;
    private double[]     currentRefAng_;
    private long[]       currentSvStat_;

    private List<LinkInfoLocal> jointList_ = new ArrayList<LinkInfoLocal>();
    private String[] forceName_;
    
    private Combo comboModelName_;
    private List<GrxModelItem> modelList_;
    
    private TableViewer[] viewers_;
    private TableViewer jointTV_;
    private TableViewer forceTV_;
    private TableViewer sensorTV_;
    
    //private Label lblInstruction1_;// = new Label("Select Model Item on ItemView");
    //private Label lblInstruction2_;
    
    private Color white_;
    private Color black_;
    private Color red_;
    private Color yellow_;
    
    private static final int COMBO_WIDTH = 100;

    public GrxRobotStatView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager,vp,parent);
        white_ = parent.getDisplay().getSystemColor(SWT.COLOR_WHITE);
        black_ = parent.getDisplay().getSystemColor(SWT.COLOR_BLACK);
        red_ = parent.getDisplay().getSystemColor(SWT.COLOR_RED);
        yellow_ = parent.getDisplay().getSystemColor(SWT.COLOR_YELLOW);
        
        FontRegistry registry = new FontRegistry();

        FontData[] data = parent.getFont().getFontData();
        //windowsだとテーブルのセルに収まらないのでフォントサイズはデフォルトにする
        //for(int i = 0; i < data.length; i++){
        //    data[i].setHeight(12);
        //}
        registry.put("plain12",data);
        plain12_ = registry.get("plain12");
        for(int i = 0; i < data.length; i++){
            //data[i].setHeight(12);
            data[i].setStyle(SWT.BOLD);
        }
        registry.put("bold12",data);
        bold12_ = registry.get("bold12");
        
        Composite mainPanel = new Composite(composite_, SWT.NONE);
        mainPanel.setLayout(new GridLayout(1,false));
        modelList_ = new ArrayList<GrxModelItem>();

        comboModelName_ = new Combo(mainPanel,SWT.READ_ONLY);
        GridData gridData = new GridData();
        gridData.widthHint = COMBO_WIDTH;
        comboModelName_.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        comboModelName_.addSelectionListener(new SelectionAdapter(){
            //選択が変更されたとき呼び出される
            public void widgetSelected(SelectionEvent e) {
                forceName_ = null;
                GrxModelItem item = modelList_.get(comboModelName_.getSelectionIndex());
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

        String[][] header = new String[][] {
                { "No", "Joint", "Angle", "Target", "Current", "PWR", "SRV", "Pgain", "Dgain" },
                { "Force", "Fx[N]", "Fy[N]", "Fz[N]", "Mx[Nm]", "My[Nm]", "Mz[Nm]" }, 
				{ "Sensor", "Xaxis", "Yaxis", "Zaxis" } };
        int[][] alignment = new int[][] {
                { SWT.LEFT, SWT.LEFT,  SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.CENTER, SWT.CENTER, SWT.RIGHT, SWT.RIGHT },
                { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT },
                { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT } };
        int[][] columnSize = new int[][] {
                { 18, 100, 60, 60, 50, 28, 28, 32, 32 },
                { 102, 51, 50, 51, 50, 51, 50 }, 
				{ 102, 101, 100, 100 } };
        
        jointTV_ = new TableViewer(mainPanel,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.FULL_SELECTION);
        forceTV_ = new TableViewer(mainPanel,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.FULL_SELECTION);    
        sensorTV_ = new TableViewer(mainPanel,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.FULL_SELECTION);    
        
        jointTV_.setContentProvider(new ArrayContentProvider());
        forceTV_.setContentProvider(new ArrayContentProvider());
        sensorTV_.setContentProvider(new ArrayContentProvider());
        
        jointTV_.setLabelProvider(new JointTableLabelProvider());
        forceTV_.setLabelProvider(new ForceTableLabelProvider());
        sensorTV_.setLabelProvider(new SensorTableLabelProvider());
        
        viewers_ = new TableViewer[]{jointTV_,forceTV_,sensorTV_};
        for(int i=0;i<viewers_.length;i++){
            TableLayout tableLayout = new TableLayout();
            for(int j=0;j<header[i].length;j++){
                TableColumn column = new TableColumn(viewers_[i].getTable(),j);
                column.setText(header[i][j]);
                column.setAlignment(alignment[i][j]);
                //column.setWidth(columnSize[i][j]);
                tableLayout.addColumnData(new ColumnWeightData(1,true));
            }
            viewers_[i].getTable().setLayout(tableLayout);
            viewers_[i].getTable().setHeaderVisible(true);
            viewers_[i].getTable().setLinesVisible(true);
            viewers_[i].getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        setScrollMinSize();
    }
    
    
    //TableViewer#setInputには
    //これらの_create○○TVInput()の戻り値を与えている。
    //具体的には表示したい行数分の長さを持ったInteger型配列
    //行番号を値としてもつ
    //LabelProviderはgetColumnText()内でこのInputを行番号として扱いカラムのテキストを設定する
    //（Swingからの移植を容易にするための処置）
    private Integer[] _createJointTVInput(){
        Integer[] input = new Integer[jointList_.size()];
        for(int i=0;i<input.length;i++){
            input[i] = i;
        }
        return input;
    }
    
    private Integer[] _createForceTVInput(){
        int length = 0;
        if (currentSensor_ != null && currentSensor_.force != null)
            length = currentSensor_.force.length;
        
        Integer[] input = new Integer[length];
        for(int i=0;i<input.length;i++){
            input[i] = i;
        }
        return input;
    }
    
    private Integer[] _createSensorTVInput(){
        int length = 0;
        if (currentSensor_ != null && currentSensor_.accel != null
                && currentSensor_.rateGyro != null)
            length = currentSensor_.accel.length + currentSensor_.rateGyro.length;
        
        Integer[] input = new Integer[length];
        for(int i=0;i<input.length;i++){
            input[i] = i;
        }
        return input;
    }

    public void restoreProperties() {
        super.restoreProperties();
        _resizeTables();
    }

    public void itemSelectionChanged(List<GrxBaseItem> itemList) {
        Iterator<GrxBaseItem> it = itemList.iterator();
        comboModelName_.removeAll();
        modelList_.clear();
        jointList_.clear();
        while (it.hasNext()) {
            GrxBaseItem item = it.next();
            if (item instanceof GrxWorldStateItem)
                currentWorld_ = (GrxWorldStateItem) item;
            else if (item instanceof GrxModelItem && ((GrxModelItem)item).isRobot()) {
                comboModelName_.add(item.getName());
                modelList_.add((GrxModelItem)item);
            }
        }
        
        if (comboModelName_.getItemCount() > 0) {
            forceName_ = null;
            comboModelName_.select(0);
            GrxModelItem item = modelList_.get(0);
            if (item == null || item == currentModel_)
                return;
            currentModel_ = item;
            LinkInfoLocal[] lInfo = currentModel_.lInfo_;
            for (int i = 0; i < lInfo.length; i++) {
                for (int j = 0; j < lInfo.length; j++) {
                    if (i == lInfo[j].jointId) {
                        jointList_.add(lInfo[j]);
                        break;
                    }
                }
            }
            if(comboModelName_.getItemCount() > 1)
                comboModelName_.setVisible(true);
            else//(comboModelName_.getItemCount() == 1)
                comboModelName_.setVisible(false);
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
                }
                
                if (forceName_ == null) {
                    forceName_ = currentModel_.getSensorNames("Force");
                    _resizeTables();
                }
            }
        }
        
        jointTV_.setInput(_createJointTVInput());
        forceTV_.setInput(_createForceTVInput());
        sensorTV_.setInput(_createSensorTVInput());
    }

    private void _resizeTables() {
//        for(int i=0;i<viewers_.length;i++){
//            viewers_[i].getTable().pack();
//        }
        

    }

    public boolean setup(List<GrxBaseItem> itemList) {
        currentWorld_ = null;
        currentModel_ = null;
        _resizeTables();
        return true;
    }

//    class MyCellRenderer extends JLabel implements TableCellRenderer {
//        private int[] columnAlignment_ = null;
//
//        public MyCellRenderer(int[] columnAlignment) {
//            super();
//            columnAlignment_ = columnAlignment;
//            setOpaque(true);
//            setBackground(Color.white);
//            setForeground(Color.black);
//            setFont(MONO_PLAIN_12);
//        }
//
//        public Component getTableCellRendererComponent(JTable table,
//                Object data, boolean isSelected, boolean hasFocus, int row,
//                int column) {
//            setHorizontalAlignment(columnAlignment_[column]);
//            if (data == null) {
//                setText("---");
//                setForeground(Color.black);
//                setBackground(Color.white);
//                setFont(MONO_PLAIN_12);
//            } else if (data instanceof String) {
//                setText((String) data);
//                setForeground(Color.black);
//                setBackground(Color.white);
//                setFont(MONO_PLAIN_12);
//            } else if (data instanceof CellState) {
//                CellState state = (CellState) data;
//                setText(state.value);
//                setForeground(state.fgColor);
//                setBackground(state.bgColor);
//                setFont(state.font);
//            }
//            return this;
//        }
//    }

//    class UpdatableTableModel extends DefaultTableModel {
//        public UpdatableTableModel(String[] columnName, int rowNum) {
//            super(columnName, rowNum);
//        }
//
//        public void updateCell(int row, int col) {
//            if (getValueAt(row, col) != null)
//                fireTableCellUpdated(row, col);
//        }
//
//        public void updateRow(int firstRow, int lastRow) {
//            fireTableRowsUpdated(firstRow, lastRow);
//        }
//
//        public void updateAll() {
//            updateRow(0, getRowCount());
//        }
//
//        public void updateAsNeeded() {
//            for (int i = 0; i < getRowCount(); i++) {
//                for (int j = 0; j < getColumnCount(); j++) {
//                    updateCell(i, j);
//                }
//            }
//        }
//    }

    class JointTableLabelProvider implements ITableLabelProvider,ITableColorProvider,ITableFontProvider {
        
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            int rowIndex = ((Integer)element).intValue();
            
            if (currentModel_ != null) {
                if (jointList_.get(rowIndex) == currentModel_.activeLinkInfo_) {
                    return "---";
                }
            }
            switch (columnIndex) {
                case 0:
                    return Integer.toString(rowIndex);
                case 1:
                    if (jointList_.size() <= 0)
                        break;
                    return jointList_.get(rowIndex).name;
                case 2:
                    if (jointList_.size() <= 0)
                        break;
                    return FORMAT1.format(Math.toDegrees(jointList_.get(rowIndex).jointValue));
                case 3:
                    if (currentRefAng_ == null)
                        break;
                    return FORMAT1.format(Math.toDegrees(currentRefAng_[rowIndex]));
                case 6:
                    if (currentSvStat_ == null)
                        break;
                    if (_isSwitchOn(rowIndex, currentSvStat_)) {
                        return "ON";
                    } else return "OFF";
                default:
                    break;
            }
            return "---";
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }

        public Color getBackground(Object element, int columnIndex) {
            int rowIndex = ((Integer)element).intValue();;
            
            if (currentModel_ != null) {
                if (jointList_.get(rowIndex) == currentModel_.activeLinkInfo_) {
                    return yellow_;
                }
            }
            return white_;
        }

        public Color getForeground(Object element, int columnIndex) {
            int rowIndex = ((Integer)element).intValue();
            
            switch (columnIndex) {
                case 0:
                    if (currentSvStat_ != null
                        && !_isSwitchOn(rowIndex, currentSvStat_)) {
                        return red_;
                    }
                case 1:
                case 2:
                    if (jointList_.size() <= 0)
                        break;
                    LinkInfoLocal info = jointList_.get(rowIndex);
                    if (info.llimit[0] < info.ulimit[0]
                        && (info.jointValue <= info.llimit[0] || info.ulimit[0] <= info.jointValue)) {
                        return red_;
                    }
                case 6:
                    if (currentSvStat_ == null)
                        break;
                    if (_isSwitchOn(rowIndex, currentSvStat_))
                        return red_;
                default:
                    break;
            }

            return black_;
        }

        public Font getFont(Object element, int columnIndex) {
            int rowIndex = ((Integer)element).intValue();
            
            switch (columnIndex) {
                case 0:
                    if (currentSvStat_ != null
                        && !_isSwitchOn(rowIndex, currentSvStat_)) {
                        return bold12_;
                    }
                case 2:
                    LinkInfoLocal info = jointList_.get(rowIndex);
                    if (info.llimit[0] < info.ulimit[0]
                        && (info.jointValue <= info.llimit[0] || info.ulimit[0] <= info.jointValue)) {
                        return bold12_;
                    }
               case 6:
                    if (currentSvStat_ == null)
                        break;
                    if (_isSwitchOn(rowIndex, currentSvStat_)) {
                        return bold12_;
                    }
                case 4:
                case 5:
                case 7:
                case 8:
                default:
                    break;
            }
            return plain12_;
        }

    }

    private boolean _isSwitchOn(int ch, long[] state) {
        long a = 1 << (ch % 64);
        if ((state[ch / 64] & a) > 0)
            return true;
        return false;
    }

    
    class ForceTableLabelProvider implements ITableLabelProvider,ITableColorProvider,ITableFontProvider{

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
        
        public String getColumnText(Object element, int columnIndex) {
            int rowIndex = ((Integer)element).intValue();
            if (columnIndex == 0) {
                if (forceName_ != null)
                    return forceName_[rowIndex];
            } else if (columnIndex < forceTV_.getTable().getColumnCount())
                return FORMAT2.format(currentSensor_.force[rowIndex][columnIndex - 1]);

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }

        public Color getBackground(Object element, int columnIndex) {
            return white_;
        }

        public Color getForeground(Object element, int columnIndex) {
            return black_;
        }

        public Font getFont(Object element, int columnIndex) {
            return plain12_;
        }
        
    }

    class SensorTableLabelProvider implements ITableLabelProvider,ITableColorProvider,ITableFontProvider{

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
        
        public String getColumnText(Object element, int columnIndex) {
            int rowIndex = ((Integer)element).intValue();
            
            if (currentSensor_.accel == null || currentSensor_.accel == null
                    || currentSensor_.rateGyro == null)
                return null;
            
            int numAccel = currentSensor_.accel.length;
            if (columnIndex == 0) {
                if (rowIndex < numAccel)
                    return "Acc_" + rowIndex + "[m/s^2]";
                else
                    return "Gyro_" + (rowIndex - numAccel) + "[rad/s]";
            } else {
                if (rowIndex < numAccel)
                    return FORMAT2.format(currentSensor_.accel[rowIndex][columnIndex - 1]);
                else
                    return FORMAT2.format(currentSensor_.rateGyro[rowIndex - numAccel][columnIndex - 1]);
            }
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
        }

        public Color getBackground(Object element, int columnIndex) {
            return white_;
        }

        public Color getForeground(Object element, int columnIndex) {
            return black_;
        }

        public Font getFont(Object element, int columnIndex) {
            return plain12_;
        }
        
    }
}

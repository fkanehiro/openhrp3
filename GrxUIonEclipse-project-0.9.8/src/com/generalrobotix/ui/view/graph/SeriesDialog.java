/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.view.graph;

//import java.awt.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.MessageBundle;


/**
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
@SuppressWarnings("serial")
public class SeriesDialog extends Dialog {

    private static final int BORDER_GAP = 12;
    //private static final int LABEL_GAP = 12;
    private static final int BUTTON_GAP = 5;
    //private static final int ITEM_GAP = 11;

    private boolean updated_;
    private ArrayList<DataItemInfo> dataItemInfoList_; 
    private ArrayList<DataItemInfo> removedList_;
    private DataItemInfo[] dataItemInfoArray_;
    private DataItemInfo[] removedArray_;
    
    private ArrayList<DataItemInfo> addedList_;
    private DataItemInfo[] addedArray_;
    
    private JTable seriesTable_ = new JTable();
    private static final String ROBOT_MODEL = "ROBOT MODEL";
    private static final String DATA_TYPE   = "DATA TYPE";
    private static final String LINK_NAME   = "LINK NAME";
    private static final String ATTRIBUTE   = "ATTRIBUTE";
    private static final String colNode = MessageBundle.get("dialog.graph.series.table.node");
    private static final String colAttribute = MessageBundle.get("dialog.graph.series.table.attribute");
    private static final String colIndex = MessageBundle.get("dialog.graph.series.table.index");
    private static final String colColor = MessageBundle.get("dialog.graph.series.table.color");
    private static final String colLegend = MessageBundle.get("dialog.graph.series.table.legend");
    public static final Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
    public static final Color YELLOW = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
    public static final Color CYAN = Display.getDefault().getSystemColor(SWT.COLOR_CYAN);
    public static final Color MAGENTA = Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA);
    public static final Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    public static final Color BLUE = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
    private Combo comboModel_ ;
    private Combo comboType_ ;
    private Combo comboLink_ ;
    private Combo comboAttr_ ;
    private Button setButton_;
	private Button removeButton_;
		
    private static final String GRAPH_PROPERTIES = "/resources/graph.properties";
    private URL url = this.getClass().getResource(GRAPH_PROPERTIES);
    private Properties prop = new Properties();
    private GraphElement currentGraph_;
    
    private MyModel tableModel_;
	private final Map<String, ArrayList<String>> nodeMap =  new HashMap<String, ArrayList<String>>();
	//private boolean isComboChanging_ = true;
	private int graphIndex = 0;
    private List<GrxModelItem> currentModels_ = null;
    // -----------------------------------------------------------------
    public SeriesDialog(GraphElement initialGraph, Shell shell) {
    	super(shell);
        currentGraph_ = initialGraph;
        
        tableModel_ = new MyModel(
        		new Object[]{
        			colNode,
                    colAttribute,
                    colIndex,
                    colColor,
                    colLegend,
                }
        );
        
        try {
        	prop.load(url.openStream());
        } catch (java.io.IOException e) {
        	e.printStackTrace();
        }
        
//  	 this is temporary limit for data type
        List<String> typeList = new ArrayList<String>();
        typeList.add("Joint");
        typeList.add("ForceSensor");
        typeList.add("Gyro");
        typeList.add("AccelerationSensor");
        
        Iterator<Object> it = prop.keySet().iterator();
        while (it.hasNext()) {
        	String key = (String) it.next();
        	String[] property = key.split("[.]");
        	if (property.length > 2 && property[2].equals("dataKind") && typeList.contains(property[0])) {
        		if (!nodeMap.containsKey(property[0]))
        			nodeMap.put(property[0], new ArrayList<String>());
        		nodeMap.get(property[0]).add(property[1]);
        	}
        }      
    }
    
    protected void configureShell(Shell newShell) {   
        super.configureShell(newShell);
        newShell.setText(MessageBundle.get("dialog.graph.series.title"));
    }
    
    protected Control createDialogArea(Composite parent) {
    	Composite composite = (Composite)super.createDialogArea(parent);
    	composite.setLayout(new RowLayout(SWT.VERTICAL));
    	Composite comp = new Composite(composite, SWT.EMBEDDED);
    	RowData rowdata = new RowData();
    	rowdata.height = 300;
    	rowdata.width = 600;
    	comp.setLayoutData(rowdata);
    	//comp.setLayout( new GridLayout(1,true));
    	Frame frame = SWT_AWT.new_Frame( comp );
    	
    	JPanel line1 = new JPanel();
    	line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
    	line1.add(Box.createHorizontalStrut(BORDER_GAP));
    	line1.add(new JLabel(MessageBundle.get("dialog.graph.series.dataseries")));
    	line1.setAlignmentX(Component.LEFT_ALIGNMENT);
    	Dimension dimension = line1.getPreferredSize();
    	line1.setMaximumSize(dimension);
             
        DefaultComboBoxModel model = new DefaultComboBoxModel(new java.awt.Color[]{
          		java.awt.Color.green, java.awt.Color.yellow, java.awt.Color.cyan, 
          		java.awt.Color.magenta, java.awt.Color.red,  java.awt.Color.blue,
            });
        JComboBox colorCombo = new JComboBox(model);
        colorCombo.setRenderer(new ColorCellRenderer());
        DefaultTableCellRenderer colorRenderer = new DefaultTableCellRenderer() {
        	private static final int X_GAP = 10;
        	private java.awt.Color color_;

        	public void setValue(Object value) {
        		if (value instanceof Color) {
        			Color color = (Color)value;
        			if(color.getRGB().equals(GREEN.getRGB()))
        				color_ = java.awt.Color.green;
        			if(color.getRGB().equals(YELLOW.getRGB()))
        				color_ = java.awt.Color.yellow;
        			if(color.getRGB().equals(CYAN.getRGB()))
        				color_ = java.awt.Color.cyan;
        			if(color.getRGB().equals(MAGENTA.getRGB()))
        				color_ = java.awt.Color.magenta;
        			if(color.getRGB().equals(RED.getRGB()))
        				color_ = java.awt.Color.red;
        			if(color.getRGB().equals(BLUE.getRGB()))
        				color_ = java.awt.Color.blue;
        		} else {
        			super.setValue(value);
                }
        	}

        	public void paint(Graphics g) {
        		int width = getSize().width;
        		int height = getSize().height;
        		g.setColor(java.awt.Color.black);
        		g.fillRect(0, 0, width, height);
        		g.setColor(color_);
        		g.drawLine(X_GAP, height / 2, width - X_GAP, height / 2);
        	}
        };

        DefaultTableCellRenderer indexRenderer = new DefaultTableCellRenderer() {
        	public void setValue(Object value) {
        		if (value instanceof Integer) {
        			int ind = ((Integer)value).intValue();
        			setText(ind < 0 ? "":""+ind);
        		} else {
        			super.setValue(value);
                }
        	}
        };

        
        seriesTable_.setModel(tableModel_);
        seriesTable_.getTableHeader().setReorderingAllowed(false);
        //seriesTable_.setColumnSelectionAllowed(false);
        seriesTable_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        seriesTable_.setRowHeight(20);
        seriesTable_.getColumn(colNode).setPreferredWidth(100);
        seriesTable_.getColumn(colAttribute).setPreferredWidth(80);
        seriesTable_.getColumn(colLegend).setPreferredWidth(200);
        TableColumn indexColumn = seriesTable_.getColumn(colIndex);
        indexRenderer.setHorizontalAlignment(JLabel.RIGHT);
        indexColumn.setCellRenderer(indexRenderer);
        indexColumn.setPreferredWidth(10);
        TableColumn colorColumn = seriesTable_.getColumn(colColor);
        colorColumn.setCellEditor(new DefaultCellEditor(colorCombo));
        colorRenderer.setHorizontalAlignment(JLabel.CENTER);
        colorColumn.setCellRenderer(colorRenderer);
        
        JScrollPane tablePane = new JScrollPane(seriesTable_);
        tablePane.setPreferredSize(new Dimension(600, 160));
        JPanel line2 = new JPanel();
        line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.add(tablePane);
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);
         
        Container pane = new Container();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(Box.createVerticalStrut(BORDER_GAP));
        pane.add(line1);
        pane.add(Box.createVerticalStrut(BUTTON_GAP));
        pane.add(line2);
        pane.add(Box.createVerticalStrut(BUTTON_GAP));
        frame.add(pane);
            
        Composite line3 = new Composite(composite, SWT.NONE);
        line3.setLayout(new RowLayout());
        Group group0 = new Group(line3, SWT.NONE);
        group0.setText(ROBOT_MODEL);
        group0.setLayout(new FillLayout());
        comboModel_ = new Combo(group0,SWT.READ_ONLY);
        comboModel_.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}
			public void widgetSelected(SelectionEvent e) {
				comboType_.removeAll();
				comboLink_.removeAll();
				comboAttr_.removeAll();
				
				Iterator<String> it = nodeMap.keySet().iterator();
				if (seriesTable_.getRowCount() > 0) {
					String curAttr = currentGraph_.getTrendGraph().getDataItemInfoList()[0].dataItem.attribute;
					while (it.hasNext()) {
						String key = it.next();
						if (nodeMap.get(key).contains(curAttr))
							comboType_.add(key);
					}
				} else {
					while (it.hasNext()) {
						comboType_.add(it.next());
                  	}
				}
				comboType_.setEnabled(true);
				comboLink_.setEnabled(false);
				comboAttr_.setEnabled(false);
				setButton_.setEnabled(false);
				
				
				
				if(comboType_.getItemCount()>0){
					comboType_.select(0);
					comboType_.notifyListeners(SWT.Selection, null);
				}
				
			}
		});     
        Group group1 = new Group(line3, SWT.NONE);
        group1.setText(DATA_TYPE);
        group1.setLayout(new FillLayout());
        comboType_ = new Combo(group1,SWT.READ_ONLY);
        comboType_.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}
			public void widgetSelected(SelectionEvent e) {
				
				comboLink_.removeAll();
				comboAttr_.removeAll();
				
				String type = comboType_.getItem(comboType_.getSelectionIndex());
				String modelName = comboModel_.getItem(comboModel_.getSelectionIndex());
				Iterator<GrxModelItem> it = currentModels_.iterator();
				GrxModelItem model=null;
				while (it.hasNext()) {
					model = it.next();
					if(model.getName().equals(modelName))
						break;
				}
				if (type.equals("Joint")) {
					Vector<GrxLinkItem> li = model.links_;
					for (int i = 0; i < li.size(); i++) 
						if(li.get(i).jointType().equals("rotate"))
							comboLink_.add(li.get(i).getName());
				} else {
					String t = null;
					if (type.equals("ForceSensor"))
						t = "Force";
					else if (type.equals("Gyro"))
						t = "RateGyro";
					else if (type.equals("AccelerationSensor"))
						t = "Acceleration";
					String[] snames = model.getSensorNames(t);

					if (snames != null) {
						for (int i=0; i<snames.length; i++) {
							comboLink_.add(snames[i]);
						}
					}
				}
				comboLink_.setEnabled(true);
				comboAttr_.setEnabled(false);
				setButton_.setEnabled(false);
				
							
				if(comboLink_.getItemCount()>0){
					comboLink_.select(0);
					comboLink_.notifyListeners(SWT.Selection, null);
				}
			}
		});
        Group group2 = new Group(line3, SWT.NONE);
        group2.setText(LINK_NAME);
        group2.setLayout(new FillLayout());
        comboLink_ = new Combo(group2,SWT.READ_ONLY);
        comboLink_.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}

			public void widgetSelected(SelectionEvent e) {
				
   				comboAttr_.removeAll();
   				
				List<String> l = nodeMap.get(comboType_.getItem(comboType_.getSelectionIndex()));
					/*if (currentGraph_.getTrendGraph().getDataItemInfoList().length > 0) {
						String curAttr = currentGraph_.getTrendGraph().getDataItemInfoList()[0].dataItem.attribute;
						if (l.contains(curAttr))
							comboAttr_.addItem(curAttr);
					} else */
				if (seriesTable_.getRowCount() > 0) {
					String curAttr = (String)seriesTable_.getValueAt(0, 1);
					if (l.contains(curAttr))
						comboAttr_.add(curAttr);
				} else {
					Iterator<String> it = l.iterator();
					while (it.hasNext()) {
						comboAttr_.add(it.next());
                 	}
				}
				comboAttr_.setEnabled(true);
				
				setButton_.setEnabled(comboAttr_.getItemCount() > 0);
				
							
				if(comboAttr_.getItemCount()>0)
					comboAttr_.select(0);
			} 
		});
        Group group3 = new Group(line3, SWT.NONE);
        group3.setText(ATTRIBUTE);
        group3.setLayout(new FillLayout());
        comboAttr_ = new Combo(group3,SWT.READ_ONLY);
        removeButton_ = new Button(line3,SWT.PUSH);
        removeButton_.setText(MessageBundle.get("dialog.graph.series.remove"));
        removeButton_.addSelectionListener(new SelectionListener(){
        	public void widgetDefaultSelected(SelectionEvent e) {
        		// TODO 自動生成されたメソッド・スタブ
        	}
        	
        	public void widgetSelected(SelectionEvent e) {
        		int ind = seriesTable_.getSelectedRow();
        		if (ind < 0) 
        			return;
        		
        		CellEditor ce = seriesTable_.getCellEditor();
        		if (ce != null) 
        			ce.stopCellEditing();
        		
        		tableModel_.removeRow(ind);
        		int cnt = tableModel_.getRowCount();
        		if (cnt < 1) {
        			_resetSelection();
        			return;
        		}
        		if (ind >= cnt) {
        			ind -= 1;
        		}
        		seriesTable_.setRowSelectionInterval(ind, ind);
        	}
        });
        removeButton_.setEnabled(true); 
        setButton_ = new Button(line3,SWT.PUSH);
        setButton_.setText(MessageBundle.get("dialog.graph.series.set"));
        setButton_.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}

			public void widgetSelected(SelectionEvent e) {
				int length = 1;
				String combo1 = (String)comboType_.getItem(comboType_.getSelectionIndex());
				if (combo1.equals("ForceSensor") ||
					combo1.equals("AccelerationSensor") ||
					combo1.equals("Gyro")) 
					length = 3;
				for (int i=0; i<length; i++) {
    					Object[] rowData = new Object[5];
    	                rowData[4] = comboModel_.getItem(comboModel_.getSelectionIndex()) + "." 
    			                   + (String)comboLink_.getItem(comboLink_.getSelectionIndex()) + "."
    			                   + (String)comboAttr_.getItem(comboAttr_.getSelectionIndex()) + (length > 1 ? "." + i : "");
    	                if(tableModel_.getRow((String)rowData[4])==-1){
    	                   	rowData[0] = comboModel_.getItem(comboModel_.getSelectionIndex()) + "." 
    				           + (String)comboLink_.getItem(comboLink_.getSelectionIndex());
    	                	rowData[1] = (String)comboAttr_.getItem(comboAttr_.getSelectionIndex());
    	                	if(length>1)
    	                		rowData[2] = new Integer(i);
    	                	else
    	                		rowData[2] = null;
    	                	rowData[3] = currentGraph_.getTrendGraph().getGraphColor(graphIndex);
    	                	tableModel_.addRow(rowData,(String)rowData[4]);
    	                	graphIndex++;
    	                }
				}
				removeButton_.setEnabled(true);
			}
        });
       	_resetSelection();
       	/*
       	comp.addKeyListener(new KeyListener(){
       		public void keyPressed(KeyEvent e) {
				if(e.keyCode==SWT.ESC){
					 CellEditor ce = seriesTable_.getCellEditor();
					 if (ce != null) {
						 ce.stopCellEditing();
					 }
					 removeAllRows();
				}
			}
			public void keyReleased(KeyEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}
       		
       	});
		*/
        return composite;
    }

    protected void buttonPressed(int buttonId) {
    	if (buttonId == IDialogConstants.OK_ID) {
    		CellEditor ce = seriesTable_.getCellEditor();
            if (ce != null) {
                ce.stopCellEditing();
            }
            ArrayList<DataItemInfo> newDataItemInfoList = new ArrayList<DataItemInfo>();
            for(int i=0; i<tableModel_.getRowCount(); i++){
            	String fullName = tableModel_.getString(i);
            	Iterator<DataItemInfo> it = dataItemInfoList_.iterator();
            	boolean contain = false;
                while(it.hasNext()){
                	DataItemInfo info = it.next();
                	if(fullName.equals(info.dataItem.toString())){
                		info.color = (Color)tableModel_.getValueAt(i, 3);
            			info.legend = (String)tableModel_.getValueAt(i, 4);
            			newDataItemInfoList.add(info);
            			dataItemInfoList_.remove(info);
            			contain = true;
            			break;
                	}
                }
                if(!contain){
                	String str = (String)tableModel_.getValueAt(i, 0);
                	String[] model = str.split("\\.",0);
                	Integer indexI = ((Integer)tableModel_.getValueAt(i, 2));
                	int index;
                	if(indexI==null)
                		index=-1;
                	else
                		index=indexI.intValue();
                	addedList_.add(new DataItemInfo(new DataItem(
					    	model[0],model[1],
					    	(String)tableModel_.getValueAt(i, 1),
					    	index,
					    	(String)comboType_.getText()),
					    	(Color)tableModel_.getValueAt(i, 3),
					    	(String)tableModel_.getValueAt(i, 4)
					    
                	));
                }
            }
            Iterator<DataItemInfo> it = dataItemInfoList_.iterator();
            while(it.hasNext()){
            	removedList_.add(it.next());
            }
            dataItemInfoArray_ =
            	newDataItemInfoList.toArray(new DataItemInfo[0]);
        	removedArray_ = removedList_.toArray(new DataItemInfo[0]);
        	removeAllRows();
            addedArray_ = addedList_.toArray(new DataItemInfo[0]);
        	updated_ = true;
    	}else if(buttonId == IDialogConstants.CANCEL_ID){
    		CellEditor ce = seriesTable_.getCellEditor();
            if (ce != null) {
                ce.stopCellEditing();
            }
            removeAllRows();
    	}
    	setReturnCode(buttonId);
    	close();
        super.buttonPressed(buttonId);
    }
    
    private void _resetSelection() {
		graphIndex = 0;
	
		
		comboModel_.removeAll();
		Iterator<GrxModelItem> it = currentModels_.iterator();
		while (it.hasNext()) {
			GrxModelItem model = it.next();
			if (model.isRobot())
				comboModel_.add(model.getName());
		}
		comboType_.removeAll();
		comboLink_.removeAll();
		comboAttr_.removeAll();
		
		comboModel_.setEnabled(true);
		comboType_.setEnabled(false);
		comboLink_.setEnabled(false);
		comboAttr_.setEnabled(false);
		setButton_.setEnabled(false);
		
		
		if(comboModel_.getItemCount()>0){
			comboModel_.select(0);
			comboModel_.notifyListeners(SWT.Selection, null);
		}
    }

    public void setCurrentGraph(GraphElement currentGraph) {
        currentGraph_ = currentGraph;
    }

    
    /**
     *
     * @param   visible 
     */

    public int open( ) {
     	if(seriesTable_.getRowCount() > 0)
    		seriesTable_.setRowSelectionInterval(0, 0); 
    	removedList_ = new ArrayList<DataItemInfo>(); 
    	addedList_ = new ArrayList<DataItemInfo>();
    	updated_ = false;     
        return super.open(); 
    }


    /**
     *
     */
    public void setDataItemInfoList( DataItemInfo[] dii) {
        dataItemInfoList_ = new ArrayList<DataItemInfo>();
        Object[] rowData = new Object[5];
        for (int i = 0; i < dii.length; i++) {
            dataItemInfoList_.add(dii[i]);
            DataItem di = dii[i].dataItem;
            if (di.object == null) {
                rowData[0] = di.node;
            } else {
                rowData[0] = di.object + "." + di.node;
            }
            rowData[1] = di.attribute;
            rowData[2] = new Integer(di.index);
            rowData[3] = dii[i].color;
            rowData[4] = dii[i].legend;
            tableModel_.addRow(rowData,di.toString());
        }
    }

    /**
     *
     */
    public DataItemInfo[] getDataItemInfoList() {
        return dataItemInfoArray_;
    }

    /**
     *
     */
    public DataItemInfo[] getRemovedList() {
        return removedArray_;
    }
    public DataItemInfo[] getAddedList() {
        return addedArray_;
    }
    /**
     *
     */
    public boolean isUpdated() {
        return updated_;
    }

    /**
     *
     */
    private void removeAllRows() {
        int cnt = tableModel_.getRowCount();
        for (int i = 0; i < cnt; i++) {
            tableModel_.removeRow(0);
        }
    }

    // -----------------------------------------------------------------
    /**
     * @author Kernel Inc.
     * @version 1.0 (2001/8/20)
     */
    private class ColorCellRenderer extends JPanel implements ListCellRenderer{

        private static final int X_GAP = 10;
        private final java.awt.Color selectedColor_ = new java.awt.Color(153, 153, 204);

        private boolean selected_;
        private java.awt.Color color_;

        // -----------------------------------------------------------------
        /**
         *
         */
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            color_ = (java.awt.Color)value;
            selected_ = isSelected;
            setPreferredSize(new Dimension(40, 16));

            return this;
        }

        // -----------------------------------------------------------------
        /**
         *
         */
        public void paint(Graphics g) {
            int width = getSize().width;
            int height = getSize().height;
            if (selected_) {
                g.setColor(selectedColor_);
                g.fillRect(0, 0, width, height);
                g.setColor(java.awt.Color.black);
                g.fillRect(2, 2, width - 4, height - 4);
            } else {
                g.setColor(java.awt.Color.black);
                g.fillRect(0, 0, width, height);
            }
            g.setColor(color_);
            g.drawLine(X_GAP, height / 2, width - X_GAP, height / 2);
        }
    }

    /**
     *
     * @author Kernel Inc.
     * @version 1.0 (2001/8/20)
     */
    private class MyModel extends DefaultTableModel {
    	private ArrayList<String> tableList = new ArrayList<String>();
        public MyModel(
            Object[] columnNames
        ) {
            super(columnNames, 0);
        }

        public boolean isCellEditable(int row, int col) {
            return (col == 3 || col == 4);
        }
        
        public int getRow(String str){
        	return tableList.indexOf(str);
        }
        
        public String getString(int i){
        	return tableList.get(i);
        }
        
        public void removeRow(int i){
        	super.removeRow(i);
        	tableList.remove(i);
        }
        
        public void addRow(Object[] rowData, String str){
        	super.addRow(rowData);
        	tableList.add(str);
        }
    }
    
    public void setModelList(List<GrxModelItem> list){
    	currentModels_ = list;
    }
    
}

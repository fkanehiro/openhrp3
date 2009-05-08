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
import java.net.URL;
import java.util.*;


import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColorCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

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
	private boolean updated_;
    private ArrayList<DataItemInfo> dataItemInfoList_; 
    private ArrayList<DataItemInfo> removedList_;
    private DataItemInfo[] dataItemInfoArray_;
    private DataItemInfo[] removedArray_;
    
    private ArrayList<DataItemInfo> addedList_;
    private DataItemInfo[] addedArray_;
    
   // private JTable seriesTable_ = new JTable();
    private TableViewer tableviewer_;
    private static final String ROBOT_MODEL = "ROBOT MODEL";
    private static final String DATA_TYPE   = "DATA TYPE";
    private static final String LINK_NAME   = "LINK NAME";
    private static final String ATTRIBUTE   = "ATTRIBUTE";
    private static final String colNode = MessageBundle.get("dialog.graph.series.table.node");
    private static final String colAttribute = MessageBundle.get("dialog.graph.series.table.attribute");
    private static final String colIndex = MessageBundle.get("dialog.graph.series.table.index");
    private static final String colColor = MessageBundle.get("dialog.graph.series.table.color");
    private static final String colLegend = MessageBundle.get("dialog.graph.series.table.legend");
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
    
    private MyTable tableModel_;
	private final Map<String, ArrayList<String>> nodeMap =  new HashMap<String, ArrayList<String>>();
	private int graphIndex = 0;
    private List<GrxModelItem> currentModels_ = null;
    // -----------------------------------------------------------------
    public SeriesDialog(GraphElement initialGraph, Shell shell) {
    	super(shell);
        currentGraph_ = initialGraph;
        
        tableModel_ = new MyTable();
        
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
    	RowData rowdata = new RowData();
    	rowdata.height = 100;
    	rowdata.width = 600;
    	
    	Label label = new Label(composite, SWT.LEFT);
    	label.setText(MessageBundle.get("dialog.graph.series.dataseries"));
    	tableviewer_ = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER);
    	tableviewer_.getControl().setLayoutData(rowdata);
    	Table table = tableviewer_.getTable();
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        TableColumn column = new TableColumn(table,SWT.NONE);
        column.setText(colNode);
        column.setWidth(100);
        column = new TableColumn(table,SWT.NONE);
        column.setText(colAttribute);
        column.setWidth(100);
        column = new TableColumn(table,SWT.NONE);
        column.setText(colIndex);
        column.setWidth(60);
        column = new TableColumn(table,SWT.NONE);
        column.setText(colColor);
        column.setWidth(60);
        column = new TableColumn(table,SWT.NONE);
        column.setText(colLegend);
        column.setWidth(280);
        
        String[] properties = new String[]{ null, null, null, "color", "legend"};
        tableviewer_.setColumnProperties(properties); 
        CellEditor[] editors = new CellEditor[]{ null, null, null, 
                new ColorCellEditor(table),
                new TextCellEditor(table)  };
        tableviewer_.setCellEditors(editors);
        tableviewer_.setContentProvider(new ArrayContentProvider());
        tableviewer_.setLabelProvider(new MyLabelProvider());
        tableviewer_.setCellModifier(new MyCellModifier(tableviewer_));
        
      	tableviewer_.setInput(tableModel_.getList());
       
        Composite line3 = new Composite(composite, SWT.NONE);
        line3.setLayout(new RowLayout());
        Group group0 = new Group(line3, SWT.NONE);
        group0.setText(ROBOT_MODEL);
        group0.setLayout(new FillLayout());
        comboModel_ = new Combo(group0,SWT.READ_ONLY);
        comboModel_.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {				
			}
			public void widgetSelected(SelectionEvent e) {
				comboType_.removeAll();
				comboLink_.removeAll();
				comboAttr_.removeAll();
				
				Iterator<String> it = nodeMap.keySet().iterator();
				if (tableModel_.getRowCount() > 0) {
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
			}

			public void widgetSelected(SelectionEvent e) {
				
   				comboAttr_.removeAll();
   				
				List<String> l = nodeMap.get(comboType_.getItem(comboType_.getSelectionIndex()));
					/*if (currentGraph_.getTrendGraph().getDataItemInfoList().length > 0) {
						String curAttr = currentGraph_.getTrendGraph().getDataItemInfoList()[0].dataItem.attribute;
						if (l.contains(curAttr))
							comboAttr_.addItem(curAttr);
					} else */
				if (tableModel_.getRowCount() > 0) {
					String curAttr = (String)tableModel_.getValueAt(0, 1);
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
        	}
        	
        	public void widgetSelected(SelectionEvent e) {
        		int ind = tableviewer_.getTable().getSelectionIndex();
        		if (ind < 0) 
        			return;
        		
        		tableModel_.removeRow(ind);
        		tableviewer_.refresh();
        		int cnt = tableModel_.getRowCount();
        		if (cnt < 1) {
        			_resetSelection();
        			return;
        		}
        		if (ind >= cnt) {
        			ind -= 1;
        		}
        		tableviewer_.getTable().select(ind);
        	}
        });
        removeButton_.setEnabled(true); 
        setButton_ = new Button(line3,SWT.PUSH);
        setButton_.setText(MessageBundle.get("dialog.graph.series.set"));
        setButton_.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				int length = 1;
				String combo1 = (String)comboType_.getItem(comboType_.getSelectionIndex());
				if (combo1.equals("ForceSensor") ||
					combo1.equals("AccelerationSensor") ||
					combo1.equals("Gyro")) 
					length = 3;
				for (int i=0; i<length; i++) {
					String model = comboModel_.getItem(comboModel_.getSelectionIndex());
					String link = comboLink_.getItem(comboLink_.getSelectionIndex());
					String attr = comboAttr_.getItem(comboAttr_.getSelectionIndex());
					String legend = model + "." + link + "." + attr + (length > 1 ? "." + i : "");
					if(!tableModel_.contains(legend)){
						DataItemInfo newDataItemInfo = new DataItemInfo(new DataItem(model,link,attr,
							length > 1 ? i : -1,
					    	comboType_.getItem(comboType_.getSelectionIndex())),
					    	currentGraph_.getTrendGraph().getGraphColor(graphIndex),
					    	legend);
						tableModel_.addRow(newDataItemInfo, legend);
						tableviewer_.refresh();
						graphIndex++;
					}
				}
				removeButton_.setEnabled(true);
        		comboLink_.notifyListeners(SWT.Selection, null);
			}
        });
       	_resetSelection();
        return composite;
    }

    protected void buttonPressed(int buttonId) {
    	if (buttonId == IDialogConstants.OK_ID) {
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
                	addedList_.add(tableModel_.getItem(i));
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
        for (int i = 0; i < dii.length; i++) {
            dataItemInfoList_.add(dii[i]);
            tableModel_.addRow(dii[i],dii[i].dataItem.toString());
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

    private class MyTable {
    	private ArrayList<String> nameList = new ArrayList<String>();
    	private ArrayList<DataItemInfo> itemList = new ArrayList<DataItemInfo>(); 
    	
    	public void addRow(DataItemInfo item, String name){
    		itemList.add(item);
    		nameList.add(name);
    	}
    	
    	public void removeRow(int i){
    		itemList.remove(i);
    		nameList.remove(i);
    	}
    	
    	public DataItemInfo getItem(int i){
    		return itemList.get(i);
    	}
    	
    	public int getRowCount(){
    		return itemList.size();
    	}
    	
    	public String getString(int i){
    		return nameList.get(i);
    	}
    	
    	public ArrayList getList(){
    		return itemList;
    	}
    
        public boolean contains(String name){
            return nameList.contains(name);
        }
        
        public Object getValueAt(int i, int j){
        	DataItemInfo dii = itemList.get(i);
    		DataItem di = dii.dataItem;
    		switch (j) {
    		    case 0:
    		    	 if (di.object == null) {
    		    		 return di.node;
    		    	 }else{
    		    		 return di.object + "." + di.node;
    		    	 }
    		    case 1:
    		    	return di.attribute;
    		    case 2:
    		    	return new Integer(di.index);
    		    case 3:
    		    	return dii.color;
    		    case 4:
    		    	return dii.legend;
    		    default:
    		    	break;
    		}
    		return null;
        }
    }
    
    public void setModelList(List<GrxModelItem> list){
    	currentModels_ = list;
    }
    
    public class MyLabelProvider extends LabelProvider 
    implements ITableLabelProvider, ITableColorProvider { 

    	public Image getColumnImage(Object element, int columnIndex) {
    		return null;
    	}

    	public String getColumnText(Object element, int columnIndex) {
    		DataItemInfo item = (DataItemInfo) element;
    		String result = "";
    		DataItem di = item.dataItem;
    		switch (columnIndex) {
    		    case 0:
    		    	 if (di.object == null) {
    		    		 result = di.node;
    		    	 }else{
    		    		 result = di.object + "." + di.node;
    		    	 }
    		    	 break;
    		    case 1:
    		    	result = di.attribute;
    		    	break;
    		    case 2:
    		    	if(di.index < 0)
    		    		result = "";
    		    	else
    		    		result = (new Integer(di.index)).toString();
    		    	break;
    		    case 3:
    		    	result = "";
    		    	break;
    		    case 4:
    		    	result = item.legend;
    		    	break;
    		    default:
    		    	break;
    		}
    		return result;
    	}

		public Color getBackground(Object element, int columnIndex) {
			DataItemInfo item = (DataItemInfo) element;
    		if(columnIndex == 3)
    			return item.color;
    		else
    			return null;
		}

		public Color getForeground(Object element, int columnIndex) {
			return null;
		}
    }
    
    public class MyCellModifier implements ICellModifier {
    	  private TableViewer viewer_;

    	  public MyCellModifier(TableViewer viewer) {
    	    this.viewer_ = viewer;
    	  }

		public boolean canModify(Object element, String property) {
			if(property == "color" || property == "legend")
				return true;
			else
				return false;
		}

		public Object getValue(Object element, String property) {
			DataItemInfo item = (DataItemInfo) element;
			if(property == "color")
				return item.color.getRGB();
			else if(property == "legend")
				return item.legend;
			return null;
		}

		public void modify(Object element, String property, Object value) {
			if (element instanceof Item) {
			      element = ((Item) element).getData();
			}
			DataItemInfo item = (DataItemInfo) element;
			if(property == "color")
				item.color = new Color(Display.getDefault(),((RGB)value));
			else if(property == "legend")
				item.legend = (String)value;
				
			 viewer_.update(element, null);
		}
    }
}

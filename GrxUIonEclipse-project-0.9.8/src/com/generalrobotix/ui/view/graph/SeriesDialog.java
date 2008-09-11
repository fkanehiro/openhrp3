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

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.MessageBundle;


/**
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
@SuppressWarnings("serial")
public class SeriesDialog extends JDialog {

    private static final int BORDER_GAP = 12;
    //private static final int LABEL_GAP = 12;
    private static final int BUTTON_GAP = 5;
    //private static final int ITEM_GAP = 11;
    private static final int CONTENTS_GAP = 17;

    private boolean updated_;
    private ArrayList<DataItemInfo> dataItemInfoList_; 
    private ArrayList<DataItemInfo> removedList_;
    private DataItemInfo[] dataItemInfoArray_;
    private DataItemInfo[] removedArray_;
    
    private ArrayList<AttributeInfo> addedList_;
    private AttributeInfo[] addedArray_;
    
    private JTable seriesTable_;
    private static final String ROBOT_MODEL = "ROBOT MODEL";
    private static final String DATA_TYPE   = "DATA TYPE";
    private static final String LINK_NAME   = "LINK NAME";
    private static final String ATTRIBUTE   = "ATTRIBUTE";
    private JComboBox comboModel_ = new JComboBox();
    private JComboBox comboType_ = new JComboBox();
    private JComboBox comboLink_ = new JComboBox();
    private JComboBox comboAttr_ = new JComboBox();
    private JButton setButton_ = new JButton(MessageBundle.get("dialog.graph.series.set"));
	private JButton removeButton_ = new JButton(MessageBundle.get("dialog.graph.series.remove"));
		
    private static final String GRAPH_PROPERTIES = "/resources/graph.properties";
    private URL url = this.getClass().getResource(GRAPH_PROPERTIES);
    private Properties prop = new Properties();
    private GraphElement currentGraph_;
    
    private MyModel tableModel_;
    private GrxPluginManager manager_;
	private final Map<String, ArrayList<String>> nodeMap =  new HashMap<String, ArrayList<String>>();
	private boolean isComboChanging_ = true;
	private int graphIndex = 0;
    // -----------------------------------------------------------------

    //public SeriesDialog(GrxPluginManager manager,GraphElement initialGraph,Composite parent) {
    public SeriesDialog(GrxPluginManager manager,GraphElement initialGraph, Frame frame) {
        //super(SWT_AWT.new_Frame(new Composite(parent,SWT.EMBEDDED)), MessageBundle.get("dialog.graph.series.title"), true);
    	super( frame, MessageBundle.get("dialog.graph.series.title"), true);
        manager_ = manager;
        currentGraph_ = initialGraph;
        
        JPanel line1 = new JPanel();
        line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.add(new JLabel(MessageBundle.get("dialog.graph.series.dataseries")));
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        DefaultComboBoxModel model = new DefaultComboBoxModel(new Color[]{
      		Color.green, Color.yellow, Color.pink, Color.cyan, Color.magenta, Color.red, Color.orange, Color.blue,
        });
        JComboBox colorCombo = new JComboBox(model);
        colorCombo.setRenderer(new ColorCellRenderer());
        DefaultTableCellRenderer colorRenderer = new DefaultTableCellRenderer() {
            private static final int X_GAP = 10;
            private Color color_;

            public void setValue(Object value) {
                if (value instanceof Color) {
                    color_ = (Color)value;
                } else {
                    super.setValue(value);
                }
            }

            public void paint(Graphics g) {
                int width = getSize().width;
                int height = getSize().height;
                g.setColor(Color.black);
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

        String colNode = MessageBundle.get("dialog.graph.series.table.node");
        String colAttribute = MessageBundle.get("dialog.graph.series.table.attribute");
        String colIndex = MessageBundle.get("dialog.graph.series.table.index");
        String colColor = MessageBundle.get("dialog.graph.series.table.color");
        String colLegend = MessageBundle.get("dialog.graph.series.table.legend");

        tableModel_ = new MyModel(
            new Object[]{
                colNode,
                colAttribute,
                colIndex,
                colColor,
                colLegend
            }
        );
        seriesTable_ = new JTable(tableModel_);
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

		comboType_.setBackground(Color.WHITE);
		comboModel_.setBackground(Color.WHITE);
		comboLink_.setBackground(Color.WHITE);
		comboAttr_.setBackground(Color.WHITE);

        removeButton_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    int ind = seriesTable_.getSelectedRow();
                    if (ind < 0) 
                        return;
                    
                    CellEditor ce = seriesTable_.getCellEditor();
                    if (ce != null) 
                        ce.stopCellEditing();
                    
                    String str = (String)tableModel_.getValueAt(ind, 4);
                    for (int i=0; i<dataItemInfoList_.size() ;i++) {
                    	if (str.equals(dataItemInfoList_.get(i).legend)) {
                    		removedList_.add(dataItemInfoList_.remove(i));
                    		tableModel_.removeRow(i);
                    		break;
                    	}
                    }
                    for (int i=0; i<addedList_.size() ; i++) {
                    	String addListLegend = 
                    		addedList_.get(i).objectName + "." +
                    		addedList_.get(i).nodeName + "." +
                    		addedList_.get(i).attribute + "." +
                    		"0";
                    	if (str.equals(addListLegend)) {
                    		addedList_.remove(i);
                    		tableModel_.removeRow(i);
                    		break;
                    	}
                    }
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
            }
        );

        try {
			prop.load(url.openStream());
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
		
		// this is temporary limit for data type
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
		
		this.addComponentListener(new ComponentAdapter(){
    		public void componentShown(ComponentEvent e) {
    			_resetSelection();
			}
		});

		comboModel_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isComboChanging_)
					return;
				
				isComboChanging_ = true;
				
				comboType_.removeAllItems();
				comboType_.addItem(DATA_TYPE);
				comboLink_.removeAllItems();
				comboLink_.addItem(LINK_NAME);
				comboAttr_.removeAllItems();
				comboAttr_.addItem(DATA_TYPE);
				
				if (comboModel_.getSelectedIndex() > 0) {
					Iterator<String> it = nodeMap.keySet().iterator();
					if (seriesTable_.getRowCount() > 0) {
						String curAttr = currentGraph_.getTrendGraph().getDataItemInfoList()[0].dataItem.attribute;
						while (it.hasNext()) {
							String key = it.next();
							if (nodeMap.get(key).contains(curAttr))
								comboType_.addItem(key);
						}
					} else {
                   		while (it.hasNext()) {
                  			comboType_.addItem(it.next());
                  		}
					}
					comboType_.setEnabled(true);
				} else 
					comboType_.setEnabled(false);
				
				comboLink_.setEnabled(false);
				comboAttr_.setEnabled(false);
				setButton_.setEnabled(false);
				
				isComboChanging_ = false;
				
				if (comboType_.getItemCount() == 2)
					comboType_.setSelectedIndex(1);
			}
		});
		
		comboType_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isComboChanging_)
					return;
				
				isComboChanging_ = true;
				comboLink_.removeAllItems();
				comboLink_.addItem(LINK_NAME);
				comboAttr_.removeAllItems();
				comboAttr_.addItem(LINK_NAME);
				
				if (comboType_.getSelectedIndex() > 0) {
					Object type = comboType_.getSelectedItem();
					GrxModelItem model = (GrxModelItem)comboModel_.getSelectedItem();
					if (type.equals("Joint")) {
						Vector<GrxLinkItem> li = model.links_;
						for (int i = 0; i < li.size(); i++) 
							comboLink_.addItem(li.get(i).getName());
					} else {
						//SensorType t = null;
						//if (type.equals("ForceSensor"))
						//	t = SensorType.FORCE_SENSOR;
						//else if (type.equals("Gyro"))
						//	t = SensorType.RATE_GYRO;
						//else if (type.equals("AccelerationSensor"))
						//	t = SensorType.ACCELERATION_SENSOR;
						//String[] snames = model.getSensorNames(t);
						String[] snames = model.getSensorNames((String)type);

						if (snames != null) {
							for (int i=0; i<snames.length; i++) {
								comboLink_.addItem(snames[i]);
							}
						}
					}
					comboLink_.setEnabled(true);
				} else
					comboLink_.setEnabled(false);

				comboAttr_.setEnabled(false);
				setButton_.setEnabled(false);
				
				isComboChanging_ = false;
				
				if (comboLink_.getItemCount() == 2)
					comboLink_.setSelectedIndex(1);
			}
		});
		
		comboLink_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isComboChanging_)
					return;
				
				isComboChanging_ = true;
				
   				comboAttr_.removeAllItems();
				comboAttr_.addItem(ATTRIBUTE);
   				
				if (comboLink_.getSelectedIndex() > 0) {
					List<String> l = nodeMap.get(comboType_.getSelectedItem());
					/*if (currentGraph_.getTrendGraph().getDataItemInfoList().length > 0) {
						String curAttr = currentGraph_.getTrendGraph().getDataItemInfoList()[0].dataItem.attribute;
						if (l.contains(curAttr))
							comboAttr_.addItem(curAttr);
					} else */
					if (seriesTable_.getRowCount() > 0) {
						String curAttr = (String)seriesTable_.getValueAt(0, 1);
						if (l.contains(curAttr))
							comboAttr_.addItem(curAttr);
					} else {
						Iterator<String> it = l.iterator();
                   		while (it.hasNext()) {
                  			comboAttr_.addItem(it.next());
                  		}
					}
					comboAttr_.setEnabled(true);
				} else 
					comboAttr_.setEnabled(false);
				
				setButton_.setEnabled(comboAttr_.getItemCount() > 1);
				
				isComboChanging_ = false;
				
				if (comboAttr_.getItemCount() == 2)
					comboAttr_.setSelectedIndex(1);
			} 
		});
		
		
		setButton_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int length = 1;
				String combo1 = (String)comboType_.getSelectedItem();
				if (combo1.equals("ForceSensor") ||
					combo1.equals("AccelerationSensor") ||
					combo1.equals("Gyro")) {
					length = 3;
				}
				
				for (int i=0; i<length; i++) {
					graphIndex++;
					Object[] rowData = new Object[5];
					rowData[0] = comboModel_.getSelectedItem().toString() + "." 
					           + (String)comboLink_.getSelectedItem();
	                rowData[1] = (String)comboAttr_.getSelectedItem();
	                rowData[2] = new Integer(i);
	                rowData[3] = currentGraph_.getTrendGraph().getGraphColor(graphIndex);
	                rowData[4] = comboModel_.getSelectedItem().toString() + "." 
			                   + (String)comboLink_.getSelectedItem() + "."
			                   + (String)comboAttr_.getSelectedItem() + "."
			                   + i;
	                
	                tableModel_.addRow(rowData);
				}
				
                addedList_.add(new AttributeInfo(
			    	(String)comboType_.getSelectedItem(),
			    	comboModel_.getSelectedItem().toString(), 
			    	(String)comboLink_.getSelectedItem(),
			    	(String)comboAttr_.getSelectedItem(),
			    	length == 1 ? 0 : length
			    ));
				removeButton_.setEnabled(true);
			}
		});
				
        JPanel line3 = new JPanel();
        line3.setLayout(new BoxLayout(line3, BoxLayout.X_AXIS));
        line3.add(Box.createHorizontalGlue());
        
        line3.add(comboModel_);
        line3.add(Box.createHorizontalStrut(BUTTON_GAP));
        line3.add(comboType_);
        line3.add(Box.createHorizontalStrut(BUTTON_GAP));
        line3.add(comboLink_);
        line3.add(Box.createHorizontalStrut(BUTTON_GAP));
        line3.add(comboAttr_);
        line3.add(Box.createHorizontalStrut(BUTTON_GAP));
        line3.add(setButton_);
        line3.add(Box.createHorizontalStrut(BUTTON_GAP));
        line3.add(removeButton_);
        
        line3.add(Box.createHorizontalStrut(BORDER_GAP));
        line3.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton okButton = new JButton(MessageBundle.get("dialog.okButton"));
        this.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    CellEditor ce = seriesTable_.getCellEditor();
                    if (ce != null) {
                        ce.stopCellEditing();
                    }
                    dataItemInfoArray_ =
                        dataItemInfoList_.toArray(new DataItemInfo[0]);
                    for (int i = 0; i < dataItemInfoArray_.length; i++) {
                        dataItemInfoArray_[i].color = (Color)tableModel_.getValueAt(i, 3);
                        dataItemInfoArray_[i].legend = (String)tableModel_.getValueAt(i, 4);
                    }
                	removedArray_ = removedList_.toArray(new DataItemInfo[0]);
                	removeAllRows();
                    addedArray_ = addedList_.toArray(new AttributeInfo[0]);
                	updated_ = true;
                    SeriesDialog.this.setVisible(false);
                }
            }
        );
        JButton cancelButton = new JButton(MessageBundle.get("dialog.cancelButton"));
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    CellEditor ce = seriesTable_.getCellEditor();
                    if (ce != null) {
                        ce.stopCellEditing();
                    }
                    removeAllRows();
                    SeriesDialog.this.setVisible(false);
                }
            }
        );
        this.addKeyListener(
            new KeyAdapter() {
                public void keyPressed(KeyEvent evt) {
                    if (evt.getID() == KeyEvent.KEY_PRESSED
                        && evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        CellEditor ce = seriesTable_.getCellEditor();
                        if (ce != null) {
                            ce.stopCellEditing();
                        }
                        removeAllRows();
                        SeriesDialog.this.setVisible(false);
                    }
                }
            }
        );
        JPanel bLine = new JPanel();
        bLine.setLayout(new BoxLayout(bLine, BoxLayout.X_AXIS));
        bLine.add(Box.createHorizontalGlue());
        bLine.add(okButton);
        bLine.add(Box.createHorizontalStrut(BUTTON_GAP));
        bLine.add(cancelButton);
        bLine.add(Box.createHorizontalStrut(BORDER_GAP));
        bLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        Container pane = getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(Box.createVerticalStrut(BORDER_GAP));
        pane.add(line1);
        pane.add(Box.createVerticalStrut(BUTTON_GAP));
        pane.add(line2);
        pane.add(Box.createVerticalStrut(BUTTON_GAP));
        pane.add(line3);
        pane.add(Box.createVerticalStrut(CONTENTS_GAP));
        pane.add(bLine);
        pane.add(Box.createVerticalStrut(BORDER_GAP));

        setResizable(false);
    }
    
    private void _resetSelection() {
		graphIndex = 0;
		isComboChanging_ = true;
		
		comboModel_.removeAllItems();
		comboModel_.addItem(ROBOT_MODEL);
		Iterator it = manager_.getItemMap(GrxModelItem.class).values().iterator();
		while (it.hasNext()) {
			GrxModelItem model = (GrxModelItem)it.next();
			if (model.isRobot())
				comboModel_.addItem(model);
		}
		comboType_.removeAllItems();
		comboType_.addItem(DATA_TYPE);
		comboLink_.removeAllItems();
		comboLink_.addItem(LINK_NAME);
		comboAttr_.removeAllItems();
		comboAttr_.addItem(ATTRIBUTE);
		
		comboModel_.setEnabled(true);
		comboType_.setEnabled(false);
		comboLink_.setEnabled(false);
		comboAttr_.setEnabled(false);
		setButton_.setEnabled(false);
		
		isComboChanging_ = false;
		
		if (comboModel_.getItemCount() == 2)
			comboModel_.setSelectedIndex(1);
    }

    public void setCurrentGraph(GraphElement currentGraph) {
        currentGraph_ = currentGraph;
    }

    /**
     *
     * @param   visible 
     */
    public void setVisible( boolean visible) {
        if (visible) {  
            if(seriesTable_.getRowCount() > 0)
            	seriesTable_.setRowSelectionInterval(0, 0); 
            removedList_ = new ArrayList<DataItemInfo>(); 
            addedList_ = new ArrayList<AttributeInfo>();
            removeButton_.setEnabled(true); 
            updated_ = false;               
            pack(); 

          //  Dimension d1 = getOwner().getSize();
          //  Dimension d2 = getSize();
            this.setLocationRelativeTo(getOwner());//((d1.width-d2.width)/2, (d1.height-d1.height)/2);
        }
        super.setVisible(visible); 
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
            tableModel_.addRow(rowData);
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
    public AttributeInfo[] getAddedList() {
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
        private final Color selectedColor_ = new Color(153, 153, 204);

        private boolean selected_;
        private Color color_;

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
            color_ = (Color)value;
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
                g.setColor(Color.black);
                g.fillRect(2, 2, width - 4, height - 4);
            } else {
                g.setColor(Color.black);
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

        // -----------------------------------------------------------------
        /**
         *
         */
        public MyModel(
            Object[] columnNames
        ) {
            super(columnNames, 0);
        }

        // -----------------------------------------------------------------
        /**
         *
         */
        public boolean isCellEditable(int row, int col) {
            return (col == 3 || col == 4);
        }
    }
}

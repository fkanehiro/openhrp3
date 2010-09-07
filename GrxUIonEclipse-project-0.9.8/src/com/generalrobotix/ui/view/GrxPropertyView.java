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

import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial") //$NON-NLS-1$
/**
 * @brief property viewer
 */
public class GrxPropertyView extends GrxBaseView {
    
    public static final String TITLE = "Property"; //$NON-NLS-1$

    private GrxBasePlugin currentPlugin_=null;

    private TableViewer viewer_ = null;

    private Table table_ = null;
    
    private Text nameText_ = null;
    private String oldText = "";

    private final String[] clmName_ = { MessageBundle.get("GrxPropertyView.text.name"), MessageBundle.get("GrxPropertyView.text.value") }; //$NON-NLS-1$ //$NON-NLS-2$
    
    private MenuManager menuMgr_= new MenuManager();

    /**
     * @brief constructor
     * @param name
     * @param manager
     * @param vp
     * @param parent
     */
    public GrxPropertyView(
        String name,
        GrxPluginManager manager,
        GrxBaseViewPart vp,
        Composite parent) {

        super(name, manager, vp, parent);
        
        GrxDebugUtil.println("GrxPropertyView init"); //$NON-NLS-1$

		GridLayout gl = new GridLayout(1,false);
		composite_.setLayout( gl );

		nameText_ = new Text(composite_, SWT.SINGLE|SWT.BORDER);
        GridData textGridData = new GridData(GridData.FILL_HORIZONTAL);
        textGridData.heightHint = 20;
        nameText_.setLayoutData(textGridData);
		
        final GrxPropertyView view = this;
        nameText_.addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.CR && !nameText_.getText().equals(oldText)) {
                	GrxBasePlugin p = null;
                	if ((p = manager_.getView(nameText_.getText(), true)) != null){
                		if(currentPlugin_ != p){
                			if(currentPlugin_ != null)
                				currentPlugin_.deleteObserver(view);
                			_setInput(p);
                			currentPlugin_ = p;
                			currentPlugin_.addObserver(view);
                		}
                	}else if ((p = manager_.getItem(nameText_.getText())) != null){
                		manager_.focusedItem((GrxBaseItem) p);
                	}else{
                		nameText_.setText(""); //$NON-NLS-1$
                		if(currentPlugin_ != null)
            				currentPlugin_.deleteObserver(view);
            			_setInput(null);
            			currentPlugin_ = null;
                	}
                }
            }
        });

        viewer_ = new TableViewer(composite_, SWT.MULTI | SWT.FULL_SELECTION
            | SWT.BORDER | SWT.VIRTUAL);
        
        TableLayout layout = new TableLayout();
        layout.addColumnData(new ColumnWeightData(40, true));
        layout.addColumnData(new ColumnWeightData(60, true));
        table_ = viewer_.getTable();
        table_.setLayout(layout);
        GridData tableGridData = new GridData(GridData.FILL_BOTH);
        table_.setLayoutData(tableGridData);
        
        TableColumn column1 = new TableColumn(table_, SWT.LEFT);
        TableColumn column2 = new TableColumn(table_, SWT.LEFT);
        column1.setText(clmName_[0]);
        column2.setText(clmName_[1]);
        column1.setMoveable(true);
        column2.setMoveable(true);
        column1.addSelectionListener(new ColumnSelectionListener());
        column2.addSelectionListener(new ColumnSelectionListener());
        
        viewer_.setColumnProperties(clmName_);

        viewer_.setContentProvider(new PropertyTableContentProvider());
        viewer_.setLabelProvider(new PropertyTableLabelProvider());
        CellEditor[] editors = new CellEditor[] {
            new TextCellEditor(table_),
            new TextCellEditor(table_) };
        viewer_.setCellEditors(editors);
        viewer_.setCellModifier(new PropertyTableCellModifier());
        viewer_.setSorter(new PropertyTableViewerSorter());
        
        table_.setLinesVisible(true);
        table_.setHeaderVisible(true);

        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        
        GrxBaseItem item = manager_.focusedItem();
        manager_.registerItemChangeListener(this, GrxBaseItem.class);
        viewer_.setInput(item);
        currentPlugin_ = item;
        if(currentPlugin_ != null)
        	currentPlugin_.addObserver(this);
        
        //      右クリックメニュー
        table_.setMenu(menuMgr_.createContextMenu( table_ ));
        table_.addListener ( SWT.MenuDetect, new Listener () {
    		public void handleEvent (Event event) {
    			final TableItem[] selection = table_.getSelection();
                menuMgr_.removeAll(); 
                if(selection.length!=0)
                	menuMgr_.add(new Action(){
                		public String getText(){ return MessageBundle.get("GrxPropertyView.menu.InputValue"); }
                		public void run(){
                			InputDialog dialog = new InputDialog( null, getText(),
                					MessageBundle.get("GrxPropertyView.dialog.message.input"), "", null); //$NON-NLS-1$
                			if ( dialog.open() == InputDialog.OK && dialog.getValue() != null){
                				String _value = dialog.getValue();
                				String[] _key = new String[selection.length];
                				for(int i=0; i<selection.length; i++){  // value を変更するとソートがかかってselectionが変わってしまうので、まずkeyを保存
                					_key[i] = selection[i].getText(0);
                				}
                				for(int i=0; i<_key.length; i++){
                					if(isValidChangeValue(_key[i], _value) == false)
                						continue;
                					
                					if(!currentPlugin_.getProperty(_key[i]).equals(_value)){
                		                if (!currentPlugin_.propertyChanged(_key[i], _value)){
                		                	currentPlugin_.setProperty(_key[i], _value);
                		                }
                	                }
                				}
                			}
                					
                		}
                	});
    		}
    	});

        updateTableFont();
    }

    private boolean isValidChangeValue(String key, String value){
    	GrxBasePlugin.ValueEditType editType = currentPlugin_.GetValueEditType(key);
    	if(editType instanceof ValueEditText){
			return true;
    	}else if(editType instanceof ValueEditCombo){
    		String[] items = ((ValueEditCombo)editType).GetItems();
    		for(int idx=0; idx<items.length; idx++){
    			if(items[idx].equals(value)){
    				return true;
    			}
    		}
    		return false;
    	}
    	return false;
    }

    public void registerItemChange(GrxBaseItem item, int event){
    	switch(event){
    	case GrxPluginManager.FOCUSED_ITEM:
    		if(currentPlugin_ != item){
    			if(currentPlugin_ != null)
    				currentPlugin_.deleteObserver(this);
    			_setInput(item);
    			currentPlugin_ = item;
    			currentPlugin_.addObserver(this);
    		}
    		break;
    	case GrxPluginManager.REMOVE_ITEM:
    		if(currentPlugin_ == item){
   				currentPlugin_.deleteObserver(this);
    			_setInput(null);
    			currentPlugin_ = null;
    		}
    		break;
    	default:
    		break;
    	}
    }
   
    public void update(GrxBasePlugin plugin, Object... arg) {
    	if((String)arg[0]!="PropertyChange") //$NON-NLS-1$
    		return;
    	_refresh();
    }
    
    private class PropertyTableContentProvider implements
        IStructuredContentProvider {

        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Properties) {
                return ((Properties)inputElement).entrySet().toArray();
            }
            return new Object[0];
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

    }

    private class PropertyTableLabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            String str = null;
            if (element instanceof Map.Entry && columnIndex < clmName_.length) {
                switch (columnIndex) {
                    case 0:
                        str = (String) ((Map.Entry)element).getKey();
                        break;
                    case 1:
                        str = (String) ((Map.Entry)element).getValue();
                        break;
                    default:
                        str = "---"; //$NON-NLS-1$
                        break;
                }
            }
            return str;
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

    }

    private class PropertyTableCellModifier implements ICellModifier {

        public boolean canModify(Object element, String property) {
            if(currentPlugin_ instanceof GrxGraphItem || currentPlugin_ instanceof GrxCollisionPairItem )
                return false;
            if(property.equals(clmName_[1])){
                if (element instanceof Map.Entry) {
	            	Map.Entry<String, String> _element = (Map.Entry)element;
	            	if(_element.getKey().equals("url"))	
	            		return false;
                    GrxBasePlugin.ValueEditType editType = currentPlugin_.GetValueEditType(_element.getKey());
                    if(editType instanceof ValueEditCombo){
	            		CellEditor[] editors = new CellEditor[] { 
	            				new TextCellEditor(table_),
	            	            new ComboBoxCellEditor(table_, ((ValueEditCombo) editType).GetItems()) };
	            		viewer_.setCellEditors(editors);
                    }else if(editType instanceof ValueEditText){
	            		CellEditor[] editors = new CellEditor[] { 
	            				new TextCellEditor(table_),
	            				new TextCellEditor(table_) };
	            		viewer_.setCellEditors(editors);
                    }else{
                    	return false;
                    }
	            }
            	return true;
        	}else 
        		return false;
        }

        public Object getValue(Object element, String property) {
            if (property.equals(clmName_[1]) &&
                element instanceof Map.Entry) {
            	Map.Entry<String, String> _element = (Map.Entry)element;
            	CellEditor[] ce = viewer_.getCellEditors();
            	if(2 <= ce.length && ce[1] instanceof ComboBoxCellEditor){
            		String[] items = ((ComboBoxCellEditor)ce[1]).getItems();
            		for(int i=0; i<items.length; i++)
            			if(_element.getValue().equals(items[i]))
            				return new Integer(i);
            		return new Integer(0);
            	}else{
            		return _element.getValue();
            	}
            }
            return null;
        }

        @SuppressWarnings("unchecked") //$NON-NLS-1$
        public void modify(Object element, String property, Object value) {
            try{
            if (element instanceof TableItem){
            	TableItem item = (TableItem) element;
            	String _value = null;
            	if( value instanceof String) {
            		_value = (String)value;
            	}else if(value instanceof Integer){
            		int idx = ((Integer)value).intValue();
                	CellEditor[] ce = viewer_.getCellEditors();
                	if(2 <= ce.length && ce[1] instanceof ComboBoxCellEditor){
                		
                		String[] items = ((ComboBoxCellEditor)ce[1]).getItems();
                		if(0 <= idx && idx < items.length)
                			_value = items[idx];
                		else
                		{
                    		String str = ((CCombo)ce[1].getControl()).getText();
                    		for(int i = 0; i < items.length; i++)
                    		{
                    			if(items[i].equals(str))
                    			{
                    				_value = str;
                    				break;
                    			}
                    		}
                		}
                	}
            	}

            	if(_value != null && 
                   !currentPlugin_.getProperty(item.getText()).equals(_value)){
	                if (!currentPlugin_.propertyChanged(item.getText(), _value)){
	                	// if validity of value is not checked by currentPlugin_, it is set
	                	currentPlugin_.setProperty(item.getText(), _value);
	                }
                }
                _refresh();
            }
            }catch(Exception ex){
            	ex.printStackTrace();
            }
        }

    }

    private void _setInput(GrxBasePlugin p){
    	if(p == null){
    		table_.setVisible(false);
            nameText_.setText(""); //$NON-NLS-1$
            oldText="";
    	}else if (p != currentPlugin_) {
            table_.setVisible(false);
            nameText_.setText(p.getName());
            oldText=p.getName();
            viewer_.setInput(p);
            table_.setVisible(true);
        }    	
    }
    /**
     * @brief refresh contents of table
     */
    private void _refresh(){
        table_.setVisible(false);
        nameText_.setText(currentPlugin_.getName());
        oldText=currentPlugin_.getName();
        viewer_.setInput(currentPlugin_);
        table_.setVisible(true);
    }
    
    private class PropertyTableViewerSorter extends ViewerSorter {

        public int compare(Viewer viewer, Object e1, Object e2) {
            Table table = ((TableViewer)viewer).getTable();
            if(table.getSortColumn() == null){
                return 0;
            }
            TableColumn sortColumn = table.getSortColumn();
            int direction = table.getSortDirection();
            
            String comperedFactor1 = ""; //$NON-NLS-1$
            String comperedFactor2 = ""; //$NON-NLS-1$
            
            if(sortColumn.getText().equals(clmName_[0])){
                comperedFactor1 = (String)((Map.Entry)e1).getKey();
                comperedFactor2 = (String)((Map.Entry)e2).getKey();
            }
            else if(sortColumn.getText().equals(clmName_[1])){
                comperedFactor1 = (String)((Map.Entry)e1).getValue();
                comperedFactor2 = (String)((Map.Entry)e2).getValue();
            }
            else{
                return 0;
            }
            
            if (direction == SWT.NONE) {
                return 0;
            }
            else if(direction == SWT.UP){
                return comperedFactor1.compareTo(comperedFactor2);
            }
            else if(direction == SWT.DOWN){
                return (-1) * comperedFactor1.compareTo(comperedFactor2);
            }
            else{
                return 0;
            }
        }
    }
    
    
    private class ColumnSelectionListener implements SelectionListener{

        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }

        public void widgetSelected(SelectionEvent e) {
            TableColumn column = (TableColumn)e.widget;
            
            int currentDirection = table_.getSortDirection(); 
            
            if(column != table_.getSortColumn() || currentDirection == SWT.NONE){
                table_.setSortDirection(SWT.UP);
            }
            else if(currentDirection == SWT.DOWN){
                table_.setSortDirection(SWT.NONE);
            }
            else if(currentDirection == SWT.UP){
                table_.setSortDirection(SWT.DOWN);
            }
            else{
                table_.setSortDirection(SWT.NONE);
            }
            table_.setSortColumn(column);
            viewer_.setSorter(new PropertyTableViewerSorter());
            e.doit = false;
        }
    }
    
    public void shutdown(){
    	manager_.removeItemChangeListener(this, GrxBaseItem.class);
    	if(currentPlugin_ != null)
			currentPlugin_.deleteObserver(this);
    }

    public void updateTableFont(){
        viewer_.getTable().setFont(Activator.getDefault().getFont("preference_table"));
    }
}

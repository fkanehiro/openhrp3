package com.generalrobotix.ui.grxui;


import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.generalrobotix.ui.util.MessageBundle;

public class PreferenceItemPage extends PreferencePage implements IWorkbenchPreferencePage {
	TableViewer viewer_=null;
	ArrayList<Item> input_=null;
	String oldValue_=null;
	
	public PreferenceItemPage() {
		super("");
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	protected Control createContents(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		
		viewer_ = new TableViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		viewer_.setContentProvider(new ArrayContentProvider());
		viewer_.setLabelProvider(new ItemLabelProvider());
		viewer_.setCellModifier(new CellModifier(viewer_));
		Table table = viewer_.getTable();
		String[] properties = new String[]{ "class","classPath","visible" };
		viewer_.setColumnProperties(properties); 
		CellEditor[] editors = new CellEditor[]{
				new TextCellEditor(table),
				new TextCellEditor(table),
			    new CheckboxCellEditor(table)
		};
		viewer_.setCellEditors(editors);
		
	    table.setLinesVisible(true);
	    table.setHeaderVisible(true);

	    TableColumn column = new TableColumn(table, SWT.NONE);
	    column.setText(MessageBundle.get("PreferenceItemPage.table.column0"));
	    column = new TableColumn(table, SWT.NONE);
	    column.setText(MessageBundle.get("PreferenceItemPage.table.column1"));
	    column = new TableColumn(table,SWT.NONE);
	    column.setText(MessageBundle.get("PreferenceItemPage.table.column2"));
	    viewer_.setInput(input_);
		
	    TableColumn[] columns = table.getColumns();
	    for(int i = 0; i < columns.length; i++) {
	      columns[i].pack();
	    }

	    final Composite buttons = new Composite(parent, SWT.NONE);
        GridData layoutData = new GridData();
        layoutData.verticalAlignment = GridData.FILL;
        buttons.setLayoutData(layoutData);
        buttons.setLayout(new GridLayout());
        
        Button addButton = new Button(buttons, SWT.PUSH);
        layoutData = new GridData();
        layoutData.horizontalAlignment = GridData.FILL;
        addButton.setLayoutData(layoutData);
        addButton.setText(MessageBundle.get("PreferenceItemPage.button.add"));
        addButton.addSelectionListener(new SelectionListener() {
        	public void widgetSelected(SelectionEvent e) {
        		InputDialog dialog = new InputDialog( buttons.getShell(), MessageBundle.get("PreferenceItemPage.dialog.title"), 
                                        MessageBundle.get("PreferenceItemPage.dialog.message"), null, null);
        		if(dialog.open()==InputDialog.OK){
        			String name = dialog.getValue();
                		if(!name.equals("")){
                			input_.add(new Item(name, "", "true", true));
                			viewer_.refresh();
                		}
                	}
            	}
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        
        Button delButton = new Button(buttons, SWT.PUSH);
        layoutData = new GridData();
        layoutData.horizontalAlignment = GridData.FILL;
        delButton.setLayoutData(layoutData);
        delButton.setText(MessageBundle.get("PreferenceItemPage.button.delete"));
        delButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
            	 Item item = (Item) ((IStructuredSelection) viewer_.getSelection()).getFirstElement();
            	 if(item.isUser_){
            		 input_.remove(item);
            		 viewer_.refresh();
            	 }
            }
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        
	    return viewer_.getControl();
	}

	private void setItem(){
	    String itemList=getPreferenceStore().getString(PreferenceConstants.ITEM+"."+PreferenceConstants.CLASS);
	    String[] item=itemList.split(PreferenceConstants.SEPARATOR, -1);
	    String visibleList=getPreferenceStore().getString(PreferenceConstants.ITEM+"."+PreferenceConstants.VISIBLE);
	    String[] visible=visibleList.split(PreferenceConstants.SEPARATOR, -1);
	    for(int i=0; i<item.length; i++){
	    	input_.add(new Item(item[i], "", visible[i], false));
	    }
	    itemList = getPreferenceStore().getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASS);
	    item=itemList.split(PreferenceConstants.SEPARATOR, -1);
		String pathList=getPreferenceStore().getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASSPATH);
		String[] path=pathList.split(PreferenceConstants.SEPARATOR, -1);
		visibleList=getPreferenceStore().getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.VISIBLE);
	    visible=visibleList.split(PreferenceConstants.SEPARATOR, -1);
		for(int i=0; i<item.length; i++){
			if(!item[i].equals(""))
				input_.add(new Item(item[i], path[i], visible[i], true));
	    }
	}
	
	public void init(IWorkbench workbench) {	
		input_ = new ArrayList<Item>();
		setItem();
		oldValue_=getPreferenceStore().getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASS);
	}

	private class Item {
		private String name_=null;
		private String path_=null;
		private boolean visible_=true;
		private boolean isUser_=false;
		
		public Item(String name, String path, String visible, boolean isUser){
			name_=name;
			path_=path;
			visible_=visible.equals("true")? true : false;
			isUser_=isUser;
		}
	}
	
	private class ItemLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			 Item item = (Item)element;
			 switch (columnIndex) {
			 case 0:
				 return item.name_;
			 case 1:
				 return item.path_;
			 case 2:
				 return item.visible_==true? "true" : "false";
			 default:
				 break;	 
			 }
			return null;
		}
		
		public Color getForeground(Object element, int columnIndex) {
			Item item = (Item)element;
			if(item.isUser_)
				return null;
			else
				return Activator.getDefault().getColor("gray");
		}

		public Color getBackground(Object element, int columnIndex) {
			return null;
		}
	}
	
	private class CellModifier implements ICellModifier {
		private TableViewer viewer;
		
		public CellModifier(TableViewer viewer) {
			this.viewer = viewer;
		}
		
		public boolean canModify(Object element, String property) {
			Item item = (Item)element;
			if(item.isUser_)
				return true;
			else
				return false;
		}
		
		public Object getValue(Object element, String property) {
			Item item = (Item) element;
		    if (property == "class")
		    	return item.name_;
		    if (property == "classPath")
		    	return item.path_;
		    if (property == "visible")
		    	return item.visible_;
		    return null;
		}

		public void modify(Object element, String property, Object value) {
			if (element instanceof org.eclipse.swt.widgets.Item) {
				element = ((org.eclipse.swt.widgets.Item) element).getData();
		    }
		    Item item = (Item) element;
		    if(property == "class") {
		    	item.name_=(String)value;
		    }
		    if(property == "classPath") {
		    	item.path_=(String)value;
		    }
		    if(property == "visible") {
		      item.visible_=(Boolean)value;
		    }
		    viewer.update(element, null);
		}
	}
	
	public boolean performOk() {
		String name="";
		String path="";
		String visible="";
		Iterator<Item> it = input_.iterator();
		while(it.hasNext()){
			Item item=it.next();
			if(item.isUser_){
				name += item.name_;
				path += item.path_;
				visible += item.visible_? "true" : "false";
				if(it.hasNext()){
					name += PreferenceConstants.SEPARATOR;
					path += PreferenceConstants.SEPARATOR;
					visible += PreferenceConstants.SEPARATOR;
				}
			}
		}
		getPreferenceStore().putValue(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASSPATH, path);
		getPreferenceStore().putValue(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASS, name);
		getPreferenceStore().putValue(PreferenceConstants.USERITEM+"."+PreferenceConstants.VISIBLE, visible);
		getPreferenceStore().firePropertyChangeEvent("userItemChange", oldValue_, name);
		return true;
    }
   
	protected void performDefaults() {
		getPreferenceStore().setToDefault(PreferenceConstants.ITEM+"."+PreferenceConstants.CLASS);
		getPreferenceStore().setToDefault(PreferenceConstants.ITEM+"."+PreferenceConstants.VISIBLE);
		getPreferenceStore().setToDefault(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASS);
		getPreferenceStore().setToDefault(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASSPATH);
		getPreferenceStore().setToDefault(PreferenceConstants.USERITEM+"."+PreferenceConstants.VISIBLE);
	    input_.clear();
		setItem();
	    viewer_.refresh();
	}
	
	protected void performApply() {
		performOk();
		oldValue_=getPreferenceStore().getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASS);
	}
}

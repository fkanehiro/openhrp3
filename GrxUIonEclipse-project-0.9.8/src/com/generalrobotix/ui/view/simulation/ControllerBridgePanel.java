package com.generalrobotix.ui.view.simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PopupList;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSensorItem;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;

public class ControllerBridgePanel extends Dialog{
	private Vector<String> controllerBridgeFile_ = new Vector<String>();
	private String fileName_;
	private String rtcConfFileName_;
	private String controllerName_;
	private String projectControllerName_;
	private String robotName_;
	private GrxModelItem robot_;
	private String robotRtcName_;
	private Vector<Inport> inport_ = new Vector<Inport>();
	private Vector<Outport> outport_ = new Vector<Outport>();
	private HashMap<String, String> connection_ = new HashMap<String, String>();
	private NameServer nameServer_ = new NameServer();
	private Vector<Port> portTable_ = new Vector<Port>();
	private enum DataTypeId {JOINT_VALUE, JOINT_VELOCITY, JOINT_ACCELERATION, JOINT_TORQUE, EXTERNAL_FORCE,
	    			  ABS_TRANSFORM, ABS_VELOCITY, ABS_ACCELERATION, FORCE_SENSOR, RATE_GYRO_SENSOR, ACCELERATION_SENSOR,
	    			  RANGE_SENSOR, CONSTRAINT_FORCE, COLOR_IMAGE, GRAYSCALE_IMAGE, DEPTH_IMAGE;
					  public static enum NUMID_CODE {INVALID_TYPE, NOT_USED, USED, ONLY_ONE, NUMBER}; 
					  public static String[] getNames(){
	    				  String[] s = new String[values().length];
	    				  int i=0;
	    				  for(DataTypeId id : values()){
	    					  s[i++] = id.name();
	    				  }
						return s;
	    			  }
	    			  public static boolean isInport(String type){
	    				  if(DataTypeId.valueOf(type).compareTo(DataTypeId.ABS_ACCELERATION) > 0 )
	    					  return false;
	    				  else
	    					  return true;
	    			  }
	    			  public static boolean isOutport(String type){
	    				  if(DataTypeId.valueOf(type).compareTo(DataTypeId.EXTERNAL_FORCE) == 0 )
	    					  return false;
	    				  else
	    					  return true;
	    			  }
	    			  public static NUMID_CODE numId(String type){
	    				  int ordinal = DataTypeId.valueOf(type).ordinal();
	    				  if(ordinal <= 3 || (8<=ordinal && ordinal <=11) )
	    					  return NUMID_CODE.NOT_USED;
	    				  if(4<=ordinal && ordinal<=7)
	    					  return NUMID_CODE.USED;
	    				  if(ordinal==12)
	    					  return NUMID_CODE.ONLY_ONE;
	    				  if(13<=ordinal && ordinal<=15)
	    					  return NUMID_CODE.NUMBER;
						return NUMID_CODE.INVALID_TYPE;
	    			  }
	}
	private String errorMessage_;
	private Vector<String> rtcConfFile_ = new Vector<String>();
	private NameServer rtcNameServer_ = new NameServer();
	private String moduleName_;
	private String controllerRtcName_;
	private String endChar_ = "";
	private int commandLineStart_=0;
	private int commandLineEnd_=0;
	
	private StyledText cbtext_;
	private StyledText rctext_;
	private Text rrnText_;
	private Text nshText_;
	private Text nspText_;
	private TableViewer tableviewer_;
	private StyledText emText_;
	private Button saveButton_ ;
	private Button saveAsButton_ ; 
	private Label fileNameLabel_;
	private Label rtcConfFileNameLabel_;
	
	private int os_;
	private final int LINUX = 0;
	private final int WINDOWS = 1;
	
	ControllerBridgePanel(Shell shell) {
		super(shell);
		if(System.getProperty("os.name").equals("Linux") || System.getProperty("os.name").equals("Mac OS X")){
			os_ = LINUX;
			endChar_ = "\\";
		}else{
			endChar_ = "^";
			os_ = WINDOWS;
		}
		init();
	}
	
	protected void configureShell(Shell newShell) {   
        super.configureShell(newShell);
        newShell.setText(MessageBundle.get("panel.Bridge.title"));
    }
	
	protected Control createDialogArea(Composite parent) {
    	Composite composite = (Composite)super.createDialogArea(parent);
    	composite.setLayout(new GridLayout(1,true));
    	
    	Composite panel0 = new Composite(composite,SWT.NONE);
    	panel0.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	panel0.setLayout(new GridLayout(3,false));
    	Label label = new Label(panel0, SWT.LEFT );
    	label.setText(MessageBundle.get("panel.Bridge.controllerName")+":");
    	final Text text = new Text(panel0, SWT.BORDER);
    	text.setText(controllerName_);
    	text.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				String newName = text.getText();
				if(projectControllerName_.equals(newName))
					text.setForeground(Activator.getDefault().getColor("black"));
				else
					text.setForeground(Activator.getDefault().getColor("red"));
				controllerName_ = newName;
				rrnTextUpdate();
				saveButtonEnabled(false);
			}
    	});
    	
    	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        text.setLayoutData(gridData);
    	Label label0 = new Label(panel0, SWT.LEFT );
    	label0.setText(MessageBundle.get("panel.Bridge.robotName")+":"+robotName_+"            ");
    	Label label1 = new Label(panel0, SWT.LEFT);
    	label1.setText(MessageBundle.get("panel.Bridge.robotRtcName")+":");
    	label1.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    	rrnText_ = new Text(panel0, SWT.BORDER);
    	GridData gridData0 = new GridData();
    	gridData0.horizontalAlignment = GridData.FILL;
    	gridData0.grabExcessHorizontalSpace = true;
    	rrnText_.setLayoutData(gridData0);
    	rrnTextUpdate();
    	rrnText_.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				if(rrnText_.isFocusControl()){
					robotRtcName_ = rrnText_.getText();
					saveButtonEnabled(false);
				}
			}
    	});
  
    	tableviewer_ = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER);
    	Table table = tableviewer_.getTable();
        table.setFont(Activator.getDefault().getFont("preference_table"));
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        TableColumn column = new TableColumn(table,SWT.NONE);
        column.setText("Type");
        column.setWidth(50);
        column = new TableColumn(table,SWT.NONE);
        column.setText("name");
        column.setWidth(100);
        column = new TableColumn(table,SWT.NONE);
        column.setText("property");
        column.setWidth(100);
        column = new TableColumn(table,SWT.NONE);
        column.setText("id");
        column.setWidth(100);
        column = new TableColumn(table,SWT.NONE);
        column.setText("output Time");
        column.setWidth(100);
        column = new TableColumn(table,SWT.NONE);
        column.setText("Controller : Port name");
        column.setWidth(200);
        GridData gridData1 = new GridData(GridData.FILL_VERTICAL);
        gridData1.minimumHeight = 200;
        tableviewer_.getControl().setLayoutData(gridData1);
        tableviewer_.setContentProvider(new ArrayContentProvider());
        tableviewer_.setLabelProvider(new InportLabelProvider());
        tableviewer_.setInput(portTable_);
        tableviewer_.setColumnProperties(new String[] {"type", "name", "property", "id", "time", "portName"});
        DialogCellEditor idCellEditor =new DialogCellEditor(tableviewer_.getTable()){
			protected Object openDialogBox(Control cellEditorWindow) {
				IdCellDialog dialog = new IdCellDialog(cellEditorWindow.getShell());
				dialog.setId((String)getValue());
		        if(dialog.open() == IDialogConstants.OK_ID)
		        	setValue(dialog.getId());
		        return null;
			}
        };
        CellEditor[] editors = new CellEditor[] {
        		new ComboBoxCellEditor(tableviewer_.getTable(), new String[]{"IN", "OUT" }), 
        		new TextCellEditor(tableviewer_.getTable()),
        		new ComboBoxCellEditor(tableviewer_.getTable(), DataTypeId.getNames()),
        		idCellEditor,
        		new TextCellEditor(tableviewer_.getTable()),
        		new TextCellEditor(tableviewer_.getTable())	 };
        tableviewer_.setCellEditors(editors);
        tableviewer_.setCellModifier(new TableCellModifier());
        tableviewer_.getControl().addMouseListener(new MouseListener(){
			public void mouseDoubleClick(MouseEvent e) {			
			}
			public void mouseDown(MouseEvent e) {
				if(e.button!=1){
					PopupList list = new PopupList(getShell());
					list.setItems(new String[]{MessageBundle.get("panel.Bridge.add"),
							MessageBundle.get("panel.Bridge.delete")});
					Rectangle rectangle = getShell().getBounds();
					String s = list.open(new Rectangle(rectangle.x+e.x, rectangle.y+e.y, 100, 100));
					if(s.equals(MessageBundle.get("panel.Bridge.add"))){
						Port _port = new Port();
						_port.type_ = "IN";
						_port.name_ = "";
						_port.propertyName_ = "JOINT_VALUE";
						_port.id_ = "All joints";
						_port.outputTime_ = "";
						_port.controllerPortName_ = "";
						portTable_.add(_port);
						tableviewer_.refresh();
						saveButtonEnabled(false);
					}else{
						int row = tableviewer_.getTable().getSelectionIndex();
						portTable_.remove(row);
						tableviewer_.refresh();
						saveButtonEnabled(false);
					}
				}
			}
			public void mouseUp(MouseEvent e) {
			}
        	
        });
        
        Composite panel1 = new Composite(composite,SWT.NONE);
        panel1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	panel1.setLayout(new GridLayout(3,false));
        Label label2 = new Label(panel1, SWT.LEFT );
    	label2.setText(MessageBundle.get("panel.Bridge.nameServer")+": ");
    	Label label3 = new Label(panel1, SWT.LEFT );
    	label3.setText(MessageBundle.get("panel.Bridge.host")+": ");
    	nshText_ = new Text(panel1, SWT.BORDER);
    	Label dumy = new Label(panel1, SWT.LEFT );
    	Label label4 = new Label(panel1, SWT.LEFT );
    	label4.setText(MessageBundle.get("panel.Bridge.port")+": ");
    	nspText_ = new Text(panel1, SWT.BORDER);
    	nshText_.setText(nameServer_.host_);
    	nshText_.setLayoutData(gridData0);
    	nspText_.setText(nameServer_.port_);
    	nspText_.setLayoutData(gridData0);
    	ModifyListener nsModifyListener = new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				nameServer_.host_ = nshText_.getText();
				nameServer_.port_ = nspText_.getText();
				saveButtonEnabled(false);
			}
    	};
    	nshText_.addModifyListener(nsModifyListener);
    	nspText_.addModifyListener(nsModifyListener);
    	
    	Composite panel5 = new Composite(composite,SWT.NONE);
    	panel5.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	panel5.setLayout(new GridLayout(4,false));
    	Label label8 = new Label(panel5, SWT.LEFT );
    	label8.setText(MessageBundle.get("panel.Bridge.moduleName")+": ");
    	final Text text4 = new Text(panel5, SWT.BORDER);
    	text4.setLayoutData(gridData0);
    	text4.setText(moduleName_);
    	text4.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				moduleName_ = text4.getText();
				saveButtonEnabled(false);
			}
    	});
    	Label label9 = new Label(panel5, SWT.LEFT );
    	label9.setText(MessageBundle.get("panel.Bridge.controllerRTCName")+": ");
    	final Text text5 = new Text(panel5, SWT.BORDER);
    	text5.setLayoutData(gridData0);
    	text5.setText(controllerRtcName_);
    	text5.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				controllerRtcName_ = text5.getText();
				saveButtonEnabled(false);
			}
    	});
    	
    	Composite panel4 = new Composite(composite,SWT.NONE);
    	panel4.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	panel4.setLayout(new GridLayout(1,false));
    	Button button = new Button(panel4, SWT.PUSH);
    	button.setText(MessageBundle.get("panel.Bridge.check"));
    	button.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				errorCheck();
				updateControllerBridgeFile();
				cbTextUpdate();
				updateRtcConfFile();
				rcTextUpdate();
				saveButtonEnabled(true);
				emText_.setText(errorMessage_);
			}
    	});
    	fileNameLabel_ = new Label(panel4, SWT.LEFT);
    	if(fileName_=="")
    		fileNameLabel_.setText("new file");
    	else
    		fileNameLabel_.setText(fileName_);
    	cbtext_ = new StyledText(panel4, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
    	GridData gridData2 = new GridData();
    	gridData2.horizontalAlignment = GridData.FILL;
    	gridData2.grabExcessHorizontalSpace = true;
    	gridData2.verticalAlignment = GridData.FILL;
    	gridData2.grabExcessVerticalSpace = true;
    	gridData2.minimumHeight = 150;
    	cbtext_.setLayoutData(gridData2);
    	cbtext_.setBackground(Activator.getDefault().getColor("gray"));
    	cbtext_.setEditable(false);
    	cbTextUpdate();
    	
    	Composite panel3 = new Composite(composite,SWT.NONE);
    	panel3.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	panel3.setLayout(new GridLayout(1,false));
    	rtcConfFileNameLabel_ = new Label(panel3, SWT.LEFT);
    	if(rtcConfFileName_.equals(""))
    		rtcConfFileNameLabel_.setText("rtc.conf");
    	else
    		rtcConfFileNameLabel_.setText(rtcConfFileName_);
    	rctext_ = new StyledText(panel3,  SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL );
    	rctext_.setLayoutData(gridData2);
    	rctext_.setBackground(Activator.getDefault().getColor("gray"));
    	rcTextUpdate();
    	rctext_.setEditable(false);
    	
    	emText_ = new StyledText(panel3, SWT.NONE );
    	GridData gridData3 = new GridData();
    	gridData3.horizontalAlignment = GridData.FILL;
    	gridData3.grabExcessHorizontalSpace = true;
    	gridData3.verticalAlignment = GridData.FILL;
    	gridData3.grabExcessVerticalSpace = true;
    	gridData3.minimumHeight = 50;
    	emText_.setLayoutData(gridData3);
    	emText_.setBackground(panel3.getBackground());
    	emText_.setEditable(false);
    	emText_.setText(errorMessage_);
    	emText_.setForeground(Activator.getDefault().getColor("red"));
    	
		return composite;
	}
	
	private final int SAVE_ID = IDialogConstants.CLIENT_ID;
	private final int SAVE_AS_ID = IDialogConstants.CLIENT_ID+1;
	protected void createButtonsForButtonBar(Composite parent) {
		saveButton_ = createButton(parent, SAVE_ID, MessageBundle.get("GrxTextItem.menu.save"),	false);
		saveAsButton_ = createButton(parent, SAVE_AS_ID, MessageBundle.get("GrxTextItem.menu.saveAs"),	false);
		saveButtonEnabled(false);
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);		
	}
	
	protected void buttonPressed(int buttonId) {
    	if (buttonId == SAVE_ID) {
    		save();
    	}else if(buttonId == SAVE_AS_ID){
    		if(setFileName())
    			save();
    	}else if(buttonId == IDialogConstants.CLOSE_ID){
    		close();
    	}
	}
	
	private class InportLabelProvider implements ITableLabelProvider{
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		public String getColumnText(Object element, int columnIndex) {
			Port node = (Port)element;
			switch(columnIndex){
			case 0:
				return node.type_;
			case 1:
				return node.name_;
			case 2:
				return node.propertyName_;
			case 3:
				return node.id_;
			case 4:
				return node.outputTime_;
			case 5:
				if(!node.controllerPortName_.equals(""))
					return node.type_.equals("IN") ? "<-- "+node.controllerPortName_ : "--> "+node.controllerPortName_;
			}
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
	}
	
	private class TableCellModifier implements ICellModifier {
		public boolean canModify(Object element, String property) {
			return true;
		}
		
		public Object getValue(Object element, String property) {
			Port port = (Port)element;
			if(property.equals("type"))
				return port.type_.equals("IN") ? 0 : 1 ;
			else if(property.equals("name"))
				return port.name_;
			else if(property.equals("property")){
				return DataTypeId.valueOf(port.propertyName_).ordinal();
			}else if(property.equals("id"))
				return port.id_;
			else if(property.equals("time"))
				return port.outputTime_;
			else if(property.equals("portName"))
				return port.controllerPortName_;
			return null;
		}
		
		public void modify(Object element, String property, Object value) {
			if (element instanceof Item) {
			      element = ((Item) element).getData();
			}
			Port port = (Port)element;
			if(property.equals("type"))
				port.type_ = (Integer)value==0 ? "IN" : "OUT";
			else if(property.equals("name"))
				port.name_ = (String)value;
			else if(property.equals("property")){
				port.propertyName_ = DataTypeId.values()[(Integer)value].name();
			}else if(property.equals("id"))
					port.id_ = (String)value;
			else if(property.equals("time"))
				port.outputTime_ = (String)value;
			else if(property.equals("portName"))
				port.controllerPortName_ = (String)value;
			saveButtonEnabled(false);
			tableviewer_.update(element, null);
		}
	}
	
	public boolean load(String fileName){
		File file = new File(fileName);
		if (file == null || !file.isFile())
			return false;
		clear();
		fileName_ = fileName;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String option="";
			boolean comline = false;
			int i=0;
			while (reader.ready()) {
				String string = reader.readLine();
				controllerBridgeFile_.add(string);
				int index = string.indexOf("openhrp-controller-bridge");
				if( index != -1 ){
					comline = true;
					string = string.substring(index+25).trim();
					commandLineStart_ = i;
				}
				if(comline){
					string = string.trim();
					if(string.endsWith(endChar_)){
						option += string.substring(0,string.length()-1);
					}
					else{
						option += string;
						commandLineEnd_ =i;
						comline = false;
					}
				}
				i++;
			}
			if(!option.equals("")){
				String[] options = option.split("--");
				rtcConfFileName_ = file.getCanonicalFile().getParent()+File.separator+"rtc.conf";
				boolean ret = loadRtcConf(rtcConfFileName_);
				parse(options);
				createPortTable();
				if(!ret){
					rtcConfFileName_="";
					rtcConfInit(nameServer_.host_, nameServer_.port_);
				}
			}else
				;// no command line
		} catch (FileNotFoundException e) {
			GrxDebugUtil.println("File Not Found. ("+file.getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean loadRtcConf(String fileName){
		File file = new File(fileName);
		if (file == null || !file.isFile())
			return false;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				String string = reader.readLine();
				rtcConfFile_.add(string);
				String string0 = string.trim();
				if(string0.startsWith("corba.nameservers:")){
					String[] s=string0.substring(18).trim().split(":");
					if(s.length == 2){
						rtcNameServer_.host_ = s[0];
						rtcNameServer_.port_ = s[1];
					}
				}else if(string.trim().startsWith("manager.modules.preload:"))
					moduleName_ = string0.substring(24).trim();
				else if(string.trim().startsWith("manager.components.precreate:"))
					controllerRtcName_ = string0.substring(29).trim();
				//else if(string.trim().startsWith("exec_cxt.periodic.type"))
				//	;
			}
		} catch (FileNotFoundException e) {
			GrxDebugUtil.println("File Not Found. ("+file.getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void init(){
		fileName_ = "";
		rtcConfFileName_ = "";
		controllerBridgeFile_.clear();
		if(os_==LINUX){
			controllerBridgeFile_.add("#!/bin/sh");
			commandLineStart_ = 1;
			commandLineEnd_ = 1;
		}else{
			commandLineStart_ = 0;
			commandLineEnd_ = 0;
		}
		controllerBridgeFile_.add("openhrp-controller-bridge "+endChar_);
		
		portTable_.clear();
		controllerName_ = "";
		robotRtcName_ = "";
		inport_.clear();
		outport_.clear();
		connection_.clear();
		nameServer_.host_ = "localhost";
		nameServer_.port_ = "2809";
		errorMessage_ = "";
		
		rtcConfInit(nameServer_.host_, nameServer_.port_);
	}
	
	private void rtcConfInit(String host, String port){
		rtcConfFile_.clear();
		rtcConfFile_.add("corba.nameservers: "+host+":"+port);
		rtcConfFile_.add("naming.formats: %n.rtc");
		rtcConfFile_.add("logger.log_level: TRACE");
		rtcConfFile_.add("exec_cxt.periodic.type: SynchExtTriggerEC");
		rtcConfFile_.add("exec_cxt.periodic.rate: 1000000");
		rtcConfFile_.add("manager.modules.load_path: .");
		rtcConfFile_.add("manager.modules.abs_path_allowed: yes");
		rtcConfFile_.add("manager.modules.preload: ");
		rtcConfFile_.add("manager.components.precreate: ");
		rtcNameServer_.host_ = host;
		rtcNameServer_.port_ = port;
		moduleName_ = "";
		controllerRtcName_ = "";

	}
	
	private void clear(){
		fileName_ = "";
		controllerBridgeFile_.clear();
		portTable_.clear();
		controllerName_ = "";
		robotRtcName_ = "";
		inport_.clear();
		outport_.clear();
		connection_.clear();
		nameServer_.host_ = "localhost";
		nameServer_.port_ = "2809";
		errorMessage_ = "";
		
		rtcConfFile_.clear();
		rtcNameServer_.host_ = "";
		rtcNameServer_.port_ = "";
		moduleName_ = "";
		controllerRtcName_ = "";
	}
	
	private void parse(String[] options){
		for(int i=0; i<options.length; i++){
			options[i] = options[i].trim();
			int endIndex = options[i].indexOf(" ");
			if(endIndex == -1)
				continue;
			String option = options[i].substring(0,endIndex);
			String parameter = options[i].substring(endIndex).trim();
			if(option.equals("server-name")){
				controllerName_ = parameter;
			}else if(option.equals("in-port")){
				Inport _inport = new Inport();
				String[] s = parameter.split(":");
				if(s.length==2){
					_inport.name_ = s[0].trim();
					_inport.propertyName_ = s[1].trim();
					_inport.id_ = null;
				}else if(s.length==3){
					_inport.name_ = s[0].trim();
					_inport.propertyName_ = s[2].trim();
					_inport.id_ = s[1].split(",");
					for(int j=0; j<_inport.id_.length; j++)
						_inport.id_[j] = _inport.id_[j].trim();
				}else
					continue;
				inport_.add(_inport);
				
			}else if(option.equals("out-port")){
				Outport _outport = new Outport();
				String[] s = parameter.split(":");
				_outport.name_ = s[0].trim();
				_outport.idName_ = null;
			    int j;
			    for(j=1; j<3; j++){
			    	try{
			    		DataTypeId type = DataTypeId.valueOf(s[j].trim());
			    		// プロパティ名 //
			    		_outport.propertyName_ = s[j].trim();
			    		j++;
			    		break;
			    	}catch(IllegalArgumentException e){
			    		 // プロパティ名でないので識別名　 //
			            if(j==2)    // 3番目までにプロパティ名がないのでエラー　//
			                break;
			            String[] _id = s[j].split(",");
			            _outport.idName_ = new String[_id.length];
			            for(int k=0; k<_id.length; k++){
			            	try{
			            		_outport.idNo_ = Integer.parseInt(_id[k]);
			            		_outport.idName_[k] = "";
			            	}catch(NumberFormatException ex){
			            		_outport.idName_[k] = _id[k].trim();
			            		_outport.idNo_ = -1;
			            	}
			            }
			    	}
			    }
			    if(j<s.length){    // まだパラメターがあるならサンプリング時間　//
			    	_outport.outputTime_ = Double.parseDouble(s[j]);
			    }else{
			        _outport.outputTime_ = -1;
			    }
				outport_.add(_outport);
			}else if(option.equals("connection")){
				String[] s = parameter.split(":");
				String controllerPortName="";
				if(s.length == 2){
					controllerPortName = controllerRtcName_+"0:"+s[1].trim();
				}else if(s.length ==3){
					controllerPortName = s[1].trim()+":"+s[2].trim();
				}
				connection_.put(s[0].trim(), controllerPortName);
			}else if(option.equals("robot-name")){
				robotRtcName_ = parameter;
			}else if(option.equals("name-server")){
				String[] s = parameter.split(":");
				nameServer_.host_ = s[0].trim();
				nameServer_.port_ = s[1].trim();
			}else if(option.equals("config-file")){
				loadConfigFile(parameter);
			}
		}
	}
	
	private void loadConfigFile(String fileName){
		File file = new File(fileName);
		if (file == null || !file.isFile())
			return;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Vector<String> options = new Vector<String>();
			while (reader.ready()) {
				String string = reader.readLine();
				String[] s = string.split("=");
				if(s.length==2)
					options.add(s[0]+" "+s[1]);
			}
			parse((String[]) options.toArray());
		
		} catch (FileNotFoundException e) {
			GrxDebugUtil.println("File Not Found. ("+file.getName()+")"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		return;
	}
	
	private void createPortTable() {
		Iterator<Inport> it = inport_.iterator();
		while(it.hasNext()){
			Inport inport = it.next();
			Port _port = new Port();
			_port.type_ = "IN";
			_port.name_ = inport.name_;
			_port.propertyName_ = inport.propertyName_;
			if(inport.id_ == null)
				_port.id_ = "All joints";
			else{
				_port.id_ = "";
				for(int i=0; i<inport.id_.length-1; i++)
					_port.id_ += inport.id_[i] + ",";
				_port.id_ += inport.id_[inport.id_.length-1];
			}
			_port.outputTime_ = "";
			String controllerPort = connection_.get(inport.name_);
			if(controllerPort==null)
				controllerPort = "";
			_port.controllerPortName_ = controllerPort;
			portTable_.add(_port);
		}
		Iterator<Outport> it0 = outport_.iterator();
		while(it0.hasNext()){
			Outport outport = it0.next();
			Port _port = new Port();
			_port.type_ = "OUT";
			_port.name_ = outport.name_;
			_port.propertyName_ = outport.propertyName_;
			if(outport.idName_ == null)
				_port.id_ = "All joints";
			else if(outport.idNo_ != -1){
				_port.id_ = String.valueOf(outport.idNo_);
			}else{
				_port.id_ = "";
				for(int i=0; i<outport.idName_.length-1; i++)
					_port.id_ += outport.idName_[i] + ",";
				_port.id_ += outport.idName_[outport.idName_.length-1];
			}
			if(outport.outputTime_<0)
				_port.outputTime_ = "control Time";
			else
				_port.outputTime_ = String.valueOf(outport.outputTime_);
			String controllerPort = connection_.get(outport.name_);
			if(controllerPort==null)
				controllerPort = "";
			_port.controllerPortName_ = controllerPort;
			portTable_.add(_port);
		}
	}

	private class Inport {
		public String name_;
		public String propertyName_;
		public String[] id_;
	}
	
	private class Outport {
		public String name_;
		public String propertyName_;
		public String[] idName_;
		public int idNo_;
		public double outputTime_;
	}

	private class Port {
		public String type_;
		public String name_;
		public String propertyName_;
		public String id_;
		public String outputTime_;
		public String controllerPortName_;
	}
	
	private class NameServer {
		public String host_;
		public String port_;
	}

	public void setRobot(GrxModelItem robot) {
		robot_ = robot;
		robotName_ = robot_.getName();
	}

	public boolean setProjectControllerName(String _controllerName) {
		projectControllerName_ = _controllerName;
		if(projectControllerName_.equals(controllerName_))
			return true;
		else
			return false;
	}
	
	public void setControllerName(String _controllerName) {
		controllerName_ = _controllerName;
	}
	
	public boolean checkNameServer(){
		if(nameServer_.host_.equals(rtcNameServer_.host_) && nameServer_.port_.equals(rtcNameServer_.port_))
			return true;
		else
			return false;
	}
	
	public void setNameServer(){
		nameServer_.host_ = rtcNameServer_.host_;
		nameServer_.port_ = rtcNameServer_.port_;
	}
	
	public void save(){
		if(fileName_.equals(""))
			if(!setFileName())
				return;
		if(rtcConfFileName_.equals("")){
			File file = new File(fileName_);
			try {
				rtcConfFileName_ = file.getCanonicalFile().getParent()+File.separator+"rtc.conf";
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName_));
			Iterator<String> it = controllerBridgeFile_.iterator();
			while(it.hasNext()){
				writer.write(it.next());
				writer.newLine();
			}
			writer.flush();
			writer.close();
			
			writer = new BufferedWriter(new FileWriter(rtcConfFileName_));
			it = rtcConfFile_.iterator();
			while(it.hasNext()){
				writer.write(it.next());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		fileNameLabel_.setText(fileName_);
		rtcConfFileNameLabel_.setText(rtcConfFileName_);
		
	}
	
	private boolean setFileName(){
		FileDialog fdlg = new FileDialog( GrxUIPerspectiveFactory.getCurrentShell(), SWT.SAVE);
		fileName_ = fdlg.open();
		if( fileName_ != null ) {
			File file = new File(fileName_);
			try {
				rtcConfFileName_ = file.getCanonicalFile().getParent()+File.separator+"rtc.conf";
				file = new File(rtcConfFileName_);
				if(file.exists()){
					if(!MessageDialog.openQuestion(getShell(), rtcConfFileName_, MessageBundle.get("Grx3DView.dialog.message.fileExist"))){
						fdlg.setFileName(rtcConfFileName_);
						rtcConfFileName_ = fdlg.open();
						if(rtcConfFileName_==null)
							return false;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}
	
	/*
	private void modifyControllerBridgeFile(String option, String oldString, String newString){
		int i=0;
		boolean find = false;
		for(i=commandLineStart_; i<commandLineEnd_+1; i++){
			String string = controllerBridgeFile_.get(i);
			int optionIndex = string.indexOf("--"+option);
			if(optionIndex!=-1){
				find = true;
				String string0 = string.substring(optionIndex+2+option.length());
				String string1 = "";
				if(!oldString.equals(""))
					string1 =string0.replace(oldString, newString);
				else
					string1 = " " + newString + " " + string0.trim();
				controllerBridgeFile_.set(i, string.substring(0,optionIndex+2+option.length())+string1);
				if(newString.equals("")){
					String s=string.substring(0,optionIndex) + string1.trim();
					if(s.equals(endChar_)){
						controllerBridgeFile_.remove(i);
						commandLineEnd_--;
					}else if(s.equals("")){
						controllerBridgeFile_.remove(i);
						if(i==commandLineEnd_ && i>1){
							String string2 = controllerBridgeFile_.get(i-1);
							if(string2.endsWith(endChar_)){
								controllerBridgeFile_.set(i-1, string2.substring(0,string2.length()-1).trim());
							}
						}
						commandLineEnd_--;
					}else{
						controllerBridgeFile_.set(i, s);
					}
				}
				break;
			}
		}
		if(!find){
			String s = controllerBridgeFile_.get(commandLineEnd_) + " " + endChar_;
			controllerBridgeFile_.set(commandLineEnd_, s);
			s = "--"+option+" "+newString;
			commandLineEnd_++;
			controllerBridgeFile_.insertElementAt(s, commandLineEnd_);
		}
		cbTextUpdate();
	}
	*/
	
	private void updateControllerBridgeFile(){
		String commandLine = controllerBridgeFile_.get(commandLineStart_);
		int index = commandLine.indexOf("openhrp-controller-bridge");
		if( index != -1 ){
			commandLine = commandLine.substring(0,index+25)+" "+endChar_;
		}
		for(int i=commandLineEnd_; i>=commandLineStart_; i--)
			controllerBridgeFile_.remove(i);
		int i = commandLineStart_;
		controllerBridgeFile_.insertElementAt(commandLine, i++);
		if(!controllerName_.equals("")){
			String string = "--server-name "+controllerName_ +" "+endChar_;
			controllerBridgeFile_.insertElementAt(string, i++);
		}
		Iterator<Port> it = portTable_.iterator();
		while(it.hasNext()){
			String string="";
			Port port = it.next();
			if(port.type_.equals("IN")){
				string += "--in-port "+port.name_;
				if(!port.id_.equals("All joints") && !port.id_.equals(""))
					string += ":"+port.id_;
				string += ":"+port.propertyName_;
			}else{
				string += "--out-port "+port.name_;
				if(!port.id_.equals("All joints") && !port.id_.equals(""))
					string += ":"+port.id_;
				string += ":"+port.propertyName_;
				if(!port.outputTime_.equals("control Time"))
					string += ":"+port.outputTime_;
			}
			string += " "+endChar_;
			controllerBridgeFile_.insertElementAt(string, i++);
			if(!port.controllerPortName_.equals("")){
				String[] s = port.controllerPortName_.split(":");
				String controllerPortName = port.controllerPortName_;
				if(s.length==2 && s[0].equals(controllerRtcName_+"0"))
					controllerPortName = s[1];
				string = "--connection "+port.name_+":"+controllerPortName+" "+endChar_;
				controllerBridgeFile_.insertElementAt(string, i++);
			}
		}
		if(!robotRtcName_.equals("")){
			String string = "--robot-name "+robotRtcName_ +" "+endChar_;
			controllerBridgeFile_.insertElementAt(string, i++);
		}
		if(!nameServer_.host_.equals("localhost") || !nameServer_.port_.equals("2809")){
			String string = "--name-server "+nameServer_.host_+":"+nameServer_.port_+" " +endChar_;
			controllerBridgeFile_.insertElementAt(string, i++);
		}

		commandLineEnd_ = i-1;
		String string = controllerBridgeFile_.get(commandLineEnd_);
		string = string.substring(0, string.length()-1);
		controllerBridgeFile_.setElementAt(string, commandLineEnd_);
	}
	
	private void updateRtcConfFile(){
		for(int i=0; i<rtcConfFile_.size(); i++){
			String string = rtcConfFile_.get(i).trim();
			if(string.startsWith("corba.nameservers:")){
				String s=string.substring(0,18)+" "+ nameServer_.host_+":"+nameServer_.port_;
				rtcConfFile_.setElementAt(s, i);
			}else if(string.startsWith("manager.modules.preload:")){
				String s=string.substring(0,24)+" "+ moduleName_;
				rtcConfFile_.setElementAt(s, i);
			}else if(string.startsWith("manager.components.precreate:")){
				String s=string.substring(0,29)+" "+ controllerRtcName_;
				rtcConfFile_.setElementAt(s, i);
			}
		}
	}
	
	private void cbTextUpdate(){
		String s="";
    	Iterator<String> it = controllerBridgeFile_.iterator();
    	while(it.hasNext())
    		s += it.next() + "\n";
    	cbtext_.setText(s);
	}
	
	private void rcTextUpdate(){
		String s="";
    	Iterator<String> it = rtcConfFile_.iterator();
    	while(it.hasNext())
    		s += it.next() + "\n";
    	rctext_.setText(s);
	}
	
	private void rrnTextUpdate(){
		if(robotRtcName_.equals(""))
			rrnText_.setText(controllerName_+"(robot)");
		else
			rrnText_.setText(robotRtcName_);
	}

	private void saveButtonEnabled(boolean b){
		saveButton_.setEnabled(b);
		saveAsButton_.setEnabled(b);
	}
	
	private void errorCheck(){
		errorMessage_ = "";
		if(!projectControllerName_.equals(controllerName_))
			errorMessage_ += MessageBundle.get("panel.Bridge.error.controlName")+"\n";
		Iterator<Port> it = portTable_.iterator();
		while(it.hasNext()){
			Port port = it.next();
			if(port.name_.equals(""))
				errorMessage_ += MessageBundle.get("panel.Bridge.error.setPortName")+"\n";
			DataTypeId.NUMID_CODE numId = DataTypeId.numId(port.propertyName_);
			if(!(numId==DataTypeId.NUMID_CODE.NOT_USED) && (port.id_.equals("All joints") || port.id_.equals("")))
				errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+" "+MessageBundle.get("panel.Bridge.error.useID")+"\n";
			if(numId==DataTypeId.NUMID_CODE.ONLY_ONE){
				String[] s = port.id_.split(",");
				if(s.length > 1)
					errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+" "+MessageBundle.get("panel.Bridge.error.oneID")+"\n";
			}
			if(numId==DataTypeId.NUMID_CODE.NUMBER){
				try{
					Integer.valueOf(port.id_);
				}catch(NumberFormatException e){
					errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+" "+MessageBundle.get("panel.Bridge.error.sensorId")+"\n";
				}
			}
			if(port.type_.equals("IN")){
				if(!DataTypeId.isInport(port.propertyName_))
					errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+" : "+port.propertyName_+MessageBundle.get("panel.Bridge.error.inportPropertyName")+"\n";
				if(!port.outputTime_.equals(""))
					errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+ MessageBundle.get("panel.Bridge.error.inportTime")+"\n";
			}else{
				if(!DataTypeId.isOutport(port.propertyName_))
					errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+" : "+port.propertyName_+MessageBundle.get("panel.Bridge.error.outportPropertyName")+"\n";
				if(!port.outputTime_.equals("control Time") && !port.outputTime_.equals("")){
					try{
						Double.valueOf(port.outputTime_);
					}catch(NumberFormatException e){
						errorMessage_ +=  MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+ MessageBundle.get("panel.Bridge.error.outportTime")+"\n";
					}
				}
			}
			if(port.controllerPortName_.startsWith("0:"))
				errorMessage_ += MessageBundle.get("panel.Bridge.errorPort")+" "+port.name_+ MessageBundle.get("panel.Bridge.error.controllerPortName")+"\n";
		}
		
		if(moduleName_.equals(""))
			errorMessage_ += MessageBundle.get("panel.Bridge.error.moduleName")+"\n";
		if(controllerRtcName_.equals(""))
			errorMessage_ += MessageBundle.get("panel.Bridge.error.controlRTCName")+"\n";
	}
	
	private class IdCellDialog extends Dialog {
		private Vector<String> links_ = new Vector<String>();
		private Vector<String> joints_ = new Vector<String>();
		private String[][] sensors_ = new String[GrxSensorItem.sensorType.length][];
		private Integer[] cameraId_ = new Integer[0];
		private String id_;
		
		private Button[] linkButtons_;
		private Button[][] sensorButtons_ = new Button[GrxSensorItem.sensorType.length][];
		private Button allJointButton_;
		
		protected IdCellDialog(Shell parentShell) {
			super(parentShell);
			Iterator<GrxLinkItem> it = robot_.links_.iterator();
			while(it.hasNext())
				links_.add(it.next().getName());
			linkButtons_ = new Button[links_.size()];
			String[] joints = robot_.getJointNames();
			for(int i=0; i<joints.length; i++)
				joints_.add(joints[i]);
    		for(int i=0; i<GrxSensorItem.sensorType.length; i++){
    			sensors_[i] = robot_.getSensorNames(GrxSensorItem.sensorType[i]);
    			if(sensors_[i] == null)
    				sensors_[i] = new String[0];
    			sensorButtons_[i] = new Button[sensors_[i].length];
    		}
    		List<GrxSensorItem> cameras_ = robot_.getSensors("Vision");
    		if(cameras_!=null){
	    		cameraId_ = new Integer[cameras_.size()];
	    		for(int i=0; i<cameras_.size(); i++)
	    			cameraId_[i] = cameras_.get(i).id_;
    		}
		}
		
		protected Control createDialogArea(Composite parent) {
	    	Composite composite = (Composite)super.createDialogArea(parent);
	    	composite.setLayout(new GridLayout(2,true));
	    	
	    	Composite panel0 = new Composite(composite,SWT.NONE);
	    	panel0.setLayoutData(new GridData(GridData.FILL_VERTICAL));
	    	panel0.setLayout(new RowLayout(SWT.VERTICAL));
	    	Label label0 = new Label(panel0, SWT.NONE);
			label0.setText("Link");
			label0.setForeground(Activator.getDefault().getColor("blue"));
			allJointButton_ = new Button(panel0, SWT.TOGGLE|SWT.CENTER);
			allJointButton_.setText("All joints");
			allJointButton_.addSelectionListener(new SelectionListener(){
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					if(allJointButton_.getSelection()){
						for(int i=0; i<links_.size(); i++){
							if(joints_.indexOf(links_.get(i)) >= 0)
								linkButtons_[i].setSelection(true);
						}
					}else{
						for(int i=0; i<links_.size(); i++){
							if(joints_.indexOf(links_.get(i)) >= 0)
								linkButtons_[i].setSelection(false);
						}
					}
				}			
			});
	    	for(int i=0; i<links_.size(); i++){
	    		linkButtons_[i] = new Button(panel0, SWT.CHECK|SWT.LEFT);
	    		linkButtons_[i].setText(links_.get(i));
	    		linkButtons_[i].addSelectionListener(new SelectionListener(){
					public void widgetDefaultSelected(SelectionEvent e) {
					}
					public void widgetSelected(SelectionEvent e) {
						boolean b = true;
						for(int i=0; i<links_.size(); i++){
							if(joints_.indexOf(links_.get(i)) >= 0)
								b = b && linkButtons_[i].getSelection();
						}	
						if(b)
							allJointButton_.setSelection(true);
						else
							allJointButton_.setSelection(false);
					}	    			
	    		});
	    	}
	    	
	    	Composite panel1 = new Composite(composite,SWT.NONE);
	    	panel1.setLayoutData(new GridData(GridData.FILL_VERTICAL));
	    	panel1.setLayout(new RowLayout(SWT.VERTICAL));
	    	for(int i=0; i<GrxSensorItem.sensorType.length; i++){
	    		Label label = new Label(panel1, SWT.NONE);
    			label.setText(GrxSensorItem.sensorType[i]);
    			label.setForeground(Activator.getDefault().getColor("blue"));
	    		for(int j=0; j<sensors_[i].length; j++){
	    			sensorButtons_[i][j] = new Button(panel1, SWT.CHECK|SWT.LEFT);
	    			String s = sensors_[i][j];
	    			if(i==0)
	    				s += " (id= "+cameraId_[j]+" )";
	    			sensorButtons_[i][j].setText(s);
	    		}
	    		Label dumy = new Label(panel1, SWT.NONE);
	    	}
	    	updateButton();
			return composite;
		}

		protected void buttonPressed(int buttonId) {
	    	if (buttonId == IDialogConstants.OK_ID) {
				Iterator<String> it = joints_.iterator();
				boolean b = true;
				while(it.hasNext())
					if(!linkButtons_[links_.indexOf(it.next())].getSelection()){
						b = false;
						break;
					}
				if(b && joints_.size()!=0){
					id_ = "All joints";
				}else{	
					id_ = "";
					for(int i=0; i<linkButtons_.length; i++)
						if(linkButtons_[i].getSelection())
							id_ += linkButtons_[i].getText() + ",";
					for(int i=1; i<sensorButtons_.length; i++)
						for(int j=0; j<sensorButtons_[i].length; j++)
							if(sensorButtons_[i][j].getSelection())
								id_ += sensorButtons_[i][j].getText() + ",";
					if(id_.length()!=0)
						id_ = id_.substring(0, id_.length()-1);
					else
						for(int i=0; i<sensorButtons_[0].length; i++)
							if(sensorButtons_[0][i].getSelection())
								id_ = cameraId_[i].toString();
				}
	    	}
	    	super.buttonPressed(buttonId);
		}

		public String getId(){
			return id_;
		}
		
		public void setId(String value) {
			id_ = value;
		}
		
		private void updateButton(){
			if(id_.equals("All joints")){
				for(int i=0; i<links_.size(); i++){
					if(joints_.indexOf(links_.get(i)) >= 0)
						linkButtons_[i].setSelection(true);
				}
				allJointButton_.setSelection(true);
			}else{
				String[] names = id_.split(",");
				for(int i=0; i<names.length; i++){
					String name = names[i].trim();
					int index = links_.indexOf(name);
					if(index >= 0){
						linkButtons_[index].setSelection(true);
						continue;
					}
					for(int j=1; j<GrxSensorItem.sensorType.length; j++){
						for(int k=0; k<sensors_[j].length; k++){
							if(sensors_[j][k].equals(name)){
								sensorButtons_[j][k].setSelection(true);
								continue;
							}
						}
					}
					try{
						int id = Integer.valueOf(name);
						for(int j=0; j<cameraId_.length; j++){
							if(id == cameraId_[j]){
								sensorButtons_[0][j].setSelection(true);
								continue;
							}
						}
					}catch(NumberFormatException e){
						;
					}
				}
				allJointButton_.setSelection(false);
			}
		}
	}
	
}

/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */

package com.generalrobotix.ui.view;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.util.GrxServerManager;
import com.generalrobotix.ui.util.GrxServerManagerPanel;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
import com.generalrobotix.ui.view.Grx3DView;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener; 
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.osgi.util.NLS;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxServerManagerView extends GrxBaseView 
    implements DisposeListener {
    public static final String TITLE = "Server Manager"; //$NON-NLS-1$
    static private final int    HORIZONTAL_INDENT = 8;
    static private final int    COLLUMN_NUM = 4;
    static private final int    NAME_SERVER_GROUP_COLLUMN_NUM = 3;
    
    private GrxServerManager serverManager_ = null;
    private GrxSimulationItem simItem_ = null;
    private Vector<TabItem> vecTabItem_      = new Vector<TabItem>();
    private TabFolder folder_;
    private Text       textNsPort_ = null;
    private Text       textNsHost_ = null;
    private Button     updateNameServerBtn_ = null;    

    public GrxServerManagerView(String name, GrxPluginManager manager_,
            GrxBaseViewPart vp, Composite parent) {
        super(name, manager_, vp, parent);
        serverManager_ = (GrxServerManager)manager_.getItem(GrxServerManager.class, null);
		if(serverManager_ != null){
			InitItems();
			serverManager_.addObserver(this);
		}
		manager_.registerItemChangeListener(this, GrxServerManager.class);
		simItem_ = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
		if(simItem_!=null){
			simItem_.addObserver(this);
		}
		manager_.registerItemChangeListener(this, GrxSimulationItem.class);
        //setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
	}
    
    // GUIの配置、イベントの設定
    private void InitItems(){
        Composite top = new Composite(composite_,SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout localGridLayout = new GridLayout( COLLUMN_NUM, false);
        localGridLayout.marginWidth = 0;
        localGridLayout.horizontalSpacing = 0;
        top.setLayout( localGridLayout );
        
        Button insertBtn = new Button(top , SWT.PUSH);
        insertBtn.setText(MessageBundle.get("GrxServerManagerView.button.insert")); //$NON-NLS-1$
        GridData insertBtnGridData = new GridData();
        insertBtnGridData.horizontalIndent = HORIZONTAL_INDENT;
        insertBtn.setLayoutData(insertBtnGridData);
        
        Button addBtn = new Button(top , SWT.PUSH);
        addBtn.setText(MessageBundle.get("GrxServerManagerView.button.add")); //$NON-NLS-1$
        GridData addBtnGridData = new GridData();
        addBtnGridData.horizontalIndent = HORIZONTAL_INDENT;
        addBtn.setLayoutData(addBtnGridData);
        
        Button delBtn = new Button(top , SWT.PUSH);
        delBtn.setText(MessageBundle.get("GrxServerManagerView.button.remove")); //$NON-NLS-1$
        GridData delBtnGridData = new GridData();
        delBtnGridData.horizontalIndent = HORIZONTAL_INDENT;
        delBtn.setLayoutData(delBtnGridData);
        
        Button defBtn = new Button(top , SWT.PUSH);
        defBtn.setText(MessageBundle.get("GrxServerManagerView.button.default")); //$NON-NLS-1$
        GridData defBtnGridData = new GridData();
        defBtnGridData.horizontalIndent = HORIZONTAL_INDENT;
        defBtn.setLayoutData(defBtnGridData);

        folder_ = new TabFolder(top,SWT.FILL);
        
        GridData folderGridData = new GridData();
        folderGridData.horizontalSpan = COLLUMN_NUM;
        folderGridData.horizontalAlignment = SWT.FILL;
        folderGridData.grabExcessHorizontalSpace = true;
        folder_.setLayoutData(folderGridData);
        
        insertBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                insertServerBtn( folder_ );
            }
        }
        );
        addBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                addServerBtn( folder_ );
            }
        }
        );
        delBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                removeServerBtn( folder_ );
            }
        }
        );
        
        defBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
            	restoreDefault();
            }
        }
        );

        GrxServerManagerPanel panel = null;
        Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();
        
        for (int i = 0; i < vecServerInfo.size() ; ++i ){
            vecTabItem_.add(new TabItem(folder_,SWT.NONE)) ;
            vecTabItem_.elementAt(i).setText(vecServerInfo.elementAt(i).id);
            
            folder_.setSelection(i);
            panel = new GrxServerManagerPanel(serverManager_, folder_ , SWT.NONE , i, manager_ );
            vecTabItem_.elementAt(i).setControl(panel);
        }
        folder_.addDisposeListener(this);
        folder_.setSelection(0);
        
        // Name Server Goup setting
        Group localGroup = new Group(top, SWT.FILL);
        localGroup.setText("Name Service");
        GridData groupGridData = new GridData();
        groupGridData.heightHint = 60;
        groupGridData.horizontalAlignment = SWT.FILL;
        groupGridData.horizontalSpan = NAME_SERVER_GROUP_COLLUMN_NUM; 
        
        localGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL |
                GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));

        GridLayout groupGridLayout = new GridLayout(NAME_SERVER_GROUP_COLLUMN_NUM, false);
        groupGridLayout.marginWidth = 2;
        groupGridLayout.marginHeight = 2;
        
        localGroup.setLayout(groupGridLayout);
        localGroup.setLayoutData(groupGridData);
        
        Label localHostLabel = new Label(localGroup, SWT.RIGHT);
        GridData hostLabelGridData = new GridData();
        hostLabelGridData.widthHint = 100;
        localHostLabel.setText(MessageBundle.get("GrxORBMonitor.label.host"));
        localHostLabel.setLayoutData(hostLabelGridData);
        
        textNsHost_ = new Text(localGroup, SWT.SINGLE | SWT.BORDER);
        GridData textNsHostGridData = new GridData();
        textNsHostGridData.widthHint = 100;
        textNsHost_.setText( String.valueOf( serverManager_.getNewHost()) );
        textNsHost_.setLayoutData(textNsHostGridData);
        textNsHost_.addModifyListener( new ModifyListener(){
            public void modifyText(ModifyEvent e){
                updateNameServerBtn_.setEnabled(!equalsHostPort());
            }
        }
        );
        updateNameServerBtn_ = new Button(localGroup , SWT.PUSH);
        GridData btnGridData = new GridData();
        btnGridData.widthHint = 64;
        btnGridData.verticalSpan = 2;
        
        updateNameServerBtn_.setText(MessageBundle.get("GrxServerManagerView.NameServer.button"));
        
        updateNameServerBtn_.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                updateNameServerBtn();
            }
        }); 
        
        updateNameServerBtn_.setEnabled(false);        
        updateNameServerBtn_.setLayoutData(btnGridData);
        
        Label localPortLabel = new Label(localGroup, SWT.RIGHT);
        GridData portLabelGridData = new GridData();
        portLabelGridData.widthHint = 100;
        localPortLabel.setText(MessageBundle.get("GrxORBMonitor.label.port"));
        localPortLabel.setLayoutData(portLabelGridData);
        
        textNsPort_ = new Text(localGroup, SWT.SINGLE | SWT.BORDER);
        GridData textNsPortGridData = new GridData();
        textNsPortGridData.widthHint = 100;
        textNsPort_.setText( String.valueOf( serverManager_.getNewPort()) );
        textNsPort_.setLayoutData(textNsPortGridData);
        
        textNsPort_.addModifyListener( new ModifyListener(){
            public void modifyText(ModifyEvent e){
                updateNameServerBtn_.setEnabled(!equalsHostPort());
            }
        }
        );
    }
    
    private void updateItems(){
    	textNsHost_.setText( String.valueOf( serverManager_.getNewHost()) );
    	textNsPort_.setText( String.valueOf( serverManager_.getNewPort()) );
    	for (int i = 0; i < vecTabItem_.size() ; ++i ){
    		vecTabItem_.elementAt(i).getControl().dispose();
    		vecTabItem_.elementAt(i).dispose();
    	}
    	vecTabItem_.clear();
    	Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();       
        for (int i = 0; i < vecServerInfo.size() ; ++i ){
            vecTabItem_.add(new TabItem(folder_,SWT.NONE)) ;
            vecTabItem_.elementAt(i).setText(vecServerInfo.elementAt(i).id);
            GrxServerManagerPanel panel = new GrxServerManagerPanel(serverManager_, folder_ , SWT.NONE , i, manager_ );
            vecTabItem_.elementAt(i).setControl(panel);
        }
        folder_.setSelection(0);
    }
    
    private void insertServerBtn(final TabFolder folder){
        Display display = Display.getCurrent();
        final Shell shell = new Shell(display , SWT.TITLE | SWT.OK);
        shell.setLayout(new GridLayout(2,false));
        shell.setText(MessageBundle.get("GrxServerManagerView.text.newServer")); //$NON-NLS-1$
        Label idLabel = new Label( shell , SWT.RIGHT);
        idLabel.setText("id:"); //$NON-NLS-1$
        final Text id = new Text( shell , SWT.BORDER);
        GridData idGridData = new GridData();
        idGridData.horizontalAlignment = SWT.FILL;
        idGridData.grabExcessHorizontalSpace = true;
        id.setLayoutData(idGridData);
        Button okBtn = new Button( shell , SWT.PUSH);
        okBtn.setText(MessageBundle.get("GrxServerManagerView.button.ok")); //$NON-NLS-1$
        Point point = display.getCursorLocation();
        shell.setLocation(point.x , point.y );
        shell.setSize(200, 100);
        shell.open();
        
        okBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                if( checkID(id.getText()) ){
                    int index = folder.getSelectionIndex();
                    if(index<0)	index=0;
                    ProcessInfo localInfo = new ProcessInfo();
                    localInfo.id = id.getText();
                    localInfo.com.clear();
                    localInfo.com.add(""); //$NON-NLS-1$
                    localInfo.args = ""; //$NON-NLS-1$
                    localInfo.autoStart = false;
                    localInfo.useORB = false;
                    localInfo.dir=serverManager_.serverInfoDefaultDir_;
                    localInfo.waitCount=serverManager_.serverInfoDefaultWaitCount_;           
                    Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();    
                    vecServerInfo.add(index,localInfo);
                    vecTabItem_.add(index , new TabItem(folder,SWT.NONE , index)) ;
                    vecTabItem_.elementAt(index).setText(vecServerInfo.elementAt(index).id);
                    folder.setSelection(index);
                    GrxServerManagerPanel panel = new GrxServerManagerPanel(serverManager_,folder , SWT.NONE , index, manager_ );
                    vecTabItem_.elementAt(index).setControl(panel);
                }
                shell.close();
            }
        }
        );

    }

    private void addServerBtn(final TabFolder folder){
        Display display = Display.getCurrent();
        final Shell shell = new Shell(display , SWT.TITLE | SWT.OK);
        shell.setLayout(new GridLayout(2,false));
        shell.setText(MessageBundle.get("GrxServerManagerView.text.addServer")); //$NON-NLS-1$
        Label idLabel = new Label( shell , SWT.RIGHT);
        idLabel.setText("id:"); //$NON-NLS-1$
        final Text id = new Text( shell , SWT.BORDER);
        GridData idGridData = new GridData();
        idGridData.horizontalAlignment = SWT.FILL;
        idGridData.grabExcessHorizontalSpace = true;
        id.setLayoutData(idGridData);
        Button okBtn = new Button( shell , SWT.PUSH);
        okBtn.setText(MessageBundle.get("GrxServerManagerView.button.ok")); //$NON-NLS-1$
        Point point = display.getCursorLocation();
        shell.setLocation(point.x , point.y );
        shell.setSize(200, 100);
        shell.open();
        okBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                if( checkID(id.getText()) ){
                    int index = folder.getSelectionIndex() + 1;
                    ProcessInfo localInfo = new ProcessInfo();
                    localInfo.id = id.getText();
                    localInfo.com.clear();
                    localInfo.com.add(""); //$NON-NLS-1$
                    localInfo.args = ""; //$NON-NLS-1$
                    localInfo.autoStart = false;
                    localInfo.useORB = false;
                    localInfo.dir=serverManager_.serverInfoDefaultDir_;
                    localInfo.waitCount=serverManager_.serverInfoDefaultWaitCount_;
                    Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();    
                    vecServerInfo.add(index,localInfo);
                    vecTabItem_.add(index , new TabItem(folder,SWT.NONE , index)) ;
                    vecTabItem_.elementAt(index).setText(vecServerInfo.elementAt(index).id);
                    folder.setSelection(index);
                    GrxServerManagerPanel panel = new GrxServerManagerPanel(serverManager_, folder , SWT.NONE , index, manager_ );
                    vecTabItem_.elementAt(index).setControl(panel);
                }
                shell.close();
            }
        }
        );

    }
    
    private void removeServerBtn(TabFolder folder){
        int index = folder.getSelectionIndex();
        Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();

        String mes = MessageBundle.get("GrxServerManagerView.dialog.message.remove"); //$NON-NLS-1$
        mes = NLS.bind(mes, new String[]{vecTabItem_.elementAt(index).getText()});
        
        // 確認ダイアログ表示後 当該 タブを削除
        if ( MessageDialog.openConfirm(
                composite_.getShell(),
                MessageBundle.get("GrxServerManagerView.dialog.title.remove"), //$NON-NLS-1$
                mes))
        {
            vecTabItem_.elementAt(index).getControl().dispose();
            folder.getItem(index).dispose();
            vecTabItem_.remove(index);
            vecServerInfo.remove(index);
        }
    }
    
    private boolean checkID(String id){
        if(id.isEmpty()){
            MessageDialog.openWarning(
                    composite_.getShell(),
                    MessageBundle.get("GrxServerManagerView.dialog.title.warning") , //$NON-NLS-1$
                    MessageBundle.get("GrxServerManagerView.dialog.message.emptyID")  ); //$NON-NLS-1$
            return false;
        }
        boolean ret = true;
        String localID = id.toUpperCase();
        for (TabItem i:vecTabItem_){
            String srcStr = i.getText().toUpperCase();
            if( srcStr.equals(localID) ){
                ret = false;
                MessageDialog.openWarning(
                        composite_.getShell(),
                        MessageBundle.get("GrxServerManagerView.dialog.title.warning") , //$NON-NLS-1$
                        MessageBundle.get("GrxServerManagerView.dialog.message.sameID")  ); //$NON-NLS-1$
                break;
            }
        }
        return ret;
    }
    
    private boolean equalsHostPort(){
        boolean ret = true;
        String port = textNsPort_.getText();
        String host = textNsHost_.getText();
        
        if( !host.equals(serverManager_.getNewHost()) ){
            ret= false;
        }
        
        try{
            if( serverManager_.getNewPort() != Integer.valueOf(port) ){
                ret = false;
            }
        }catch (NumberFormatException ex){
            ret = false;
        }catch (Exception ex){
            ex.printStackTrace();
            ret = false;
        }
        
        return ret;
    }
    
    //タブウィンドウが閉じるときGUIによって生じた変更を反映する。
    private void storevecServerInfo(){       
        for (int i = 0; i < vecTabItem_.size(); ++i){
            GrxServerManagerPanel localPanel = (GrxServerManagerPanel) vecTabItem_.elementAt(i).getControl();
            localPanel.updateProcessInfo();
        }
    }
    
    private void updateNameServerBtn(){
        StringBuffer refHost = new StringBuffer("");
        StringBuffer refPort = new StringBuffer("");
        
        boolean ret = MessageDialog.openQuestion(composite_.getShell(),
                MessageBundle.get("GrxServerManagerView.dialog.infomation.tittle.updateNameServer"),
                MessageBundle.get("GrxServerManagerView.dialog.infomation.message.updateNameServer"));
        
        if(ret){
            String message = serverManager_.setNewHostPort(textNsHost_.getText(), textNsPort_.getText(), refHost, refPort );
            // TODO 
            if(message.isEmpty()){
                updateNameServerBtn_.setEnabled(false);
                restartServers();
            }else{
                MessageDialog.openError(composite_.getShell(),
                        MessageBundle.get("GrxServerManagerView.dialog.error.tittle.updateNameServer"),
                        message);
                if( refHost.length() != 0){
                    textNsHost_.setText(refHost.toString());
                }
                if( refPort.length() != 0 ){
                    textNsPort_.setText(refPort.toString());
                }
            }
        }
    }

    private void restartServers(){
        try {
            IRunnableWithProgress iProgress = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(MessageBundle.get("GrxServerManagerView.dialog.title.progress"),
                            2 + ( serverManager_.getServerInfo().size() + 1) * 2); //$NON-NLS-1$
                    restartServers(monitor);
                    monitor.done();
                }
            };
            new ProgressMonitorDialog(GrxUIPerspectiveFactory.getCurrentShell()).run(false, false, iProgress);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
        }
        
        
    }

    private void restartServers(IProgressMonitor monitor) throws InterruptedException{
        Grx3DView dview =  (Grx3DView)manager_.getView( Grx3DView.class, false );
        if(dview != null){
            dview.unregisterCORBA();
        }
        if(simItem_ != null){
            simItem_.unregisterCORBA();
        }
        monitor.worked(1);
        serverManager_.restart(monitor);
        monitor.worked(1);
        if(simItem_ != null){
            simItem_.registerCORBA();
        }
        if(dview != null){
            dview.registerCORBA();
        }
    }
    
    private void restoreDefault(){
    	serverManager_.restoreDefault();
    	updateItems();
    }
    
    public void registerItemChange(GrxBaseItem item, int event){
		if(item instanceof GrxServerManager){
			GrxServerManager serverManager = (GrxServerManager) item;
	    	switch(event){
	    	case GrxPluginManager.ADD_ITEM:
	    		if(serverManager_ == null){
	    			serverManager_ = serverManager;
	    			updateItems();
	    			serverManager_.addObserver(this);
	    		}
	    		break;
	    	default:
	    		break;
	    	}
		}else if(item instanceof GrxSimulationItem){
    		GrxSimulationItem simItem = (GrxSimulationItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(simItem_!=simItem){
    				simItem_ = simItem;
    				simItem_.addObserver(this);
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(simItem_==simItem){
	    			simItem_.deleteObserver(this);
	    			simItem_ = null;
	    		}
	    		break;
	    	default:
	    		break;
    		}
    	}
	}
    
    public void widgetDisposed(DisposeEvent e)
    {
        storevecServerInfo();
    }
    
    public void update(GrxBasePlugin plugin, Object... arg) {
    	if(serverManager_==plugin){
    		if((String)arg[0]=="ProcessEnd"){
    			for (TabItem i: vecTabItem_){
    	            if(i.getText().equals((String)arg[1])){
    	            	((GrxServerManagerPanel)i.getControl()).setStartText();
    	            }
    			}
    		}
    	}else if(simItem_==plugin){
    		if((String)arg[0]=="StartSimulation"){
    			setEditableHostPortText(false);
    		}else if((String)arg[0]=="StopSimulation"){
    			setEditableHostPortText(true);
    		}
    	}
    }
    
    public void shutdown() {
        manager_.removeItemChangeListener(this, GrxServerManager.class);
        if(serverManager_!=null)
        	serverManager_.deleteObserver(this);
        serverManager_.SaveServerInfo();
        if(simItem_!=null)
			simItem_.deleteObserver(this);
		manager_.removeItemChangeListener(this, GrxSimulationItem.class);
	}

    public void setEditableHostPortText(boolean bEnable){
        textNsHost_.setEditable(bEnable);
        textNsPort_.setEditable(bEnable);
    }
}

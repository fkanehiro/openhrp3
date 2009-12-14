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

import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxServerManager;
import com.generalrobotix.ui.util.GrxServerManagerConfigXml;
import com.generalrobotix.ui.util.GrxServerManagerPanel;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GrxServerManagerView extends GrxBaseView 
    implements DisposeListener {
    public static final String TITLE = "Server Manager"; //$NON-NLS-1$
    static private final int    HORIZONTAL_INDENT = 8;
    static private final int    COLLUMN_NUM = 3;

    private GrxServerManager serverManager_ = null;
    private Vector<TabItem> vecTabItem_      = new Vector<TabItem>();
    
    public GrxServerManagerView(String name, GrxPluginManager manager_,
            GrxBaseViewPart vp, Composite parent) {
        super(name, manager_, vp, parent);
        serverManager_ = new GrxServerManager();
        InitItems();
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

        final TabFolder folder = new TabFolder(top,SWT.FILL);
        
        GridData folderGridData = new GridData();
        folderGridData.horizontalSpan = COLLUMN_NUM;
        folderGridData.horizontalAlignment = SWT.FILL;
        folderGridData.grabExcessHorizontalSpace = true;
        folder.setLayoutData(folderGridData);
        
        insertBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                insertServerBtn( folder );
            }
        }
        );
        addBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}
            public void widgetSelected(SelectionEvent e) {
                addServerBtn( folder );
            }
        }
        );
        delBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                removeServerBtn( folder );
            }
        }
        );

        GrxServerManagerPanel panel = null;
        Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();
        
        for (int i = 0; i < vecServerInfo.size() ; ++i ){
            vecTabItem_.add(new TabItem(folder,SWT.NONE)) ;
            vecTabItem_.elementAt(i).setText(vecServerInfo.elementAt(i).id);
            
            folder.setSelection(i);
            panel = new GrxServerManagerPanel(serverManager_, folder , SWT.NONE , i );
            vecTabItem_.elementAt(i).setControl(panel);
        }
        folder.addDisposeListener(this);
        folder.setSelection(0);
        
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
                if( isExistenceID(id.getText()) ){
                    MessageDialog.openWarning(
                            composite_.getShell(),
                            MessageBundle.get("GrxServerManagerView.dialog.title.warning") , //$NON-NLS-1$
                            MessageBundle.get("GrxServerManagerView.dialog.message.sameID")  ); //$NON-NLS-1$
                }else{
                    int index = folder.getSelectionIndex();
                    ProcessInfo localInfo = new ProcessInfo();
                    localInfo.id = id.getText();
                    localInfo.com.clear();
                    localInfo.com.add(""); //$NON-NLS-1$
                    localInfo.args = ""; //$NON-NLS-1$
                    localInfo.autoStart = false;
                    localInfo.useORB = false;
                    Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();    
                    vecServerInfo.add(index,localInfo);
                    vecTabItem_.add(index , new TabItem(folder,SWT.NONE , index)) ;
                    vecTabItem_.elementAt(index).setText(vecServerInfo.elementAt(index).id);
                    GrxServerManagerConfigXml.insertServerNode(vecServerInfo.elementAt(index+1),localInfo);
                    folder.setSelection(index);
                    GrxServerManagerPanel panel = new GrxServerManagerPanel(serverManager_,folder , SWT.NONE , index );
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
                if( isExistenceID(id.getText()) ){
                    MessageDialog.openWarning(
                            composite_.getShell(),
                            MessageBundle.get("GrxServerManagerView.dialog.title.warning") , //$NON-NLS-1$
                            MessageBundle.get("GrxServerManagerView.dialog.message.sameID")  ); //$NON-NLS-1$
                }else{
                    int index = folder.getSelectionIndex() + 1;
                    ProcessInfo localInfo = new ProcessInfo();
                    localInfo.id = id.getText();
                    localInfo.com.clear();
                    localInfo.com.add(""); //$NON-NLS-1$
                    localInfo.args = ""; //$NON-NLS-1$
                    localInfo.autoStart = false;
                    localInfo.useORB = false;
                    Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();    
                    vecServerInfo.add(index,localInfo);
                    vecTabItem_.add(index , new TabItem(folder,SWT.NONE , index)) ;
                    vecTabItem_.elementAt(index).setText(vecServerInfo.elementAt(index).id);
                    GrxServerManagerConfigXml.addServerNode(vecServerInfo.elementAt(index-1),localInfo);
                    folder.setSelection(index);
                    GrxServerManagerPanel panel = new GrxServerManagerPanel(serverManager_, folder , SWT.NONE , index );
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

        // 確認ダイアログ表示後 当該 タブを削除
        if ( MessageDialog.openConfirm(
                composite_.getShell(),
                MessageBundle.get("GrxServerManagerView.dialog.title.remove") + vecTabItem_.elementAt(index).getText(), //$NON-NLS-1$
                MessageBundle.get("GrxServerManagerView.dialog.message.remove") + vecTabItem_.elementAt(index).getText() +" ?"  )) //$NON-NLS-1$ //$NON-NLS-2$
        {
            vecTabItem_.elementAt(index).getControl().dispose();
            folder.getItem(index).dispose();
            vecTabItem_.remove(index);
            GrxServerManagerConfigXml.deleteServerNode(vecServerInfo.elementAt(index));
            vecServerInfo.remove(index);
        }
    }
    
    private boolean isExistenceID(String id){
        boolean ret = false;
        String localID = id.toUpperCase();
        for (TabItem i:vecTabItem_){
            String srcStr = i.getText().toUpperCase();
            if( srcStr.equals(localID) ){
                ret = true;
                break;
            }
        }
        return ret;
    }
    
    //タブウィンドウが閉じるときGUIによって生じた変更を反映する。
    private void storevecServerInfo()throws Exception{
        Vector<ProcessInfo> vecServerInfo = serverManager_.getServerInfo();        
        for (int i = 0; i < vecTabItem_.size(); ++i){
            GrxServerManagerPanel localPanel = (GrxServerManagerPanel) vecTabItem_.elementAt(i).getControl();
            localPanel.updateProcessInfo(vecServerInfo.elementAt(i));
        }
        GrxServerManager.SaveServerInfo();
    }
    

    
    public void widgetDisposed(DisposeEvent e)
    {
        // TODO 自動生成されたメソッド・スタブ
        try{
            storevecServerInfo();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

}

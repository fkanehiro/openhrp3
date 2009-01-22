/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */

package com.generalrobotix.ui.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.swt.custom.*;

import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.util.GrxServerManagerConfigXml;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
//import com.generalrobotix.ui.view.simulation.CollisionPairPanel.InnerTableLabelProvider;
import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxServerManagerPanel;

@SuppressWarnings("serial")
public class GrxServerManager extends Composite{
    public static String homePath_;
    
    enum  GrxServer{
      COLLISION_DETECTOR,
      DYNAMIS_SIMULATOR,
      MODEL_LOADER,
      NAME_SERVER
    };
    static public final String LINUX_TMP_DIR = System.getenv( "HOME" ) + File.separator + ".OpenHRP-3.1" + File.separator;
    static public final String WIN_TMP_DIR = System.getenv( "APPDATA" ) + File.separator + "OpenHRP-3.1" + File.separator;
    static private final String CONFIG_XML = "grxuirc.xml";

    static volatile public Vector<ProcessInfo> vecServerInfo = new Vector<ProcessInfo>();
    static volatile private boolean bInitializedServerInfo = false;
    static volatile private String strTempDir = null;
    
    static private synchronized void InitServerInfo(){
        if( !bInitializedServerInfo ){
            File fileXml = getConfigXml();
            loadConfigXml(fileXml);
            bInitializedServerInfo = true;
        }
    }
    
    //XMLファイルの読み込みとlistServerInfoの初期化
    static private void loadConfigXml(File fileXml){
        GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml(fileXml);
        vecServerInfo.clear();
        for (int i = 0; ; ++i){
            ProcessInfo localInfo = localXml.getServerInfo( i );
            if ( localInfo == null ){
                break;
            }
            if ( localInfo.id.equals(""))
            	continue ;
            vecServerInfo.add( localInfo );
        }
        GrxDebugUtil.print(fileXml.getPath() + "\n");
        GrxDebugUtil.print(fileXml.getParent() + "\n");
    }

    //Xmlファイルへ次回起動のための値を保存
    static public synchronized void ShutdownServerInfo(){
        if( bInitializedServerInfo ){
            GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml( getConfigXml() );
            for (int i = 0; i < vecServerInfo.size(); ++i){
                localXml.setServerNode( vecServerInfo.elementAt(i) );
            }
            localXml.SaveServerInfo();
        }
    }
    
    //Xmlファイルハンドルの取得
    static private File getConfigXml(){
        File ret = null;
        if (System.getProperty("os.name").equals("Linux")||System.getProperty("os.name").equals("Mac OS X")) {
            strTempDir = LINUX_TMP_DIR;
        } else { //Windows と　仮定
            strTempDir = WIN_TMP_DIR;
        }
        ret = new File(strTempDir, CONFIG_XML);
        return ret;
    }
    
    public static Vector<Button> vecButton = new Vector<Button>();
    public static Vector<Button> vecChkBox = new Vector<Button>();
    public static Vector<Button> vecUseORBChkBox = new Vector<Button>();
    public static Vector<Text> vecPathText = new Vector<Text>();
    public static Vector<Text> vecArgsText = new Vector<Text>();
    public static Vector<TabItem> vecTabItem = new Vector<TabItem>();
    

    /**
     * GrxServerManagerを作り、処理を開始する。
     */
	public GrxServerManager(Composite parent, int style) {
		super( parent, style);
        this.setLayout(new GridLayout(1,false));
        InitServerInfo();

        Composite top = new Composite(this,SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        top.setLayout(new RowLayout() );
        Button insertBtn = new Button(top , SWT.PUSH);
        insertBtn.setText("Insert");
        Button addBtn = new Button(top , SWT.PUSH);
        addBtn.setText("Add");
        Button delBtn = new Button(top , SWT.PUSH);
        delBtn.setText("Remove");
/*
        Button forwordBtn = new Button(top , SWT.PUSH);
        forwordBtn.setText("<<");
        Button backBtn = new Button(top , SWT.PUSH);
        backBtn.setText(">>");
*/
        final TabFolder folder = new TabFolder(this,SWT.NONE);

        insertBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                insertServerBtn( folder );
            }
        }
        );
        addBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                addServerBtn( folder );
            }
        }
        );
        delBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                removeServerBtn( folder );
            }
        }
        );

        GrxServerManagerPanel panel ;
        for (int i = 0; i < vecServerInfo.size() ; ++i ){
            vecTabItem.add(new TabItem(folder,SWT.NONE)) ;
            vecTabItem.elementAt(i).setText(vecServerInfo.elementAt(i).id);
            folder.setSelection(i);
            panel = new GrxServerManagerPanel(folder , SWT.NONE , i );
            vecTabItem.elementAt(i).setControl(panel);
        }
        folder.setSelection(0);
		String dir = System.getenv("ROBOT_DIR");
		if (dir != null && new File(dir).isDirectory())
			homePath_ = dir+File.separator;
		else
			homePath_ = System.getProperty( "user.home", "" )+File.separator;
        
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                try{
                    storevecServerInfo();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        );
    }
    
    private void insertServerBtn(final TabFolder folder){
    	Display display = Display.getCurrent();
    	final Shell shell = new Shell(display , SWT.TITLE | SWT.OK);
    	shell.setLayout(new GridLayout(2,false));
    	shell.setText("Insert New Server");
    	Label idLabel = new Label( shell , SWT.RIGHT);
    	idLabel.setText("id:");
    	final Text id = new Text( shell , SWT.BORDER);
    	Button okBtn = new Button( shell , SWT.PUSH);
    	okBtn.setText("OK");
    	Point point = display.getCursorLocation();
    	shell.setLocation(point.x , point.y );
    	shell.setSize(200, 100);
    	shell.open();
        okBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
            	int index = folder.getSelectionIndex();
            	ProcessInfo localInfo = new ProcessInfo();
            	localInfo.id = id.getText();
                localInfo.com.clear();
                localInfo.com.add("");
                localInfo.args = "";
                localInfo.autoStart = false;
                localInfo.useORB = false;

            	vecServerInfo.add(index,localInfo);
                vecTabItem.add(index , new TabItem(folder,SWT.NONE , index)) ;
                vecTabItem.elementAt(index).setText(vecServerInfo.elementAt(index).id);
            	GrxServerManagerConfigXml.insertServerNode(vecServerInfo.elementAt(index+1),localInfo);
                folder.setSelection(index);
                GrxServerManagerPanel panel = new GrxServerManagerPanel(folder , SWT.NONE , index );
                vecTabItem.elementAt(index).setControl(panel);
                shell.close();
            }
        }
        );

    }

    private void addServerBtn(final TabFolder folder){
    	Display display = Display.getCurrent();
    	final Shell shell = new Shell(display , SWT.TITLE | SWT.OK);
    	shell.setLayout(new GridLayout(2,false));
    	shell.setText("Add New Server");
    	Label idLabel = new Label( shell , SWT.RIGHT);
    	idLabel.setText("id:");
    	final Text id = new Text( shell , SWT.BORDER);
    	Button okBtn = new Button( shell , SWT.PUSH);
    	okBtn.setText("OK");
    	Point point = display.getCursorLocation();
    	shell.setLocation(point.x , point.y );
    	shell.setSize(200, 100);
    	shell.open();
        okBtn.addSelectionListener(new SelectionListener(){
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
            	int index = folder.getSelectionIndex() + 1;
            	ProcessInfo localInfo = new ProcessInfo();
            	localInfo.id = id.getText();
                localInfo.com.clear();
                localInfo.com.add("");
                localInfo.args = "";
                localInfo.autoStart = false;
                localInfo.useORB = false;

            	vecServerInfo.add(index,localInfo);
                vecTabItem.add(index , new TabItem(folder,SWT.NONE , index)) ;
                vecTabItem.elementAt(index).setText(vecServerInfo.elementAt(index).id);
            	GrxServerManagerConfigXml.addServerNode(vecServerInfo.elementAt(index-1),localInfo);
                folder.setSelection(index);
                GrxServerManagerPanel panel = new GrxServerManagerPanel(folder , SWT.NONE , index );
                vecTabItem.elementAt(index).setControl(panel);
                shell.close();
            }
        }
        );

    }
    
    private void removeServerBtn(TabFolder folder){
    	int index = folder.getSelectionIndex();
    	vecTabItem.elementAt(index).getControl().dispose();
    	folder.getItem(index).dispose();
    	vecTabItem.remove(index);
    	GrxServerManagerConfigXml.deleteServerNode(vecServerInfo.elementAt(index));
    	vecServerInfo.remove(index);
    	vecButton.remove(index);
    	vecChkBox.remove(index);
    	vecUseORBChkBox.remove(index);
    	vecPathText.remove(index);
    	vecArgsText.remove(index);
    	
    }
    
    
    //タブウィンドウが閉じるときGUIによって生じた変更を反映する。
    private void storevecServerInfo()throws Exception{
        for (int i = 0; i < vecServerInfo.size(); ++i){
            vecServerInfo.elementAt(i).com.clear();
            vecServerInfo.elementAt(i).com.add(vecPathText.elementAt(i).getText());
            vecServerInfo.elementAt(i).args = vecArgsText.elementAt(i).getText();
            vecServerInfo.elementAt(i).autoStart = vecChkBox.elementAt(i).getSelection();
            vecServerInfo.elementAt(i).useORB = vecUseORBChkBox.elementAt(i).getSelection();
        }
    }
}

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;

import jp.go.aist.hrp.simulator.ServerObject;
import jp.go.aist.hrp.simulator.ServerObjectHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxServerManagerConfigXml;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

@SuppressWarnings("serial")
public class GrxServerManager extends Composite{
    enum  GrxServer{
      COLLISION_DETECTOR,
      DYNAMIS_SIMULATOR,
      MODEL_LOADER,
      NAME_SERVER
    };
    static public final String LINUX_TMP_DIR = System.getenv( "HOME" ) + File.separator ;
    static private final String START = "Start";
    static private final String STOP = "Stop";
    static private final String CONFIG_XML = "grxuirc.xml";
    static private final String DIM_SERVERS[] = {
        "CollisionDetectorFactory", "DynamicsSimulatorFactory", "ModelLoader", "NameService"
        };
    static private final int HEIGHT_HINT = 26;
    static private final int HORIZON_INDENT = 4;
    static private final int LABEL_LENGTH = 32;
    static private final int TEXT_LENGTH = 256;

    static volatile private Vector<ProcessInfo> vecServerInfo = new Vector<ProcessInfo>();
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
        for (int i = 0; i < GrxServerManager.DIM_SERVERS.length; ++i){
            ProcessInfo localInfo = localXml.getServerInfo( GrxServerManager.DIM_SERVERS[i]);
            if ( localInfo == null ){
                localInfo = new ProcessInfo();
                localInfo.id = GrxServerManager.DIM_SERVERS[i];
            }
            vecServerInfo.add( localInfo );
        }
        GrxDebugUtil.print(fileXml.getPath() + "\n");
        GrxDebugUtil.print(fileXml.getParent() + "\n");
    }

    //Xmlファイルへ次回起動のための値を保存
    static public synchronized void ShutdownServerInfo(){
        if( bInitializedServerInfo ){
            GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml( getConfigXml() );
            for (int i = 0; i < GrxServerManager.DIM_SERVERS.length; ++i){
                // i=2の時ServerInfoのnameが設定されていないことのエラーがある
                localXml.setServerNode( vecServerInfo.elementAt(i) );
            }
            localXml.SaveServerInfo();
        }
    }
    
    //Xmlファイルハンドルの取得
    static private File getConfigXml(){
        File ret = null;
        if (System.getProperty("os.name").equals("Linux")) {
            strTempDir = LINUX_TMP_DIR;
        } else { //Windows と　仮定
            strTempDir = new String( System.getenv("TEMP") );
        }
        ret = new File(strTempDir, CONFIG_XML);
        return ret;
    }
    
    private Vector<Button> vecButton = new Vector<Button>();
    private Vector<Button> vecChkBox = new Vector<Button>();
    private Vector<Text> vecPathText = new Vector<Text>();
    private Vector<Text> vecArgsText = new Vector<Text>();
    
    /**
     * GrxServerManagerを作り、処理を開始する。
     */
	public GrxServerManager(Composite parent, int style) {
		super( parent, style);
        this.setLayout(new GridLayout(1,false));
        InitServerInfo();
        for (int i = 0; i < GrxServer.values().length; ++i ){
            InitPanels(GrxServer.values()[i]);
        }
        
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
    
    /**
     * プロセス設定用のパネルを作る。
     */
    private void InitPanels(GrxServer enLocal){
        //雛形部分の作成
        int localDim = enLocal.ordinal();
        Composite localPanel = new Composite(this,SWT.NONE);
        localPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));
        
        GridLayout localGridLayout = new GridLayout(3, false);
        
        localGridLayout.marginWidth = 0;
        localGridLayout.horizontalSpacing = 0;
        localPanel.setLayout( localGridLayout );
        
        Group localGroup = new Group(localPanel, SWT.NONE);
        GridData localGroupData = new GridData(GridData.FILL_HORIZONTAL);
        //GridData localGroupData = new GridData();
        localGroup.setLayoutData( localGroupData );
        localGroup.setLayout(localGridLayout);
        
        Label localLabelPath = new Label(localGroup, SWT.RIGHT);
        localLabelPath.setText("Path: ");
        GridData labelPathGridData = new GridData();
        //labelGridData.verticalIndent = 2;
        labelPathGridData.widthHint = LABEL_LENGTH;
        
        Text localPath = new Text(localGroup, SWT.BORDER);
        GridData pathGridData = new GridData(GridData.FILL_HORIZONTAL);
        pathGridData.widthHint = TEXT_LENGTH;
        localPath.setText( vecServerInfo.elementAt(localDim).com.get(0));
        //pathGridData.heightHint = this.HEIGHT_HINT;
        
        Button localRefButton = new Button(localGroup, SWT.PUSH);
        localRefButton.setText("...");
        GridData refbtnGridData = new GridData();
        refbtnGridData.horizontalIndent = HORIZON_INDENT;
        //refbtnGridData.heightHint = this.HEIGHT_HINT;
        
        Label localLabelArgs = new Label(localGroup, SWT.RIGHT);
        localLabelArgs.setText("Args: ");
        GridData labelArgsGridData = new GridData();
        //labelGridData.verticalIndent = 2;
        labelArgsGridData.widthHint = LABEL_LENGTH;
        
        Text localArgs = new Text(localGroup, SWT.BORDER);
        GridData argsGridData = new GridData(GridData.FILL_HORIZONTAL);
        //argsGridData.horizontalIndent = HORIZON_INDENT;
        argsGridData.widthHint = TEXT_LENGTH;
        localArgs.setText( vecServerInfo.elementAt(localDim).args );
        //argsGridData.heightHint = this.HEIGHT_HINT;
        
        Button localButton = new Button(localGroup, SWT.PUSH);
        if(vecServerInfo.elementAt(localDim).autoStart)
        {
            localButton.setText(STOP);
        }
        else
        {
            localButton.setText(START);
        }
        GridData btnGridData = new GridData();
        btnGridData.horizontalIndent = HORIZON_INDENT;
        //btnGridData.heightHint = this.HEIGHT_HINT;
        btnGridData.widthHint = 64;

        Label localLabelAuto = new Label(localGroup, SWT.RIGHT | SWT.FILL);
        localLabelAuto.setText("Automatic start ");
        GridData labelAutoGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelAutoGridData.horizontalSpan = 2;
        //labelGridData.verticalIndent = 2;
        labelAutoGridData.widthHint = LABEL_LENGTH + TEXT_LENGTH;
        
        Button localChkBox = new Button(localGroup, SWT.CHECK);
        GridData chkbtnGridData = new GridData();
        chkbtnGridData.horizontalIndent = HORIZON_INDENT;
        localChkBox.setSelection( vecServerInfo.elementAt(localDim).autoStart );
        //btnGridData.heightHint = this.HEIGHT_HINT;
        
        //各サーバ分GUIパーツの独自設定
        //Groupタイトル名設定
        String strServerName = GrxServerManager.DIM_SERVERS[localDim];
        localGroup.setText(strServerName + " ");

        //リスナー登録
        switch ( enLocal ){
            case DYNAMIS_SIMULATOR:
                localRefButton.addSelectionListener(new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateRefBtn( GrxServerManager.GrxServer.DYNAMIS_SIMULATOR );
                    }
                }
                );
                
                localButton.addSelectionListener(new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateBtn( GrxServerManager.GrxServer.DYNAMIS_SIMULATOR );
                    }
                }
                );
                break;
            case COLLISION_DETECTOR:
                localRefButton.addSelectionListener(new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateRefBtn( GrxServerManager.GrxServer.COLLISION_DETECTOR );
                    }
                }
                );
                localButton.addSelectionListener(new SelectionListener(){

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateBtn( GrxServerManager.GrxServer.COLLISION_DETECTOR );
                    }
                }
                );
                break;
            case MODEL_LOADER:
                localRefButton.addSelectionListener(new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateRefBtn( GrxServerManager.GrxServer.MODEL_LOADER );
                    }
                }
                );
                localButton.addSelectionListener(new SelectionListener(){

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateBtn( GrxServerManager.GrxServer.MODEL_LOADER );
                    }
                }
                );
                break;
            case NAME_SERVER:
                localRefButton.addSelectionListener(new SelectionListener(){
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateRefBtn( GrxServerManager.GrxServer.NAME_SERVER );
                    }
                }
                );
                localButton.addSelectionListener(new SelectionListener(){

                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        updateBtn( GrxServerManager.GrxServer.NAME_SERVER );
                    }
                }
                );
                break;
        }
        localLabelPath.setLayoutData(labelPathGridData);
        localPath.setLayoutData(pathGridData);
        localRefButton.setLayoutData(refbtnGridData);
        localLabelArgs.setLayoutData(labelArgsGridData);
        localArgs.setLayoutData(argsGridData);
        localButton.setLayoutData(btnGridData);
        localLabelAuto.setLayoutData(labelAutoGridData);
        localChkBox.setLayoutData(chkbtnGridData);
        vecButton.add( localButton );
        vecChkBox.add( localChkBox );
        vecPathText.add( localPath );
        vecArgsText.add( localArgs );
    }
    
    //ファイル参照用のモーダルダイアログを開く
    private void updateRefBtn(GrxServer enLocal){
        //ファイル名の取得
        String[] filterNames = new String[] { "すべてのファイル(*)" };
        String[] filterExtensions = new String[] { "*" };
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        FileDialog fileDlg = new FileDialog( window.getShell(), SWT.OPEN);
        
        fileDlg.setText("");
        //fileDlg.setFilterPath("C:/");
        fileDlg.setFilterNames(filterNames);
        fileDlg.setFilterExtensions(filterExtensions);

        String strServerName = GrxServerManager.DIM_SERVERS[enLocal.ordinal()];
        fileDlg.setText( "Select " + strServerName );
        vecPathText.elementAt(enLocal.ordinal()).setText(fileDlg.open());
    }

    //プロセス開始
    private void updateBtn(GrxServer enLocal){
        int localDim = enLocal.ordinal();
        Composite output = null;
        GrxProcessManager pm = GrxProcessManager.getInstance(output);
        ProcessInfo pi = new ProcessInfo();
        
        if( vecButton.elementAt(localDim).getText().equals(START))
        {
            pi.id = vecServerInfo.elementAt(localDim).id ;
            pi.args = vecArgsText.elementAt(localDim).getText();
            pi.com.add( vecPathText.elementAt(localDim).getText() + " " + pi.args);
            pm.register(pi);
            GrxProcessManager.AProcess p = pm.get(pi.id);
            if( p.start(null)){
                vecButton.elementAt(localDim).setText(STOP);
            }
        }
        else
        {
            pi.id = vecServerInfo.elementAt(localDim).id ;
            pi.args = vecArgsText.elementAt(localDim).getText();
            pi.com.add( vecPathText.elementAt(localDim).getText() + " " + pi.args);
            GrxProcessManager.AProcess p = pm.get(pi.id);
            if( p.stop()){
            	vecButton.elementAt(localDim).setText(START);
                pm.unregister(pi.id);
            }
        }
    }
    
    //タブウィンドウが閉じるときGUIによって生じた変更を反映する。
    private void storevecServerInfo()throws Exception{
        for (int i = 0; i < GrxServerManager.DIM_SERVERS.length; ++i){
            vecServerInfo.elementAt(i).com.clear();
            vecServerInfo.elementAt(i).com.add(vecPathText.elementAt(i).getText());
            vecServerInfo.elementAt(i).args = vecArgsText.elementAt(i).getText();
            vecServerInfo.elementAt(i).autoStart = vecChkBox.elementAt(i).getSelection();
        }
    }
}

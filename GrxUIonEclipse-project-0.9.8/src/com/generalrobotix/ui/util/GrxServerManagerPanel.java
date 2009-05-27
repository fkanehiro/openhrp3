/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * CollisionPairPanel.java
 *
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

@SuppressWarnings("serial")
public class GrxServerManagerPanel extends Composite {

    static private final int    HORIZON_INDENT_ = 4;
    static private final int    LABEL_LENGTH_   = 32;
    static private final int    TEXT_LENGTH_    = 256;
    static private final String START_          = "Start";
    static private final String STOP_           = "Stop";
    
    private Button  toggleButton_ = null; // Start <-> Stop Button
    private Button  autoChkBox_   = null; // Automatic start process flag
    private Button  useORBChkBox_ = null;
    private Text    pathText_     = null;
    private Text    argsText_     = null;
    private GrxServerManager serverManager_ = null;
    
    private String  pathStr_ = null;
    private String  argsStr_ = null;
    private boolean bUseORB_;
    private boolean bAuto_;
    
    public GrxServerManagerPanel(GrxServerManager manager, TabFolder parent, int style, int index) {
        super(parent, style);
        serverManager_ = manager;
        final int localDim = parent.getSelectionIndex();
        Composite localPanel = this;
        localPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL |
                GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));

        GridLayout localGridLayout = new GridLayout(3, false);

        localGridLayout.marginWidth = 0;
        localGridLayout.horizontalSpacing = 0;
        localPanel.setLayout(localGridLayout);

        Label localLabelPath = new Label(localPanel, SWT.RIGHT);
        localLabelPath.setText("Path: ");
        GridData labelPathGridData = new GridData();
        labelPathGridData.widthHint = LABEL_LENGTH_;

        pathText_ = new Text(localPanel, SWT.BORDER);
        GridData pathGridData = new GridData(GridData.FILL_HORIZONTAL);
        pathGridData.widthHint = TEXT_LENGTH_;
        pathText_.setText(GrxServerManager.vecServerInfo.elementAt(localDim).com.get(0));
        pathStr_ = GrxServerManager.vecServerInfo.elementAt(localDim).com.get(0);
        pathText_.addModifyListener( new ModifyListener() {
            public void modifyText(ModifyEvent e){
                pathStr_ = new String( pathText_.getText().trim());
            }
        });
        
        Button localRefButton = new Button(localPanel, SWT.PUSH);
        localRefButton.setText("...");
        GridData refbtnGridData = new GridData();
        refbtnGridData.horizontalIndent = HORIZON_INDENT_;

        Label localLabelArgs = new Label(localPanel, SWT.RIGHT);
        localLabelArgs.setText("Args: ");
        GridData labelArgsGridData = new GridData();
        labelArgsGridData.widthHint = LABEL_LENGTH_;

        argsText_ = new Text(localPanel, SWT.BORDER);
        GridData argsGridData = new GridData(GridData.FILL_HORIZONTAL);
        argsGridData.widthHint = TEXT_LENGTH_;
        argsText_.setText(GrxServerManager.vecServerInfo.elementAt(localDim).args);
        argsStr_ = GrxServerManager.vecServerInfo.elementAt(localDim).args;
        argsText_.addModifyListener( new ModifyListener() {
            public void modifyText(ModifyEvent e){
                argsStr_ = new String(argsText_.getText().trim()); 
            }
        });

        toggleButton_ = new Button(localPanel, SWT.PUSH);
        if (GrxServerManager.vecServerInfo.elementAt(localDim).autoStart) {
            toggleButton_.setText(STOP_);
        } else {
            toggleButton_.setText(START_);
        }
        
        localRefButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}
            
            public void widgetSelected(SelectionEvent e) {
                updateRefBtn(localDim);
            }
        });

        toggleButton_.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                updateBtn(localDim);
            }
        });

        GridData btnGridData = new GridData();
        btnGridData.horizontalIndent = HORIZON_INDENT_;
        btnGridData.widthHint = 64;

        Label localLabelAuto = new Label(localPanel, SWT.RIGHT | SWT.FILL);
        localLabelAuto.setText("Automatic start ");
        GridData labelAutoGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelAutoGridData.horizontalSpan = 2;
        labelAutoGridData.widthHint = LABEL_LENGTH_ + TEXT_LENGTH_;

        autoChkBox_ = new Button(localPanel, SWT.CHECK);
        GridData chkbtnGridData = new GridData();
        chkbtnGridData.horizontalIndent = HORIZON_INDENT_;
        autoChkBox_.setSelection(GrxServerManager.vecServerInfo.elementAt(localDim).autoStart);
        bAuto_ = GrxServerManager.vecServerInfo.elementAt(localDim).autoStart;
        autoChkBox_.addSelectionListener( new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e){}
            
            public void widgetSelected(SelectionEvent e){
                bAuto_ = autoChkBox_.getSelection();
            }
            
        });
        Label localLabelUseORB = new Label(localPanel, SWT.RIGHT | SWT.FILL);
        localLabelUseORB.setText("Use -ORBInitRef option ");
        GridData labelUseORBGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelUseORBGridData.horizontalSpan = 2;
        labelUseORBGridData.widthHint = LABEL_LENGTH_ + TEXT_LENGTH_;

        useORBChkBox_ = new Button(localPanel, SWT.CHECK);
        GridData UseORBchkbtnGridData = new GridData();
        UseORBchkbtnGridData.horizontalIndent = HORIZON_INDENT_;
        useORBChkBox_.setSelection(GrxServerManager.vecServerInfo.elementAt(localDim).useORB);
        bUseORB_ = GrxServerManager.vecServerInfo.elementAt(localDim).useORB;
        useORBChkBox_.addSelectionListener( new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e){}
            
            public void widgetSelected(SelectionEvent e){
                bUseORB_ = useORBChkBox_.getSelection();
            }
            
        });
        localLabelPath.setLayoutData(labelPathGridData);
        pathText_.setLayoutData(pathGridData);
        localRefButton.setLayoutData(refbtnGridData);
        localLabelArgs.setLayoutData(labelArgsGridData);
        argsText_.setLayoutData(argsGridData);
        toggleButton_.setLayoutData(btnGridData);
        localLabelAuto.setLayoutData(labelAutoGridData);
        autoChkBox_.setLayoutData(chkbtnGridData);
        localLabelUseORB.setLayoutData(labelUseORBGridData);
        useORBChkBox_.setLayoutData(UseORBchkbtnGridData);
    }

    //パネルの情報を元にProcessInfoを更新する
    public void updateProcessInfo(ProcessInfo ref){
        ref.autoStart = bAuto_;
        ref.useORB = bUseORB_;
        ref.args = argsStr_;
        
        // comに関してはupdateBtn(int localDim)を参照
        ref.com.clear();
        ref.com.add( pathStr_ );
    }
    
    //ファイル参照用のモーダルダイアログを開く
    private void updateRefBtn(int localDim) {
        //ファイル名の取得
        String[] filterNames = new String[] { "すべてのファイル(*)" };
        String[] filterExtensions = new String[] { "*" };
        FileDialog fileDlg = new FileDialog(GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);

        fileDlg.setText("");
        fileDlg.setFilterNames(filterNames);
        fileDlg.setFilterExtensions(filterExtensions);

        String strServerName = GrxServerManager.vecServerInfo.elementAt(localDim).id;
        fileDlg.setText("Select " + strServerName);
        pathText_.setText(fileDlg.open());
    }

    //プロセス停止と開始
    private void updateBtn(int localDim) {
        // 確認ダイアログ表示後 当該 タブを削除
        if ( MessageDialog.openConfirm(
                this.getShell(),
                "Confirmation process " + toggleButton_.getText(),
                "Would you like to " + toggleButton_.getText() +" ?"  ))
        {
            TabFolder tabfolder = (TabFolder)getParent();
            ProcessInfo localInfo = new ProcessInfo(); 
            updateProcessInfo(localInfo);
            localInfo.id = tabfolder.getItem(localDim).getText();
            if( serverManager_.toggleProcess(localInfo) )
            {
                toggleButton_.setText(STOP_);
            }else{
                toggleButton_.setText(START_);
            }
        }
    }
}

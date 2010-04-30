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
import org.eclipse.osgi.util.NLS;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.util.GrxProcessManager.AProcess;
import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;

public class GrxServerManagerPanel extends Composite {

    static private final int    HORIZON_INDENT_ = 4;
    static private final int    LABEL_LENGTH_   = 40;
    static private final int    LOCAL_REF_BUTTON_LENGTH_ = 18;
    static private final int    BUTTON_LENGTH_   = 64;
    static private final int    TEXT_LENGTH_    = 256;
    static private final String START_          = MessageBundle.get("GrxServerManagerPanel.button.start"); //$NON-NLS-1$
    static private final String STOP_           = MessageBundle.get("GrxServerManagerPanel.button.stop"); //$NON-NLS-1$
    static private final String RESTART_        = MessageBundle.get("GrxServerManagerPanel.button.restart"); //$NON-NLS-1$
    
    private Button  toggleButton_ = null; // Start <-> Stop Button
    private Button  restartButton_= null; // Restart Button
    private Button  autoChkBox_   = null; // Automatic start process flag
    private Button  useORBChkBox_ = null;
    private Text    pathText_     = null;
    private Text    argsText_     = null;
    private GrxServerManager serverManager_ = null;
    
    private String  pathStr_ = null;
    private String  argsStr_ = null;
    private boolean bUseORB_;
    private boolean bAuto_;
    
    private ProcessInfo processInfo_=null;
    private boolean restartFlag_ = false;
    private GrxPluginManager pluginManager_ = null;
    
    public GrxServerManagerPanel(GrxServerManager manager, TabFolder parent, int style, int index, GrxPluginManager pluginManager) {
        super(parent, style);
        pluginManager_ = pluginManager;
        serverManager_ = manager;
        processInfo_ = manager.getServerInfo().elementAt(index);
        Composite localPanel = this;
        localPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL |
                GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));

        GridLayout localGridLayout = new GridLayout(4, false);

        localGridLayout.marginLeft = 0;
        localGridLayout.horizontalSpacing = 0;
        localPanel.setLayout(localGridLayout);

        Label localLabelPath = new Label(localPanel, SWT.RIGHT);
        localLabelPath.setText(MessageBundle.get("GrxServerManagerPanel.label.path")); //$NON-NLS-1$
        GridData labelPathGridData = new GridData();
        labelPathGridData.widthHint = LABEL_LENGTH_;

        pathText_ = new Text(localPanel, SWT.BORDER);
        GridData pathGridData = new GridData(GridData.FILL_HORIZONTAL);
        pathGridData.widthHint = TEXT_LENGTH_;
        pathText_.setText(processInfo_.com.get(processInfo_.editComIndex));
        pathStr_ = processInfo_.com.get(processInfo_.editComIndex);
        pathText_.addModifyListener( new ModifyListener() {
            public void modifyText(ModifyEvent e){
                pathStr_ = new String( pathText_.getText().trim());
            }
        });
        
        Button localRefButton = new Button(localPanel, SWT.PUSH);
        localRefButton.setText("..."); //$NON-NLS-1$
        GridData refbtnGridData = new GridData();
        refbtnGridData.horizontalIndent = HORIZON_INDENT_;
        refbtnGridData.widthHint = LOCAL_REF_BUTTON_LENGTH_;
        
        restartButton_ = new Button(localPanel, SWT.PUSH);
        restartButton_.setText(RESTART_);
        GridData rstbtnGridData = new GridData();
        rstbtnGridData.horizontalIndent = HORIZON_INDENT_;
        rstbtnGridData.widthHint = BUTTON_LENGTH_;

        Label localLabelArgs = new Label(localPanel, SWT.RIGHT);
        localLabelArgs.setText(MessageBundle.get("GrxServerManagerPanel.label.args")); //$NON-NLS-1$
        GridData labelArgsGridData = new GridData();
        labelArgsGridData.widthHint = LABEL_LENGTH_;

        argsText_ = new Text(localPanel, SWT.BORDER);
        GridData argsGridData = new GridData(GridData.FILL_HORIZONTAL);
        argsGridData.widthHint = TEXT_LENGTH_;
        argsText_.setText(processInfo_.args);
        argsStr_ = processInfo_.args;
        argsText_.addModifyListener( new ModifyListener() {
            public void modifyText(ModifyEvent e){
                argsStr_ = new String(argsText_.getText().trim()); 
            }
        });

        toggleButton_ = new Button(localPanel, SWT.PUSH);
        GrxProcessManager pm = (GrxProcessManager) pluginManager_.getItem("processManager");
        AProcess process = pm.get(processInfo_.id);
        if ( process!=null && process.isRunning() ) {
            toggleButton_.setText(STOP_);
            restartButton_.setVisible(true);
        } else {
            toggleButton_.setText(START_);
            restartButton_.setVisible(false);
        }
        
        localRefButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}
            
            public void widgetSelected(SelectionEvent e) {
                updateRefBtn();
            }
        });

        toggleButton_.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                updateBtn();
            }
        });

        restartButton_.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                updateRestartBtn();
            }
        });
        
        GridData btnGridData = new GridData();
        btnGridData.horizontalIndent = HORIZON_INDENT_;
        btnGridData.widthHint = BUTTON_LENGTH_;
        btnGridData.horizontalAlignment = SWT.END;
        btnGridData.horizontalSpan = 2;

        Label localLabelAuto = new Label(localPanel, SWT.RIGHT | SWT.FILL);
        localLabelAuto.setText(MessageBundle.get("GrxServerManagerPanel.label.start")); //$NON-NLS-1$
        GridData labelAutoGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelAutoGridData.horizontalSpan = 2;
        labelAutoGridData.widthHint = LABEL_LENGTH_ + TEXT_LENGTH_;

        autoChkBox_ = new Button(localPanel, SWT.CHECK);
        GridData chkbtnGridData = new GridData();
        chkbtnGridData.horizontalIndent = HORIZON_INDENT_;
        autoChkBox_.setSelection(processInfo_.autoStart);
        bAuto_ = processInfo_.autoStart;
        autoChkBox_.addSelectionListener( new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e){}
            
            public void widgetSelected(SelectionEvent e){
                bAuto_ = autoChkBox_.getSelection();
            }
            
        });
        Label localLabelUseORB = new Label(localPanel, SWT.RIGHT | SWT.FILL);
        localLabelUseORB.setText(MessageBundle.get("GrxServerManagerPanel.label.useeRef")); //$NON-NLS-1$
        GridData labelUseORBGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelUseORBGridData.horizontalSpan = 2;
        labelUseORBGridData.widthHint = LABEL_LENGTH_ + TEXT_LENGTH_;

        useORBChkBox_ = new Button(localPanel, SWT.CHECK);
        GridData UseORBchkbtnGridData = new GridData();
        UseORBchkbtnGridData.horizontalIndent = HORIZON_INDENT_;
        useORBChkBox_.setSelection(processInfo_.useORB);
        bUseORB_ = processInfo_.useORB;
        useORBChkBox_.addSelectionListener( new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e){}
            
            public void widgetSelected(SelectionEvent e){
                bUseORB_ = useORBChkBox_.getSelection();
            }
            
        });
        localLabelPath.setLayoutData(labelPathGridData);
        pathText_.setLayoutData(pathGridData);
        localRefButton.setLayoutData(refbtnGridData);
        restartButton_.setLayoutData(rstbtnGridData);
        localLabelArgs.setLayoutData(labelArgsGridData);
        argsText_.setLayoutData(argsGridData);
        toggleButton_.setLayoutData(btnGridData);
        localLabelAuto.setLayoutData(labelAutoGridData);
        autoChkBox_.setLayoutData(chkbtnGridData);
        localLabelUseORB.setLayoutData(labelUseORBGridData);
        useORBChkBox_.setLayoutData(UseORBchkbtnGridData);
    }

    //パネルの情報を元にProcessInfoを更新する
    public void updateProcessInfo(){
        processInfo_.autoStart = bAuto_;
        processInfo_.useORB = bUseORB_;
        processInfo_.args = argsStr_;
        
        // comに関してはupdateBtn(int localDim)を参照
        processInfo_.com.remove(processInfo_.editComIndex);
        processInfo_.com.add( processInfo_.editComIndex, pathStr_ );
    }
    
    //ファイル参照用のモーダルダイアログを開く
    private void updateRefBtn() {
        //ファイル名の取得
        String[] filterNames = new String[] { MessageBundle.get("GrxServerManagerPanel.filedialog.filterName") }; //$NON-NLS-1$
        String[] filterExtensions = new String[] { "*" }; //$NON-NLS-1$
        FileDialog fileDlg = new FileDialog(GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);

        fileDlg.setText(""); //$NON-NLS-1$
        fileDlg.setFilterNames(filterNames);
        fileDlg.setFilterExtensions(filterExtensions);

        String strServerName = processInfo_.id;
        fileDlg.setText(MessageBundle.get("GrxServerManagerPanel.filedialog.title") + strServerName); //$NON-NLS-1$
        pathText_.setText(fileDlg.open());
    }

    //プロセス停止と開始
    private void updateBtn() {
        String mes = MessageBundle.get("GrxServerManagerPanel.dialog.message.update"); //$NON-NLS-1$
        mes = NLS.bind(mes, new String[]{toggleButton_.getText()});

        // 確認ダイアログ表示後 当該 タブを削除
        if ( MessageDialog.openConfirm(
                this.getShell(),
                MessageBundle.get("GrxServerManagerPanel.dialog.title.update") + toggleButton_.getText(), //$NON-NLS-1$
                mes)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            updateProcessInfo();
            if( serverManager_.toggleProcess(processInfo_) )
            {
                toggleButton_.setText(STOP_);
                restartButton_.setVisible(true);
            }else{
                toggleButton_.setText(START_);
                restartButton_.setVisible(false);
            }
        }
    }

    //プロセス再起動
    private void updateRestartBtn() {
        String mes = MessageBundle.get("GrxServerManagerPanel.dialog.message.restart"); //$NON-NLS-1$
        String title = MessageBundle.get("GrxServerManagerPanel.dialog.title.update") + restartButton_.getText(); //$NON-NLS-1$
        // 確認ダイアログ表示後 再起動
        if ( MessageDialog.openConfirm(
                this.getShell(),
                title,
                mes)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            updateProcessInfo();

            GrxProcessManager pm = (GrxProcessManager) pluginManager_.getItem("processManager");
            AProcess process = pm.get(processInfo_.id);
            
            //プロセス停止時は起動のみ行う
            //(停止時は非表示のためイレギュラーケース)
            if(process==null || !process.isRunning()){
                if( serverManager_.toggleProcess(processInfo_) )
                {
                    toggleButton_.setText(STOP_);
                    restartButton_.setVisible(true);
                }else{
                    toggleButton_.setText(START_);
                    restartButton_.setVisible(false);
                    MessageDialog.openError(
                            this.getShell(),title,
                            MessageBundle.get("GrxServerManagerPanel.dialog.message.errorRestart"));
                }
                return;
            }

            // サーバの停止
            if(serverManager_.toggleProcess(processInfo_))
            {
                toggleButton_.setText(STOP_);
                restartButton_.setVisible(true);
                MessageDialog.openError(
                        this.getShell(),title,
                        MessageBundle.get("GrxServerManagerPanel.dialog.message.errorRestart"));
                return;
            }
            // サーバの起動
            if(!serverManager_.toggleProcess(processInfo_))
            {
                toggleButton_.setText(START_);
                restartButton_.setVisible(false);
                MessageDialog.openError(
                        this.getShell(),title,
                        MessageBundle.get("GrxServerManagerPanel.dialog.message.errorRestart"));
                return;
            }

            toggleButton_.setText(STOP_);
            restartButton_.setVisible(true);
            restartFlag_ = true;
        }
    }
    
    public void setStartText(){
        // 再起動時はプロセス停止notifyを一回無視する
        if(restartFlag_)
        {
            restartFlag_ = false;
            return;
        }

        toggleButton_.setText(START_);
        restartButton_.setVisible(false);
    }
}

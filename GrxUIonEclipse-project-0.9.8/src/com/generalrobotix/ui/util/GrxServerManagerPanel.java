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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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

import com.generalrobotix.ui.util.GrxProcessManager.ProcessInfo;
import com.generalrobotix.ui.grxui.Activator;

@SuppressWarnings("serial")
public class GrxServerManagerPanel extends Composite {

    static private final int    HORIZON_INDENT = 4;
    static private final int    LABEL_LENGTH   = 32;
    static private final int    TEXT_LENGTH    = 256;
    static private final String START          = "Start";
    static private final String STOP           = "Stop";

    public GrxServerManagerPanel(TabFolder parent, int style, int index) {
        super(parent, style);
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
        labelPathGridData.widthHint = LABEL_LENGTH;

        Text localPath = new Text(localPanel, SWT.BORDER);
        GridData pathGridData = new GridData(GridData.FILL_HORIZONTAL);
        pathGridData.widthHint = TEXT_LENGTH;
        localPath.setText(GrxServerManager.vecServerInfo.elementAt(localDim).com.get(0));

        Button localRefButton = new Button(localPanel, SWT.PUSH);
        localRefButton.setText("...");
        GridData refbtnGridData = new GridData();
        refbtnGridData.horizontalIndent = HORIZON_INDENT;

        Label localLabelArgs = new Label(localPanel, SWT.RIGHT);
        localLabelArgs.setText("Args: ");
        GridData labelArgsGridData = new GridData();
        labelArgsGridData.widthHint = LABEL_LENGTH;

        Text localArgs = new Text(localPanel, SWT.BORDER);
        GridData argsGridData = new GridData(GridData.FILL_HORIZONTAL);
        argsGridData.widthHint = TEXT_LENGTH;
        localArgs.setText(GrxServerManager.vecServerInfo.elementAt(localDim).args);

        Button localButton = new Button(localPanel, SWT.PUSH);
        if (GrxServerManager.vecServerInfo.elementAt(localDim).autoStart) {
            localButton.setText(STOP);
        } else {
            localButton.setText(START);
        }
        localRefButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}
            
            public void widgetSelected(SelectionEvent e) {
                updateRefBtn(localDim);
            }
        });

        localButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {}

            public void widgetSelected(SelectionEvent e) {
                updateBtn(localDim);
            }
        });

        GridData btnGridData = new GridData();
        btnGridData.horizontalIndent = HORIZON_INDENT;
        btnGridData.widthHint = 64;

        Label localLabelAuto = new Label(localPanel, SWT.RIGHT | SWT.FILL);
        localLabelAuto.setText("Automatic start ");
        GridData labelAutoGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelAutoGridData.horizontalSpan = 2;
        labelAutoGridData.widthHint = LABEL_LENGTH + TEXT_LENGTH;

        Button localChkBox = new Button(localPanel, SWT.CHECK);
        GridData chkbtnGridData = new GridData();
        chkbtnGridData.horizontalIndent = HORIZON_INDENT;
        localChkBox.setSelection(GrxServerManager.vecServerInfo.elementAt(localDim).autoStart);

        Label localLabelUseORB = new Label(localPanel, SWT.RIGHT | SWT.FILL);
        localLabelUseORB.setText("Use -ORBInitRef option ");
        GridData labelUseORBGridData = new GridData(GridData.FILL_HORIZONTAL);
        labelUseORBGridData.horizontalSpan = 2;
        labelUseORBGridData.widthHint = LABEL_LENGTH + TEXT_LENGTH;

        Button localUseORBChkBox = new Button(localPanel, SWT.CHECK);
        GridData UseORBchkbtnGridData = new GridData();
        UseORBchkbtnGridData.horizontalIndent = HORIZON_INDENT;
        localUseORBChkBox.setSelection(GrxServerManager.vecServerInfo.elementAt(localDim).useORB);

        localLabelPath.setLayoutData(labelPathGridData);
        localPath.setLayoutData(pathGridData);
        localRefButton.setLayoutData(refbtnGridData);
        localLabelArgs.setLayoutData(labelArgsGridData);
        localArgs.setLayoutData(argsGridData);
        localButton.setLayoutData(btnGridData);
        localLabelAuto.setLayoutData(labelAutoGridData);
        localChkBox.setLayoutData(chkbtnGridData);
        localLabelUseORB.setLayoutData(labelUseORBGridData);
        localUseORBChkBox.setLayoutData(UseORBchkbtnGridData);

        GrxServerManager.vecButton.add(index, localButton);
        GrxServerManager.vecChkBox.add(index, localChkBox);
        GrxServerManager.vecUseORBChkBox.add(index, localUseORBChkBox);
        GrxServerManager.vecPathText.add(index, localPath);
        GrxServerManager.vecArgsText.add(index, localArgs);
    }

    //ファイル参照用のモーダルダイアログを開く
    private void updateRefBtn(int localDim) {
        //ファイル名の取得
        String[] filterNames = new String[] { "すべてのファイル(*)" };
        String[] filterExtensions = new String[] { "*" };
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        FileDialog fileDlg = new FileDialog(window.getShell(), SWT.OPEN);

        fileDlg.setText("");
        fileDlg.setFilterNames(filterNames);
        fileDlg.setFilterExtensions(filterExtensions);

        String strServerName = GrxServerManager.vecServerInfo.elementAt(localDim).id;
        fileDlg.setText("Select " + strServerName);
        GrxServerManager.vecPathText.elementAt(localDim).setText(fileDlg.open());
    }

    //プロセス開始
    private void updateBtn(int localDim) {
        GrxProcessManager pm = GrxProcessManager.getInstance();
        ProcessInfo pi = new ProcessInfo();
        StringBuffer nsHost = new StringBuffer("");
        StringBuffer nsPort = new StringBuffer("");
        Activator.refNSHostPort(nsHost, nsPort);

        String nsOpt = " -ORBInitRef NameService=corbaloc:iiop:" + nsHost +
            ":" + nsPort + "/NameService";

        if (GrxServerManager.vecButton.elementAt(localDim).getText().equals(START)) {
            pi.id = GrxServerManager.vecServerInfo.elementAt(localDim).id;
            pi.args = GrxXmlUtil.expandEnvVal(GrxServerManager.vecArgsText.elementAt(localDim).getText());
            pi.com.clear();
            if (pi.id.equals("NameService")) {
                // log のクリア
                String[] com;
                if (System.getProperty("os.name").equals("Linux") ||
                        System.getProperty("os.name").equals("Mac OS X")) {
                    com = new String[] { "/bin/sh", "-c",
                            "rm " + GrxServerManager.homePath_ +
                            ".OpenHRP-3.1/omninames-log/*" };
                } else {
                    com = new String[] { "cmd", "/c",
                            "del " + "\"" + 
                            GrxServerManager.WIN_TMP_DIR +
                            "omninames-*.*" + "\"" };
                }
                try {
                    Process pr = Runtime.getRuntime().exec(com);
                    InputStream is = pr.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (Exception e) {
                    ;
                }
                pi.com.add(GrxXmlUtil.expandEnvVal(GrxServerManager.vecPathText.elementAt(localDim).getText()) + " " + pi.args);
            } else if (pi.useORB) {
                pi.com.add(GrxXmlUtil.expandEnvVal(GrxServerManager.vecPathText.elementAt(localDim).getText()) + " " + pi.args + nsOpt);
            } else {
                pi.com.add(
                        GrxXmlUtil.expandEnvVal(GrxServerManager.vecPathText.elementAt(localDim).getText()) + " " + pi.args);
            }
            pm.register(pi);
            GrxProcessManager.AProcess p = pm.get(pi.id);
            if (p.start(null)) {
                GrxServerManager.vecButton.elementAt(localDim).setText(STOP);
            } else // start 失敗
            {
                pm.unregister(pi.id);
            }
        } else {
            pi.id = GrxServerManager.vecServerInfo.elementAt(localDim).id;
            pi.args = GrxXmlUtil.expandEnvVal(GrxServerManager.vecArgsText.elementAt(localDim).getText());
            pi.com.clear();
            if (pi.useORB) {
                pi.com.add(GrxXmlUtil.expandEnvVal(GrxServerManager.vecPathText.elementAt(localDim).getText()) + " " + pi.args + nsOpt);
            } else {
                pi.com.add(GrxXmlUtil.expandEnvVal(GrxServerManager.vecPathText.elementAt(localDim).getText()) + " " + pi.args);
            }
            GrxProcessManager.AProcess p = pm.get(pi.id);
            if (p.stop()) {
                GrxServerManager.vecButton.elementAt(localDim).setText(START);
                pm.unregister(pi.id);
            }
        }
    }
}

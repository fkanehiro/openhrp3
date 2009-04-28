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

import java.io.File;
import java.util.Vector;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.generalrobotix.ui.util.GrxServerManagerConfigXml;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxProcessManager.*;
import com.generalrobotix.ui.grxui.Activator;

@SuppressWarnings("serial")
public class GrxServerManager{
    public static String homePath_;

    enum GrxServer {
        COLLISION_DETECTOR,
        DYNAMIS_SIMULATOR,
        MODEL_LOADER,
        NAME_SERVER
    };

    public static final String                 LINUX_TMP_DIR          = System.getenv("HOME") + File.separator + ".OpenHRP-3.1" + File.separator;
    public static final String                 WIN_TMP_DIR            = System.getenv("APPDATA") + File.separator + "OpenHRP-3.1" + File.separator;
    private static final String                CONFIG_XML             = "grxuirc.xml";

    public static volatile Vector<ProcessInfo> vecServerInfo          = new Vector<ProcessInfo>();
    private static volatile boolean           bInitializedServerInfo = false;
    private static volatile String             strTempDir             = null;

    static private synchronized void InitServerInfo() {
        if (!bInitializedServerInfo) {
            File fileXml = getConfigXml();
            loadConfigXml(fileXml);
            String dir = System.getenv("ROBOT_DIR");
            if (dir != null && new File(dir).isDirectory()) {
                homePath_ = dir+File.separator;
            } else {
                homePath_ = System.getProperty( "user.home", "" )+File.separator;
            }
            bInitializedServerInfo = true;
        }
    }

    //XMLファイルの読み込みとlistServerInfoの初期化
    static private void loadConfigXml(File fileXml) {
        GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml(fileXml);
        vecServerInfo.clear();
        for (int i = 0;; ++i) {
            ProcessInfo localInfo = localXml.getServerInfo(i);
            if (localInfo == null) {
                break;
            }
            if (localInfo.id.equals("")){
                continue;
            }
            localInfo.dir = GrxServerManagerConfigXml.getDefaultDir();
            localInfo.waitCount = GrxServerManagerConfigXml.getDefaultWaitCount_();
            vecServerInfo.add(localInfo);
        }
        GrxDebugUtil.print(fileXml.getPath() + "\n");
        GrxDebugUtil.print(fileXml.getParent() + "\n");
    }

    //Xmlファイルへ次回起動のための値を保存
    static public synchronized void SaveServerInfo() {
        if (bInitializedServerInfo) {
            GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml(getConfigXml());
            for (int i = 0; i < vecServerInfo.size(); ++i) {
                localXml.setServerNode(vecServerInfo.elementAt(i));
            }
            localXml.SaveServerInfo();
        }
    }

    //Xmlファイルハンドルの取得
    static private File getConfigXml() {
        File ret = null;
        if ( System.getProperty("os.name").equals("Linux") ||
             System.getProperty("os.name").equals("Mac OS X")) {
            strTempDir = LINUX_TMP_DIR;
        } else { //Windows と　仮定
            strTempDir = WIN_TMP_DIR;
        }
        ret = new File(strTempDir, CONFIG_XML);
        return ret;
    }

    /**
     * GrxServerManagerを作り、処理を開始する。
     */
	public GrxServerManager() {
        InitServerInfo();
    }
    
    public Vector<ProcessInfo> getServerInfo(){
        return vecServerInfo;
    }

    //
    public boolean toggleProcess(ProcessInfo pInfo){
        boolean ret = true;
        GrxProcessManager pm = GrxProcessManager.getInstance();
        AProcess process = pm.get(pInfo.id);
        StringBuffer nsHost = new StringBuffer("");
        StringBuffer nsPort = new StringBuffer("");
        Activator.refNSHostPort(nsHost, nsPort);
        String nsOpt = "-ORBInitRef NameService=corbaloc:iiop:" + nsHost +
            ":" + nsPort + "/NameService";

        if (process == null) {
            // 新規登録と開始処理
            pInfo.waitCount = GrxServerManagerConfigXml.getDefaultWaitCount_();
            pInfo.dir = GrxServerManagerConfigXml.getDefaultDir();
            updatedParam(pInfo);

            pInfo.dir = GrxXmlUtil.expandEnvVal(GrxServerManagerConfigXml.getDefaultDir());
            pInfo.args = GrxXmlUtil.expandEnvVal(pInfo.args);
            for (int i = 0; i < pInfo.com.size(); ++i) {
                pInfo.com.set(i, GrxXmlUtil.expandEnvVal(pInfo.com.get(i)));
            }
            if (pInfo.useORB) {
                pInfo.com.add(nsOpt);
            }
            pm.register(pInfo);
            if (!pm.get(pInfo.id).start(null)) {
                // start 失敗
                pm.unregister(pInfo.id);
                ret = false;
            }
        } else {
            if (process.isRunning()) {
                // 停止処理
                process.stop();
                ret = false;
            } else {
                // 開始処理
                if (pInfo.id.equals("NameService")) {
                    deleteNameServerLog();
                }
                if (updatedParam(pInfo)) {
                    process.pi_.autoStart = pInfo.autoStart;
                    process.pi_.useORB = pInfo.useORB;
                    process.pi_.args = GrxXmlUtil.expandEnvVal(pInfo.args);
                    process.pi_.com.clear();
                    for (int i = 0; i < pInfo.com.size(); ++i) {
                        process.pi_.com.add(GrxXmlUtil.expandEnvVal(pInfo.com.get(i)));
                    }
                    if (process.pi_.useORB) {
                        process.pi_.com.add(nsOpt);
                    }
                    process.updateCom();
                }

                if (!process.start(null)) {
                    // start 失敗
                    ret = false;
                }
            }
        }
        return ret;
    }
    
    
    /**
     * @brief vecServerInfoの更新 vecServerInfoに同名のidが存在すればpInfoを元に更新する
     * @param pInfo
     *            現在の値
     * @return boolean 更新した時はtrue
     */
    private boolean updatedParam(ProcessInfo pInfo) {
        boolean ret = false;
        for (ProcessInfo pi : vecServerInfo) {

            if (pi.id.equals(pInfo.id)) {
                if (pInfo.autoStart != pi.autoStart) {
                    pi.autoStart = pInfo.autoStart;
                    ret = true;
                }
                if (pInfo.useORB != pi.useORB) {
                    pi.useORB = pInfo.useORB;
                    ret = true;
                }
                if (!pInfo.com.equals(pi.com)) {
                    pi.com.clear();
                    for (String i : pInfo.com) {
                        pi.com.add(new String(i));
                    }
                    ret = true;
                }
                if (!pInfo.args.equals(pi.args)) {
                    pi.args = pInfo.args;
                    ret = true;
                }
                break;
            }
        }

        return ret;
    }
    
    /**
     * @brief NameServerのログを削除
     * 
     * 
     * 
     */
    private void deleteNameServerLog(){
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
                    GrxServerManager.WIN_TMP_DIR + "omninames-log" +
                    File.separator + "omninames-*.*" +  "\"" };
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
            e.printStackTrace();
        }
    }    
    
}

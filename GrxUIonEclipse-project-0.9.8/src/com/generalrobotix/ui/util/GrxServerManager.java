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

import com.generalrobotix.ui.util.GrxServerManagerConfigXml;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxProcessManager.*;
import com.generalrobotix.ui.util.FileUtil;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.util.SynchronizedAccessor;

@SuppressWarnings("serial")
public class GrxServerManager{

    private static final String                CONFIG_XML             = "grxuirc.xml";
    private static File                         CONFIG_XML_PATH = null;                 
    public static volatile Vector<ProcessInfo> vecServerInfo          = new Vector<ProcessInfo>();
    public static volatile ProcessInfo         nameServerInfo = new ProcessInfo();
    private static volatile boolean           bInitializedServerInfo = false;
    private static SynchronizedAccessor<Integer>    newPort_ = new SynchronizedAccessor<Integer>(0);
    private static SynchronizedAccessor<String>    newHost_ = new SynchronizedAccessor<String>("");
    private static SynchronizedAccessor<String>    nameServerLogDir = new SynchronizedAccessor<String>("");
    private static final int    MAXMUM_PORT_NUMBER  = 65535;
    private static final String LINE_SEPARATOR = new String( System.getProperty("line.separator") );

    
    static private synchronized void InitServerInfo() {
        if (!bInitializedServerInfo) {
            CONFIG_XML_PATH = getConfigXml();
            loadConfigXml(CONFIG_XML_PATH);
            newPort_.set(GrxServerManagerConfigXml.getNameServerPort());
            newHost_.set(GrxServerManagerConfigXml.getNameServerHost());
            nameServerLogDir.set(GrxServerManagerConfigXml.getNameServerLogDir());
            bInitializedServerInfo = true;
        }
    }

    //XMLファイルの読み込みとlistServerInfoの初期化
    static private void loadConfigXml(File fileXml) {
        GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml(fileXml);
        nameServerInfo = localXml.getNameServerInfo();
        vecServerInfo.clear();
        for (int i = 0;; ++i) {
            ProcessInfo localInfo = localXml.getServerInfo(i);
            if (localInfo == null) {
                break;
            }
            if (localInfo.id.equals("")){
                continue;
            }
            vecServerInfo.add(localInfo);
        }
    }

    //Xmlファイルへ次回起動のための値を保存
    static public synchronized void SaveServerInfo() {
        if (bInitializedServerInfo) {
            GrxServerManagerConfigXml localXml = new GrxServerManagerConfigXml(CONFIG_XML_PATH);
            for (int i = 0; i < vecServerInfo.size(); ++i) {
                localXml.setServerNode(vecServerInfo.elementAt(i));
            }
            localXml.SaveServerInfo(newPort_.get(), newHost_.get());
        }
    }


    //Xmlファイルハンドルの取得
    static private File getConfigXml() {
        return new File(Activator.getDefault().getTempDir(), CONFIG_XML);
    }

    
    
    /**
     * GrxServerManagerを作り、処理を開始する。
     */
	public GrxServerManager() {
        InitServerInfo();
    }
    
    /**
     * @brief vecServerInfoの取得
     * @return Vector<ProcessInfo> vecServerInfo
     */
    public Vector<ProcessInfo> getServerInfo(){
        return vecServerInfo;
    }

    /**
     * @brief NameServerのProcessInfo取得
     * @return ProcessInfo nameServerInfo
     */
    public ProcessInfo getNameServerInfo(){
        return nameServerInfo;
    }
    
    
    public String getNewHost(){
        return newHost_.get();
    }
    
    public int getNewPort(){
        return newPort_.get();
    }
    
    public String getNameserverLogDir(){
        return nameServerLogDir.get(); 
    }
    
    public String setNewHostPort(String host, String port,StringBuffer refHost, StringBuffer refPort){
        String retHost = checkHostFormat(host);
        String retPort = checkPortFormat(port);
        
        String ret = retHost + retPort;
        if( ret.isEmpty() ){
            newPort_.set(Integer.valueOf(port));
            newHost_.set(host);
        } else {
            if(!retHost.isEmpty()){
                refHost.append( getNewHost() );
            }
            if(!retPort.isEmpty()){
                refPort.append( Integer.toString(getNewPort()) );
            }
        }
        return ret;
    }
    
    /**
     * @brief サーバプログラムの起動トグル
     * @param ProcessInfo
     * 　　　　　開始と停止を遷移させるサーバプログラム
     * @return boolean
     *          true:開始状態へ遷移
     *          false:停止状態へ遷移
     */
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
            pInfo.waitCount = GrxServerManagerConfigXml.getDefaultWaitCount();
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
                if (updatedParam(pInfo)) {
                    process.pi_.autoStart = pInfo.autoStart;
                    process.pi_.useORB = pInfo.useORB;
                    process.pi_.args = GrxXmlUtil.expandEnvVal(pInfo.args);
                    process.pi_.com.clear();
                    if(pInfo.com.size() > 0){
                        process.pi_.com.add(GrxXmlUtil.expandEnvVal(pInfo.com.get(0)) + " " + process.pi_.args);
                        for (int i = 1; i < pInfo.com.size(); ++i) {
                            process.pi_.com.add(GrxXmlUtil.expandEnvVal(pInfo.com.get(i)));
                        }
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

    private String checkHostFormat(String host){
        String ret = "";
        if(host.length() > 255){
            return MessageBundle.get("GrxServerManager.message.hostFormat.num") + LINE_SEPARATOR;
        }
        
        int limit = host.length() - host.replace(".", "").length();
        
        String [] splitStr = host.split("\\.", limit == 0 ? limit : limit + 1); 
        
        for(String i : splitStr){
            if(i.length() < 1 || i.length() > 63){
                return MessageBundle.get("GrxServerManager.message.hostFormat.label") + LINE_SEPARATOR;
            }
            if( i.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]$") || 
                i.matches("^[a-zA-Z0-9]$") ){
                continue;
            } else {
                return MessageBundle.get("GrxServerManager.message.hostFormat.rfc") + LINE_SEPARATOR;
            }
        }
        return ret;
    }
    
    private String checkPortFormat(String port){
        String ret = "";
        try{
            int portInt = Integer.valueOf(port); 
            if( portInt > MAXMUM_PORT_NUMBER || portInt < 0)
                ret = MessageBundle.get("GrxServerManager.message.portFormat1") +
                        Integer.toString(MAXMUM_PORT_NUMBER) + MessageBundle.get("GrxServerManager.message.portFormat2") + LINE_SEPARATOR;
        }catch (NumberFormatException ex){
            ret = MessageBundle.get("GrxServerManager.message.portFormat1") +
                Integer.toString(MAXMUM_PORT_NUMBER) + MessageBundle.get("GrxServerManager.message.portFormat2") + LINE_SEPARATOR;
        }
        return ret;
    }
    
    
}

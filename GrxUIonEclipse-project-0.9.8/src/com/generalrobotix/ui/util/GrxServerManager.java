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
import java.io.IOException;
import java.util.Vector;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxProcessManager.*;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.PreferenceConstants;
import com.generalrobotix.ui.util.SynchronizedAccessor;

@SuppressWarnings("serial")
public class GrxServerManager extends GrxBaseItem{            
    private static volatile Vector<ProcessInfo> vecServerInfo          = new Vector<ProcessInfo>();
    private static volatile ProcessInfo         nameServerInfo = new ProcessInfo();
    private static SynchronizedAccessor<Integer>    newPort_ = new SynchronizedAccessor<Integer>(0);
    private static SynchronizedAccessor<String>    newHost_ = new SynchronizedAccessor<String>("");
    private static SynchronizedAccessor<String>    nameServerLogDir = new SynchronizedAccessor<String>("");
    private static final int    MAXMUM_PORT_NUMBER  = 65535;
    private static final String LINE_SEPARATOR = new String( System.getProperty("line.separator") );
    
    public static int     NAME_SERVER_PORT_ ;
    public static String  NAME_SERVER_HOST_ ;
    public static String  NAME_SERVER_LOG_DIR_ ;
    public String  serverInfoDefaultDir_  = "";
    public int     serverInfoDefaultWaitCount_ = 0;

    //次回起動のための値を保存
    public synchronized void SaveServerInfo() {
    	setServerInfoToPreferenceStore();
    	setNameServerInfoToPreferenceStore();
    }
    
    /**
     * GrxServerManagerを作り、処理を開始する。
     */
	public GrxServerManager(String name, GrxPluginManager manager) {
		super(name, manager);
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
        GrxProcessManager pm = GrxProcessManager.getInstance();
        AProcess process = pm.get(pInfo.id);
        StringBuffer nsHost = new StringBuffer("");
        StringBuffer nsPort = new StringBuffer("");
        Activator.refNSHostPort(nsHost, nsPort);
        String nsOpt = "-ORBInitRef NameService=corbaloc:iiop:" + nsHost +
            ":" + nsPort + "/NameService";

        if (process!=null){
        	if (process.isRunning()) {
                // 停止処理
                process.stop();
                return false;
        	}else
        		pm.unregister(pInfo.id);
        }
        // 新規登録と開始処理
        String s = pInfo.com.get(0);
        if(!(new File(s)).isAbsolute()){
        	pInfo.com.clear();
        	String ss = comToAbsolutePath(s);
        	if(ss!=null)
        		pInfo.com.add(ss);
        	pInfo.com.add(s);
        }
        if (pInfo.useORB) {
        	for(int i=0; i<pInfo.com.size(); i++)
        		pInfo.com.set(i, pInfo.com.get(i) + " " + nsOpt);
        }
        updatedParam(pInfo);
        pm.register(pInfo);
        if (!pm.get(pInfo.id).start(null)) {
        	// start 失敗
        	pm.unregister(pInfo.id);
        	return false;
        }
        
        return true;
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
    
    public void initialize(){
    	getNameServerInfoFromPerferenceStore();
        vecServerInfo.clear();
        getServerInfoFromPerferenceStore();       
        newPort_.set(NAME_SERVER_PORT_);
        newHost_.set(NAME_SERVER_HOST_);
        nameServerLogDir.set(NAME_SERVER_LOG_DIR_);
    }
    
    private void getNameServerInfoFromPerferenceStore(){
    	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
        nameServerInfo.id = PreferenceConstants.NAMESERVER; 
        String _dir = store.getString(
        		PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.LOGGIR);
        NAME_SERVER_LOG_DIR_ = GrxXmlUtil.expandEnvVal(_dir).trim();       
        if(!NAME_SERVER_LOG_DIR_.isEmpty()){
            String localStr = NAME_SERVER_LOG_DIR_.replaceFirst("^\"", "");
            File logDir = new File(localStr.replaceFirst("\"$", ""));
            if(!logDir.exists()){
                logDir.mkdirs();
            }
        }
        NAME_SERVER_PORT_ = store.getInt(
        		PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.PORT);
        if(NAME_SERVER_PORT_==0)
        	NAME_SERVER_PORT_ = store.getDefaultInt(
            		PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.PORT);
        NAME_SERVER_HOST_ = store.getString(
        			PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.HOST);
        if(NAME_SERVER_HOST_.equals(""))
        	NAME_SERVER_HOST_ = store.getDefaultString(
            		PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.HOST);
        
        nameServerInfo.args = "-start " + Integer.toString(NAME_SERVER_PORT_) + " -logdir " + NAME_SERVER_LOG_DIR_ +
        " -ORBendPointPublish giop:tcp:"+ NAME_SERVER_HOST_ + ":";
        
        String s = store.getString(
    			PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.COM);
        //      優先度　SERVER_DIRの下, Pathの通っているところ, AbsolutePath　//
        String ss=comToAbsolutePath(s);
        if(ss!=null)
        	nameServerInfo.com.add(ss);
        nameServerInfo.com.add(s);
        s = store.getString(
    			PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.COM+0);
        if(!s.equals(""))
        	nameServerInfo.com.add(s);
        
        nameServerInfo.autoStart = true;
        nameServerInfo.waitCount = store.getInt(
        			PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.WAITCOUNT);
        nameServerInfo.dir = store.getString(
    			PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.DIR).trim();
        nameServerInfo.isCorbaServer = false;
        nameServerInfo.hasShutdown = store.getBoolean(
    			PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.HASSHUTDOWN);
        nameServerInfo.doKillall = false;
        nameServerInfo.autoStop = true;
    }
    
    private String[] parse(String string){
    	String[] ret = string.split("\""+PreferenceConstants.SEPARATOR+"\"",-1);
    	if(ret.length!=0){
    		if(ret[0].charAt(0)=='\"')
    			ret[0] = ret[0].substring(1,ret[0].length());
    		String s=ret[ret.length-1];
    		if(s.charAt(s.length()-1)=='\"')
    			ret[ret.length-1] = s.substring(0,s.length()-1);
    	}
    	return ret;
    }
    
    private void getServerInfoFromPerferenceStore(){
    	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
    	String idList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.ID);
    	String[] id = idList.split(PreferenceConstants.SEPARATOR,-1);
    	String dirList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.DIR);
    	String[] dir = parse(dirList);
    	String waitCountList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.WAITCOUNT);
    	String[] waitCount = waitCountList.split(PreferenceConstants.SEPARATOR,-1);
    	String comList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.COM);
    	String[] com = parse(comList);
    	String argsList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.ARGS);
    	String[] args = parse(argsList);
    	String autoStartList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.AUTOSTART);
    	String[] autoStart = autoStartList.split(PreferenceConstants.SEPARATOR,-1);
    	String useORBList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.USEORB);
    	String[] useORB = useORBList.split(PreferenceConstants.SEPARATOR,-1);
    	String hasShutDownList = store.getString(PreferenceConstants.PROCESS+"."+PreferenceConstants.HASSHUTDOWN);
    	String[] hasShutDown = hasShutDownList.split(PreferenceConstants.SEPARATOR,-1);
    	for(int i=1; i<id.length; i++){
    		ProcessInfo processInfo = new ProcessInfo();
    		processInfo.id = id[i];
    		String s=null;
    		if(i<com.length && !com[i].equals(""))
    			s = com[i].trim();
    		else
    			s = com[0].trim();
    		//  優先度　AbsolutePath, SERVER_DIRの下, Pathの通っているところ　//
    		if((new File(s)).isAbsolute())
    			processInfo.com.add(s);
    		else{
    			String ss=comToAbsolutePath(s);
    			if(ss!=null){
    				processInfo.com.add(ss);
    				processInfo.editComIndex = 1;
    			}
    			processInfo.com.add(s);
    		}
    		if(i<args.length && !args[i].equals(""))
    			processInfo.args = args[i].trim();
    		else
    			processInfo.args = args[0].trim();
    		if(i<autoStart.length && !autoStart[i].equals(""))
    			processInfo.autoStart = autoStart[i].equals("true")? true : false;
    		else
    			processInfo.autoStart = autoStart[0].equals("true")? true : false;
    		if(i<useORB.length && !useORB[i].equals(""))
    			processInfo.useORB = useORB[i].equals("true")? true : false;
    		else
    			processInfo.useORB = useORB[0].equals("true")? true : false;
    		if(i<hasShutDown.length && !hasShutDown[i].equals(""))
    			processInfo.hasShutdown = hasShutDown[i].equals("true")? true : false;
    		else
    			processInfo.hasShutdown = hasShutDown[0].equals("true")? true : false;
    		if(i<waitCount.length && !waitCount[i].equals(""))
    			processInfo.waitCount = Integer.parseInt(waitCount[i]);
    		else
    			processInfo.waitCount = Integer.parseInt(waitCount[0]);
    		if(i<dir.length && !dir[i].equals(""))
    			processInfo.dir = dir[i].trim();
    		else
    			processInfo.dir = dir[0].trim();
    		processInfo.isCorbaServer = true;
    		processInfo.doKillall = false;
    		processInfo.autoStop = true;   
    		vecServerInfo.add(processInfo);
    	}	
    	serverInfoDefaultDir_ = dir[0].trim();
    	serverInfoDefaultWaitCount_ = Integer.parseInt(waitCount[0]);
    	
    	if(nameServerInfo.waitCount==0)
        	nameServerInfo.waitCount = serverInfoDefaultWaitCount_;
    	if(nameServerInfo.dir.equals(""))
        	nameServerInfo.dir = serverInfoDefaultDir_;
    }
    
    private String comToAbsolutePath(String com){
    	if((new File(com)).isAbsolute())
    		return null;
    	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
    	String serverDir = store.getString("SERVER_DIR");
        if(!serverDir.equals("")){
        	File file = new File(serverDir);
        	String[] list = file.list();
        	for(int i=0; i<list.length; i++){
        		int endIndex = list[i].lastIndexOf(".");
        		if(endIndex<0)
        			endIndex = list[i].length();
        		String s=list[i].substring(0, endIndex);
        		if(s.equals(com)){
        			return serverDir+"/"+com;
        		}
        	}
        }
        return null;
    }
    
    public static void setServerInfoToPreferenceStore(){
    	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
    	String id = PreferenceConstants.ALLSERVER;
    	String com = "\"\"";
    	String args = "\"\"";
    	String autoStart= "false";
    	String useORB= "false";
    	for(int i=0; i<vecServerInfo.size(); i++){
    		ProcessInfo processInfo =vecServerInfo.elementAt(i);
    		id += PreferenceConstants.SEPARATOR + processInfo.id;
    		com += PreferenceConstants.SEPARATOR + "\""+processInfo.com.get(processInfo.editComIndex)+"\"";
    		args += PreferenceConstants.SEPARATOR + "\""+processInfo.args+"\"";
    		autoStart += PreferenceConstants.SEPARATOR + (processInfo.autoStart? "true" : "false");
    		useORB += PreferenceConstants.SEPARATOR + (processInfo.useORB? "true" : "false");
    	}
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.ID, id);
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.COM, com);
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.ARGS, args);
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.AUTOSTART, autoStart);
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.USEORB, useORB);
    }
    
    public static void setNameServerInfoToPreferenceStore(){
    	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.PORT, 
    			newPort_.get());
    	store.setValue(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.HOST, 
    			newHost_.get());
    }
    
    public void restoreDefault(){
    	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.PORT);
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.NAMESERVER+"."+PreferenceConstants.HOST);
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.ID);
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.COM);
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.ARGS);
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.AUTOSTART);
    	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.USEORB);
    	initialize();
    }
    
}

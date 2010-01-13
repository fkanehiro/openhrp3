/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/*
 *  GrxProcessManager.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  2004/03/16
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
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import jp.go.aist.hrp.simulator.ServerObject;
import jp.go.aist.hrp.simulator.ServerObjectHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Element;

import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.util.SynchronizedAccessor;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.GrxServerManager;
import com.generalrobotix.ui.util.FileUtil;

public class GrxProcessManager {
    private static GrxProcessManager      GrxProcessManagerThis_            = null;
    
    private java.util.List<AProcess>      process_         = null;
    private boolean                       isEnd_           = false;
    private SynchronizedAccessor<Boolean> isType_          = new SynchronizedAccessor<Boolean>(true);
    private Composite                     outputComposite_ = null;
    private StyledText                    outputArea_      = null;
    private StringBuffer                  outputBuffer_    = null;
    private ConcurrentLinkedQueue<String> lineQueue        = null;
    private Thread                        thread_          = null;

    private GrxProcessManager() {
        process_ = new java.util.ArrayList<AProcess>();
        outputComposite_ = null;
        outputArea_ = null;
        outputBuffer_ = new StringBuffer();
        lineQueue = new ConcurrentLinkedQueue<String>();
    }

    public static synchronized GrxProcessManager getInstance() {
        if (GrxProcessManagerThis_ == null) {
            GrxProcessManagerThis_ = new GrxProcessManager();
        }
        return GrxProcessManagerThis_;
    }

    public static synchronized void shutDown() {
        if (GrxProcessManagerThis_ != null) {
            GrxProcessManagerThis_.autoStop();
            GrxProcessManagerThis_.stopThread();
            GrxProcessManagerThis_.process_.clear();
            GrxProcessManagerThis_.lineQueue.clear();
            GrxProcessManagerThis_ = null;
        }
    }

    public void initializeComposite(Composite output) {
        outputComposite_ = output;
        outputArea_ = new StyledText(outputComposite_, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        outputArea_.setEditable(false);

        // 右クリックメニューを自動再生成させる
        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager menu) {
                for (Action action : getOutputMenu()) {
                    menu.add(action);
                }
            }
        });
        outputArea_.setMenu(manager.createContextMenu(outputArea_));
    }

    public void setProcessList(Element root) {
        GrxServerManager serverManager_ = new GrxServerManager();
        StringBuffer nsHost = new StringBuffer(""); //$NON-NLS-1$
        StringBuffer nsPort = new StringBuffer(""); //$NON-NLS-1$
        Activator.refNSHostPort(nsHost, nsPort);
        String nsOpt = " -ORBInitRef NameService=corbaloc:iiop:" + nsHost + ":" + nsPort + "/NameService"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if(!GrxCorbaUtil.isAliveNameService()){
            ProcessInfo pi = serverManager_.getNameServerInfo();
            FileUtil.deleteNameServerLog(serverManager_.getNameserverLogDir());
            if ( isRegistered(pi) ) {
                if( !isRunning(pi) ){
                    unregister(pi.id);
                    register(pi);
                }
            } else {
                register(pi);
            }
        }
        for(ProcessInfo pi : serverManager_.getServerInfo()){
            ProcessInfo localPi = pi.expandEnv();
            if (localPi.useORB) {
                localPi.com.set(0, localPi.com.get(0) + " " + localPi.args + nsOpt);
            } else {
                localPi.com.set(0, localPi.com.get(0) + " " + localPi.args);
            }
            
            
            if ( isRegistered(localPi) ) {
                if( !isRunning(localPi) ){
                    unregister(localPi.id);
                    register(localPi);
                }
            } else {
                register(localPi);
            }
        }
        autoStart();
    }

    public boolean isRunning(ProcessInfo pi) {
        AProcess localProcess = get(pi.id);
        if (localProcess != null) {
            return localProcess.isRunning();
        }
        return false;
    }

    public boolean isRegistered(ProcessInfo pi) {
        if ( get(pi.id) != null) {
            return true;
        }
        return false;
    }

    
    public boolean register(ProcessInfo pi) {
        if (get(pi.id) != null) {
            return false;
        }
        process_.add(new AProcess(pi));
        pi.print();
        return true;
    }

    public boolean unregister(String id) {
        AProcess p = get(id);
        if (p == null) {
            GrxDebugUtil.println("can't find '" + id + "' on processManager."); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        if (p.isRunning()) {
            GrxDebugUtil.println(id + " is running."); //$NON-NLS-1$
            return false;
        }
        if (process_.remove(p)) {
            GrxDebugUtil.println(id + " is unregistered."); //$NON-NLS-1$
            return true;
        }

        return false;
    };

    public void clearList() {
        process_.clear();
    }

    public AProcess get(String name) {
        for (int i = 0; i < process_.size(); i++) {
            if (get(i).pi_.id.equals(name)) {
                return get(i);
            }
        }
        return null;
    }

    public AProcess get(int n) {
        if (0 <= n && n < process_.size()) {
            return process_.get(n);
        }
        return null;
    }

    public int size() {
        return process_.size();
    }

    public void autoStart() {
        for (int i = 0; i < process_.size(); i++) {
            AProcess p = get(i);
            if (p.pi_.autoStart) {
                try {
                    (p.getReference())._non_existent();
                } catch (Exception e) {
                    p.start(null);
                }
            }
        }
    }

    public void autoStop() {
        for (int i = process_.size(); i > 0; i--) {
            AProcess p = get(i - 1);
            if (p.pi_.autoStop) {
                p.stop();
            }
        }
    }

    private void killall(String pname) {
        try {
            if (!System.getProperty("os.name").equals("Linux")) { //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            ProcessInfo pi = new ProcessInfo();
            pi.id = "killall"; //$NON-NLS-1$
            pi.com.add("/usr/bin/killall"); //$NON-NLS-1$
            pi.dir = "/usr/bin/"; //$NON-NLS-1$
            pi.autoStart = false;
            pi.autoStop = true;
            pi.waitCount = 0;
            register(pi);
            GrxDebugUtil.println("\nkillall " + pname + ":"); //$NON-NLS-1$ //$NON-NLS-2$
            AProcess p = get(pi.id);
            p.start(pname);
            p.waitFor();
            GrxDebugUtil.println(p.readBuffer());
            p.clearBuffer();
            // p.stop();
        } catch (Exception e) {
            GrxDebugUtil.printErr("killall:", e); //$NON-NLS-1$
        }
    }

    private String psaxGrep(String str) {
        try {
            if (!System.getProperty("os.name").equals("Linux")) { //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
            ProcessInfo pi = new ProcessInfo();
            pi.id = "psaxGrep"; //$NON-NLS-1$
            pi.com.add("/bin/ps ax"); //$NON-NLS-1$
            pi.dir = "/bin/"; //$NON-NLS-1$
            pi.autoStart = false;
            pi.autoStop = true;
            pi.waitCount = 0;
            register(pi);
            GrxDebugUtil.println("\nps ax:"); //$NON-NLS-1$
            AProcess p = get(pi.id);
            p.clearBuffer();
            p.start(null);
            p.waitFor();
            // p.stop();
            String ret = p.readBuffer();
            GrxDebugUtil.println(ret);
            String[] rets = ret.split("\n"); //$NON-NLS-1$
            ret = ""; //$NON-NLS-1$
            for (int i = 0; i < rets.length; i++) {
                if (rets[i].indexOf(str + " ") != -1) { //$NON-NLS-1$
                    ret += rets[i] + "\n"; //$NON-NLS-1$
                }
            }
            return ret;
        } catch (Exception e) {
            GrxDebugUtil.printErr("psaxGrep:", e); //$NON-NLS-1$
        }
        return null;
    }
    
    public  List<Integer> getPID(String processName){
        int splitNum = 1;
        List<Integer> ret = new ArrayList<Integer>();
        String []commands={"tasklist","/NH"}; //$NON-NLS-1$
        String key = new String(processName + ".exe"); //$NON-NLS-1$
        Runtime r = Runtime.getRuntime();
        if (System.getProperty("os.name").equals("Linux") ||    //$NON-NLS-1$
            System.getProperty("os.name").equals("Mac OS X")) { //$NON-NLS-1$
            commands[0] = new String ("/bin/ps"); //$NON-NLS-1$
            commands[1] = new String ("axh"); //$NON-NLS-1$
            key = new String("/" + processName);
            splitNum = 0;
        }
        
        try{
            Process p = r.exec(commands);
            p.waitFor();
            InputStream in = p.getInputStream(); 
            p.getOutputStream().toString();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if( line.indexOf(key) >= 0){
                    line = line.replaceAll("^[\\s]+", "");
                    String[] splitLine = line.split("[\\s]+");
                    if( splitLine.length > splitNum ){
                        try {
                            Integer pid = Integer.valueOf(splitLine[splitNum]);
                            if(pid > 0 ){
                                ret.add(pid);
                            }
                        } catch (NumberFormatException ex){
                            ex.printStackTrace();
                        }
                    }       
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static class ProcessInfo implements Cloneable{
        public String       id            = null;
        public List<String> com           = new ArrayList<String>();
        public List<String> env           = new ArrayList<String>();
        public String       dir           = null;
        public int          waitCount     = -1;
        public boolean      isCorbaServer = false;
        public boolean      hasShutdown   = false;
        public boolean      doKillall     = false;
        public boolean      autoStart     = true;
        public boolean      autoStop      = true;
        public String       args          = null;
        public boolean      useORB        = false;

        public void print() {
            GrxDebugUtil.println("\nID: " + id); //$NON-NLS-1$
            for (int i = 0; i < com.size(); i++) {
                GrxDebugUtil.println("COM" + i + ": " + com.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (env.size() > 0) {
                for (int i = 0; i < env.size(); i++)
                    GrxDebugUtil.println("ENV" + i + ": " + env.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                GrxDebugUtil.println("ENV: use parent process environment"); //$NON-NLS-1$
            }
            GrxDebugUtil.println("DIR: " + dir); //$NON-NLS-1$
        }
        
        public ProcessInfo expandEnv(){
            ProcessInfo ret = clone();
            
            for(int i = 0; i < com.size(); ++i){
                ret.com.set(i, GrxXmlUtil.expandEnvVal( com.get(i) )); 
            }
            ret.dir = GrxXmlUtil.expandEnvVal(dir);
            ret.args = GrxXmlUtil.expandEnvVal(args);
            
            return ret;
        }
        
        protected ProcessInfo clone(){
            ProcessInfo ret = null;
            try{
                ret = (ProcessInfo)super.clone();
                ret.args = new String(args);
                ret.dir = new String(dir);
                ret.com = new ArrayList<String>();
                for(String i : com){
                    ret.com.add(new String(i));
                }
                ret.env = new ArrayList<String>();
                for(String i : env){
                    ret.com.add(new String(i));
                }
            }catch(CloneNotSupportedException ex){
                ex.printStackTrace();
            }
            return ret;
        }
    }

    public void createThread() {
        if( thread_ == null ){
            thread_ = new Thread() {
                public void run() {
                    while (!isEnd_) {
                        if (GrxProcessManagerThis_ != null) {
                            updateIO();
                        }
                    }
                }
            };
            thread_.start();
        } else {
            startType();
        }
    }

    private void updateIO() {
        for (int i = 0; i < size(); i++) {
            AProcess p = process_.get(i);
            if (p == null || p.expecting_) {
                continue;
            }
            StringBuffer sb = p.readLines();
            if (sb == null || sb.length() == 0) {
                continue;
            }
            String newLine = sb.toString();
            if (outputBuffer_.length() > 50000) {
                outputBuffer_.delete(0, newLine.length());
            }
            outputBuffer_.append(newLine);
            lineQueue.offer(newLine);

            //排他処理
            isType_.lock();
            if( isType_.get() ){
                isType_.unlock();
            } else {
                //GrxProcessManagerViewが無いときはoutputBuffer_に出力文字列を貯めるだけ
                isType_.unlock();
                continue;
            }
            
            if (p.showOutput_) {
                // SWTEDT(イベントディスパッチスレッド)外からの呼び出しなので、SWTEDTに通知してやってもらう
                Display display = Display.getDefault();
                if (display != null && !display.isDisposed()) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            String newLine = null;
                            while ((newLine = lineQueue.poll()) != null) {
                                outputArea_.append(newLine);
                            }
                            outputArea_.setTopIndex(outputArea_.getLineCount());
                        }
                    });
                }
            }
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Vector<Action> getRunMenu() {
        Vector<Action> vector = new Vector<Action>(size());
        for (int i = 0; i < size(); i++) {
            final AProcess p = get(i);
            Action action = new Action(p.pi_.id, Action.AS_CHECK_BOX) {
                public void run() {
                    if (p.isRunning())
                        p.stop();
                    else
                        p.start(null);
                    setChecked(p.isRunning());
                }
            };
            action.setChecked(p.isRunning());
            vector.add(action);
        }

        Action actionClearAll = new Action(MessageBundle.get("GrxProcessManager.menu.clearAll0"), Action.AS_PUSH_BUTTON) { //$NON-NLS-1$
            public void run() {
                outputArea_.setText(""); //$NON-NLS-1$
                outputBuffer_ = new StringBuffer();
            }
        };
        vector.add(actionClearAll);

        return vector;
    }

    public Composite getOutputComposite() {
        return outputComposite_;
    }

    public StyledText getOutputArea() {
        return outputArea_;
    }

    private Vector<Action> getOutputMenu() {
        Vector<Action> vector = new Vector<Action>(size());
        for (int i = 0; i < size(); i++) {
            final AProcess p = get(i);
            Action action = new Action(p.pi_.id, Action.AS_CHECK_BOX) {
                public void run() {
                    p.showOutput_ = isChecked();
                    String[] processed = outputBuffer_.toString().split("\n"); //$NON-NLS-1$
                    outputArea_.setText(""); //$NON-NLS-1$
                    for (int i = 0; i < processed.length; i++) {
                        for (int j = 0; j < size(); j++) {
                            AProcess p2 = get(j);
                            if (p2.showOutput_ && processed[i].startsWith("[" + p2.pi_.id)) { //$NON-NLS-1$
                                outputArea_.append(processed[i] + "\n"); //$NON-NLS-1$
                            }

                        }
                    }
                }
            };
            action.setChecked(p.showOutput_);
            vector.add(action);
        }

        Action actionClearAll = new Action(MessageBundle.get("GrxProcessManager.menu.clearAll1"), Action.AS_PUSH_BUTTON) { //$NON-NLS-1$
            public void run() {
                outputArea_.setText(""); //$NON-NLS-1$
                outputBuffer_ = new StringBuffer();
            }
        };
        vector.add(actionClearAll);
        return vector;

    }

    public void stopType() {
        isType_.lock();
        isType_.set(false);
        isType_.unlock();
    }

    public void startType() {
        isType_.lock();
        isType_.set(true);
        isType_.unlock();
    }
    
    public void stopThread() {
        isEnd_ = true;
        try {
        	if(thread_!=null)
        		thread_.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public class AProcess {
        // configs
        public ProcessInfo     pi_         = null;
        private Process        process_    = null;
        private StringBuffer   com_        = null;
        private String[]       env_        = null;
        private File           dir_        = null;
        private boolean        showOutput_ = true;

        // variables
        private boolean        expecting_  = false;
        private InputStream    is_         = null;
        private BufferedReader br_         = null;
        private InputStream    es_         = null;
        private BufferedReader bre_        = null;
        private PrintStream    ps_         = null;
        private StringBuffer   buf_        = null;

        public AProcess(ProcessInfo pi) {
            pi_ = pi;
            updateCom();
            if (pi_.env.size() > 0) {
                env_ = new String[pi_.env.size()];
                for (int i = 0; i < pi_.env.size(); i++)
                    env_[i] = pi_.env.get(i);
            }

            try {
                dir_ = new File( pi_.dir );
            } catch (Exception e) {
                dir_ = null;
            }

            buf_ = new StringBuffer();
        }

        public void setCom(String com){
            com_ = new StringBuffer(com);
        }
        
        public void updateCom(){
            if (pi_.com.size() > 0) {
                com_ = new StringBuffer();
                for (int i = 0; i < pi_.com.size(); i++)
                    com_.append(pi_.com.get(i) + " "); //$NON-NLS-1$
            }
        }
        
        public boolean start(String opt) {
            // StatusOut.append("\nStarting "+pi_.id+" ... ");
            if (isRunning()) {
                // StatusOut.append("already running.\n");
            } else {
                Runtime rt = Runtime.getRuntime();
                try {
                    if (opt == null) {
                        opt = ""; //$NON-NLS-1$
                    }
                    GrxDebugUtil.println(com_.toString() + " " + opt); //$NON-NLS-1$
                    if(dir_ != null){
                        if(!dir_.exists())
                        	dir_ = null;
                    }
                    process_ = rt.exec(com_.toString() + " " + opt, env_, dir_); //$NON-NLS-1$

                    is_ = process_.getInputStream();
                    br_ = new BufferedReader(new InputStreamReader(is_));
                    es_ = process_.getErrorStream();
                    bre_ = new BufferedReader(new InputStreamReader(es_));
                    ps_ = new PrintStream(process_.getOutputStream());

                    if (pi_.waitCount > 0) {
                        Thread.sleep(pi_.waitCount);
                    }
                    GrxDebugUtil.println("start:OK(" + pi_.id + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                    return true;
                } catch (Exception e) {
                    process_ = null;
                    GrxDebugUtil.printErr("start:NG(" + pi_.id + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                }
            }
            return true;
        }

        public boolean stop() {
            GrxDebugUtil.println("[PMView] stop:stopping " + pi_.id); //$NON-NLS-1$
            // StatusOut.append("\nStopping "+pi_.id+" ... ");
            if (isRunning()) {
                if (pi_.hasShutdown) {
                    return shutdown();
                }
                try {
                    if (pi_.doKillall) {
                        String com0 = (pi_.com.get(0));
                        String path = com0.split(" ")[0]; //$NON-NLS-1$
                        String name = new File(path).getName();
                        killall(name);
                    } else {
                        process_.destroy();
                        process_.waitFor();
                    }
                    if (!isRunning()) {
                        process_ = null;
                        // StatusOut.append("OK\n");
                        GrxDebugUtil.println("stop:OK(" + pi_.id + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                        GrxServerManager local = new GrxServerManager();
                        if (pi_.id.equals(local.getNameServerInfo().id)) { //$NON-NLS-1$
                            GrxCorbaUtil.removeNameServiceFromList();
                        }
                        return true;
                    }
                    // StatusOut.append("NG\n");
                    GrxDebugUtil.println("stop:NG(" + pi_.id + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                } catch (Exception e) {
                    // StatusOut.append("NG\n");
                    GrxDebugUtil.printErr("stop:NG(" + pi_.id + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } else {
                // StatusOut.append("not running.\n");
                GrxDebugUtil.println("stop:" + pi_.id + " is not running."); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }
            return false;
        }

        private boolean shutdown() {
            try {
                // StatusOut.append("\nShutting down "+pi_.id+" ... ");
                org.omg.CORBA.Object obj = GrxCorbaUtil.getReference(pi_.id);
                ServerObject serverObj = ServerObjectHelper.narrow(obj);
                serverObj.shutdown();
                // StatusOut.append("OK\n");
                GrxDebugUtil.println("shutdown:OK(" + pi_.id + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            } catch (Exception e) {
                // StatusOut.append("NG\n");
                GrxDebugUtil.printErr("shutdown:NG(" + pi_.id + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
            } finally {
                process_.destroy();
                process_ = null;
            }
            return false;
        }

        private void closeReader() {
            try {
                br_.close();
                is_.close();
                bre_.close();
                es_.close();
            } catch (IOException e) {
                GrxDebugUtil.printErr("ProcessManager.closeReader:" + pi_.id + " couldn't close input stream", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        public boolean isRunning() {
            try {
                process_.exitValue();
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isOpenHRPObject() {
            return pi_.hasShutdown;
        }

        private String expect(String key) {
            String[] k = { key };
            return expect(k);
        }

        private String expect(String[] key) {
            expecting_ = true;
            String str = checkKey(buf_, key);
            while (isRunning()) {
                if (str != null) {
                    expecting_ = false;
                    return str;
                }
                str = checkKey(readLines(), key);
                try {
                    Thread.sleep(10);
                } catch (Exception e1) {}
            }
            str = checkKey(readLines(), key);
            expecting_ = false;
            return str;
        }

        private String checkKey(StringBuffer str, String[] key) {
            if (str != null) {
                for (int i = 0; i < key.length; i++) {
                    if (str.indexOf(key[i]) != -1) {
                        return key[i];
                    }
                }
            }
            return null;
        }

        private StringBuffer readLine(BufferedReader br) {
            if (br == null)
                return null;

            StringBuffer ret = new StringBuffer();
            try {
                while (br.ready()) {
                    char c = (char) br.read();
                    ret.append(c);
                    if (c == '\n')
                        break;
                }
            } catch (Exception e) {
                GrxDebugUtil.printErr("ProcessManager.readLine:(" + pi_.id + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (ret.length() == 0) {
                return null;
            }
            return ret;
        }

        private StringBuffer readLines() {
            if (isRunning() || expecting_) {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < 100; i++) {
                    StringBuffer line1 = readLine(br_);
                    StringBuffer line2 = readLine(bre_);
                    if (line1 != null) {
                        buf.append("[" + pi_.id + ":O] " + line1); //$NON-NLS-1$ //$NON-NLS-2$
                    } else if (line2 == null) {
                        break;
                    }
                    if (line2 != null) {
                        buf.append("[" + pi_.id + ":E] " + line2); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }

                buf_.append(buf);
                return buf;
            }
            return null;
        }

        private String readBuffer() {
            String ret = buf_.toString();
            clearBuffer();
            return ret;
        }

        private void clearBuffer() {
            buf_.delete(0, buf_.toString().length());
        }

        private void println(String line) {
            if (ps_ != null) {
                ps_.println(line + "\n"); //$NON-NLS-1$
                ps_.flush();
            }
        }

        private void waitFor() {
            try {
                process_.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private org.omg.CORBA.Object getReference() {
            return GrxCorbaUtil.getReference(pi_.id);
        }

    }
}

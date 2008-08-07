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

public class GrxProcessManager {
	private static GrxProcessManager this_ = null;
	private java.util.List<AProcess> process_ = new java.util.ArrayList<AProcess>();
	private boolean isEnd_ = false;
    
    private Composite outputComposite_;
	private StyledText outputArea_;
	private StringBuffer outputBuffer_;
    
	private ConcurrentLinkedQueue<String> lineQueue = new ConcurrentLinkedQueue<String>();
	
	public GrxProcessManager(Composite output) {
        initialize(output);
    }

	public GrxProcessManager(Composite output,java.util.List pInfo) {
        initialize(output);
		for (int i = 0; i < pInfo.size(); i++)
			process_.add(new AProcess((ProcessInfo) pInfo.get(i)));
	}

	public GrxProcessManager(Composite output,ProcessInfo[] pInfo) {
        initialize(output);
		for (int i = 0; i < pInfo.length; i++)
			process_.add(new AProcess(pInfo[i]));
	}

	public static GrxProcessManager getInstance(Composite output) {
		if (this_ == null) {
			this_ = new GrxProcessManager(output);
		}
		return this_;
	}
    
    
    private void initialize(Composite output){
        this_ = this;
        outputComposite_ = output;
        outputArea_ = new StyledText(outputComposite_,SWT.MULTI|SWT.BORDER|SWT.V_SCROLL|SWT.H_SCROLL);
        outputArea_.setEditable(false);
        //outputArea_.setWordWrap(true);

        outputBuffer_ = new StringBuffer();
        
        // 右クリックメニューを自動再生成させる
        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {
        	public void menuAboutToShow(IMenuManager menu) {
                for(Action action : getOutputMenu()){
                    menu.add(action);
                }
            }
        } );
        outputArea_.setMenu(manager.createContextMenu(outputArea_));
    }
    
	public boolean register(ProcessInfo pi) {
		if (get(pi.id) != null)
			return false;
		process_.add(new AProcess(pi));
		GrxDebugUtil.println("\nID: " + pi.id);
		for (int i = 0; i < pi.com.size(); i++)
			GrxDebugUtil.println("COM" + i + ": " + pi.com.get(i));
		if (pi.env.size() > 0) {
			for (int i = 0; i < pi.env.size(); i++)
				GrxDebugUtil.println("ENV" + i + ": " + pi.env.get(i));
		} else {
			GrxDebugUtil.println("ENV: use parent process environment");
		}
		GrxDebugUtil.println("DIR: " + pi.dir);
		return true;
	}

	public boolean unregister(String id) {
		AProcess p = get(id);
		if (p == null) {
			GrxDebugUtil.println("can't find '" + id + "' on processManager.");
			return false;
		}
		if (p.isRunning()) {
			GrxDebugUtil.println(id + " is running.");
			return false;
		}
		if (process_.remove(p)) {
			GrxDebugUtil.println(id + " is unregistered.");
			return true;
		}

		return false;
	};

	public void clearList() {
		process_.clear();
	}

	public AProcess get(String name) {
		for (int i = 0; i < process_.size(); i++) {
			if (get(i).pi_.id.equals(name))
				return get(i);
		}
		return null;
	}

	public AProcess get(int n) {
		if (0 <= n && n < process_.size())
			return process_.get(n);
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
			if (p.pi_.autoStop)
				p.stop();
		}
	}

	void killall(String pname) {
		try {
			if (!System.getProperty("os.name").equals("Linux")) {
				return;
			}
			ProcessInfo pi = new ProcessInfo();
			pi.id = "killall";
			pi.com.add("/usr/bin/killall");
			pi.dir = "/usr/bin/";
			pi.autoStart = false;
			pi.autoStop = true;
			pi.waitCount = 0;
			register(pi);
			GrxDebugUtil.println("\nkillall " + pname + ":");
			AProcess p = get(pi.id);
			p.start(pname);
			p.waitFor();
			GrxDebugUtil.println(p.readBuffer());
			p.clearBuffer();
			// p.stop();
		} catch (Exception e) {
			GrxDebugUtil.printErr("killall:", e);
		}
	}

	String psaxGrep(String str) {
		try {
			if (!System.getProperty("os.name").equals("Linux")) {
				return null;
			}
			ProcessInfo pi = new ProcessInfo();
			pi.id = "psaxGrep";
			pi.com.add("/bin/ps ax");
			pi.dir = "/bin/";
			pi.autoStart = false;
			pi.autoStop = true;
			pi.waitCount = 0;
			register(pi);
			GrxDebugUtil.println("\nps ax:");
			AProcess p = get(pi.id);
			p.clearBuffer();
			p.start(null);
			p.waitFor();
			// p.stop();
			String ret = p.readBuffer();
			GrxDebugUtil.println(ret);
			String[] rets = ret.split("\n");
			ret = "";
			for (int i = 0; i < rets.length; i++) {
				if (rets[i].indexOf(str + " ") != -1) {
					ret += rets[i] + "\n";
				}
			}
			return ret;
		} catch (Exception e) {
			GrxDebugUtil.printErr("psaxGrep:", e);
		}
		return null;
	}

	public static class ProcessInfo {
		public String id = null;
		public List<String> com = new ArrayList<String>();
		public List<String> env = new ArrayList<String>();
		public String dir = null;
		public int waitCount = -1;
		public String nsHost = null;
		public int nsPort = -1;
		public boolean isCorbaServer = false;
		public boolean hasShutdown = false;
		public boolean doKillall = false;
		public boolean autoStart = true;
		public boolean autoStop = true;
	}

	public void createThread() {
		Thread t = new Thread() {
			public void run() {
				while (!isEnd_) {
   					updateIO();
				}
			}
		};
		t.start();
	}
	
	public void updateIO() {
		for (int i = 0; i < size(); i++) {
			AProcess p = process_.get(i);
			if (p == null || p.expecting_)
				continue;

			StringBuffer sb = p.readLines();
			if (outputArea_ == null || sb == null || sb.length() == 0)
				continue;

			String newLine = sb.toString(); 
			if (outputBuffer_.length() > 50000) {
				outputBuffer_.delete(0, newLine.length());
			}
			outputBuffer_.append(newLine);
			lineQueue.offer( newLine );

			if (p.showOutput_) {
				// SWTEDT(イベントディスパッチスレッド)外からの呼び出しなので、SWTEDTに通知してやってもらう
				Display display = Display.getDefault();
				if( display != null && ! display.isDisposed() ) {
					display.asyncExec( new Runnable(){
	                    public void run(){
	                    	String newLine=null;
	                    	while( ( newLine = lineQueue.poll() ) != null ) {
	                    		outputArea_.append( newLine );
	                    	}
	                    	outputArea_.setTopIndex( outputArea_.getLineCount() );
	                    }
	                } );
				}
			}
		}

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}
	}
	
	public Vector<Action> getRunMenu() {
        Vector<Action> vector = new Vector<Action>(size());
        for(int i=0;i<size();i++){
            final AProcess p = get(i);
            Action action = new Action(p.pi_.id,Action.AS_CHECK_BOX){
                public void run(){
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
        
        Action actionClearAll = new Action("Clear All!",Action.AS_PUSH_BUTTON){
            public void run(){
                outputArea_.setText("");
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

    private Vector<Action> getOutputMenu(){
        Vector<Action> vector = new Vector<Action>(size());
        for(int i=0;i<size();i++){
            final AProcess p = get(i);
            Action action = new Action(p.pi_.id,Action.AS_CHECK_BOX){
                public void run(){
                    p.showOutput_ = isChecked();
                    String[] processed = outputBuffer_.toString().split("\n");
                    outputArea_.setText("");
                    for (int i = 0; i < processed.length; i++) {
                        for (int j = 0; j < size(); j++) {
                            AProcess p2 = get(j);
                            if (p2.showOutput_ && processed[i].startsWith("[" + p2.pi_.id)) {
                                outputArea_.append(processed[i] + "\n");
                            }
                            
                        }
                    };
               }
            };
            action.setChecked(p.showOutput_);
            vector.add(action);
        }
        
        Action actionClearAll = new Action("Clear All",Action.AS_PUSH_BUTTON){
            public void run(){
                outputArea_.setText("");
                outputBuffer_ = new StringBuffer();
            }
        };
        vector.add(actionClearAll);
        return vector;
        
    }
	
	public void stopThread() {
		isEnd_ = true;
	}

	public class AProcess {
		// configs
		public ProcessInfo pi_ = null;
		private Process process_ = null;
		private StringBuffer com_ = null;
		private String[] env_ = null;
		private File dir_ = null;
		private boolean showOutput_ = true;

		// variables
		private boolean expecting_ = false;
		private InputStream is_ = null;
		private BufferedReader br_ = null;
		private InputStream es_ = null;
		private BufferedReader bre_ = null;
		private PrintStream ps_ = null;
		private StringBuffer buf_ = null;
		
		public AProcess(ProcessInfo pi) {
			pi_ = pi;

			if (pi_.com.size() > 0) {
				com_ = new StringBuffer();
				for (int i = 0; i < pi_.com.size(); i++)
					com_.append(pi_.com.get(i) + " ");
			}

			if (pi_.env.size() > 0) {
				env_ = new String[pi_.env.size()];
				for (int i = 0; i < pi_.env.size(); i++)
					env_[i] = pi_.env.get(i);
			}

			try {
				dir_ = new File(pi_.dir);
			} catch (Exception e) {
				dir_ = null;
			}

			buf_ = new StringBuffer();
		}

		public boolean start(String opt) {
			// StatusOut.append("\nStarting "+pi_.id+" ... ");
			if (isRunning()) {
				// StatusOut.append("already running.\n");
			} else {
				Runtime rt = Runtime.getRuntime();
				try {
					if (opt == null)
						opt = "";
					GrxDebugUtil.println(com_.toString() + " " + opt);
					process_ = rt.exec(com_.toString() + " " + opt, env_, dir_);

					is_ = process_.getInputStream();
					br_ = new BufferedReader(new InputStreamReader(is_));
					es_ = process_.getErrorStream();
					bre_ = new BufferedReader(new InputStreamReader(es_));
					ps_ = new PrintStream(process_.getOutputStream());

					if (pi_.waitCount > 0)
						Thread.sleep(pi_.waitCount);
					GrxDebugUtil.println("start:OK(" + pi_.id + ")");
					// StatusOut.append("OK\n");
					return true;
				} catch (Exception e) {
					process_ = null;
					GrxDebugUtil.printErr("start:NG(" + pi_.id + ")", e);
					// StatusOut.append("NG\n");
					return false;
				}
			}
			return true;
		}

		public boolean stop() {
			GrxDebugUtil.println("stop:stoping " + pi_.id);
			// StatusOut.append("\nStopping "+pi_.id+" ... ");
			if (isRunning()) {
				if (pi_.hasShutdown) {
					return shutdown();
				}
				try {
					if (pi_.doKillall) {
						String com0 = (pi_.com.get(0));
						String path = com0.split(" ")[0];
						String name = new File(path).getName();
						killall(name);
					} else {
						process_.destroy();
					}
					if (!isRunning()) {
						process_ = null;
						// StatusOut.append("OK\n");
						GrxDebugUtil.println("stop:OK(" + pi_.id + ")");
						return true;
					}
					// StatusOut.append("NG\n");
					GrxDebugUtil.println("stop:NG(" + pi_.id + ")");
					return false;
				} catch (Exception e) {
					// StatusOut.append("NG\n");
					GrxDebugUtil.printErr("stop:NG(" + pi_.id + ")", e);
				}
			} else {
				// StatusOut.append("not running.\n");
				GrxDebugUtil.println("stop:" + pi_.id + " is not running.");
				return true;
			}
			return false;
		}

		boolean shutdown() {
			try {
				// StatusOut.append("\nShutting down "+pi_.id+" ... ");
				org.omg.CORBA.Object obj = GrxCorbaUtil.getReference(pi_.id,
						pi_.nsHost, pi_.nsPort);
				ServerObject serverObj = ServerObjectHelper.narrow(obj);
				serverObj.shutdown();
				// StatusOut.append("OK\n");
				GrxDebugUtil.println("shutdown:OK(" + pi_.id + ")");
				return true;
			} catch (Exception e) {
				// StatusOut.append("NG\n");
				GrxDebugUtil.printErr("shutdown:NG(" + pi_.id + ")", e);
			} finally {
				process_.destroy();
				process_ = null;
			}
			return false;
		}

		void closeReader() {
			try {
				br_.close();
				is_.close();
				bre_.close();
				es_.close();
			} catch (IOException e) {
				GrxDebugUtil.printErr("ProcessManager.closeReader:" + pi_.id
						+ " couldn't close input stream", e);
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

		boolean isOpenHRPObject() {
			return pi_.hasShutdown;
		}

		String expect(String key) {
			String[] k = { key };
			return expect(k);
		}

		String expect(String[] key) {
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
				} catch (Exception e1) {
				}
			}
			str = checkKey(readLines(), key);
			expecting_ = false;
			return str;
		}

		String checkKey(StringBuffer str, String[] key) {
			if (str != null) {
				for (int i = 0; i < key.length; i++) {
					if (str.indexOf(key[i]) != -1)
						return key[i];
				}
			}
			return null;
		}

		StringBuffer readLine(BufferedReader br) {
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
				GrxDebugUtil.printErr("ProcessManager.readLine:(" + pi_.id + ")", e);
			}
			if (ret.length() == 0)
				return null;
			return ret;
		}

		StringBuffer readLines() {
			if (isRunning() || expecting_) {
				StringBuffer buf = new StringBuffer();
				for (int i = 0; i < 100; i++) {
					StringBuffer line1 = readLine(br_);
					StringBuffer line2 = readLine(bre_);
					if (line1 != null)
						buf.append("[" + pi_.id + ":O] " + line1);
					else if (line2 == null)
						break;
					if (line2 != null)
						buf.append("[" + pi_.id + ":E] " + line2);
				}

				buf_.append(buf);
				return buf;
			}
			return null;
		}

		String readBuffer() {
			String ret = buf_.toString();
			clearBuffer();
			return ret;
		}

		void clearBuffer() {
			buf_.delete(0, buf_.toString().length());
		}

		void println(String line) {
			if (ps_ != null) {
				ps_.println(line + "\n");
				ps_.flush();
			}
		}

		void waitFor() {
			try {
				process_.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		org.omg.CORBA.Object getReference() {
			return GrxCorbaUtil.getReference(pi_.id, pi_.nsHost, pi_.nsPort);
		}
        
	}
}

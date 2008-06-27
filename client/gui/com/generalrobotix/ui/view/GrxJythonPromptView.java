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
 *  GrxJythonPromptView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Component;
import java.io.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.python.core.PyList;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.item.GrxPythonScriptItem;
import com.generalrobotix.ui.util.*;

@SuppressWarnings("serial")
public class GrxJythonPromptView extends GrxBaseView {
    public static final String TITLE = "Jython Prompt";

	private PythonInterpreter interpreter_ = new PythonInterpreter();
	private Thread thread_1_;
	private Thread thread_2_;

	private JToggleButton btnExec_ = new JToggleButton(SIM_SCRIPT_START_ICON);
	private JTextAreaEx area_ = new JTextAreaEx();
	private JScrollPane scArea_ = new JScrollPane(area_);
	private static final int DEFAULT_MAX_ROW = 500;
	private String prompt_ = ">>> ";
	private Writer writer_;
	
	private JSplitPane spltPane_ = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private MenuDialog commandPane_;
	private GrxPythonScriptItem currentItem_;
	
	private static final int HISTORY_SIZE = 50;
	private List<String> history_ = new LinkedList<String>();
	private int hpos_ = 0;
	
	private static final KeyStroke KS_SHIFT = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_MASK);
	private static final KeyStroke KS_CTRL  = KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, KeyEvent.CTRL_MASK);
	
	private static final KeyStroke KS_CTRL_C = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_U = KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_A = KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_E = KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_F = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_B = KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_N = KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_CTRL_P = KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK);
	
	private static final KeyStroke KS_BS  = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
	private static final KeyStroke KS_DEL = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
	private static final KeyStroke KS_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP,0);
	private static final KeyStroke KS_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0);
	private static final KeyStroke KS_LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0);
	private static final KeyStroke KS_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0);
	private static final KeyStroke KS_ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0);
	private static final KeyStroke KS_ENTER_ALT = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,KeyEvent.ALT_MASK);
	private static final KeyStroke KS_ENTER_CTRL = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,KeyEvent.CTRL_MASK);
	private static final KeyStroke KS_ENTER_SHIFT = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,KeyEvent.SHIFT_MASK);
	
	private static final ImageIcon SIM_SCRIPT_START_ICON = new ImageIcon(java.awt.Toolkit.getDefaultToolkit().
		getImage(GrxJythonPromptView.class.getResource( "/resources/images/sim_script_start.png")));
	private static final ImageIcon SIM_SCRIPT_STOP_ICON = new ImageIcon(java.awt.Toolkit.getDefaultToolkit().
		getImage(GrxJythonPromptView.class.getResource( "/resources/images/sim_script_stop.png")));
	
	public GrxJythonPromptView(String name, GrxPluginManager manager) {
		super(name, manager);
		isScrollable_ = false;

		JToolBar toolBar = new JToolBar();
		toolBar.add(btnExec_);
		setToolBar(toolBar);
		
		btnExec_.setToolTipText("execute script file");
		btnExec_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		btnExec_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		btnExec_.setEnabled(false);
		btnExec_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (btnExec_.isSelected()) {
					btnExec_.setIcon(SIM_SCRIPT_STOP_ICON);
					btnExec_.setToolTipText("interrupt python threads");
					execFile();
				} else {
					if (commandPane_ == null) {
						if (JOptionPane.showConfirmDialog(manager_.getFrame(),
								"Are you sure stop script ?\n",
								"exec script",JOptionPane.OK_CANCEL_OPTION) 
								== JOptionPane.CANCEL_OPTION) {
							btnExec_.setSelected(true);
							return;
						}
					} else {
			   			if (!commandPane_.exit()) { 
							btnExec_.setSelected(true);
							return;
						}
					}

					btnExec_.setIcon(SIM_SCRIPT_START_ICON);
					btnExec_.setToolTipText("execute script file");
					area_.setEnabled(true);
					interrupt(thread_2_);
				}
			}
		});
		
		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(scArea_);

		area_.setEditable(false);
		area_.setText(prompt_);
		area_.setCaretPosition(prompt_.length());
		area_.setCaretColor(Color.blue);
		area_.setLineWrap(true);
		area_.addKeyListener(new ConsoleKeyAdapter());
		area_.setEditable(true);
		area_.setMaximumRowCount(DEFAULT_MAX_ROW);
		//area_.setCursor(Cursor.getDefaultCursor());

		writer_ = new BufferedWriter(new PrintWriter(new JTextComponentWriter(area_)));
		interpreter_.setErr(writer_);
		interpreter_.setOut(writer_);
		interpreter_.set("uimanager", manager_);

		interpreter_.exec("import sys");
		interpreter_.exec("sys.path.append('script')");
		interpreter_.exec("import rbimporter");
		URL[] urls = manager_.pluginLoader_.getURLs();
		for (int i=0; i<urls.length; i++) {
			interpreter_.exec("sys.path.append('"+urls[i].getPath()+"')");
			interpreter_.exec("print \"sys.path.append(\'"+urls[i].getPath()+"\')\"");
		}

		ClassLoader cl = this.getClass().getClassLoader();
		interpreter_.set("auditorClassLoader", cl);
		interpreter_.exec("sys.setClassLoader(auditorClassLoader)");
		interpreter_.exec("del auditorClassLoader");
		interpreter_.exec("import __builtin__");
		
		interpreter_.set("view", this);
		interpreter_.exec("__builtin__.waitInput = view.waitInput");
		interpreter_.exec("__builtin__.waitInputConfirm = view.waitInputConfirm");
		interpreter_.exec("__builtin__.waitInputSelect  = view.waitInputSelect");
		interpreter_.exec("__builtin__.waitInputMessage = view.waitInputMessage");
		interpreter_.exec("__builtin__.waitInputMenu   = view.waitInputMenu");
		interpreter_.exec("__builtin__.showCommandPane = view.waitInputMenuNoDialog");
		interpreter_.exec("__builtin__.waitInputSetMessage = view.waitInputSetMessage");
		interpreter_.exec("del view\n");
		interpreter_.exec("del __builtin__");

		history_.add("");
		JMenuItem initPython = new JMenuItem("cleanup python");
		initPython.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {	
				interpreter_.cleanup();
				interpreter_.exec("rbimporter.refresh()");
			}
		});
		setMenuItem(initPython);

		spltPane_.setOneTouchExpandable(true);
	}
	
	public void interrupt(Thread thread) {
		if (thread != null) {
			try {
				thread.interrupt();
			} catch (Exception e) {
				GrxDebugUtil.printErr("interrupt thread:", e);
			}
		}
	}
	
	private class ConsoleKeyAdapter extends KeyAdapter {
		public void keyPressed(KeyEvent evt) {
			KeyStroke ks = KeyStroke.getKeyStrokeForEvent(evt);
			int len = area_.getText().length();
			int cp = area_.getCaretPosition();

			if (ks == KS_CTRL_C) {
				interrupt(thread_1_);
			} else if (ks == KS_ENTER      || ks == KS_ENTER_ALT
					|| ks == KS_ENTER_CTRL || ks == KS_ENTER_SHIFT) {
			    if (thread_1_ != null)  
					return;

				thread_1_ = new Thread() {
					public void run() {
						String com = getCommand();

						if (com.trim().length() <= 0) {
							area_.append("\n"+prompt_);
						} else {
							area_.append("\n");
							area_.setCaretPosition(area_.getText().length());
							if (com.startsWith("dir(")) 
								com = "print "+com;
							try {
								interpreter_.exec(com);
							} catch (Exception e) {
								showScriptError(e);
							} 
							history_.add(1, com);
							if (history_.size() > HISTORY_SIZE)
				   				history_.remove(HISTORY_SIZE-1);
				   			area_.requestFocus();
				   			area_.append(prompt_);
						}
						hpos_ = 0;
						thread_1_ = null;
						area_.setCaretPosition(area_.getText().length());
					}
				};

				try {
					thread_1_.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (thread_1_ != null) {
				return;
		        //evt.consume();	
			} else if (ks == KS_CTRL_U) {
				area_.replaceRange("", len - getCommand().length(), len);
			} else if (ks == KS_CTRL_A) {
				area_.setCaretPosition(len - getCommand().length());
				evt.consume();
			} else if (ks == KS_CTRL_E) {
				area_.setCaretPosition(len);
			} else if ((ks == KS_CTRL_F || ks == KS_RIGHT)) {
				if (cp < len)
					area_.setCaretPosition(cp + 1);
			} else if ((ks == KS_CTRL_B || ks == KS_LEFT)) {
				if (len - getCommand().length() < cp)
					area_.setCaretPosition(cp - 1);
				
			} else if (ks == KS_SHIFT) {   // ignore input of shift key
			} else if (ks == KS_CTRL) { // and control key
				
			} else if (ks == KS_BS) {
				int p = area_.getText().lastIndexOf('\n') + prompt_.length() + 1;
				if (p < cp) {
					area_.replaceRange("", cp - 1, cp);
					if (hpos_ > 0) {
					    history_.remove(hpos_);
					    history_.add(hpos_, getCommand());
					}
				}
			} else if (ks == KS_DEL) {
				if (cp < len) {
				    area_.replaceRange("", cp, cp + 1);
					if (hpos_ > 0) {
					    history_.remove(hpos_);
					    history_.add(hpos_, getCommand());
					}
				}
			} else if (ks == KS_UP || ks == KS_CTRL_P) {
				if (hpos_ < history_.size() - 1) {
					int start = area_.getText().lastIndexOf('\n') + prompt_.length() + 1;
					if (start <= len)
						area_.replaceRange(history_.get(++hpos_), start, len);
				}
				evt.consume();
			} else if (ks == KS_DOWN || ks == KS_CTRL_N) {
				area_.setCaretPosition(len);
				if (hpos_ > 0) {
					int start = area_.getText().lastIndexOf('\n') + prompt_.length() + 1;
					if (start <= len)
						area_.replaceRange(history_.get(--hpos_), start, len);
				}
				evt.consume();
			} else {
				if (cp <area_.getText().lastIndexOf('\n')) {
					cp = len;
					area_.setCaretPosition(len);
				}
				//if (!arg0.isAltDown() && !arg0.isControlDown()) {
					//area_.insert(String.valueOf(arg0.getKeyChar()), cp);
					if (hpos_ > 0) {
					    history_.remove(hpos_);
					    history_.add(hpos_, getCommand());
					}
				//}
			}
			evt.consume();
		}
		
		public void keyReleased(KeyEvent e){
			KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
			if (ks.getKeyCode() == KS_UP.getKeyCode())
				area_.setCaretPosition(area_.getText().length());
		}
	}

	private void showScriptError(Exception e) {
		if (e.toString().indexOf("Script Canceled.") > 0) {
			JOptionPane.showMessageDialog(manager_.getFrame(), "Script Canceled.");
		} else if (e.toString().indexOf("org.omg.CORBA.COMM_FAILURE:") > 0) {
			String err = e.toString().split("org.omg.CORBA.COMM_FAILURE:")[0];
			JOptionPane.showMessageDialog(manager_.getFrame(), "Communication error occured."+err);
			System.out.println(e.toString());
		} else {
			e.printStackTrace();
		}	
	}

	public void execFile() {
		if (currentItem_ == null)
			return;

		if (currentItem_.isEdited()) {
			Component parent = commandPane_;
		  	if (parent== null)
				parent = manager_.getFrame();
			int ans = JOptionPane.showConfirmDialog(parent,
				"File has been changed. Save before execute",
				"exec script",JOptionPane.OK_CANCEL_OPTION);
			if (ans == JOptionPane.CANCEL_OPTION || !currentItem_.save())
				return;
			final String url = currentItem_.getURL(true);
		}

		execFile(currentItem_.getURL(true));
	}
		
	public void execFile(final String url) {
		if (thread_2_ != null) {
			JOptionPane.showMessageDialog(manager_.getFrame(), 
				"The previous script has been stil running.");
			return;
		}

		File f = new File(url);
		if (!f.isFile()) {
			JOptionPane.showMessageDialog(manager_.getFrame(), 
				"File " + f.getPath() + " is not exist.");
			return;
		}

		File p = f.getParentFile();
		interpreter_.exec("import sys");
		interpreter_.exec("rbimporter.refresh()");
		PyString pyStr = new PyString(p.getAbsolutePath());
		PyList pathList = (PyList)interpreter_.eval("sys.path");
		if (!pathList.__contains__(pyStr)) {
			String com = "sys.path.append(\""+p.getAbsolutePath()+"\")";
			interpreter_.exec("print '"+com+"'");
			interpreter_.exec(com);
		}
		
		thread_2_ = new Thread() {
			public void run() {
				while(!isRunning()) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				btnExec_.setEnabled(true);
				btnExec_.setSelected(true);
				btnExec_.setIcon(SIM_SCRIPT_STOP_ICON);
				btnExec_.setToolTipText("interrupt python threads");

				interpreter_.exec("print 'execfile("+url+")'");
				try {
					interpreter_.execfile(url);
				} catch (Exception e) {
					showScriptError(e);
				} finally {
					if (currentItem_ == null) 
						btnExec_.setEnabled(false);
					btnExec_.setSelected(false);
					btnExec_.setIcon(SIM_SCRIPT_START_ICON);
					btnExec_.setToolTipText("execute script file");
					area_.setCaretPosition(area_.getText().length());
					hpos_ = 0;
					thread_2_ = null;
				}
			}
		};
		thread_2_.start();
	}
	
	private String getCommand() {
		String com = area_.getText();
		int idx = com.lastIndexOf('\n');
		if (idx > 0)
			com = com.substring(idx+1);
		return com.replaceFirst(prompt_, "");
	}
	
	public void restoreProperties() {
		super.restoreProperties();
		String nsHost = manager_.getProjectProperty("nsHost");
		String nsPort = manager_.getProjectProperty("nsPort");
		if (nsHost == null)
			nsHost = "localhost";
		if (nsPort == null)
			nsPort = "2809";
			
		String NS_OPT = "-ORBInitRef NameService=corbaloc:iiop:"+nsHost+":"+nsPort+"/NameService";
		System.setProperty("NS_OPT", NS_OPT);
		interpreter_.cleanup();
		interpreter_.exec("print 'NS_OPT="+NS_OPT+"'");
		interpreter_.exec("print '"+prompt_+"rbimporter.refresh()'");
		interpreter_.exec("rbimporter.refresh()");
		String nameservice = "could not parse";
		try {
			nameservice = NS_OPT.split("=")[1];
			//interpreter_.exec("print '"+prompt_+"import hrp'");
			//interpreter_.exec("import hrp");
			//interpreter_.exec("hrp.initCORBA()");
		} catch (Exception e) {
			interpreter_.exec("print 'failed to connect NameService("+nameservice+")'");
		}
		area_.setMaximumRowCount(getInt("maxRowCount", DEFAULT_MAX_ROW));
		
		String defaultScript = System.getProperty("SCRIPT");
		if (defaultScript != null) {
			defaultScript = manager_.getHomePath() + defaultScript;
			System.clearProperty("SCRIPT");
			File f = new File(defaultScript);
			String name = f.getName().replace(".py", "");
			currentItem_ = (GrxPythonScriptItem)manager_.loadItem(GrxPythonScriptItem.class, name, f.getAbsolutePath());
			execFile();
		}
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		currentItem_ = (GrxPythonScriptItem)manager_.getSelectedItem(GrxPythonScriptItem.class, null);
		btnExec_.setEnabled(currentItem_ != null);
	}
	
	public void waitInput(String msg) {
		Component parent = commandPane_;
	  	if (parent == null)
			parent = manager_.getFrame();
		JOptionPane.showMessageDialog(parent, msg);
	}

	public boolean waitInputConfirm(String msg) throws Exception { 
		Component parent = commandPane_;
	  	if (parent == null)
			parent = manager_.getFrame();
		int ans = JOptionPane.showConfirmDialog(parent, msg, "waitInputConfirm",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
			manager_.ROBOT_ICON);

		if (ans == JOptionPane.OK_OPTION)
			return true;
		
		throw new Exception("Script Canceled.");
	}

	public boolean waitInputSelect(String msg) throws Exception {
		Component parent = commandPane_;
	  	if (parent == null)
			parent = manager_.getFrame();
		int ans = JOptionPane.showConfirmDialog(parent, msg, "waitInputSelect",
			JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
			manager_.ROBOT_ICON);

		if (ans == JOptionPane.YES_OPTION)
			return true;

	    if (ans != JOptionPane.NO_OPTION) 
			throw new Exception("Script Canceled.");

		return false;
	}

	public Object waitInputMessage(String msg) throws Exception {
		Component p = commandPane_;
	  	if (p == null)
			p = manager_.getFrame();
		Object ret = JOptionPane.showInputDialog(p, msg, "waitInputMessage",
			JOptionPane.INFORMATION_MESSAGE, manager_.ROBOT_ICON, null, null);

		if (ret == null) 
			throw new Exception("Script Canceled.");

		return ret;
	}

	public void waitInputMenu(String[][] menuList) {
		waitInputMenu(menuList, false);
	}

	public void waitInputMenuNoDialog(String[][] menuList) {
		waitInputMenu(menuList, true);
	}

	public void waitInputMenu(String[][] menuList, boolean noDialog) {
		commandPane_ = new MenuDialog(menuList);

		JPanel cp = getContentPane();
		if (noDialog) {
			cp.setVisible(false);
			cp.removeAll();
			cp.add(spltPane_);
			spltPane_.setLeftComponent(commandPane_);
			spltPane_.setRightComponent(scArea_);
			cp.setVisible(true);
			spltPane_.setDividerLocation(0.7);

			commandPane_.start(interpreter_);

			cp.setVisible(false);
			cp.removeAll();
			cp.add(scArea_);
			cp.setVisible(true);
		} else {
			JDialog dialog = new JDialog(manager_.getFrame(), "WaitInputMenu" ,false);	
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.setContentPane(commandPane_);
			dialog.setSize(300, 400);
			dialog.setVisible(true);

			commandPane_.start(interpreter_);

			dialog.setVisible(false);
		}

		commandPane_ = null;
	}

	public void waitInputSetMessage(String msg) {
        if (commandPane_ != null)
			commandPane_.setMessage(msg);
	}

	private class JTextComponentWriter extends Writer {
		private JTextComponent out = null;

		public JTextComponentWriter(JTextComponent out) {
			this.out = out;
		}

		public void write(char[] cbuf, int off, int len) throws IOException {
			synchronized (lock) {
				check();
				if ((off < 0) || (off > cbuf.length) || (len < 0)
						|| ((off + len) > cbuf.length) || ((off + len) < 0)) {
					throw new IndexOutOfBoundsException();
				} 

				char[] c = new char[len];
				System.arraycopy(cbuf, off, c, 0, len);
				String str = String.valueOf(c);
				if (out instanceof JTextArea) {
					((JTextArea)out).append(str);
					out.setCaretPosition(out.getDocument().getLength());
				} else
					out.setText(out.getText() + str);
			}
		}

		public void close() throws IOException {
			synchronized (lock) {
				check();
				flush();
				out = null;
			}
		}

		public void flush() throws IOException {
			synchronized (lock) {
				check();
			}
		}

		private void check() throws IOException {
			synchronized (lock) {
				if (out == null)
					throw new IOException();
			}
		}
	}
}

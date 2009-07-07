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


import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxPythonScriptItem;
import com.generalrobotix.ui.util.GrxCorbaUtil;

@SuppressWarnings("serial")
/**
 * @brief
 */
public class GrxJythonPromptView extends GrxBaseView { 
    private PythonInterpreter interpreter_ = new PythonInterpreter();
    private Thread thread_1_;
    private Thread thread_2_;
    private String prompt_ = ">>> ";
    private Display display_;
    private Composite parent_;
    private StyledText styledText_;
    private MenuDialog menuDialog;
    private Frame frame_;
    private static final int HISTORY_SIZE = 50;
    private List<String> history_ = new LinkedList<String>();
    private int hpos_ = 0;
    private String com_;
    private Button btnExec_;
    private Writer writer_;
    private GrxPythonScriptItem currentItem_;
    private String message_;
    private Object result_;

    private static final int KS_SHIFT = SWT.SHIFT;
    private static final int KS_CONTROL = SWT.CTRL;
    private static final int KS_CTRL_U = 'u' | SWT.CTRL;
    private static final int KS_CTRL_A = 'a' | SWT.CTRL;
    private static final int KS_CTRL_E = 'e' | SWT.CTRL;
    private static final int KS_CTRL_F = 'f' | SWT.CTRL;
    private static final int KS_CTRL_B = 'b' | SWT.CTRL;
    private static final int KS_CTRL_N = 'n' | SWT.CTRL;
    private static final int KS_CTRL_P = 'p' | SWT.CTRL;
    private static final int KS_UP = SWT.ARROW_UP;
    private static final int KS_DOWN = SWT.ARROW_DOWN;
    private static final int KS_LEFT = SWT.ARROW_LEFT;
    private static final int KS_RIGHT = SWT.ARROW_RIGHT;
    private static final int KS_ENTER = SWT.CR;
    private static final int KS_ENTER_ALT = SWT.CR | SWT.ALT;
    private static final int KS_ENTER_CTRL = SWT.CR | SWT.CTRL;
    private static final int KS_ENTER_SHIFT = SWT.CR | SWT.SHIFT;
    private static final int KS_BACK_SPACE = SWT.BS;
    private static final int KS_DELETE = SWT.DEL;
  
    private Image simScriptStartIcon_;
    private Image simScriptStopIcon_;
    
    /**
     * @brief constructor
     * @param name name of this view
     * @param manager PluginManager
     * @param vp
     * @param parent
     */
    public GrxJythonPromptView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);

        display_ = parent.getDisplay();
        parent_ = parent;
        composite_.setLayout(new GridLayout(1,false));
        simScriptStartIcon_ = ImageDescriptor.createFromURL(GrxJythonPromptView.class.getResource( "/resources/images/sim_script_start.png")).createImage();
        simScriptStopIcon_ = ImageDescriptor.createFromURL(GrxJythonPromptView.class.getResource( "/resources/images/sim_script_stop.png")).createImage();
        frame_ = SWT_AWT.new_Frame(new Composite(parent.getShell(),SWT.EMBEDDED));
        
        btnExec_ = new Button(composite_,SWT.TOGGLE);
        btnExec_.setImage(simScriptStartIcon_);
        btnExec_.setText("execute script file");
        //btnExec_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
        //btnExec_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
        btnExec_.setEnabled(false);
        
        styledText_ = new StyledText(composite_,SWT.MULTI|SWT.V_SCROLL|SWT.WRAP);
        styledText_.setEditable(false);
        styledText_.setText(prompt_);
        styledText_.addVerifyKeyListener(new ConsoleKeyListener());
        styledText_.setEditable(true);
        styledText_.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        btnExec_.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                if (btnExec_.getSelection()) {
                    btnExec_.setImage(simScriptStopIcon_);
                    btnExec_.setToolTipText("interrupt python threads");
                    execFile();
                } else {
                    btnExec_.setImage(simScriptStartIcon_);
                    btnExec_.setToolTipText("execute script file");
                    styledText_.setEnabled(true);
                    interrupt();
                }
            }
            
        });
             
        writer_ = new BufferedWriter(new PrintWriter(new StyledTextWriter(styledText_)));
        interpreter_.setErr(writer_);
        interpreter_.setOut(writer_);
        interpreter_.set("uimanager", manager_);
        interpreter_.exec("import sys");
        interpreter_.exec("sys.path.append('" + Activator.getPath() + "/script')");
        interpreter_.exec("sys.path.append('" + Activator.getPath() + "/jythonLib')");
        URL[] urls = manager_.pluginLoader_.getURLs();
        for (int i=0; i<urls.length; i++) {
            interpreter_.exec("sys.path.append('"+urls[i].getPath()+"')");
            interpreter_.exec("print \"sys.path.append(\'"+urls[i].getPath()+"\')\"");
        }
        interpreter_.exec("import rbimporter");
        interpreter_.exec("import __builtin__");
        
        interpreter_.set("view", this);
        interpreter_.exec("__builtin__.waitInput = view.waitInput");
        interpreter_.exec("__builtin__.waitInputConfirm = view.waitInputConfirm");
        interpreter_.exec("__builtin__.waitInputSelect = view.waitInputSelect");
        interpreter_.exec("__builtin__.waitInputMessage = view.waitInputMessage");
        interpreter_.exec("__builtin__.waitInputMenu = view.waitInputMenu");
        interpreter_.exec("__builtin__.waitInputSetMessage = view.waitInputSetMessage");
        interpreter_.exec("del view\n");
        interpreter_.exec("del __builtin__");
        history_.add("");
        
        setMenuItem(new InitPythonAction());
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
        
        currentItem_ = manager_.<GrxPythonScriptItem>getSelectedItem(GrxPythonScriptItem.class, null);
        btnExec_.setEnabled(currentItem_ != null);
        manager_.registerItemChangeListener(this, GrxPythonScriptItem.class);
    }
        
    private class InitPythonAction extends Action{
        public InitPythonAction(){
            super("cleanup python",IAction.AS_PUSH_BUTTON);
        }
        public void run(){
            interpreter_.cleanup();
            interpreter_.exec("rbimporter.refresh()");
        }
    }
    
    public void interrupt() {
        if (thread_1_ != null) 
            thread_1_.interrupt();
        if (thread_2_ != null) 
            thread_2_.interrupt();
    }
    
    private class ConsoleKeyListener implements VerifyKeyListener {

        public void verifyKey(VerifyEvent event) {
            int ks = event.keyCode | event.stateMask;
            int len = styledText_.getText().length();
            int cp = styledText_.getCaretOffset();

            com_ = getCommand();
            event.doit = false;
            if (thread_1_ != null) {
                return;
            } else if (ks == KS_ENTER      || ks == KS_ENTER_ALT
                    || ks == KS_ENTER_CTRL || ks == KS_ENTER_SHIFT) {
                try {
                	if (com_.trim().length() <= 0){
                		styledText_.append("\n"+prompt_);
                		styledText_.setCaretOffset(styledText_.getText().length());
                        styledText_.setTopIndex(styledText_.getLineCount());
                	}else{
                		styledText_.append("\n");
                        btnExec_.setSelection(true);
                        btnExec_.setImage(simScriptStopIcon_);
                        final String com = com_.trim();
                        thread_1_ = new Thread() {
                        	public void run() {
                         		result_=null;
                         		try{
	                        		if (com.startsWith("dir(")) {
	                        			interpreter_.exec("__tmp__ = " +com);
	                        			result_ = interpreter_.eval("__tmp__");
	                        		} else {
	                        			result_ = interpreter_.eval(com);
	                        		}
                         		}catch (org.python.core.PyException e) {
                         			try{
                         				interpreter_.exec(com);
                         			}catch( org.python.core.PyException exception ){
                         				result_ = exception.toString();
                         			}
                         		}
                        		history_.add(1, com);
                                if (history_.size() > HISTORY_SIZE)
                                    history_.remove(HISTORY_SIZE-1);
                        		display_.syncExec(new Thread(){
                                    public void run(){
		                        		if (result_ != null){
		                        			styledText_.append(result_.toString()+"\n");
		                        		}
		                        		btnExec_.setSelection(false);
                                        btnExec_.setImage(simScriptStartIcon_);
                                        styledText_.setFocus();
                                        styledText_.append(prompt_);
                                        styledText_.setCaretOffset(styledText_.getText().length());
                                        styledText_.setTopIndex(styledText_.getLineCount());
                                    }
                        		});
                        		hpos_ = 0;
                                thread_1_ = null;
                        	};
                        };
                        thread_1_.start();
                	}
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ks == KS_CTRL_U) {
                styledText_.replaceTextRange(len - com_.length(), com_.length(),"");
            } else if (ks == KS_CTRL_A) {
                styledText_.setCaretOffset(len - com_.length());
            } else if (ks == KS_CTRL_E) {//Eclipseの標準キーバインド
                styledText_.setCaretOffset(len);
            } else if ((ks == KS_CTRL_F || ks == KS_RIGHT)) {//Eclipseの標準キーバインド
                if (cp < len)
                    styledText_.setCaretOffset(cp + 1);
            } else if ((ks == KS_CTRL_B || ks == KS_LEFT)) {//Eclipseの標準キーバインド
                if (len - com_.length() < cp)
                    styledText_.setCaretOffset(cp - 1);
            } else if (ks == KS_SHIFT) {   // ignore input of shift key
            } else if (ks == KS_CONTROL) { // and control key
            } else if (ks == KS_BACK_SPACE) {
                int p = styledText_.getText().lastIndexOf('\n') + prompt_.length() + 1;
                if (p < cp) {
                    styledText_.replaceTextRange(cp - 1, 1,"");
                    if (hpos_ > 0) {
                        history_.remove(hpos_);
                        history_.add(hpos_,getCommand());
                    }
                }
            } else if (ks == KS_DELETE) {
                if (cp < len) {
                    styledText_.replaceTextRange( cp,1,"");
                    if (hpos_ > 0) {
                        history_.remove(hpos_);
                        history_.add(hpos_,getCommand());
                    }
                }
            } else if (ks == KS_UP || ks == KS_CTRL_P) {
                if (hpos_ < history_.size() - 1) {
                    int start = styledText_.getText().lastIndexOf('\n') + prompt_.length() + 1;
                    if (start <= len)
                        styledText_.replaceTextRange(start, len-start, history_.get(++hpos_));
                    styledText_.setCaretOffset(styledText_.getText().length());
                }
            } else if (ks == KS_DOWN || ks == KS_CTRL_N) {//KS_CTRL_N
                styledText_.setCaretOffset(len);
                if (hpos_ > 0) {
                    int start = styledText_.getText().lastIndexOf('\n') + prompt_.length() + 1;
                    if (start <= len)
                        styledText_.replaceTextRange(start, len-start,history_.get(--hpos_));
                }
            } else {
                if (cp <styledText_.getText().lastIndexOf('\n')) {
                    cp = len;
                    styledText_.setCaretOffset(len);
                }
                if (hpos_ > 0) {
                    history_.remove(hpos_);
                    history_.add(hpos_,getCommand());
                }
                event.doit = true;
            }
        }
    }
    
    public void execFile() {
        if (currentItem_ == null)
            return;

        if (currentItem_.isEdited()) {
            boolean ans = MessageDialog.openConfirm(parent_.getShell(), "exec script", "File has been changed. Save before execute");
            if (!ans || !currentItem_.save())
                return;
        }
        
        final String url = currentItem_.getURL(true);
        File f = new File(url);
        File p = f.getParentFile();
        interpreter_.exec("import sys");
        PyString pyStr = new PyString(p.getAbsolutePath());
        PyList pathList = (PyList)interpreter_.eval("sys.path");
        if (!pathList.__contains__(pyStr)) {
            String com = "sys.path.append(\""+p.getAbsolutePath()+"\")";
            interpreter_.exec("print '"+com+"'");
            interpreter_.exec(com);
        }
        
        thread_2_ = new Thread() {
            public void run() {
                try {
                    interpreter_.exec("print 'execfile("+url+")'");
                    interpreter_.execfile(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                display_.syncExec(new Thread(){
                    public void run(){
                        btnExec_.setSelection(false);
                        btnExec_.setImage(simScriptStartIcon_);
                        btnExec_.setToolTipText("execute script file");
                        styledText_.setFocus();
                        styledText_.append(prompt_);
                        styledText_.setCaretOffset(styledText_.getText().length());
                        styledText_.setTopIndex(styledText_.getLineCount());
                    }
                });
                hpos_ = 0;
            }
        };
        thread_2_.start();
    }

    private String getCommand() {
        String com = styledText_.getText();
        int idx = com.lastIndexOf('\n');
        if (idx > 0)
            com = com.substring(idx + 1);
        return com.replaceFirst(prompt_, "");
    }
    
    public void restoreProperties() {
        super.restoreProperties();
        String nsHost = manager_.getProjectProperty("nsHost");
        String nsPort = manager_.getProjectProperty("nsPort");
        if (nsHost == null)
            nsHost = GrxCorbaUtil.nsHost();
        if (nsPort == null){
        	Integer np = new Integer(GrxCorbaUtil.nsPort());
            nsPort = np.toString();
        }
            
        String NS_OPT = "-ORBInitRef NameService=corbaloc:iiop:"+nsHost+":"+nsPort+"/NameService";
        System.setProperty("NS_OPT", NS_OPT);
        interpreter_.cleanup();
        interpreter_.exec("print 'NS_OPT="+NS_OPT+"'");
        interpreter_.exec("print '"+prompt_+"rbimporter.refresh()'");
        interpreter_.exec("rbimporter.refresh()");
        String nameservice = "could not parse";
        try {
            nameservice = NS_OPT.split("=")[1];
        } catch (Exception e) {
            interpreter_.exec("print 'failed to connect NameService("+nameservice+")'");
        }
        
        //TODO:UIスレッド以外から呼ばれることはないならはずしてもいい。
        display_.asyncExec(new Thread(){
            public void run(){
                styledText_.setCaretOffset(styledText_.getText().length());
                //area_.setMaximumRowCount(getInt("maxRowCount", DEFAULT_MAX_ROW));
            }
        });
        
        String defaultScript = System.getProperty("SCRIPT");
        if (defaultScript != null) {
            System.clearProperty("SCRIPT");
            File f = new File(defaultScript);
            String name = f.getName().replace(".py", "");
            GrxBaseItem newItem = manager_.loadItem(GrxPythonScriptItem.class, name, f.getAbsolutePath());
            manager_.itemChange(newItem, GrxPluginManager.ADD_ITEM);
            manager_.setSelectedItem(newItem, true);
            execFile();
        }
    }
   
    public void registerItemChange(GrxBaseItem item, int event){
    	if(item instanceof GrxPythonScriptItem){
    		GrxPythonScriptItem pythonScriptItem = (GrxPythonScriptItem)item;
    		switch(event){
	    	case GrxPluginManager.SELECTED_ITEM:
	    		currentItem_ = pythonScriptItem;
	    		btnExec_.setEnabled(true);
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		currentItem_ = null;
	    		btnExec_.setEnabled(false);
	    		break;
	    	default:
	    		break;
	    	}
    	}
    }
 
    public void waitInput(final String msg) {
        JOptionPane.showMessageDialog(frame_, msg);
//        MessageDialog.openInformation(parent_.getShell(), "", msg);
    }
    public void waitInputConfirm(final String msg) {
        int ans = JOptionPane.showConfirmDialog(frame_,
            msg,"waitInputConfirm",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null);
        if (ans != JOptionPane.OK_OPTION)
            interrupt();
//        boolean ans = MessageDialog.openConfirm(parent_.getShell(), "waitInputConfirm", msg);
//        if (!ans)
//            interrupt();

    }
    public boolean waitInputSelect(final String msg) {
        int ans = JOptionPane.showConfirmDialog(frame_,
            msg,"waitInputSelect",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null);
        if (ans == JOptionPane.YES_OPTION)
            return true;
        else if (ans != JOptionPane.NO_OPTION)
            interrupt();
        return false;
//        int ans = new MessageDialog(parent_.getShell(),"waitInputSelect",null,msg,MessageDialog.QUESTION,new String[]{"YES","NO","CANCEL"},2).open();
//        if (ans == 0)//YES
//            return true;
//        else if (ans != 1)//CANCEL or dialog close
//            interrupt();
//        return false;
    }
    public Object waitInputMessage(String msg) {
        return JOptionPane.showInputDialog(frame_,
            msg, "waitInputMessage",
            JOptionPane.INFORMATION_MESSAGE,
            null,null,null);
//        return new InputDialog(parent_.getShell(),"waitInputMessage",msg,null,null).open();
    }
    public void waitInputMenu(String[][] menuList) {
        menuDialog = new MenuDialog(menuList, interpreter_, message_);
        menuDialog.showDialog(frame_, currentItem_.getName(), false);
    }
    public void waitInputSetMessage(String msg) {
        message_ = msg;
        if(MenuDialog.getCurrentMenuDialog()!=null)
        	menuDialog.setMessage(msg);
    }

    public class StyledTextWriter extends Writer {
        private StyledText out = null;

        public StyledTextWriter(StyledText out) {
            this.out = out;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            synchronized (lock) {
                check();
                if ((off < 0) || (off > cbuf.length) || (len < 0)
                        || ((off + len) > cbuf.length) || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                }

                char[] c = new char[len];
                System.arraycopy(cbuf, off, c, 0, len);
                final String str = String.valueOf(c);
                display_.asyncExec(new Thread(){
                    public void run(){
                        out.append(str);
                        out.setCaretOffset(out.getText().length());
                    }
                });
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
                if (out == null) {
                    throw new IOException();
                }
            }
        }
    }
    
    public void sutdown(){
    	manager_.removeItemChangeListener(this, GrxPythonScriptItem.class);
    }
//    public void add(JPanel pnl) {
//        getContentPane().add(pnl, BorderLayout.SOUTH);
//    }
}

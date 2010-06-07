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
 *  MenuDialog.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  Created on 2005/01/31
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.GrxGuiUtil;
import com.generalrobotix.ui.view.GrxJythonPromptView.StyledTextWriter;
/**
 * Dialog with buttons that is be able to set jython funtion 
 * 
 * @author kawasumi
 * 
 */
@SuppressWarnings("serial")
public class MenuDialog extends JPanel {
    private static MenuDialog currentMenuPanel_ = null;
    private JDialog dialog_ = null;
    private JPanel  jPanel_localMenu = null;
    private JButton jButton_previous = null;
    private JButton jButton_next = null;
    private JLabel  jLabel = null;
    private JCheckBox jCheckBoxSequential = null;
    private JTextArea jTextArea = null;
    
    private JCheckBox jCheckBoxContinuous = null;
    //private JButton currentGoNextButton_ = null;
    private List<JTextField> currentFields_ = new ArrayList<JTextField>();
    //private boolean isClickedButtonGoNext_ = false;
    
    private String[][] menu_;
    private int showingStage_ = 0;
    private int currentStage_ = 0;
    private String command_ = null;
    private boolean isWaiting_ = false;
    private static final SimpleDateFormat dateFormat_ = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final String QUITBUTTON_TITLE  = "Quit";
    private HashMap<String, String> exceptMap = new HashMap<String, String>();
    private String message_;
    private PythonInterpreter interpreter_;
    private StyledTextWriter writer_;
    
    public MenuDialog(String[][] src, PythonInterpreter interpreter, String message, StyledTextWriter writer){
        menu_ = src;
        interpreter_ = interpreter;
        message_ = message;
        writer_ = writer;
        initialize();
    }
    private void initialize() {
        exceptMap.put(QUITBUTTON_TITLE,QUITBUTTON_TITLE);
        // set panel configuration
        // local panel
        JPanel localPanel = new JPanel(new BorderLayout());
        localPanel.add(new JScrollPane(getLocalMenuPanel()),BorderLayout.CENTER);
        
        JPanel localPanelLower = new JPanel();
        localPanelLower.add(getPreviousButton(), null);
        jLabel = new JLabel("  1 / 1  ");
        localPanelLower.add(jLabel, null);
        localPanelLower.add(getNextButton(), null);
        localPanelLower.add(getJCheckBoxSequential(), null);
        localPanel.add(localPanelLower,BorderLayout.SOUTH);

        // main tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Local",  null, localPanel, null);
        tabbedPane.addTab("Global", null, new JScrollPane(getGlobalMenuPanel()), null);
        tabbedPane.addTab("History",null, new JScrollPane(getJTextArea()), null);
        tabbedPane.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e){
                JTabbedPane jtp = (JTabbedPane)e.getSource();
                if (jtp.getSelectedIndex() != 2)
                    _clearTextComponentRecursive((Container)jtp.getSelectedComponent());
            }
        });
        
        // content pane
        setLayout(new BorderLayout());
        add(tabbedPane,BorderLayout.CENTER);
        
        isWaiting_ = false;
    }

    void setReadyToNext(){
        command_ = null;
        if (getJCheckBoxSequential().isSelected()){
            if (currentStage_ < menu_.length){
                showingStage_ = currentStage_;
               // boolean doClick = (getJCheckBoxContinuous().isSelected()) && 
               //                   isClickedButtonGoNext_;
                showLocalMenu();
            } else {
                showingStage_ = currentStage_ = menu_.length-1;
                showLocalMenu();
               // _setTabSelected(getGlobalMenuPanel());
               // getGlobalMenuPanel().setVisible(true);
            }
        } else {
            showLocalMenu();
        }
    }

    // Global Menu Panel -------------------------
    JPanel jpanel_globalMenu = null;
    private void showGlobalMenu(){
        getGlobalMenuPanel().removeAll();
        
        JPanel  jpanel = new JPanel();
        JButton buttonQuit = new JButton(QUITBUTTON_TITLE);
        buttonQuit.addActionListener(new java.awt.event.ActionListener() { 
            public void actionPerformed(java.awt.event.ActionEvent e) {    
                exit();
            }
        }); 
        jpanel.add(buttonQuit);
        jpanel.setAlignmentX( JComponent.CENTER_ALIGNMENT );
        getGlobalMenuPanel().add(jpanel);
        
        jpanel = new JPanel();
        JButton buttonRetry = new JButton("RETRY(from first)");
        buttonRetry.addActionListener(new java.awt.event.ActionListener() { 
            public void actionPerformed(java.awt.event.ActionEvent e) {    
                refresh();
                _setTabSelected(getLocalMenuPanel());
                getLocalMenuPanel().setVisible(true);
            }
        }); 
        jpanel.add(buttonRetry);  
        jpanel.setAlignmentX( JComponent.CENTER_ALIGNMENT );
        getGlobalMenuPanel().add(jpanel);
        
        for(int i=1; i<menu_[0].length; i=i+2){
            String m1 = menu_[0][i-1].trim();
            String m2 = menu_[0][i].trim();
            if (m2.equals("#label")){
            	JLabel label = new JLabel(m1,  SwingConstants.LEFT);
            	label.setAlignmentX( JComponent.CENTER_ALIGNMENT );
            	getGlobalMenuPanel().add(label);
            }else if(m2.equals("#monitor")) {
				PyObject res = interpreter_.eval(m1);
				JLabel label = new JLabel(res.toString(),  SwingConstants.LEFT);
            	label.setAlignmentX( JComponent.CENTER_ALIGNMENT );
				getGlobalMenuPanel().add(label);
            }else {
                addButton(getGlobalMenuPanel(),m1,m2,false);
            }
        }
    }
    
    private JPanel getGlobalMenuPanel() {
        if (jpanel_globalMenu == null){
            jpanel_globalMenu = new JPanel();
            jpanel_globalMenu.setLayout(new BoxLayout(jpanel_globalMenu, BoxLayout.Y_AXIS));
        }
        return jpanel_globalMenu;
    }       
    
    // Local Menu Panel -------------------------
    private void showLocalMenu(){
        getLocalMenuPanel().setVisible(false);
        getLocalMenuPanel().removeAll();
        getLocalMenuPanel().setVisible(true);
        if (showingStage_ >= menu_.length-1){
            showingStage_ = menu_.length-1;
            getNextButton().setEnabled(false);
        } else {
            getNextButton().setEnabled(true);
        }
        if (showingStage_ < 2){
            showingStage_ = 1;
            getPreviousButton().setEnabled(false);
        } else {
            getPreviousButton().setEnabled(true);
        }
        
        currentFields_.clear();
        for(int i=1; i<menu_[showingStage_].length; i=i+2){
            String m1 = menu_[showingStage_][i-1].trim();
            String m2 = menu_[showingStage_][i].trim();
            if (m2.equals("#label")){
            	JLabel label = new JLabel(m1,  SwingConstants.LEFT);
            	label.setAlignmentX( JComponent.CENTER_ALIGNMENT );
                getLocalMenuPanel().add(label);
            }else if(m2.equals("#monitor")) {
				PyObject res = interpreter_.eval(m1);
				JLabel label = new JLabel(res.toString(),  SwingConstants.LEFT);
            	label.setAlignmentX( JComponent.CENTER_ALIGNMENT );
				getLocalMenuPanel().add(label);
            }else {
            	addButton(getLocalMenuPanel(),m1,m2,(i<=1));
            }
        }

        jLabel.setText(showingStage_+" / "+(menu_.length-1));
        
        if (command_ != null){
            GrxGuiUtil.setEnableRecursive(false,getLocalMenuPanel(),null);
            GrxGuiUtil.setEnableRecursive(false,getGlobalMenuPanel(),exceptMap);
        } else {
            if (getJCheckBoxSequential().isSelected() && showingStage_ != currentStage_)
                GrxGuiUtil.setEnableRecursive(false,getLocalMenuPanel(),null);
            GrxGuiUtil.setEnableRecursive(true,getGlobalMenuPanel(),null);
        }
      //  getLocalMenuPanel().add(javax.swing.Box.createVerticalGlue());
        //getLocalMenuPanel().setVisible(true);
        //setVisible(true);
    }
    
    private JPanel getLocalMenuPanel() {
        if (jPanel_localMenu == null) {
            jPanel_localMenu = new JPanel();
            jPanel_localMenu.setLayout(new BoxLayout(jPanel_localMenu, BoxLayout.Y_AXIS));
        }
        return jPanel_localMenu;
    }
    private JButton getPreviousButton() {
        if (jButton_previous == null) {
            jButton_previous = new JButton("Previous");
            jButton_previous.setToolTipText("show previous menu");
            jButton_previous.addActionListener(new java.awt.event.ActionListener() { 
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    showingStage_--;
                    showLocalMenu();
                }
            });
        }
        return jButton_previous;
    }
    private JButton getNextButton() {
        if (jButton_next == null) {
            jButton_next = new JButton("    Next    ");
            jButton_next.setToolTipText("show next menu");
            jButton_next.addActionListener(new java.awt.event.ActionListener() { 
                public void actionPerformed(java.awt.event.ActionEvent e) {    
                    showingStage_++;
                    showLocalMenu();
                }
            });
        }
        return jButton_next;
    }
    private JCheckBox getJCheckBoxSequential() {
        if (jCheckBoxSequential == null) {
            jCheckBoxSequential = new JCheckBox("Sequential",true);
            jCheckBoxSequential.setToolTipText("enable only current button");
            jCheckBoxSequential.addActionListener(new java.awt.event.ActionListener() { 
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (jCheckBoxSequential.isSelected()){
                        showingStage_ = currentStage_;
                        showLocalMenu();
                        getJCheckBoxContinuous().setEnabled(true);
                    } else {
                        if (isIdle()){
                            GrxGuiUtil.setEnableRecursive(true,getLocalMenuPanel(),null);
                        }
                        getJCheckBoxContinuous().setEnabled(false);
                    }
                }
            });
        }
        return jCheckBoxSequential;
    }
    private JCheckBox getJCheckBoxContinuous() {
        if (jCheckBoxContinuous == null) {
            jCheckBoxContinuous = new JCheckBox("continuous",false);
            jCheckBoxContinuous.setToolTipText("excecute continuously");
        }
        return jCheckBoxContinuous;
    }
    private JButton addButton(JPanel pnl,final String name,final String com,final boolean goNext){
        JPanel  jpanel = new JPanel();
        JButton button = new JButton(name); 
        jpanel.add(button);
        
        final String[] str = parseCommand(com);
        int len = 1;
        if (str!=null) len = str.length;
        final JTextField[] fields = new JTextField[len];
        if (str!= null && len > 0){
            for (int i=0;i<len;i++){
                fields[i] = new JTextField();
                fields[i].setPreferredSize(new Dimension(60,20));
                jpanel.add(fields[i]);
                //currentFields_.add(fields[i]);
            }   
        }
    
        if (goNext){
            //currentGoNextButton_ = button;
            if (com.indexOf("#continuous")!=-1){
                getJCheckBoxContinuous().setSelected(true);
                jpanel.add(getJCheckBoxContinuous());
            } else {
                getJCheckBoxContinuous().setSelected(false);
            }
        }
        
        button.addActionListener(new java.awt.event.ActionListener() { 
            public void actionPerformed(java.awt.event.ActionEvent e) {    
                if (command_ == null){
                    String rcom = com;
                    //DebugUtil.print("command:"+rcom);
                    try{
                        if (com.indexOf("#appendlog") < 0){
                            //BodyStatPanel.getInstance().saveData();
                        }
                        if (str!=null && str.length > 0){
                            for (int i=0;i<str.length;i++){
                                char c = str[i].charAt(1);
                                String s = fields[i].getText();
                                if (c == 'd' || c == 'D'){
                                    Double.parseDouble(s);
                                } else if (c == 'i' || c == 'I'){
                                    Integer.parseInt(s);
                                } else if (c == 't' || c == 'T'){
                                	s = "\'"+s+"\'";
                                }
                                rcom = rcom.replaceFirst(str[i],s);
                            }
                            //DebugUtil.print("replaced command:"+rcom);
                        }
                        command_ = rcom;
                        String comLog = dateFormat_.format(new Date())+" : "+name+" : "+command_+"\n";
                        getJTextArea().append(comLog);
                        
                        if (getJCheckBoxSequential().isSelected() && goNext ){ // && getJCheckBoxContinuous().isSelected()){
                           // isClickedButtonGoNext_ = true;
                            currentStage_++;
                        } else {
                           // isClickedButtonGoNext_ = false;
                        }
                        GrxGuiUtil.setEnableRecursive(false,getLocalMenuPanel(),null);
                        GrxGuiUtil.setEnableRecursive(false,getGlobalMenuPanel(),exceptMap);
                        //clearTextComponent((Container)(getJTabbedPane().getSelectedComponent()));
                        interpreter_.exec(command_);
                        setReadyToNext();
                        //showMessage(message_);                        
                    } catch (NumberFormatException e1){
                        GrxDebugUtil.printErr("MenuDialog: parse error");
                        writer_.write("MenuDialog: parse error  "+e1+System.getProperty("line.separator"));
                    }
                }
            }
        });
        jpanel.setAlignmentX( JComponent.CENTER_ALIGNMENT );
        pnl.add(jpanel);
        return button;
    }
        
    private String[] parseCommand(String com){
        StringBuffer sb = new StringBuffer();
        int idx = -1;
        while((idx = com.indexOf("#",idx+1)) != -1){
            char c = com.charAt(idx+1);
            if (c =='d' || c =='D'||
                c =='i' || c =='I' ||
               	c =='t' || c =='T'	) {
                sb.append("#"+c+" ");
            }
        }
        if (sb.toString().trim().equals(""))
          return null;
        return sb.toString().split(" ");
    }

    // Log Panel------------------------------------  
    private JTextArea getJTextArea() {
        if (jTextArea == null) {
            jTextArea = new JTextArea();
            jTextArea.setEditable(false);
        }
        return jTextArea;
    }
    
    private static void _clearTextComponentRecursive(Container container){
        Component[] components = container.getComponents();
        for (int i=0;i<components.length;i++){
            Component c = components[i];
            if (c instanceof JTextComponent){
                ((JTextComponent)c).setText("");
            } else if (c instanceof Container) {    
                _clearTextComponentRecursive((Container)c);
            }
        }
    }   
    private static void _setTabSelected(Component c){
        Component parent = c.getParent();
        if (parent == null)
            return;
        if (parent instanceof JTabbedPane){
            JTabbedPane tabpane = (JTabbedPane)parent;
            tabpane.setSelectedComponent(c);
            return;
        }
        _setTabSelected(parent);    
    }
    // Public Methods
    //第一引数をJFrameからawtのFrameに変更
    public void showDialog(Frame owner, String title, boolean modal){
        if (dialog_ == null ){
            dialog_ = new JDialog(owner,title,modal);
            //GrxGuiUtil.setWindowConfig("menudialog",dialog_,new Dimension(300,400));
            dialog_.setSize(new Dimension(400, 400));
            dialog_.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog_.setContentPane(this);
        }
        
        refresh();

    }
    private void refresh(){
        dialog_.setVisible(true);
        currentStage_ = showingStage_ = 1;
        isWaiting_ = true;
        currentMenuPanel_ = this;
        showGlobalMenu();
        showLocalMenu();
    }
    public boolean isWaiting(){
        return isWaiting_;
    }
    public boolean isIdle(){
        return (command_ == null);
    }
    public String getCommand(){
        return command_;
    }
    public static MenuDialog getCurrentMenuDialog(){
        return currentMenuPanel_;
    }
    public String getCurrentMenuItem(){
        if (getJCheckBoxSequential().isSelected()){
            if (command_ != null) 
                return command_;
            return menu_[Math.min(currentStage_,menu_.length-1)][0];
        }
        return menu_[Math.min(showingStage_,menu_.length-1)][0];
    }
    public boolean isAllDone(){
        if (currentStage_ >= menu_.length-1)
            return true;
        return false;
    }
    public void showMessage(String msg){
        int idx = -1;
        if (currentFields_ != null && msg != null){
            //DebugUtil.print("setMessage:"+msg);
            while((idx = msg.indexOf("$",idx+1)) != -1){
                int idx2 = msg.indexOf("=",idx+1);
                String s = msg.substring(idx+1,idx2);
                int idx3 = msg.indexOf(" ",idx2+1);
                if (idx3 < 0)
                    idx3 = msg.length();
                String val = msg.substring(idx2+1,idx3);
                try {
                    int i = Integer.parseInt(s.trim());
                    ((JTextField)(currentFields_.get(i))).setText(val);
                    //DebugUtil.print("setMessage:$"+i+"="+val);
                } catch (Exception e){
                    GrxDebugUtil.printErr("MenuDialog.setMessage():",e);
                }
                idx = idx2;
            }
        }
    }
    public void setMessage(String message){
    	message_ = message;
    }
    
    public void setContinuous(boolean b){
        getJCheckBoxContinuous().setSelected(b);
    }
    public void exit(){
        dialog_.setVisible(false);
        isWaiting_ = false;
        currentMenuPanel_ = null;
    }
}
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
 * ControllerPanel.java
 *
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.simulation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.view.graph.SEDoubleTextWithSpin;

@SuppressWarnings("serial")
public class ControllerPanel extends JPanel {
    private GrxPluginManager manager_;
    private JTable table_;
    private JScrollPane scrollPane_;
    
    private Vector<GrxBaseItem> vecRobot_;
    
    private JButton btnRemove_;
    private JButton btnEdit_;
    
    private ControllerEditorPanel editorPanel_;
    
    private static final String ATTRIBUTE_CONTROLLER = "controller";
    private static final String ATTRIBUTE_CONTROL_TIME = "controlTime";
    private static final String ATTRIBUTE_SETUP_DIRECTORY = "setupDirectory";
    private static final String ATTRIBUTE_SETUP_COMMAND = "setupCommand";
    
    public ControllerPanel(GrxPluginManager manager) {
        manager_ = manager;
        vecRobot_ = new Vector<GrxBaseItem>();
        setLayout(new GridLayout(2,1));
        AbstractTableModel dataModel = new AbstractTableModel() {
            private final String[] clmName_ ={
                MessageBundle.get("panel.controller.table.robot"),
                MessageBundle.get("panel.controller.table.controller"),
                MessageBundle.get("panel.controller.table.controlTime"),
                MessageBundle.get("panel.controller.table.setupDirectory"),
                MessageBundle.get("panel.controller.table.setupCommand")
            };
            private final String[] attrName_ = {
                "dummy",
                ATTRIBUTE_CONTROLLER,
                ATTRIBUTE_CONTROL_TIME,
                ATTRIBUTE_SETUP_DIRECTORY,
                ATTRIBUTE_SETUP_COMMAND
            };
            
            public int getColumnCount() { return 5; }
            public int getRowCount() { return vecRobot_.size();}
            
            public String getColumnName(int col) {
                return clmName_[col];
            }
            
            public Object getValueAt(int row, int col) {
                GrxModelItem node = (GrxModelItem)vecRobot_.get(row);
                String str = null;
                if (col ==0){
                    return node.getName();
                }
                try{
                    str = node.getProperty(attrName_[col]);
                }catch(Exception ex){
                    ex.printStackTrace();
                    return "";
                }
                if (str == null)
                  str = "";
                
                return str;
            }
            public Class<? extends Object> getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
         
        table_ = new JTable(dataModel);
        table_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table_.getSelectionModel().addListSelectionListener( 
            new ListSelectionListener(){
                public void valueChanged(ListSelectionEvent e){
                    int row = table_.getSelectedRow() ;
                    if(row>=0 && row<vecRobot_.size()){
                        editorPanel_.setNode((GrxModelItem)vecRobot_.get(row));
                    }
                }
            });
         
        scrollPane_ = new JScrollPane(table_);
        scrollPane_.setBounds(12,12,360,120);
         
        JPanel pnlBttn = new JPanel();
        pnlBttn.setLayout(new GridLayout(1,4));
        
        btnRemove_ = new JButton(MessageBundle.get("panel.detach"));
        btnRemove_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = table_.getSelectedRow() ;
                    if (row >= 0 && row < vecRobot_.size()) {
                        if (_checkDialog(MessageBundle.get("controller.remove")))
                        {
                            GrxBaseItem node =
                                (GrxBaseItem)vecRobot_.get(row);
                            try {
                                node.setProperty(ATTRIBUTE_CONTROLLER, "");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            _repaint();
                        }
                    }
                }
            }
        );
        pnlBttn.add(btnRemove_);
        
        btnEdit_ = new JButton(MessageBundle.get("panel.edit"));
        btnEdit_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = table_.getSelectedRow() ;
                    if(row>=0 && row<vecRobot_.size()){
                        _setButtonEnabled(false);
                        editorPanel_.startEditMode((GrxModelItem)vecRobot_.get(row));
                    }
                }
            }
        );

        //btnEdit_.setBounds(24 + 72 + 72,144+24,60,24);
        pnlBttn.add(btnEdit_);
        
        JPanel pnlTable = new JPanel();
        pnlTable.setLayout(new BorderLayout());
        pnlTable.add(BorderLayout.CENTER,scrollPane_);
        pnlTable.add(BorderLayout.SOUTH,pnlBttn);
        pnlTable.setPreferredSize(new Dimension(260,90));
        add(pnlTable);
        
        editorPanel_ = new ControllerEditorPanel();
        editorPanel_.setPreferredSize(new Dimension(260,90));
        //editorPanel_.setBounds(12,280,360,300);
        add(editorPanel_);
    }
    
    private boolean _checkDialog(String msg) {
        int overwrite =
            new ConfirmDialog(manager_.getFrame(), MessageBundle.get("dialog.overwrite"), msg).showModalDialog();

        switch (overwrite) {
        case ModalDialog.YES_BUTTON:
            return true;
        case ModalDialog.NO_BUTTON:
            return false;
        case ModalDialog.CLOSE_BUTTON:
            return false;
        default:
            return false;
        }
    }

    private void _setButtonEnabled(boolean flag) {
        btnRemove_.setEnabled(flag);
        btnEdit_.setEnabled(flag);
        table_.setEnabled(flag);
        _repaint();
    }

    private void _repaint() {
        table_.columnMarginChanged(new ChangeEvent(table_) );
        scrollPane_.repaint();
    }
    
    public void setEnabled(boolean flag) {
        super.setEnabled(flag);
        editorPanel_.doCancel();
        _setButtonEnabled(flag);
    }
    
    public double getMaxControllTime() {
        double maxTime = 0;
        boolean flag = false;
        Iterator it = manager_.getItemMap(GrxModelItem.class).values().iterator();
        while (it.hasNext()) {
            GrxBaseItem node = (GrxBaseItem)it.next();
            if (node instanceof GrxModelItem) {
                String controllName = node.getProperty(ATTRIBUTE_CONTROLLER);
                if (controllName != null) {
                    double t = node.getDbl(ATTRIBUTE_CONTROL_TIME, 0.001);
                    if (maxTime < t) {
                        maxTime = t;
                        flag = true;
                    }
                }
            }
        }

        if (flag) 
            return maxTime;
        return Double.MAX_VALUE;
    }


    //--------------------------------------------------------------------
    private class ControllerEditorPanel extends JPanel {
        //private static final int MODE_ADD = 0 ;
        private static final int MODE_EDIT = 1 ;

        private int mode_;
        private GrxBaseItem node_;
        private JComboBox boxController_;
        private SEDoubleTextWithSpin spinControlTime_;
		private JTextField tfSetupDirectory_;
        private JComboBox  boxSetupCommand_;
        private JButton btnOk_,btnCancel_;
        
        public ControllerEditorPanel() {
            setLayout(null);
		    // Controller	
            JLabel lbl = new JLabel( MessageBundle.get("panel.controller.controller"), JLabel.RIGHT);
            lbl.setBounds(0,0,120,24);
            this.add(lbl);

            boxController_ = new JComboBox();
            boxController_.setEditable(true);
            boxController_.setBounds(120 + 6,0,180,24);
            boxController_.setLightWeightPopupEnabled(false);
            boxController_.addPopupMenuListener(new PopupMenuListener(){
              public void popupMenuCanceled(PopupMenuEvent e) {}
              public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
              public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                String[] names = GrxCorbaUtil.getObjectNameList();
				Object obj = boxController_.getSelectedItem();
                boxController_.removeAllItems();
				boxController_.addItem(obj);
                for (int i=0; i<names.length; i++)
                  boxController_.addItem(names[i]);
				boxController_.setSelectedIndex(0);
              }            
            });
            this.add(boxController_);
            
			// Control Time
            lbl = new JLabel(MessageBundle.get("panel.controller.controlTime"), JLabel.RIGHT);
            lbl.setBounds(0,30,120,24);
            spinControlTime_ = new SEDoubleTextWithSpin(0,10,0.001);
            spinControlTime_.setBounds(120 + 6,30,180,24);
            this.add(lbl);
            this.add(spinControlTime_);
            
			// Setup Command Working Directory
            lbl = new JLabel(MessageBundle.get("panel.controller.setupDirectory"), JLabel.RIGHT);
            lbl.setBounds(0,30+30,120,24);
            this.add(lbl);
            
            tfSetupDirectory_ = new JTextField("$(BIN_DIR)");
            tfSetupDirectory_.setBounds(120+6, 30+30, 180, 24);
            this.add(tfSetupDirectory_);
			// Setup Command
            lbl = new JLabel(MessageBundle.get("panel.controller.setupCommand"), JLabel.RIGHT);
            lbl.setBounds(0,30+30+30,120,24);
            this.add(lbl);
            
            boxSetupCommand_ = new JComboBox();
            boxSetupCommand_.setBounds(120+6, 30+30+30, 180, 24);
            boxSetupCommand_.setLightWeightPopupEnabled(false);
            boxSetupCommand_.setEditable(true);
            boxSetupCommand_.addPopupMenuListener(new PopupMenuListener(){
              public void popupMenuCanceled(PopupMenuEvent e) {}
              public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
              public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				File setupDir = new File(GrxXmlUtil.expandEnvVal(tfSetupDirectory_.getText()));
                String[] names = setupDir.list(new SetupCommandFilter());
				Object obj = boxSetupCommand_.getSelectedItem();
                boxSetupCommand_.removeAllItems();
				boxSetupCommand_.addItem(obj);
				if (names != null) {
                  for (int i=0; i<names.length; i++)
                    boxSetupCommand_.addItem(names[i]);
				}
				boxSetupCommand_.setSelectedIndex(0);
              }            
            });
            this.add(boxSetupCommand_);
            
			// OK Button
            btnOk_ = new JButton(MessageBundle.get("dialog.okButton"));
            btnOk_.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                      switch (mode_) {
                      case MODE_EDIT:
                          _setAttribute(node_);
                          break;
                      }
                      setEnabled(false);
                    }
                    private boolean _setAttribute(GrxBaseItem node) {
                        try {
                            node.setProperty(
                                ATTRIBUTE_CONTROLLER,
                                boxController_.getSelectedItem().toString()
                            );
                            node.setProperty(
                                ATTRIBUTE_CONTROL_TIME,
                                spinControlTime_.getValue().toString()
                            );
                            node.setProperty(
                                ATTRIBUTE_SETUP_DIRECTORY,
                                tfSetupDirectory_.getText()
                            );
                            node.setProperty(
                                ATTRIBUTE_SETUP_COMMAND,
                                boxSetupCommand_.getSelectedItem().toString()
                            );
                        } catch (Exception ex) {
                            new WarningDialog(
                                manager_.getFrame(),
                                "",
                                MessageBundle.get("message.attributeerror")
                            ).showModalDialog();
                            return false;
               
                            //ex.printStackTrace();
                        }
                        return true;
                    }
                }
            );
            btnOk_.setBounds(24,30+30+30+30,84,24);
            this.add(btnOk_);

			// Cancel Button
            btnCancel_ = new JButton(MessageBundle.get("dialog.cancelButton"));
            btnCancel_.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                      doCancel();
                    }
                }
            );
            btnCancel_.setBounds(24 + 84 + 12, 30+30+30+30, 84, 24);
            this.add(btnCancel_);
        }

        public void startEditMode(GrxModelItem node) {
            mode_ = MODE_EDIT;
            setNode(node);
            setEnabled(true);
        }
        
        public void doCancel() {
            setEnabled(false);
        }
        
        public void setNode(GrxModelItem node) {
            try {
                boxController_.setSelectedItem(node.getProperty(ATTRIBUTE_CONTROLLER, ""));
               	spinControlTime_.setValue(node.getProperty(ATTRIBUTE_CONTROL_TIME, "0.001"));
				tfSetupDirectory_.setText(node.getProperty(ATTRIBUTE_SETUP_DIRECTORY ,"$(BIN_DIR)"));
                boxSetupCommand_.setSelectedItem(node.getProperty(ATTRIBUTE_SETUP_COMMAND, ""));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            node_ = node;
        }
        
        public void setEnabled(boolean flag) {
            super.setEnabled(flag);
            Component[] cmps = getComponents();
            for(int i = 0; i < cmps.length; i ++) {
                cmps[i].setEnabled(flag);
            }
            _setButtonEnabled(!flag);
        }
    }
    
    public void childAdded(GrxBaseItem node) {
      if(node instanceof GrxModelItem){
          int i;
          for (i = 0; i < vecRobot_.size(); i ++) {
              vecRobot_.get(i);
              if (((GrxBaseItem)vecRobot_.get(i)).getName().compareTo(node.getName()) > 0)
                break;
            //  if (viewable.compareTo(node) > 0)
            //      break;
          }
          vecRobot_.add(i, node);
          _repaint();
          return;
      }
  }

  public void childRemoved(GrxBaseItem node) {
      if(node instanceof GrxModelItem){
          vecRobot_.remove(node);
          _repaint();
          editorPanel_.doCancel();
          return;
      }
  }
  
  public void updateRobots(List<GrxBaseItem> list) {
    editorPanel_.doCancel();
    vecRobot_ = new Vector<GrxBaseItem>();
    for (int i=0; i<list.size(); i++) {
      GrxBaseItem item = list.get(i);
      if (item instanceof GrxModelItem && ((GrxModelItem)item).isRobot()) 
        vecRobot_.add(list.get(i));
    }
    setEnabled(vecRobot_.size() > 0);
    table_.getSelectionModel().clearSelection();
    _repaint();
  }
   
  public int getRobotNum() {
    return vecRobot_.size();
  }

  private static class SetupCommandFilter implements FilenameFilter {
    public  boolean accept(File dir, String name) {
      if (!name.startsWith(".") && (name.endsWith(".sh") || name.endsWith(".bat")))
        return true;
      return false;
    }
  }
}

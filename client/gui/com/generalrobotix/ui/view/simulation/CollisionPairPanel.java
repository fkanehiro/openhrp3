/**
 * CollisionPairPanel.java
 *
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.simulation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.util.ConfirmDialog;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.ModalDialog;
import com.generalrobotix.ui.util.WarningDialog;
import com.generalrobotix.ui.view.graph.SEBoolean;

@SuppressWarnings("serial")
public class CollisionPairPanel extends JPanel {
    private GrxPluginManager manager_;
    private Frame parentFrame_;
    private JTable table_;
    private JScrollPane scrollPane_;
    private Vector<GrxBaseItem> vecCollision_;
    
    private JButton btnAdd_;
    private JButton btnRemove_;
    private JButton btnEdit_;
    private JButton btnAddAll_;
    private CollisionPairEditorPanel editorPanel_;
    
    private String defaultStaticFriction_;
    private String defaultSlidingFriction_;
    
    private static final String ATTR_NAME_SPRING = "springConstant";
    private static final String ATTR_NAME_DAMPER = "damperConstant";
    private static final String ATTR_NAME_SD = "springDamperModel";
    private static final String ATTR_NAME_STATIC_FRICTION = "staticFriction";
    private static final String ATTR_NAME_SLIDING_FRICTION = "slidingFriction";
    
    public void setEnabled(boolean flag) {
        super.setEnabled(flag);
        if (editorPanel_ != null)
          editorPanel_.doCancel();
        _setButtonEnabled(flag);
    }
    /*
    private String _getJointName(String linkName){
        if(linkName.indexOf("_")==-1){
            return"";
        }else{
            return linkName.substring(linkName.indexOf("_") + 1);
        }
    }
    private String _getObjectName(String linkName){
        if(linkName.indexOf("_")==-1){
            return linkName;
        }else{
            return linkName.substring(0,linkName.indexOf("_") );
        }
    }
    */
    
    public CollisionPairPanel(GrxPluginManager manager) {
        manager_ = manager;
        parentFrame_ = manager_.getFrame();
        //AttributeProperties props = AttributeProperties.getProperties("CollisionPair");
        defaultStaticFriction_ = "0.5";//props.getProperty(ATTR_NAME_STATIC_FRICTION,AttributeProperties.PROPERTY_DEFAULT_VALUE);
        defaultSlidingFriction_ = "0.5";//props.getProperty(ATTR_NAME_SLIDING_FRICTION,AttributeProperties.PROPERTY_DEFAULT_VALUE);
        
        vecCollision_ = new Vector<GrxBaseItem>();
   
        setLayout(new GridLayout(2,1));
        
        AbstractTableModel dataModel = new AbstractTableModel() {
            private final String[] clmName_ ={
                MessageBundle.get("panel.collision.table.obj1"),
                MessageBundle.get("panel.collision.table.link1"),
                MessageBundle.get("panel.collision.table.obj2"),
                MessageBundle.get("panel.collision.table.link2"),
                MessageBundle.get("panel.collision.table.sd")
            };

            private final String[] attrName_ ={
                "objectName1","jointName1",
                "objectName2","jointName2",
                ATTR_NAME_SD
            };
            
            public int getColumnCount() { return 5; }
            public int getRowCount() { return vecCollision_.size();}
            public String getColumnName(int col) {
                return clmName_[col];
            }
            public java.lang.Object getValueAt(int row, int col) {
                GrxBaseItem item = vecCollision_.get(row);
                String str = null;
                try {
                    str = item.getProperty(attrName_[col], "");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                return str;
            }
            public Class<? extends Object> getColumnClass(int c) {return getValueAt(0, c).getClass();}
            public boolean isCellEditable(int row, int col) {return false;}
         };
         
         table_ = new JTable(dataModel);
         table_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION   );
         table_.getSelectionModel().addListSelectionListener( 
            new ListSelectionListener(){
                public void valueChanged(ListSelectionEvent e) {
                    int row = table_.getSelectedRow() ;
                    if (row >= 0 && row < vecCollision_.size()) {
                        editorPanel_.setNode(
                            (GrxCollisionPairItem)vecCollision_.get(row)
                        );
                    }
                }
            }
         );
         
         scrollPane_ = new JScrollPane(table_);
         scrollPane_.setBounds(12, 12, 360, 120);
         
        
        JPanel pnlBttn = new JPanel();
        pnlBttn.setLayout(new GridLayout(1,4));
        
        btnAdd_ = new JButton(MessageBundle.get("panel.add"));
        btnAdd_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _setButtonEnabled(false);
                    editorPanel_.startAddMode();
                }
            }
        );
        
        //btnAdd_.setBounds(24,144+24,60,24);
        pnlBttn.add(btnAdd_);
        
        btnRemove_ = new JButton(MessageBundle.get("panel.remove"));
        btnRemove_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int row = table_.getSelectedRow() ;
                    if (row >= 0 && row < vecCollision_.size()) {
                        if (_checkDialog(MessageBundle.get("collision.remove"))) {
                        	manager_.removeItem((GrxBaseItem)vecCollision_.get(row));
//                            world_.removeChild(
//                                (GrxBaseItem)vecCollision_.get(row)
//                            );
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
                    if(row>=0 && row<vecCollision_.size()){
                        _setButtonEnabled(false);
                        editorPanel_.startEditMode(
                            (GrxCollisionPairItem)vecCollision_.get(row)
                        );
                    }
                }
            }
        );
        pnlBttn.add(btnEdit_);

        btnAddAll_ = new JButton(MessageBundle.get("panel.addAll"));
        btnAddAll_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    List<GrxBaseItem> list = manager_.getSelectedItemList(GrxModelItem.class);
                    for (int i=0; i<list.size(); i++) {
                    	GrxModelItem m1 = (GrxModelItem)list.get(i);
                    	for (int j=0; j<list.size(); j++) {
                    		GrxModelItem m2 = (GrxModelItem)list.get(j);
                    		for (int k=1; k<m1.lInfo_.length; k++) {
                    			for (int l=1; l<m2.lInfo_.length; l++) {
                    				if (i != j || k != l)
                    					_createItem(m1.getName(), m1.lInfo_[k].name, m2.getName(), m2.lInfo_[l].name);
                    			}
                    		}
                    	}
                    }
/*                   
                    Vector<GrxBaseItem> vector = new Vector<GrxBaseItem>();
                    for (int i = 0; i < vector.size(); i ++) {
                        for (int j = i + 1; j < vector.size(); j ++) {
                            CollisionPairNode node = new CollisionPairNode(
                                ((SimulationNode)vector.get(i)).getName(),
                                "",
                                ((SimulationNode)vector.get(j)).getName(),
                                ""
                            );
                            CollisionPairNode sameNode = _checkSameNode(node);
                            if (sameNode == null) {
                                world_.addChild(node);
                            }
                        }
                    }
*/
                }
            }
        );
        //btnAddAll_.setBounds(24 + 72 + 72 +72,144+24,60,24);
        pnlBttn.add(btnAddAll_);
        
        JPanel pnlTable = new JPanel();
        pnlTable.setLayout(new BorderLayout());
        pnlTable.add(BorderLayout.CENTER,scrollPane_);
        pnlTable.add(BorderLayout.SOUTH,pnlBttn);
        pnlTable.setPreferredSize(new Dimension(360,180));
        add(pnlTable);
    }

    private boolean _checkDialog(String msg) {
        int overwrite =
            new ConfirmDialog(parentFrame_, MessageBundle.get("dialog.overwrite"), msg).showModalDialog();

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
        btnAdd_.setEnabled(flag);
        btnRemove_.setEnabled(flag);
        btnEdit_.setEnabled(flag);
        btnAddAll_.setEnabled(flag);
        table_.setEnabled(flag);
        _repaint();
    }

    private void _repaint() {
        table_.columnMarginChanged(new ChangeEvent(table_) );
        scrollPane_.repaint();
    }    

    private class CollisionPairEditorPanel extends JPanel {

        private static final int MODE_ADD = 0 ;
        private static final int MODE_EDIT = 1 ;

        private int mode_;
        private GrxCollisionPairItem node_;
        private JointSelectPanel pnlJoint1_;
        private JointSelectPanel pnlJoint2_;
        private JButton btnOk_, btnCancel_;
        private JTextField txtSpring_,txtDamper_;
        private JLabel lblSpring_,lblDamper_;
        
        private JTextField txtStaticFric_,txtSlidingFric_;
        private JLabel lblFriction_,lblStaticFric_,lblSlidingFric_;
        
        private JCheckBox chkDamper_;

        public CollisionPairEditorPanel() {
            setLayout(null);
            lblSpring_ = new JLabel(
                MessageBundle.get("panel.collision.springConst"),
                JLabel.RIGHT
            );

            lblSpring_.setBounds(0,60+24,144,24);
            add(lblSpring_);
            
            lblDamper_ = new JLabel(
                MessageBundle.get("panel.collision.dumperConst"),
                JLabel.RIGHT
            );
            lblDamper_.setBounds(0,60+24+24,144,24);
            add(lblDamper_);
            
            txtSpring_ = new JTextField();
            txtDamper_ = new JTextField();
            txtSpring_.setBounds(150,60+24,180,24);
            txtDamper_.setBounds(150,60+24+24,180,24);
            this.add(txtSpring_);
            this.add(txtDamper_);
            
            lblFriction_ = new JLabel(
                MessageBundle.get("panel.collision.friction"),
                JLabel.LEFT
            );
            lblFriction_.setBounds(20,60+24+24+ 24,144,24);
            this.add(lblFriction_);
            
            lblStaticFric_ = new JLabel(
                MessageBundle.get("panel.collision.staticFriction"),
                JLabel.RIGHT
            );
            lblStaticFric_.setBounds(0,60+24 + 24+ 24 + 24,144,24);
            add(lblStaticFric_);
            
            lblSlidingFric_ = new JLabel(
                MessageBundle.get("panel.collision.slidingFriction"),
                JLabel.RIGHT
            );
            lblSlidingFric_.setBounds(0,60+24+24+ 24 + 24 + 24,144,24);
            add(lblSlidingFric_);
            
            txtStaticFric_ = new JTextField();
            txtSlidingFric_ = new JTextField();
            txtStaticFric_.setBounds(150,60+24+ 24 + 24+ 24,80,24);
            txtSlidingFric_.setBounds(150,60+24+24+ 24 + 24+ 24,80,24);
            this.add(txtStaticFric_);
            this.add(txtSlidingFric_);

            chkDamper_ = new JCheckBox(
                MessageBundle.get("panel.collision.sd"),
                false
            );

            chkDamper_.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setEnabled(isEnabled());
                    }
                }
            );

            chkDamper_.setBounds(12,60,180,24);
            add(chkDamper_);
            
            pnlJoint1_ = new JointSelectPanel("1");
            pnlJoint2_ = new JointSelectPanel("2");
            
            pnlJoint1_.setBounds(0,0,180,60);
            pnlJoint2_.setBounds(0 + 180,0,180,60);
            this.add(pnlJoint1_);
            this.add(pnlJoint2_);
            
            btnOk_ = new JButton(MessageBundle.get("dialog.okButton"));
            btnOk_.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        switch (mode_) {
                        case MODE_ADD:
                            if (pnlJoint1_.getObjectName().equals(pnlJoint2_.getObjectName())
                                && pnlJoint1_.getJointName().equals(pnlJoint2_.getJointName())) {
                                new WarningDialog(
                                    parentFrame_,
                                    "",
                                    MessageBundle.get("collision.invalid")
                                ).showModalDialog();
                                return;
                            }

                            GrxCollisionPairItem item = _createItem(pnlJoint1_.getObjectName(), pnlJoint1_.getJointName(),
                                	pnlJoint2_.getObjectName(), pnlJoint2_.getJointName());
                            
                            if (item == null || !_setAttribute(item)){
                            	System.out.println("error: item = "+item);
                            	return;
                            }
                            
                            /*GrxBaseItem sameNode = _checkSameNode(currentWorld_);
                            if (sameNode == null) {
                                world_.addChild(node_);
                            } else {
                                if (_checkDialog(MessageBundle.get("collision.overwrite"))){
                                    _setAttribute(sameNode);
                                } else {
                                    return;
                                }
                            }
                            break;
                            */
                          	break;
                        case MODE_EDIT:
                            _setAttribute(node_);
                            break;
                        }
                        setEnabled(false);
                    }

					private boolean _setAttribute(GrxCollisionPairItem node) {
                          try{
                              node.setProperty(
                                  ATTR_NAME_SD,
                                  new SEBoolean(chkDamper_.isSelected()).toString()
                              );
                              node.setProperty( 
                                  ATTR_NAME_SPRING,
                                  txtSpring_.getText()
                              );
                              node.setProperty( 
                                  ATTR_NAME_DAMPER,
                                  txtDamper_.getText()
                              );
                              node.setProperty( 
                                  ATTR_NAME_STATIC_FRICTION,
                                  txtStaticFric_.getText()
                              );
                              node.setProperty( 
                                  ATTR_NAME_SLIDING_FRICTION,
                                  txtSlidingFric_.getText()
                              );
                          } catch (Exception ex) {
                              new WarningDialog(
                                  parentFrame_,
                                  "",
                                  MessageBundle.get("message.attributeerror")
                              ).showModalDialog();
                              return false;
                          }
                          return true;
                    }
                }
            );
            btnOk_.setBounds(24,144+ 24+ 24+ 24,84,24);
            this.add(btnOk_);


            btnCancel_ = new JButton(MessageBundle.get("dialog.cancelButton"));
            btnCancel_.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        doCancel();
                    }
                }
            );
            btnCancel_.setBounds(24 + 84 + 12 ,144+ 24+ 24+ 24,84,24);
            this.add(btnCancel_);
        }

        public void startAddMode() {
            mode_ = MODE_ADD;
            setEnabled(true);
            if (pnlJoint1_.entry()){
                pnlJoint2_.entry();
                txtSpring_.setText("0 0 0 0 0 0");
                txtDamper_.setText("0 0 0 0 0 0");
                txtStaticFric_.setText(defaultStaticFriction_);
                txtSlidingFric_.setText(defaultSlidingFriction_);
                node_ = null;
            }else{
                doCancel();
            }
        }

        public void startEditMode(GrxCollisionPairItem node) {
            mode_ = MODE_EDIT;
            setNode(node);
            setEnabled(true);
        }

        public void doCancel() {
            setEnabled(false);
            txtSpring_.setText("");
            txtDamper_.setText("");
            txtStaticFric_.setText("");
            txtSlidingFric_.setText("");
        }

        public void setNode(GrxCollisionPairItem node) {
            try{
                pnlJoint1_.setJoint(
                    node.getStr("objectName1", ""), 
                    node.getStr("jointName1", "")
                );
                pnlJoint2_.setJoint(
                    node.getStr("objectName2", ""), 
                    node.getStr("jointName2", "")
                );
                chkDamper_.setSelected(node.isTrue(ATTR_NAME_SD,false));
                txtSpring_.setText(node.getStr(ATTR_NAME_SPRING, ""));
                txtDamper_.setText(node.getStr(ATTR_NAME_DAMPER, ""));
                txtStaticFric_.setText(node.getStr(ATTR_NAME_STATIC_FRICTION, ""));
                txtSlidingFric_.setText(node.getStr(ATTR_NAME_SLIDING_FRICTION, ""));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            node_ = node;
        }
        
        public void setEnabled(boolean flag) {
            super.setEnabled(flag);
            Component[] cmps = getComponents();
            for (int i = 0; i < cmps.length; i ++) {
                cmps[i].setEnabled(flag);
            }

            if (mode_ == MODE_EDIT) {
                pnlJoint1_.setEnabled(false);
                pnlJoint2_.setEnabled(false);
            }
            _setButtonEnabled(!flag);
            lblSpring_.setEnabled(flag && chkDamper_.isSelected());
            lblDamper_.setEnabled(flag && chkDamper_.isSelected());
            txtSpring_.setEnabled(true);
            txtSpring_.setEditable(flag && chkDamper_.isSelected() );
            txtDamper_.setEnabled(true);
            txtDamper_.setEditable(flag && chkDamper_.isSelected());
            
            lblStaticFric_.setEnabled(flag);
            lblSlidingFric_.setEnabled(flag);
            txtStaticFric_.setEnabled(true);
            txtStaticFric_.setEditable(flag);
            txtSlidingFric_.setEnabled(true);
            txtSlidingFric_.setEditable(flag);
        }

        private class JointSelectPanel extends JPanel {
            //private TitledBorder border_ = null;
            private JComboBox  boxObject_;
            private JComboBox  boxLink_;
            boolean flag_ = false;
            public JointSelectPanel(String title) {
                JLabel label;
                
                setLayout(null);
                
                label = new JLabel(
                    MessageBundle.get("panel.joint.object") + title,
                    JLabel.RIGHT
                );
                label.setBounds(0,6,48,12);
                add(label);
                
                label = new JLabel(
                    MessageBundle.get("panel.joint.link") + title,
                    JLabel.RIGHT
                );
                label.setBounds(0,6 + 24, 48, 12);
                add(label);
                
                boxObject_ = new JComboBox();
                boxObject_.setBounds(6+48,6 ,126,24);
                boxObject_.setLightWeightPopupEnabled(false);
                add(boxObject_);
                
                boxLink_ = new JComboBox();
                boxLink_.setBounds(6+48,6 + 24,126,24);
                boxLink_.setLightWeightPopupEnabled(false);
                add(boxLink_);
                
                boxObject_.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent  evt) {
                          if (!flag_) {
                              _entryLink( 
                                  (GrxModelItem)boxObject_.getSelectedItem() 
                              );
                          }
                        }
                    }
                );
            }
            
            private void _entryLink(GrxModelItem model) {
                 boxLink_.removeAllItems();
                 boxLink_.addItem("");        
                 for (int i=0; i<model.lInfo_.length; i++)
                     boxLink_.addItem(model.lInfo_[i].name);
            }
            
            public boolean  entry() {
                boolean addFlag = false;
                
                flag_ = true;
                boxObject_.removeAllItems();
                flag_ = false;
                
                Iterator it = manager_.getItemMap(GrxModelItem.class).values().iterator();
                while (it.hasNext()) {
                  boxObject_.addItem(it.next());
                  addFlag = true;
                }
                return addFlag;
            }

            public void remove() {
                flag_ = true;
                boxObject_.removeAllItems();
                flag_ = false;
                boxLink_.removeAllItems();
            }
            
            // TODO bad 
            public void setJoint(String obj,String joint) {
                remove();
                flag_ = true;
                
                boxObject_.addItem(obj);
                boxLink_.addItem(joint);
                flag_ = false;
            }
            
            public String getJointName() {
            	Object o = boxLink_.getSelectedItem();
            	if (o != null)
            		return (String)o;
            	else 
            		return null;
                /*
                Object link = boxLink_.getSelectedItem();
                if (link instanceof GrxBaseItem) {
                    String linkName = ((GrxBaseItem)(link)).getName();
                    return linkName;
                }
                return "";
                */
            }

            public String getObjectName() {
                String objName =
                    ((GrxBaseItem)(boxObject_.getSelectedItem())).getName();
                return objName;
            }
            
            public void setEnabled(boolean flag) {
                super.setEnabled(flag);
                Component[] cmps = getComponents();
                for(int i = 0; i < cmps.length; i ++) {
                    cmps[i].setEnabled(flag);
                }
            }
        }    

    }

    public void updateCollisionPairs(List<GrxBaseItem> list) {
      if(editorPanel_!=null)
        remove(editorPanel_);
      
      editorPanel_ = new CollisionPairEditorPanel();
      editorPanel_.setPreferredSize(new Dimension(360,180));
      //editorPanel_.setBounds(12,280,360,300);
      add(editorPanel_);
      editorPanel_.doCancel();
      vecCollision_ = new Vector<GrxBaseItem>();
      boolean isAnyRobot = false;
      for (int i=0; i<list.size(); i++) {
        if (list.get(i) instanceof GrxCollisionPairItem)
          vecCollision_.add(list.get(i));
        else if (list.get(i) instanceof GrxModelItem)
          isAnyRobot = true;
      }
      setEnabled(vecCollision_.size() > 0 || isAnyRobot);
      table_.getSelectionModel().clearSelection();
      _repaint();
    }
    
    public void replaceWorld(GrxWorldStateItem world) {
        if(editorPanel_!=null)
          remove(editorPanel_);
        
        editorPanel_ = new CollisionPairEditorPanel();
        editorPanel_.setPreferredSize(new Dimension(360,180));
        //editorPanel_.setBounds(12,280,360,300);
        add(editorPanel_);
        editorPanel_.doCancel();
        
        vecCollision_ = new Vector<GrxBaseItem>();
        
    }

    public void childAdded(GrxBaseItem item) {
      if(item instanceof GrxCollisionPairItem){
        int i;
        for (i = 0; i < vecCollision_.size(); i ++) {
            vecCollision_.get(i);
            if (vecCollision_.get(i).getName().compareTo(item.getName()) > 0)
              break;
        }
        vecCollision_.add(i, item);
        _repaint();
        return;
      }
    }
/*
    public void childRemoved(SimulationNode node) {
        if (node instanceof CollisionPairNode) {
            vecCollision_.remove(node);
            _repaint();
            editorPanel_.doCancel();
            return;
        } else if (
            node instanceof RobotNode ||
            node instanceof EnvironmentNode
        ){
            Vector delete = new Vector();
            for(int i = 0; i < vecCollision_.size(); i ++) {
                CollisionPairNode col = (CollisionPairNode)vecCollision_.get(i);
                if (col.isYourObject(node.getName())) {
                    delete.add(col);
                }
            }

            for (int i = 0; i < delete.size(); i ++) {
                world_.removeChild((SimulationNode)delete.get(i));
            }
            editorPanel_.doCancel();
        }
    }
*/
    public Vector vecCollision(){ return vecCollision_; }
    
    private GrxCollisionPairItem _createItem(String oName1, String jName1, String oName2, String jName2) {
        GrxCollisionPairItem item = (GrxCollisionPairItem)manager_.createItem(
             GrxCollisionPairItem.class,
             "CP#"+oName1+"_"+jName1+"#"+oName2+"_"+jName2);
        if (item != null) {
         	item.setProperty("objectName1", oName1);
           	item.setProperty("jointName1",  jName1); 
           	item.setProperty("objectName2", oName2);
           	item.setProperty("jointName2",  jName2); 
        } 
        return item;
	}
}

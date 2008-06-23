/**
 * ControllerPanel.java.java
 *
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.simulation;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.graph.SEDoubleTextWithSpinForSWT;

@SuppressWarnings("serial")
public class ControllerPanel extends Composite{
    private GrxPluginManager manager_;
    private TableViewer viewer_;
    //private JScrollPane scrollPane_;
    
    private Vector<GrxBaseItem> vecRobot_;
    
    private Button btnRemove_;
    private Button btnEdit_;
    
    private ControllerEditorPanel editorPanel_;
    
    private static final String ATTRIBUTE_CONTROLLER = "controller";
    private static final String ATTRIBUTE_CONTROL_TIME = "controlTime";
    private static final String ATTRIBUTE_IMAGEPROCESSOR = "imageProcessor";
    private static final String ATTRIBUTE_IMAGEPROCESS_TIME = "imageProcessTime";
    
    private static final int BUTTONS_HEIGHT = 26;
    
    private final String[] clmName_ ={
        MessageBundle.get("panel.controller.table.robot"),
        MessageBundle.get("panel.controller.table.controller"),
        MessageBundle.get("panel.controller.table.controlTime"),
        MessageBundle.get("panel.controller.table.imageProcessor"),
        MessageBundle.get("panel.controller.table.imageProcessTime")
    };
    private final String[] attrName_ = {
        "dummy",
        ATTRIBUTE_CONTROLLER,
        ATTRIBUTE_CONTROL_TIME,
        ATTRIBUTE_IMAGEPROCESSOR,
        ATTRIBUTE_IMAGEPROCESS_TIME,
    };
    
    public ControllerPanel(Composite parent,int style, GrxPluginManager manager) {
        super(parent,style);
        
        manager_ = manager;
        vecRobot_ = new Vector<GrxBaseItem>();
        
        setLayout(new GridLayout(1,false));
       
        viewer_ = new TableViewer(this,SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        viewer_.setContentProvider(new ArrayContentProvider());
        viewer_.setLabelProvider(new ControllerPanelTableLabelProvider());
        TableLayout tableLayout = new TableLayout();
        for(int i=0;i<clmName_.length;i++){
            new TableColumn(viewer_.getTable(),i).setText(clmName_[i]);
            tableLayout.addColumnData(new ColumnWeightData(1,true));
        }
        viewer_.getTable().setLayout(tableLayout);
        viewer_.getTable().setHeaderVisible(true);
        
        viewer_.getTable().addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                int row = viewer_.getTable().getSelectionIndex();
                if(row>=0 && row<vecRobot_.size()){
                    editorPanel_.setNode((GrxModelItem)vecRobot_.get(row));
                }
            }
        });
        viewer_.getTable().setLinesVisible(true);
        viewer_.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer_.setInput(vecRobot_);
        
        Composite pnlBttn = new Composite(this,SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = BUTTONS_HEIGHT;
        pnlBttn.setLayoutData(gridData);
        pnlBttn.setLayout(new FillLayout(SWT.HORIZONTAL));
        
        btnRemove_ = new Button(pnlBttn,SWT.PUSH);
        btnRemove_.setText(MessageBundle.get("panel.detach"));
        btnRemove_.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                int row = viewer_.getTable().getSelectionIndex();
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
            
        });
        
        btnEdit_ = new Button(pnlBttn,SWT.PUSH);
        btnEdit_.setText(MessageBundle.get("panel.edit"));
        btnEdit_.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                int row = viewer_.getTable().getSelectionIndex();
                if(row>=0 && row<vecRobot_.size()){
                    _setButtonEnabled(false);
                    editorPanel_.startEditMode((GrxModelItem)vecRobot_.get(row));
                }
            }
            
        });
        
        String[] names = GrxCorbaUtil.getObjectNameList("localhost",2809);
//        List<String> list = new ArrayList<String>();
//        if (names != null) {
//          for (int i=0; i<names.length; i++) {  
//            //org.omg.CORBA.Object obj = GrxCorbaUtil.getReferenceURL(names[i], "localhost", 2809);
//            //try {
//            //  jp.go.aist.hrp.simulator.ControllerFactoryHelper.narrow(obj);
//              list.add(names[i]);
//            //} catch (Exception e) {}
//          }
//        }
//        list.add(0,"");
        if(names == null){
            names = new String[0];
        }
        
        editorPanel_ = new ControllerEditorPanel(this,SWT.NONE,names);
    }
    
    
    
    private boolean _checkDialog(String msg) {
        boolean overwrite = MessageDialog.openQuestion(getShell(), MessageBundle.get("dialog.overwrite"), msg);
        return overwrite;
    }

    private void _setButtonEnabled(boolean flag) {
        btnRemove_.setEnabled(flag);
        btnEdit_.setEnabled(flag);
        viewer_.getTable().setEnabled(flag);
        _repaint();
    }

    private void _repaint() {
        viewer_.getTable().update();
        //viewer_.columnMarginChanged(new ChangeEvent(viewer_) );
        //scrollPane_.repaint();
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
    private class ControllerEditorPanel extends Composite {
        //private static final int MODE_ADD = 0 ;
        private static final int MODE_EDIT = 1 ;

        private static final int COMBO_WIDTH = 100;
        private static final int BUTTON_WIDTH = 50;
        
        private int mode_;
        private GrxBaseItem node_;
        private Combo boxController_;
        private Combo boxImageProcessor_;
        private SEDoubleTextWithSpinForSWT spinControlTime_;
        private SEDoubleTextWithSpinForSWT spinImageProcessTime_;
        private Button btnOk_,btnCancel_;
        private Label lblControlTime_;
        private Label lblImageProcessTime_;
        private Label lblController_;
        private Label lblImageProcessor_;
        
        public ControllerEditorPanel(Composite parent,int style,String[] initialNames) {
            super(parent,style);
            setLayout(new GridLayout(2,true));

            lblController_ = new Label(this,SWT.SHADOW_NONE);
            lblController_.setText(MessageBundle.get("panel.controller.controller"));
            lblController_.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            
            boxController_ = new Combo(this,SWT.DROP_DOWN);
            boxController_.setItems(initialNames);
            boxController_.select(0);
            GridData gridData = new GridData();
            gridData.widthHint = COMBO_WIDTH;
            boxController_.setLayoutData(gridData);
            //boxController_.setEditable(true);
            //boxController_.setLightWeightPopupEnabled(false);
            
            //もともとはSwingのPopupMenuListenrを使っていたが、MouseListenerで代用
            boxController_.addMouseListener(new MouseListener(){

                public void mouseDoubleClick(MouseEvent e) {
                }

                public void mouseDown(MouseEvent e) {
                  String[] names = GrxCorbaUtil.getObjectNameList("localhost",2809);
                  if(names == null){
                      names = new String[0];
                  }
                  boxController_.removeAll();
                  boxController_.setItems(names);
                }

                public void mouseUp(MouseEvent e) {
                }
                
            });
//            boxController_.addPopupMenuListener(new PopupMenuListener(){
//              public void popupMenuCanceled(PopupMenuEvent e) {}
//              public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
//              public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//                String[] names = GrxCorbaUtil.getObjectNameList("localhost",2809);
//                boxController_.removeAllItems();
//                for (int i=0; i<names.length; i++)
//                  boxController_.addItem(names[i]);
//              }            
//            });
            
            lblControlTime_ = new Label(this,SWT.SHADOW_NONE);
            lblControlTime_.setText(MessageBundle.get("panel.controller.controlTime"));
            lblControlTime_.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            
            spinControlTime_ = new SEDoubleTextWithSpinForSWT(this,SWT.NONE,0,10,0.001);
            
            lblImageProcessor_ = new Label(this,SWT.SHADOW_NONE);
            lblImageProcessor_.setText(MessageBundle.get("panel.controller.imageProcessor"));
            lblImageProcessor_.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            
            boxImageProcessor_ = new Combo(this,SWT.DROP_DOWN);
            gridData = new GridData();
            gridData.widthHint = COMBO_WIDTH;
            boxImageProcessor_.setLayoutData(gridData);
            //boxImageProcessor_.setLightWeightPopupEnabled(false);
            
            // TODO コンボボックスを開いたときにこのイベントが起こるかどうか確認、起こらなければ他のイベントリスナを検討
            //もともとはSwingのPopupMenuListenrを使っていたが、MouseListenerで代用
            boxImageProcessor_.addMouseListener(new MouseListener(){

                public void mouseDoubleClick(MouseEvent e) {
                }

                public void mouseDown(MouseEvent e) {
                  String[] names = GrxCorbaUtil.getObjectNameList("localhost",2809);
                  if(names == null){
                      names = new String[0];
                  }
                  boxImageProcessor_.removeAll();
                  boxImageProcessor_.setItems(names);
                }

                public void mouseUp(MouseEvent e) {
                }
                
            });
//            boxImageProcessor_.addPopupMenuListener(new PopupMenuListener(){
//              public void popupMenuCanceled(PopupMenuEvent e) {}
//              public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
//              public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//                String[] names = GrxCorbaUtil.getObjectNameList("localhost",2809);
//                boxImageProcessor_.removeAllItems();
//                for (int i=0; i<names.length; i++)
//                  boxImageProcessor_.addItem(names[i]);
//              }            
//            });
            
            lblImageProcessTime_ = new Label(this,SWT.SHADOW_NONE);
            lblImageProcessTime_.setText(MessageBundle.get("panel.controller.imageProcessTime"));
            lblImageProcessTime_.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            
            spinImageProcessTime_ = new SEDoubleTextWithSpinForSWT(this,SWT.NONE,0,10,0.001);
            
            btnOk_ = new Button(this,SWT.PUSH);
            btnOk_.setText(MessageBundle.get("dialog.okButton"));
            btnOk_.addSelectionListener(new SelectionListener(){

                public void widgetDefaultSelected(SelectionEvent e) {
                }

                public void widgetSelected(SelectionEvent e) {
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
                            boxController_.getText()
                        );
                        node.setProperty(
                            ATTRIBUTE_CONTROL_TIME,
                            spinControlTime_.getValue().toString()
                        );
                        node.setProperty(
                            ATTRIBUTE_IMAGEPROCESSOR,
                            boxImageProcessor_.getText()
                        );
                        node.setProperty(
                            ATTRIBUTE_IMAGEPROCESS_TIME,
                            spinImageProcessTime_.getValue().toString()
                        );
                    } catch (Exception ex) {
                        MessageDialog.openWarning(getShell(),"", MessageBundle.get("message.attributeerror"));
                        return false;
                        //ex.printStackTrace();
                    }
                    return true;
                }
                
            });
            gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
            gridData.widthHint = BUTTON_WIDTH;
            btnOk_.setLayoutData(gridData);
            
            btnCancel_ = new Button(this,SWT.PUSH);
            btnCancel_.setText(MessageBundle.get("dialog.cancelButton"));
            btnCancel_.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    public void widgetSelected(SelectionEvent e) {
                        doCancel();
                    }
                }
            );
            gridData = new GridData();
            //gridData.widthHint = BUTTON_WIDTH;
            btnCancel_.setLayoutData(gridData);
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
                String controlName = node.getProperty(ATTRIBUTE_CONTROLLER);
                if (controlName != null) {
                	for (int i = 0; i < boxController_.getItemCount(); i ++) {
                	    if (controlName.equals(boxController_.getItem(i).toString())) {
                            boxController_.select(i);
                	        break;
                	    }
                	}
                	spinControlTime_.setValue(
                	    node.getProperty(ATTRIBUTE_CONTROL_TIME, "0.001")
                	);
                }
                

                controlName = node.getProperty(ATTRIBUTE_IMAGEPROCESSOR);
                if (controlName != null) {
                	for (int i = 0; i < boxImageProcessor_.getItemCount(); i ++) {
                	    if (controlName.equals(boxImageProcessor_.getItem(i).toString())) {
                	        boxImageProcessor_.select(i);
                	        break;
                	    }
                	}
                	spinImageProcessTime_.setValue(
                	    node.getProperty(ATTRIBUTE_IMAGEPROCESS_TIME, "0.033")
                	);
                }
                
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            node_ = node;
        }
        
        public void setEnabled(boolean flag) {
            super.setEnabled(flag);
            Control[] cmps = this.getChildren();
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

    private class ControllerPanelTableLabelProvider implements ITableLabelProvider{

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            GrxModelItem node = (GrxModelItem)element;
            String str = null;
            if (columnIndex ==0){
                return node.getName();
            }
            try{
                str = node.getProperty(attrName_[columnIndex]);
            }catch(Exception ex){
                ex.printStackTrace();
                return "";
            }
            if (str == null)
              str = "";
            
            return str;
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
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
    viewer_.setInput(vecRobot_);
    _repaint();
  }
   
  public int getRobotNum() {
    return vecRobot_.size();
  }
  
}

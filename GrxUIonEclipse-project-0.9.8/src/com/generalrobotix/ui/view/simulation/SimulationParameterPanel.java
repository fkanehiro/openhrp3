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
 * SimulationParameterPanel.java.java
 *
 * @author  Kernel, Inc.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.simulation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.util.ItemPropertyDoubleSpinForSWT;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.graph.SEEnumeration;

@SuppressWarnings("serial") //$NON-NLS-1$
public class SimulationParameterPanel extends Composite{
  private GrxBaseItem currentItem_;
  public static final String[] METHOD_NAMES = { "RUNGE_KUTTA", "EULER" }; //$NON-NLS-1$ //$NON-NLS-2$
  private static final SEEnumeration seTemp_ = new SEEnumeration(METHOD_NAMES, 0);
  
  private static final int COMBO_WIDTH = 100;
  private static final double MIN_TIME_STEP = 0.00001;
  
  ItemPropertyDoubleSpinForSWT spinTotalTime_;
  ItemPropertyDoubleSpinForSWT spinStepTime_;
  ItemPropertyDoubleSpinForSWT spinLogStepTime_;
  ItemPropertyDoubleSpinForSWT spinGravity_;
  Button chkIntegrate_;
  Button chkViewSimulate_;
  Combo cmbMethod_;
  
  public SimulationParameterPanel(Composite parent,int style) {
    super(parent, style);
    setLayout(new GridLayout(2,true));

    Label label = new Label(this,SWT.SHADOW_NONE);
    label.setText(MessageBundle.get("panel.simulation.start.title")); //$NON-NLS-1$
    //label.setBounds(12, 12, 240, 24);

    label = new Label(this,SWT.SHADOW_NONE);//dummy

    label = new Label(this,SWT.SHADOW_NONE);
    label.setText(MessageBundle.get("panel.simulation.start.totalTime")); //$NON-NLS-1$
    GridData gridData = new GridData();
    gridData.horizontalAlignment = SWT.END;
    label.setLayoutData(gridData);
    //label.setBounds(12, 12 + 36, 130, 24);
    
    spinTotalTime_ = new ItemPropertyDoubleSpinForSWT(this,SWT.NONE,MIN_TIME_STEP, Double.POSITIVE_INFINITY, 0.1);
    spinTotalTime_.setKey("totalTime"); //$NON-NLS-1$
    //spinTotalTime_.setBounds(12 + 130 + 6, 12 + 36, 100, 24);

    label = new Label(this,SWT.SHADOW_NONE);
    label.setText(MessageBundle.get("panel.simulation.start.stepTime")); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalAlignment = SWT.END;
    label.setLayoutData(gridData);
    //label.setBounds(12, 12 + 36 + 36, 130, 24);
    
    spinStepTime_ = new ItemPropertyDoubleSpinForSWT(this,SWT.NONE,MIN_TIME_STEP, 0.1, 0.001);
    spinStepTime_.setKey("timeStep"); //$NON-NLS-1$
    //spinStepTime_.setBounds(12 + 130 + 6, 12 + 36 + 36, 100, 24);
    
    label = new Label(this,SWT.SHADOW_NONE);
    label.setText(MessageBundle.get("panel.simulation.start.logStepTime")); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalAlignment = SWT.END;
    label.setLayoutData(gridData);
    //label.setBounds(12, 12 + 36 + 36 + 36, 130, 24);

    spinLogStepTime_ = new ItemPropertyDoubleSpinForSWT(this,SWT.NONE,0.00001, 0.1, 0.001);
    spinLogStepTime_.setKey("logTimeStep"); //$NON-NLS-1$
    //spinLogStepTime_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36, 100, 24);
    
    label = new Label(this,SWT.SHADOW_NONE);
    label.setText(MessageBundle.get("panel.simulation.start.method")); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalAlignment = SWT.END;
    label.setLayoutData(gridData);
    //label.setBounds(12, 12 + 36 + 36 + 36 + 36, 130, 24);

    cmbMethod_ = new Combo(this,SWT.DROP_DOWN | SWT.READ_ONLY);
    cmbMethod_.setItems(METHOD_NAMES);
    gridData = new GridData();
    gridData.widthHint = COMBO_WIDTH;
    cmbMethod_.setLayoutData(gridData);
    cmbMethod_.select(0);
    //cmbMethod_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36 + 36, 100, 24);
    cmbMethod_.addSelectionListener(new SelectionListener(){

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            if (currentItem_ != null)
                currentItem_.setProperty("method", cmbMethod_.getText()); //$NON-NLS-1$
        }
        
    });

//    cmbMethod_.removeAll();
//    for (int i = 0; i < METHOD_NAMES.length; i++) {
//        cmbMethod_.addItem(new SEEnumeration(METHOD_NAMES, i));
//    }

    //-------------------------

    label = new Label(this,SWT.SHADOW_NONE);
    label.setText(MessageBundle.get("panel.simulation.start.gravitation")); //$NON-NLS-1$
    gridData = new GridData();
    gridData.horizontalAlignment = SWT.END;
    label.setLayoutData(gridData);
    //label.setBounds(12, 12 + 36 + 36 + 36 + 36 + 36, 130, 24);

    spinGravity_ = new ItemPropertyDoubleSpinForSWT(this,SWT.NONE,0, Double.POSITIVE_INFINITY, 0.1);
    spinGravity_.setKey("gravity"); //$NON-NLS-1$
    spinGravity_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36 + 36 + 36, 100, 24);

    chkIntegrate_ = new Button(this,SWT.CHECK);
    chkIntegrate_.setText(MessageBundle.get("panel.simulation.start.integrate")); //$NON-NLS-1$
    //chkIntegrate_.setBounds(12 + 80 + 6, 12 + 36 + 36 + 36 + 36 + 36 + 36, 160, 24);
    chkIntegrate_.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            if (currentItem_ != null)
                currentItem_.setProperty("integrate", String.valueOf(chkIntegrate_.getSelection())); //$NON-NLS-1$
        }
    });
    
    label = new Label(this,SWT.SHADOW_NONE);//dummy

    chkViewSimulate_ = new Button(this,SWT.CHECK);
    chkViewSimulate_.setText(MessageBundle.get("panel.simulation.start.viewsimulate")); //$NON-NLS-1$
    chkViewSimulate_.addSelectionListener(new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            if (currentItem_ != null)
                currentItem_.setProperty("viewsimulate", String.valueOf(chkViewSimulate_.getSelection())); //$NON-NLS-1$
        }

    });
    //chkViewSimulate_.setBounds(12 + 80 + 6, 12 + 36 + 36 + 36 + 36 + 36 + 36 + 36, 160, 24);
    
    label = new Label(this,SWT.SHADOW_NONE);//dummy

    gridData = new GridData();
    gridData.horizontalAlignment = SWT.END;
    
    this.setSize(260,330);
    //setPreferredSize(new Dimension(260, 300));
  }

  public void setEnabled(boolean flag) {
    super.setEnabled(flag);
    Control[] cmps = this.getChildren();
    for (int i = 0; i < cmps.length; i++) {
      cmps[i].setEnabled(flag);
    }
  }
  

  public void setMethod(String method) {
    seTemp_.fromString(method);
    cmbMethod_.setText(cmbMethod_.getItem(seTemp_.getSelectedIndex()));
  }
  
  public void setStepTime(double time) {
    spinStepTime_.setValue(time);
  }
  
  public void setLogStepTime(double time) {
    spinLogStepTime_.setValue(time);
  }
  
  public void setTotalTime(double time) {
    spinTotalTime_.setValue(time);
  }
    
  public void setIntegrate(boolean f) {
    chkIntegrate_.setSelection(f);
  }

  public void setViewSimulate(boolean f) {
    chkViewSimulate_.setSelection(f);
  }

  public void setGravity(double g) {
    spinGravity_.setValue(g);
  }

  public double getStepTime() {
    return spinStepTime_.getValueDouble();
  }
  
  public double getLogStepTime() {
    return spinLogStepTime_.getValueDouble();
  }
  
  public double getTotalTime() {
      return spinTotalTime_.getValueDouble();
  }
  
  public boolean isIntegrate() {
    return chkIntegrate_.getSelection();
  }

  public boolean isSimulatingView() {
    return chkViewSimulate_.getSelection();
  }

  public double getGravity() {
	    return spinGravity_.getValueDouble();
  }
  
  public SEEnumeration getMethod() {
      return new SEEnumeration(METHOD_NAMES,cmbMethod_.getSelectionIndex());
      //return (SEEnumeration) cmbMethod_.getSelectedItem();
  }
  
  public void updateItem(GrxBaseItem item) {
      setEnabled(item != null);
      currentItem_ = item;
      spinTotalTime_.setItem(item);
      spinStepTime_.setItem(item);
  	  spinLogStepTime_.setItem(item);
  	  spinGravity_.setItem(item);
	  
	  if (item != null) { 
		  setTotalTime(item.getDbl("totalTime", 20.0)); //$NON-NLS-1$
		  setStepTime(item.getDbl("timeStep", 0.001)); //$NON-NLS-1$
		  setLogStepTime(item.getDbl("logTimeStep", 0.001)); //$NON-NLS-1$
		  setGravity(item.getDbl("gravity", 9.8)); //$NON-NLS-1$
		  setMethod(item.getProperty("method",METHOD_NAMES[0])); //$NON-NLS-1$
		  setIntegrate(item.isTrue("integrate", true)); //$NON-NLS-1$
		  setViewSimulate(item.isTrue("viewsimulate", false)); //$NON-NLS-1$
		  setEnabled(true);
	  }
  }
}

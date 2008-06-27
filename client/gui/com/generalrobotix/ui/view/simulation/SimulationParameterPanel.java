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
 * SimulationParameterPanel.java
 *
 * @author  Kernel, Inc.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.simulation;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.util.ItemPropertyDoubleSpin;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.graph.SEEnumeration;


@SuppressWarnings("serial")
public class SimulationParameterPanel extends JPanel {
  private GrxBaseItem currentItem_;
  public static final String[] METHOD_NAMES = { "RUNGE_KUTTA", "EULER" };
  private static final SEEnumeration seTemp_ = new SEEnumeration(METHOD_NAMES, 0);
  
  ItemPropertyDoubleSpin spinTotalTime_;
  ItemPropertyDoubleSpin spinStepTime_;
  ItemPropertyDoubleSpin spinLogStepTime_;
  ItemPropertyDoubleSpin spinGravity_;
  ItemPropertyDoubleSpin spinViewSimulationStepTime_;

  JCheckBox chkIntegrate_;
  JCheckBox chkViewSimulate_;
  JComboBox cmbMethod_;
  
  JLabel lblViewSimulationStepTime_;

  public SimulationParameterPanel() {
    setLayout(null);
    
    JLabel label = new JLabel(MessageBundle.get("panel.simulation.start.title"));
    label.setBounds(12, 12, 240, 24);
    add(label);

    label = new JLabel(MessageBundle.get("panel.simulation.start.totalTime"), JLabel.RIGHT);
    label.setBounds(12, 12 + 36, 130, 24);
    add(label);

    spinTotalTime_ = new ItemPropertyDoubleSpin(0.1, Double.POSITIVE_INFINITY, 0.1);
    spinTotalTime_.setBounds(12 + 130 + 6, 12 + 36, 100, 24);
    this.add(spinTotalTime_);

    label = new JLabel(MessageBundle.get("panel.simulation.start.stepTime"), JLabel.RIGHT);
    label.setBounds(12, 12 + 36 + 36, 130, 24);
    add(label);

    spinStepTime_ = new ItemPropertyDoubleSpin(0.00001, 0.1, 0.001);
    spinStepTime_.setBounds(12 + 130 + 6, 12 + 36 + 36, 100, 24);    
    this.add(spinStepTime_);
    
    label = new JLabel(MessageBundle.get("panel.simulation.start.logStepTime"), JLabel.RIGHT);
    label.setBounds(12, 12 + 36 + 36 + 36, 130, 24);
    add(label);

    spinLogStepTime_ = new ItemPropertyDoubleSpin(0.00001, 0.1, 0.001);
    spinLogStepTime_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36, 100, 24);
    this.add(spinLogStepTime_);
    
    label = new JLabel(MessageBundle.get("panel.simulation.start.method"), JLabel.RIGHT);
    label.setBounds(12, 12 + 36 + 36 + 36 + 36, 130, 24);
    add(label);

    cmbMethod_ = new JComboBox(METHOD_NAMES);
    cmbMethod_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36 + 36, 100, 24);
    cmbMethod_.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			if (currentItem_ != null)
				currentItem_.setProperty("method", cmbMethod_.getSelectedItem().toString());
		}
    });
    this.add(cmbMethod_);

    cmbMethod_.removeAllItems();
    for (int i = 0; i < METHOD_NAMES.length; i++) {
      cmbMethod_.addItem(new SEEnumeration(METHOD_NAMES, i));
    }

    //-------------------------

    label = new JLabel(MessageBundle.get("panel.simulation.start.gravitation"), JLabel.RIGHT);
    label.setBounds(12, 12 + 36 + 36 + 36 + 36 + 36, 130, 24);
    add(label);

    spinGravity_ = new ItemPropertyDoubleSpin(0, Double.POSITIVE_INFINITY, 0.1);
    spinGravity_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36 + 36 + 36, 100, 24);
    this.add(spinGravity_);

    chkIntegrate_ = new JCheckBox(
      MessageBundle.get("panel.simulation.start.integrate"));
    chkIntegrate_.setBounds(12 + 80 + 6, 12 + 36 + 36 + 36 + 36 + 36 + 36, 160, 24);
    chkIntegrate_.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (currentItem_ != null)
				currentItem_.setProperty("integrate", String.valueOf(chkIntegrate_.isSelected()));
		}
    });
    this.add(chkIntegrate_);

    chkViewSimulate_ = new JCheckBox(MessageBundle.get("panel.simulation.start.viewsimulate"));
    chkViewSimulate_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lblViewSimulationStepTime_.setEnabled(isViewSimulate());
        spinViewSimulationStepTime_.setEnabled(isViewSimulate());
        if (currentItem_ != null)
        	currentItem_.setProperty("viewsimulate", String.valueOf(chkViewSimulate_.isSelected()));
      }
    });
    chkViewSimulate_.setBounds(12 + 80 + 6, 12 + 36 + 36 + 36 + 36 + 36 + 36 + 36, 160, 24);
    this.add(chkViewSimulate_);

    lblViewSimulationStepTime_ = new JLabel(
      MessageBundle.get("panel.simulation.start.viewsimulationStepTime"), JLabel.RIGHT);
    lblViewSimulationStepTime_.setBounds(12, 12 + 36 + 36 + 36 + 36 + 36 + 36 + 36 + 36, 130, 24);
    add(lblViewSimulationStepTime_);

    spinViewSimulationStepTime_ = new ItemPropertyDoubleSpin(0.033, 100, 0.001);
    spinViewSimulationStepTime_.setBounds(12 + 130 + 6, 12 + 36 + 36 + 36 + 36 + 36 + 36 + 36 + 36, 100, 24);
    this.add(spinViewSimulationStepTime_);
    
    setPreferredSize(new Dimension(260, 300));
  }

  public void setEnabled(boolean flag) {
    super.setEnabled(flag);
    Component[] cmps = getComponents();
    for (int i = 0; i < cmps.length; i++) {
      cmps[i].setEnabled(flag);
    }
    lblViewSimulationStepTime_.setEnabled(flag && isViewSimulate());
    spinViewSimulationStepTime_.setEnabled(flag && isViewSimulate());
  }
  

  public void setMethod(String method) {
    seTemp_.fromString(method);
    cmbMethod_.setSelectedIndex(seTemp_.getSelectedIndex());
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
    chkIntegrate_.setSelected(f);
  }

  public void setViewSimulate(boolean f) {
    chkViewSimulate_.setSelected(f);
  }

  public void setGravity(double g) {
    spinGravity_.setValue(g);
  }

  public void setViewSimulationStepTime(double g) {
    spinViewSimulationStepTime_.setValue(g);
  }
  
  public double getStepTime() {
    return spinStepTime_.getValue();
  }
  
  public double getLogStepTime() {
    return spinLogStepTime_.getValue();
  }
  
  public double getTotalTime() {
    return spinTotalTime_.getValue();
  }
  
  public boolean isIntegrate() {
    return chkIntegrate_.isSelected();
  }

  public boolean isViewSimulate() {
    return chkViewSimulate_.isSelected();
  }

  public double getGravity() {
	    return spinGravity_.getValue();
  }
  
  public double getViewSimulationStepTime() {
    return spinViewSimulationStepTime_.getValue();
  }
  
  public SEEnumeration getMethod() {
    return (SEEnumeration) cmbMethod_.getSelectedItem();
  }
  
  public void updateItem(GrxBaseItem item) {
      setEnabled(item != null);
      currentItem_ = item;
      spinTotalTime_.setItem(item, "totalTime");
      spinStepTime_.setItem(item, "timeStep");
  	  spinLogStepTime_.setItem(item, "logTimeStep");
  	  spinGravity_.setItem(item, "gravity");
  	  spinViewSimulationStepTime_.setItem(item, "viewsimulationTimeStep");
	  
	  if (item != null) { 
		  setTotalTime(item.getDbl("totalTime", 20.0));
		  setStepTime(item.getDbl("timeStep", 0.001));
		  setLogStepTime(item.getDbl("logTimeStep", 0.001));
		  setGravity(item.getDbl("gravity", 9.8));
		  setViewSimulationStepTime(item.getDbl("viewsimulationTimeStep", 0.033));
		  setMethod(item.getProperty("method",METHOD_NAMES[0]));
		  setIntegrate(item.isTrue("integrate", true));
		  setViewSimulate(item.isTrue("viewsimulate", false));
		  setEnabled(true);
	  }
  }
}

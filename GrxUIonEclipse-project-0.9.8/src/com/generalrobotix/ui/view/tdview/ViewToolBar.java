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
 * ViewToolBar.java
 *
 * @author  Kernel, Inc.
 * @version  2.0 (Thu Nov 29 2001)
 */
package com.generalrobotix.ui.view.tdview;

import java.awt.*;
import javax.swing.*;

import com.generalrobotix.ui.util.IconProperties;
import com.generalrobotix.ui.util.MessageBundle;


import java.awt.event.*;


/**
 * 視点操作モード切換えのためのツールバー
 *
 * @history  2.0 (Thu Nov 29 2001)
 */
public class ViewToolBar extends JToolBar implements ItemListener, BehaviorHandler {
    //--------------------------------------------------------------------
    // Constant Variables
    public static final int DISABLE_MODE  = 0;
    public static final int ROOM_MODE     = 1;
    public static final int WALK_MODE     = 2;
    public static final int PARALLEL_MODE = 3;

    public static final int COMBO_SELECT_ROOM   = 0;
    public static final int COMBO_SELECT_WALK   = 1;
    public static final int COMBO_SELECT_FRONT  = 2;
    public static final int COMBO_SELECT_BACK   = 3;
    public static final int COMBO_SELECT_LEFT   = 4;
    public static final int COMBO_SELECT_RIGHT  = 5;
    public static final int COMBO_SELECT_TOP    = 6;
    public static final int COMBO_SELECT_BOTTOM = 7;

    public static final int PAN    = 1;
    public static final int ZOOM   = 2;
    public static final int ROTATE = 3;

    //--------------------------------------------------------------------
    // Instance Variables
    protected int operation_;
    protected int mode_;

    protected JComboBox cmb_;
    protected JToggleButton pan_;
    protected JToggleButton zoom_;
    protected JToggleButton rotate_;
    protected JToggleButton wireFrame_;
    protected JToggleButton gamen_;
    protected JButton bgColor_;
    protected JButton capture_;
    protected JToggleButton selectedButton_;

    protected ButtonGroup group_;

    //--------------------------------------------------------------------
    // Constructor
    public ViewToolBar() {
        super(MessageBundle.get("tool.view.title"));
        Dimension size = new Dimension(IconProperties.WIDTH, IconProperties.HEIGHT);
        
        group_ = new ButtonGroup();

        cmb_ = new JComboBox();
        cmb_.setToolTipText(MessageBundle.get("toolTipText.type"));
        cmb_.setLightWeightPopupEnabled(false);
        cmb_.addItem(GUIAction.ROOM_VIEW);
        cmb_.addItem(GUIAction.WALK_VIEW);
        cmb_.addItem(GUIAction.FRONT_VIEW);
        cmb_.addItem(GUIAction.BACK_VIEW);
        cmb_.addItem(GUIAction.LEFT_VIEW);
        cmb_.addItem(GUIAction.RIGHT_VIEW);
        cmb_.addItem(GUIAction.TOP_VIEW);
        cmb_.addItem(GUIAction.BOTTOM_VIEW);

        cmb_.setMinimumSize(cmb_.getPreferredSize());
        cmb_.setMaximumSize(cmb_.getPreferredSize());

        cmb_.addItemListener(this);
        add(cmb_);
        
        addSeparator();

        rotate_ = new JToggleButton(GUIAction.VIEW_ROTATION_MODE);
        rotate_.setPreferredSize(size);
        rotate_.setMaximumSize(size);
        rotate_.setMinimumSize(size);
        rotate_.setSelectedIcon(IconProperties.get("icon.rotationModeOn"));

        add(rotate_);
        group_.add(rotate_);

        zoom_ = new JToggleButton(GUIAction.VIEW_ZOOM_MODE);
        zoom_.setPreferredSize(size);
        zoom_.setMaximumSize(size);
        zoom_.setMinimumSize(size);
        zoom_.setSelectedIcon(IconProperties.get("icon.zoomModeOn"));
        add(zoom_);
        group_.add(zoom_);

        pan_ = new JToggleButton(GUIAction.VIEW_PAN_MODE);
        pan_.setPreferredSize(size);
        pan_.setMaximumSize(size);
        pan_.setMinimumSize(size);
        pan_.setSelectedIcon(IconProperties.get("icon.panModeOn"));
        add(pan_);
        group_.add(pan_);

        addSeparator();

        wireFrame_ = new JToggleButton(GUIAction.WIRE_FRAME);
        wireFrame_.setPreferredSize(size);
        wireFrame_.setMaximumSize(size);
        wireFrame_.setMinimumSize(size);
        add(wireFrame_);

        gamen_ = new JToggleButton(GUIAction.SPLIT_MODE);
        gamen_.setPreferredSize(size);
        gamen_.setMaximumSize(size);
        gamen_.setMinimumSize(size);
       // add(gamen_);


        bgColor_ = new JButton(GUIAction.BG_COLOR);
        bgColor_.setPreferredSize(size);
        bgColor_.setMaximumSize(size);
        bgColor_.setMinimumSize(size);
        add(bgColor_);
        
        addSeparator();
        
        capture_ = new JButton(GUIAction.CAPTURE);
        capture_.setPreferredSize(size);
        capture_.setMaximumSize(size);
        capture_.setMinimumSize(size);
        add(capture_);

        setMode(mode_);
        
        setOperation(ROTATE);
    }

    //--------------------------------------------------------------------
    // Public Methods
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            mode_ = DISABLE_MODE;
        }

        cmb_.setEnabled(enabled);
        pan_.setEnabled(enabled);
        zoom_.setEnabled(enabled);
        rotate_.setEnabled(enabled);
        wireFrame_.setEnabled(enabled);
        gamen_.setEnabled(enabled);
    }

    public void setMode(int mode) {
        switch (mode) {
        case DISABLE_MODE:
            cmb_.setEnabled(false);
            pan_.setEnabled(false);
            zoom_.setEnabled(false);
            rotate_.setEnabled(false);
            wireFrame_.setEnabled(false);
            gamen_.setEnabled(false);
            break;
        case ROOM_MODE:
            cmb_.setEnabled(true);
            pan_.setEnabled(true);    
            zoom_.setEnabled(true);    
            rotate_.setEnabled(true);    
            wireFrame_.setEnabled(true);
            gamen_.setEnabled(true);
            break;
        case WALK_MODE:
            cmb_.setEnabled(true);
            pan_.setEnabled(true);    
            zoom_.setEnabled(true);    
            rotate_.setEnabled(true);    
            wireFrame_.setEnabled(true);
            gamen_.setEnabled(true);
            break;
        case PARALLEL_MODE:
            if (rotate_.isSelected()) {
                //pan_.setSelected(true);
                pan_.doClick();
            }
            cmb_.setEnabled(true);
            pan_.setEnabled(true);    
            zoom_.setEnabled(true);    
            rotate_.setEnabled(false);    
            wireFrame_.setEnabled(true);
            gamen_.setEnabled(true);
            break;
        default:
            return;
        }
        mode_ = mode;
    }

    public final int getMode() { return mode_; }

    public void setOperation(int operation) {
        switch (operation) {
        case PAN:
            pan_.setSelected(true);
            break;
        case ZOOM:
            zoom_.setSelected(true);
            break;
        case ROTATE:
            rotate_.setSelected(true);
            break;
        default:
            return;
        }
        operation_ = operation;
    }

    public final int getOperation() { return operation_; }

    public final void setComboSelection(int index) {
        cmb_.setSelectedIndex(index);
    }

    public void itemStateChanged(ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            GUIAction action = (GUIAction)evt.getItem();
            action.fireAction();
        }
    }

    public boolean isWireFrameSelected() {
        return wireFrame_.isSelected();
    }

    public boolean isGamenSelected() {
        return gamen_.isSelected();
    }
    
    public void setGamen(boolean flag){
        gamen_.setSelected(flag);
    }
    //--------------------------------------------------------------------
    // Implementation of BehaviorHandler
    public void processPicking(MouseEvent evt, BehaviorInfo info) {
        if (zoom_.isSelected()) {
            selectedButton_ = zoom_;
        } else if (pan_.isSelected()) {
            selectedButton_ = pan_;
        } else if (rotate_.isSelected()) {
            selectedButton_ = rotate_;
        }

        if (evt.isMetaDown()) {
            pan_.setSelected(true);
        } else if (evt.isAltDown()) {
            zoom_.setSelected(true);
        }
    }

    public void processStartDrag(MouseEvent evt, BehaviorInfo info) {}

    public void processDragOperation(MouseEvent evt, BehaviorInfo info) {}

    public void processReleased(MouseEvent evt, BehaviorInfo info) {
        selectedButton_.setSelected(true);
    }

    public void processTimerOperation(BehaviorInfo info) {}
}

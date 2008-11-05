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
 * ResizableCanvas.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.tdview;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

@SuppressWarnings("serial")
public class ResizableCanvas extends JScrollPane {
    Canvas canvas_;

    /**
     * コンストラクタ
     *
     * @param   canvas    キャンバス
     */
    public ResizableCanvas(Canvas canvas) {
        super(canvas, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER);
        canvas_ = canvas;
        setBorder(null);

        addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent evt) {
                    canvas_.setSize(
                        ResizableCanvas.this.getSize().width,
                        ResizableCanvas.this.getSize().height
                    );
                }
            }
        );
    }

    /**
     * キャンバス取得
     *
     * @return   Canvas    キャンバス
     */
    public Canvas getCanvas() {
        return canvas_;
    }
}

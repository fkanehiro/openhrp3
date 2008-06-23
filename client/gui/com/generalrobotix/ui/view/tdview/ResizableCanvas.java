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

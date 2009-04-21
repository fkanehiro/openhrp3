/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.view.graph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.generalrobotix.ui.util.ErrorDialog;
import com.generalrobotix.ui.util.MessageBundle;

/**
 * グラフエレメント
 *   グラフおよび凡例を載せるパネル
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class GraphElement
    extends JPanel
    implements MouseListener, ActionListener
{
    JSplitPane graphPane_;  // 分割ペイン
    JComponent graph_;      // グラフ
    JComponent legend_;     // 凡例
    TrendGraph tg_;         // トレンドグラフ

    ActionListener actionListener_; // アクションリスナ列
    private static final String CMD_CLICKED = "clicked";    // アクションコマンド名

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   tg      トレンドグラフ
     * @param   graph   グラフパネル
     * @param   legend  凡例パネル
     */
    public GraphElement(
        TrendGraph tg,
        JComponent graph,
        JComponent legend
    ) {
        super();

        // 参照保存
        tg_ = tg;   // トレンドグラフ
        graph_ = graph; // グラフ
        legend_ = legend;   // 凡例

        // スプリットペイン
        graphPane_ = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            true,
            graph,
            legend
        );
        graphPane_.setResizeWeight(0.8);
        graphPane_.setDividerSize(6);
        setLayout(new BorderLayout());
        add(graphPane_, BorderLayout.CENTER);

        // リスナ設定
        ((DroppableXYGraph)graph_).addActionListener(this); // ドロップアクションリスナ
        graph.addMouseListener(this);
        legend.addMouseListener(this);
        graphPane_.addMouseListener(this);
        addMouseListener(this);
    }

    // -----------------------------------------------------------------
    // メソッド
    /**
     * トレンドグラフ取得
     *
     * @return  トレンドグラフ
     */
    public TrendGraph getTrendGraph() {
        return tg_;
    }

    /**
     * グラフパネル取得
     *
     * @return  グラフパネル
     */
    public JComponent getGraph() {
        return graph_;
    }

    /**
     * 凡例パネル取得
     *
     * @return  凡例パネル
     */
    public JComponent getLegend() {
        return legend_;
    }

    // -----------------------------------------------------------------
    // ActionListener登録および削除
    public void addActionListener(ActionListener listener) {
        actionListener_ = AWTEventMulticaster.add(actionListener_, listener);
    }
    public void removeActionListener(ActionListener listener) {
        actionListener_ = AWTEventMulticaster.remove(actionListener_, listener);
    }

    // -----------------------------------------------------------------
    // MouseListenerの実装
    public void mousePressed(MouseEvent evt) {
        //System.out.println("Clicked");
        raiseActionEvent();
    }
    public void mouseClicked(MouseEvent evt){}
    public void mouseEntered(MouseEvent evt){}
    public void mouseExited(MouseEvent evt) {}
    public void mouseReleased(MouseEvent evt){}

    // -----------------------------------------------------------------
    // ActionListenerの実装
    public void actionPerformed(ActionEvent evt) {
    	/*
        int result = tg_.addDataItem(
            (AttributeInfo)(((DroppableXYGraph)graph_).getDroppedObject())
        );
        if (result == TrendGraph.SUCCEEDED) {   // 成功?
            ((DroppableXYGraph)graph_).setDropSucceeded(true);
            raiseActionEvent();
            repaint();
        } else if (result == TrendGraph.NOT_MATCHED) {  // データ種別不一致?
            ((DroppableXYGraph)graph_).setDropSucceeded(false);
            new ErrorDialog(
                (Frame)null,
                MessageBundle.get("dialog.graph.mismatch.title"),
                MessageBundle.get("dialog.graph.mismatch.message")
            ).showModalDialog();
        } else {    // データ種別非サポート
            ((DroppableXYGraph)graph_).setDropSucceeded(false);
            new ErrorDialog(
                (Frame)null,
                MessageBundle.get("dialog.graph.unsupported.title"),
                MessageBundle.get("dialog.graph.unsupported.message")
            ).showModalDialog();
        }
        */
    }

    /**
     * アクションイベント発生(クリック時)
     *
     * @return  凡例パネル
     */
    private void raiseActionEvent() {
        if(actionListener_ != null) {
            actionListener_.actionPerformed(
                new ActionEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    CMD_CLICKED
                )
            );
        }
    }
}

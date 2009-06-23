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

//import java.awt.*;
//import java.awt.event.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.generalrobotix.ui.view.graph.LegendPanel;

/**
 * グラフエレメント
 *   グラフおよび凡例を載せるパネル
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class GraphElement extends Composite implements MouseListener,PaintListener
 //   implements MouseListener, ActionListener
{
	SashForm graphPane_;  // 分割ペイン
	DroppableXYGraph graph_;      // グラフ
	LegendPanel legend_;     // 凡例
    TrendGraph tg_;         // トレンドグラフ
    GraphPanel gp_;

    SelectionListener actionListener_; // アクションリスナ列
    private static final String CMD_CLICKED = "clicked";    // アクションコマンド名

    private static final int GRAPH_LEFT_MARGIN = 50;
    private static final int GRAPH_RIGHT_MARGIN = 50;
    private static final int GRAPH_TOP_MARGIN = 20;
    private static final int GRAPH_BOTTOM_MARGIN = 30;

    private static final Color normalColor_ = new Color(Display.getDefault(),0, 0, 0);
    private static final Color focusedColor_ = new Color(Display.getDefault(),0, 0, 100);
    private static final Font GRAPH_LEGEND_FONT = new Font(Display.getDefault(),"dialog", 12, SWT.NORMAL );
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
    	GraphPanel gp,
    	Composite parent,
        TrendGraph tg,
        int height
    ) {
        super(parent, SWT.NONE);

        // 参照保存
        tg_ = tg;   // トレンドグラフ
        gp_ = gp;
        
        GridData gridData0 = new GridData(0,height);
 		gridData0.horizontalAlignment = GridData.FILL;
 		gridData0.grabExcessHorizontalSpace = true;
 		setLayoutData(gridData0);
        
 		setLayout(new GridLayout(1,true));
        
        // スプリットペイン
        graphPane_ = new SashForm( this, SWT.HORIZONTAL  );
        GridData gridData = new GridData();
 		gridData.horizontalAlignment = GridData.FILL;
 		gridData.grabExcessHorizontalSpace = true;
 		gridData.verticalAlignment = GridData.FILL;
 		gridData.grabExcessVerticalSpace = true;
 		graphPane_.setLayoutData(gridData);
 		
        graph_ = new DroppableXYGraph(
        		graphPane_, 
                GRAPH_LEFT_MARGIN,
                GRAPH_RIGHT_MARGIN,
                GRAPH_TOP_MARGIN,
                GRAPH_BOTTOM_MARGIN
            );
        legend_ = new LegendPanel(
        		graphPane_,
                new Font(parent.getDisplay(),"dialog",10,SWT.NORMAL),
                parent.getDisplay().getSystemColor(SWT.COLOR_BLACK),
            	parent.getDisplay().getSystemColor(SWT.COLOR_WHITE)
            );
        graphPane_.setWeights(new int[] { 4,1});
        graphPane_.SASH_WIDTH = 6;
        
        ((XYLineGraph)graph_).setBorderColor(normalColor_);
        legend_.setBackColor(normalColor_);
        legend_.setFont(GRAPH_LEGEND_FONT);

        tg.setGraph((XYLineGraph) graph_, legend_);

        addPaintListener(this);
        // リスナ設定
       // ((DroppableXYGraph)graph_).addActionListener(this); // ドロップアクションリスナ
        graph_.addMouseListener(this);
        legend_.addMouseListener(this);
       // graphPane_.addMouseListener(this);
       // addMouseListener(this);
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
    public DroppableXYGraph getGraph() {
        return graph_;
    }

    /**
     * 凡例パネル取得
     *
     * @return  凡例パネル
     */
    public LegendPanel getLegend() {
        return legend_;
    }
/*
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
        
    }
*/
    /**
     * アクションイベント発生(クリック時)
     *
     * @return  凡例パネル
     */
    /*
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
*/
	public void setBorderColor(Color color) {
		graph_.setBorderColor(color);
		legend_.setBackColor(color);
		
	}

	public void mouseDoubleClick(MouseEvent e) {
		// TODO 自動生成されたメソッド・スタブ
		
	}
	
	public void mouseDown(MouseEvent e) {
		// TODO 自動生成されたメソッド・スタブ
		
	}
	
	public void mouseUp(MouseEvent e) {
		gp_.setFocuse(this);
	}

	public void paintControl(PaintEvent e) {
		graph_.redraw();
		legend_.redraw();
	}
	
}

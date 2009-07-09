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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import java.util.*;
import java.text.*;

/**
 * 折れ線グラフクラス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class XYLineGraph extends Canvas implements PaintListener {

    // -----------------------------------------------------------------
    // 定数
    // 軸定数
    public static final int AXIS_LEFT   = 0;    // 左軸
    public static final int AXIS_RIGHT  = 1;    // 右軸
    public static final int AXIS_TOP    = 2;    // 上軸
    public static final int AXIS_BOTTOM = 3;    // 下軸
    // グラフの最小サイズ
    private static final int MIN_HEIGHT = 30;   // グラフの最小高
    private static final int MIN_WIDTH = 30;    // グラフの最小幅
    // ティックとラベルの隙間
    private static final int LABEL_GAP_LEFT = 5;
    private static final int LABEL_GAP_RIGHT = 4;
    private static final int LABEL_GAP_TOP = 3;
    private static final int LABEL_GAP_BOTTOM = -5;
    // 描画フラグ
    private static final int DRAW_AXIS = 1;     // 軸描画
    private static final int DRAW_TICK = 2;     // ティック描画
    private static final int DRAW_LABEL = 4;    // ラベル描画
    private static final int DRAW_GRID = 8;     // グリッド描画
    private static final int DRAW_MARKER = 16;  // マーカ描画

    private static final int EPS_SCALE = 20;            // EPS時スケール
    //private static final double EPS_LINE_WIDTH = 0.3;   // EPS線幅

    // -----------------------------------------------------------------
    // インスタンス変数
    // マージン
    private int leftMargin_;    // 左マージン
    private int rightMargin_;   // 右マージン
    private int topMargin_;     // 上マージン
    private int bottomMargin_;  // 下マージン
    // 軸
    private AxisInfo[] axisInfo_;   // 軸情報
    // データ系列
    private ArrayList<DataSeries> dsList_;  // データ系列リスト
    private HashMap<DataSeries, DataSeriesInfo>   dsInfoMap_;   // データ系列情報マップ
    // 凡例
    private LegendPanel legendPanel_;    // 凡例パネル
    // 色
    private Color backColor_;       // 背景色
    private Color borderColor_;     // 周辺色
//    private Color nullAxisColor_;   // 軸が無い場合の色

    private boolean epsMode_;
        
    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   leftMargin      int 左マージン
     * @param   rightMargin     int 右マージン
     * @param   topMargin       int 上マージン
     * @param   bottomMargin    int 下マージン
     */
    public XYLineGraph(
    	Composite parent,
        int leftMargin,     // 左マージン
        int rightMargin,    // 右マージン
        int topMargin,      // 上マージン
        int bottomMargin    // 下マージン
    ) {
    	super(parent,SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
        // マージン設定
        leftMargin_   = leftMargin;
        rightMargin_  = rightMargin;
        topMargin_    = topMargin;
        bottomMargin_ = bottomMargin;

        // 凡例パネル生成
        

        // デフォルト色設定
        backColor_ = parent.getDisplay().getSystemColor(SWT.COLOR_BLACK);
        borderColor_ = parent.getDisplay().getSystemColor(SWT.COLOR_BLACK);
//        nullAxisColor_ = Color.darkGray;

        // 軸情報クリア
        axisInfo_ = new AxisInfo[4];
        axisInfo_[AXIS_LEFT]   = null;
        axisInfo_[AXIS_RIGHT]  = null;
        axisInfo_[AXIS_TOP]    = null;
        axisInfo_[AXIS_BOTTOM] = null;

        // データ系列管理データ初期化
        dsList_ = new ArrayList<DataSeries>();  // データ系列リスト
        dsInfoMap_ = new HashMap<DataSeries, DataSeriesInfo>(); // データ系列情報マップ

        // EPSモード
        epsMode_ = false;
        
        addPaintListener(this);
    }

    // -----------------------------------------------------------------
    // メソッド
    /**
     * EPSモード設定
     *
     * @param   flag    フラグ
     */
    public void setEPSMode(
        boolean flag
    ) {
        epsMode_ = flag;
    }

    /**
     * 背景色の設定
     *
     * @param   color   Color   色
     */
    public void setBackColor(
        Color color
    ) {
        backColor_ = color;
    }

    /**
     * 周辺色の設定
     *
     * @param   color   Color   色
     */
    public void setBorderColor(
        Color color
    ) {
        borderColor_ = color;
    }

    /**
     * データ系列の追加
     *
     * @param   ds      DataSeries  データ系列
     * @param   xai     AxisInfo    X軸情報
     * @param   yai     AxisInfo    Y軸情報
     * @param   color   Color       色
     * @param   legend  String      凡例文字列
     */
    public void addDataSeries(
        DataSeries ds,
        AxisInfo xai,
        AxisInfo yai,
        Color color, 
        String legend
    ) {
        DataSeriesInfo dsi = new DataSeriesInfo(
            xai, yai, color,
            new LegendInfo(color, legend)
        );
        legendPanel_.addLegend(dsi.legend);
        dsList_.add(ds);
        dsInfoMap_.put(ds, dsi);
    }

    /**
     * データ系列の削除
     *
     * @param   ds      DataSeries  データ系列
     */
    public void removeDataSeries(
        DataSeries ds
    ) {
        int ind = dsList_.indexOf(ds);
        dsList_.remove(ind);
        DataSeriesInfo dsi = (DataSeriesInfo)dsInfoMap_.get(ds);
        legendPanel_.removeLegend(dsi.legend);
        dsInfoMap_.remove(ds);
    }

    /**
     * 全データ系列の取得
     *
     * @return  Iterator    全データ系列
     */
    public Iterator getDataSeries() {
        return (Iterator)dsList_.listIterator();
    }

    /**
     * 軸情報の設定
     *      axisにはAXIS_LEFT,AXIS_RIGHT,AXIS_TOP,AXIS_BOTTOMを指定する
     *      aiにnullを指定した場合はその軸は非表示
     *
     * @param   axis    int         軸指定
     * @param   ai      AxisInfo    軸情報
     */
    public void setAxisInfo(
        int axis,
        AxisInfo ai
    ) {
        axisInfo_[axis] = ai;
    }

    /**
     * 軸情報の取得
     *      axisにはAXIS_LEFT,AXIS_RIGHT,AXIS_TOP,AXIS_BOTTOMを指定する
     *
     * @param   axis    int         軸指定
     * @return  AxisInfo    軸情報
     */
    public AxisInfo getAxisInfo(
        int axis
    ) {
        return axisInfo_[axis];
    }

    /**
     * データ系列の色の設定
     *
     * @param   ds      DataSeries  データ系列
     * @param   color   Color       色
     */
    public void setStyle(DataSeries ds, Color color) {
        DataSeriesInfo dsi = (DataSeriesInfo)dsInfoMap_.get(ds);
        dsi.color = color;
        dsi.legend.color = color;
    }

    /**
     * データ系列の色の取得
     *
     * @param   ds  データ系列
     * @return  色
     */
    public Color getStyle(DataSeries ds) {
        DataSeriesInfo dsi = (DataSeriesInfo)dsInfoMap_.get(ds);
        return dsi.color;
    }

    /**
     * データ系列の凡例文字列の設定
     *
     * @param   ds      DataSeries  データ系列
     * @param   legend  String      凡例文字列
     */
    public void setLegendLabel(DataSeries ds, String legend) {
        DataSeriesInfo dsi = (DataSeriesInfo)dsInfoMap_.get(ds);
        dsi.legend.label = legend;
    }

    /**
     * データ系列の凡例文字列の取得
     *
     * @param   ds  データ系列
     * @return  凡例文字列
     */
    public String getLegendLabel(DataSeries ds) {
        DataSeriesInfo dsi = (DataSeriesInfo)dsInfoMap_.get(ds);
        return dsi.legend.label;
    }

    /**
     * 凡例パネルの取得
     *
     * @return  JPanel  凡例パネル
     */
    public Canvas getLegendPanel() {
        return null;//legendPanel_;
    }

    /**
     * 描画
     *
     * @param   g   Graphics    グラフィックス
     */
    public void paintControl(PaintEvent e) {
           	
        // サイズの決定
        int width = getSize().x;
        int height = getSize().y;
        e.gc.setBackground(backColor_);
        e.gc.fillRectangle(0, 0, width, height);
        int minWidth = leftMargin_ + MIN_WIDTH + rightMargin_;
        int minHeight = topMargin_ + MIN_HEIGHT + bottomMargin_;
        if (width < minWidth) {
            width = minWidth;
        }
        if (height < minHeight) {
            height = minHeight;
        }

        // 角座標の決定
        int xl = leftMargin_;
        int xr = width - rightMargin_ - 1;
        int yt = topMargin_;
        int yb = height - bottomMargin_ - 1;

        // スケールを変更する
        //TODO  hattori
        /*
        if (epsMode_) {
            EPSGraphics eg = (EPSGraphics)g;
            eg.setScale(EPS_SCALE);
            //eg.setLineWidth(EPS_LINE_WIDTH);
            width *= EPS_SCALE;
            height *= EPS_SCALE;
            xl *= EPS_SCALE;
            xr *= EPS_SCALE;
            yt *= EPS_SCALE;
            yb *= EPS_SCALE;
        }
	*/
        // グリッドの描画
        int flag = DRAW_GRID;
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_LEFT,   flag);
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_RIGHT,  flag);
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_TOP,    flag);
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_BOTTOM, flag);

        // クリッピング(EPS時のみ)
        if (epsMode_) {
        	e.gc.setClipping(xl, yt, xr - xl, yb - yt);
        }



        // データ系列の描画
        ListIterator li = dsList_.listIterator();
        while (li.hasNext()) {
            DataSeries ds = (DataSeries)li.next();

            DataSeriesInfo dsi = (DataSeriesInfo)dsInfoMap_.get(ds);

            double xbase = dsi.xAxisInfo.base;
            double ybase = dsi.yAxisInfo.base;
            double xscale = (xr - xl) / dsi.xAxisInfo.extent;
            double yscale = (yb - yt) / dsi.yAxisInfo.extent;
            double factor = dsi.yAxisInfo.factor;

            double xOffset = ds.getXOffset();
            double xStep = ds.getXStep();
            double[] data = ds.getData();
            int headPos = ds.getHeadPos();
            int length = data.length;
            int ox = 0, oy = 0;
            int nx = 0, ny = 0;
            boolean connect = false;
            e.gc.setForeground(dsi.color);
            int iofs = - headPos;
            //System.out.println("headPos=" + headPos);
            //System.out.println("headPos=" + headPos + " length=" + length);
            for (int i = headPos; i < length; i++) {
                if (Double.isNaN(data[i])) {    // データなし?
                    //if (i == headPos) {
                    //    System.out.println("data[i]=NaN");
                    //}
                    if (connect) {
                        //System.out.println("1 ox=" + ox + " oy=" + oy);
                        e.gc.drawLine(ox, oy, ox, oy);
                        connect = false;
                    }
                } else {    // データあり?
                    nx = xl + (int)(((xStep * (i + iofs) + xOffset) - xbase) * xscale);
                    //if (i == headPos) {
                    //    System.out.println("iofs=" + iofs);
                    //    System.out.println("xOffset" + xOffset);
                    //    System.out.println("xbase" + xbase);
                    //    System.out.println("xscale" + xscale);
                    //}
                    ny = yb - (int)((data[i] * factor - ybase) * yscale);
                    if (connect) {
                        //System.out.println("2 ox=" + ox + " oy=" + oy + " nx=" + nx + " ny=" + ny);
                        e.gc.drawLine(ox, oy, nx, ny);
                        ox = nx;
                        oy = ny;
                    } else {
                        ox = nx;
                        oy = ny;
                        connect = true;
                    }
                }
            }
            iofs = length - headPos;
            for (int i = 0; i < headPos; i++) {
                if (Double.isNaN(data[i])) {    // データなし?
                    if (connect) {
                        //System.out.println("3 ox=" + ox + " oy=" + oy);
                        e.gc.drawLine(ox, oy, ox, oy);
                        connect = false;
                    }
                } else {    // データあり?
                    nx = xl + (int)(((xStep * (i + iofs) + xOffset) - xbase) * xscale);
                    ny = yb - (int)((data[i] * factor - ybase) * yscale);
                    if (connect) {
                        //System.out.println("4 ox=" + ox + " oy=" + oy + " nx=" + nx + " ny=" + ny);
                        e.gc.drawLine(ox, oy, nx, ny);
                        ox = nx;
                        oy = ny;
                    } else {
                        ox = nx;
                        oy = ny;
                        connect = true;
                    }
                }
            }


        }


        if (epsMode_) {
            // クリッピング解除
        	//TODO hattori
            //e.gc.setClipping(null);
        } else {
            // マスク処理
            e.gc.setBackground(borderColor_);
            e.gc.fillRectangle(0,      0,      xl,     height);
            e.gc.fillRectangle(xr + 1, 0,      width,  height);
            e.gc.fillRectangle(0,      0,      width,  yt);
            e.gc.fillRectangle(0,      yb + 1, width,  height);
        }

        // 各軸の描画
        flag = DRAW_AXIS + DRAW_TICK + DRAW_LABEL + DRAW_MARKER;
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_LEFT,   flag);
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_RIGHT,  flag);
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_TOP,    flag);
        drawAxis(e.gc, xl, yt, xr, yb, /*width, height,*/ AXIS_BOTTOM, flag);

        /* ★実験 (半透明のテスト)
        width = getSize().width;
        height = getSize().height;
        g.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
        g.fillRect(0, 0, width, height);
        */

        // スケールをリセットする
        if (epsMode_) {
            //TODO hattori
        	//EPSGraphics eg = (EPSGraphics)g;
            //eg.setScale(1);
        }


    }

    /**
     * 軸の描画
     *
     * @param   g       Graphics    グラフィックス
     * @param   width   int         幅
     * @param   height  int         高さ
     * @param   axis    int         軸種別
     * @param   flag    int         描画フラグ
     */
    private void drawAxis(
        GC gc,
        //int width,
        //int height,
        int xl,
        int yt,
        int xr,
        int yb,
        int axis,
        int flag
    ) {
        // 四隅の座標の決定
        //int xl = leftMargin_;
        //int xr = width - rightMargin_ - 1;
        //int yt = topMargin_;
        //int yb = height - bottomMargin_ - 1;

        // 軸の描画
        AxisInfo ai = axisInfo_[axis];  // 軸情報取得
        if (ai != null) {   // 軸情報あり?
            int tickLength = ai.tickLength;
            int unitXOfs = ai.unitXOfs;
            int unitYOfs = ai.unitYOfs;
            int ascale = 1;
            if (epsMode_) {
                tickLength *= EPS_SCALE;
                unitXOfs *= EPS_SCALE;
                unitYOfs *= EPS_SCALE;
                ascale = EPS_SCALE;
            }
            // 軸
            if ((flag & DRAW_AXIS) != 0) {
                gc.setForeground(ai.color);
                switch (axis) {
                    case AXIS_LEFT:
                        gc.drawLine(xl, yt, xl, yb);
                        break;
                    case AXIS_RIGHT:
                        gc.drawLine(xr, yt, xr, yb);
                        break;
                    case AXIS_TOP:
                        gc.drawLine(xl, yt, xr, yt);
                        break;
                    case AXIS_BOTTOM:
                        gc.drawLine(xl, yb, xr, yb);
                        break;
                }
            }
            double base = ai.base;
            double extent = ai.extent;
            double scale = 1;
            switch (axis) {
                case AXIS_LEFT:
                case AXIS_RIGHT:
                    scale = (yb - yt) / extent;
                    break;
                case AXIS_TOP:
                case AXIS_BOTTOM:
                    scale = (xr - xl) / extent;
                    break;
            }
            double min = base;
            double max = base + extent;
            if (ai.minLimitEnabled && min < ai.min) {
                min = ai.min;
            }
            if (ai.maxLimitEnabled && max > ai.max) {
                max = ai.max;
            }
            // ティック
            double every = ai.tickEvery;
            if (every > 0.0 && ((flag & DRAW_TICK) != 0)) {
                gc.setForeground(ai.color);
                int cfrom = (int)(Math.ceil(min / every));
                int cto = (int)(Math.floor(max / every));
                int pos;
                switch (axis) {
                    case AXIS_LEFT:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = yb - (int)((every * i - base) * scale);
                            gc.drawLine(xl, pos, xl - tickLength, pos);
                        }
                        break;
                    case AXIS_RIGHT:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = yb - (int)((every * i - base) * scale);
                            gc.drawLine(xr, pos, xr + tickLength, pos);
                        }
                        break;
                    case AXIS_TOP:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = xl + (int)((every * i - base) * scale);
                            gc.drawLine(pos, yt, pos, yt - tickLength);
                        }
                        break;
                    case AXIS_BOTTOM:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = xl + (int)((every * i - base) * scale);
                            gc.drawLine(pos, yb, pos, yb + tickLength);
                        }
                        break;
                }
            }
            // グリッド
            every = ai.gridEvery;
            if (every > 0.0 && ((flag & DRAW_GRID) != 0)) {
                gc.setForeground(ai.gridColor);
                int cfrom = (int)(Math.ceil(min / every));
                int cto = (int)(Math.floor(max / every));
                int pos;
                switch (axis) {
                    case AXIS_LEFT:
                    case AXIS_RIGHT:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = yb - (int)((every * i - base) * scale);
                            gc.drawLine(xl + 1, pos, xr - 1, pos);
                        }
                        break;
                    case AXIS_TOP:
                    case AXIS_BOTTOM:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = xl + (int)((every * i - base) * scale);
                            gc.drawLine(pos, yt + 1, pos, yb - 1);
                        }
                        break;
                }
            }
            // ラベル
            every = ai.labelEvery;
            if (every > 0.0 && ((flag & DRAW_LABEL) != 0)) {
                DecimalFormat lformat = new DecimalFormat(ai.labelFormat);
                gc.setForeground(ai.labelColor);
                gc.setFont(ai.labelFont);
                FontMetrics lmetrics = gc.getFontMetrics();
                int cfrom = (int)(Math.ceil(min / every));
                int cto = (int)(Math.floor(max / every));
                int xpos, ypos;
                String lstr;
                switch (axis) {
                    case AXIS_LEFT:
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xl - tickLength - ascale * LABEL_GAP_LEFT
                                - ascale * lmetrics.getAverageCharWidth();
                            ypos = yb - (int)((every * i - base) * scale)
                                + (int)(ascale * lmetrics.getHeight() / 3.5);
                            gc.drawString(lstr, xpos, ypos);
                        }
                        break;
                    case AXIS_RIGHT:
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xr + tickLength + ascale * LABEL_GAP_RIGHT;
                            ypos = yb - (int)((every * i - base) * scale)
                                + (int)(ascale * lmetrics.getHeight() / 3.5);
                            gc.drawString(lstr, xpos, ypos);
                        }
                        break;
                    case AXIS_TOP:
                        ypos = yt - tickLength - ascale * LABEL_GAP_TOP;
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xl + (int)((every * i - base) * scale)
                                - ascale * lmetrics.getAverageCharWidth() / 2;
                            gc.drawString(lstr, xpos, ypos);
                        }
                        break;
                    case AXIS_BOTTOM:
                        ypos = yb + tickLength
                            + ascale * LABEL_GAP_BOTTOM
                            + ascale * lmetrics.getHeight();
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xl + (int)((every * i - base) * scale)
                                - ascale * lmetrics.getAverageCharWidth() / 2;
                            gc.drawString(lstr, xpos, ypos);
                        }
                        break;
                }
                // 単位
                //gc.setFont(ai.unitFont);
                FontMetrics umetrics = gc.getFontMetrics();
                int ux, uy;
                gc.setForeground(ai.unitColor);

                switch (axis) {
                    case AXIS_LEFT:
                        ux = xl - unitXOfs - ascale * umetrics.getAverageCharWidth();
                        uy = yt - unitYOfs;
                        gc.drawString(ai.unitLabel, ux, uy);
                        break;
                    case AXIS_RIGHT:
                        ux = xr + unitXOfs;
                        uy = yt - unitYOfs;
                        gc.drawString(ai.unitLabel, ux, uy);
                        break;
                    case AXIS_TOP:
                        ux = xr + unitXOfs;
                        uy = yt - unitYOfs;
                        gc.drawString(ai.unitLabel, ux, uy);
                        break;
                    case AXIS_BOTTOM:
                        ux = xr + unitXOfs;
                        uy = yb + unitYOfs + ascale * umetrics.getHeight();
                        gc.drawString(ai.unitLabel, ux, uy);
                        break;
                }
            }
            // マーカ
            if (ai.markerVisible
                //&& ai.markerPos >= min && ai.markerPos <= max
                && ((flag & DRAW_MARKER) != 0)) {
                //gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
                gc.setForeground(ai.markerColor);
                int pos;
                switch (axis) {
                    case AXIS_LEFT:
                    case AXIS_RIGHT:
                        //pos = yb - (int)((ai.markerPos - base) * scale);
                        //pos = yb - (int)(ai.markerPos * scale);
                        pos = yb - (int)(ai.markerPos * (yb - yt));
                        gc.drawLine(xl + 1, pos - 1, xr - 1, pos - 1);
                        gc.drawLine(xl + 1, pos,     xr - 1, pos);
                        gc.drawLine(xl + 1, pos + 1, xr - 1, pos + 1);
                        break;
                    case AXIS_TOP:
                    case AXIS_BOTTOM:
                        //pos = xl + (int)((ai.markerPos - base) * scale);
                        //pos = xl + (int)(ai.markerPos * scale);
                        pos = xl + (int)(ai.markerPos * (xr - xl));
                        gc.drawLine(pos - 1, yt + 1, pos - 1, yb - 1);
                        gc.drawLine(pos,     yt + 1, pos,     yb - 1);
                        gc.drawLine(pos + 1, yt + 1, pos + 1, yb - 1);
                        break;
                }
                //gc.setPaintMode();
            }
        } /* else {    // 軸情報なし?
            if ((flag & DRAW_AXIS) != 0) {
                g.setColor(nullAxisColor_);
                switch (axis) {
                    case AXIS_LEFT:
                        g.drawLine(xl, yt + 1, xl, yb - 1);
                        break;
                    case AXIS_RIGHT:
                        g.drawLine(xr, yt + 1, xr, yb - 1);
                        break;
                    case AXIS_TOP:
                        g.drawLine(xl + 1, yt, xr - 1, yt);
                        break;
                    case AXIS_BOTTOM:
                        g.drawLine(xl + 1, yb, xr - 1, yb);
                        break;
                }
            }
        } */
    }

    // -----------------------------------------------------------------
    // 内部クラス
    /**
     * データ系列情報クラス
     *
     */
    private class DataSeriesInfo {

        // -----------------------------------------------------------------
        // インスタンス変数
        public AxisInfo   xAxisInfo;    // X軸情報
        public AxisInfo   yAxisInfo;    // Y軸情報
        public Color      color;        // 描画色
        public LegendInfo legend;       // 凡例情報

        // -----------------------------------------------------------------
        // コンストラクタ
        /**
         * コンストラクタ
         *
         * @param   xAxisInfo   AxisInfo    X軸情報
         * @param   yAxisInfo   AxisInfo    Y軸情報
         * @param   color       Color       描画色
         * @param   legend      LegendInfo  凡例情報
         */
        public DataSeriesInfo(
            AxisInfo xAxisInfo,
            AxisInfo yAxisInfo,
            Color    color,
            LegendInfo   legend
        ) {
            this.xAxisInfo = xAxisInfo;
            this.yAxisInfo = yAxisInfo;
            this.color     = color;
            this.legend    = legend;
        }
    }

	public void setLegend(LegendPanel legend) {
		legendPanel_ = legend;
	}
 
}

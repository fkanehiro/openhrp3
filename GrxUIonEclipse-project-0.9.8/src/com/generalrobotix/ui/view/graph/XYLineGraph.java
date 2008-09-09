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

import javax.swing.*;
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.*;

/**
 * 折れ線グラフクラス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class XYLineGraph extends JPanel {

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
    private static final int LABEL_GAP_BOTTOM = 0;
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
        int leftMargin,     // 左マージン
        int rightMargin,    // 右マージン
        int topMargin,      // 上マージン
        int bottomMargin    // 下マージン
    ) {
        // マージン設定
        leftMargin_   = leftMargin;
        rightMargin_  = rightMargin;
        topMargin_    = topMargin;
        bottomMargin_ = bottomMargin;

        // 凡例パネル生成
        legendPanel_ = new LegendPanel(
            new Font("dialog", Font.PLAIN, 10),
            Color.black,
            Color.white
        );

        // デフォルト色設定
        backColor_ = Color.black;
        borderColor_ = Color.black;
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
     * 凡例フォントの設定
     *
     * @param   font    Font    フォント
     */
    public void setLegendFont(
        Font font
    ) {
        legendPanel_.setFont(font);
    }

    /**
     * 凡例ラベル色の設定
     *
     * @param   color   Color   色
     */
    public void setLegendLabelColor(
        Color color
    ) {
        legendPanel_.setLabelColor(color);
    }

    /**
     * 凡例背景色の設定
     *
     * @param   color   Color   色
     */
    public void setLegendBackColor(
        Color color
    ) {
        legendPanel_.setBackColor(color);
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
    public JPanel getLegendPanel() {
        return legendPanel_;
    }

    /**
     * 描画
     *
     * @param   g   Graphics    グラフィックス
     */
    public void paint(
        Graphics g
    ) {
        //super.paint(g);

        // サイズの決定
        int width = getSize().width;
        int height = getSize().height;
        g.setColor(backColor_);
        g.fillRect(0, 0, width, height);
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

        // グリッドの描画
        int flag = DRAW_GRID;
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_LEFT,   flag);
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_RIGHT,  flag);
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_TOP,    flag);
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_BOTTOM, flag);

        // クリッピング(EPS時のみ)
        if (epsMode_) {
            g.setClip(xl, yt, xr - xl, yb - yt);
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
            g.setColor(dsi.color);
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
                        g.drawLine(ox, oy, ox, oy);
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
                        g.drawLine(ox, oy, nx, ny);
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
                        g.drawLine(ox, oy, ox, oy);
                        connect = false;
                    }
                } else {    // データあり?
                    nx = xl + (int)(((xStep * (i + iofs) + xOffset) - xbase) * xscale);
                    ny = yb - (int)((data[i] * factor - ybase) * yscale);
                    if (connect) {
                        //System.out.println("4 ox=" + ox + " oy=" + oy + " nx=" + nx + " ny=" + ny);
                        g.drawLine(ox, oy, nx, ny);
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
            g.setClip(null);
        } else {
            // マスク処理
            g.setColor(borderColor_);
            g.fillRect(0,      0,      xl,     height);
            g.fillRect(xr + 1, 0,      width,  height);
            g.fillRect(0,      0,      width,  yt);
            g.fillRect(0,      yb + 1, width,  height);
        }

        // 各軸の描画
        flag = DRAW_AXIS + DRAW_TICK + DRAW_LABEL + DRAW_MARKER;
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_LEFT,   flag);
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_RIGHT,  flag);
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_TOP,    flag);
        drawAxis(g, xl, yt, xr, yb, /*width, height,*/ AXIS_BOTTOM, flag);

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
            EPSGraphics eg = (EPSGraphics)g;
            eg.setScale(1);
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
        Graphics g,
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
                g.setColor(ai.color);
                switch (axis) {
                    case AXIS_LEFT:
                        g.drawLine(xl, yt, xl, yb);
                        break;
                    case AXIS_RIGHT:
                        g.drawLine(xr, yt, xr, yb);
                        break;
                    case AXIS_TOP:
                        g.drawLine(xl, yt, xr, yt);
                        break;
                    case AXIS_BOTTOM:
                        g.drawLine(xl, yb, xr, yb);
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
                g.setColor(ai.color);
                int cfrom = (int)(Math.ceil(min / every));
                int cto = (int)(Math.floor(max / every));
                int pos;
                switch (axis) {
                    case AXIS_LEFT:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = yb - (int)((every * i - base) * scale);
                            g.drawLine(xl, pos, xl - tickLength, pos);
                        }
                        break;
                    case AXIS_RIGHT:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = yb - (int)((every * i - base) * scale);
                            g.drawLine(xr, pos, xr + tickLength, pos);
                        }
                        break;
                    case AXIS_TOP:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = xl + (int)((every * i - base) * scale);
                            g.drawLine(pos, yt, pos, yt - tickLength);
                        }
                        break;
                    case AXIS_BOTTOM:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = xl + (int)((every * i - base) * scale);
                            g.drawLine(pos, yb, pos, yb + tickLength);
                        }
                        break;
                }
            }
            // グリッド
            every = ai.gridEvery;
            if (every > 0.0 && ((flag & DRAW_GRID) != 0)) {
                g.setColor(ai.gridColor);
                int cfrom = (int)(Math.ceil(min / every));
                int cto = (int)(Math.floor(max / every));
                int pos;
                switch (axis) {
                    case AXIS_LEFT:
                    case AXIS_RIGHT:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = yb - (int)((every * i - base) * scale);
                            g.drawLine(xl + 1, pos, xr - 1, pos);
                        }
                        break;
                    case AXIS_TOP:
                    case AXIS_BOTTOM:
                        for (int i = cfrom; i <= cto; i ++) {
                            pos = xl + (int)((every * i - base) * scale);
                            g.drawLine(pos, yt + 1, pos, yb - 1);
                        }
                        break;
                }
            }
            // ラベル
            every = ai.labelEvery;
            if (every > 0.0 && ((flag & DRAW_LABEL) != 0)) {
                DecimalFormat lformat = new DecimalFormat(ai.labelFormat);
                FontMetrics lmetrics = getFontMetrics(ai.labelFont);
                g.setColor(ai.labelColor);
                g.setFont(ai.labelFont);
                int cfrom = (int)(Math.ceil(min / every));
                int cto = (int)(Math.floor(max / every));
                int xpos, ypos;
                String lstr;
                switch (axis) {
                    case AXIS_LEFT:
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xl - tickLength - ascale * LABEL_GAP_LEFT
                                - ascale * lmetrics.stringWidth(lstr);
                            ypos = yb - (int)((every * i - base) * scale)
                                + (int)(ascale * lmetrics.getHeight() / 3.5);
                            g.drawString(lstr, xpos, ypos);
                        }
                        break;
                    case AXIS_RIGHT:
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xr + tickLength + ascale * LABEL_GAP_RIGHT;
                            ypos = yb - (int)((every * i - base) * scale)
                                + (int)(ascale * lmetrics.getHeight() / 3.5);
                            g.drawString(lstr, xpos, ypos);
                        }
                        break;
                    case AXIS_TOP:
                        ypos = yt - tickLength - ascale * LABEL_GAP_TOP;
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xl + (int)((every * i - base) * scale)
                                - ascale * lmetrics.stringWidth(lstr) / 2;
                            g.drawString(lstr, xpos, ypos);
                        }
                        break;
                    case AXIS_BOTTOM:
                        ypos = yb + tickLength
                            + ascale * LABEL_GAP_BOTTOM
                            + ascale * lmetrics.getHeight();
                        for (int i = cfrom; i <= cto; i ++) {
                            lstr = lformat.format(every * i);
                            xpos = xl + (int)((every * i - base) * scale)
                                - ascale * lmetrics.stringWidth(lstr) / 2;
                            g.drawString(lstr, xpos, ypos);
                        }
                        break;
                }
                // 単位
                FontMetrics umetrics = getFontMetrics(ai.unitFont);
                int ux, uy;
                g.setColor(ai.unitColor);
                g.setFont(ai.unitFont);
                switch (axis) {
                    case AXIS_LEFT:
                        ux = xl - unitXOfs - ascale * umetrics.stringWidth(ai.unitLabel);
                        uy = yt - unitYOfs;
                        g.drawString(ai.unitLabel, ux, uy);
                        break;
                    case AXIS_RIGHT:
                        ux = xr + unitXOfs;
                        uy = yt - unitYOfs;
                        g.drawString(ai.unitLabel, ux, uy);
                        break;
                    case AXIS_TOP:
                        ux = xr + unitXOfs;
                        uy = yt - unitYOfs;
                        g.drawString(ai.unitLabel, ux, uy);
                        break;
                    case AXIS_BOTTOM:
                        ux = xr + unitXOfs;
                        uy = yb + unitYOfs + ascale * umetrics.getHeight();
                        g.drawString(ai.unitLabel, ux, uy);
                        break;
                }
            }
            // マーカ
            if (ai.markerVisible
                //&& ai.markerPos >= min && ai.markerPos <= max
                && ((flag & DRAW_MARKER) != 0)) {
                g.setColor(Color.white);
                g.setXORMode(ai.markerColor);
                int pos;
                switch (axis) {
                    case AXIS_LEFT:
                    case AXIS_RIGHT:
                        //pos = yb - (int)((ai.markerPos - base) * scale);
                        //pos = yb - (int)(ai.markerPos * scale);
                        pos = yb - (int)(ai.markerPos * (yb - yt));
                        g.drawLine(xl + 1, pos - 1, xr - 1, pos - 1);
                        g.drawLine(xl + 1, pos,     xr - 1, pos);
                        g.drawLine(xl + 1, pos + 1, xr - 1, pos + 1);
                        break;
                    case AXIS_TOP:
                    case AXIS_BOTTOM:
                        //pos = xl + (int)((ai.markerPos - base) * scale);
                        //pos = xl + (int)(ai.markerPos * scale);
                        pos = xl + (int)(ai.markerPos * (xr - xl));
                        g.drawLine(pos - 1, yt + 1, pos - 1, yb - 1);
                        g.drawLine(pos,     yt + 1, pos,     yb - 1);
                        g.drawLine(pos + 1, yt + 1, pos + 1, yb - 1);
                        break;
                }
                g.setPaintMode();
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

    /**
     * 凡例パネルクラス
     *
     */
    @SuppressWarnings("serial")
	public class LegendPanel extends JPanel {

        // -----------------------------------------------------------------
        // 定数
        private static final int MARGIN_X = 15;
        private static final int MARGIN_Y = 15;
        private static final int GAP_X = 10;
        private static final int GAP_Y = 5;
        private static final int LEN_LINE = 20;

        // -----------------------------------------------------------------
        // インスタンス変数
        private ArrayList<LegendInfo> legendList_;  // 凡例系列リスト
        private Font font_;             // ラベルフォント
        private Color backColor_;       // 背景色
        private Color labelColor_;      // ラベル色
        private Dimension size_;    // パネルサイズ

        // -----------------------------------------------------------------
        // コンストラクタ
        /**
         * コンストラクタ
         *
         * @param   font        Font    ラベルフォント
         * @param   backColor   Color   背景色
         * @param   labelColor  Color   ラベル色
         */
        public LegendPanel(
            Font font,
            Color backColor,
            Color labelColor
        ) {
            font_ = font;
            backColor_ = backColor;
            labelColor_ = labelColor;
            size_ = new Dimension(0, 0);
            //setPreferredSize(size_);
            legendList_ = new ArrayList<LegendInfo>();
        }

        /**
         * 凡例追加
         *
         * @param   legend  LegendInfo  凡例情報
         */
        public void addLegend(
            LegendInfo legend
        ) {
            legendList_.add(legend);
            updateSize();
        }

        /**
         * 凡例削除
         *
         * @param   legend  LegendInfo  凡例情報
         */
        public void removeLegend(
            LegendInfo legend
        ) {
            int ind = legendList_.indexOf(legend);
            legendList_.remove(ind);
            updateSize();
        }

        /**
         * ラベルフォント設定
         *
         * @param   font        Font    ラベルフォント
         */
        public void setFont(
            Font font
        ) {
            font_ = font;
        }

        /**
         * 背景色
         *
         * @param   backColor   Color   背景色
         */
        public void setBackColor(
            Color color
        ) {
            backColor_ = color;
        }

        /**
         * ラベル色設定
         *
         * @param   labelColor  Color   ラベル色
         */
        public void setLabelColor(
            Color color
        ) {
            labelColor_ = color;
        }

        /**
         * 描画
         *
         * @param   g   Graphics    グラフィックス
         */
        public void paint(
            Graphics g
        ) {
            // 背景
            int width = getSize().width;
            int height = getSize().height;
            g.setColor(backColor_);
            g.fillRect(0, 0, width, height);
            // 凡例
            g.setFont(font_);
            FontMetrics metrics = getFontMetrics(font_);    // フォントメトリクス
            int yofs = (int)(metrics.getHeight() / 3.5);    // ラベルYオフセット
            int ygap = metrics.getHeight() + GAP_Y;         // Y間隔
            ListIterator li = legendList_.listIterator();
            int ypos = MARGIN_Y;    // Y位置初期化
            while (li.hasNext()) {  // 全凡例をループ
                LegendInfo legend = (LegendInfo)li.next();  // 次の凡例
                g.setColor(legend.color);   // 凡例線色
                //g.drawLine(MARGIN_X, ypos - 1, MARGIN_X + LEN_LINE, ypos - 1);
                g.drawLine(MARGIN_X, ypos, MARGIN_X + LEN_LINE, ypos);  // 線の描画
                //g.drawLine(MARGIN_X, ypos + 1, MARGIN_X + LEN_LINE, ypos + 1);
                g.setColor(labelColor_);    // ラベル色
                g.drawString(   // ラベルの描画
                    legend.label,
                    MARGIN_X + LEN_LINE + GAP_X,
                    ypos + yofs
                );
                ypos += ygap;   // Y位置の更新
            }
        }

        /**
         * 必要十分サイズ取得
         *   凡例を表示するのに必要十分なサイズを取得する
         *      (現時点では使用していない)
         *
         */
        public Dimension getMinimalSize() {
            return size_;
        }

        /**
         * サイズ更新
         *   凡例の数や長さに応じてパネルのサイズを決定する
         *
         */
        private void updateSize() {
            FontMetrics metrics = getFontMetrics(font_);    // フォントメトリクス
            int ygap = metrics.getHeight() + GAP_Y; // Y間隔
            ListIterator li = legendList_.listIterator();
            int ysize = MARGIN_Y;   // 高さ
            int max = 0;    // ラベル最大長
            while (li.hasNext()) {  // 全凡例をループ
                LegendInfo legend = (LegendInfo)li.next();
                int len = metrics.stringWidth(legend.label);    // ラベルの長さを取得
                if (len > max) {    // 最大長?
                    max = len;  // 最大長を更新
                }
                if (li.hasNext()) { // 最後の凡例でない?
                    ysize += ygap;  // 高さを更新
                }
            }
            ysize += MARGIN_Y;  // 下マージン
            size_.width = MARGIN_X + LEN_LINE + GAP_X + max + MARGIN_X; // 幅更新
            size_.height = ysize;   // 高さ更新
            //System.out.println("ygap = " + ygap);
            //System.out.println("(" + size_.width + ", " + size_.height + ")");
        }
    }

    /**
     * 凡例情報クラス
     *
     */
    private class LegendInfo {

        // -----------------------------------------------------------------
        // インスタンス変数
        public Color color;     // 色
        public String label;    // ラベル

        // -----------------------------------------------------------------
        // コンストラクタ
        /**
         * コンストラクタ
         *
         * @param   color   Color   描画色
         * @param   label   String  ラベル
         */
        public LegendInfo(
            Color color,
            String label
        ) {
            this.color = color;
            this.label = label;
        }
    }
}

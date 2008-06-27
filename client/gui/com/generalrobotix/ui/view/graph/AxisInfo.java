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

/**
 * 軸情報クラス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class AxisInfo {

    // -----------------------------------------------------------------
    // インスタンス変数
    public double base;             // 軸下端(左端)の値
    public double extent;           // 軸下端(左端)から軸上端(右端)までの幅
    public double max;              // 最大値(この値より大きいティックやラベルを振らない)
    public double min;              // 最小値(この値未満のティックやラベルを振らない)
    public boolean maxLimitEnabled; // ティックやラベルの最大値制限を有効にする
    public boolean minLimitEnabled; // ティックやラベルの最小値制限を有効にする
    public Color color;             // 軸の色
    public double factor;           // データにこの係数を掛けてプロットする
    public double tickEvery;        // ティック間隔
    public int tickLength;          // ティックの長さ
    public double labelEvery;       // ラベル間隔
    public String labelFormat;      // ラベルフォーマット
    public Font labelFont;          // ラベルフォント
    public Color labelColor;        // ラベル色
    public Font unitFont;           // 単位ラベルフォント
    public String unitLabel;        // 単位ラベル
    public Color unitColor;         // 単位色
    public int unitXOfs;
    public int unitYOfs;
    public double gridEvery;        // グリッド間隔
    public Color gridColor;         // グリッドの色
    public boolean markerVisible;   // マーカーの表示フラグ
    public double markerPos;        // マーカーの表示位置
    public Color markerColor;       // マーカの色

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   base    double  軸下端(左端)の値
     * @param   extent  double  軸下端(左端)から軸上端(右端)までの幅
     */
    public AxisInfo(
        double base,
        double extent
    ) {
        this.base = base;
        this.extent = extent;
        max = 0.0;
        min = 0.0;
        maxLimitEnabled = false;
        minLimitEnabled = false;
        color = Color.white;
        factor = 1.0;
        tickEvery = 0.0;
        tickLength = 3;
        labelEvery = 0.0;
        labelFormat = "0";
        labelFont = new Font("monospaced", Font.PLAIN, 10);
        labelColor = Color.white;
        unitFont = new Font("dialog", Font.PLAIN, 10);
        unitLabel = "";
        unitColor = Color.white;
        unitXOfs = 0;
        unitYOfs = 0;
        gridEvery = 0.0;
        gridColor = Color.darkGray;
        markerVisible = false;
        markerPos = 0.0;
        markerColor = Color.lightGray;
    }
}

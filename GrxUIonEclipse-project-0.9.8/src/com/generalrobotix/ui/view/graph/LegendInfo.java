package com.generalrobotix.ui.view.graph;

import org.eclipse.swt.graphics.Color;

/**
 * 凡例情報クラス
 *
 */
public class LegendInfo {

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
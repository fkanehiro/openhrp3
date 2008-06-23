package com.generalrobotix.ui.view.graph;

/**
 * データ種別
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class DataKind {

    public final String name;       // データ種別名
    public final String unitLabel;  // 単位ラベル
    public final double base;       // 基準値
    public final double extent;     // 幅
    public final double factor;     // プロット係数

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   name        データ種別名
     * @param   unitLabel   単位ラベル
     * @param   base        基準値
     * @param   extent      幅
     * @param   factor      プロット係数
     */
    public DataKind(
        String name,
        String unitLabel,
        double base,
        double extent,
        double factor
    ) {
        this.name = name;
        this.unitLabel = unitLabel;
        this.base = base;
        this.extent = extent;
        this.factor = factor;
    }
}

package com.generalrobotix.ui.view.graph;

/**
 * データモデル
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class DataModel {

    public final DataItem dataItem;     // データアイテム
    public final DataSeries dataSeries; // データ系列

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   dataItem    データアイテム
     * @param   dataSeries  データ系列
     */
    public DataModel(
        DataItem dataItem,
        DataSeries dataSeries
    ) {
        this.dataItem = dataItem;
        this.dataSeries = dataSeries;
    }
}

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

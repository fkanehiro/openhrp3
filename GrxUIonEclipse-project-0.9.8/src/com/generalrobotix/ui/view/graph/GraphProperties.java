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

import java.util.*;
import java.io.*;
import java.net.URL;

/**
 * グラフプロパティ
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class GraphProperties {

    // -----------------------------------------------------------------
    // 定数
    private static final String GRAPH_PROPERTIES = "/resources/graph.properties";
    private static final String SEP = ".";
    private static final String DATA_KIND_NAMES = "dataKindNames";
    private static final String UNIT = "unit";
    private static final String BASE = "base";
    private static final String EXTENT = "extent";
    private static final String FACTOR = "factor";
    private static final String DATA_KIND = "dataKind";

    // -----------------------------------------------------------------
    // クラス変数
    private static GraphProperties this_;   // シングルトン用オブジェクトホルダ
    private static HashMap<String, DataKind> dataKindMap_;    // データ種別名とデータ種別の対応表
    private static HashMap<String, DataKind> attributeMap_;   // アトリビュートとデータ種別の対応表

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *   非公開
     *
     */
    private GraphProperties() {
        // プロパティファイルの読み込み
        URL url = this.getClass().getResource(GRAPH_PROPERTIES);
        Properties prop = new Properties();
        try {
            prop.load(url.openStream());    // プロパティファイル読み込み
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(0);
        }

        // データ種別の読み込み
        dataKindMap_ = new HashMap<String, DataKind>();
        StringTokenizer dkNames =
            new StringTokenizer(prop.getProperty(DATA_KIND_NAMES), ",");
        while (dkNames.hasMoreTokens()) {
            String dkName = dkNames.nextToken();
            String unit = prop.getProperty(dkName + SEP + UNIT);
            double base = Double.parseDouble(prop.getProperty(dkName + SEP + BASE));
            double extent = Double.parseDouble(prop.getProperty(dkName + SEP + EXTENT));
            double factor = (
                (prop.containsKey(dkName + SEP + FACTOR))
                ? Double.parseDouble((String)prop.getProperty(dkName + SEP + FACTOR))
                : 1
            );
            DataKind dk = new DataKind(dkName, unit, base, extent, factor);
            dataKindMap_.put(dkName, dk);
        }

        // アトリビュート毎のデータ種別の読み込み
        attributeMap_ = new HashMap<String, DataKind>();
        String postfix = SEP + DATA_KIND;
        int postfixlen = postfix.length();
        Enumeration elm = prop.propertyNames();
        while (elm.hasMoreElements()) {
            String pname = (String)elm.nextElement();
            if (pname.endsWith(postfix)) {
                String aname = pname.substring(
                    0, pname.length() - postfixlen
                );
                attributeMap_.put(aname, dataKindMap_.get(prop.getProperty(pname)));
            }
        }
    }

    // -----------------------------------------------------------------
    // クラスメソッド
    /**
     * データ種別名からデータ種別取得
     *
     * @param   dataKindName    データ種別名
     */
    public static DataKind getDataKindFromName(
        String dataKindName
    ) {
        if (this_ == null) {
            this_ = new GraphProperties();
        }
        return (DataKind)dataKindMap_.get(dataKindName);
    }

    /**
     * アトリビュート名からデータ種別取得
     *
     * @param   attribute   アトリビュート名("Joint.angle"のような形式で指定)
     */
    public static DataKind getDataKindFromAttr(
        String attribute
    ) {
        if (this_ == null) {
            this_ = new GraphProperties();
        }
        return (DataKind)attributeMap_.get(attribute);
    }
}

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
 * データアイテム
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class DataItem {

    public final String object;     // オブジェクト名
    public final String node;       // ノード名
    public final String attribute;  // アトリビュート名
    public final int index;         // 添字

    final String fullName_;         // 完全名
    final String attributePath_;    // アトリビュートパス(添字なし)

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   object      オブジェクト名
     * @param   node        ノード名
     * @param   attribute   アトリビュート名
     * @param   index       添字 (添字が不要場合は-1を与える)
     */
    public DataItem(
        String object,
        String node,
        String attribute,
        int index
    ) {
        this.object = object;
        this.node = node;
        this.attribute = attribute;
        this.index = index;

        StringBuffer sb;
        if (object == null) {   // オブジェクト名なし?
            sb = new StringBuffer();    // オブジェクト名除外
        } else {    // オブジェクト名あり?
            sb = new StringBuffer(object);  // オブジェクト名付加
            sb.append(".");
        }
        sb.append(node);    // ノード
        sb.append(".");
        sb.append(attribute);   // アトリビュート
        attributePath_ = sb.toString();
        if (index >= 0) {       // 多次元?
            sb.append(".");
            sb.append(index);   // 添字
        }
        fullName_ = sb.toString();
    }

    /**
     * 文字列表現取得
     *   添字まで含めた完全な名前を返す
     *   (例: "rob1.LARM_JOINT2.absPos.2")
     *
     * @return  文字列表現
     */
    public String toString() {
        return fullName_;
    }

    /**
     * アトリビュートパス取得
     *   アトリビュートパスを返す(添字は含まない)
     *   (例: "rob1.LARM_JOINT2.absPos")
     *
     * @return  アトリビュートパス
     */
    public String getAttributePath() {
        return attributePath_;
    }

    /**
     * 配列か否か
     *
     * @return  配列か否か
     */
    public boolean isArray() {
        return (index >= 0);
    }
}

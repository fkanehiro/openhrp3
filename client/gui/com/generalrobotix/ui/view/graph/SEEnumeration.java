/**
 * SEEnumeration.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

import java.util.StringTokenizer;

public class SEEnumeration implements StringExchangeable {
    String[] item_;
    int selection_;
    
    public SEEnumeration(String[] s, int selection) {
        item_ = s;
        selection_ = selection;
    }

    /**
     *  カンマで区切られた文字列から、item_を生成する
     */
    public SEEnumeration(String str) {
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        item_ = new String[tokenizer.countTokens()];
        for (int i = 0; tokenizer.hasMoreTokens(); i ++) {
            item_[i] = tokenizer.nextToken();
        }
        selection_ = 0;
    }

    /**
     * String値を取得
     *
     * @return   String値
     */
    public String toString() {
        return item_[selection_];
    }

    /**
     * String値からObjectを取得
     *
     * @param  str  String値
     * @return      Object値
     */
    public Object fromString(String str) {
        if (str == null) {
            selection_ = 0;
            return item_[selection_];
        }

        for(int i =0; i < item_.length; i ++) {
            if (str.equals(item_[i])) {
                selection_ = i;
                break;
            }
        }
        return item_[selection_];
    }

    /**
     * Object値の設定
     *
     * @param  value  Object値
     */

    public void setValue(Object value) {
        fromString((String)value);
    }

    /**
     * String値の設定
     *
     * @param  str  String値
     */
    public void setValue(String str) {
        fromString((String)str);
    }

    /**
     * Object値の取得
     *
     * @return  Object値
     */
    public Object getValue() {
        return  item_[selection_];
    }

    /**
     * String[]値の取得
     *
     * @return  String[]値
     */
    public String[] getSelectionNames() {
        return item_;
    }
    /**
     * Index値の取得
     *
     * @return  String[]値
     */
    public int getSelectedIndex() {
        return selection_;
    }
}

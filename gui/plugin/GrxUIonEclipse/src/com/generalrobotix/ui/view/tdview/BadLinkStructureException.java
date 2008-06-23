/**
 * BadLinkStructureException.java
 *
 * リンク構造が正しくないことを伝える例外
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.tdview;

public class BadLinkStructureException extends Exception {
    /**
     * コンストラクタ
     *
     * @param   String str    詳細
     */
    public BadLinkStructureException() {
    super();
    }

    public BadLinkStructureException(String str) {
        super(str);
    }
}

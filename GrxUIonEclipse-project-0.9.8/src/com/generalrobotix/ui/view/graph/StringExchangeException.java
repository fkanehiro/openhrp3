/**
 * StringExchangeException.java
 *
 * StringExchangeableを実装したクラスが変換できない文字列を与えられた時に
 * 発生する例外
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;

import java.lang.RuntimeException;

public class StringExchangeException extends RuntimeException {
    /**
     * コンストラクタ
     *
     * @param   str    詳細
     */
    public StringExchangeException() {
        super();
    }

    public StringExchangeException(String str) {
        super(str);
    }
};

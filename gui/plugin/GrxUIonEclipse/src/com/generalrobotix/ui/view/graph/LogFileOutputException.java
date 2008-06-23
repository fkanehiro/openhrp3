/**
 * LogFileOutputException.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.graph;

public class LogFileOutputException extends Exception {
    /**
     * コンストラクタ
     *
     * @param   String str    詳細
     */
    public LogFileOutputException() {
        super();
    }

    public LogFileOutputException(String str) {
        super(str);
    }
}

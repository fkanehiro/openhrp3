/**
 * LogFileFormatException.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.graph;

public class LogFileFormatException extends Exception {
    /**
     * コンストラクタ
     *
     * @param   String str    詳細
     */
    public LogFileFormatException() {
        super();
    }

    public LogFileFormatException(String str) {
        super(str);
    }
}

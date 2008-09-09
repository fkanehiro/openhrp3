/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
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

@SuppressWarnings("serial")
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

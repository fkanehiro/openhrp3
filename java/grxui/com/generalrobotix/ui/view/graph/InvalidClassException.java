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
 * InvalidClassException.java
 *
 * 与えられたクラスが間違っていることを伝える例外
 *  SimulationNodeのサブクラスでaddChild()するとき、子ノードとして
 *  取ることの出来ないクラスが与えられた時、この例外を発生する。
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.graph;

import java.lang.RuntimeException;

public class InvalidClassException extends RuntimeException {
    /**
     * コンストラクタ
     *
     * @param   s    詳細
     */
    public InvalidClassException() {
        super();
    }

    public InvalidClassException(String s) {
        super(s);
    }
};

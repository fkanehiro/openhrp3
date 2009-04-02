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
 *  WorldChangeListener.java
 *
 *  @author  Kernel, Inc.
 *  @version  1.1 (2001/07/17)
 */
 
package com.generalrobotix.ui.view.graph;

import java.util.EventListener;

public interface SpinListener extends EventListener {
    public void up();
    public void down();
}

/**
 *  WorldChangeListener.java
 *
 *  @author  Kernel, Inc.
 *  @version  1.1 (2001/07/17)
 */
 
package com.generalrobotix.ui.view.tdview;

import java.util.EventListener;
import java.util.List;

import com.generalrobotix.ui.GrxBaseItem;

public interface WorldReplaceListener extends EventListener {
    public void replaceWorld(List<GrxBaseItem> list);
}

package com.generalrobotix.test;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxSeqMotionItem extends GrxBaseItem {
  public static String ITEM_NAME = "Seqplay Motion";
  public GrxSeqMotionItem(String name, GrxPluginManager manager) {
    super(name, manager);
  }
}

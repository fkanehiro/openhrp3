/*
 *  GrxCollisionPairItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

//import javax.swing.ImageIcon;

import org.eclipse.swt.graphics.Image;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxCollisionPairItem extends GrxBaseItem {
    public static final String TITLE = "Collision Pair";
  
    public GrxCollisionPairItem(String name, GrxPluginManager manager) {
        super(name, manager);
        //setIcon(new ImageIcon(getClass().getResource("/resources/images/collision.png")));
        setIcon( "collision.png" );
    }
}

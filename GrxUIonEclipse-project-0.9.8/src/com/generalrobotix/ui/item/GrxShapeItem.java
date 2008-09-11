/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */

/*
 *  GrxModelItem.java
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  @author Shin'ichiro Nakaoka (AIST)
 */

package com.generalrobotix.ui.item;

import javax.media.j3d.BranchGroup;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
/**
 * @brief sensor
 */
public class GrxShapeItem extends GrxBaseItem{
	public BranchGroup bg_;
	/*
    final public double[] translation;
    final public double[] rotation;
    */
    public GrxLinkItem parent_;

    /**
     * @brief delete this shape
     */
    public void delete() {
    	super.delete();
    	bg_.detach();
    	parent_.removeShape(this);
    }
    
    
    /**
     * @brief constructor
     * @param info SensorInfo retrieved through ModelLoader
     */
    public GrxShapeItem(String name, GrxPluginManager manager, BranchGroup bg) {
    	super(name, manager);
    	bg_ = bg;

		getMenu().clear();
		
		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return "rename";
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, null,
						"Input new name.", getName(),null);
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return "delete";
			}
			public void run(){
				if( MessageDialog.openQuestion( null, "delete shape",
						"Are you sure to delete " + getName() + " ?") )
					delete();
			}
		};
		setMenuItem(item);

		/*
        translation = info.translation;
        rotation = info.rotation;
		*/
    }


}

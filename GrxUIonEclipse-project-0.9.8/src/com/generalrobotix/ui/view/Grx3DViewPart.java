package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;

public class Grx3DViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
    	//  3DViewは、一つしか開かない。。//
    	Activator act = Activator.getDefault();
		if(act != null){
			GrxPluginManager manager = act.manager_;
			if(manager != null){
				GrxBaseView view = manager.getView(Grx3DView.class, false);
				if(view == null)
					createView( Grx3DView.class, "3DView", this, parent );
			}
		}
    }
}

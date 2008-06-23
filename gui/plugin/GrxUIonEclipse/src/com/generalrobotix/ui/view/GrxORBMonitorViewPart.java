package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.grxui.Activator;

public class GrxORBMonitorViewPart extends GrxBaseViewPart {
    
    public void createPartControl(Composite parent) {
        createView( GrxORBMonitorView.class, "NameService Monitor", this, parent );
    }


}

package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxORBMonitorViewPart extends GrxBaseViewPart {
    
    public void createPartControl(Composite parent) {
        createView( GrxORBMonitorView.class, "NameService Monitor", this, parent );
    }


}

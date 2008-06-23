package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class Grx3DViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
        createView( Grx3DView.class, "Test View", this, parent );
    }

}

package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxControllerViewPart extends GrxBaseViewPart {
    public void createPartControl(Composite parent) {
        createView( GrxControllerView.class, "ControllerView", this, parent );
    }

}

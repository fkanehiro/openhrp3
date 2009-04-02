package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxProcessManagerViewPart extends GrxBaseViewPart {
    public void createPartControl(Composite parent) {
        createView( GrxProcessManagerView.class, "Process Manager", this, parent );
    }

}

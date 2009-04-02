package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxGraphViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
        createView( GrxGraphView.class, "Graph", this, parent );
    }

}

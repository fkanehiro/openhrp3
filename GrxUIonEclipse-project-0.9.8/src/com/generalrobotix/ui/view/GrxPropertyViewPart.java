package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxPropertyViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
        createView( GrxPropertyView.class, "Property View", this, parent );
    }

}

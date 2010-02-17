package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.grxui.Activator;

public class GrxOpenHRPViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
        createView( GrxOpenHRPView.class, "OpenHRP", this, parent );
    }

}

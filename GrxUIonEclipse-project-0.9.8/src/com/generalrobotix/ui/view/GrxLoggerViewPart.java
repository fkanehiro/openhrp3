package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxLoggerViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
        createView( GrxLoggerView.class, "Test View", this, parent );
    }


}

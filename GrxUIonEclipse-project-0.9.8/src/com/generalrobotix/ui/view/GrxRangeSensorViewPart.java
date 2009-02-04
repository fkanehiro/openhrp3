package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxRangeSensorViewPart extends GrxBaseViewPart {

    public void createPartControl(Composite parent) {
        createView( GrxRangeSensorView.class, "Range Sensor View", this, parent );
    }

}

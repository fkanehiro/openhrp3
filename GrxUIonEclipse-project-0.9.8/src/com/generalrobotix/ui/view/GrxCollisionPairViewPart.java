package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxCollisionPairViewPart extends GrxBaseViewPart {
    public void createPartControl(Composite parent) {
        createView( GrxCollisionPairView.class, "CollisionPairView", this, parent );
    }

}

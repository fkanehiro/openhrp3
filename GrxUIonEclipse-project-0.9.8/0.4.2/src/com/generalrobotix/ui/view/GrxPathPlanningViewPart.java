package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxPathPlanningViewPart extends GrxBaseViewPart{

	public void createPartControl(Composite parent) {
		createView( GrxPathPlanningView.class, "Path Planning", this, parent );
	}
}

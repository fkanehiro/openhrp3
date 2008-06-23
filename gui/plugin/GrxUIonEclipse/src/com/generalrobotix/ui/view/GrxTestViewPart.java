package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxTestViewPart extends GrxBaseViewPart{

	public void createPartControl(Composite parent) {
		createView( GrxTestView.class, "Test View", this, parent );
	}



}

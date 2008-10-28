package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxItemViewPart extends GrxBaseViewPart{

	public void createPartControl(Composite parent) {
		createView( GrxItemView.class, "Item View", this, parent );
	}
}

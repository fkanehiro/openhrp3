package com.generalrobotix.ui.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.generalrobotix.ui.GrxBaseViewPart;

public class GrxTextEditorViewPart extends GrxBaseViewPart{

	public void createPartControl(Composite parent) {
		createView( GrxTextEditorView.class, "Text Editor", this, parent );
	}
}

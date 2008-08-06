package com.generalrobotix.ui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxORBMonitor;

@SuppressWarnings("serial")
public class GrxORBMonitorView extends GrxBaseView {
    public static final String TITLE = "NameService Monitor";

    public GrxORBMonitorView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
        super(name, manager, vp, parent);

        new GrxORBMonitor(composite_,SWT.NULL);
        setScrollMinSize();
    }
}

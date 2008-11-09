package com.generalrobotix.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.view.Grx3DView;
import com.generalrobotix.ui.view.GrxOpenHRPView;

public class StartSimulate implements IWorkbenchWindowActionDelegate {
	public StartSimulate() {}

	public void run(IAction action) {
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		Grx3DView tdview = (Grx3DView)manager_.getView(Grx3DView.class);
		tdview.disableOperation();
		GrxOpenHRPView hrp =  (GrxOpenHRPView)manager_.getView( GrxOpenHRPView.class );
		hrp.startSimulation(true);
	}

	public void selectionChanged(IAction action, ISelection selection) {}
	public void dispose() {}
	public void init(IWorkbenchWindow window) {}
}
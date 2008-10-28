package com.generalrobotix.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.view.GrxOpenHRPView;

public class StartSimulate implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	public StartSimulate() {}

	public void run(IAction action) {
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		GrxOpenHRPView hrp =  (GrxOpenHRPView)manager_.getView( GrxOpenHRPView.class );
		hrp.startSimulation(true);
	}

	public void selectionChanged(IAction action, ISelection selection) {}
	public void dispose() {}
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}
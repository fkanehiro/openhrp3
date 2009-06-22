package com.generalrobotix.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.view.GrxOpenHRPView;

public class StopSimulate implements IWorkbenchWindowActionDelegate {
	public StopSimulate() {}

	public void run(IAction action) {
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		GrxOpenHRPView hrp =  (GrxOpenHRPView)manager_.getView( GrxOpenHRPView.class );
		if(hrp==null){
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			try {
				page.showView("com.generalrobotix.ui.view.GrxOpenHRPViewPart", null, IWorkbenchPage.VIEW_CREATE);  
			} catch (PartInitException e1) {
				e1.printStackTrace();
			}
			hrp =  (GrxOpenHRPView)manager_.getView( GrxOpenHRPView.class );
		}
		hrp.stopSimulation();
	}

	public void selectionChanged(IAction action, ISelection selection) {}
	public void dispose() {}
	public void init(IWorkbenchWindow window) {}
}
package com.generalrobotix.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxProjectItem;

public class SaveProjectAs implements IWorkbenchWindowActionDelegate {

	public void run(IAction action) {
		GrxPluginManager manager_ = Activator.getDefault().manager_;
		manager_.getProjectMenu().get( GrxProjectItem.MENU_SAVE_AS ).run();
	}

	public void dispose() {}
	public void init(IWorkbenchWindow window) {}
	public void selectionChanged(IAction action, ISelection selection) {}
}

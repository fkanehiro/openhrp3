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
import com.generalrobotix.ui.view.GrxJythonPromptView;

public class ExecuteScript implements IWorkbenchWindowActionDelegate {
    public ExecuteScript() {}

    public void run(IAction action) {
        GrxPluginManager manager_ = Activator.getDefault().manager_;
        GrxJythonPromptView jythonView =  (GrxJythonPromptView)manager_.getView( GrxJythonPromptView.class );
        if(jythonView==null){
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            IWorkbenchPage page = window.getActivePage();
            try {
                page.showView("com.generalrobotix.ui.view.GrxJythonPromptViewPart", null, IWorkbenchPage.VIEW_CREATE);
            } catch (PartInitException e1) {
                e1.printStackTrace();
            }
            jythonView =  (GrxJythonPromptView)manager_.getView( GrxJythonPromptView.class );
        }
        if(jythonView.getEnabledExecBtn())
            jythonView.selectExecBtn();
    }

    public void selectionChanged(IAction action, ISelection selection) {}
    public void dispose() {}
    public void init(IWorkbenchWindow window) {}
}
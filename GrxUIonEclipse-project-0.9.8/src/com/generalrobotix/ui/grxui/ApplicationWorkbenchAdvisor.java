package com.generalrobotix.ui.grxui;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory";

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	public void initialize(IWorkbenchConfigurer configurer) {
        configurer.setSaveAndRestore(true);
    }
	
	public void preStartup(){
		//window が開く前に実行される　//
        
		Activator activator = Activator.getDefault();
		IWorkbench workbench = PlatformUI.getWorkbench();
    	workbench.addWorkbenchListener(activator);

        try{
            activator.tryLockFile();
        } catch (Exception ex) {
            activator.breakStart( ex, null );
        }
		if(activator.getImageRegistry() == null)
			try {
				activator.registryImage();
			} catch (Exception e) {
				e.printStackTrace();
			}
		if(activator.getFontRegistry() == null)
			activator.registryFont();
		if(activator.getColorRegistry() == null)
			activator.registryColor();
        activator.startGrxUI();
	}

	public void postStartup(){
		Activator.getDefault().loadInitialProject();
	}
}

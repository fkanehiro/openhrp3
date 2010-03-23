package com.generalrobotix.ui.grxui;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxProcessManager;

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
	
	public boolean preShutdown() {
        try {
        	IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        	for(IWorkbenchWindow window : windows){
        		IWorkbenchPage[] pages = window.getPages();
        		for(IWorkbenchPage page : pages){
        			IPerspectiveDescriptor[] perspectives = page.getOpenPerspectives();
        			for(IPerspectiveDescriptor perspective : perspectives){
        				if(perspective.getId().equals(GrxUIPerspectiveFactory.ID+".project"))
            				page.closePerspective(perspective, false, false);
        			}
        		}
        	}
        	IPerspectiveRegistry perspectiveRegistry=PlatformUI.getWorkbench().getPerspectiveRegistry();
        	IPerspectiveDescriptor tempPd=perspectiveRegistry.findPerspectiveWithId(GrxUIPerspectiveFactory.ID + ".project");
       		if(tempPd!=null)
       			perspectiveRegistry.deletePerspective(tempPd);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }
	
	public void postShutdown(){
		//window が閉じられたのち実行される　//
		GrxProcessManager.shutDown();
        try {
            GrxCorbaUtil.getORB().shutdown(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
		Activator.getDefault().stopGrxUI();
	}
}

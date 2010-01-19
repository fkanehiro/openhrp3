package com.generalrobotix.ui.grxui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;

public class GrxUIPerspectiveFactory implements IPerspectiveFactory {

    public static final String ID = "com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory";
    
    public static final String[] views = {
            "com.generalrobotix.ui.view.GrxItemViewPart",
            "com.generalrobotix.ui.view.GrxJythonPromptViewPart",
            "com.generalrobotix.ui.view.GrxORBMonitorViewPart",
            "com.generalrobotix.ui.view.GrxProcessManagerViewPart",
            "com.generalrobotix.ui.view.GrxServerManagerViewPart",
            "com.generalrobotix.ui.view.GrxLoggerViewPart",
            "com.generalrobotix.ui.view.GrxGraphViewPart",
            "com.generalrobotix.ui.view.GrxPropertyViewPart",
            "com.generalrobotix.ui.view.GrxRobotStatViewPart",
            "com.generalrobotix.ui.view.Grx3DViewPart",
            "com.generalrobotix.ui.view.GrxOpenHRPViewPart",
            "com.generalrobotix.ui.view.GrxTextEditorViewPart" };

    
    public static Shell getCurrentShell(){
        Shell   ret = null;
        IWorkbench workbench = PlatformUI.getWorkbench();
        if( workbench != null){
            for( IWorkbenchWindow window : workbench.getWorkbenchWindows() ){
                for(IWorkbenchPage page:window.getPages()){
                    if ( page.getPerspective().getId().equals(GrxUIPerspectiveFactory.ID) ){
                        ret = window.getShell();
                        break;
                    }
                }
            }
        }
        return ret;
    }
    
    public GrxUIPerspectiveFactory() {
        System.out.println("INIT GrxUIPerspectiveFactory");
    }

    public void createInitialLayout(IPageLayout layout) {
        System.out.println("START GrxUIPerspectiveFactory");
        
        // エディタ領域を取得
        String editorArea = layout.getEditorArea();

        // フォルダの作成
        IFolderLayout top_folder = layout.createFolder("TopViews",
                IPageLayout.TOP, (float) 0.15f, editorArea);
        IFolderLayout right_middle = layout.createFolder("RightMIDDLEViews",
                IPageLayout.RIGHT, (float) 0.6f, editorArea);
        IFolderLayout middle = layout.createFolder("MiddleViews",
                IPageLayout.BOTTOM, (float) 0.4f, editorArea);
        IFolderLayout top_left = layout.createFolder("TopLeftViews",
                IPageLayout.LEFT, (float) 0.6f, editorArea);
        IFolderLayout top_right = layout.createFolder("TopRightViews",
                IPageLayout.RIGHT, (float) 0.35f, "TopLeftViews");

        top_folder.addView("com.generalrobotix.ui.view.GrxLoggerViewPart");
        top_left.addView("com.generalrobotix.ui.view.GrxItemViewPart");

        top_right.addView("com.generalrobotix.ui.view.GrxORBMonitorViewPart");
        top_right
                .addView("com.generalrobotix.ui.view.GrxProcessManagerViewPart");
        top_right
                .addView("com.generalrobotix.ui.view.GrxServerManagerViewPart");
        //top_right.addView("com.generalrobotix.ui.view.GrxLoggerViewPart");

        right_middle.addView("com.generalrobotix.ui.view.GrxGraphViewPart");
        right_middle.addView("com.generalrobotix.ui.view.GrxPropertyViewPart");
        right_middle.addView("com.generalrobotix.ui.view.GrxRobotStatViewPart");

        middle.addView("com.generalrobotix.ui.view.Grx3DViewPart");
        middle.addView("com.generalrobotix.ui.view.GrxOpenHRPViewPart");
        middle.addView("com.generalrobotix.ui.view.GrxTextEditorViewPart");

        // エディタ領域不可視
        layout.setEditorAreaVisible(false);
    }
}

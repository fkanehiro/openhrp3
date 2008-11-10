package com.generalrobotix.ui.grxui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class GrxUIPerspectiveFactory implements IPerspectiveFactory {

	public static final String ID = "com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory";

	public static final String[] views = {
	    "com.generalrobotix.ui.view.GrxItemViewPart",
	    "com.generalrobotix.ui.view.GrxJythonPromptViewPart",
		"com.generalrobotix.ui.view.GrxORBMonitorViewPart",
		"com.generalrobotix.ui.view.GrxProcessManagerViewPart",
		"com.generalrobotix.ui.view.GrxLoggerViewPart",
		"com.generalrobotix.ui.view.GrxGraphViewPart",
		"com.generalrobotix.ui.view.GrxPropertyViewPart",
		"com.generalrobotix.ui.view.GrxRobotStatViewPart",
		"com.generalrobotix.ui.view.Grx3DViewPart",
		"com.generalrobotix.ui.view.GrxOpenHRPViewPart",
		"com.generalrobotix.ui.view.GrxTextEditorViewPart"
	};
	
	public GrxUIPerspectiveFactory() {
		System.out.println("INIT GrxUIPerspectiveFactory");
	}
	
	public void createInitialLayout(IPageLayout layout) {
		System.out.println("START GrxUIPerspectiveFactory");

		// エディタ領域を取得
        String editorArea = layout.getEditorArea();

        // フォルダの作成
	    IFolderLayout right_middle = layout.createFolder("RightMIDDLEViews",
	    		IPageLayout.BOTTOM, (float)0.2f, "RightTOPViews");
	    IFolderLayout top_right = layout.createFolder("TopRightViews",
	    		IPageLayout.TOP, (float)0.3f, editorArea);
	    IFolderLayout top_left = layout.createFolder("TopLeftViews",
	    		IPageLayout.LEFT, (float)0.3f, "TopRightViews");
	    IFolderLayout middle = layout.createFolder("MiddleViews",
	    		IPageLayout.TOP, (float)0.9f, editorArea);

	    top_left.addView( "com.generalrobotix.ui.view.GrxItemViewPart" );

		top_right.addView( "com.generalrobotix.ui.view.GrxJythonPromptViewPart" );
		top_right.addView( "com.generalrobotix.ui.view.GrxORBMonitorViewPart" );
		top_right.addView( "com.generalrobotix.ui.view.GrxProcessManagerViewPart" );
		top_right.addView( "com.generalrobotix.ui.view.GrxLoggerViewPart" );

		right_middle.addView( "com.generalrobotix.ui.view.GrxGraphViewPart" );
		right_middle.addView( "com.generalrobotix.ui.view.GrxPropertyViewPart" );
		right_middle.addView( "com.generalrobotix.ui.view.GrxRobotStatViewPart" );

		middle.addView( "com.generalrobotix.ui.view.Grx3DViewPart" );
		middle.addView( "com.generalrobotix.ui.view.GrxOpenHRPViewPart" );
		middle.addView( "com.generalrobotix.ui.view.GrxTextEditorViewPart" );
	}
}

/*
 *  GrxTestView.java
 *
 *  Copyright (C) 2007 s-cubed, Inc.
 *  All Rights Reserved
 *
 *  @author keisuke kobayashi (s-cubed, Inc.)
 */

package com.generalrobotix.ui.view;

import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxModelItem.LinkInfoLocal;
import com.generalrobotix.ui.util.Grx3DViewClickListener;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.pickfast.PickTool;


@SuppressWarnings("serial")
public class GrxTestView extends GrxBaseView {
	// GrxPluginManager内で、PluginInfoに保存される。
	// Viewの場合はほとんど関係ないかな…
    public static final String TITLE = "Test View";
	public static final String FILE_EXTENSION = "test";
	public static final String DEFAULT_RELATIVE_PATH = "testDir";

	GrxTestViewPart vp;

	int imgCnt=1,imgMax=13;

	private Label imgLabel;
	private Button listenButton, createView;
	private Text viewName;
	
	
	Grx3DView tdView = null;

	//private static MyService myService;
	//private static PathPlanner pathPlanner;
	
	public GrxTestView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);
		GrxDebugUtil.println("[TEST]@GrxTestView Construct GrxTestView");

    	tdView = get3DView();
		
		GridLayout layout = new GridLayout(4,false);
		getComposite().setLayout( layout );
		getComposite().setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
    	
		listenButton = new Button( getComposite(), SWT.LEFT|SWT.BORDER );
		listenButton.setText("3DViewListener");
		listenButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	// Grx3DViewにリスナーを追加
            	System.out.println("Add Listener to Grx3DView");
            	tdView.addClickListener(new Grx3DViewClickListener(){
            		public void run(int x, int y){
            			addPoint(x,y);
            		}
            	});
			}
        });

		createView= new Button( getComposite(), SWT.LEFT|SWT.BORDER );
		createView.setText("Create View");
		createView.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
        		IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                IWorkbenchPage page = window.getActivePage();
                IViewPart view=null;// = page.findView(viewId);
                if(view==null) {
                	try {
                		// ビューを表示せずに初期化する
                		view = page.showView(viewName.getText(), null, IWorkbenchPage.VIEW_CREATE );
                	}catch(Exception e1){
                		e1.printStackTrace();
                	}
                }

            }
        });

		viewName = new Text( getComposite(), SWT.BORDER );
		GridData gd = new GridData();
		gd.widthHint = 200;
		viewName.setLayoutData( gd );
		
		imgLabel = new Label( getComposite(),SWT.LEFT|SWT.BORDER);
		imgLabel.setImage( Activator.getDefault().getImage( "grxrobot"+imgCnt+".png" ) );
	}

	public void control(List<GrxBaseItem> items) {
		imgCnt++;
		if( imgCnt > imgMax )
			imgCnt=1;
		imgLabel.setImage( Activator.getDefault().getImage( "grxrobot"+imgCnt+".png" ) );
	}
	
	public void itemSelectionChanged( List<GrxBaseItem> itemList )
	{
	}


	Grx3DView get3DView()
	{
		Grx3DView v = (Grx3DView)manager_.getView(Grx3DView.class);
		if( v == null )
			System.out.println("Grx3DView get Error");
		return v;
	}

	public void addPoint(int x, int y)
	{
		if( tdView == null ){
	        System.out.println("Grx3DView is null");
			return;
		}

		//Grx3DViewにクリック位置を表示する
		Point3d clicked = tdView.getClickPoint(x, y);
		if(clicked!=null){
			//オブジェクトの作成
			Appearance appea = new Appearance( );
			Material material = new Material( );
			Color3f color = new Color3f( 1.0f, 0, 0 );
			material.setDiffuseColor( color );
			appea.setMaterial( material );
			appea.setTransparencyAttributes(new TransparencyAttributes( TransparencyAttributes.NICEST,0.6f ) );
			Sphere sphere = new Sphere( 0.01f, Sphere.GENERATE_NORMALS, appea );
			//追加
			Transform3D rayTr = new Transform3D();
			rayTr.setTranslation( new Vector3d( clicked ) );
			BranchGroup bg = new BranchGroup();
			TransformGroup newtg = new TransformGroup( rayTr );
			bg.addChild( newtg );
			newtg.addChild( sphere );
			tdView.attachUnclickable( bg );
		}

		//クリックされたノードを取得する
        TransformGroup clickedTg = tdView.getClickNode( x,y,PickTool.TYPE_TRANSFORM_GROUP );
        if (clickedTg == null)
        	System.out.println("null TransformGroup\n");
        else {
	        Hashtable ht = SceneGraphModifier.getHashtableFromTG(clickedTg);
    	    if (ht == null) 
    	    {
    	    	System.out.println("hashTable can`t get");
    	    	return;
    	    }

			Transform3D target2vw = new Transform3D();
			Transform3D l2vw = new Transform3D();
			Transform3D tr = new Transform3D();
			clickedTg.getLocalToVworld(l2vw);
			((TransformGroup) clickedTg).getTransform(tr);
			target2vw.mul(l2vw, tr);

			Point3d target = new Point3d();
			target2vw.transform( target );

	        GrxModelItem model = (GrxModelItem)ht.get("object");
	        String objectName = (String)ht.get("objectName");
	        String linkName = (String)ht.get("jointName");
	        LinkInfoLocal l = (LinkInfoLocal)ht.get("linkInfo");
			TransformGroup tg = l.tg;

			NumberFormat format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(2);
			System.out.println("LINK="+objectName+":"+linkName+"("
					+format.format(target.x)+","
					+format.format(target.y)+","
					+format.format(target.z)+")" );
		}

	}
}

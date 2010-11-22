package com.generalrobotix.ui.view;

import java.awt.Color;
import java.io.IOException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.BodyInfo;
import jp.go.aist.hrp.simulator.DynamicsSimulator;
import jp.go.aist.hrp.simulator.PathPlanner;
import jp.go.aist.hrp.simulator.StringSequenceHolder;
import jp.go.aist.hrp.simulator.WorldStateHolder;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.LinkDataType;
import jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder;
import jp.go.aist.hrp.simulator.PathPlannerPackage.RoadmapHolder;
import jp.go.aist.hrp.simulator.PathPlannerPackage.RoadmapNode;
import jp.go.aist.rtm.RTC.Manager;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jp.go.aist.hrp.simulator.PathConsumerComp;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxPathPlanningAlgorithmItem;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.tdview.ObjectToolBar;
import com.sun.j3d.utils.geometry.Box;

@SuppressWarnings("serial") //$NON-NLS-1$
/**
 * @brief view for PathPlanner
 */
public class GrxPathPlanningView extends GrxBaseView {
    public static final String TITLE = "Path Planning"; //$NON-NLS-1$

	private Grx3DView view_ = null;

    // モデル
	private Combo modelSelect;
	
	// 移動アルゴリズム
	private Combo mobilitySelect;
	
	// 経路最適化アルゴリズム
	private Combo optimizerSelect;
	private Button optimizeButton;

	// 位置、角度の表示用
	private static final String FORMAT = "%.3f"; //$NON-NLS-1$
	private static final int TEXT_WIDTH = 64;
	// Start Position
	private Button btnSetStartPoint; 
	private Text textStartX, textStartY, textStartTheta;
	// End Position
	private Button btnSetEndPoint; 
	private Text textEndX, textEndY, textEndTheta;

	// 経路計画アルゴリズムとパラメータの選択
	private Combo algoSelect;
	private Button updatePropertyButton;
	
	// 計算開始、キャンセル
	private Button btnCalcStart, /*btnCalcCancel,*/btnVisible;
	private Label imgLabel;
	
	// 経路の高さ、可視性
	private Text carpetZ;
	
	// 干渉チェックのトレランス
	private Text tolerance_;
	
	private Button chkRebuildRoadmap_;
	
	// 経路表示
	private Switch switcher_;
	private Transform3D carpetTrans;
	private TransformGroup carpetTransGroup; 
	private BranchGroup carpet_;
	final double DEFAULT_CARPET_Z = 0.01;
	
	private GrxPathPlanningAlgorithmItem ppaItem_ = null;

	
	/**
	 * @brief constructor
	 * @param name
	 * @param manager
	 * @param vp
	 * @param parent
	 */
	public GrxPathPlanningView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);

		GridLayout gl = new GridLayout(1,false);
		getComposite().setLayout( gl );

	    initGUI();
		initJava3D();
		
		setUp();
		manager_.registerItemChangeListener(this, GrxPathPlanningAlgorithmItem.class);
	}

	public void setUp(){
		if(ppaItem_ != null)
			ppaItem_.deleteObserver(this);
		ppaItem_ = manager_.<GrxPathPlanningAlgorithmItem>getSelectedItem(GrxPathPlanningAlgorithmItem.class, null);
		if(ppaItem_!=null){
			update();
			setppaItem();
			ppaItem_.addObserver(this);
		}else
			setEnabled(false);
	}
	
	/**
	 * @brief
	 * @param grid
	 * @param style
	 * @return
	 */
	private Composite getComp(int grid, int style){
		Composite comp = new Composite( getComposite(), style );
		GridLayout layout = new GridLayout( grid, false );
		comp.setLayout( layout );
		comp.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		return comp;
	}
	
	/**
	 * @brief
	 */
	private void initGUI(){
	    //使いまわす
	    Composite comp;
		Label l; 
		GridData gd;

		//---- robot select
	
		comp = getComp( 5, SWT.NONE );

		l = new Label(comp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.model")); //$NON-NLS-1$
		modelSelect = new Combo( comp, SWT.NONE|SWT.READ_ONLY );
		modelSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		modelSelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("model", modelSelect.getItem(modelSelect.getSelectionIndex())); //$NON-NLS-1$
			}
		});

		l = new Label(comp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.mobility")); //$NON-NLS-1$
		mobilitySelect = new Combo( comp, SWT.NONE|SWT.READ_ONLY );
		mobilitySelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		mobilitySelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("mobility", mobilitySelect.getItem(mobilitySelect.getSelectionIndex())); //$NON-NLS-1$
			}
		});

		//---- start and goal
		
		comp = getComp( 8, SWT.NONE);

		btnSetStartPoint = new Button( comp, SWT.NONE );
		btnSetStartPoint.setText(MessageBundle.get("GrxPathPlanningView.button.start")); //$NON-NLS-1$
		btnSetStartPoint.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	getStartPoint();
			}
        });


		l = new Label( comp, SWT.NONE );
		l.setText("X:"); //$NON-NLS-1$
		textStartX = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textStartX.setLayoutData( gd );
		textStartX.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				Double d = Double.parseDouble(textStartX.getText());
				if (d != null){
					ppaItem_.setDbl("startX", d); //$NON-NLS-1$
				}
			}
		});

		l = new Label( comp, SWT.NONE );
		l.setText("Y:"); //$NON-NLS-1$
		textStartY = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textStartY.setLayoutData( gd );
		textStartY.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				Double d = Double.parseDouble(textStartY.getText());
				if (d != null){
					ppaItem_.setDbl("startY", d); //$NON-NLS-1$
				}
			}
		});


		l = new Label( comp, SWT.NONE );
		l.setText("Yaw:"); //$NON-NLS-1$
		textStartTheta = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textStartTheta.setLayoutData( gd );
		textStartTheta.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				Double d = Double.parseDouble(textStartTheta.getText());
				if (d != null){
					ppaItem_.setDbl("startTheta", d); //$NON-NLS-1$
				}
			}
		});


		Button setStart = new Button( comp, SWT.NONE );
		setStart.setText(MessageBundle.get("GrxPathPlanningView.button.set")); //$NON-NLS-1$
		setStart.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	ppaItem_.setStartPoint();
			}
        });

		
		btnSetEndPoint = new Button( comp, SWT.NONE );
		btnSetEndPoint.setText(MessageBundle.get("GrxPathPlanningView.button.end")); //$NON-NLS-1$
		btnSetEndPoint.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	getEndPoint();
			}
        });

		l = new Label( comp, SWT.NONE );
		l.setText("X:"); //$NON-NLS-1$
		textEndX = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textEndX.setLayoutData( gd );
		textEndX.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				Double d = Double.parseDouble(textEndX.getText());
				if (d != null){
					ppaItem_.setDbl("goalX", d); //$NON-NLS-1$
				}
			}
		});



		l = new Label( comp, SWT.NONE );
		l.setText("Y:"); //$NON-NLS-1$
		textEndY = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textEndY.setLayoutData( gd );
		textEndY.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				Double d = Double.parseDouble(textEndY.getText());
				if (d != null){
					ppaItem_.setDbl("goalY", d); //$NON-NLS-1$
				}
			}
		});

		l = new Label( comp, SWT.NONE );
		l.setText("Yaw:"); //$NON-NLS-1$
		textEndTheta = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textEndTheta.setLayoutData( gd );
		textEndTheta.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				Double d = Double.parseDouble(textEndTheta.getText());
				if (d != null){
					ppaItem_.setDbl("goalTheta", d); //$NON-NLS-1$
				}
			}
		});

		Button setEnd = new Button( comp, SWT.NONE );
		setEnd.setText(MessageBundle.get("GrxPathPlanningView.button.set")); //$NON-NLS-1$
		setEnd.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	ppaItem_.setEndPoint();
			}
        });

		// ---- robot Z position
		Composite roboZComp = getComp( 4, SWT.NONE );

		l = new Label(roboZComp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.collision")); //$NON-NLS-1$
		
		tolerance_ = new Text(roboZComp, SWT.SINGLE | SWT.BORDER);
		tolerance_.setText("0"); //$NON-NLS-1$
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		tolerance_.setLayoutData( gd );
		tolerance_.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e) {
			}
			public void focusLost(FocusEvent e) {
				Double d = Double.parseDouble(tolerance_.getText());
				if (d != null){
					ppaItem_.setDbl("tolerance", d); //$NON-NLS-1$
				}
			}
		});
		
	    chkRebuildRoadmap_ = new Button(roboZComp,SWT.CHECK);
	    chkRebuildRoadmap_.setText(MessageBundle.get("GrxPathPlanningView.button.rebuild")); //$NON-NLS-1$
		chkRebuildRoadmap_.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				if (chkRebuildRoadmap_.getSelection()){
					ppaItem_.setProperty("rebuildRoadmap", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				}else{
					ppaItem_.setProperty("rebuildRoadmap", "false"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		});
	    chkRebuildRoadmap_.setSelection(true);

		
		//---- algorithm
	
		Composite algoComp = getComp( 3, SWT.NONE );

		l = new Label(algoComp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.algorithm")); //$NON-NLS-1$
		algoSelect = new Combo( algoComp, SWT.READ_ONLY );
		algoSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		algoSelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("algorithm", algoSelect.getItem(algoSelect.getSelectionIndex())); //$NON-NLS-1$
			}
		});

		updatePropertyButton = new Button(algoComp, SWT.NONE);
		updatePropertyButton.setText(MessageBundle.get("GrxPathPlanningView.button.getProperties")); //$NON-NLS-1$
		updatePropertyButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	ppaItem_.propertyUpdate();
			}
        });
		
		//---- calculate

		Composite calcComp = getComp( 5, SWT.NONE );

		btnCalcStart = new Button( calcComp, SWT.NONE );
		btnCalcStart.setText(MessageBundle.get("GrxPathPlanningView.button.calcStart")); //$NON-NLS-1$
		btnCalcStart.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	removeCarpet();
            	ppaItem_.startCalc();
            	displayCarpet();
            	pathGraph();
			}
        });

		imgLabel = new Label(calcComp, SWT.BORDER);
		imgLabel.setImage( Activator.getDefault().getImage("grxrobot1.png" ) ); //$NON-NLS-1$

		
		l = new Label(calcComp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.optimizer")); //$NON-NLS-1$
		optimizerSelect = new Combo( calcComp, SWT.NONE|SWT.READ_ONLY );
		optimizerSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		optimizerSelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("optimizer", optimizerSelect.getItem(optimizerSelect.getSelectionIndex())); //$NON-NLS-1$
			}
		});

		optimizeButton = new Button( calcComp, SWT.NONE );
		optimizeButton.setText(MessageBundle.get("GrxPathPlanningView.button.optimize")); //$NON-NLS-1$
		optimizeButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	ppaItem_.optimize();
        		removeCarpet();
        		displayCarpet();
            	pathGraph();
			}
        });


		// ---- Route View
		Composite carpetComp = getComp( 5, SWT.NONE );

		l = new Label(carpetComp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.path")); //$NON-NLS-1$
		
		btnVisible = new Button( carpetComp, SWT.TOGGLE );
		btnVisible.setText(MessageBundle.get("GrxPathPlanningView.button.visible")); //$NON-NLS-1$
		btnVisible.setSelection(true);
		btnVisible.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
               	boolean selection = btnVisible.getSelection();
               	if (selection) {
               		System.out.println("TRUE"); //$NON-NLS-1$
               		switcher_.setWhichChild(0);
               	} else {
               		System.out.println("FALSE"); //$NON-NLS-1$
               		switcher_.setWhichChild(Switch.CHILD_NONE);
               	}
               	
			}
        });

 		l = new Label(carpetComp, SWT.NONE);
		l.setText(MessageBundle.get("GrxPathPlanningView.label.carpetZ")); //$NON-NLS-1$
		
		carpetZ = new Text(carpetComp, SWT.SINGLE | SWT.BORDER );
		carpetZ.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		carpetZ.setText( String.valueOf(DEFAULT_CARPET_Z) );
        carpetZ.addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.CR) {
                	updateCarpet();
                }
            }
        });
		carpetZ.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e) {
			}
			public void focusLost(FocusEvent e) {
				Double d = Double.parseDouble(carpetZ.getText());
				if (d != null){
					ppaItem_.setDbl("carpetZ", d); //$NON-NLS-1$
				}
			}
		});

        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
	}

	
	private void initJava3D(){
		carpet_ = new BranchGroup();
		carpet_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		carpet_.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		carpet_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		carpet_.setCapability(BranchGroup.ALLOW_DETACH); //親(carpetTransGroup)から離れられるようにする
	
		carpetTrans = new Transform3D();
		carpetTrans.setTranslation( new Vector3d(0,0, getCarpetZPosition()) );
	
		carpetTransGroup = new TransformGroup();
		carpetTransGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
		carpetTransGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
		carpetTransGroup.setCapability( Group.ALLOW_CHILDREN_WRITE);
		carpetTransGroup.setCapability( Group.ALLOW_CHILDREN_EXTEND);
		carpetTransGroup.setTransform( carpetTrans );
		carpetTransGroup.addChild( carpet_ );
	
		switcher_ = new Switch();
	    switcher_.setCapability(Switch.ALLOW_SWITCH_READ);
	    switcher_.setCapability(Switch.ALLOW_SWITCH_WRITE);
	    switcher_.addChild( carpetTransGroup );
	    switcher_.setWhichChild(0);
	}

	
	/**
	 * @brief 移動動作設計コンポーネントから経路計画アルゴリズム名、移動アルゴリズム名、経路最適化アルゴリズム名を取得する
	 */
	private void update(){
	    String[] algoNames = ppaItem_.getAlgorithms();
	    algoSelect.removeAll();
		for( String s: algoNames )
			algoSelect.add( s );
	
		// mobilities
	    String[] mobilityNames = ppaItem_.getMobilityNames();
	    mobilitySelect.removeAll();
		for( String s: mobilityNames )
			mobilitySelect.add( s );
		
		// optimizers
	    String[] optimizerNames = ppaItem_.getOptimizerNames();
	    optimizerSelect.removeAll();
		for( String s: optimizerNames )
			optimizerSelect.add( s );
		
		if (ppaItem_ != null){
			String algorithmName = ppaItem_.getStr("algorithm", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (algoSelect.indexOf(algorithmName) >= 0){
				algoSelect.select(algoSelect.indexOf(algorithmName));
			}
			String mobilityName = ppaItem_.getStr("mobility", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (mobilitySelect.indexOf(mobilityName) >= 0){
				mobilitySelect.select(mobilitySelect.indexOf(mobilityName));
			}
			String optimizerName = ppaItem_.getStr("optimizer", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (optimizerSelect.indexOf(optimizerName) >= 0){
				optimizerSelect.select(optimizerSelect.indexOf(optimizerName));
			}
		}
	}
	
	/**
	 * @brief カーペットを削除
	 */
	private void removeCarpet() {
		try{
    		carpetTransGroup.removeChild( carpet_ );
    		carpet_.removeAllChildren();
    		carpetTransGroup.addChild( carpet_ );
		}catch( Exception e ){
			e.printStackTrace();
		}
	}

	private String getRobotBaseLink( GrxModelItem model ) {
		return model.rootLink().getName();
	}

	private void getStartPoint(){
		String robotName = modelSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		if( robot == null )
			return;
		double [] tr = robot.getDblAry(getRobotBaseLink(robot)+".translation",null); //$NON-NLS-1$
		double [] rot = robot.getDblAry(getRobotBaseLink(robot)+".rotation",null); //$NON-NLS-1$
		try{
			textStartX.setText( String.format( FORMAT, tr[0]) );
			textStartY.setText( String.format( FORMAT, tr[1]) );
			textStartTheta.setText( String.format( FORMAT, rot[3] / (2f*Math.PI) * 360f) );
			if (ppaItem_ != null){
				ppaItem_.setDbl("startX", tr[0]); //$NON-NLS-1$
				ppaItem_.setDbl("startY", tr[1]); //$NON-NLS-1$
				ppaItem_.setDbl("startTheta", rot[3]); //$NON-NLS-1$
			}
		}catch(Exception e){
			return;
		}
	}

	private void getEndPoint(){
		String robotName = modelSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		if( robot == null )
			return;
		double [] tr = robot.getDblAry(getRobotBaseLink(robot)+".translation",null); //$NON-NLS-1$
		double [] rot = robot.getDblAry(getRobotBaseLink(robot)+".rotation",null); //$NON-NLS-1$
		try{
			textEndX.setText( String.format( FORMAT, tr[0]) );
			textEndY.setText( String.format( FORMAT, tr[1]) );
			textEndTheta.setText( String.format( FORMAT, rot[3] / (2f*Math.PI) * 360f) );
			if (ppaItem_ != null){
				ppaItem_.setDbl("goalX", tr[0]); //$NON-NLS-1$
				ppaItem_.setDbl("goalY", tr[1]); //$NON-NLS-1$
				ppaItem_.setDbl("goalTheta", rot[3]); //$NON-NLS-1$
			}
		}catch(Exception e){
			return;
		}
	}

	Grx3DView get3DView()
	{
		if( view_ == null )
			view_ = (Grx3DView)manager_.getView(Grx3DView.class, false);
		return view_;
	}

	private void displayCarpet(){
    	double[][] path = ppaItem_.getPath();
		for( int i=0; i+1<path.length; i++ ) {
    		double []p1 = path[i], p2 = path[i+1];
    		carpet( p1, p2 );
    	}
	}
	
	/**
	 * 経路の一部分を表示する.
	 * @param start 始点
	 * @param goal 終点
	 */
	private void carpet( double[] start, double[] goal ){
		double carpetHeight=0.01;
		double dx = goal[0] - start[0], dy = goal[1] - start[1], len = Math.sqrt( dx/2*dx/2+dy/2*dy/2 );
		double theta;
		if (len < 0.001){
			len = 0.01;
			theta = goal[2];
		}else{
			theta = Math.atan2( dy,dx ); 
		}
		//Point3d destPoint = new Point3d( x2+dx/2, y2+dy/2, getZPosition()+carpetHeight );
		Point3d centerPoint = new Point3d( (start[0]+goal[0])/2, (start[1]+goal[1])/2, 0 );

		// APPEARANCE
		Appearance appea = new Appearance( );
		Material material = new Material( );
		Color3f color = new Color3f( 0, 1f, 0 );
		material.setDiffuseColor( color );
		appea.setMaterial( material );
		appea.setTransparencyAttributes(new TransparencyAttributes( TransparencyAttributes.NICEST,0.7f ) );
		// carpet block
		Box box = new Box( (float)len, 0.1f, (float) carpetHeight, appea );
		// 移動回転
		Transform3D tr = new Transform3D();
		tr.setTranslation( new Vector3d( centerPoint ) );
		Transform3D rot = new Transform3D();
		rot.rotZ( theta );
		tr.mul(rot);
		// ノードの追加
		BranchGroup bg = new BranchGroup();
		TransformGroup newtg = new TransformGroup( tr );
		bg.addChild( newtg );
		newtg.addChild( box );
		carpet_.addChild( bg );
	}


    
	/**
	 * 経路グラフを表示する
	 */
    private void pathGraph(){
		if ( view_ == null ){
			view_ = get3DView();
			if( view_ != null ){
				BranchGroup bg = new BranchGroup();
			    bg.addChild( switcher_ );
			    view_.attachUnclickable( bg );
			}else{	
			    GrxDebugUtil.println("[GrxPathPlanningView] can't find 3DView"); //$NON-NLS-1$
			}
		}

    	RoadmapNode[] tree = ppaItem_.getRoadmap();
    	
        Vector<Point3d> vertex = new Vector<Point3d>();

        // LineArrayで使うためにvectorへ放り込む
        for( RoadmapNode n : tree ){
        	Point3d start = new Point3d( n.cfg[0], n.cfg[1], 0 ); //三つめはtheta
        	for( int l : n.neighbors ){
        		// 一方向リンクなのでダブりは考えなくてよし
            	Point3d to = new Point3d(tree[l].cfg[0],tree[l].cfg[1],0);
            	vertex.add( start );
        		vertex.add( to );
        		//GrxDebugUtil.println("[PPV]@pathGraph start"+start+" to "+to );
        	}
        }

        GrxDebugUtil.println("[PPV]@pathGraph size="+vertex.size()); //$NON-NLS-1$
        if (vertex.size() == 0) return;
        LineArray geometry = new LineArray(vertex.size(), GeometryArray.COORDINATES | GeometryArray.COLOR_3);
        geometry.setCoordinates(0, vertex.toArray(new Point3d[vertex.size()]) );
        for( int i=0; i<vertex.size(); i++)
        	geometry.setColor(i, new Color3f(Color.red));

		// ノードの追加
        Shape3D graph = new Shape3D(geometry);
        graph.setPickable(false);
		// 移動回転
		Transform3D tr = new Transform3D();
		tr.setTranslation( new Vector3d( 0,0,0 ) );
		BranchGroup bg = new BranchGroup();
		TransformGroup newtg = new TransformGroup( tr );
		bg.addChild( newtg );
		newtg.addChild( graph );

		carpet_.addChild( bg );
        
        return;
    }
    
    
    public void registerItemChange(GrxBaseItem item, int event){
    	if(item instanceof GrxPathPlanningAlgorithmItem){
    		GrxPathPlanningAlgorithmItem ppa = (GrxPathPlanningAlgorithmItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(ppaItem_!= ppa){
    				ppaItem_ = ppa;
    				ppaItem_.addObserver(this);
    				update();
    				setppaItem();
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
                removeCarpet();
    		case GrxPluginManager.NOTSELECTED_ITEM:
    			if(ppaItem_==ppa){
    				ppaItem_.deleteObserver(this);
    				ppaItem_ = null;
    				setEnabled(false);
    			}
    			break;
    		default:
    			break;
    		}
    	}
    }
    
    private void setppaItem(){
    	setEnabled(true);
		String modelName = null;
		if (ppaItem_ != null){
			modelName = ppaItem_.getStr("model", null); //$NON-NLS-1$
		}else{
			modelName = modelSelect.getText();
		}
		
		Object[] items = manager_.getSelectedItemList( GrxModelItem.class ).toArray();
		modelSelect.removeAll();
		for( Object o: items ){
			GrxModelItem model = (GrxModelItem)o;
			if (model.isRobot()){
				modelSelect.add( o.toString() );
			}
		}
		if (modelName != null && modelSelect.indexOf(modelName) >= 0){
			modelSelect.select(modelSelect.indexOf(modelName));
		}
		
		if (ppaItem_ != null){
			String mobilityName = ppaItem_.getStr("mobility", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (mobilitySelect.indexOf(mobilityName) >= 0){
				mobilitySelect.select(mobilitySelect.indexOf(mobilityName));
			}else{
				mobilitySelect.deselectAll();
			}
			String optimizerName = ppaItem_.getStr("optimizer", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (optimizerSelect.indexOf(optimizerName) >= 0){
				optimizerSelect.select(optimizerSelect.indexOf(optimizerName));
			}else{
				optimizerSelect.deselectAll();
			}
			String algorithmName = ppaItem_.getStr("algorithm", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (algoSelect.indexOf(algorithmName) >= 0){
				algoSelect.select(algoSelect.indexOf(algorithmName));
			}else{
				algoSelect.deselectAll();
			}
			Double sx = ppaItem_.getDbl("startX", null); //$NON-NLS-1$
			if (sx != null){
				textStartX.setText(sx.toString());
			}else{
				textStartX.setText(""); //$NON-NLS-1$
			}
			Double sy = ppaItem_.getDbl("startY", null); //$NON-NLS-1$
			if (sy != null){
				textStartY.setText(sy.toString());
			}else{
				textStartY.setText(""); //$NON-NLS-1$
			}
			Double st = ppaItem_.getDbl("startTheta", null); //$NON-NLS-1$
			if (st != null){
				textStartTheta.setText(st.toString());
			}else{
				textStartTheta.setText(""); //$NON-NLS-1$
			}
			Double ex = ppaItem_.getDbl("goalX", null); //$NON-NLS-1$
			if (ex != null){
				textEndX.setText(ex.toString());
			}else{
				textEndX.setText(""); //$NON-NLS-1$
			}
			Double ey = ppaItem_.getDbl("goalY", null); //$NON-NLS-1$
			if (ey != null){
				textEndY.setText(ey.toString());
			}else{
				textEndY.setText(""); //$NON-NLS-1$
			}
			Double et = ppaItem_.getDbl("goalTheta", null); //$NON-NLS-1$
			if (et != null){
				textEndTheta.setText(et.toString());
			}else{
				textEndTheta.setText(""); //$NON-NLS-1$
			}
			chkRebuildRoadmap_.setSelection(ppaItem_.isTrue("rebuildRoadmap", true)); //$NON-NLS-1$
			tolerance_.setText(ppaItem_.getDbl("tolerance",0.0).toString()); //$NON-NLS-1$
			carpetZ.setText(ppaItem_.getDbl("carpetZ",0.01).toString()); //$NON-NLS-1$
		}
		
	}

	/**
	 * @brief get carpet z position
	 * @return z position
	 */
	double getCarpetZPosition(){
		double z;
		try{
			z= Double.valueOf( carpetZ.getText() );
		}catch(Exception e){
			z=0.2;
			e.printStackTrace();
		}
		return z;
	}

	/**
	 * @brief update carpet z position
	 *
	 */
	void updateCarpet(){
		carpetTrans.setTranslation( new Vector3d(0,0, getCarpetZPosition() ) );
		carpetTransGroup.setTransform( carpetTrans );
	}
	
	/**
	 * enable/disable this view
	 * @param b true to enable, false otherwise
	 */
	public void setEnabled(boolean b){
		Control[] ctl = getComposite().getChildren();
		for (int i=0; i<ctl.length; i++){
			ctl[i].setEnabled(b);
			if (ctl[i] instanceof Composite){
				Composite cmp = (Composite)ctl[i];
				Control[] ctl2 = cmp.getChildren();
				for (int j=0; j<ctl2.length; j++){
					ctl2[j].setEnabled(b);
				}
			}
		}
	}
	
	public void update(GrxBasePlugin plugin, Object... arg) {
		if(ppaItem_==plugin){
			if(((String)arg[0]).equals("connected")){ //$NON-NLS-1$
				update();
			}
		}
	}

	public void shutdown() {
        manager_.removeItemChangeListener(this, GrxPathPlanningAlgorithmItem.class);
        if(ppaItem_!=null)
        	ppaItem_.deleteObserver(this);
	}
}

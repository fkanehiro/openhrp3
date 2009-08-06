package com.generalrobotix.ui.view;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
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
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxPathPlanningAlgorithmItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.sun.j3d.utils.geometry.Box;

@SuppressWarnings("serial")
/**
 * @brief view for PathPlanner
 */
public class GrxPathPlanningView extends GrxBaseView {
    public static final String TITLE = "Path Planning";

	private Grx3DView view_ = null;
	boolean calcSucceed = false;

	// 経路計画コンポーネント
	private PathPlanner planner_ = null;
	PathConsumerComp ppcomp_ = null;
	private PointArrayHolder path_ = new PointArrayHolder();

    // モデル
	private Combo modelSelect;
	
	// 移動アルゴリズム
	private Combo mobilitySelect;
	
	// 経路最適化アルゴリズム
	private Combo optimizerSelect;
	private Button optimizeButton;

	// 位置、角度の表示用
	private static final String FORMAT = "%.3f";
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
	private StringSequenceHolder propertyNames_ = new StringSequenceHolder();
	private StringSequenceHolder propertyDefaults_ = new StringSequenceHolder();
	
	// 計算開始、キャンセル
	private Button btnCalcStart, /*btnCalcCancel,*/btnVisible;
	private boolean isCalculating_ = false;
	private int imgCnt=1,imgMax=13;
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
	
	private Thread thread_;
	
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
		setEnabled(false);
		
		manager_.registerItemChangeListener(this, GrxPathPlanningAlgorithmItem.class);
		
		thread_ = new Thread(){
			public void run(){
				while(true){
					PathPlanner planner_old = planner_;
					planner_ = _pathConsumer().getImpl();
					if (planner_old == null && planner_ != null){
						syncExec(new Runnable(){
							public void run(){
								update();
							}
						});
						System.out.println("[PPView] update() is called.");
					}
					if( isCalculating_ ) {
						imgCnt++;
						if( imgCnt > imgMax )
							imgCnt=1;
						//imgLabel.setImage( Activator.getDefault().getImage("grxrobot"+imgCnt+".png" ) );
					}else{
						//imgLabel.setImage( null );
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread_.start();
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
	
	private PathConsumerComp _pathConsumer(){
		if (ppcomp_ == null){
			execPathPlannerConsumer();
		}
		return ppcomp_;
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
		l.setText("Model:");
		modelSelect = new Combo( comp, SWT.NONE|SWT.READ_ONLY );
		modelSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		modelSelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("model", modelSelect.getItem(modelSelect.getSelectionIndex()));
			}
		});

		l = new Label(comp, SWT.NONE);
		l.setText("Mobility:");
		mobilitySelect = new Combo( comp, SWT.NONE|SWT.READ_ONLY );
		mobilitySelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		mobilitySelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("mobility", mobilitySelect.getItem(mobilitySelect.getSelectionIndex()));
			}
		});

		//---- start and goal
		
		comp = getComp( 8, SWT.NONE);

		btnSetStartPoint = new Button( comp, SWT.NONE );
		btnSetStartPoint.setText("START");
		btnSetStartPoint.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	getStartPoint();
			}
        });


		l = new Label( comp, SWT.NONE );
		l.setText("X:");
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
					ppaItem_.setDbl("startX", d);
				}
			}
		});

		l = new Label( comp, SWT.NONE );
		l.setText("Y:");
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
					ppaItem_.setDbl("startY", d);
				}
			}
		});


		l = new Label( comp, SWT.NONE );
		l.setText("Yaw:");
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
					ppaItem_.setDbl("startTheta", d);
				}
			}
		});


		Button setStart = new Button( comp, SWT.NONE );
		setStart.setText("SET");
		setStart.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	setStartPoint();
			}
        });

		
		btnSetEndPoint = new Button( comp, SWT.NONE );
		btnSetEndPoint.setText("END");
		btnSetEndPoint.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	getEndPoint();
			}
        });

		l = new Label( comp, SWT.NONE );
		l.setText("X:");
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
					ppaItem_.setDbl("goalX", d);
				}
			}
		});



		l = new Label( comp, SWT.NONE );
		l.setText("Y:");
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
					ppaItem_.setDbl("goalY", d);
				}
			}
		});

		l = new Label( comp, SWT.NONE );
		l.setText("Yaw:");
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
					ppaItem_.setDbl("goalTheta", d);
				}
			}
		});

		Button setEnd = new Button( comp, SWT.NONE );
		setEnd.setText("SET");
		setEnd.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	setEndPoint();
			}
        });

		// ---- robot Z position
		Composite roboZComp = getComp( 4, SWT.NONE );

		l = new Label(roboZComp, SWT.NONE);
		l.setText("Collision detection tolerance:");
		
		tolerance_ = new Text(roboZComp, SWT.SINGLE | SWT.BORDER);
		tolerance_.setText("0");
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		tolerance_.setLayoutData( gd );
		tolerance_.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e) {
			}
			public void focusLost(FocusEvent e) {
				Double d = Double.parseDouble(tolerance_.getText());
				if (d != null){
					ppaItem_.setDbl("tolerance", d);
				}
			}
		});
		
	    chkRebuildRoadmap_ = new Button(roboZComp,SWT.CHECK);
	    chkRebuildRoadmap_.setText("Rebuild Roadmap");
		chkRebuildRoadmap_.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				if (chkRebuildRoadmap_.getSelection()){
					ppaItem_.setProperty("rebuildRoadmap", "true");
				}else{
					ppaItem_.setProperty("rebuildRoadmap", "false");
				}
			}
		});
	    chkRebuildRoadmap_.setSelection(true);

		
		//---- algorithm
	
		Composite algoComp = getComp( 3, SWT.NONE );

		l = new Label(algoComp, SWT.NONE);
		l.setText("Algorithm:");
		algoSelect = new Combo( algoComp, SWT.READ_ONLY );
		algoSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		algoSelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("algorithm", algoSelect.getItem(algoSelect.getSelectionIndex()));
			}
		});

		updatePropertyButton = new Button(algoComp, SWT.NONE);
		updatePropertyButton.setText("Get Properties");
		updatePropertyButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	propertyUpdate();
			}
        });
		
		//---- calculate

		Composite calcComp = getComp( 5, SWT.NONE );

		btnCalcStart = new Button( calcComp, SWT.NONE );
		btnCalcStart.setText("CALC START");
		btnCalcStart.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	startCalc();
			}
        });

		imgLabel = new Label(calcComp, SWT.BORDER);
		imgLabel.setImage( Activator.getDefault().getImage("grxrobot1.png" ) );

		
		l = new Label(calcComp, SWT.NONE);
		l.setText("Optimizer:");
		optimizerSelect = new Combo( calcComp, SWT.NONE|SWT.READ_ONLY );
		optimizerSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		optimizerSelect.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				ppaItem_.setProperty("optimizer", optimizerSelect.getItem(optimizerSelect.getSelectionIndex()));
			}
		});

		optimizeButton = new Button( calcComp, SWT.NONE );
		optimizeButton.setText("Optimize");
		optimizeButton.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	optimize();
			}
        });


		// ---- Route View
		Composite carpetComp = getComp( 5, SWT.NONE );

		l = new Label(carpetComp, SWT.NONE);
		l.setText("Path Visible:");
		
		btnVisible = new Button( carpetComp, SWT.TOGGLE );
		btnVisible.setText("Visible");
		btnVisible.setSelection(true);
		btnVisible.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
               	boolean selection = btnVisible.getSelection();
               	if (selection) {
               		System.out.println("TRUE");
               		switcher_.setWhichChild(0);
               	} else {
               		System.out.println("FALSE");
               		switcher_.setWhichChild(Switch.CHILD_NONE);
               	}
               	
			}
        });

 		l = new Label(carpetComp, SWT.NONE);
		l.setText("Carpet Z Position:");
		
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
					ppaItem_.setDbl("carpetZ", d);
				}
			}
		});

        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
	}

	/**
	 * @brief 経路を最適化する
	 */
	private void optimize() {
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "PathPlanner Component is not connected.");
	    	return;
	    }
	    
		String optimizer = optimizerSelect.getText();
		if( optimizer == null || optimizer.equals("") ){
			MessageDialog.openInformation(getParent().getShell(),"", "Optimizer is not selected.");
			return;
		}

	    planner_.optimize(optimizer);
		removeCarpet();
	    displayPath();
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
	 * @brief 移動動作設計コンポーネントから現在選択している経路計画アルゴリズムに対するプロパティを取得し、選択しているアイテムに設定する
	 */
	private void propertyUpdate(){
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "PathPlanner Component is not connected.");
	    	return;
	    }

		planner_.getProperties( algoSelect.getText(), propertyNames_, propertyDefaults_ );
		String[] props = propertyNames_.value;
		String[] defaults = propertyDefaults_.value;
		
		//ppa.clear(); // プロパティを全て破棄
		for( int i=0; i<props.length; i++ )
			ppaItem_.setProperty( props[i], defaults[i] );
	}
	
	/**
	 * @brief 移動動作設計コンポーネントから経路計画アルゴリズム名、移動アルゴリズム名、経路最適化アルゴリズム名を取得する
	 */
	private void update(){
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "PathPlanner Component is not connected.");
	    	return;
	    }
	    
		StringSequenceHolder names = new StringSequenceHolder();

	    // planning algorithms
	    planner_.getAlgorithmNames( names );
	    String[] algoNames = names.value;
	    algoSelect.removeAll();
		for( String s: algoNames )
			algoSelect.add( s );
	
		// mobilities
	    planner_.getMobilityNames( names );
	    String[] mobilityNames = names.value;
	    mobilitySelect.removeAll();
		for( String s: mobilityNames )
			mobilitySelect.add( s );
		
		// optimizers
	    planner_.getOptimizerNames( names );
	    String[] optimizerNames = names.value;
	    optimizerSelect.removeAll();
		for( String s: optimizerNames )
			optimizerSelect.add( s );
		
		if (ppaItem_ != null){
			String algorithmName = ppaItem_.getStr("algorithm", "");
			if (algoSelect.indexOf(algorithmName) >= 0){
				algoSelect.select(algoSelect.indexOf(algorithmName));
			}
			String mobilityName = ppaItem_.getStr("mobility", "");
			if (mobilitySelect.indexOf(mobilityName) >= 0){
				mobilitySelect.select(mobilitySelect.indexOf(mobilityName));
			}
			String optimizerName = ppaItem_.getStr("optimizer", "");
			if (optimizerSelect.indexOf(optimizerName) >= 0){
				optimizerSelect.select(optimizerSelect.indexOf(optimizerName));
			}
		}
	}
	
	private void startCalc(){
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "PathPlanner Component is not connected.");
	    	return;
	    }

    	isCalculating_ = true;
    	if ( chkRebuildRoadmap_.getSelection()){
    		if (!initPathPlanner()){
    			isCalculating_ = false;
    			return;
    		}
    	}
    	_setStartPosition();
    	_setGoalPosition();
    	
    	//ロボットをスタート位置に移動
    	setStartPoint();
    	// 前回のカーペットを削除する
    	removeCarpet();
    	calc();
    	isCalculating_ = false;
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

	private void calc(){
		GrxDebugUtil.println("[GrxPathPlanner]@calc" );
		calcSucceed = false;
		IRunnableWithProgress runnableProgress = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException {
				Thread calcThread = new Thread( new Runnable(){
					public void run(){
				    	calcSucceed = planner_.calcPath();
					}
				});
				GrxDebugUtil.println("[GrxPathPlanner]@calc Thread Start" );
				calcThread.start();
				monitor.beginTask("Planning a path. Please Wait.", IProgressMonitor.UNKNOWN );
				while( calcThread.isAlive() ){
					Thread.sleep(200);
					GrxDebugUtil.print("." );
					if( monitor.isCanceled() ){
						GrxDebugUtil.println("[GrxPathPlanner]@calc Cancel" );
						planner_.stopPlanning();
						break;
					}
				}
				monitor.done();
			}
		};
		ProgressMonitorDialog progressMonitorDlg = new ProgressMonitorDialog( getComposite().getShell());
		try {
			progressMonitorDlg.run(true,true, runnableProgress);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		if( calcSucceed == false ){
			MessageDialog.openInformation( getParent().getShell(), "Path planning is canceled", "Path planning is canceled.");
		}
		
		displayPath();
	}

	private void displayPath() {
    	// path[N][0]=X path[N][1]=Y path[N][2]=Theta
    	planner_.getPath( path_ );
		GrxDebugUtil.println("[GrxPathPlanningView] Path length="+path_.value.length );

    	// TODO:DynamicsSimulatorのdefault simulation time 20sec オーバすると落ちる
    	double dt = 10.0 / path_.value.length, nowTime = 0;
    	GrxWorldStateItem currentWorld_ = (GrxWorldStateItem)manager_.getItem( GrxWorldStateItem.class, null );
    	if( currentWorld_ == null ) {
    		GrxDebugUtil.printErr("[GrxPathPlanningView] There is no World.");
    		return;
    	}
    	currentWorld_.clearLog();
    	currentWorld_.setDbl("logTimeStep", dt);
		DynamicsSimulator dynSim = getDynamicsSimulator();
		if( dynSim == null ){
    		GrxDebugUtil.printErr("[GrxPathPlanningView] Faild to get DynamicsSimulator.");
    		return;
		}

    	// register robot
		String model = modelSelect.getText();
		String base = getRobotBaseLink(model);
	    WorldStateHolder stateH_ = new WorldStateHolder();
    	for( double p[] : path_.value ) {
    		//System.out.println("[GrxPathPlanner] x="+p[0]+" y="+p[1]+" theta="+p[2] );
    		double newPos[] = new double[12];
    		newPos[0] = p[0];            newPos[1] = p[1];            newPos[2] = getZPosition();
    		newPos[0 + 3] = Math.cos( p[2] ); newPos[1 + 3] = -Math.sin( p[2] ); newPos[2 + 3] = 0.0;
    		newPos[3 + 3] = Math.sin( p[2] ); newPos[4 + 3] =  Math.cos( p[2] ); newPos[5 + 3] = 0.0;
    		newPos[6 + 3] = 0.0;         newPos[7 + 3] = 0.0;         newPos[8 + 3] = 1.0;

    		dynSim.setCharacterLinkData( model, base, LinkDataType.ABS_TRANSFORM, newPos);

    		dynSim.calcWorldForwardKinematics();

    	    // 結果を得る
    		dynSim.getWorldState(stateH_);
			WorldStateEx wsx = new WorldStateEx(stateH_.value);

			wsx.time = nowTime;
            currentWorld_.addValue(nowTime, wsx);
			nowTime += dt;
    	}
    	System.out.println("worldstate.getLogSize() = "+currentWorld_.getLogSize());
    	currentWorld_.setPosition(currentWorld_.getLogSize()-1);

    	// make carpet
    	for( int i=0; i+1<path_.value.length; i++ ) {
    		double []p1 = path_.value[i], p2 = path_.value[i+1];
    		carpet( p1, p2 );
    	}
    	// make graph
    	pathGraph();
	}

	// 経路計画サーバへ渡すためDynamicsSimulatorを取得
	DynamicsSimulator getDynamicsSimulator(){
		GrxOpenHRPView hrpView = (GrxOpenHRPView)manager_.getView( GrxOpenHRPView.class );
		if( hrpView == null ) {
			MessageDialog.openInformation(getParent().getShell(),"", "Failed to get OpenHRPView.");
			return null;
		}
		if( ! hrpView.initDynamicsSimulator() ){
			MessageDialog.openInformation(getParent().getShell(),"", "Failed to initialize DynamicsSimulator.");
			return null;
		}
		return hrpView.getDynamicsSimulator(false);
	}
	
	/**
	 * コンフィグファイルの探索
	 * @return コンフィグファイルのパス
	 */
	private String getConfigFilePath(){
		String confPath="";
		URL defaultConfURL = FileLocator.find( Activator.getDefault().getBundle(), new Path("rtc.conf"), null);
		try {
			confPath = FileLocator.resolve(defaultConfURL).getPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	GrxDebugUtil.println("[GrxPathPlanner] default Config File path="+confPath);
    	// From Property ( if exist )
		String confPathFromProperty = System.getProperty("com.generalrobotix.grxui.rtcpath");
		if( confPathFromProperty != null ) {
			confPath = confPathFromProperty;
	    	GrxDebugUtil.println("[GrxPathPlanner] Config File path from Property="+confPath);
		}

		return confPath;
	}
	
	/**
	 *  @brief 経路計画コンポーネントのコンシューマを立ち上げ
	 */
	private void execPathPlannerConsumer(){
    	String[] args = {"-f", getConfigFilePath() };
    	GrxDebugUtil.println("[GrxPathPlanner] RTC SERVICE CONSUMER THREAD START");

    	// Initialize manager
    	final Manager manager = Manager.init(args);

    	// Set module initialization procedure
    	// This procedure will be invoked in activateManager() function.
    	ppcomp_ = new PathConsumerComp();
    	manager.setModuleInitProc(ppcomp_);

    	// Activate manager and register to naming service
    	manager.activateManager();

    	// run the manager in blocking mode
    	// runManager(false) is the default.
    	//manager.runManager();

    	// If you want to run the manager in non-blocking mode, do like this
    	manager.runManager(true);
	}

	/**
	 * 経路計画コンポーネントにデータを渡す
	 * @return 初期化の成否
	 */
	private boolean initPathPlanner(){
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner" );
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "PathPlanner Component is not connected.");
	    	return false;
	    }

	    //初期化
		planner_.initPlanner();
		
		//アルゴリズムを指定
		planner_.setAlgorithmName( algoSelect.getText() );

	    // 全ての有効なモデルを登録
		GrxDebugUtil.println("[GrxPathPlanner]@initPathPlanner register character" );
		List<GrxBaseItem> models = manager_.getSelectedItemList( GrxModelItem.class );
		for( GrxBaseItem model: models ){
			GrxModelItem m = (GrxModelItem) model;
			BodyInfo binfo = m.getBodyInfo();
			if (binfo != null) {
				GrxDebugUtil.println("register "+m.getName() );
		    	planner_.registerCharacter( m.getName(), binfo );
			}else{
				GrxDebugUtil.println("not register "+m.getName() );
			}
		}

		//全てのコリジョンペアを登録する
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner register collision pair" );
		double t = Double.parseDouble(tolerance_.getText());
		List<GrxBaseItem> collisionPair = manager_.getSelectedItemList(GrxCollisionPairItem.class);
		for(GrxBaseItem i : collisionPair) {
			GrxCollisionPairItem item = (GrxCollisionPairItem) i;
			planner_.registerIntersectionCheckPair(
					item.getStr("objectName1", ""), item.getStr("jointName1", ""), 
					item.getStr("objectName2", ""), item.getStr("jointName2", ""), 
					t);
		}

		//移動ロボットとして使用するmodelとrobotを指定
		String modelName = modelSelect.getText();
		if( modelName == null || modelName.equals("") ){
			MessageDialog.openInformation(getParent().getShell(),"", "Model is not selected");
			return false;
		}
		String mobilityName = mobilitySelect.getText();
		if( mobilityName == null || mobilityName.equals("") ){
			MessageDialog.openInformation(getParent().getShell(),"", "Mobility is not selected");
			return false;
		}
		planner_.setMobilityName(mobilityName);
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set robot name" );
		planner_.setRobotName(modelName);
		

		// Algorithm Property
		String[][]p = getAlgoProperty();
		if( p == null )
			return false;
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set property" );
		planner_.setProperties( p );

		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner initialize simulation" );
		planner_.initSimulation();

		for( GrxBaseItem model: models ){
			GrxModelItem m = (GrxModelItem) model;
			BodyInfo binfo = m.getBodyInfo();
			if (binfo != null) {
			    double v[] = m.getTransformArray(m.rootLink());
			    GrxDebugUtil.println(m.getName()+":"+v[0]+" "+v[1]+" "+v[2]);
			    planner_.setCharacterPosition(m.getName(), v);
			}
		}

		GrxDebugUtil.println("[GrxPathPlanningView] Planner initialize Succeed");
		return true;
	}

	/**
	 * @brief set start position of planning to planner
	 * @return true if set successfully, false otherwise
	 */
	private boolean _setStartPosition(){
		// Start POSITION
		double sx = 0,sy = 0,st = 0;
		try{
			sx = Double.valueOf( textStartX.getText() );
			sy = Double.valueOf( textStartY.getText() );
			st = Double.valueOf( textStartTheta.getText() ) / 360f * (2*Math.PI);
			while (st < 0) st += 2*Math.PI;
			while (st > 2*Math.PI) st -= 2*Math.PI;
		}catch(Exception e){
			MessageDialog.openInformation(getParent().getShell(),"", "Invalid start position");
			return false;
		}
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set start" );
		planner_.setStartPosition( sx,sy,st );
		return true;
	}
	
	/**
	 * @brief set goal position of planning to planner
	 * @return true if set successfully, false otherwise
	 */
	private boolean _setGoalPosition(){
		// GOAL Position
		double ex = 0,ey = 0,et = 0;
		try{
			ex = Double.valueOf( textEndX.getText() );
			ey = Double.valueOf( textEndY.getText() );
			et = Double.valueOf( textEndTheta.getText() ) / 360f * (2*Math.PI);
			while (et < 0) et += 2*Math.PI;
			while (et > 2*Math.PI) et -= 2*Math.PI;
		}catch(Exception e){
			MessageDialog.openInformation(getParent().getShell(),"", "Invalid end position");
			return false;
		}
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set end" );
		planner_.setGoalPosition( ex,ey,et);
		return true;
	}

	private String[][] getAlgoProperty(){
		// 値の入っているパラメータを取得
		Vector<String[]> tmp = new Vector<String[]>();
		for( Object s : ppaItem_.keySet() ){
			if( ! ppaItem_.getStr( s.toString() ).equals("") ){
				String []tmp2 = { s.toString(), ppaItem_.getStr( s.toString() ) }; 
				tmp.add( tmp2 );
			}
		}
		String [][]params = new String[tmp.size()+1][2];
		for( int i=0; i<tmp.size(); i++ )
			params[i] = tmp.get(i);
		
		// Z position from input
		Double z = getZPosition();
		if( z == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "Invalid robot Z position");
			return null;
		}
			
		params[ params.length-1 ][0] = "z-pos";
		params[ params.length-1 ][1] = String.valueOf( getZPosition() );

		return params;
	}
	
	private String getRobotBaseLink(String name ) {
		 GrxModelItem model = (GrxModelItem) manager_.getItem( GrxModelItem.class, name );
		return model.rootLink().getName();
	}
	private String getRobotBaseLink( GrxModelItem model ) {
		return model.rootLink().getName();
	}

	private void getStartPoint(){
		String robotName = modelSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		if( robot == null )
			return;
		double [] tr = robot.getDblAry(getRobotBaseLink(robot)+".translation",null);
		double [] rot = robot.getDblAry(getRobotBaseLink(robot)+".rotation",null);
		try{
			textStartX.setText( String.format( FORMAT, tr[0]) );
			textStartY.setText( String.format( FORMAT, tr[1]) );
			textStartTheta.setText( String.format( FORMAT, rot[3] / (2f*Math.PI) * 360f) );
			if (ppaItem_ != null){
				ppaItem_.setDbl("startX", tr[0]);
				ppaItem_.setDbl("startY", tr[1]);
				ppaItem_.setDbl("startTheta", rot[3]);
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
		double [] tr = robot.getDblAry(getRobotBaseLink(robot)+".translation",null);
		double [] rot = robot.getDblAry(getRobotBaseLink(robot)+".rotation",null);
		try{
			textEndX.setText( String.format( FORMAT, tr[0]) );
			textEndY.setText( String.format( FORMAT, tr[1]) );
			textEndTheta.setText( String.format( FORMAT, rot[3] / (2f*Math.PI) * 360f) );
			if (ppaItem_ != null){
				ppaItem_.setDbl("goalX", tr[0]);
				ppaItem_.setDbl("goalY", tr[1]);
				ppaItem_.setDbl("goalTheta", rot[3]);
			}
		}catch(Exception e){
			return;
		}
	}

	private void setStartPoint(){
		double sx = 0,sy = 0,st = 0;

		try{
			sx = Double.valueOf( textStartX.getText() );
			sy = Double.valueOf( textStartY.getText() );
			st = Double.valueOf( textStartTheta.getText() ) / 360f * (2*Math.PI);
		}catch(Exception e){
			return;
		}

		String modelName = modelSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, modelName ));
		if( robot == null )
			return;
		robot.propertyChanged(getRobotBaseLink(robot)+".translation", ""+sx+" "+sy+" "+getZPosition());
		robot.propertyChanged(getRobotBaseLink(robot)+".rotation", "0.0 0.0 1.0 "+st);
	}

	private void setEndPoint(){
		double ex = 0,ey = 0,et = 0;

		try{
			ex = Double.valueOf( textEndX.getText() );
			ey = Double.valueOf( textEndY.getText() );
			et = Double.valueOf( textEndTheta.getText() ) / 360f * (2*Math.PI);
		}catch(Exception e){
			return;
		}

		String modelName = modelSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, modelName ));
		robot.propertyChanged(getRobotBaseLink(robot)+".translation", ""+ex+" "+ey+" "+getZPosition() );
		robot.propertyChanged(getRobotBaseLink(robot)+".rotation", "0.0 0.0 1.0 "+et);
	}

	Grx3DView get3DView()
	{
		if( view_ == null )
			view_ = (Grx3DView)manager_.getView(Grx3DView.class);
		return view_;
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
			    GrxDebugUtil.println("[GrxPathPlanningView] can't find 3DView");
			}
		}

    	RoadmapHolder h = new RoadmapHolder();
    	planner_.getRoadmap(h);
    	RoadmapNode[] tree = h.value;
    	
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

        GrxDebugUtil.println("[PPV]@pathGraph size="+vertex.size());
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
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
    		case GrxPluginManager.NOTSELECTED_ITEM:
    			if(ppaItem_==ppa){
    				ppaItem_.deleteObserver(this);
    				ppaItem_ = null;
    			}
    			break;
    		default:
    			break;
    		}
    	}

    	setEnabled(ppaItem_ != null);

		String modelName = null;
		if (ppaItem_ != null){
			modelName = ppaItem_.getStr("model", null);
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
			String mobilityName = ppaItem_.getStr("mobility", "");
			if (mobilitySelect.indexOf(mobilityName) >= 0){
				mobilitySelect.select(mobilitySelect.indexOf(mobilityName));
			}else{
				mobilitySelect.deselectAll();
			}
			String optimizerName = ppaItem_.getStr("optimizer", "");
			if (optimizerSelect.indexOf(optimizerName) >= 0){
				optimizerSelect.select(optimizerSelect.indexOf(optimizerName));
			}else{
				optimizerSelect.deselectAll();
			}
			String algorithmName = ppaItem_.getStr("algorithm", "");
			if (algoSelect.indexOf(algorithmName) >= 0){
				algoSelect.select(algoSelect.indexOf(algorithmName));
			}else{
				algoSelect.deselectAll();
			}
			Double sx = ppaItem_.getDbl("startX", null);
			if (sx != null){
				textStartX.setText(sx.toString());
			}else{
				textStartX.setText("");
			}
			Double sy = ppaItem_.getDbl("startY", null);
			if (sy != null){
				textStartY.setText(sy.toString());
			}else{
				textStartY.setText("");
			}
			Double st = ppaItem_.getDbl("startTheta", null);
			if (st != null){
				textStartTheta.setText(st.toString());
			}else{
				textStartTheta.setText("");
			}
			Double ex = ppaItem_.getDbl("goalX", null);
			if (ex != null){
				textEndX.setText(ex.toString());
			}else{
				textEndX.setText("");
			}
			Double ey = ppaItem_.getDbl("goalY", null);
			if (ey != null){
				textEndY.setText(ey.toString());
			}else{
				textEndY.setText("");
			}
			Double et = ppaItem_.getDbl("goalTheta", null);
			if (et != null){
				textEndTheta.setText(et.toString());
			}else{
				textEndTheta.setText("");
			}
			chkRebuildRoadmap_.setSelection(ppaItem_.isTrue("rebuildRoadmap", true));
			tolerance_.setText(ppaItem_.getDbl("tolerance",0.0).toString());
			carpetZ.setText(ppaItem_.getDbl("carpetZ",0.01).toString());
		}
		
	}
	
	/**
	 * @brief get z position of the robot
	 * @return z position
	 */
	Double getZPosition(){
		String modelName = modelSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, modelName ));
		if (robot == null){
			return 0d;
		}else{
			double [] tr = robot.getDblAry(getRobotBaseLink(robot)+".translation",null);
			return tr[2];
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
}

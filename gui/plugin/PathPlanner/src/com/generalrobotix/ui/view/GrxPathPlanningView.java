package com.generalrobotix.ui.view;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;

import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import jp.go.aist.hrp.simulator.DynamicsSimulator;
import jp.go.aist.hrp.simulator.PathPlanner;
import jp.go.aist.hrp.simulator.WorldStateHolder;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.LinkDataType;
import jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder;
import jp.go.aist.rtm.RTC.Manager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import pathplanner.PathConsumerComp;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;

import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxPathPlanningArgolithmItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Sphere;

@SuppressWarnings("serial")
public class GrxPathPlanningView extends GrxBaseView {
    public static final String TITLE = "Path Planning";

	private Grx3DView view = null;

	// 経路計画コンポ�?ネン�?
	private PathPlanner planner_ = null;
	PathConsumerComp ppcomp;
	private PointArrayHolder path_ = new PointArrayHolder();

    // モ�?��の�?��、ロボットとして扱�?��のを選択す�?
	private Combo robotSelect;

	// 位置、角度の表示用フォーマッ�?
	private static final String FORMAT = "%.3f";
	private static final int TEXT_WIDTH = 64;
	// Start Position
	private Button btnSetStartPoint; 
	private Text textStartX, textStartY, textStartTheta;
	// End Position
	private Button btnSetEndPoint; 
	private Text textEndX, textEndY, textEndTheta;
	// ロボット�?Z位置 3.3までのスピナは�??値を扱えな�?��なんじ�?��ら）�?でとりあえず�?��ストで�?
    //private Spinner robotZ, carpetZ;
	private Text robotZ;

	
	// アルゴリズ�??選択と、パラメータアイ�?��の選�?
	private Combo argoSelect;
	private Combo argoParameter;
	
	// 計算開始�?キャンセル、実行中表示
	private Button btnCalcStart, btnCalcCancel,btnVisible;
	private boolean isCalculating_ = false;
	private int imgCnt=1,imgMax=13;
	private Label imgLabel;
	
	// カーペット�?Z位置、ア�??�??ト�?タン
	private Text carpetZ;
	private Button carpetUpdate; 
	// カーペット�?有効・無効を制御するスイ�?��ノ�?�?
	private Switch switcher;
	private Transform3D carpetTrans;
	private TransformGroup carpetTransGroup; // カーペット�?体�?位置
	private BranchGroup carpet_; 	// カーペット�?プリミティブを追�?��るノー�?
	/*
	switcher->Transform->carpet->primitives
	switcher->Transform |CAT OFF| carpet->primitives
	*/

	public GrxPathPlanningView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);

		execPathPlannerConsumer();

		GridLayout gl = new GridLayout(1,false);
		composite_.setLayout( gl );

		initJava3D();
	    initGUI();
	}

	private void initGUI(){
	    //使�?��わす
	    Composite comp;
		Label l; 
		GridData gd;
		
		//---- ロボット選�?
	
		comp = new Composite( composite_, SWT.NONE| SWT.BORDER );
		GridLayout roboLayout = new GridLayout(2,false);
		comp.setLayout( roboLayout );
		comp.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );

		l = new Label(comp, SWT.NONE);
		l.setText("Robot:");
		robotSelect = new Combo( comp, SWT.NONE );
		robotSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		//robotSelect.add("Select Robot");
		//robotSelect.select(0);

		//---- 始点終点
		
		comp = new Composite ( composite_, SWT.BORDER);
		GridLayout pointLayout = new GridLayout(8,false);
		comp.setLayout( pointLayout );
		comp.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );

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

		l = new Label( comp, SWT.NONE );
		l.setText("Y:");
		textStartY = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textStartY.setLayoutData( gd );

		l = new Label( comp, SWT.NONE );
		l.setText("Yaw:");
		textStartTheta = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textStartTheta.setLayoutData( gd );

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


		l = new Label( comp, SWT.NONE );
		l.setText("Y:");
		textEndY = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textEndY.setLayoutData( gd );

		l = new Label( comp, SWT.NONE );
		l.setText("Yaw:");
		textEndTheta = new Text( comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		textEndTheta.setLayoutData( gd );

		Button setEnd = new Button( comp, SWT.NONE );
		setEnd.setText("SET");
		setEnd.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	setEndPoint();
			}
        });

		// ---- ロボッ�?軸高さ
		Composite roboZComp = new Composite( composite_, SWT.BORDER );
		GridLayout robozLayout = new GridLayout(2,false);
		robozLayout.makeColumnsEqualWidth = false;
		roboZComp.setLayout( robozLayout );
		roboZComp.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );

		l = new Label(roboZComp, SWT.NONE);
		l.setText("Robot Z Position:");
		
		robotZ = new Text(roboZComp, SWT.NONE);
		gd = new GridData();
		gd.widthHint = TEXT_WIDTH;
		robotZ.setLayoutData( gd );
		
		
		//---- アルゴリズ�?���?
	
		Composite argoComp = new Composite( composite_, SWT.BORDER );
		GridLayout argoLayout = new GridLayout(2,false);
		argoLayout.makeColumnsEqualWidth = false;
		argoComp.setLayout( argoLayout );
		argoComp.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );

		l = new Label(argoComp, SWT.NONE);
		l.setText("Argolithm:");
		argoSelect = new Combo( argoComp, SWT.READ_ONLY );
		argoSelect.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		argoSelect.add("RRT");
		argoSelect.add("PRM");
		argoSelect.select(0);

		l = new Label(argoComp, SWT.NONE);
		l.setText("Argolithm Parameters:");
		argoParameter = new Combo( argoComp, SWT.NONE );
		argoParameter.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );
		
		//---- 計算実�?

		Composite calcComp = new Composite( composite_, SWT.BORDER );
		GridLayout calcLayout = new GridLayout(4,false);
		calcLayout.makeColumnsEqualWidth = false;
		calcComp.setLayout( calcLayout );

		l = new Label(calcComp, SWT.NONE);
		l.setText("Calc:");

		btnCalcStart = new Button( calcComp, SWT.NONE );
		btnCalcStart.setText("START");
		btnCalcStart.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	startCalc();
			}
        });

		btnCalcCancel= new Button( calcComp, SWT.NONE );
		btnCalcCancel.setText("CANCEL");
		btnCalcCancel.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	cancelCalc();
			}
        });
		btnCalcCancel.setEnabled(false);

		imgLabel = new Label(calcComp, SWT.BORDER);
		imgLabel.setImage( Activator.getDefault().getImage("grxrobot1.png" ) );

		
		// ---- 経路
		Composite carpetComp = new Composite( composite_, SWT.BORDER );
		GridLayout carpetLayout = new GridLayout( 5, false);
		carpetLayout.makeColumnsEqualWidth = false;
		carpetComp.setLayout( carpetLayout );
		carpetComp.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );

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
               		switcher.setWhichChild(0);
               	} else {
               		System.out.println("FALSE");
               		switcher.setWhichChild(Switch.CHILD_NONE);
               	}
               	
			}
        });
 		btnVisible.setEnabled(false);

 		l = new Label(carpetComp, SWT.NONE);
		l.setText("Carpet Z Position:");
		
		carpetZ = new Text(carpetComp, SWT.NONE);
		carpetZ.setLayoutData( new GridData(GridData.FILL_HORIZONTAL) );

		carpetUpdate = new Button( carpetComp, SWT.NONE );
		carpetUpdate.setText("UPDATE");
		carpetUpdate.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	updateCarpet();
            }
        });
 		
		carpetZ.setEnabled(false);
		carpetUpdate.setEnabled(false);
 		
        setScrollMinSize();
	}

	private void initJava3D(){
		carpet_ = new BranchGroup();
		carpet_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		carpet_.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		carpet_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		carpet_.setCapability(BranchGroup.ALLOW_DETACH); //親(carpetTransGroup)から離れられるようにする
	
		carpetTrans = new Transform3D();
		carpetTrans.setTranslation( new Vector3d(0,0,0) );
	
		carpetTransGroup = new TransformGroup();
		carpetTransGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
		carpetTransGroup.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
		carpetTransGroup.setCapability( Group.ALLOW_CHILDREN_WRITE); //子要�??追�?��可
		carpetTransGroup.setCapability( Group.ALLOW_CHILDREN_EXTEND); //子要�??削除許可
		carpetTransGroup.setTransform( carpetTrans );
		carpetTransGroup.addChild( carpet_ );
	
		switcher = new Switch();
	    switcher.setCapability(Switch.ALLOW_SWITCH_READ);
	    switcher.setCapability(Switch.ALLOW_SWITCH_WRITE);
	    switcher.addChild( carpetTransGroup );
	    switcher.setWhichChild(0);
	}

	private void startCalc(){
    	isCalculating_ = true;
    	btnCalcStart.setEnabled(false);
    	btnCalcCancel.setEnabled(true);

    	robotSelect.setEnabled(false);
    	argoSelect.setEnabled(false);

    	btnSetStartPoint.setEnabled(false);
    	textStartX.setEnabled(false);
    	textStartY.setEnabled(false);
    	textStartTheta.setEnabled(false);

    	btnSetEndPoint.setEnabled(false); 
    	textEndX.setEnabled(false);
    	textEndY.setEnabled(false);
    	textEndTheta.setEnabled(false);

    	robotZ.setEnabled(false);
    	
    	btnVisible.setEnabled(false);

    	if( initPathPlanner() ){
    		//ロボットをスタート位置に移動す�?
    		setStartPoint();
    		// 前回のカーペットを削除する
    		try{
	    		carpetTransGroup.removeChild( carpet_ );
	    		carpet_.removeAllChildren();
	    		carpetTransGroup.addChild( carpet_ );
    		}catch( Exception e ){
    			e.printStackTrace();
    		}
    		calc();
    	}

    	cancelCalc();
	}

	void calc(){
		IRunnableWithProgress runnableProgress = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException {
				monitor.beginTask("Calculating Path Plan. Please Wait.", IProgressMonitor.UNKNOWN );
		    	planner_.calcPath();
				monitor.done();
			}
		};
		ProgressMonitorDialog progressMonitorDlg = new ProgressMonitorDialog(null);
		try {
			progressMonitorDlg.run(true,false, runnableProgress);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    	//パスのリストを取得す�?
    	//path[N][0]=X path[N][1]=Y path[N][2]=Theta が�?ってる予�?
    	planner_.getPath( path_ );
		GrxDebugUtil.println("[GrxPathPlanner] Path length="+path_ );

    	// DynamicsSimulatorの�?��ォルトシミュレート時間�?20secなので、ス�?��プ�?��適当に設定してオーバ�?しな�?���?��?�落ちるよ??
    	double dt_ = 19.0 / path_.value.length, nowTime = 0;
    	//WorldStateItemを取�?
    	GrxWorldStateItem currentWorld_ = (GrxWorldStateItem)manager_.getItem( GrxWorldStateItem.class, null );
    	//ログを消去
    	currentWorld_.clearLog();
    	if( currentWorld_ == null ) {
    		GrxDebugUtil.println("[GrxPathPlanner] There is no World.");
    		return;
    	}
		DynamicsSimulator dynSim = getDynamicsSimulator();
		if( dynSim == null ){
    		GrxDebugUtil.println("[GrxPathPlanner] Faild to get DynamicsSimulator.");
    		return;
		}

    	// ロボット�?移動を登録する
    	for( double p[] : path_.value ) {
    		//System.out.println("[GrxPathPlanner] x="+p[0]+" y="+p[1]+" theta="+p[2] );
    		double newPos[] = new double[12];
    		newPos[0] = p[0];            newPos[1] = p[1];            newPos[2] = getZPosition();
    		newPos[0 + 3] = Math.cos( p[2] ); newPos[1 + 3] = Math.sin( p[2] ); newPos[2 + 3] = 0.0;
    		newPos[3 + 3] =-Math.sin( p[2] ); newPos[4 + 3] = Math.cos( p[2] ); newPos[5 + 3] = 0.0;
    		newPos[6 + 3] = 0.0;         newPos[7 + 3] = 0.0;         newPos[8 + 3] = 1.0;

    		dynSim.setCharacterLinkData( robotSelect.getText(), getRobotBaseLink( robotSelect.getText() ), LinkDataType.ABS_TRANSFORM, newPos);

    	    // 計�?
    		dynSim.calcWorldForwardKinematics();

    	    // 結果を得る
    	    WorldStateHolder stateH_ = new WorldStateHolder();
    		dynSim.getWorldState(stateH_);
			WorldStateEx wsx = new WorldStateEx(stateH_.value);
			// ワールドス�??ト�?追�?
            wsx.time = nowTime;
            currentWorld_.addValue(nowTime, wsx);
			nowTime += dt_;
    	}

    	//カーペットを敷�?
    	for( int i=0; i+1<path_.value.length; i++ ) {
    		double []p1 = path_.value[i], p2 = path_.value[i+1];
    		carpet( p1[0],p1[1], p2[0], p2[1] );
    	}

	}

	// 経路計画サーバへ渡すため�?DynamicsSimulatorを取得す�?
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
	
	private void cancelCalc(){
    	isCalculating_ = false;
    	btnCalcStart.setEnabled(true);
    	btnCalcCancel.setEnabled(false);

    	robotSelect.setEnabled(true);
    	argoSelect.setEnabled(true);

    	btnSetStartPoint.setEnabled(true);
    	textStartX.setEnabled(true);
    	textStartY.setEnabled(true);
     	textStartTheta.setEnabled(true);

    	btnSetEndPoint.setEnabled(true); 
    	textEndX.setEnabled(true);
    	textEndY.setEnabled(true);
    	textEndTheta.setEnabled(true);

    	robotZ.setEnabled(true);

    	btnVisible.setEnabled(true);

    	//未実�??これから決めるー
//    	endPathPlanning();
	}
	
	/**
	 * 経路計画コンポ�?ネント�?コンシューマを立ち上げる�?
	 */
	private void execPathPlannerConsumer(){
		String rtcConfPath = System.getProperty("com.generalrobotix.grxui.rtcpath");
		if( rtcConfPath == null )
			rtcConfPath = "rtc.conf"; // �?��ォルト�?Eclipseの実行フォル�?
    	String[] args = {"-f", rtcConfPath };
    	GrxDebugUtil.println("[GrxPathPlanner] RTC SERVICE CONSUMER THREAD START");

    	// Initialize manager
        final Manager manager = Manager.init(args);

        // Set module initialization proceduer
        // This procedure will be invoked in activateManager() function.
        /*PathConsumerComp*/ ppcomp = new PathConsumerComp();
        manager.setModuleInitProc(ppcomp);

        // Activate manager and register to naming service
        manager.activateManager();

        // run the manager in blocking mode
        // runManager(false) is the default.
        //manager.runManager();

        // If you want to run the manager in non-blocking mode, do like this
        manager.runManager(true);
	}

	/**
	 * 経路計画コンポ�?ネントに�?���?��データを渡�?
	 * @return コンポ�?ネント�?期化の成否
	 */
	private boolean initPathPlanner(){
	    planner_ = ppcomp.getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "PathPlanner Component not Connected.");
	    	return false;
	    }
	
		//アルゴリズ�?���?��して初期化す�?
		planner_.initPlanner( argoSelect.getText() );

	    // 全ての有効化されて�?��モ�?��の�??タを登録する
		// モ�?��のリストを更新
		List<GrxBaseItem> models = manager_.getSelectedItemList( GrxModelItem.class );
		for( GrxBaseItem model: models ){
		    planner_.registerCharacter( model.getName(), ((GrxModelItem)model).cInfo_ );
		}
	    
	    //全てのコリジョンペアを登録する
		List<GrxBaseItem> collisionPair = manager_.getSelectedItemList(GrxCollisionPairItem.class);
		for (int i=0; i<collisionPair.size(); i++) {
			GrxCollisionPairItem item = (GrxCollisionPairItem) collisionPair.get(i);
			planner_.registerCollisionCheckPair(
					item.getStr("objectName1", ""), 
					item.getStr("jointName1", ""), 
					item.getStr("objectName2", ""),
					item.getStr("jointName2", ""), 
					item.getDbl("staticFriction", 0.5),
					item.getDbl("slidingFriction", 0.5),
					item.getDblAry("springConstant",new double[]{0.0,0.0,0.0,0.0,0.0,0.0}), 
					item.getDblAry("damperConstant",new double[]{0.0,0.0,0.0,0.0,0.0,0.0})); 
		}
		//移動ロボットとして使用するモ�?��を指定す�?
		String robotName = robotSelect.getText();
		if( robotName == null || robotName.equals("") ){
			MessageDialog.openInformation(getParent().getShell(),"", "Robot Name not Selected");
			return false;
		}
		planner_.setRobotName( robotName, getRobotBaseLink( robotName ) );

		//スタート位置を指�?
		double sx = 0,sy = 0,st = 0;
		try{
			sx = Double.valueOf( textStartX.getText() );
			sy = Double.valueOf( textStartY.getText() );
			st = Double.valueOf( textStartTheta.getText() ) / 360f * (2*Math.PI);
		}catch(Exception e){
			MessageDialog.openInformation(getParent().getShell(),"", "Start position Values invalid");
			return false;
		}
		planner_.setStartPosition( sx,sy,st );

		//ゴール位置を指�?
		double ex = 0,ey = 0,et = 0;
		try{
			ex = Double.valueOf( textEndX.getText() );
			ey = Double.valueOf( textEndY.getText() );
			et = Double.valueOf( textEndTheta.getText() ) / 360f * (2*Math.PI);
		}catch(Exception e){
			MessageDialog.openInformation(getParent().getShell(),"", "End position Values invalid");
			return false;
		}
		planner_.setGoalPosition( ex,ey,et);

		//アルゴリズ�??プロパティを指定す�?
		String[][]p = getArgoParameter();
		if( p == null )
			return false;
		planner_.setProperties( p );

		planner_.initSimulation();

		GrxDebugUtil.println("[GrxPathPlanner] Planner initialize Succeed");
		return true;
	}

	private String[][] getArgoParameter(){
		String ppaName = argoParameter.getText();
		if( ppaName == null || ppaName.equals("") ){
			MessageDialog.openInformation(getParent().getShell(),"", "ArgolithmParameterItem not selected.");
			return null;
		}
		
		GrxPathPlanningArgolithmItem ppa = (GrxPathPlanningArgolithmItem) manager_.getItem( GrxPathPlanningArgolithmItem.class, ppaName);

		// 空�?���?でな�?��の�?��取り出�?
		Vector<String[]> tmp = new Vector<String[]>();
		for( Object s : ppa.keySet() ){
			if( ! ppa.getStr( s.toString() ).equals("") ){
				String []tmp2 = { s.toString(), ppa.getStr( s.toString() ) }; 
				tmp.add( tmp2 );
			}
		}
		String [][]params = new String[tmp.size()+1][2];
		for( int i=0; i<tmp.size(); i++ )
			params[i] = tmp.get(i);
		
		// Z高さ�?��入力�?�?��スから取�?
		Double z = getZPosition();
		if( z == null ){
			MessageDialog.openInformation(getParent().getShell(),"", "Robot Z Position invalid");
			return null;
		}
			
		params[ params.length-1 ][0] = "z-pos";
		params[ params.length-1 ][1] = String.valueOf( getZPosition() );

		return params;
	}
	
	private String getRobotBaseLink(String name ) {
		 GrxModelItem model = (GrxModelItem) manager_.getItem( GrxModelItem.class, name );
		return model.lInfo_[0].name;
	}
	private String getRobotBaseLink( GrxModelItem model ) {
		return model.lInfo_[0].name;
	}

	private void getStartPoint(){
		String robotName = robotSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		if( robot == null )
			return;

		String WAIST_translation = robot.getProperty( getRobotBaseLink(robot)+".translation");
		String[] tr = WAIST_translation.split(" ");
		String WAIST_rotation = robot.getProperty(getRobotBaseLink(robot)+".rotation");
		String[] rot = WAIST_rotation.split(" ");
		try{
			textStartX.setText( String.format( FORMAT, Double.valueOf( tr[0] ) ) );
			textStartY.setText( String.format( FORMAT, Double.valueOf( tr[1] )) );
			textStartTheta.setText( String.format( FORMAT, Double.valueOf( rot[3] ) / (2f*Math.PI) * 360f) );
		}catch(Exception e){
			return;
		}
	}

	private void getEndPoint(){
		String robotName = robotSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		if( robot == null )
			return;
		String WAIST_translation = robot.getProperty(getRobotBaseLink(robot)+".translation");
		String[] tr = WAIST_translation.split(" ");
		String WAIST_rotation = robot.getProperty(getRobotBaseLink(robot)+".rotation");
		String[] rot = WAIST_rotation.split(" ");
		try{
			textEndX.setText( String.format( FORMAT, Double.valueOf( tr[0] ) ) );
			textEndY.setText( String.format( FORMAT, Double.valueOf( tr[1] )) );
			textEndTheta.setText( String.format( FORMAT, Double.valueOf( rot[3] ) / (2f*Math.PI) * 360f) );
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

		String robotName = robotSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		if( robot == null )
			return;
		robot.setProperty(getRobotBaseLink(robot)+".translation", ""+sx+" "+sy+" "+getZPosition());
		robot.setProperty(getRobotBaseLink(robot)+".rotation", "0.0 0.0 1.0 "+st);
		robot.propertyChanged();		
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

		String robotName = robotSelect.getText();
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, robotName ));
		robot.setProperty(getRobotBaseLink(robot)+".translation", ""+ex+" "+ey+" "+getZPosition() );
		robot.setProperty(getRobotBaseLink(robot)+".rotation", "0.0 0.0 1.0 "+et);
		robot.propertyChanged();
	}

	public void control(List<GrxBaseItem> items) {
		if( isCalculating_ ) {
			imgCnt++;
			if( imgCnt > imgMax )
				imgCnt=1;
			imgLabel.setImage( Activator.getDefault().getImage("grxrobot"+imgCnt+".png" ) );
		}else{
			//imgLabel.setImage( null );
		}
	}

	Grx3DView get3DView()
	{
		if( view == null )
			view = (Grx3DView)manager_.getView(Grx3DView.class);
		return view;
	}

	public boolean setup(List<GrxBaseItem> itemList){
		view = get3DView();
		if( view == null ){
		    GrxDebugUtil.println("[GrxPathPlanning] init Failed");
			return false;
		}

		BranchGroup bg = new BranchGroup();
	    bg.addChild( switcher );
	    view.attachUnclickable( bg );
	    GrxDebugUtil.println("[GrxPathPlanning] init success");

		return true;
	}

	/**
	 * 経路を示すカーペットを�?��表示する。x1,y1が開始位置、x2,y2が終�?��置.
	 */
	void carpet( double x1, double y1, double x2, double y2 ){
		double carpetHeight=0.01;
		double dx = x1 - x2, dy = y1 - y2, theta = Math.atan2( dy,dx ), len = Math.sqrt( dx/2*dx/2+dy/2*dy/2 );
		//Point3d destPoint = new Point3d( x2+dx/2, y2+dy/2, getZPosition()+carpetHeight );
		Point3d startPoint = new Point3d( x1, y1, getZPosition() );

		// 経路表示?�カーペット�?
		Appearance appea = new Appearance( );
		Material material = new Material( );
		Color3f color = new Color3f( 0, 1f, 0 );
		material.setDiffuseColor( color );
		appea.setMaterial( material );
		appea.setTransparencyAttributes(new TransparencyAttributes( TransparencyAttributes.NICEST,0.7f ) );
		// カーペッ�?
		Box box = new Box( (float)len, 0.1f, (float) carpetHeight, appea );
		// 移動�?回転
		Transform3D tr = new Transform3D();
		tr.setTranslation( new Vector3d( startPoint ) );
		Transform3D rot = new Transform3D();
		rot.rotZ( theta );
		tr.mul(rot);
		// ノ�?ド追�?
		BranchGroup bg = new BranchGroup();
		TransformGroup newtg = new TransformGroup( tr );
		bg.addChild( newtg );
		newtg.addChild( box );
		carpet_.addChild( bg );
	}
	
	public void itemSelectionChanged( List<GrxBaseItem> itemList )
	{
		int x=0,point=0;

		// 選択中のモ�?��名�?アルゴリズ�?��を取�?
		String roboName = robotSelect.getText();
		String argoName = argoSelect.getText();

		// モ�?��のリストを更新
		Object[] items = manager_.getSelectedItemList( GrxModelItem.class ).toArray();
		robotSelect.removeAll();
		for( Object o: items ){
			robotSelect.add( o.toString() );
			if( o.toString().equals(roboName) )
				point=x;
			x++;
		}
		robotSelect.select( point );	

		// アルゴリズ�?��ラメータのリストを更新
		Object[] argos = manager_.getSelectedItemList( GrxPathPlanningArgolithmItem.class ).toArray();
		argoParameter.removeAll();
		for( Object o: argos ){
			argoParameter.add( o.toString() );
			if( o.toString().equals(argoName) )
				point=x;
			x++;
		}
		argoParameter.select( point );
	}
	
	Double getZPosition(){
		try{
			//z= Double.valueOf( robotZ.getSelection()/Math.pow(10, robotZ.getDigits()) );
			double z = Double.valueOf( robotZ.getText() );
			return z;
		}catch(Exception e){
			//e.printStackTrace();
		}
		return 0d;
	}

	double getCarpetZPosition(){
		double z=0.2;
		try{
			//z= Double.valueOf( robotZ.getSelection()/Math.pow(10, robotZ.getDigits()) );
			z= Double.valueOf( carpetZ.getText() );
		}catch(Exception e){
			z=0.2;
			e.printStackTrace();
		}
		return z;
	}

	void updateCarpet(){
		System.out.println( "carpet height is"+getCarpetZPosition() );
		carpetTrans.setTranslation( new Vector3d(0,0, getCarpetZPosition() ) );
	}
	
}

/*
 *  GrxPathPlanningAlgorithmItem.java
 *
 *  Copyright (C) 2007 s-cubed, Inc.
 *  All Rights Reserved
 *
 *  @author keisuke kobayashi (s-cubed, Inc.)
 */
 
package com.generalrobotix.ui.item;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import jp.go.aist.hrp.simulator.BodyInfo;
import jp.go.aist.hrp.simulator.DynamicsSimulator;
import jp.go.aist.hrp.simulator.PathConsumerComp;
import jp.go.aist.hrp.simulator.PathPlanner;
import jp.go.aist.hrp.simulator.StringSequenceHolder;
import jp.go.aist.hrp.simulator.WorldStateHolder;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.LinkDataType;
import jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder;
import jp.go.aist.hrp.simulator.PathPlannerPackage.RoadmapHolder;
import jp.go.aist.hrp.simulator.PathPlannerPackage.RoadmapNode;
import jp.go.aist.rtm.RTC.Manager;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial")
/**
 * @brief 
 */
public class GrxPathPlanningAlgorithmItem extends GrxBaseItem {

	public static final String TITLE = "PPAlgorithm";
	public static final String FILE_EXTENSION = "ppa";
	
	// 経路計画コンポーネント
	private PathPlanner planner_ = null;
	private static PathConsumerComp ppcomp_ = null;
	private boolean connectChange_ = false;

	public GrxPathPlanningAlgorithmItem(String name, GrxPluginManager manager) {
		super(name, manager);

		setExclusive(true);

		setProperty("rebuildRoadmap", "true");
		setProperty("carpetZ", "0.01");
		setProperty("tolerance", "0.0");
		
		_pathConsumer();
		
		Runnable run = new Runnable(){
			public void run() {	
				if(connectChange_){
					notifyObservers("connected");
					connectChange_ = false;
				}
				Display display = Display.getCurrent();
				if ( display!=null && !display.isDisposed()){
					display.timerExec(100, this);
				}
			}
		};
		Display display = Display.getCurrent();
		if ( display!=null && !display.isDisposed()){
			display.timerExec(100, run);
		}
	}

	public boolean create() {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void delete(){
		Collection<GrxPathPlanningAlgorithmItem> col=(Collection<GrxPathPlanningAlgorithmItem>) manager_.getItemMap(GrxPathPlanningAlgorithmItem.class).values();
		if(col.size()==1 && col.contains(this))
			Manager.instance().deleteComponent(ppcomp_.getInstanceName());
		ppcomp_ = null;
		super.delete(); 
	}
	
	public boolean propertyChanged(String key, String value){
		if (super.propertyChanged(key, value)){
			
		}else{
			setProperty(key, value);
			return true;
		}
		return false;
	}
	
	public PathPlanner getPlanner(){
		return _pathConsumer().getImpl();
	}
	
	private PathConsumerComp _pathConsumer(){
		if (ppcomp_ == null){
			execPathPlannerConsumer();
		}
		return ppcomp_;
	}
	
	/**
	 *  @brief 経路計画コンポーネントのコンシューマを立ち上げ
	 */
	private void execPathPlannerConsumer(){
        String [] args = {"-f", getConfigFilePath()};
        if( args[1].isEmpty() ){
            args[0] = "";
        }
    	GrxDebugUtil.println("[GrxPathPlanner] RTC SERVICE CONSUMER THREAD START"); //$NON-NLS-1$

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
    	
		ppcomp_.setConnectedCallback(this);
	}
	
	/**
	 * コンフィグファイルの探索
	 * @return コンフィグファイルのパス
	 */
	private String getConfigFilePath(){
		String confPath=""; //$NON-NLS-1$
        File defualtRtcFile = new File(Activator.getDefault().getTempDir() + File.separator + "rtc.conf");
        if( defualtRtcFile.isFile() ){
            confPath = defualtRtcFile.getPath(); 
        }
    	GrxDebugUtil.println("[GrxPathPlanner] default Config File path="+confPath); //$NON-NLS-1$
    	// From Property ( if exist )
		String confPathFromProperty = System.getProperty("com.generalrobotix.grxui.rtcpath"); //$NON-NLS-1$
		if( confPathFromProperty != null ) {
			confPath = confPathFromProperty;
	    	GrxDebugUtil.println("[GrxPathPlanner] Config File path from Property="+confPath); //$NON-NLS-1$
		}

		return confPath;
	}
	
	/**
	 * @brief 移動動作設計コンポーネントから現在選択している経路計画アルゴリズムに対するプロパティを取得し、選択しているアイテムに設定する
	 */
	public void propertyUpdate(){
		planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.messasge.notConnect")); //$NON-NLS-1$ //$NON-NLS-2$
	    	return;
	    }

	    StringSequenceHolder propertyNames_ = new StringSequenceHolder();
		StringSequenceHolder propertyDefaults_ = new StringSequenceHolder();
		planner_.getProperties( getProperty("algorithm"), propertyNames_, propertyDefaults_ );
		String[] props = propertyNames_.value;
		String[] defaults = propertyDefaults_.value;
		
		//ppa.clear(); // プロパティを全て破棄
		for( int i=0; i<props.length; i++ )
			setProperty( props[i], defaults[i] );
	}
	
	/**
	 * 経路計画コンポーネントにデータを渡す
	 * @return 初期化の成否
	 */
	private boolean initPathPlanner(){
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner" ); //$NON-NLS-1$
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.notConnect")); //$NON-NLS-1$ //$NON-NLS-2$
	    	return false;
	    }

	    //初期化
		planner_.initPlanner();
		
		//アルゴリズムを指定
		planner_.setAlgorithmName( getProperty("algorithm") );

	    // 全ての有効なモデルを登録
		GrxDebugUtil.println("[GrxPathPlanner]@initPathPlanner register character" ); //$NON-NLS-1$
		List<GrxBaseItem> models = manager_.getSelectedItemList( GrxModelItem.class );
		for( GrxBaseItem model: models ){
			GrxModelItem m = (GrxModelItem) model;
			BodyInfo binfo = m.getBodyInfo();
			if (binfo != null) {
				GrxDebugUtil.println("register "+m.getName() ); //$NON-NLS-1$
		    	planner_.registerCharacter( m.getName(), binfo );
			}else{
				GrxDebugUtil.println("not register "+m.getName() ); //$NON-NLS-1$
			}
		}

		//全てのコリジョンペアを登録する
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner register collision pair" ); //$NON-NLS-1$
		double t = getDbl("tolerance", 0.0);
		List<GrxBaseItem> collisionPair = manager_.getSelectedItemList(GrxCollisionPairItem.class);
		for(GrxBaseItem i : collisionPair) {
			GrxCollisionPairItem item = (GrxCollisionPairItem) i;
			planner_.registerIntersectionCheckPair(
					item.getStr("objectName1", ""), item.getStr("jointName1", ""),  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					item.getStr("objectName2", ""), item.getStr("jointName2", ""),  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					t);
		}

		//移動ロボットとして使用するmodelとrobotを指定
		String modelName = getProperty("model");
		if( modelName == null || modelName.equals("") ){ //$NON-NLS-1$
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.model")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		String mobilityName = getProperty("mobility");
		if( mobilityName == null || mobilityName.equals("") ){ //$NON-NLS-1$
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.mobility")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		planner_.setMobilityName(mobilityName);
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set robot name" ); //$NON-NLS-1$
		planner_.setRobotName(modelName);
		

		// Algorithm Property
		String[][]p = getAlgoProperty();
		if( p == null )
			return false;
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set property" ); //$NON-NLS-1$
		planner_.setProperties( p );

		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner initialize simulation" ); //$NON-NLS-1$
		planner_.initSimulation();

		for( GrxBaseItem model: models ){
			GrxModelItem m = (GrxModelItem) model;
			BodyInfo binfo = m.getBodyInfo();
			if (binfo != null) {
			    double v[] = m.getTransformArray(m.rootLink());
			    GrxDebugUtil.println(m.getName()+":"+v[0]+" "+v[1]+" "+v[2]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			    planner_.setCharacterPosition(m.getName(), v);
			}
		}

		GrxDebugUtil.println("[GrxPathPlanningView] Planner initialize Succeed"); //$NON-NLS-1$
		return true;
	}
	
	private String[][] getAlgoProperty(){
		// 値の入っているパラメータを取得
		Vector<String[]> tmp = new Vector<String[]>();
		for( Object s : keySet() ){
			if( ! getStr( s.toString() ).equals("") ){ //$NON-NLS-1$
				String []tmp2 = { s.toString(), getStr( s.toString() ) }; 
				tmp.add( tmp2 );
			}
		}
		String [][]params = new String[tmp.size()+1][2];
		for( int i=0; i<tmp.size(); i++ )
			params[i] = tmp.get(i);
		
		// Z position from input
		Double z = getZPosition();
		if( z == null ){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.invalidRobot")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
			
		params[ params.length-1 ][0] = "z-pos"; //$NON-NLS-1$
		params[ params.length-1 ][1] = String.valueOf( getZPosition() );

		return params;
	}
	
	/**
	 * @brief get z position of the robot
	 * @return z position
	 */
	Double getZPosition(){
		String modelName = getProperty("model");
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, modelName ));
		if (robot == null){
			return 0d;
		}else{
			double [] tr = robot.getDblAry(getRobotBaseLink(robot)+".translation",null); //$NON-NLS-1$
			return tr[2];
		}
	}

	private String getRobotBaseLink(String name ) {
		 GrxModelItem model = (GrxModelItem) manager_.getItem( GrxModelItem.class, name );
		return model.rootLink().getName();
	}
	
	private String getRobotBaseLink( GrxModelItem model ) {
		return model.rootLink().getName();
	}
	
	/**
	 * @brief set start position of planning to planner
	 * @return true if set successfully, false otherwise
	 */
	private boolean _setStartPosition(){
		// Start POSITION
		double sx = 0,sy = 0,st = 0;
		try{
			sx = getDbl("startX", 0.0);
			sy = getDbl("startY", 0.0);
			st = getDbl("startTheta", 0.0)/ 360f * (2*Math.PI);
			while (st < 0) st += 2*Math.PI;
			while (st > 2*Math.PI) st -= 2*Math.PI;
		}catch(Exception e){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.invalidStart")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set start" ); //$NON-NLS-1$
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
			ex = getDbl("goalX", 0.0);
			ey = getDbl("goalY", 0.0);
			et = getDbl("goalTheta", 0.0) / 360f * (2*Math.PI);
			while (et < 0) et += 2*Math.PI;
			while (et > 2*Math.PI) et -= 2*Math.PI;
		}catch(Exception e){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.InvalidEnd")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		GrxDebugUtil.println("[GrxPathPlanningView]@initPathPlanner set end" ); //$NON-NLS-1$
		planner_.setGoalPosition( ex,ey,et);
		return true;
	}

	public void setStartPoint(){
		double sx = 0,sy = 0,st = 0;

		try{
			sx = getDbl("startX", 0.0);
			sy = getDbl("startY", 0.0);
			st = getDbl("startTheta", 0.0)/ 360f * (2*Math.PI);
		}catch(Exception e){
			return;
		}

		String modelName = getProperty("model");
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, modelName ));
		if( robot == null )
			return;
		robot.propertyChanged(getRobotBaseLink(robot)+".translation", ""+sx+" "+sy+" "+getZPosition()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		robot.propertyChanged(getRobotBaseLink(robot)+".rotation", "0.0 0.0 1.0 "+st); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void setEndPoint(){
		double ex = 0,ey = 0,et = 0;

		try{
			ex = getDbl("goalX", 0.0);
			ey = getDbl("goalY", 0.0);
			et = getDbl("goalTheta", 0.0) / 360f * (2*Math.PI);
		}catch(Exception e){
			return;
		}

		String modelName = getProperty("model");
		GrxModelItem robot = (GrxModelItem)(manager_.getItem( GrxModelItem.class, modelName ));
		robot.propertyChanged(getRobotBaseLink(robot)+".translation", ""+ex+" "+ey+" "+getZPosition() ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		robot.propertyChanged(getRobotBaseLink(robot)+".rotation", "0.0 0.0 1.0 "+et); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void startCalc(){
	    planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.notConnect")); //$NON-NLS-1$ //$NON-NLS-2$
	    	return;
	    }

    	if ( getProperty("rebuildRoadmap").equals("true")){
    		if (!initPathPlanner()){
    			return;
    		}
    	}
    	_setStartPosition();
    	_setGoalPosition();
    	
    	//ロボットをスタート位置に移動
    	setStartPoint();
    	calc();
	}
	
	private boolean calcSucceed=false;
	private void calc(){
		GrxDebugUtil.println("[GrxPathPlanner]@calc" ); //$NON-NLS-1$
		calcSucceed = false;
		IRunnableWithProgress runnableProgress = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException {
				Thread calcThread = new Thread( new Runnable(){
					public void run(){
				    	calcSucceed = planner_.calcPath();
					}
				});
				GrxDebugUtil.println("[GrxPathPlanner]@calc Thread Start" ); //$NON-NLS-1$
				calcThread.start();
				monitor.beginTask("Planning a path. Please Wait.", IProgressMonitor.UNKNOWN ); //$NON-NLS-1$
				while( calcThread.isAlive() ){
					Thread.sleep(200);
					GrxDebugUtil.print("." ); //$NON-NLS-1$
					if( monitor.isCanceled() ){
						GrxDebugUtil.println("[GrxPathPlanner]@calc Cancel" ); //$NON-NLS-1$
						planner_.stopPlanning();
						break;
					}
				}
				monitor.done();
			}
		};
		ProgressMonitorDialog progressMonitorDlg = new ProgressMonitorDialog( GrxUIPerspectiveFactory.getCurrentShell() );
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
			MessageDialog.openInformation( GrxUIPerspectiveFactory.getCurrentShell(), MessageBundle.get("GrxPathPlanningView.dialog.message.cancel0"), MessageBundle.get("GrxPathPlanningView.dialog.message.cancel1")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		displayPath();
	}

	/**
	 * @brief 経路を最適化する
	 */
	public void optimize() {
		planner_ = _pathConsumer().getImpl();
	    if( planner_ == null ){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.notConnect")); //$NON-NLS-1$ //$NON-NLS-2$
	    	return;
	    }
	    
		String optimizer = getProperty("optimizer");
		if( optimizer == null || optimizer.equals("") ){ //$NON-NLS-1$
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.mesage.notSelect")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

	    planner_.optimize(optimizer);
	    displayPath();
	}

	private void displayPath() {
		PointArrayHolder path = new PointArrayHolder();
    	planner_.getPath( path );
		GrxDebugUtil.println("[GrxPathPlanningView] Path length="+path.value.length ); //$NON-NLS-1$

    	// TODO:DynamicsSimulatorのdefault simulation time 20sec オーバすると落ちる
    	double dt = 10.0 / path.value.length, nowTime = 0;
    	GrxWorldStateItem currentWorld_ = (GrxWorldStateItem)manager_.getItem( GrxWorldStateItem.class, null );
    	if( currentWorld_ == null ) {
    		GrxDebugUtil.printErr("[GrxPathPlanningView] There is no World."); //$NON-NLS-1$
    		return;
    	}
    	currentWorld_.clearLog();
    	currentWorld_.setDbl("logTimeStep", dt); //$NON-NLS-1$
		DynamicsSimulator dynSim = getDynamicsSimulator();
		if( dynSim == null ){
    		GrxDebugUtil.printErr("[GrxPathPlanningView] Faild to get DynamicsSimulator."); //$NON-NLS-1$
    		return;
		}

    	// register robot
		String model = getProperty("model");
		String base = getRobotBaseLink(model);
	    WorldStateHolder stateH_ = new WorldStateHolder();
    	for( double p[] : path.value ) {
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
    	System.out.println("worldstate.getLogSize() = "+currentWorld_.getLogSize()); //$NON-NLS-1$
    	currentWorld_.setPosition(currentWorld_.getLogSize()-1);
	}

	// 経路計画サーバへ渡すためDynamicsSimulatorを取得
	DynamicsSimulator getDynamicsSimulator(){
		GrxSimulationItem simItem = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
		if(simItem==null){
			simItem = (GrxSimulationItem)manager_.createItem(GrxSimulationItem.class, null);
			manager_.itemChange(simItem, GrxPluginManager.ADD_ITEM);
			manager_.setSelectedItem(simItem, true);
		}
		if( ! simItem.initDynamicsSimulator() ){
			MessageDialog.openInformation(GrxUIPerspectiveFactory.getCurrentShell(),"", MessageBundle.get("GrxPathPlanningView.dialog.message.Dynamics")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		return simItem.getDynamicsSimulator(false);
	}

	public String[] getAlgorithms(){
		if(planner_==null)
			return new String[0];
		StringSequenceHolder names = new StringSequenceHolder();
		// planning algorithms
		planner_.getAlgorithmNames( names );
		return  names.value;
	}
	
	public String[] getMobilityNames(){
		if(planner_==null)
			return new String[0];
		StringSequenceHolder names = new StringSequenceHolder();
		planner_.getMobilityNames( names );
		return names.value;
	}
	
	public String[] getOptimizerNames(){
		if(planner_==null)
			return new String[0];
		StringSequenceHolder names = new StringSequenceHolder();
		planner_.getOptimizerNames( names );
		return names.value;
	}
	
	public RoadmapNode[] getRoadmap(){
		if(planner_==null)
			return new RoadmapNode[0];
		RoadmapHolder h = new RoadmapHolder();
		planner_.getRoadmap(h);
		return (RoadmapNode[])h.value;
	}
	
	public double[][] getPath(){
		if(planner_==null)
			return new double[0][0];
		PointArrayHolder path = new PointArrayHolder();
		planner_.getPath( path );
		return path.value;
	}

	public void connectedCallback(boolean b) {
		if(b)
			planner_ = _pathConsumer().getImpl();
		else
			planner_ = null;
		connectChange_ = true;
	}
}

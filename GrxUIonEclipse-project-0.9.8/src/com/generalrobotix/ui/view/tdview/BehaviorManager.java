/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * BehaviorManager.java
 *
 * @author  Kernel, Inc.
 * @version  2.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import jp.go.aist.hrp.simulator.BodyInfo;
import jp.go.aist.hrp.simulator.DynamicsSimulator;
import jp.go.aist.hrp.simulator.DynamicsSimulatorFactory;
import jp.go.aist.hrp.simulator.DynamicsSimulatorFactoryHelper;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.IntegrateMethod;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.LinkDataType;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.SensorOption;
import jp.go.aist.hrp.simulator.*;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.Grx3DViewClickListener;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.Grx3DView;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickTool;

/**
 * ビヘイビア管理のための窓口。
 * モードの切替えをIseBehaviorクラス、IseBehaviorHandlerクラスに伝える。
 * ハンドラクラスからイベントを受け取るリスナの役割をもつ。
 */
public class BehaviorManager implements WorldReplaceListener {
    //--------------------------------------------------------------------
    // 定数
    // オペレーションモード
    public static final int OPERATION_MODE_NONE         = 0;
    public static final int OBJECT_ROTATION_MODE        = 1;
    public static final int OBJECT_TRANSLATION_MODE     = 2;
    public static final int JOINT_ROTATION_MODE         = 3;
    public static final int FITTING_FROM_MODE           = 4;
    public static final int FITTING_TO_MODE             = 5;
    public static final int INV_KINEMA_FROM_MODE        = 6;
    public static final int INV_KINEMA_TRANSLATION_MODE = 7;
    public static final int INV_KINEMA_ROTATION_MODE    = 8;

    //  ビューモード
    public static final int WALK_VIEW_MODE     = 1;
    public static final int ROOM_VIEW_MODE     = 2;
    public static final int PARALLEL_VIEW_MODE = 3;

    // インスタンス変数
    DynamicsSimulator currentDynamics_;
    private IseBehavior behavior_;
    private IseBehaviorHandler handler_;
    private int viewMode_;
    private int operationMode_;
    private ThreeDDrawable drawable_;
    private BehaviorInfo info_;
    private BehaviorHandler indicator_;
    private GrxPluginManager manager_;
    private Grx3DView viewer_;
    private InvKinemaResolver resolver_;
    private boolean itemChangeFlag_ = false;
    private boolean messageSkip_ = false;
    
    private List<GrxModelItem> currentModels_ = null;
    private List<GrxCollisionPairItem> currentCollisionPairs_ = null;
    
    //--------------------------------------------------------------------
    // コンストラクタ
    public BehaviorManager(GrxPluginManager manager) {
    	manager_ = manager;
    	resolver_ = new InvKinemaResolver(manager_);
        //Simulator.getInstance().addWorldReplaceListener(this);
    }

    // 公開メソッド
    public void setThreeDViewer(Grx3DView viewer) {
    	viewer_ = viewer;
        PickCanvas pickCanvas = new PickCanvas(
            viewer.getCanvas3D(),
            viewer.getBranchGroupRoot()
        );
        pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);

        handler_ = new IseBehaviorHandler(this);

        behavior_ = new IseBehavior(handler_);
 
        BranchGroup bg = new BranchGroup();
        bg.addChild(behavior_);

        BranchGroup bgRoot = viewer_.getBranchGroupRoot();
        bgRoot.addChild(bg);
        
        //drawable_ = viewer.getDrawable();
        drawable_ = viewer;

        ViewInfo viewInfo = drawable_.getViewInfo();
        switch (viewInfo.getViewMode()) {
        case ViewInfo.VIEW_MODE_ROOM:
            setViewMode(ROOM_VIEW_MODE);
            break;
        case ViewInfo.VIEW_MODE_WALK:
            setViewMode(WALK_VIEW_MODE);
            break;
        case ViewInfo.VIEW_MODE_PARALLEL:
            setViewMode(PARALLEL_VIEW_MODE);
            break;
        }
        //TransformGroup tgView = drawable_.getTransformGroupRoot();
        info_ = new BehaviorInfo(manager_, pickCanvas, drawable_);

        behavior_.setBehaviorInfo(info_);
    }

    public void setViewIndicator(BehaviorHandler handler) {
        indicator_ = handler;
    }

    public void setViewMode(int mode) {
        viewMode_ = mode;
        handler_.setViewMode(viewMode_);
    }

    public void setOperationMode(int mode) {
        operationMode_ = mode;
        handler_.setOperationMode(operationMode_);
    }

    public void setViewHandlerMode(String str) {
        handler_.setViewHandlerMode(str);
    }

    public boolean fit() {
        return handler_.fit(info_);
    }

    public void setPickTarget(TransformGroup tg) {
        BehaviorInfo info = behavior_.getBehaviorInfo();
        handler_.setPickTarget(tg, info);
    }

    //--------------------------------------------------------------------
    // ViewChangeListenerの実装
    public void viewChanged(ViewChangeEvent evt) {
        Transform3D transform = new Transform3D();
        evt.getTransformGroup().getTransform(transform);

        // データの通知は ViewNode に任せる
        drawable_.setTransform(transform);
    }

    //--------------------------------------------------------------------
    // WorldReplaceListenerの実装
    public void replaceWorld(List<GrxBaseItem> list) {
        //handler_ = new IseBehaviorHandler();
        //behavior_ = new IseBehavior(handler_);
 
        //BranchGroup bg = new BranchGroup();
        //bg.addChild(behavior_);

        //BranchGroup bgRoot = viewer_.getBranchGroupRoot();
        //bgRoot.addChild(bg);

        //InvKinemaResolver resolver = new InvKinemaResolver(world_);
        //ProjectManager project = ProjectManager.getInstance();
        //resolver.setDynamicsSimulator(project.getDynamicsSimulator());
        //handler_.setInvKinemaResolver(resolver);
        
        if (handler_ != null) 
        	handler_.setViewIndicator(indicator_);
    }

    /**
     * @brief create dynamics server
     * @param update if true is given, existing dynamics server is destroyed and re-created
     * @return dynamics server
     */
	public DynamicsSimulator getDynamicsSimulator(boolean update) {
		//currentDynamics_ = dynamicsMap_.get(currentWorld_);
		if (update && currentDynamics_ != null) {
            try {
			  currentDynamics_.destroy();
            } catch (Exception e) {
				GrxDebugUtil.printErr("getDynamicsSimulator: destroy failed."); //$NON-NLS-1$
            }
			currentDynamics_ = null;
		}
		
		if (currentDynamics_ == null) {
			try {
				org.omg.CORBA.Object obj = //process_.get(DynamicsSimulatorID_).getReference();
				GrxCorbaUtil.getReference("DynamicsSimulatorFactory"); //$NON-NLS-1$
				DynamicsSimulatorFactory ifactory = DynamicsSimulatorFactoryHelper
						.narrow(obj);
				currentDynamics_ = ifactory.create();
				currentDynamics_._non_existent();
				//dynamicsMap_.put(currentWorld_, currentDynamics_);

			} catch (Exception e) {
				GrxDebugUtil.printErr("getDynamicsSimulator: create failed."); //$NON-NLS-1$
				currentDynamics_ = null;
			}
		}
		return currentDynamics_;
	}
	
	public int getOperationMode() {
		return operationMode_;
	}

	/**
	 * @brief initialize dynamics server
	 * 
	 * dynamics server object is created and existing model items are registered.
	 * And then, collision check pairs between items are registered.
	 * @return true initialized successfully, false otherwise
	 */
	public boolean initDynamicsSimulator() {	
		boolean ModelModified = false;
		Iterator<GrxModelItem> itr = currentModels_.iterator();
		while(itr.hasNext()){
			GrxModelItem model = itr.next();
			if(model.isModified()){
				ModelModified = true;
				if(!messageSkip_){
					final String name = model.getName();
					Display display = Display.getDefault();
					display.syncExec(new Runnable(){
						public void run(){
							MessageDialog.openInformation(null, "", MessageBundle.get("BehaviorManager.dialog.message.reloadModel")+name+") "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					});
				}
			}
			if(ModelModified) return false;
		}
		if(!itemChangeFlag_) return true;
		if(getDynamicsSimulator(true) == null) return false;

		try {
			// register characters
			for (int i=0; i<currentModels_.size(); i++) {
				final GrxModelItem model = currentModels_.get(i);
				BodyInfo bodyInfo = model.getBodyInfo();
				if(bodyInfo==null)	return false;
				currentDynamics_.registerCharacter(model.getName(), bodyInfo);
			}

			IntegrateMethod m = IntegrateMethod.EULER;
			currentDynamics_.init(0.005, m, SensorOption.ENABLE_SENSOR);
			currentDynamics_.setGVector(new double[] { 0.0, 0.0, 9.8});
			
			// set position/orientation and joint angles
			for (int i=0; i<currentModels_.size(); i++) {
				GrxModelItem model = currentModels_.get(i);
				if (model.links_ == null)
					continue;
				
				GrxLinkItem base = model.rootLink();
				currentDynamics_.setCharacterLinkData(
					model.getName(), base.getName(), LinkDataType.ABS_TRANSFORM, 
					model.getTransformArray(base));
				
				currentDynamics_.setCharacterAllLinkData(
					model.getName(), LinkDataType.JOINT_VALUE, 
					model.getJointValues());
			}
			
            // set collision check pairs 
			Map<String, GrxModelItem> modelmap = (Map<String, GrxModelItem>)manager_.getItemMap(GrxModelItem.class);
			for (int i=0; i<currentCollisionPairs_.size(); i++) {
				GrxCollisionPairItem item = (GrxCollisionPairItem) currentCollisionPairs_.get(i);
				GrxModelItem m1 = modelmap.get(item.getStr("objectName1", "")); //$NON-NLS-1$ //$NON-NLS-2$
				GrxModelItem m2 = modelmap.get(item.getStr("objectName2", "")); //$NON-NLS-1$ //$NON-NLS-2$
				if (m1 == null || m2 == null) continue;
				Vector<GrxLinkItem> links1, links2;
				String lname1 = item.getStr("jointName1",""); //$NON-NLS-1$ //$NON-NLS-2$
				if (lname1.equals("")){ //$NON-NLS-1$
					links1 = m1.links_;
				}else{
					links1 = new Vector<GrxLinkItem>();
					GrxLinkItem l = m1.getLink(lname1);
					if (l != null) links1.add(l);
				}
				String lname2 = item.getStr("jointName2",""); //$NON-NLS-1$ //$NON-NLS-2$
				if (lname2.equals("")){ //$NON-NLS-1$
					links2 = m2.links_;
				}else{
					links2 = new Vector<GrxLinkItem>();
					GrxLinkItem l = m2.getLink(lname2);
					if (l != null) links2.add(l);
				}
				for (int j=0; j<links1.size(); j++){
					for (int k=0; k<links2.size(); k++){
						currentDynamics_.registerIntersectionCheckPair(
								m1.getName(), links1.get(j).getName(),
								m2.getName(), links2.get(k).getName(),
								links1.get(j).getDbl("tolerance",0.0)+links2.get(k).getDbl("tolerance",0.0)); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
            //state_.value = null;
		} catch (Exception e) {
			GrxDebugUtil.printErr("initDynamicsSimulator:", e); //$NON-NLS-1$
			return false;
		}
		resolver_.setDynamicsSimulator(currentDynamics_);
		handler_.setInvKinemaResolver(resolver_);
		itemChangeFlag_ = false;
		return true;
	}

	public void addClickListener( Grx3DViewClickListener listener ){
		behavior_.addClickListener( listener );
	}
	public void removeClickListener( Grx3DViewClickListener listener ){
		behavior_.removeClickListener( listener );
	}
	
	private boolean setCharacterData(){
		if(currentCollisionPairs_.isEmpty() || currentModels_.isEmpty()) return false;
		if(!initDynamicsSimulator())	return false;
		if (currentDynamics_ == null) return false;
		for (int i=0; i<currentModels_.size(); i++)  {
			GrxModelItem model = currentModels_.get(i);
			String name = model.getName();
			GrxLinkItem base = model.rootLink();
			double[] data = model.getTransformArray(base);
			currentDynamics_.setCharacterLinkData(name, base.getName(), LinkDataType.ABS_TRANSFORM, data);
			data = model.getJointValues();
			currentDynamics_.setCharacterAllLinkData(name, LinkDataType.JOINT_VALUE, data);
		}
		return true;
	}
	
	/**
	 * @brief get collision information
	 * 
	 * Before calling this method, dynamics server object must be initialized by calling initDynamicsSimulator()
	 * @param modelList list of model items. Positions of these items are updated
	 * @return collision information
	 */
	public Collision[] getCollision() {
		if(!setCharacterData())
			return null;
		if (currentDynamics_.checkCollision(true)){
			WorldStateHolder wsH = new WorldStateHolder();
			currentDynamics_.getWorldState(wsH);
			return wsH.value.collisions;
		}else{
			return null;
		}
	}

	/**
	 * @brief get distance information
	 * 
	 * Before calling this method, dynamics server object must be initialized by calling initDynamicsSimulator()
	 * @param modelList list of model items. Positions of these items are updated
	 * @return distance information
	 */
	public Distance[] getDistance() {
		if(!setCharacterData())
			return null;
		return currentDynamics_.checkDistance();
	}
	
	/**
	 * @brief get intersection information
	 * 
	 * Before calling this method, dynamics server object must be initialized by calling initDynamicsSimulator()
	 * @param modelList list of model items. Positions of these items are updated
	 * @return intersecting pairs
	 */
	public LinkPair[] getIntersection() {
		if(!setCharacterData())
			return null;
		return currentDynamics_.checkIntersection(true);
	}

	public void setItem(List<GrxModelItem> models, List<GrxCollisionPairItem> cols){
		currentModels_ = models;
		currentCollisionPairs_ = cols;
		itemChangeFlag_ = true;
	}
	
	public void destroyDynamicsSimulator(){
		try {
			currentDynamics_.destroy();
		} catch (Exception e) {
			GrxDebugUtil.printErr("getDynamicsSimulator: destroy failed."); //$NON-NLS-1$
		}
		currentDynamics_ = null;
	}
	
	public void setMessageSkip(boolean flg){
		messageSkip_ = flg;
	}
}

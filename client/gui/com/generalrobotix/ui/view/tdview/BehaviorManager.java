/**
 * BehaviorManager.java
 *
 * @author  Kernel, Inc.
 * @version  2.0 (Mon Nov 12 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.*;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.DynamicsSimulatorPackage.*;

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickTool;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.view.Grx3DView;

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
    private IseBehavior behavior_;
    private IseBehaviorHandler handler_;
    private int viewMode_;
    private int operationMode_;
    private ThreeDDrawable drawable_;
    private BehaviorInfo info_;
    private BehaviorHandler indicator_;
    private GrxPluginManager manager_;
    private Grx3DView viewer_;

    private DynamicsSimulator currentDynamics_;
	private CollisionDetector collisionDetector_;
    //--------------------------------------------------------------------
    // コンストラクタ
    public BehaviorManager(GrxPluginManager manager) {
    	manager_ = manager;
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
        
        handler_ = new IseBehaviorHandler();
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
       	handler_.setViewIndicator(handler);
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
		try {
			if (currentDynamics_ != null) {
				try {
					currentDynamics_.destroy();
				} catch (Exception e) {
					GrxDebugUtil.printErr("getDynamicsSimulator: destroy failed.");
				}
				currentDynamics_ = null;
			}
		    	
			currentDynamics_ = initDynamicsSimulator(list);
			InvKinemaResolver resolver = new InvKinemaResolver(manager_);
			resolver.setDynamicsSimulator(currentDynamics_);
			handler_.setInvKinemaResolver(resolver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getOperationMode() {
		return operationMode_;
	}

	private DynamicsSimulator initDynamicsSimulator(List<GrxBaseItem> modelList) {
		DynamicsSimulator dynamics;
		try {
			org.omg.CORBA.Object obj = GrxCorbaUtil.getReference("DynamicsSimulatorFactory", "localhost", 2809);
			DynamicsSimulatorFactory ifactory = DynamicsSimulatorFactoryHelper.narrow(obj);
			dynamics = ifactory.create();
		} catch (Exception e) {
			GrxDebugUtil.printErr("initDynamicsSimulator: create failed.", e);
			return null;
		}
		
		try {
			for (int i=0; i<modelList.size(); i++) {
                //<<<<<<< .working
				//GrxModelItem model = (GrxModelItem)modelList.get(i);
				//if (model.bInfo_ != null)
				//	currentDynamics_.registerCharacter(model.getName(), model.bInfo_);
                //=======
                //######
				GrxBaseItem item = modelList.get(i);
				if (item instanceof GrxModelItem) {
					GrxModelItem model = (GrxModelItem)item;
					if (model.bInfo_ != null) 
						dynamics.registerCharacter(model.getName(), model.bInfo_);
				}
                // >>>>>>> .merge-right.r1667
			}
			
			IntegrateMethod m = IntegrateMethod.EULER;
			dynamics.init(0.005, m, SensorOption.ENABLE_SENSOR);
			dynamics.setGVector(new double[] { 0.0, 0.0, 9.8});
			
			for (int i=0; i<modelList.size(); i++) {
				GrxBaseItem item = modelList.get(i);
				if (item instanceof GrxModelItem) {
					GrxModelItem model = (GrxModelItem)item;
					if (model.lInfo_ == null)
						continue;
					String name = model.getName();
					String base = model.lInfo_[0].name;
					// Set initial robot position and attitude
					dynamics.setCharacterLinkData(
						name, base, LinkDataType.ABS_TRANSFORM, 
						model.getTransformArray(base));
	
					// Set joint values
					dynamics.setCharacterAllLinkData(
						name, LinkDataType.JOINT_VALUE, 
						model.getJointValues());
				}
			}
			dynamics.calcWorldForwardKinematics();
			
            // Set collision pair 
			List<GrxBaseItem> collisionPair = manager_.getSelectedItemList(GrxCollisionPairItem.class);
			for (int i=0; i<collisionPair.size(); i++) {
				GrxBaseItem pair = collisionPair.get(i);
				dynamics.registerCollisionCheckPair(
						pair.getStr("objectName1", ""), pair.getStr("jointName1", ""), 
						pair.getStr("objectName2", ""), pair.getStr("jointName2", ""), 
						pair.getDbl("staticFriction", 0.5), 
						pair.getDbl("slidingFriction", 0.5),
						pair.getDblAry("springConstant", new double[]{0, 0, 0, 0, 0, 0}), 
						pair.getDblAry("damperConstant", new double[]{0, 0, 0, 0, 0, 0})
				); 
			}

			// Initialize server
			dynamics.initSimulation();
		} catch (Exception e) {
			GrxDebugUtil.printErr("initDynamicsSimulator:", e);
			return null;
		}
		return dynamics;
	}

	public Collision[] getCollision(List<GrxModelItem> modelList) {
		for (int i=0; i<modelList.size(); i++)  {
			GrxModelItem model = modelList.get(i);
			String name = model.getName();
			String base = model.lInfo_[0].name;
			double[] data = model.getTransformArray(base);
			currentDynamics_.setCharacterLinkData(name, base, LinkDataType.ABS_TRANSFORM, data);
			data = model.getJointValues();
			currentDynamics_.setCharacterAllLinkData(name, LinkDataType.JOINT_VALUE, data);
		}
		currentDynamics_.checkCollision();
		WorldStateHolder wsH = new WorldStateHolder();
		currentDynamics_.getWorldState(wsH);
		return wsH.value.collisions;
	}
}

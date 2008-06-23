/*
 *  GrxModelItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.*;

import javax.swing.ImageIcon;
//import javax.swing.JMenuItem;
//import javax.swing.SwingUtilities;
import javax.media.j3d.*;
import javax.vecmath.*;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;

import com.sun.j3d.loaders.vrml97.VrmlLoader;
import com.sun.j3d.loaders.vrml97.VrmlScene;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.picking.PickTool;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.view.tdview.*;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.CameraPackage.CameraParameter;
import jp.go.aist.hrp.simulator.CameraPackage.CameraType;

@SuppressWarnings("serial")
public class GrxModelItem extends GrxBaseItem implements Manipulatable {
	public static final String TITLE = "Model";
	public static final String FILE_EXTENSION = "wrl";
		
	private boolean isRobot_ = true;
	public boolean update_ = true;
	
	public BranchGroup bgRoot_;
	public CharacterInfo cInfo_;
	public LinkInfoLocal[] lInfo_;
	public LinkInfoLocal activeLinkInfo_;
	private int[] jointToLink_; // length = joint number
	private final Map<String, LinkInfoLocal> lInfoMap_ = new HashMap<String, LinkInfoLocal>();
	private final Vector<Shape3D> shapeVector_ = new Vector<Shape3D>();
	private final Map<SensorType, List<SensorInfoLocal>> sensorMap_ = new HashMap<SensorType, List<SensorInfoLocal>>();

	private Switch switchCom_;
	private TransformGroup tgCom_;
	private Switch switchComZ0_;
	private TransformGroup tgComZ0_;
	private static final double DEFAULT_RADIOUS = 0.05;
    
	private Transform3D t3d = new Transform3D(); 
	private Transform3D t3dm = new Transform3D(); 
	private Vector3d v3d = new Vector3d();
	private AxisAngle4d a4d = new AxisAngle4d();
	private Matrix3d m3d = new Matrix3d();
	private Matrix3d m3d2 = new Matrix3d();
    private Vector3d v3d2 = new Vector3d();

    /*
   	private static final ImageIcon robotIcon = 
   		new ImageIcon(GrxModelItem.class.getResource("/resources/images/robot.png"));
   	private static final ImageIcon envIcon = 
   		new ImageIcon(GrxModelItem.class.getResource("/resources/images/environment.png"));
   	*/
   	private static final String robotIcon = "robot.png";
   	private static final String envIcon = "environment.png";

	//private JMenuItem menuChangeType_ = new JMenuItem("change into environment model");
	class MenuChangeType extends Action {
        String s1 = "change into environment model";
        String s2 = "change into robot model";
		String s=s1;
        public String getText(){ return s; }
        public void toggleMode(){
        	if( s.equals(s1) )
        		s=s2;
        	else
        		s=s1;
        }
		public void run(){
			_setModelType(!isRobot_);
		}
	};
	MenuChangeType menuChangeType_ = new MenuChangeType();

	public GrxModelItem(String name, GrxPluginManager item) {
		super(name, item);
		setIcon(robotIcon);
		/*
		JMenuItem reload = new JMenuItem("reload");
		reload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manager_.processingWindow_.setMessage("reloading model :"+getName()+" ...");
				manager_.processingWindow_.setVisible(true);
				Thread t = new Thread() {
					public void run() {
						load(GrxModelItem.this.file_);
						manager_.processingWindow_.setVisible(false);
					}
				};
				t.start();
			}
		});
		setMenuItem(reload);
		
		menuChangeType_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				_setModelType(!isRobot_);
			}
		});
		setMenuItem(menuChangeType_);
		*/
	
		setMenuItem(new Action(){
            public String getText(){ return "reload"; }
			public void run(){
				load(GrxModelItem.this.file_);
			}
		});

        setMenuItem(menuChangeType_);
	}

	public void restoreProperties() {
		super.restoreProperties();
		
		_setModelType(isTrue("isRobot", isRobot_));
		_setupMarks();
		propertyChanged();
	}
	
	public void propertyChanged() {
		double[] p = getDblAry(lInfo_[0].name+".translation", null);
		if (p == null || p.length != 3)
			p = new double[]{0, 0, 0};
		
		double[] R = getDblAry(lInfo_[0].name+".rotation", null);
		if (R != null && R.length == 4) {  // AxisAngle
			Matrix3d m3d = new Matrix3d();
			m3d.set(new AxisAngle4d(R));
			R = new double[9];
			for (int i=0; i<3; i++) {
				for (int j=0; j<3; j++) {
					R[i*3+j] = m3d.getElement(i, j);
				}
			}
		} else {
			R = new double[]{1, 0 ,0 , 0, 1, 0, 0, 0, 1};
		}
		
		_setTransform(0, p, R);
		
		for (int j = 0; j < lInfo_.length; j++) 
			lInfo_[j].jointValue = getDbl(lInfo_[j].name + ".angle", 0.0);
		
		calcForwardKinematics();
	}
	
	private void _setModelType(boolean isRobot) {
		isRobot_ = isRobot;
		setProperty("isRobot", String.valueOf(isRobot_));

		if (isRobot_) {
			setIcon(robotIcon);
			menuChangeType_.toggleMode();
		} else {
			setIcon(envIcon);
			menuChangeType_.toggleMode();
			setVisibleCoM(false);
			setVisibleCoMonFloor(false);
		}
		manager_.reselectItems();
	}
	private void _setupMarks() {
		double radious = getDbl("markRadious", DEFAULT_RADIOUS);
		if (switchCom_ == null || radious != DEFAULT_RADIOUS) {
			switchCom_ = createBall(radious, new Color3f(1.0f, 1.0f, 0.0f));
			switchComZ0_= createBall(radious, new Color3f(0.0f, 1.0f, 0.0f)); 
			tgCom_ = (TransformGroup)switchCom_.getChild(0);
			tgComZ0_ = (TransformGroup)switchComZ0_.getChild(0);
			lInfo_[0].tg.addChild(switchCom_);
			lInfo_[0].tg.addChild(switchComZ0_);
		}
	}
	
	public boolean load(File f) {
		if (bgRoot_ != null)
			manager_.setSelectedItem(this, false);
		bgRoot_ = new BranchGroup();
		bgRoot_.setCapability(BranchGroup.ALLOW_DETACH);

		file_ = f;
		String url = "file:///" + f.getAbsolutePath();
		GrxDebugUtil.println("Loading " + url);
		
		// TODO:モデルローダのIPを、Propertyから持ってくるようにした。この方式でいいかな？grxuirc.xmlよりいいと思うが...
		String loaderName = System.getProperty("com.generalrobotix.grxui.modelloader");
		if( loaderName == null )
			loaderName = "localhost";

		try {
			ModelLoader mloader = ModelLoaderHelper.narrow(
				GrxCorbaUtil.getReference("ModelLoader", loaderName, 2809));
			cInfo_ = mloader.loadURL(url);
			lInfo_ = new LinkInfoLocal[cInfo_.links().length];
			lInfoMap_.clear();
			
			int jointCount = 0;
			for (int i = 0; i < lInfo_.length; i++) {
				lInfo_[i] = new LinkInfoLocal(cInfo_.links()[i]);
				lInfoMap_.put(lInfo_[i].name, lInfo_[i]);
				if (lInfo_[i].jointId >= 0)
					jointCount++;
			}
			
			jointToLink_ = new int[jointCount];
			for (int i=0; i<jointCount; i++) {
				for (int j=0; j<lInfo_.length; j++) {
					if (lInfo_[j].jointId == i)
						jointToLink_[i] = j;
				}
			}
			
			Iterator<List<SensorInfoLocal>> it = sensorMap_.values().iterator();
			while (it.hasNext()) {
				Collections.sort(it.next());
			}
			
			_loadVrmlScene(url);
			setURL(url);
			manager_.setSelectedItem(this, true);
		} catch (Exception ex) {
            System.out.println("Failed to load vrml model:" + url);
			//ex.printStackTrace();
			return false;
		}
		return true;
	}
	private void _loadVrmlScene(String url) throws BadLinkStructureException {
		VrmlScene scene;
		try {
			scene = (VrmlScene) new VrmlLoader().load(new URL(url));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		Map nodeMap = scene.getNamedObjects();
		for (int i=0 ;i<lInfo_.length; i++) {
			LinkInfoLocal info = lInfo_[i];
			Link l = (Link) nodeMap.get(info.name);
			SharedGroup sg = l.getSharedGroup();
			if (sg.numChildren() > 0) {
				TransformGroup g = (TransformGroup)sg.getChild(0);
				sg.removeChild(0);
				if (i ==0)
					bgRoot_.addChild(g);
				else
					new BranchGroup().addChild(g);
				info.tg = g;
				for (int j=0; j<info.sensors.length; j++) {
					SensorInfoLocal si = info.sensors[j];
					if (si.type.equals(SensorType.VISION_SENSOR)) {
						Camera_impl camera = cameraList.get(si.id);
						g.addChild(camera.getBranchGraph());
						double[] pos = si.translation;
						double[] rot = si.rotation;
						Transform3D t3d = new Transform3D();
						t3d.setTranslation(new Vector3d(pos));
						t3d.setRotation(new Matrix3d(rot));
						camera.getTransformGroup().setTransform(t3d);
					}
				}
			}
		}
		
		SceneGraphModifier modifier = SceneGraphModifier.getInstance();
		for (int i = 0; i < lInfo_.length; i++) {
			Map<String, Object> userData = new Hashtable<String, Object>();
			LinkInfoLocal info = lInfo_[i];
			userData.put("object", this);
			userData.put("linkInfo", info);
			userData.put("objectName", this.getName());
			userData.put("jointName", info.name);
			Vector3d jointAxis = new Vector3d(info.jointAxis);
			LinkInfoLocal itmp = info;
			while (itmp.jointId == -1 && itmp.mother != -1)
				itmp = lInfo_[itmp.mother];
			userData.put("controllableJoint", itmp.name);
	        
			TransformGroup g = lInfo_[i].tg;
			Transform3D t3dOrg = new Transform3D();	
			g.getTransform(t3dOrg);
			
	        Transform3D tr = new Transform3D();
			tr.setIdentity();
	        g.setTransform(tr);
	        
		    modifier.init_ = true;	
	        modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
			modifier._calcUpperLower(g, tr);
			g.setTransform(t3dOrg);
			
			Color3f color = new Color3f(1.0f, 0.0f, 0.0f);
			Switch bbSwitch =  modifier._makeSwitchNode(modifier._makeBoundingBox(color));
			bbSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
			bbSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
			bbSwitch.setCapability(Switch.ALLOW_CHILDREN_READ);
			bbSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
			g.addChild(bbSwitch);
			
			userData.put("boundingBoxSwitch", bbSwitch);
			if (jointAxis != null) {
				Switch axisSwitch = modifier._makeSwitchNode(modifier._makeAxisLine(jointAxis));
				g.addChild(axisSwitch);
				userData.put("axisLineSwitch", axisSwitch);
			}
			
			g.setUserData(userData);
			g.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
			g.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
			
			int mid = lInfo_[i].mother;
			if (mid != -1) {
				Group mg = (Group)lInfo_[mid].tg.getParent();
				mg.addChild(g.getParent());
			} 
		}
		
		lInfo_[0].tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
		_setupMarks();
		
		_traverse(bgRoot_, 0);
		
		modifier.modifyRobot(this);
		
		for (int i = 0; i < lInfo_.length; i++) {
			Node n = lInfo_[i].tg.getChild(0);
			if (n.getCapability(Node.ENABLE_PICK_REPORTING))
				n.clearCapability(Node.ENABLE_PICK_REPORTING);
		}
		calcForwardKinematics();
		updateInitialTransformRoot();
		updateInitialJointValues();
	}
	private void _traverse(Node node, int depth) {
		if (node instanceof Switch) {
			return;
		} else if (node instanceof BranchGroup) {
			BranchGroup bg = (BranchGroup) node;
			bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
			bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
			for (int i = 0; i < bg.numChildren(); i++)
				_traverse(bg.getChild(i), depth + 1);
			
		} else if (node instanceof TransformGroup) {
			TransformGroup tg = (TransformGroup) node;
			tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
			tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
			tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			tg.setCapability(TransformGroup.ALLOW_LOCAL_TO_VWORLD_READ);
			for (int i = 0; i < tg.numChildren(); i++)
				_traverse(tg.getChild(i), depth + 1);
			
		} else if (node instanceof Group) {
			Group g = (Group) node;
			g.setCapability(Group.ALLOW_CHILDREN_READ);
			g.setCapability(Group.ALLOW_CHILDREN_WRITE);
			for (int i = 0; i < g.numChildren(); i++)
				_traverse(g.getChild(i), depth + 1);
			
		} else if (node instanceof Link) {
			Link l = (Link) node;
			l.setCapability(Link.ALLOW_SHARED_GROUP_READ);
			SharedGroup sg = l.getSharedGroup();
			sg.setCapability(SharedGroup.ALLOW_CHILDREN_READ);
			for (int i = 0; i < sg.numChildren(); i++)
				_traverse(sg.getChild(i), depth + 1);
			
		} else if (node instanceof Shape3D) {
			Shape3D s3d = (Shape3D) node;
			s3d.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
			s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
			s3d.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
			s3d.setCapability(GeometryArray.ALLOW_COUNT_READ);
			PickTool.setCapabilities(s3d, PickTool.INTERSECT_FULL);
			
			Appearance app = s3d.getAppearance();
			if (app != null) {
				app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
				TransparencyAttributes ta = app.getTransparencyAttributes();
				if (ta != null) {
					ta.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
					ta.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
					ta.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
					ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
				}

				app.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
				PolygonAttributes pa = app.getPolygonAttributes();
				if (pa == null) {
					pa = new PolygonAttributes();
					pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
					app.setPolygonAttributes(pa);
				}
				pa.setCapability(PolygonAttributes.ALLOW_MODE_READ);
				pa.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);

				app.setCapability(Appearance.ALLOW_MATERIAL_READ);
				Material ma = app.getMaterial();
				if (ma != null) {
					ma.setCapability(Material.ALLOW_COMPONENT_READ);
					ma.setCapability(Material.ALLOW_COMPONENT_WRITE);
				}
			}
			shapeVector_.add(s3d);
		} else {
			GrxDebugUtil.println("* The node " + node.toString()
					+ " is not supported.");
		}
	}

	public void setCharacterPos(LinkPosition[] lpos, SensorState sensor) {
		if (!update_)
			return;
		
		for (int i=0; i<lInfo_.length; i++) {
			if (lpos[i] != null) {
				_setTransform(i, lpos[i].p, lpos[i].R);
			}
		}
		
		if (sensor != null && sensor.q != null)	{
			for (int i=0; i<jointToLink_.length; i++) {
				lInfo_[jointToLink_[i]].jointValue = sensor.q[i];
			}
		}
		
		_updateCoM();
	}
	public void setTransformRoot(Vector3d pos, Matrix3d rot) {
		_setTransform(0, pos, rot);
	}
	private void _setTransform(int linkId, Vector3d pos, Matrix3d rot) {
		TransformGroup tg = lInfo_[linkId].tg;
		tg.getTransform(t3d);
		if (pos != null)
			t3d.set(pos);
		if (rot != null)
			t3d.setRotation(rot);
		tg.setTransform(t3d);
	}
	private void _setTransform(int linkId, double[] p, double[] R) {
		Vector3d pos = null;
		Matrix3d rot = null;
		if (p != null)
			pos = new Vector3d(p);
		if (R != null)
			rot = new Matrix3d(R);
		_setTransform(linkId, pos, rot);
	}

	public void setJointValue(String jname, double value) {
		LinkInfoLocal l = getLinkInfo(jname);
		if (l != null) 
			l.jointValue = value;
	}
	public void setJointValues(final double[] values) {
		if (values.length != jointToLink_.length)
			return;
		for (int i=0; i<jointToLink_.length; i++) {
			LinkInfoLocal l = lInfo_[jointToLink_[i]];
			l.jointValue = values[i];
		}
	}
	public void setJointValuesWithinLimit() {
	    for (int i = 1; i < lInfo_.length; i++) {
	    	LinkInfoLocal l = lInfo_[i];
	    	if (l.llimit[0] < l.ulimit[0]) {
				if (l.jointValue < l.llimit[0]) {
					l.jointValue = l.llimit[0];
				}else if (l.ulimit[0] < l.jointValue) {
					l.jointValue = l.ulimit[0];
				}
			}
		}
	}
		
	public void updateInitialTransformRoot() {
        // 普通のアトリビュート設定
        Transform3D t3d = new Transform3D();
        Matrix3d m3d = new Matrix3d();
        Vector3d v3d = new Vector3d();
        
        lInfo_[0].tg.getTransform(t3d);
        t3d.get(m3d, v3d);
        setDblAry(lInfo_[0].name+".translation", new double[]{v3d.x, v3d.y, v3d.z});
        
        AxisAngle4d a4d = new AxisAngle4d();
        a4d.set(m3d);
        setDblAry(lInfo_[0].name+".rotation", new double[]{a4d.x, a4d.y, a4d.z, a4d.angle});
	}
	public void updateInitialJointValue(String jname) {
		LinkInfoLocal l = getLinkInfo(jname);
		if (l != null)
			setDbl(jname+".angle", l.jointValue);
	}
	public void updateInitialJointValues() {
		for (int i=0; i<jointToLink_.length; i++) {
			LinkInfoLocal l = lInfo_[jointToLink_[i]];
			setDbl(l.name+".angle", l.jointValue);
		}
	}

	public  void calcForwardKinematics() {
		calcForwardKinematics(0);
		_updateCoM();
	}
	private void calcForwardKinematics(int linkId) {
		if (linkId < 0)
			return;
		
		LinkInfoLocal l = lInfo_[linkId];
		if (linkId != 0 && l.mother != -1) {
			lInfo_[l.mother].tg.getTransform(t3dm);

                        if (l.jointType.equals("rotate")) {
                                v3d.set(l.translation[0], l.translation[1], l.translation[2]);
                                t3d.setTranslation(v3d);
			
                                m3d.set(l.rotation);
                                a4d.set(l.jointAxis[0], l.jointAxis[1], l.jointAxis[2], lInfo_[linkId].jointValue);
                                m3d2.set(a4d);
                                m3d.mul(m3d2);
                                t3d.setRotation(m3d);
                        }
                        else if(l.jointType.equals("slide")) {
                                v3d.set(l.translation[0], l.translation[1], l.translation[2]);
                                v3d2.set(l.jointAxis[0], l.jointAxis[1], l.jointAxis[2]);
                                v3d2.scale(lInfo_[linkId].jointValue);
                                v3d.add(v3d2);
                                t3d.setTranslation(v3d);
			
                                m3d.set(l.rotation);
                                m3d.mul(m3d);
                                t3d.setRotation(m3d);
                        }

                        t3dm.mul(t3d);
			l.tg.setTransform(t3dm);
		}
		
		calcForwardKinematics(l.daughter);
		calcForwardKinematics(l.sister);
	}
	private void _updateCoM() {
		if (switchCom_.getWhichChild() == Switch.CHILD_ALL ||
				switchComZ0_.getWhichChild() == Switch.CHILD_ALL) {
			getCoM(v3d);
			Vector3d vz0 = new Vector3d(v3d);
			
			_globalToRoot(v3d);
			t3d.set(v3d);
			tgCom_.setTransform(t3d);
			
			vz0.z = 0.0;
			_globalToRoot(vz0);
			t3d.set(vz0);
			tgComZ0_.setTransform(t3d);
		}
	}
	private void _globalToRoot(Vector3d pos) {

		Transform3D t3d = new Transform3D();
		lInfo_[0].tg.getTransform(t3d);
		Vector3d p = new Vector3d();
		t3d.get(p);
		t3d.invert();
		pos.sub(p);
		t3d.transform(pos);
	}

	public void getCoM(Vector3d pos) {
		pos.x = 0.0;
		pos.y = 0.0;
		pos.z = 0.0;
		double totalMass = 0.0;
		
		Vector3d absCom = new Vector3d();
		Vector3d p = new Vector3d();
		Transform3D t3d = new Transform3D();
		for (int i = 0; i < lInfo_.length; i++) {
			totalMass += lInfo_[i].mass;
			absCom.set(lInfo_[i].centerOfMass);
			TransformGroup tg = lInfo_[i].tg;
			tg.getTransform(t3d);
			t3d.transform(absCom);
			t3d.get(p);
			absCom.add(p);
			absCom.scale(lInfo_[i].mass);
			pos.add(absCom);
		}
		pos.scale(1.0 / totalMass);
	}

	public String[] getSensorNames(SensorType type) {
		List<SensorInfoLocal> l = sensorMap_.get(type);
		if (l == null)
			return null;
		
		String[] ret = new String[l.size()];
		for (int i=0; i<ret.length; i++)
			ret[i] = l.get(i).name;
		return ret;
	}

	public LinkInfoLocal getLinkInfo(String linkName) {
		return lInfoMap_.get(linkName);
	}
	
	public Transform3D getTransform(String linkName) {
		LinkInfoLocal l = getLinkInfo(linkName);
		if (l == null)
			return null;
		Transform3D ret = new Transform3D();
		l.tg.getTransform(ret);
		return ret;
	}
	public Transform3D getTransformfromRoot(String linkName) {
		Transform3D t3d = getTransform(linkName);
		if (t3d == null)
			return null;
		Transform3D t3dr = new Transform3D();
		lInfo_[0].tg.getTransform(t3dr);
		t3d.mulTransposeLeft(t3dr, t3d);
		return t3d;
	}
	public TransformGroup getTransformGroupRoot() {
		return lInfo_[0].tg;
	}
	
	public double[] getTransformArray(String linkName) {
		Transform3D t3d = getTransform(linkName);
		Matrix3d mat = new Matrix3d();
		Vector3d vec = new Vector3d();
		t3d.get(mat, vec);
		
		double[] ret = new double[12];
		vec.get(ret);
		ret[3] = mat.m00; ret[4] = mat.m01; ret[5] = mat.m02;
		ret[6] = mat.m10; ret[7] = mat.m11; ret[8] = mat.m12;
		ret[9] = mat.m20; ret[10]= mat.m21; ret[11]= mat.m22;
		
		return ret;
	}
	public double[] getInitialTransformArray(String linkName) {
		double[] ret = getTransformArray(linkName);
		
		double[] p = getDblAry(linkName+".translation", null);
		if (p != null && p.length == 3) {
			System.arraycopy(p, 0, ret, 0, 3);
		}
		
		double[] r = getDblAry(linkName+".rotation", null);
		if (r != null && r.length == 4) {
			Matrix3d mat = new Matrix3d();
			mat.set(new AxisAngle4d(r));
			ret[3] = mat.m00; ret[4] = mat.m01; ret[5] = mat.m02;
			ret[6] = mat.m10; ret[7] = mat.m11; ret[8] = mat.m12;
			ret[9] = mat.m20; ret[10]= mat.m21; ret[11]= mat.m22;
		} 
			
		return ret;
	}
	
	public double   getJointValue(String jname) {
		LinkInfoLocal l = getLinkInfo(jname);
		if (l != null)
			return l.jointValue;
		return 0.0;
	} 
	public double[] getJointValues() {
		double[] vals = new double[jointToLink_.length];
		for (int i=0; i<jointToLink_.length; i++)
			vals[i] = lInfo_[jointToLink_[i]].jointValue;
		return vals;
	}
    public double[] getInitialJointValues() {
    	double[] ret = new double[jointToLink_.length];
		for (int i=0; i<ret.length; i++) {
			LinkInfoLocal l = lInfo_[jointToLink_[i]];
			String jname = l.name;
			ret[i] = getDbl(jname+".angle", l.jointValue);
		}
		return ret;
    }
    public double[] getInitialJointMode() {
    	double[] ret = new double[jointToLink_.length];
		for (int i=0; i<ret.length; i++) {
			String jname = lInfo_[jointToLink_[i]].name;
			String mode = getStr(jname+".mode", "Torque");
			ret[i] = mode.equals("HighGain") ? 1.0 : 0.0;
		}
		return ret;
    }

	public boolean isRobot() {
		return isRobot_;
	}

	public void setSelected(boolean b) {
		super.setSelected(b);
		if (!b) {
			bgRoot_.detach();
			for (int i=0; i<cameraList.size(); i++)
				cameraList.get(i).getBranchGraph().detach();
		}
	}
	public void delete() {
		super.delete();
		bgRoot_.detach();
	}

	public void setVisibleCoM(boolean b) {
		if (isRobot()) {
			switchCom_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
			calcForwardKinematics();
		} else {
			switchCom_.setWhichChild(Switch.CHILD_NONE);
		}
	}
	public void setVisibleCoMonFloor(boolean b) {
		if (isRobot()) {
			switchComZ0_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
			calcForwardKinematics();
		} else {
			switchComZ0_.setWhichChild(Switch.CHILD_NONE);
		}
	}
	public void setWireFrame(boolean b) {
    	for (int i = 0; i<shapeVector_.size(); i++){
    	    Shape3D s3d = (Shape3D)shapeVector_.get(i);
    	    Appearance app = s3d.getAppearance();
    	    if (app != null){
    	        PolygonAttributes pa = app.getPolygonAttributes();
    	        if (b){
    	        	pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
    	        } else {
    	        	pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
    	        }
    	    }
    	}
	}
    public void setTransparencyMode(boolean b){
    	for (int i = 0; i<shapeVector_.size(); i++){
    	    Shape3D s3d = (Shape3D)shapeVector_.get(i);
    	    Appearance app = s3d.getAppearance();
    	    if (app != null){
    			TransparencyAttributes ta = app.getTransparencyAttributes();
    			if (ta != null){
        			if (b){
        			    ta.setTransparency(0.5f);
        			    ta.setTransparencyMode(TransparencyAttributes.FASTEST);
        			}else{
        			    ta.setTransparency(0f);
        			    ta.setTransparencyMode(TransparencyAttributes.NONE);
        			}
    			}
    	    }
    	}
    }
    
	private Switch createBall(double radius, Color3f c) {
		Material m = new Material();
		m.setDiffuseColor(c);
		m.setSpecularColor(0.01f, 0.10f, 0.02f);
		m.setLightingEnable(true);
		Appearance app = new Appearance();
		app.setMaterial(m);
		Node sphere = new Sphere((float)radius, Sphere.GENERATE_NORMALS, app);
		sphere.setPickable(false);
		
		Transform3D trans = new Transform3D();
		trans.setTranslation(new Vector3d(0.0, 0.0, 0.0));
		trans.setRotation(new AxisAngle4d(1.0, 0.0, 0.0, 0.0));
		TransformGroup tg = new TransformGroup(trans);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
		tg.addChild(sphere);
		
		Switch ret = new Switch();
        ret.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
        ret.setCapability(Switch.ALLOW_CHILDREN_READ);
        ret.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		ret.setCapability(Switch.ALLOW_SWITCH_READ);
		ret.setCapability(Switch.ALLOW_SWITCH_WRITE);
		ret.addChild(tg);
		
		return ret;
	}
	
	public class LinkInfoLocal {
		final public String name;
		final public int jointId;
		final public int mother;
		final public int daughter;
		final public int sister;
		final public String jointType;
		final public double[] jointAxis;
		final public double[] translation;
		final public double[] rotation;
		final public double[] centerOfMass;
		final public double mass;
		final public double[] inertia;
		final public double[] ulimit;
		final public double[] llimit;
		final public double[] uvlimit;
		final public double[] lvlimit;
		final public SensorInfoLocal[] sensors;
		
		public double  jointValue;
		public TransformGroup tg;
		
		public LinkInfoLocal(LinkInfo info) {
			name = info.name();
			jointId = info.jointId();
			mother = info.mother();
			daughter = info.daughter();
			sister = info.sister();

			jointType = info.jointType();
			jointAxis = info.jointAxis();
			translation    = info.translation();
			rotation    = info.rotation();
			centerOfMass = info.centerOfMass();
			mass    = info.mass();
			inertia = info.inertia();
			ulimit  = info.ulimit();
			llimit  = info.llimit();
			uvlimit = info.uvlimit();
			lvlimit = info.lvlimit();
			
			jointValue = 0.0;
			
			SensorInfo[] sinfo = info.sensors();
			sensors = new SensorInfoLocal[sinfo.length];
			for (int i=0; i<sensors.length; i++) {
				sensors[i] = new SensorInfoLocal(sinfo[i], LinkInfoLocal.this);
				List<SensorInfoLocal> l = sensorMap_.get(sensors[i].type);
				if (l == null) {
					l = new ArrayList<SensorInfoLocal>();
					sensorMap_.put(sensors[i].type, l);
				}
				l.add(sensors[i]);
				
				if (sensors[i].type.equals(SensorType.VISION_SENSOR)) {
					CameraParameter prm = new CameraParameter();
					prm.defName = new String(sensors[i].name);
					prm.sensorName = new String(sensors[i].name);
					prm.sensorId = sensors[i].id;
					
					prm.frontClipDistance = (float)sensors[i].maxValue[0];
					prm.backClipDistance = (float)sensors[i].maxValue[1];
					prm.fieldOfView = (float)sensors[i].maxValue[2];
					try {
						prm.type = CameraType.from_int((int)sensors[i].maxValue[3]);
					} catch (Exception e) {
						prm.type = CameraType.NONE;
					}
					prm.width  = (int)sensors[i].maxValue[4];
					prm.height = (int)sensors[i].maxValue[5];
					boolean offScreen = false;
					//if (prm.type.equals(CameraType.DEPTH))
				    //		offScreen = true;
					Camera_impl camera = new Camera_impl(prm, offScreen);
					cameraList.add(camera);
				}
			}
		}
	}
	
	List<Camera_impl> cameraList = new ArrayList<Camera_impl>();
	public List<Camera_impl> getCameraSequence () {
		return cameraList;
	}
	
	private class SensorInfoLocal implements Comparable {
		final String name;
		final public SensorType type;
		final int id;
		final public double[] translation;
		final public double[] rotation;
		final public double[] maxValue;
		final public LinkInfoLocal parent_;
		
		public SensorInfoLocal(SensorInfo info, LinkInfoLocal parentLink) {
			name = info.name();
			type = info.type();
			id = info.id();
			translation = info.translation();
			rotation = info.rotation();
			maxValue = info.maxValue();
			parent_ = parentLink;
		}

		public int compareTo(Object o) {
			if (o instanceof SensorInfoLocal) {
				SensorInfoLocal s = (SensorInfoLocal) o;
				if (type.value() < s.type.value())
					return -1;
				else if (type == s.type && id < s.id)
					return -1;
			}
			return 1;
		}
	}

/* for future use
	private Map<String, vrml.node.Node> def2NodeMap_ = new HashMap<String, vrml.node.Node>();
	private Map<SceneGraphObject, String> node2DefMap_ = new HashMap<SceneGraphObject, String>();
	
	private String _getNodeType(String defName) {
		vrml.node.Node node = def2NodeMap_.get(defName);
		if (node != null)
			return node.getType();
		return null;
	}
	
	private void initNodeMap(VrmlScene scene) {
		Map m = scene.getDefineTable();
		Iterator it2 = m.keySet().iterator();
		while (it2.hasNext()) {
			String key = (String)it2.next();
			vrml.node.Node node = (vrml.node.Node)m.get(key);
			def2NodeMap_.put(key, node);
			node2DefMap_.put(node.getImplObject(), key);
			//try {
				//vrml.field.MFFloat  llimit = (vrml.field.MFFloat)node.getExposedField("llimit");
			//} catch (Exception e) {
				
			//}
			//try {
				//vrml.field.MFFloat  ulimit = (vrml.field.MFFloat)node.getExposedField("ulimit");
			//} catch (Exception e) {
			//}
		}
	}
	
	String getNodeTypeString(Scene scene, String defName)
    {
        try{
            com.sun.j3d.loaders.vrml97.impl.BaseNode node = scene.use(defName);
            String strTemp = node.toString();
//            System.out.println(strTemp);
            StringTokenizer st = new StringTokenizer(strTemp," ");

            String strOneTemp = "";
            while(st.hasMoreTokens())
            {
                strOneTemp = st.nextToken();
//                System.out.println(strOneTemp);
                if(strOneTemp.equals("PROTO"))
                {
                    strOneTemp = st.nextToken();
                    break;
                }
            }

//            System.out.println(strOneTemp);
            return strOneTemp;
        }catch(Exception e)
        {
            return null;
        }
    }
*/	
}

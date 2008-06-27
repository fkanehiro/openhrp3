/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/*
 *  GrxModelItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.vecmath.*;
import javax.media.j3d.*;

import com.sun.j3d.loaders.vrml97.VrmlLoader;
import com.sun.j3d.loaders.vrml97.VrmlScene;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.image.*;
import com.sun.j3d.utils.picking.PickTool;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.view.tdview.*;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

import jp.go.aist.hrp.simulator.*;
import jp.go.aist.hrp.simulator.CameraPackage.CameraParameter;
import jp.go.aist.hrp.simulator.CameraPackage.CameraType;
import java.awt.image.BufferedImage;
import jp.go.aist.hrp.simulator.ImageData;
import jp.go.aist.hrp.simulator.PixelFormat;

@SuppressWarnings("unchecked")
public class GrxModelItem extends GrxBaseItem implements Manipulatable {
	public static final String TITLE = "Model";
	public static final String DEFAULT_DIR = "../../etc";
	public static final String FILE_EXTENSION = "wrl";

	private boolean isRobot_ = true;
	public boolean update_ = true;
	
	public BranchGroup bgRoot_;
	public BodyInfo bInfo_;
	public LinkInfoLocal[] lInfo_;
	public LinkInfoLocal activeLinkInfo_;
	private int[] jointToLink_; // length = joint number
	private final Map<String, LinkInfoLocal> lInfoMap_ = new HashMap<String, LinkInfoLocal>();
	private final Vector<Shape3D> shapeVector_ = new Vector<Shape3D>();
	private final Map<String, List<SensorInfoLocal>> sensorMap_ = new HashMap<String, List<SensorInfoLocal>>();
	private List<Camera_impl> cameraList = new ArrayList<Camera_impl>();
	
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

	private static final ImageIcon robotIcon = 
		new ImageIcon(GrxModelItem.class.getResource("/resources/images/robot.png"));
	private static final ImageIcon envIcon = 
		new ImageIcon(GrxModelItem.class.getResource("/resources/images/environment.png"));

	private JMenuItem menuChangeType_ = new JMenuItem("change into environment model");
		
	public GrxModelItem(String name, GrxPluginManager item) {
		super(name, item);
		setIcon(robotIcon);
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
	}

	public void restoreProperties() {
		super.restoreProperties();
		
		_setModelType(isTrue("isRobot", isRobot_));
		_setupMarks();
		
		if (getDblAry(lInfo_[0].name+".translation", null) == null && 
				getDblAry(lInfo_[0].name+".rotation", null) == null) 
			updateInitialTransformRoot();
		
		for (int i=0; i<jointToLink_.length; i++) {
			LinkInfoLocal l = lInfo_[jointToLink_[i]];
			Double d = getDbl(l.name+".angle", null);
			if (d == null) 
				setDbl(l.name+".angle", 0.0);
		}
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
			menuChangeType_.setText("change into environmental model");
		} else {
			setIcon(envIcon);
			menuChangeType_.setText("change into robot model");
			setVisibleCoM(false);
			setVisibleCoMonFloor(false);
		}
		manager_.reselectItems();
	}

	private void _setupMarks() {
		double radius = getDbl("markRadius", DEFAULT_RADIOUS);
		if (switchCom_ == null || radius != DEFAULT_RADIOUS) {
			switchCom_ = createBall(radius, new Color3f(1.0f, 1.0f, 0.0f));
			switchComZ0_= createBall(radius, new Color3f(0.0f, 1.0f, 0.0f)); 
			tgCom_ = (TransformGroup)switchCom_.getChild(0);
			tgComZ0_ = (TransformGroup)switchComZ0_.getChild(0);
			lInfo_[0].tg.addChild(switchCom_);
			lInfo_[0].tg.addChild(switchComZ0_);
		}
	}

	public boolean load(File f) {
		long load_stime = System.currentTimeMillis();
		if (bgRoot_ != null)
			manager_.setSelectedItem(this, false);
		bgRoot_ = new BranchGroup();
		bgRoot_.setCapability(BranchGroup.ALLOW_DETACH);
		
		file_ = f;
		String url = "file:///" + f.getAbsolutePath();
		GrxDebugUtil.println("Loading " + url);
		try {
			ModelLoader mloader = ModelLoaderHelper.narrow(
				GrxCorbaUtil.getReference("ModelLoader", "localhost", 2809));
			bInfo_ = mloader.getBodyInfo(url);
			//
			LinkInfo[] LinkInfo = bInfo_.links();
			lInfo_ = new LinkInfoLocal[LinkInfo.length];
			lInfoMap_.clear();
			
			for (int i=0; i<cameraList.size(); i++)
				cameraList.get(i).destroy();
			cameraList.clear();
			
			int jointCount = 0;
			for (int i = 0; i < lInfo_.length; i++) {
				lInfo_[i] = new LinkInfoLocal(LinkInfo[i]);
				lInfo_[i].myLinkID = (short)i;
				lInfoMap_.put(lInfo_[i].name, lInfo_[i]);
				if (lInfo_[i].jointId >= 0)
					jointCount++;
			}

			// Search root node.
			int rootIndex = -1;
			for( int i = 0 ; i < lInfo_.length ; i++ )
			{
				if( lInfo_[i].parentIndex < 0 )
				{
					if( rootIndex < 0 )
					{
						rootIndex = i;

					}
					else
					{
						System.out.println( "Error. Two or more root node exist." );
					}
				}
			}
			if( rootIndex < 0 )
			{
				System.out.println( "Error, root node doesn't exist." );
			}

			createLink( rootIndex );
/*
			// ###### DEBUG
			for( int i = 0 ; i < lInfo_.length ; i++ )
			{
				System.out.println( "=====================" );
				System.out.println( "lInfo index = " + i );
				System.out.println( "   parentIndex  = " + lInfo_[i].parentIndex );
				System.out.print  ( "   childIndices = " );
				for( int j = 0 ; j < lInfo_[i].childIndices.length ; j++ )
				{
					System.out.print( lInfo_[i].childIndices[j] + ", " );
				}
				System.out.println( "" );
				System.out.println( "   mother       = " + lInfo_[i].mother );
				System.out.println( "   sister       = " + lInfo_[i].sister );
				System.out.println( "   daughter     = " + lInfo_[i].daughter );

			}
			// ###### DEBUG
*/
			jointToLink_ = new int[jointCount];
			for (int i=0; i<jointCount; i++) {
				for (int j=0; j<lInfo_.length; j++) {
					if (lInfo_[j].jointId == i) {
						jointToLink_[i] = j;
					}
				}
			}
			
			Iterator<List<SensorInfoLocal>> it = sensorMap_.values().iterator();
			while (it.hasNext()) {
				Collections.sort(it.next());
			}
			long stime = System.currentTimeMillis();
			_loadVrmlScene(url);
			long etime = System.currentTimeMillis();
			System.out.println("_loadVrmlScene time = " + (etime-stime) + "ms");
			setURL(url);
			manager_.setSelectedItem(this, true);
		} catch (Exception ex) {
			System.out.println("Failed to load vrml model:" + url);
			ex.printStackTrace();
			return false;
		}
		long load_etime = System.currentTimeMillis();
		System.out.println("load time = " + (load_etime-load_stime) + "ms");
		return true;
	}

	private void createLink( int index ){

		LinkInfoLocal info = lInfo_[index];

		for( int i = 0 ; i < info.childIndices.length ; i++ ) 
		{
			// call recursively
			int childIndex = info.childIndices[i];
			createLink( childIndex );
		}

		for( int i = info.childIndices.length - 1 ; 0 <= i ; i-- )
		{
			// add child in sequence.
			//     Added node is referred to daughter invariably, 
			//     and past daughter is new daughter's sister.
			// child(daughter) -> sisiter.
			lInfo_[info.childIndices[i]].sister = info.daughter;
			lInfo_[info.childIndices[i]].mother = index;
			info.daughter                       = info.childIndices[i];
		}
	}

	private void _loadVrmlScene(String url) throws BadLinkStructureException {

		ShapeInfo[] ShapeInfo = bInfo_.shapes();
		AppearanceInfo[] AppearanceInfo = bInfo_.appearances();
		MaterialInfo[] MaterialInfo = bInfo_.materials();
		TextureInfo[] TextureInfo = bInfo_.textures();

		// World Top TGroup
		TransformGroup gTopgrp = new TransformGroup();
        bgRoot_.addChild(gTopgrp);

		for (int i=0 ;i<lInfo_.length; i++) {
			TransformGroup gTgrp = setTransformGroup(lInfo_[i]);
			int numChildren = lInfo_[i].shapeIndices.length;

			for (int shi=0;shi<numChildren; shi++) {					
				int shpInfIdx=(lInfo_[i].shapeIndices[shi]);
				ShapeInfoLocal shpInfo = new ShapeInfoLocal(ShapeInfo[shpInfIdx]);
				
				GeometryInfo triangleA = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
				triangleA.setCoordinates(shpInfo.vertex2);
				triangleA.setCoordinateIndices(shpInfo.indices);

				int appIndex = (int)(shpInfo.appearanceIndex);
				float creaseAngle=0.0f;
				Appearance ap = new Appearance();;
				if(appIndex != -1){
					PolygonAttributes pa = new PolygonAttributes();
					pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
					ap.setPolygonAttributes(pa);

					AppearanceInfoLocal appInfo = new AppearanceInfoLocal(AppearanceInfo[appIndex]);
					
					Color3f[] diffuseColor = setWhiteColor();
					int matIndex = (int)(appInfo.materialIndex);
					if(matIndex != -1){
						MaterialInfoLocal matInfo = new MaterialInfoLocal( MaterialInfo, matIndex );
						Material ma = setMaterial(matInfo);
						ap.setMaterial(ma);
						ma.getDiffuseColor( diffuseColor[0] );
						ma.getDiffuseColor( diffuseColor[1] );
						ma.getDiffuseColor( diffuseColor[2] );
					}else{
					}

					int texIndex = (int)(appInfo.textureIndex);			
					if(texIndex != -1){
						TextureInfoLocal texInfo = new TextureInfoLocal(TextureInfo, texIndex);
						Texture2D texture2d = setTexture(texInfo);
						if(texture2d!=null){
							ap.setTexture(texture2d);
						}
						int vTris = (shpInfo.indices.length)/3;
						Point2f[] TexPoints = new Point2f[vTris * 3];
						for(int vi=0; vi<vTris; vi++){
							TexPoints[vi*3] = new Point2f( 0.0f , 0.0f );
							TexPoints[vi*3+1] = new Point2f( 1.0f , 0.0f );
							TexPoints[vi*3+2] = new Point2f( 1.0f , 1.0f );
						}
						triangleA.setTextureCoordinates(TexPoints);
						triangleA.setTextureCoordinateIndices(shpInfo.indices);
						//TextureAttributes Set
						TextureAttributes texAttrBase =  new TextureAttributes();
						//TextureAttributes Property Set
						texAttrBase.setTextureMode(TextureAttributes.MODULATE);
						//Appearance <- TextureAttributes 
						ap.setTextureAttributes(texAttrBase);
					}else{
					}
			
					Color3f[] vcolors= new Color3f[shpInfo.indices.length];
					if( setvcolors(shpInfo, appInfo, diffuseColor, vcolors) ){
						triangleA.setColors(vcolors);
						triangleA.setColorIndices(shpInfo.indices);
					}

					//From ModelLoader IDL Normal Vector Set  ( Debug Comment Out ! )
					//Under IF Block Enable -> MoelLoader NormalVector Set
					Vector3f[] normals = new Vector3f[shpInfo.indices.length];																
					int[] normalIndices = new int[shpInfo.indices.length];
					if( setnormals( shpInfo, appInfo, normals, normalIndices ) ){
	                    triangleA.setNormals(normals);
						triangleA.setNormalIndices(normalIndices);
					}
					creaseAngle = appInfo.creaseAngle;
				}else{		//if(appIndex != -1)
				}			
				
				PrimitiveType ptype = shpInfo.type;			
				//Java3D NormalVector Generate
				//Force Java3D NormalVector Generate! 
                NormalGenerator ng = new NormalGenerator((double)creaseAngle);
                ng.generateNormals(triangleA);
				Shape3D shapeObj = new Shape3D(triangleA.getGeometryArray());
				if(appIndex != -1){
					shapeObj.setAppearance( ap );		
				}
				gTgrp.addChild(shapeObj);
			}     //for (int shi=0;shi<numChildren; shi++)

			lInfo_[i].tg = gTgrp;

			for (int j=0; j<lInfo_[i].sensors.length; j++) {
				SensorInfoLocal si = lInfo_[i].sensors[j];

				//if (si.type.equals(SensorType.VISION_SENSOR)) {
				if (si.type.equals("Vision")) {

					Camera_impl camera = cameraList.get(si.id);
					//g.addChild(camera.getBranchGraph());
					gTgrp.addChild(camera.getBranchGraph());

					double[] pos = si.translation;
					// #####[Changed] From BODYINFO Convert 			
					double[] rot = si.rotation;

					Transform3D t3d = new Transform3D();
					t3d.setTranslation(new Vector3d(pos));
					t3d.setRotation(new AxisAngle4d(rot));
					camera.getTransformGroup().setTransform(t3d);
				}
			}
			if(i == 0){
					bgRoot_.addChild(gTgrp);
			}
			else{
					new BranchGroup().addChild(gTgrp);
			}

		} // for (int i=0 ;i<lInfo_.length; i++)

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
			while (itmp.jointId == -1 && itmp.mother != -1) {
				itmp = lInfo_[itmp.mother];
			}
			userData.put("controllableJoint", itmp.name);
			 
			TransformGroup g = lInfo_[i].tg;
			Transform3D tr = new Transform3D();
			tr.setIdentity();
			g.setTransform(tr);
			
			modifier.init_ = true;
			modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
			modifier._calcUpperLower(g, tr);
			
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
		
		setDblAry(lInfo_[0].name+".translation", lInfo_[0].translation);
		setDblAry(lInfo_[0].name+".rotation", lInfo_[0].rotation);
		propertyChanged();

		for (int i=0; i<lInfo_.length; i++) {
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
			GrxDebugUtil.println("* The node " + node.toString() + " is not supported.");
		}
	}

	public void setCharacterPos(LinkPosition[] lpos, double[] q) {
		if (!update_)
			return;
		
		boolean isAllPosProvided = true;
		for (int i=0; i<lInfo_.length; i++) {
			if (lpos[i].p == null || lpos[i].R == null)
				isAllPosProvided = false;
			else
				_setTransform(i, lpos[i].p, lpos[i].R);
		}
		
		if (q != null) {
			for (int i=0; i<jointToLink_.length; i++)
				lInfo_[jointToLink_[i]].jointValue = q[i];
		}
		
		if (isAllPosProvided)
			_updateCoM();
		else
			calcForwardKinematics();
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
				if (l.jointValue < l.llimit[0])
					l.jointValue = l.llimit[0];
				else if (l.ulimit[0] < l.jointValue)
					l.jointValue = l.ulimit[0];
			}
		}
	}

	public void updateInitialTransformRoot() {
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
			
			if (l.jointType.equals("rotate") || l.jointType.equals("fixed")) {
				v3d.set(l.translation[0], l.translation[1], l.translation[2]);
				t3d.setTranslation(v3d);
				m3d.set(l.rotation);
				a4d.set(l.jointAxis[0], l.jointAxis[1], l.jointAxis[2], lInfo_[linkId].jointValue);
				m3d2.set(a4d);
				m3d.mul(m3d2);
				t3d.setRotation(m3d);
			} else if(l.jointType.equals("slide")) {
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

	//#####[Changed] NewModelLoader.IDL
	//public String[] getSensorNames(SensorType type) {
	public String[] getSensorNames(String type) {
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

	public int getDOF() {
		if (jointToLink_ == null)
			return 0;
		return jointToLink_.length;
	}

	public String[] getJointNames() {
		String[] names = new String[jointToLink_.length];
		for (int i=0; i<jointToLink_.length; i++)
			names[i] = lInfo_[jointToLink_[i]].name;
		return names;
	}

	public double getJointValue(String jname) {
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
		for (int i=0; i<shapeVector_.size(); i++) {
			Shape3D s3d = (Shape3D)shapeVector_.get(i);
			Appearance app = s3d.getAppearance();
			if (app != null) {
				PolygonAttributes pa = app.getPolygonAttributes();
				if (b) {
					pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
				} else {
					pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
				}
			}
		}
	}

	public void setTransparencyMode(boolean b) {
		for (int i=0; i<shapeVector_.size(); i++) {
			Shape3D s3d = (Shape3D)shapeVector_.get(i);
			Appearance app = s3d.getAppearance();
			if (app != null) {
				TransparencyAttributes ta = app.getTransparencyAttributes();
				if (ta != null) {
					if (b) {
						ta.setTransparency(0.5f);
						ta.setTransparencyMode(TransparencyAttributes.FASTEST);
					} else {
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

    // ##### [Changed] NewModelLoader.IDL
	//==================================================================================================
	/*!
		@brief		"ShapeInfoLocal" class
		@author		ErgoVision
		@version	0.00
		@date		200?-0?-0?
		@note		2008-03-03 M.YASUKAWA modify <BR>
		@note		"ShapeInfoLocal" class
	*/
	//==================================================================================================
	public class ShapeInfoLocal
	{
		//public float[]	vertices;
		//public long[]	triangles;
		public PrimitiveType type;
		public long	appearanceIndex;
		public	Point3f[] vertex2;
		public	int[] indices;

		public ShapeInfoLocal(ShapeInfo ShapeInfo_)
		{
			ShapeInfo shpinfo = ShapeInfo_;
			// set ShapeInfo Vertices
			int vCnts = shpinfo.vertices.length;
			//vertices = new float[vCnts];
			int vpCnts = vCnts/3;
//System.out.println( "   ShapeInfo.vertices.length /3 = " + vpCnts );

			vertex2 = new Point3f[vpCnts];
			for( int i=0; i<vpCnts; i++ )
			{
				vertex2[i] = new Point3f(shpinfo.vertices[i*3], shpinfo.vertices[i*3+1], shpinfo.vertices[i*3+2]);
//System.out.println( "   ShapeInfoLocal.vertex2[" + i + "]   = " + vertex2[i] );
			}
			// set ShapeInfo triangles
			int triCnts = shpinfo.triangles.length;
//System.out.println( "   ShapeInfo.triangles.length = " + triCnts );

			//int triCnts2 =  triCnts/3;
			//triangles = new long[triCnts];
			indices = new int[triCnts];
			for( int i=0; i<triCnts; i++ )
			{
				indices[i] = (int)(shpinfo.triangles[i]);
//System.out.println( "   ShapeInfoLocal.indices[" + i + "]   = " + indices[i] );
			}

			// set ShapeInfo appearanceIndex
			appearanceIndex = shpinfo.appearanceIndex;
//System.out.println( "   ShapeInfo.appearanceIndex = " + shpinfo.appearanceIndex );
			if(appearanceIndex < -1){
//System.out.println( "   ShapeInfo.appearanceIndex = " + shpinfo.appearanceIndex + " ; ShapeInfo.appearanceIndex Force -1 Set !" );
				appearanceIndex = -1;
			}

			// set PrimitiveType
			type = shpinfo.type;
/*
				if (type.equals(PrimitiveType.MESH)) {
System.out.println( "   ShapeInfo.PrimitiveType = MESH" );
				}else if (type.equals(PrimitiveType.BOX)) {
System.out.println( "   ShapeInfo.PrimitiveType = BOX" );
				}else if (type.equals(PrimitiveType.CYLINDER)) {
System.out.println( "   ShapeInfo.PrimitiveType = CYLINDER" );
				}else if (type.equals(PrimitiveType.CONE)) {
System.out.println( "   ShapeInfo.PrimitiveType = CONE" );
				}else if (type.equals(PrimitiveType.SPHERE)) {
System.out.println( "   ShapeInfo.PrimitiveType = SPHERE" );
				}else{
System.out.println( "   ShapeInfo.PrimitiveType error!" );

				}
*/


		}


	}
    // ##### [Changed]

   // ##### [Changed] NewModelLoader.IDL
	//==================================================================================================
	/*!
		@brief		"AppearanceInfoLocal" class
		@author		ErgoVision
		@version	0.00
		@date		200?-0?-0?
		@note		2008-03-26 M.YASUKAWA modify <BR>
		@note		"AppearanceInfoLocal" class
	*/
	//==================================================================================================
	public class AppearanceInfoLocal
	{
		//public float[]	vertices;
		//public long[]	triangles;
		//public long	appearanceIndex;

		long          materialIndex;
		public float[]		normals;
		public long[]		normalIndices;
		public boolean		normalPerVertex;
		public boolean		solid;
		public float		creaseAngle;
		public float[]		colors;
		public long[]		colorIndices;
		public boolean		coloerPerVertex;
		public long			textureIndex;
		public float[]		textureCoordinate;



		public AppearanceInfoLocal(AppearanceInfo AppearanceInfo_)
		{
			AppearanceInfo appinfo = AppearanceInfo_;

			// set materialIndex
			materialIndex = (long)(appinfo.materialIndex);

			// set AppearanceInfo normals
			int nCnts = appinfo.normals.length;
			normals = new float[nCnts];
            for( int i=0; i<nCnts; i++ )
            {
                normals[i] = appinfo.normals[i];
//System.out.println( "   AppearanceInfoLocal.normals[" + i + "]   = " + normals[i] );
            }

			// set AppearanceInfo normalIndices
			int nICnts = appinfo.normalIndices.length;
            normalIndices = new long[nICnts];
            for( int i=0; i<nICnts; i++ )
            {
                normalIndices[i] = appinfo.normalIndices[i];
//System.out.println( "   AppearanceInfoLocal.normalIndices[" + i + "]   = " + normalIndices[i] );
            }

			// set AppearanceInfo normalPerVertex
			normalPerVertex = appinfo.normalPerVertex;

			// set AppearanceInfo solid
			solid = appinfo.solid;

			// set AppearanceInfo creaseAngle
			creaseAngle = appinfo.creaseAngle;

			// set AppearanceInfo colors
			int clCnts = appinfo.colors.length;
			colors = new float[clCnts];
			for( int i=0; i<clCnts; i++ )
			{
				colors[i] = appinfo.colors[i];
//System.out.println( "   AppearanceInfoLocal.colors[" + i + "]   = " + colors[i] );
			}

			// set AppearanceInfo colorIndices
			int nCliCnts = appinfo.colorIndices.length;
			colorIndices = new long[nCliCnts];
			for( int i=0; i<nCliCnts; i++ )
			{
				colorIndices[i] = appinfo.colorIndices[i];
//System.out.println( "   AppearanceInfoLocal.colorIndices[" + i + "]   = " + colorIndices[i] );
			}

			// set AppearanceInfo textureIndex
			textureIndex = appinfo.textureIndex;

			// set AppearanceInfo textureCoordinate
			int txcCnts = appinfo.textureCoordinate.length;
			textureCoordinate = new float[txcCnts];
			for( int i=0; i<txcCnts; i++ )
			{
				textureCoordinate[i] = appinfo.textureCoordinate[i];
//System.out.println( "   AppearanceInfoLocal.textureCoordinate[" + i + "]   = " + textureCoordinate[i] );
			}

		}


	}
    // ##### [Changed]

  // ##### [Changed] NewModelLoader.IDL
	//==================================================================================================
	/*!
		@brief		"MaterialInfoLocal" class
		@author		ErgoVision
		@version	0.00
		@date		2008-04-06 M.YASUKAWA <BR>
		@note		2008-04-06 M.YASUKAWA modify <BR>
		@note		"MaterialInfoLocal" class
	*/
	//==================================================================================================
	public class MaterialInfoLocal
	{
		public float		ambientIntensity;
		public float[]		diffuseColor;
		public float[]		emissiveColor;
		public float		shininess;
		public float[]		specularColor;
		public float		transparency;

		public MaterialInfoLocal(MaterialInfo[] MaterialInfo_, int index)
		{
//System.out.println( "   MaterialInfoLocal  index = " + index );

			long mlen = MaterialInfo_.length;
//System.out.println( "   MaterialInfoLocal  bInfo.materials().length = " + mlen );		

			if((mlen > index) && (index >= 0)){

				MaterialInfo matinfo = MaterialInfo_[index];

				// set MaterialInfo ambientIntensity
				ambientIntensity = matinfo.ambientIntensity;
//System.out.println( "   MaterialInfoLocal.ambientIntensity   = " + ambientIntensity );

				// set MaterialInfo diffuseColor
				diffuseColor = new float[3];
				for( int i=0; i<3; i++ )
				{
					diffuseColor[i] = matinfo.diffuseColor[i];
//System.out.println( "   AppearanceInfoLocal.diffuseColor[" + i + "]   = " + diffuseColor[i] );
				}

				// set MaterialInfo emissiveColor
				emissiveColor = new float[3];
				for( int i=0; i<3; i++ )
				{
					emissiveColor[i] = matinfo.emissiveColor[i];
//System.out.println( "   AppearanceInfoLocal.emissiveColor[" + i + "]   = " + emissiveColor[i] );
				}

				// set MaterialInfo shininess
				shininess = matinfo.shininess;
//System.out.println( "   MaterialInfoLocal.shininess   = " + shininess );

				// set MaterialInfo specularColor
				specularColor = new float[3];
				for( int i=0; i<3; i++ )
				{
					specularColor[i] = matinfo.specularColor[i];
//System.out.println( "   AppearanceInfoLocal.specularColor[" + i + "]   = " + specularColor[i] );
				}

				// set MaterialInfo transparency
				transparency = matinfo.transparency;
//System.out.println( "   MaterialInfoLocal.transparency   = " + transparency );

			}
			else{
	//System.out.println( "   MaterialInfoLocal No Generate " );
				ambientIntensity = 1.0f;
				diffuseColor = new float[1];
				emissiveColor = new float[1];
				shininess = 1.0f;
				specularColor = new float[1];
				transparency = 0.5f;			
			}
			
			
		}
	}
    // ##### [Changed]


 // ##### [Changed] NewModelLoader.IDL
	//==================================================================================================
	/*!
		@brief		"TextureInfoLocal" class
		@author		ErgoVision
		@version	0.00
		@date		2008-04-06 M.YASUKAWA <BR>
		@note		2008-04-06 M.YASUKAWA modify <BR>
		@note		"TextureInfoLocal" class
	*/
	//==================================================================================================
	public class TextureInfoLocal
	{
//		public	ImageData		image;
		public	short			numComponents;
		public	short			width;
		public	short			height;
		public	boolean			repeatS;
		public	boolean			repeatT;
		private  BufferedImage	bimageRead;
		public  ImageComponent2D readImage;

		public TextureInfoLocal(TextureInfo[] TextureInfo_, int index)
		{
//System.out.println( "   TextureInfoLocal  index = " + index );

		long tlen = TextureInfo_.length;
//System.out.println( "   TextureInfoLocal  bInfo.textures().length = " + tlen );		

			if((tlen > index) && (index >= 0)){


				TextureInfo texinfo = TextureInfo_[index];

//				image = new ImageData();

				// set TextureInfo width
				width = texinfo.width;
//				image.width = texinfo.width;
//System.out.println( "   TextureInfo.width   = " + width );

				// set TextureInfo height
				height = texinfo.height;
//				image.height = texinfo.height;
//System.out.println( "   TextureInfo.height   = " + height );

//				image.octetData = new byte[1];
//				image.longData = new int[1];
//				image.floatData = new float[1];

//				image.format = PixelFormat.RGB;

				// set TextureInfo numComponents
				numComponents = texinfo.numComponents;
				// numComponents=1 ...  8bit
				//              =2 ... 16bit
				//              =3 ... 24bit
				//              =4 ... 32bit
//System.out.println( "   TextureInfo.numComponents   = " + numComponents );

				if((width == 0) || (height == 0)){
//System.out.println( "   TextureInfoLocal width = 0  & height = 0  => No Generate " );
					numComponents = 3;
					repeatS = false;
					repeatT = false;
					bimageRead = null;
					width = 0;
					height = 0;
					return;
				}

				// set TextureInfo image
//System.out.println( "   TextureInfo.image  " );
			// create color infomation for reading color buffer
			// type int, (Alpha:8bit,) R:8bit, G:8bit, B:8bit
				BufferedImage bimageRead = null;
				readImage = null;

				bimageRead = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				
				//int[] imagefield =((DataBufferInt)bimageRead.getRaster().getDataBuffer()).getData();
				//System.out.println( "   imagefield =((DataBufferInt)bimageRead.getRaster().getDataBuffer()).getData()" );			
				byte mByteCode;
				byte rByteCode;
				byte gByteCode;
				byte bByteCode;
				byte aByteCode;
				int mCode;
				int rCode;
				int gCode;
				int bCode;
				int aCode;
				int rgbCode;
				int[] pixels;
				pixels = ( (DataBufferInt)bimageRead.getRaster().getDataBuffer() ).getData();
				int img_i=0;
				int img_j=0;
				int imgsize = texinfo.image.length;
				int imgrgbsize = imgsize/3;
				//System.out.println( "   texinfo.image.length   = " + imgsize + " imgrgbsize = " + imgrgbsize );
				
				for(int img_a=0; img_a<imgrgbsize; img_a++){

					switch (numComponents) {

					// 1byte:Kido MonoColor
					case 1:

							 mByteCode = (byte)(texinfo.image[img_a]);
							 mCode = mByteCode & 0xFF;
							 rCode = mCode;
							 gCode = mCode;
							 bCode = mCode;

							 rgbCode = rCode * 0x10000 + gCode * 0x100 + bCode;	

			//            rgbCode =(  ( ((mByteCode) & 0xE0) << 16 ) | ( ((mByteCode) & 0xE0) << 13 ) | ( ((mByteCode) & 0x00C0) << 10 ) | 
			//( ((mByteCode) & 0x1C) << 11 ) | ( ((mByteCode) & 0x1C) <<  8 ) | ( ((mByteCode) & 0x0018) <<  5 ) | 
			//( ((mByteCode) & 0x03) <<  6 ) | ( ((mByteCode) & 0x03) <<  4 ) | 
			//( ((mByteCode) & 0x03) <<  2 ) | ( ((mByteCode) & 0x03)       ) );

//System.out.println( "   bimageRead numComponents = 1 rgbCode: " + rgbCode );


							//bimageRead.setRGB(img_i, img_j, rgbCode);
							pixels[ (width * img_j) + img_i ] = rgbCode;

							break;

					// 1byte:Kido 2byte:Transparency
					case 2:
						 mByteCode = (byte)(texinfo.image[img_a]);
						 mCode = mByteCode & 0xFF;
						 aByteCode = (byte)(texinfo.image[img_a * 2 + 1]);
						 aCode = aByteCode & 0xFF;
						 rCode = mCode;
						 gCode = mCode;
						 bCode = mCode;
						
			//            rgbCode =(  ( ((mByteCode) & 0xE0) << 16 ) | ( ((mByteCode) & 0xE0) << 13 ) | ( ((mByteCode) & 0x00C0) << 10 ) | 
			//( ((mByteCode) & 0x1C) << 11 ) | ( ((mByteCode) & 0x1C) <<  8 ) | ( ((mByteCode) & 0x0018) <<  5 ) | 
			//( ((mByteCode) & 0x03) <<  6 ) | ( ((mByteCode) & 0x03) <<  4 ) | 
			//( ((mByteCode) & 0x03) <<  2 ) | ( ((mByteCode) & 0x03)       ) );
						 rgbCode = rCode * 0x10000 + gCode * 0x100 + bCode;	

						 rgbCode = aCode * 0x1000000 + rgbCode;

//System.out.println( "   bimageRead numComponents = 2 rgbCode: " + rgbCode );

						//bimageRead.setRGB(img_i, img_j, rgbCode);
						pixels[ (width * img_j) + img_i ] = rgbCode;
						break;

					// RGB
					case 3:
						 rByteCode = (byte)(texinfo.image[img_a * 3]);
						 rCode = rByteCode & 0xFF;
						 gByteCode = (byte)(texinfo.image[img_a * 3 + 1]);
						 gCode = gByteCode & 0xFF;
						 bByteCode = (byte)(texinfo.image[img_a * 3 + 2]);
						 bCode = bByteCode & 0xFF;
//System.out.println( "   bimageRead R: " + rCode  + ", G: " + gCode  + ", B: " + bCode + ")");

						rgbCode = rCode * 0x10000 + gCode * 0x100 + bCode;

						//bimageRead.setRGB(img_i, img_j, rgbCode);
						pixels[ (width * img_j) + img_i ] = rgbCode;
						//System.out.println( "   bimageRead.setRGB( " + img_i  + "," + img_j  + "," + rgbCode + ")");
						//img_i++;
						//if(img_i >= width){
						//    img_i = 0;
						//    img_j++;
						//}			

						break;

					// RGB+Transparency
					case 4:
						 rByteCode = (byte)(texinfo.image[img_a * 4]);
						 rCode = rByteCode & 0xFF;
						 gByteCode = (byte)(texinfo.image[img_a * 4 + 1]);
						 gCode = gByteCode & 0xFF;
						 bByteCode = (byte)(texinfo.image[img_a * 4 + 2]);
						 bCode = bByteCode & 0xFF;
						 aByteCode = (byte)(texinfo.image[img_a * 4 + 3]);
						 aCode = aByteCode & 0xFF;
//System.out.println( "   bimageRead R: " + rCode  + ", G: " + gCode  + ", B: " + bCode + ", Alfa: " + aCode + ")");

						rgbCode =  aCode * 0x1000000 + rCode * 0x10000 + gCode * 0x100 + bCode;

						//bimageRead.setRGB(img_i, img_j, rgbCode);
						pixels[ (width * img_j) + img_i ] = rgbCode;
						//System.out.println( "   bimageRead.setRGB( " + img_i  + "," + img_j  + "," + rgbCode + ")");
						//img_i++;
						//if(img_i >= width){
						//    img_i = 0;
						//    img_j++;
						//}	
	
						break;

					default:
						 rByteCode = (byte)(texinfo.image[img_a * 3]);
						 rCode = rByteCode & 0xFF;
						 gByteCode = (byte)(texinfo.image[img_a * 3 + 1]);
						 gCode = gByteCode & 0xFF;
						 bByteCode = (byte)(texinfo.image[img_a * 3 + 2]);
						 bCode = bByteCode & 0xFF;
//System.out.println( "   bimageRead R: " + rCode  + ", G: " + gCode  + ", B: " + bCode + ")");

						 rgbCode = rCode * 65536 + gCode * 256 + bCode;

						//bimageRead.setRGB(img_i, img_j, rgbCode);
						pixels[ (width * img_j) + img_i ] = rgbCode;
				//System.out.println( "   bimageRead.setRGB( " + img_i  + "," + img_j  + "," + rgbCode + ")");
						//img_i++;
						//if(img_i >= width){
						//    img_i = 0;
						//    img_j++;
						//}		
						break;		
					}

					img_i++;
					if(img_i >= width){
						img_i = 0;
						img_j++;
					}			
				}
		
				
				readImage = new ImageComponent2D(ImageComponent.FORMAT_RGB, bimageRead);
//System.out.println( "   new ImageComponent2D(ImageComponent.FORMAT_RGB, bimageRead)"  );


				// set TextureInfo repeatS
				repeatS = texinfo.repeatS;
//System.out.println( "   TextureInfo.repeatS   = " + repeatS );

				// set TextureInfo repeatT
				repeatT = texinfo.repeatT;
//System.out.println( "   TextureInfo.repeatT   = " + repeatT );
			}else{
//System.out.println( "   TextureInfoLocal No Generate " );
				numComponents = 3;
				repeatS = false;
				repeatT = false;
				bimageRead = null;
				width = 0;
				height = 0;
			}

		}

	}
    // ##### [Changed]




	//==================================================================================================
	/*!
		@brief		"LinkInfoLocal" class
		@author		GeneralRobotix
		@version	0.00
		@date		200?-0?-0?
		@note		200?-0?-0? GeneralRobotix create <BR>
		@note		2008-03-03 M.YASUKAWA modify <BR>
		@note		"LinkInfoLocal" class
	*/
	//==================================================================================================
	public class LinkInfoLocal
	{
		final public String		name;
		final public int		jointId;

        // ##### [Changed] NewModelLoader.IDL
		// member properties as BinaryTree (neccessary to rebuild)
		/*final*/ public int	mother;			// mother LinkInfoLocal instance
		/*final*/ public int	daughter;		// daughter LinkInfoLocal for BinaryTree
		/*final*/ public int	sister;			// sister LinkInfoLocal for BinaryTree

        // ##### [Changed] NewModelLoader.IDL
		// member properties as NaryTree (receive)
		public short			myLinkID;		// index of this LinkInfoLocal in LinkInfoSequence of BodyInfo
		public short			parentIndex;	// parent's index in LinkInfoSequence of BodyInfo
		final public short []	childIndices;	// children's index in LinkInfoSequence of BodyInfo

		final public String		jointType;
		final public double[]	jointAxis;
		final public double[]	translation;
		final public double[]	rotation;
		final public double[]	centerOfMass;
		final public double		mass;
		final public double[]	inertia;
		final public double[]	ulimit;
		final public double[]	llimit;
		final public double[]	uvlimit;
		final public double[]	lvlimit;
		final public SensorInfoLocal[]	sensors;

        // ##### [Changed] NewModelLoader.IDL
		public short[]			shapeIndices;


		public double  jointValue;
		public TransformGroup tg;
        // ##### [Changed] ADD For Scene graph make
		//public int	topTGroupFlg;


		public LinkInfoLocal(LinkInfo info)
		{
			// #######[Changed] mother <= parentID
			parentIndex = info.parentIndex;
			//childIndices = info.childIndices;
			int childIndicesLen = info.childIndices.length;
//System.out.println( "   LinkInfoLocal.childIndices Counts = " +  childIndicesLen);

			childIndices = new short[childIndicesLen];
			for(int i=0; i<childIndicesLen; i++){
//System.out.println( "   LinkInfoLocal.childIndices[" + i + "]   = " + info.childIndices[i] );

				childIndices[i] = info.childIndices[i];
			}


			mother = info.parentIndex;
			daughter = -1;	// ##### should be -1 for release version
			sister   = -1;	// ##### should be -1 for release version
			// mother   = info.mother();	// ##### original
			// daughter = info.daughter();	// ##### original
			// sister   = info.sister();	// ##### original
			name = info.name;
			jointId = info.jointId;
			jointType = info.jointType;
			jointAxis = info.jointAxis;
			translation    = info.translation;
			// #######[Changed] rotation DblArray9 -> DblArray4 Changed For NewModelLoader.IDL
			//rotation    = info.rotation;
			 ConvRotation4to9 rotation4_9 = new ConvRotation4to9(info.rotation);
			 rotation = rotation4_9.getConvRotation4to9();
			// #######[Changed]
			centerOfMass = info.centerOfMass;
			mass    = info.mass;
			inertia = info.inertia;

			if (info.ulimit == null || info.ulimit.length == 0)
				ulimit = new double[]{0.0};
			else
				ulimit  = info.ulimit;

			if (info.llimit == null || info.llimit.length == 0)
				llimit = new double[]{0.0};
			else
				llimit = info.llimit;

			uvlimit = info.uvlimit;
			lvlimit = info.lvlimit;
			
			jointValue = 0.0;
			

			// #######[Changed] ShapeIndices For NewModelLoader.IDL

			int shapeCnts = info.shapeIndices.length;
			shapeIndices = new short[shapeCnts];
			for (int i=0; i<shapeCnts; i++){
				shapeIndices[i] = info.shapeIndices[i];
			}
			// #######[Changed]

			SensorInfo[] sinfo = info.sensors;
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

	public List<Camera_impl> getCameraSequence () {
		return cameraList;
	}

	private class SensorInfoLocal implements Comparable {
		final String name;
		//final public SensorType type;
		// ##### [Changed] NewModelLoader.ID
		//public SensorType type;
		public String type;
		final int id;
		final public double[] translation;
		final public double[] rotation;
		// ##### [Changed] NewModelLoader.IDL
		//    double[] maxValue -> float[] specValues
		final public float[] maxValue;
		final public LinkInfoLocal parent_;
		
		public SensorInfoLocal(SensorInfo info, LinkInfoLocal parentLink) {
			name = info.name;
			// ##### [Changed] NewModelLoader.IDL

			//type = SensorType.FORCE_SENSOR;

			//if( info.type.equals("Force") )
			//{
			//    type = SensorType.FORCE_SENSOR; 
			//}
			//else if( info.type.equals("RateGyro") )
			//{
			//    type = SensorType.RATE_GYRO; 
			//}
			//else if( info.type.equals("Acceleration") )
			//{
			//    type = SensorType.ACCELERATION_SENSOR; 
			//}
			type = info.type;


			id = info.id;
			translation = info.translation;
			// #######[Changed] rotation DblArray9 -> DblArray4 Changed For NewModelLoader.IDL
			rotation = info.rotation;
//			Not Convert!
//            ConvRotation4to9 rotation4_9 = new ConvRotation4to9(info.rotation);
//            rotation = rotation4_9.getConvRotation4to9();

		    // ##### [Changed] NewModelLoader.IDL
		    // [Changed] double[] maxValue -> float[] specValues
			maxValue = info.specValues;
			parent_ = parentLink;

		}

		public int compareTo(Object o) {
			if (o instanceof SensorInfoLocal) {
				SensorInfoLocal s = (SensorInfoLocal) o;
				//#####[Changed] int -> string
				//if (type < s.type)
				//    return -1;
				//else 
				if (getOrder(type) < getOrder(s.type)) 
				    return -1;
				else{
					if (id < s.id)
					return -1;
				}
			}
			return 1;
		}

		private int getOrder(String type) {
			if (type.equals("Force")) 
				return 0;
			else if (type.equals("RateGyro")) 
				return 1;
			else if (type.equals("Acceleration")) 
				return 2;
			else if (type.equals("Vision")) 
				return 3;
			else
				return -1;

		}

	}

	// ######[Changed] DblArray4 -> DblArray9 Convert For NewModelLoader.IDL
	//  Rotation <DbalArray4> (x y z a) => <DblArray9>  3x3 Rotation 
	//  [tx2+c    txy+sz    txz-sy
	//  txy-sz   ty2+c     tyz+sx
	//  txz+sy   tyz-sx    tz2+c  ]
	//  where c = cos(a), s = sin(a), and t = 1-c
	public class ConvRotation4to9
	{
	    double [] rotaton_Array4_;
	    double [] rotaton_Array9_;

		public  ConvRotation4to9( double[] rotaton_Array4 )
		{
			rotaton_Array4_ = new double[4];
			rotaton_Array9_ = new double[9];

			for( int i = 0 ; i < 4  ; i++ )
			{
				rotaton_Array4_[i] = rotaton_Array4[i];
			}

			double rotX  = rotaton_Array4[0];
			double rotY  = rotaton_Array4[1];
			double rotZ  = rotaton_Array4[2];
			double angle = rotaton_Array4[3];

			double c,s,t;

			c = Math.cos(angle);
			s = Math.sin(angle);

			rotaton_Array9_[0] = c + (1-c) * rotX * rotX;
			rotaton_Array9_[1] = (1-c) * rotX * rotY + s * rotZ;
			rotaton_Array9_[2] = (1-c) * rotX * rotZ - s * rotY;
			rotaton_Array9_[3] = (1-c) * rotX * rotY - s * rotZ;
			rotaton_Array9_[4] = c + (1-c) * rotY * rotY;
			rotaton_Array9_[5] = (1-c) * rotY * rotZ + s * rotX;
			rotaton_Array9_[6] = (1-c) * rotX * rotZ + s * rotY;
			rotaton_Array9_[7] = (1-c) * rotY * rotZ - s * rotX;
			rotaton_Array9_[8] = c + (1-c) * rotZ * rotZ;

		}

		public double[] getConvRotation4to9 ()
		{
			return( rotaton_Array9_ );
		}
	}

	public void setJointColor(int jid, java.awt.Color color) {
        if (color == null) 
			setAmbientColorRecursive(lInfo_[jointToLink_[jid]].tg, new Color3f(0.0f, 0.0f, 0.0f));
		else
			setAmbientColorRecursive(lInfo_[jointToLink_[jid]].tg, new Color3f(color));
	}

    private void setAmbientColorRecursive(Node node, Color3f color) {
    	if (node instanceof BranchGroup) {
    		BranchGroup bg = (BranchGroup)node;
    		for (int i = 0; i < bg.numChildren(); i++)
    			setAmbientColorRecursive(bg.getChild(i), color);
    	} else if (node instanceof TransformGroup) {
    		TransformGroup tg = (TransformGroup)node;
    		for (int i = 0; i < tg.numChildren(); i++)
    			setAmbientColorRecursive(tg.getChild(i), color);
    	} else if (node instanceof Group) {	
    		Group g = (Group)node;
    		for (int i = 0; i < g.numChildren(); i++)
    		    setAmbientColorRecursive(g.getChild(i), color);
    	} else if (node instanceof Link) {
    	    Link l = (Link)node;
    	    SharedGroup sg = l.getSharedGroup();
    	    for (int i = 0; i < sg.numChildren(); i++) 
    	    	setAmbientColorRecursive(sg.getChild(i), color);
    	} else if (node instanceof Shape3D) { // Is the node Shape3D ?
    	    Shape3D s3d = (Shape3D)node;
    	    Appearance app = s3d.getAppearance();
    	    if (app != null){
        		Material ma = app.getMaterial();
        		if (ma != null)
        			ma.setAmbientColor(color);
      	    }
    	} else {
    	    GrxDebugUtil.print("* The node " + node.toString() + " is not supported.");
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



	//==================================================================================================
	/*!
		@brief		ConvNTreeToBTree
		@author		M.YASUKAWA
		@version	0.00
		@date		2008-03-03
		@note		2008-03-03 M.YASUKAWA create <BR>
		@note		2008-03-03 K.FUKUDA modify <BR>
		@note		Convert NaryTree To BinaryTree
	*/
	//==================================================================================================
	public void ConvNTreeToBTree(
		short				parentIndex,		// index no of parent node
		short				currentIndex,		// index no of this LinkInof
		short []			sisterIndices )		// indeces of sister nodes( includes this node )
	{
//System.out.println( "# in ConvNTreeToBTree()." );
		// identify this node
		LinkInfoLocal info = lInfo_[currentIndex];

		int nSisters = sisterIndices.length - 1;
//System.out.println( "#  nSisters = " + nSisters );
		int myID = info.myLinkID;	// shoud be equal to currentIndex

		// set parent node index
		if( parentIndex == -1 )
		{
			info.parentIndex =  -1;		// = root node
		}
		else
		{
			info.parentIndex = parentIndex;
		}
		
		// set daughter
		int nChildren = info.childIndices.length;
		if( 0 == nChildren )
		{
			// no daughters
			info.daughter = -1;
			// and no "sistersOfDaughter"
		}
		else
		{
			// set daughter
			info.daughter = info.childIndices[0];
			short [] sistersOfDaughter = new short[nSisters];
			for( int i=0; i<nSisters; i++ )
			{
				sistersOfDaughter[i] = sisterIndices[i+1];
			}
//System.out.println( "#  set daughter." );
			ConvNTreeToBTree( (short)myID, sistersOfDaughter[0], sistersOfDaughter );
		}

		// set sister
		if( 0 == nSisters )
		{
			info.sister =  -1;
			// and no more sisters
		}
		else
		{
			// set sister
			// sisterIndices[0] means this node itself
			info.sister = sisterIndices[1];
			short [] otherSisters = new short[nSisters-1];
			for( int i=0; i<nSisters-1; i++ )
			{
				otherSisters[i] = sisterIndices[i+2];
			}
//System.out.println( "#  set sister." );
			ConvNTreeToBTree( (short)myID, otherSisters[0], otherSisters );
		}

//System.out.println( "# out ConvNTreeToBTree()." );
		return;

	}

	private TransformGroup setTransformGroup(LinkInfoLocal lInfo_){
		double[] pos_Shp = lInfo_.translation;
		ConvRotation4to9 Shprotation4_9 = new ConvRotation4to9(lInfo_.rotation);
		double[] rot_Shp = Shprotation4_9.getConvRotation4to9();
		Transform3D t3d_Shp = new Transform3D();
		t3d_Shp.setTranslation(new Vector3d(pos_Shp));
		t3d_Shp.setRotation(new Matrix3d(rot_Shp));
		return new TransformGroup(t3d_Shp);
	}

	private Color3f[] setWhiteColor( ){
		// Temporary Color Set 
		Color3f[] color = new Color3f[3];
		float colR = 1.0f;
		float colG = 1.0f;
		float colB = 1.0f;
		// Defualt White Color Set !  For Material None Case, 
		color[0] = new Color3f(colR, colG, colB);
		color[1] = new Color3f(colR, colG, colB);
		color[2] = new Color3f(colR, colG, colB);
		return color;
	}

	private Material setMaterial(MaterialInfoLocal matInfo){
		Material ma = new Material();
		float diffusecolR = 0.0f;
		float diffusecolG = 0.0f;
		float diffusecolB = 0.0f;
		if(matInfo.diffuseColor.length >= 3){
			diffusecolR = matInfo.diffuseColor[0];
			diffusecolG = matInfo.diffuseColor[1];
			diffusecolB = matInfo.diffuseColor[2];
			Color3f diffuseColor = new Color3f(diffusecolR, diffusecolG, diffusecolB);
			ma.setDiffuseColor(diffuseColor);
		}else{
			System.out.println( "## _loadVrmlScene() -> MaterialInfoLocal DiffuseColor None Force Random Color ");							
		}
		if(matInfo.specularColor.length >= 3){
			float matcolR = matInfo.specularColor[0];
			float matcolG = matInfo.specularColor[1];
			float matcolB = matInfo.specularColor[2];
			Color3f matColor = new Color3f(matcolR, matcolG, matcolB);
			ma.setSpecularColor(matColor);		
		}else{
			System.out.println( "## _loadVrmlScene() -> MaterialInfoLocal specularColor None!! ");							
		}
		if(matInfo.emissiveColor.length >= 3){
			float emicolR = matInfo.emissiveColor[0];
			float emicolG = matInfo.emissiveColor[1];
			float emicolB = matInfo.emissiveColor[2];
			Color3f emiColor = new Color3f(emicolR, emicolG, emicolB);
			//ma.setEmissiveColor(emiColor);
		}else{
			System.out.println( "## _loadVrmlScene() -> MaterialInfoLocal emissiveColor None!! ");							
		}

		float ambIntens = (float)(matInfo.ambientIntensity);
		float ambcolR = diffusecolR * ambIntens;
		float ambcolG = diffusecolG * ambIntens;
		float ambcolB = diffusecolB * ambIntens;

		ambcolR = 1.0f;    //???
		ambcolG = 1.0f;
		ambcolB = 1.0f;
		Color3f ambColor = new Color3f(ambcolR, ambcolG, ambcolB);
		ma.setAmbientColor(ambColor);	

		float shinine = (float)(matInfo.shininess);
		float shinineJava3D = 128.0f / 2.0f;
		if(shinine < 0.0){
			shinine = 0.0f;
		}else if(shinine > 1.0){
			shinine = 1.0f;
		}
		shinineJava3D = shinine * 128.0f;
		ma.setShininess(shinineJava3D);		
		return ma;
	}

	private Texture2D setTexture(TextureInfoLocal texInfo){
		int Img_width =  texInfo.width;
		int Img_height = texInfo.height;
		Texture2D texture2d=null;
		if((Img_width != 0) && (Img_height != 0)){
			ImageComponent2D icomp2d = texInfo.readImage;
			texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, Img_width, Img_height);
			texture2d.setImage(0, icomp2d);
		}
		return texture2d;
	}

	private boolean setvcolors(ShapeInfoLocal shpInfo, AppearanceInfoLocal appInfo, Color3f[] diffuseColor, Color3f[] vcolors2){
		int colorlen = (appInfo.colors.length)/3;
		int vTris = (shpInfo.indices.length)/3;
		if(colorlen > 0){
			int colorIndiceslen = 0;
			if(appInfo.coloerPerVertex){
				int colorVindex = 0;
				if (appInfo.colorIndices.length >= (vTris-1)*4+2 ){
					int[] colorindices = new int[colorIndiceslen];
					for(int vTriIdx=0; vTriIdx<vTris; vTriIdx++){
						int v1index = (int)(shpInfo.indices[vTriIdx*3]);
						int v2index = (int)(shpInfo.indices[vTriIdx*3+1]);
						int v3index = (int)(shpInfo.indices[vTriIdx*3+2]);
						int c1index = (int)(appInfo.colorIndices[vTriIdx*4]);
						float colR = appInfo.colors[c1index * 3];
						float colG = appInfo.colors[c1index * 3 + 1];
						float colB = appInfo.colors[c1index * 3 + 2];
						vcolors2[vTriIdx*3] = new Color3f(colR, colG, colB);
						int c2index = (int)(appInfo.colorIndices[vTriIdx*4+1]);
						colR = appInfo.colors[c2index * 3];
						colG = appInfo.colors[c2index * 3 + 1];
						colB = appInfo.colors[c2index * 3 + 2];
						vcolors2[vTriIdx*3+1] = new Color3f(colR, colG, colB);
						int c3index = (int)(appInfo.colorIndices[vTriIdx*4+2]);
						colR = appInfo.colors[c3index * 3];
						colG = appInfo.colors[c3index * 3 + 1];
						colB = appInfo.colors[c3index * 3 + 2];
						vcolors2[vTriIdx*3+2] = new Color3f(colR, colG, colB);
						colorindices[vTriIdx*3] = v1index;
						colorindices[vTriIdx*3+1] = v2index;
						colorindices[vTriIdx*3+2] = v3index;
					}
					return true;
				}
			}else{
				colorIndiceslen = appInfo.colorIndices.length;
				// Face color Set TODO
				//  ColorVertexCounts  >= ShapeInfo Triangle Counts 
				if (appInfo.colorIndices.length >= vTris ){
				// by face, color counts -> vertex color counts => * 3 !!
					int[] colorindices = new int[colorIndiceslen * 3];
					for(int vTriIdx=0; vTriIdx<vTris; vTriIdx++){
						int v1index = (int)(shpInfo.indices[vTriIdx*3]);
						int v2index = (int)(shpInfo.indices[vTriIdx*3+1]);
						int v3index = (int)(shpInfo.indices[vTriIdx*3+2]);
						int c1index = (int)(appInfo.colorIndices[vTriIdx]);
						if(c1index < 0){
							c1index = 0;
						}
						float colR = appInfo.colors[c1index * 3];
						float colG = appInfo.colors[c1index * 3 + 1];
						float colB = appInfo.colors[c1index * 3 + 2];
						vcolors2[vTriIdx*3] = new Color3f(colR, colG, colB);
						int c2index = (int)(c1index);
						vcolors2[vTriIdx*3+1] = new Color3f(colR, colG, colB);
						int c3index = (int)(c1index);
						vcolors2[vTriIdx*3+2] = new Color3f(colR, colG, colB);
						colorindices[vTriIdx*3] = v1index;
						colorindices[vTriIdx*3+1] = v2index;
						colorindices[vTriIdx*3+2] = v3index;
					}
					return true;
				}
			}
		}else{
		// When color vertex None, material color vertex set 
			int[] colorindices = new int[vTris * 3];
			for(int vTriIdx=0; vTriIdx<vTris; vTriIdx++){
				int v1index = (int)(shpInfo.indices[vTriIdx*3]);
				int v2index = (int)(shpInfo.indices[vTriIdx*3+1]);
				int v3index = (int)(shpInfo.indices[vTriIdx*3+2]);
				vcolors2[vTriIdx*3] = diffuseColor[0];
				vcolors2[vTriIdx*3+1] = diffuseColor[1];
				vcolors2[vTriIdx*3+2] = diffuseColor[2];
				colorindices[vTriIdx*3] = v1index;
				colorindices[vTriIdx*3+1] = v2index;
				colorindices[vTriIdx*3+2] = v3index;
			}
			return true;
		}
		return false;
	}

	private boolean setnormals(ShapeInfoLocal shpInfo, AppearanceInfoLocal appInfo, Vector3f[] normals2, int[] normalIndices ){
		int vTris = (shpInfo.indices.length)/3;
		int normalsCounts = appInfo.normals.length;
        if(normalsCounts > 0){
			int normalIndiceslen = 0;
			if(appInfo.normalPerVertex){
				normalIndiceslen = appInfo.normalIndices.length;
				int normalVindex = 0;
				if (appInfo.normalIndices.length >= (vTris-1)*4+2 ){
					for(int vTriIdx=0; vTriIdx<vTris; vTriIdx++){   //vertex2
						int v1index = (int)(shpInfo.indices[vTriIdx*3]);
						int v2index = (int)(shpInfo.indices[vTriIdx*3+1]);
						int v3index = (int)(shpInfo.indices[vTriIdx*3+2]);
						int n1index = (int)(appInfo.normalIndices[vTriIdx*4]);
						int n2index = (int)(appInfo.normalIndices[vTriIdx*4+1]);
						int n3index = (int)(appInfo.normalIndices[vTriIdx*4+2]);			
                        normals2[vTriIdx*3] = new Vector3f(appInfo.normals[n1index * 3], appInfo.normals[n1index * 3 + 1], appInfo.normals[n1index * 3 + 2]);
                        normals2[vTriIdx*3+1] = new Vector3f(appInfo.normals[n2index * 3], appInfo.normals[n2index * 3 + 1], appInfo.normals[n2index * 3 + 2]);
                        normals2[vTriIdx*3+2] = new Vector3f(appInfo.normals[n3index * 3], appInfo.normals[n3index * 3 + 1], appInfo.normals[n3index * 3 + 2]);
						normalIndices[vTriIdx*3] = v1index;
						normalIndices[vTriIdx*3+1] = v2index;
						normalIndices[vTriIdx*3+2] = v3index;
					}
					return true;
				}
	         }else{
				normalIndiceslen = appInfo.normalIndices.length;
				// Face color Set TODO
				if (appInfo.normalIndices.length >= vTris ){
				// by face, normal counts -> vertex normal counts => * 3 !!
					normalIndices = new int[normalIndiceslen * 3];	
					for(int vTriIdx=0; vTriIdx<vTris; vTriIdx++){
						int v1index = (int)(shpInfo.indices[vTriIdx*3]);
						int v2index = (int)(shpInfo.indices[vTriIdx*3+1]);
						int v3index = (int)(shpInfo.indices[vTriIdx*3+2]);
						int n1index = (int)(appInfo.normalIndices[vTriIdx]);
						if(n1index < 0){
							n1index = 0;
						}
						int n2index = (int)(n1index);
						int n3index = (int)(n1index);
						normals2[v1index] = new Vector3f(appInfo.normals[n1index * 3], appInfo.normals[n1index * 3 + 1], appInfo.normals[n1index * 3 + 2]);
                        normals2[v2index] = new Vector3f(appInfo.normals[n2index * 3], appInfo.normals[n2index * 3 + 1], appInfo.normals[n2index * 3 + 2]);
                        normals2[v3index] = new Vector3f(appInfo.normals[n3index * 3], appInfo.normals[n3index * 3 + 1], appInfo.normals[n3index * 3 + 2]);
						normalIndices[vTriIdx*3] = v1index;
						normalIndices[vTriIdx*3+1] = v2index;
						normalIndices[vTriIdx*3+2] = v3index;
					}
					return true;
				}
			}
		}else{
		}
		return false;
	}
}

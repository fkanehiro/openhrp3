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
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  @author Shin'ichiro Nakaoka (AIST)
 */

package com.generalrobotix.ui.item;

import java.io.File;
import java.util.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.view.tdview.*;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

import jp.go.aist.hrp.simulator.*;

@SuppressWarnings({ "unchecked", "serial" })
/**
 * @brief item corresponds to a robot model
 */
public class GrxModelItem extends GrxBaseItem implements Manipulatable {
    public static final String TITLE = "Model";
    public static final String DEFAULT_DIR = "../../etc";
    public static final String FILE_EXTENSION = "wrl";
    private static final double DEFAULT_RADIOUS = 0.05;

    // icons
   	private static final String robotIcon = "robot.png";
   	private static final String envIcon = "environment.png";
   	
    private boolean isRobot_ = true;
    public boolean update_ = true;

    private BodyInfo bInfo_;
    private boolean bModified_ = false; //< true if this model is modified, false otherwise

    public BranchGroup bgRoot_ = new BranchGroup();
    public Vector<GrxLinkItem> links_ = new Vector<GrxLinkItem>();
    // jontId -> link
    private int[] jointToLink_; 
    // sensor type name -> list of sensors
    private final Map<String, List<GrxSensorItem>> sensorMap_ = new HashMap<String, List<GrxSensorItem>>();
    // list of cameras
    private List<Camera_impl> cameraList_ = new ArrayList<Camera_impl>();

    // CoM
    private Switch switchCom_;
    private TransformGroup tgCom_;
    
    // CoM projected on the floor
    private Switch switchComZ0_;
    private TransformGroup tgComZ0_;

    // bounding box of the whole body
    private Switch switchBb_;
    
    /**
     * @brief notify this model is modified
     */
    public void notifyModified(){
    	System.out.println(getName()+" : modification is notified");
    	bModified_ = true;
    }
    
    /**
     * @brief get BodyInfo
     * @return BodyInfo
     */
    public BodyInfo getBodyInfo(){
    	String url = getURL(true);
    	if (bModified_ || url == null || url.equals("")){
        	MessageDialog.openInformation(null, "", "Please save model("+getName()+") before starting simulation");
        	_saveAs();
        	File f = new File(getURL(true));
        	load(f);
    	}
    	return bInfo_;
    }
    
    /**
   	 * @brief
   	 */
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

	/**
	 * @brief constructor
	 * @param name name of this item
	 * @param item plugin manager
	 */
	public GrxModelItem(String name, GrxPluginManager item) {
		super(name, item);
		setIcon(robotIcon);
		_initMenu();
		
        bgRoot_.setCapability(BranchGroup.ALLOW_DETACH);
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        // create root link
        GrxLinkItem link = new GrxLinkItem("root", manager_, this);
        link.jointType("free");
        bgRoot_.addChild(link.bg_);
        
        setProperty("url","");
        
        _setupMarks();
    }

	/**
	 * @brief add a link
	 * 
	 * This method is called by constructor of GrxLinkItem
	 * @param link link to be added
	 */
	public void addLink(GrxLinkItem link){
		System.out.println("link is added : "+link.getName());
		links_.add(link);
		notifyModified();
	}
	
	/**
	 * @brief remove a link
	 * @param link linke to be removed
	 */
	public void removeLink(GrxLinkItem link){
		System.out.println("link is removed : "+link.getName());
		links_.remove(link);
		notifyModified();
	}
	
	/**
	 * @brief initialize right-click menu
	 */
	private void _initMenu() {
		// menu item : reload
		setMenuItem(new Action(){
            public String getText(){ return "reload"; }
			public void run(){
				load(GrxModelItem.this.file_);
			}
		});

        setMenuItem(menuChangeType_);

        // menu item : save
        setMenuItem(new Action(){
        	public String getText() { return "save"; }
        	public void run(){
		        String url = getStr("url");
		        if (url == null || url.equals("")){
		        	_saveAs();
		        }else{
					GrxVrmlExporter.export(GrxModelItem.this, url);
				}
        	}
        });

        // menu item : save as
        setMenuItem(new Action(){
        	public String getText() { return "save as"; }
        	public void run(){
        		_saveAs();
        	}
        });

        /* disable copy and paste menus until they are implemented
        // menu item : copy
        setMenuItem( new Action(){
            public String getText(){
                return "copy";
            }
            public void run(){
                GrxDebugUtil.println("GrxModelItem.GrxModelItem copy Action");
                manager_.setSelectedGrxBaseItemList();
            }
        });

        // menu item : paste
        setMenuItem(new Action(){
            public String getText(){
                return "paste";
            }

            public void run(){
            }
        });
        */
	}

	/**
	 * @brief save this model as a VRML file
	 */
	private void _saveAs(){
		IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		FileDialog fdlg = new FileDialog( window.getShell(), SWT.SAVE);
		String fPath = fdlg.open();
		if( fPath != null ) {
			if (GrxVrmlExporter.export(GrxModelItem.this, fPath)){
				setURL(fPath);
			}
		}
	}
	/**
	 * @brief create a new model
	 * @return true if created successfully, false otherwise
	 */
	public boolean create() {
		return true;
	}

	/**
	 * @brief get root link
	 */
	public GrxLinkItem rootLink() {
		if (links_.size() > 0){
			return links_.get(0);
		}else{
			return null;
		}
	}

	/**
	 * @brief restore properties
	 */
    public void restoreProperties() {
        super.restoreProperties();

        _setModelType(isTrue("isRobot", isRobot_));
        _setupMarks();

        if (getDblAry(rootLink().getName()+".translation", null) == null &&
            getDblAry(rootLink().getName()+".rotation", null) == null)
            updateInitialTransformRoot();

        for (int i=0; i<jointToLink_.length; i++) {
            GrxLinkItem l = links_.get(jointToLink_[i]);
            Double d = getDbl(l.getName()+".angle", null);
            if (d == null)
                setDbl(l.getName()+".angle", 0.0);
        }
        //propertyChanged();
    }

    /**
     * @brief check validity of new value of property and update if valid
     * @param property name of property
     * @param value value of property
     * @return true if checked(even if value is not used), false otherwise
     */
    public boolean propertyChanged(String property, String value) {
    	if (super.propertyChanged(property, value)){
    	}else if(property.equals("isRobot")){
    		_setModelType(value);
    	}else if(property.equals("controlTime")){
    		try{
    			double t = Double.parseDouble(value);
    			if (t > 0){
    				setProperty("controlTime", value);
    			}
    		}catch(Exception ex){
    		}
    	}else if(property.equals(rootLink().getName()+".translation")){
    		if (rootLink().translation(value)){
    			setProperty(rootLink().getName()+".translation", value);
    			calcForwardKinematics();
    		}
    	}else if(property.equals(rootLink().getName()+".rotation")){
    		if (rootLink().rotation(value)){
    			setProperty(rootLink().getName()+".rotation", value);
    			calcForwardKinematics();
    		}
    	}else{
            for (int j = 0; j < links_.size(); j++){
            	GrxLinkItem link = links_.get(j);
            	if (property.equals(link.getName()+".angle")){
            		if (link.jointValue(value)){
                        calcForwardKinematics();
                        setProperty(link.getName()+".angle", value);
            		}
                    return true;
            	}
            }
    		return false;
    	}
    	return true;

    }

    /**
     * @brief set model type(robot or environment) from String
     * @param value "true" or "false"
     */
    private void _setModelType(String value){
    	Boolean b = Boolean.parseBoolean(value);
		if (b != null){
			_setModelType(b);
		}
    }

    /**
     * @brief set model type(robot or environment)
     * @param isRobot true if this model is robot, false otherwise
     */
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

    /**
     * @brief create spheres to display CoM and projected CoM
     */
    private void _setupMarks() {
        double radius = getDbl("markRadius", DEFAULT_RADIOUS);
        if (switchCom_ == null || radius != DEFAULT_RADIOUS) {
            switchCom_ = GrxShapeUtil.createBall(radius, new Color3f(1.0f, 1.0f, 0.0f));
            switchComZ0_= GrxShapeUtil.createBall(radius, new Color3f(0.0f, 1.0f, 0.0f));
            tgCom_ = (TransformGroup)switchCom_.getChild(0);
            tgComZ0_ = (TransformGroup)switchComZ0_.getChild(0);
            TransformGroup root = getTransformGroupRoot();
            root.addChild(switchCom_);
            root.addChild(switchComZ0_);
        }
        SceneGraphModifier modifier = SceneGraphModifier.getInstance();
        modifier.init_ = true;
        modifier.mode_ = SceneGraphModifier.CREATE_BOUNDS;
        
        Color3f color = new Color3f(0.0f, 1.0f, 0.0f);
        switchBb_ = SceneGraphModifier._makeSwitchNode(modifier._makeBoundingBox(color));
        getTransformGroupRoot().addChild(switchBb_);
    }

    /**
     * @brief load a model from file
     * @param f file that describes a model
     * @return true if loaded successfully, false otherwise
     */
    public boolean load(File f) {
        long load_stime = System.currentTimeMillis();
        if (bgRoot_ != null)
            manager_.setSelectedItem(this, false);
        bgRoot_ = new BranchGroup();
        bgRoot_.setCapability(BranchGroup.ALLOW_DETACH);
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        file_ = f;
        String url = "file:///" + f.getAbsolutePath();
        GrxDebugUtil.println("Loading " + url);
        try {
            ModelLoader mloader = ModelLoaderHelper.narrow(
                GrxCorbaUtil.getReference("ModelLoader"));
            bInfo_ = mloader.getBodyInfo(url);
            //
            LinkInfo[] linkInfoList = bInfo_.links();

            // delete existing model data
            if (rootLink() != null){
            	rootLink().delete();
            }
            switchCom_ = null;

            for (int i=0; i<cameraList_.size(); i++){
                cameraList_.get(i).destroy();
            }
            cameraList_.clear();

            int jointCount = 0;
            for (int i = 0; i < linkInfoList.length; i++) {
            	GrxLinkItem link = new GrxLinkItem(linkInfoList[i].name, manager_, this, linkInfoList[i]); 
                if (link.jointId() >= 0){
                    jointCount++;
                }
            }
            System.out.println("links_.size() = "+links_.size());

            // Search root node.
            int rootIndex = -1;
            for( int i = 0 ; i < links_.size() ; i++ ) {
                if( links_.get(i).parentIndex() < 0 ){
                    if( rootIndex < 0 ) {
                        rootIndex = i;
                    } else {
                        System.out.println( "Error. Two or more root node exist." );
                    }
                }
            }
            if( rootIndex < 0 ){
                System.out.println( "Error, root node doesn't exist." );
            }

            createLink(rootIndex);

            jointToLink_ = new int[jointCount];
            for (int i=0; i<jointCount; i++) {
                for (int j=0; j<links_.size(); j++) {
                    if (links_.get(j).jointId() == i) {
                        jointToLink_[i] = j;
                    }
                }
            }

            Iterator<List<GrxSensorItem>> it = sensorMap_.values().iterator();
            while (it.hasNext()) {
                Collections.sort(it.next());
            }
            long stime = System.currentTimeMillis();
            _loadVrmlScene(linkInfoList);
            long etime = System.currentTimeMillis();
            System.out.println("_loadVrmlScene time = " + (etime-stime) + "ms");
            setURL(url);
            manager_.setSelectedItem(this, true);
            setProperty("isRobot", Boolean.toString(isRobot_));

        } catch (Exception ex) {
            System.out.println("Failed to load vrml model:" + url);
            ex.printStackTrace();
            return false;
        }
        long load_etime = System.currentTimeMillis();
        System.out.println("load time = " + (load_etime-load_stime) + "ms");
        bModified_ = false;
        return true;
    }

    /**
     * @brief make connections between links and gather cameras 
     * @param index index of link in links_
     */
    private void createLink( int index ){

        GrxLinkItem link = links_.get(index);

        // register this to children field of parent link
        if (link.parentIndex() != -1){
        	links_.get(link.parentIndex()).addLink(link);
        }else{
            bgRoot_.addChild(link.bg_);
        }

        // gather cameras
        for (int i=0; i< link.children_.size(); i++){
        	if (link.children_.get(i) instanceof GrxSensorItem){
        		GrxSensorItem sensor = (GrxSensorItem)link.children_.get(i);
        		if (sensor.camera_ != null){
                	cameraList_.add(sensor.camera_);
        		}
        	}
        }

        for( int i = 0 ; i < link.childIndices().length ; i++ )
            {
                // call recursively
                int childIndex = link.childIndices()[i];
                createLink( childIndex );
            }
    }


    /**
     * @brief create shape item from information obtained from ModelLoader
     * @param name name of the shape
     * @param tsi TransformedShapeIndex of the shape to be created
     * @return created shape
     */
    private GrxShapeItem _createShape(String name, TransformedShapeIndex tsi) {
        ShapeInfo[] shapes = bInfo_.shapes();
        AppearanceInfo[] appearances = bInfo_.appearances();
        MaterialInfo[] materials = bInfo_.materials();
        TextureInfo[] textures = bInfo_.textures();

        int shapeIndex = tsi.shapeIndex;
        ShapeInfo shapeInfo = shapes[shapeIndex];
        AppearanceInfo appearanceInfo = null;
        MaterialInfo materialInfo = null;
        TextureInfo textureInfo = null;
        if (shapeInfo.appearanceIndex >= 0){
            appearanceInfo = appearances[shapeInfo.appearanceIndex];
            if (appearanceInfo.materialIndex >= 0){
                materialInfo = materials[appearanceInfo.materialIndex];
            }
            if (appearanceInfo.textureIndex >= 0){
            	textureInfo = textures[appearanceInfo.textureIndex];
            }
        }
        GrxShapeItem shape = new GrxShapeItem(name, manager_, this,
        tsi.transformMatrix, shapeInfo, appearanceInfo, materialInfo, textureInfo);
		return shape;
	}
    /**
     * @brief create shapes of links
     * @param links array of LinkInfo retrieved from ModelLoader
     * @throws BadLinkStructureException
     */
    private void _loadVrmlScene(LinkInfo[] links) throws BadLinkStructureException {

        int numLinks = links.length;
        for(int linkIndex = 0; linkIndex < numLinks; linkIndex++) {
            LinkInfo linkInfo = links[linkIndex];
            GrxLinkItem link = links_.get(linkIndex);
        
            int numShapes = linkInfo.shapeIndices.length;
            for(int localShapeIndex = 0; localShapeIndex < numShapes; localShapeIndex++) {
                TransformedShapeIndex tsi = linkInfo.shapeIndices[localShapeIndex];
                link.addShape(_createShape(linkInfo.name+"_shape_"+localShapeIndex, tsi));
        
                /* normal visualization */
                /*
                if(false){
                    NormalRender nrender = new NormalRender((GeometryArray)linkShape3D.getGeometry(), 0.05f, M);
                    Shape3D nshape = new Shape3D(nrender.getLineArray());
                    linkTopTransformNode.addChild(nshape);
                }
                */
            }
            
            for (int i=0; i<link.children_.size(); i++){
            	if (link.children_.get(i) instanceof GrxSensorItem){
            		GrxSensorItem sensor = (GrxSensorItem)link.children_.get(i);
            		for (int j=0; j<sensor.info_.shapeIndices.length; j++){
            			TransformedShapeIndex tsi = sensor.info_.shapeIndices[j];
            			GrxShapeItem shape = _createShape(sensor.getName()+"_shape_"+j, tsi);
            			sensor.addChild(shape);
            		}
            	}
            }
        }
        
        _setupMarks();
        
        setDblAry(rootLink().getName()+".translation", rootLink().translation());
        setDblAry(rootLink().getName()+".rotation", rootLink().rotation());
        
        for (int i=0; i<links_.size(); i++) {
            Node n = links_.get(i).tg_.getChild(0);
            if (n.getCapability(Node.ENABLE_PICK_REPORTING))
                n.clearCapability(Node.ENABLE_PICK_REPORTING);
        }
        calcForwardKinematics();
        updateInitialTransformRoot();
        updateInitialJointValues();
    }


    /**
     * @brief
     */
    public class NormalRender {
        private LineArray nline = null;

        public NormalRender(GeometryArray geom, float scale, Matrix4d T) {

            Point3f[] vertices = new Point3f[geom.getVertexCount()];
            Vector3f[] normals = new Vector3f[geom.getVertexCount()];
            for (int i=0; i<geom.getVertexCount(); i++) {
                vertices[i] = new Point3f();
                normals[i] = new Vector3f();
            }
            geom.getCoordinates(0, vertices);
            geom.getNormals(0, normals);
            Point3f[] nvertices = new Point3f[vertices.length * 2];
            int n = 0;
            for (int i=0; i<vertices.length; i++ ){

                T.transform(normals[i]);
                normals[i].normalize();
                T.transform(vertices[i]);

                nvertices[n++] = new Point3f( vertices[i] );
                nvertices[n++] = new Point3f( vertices[i].x + scale * normals[i].x,
                                              vertices[i].y + scale * normals[i].y,
                                              vertices[i].z + scale * normals[i].z );
            }
            nline = new LineArray(nvertices.length, GeometryArray.COORDINATES);
            nline.setCoordinates(0, nvertices);
        }

        public LineArray getLineArray() { return nline; }
    }


    /**
     * @brief set transformation of the root joint and all joint values
     * @param lpos transformation of the root joint
     * @param q sequence of joint values
     */
    public void setCharacterPos(LinkPosition[] lpos, double[] q) {
    	if (bModified_) return;
    	
        if (!update_)
            return;

        boolean isAllPosProvided = true;
        for (int i=0; i<links_.size(); i++) {
            if (lpos[i].p == null || lpos[i].R == null)
                isAllPosProvided = false;
            else
                _setTransform(i, lpos[i].p, lpos[i].R);
        }

        if (q != null) {
            for (int i=0; i<jointToLink_.length; i++)
                links_.get(jointToLink_[i]).jointValue(q[i]);
        }

        if (isAllPosProvided)
            _updateCoM();
        else
            calcForwardKinematics();
    }

    /**
     * @brief set transformation of the root joint
     * @param pos position
     * @param rot rotation matrix
     */
    public void setTransformRoot(Vector3d pos, Matrix3d rot) {
        _setTransform(0, pos, rot);
    }

    /**
     * @brief set transformation of the root joint
     * @param pos position
     * @param rot rotation matrix
     */
    public void setTransformRoot(Transform3D tform) {
    	Vector3d pos = new Vector3d();
    	Matrix3d rot = new Matrix3d();
    	tform.get(rot, pos);
        _setTransform(0, pos, rot);
    }

    /**
     * @brief set transformation of the root joint
     * @param pos position
     * @param rot rotation matrix
     */
    public void setTransformRoot(double[] pos, double[] rot) {
        _setTransform(0, pos, rot);
    }

    /**
     * @brief set transformation of linkId th joint
     * @param linkId id of the link
     * @param pos position
     * @param rot rotation matrix
     */
    private void _setTransform(int linkId, Vector3d pos, Matrix3d rot) {
    	GrxLinkItem link = links_.get(linkId);
    	if (link != null) link.setTransform(pos, rot);
    }

    /**
     * @brief set transformation of linkId th joint
     * @param linkId id of the link
     * @param pos position(length=3)
     * @param rot rotation matrix(length=9)
     */
    private void _setTransform(int linkId, double[] p, double[] R) {
    	GrxLinkItem link = links_.get(linkId);
    	if (link != null) link.setTransform(p, R);
    }

    /**
     * @brief set joint values
     * @param values joint values
     */
    public void setJointValues(final double[] values) {
    	rootLink().jointValue(values);
    }

    /**
     * @brief modify joint value if it exceeds limit values
     */
    public void setJointValuesWithinLimit() {
        rootLink().setJointValuesWithinLimit();
    }

    /**
     * @brief update initial transformation property from current Transform3D
     */
    public void updateInitialTransformRoot() {
        Transform3D t3d = new Transform3D();
        Matrix3d m3d = new Matrix3d();
        Vector3d v3d = new Vector3d();

        getTransformGroupRoot().getTransform(t3d);
        t3d.get(m3d, v3d);
        setDblAry(rootLink().getName()+".translation", new double[]{v3d.x, v3d.y, v3d.z});

        AxisAngle4d a4d = new AxisAngle4d();
        a4d.set(m3d);
        setDblAry(rootLink().getName()+".rotation", new double[]{a4d.x, a4d.y, a4d.z, a4d.angle});
    }

    /**
     * @brief update joint value property from current joint value
     * @param link link
     */
    public void updateInitialJointValue(GrxLinkItem link) {
        if (link != null)
            setDbl(link.getName()+".angle", link.jointValue());
    }

    /**
     * @breif update initial joint value properties from current joint values
     */
    public void updateInitialJointValues() {
        for (int i=0; i<jointToLink_.length; i++) {
            GrxLinkItem l = links_.get(jointToLink_[i]);
            setDbl(l.getName()+".angle", l.jointValue());
        }
    }

    /**
     * @brief compute forward kinematics
     */
    public  void calcForwardKinematics() {
        rootLink().calcForwardKinematics();
        _updateCoM();
    }

    /**
     * @brief update CoM and projected CoM positions
     */
    private void _updateCoM() {
        if (switchCom_.getWhichChild() == Switch.CHILD_ALL ||
            switchComZ0_.getWhichChild() == Switch.CHILD_ALL) {
        	Vector3d v3d = new Vector3d();
            getCoM(v3d);
            Vector3d vz0 = new Vector3d(v3d);

            _globalToRoot(v3d);
            Transform3D t3d = new Transform3D();
            t3d.set(v3d);
            tgCom_.setTransform(t3d);

            vz0.z = 0.0;
            _globalToRoot(vz0);
            t3d.set(vz0);
            tgComZ0_.setTransform(t3d);
        }
    }

    /**
     * @brief convert global position into robot local
     * @param pos global position It is overwritten by robot local position
     */
    private void _globalToRoot(Vector3d pos) {
        Transform3D t3d = new Transform3D();
        getTransformGroupRoot().getTransform(t3d);
        Vector3d p = new Vector3d();
        t3d.get(p);
        t3d.invert();
        pos.sub(p);
        t3d.transform(pos);
    }

    /**
     * @brief compute center of mass
     * @param pos computed center of mass
     */
    public void getCoM(Vector3d pos) {
        pos.x = 0.0;
        pos.y = 0.0;
        pos.z = 0.0;
        double totalMass = 0.0;

        for (int i = 0; i < links_.size(); i++) {
        	GrxLinkItem link = links_.get(i);
            totalMass += link.mass();
            Vector3d absCom = link.absCoM();
            absCom.scale(link.mass());
            pos.add(absCom);
        }
        pos.scale(1.0 / totalMass);
    }

    public String[] getSensorNames(String type) {
        List<GrxSensorItem> l = sensorMap_.get(type);
        if (l == null)
            return null;

        String[] ret = new String[l.size()];
        for (int i=0; i<ret.length; i++)
            ret[i] = l.get(i).getName();
        return ret;
    }

    public TransformGroup getTransformGroupRoot() {
        return rootLink().tg_;
    }

    /**
     * @brief get transform of link in array form
     * @param link link
     * @return array. lenth = 12 = position(3)+rotation(9)
     */
    public double[] getTransformArray(GrxLinkItem link) {
        Transform3D t3d = new Transform3D();
        link.tg_.getTransform(t3d);
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

    public double[] getInitialTransformArray(GrxLinkItem link) {
        double[] ret = getTransformArray(link);

        double[] p = getDblAry(link.getName()+".translation", null);
        if (p != null && p.length == 3) {
            System.arraycopy(p, 0, ret, 0, 3);
        }

        double[] r = getDblAry(link.getName()+".rotation", null);
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
            names[i] = links_.get(jointToLink_[i]).getName();
        return names;
    }

    public double[] getJointValues() {
        double[] vals = new double[jointToLink_.length];
        for (int i=0; i<jointToLink_.length; i++)
            vals[i] = links_.get(jointToLink_[i]).jointValue();
        return vals;
    }

    public double[] getInitialJointValues() {
        double[] ret = new double[jointToLink_.length];
        for (int i=0; i<ret.length; i++) {
            GrxLinkItem l = links_.get(jointToLink_[i]);
            String jname = l.getName();
            ret[i] = getDbl(jname+".angle", l.jointValue());
        }
        return ret;
    }

    public double[] getInitialJointMode() {
        double[] ret = new double[jointToLink_.length];
        for (int i=0; i<ret.length; i++) {
            String jname = links_.get(jointToLink_[i]).getName();
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
        /*
        if (!b) {
            bgRoot_.detach();
            for (int i=0; i<cameraList_.size(); i++)
                cameraList_.get(i).getBranchGroup().detach();
        }
        */
    }
    
    /**
     * @brief set/unset focus on this item
     * 
     * When this item is focused, its bounding box is displayed.
     * @param true to focus, false to unfocus
     */
    public void setFocused(boolean b){
    	if (b) resizeBoundingBox();
    	switchBb_.setWhichChild(b ? Switch.CHILD_ALL : Switch.CHILD_NONE);
    }

    /**
     * delete this item
     */
    public void delete() {
        super.delete();
        bgRoot_.detach();
    }
    
    /**
     * @brief resize bounding box which covers the whole body
     */
    public void resizeBoundingBox() {
    	SceneGraphModifier modifier = SceneGraphModifier.getInstance();
        modifier.init_  = true;
        modifier.mode_ = SceneGraphModifier.RESIZE_BOUNDS;

        // ノードのRootのTransformGroupを取得
        TransformGroup tg = getTransformGroupRoot();
        Transform3D t3dLocal = new Transform3D();
        tg.getTransform(t3dLocal);
		t3dLocal.invert();
		for (int i=0; i<links_.size(); i++) {
        	modifier._calcUpperLower(links_.get(i).tg_, t3dLocal);
		}
    	Shape3D shapeNode = (Shape3D)switchBb_.getChild(0);
    	Geometry gm = (Geometry)shapeNode.getGeometry(0);

    	Point3f[] p3fW = modifier._makePoints();
    	if (gm instanceof QuadArray) {
    		QuadArray qa = (QuadArray) gm;
    		qa.setCoordinates(0, p3fW);  // 座標
		}
    }

    /**
     * @brief set visibility of CoM
     * @param b true to make it visible, false otherwise
     */
    public void setVisibleCoM(boolean b) {
        if (isRobot()) {
            switchCom_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
            calcForwardKinematics();
        } else {
            switchCom_.setWhichChild(Switch.CHILD_NONE);
        }
    }

    /**
     * @brief set visibility of CoM projected on the floor
     * @param b true to make it visible, false otherwise
     */
    public void setVisibleCoMonFloor(boolean b) {
        if (isRobot()) {
            switchComZ0_.setWhichChild(b? Switch.CHILD_ALL:Switch.CHILD_NONE);
            calcForwardKinematics();
        } else {
            switchComZ0_.setWhichChild(Switch.CHILD_NONE);
        }
    }

    /**
     * @brief switch display mode between fill and line
     * @param b true to switch to line mode, false otherwise
     */
    public void setWireFrame(boolean b) {
    	setWireFrame(b, bgRoot_);
    }
    
    /**
     * @brief switch display mode between fill and line
     * @param b true to switch to line mode, false otherwise
     * @param node top of subtree to be processed
     */
    public void setWireFrame(boolean b, Node node){
        if (node instanceof Switch) {
            return;
        } else if (node instanceof Group) {
            Group g = (Group) node;
            for (int i = 0; i < g.numChildren(); i++)
                setWireFrame(b, g.getChild(i));

        } else if (node instanceof Link) {
            Link l = (Link) node;
            SharedGroup sg = l.getSharedGroup();
            for (int i = 0; i < sg.numChildren(); i++)
                setWireFrame(b, sg.getChild(i));

        } else if (node instanceof Shape3D) {
            Shape3D s3d = (Shape3D) node;
            Appearance app = s3d.getAppearance();
            if (app != null) {
                PolygonAttributes pa = app.getPolygonAttributes();
                if (pa != null){
                    if (b) {
                        pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
                    } else {
                        pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
                    }
                }
            }
        }    	
    }

    /**
     * @brief 
     * @param b
     */
    public void setTransparencyMode(boolean b) {
    	setTransparencyMode(b, bgRoot_);
    }

    /**
     * @brief
     * @param b
     * @param node
     */
    private void setTransparencyMode(boolean b, Node node) {
        if (node instanceof Switch) {
            return;
        } else if (node instanceof Group) {
            Group g = (Group) node;
            for (int i = 0; i < g.numChildren(); i++)
                setTransparencyMode(b, g.getChild(i));

        } else if (node instanceof Link) {
            Link l = (Link) node;
            SharedGroup sg = l.getSharedGroup();
            for (int i = 0; i < sg.numChildren(); i++)
                setTransparencyMode(b, sg.getChild(i));

        } else if (node instanceof Shape3D) {
            Shape3D s3d = (Shape3D) node;
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

    /**
     * @brief get sequence of cameras
     * @return sequence of cameras
     */
    public List<Camera_impl> getCameraSequence () {
        return cameraList_;
    }

    /**
     * @brief set color of joint
     * @param jid joint id
     * @param color color
     */
    public void setJointColor(int jid, java.awt.Color color) {
        if (color == null)
            setAmbientColorRecursive(links_.get(jointToLink_[jid]).tg_, new Color3f(0.0f, 0.0f, 0.0f));
        else
            setAmbientColorRecursive(links_.get(jointToLink_[jid]).tg_, new Color3f(color));
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


    /* this method is disabled to hide paste menu 
    public void paste(String clipVal){

        Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable data = clp.getContents(null);

        String strClip = "";

        if (data == null || !data.isDataFlavorSupported(DataFlavor.stringFlavor)){
            strClip = "転送に失敗しました";
        } else {
            try {
                strClip = (String)data.getTransferData( DataFlavor.stringFlavor );
            } catch(Exception e) {
                GrxDebugUtil.printErr("GrxModelItem.paste: " , e);
            }
        }
    }
    */
    
    /**
     * @brief Override clone method
     * @return GrxModelItem
     */   
    public GrxModelItem clone(){
    	GrxModelItem ret = (GrxModelItem)super.clone();
    	ret.bInfo_ = (BodyInfo)bInfo_._duplicate(); 
    	//ret.bgRoot_ = (BodyInfo)bgRoot_.cloneTree();
    	
    	ret.links_ = new Vector<GrxLinkItem>(links_);
    	//ret.activeLink_ = activeLink_.clone();
/*    	
	Deep copy suspension list

    public BodyInfo bInfo_;

    public BranchGroup bgRoot_ = new BranchGroup();
    public Vector<GrxLinkItem> links_ = new Vector<GrxLinkItem>();
    public GrxLinkItem activeLink_;
    private int[] jointToLink_; // length = joint number
    private final Map<String, GrxLinkItem> linkMap_ = new HashMap<String, GrxLinkItem>();
    private final Vector<Shape3D> shapeVector_ = new Vector<Shape3D>();
    // sensor type name -> list of sensors
    private final Map<String, List<GrxSensorItem>> sensorMap_ = new HashMap<String, List<GrxSensorItem>>();
    // list of cameras
    private List<Camera_impl> cameraList_ = new ArrayList<Camera_impl>();

    private Switch switchCom_;
    private TransformGroup tgCom_;
    private Switch switchComZ0_;
    private TransformGroup tgComZ0_;

    // temporary variables for computation
    private Transform3D t3d_ = new Transform3D();
    private Vector3d v3d_ = new Vector3d();
*/
    	
    	return ret;
    }
}

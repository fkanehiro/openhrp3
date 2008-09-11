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

import com.sun.j3d.utils.geometry.*;
import java.awt.image.*;
import com.sun.j3d.utils.picking.PickTool;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.view.tdview.*;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

import jp.go.aist.hrp.simulator.*;
import java.awt.image.BufferedImage;

@SuppressWarnings({ "unchecked", "serial" })
/**
 * @brief item corresponds to a robot model
 */
public class GrxModelItem extends GrxBaseItem implements Manipulatable {
    public static final String TITLE = "Model";
    public static final String DEFAULT_DIR = "../../etc";
    public static final String FILE_EXTENSION = "wrl";

    private boolean isRobot_ = true;
    public boolean update_ = true;
    
    public BodyInfo bInfo_;
	
    public BranchGroup bgRoot_;
    public Vector<GrxLinkItem> links_;
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
    private static final double DEFAULT_RADIOUS = 0.05;
    
    // temporary varibales for computation
    private Transform3D t3d_ = new Transform3D(); 
    private Transform3D t3dm_ = new Transform3D(); 
    private Vector3d v3d_ = new Vector3d();
    private AxisAngle4d a4d_ = new AxisAngle4d();
    private Matrix3d m3d_ = new Matrix3d();
    private Matrix3d m3d2_ = new Matrix3d();
    private Vector3d v3d2_ = new Vector3d();

    // icons
   	private static final String robotIcon = "robot.png";
   	private static final String envIcon = "environment.png";

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
		// menu item : reload
		setMenuItem(new Action(){
            public String getText(){ return "reload"; }
			public void run(){
				load(GrxModelItem.this.file_);
			}
		});

        setMenuItem(menuChangeType_);
    }
	
	/**
	 * @brief create a new model
	 * @return true if created successfully, false otherwise
	 */
	/* this method will be enabled later
	public boolean create() {
		return true;
	}
	*/

	/**
	 * @brief get root link
	 */
	public GrxLinkItem rootLink() {
		return links_.get(0);
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
        propertyChanged();
    }

    /**
     * @brief properties are set to robot
     */
    public void propertyChanged() {
    	super.propertyChanged();
        double[] p = getDblAry(rootLink().getName()+".translation", null);
        if (p == null || p.length != 3)
            p = new double[]{0, 0, 0};
		
        double[] R = getDblAry(rootLink().getName()+".rotation", null);
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
		
        for (int j = 0; j < links_.size(); j++) 
            links_.get(j).jointValue(getDbl(links_.get(j).getName() + ".angle", 0.0));
		
        calcForwardKinematics();
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
     * @brief get transform group of the root joint
     * @return transform group of the root joint
     */
    TransformGroup rootTransformGroup() {
    	return rootLink().tg_;
    }
    
    /**
     * @brief create spheres to display CoM and projected CoM
     */
    private void _setupMarks() {
        double radius = getDbl("markRadius", DEFAULT_RADIOUS);
        if (switchCom_ == null || radius != DEFAULT_RADIOUS) {
            switchCom_ = createBall(radius, new Color3f(1.0f, 1.0f, 0.0f));
            switchComZ0_= createBall(radius, new Color3f(0.0f, 1.0f, 0.0f)); 
            tgCom_ = (TransformGroup)switchCom_.getChild(0);
            tgComZ0_ = (TransformGroup)switchComZ0_.getChild(0);
            TransformGroup root = rootTransformGroup();
            root.addChild(switchCom_);
            root.addChild(switchComZ0_);
        }
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
		
        file_ = f;
        String url = "file:///" + f.getAbsolutePath();
        GrxDebugUtil.println("Loading " + url);
        try {
            ModelLoader mloader = ModelLoaderHelper.narrow(
                GrxCorbaUtil.getReference("ModelLoader", "localhost", 2809));
            bInfo_ = mloader.getBodyInfo(url);
            //
            LinkInfo[] linkInfoList = bInfo_.links();
            links_ = new Vector<GrxLinkItem>();
            linkMap_.clear();
            
            for (int i=0; i<cameraList_.size(); i++){
                cameraList_.get(i).destroy();
            }
            cameraList_.clear();
            
            int jointCount = 0;
            for (int i = 0; i < linkInfoList.length; i++) {
                links_.add(new GrxLinkItem(linkInfoList[i].name, manager_, linkInfoList[i]));
                linkMap_.put(links_.get(i).getName(), links_.get(i));
                if (links_.get(i).jointId() >= 0){
                    jointCount++;
                }
            }
            
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
        return true;
    }

    private void createLink( int index ){
        
        GrxLinkItem link = links_.get(index);
        
        // register this to children field of parent link
        if (link.parentIndex() != -1){
        	links_.get(link.parentIndex()).addLink(link);
        }
        
        // gather cameras
        for (int i=0; i< link.cameras_.size(); i++){
        	cameraList_.add(link.cameras_.get(i));
        }
        
        
        for( int i = 0 ; i < link.childIndices().length ; i++ ) 
            {
                // call recursively
                int childIndex = link.childIndices()[i];
                createLink( childIndex );
            }
    }


    /**
     * @brief create shapes of links
     * @param links array of LinkInfo retrieved from ModelLoader
     * @throws BadLinkStructureException
     */
    private void _loadVrmlScene(LinkInfo[] links) throws BadLinkStructureException {

        ShapeInfo[] shapes = bInfo_.shapes();
        AppearanceInfo[] appearances = bInfo_.appearances();
        MaterialInfo[] materials = bInfo_.materials();
        TextureInfo[] textures = bInfo_.textures();

        int numLinks = links.length;
        for(int linkIndex = 0; linkIndex < numLinks; linkIndex++) {

            LinkInfo linkInfo = links[linkIndex];
            GrxLinkItem link = links_.get(linkIndex);

            TransformGroup linkTopTransformNode = link.tg_;

            int numShapes = linkInfo.shapeIndices.length;
            for(int localShapeIndex = 0; localShapeIndex < numShapes; localShapeIndex++) {
                TransformedShapeIndex tsi = linkInfo.shapeIndices[localShapeIndex];
                int shapeIndex = tsi.shapeIndex;
                ShapeInfo shapeInfo = shapes[shapeIndex];
                Shape3D linkShape3D = createLinkShape3D(shapeInfo, appearances, materials, textures);

                TransformGroup shapeTransform = new TransformGroup();

                double[] m = tsi.transformMatrix;
                Matrix4d M = new Matrix4d(m[0], m[1], m[2],  m[3],
                                          m[4], m[5], m[6],  m[7],
                                          m[8], m[9], m[10], m[11],
                                          0.0,  0.0,  0.0,   1.0);
                shapeTransform.setTransform(new Transform3D(M));
                shapeTransform.addChild(linkShape3D);

                BranchGroup bg = new BranchGroup();
                bg.setCapability(BranchGroup.ALLOW_DETACH);
                bg.addChild(shapeTransform);
                
                GrxShapeItem shape = new GrxShapeItem(linkInfo.name+"_shape_"+localShapeIndex, manager_, bg);
                link.addShape(shape);

                /* normal visualization */
                if(false){
                    NormalRender nrender = new NormalRender((GeometryArray)linkShape3D.getGeometry(), 0.05f, M);
                    Shape3D nshape = new Shape3D(nrender.getLineArray());
                    linkTopTransformNode.addChild(nshape);
                }
            }

            Vector<GrxSensorItem> sensors = links_.get(linkIndex).sensors_;
            for (int j=0; j < sensors.size(); j++) {
                GrxSensorItem si = sensors.get(j);

                if (si.type().equals("Vision")) {

                    Camera_impl camera = cameraList_.get(si.id());
                    linkTopTransformNode.addChild(camera.getBranchGraph());

                    double[] pos = si.translation();
                    double[] rot = si.rotation();

                    Transform3D t3d = new Transform3D();
                    t3d.setTranslation(new Vector3d(pos));
                    t3d.setRotation(new AxisAngle4d(rot));
                    camera.getTransformGroup().setTransform(t3d);
                }
            }
        }

        SceneGraphModifier modifier = SceneGraphModifier.getInstance();
        for (int i = 0; i < links_.size(); i++) {
            Map<String, Object> userData = new Hashtable<String, Object>();
            GrxLinkItem info = links_.get(i);
            userData.put("object", this);
            userData.put("linkInfo", info);
            userData.put("objectName", this.getName());
            userData.put("jointName", info.getName());
            Vector3d jointAxis = new Vector3d(info.jointAxis());
            GrxLinkItem itmp = info;
            while (itmp.jointId() == -1 && itmp.parentIndex() != -1) {
                itmp = links_.get(itmp.parentIndex());
            }
            userData.put("controllableJoint", itmp.getName());
			 
            TransformGroup g = links_.get(i).tg_;
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
			
            bgRoot_.addChild(links_.get(i).bg_);
        }
		
        rootTransformGroup().setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        _setupMarks();
		
        _traverse(bgRoot_, 0);
		
        modifier.modifyRobot(this);
		
        setDblAry(rootLink().getName()+".translation", rootLink().translation());
        setDblAry(rootLink().getName()+".rotation", rootLink().rotation());
        propertyChanged();

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
     * @brief setup capabilities and shapeVector_
     * @param node node where this process starts
     * @param depth current depth. This can be used for debug output
     */
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

    /**
     * @brief create Shape3D object from shapeInfo, appearanceInfo, MaterialInfo and TextureInfo
     * @param shapeInfo
     * @param appearances
     * @param materials
     * @param textures
     * @return
     */
    private Shape3D createLinkShape3D
    (ShapeInfo shapeInfo, AppearanceInfo[] appearances, MaterialInfo[] materials, TextureInfo[] textures){
        
        GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

        // set vertices
        int numVertices = shapeInfo.vertices.length / 3;
        Point3f[] vertices = new Point3f[numVertices];
        for(int i=0; i < numVertices; ++i){
            vertices[i] = new Point3f(shapeInfo.vertices[i*3], shapeInfo.vertices[i*3+1], shapeInfo.vertices[i*3+2]);
        }
        geometryInfo.setCoordinates(vertices);
        
        // set triangles (indices to the vertices)
        geometryInfo.setCoordinateIndices(shapeInfo.triangles);
        
        Appearance appearance = new Appearance();
        PolygonAttributes pa = new PolygonAttributes();
        pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        pa.setBackFaceNormalFlip(true);
        appearance.setPolygonAttributes(pa);

        int appearanceIndex = shapeInfo.appearanceIndex;
        if(appearanceIndex >= 0){

            AppearanceInfo appearanceInfo = appearances[appearanceIndex];

            setColors(geometryInfo, shapeInfo, appearanceInfo);

            MaterialInfo materialInfo = null;
            int materialIndex = appearanceInfo.materialIndex;
            if(materialIndex >= 0){
                materialInfo = materials[materialIndex];
                if(materialInfo.transparency > 0.0f){
                    TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.NICEST, materialInfo.transparency);
                    appearance.setTransparencyAttributes(ta);
                }
            }

            setNormals(geometryInfo, shapeInfo, appearanceInfo);

            if(materialInfo != null){
                appearance.setMaterial(createMaterial(materialInfo));
            }

            int textureIndex = appearanceInfo.textureIndex;
            if(textureIndex >= 0){
                
                TextureInfoLocal texInfo = new TextureInfoLocal(textures[textureIndex]);

                Texture2D texture2d = null;
                if((texInfo.width != 0) && (texInfo.height != 0)){
                    ImageComponent2D icomp2d = texInfo.readImage;
                    texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, texInfo.width, texInfo.height);
                    texture2d.setImage(0, icomp2d);
                }

                if(texture2d != null){
                    appearance.setTexture(texture2d);
                }
                
                int vTris = (shapeInfo.triangles.length)/3;
                Point2f[] TexPoints = new Point2f[vTris * 3];
                for(int vi=0; vi<vTris; vi++){
                    TexPoints[vi*3] = new Point2f( 0.0f , 0.0f );
                    TexPoints[vi*3+1] = new Point2f( 1.0f , 0.0f );
                    TexPoints[vi*3+2] = new Point2f( 1.0f , 1.0f );
                }
                geometryInfo.setTextureCoordinates(TexPoints);
                geometryInfo.setTextureCoordinateIndices(shapeInfo.triangles);
                //TextureAttributes Set
                TextureAttributes texAttrBase =  new TextureAttributes();
                //TextureAttributes Property Set
                texAttrBase.setTextureMode(TextureAttributes.MODULATE);
                //Appearance <- TextureAttributes 
                appearance.setTextureAttributes(texAttrBase);
            }
        }

        Shape3D shape3D = new Shape3D(geometryInfo.getGeometryArray());
        shape3D.setAppearance(appearance);

        return shape3D;
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


    private void setColors(GeometryInfo geometryInfo, ShapeInfo shapeInfo, AppearanceInfo appearanceInfo) {

        int numColors = appearanceInfo.colors.length / 3;

        if(numColors > 0){
            float[] orgColors = appearanceInfo.colors;
            Color3f[] colors = new Color3f[numColors];
            for(int i=0; i < numColors; ++i){
                colors[i] = new Color3f(orgColors[i*3], orgColors[i*3+1], orgColors[i*3+2]);
            }
            geometryInfo.setColors(colors);
            
            int[] orgColorIndices = appearanceInfo.colorIndices;
            int numOrgColorIndices = orgColorIndices.length;
            int numTriangles = shapeInfo.triangles.length / 3;
            int[] colorIndices = new int[numTriangles * 3];
                
            if(numOrgColorIndices > 0){
                if(appearanceInfo.colorPerVertex){
                    colorIndices = orgColorIndices;
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        int colorIndex = orgColorIndices[i];
                        for(int j=0; j < 3; ++j){
                            colorIndices[pos++] = colorIndex;
                        }
                    }
                }
            } else {
                if(appearanceInfo.colorPerVertex){
                    for(int i=0; i < colorIndices.length; ++i){
                        colorIndices[i] = shapeInfo.triangles[i];
                    }
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        for(int j=0; j < 3; ++j){
                            colorIndices[pos++] = i;
                        }
                    }                        
                    
                }
            }
            geometryInfo.setColorIndices(colorIndices);
        }
    }


    private void setNormals(GeometryInfo geometryInfo, ShapeInfo shapeInfo, AppearanceInfo appearanceInfo) {

        int numNormals = appearanceInfo.normals.length / 3;

        if(numNormals == 0){
            NormalGenerator ng = new NormalGenerator(appearanceInfo.creaseAngle);
            ng.generateNormals(geometryInfo);
            
        } else {

            float[] orgNormals = appearanceInfo.normals;
            Vector3f[] normals = new Vector3f[numNormals];
            for(int i=0; i < numNormals; ++i){
                normals[i] = new Vector3f(orgNormals[i*3], orgNormals[i*3+1], orgNormals[i*3+2]);
            }
            geometryInfo.setNormals(normals);

            int[] orgNormalIndices = appearanceInfo.normalIndices;
            int numOrgNormalIndices = orgNormalIndices.length;
            int numTriangles = shapeInfo.triangles.length / 3;
            int[] normalIndices = new int[numTriangles * 3];
                
            if(numOrgNormalIndices > 0){
                if(appearanceInfo.normalPerVertex){
                    normalIndices = orgNormalIndices;
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        int normalIndex = orgNormalIndices[i];
                        for(int j=0; j < 3; ++j){
                            normalIndices[pos++] = normalIndex;
                        }
                    }
                }
            } else {
                if(appearanceInfo.normalPerVertex){
                    for(int i=0; i < normalIndices.length; ++i){
                        normalIndices[i] = shapeInfo.triangles[i];
                    }
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        for(int j=0; j < 3; ++j){
                            normalIndices[pos++] = i;
                        }
                    }                        
                    
                }
            }

            geometryInfo.setNormalIndices(normalIndices);
        }
    }


    private Material createMaterial(MaterialInfo materialInfo){

        Material material = new Material();
        
        float[] dColor = materialInfo.diffuseColor;
        material.setDiffuseColor(new Color3f(dColor[0], dColor[1], dColor[2]));

        float[] sColor = materialInfo.specularColor;
        material.setSpecularColor(new Color3f(sColor[0], sColor[1], sColor[2]));

        float[] eColor = materialInfo.emissiveColor;
        material.setEmissiveColor(new Color3f(eColor[0], eColor[1], eColor[2]));

        float r = materialInfo.ambientIntensity;
        material.setAmbientColor(new Color3f(r * dColor[0], r * dColor[1], r * dColor[2]));
        
        float shininess = materialInfo.shininess * 127.0f + 1.0f;
        material.setShininess(shininess);
        
        return material;
    }
    
    /**
     * @brief set transformation of the root joint and all joint values
     * @param lpos transformation of the root joint
     * @param q sequence of joint values
     */
    public void setCharacterPos(LinkPosition[] lpos, double[] q) {
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
     * @brief set transformation of linkId th joint
     * @param linkId id of the link
     * @param pos position
     * @param rot rotation matrix
     */
    private void _setTransform(int linkId, Vector3d pos, Matrix3d rot) {
        TransformGroup tg = links_.get(linkId).tg_;
        tg.getTransform(t3d_);
        if (pos != null)
            t3d_.set(pos);
        if (rot != null)
            t3d_.setRotation(rot);
        tg.setTransform(t3d_);
    }

    /**
     * @brief set transformation of linkId th joint
     * @param linkId id of the link
     * @param pos position
     * @param rot rotation matrix
     */
    private void _setTransform(int linkId, double[] p, double[] R) {
        Vector3d pos = null;
        Matrix3d rot = null;
        if (p != null)
            pos = new Vector3d(p);
        if (R != null)
            rot = new Matrix3d(R);
        _setTransform(linkId, pos, rot);
    }

    /**
     * @brief set joint value
     * @param jname name of the joint
     * @param value of the joint
     */
    public void setJointValue(String jname, double value) {
        GrxLinkItem l = getLinkInfo(jname);
        if (l != null) 
            l.jointValue(value);
    }

    /**
     * @brief set joint values
     * @param values joint values
     */
    public void setJointValues(final double[] values) {
        if (values.length != jointToLink_.length)
            return;
        for (int i=0; i<jointToLink_.length; i++) {
            GrxLinkItem l = links_.get(jointToLink_[i]);
            l.jointValue(values[i]);
        }
    }

    /**
     * @brief modify joint value If it exceeds limit values
     */
    public void setJointValuesWithinLimit() {
        for (int i = 1; i < links_.size(); i++) {
            GrxLinkItem l = links_.get(i);
            if (l.llimit_[0] < l.ulimit_[0]) {
                if (l.jointValue() < l.llimit_[0])
                    l.jointValue(l.llimit_[0]);
                else if (l.ulimit_[0] < l.jointValue())
                    l.jointValue(l.ulimit_[0]);
            }
        }
    }

    /**
     * @brief update transformation property of the root joint
     */
    public void updateInitialTransformRoot() {
        Transform3D t3d = new Transform3D();
        Matrix3d m3d = new Matrix3d();
        Vector3d v3d = new Vector3d();
		
        rootTransformGroup().getTransform(t3d);
        t3d.get(m3d, v3d);
        setDblAry(rootLink().getName()+".translation", new double[]{v3d.x, v3d.y, v3d.z});
		
        AxisAngle4d a4d = new AxisAngle4d();
        a4d.set(m3d);
        setDblAry(rootLink().getName()+".rotation", new double[]{a4d.x, a4d.y, a4d.z, a4d.angle});
    }

    /**
     * @brief update joint value property
     * @param jname name of the joint
     */
    public void updateInitialJointValue(String jname) {
        GrxLinkItem l = getLinkInfo(jname);
        if (l != null)
            setDbl(jname+".angle", l.jointValue());
    }

    public void updateInitialJointValues() {
        for (int i=0; i<jointToLink_.length; i++) {
            GrxLinkItem l = links_.get(jointToLink_[i]);
            setDbl(l.getName()+".angle", l.jointValue());
        }
    }

    public  void calcForwardKinematics() {
        rootLink().calcForwardKinematics();
        _updateCoM();
    }

    private void _updateCoM() {
        if (switchCom_.getWhichChild() == Switch.CHILD_ALL ||
            switchComZ0_.getWhichChild() == Switch.CHILD_ALL) {
            getCoM(v3d_);
            Vector3d vz0 = new Vector3d(v3d_);
			
            _globalToRoot(v3d_);
            t3d_.set(v3d_);
            tgCom_.setTransform(t3d_);
			
            vz0.z = 0.0;
            _globalToRoot(vz0);
            t3d_.set(vz0);
            tgComZ0_.setTransform(t3d_);
        }
    }

    private void _globalToRoot(Vector3d pos) {
        Transform3D t3d = new Transform3D();
        rootTransformGroup().getTransform(t3d);
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
        for (int i = 0; i < links_.size(); i++) {
            totalMass += links_.get(i).mass();
            absCom.set(links_.get(i).centerOfMass());
            TransformGroup tg = links_.get(i).tg_;
            tg.getTransform(t3d);
            t3d.transform(absCom);
            t3d.get(p);
            absCom.add(p);
            absCom.scale(links_.get(i).mass());
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

    public GrxLinkItem getLinkInfo(String linkName) {
        return linkMap_.get(linkName);
    }

    public Transform3D getTransform(String linkName) {
        GrxLinkItem l = getLinkInfo(linkName);
        if (l == null)
            return null;
        Transform3D ret = new Transform3D();
        l.tg_.getTransform(ret);
        return ret;
    }

    public Transform3D getTransformfromRoot(String linkName) {
        Transform3D t3d = getTransform(linkName);
        if (t3d == null)
            return null;
        Transform3D t3dr = new Transform3D();
        rootTransformGroup().getTransform(t3dr);
        t3d.mulTransposeLeft(t3dr, t3d);
        return t3d;
    }

    public TransformGroup getTransformGroupRoot() {
        return rootLink().tg_;
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
            names[i] = links_.get(jointToLink_[i]).getName();
        return names;
    }

    public double getJointValue(String jname) {
        GrxLinkItem l = getLinkInfo(jname);
        if (l != null)
            return l.jointValue();
        return 0.0;
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
        if (!b) {
            bgRoot_.detach();
            for (int i=0; i<cameraList_.size(); i++)
                cameraList_.get(i).getBranchGraph().detach();
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

        public TextureInfoLocal(TextureInfo texinfo) {
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
        }
    }
    // ##### [Changed]




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

}

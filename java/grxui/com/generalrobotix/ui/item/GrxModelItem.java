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
                    manager_.processingWindow_.setVisible(false);
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
            LinkInfo[] linkInfoList = bInfo_.links();
            lInfo_ = new LinkInfoLocal[linkInfoList.length];
            lInfoMap_.clear();
            
            for (int i=0; i<cameraList.size(); i++){
                cameraList.get(i).destroy();
            }
            cameraList.clear();
            
            int jointCount = 0;
            for (int i = 0; i < lInfo_.length; i++) {
                lInfo_[i] = new LinkInfoLocal(linkInfoList[i]);
                lInfo_[i].myLinkID = (short)i;
                lInfoMap_.put(lInfo_[i].name, lInfo_[i]);
                if (lInfo_[i].jointId >= 0){
                    jointCount++;
                }
            }
            
            // Search root node.
            int rootIndex = -1;
            for( int i = 0 ; i < lInfo_.length ; i++ ) {
                if( lInfo_[i].parentIndex < 0 ){
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
            
            createLink( rootIndex );

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


    public class NormalRender {
        private LineArray nline = null;
        public NormalRender(GeometryArray geom) { this(geom, 1.0f); }
        public NormalRender(GeometryArray geom, float scale) {
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


    private void _loadVrmlScene(LinkInfo[] links) throws BadLinkStructureException {

        ShapeInfo[] shapes = bInfo_.shapes();
        AppearanceInfo[] appearances = bInfo_.appearances();
        MaterialInfo[] materials = bInfo_.materials();
        TextureInfo[] textures = bInfo_.textures();

        int numLinks = links.length;
        for(int linkIndex = 0; linkIndex < numLinks; linkIndex++) {

            LinkInfo linkInfo = links[linkIndex];

            TransformGroup linkTopTransformNode = new TransformGroup();
            lInfo_[linkIndex].tg = linkTopTransformNode;
            
            int numShapes = linkInfo.shapeIndices.length;
            for(int localShapeIndex = 0; localShapeIndex < numShapes; localShapeIndex++) {					
                int shapeIndex = linkInfo.shapeIndices[localShapeIndex];
                ShapeInfo shapeInfo = shapes[shapeIndex];
                Shape3D linkShape3D = createLinkShape3D(shapeInfo, appearances, materials, textures);
                
                linkTopTransformNode.addChild(linkShape3D);

                /* normal visualization */
                if(false){
                    NormalRender nrender = new NormalRender((GeometryArray)linkShape3D.getGeometry(), 0.05f);
                    Shape3D nshape = new Shape3D(nrender.getLineArray());
                    linkTopTransformNode.addChild(nshape);
                }
            }

            SensorInfoLocal[] sensors = lInfo_[linkIndex].sensors;
            for (int j=0; j < sensors.length; j++) {
                SensorInfoLocal si = sensors[j];

                //if (si.type.equals(SensorType.VISION_SENSOR)) {
                if (si.type.equals("Vision")) {

                    Camera_impl camera = cameraList.get(si.id);
                    //g.addChild(camera.getBranchGraph());
                    linkTopTransformNode.addChild(camera.getBranchGraph());

                    double[] pos = si.translation;
                    // #####[Changed] From BODYINFO Convert 			
                    double[] rot = si.rotation;

                    Transform3D t3d = new Transform3D();
                    t3d.setTranslation(new Vector3d(pos));
                    t3d.setRotation(new AxisAngle4d(rot));
                    camera.getTransformGroup().setTransform(t3d);
                }
            }
        }

        if(numLinks > 0){
            bgRoot_.addChild(lInfo_[0].tg);
            for(int i=1; i < numLinks; ++i){
                new BranchGroup().addChild(lInfo_[i].tg);
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
				
                    if (sensors[i].type.equals("Vision")) {
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

}

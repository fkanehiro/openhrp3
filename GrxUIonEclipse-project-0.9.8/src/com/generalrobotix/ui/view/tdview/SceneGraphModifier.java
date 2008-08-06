/**
 * SceneGraphModifier.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 * @version 1.1 Thu Apr 11 2002    Geometry複製処理を追加
 */
package com.generalrobotix.ui.view.tdview;

import java.util.*;

import javax.vecmath.*;
import javax.media.j3d.*;

import com.generalrobotix.ui.item.GrxModelItem;
import com.sun.j3d.utils.picking.PickTool;

import com.sun.j3d.utils.geometry.*;

public class SceneGraphModifier {
    //--------------------------------------------------------------------
    // 定数
    public static final int CREATE_BOUNDS = 0;
    private static final int RESIZE_BOUNDS = 1;
    private static final int SHADING_MODE  = 2;

    public static final int SHADING    = 0;
    public static final int WIRE_FRAME = 1;

    //private static String USERDATA_SWITCH = "this is bounding box switch.";

    //--------------------------------------------------------------------
    // インスタンス変数
    private static SceneGraphModifier this_;
    private float[] lower_   = new float[3];          // 頂点のMax値
    private float[] upper_   = new float[3];          // 頂点のMin値
    public boolean init_;
    private int polygonMode_;
    private int cullFace_;
    public int mode_;

    //--------------------------------------------------------------------
    // コンストラクタ(Singleton pattern)
    private SceneGraphModifier() {}

    //--------------------------------------------------------------------
    // スタティックメソッド
    public static SceneGraphModifier getInstance() {
        if (this_ == null) {
            this_ = new SceneGraphModifier();
        }
        return this_;
    }

    public static Hashtable getHashtableFromTG(TransformGroup tg) {
        if (tg == null) { 
            return null; 
        }
        Object userData = tg.getUserData();
        if (userData instanceof Hashtable) {
            return (Hashtable)userData;
        }
        return null;
    }

    //--------------------------------------------------------------------
    // 公開メソッド
    /**
     * ロボットノードのシーングラフのモディファイ
     *
     * @param  robot         ロボット
     * @version  1.1 Thu Apr 11 2002    Geometry複製処理を追加
     */
    public void modifyRobot(GrxModelItem robot) throws BadLinkStructureException
    {
        TransformGroup tg = (TransformGroup)robot.getTransformGroupRoot(); // for GrxUI
        //_cloneGeometry(tg); // Geometry複製処理 
        _cloneGeometry(robot.bgRoot_); // Geometry複製処理

        init_ = true;
        mode_ = CREATE_BOUNDS;
        
        // 全体を囲む
 	    /*for (int i=0; i<robot.lInfo_.length; i++) {
 			Transform3D t3d = new Transform3D();
 			robot.lInfo_[i].tg.getTransform(t3d);
         	_calcUpperLower(robot.lInfo_[i].tg, t3d);
 		}*/
        
        Color3f color = new Color3f(0.0f, 1.0f, 0.0f);
        Switch bbSwitch = _makeSwitchNode(_makeBoundingBox(color));
        tg.addChild(bbSwitch);

        // スイッチノードをTGのユーザーデータ領域のストア
        Hashtable<String, Switch> userData = (Hashtable<String, Switch>)tg.getUserData();
        userData.put("fullBoundingBoxSwitch", bbSwitch);
        tg.setUserData(userData);
    }

    /**
     *
     * @param  node         SimulationElementNode
     */
    //public void resizeBounds(SimulationElementNode node) {
    public void resizeBounds(GrxModelItem node) {
        init_  = true;
        mode_ = RESIZE_BOUNDS;

        // ノードのRootのTransformGroupを取得
        //TransformGroup tg = node.getTransformGroupRoot();
        BranchGroup tg = node.bgRoot_;
        for (int i = 0; i < tg.numChildren(); i++) {
            try {
                _calcUpperLower(tg.getChild(i), new Transform3D());
            } catch (CapabilityNotSetException ex) {
                ex.printStackTrace();
            }
        }

        // BoundingBoxを作成
        // Switch_Nodeを検索
        for (int i = 0; i < tg.numChildren(); i++) {
            Node childNode = (Node)tg.getChild(i);
            if (childNode instanceof Switch) {
                Group group = (Group)childNode;
                Shape3D shapeNode = (Shape3D)group.getChild(0);
                Geometry gm = (Geometry)shapeNode.getGeometry(0);

                Point3f[] p3fW = _makePoints();
                if (gm instanceof QuadArray) {  // added for GrxUI
                	QuadArray qa = (QuadArray) gm;
                	qa.setCoordinates(0, p3fW);  // 座標
                }  								// added for GrxUI
            }
        }
    }
/*
    public void changeShadingMode(SimulationElementNode node, int mode) {
        mode_ = SHADING_MODE;

        switch (mode) {
        case SHADING:
            polygonMode_ = PolygonAttributes.POLYGON_FILL;
            cullFace_ = PolygonAttributes.CULL_BACK;
            break;
        case WIRE_FRAME:
            polygonMode_ = PolygonAttributes.POLYGON_LINE;
            cullFace_ = PolygonAttributes.CULL_BACK;
            break;
        }

        // ノードのRootのTransformGroupを取得
        TransformGroup tg = node.getTransformGroupRoot();

        for (int i = 0; i < tg.numChildren(); i++) {
            try {
                _calcUpperLower(tg.getChild(i), new Transform3D());
            } catch (CapabilityNotSetException ex) {
                ex.printStackTrace();
            }
        }
    }
*/
    //--------------------------------------------------------------------
    // プライベートメソッド
    private void _setCapabilities(TransformGroup tg) {
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_LOCAL_TO_VWORLD_READ);
        tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
    }

    private void _setSegmentCapabilities(TransformGroup tg) {
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg.setCapability(TransformGroup.ALLOW_LOCAL_TO_VWORLD_READ);
    }

    /**
     *
     * @param  node
     * @param  t3dParent
     * @param  t3dDef
     */
    public  void _calcUpperLower(Node node, Transform3D t3dParent) {
        if (init_) {
            for (int i = 0; i < 3; i  ++) {
                upper_[i] = 0.0f;
                lower_[i] = 0.0f;
            }
        }
        if (node instanceof Group) {
        	// added for GrxUI
            if (node instanceof BranchGroup) {
            	BranchGroup bg = (BranchGroup)node;
                for (int i = 0; i < bg.numChildren(); i++) {
                    _calcUpperLower(bg.getChild(i), t3dParent);
                }
            } else 
            // added for GrxUI	
            if (node instanceof TransformGroup) {
                Transform3D t3dLocal = new Transform3D();
                TransformGroup tg = (TransformGroup)node;
                if (mode_ == CREATE_BOUNDS) {
                    _setSegmentCapabilities(tg);
                }
                tg.getTransform(t3dLocal);
                Transform3D t3dWorld = new Transform3D(t3dParent);
                t3dWorld.mul(t3dLocal);
                for (int i = 0; i < tg.numChildren(); i++) {
                    _calcUpperLower(tg.getChild(i), t3dWorld);
                }
            }else{
                boolean flag = true;
                if ((node instanceof Switch)) {
                    //Object obj = node.getUserData();
                    //if(!(obj instanceof String && USERDATA_SWITCH.equals(obj)))
                    //{
                        flag = false;
                    //}
                }
                if(flag){
                    Group group = (Group)node;
                    if (mode_ == CREATE_BOUNDS) {
                        group.setCapability(Group.ALLOW_CHILDREN_READ);
                    }
                    for (int i = 0; i < group.numChildren(); i++) {
                        _calcUpperLower(group.getChild(i), t3dParent);
                    }
                }
            }
        } else if (node instanceof Link) {
            Link lk = (Link) node;
            if (mode_ == CREATE_BOUNDS) {
                lk.setCapability(Link.ALLOW_SHARED_GROUP_READ);
            }
            SharedGroup sg = lk.getSharedGroup();
            if (mode_ == CREATE_BOUNDS) {
                sg.setCapability(SharedGroup.ALLOW_CHILDREN_READ);
            }
            Group group = (Group) sg;
            for (int i = 0; i < group.numChildren(); i++) {
                _calcUpperLower(group.getChild(i), t3dParent);
            }
        } else if (node instanceof Shape3D) {
            Shape3D shape = (Shape3D) node;
            Appearance appearance = shape.getAppearance();
            
            if (mode_ == CREATE_BOUNDS) {
                shape.setCapability(Node.ENABLE_PICK_REPORTING);
                shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
                PickTool.setCapabilities(shape, PickTool.INTERSECT_FULL);

                // for shading change
                shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
                shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

		if (appearance != null){
		    appearance.setCapability(
                        Appearance.ALLOW_POLYGON_ATTRIBUTES_READ
                    );
		    appearance.setCapability(
                        Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE
                    );
		}
            }

            if (mode_ == SHADING_MODE) {
                //PolygonAttributes attributes = appearance.getPolygonAttributes();
                PolygonAttributes attributes = new PolygonAttributes();
                if (attributes == null) {
                    System.out.println("can't get PolygonAttributes");
                    return;
                }
                attributes.setPolygonMode(polygonMode_);
                attributes.setCullFace(cullFace_);
                //System.out.println("koko" + attributes + appearance);
                if(appearance!=null){
                    appearance.setPolygonAttributes(attributes);
                }
                return;
            }

            // 三角形の頂点を求める
            Geometry geometry = shape.getGeometry();

            if (mode_ == CREATE_BOUNDS) {
                geometry.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
                geometry.setCapability(GeometryArray.ALLOW_COUNT_READ);
            }

            if (geometry instanceof GeometryArray) {        // GeometryArray
                GeometryArray ga = (GeometryArray)geometry;
                for (int i = 0; i < ga.getVertexCount(); i++) {
                    float[] point = new float[3];                  // 頂点
                    ga.getCoordinate(i, point);               // 頂点座標取得
                    Point3f point3f = new Point3f(point);
                    t3dParent.transform(point3f);
                    point[0] = point3f.x;
                    point[1] = point3f.y;
                    point[2] = point3f.z;
                    _updateUpperLower(point);
               }
            }
        }
    }

    private void _updateUpperLower(float[] point) {
        if (init_) {
            for (int i = 0; i < 3; i++) {
                upper_[i] = point[i];
                lower_[i] = point[i];
            }
            init_ = false;
        } else {
            for (int i = 0; i < 3; i++) {
                if (point[i] > upper_[i]) upper_[i] = point[i];
                else if (point[i] < lower_[i]) lower_[i] = point[i];
            }
        }
    }

    /**
     * BoundingBoxを作成
     */

    private Point3f[] _makePoints() {
        Point3f[] points = new Point3f[24];
        points[0]  = new Point3f(upper_[0], upper_[1], upper_[2]); // A
        points[1]  = new Point3f(lower_[0], upper_[1], upper_[2]); // B
        points[2]  = new Point3f(lower_[0], lower_[1], upper_[2]); // C
        points[3]  = new Point3f(upper_[0], lower_[1], upper_[2]); // D
        points[4]  = new Point3f(upper_[0], lower_[1], lower_[2]); // H
        points[5]  = new Point3f(lower_[0], lower_[1], lower_[2]); // G
        points[6]  = new Point3f(lower_[0], upper_[1], lower_[2]); // F
        points[7]  = new Point3f(upper_[0], upper_[1], lower_[2]); // E
        points[8]  = new Point3f(upper_[0], upper_[1], upper_[2]); // A
        points[9]  = new Point3f(upper_[0], lower_[1], upper_[2]); // D
        points[10] = new Point3f(upper_[0], lower_[1], lower_[2]); // H
        points[11] = new Point3f(upper_[0], upper_[1], lower_[2]); // E
        points[12] = new Point3f(lower_[0], upper_[1], lower_[2]); // F
        points[13] = new Point3f(lower_[0], lower_[1], lower_[2]); // G
        points[14] = new Point3f(lower_[0], lower_[1], upper_[2]); // C
        points[15] = new Point3f(lower_[0], upper_[1], upper_[2]); // B
        points[16] = new Point3f(upper_[0], lower_[1], upper_[2]); // C
        points[17] = new Point3f(lower_[0], lower_[1], upper_[2]); // D
        points[18] = new Point3f(lower_[0], lower_[1], lower_[2]); // H
        points[19] = new Point3f(upper_[0], lower_[1], lower_[2]); // G
        points[20] = new Point3f(upper_[0], upper_[1], lower_[2]); // A
        points[21] = new Point3f(lower_[0], upper_[1], lower_[2]); // B
        points[22] = new Point3f(lower_[0], upper_[1], upper_[2]); // F
        points[23] = new Point3f(upper_[0], upper_[1], upper_[2]); // E
        return points;
    }

    private Vector3f[] _makeNormals() {
        Vector3f[] normal = new Vector3f[24];
        normal[0] = new Vector3f(0.0f, 0.0f, 1.0f);
        normal[1] = normal[0];
        normal[2] = normal[0];
        normal[3] = normal[0];
        normal[4] = new Vector3f(0.0f, 0.0f, -1.0f);
        normal[5] = normal[4];
        normal[6] = normal[4];
        normal[7] = normal[4];
        normal[8] = new Vector3f(1.0f, 0.0f, 0.0f);
        normal[9] = normal[8];
        normal[10] = normal[8];
        normal[11] = normal[8];
        normal[12] = new Vector3f(-1.0f, 0.0f, 0.0f);
        normal[13] = normal[12];
        normal[14] = normal[12];
        normal[15] = normal[12];
        normal[16] = new Vector3f(0.0f, -1.0f, 0.0f);
        normal[17] = normal[16];
        normal[18] = normal[16];
        normal[19] = normal[16];
        normal[20] = new Vector3f(0.0f, 1.0f, 0.0f);
        normal[21] = normal[20];
        normal[22] = normal[20];
        normal[23] = normal[20];
        return normal;
    }

    public Shape3D _makeBoundingBox(Color3f color) {
        Point3f[] points = _makePoints();
        QuadArray quads = new QuadArray(
            points.length,
            QuadArray.COLOR_3 | QuadArray.COORDINATES | QuadArray.NORMALS
        );
        quads.setCapability(QuadArray.ALLOW_COORDINATE_READ);
        quads.setCapability(QuadArray.ALLOW_COORDINATE_WRITE);
        quads.setCoordinates(0, points);
        Vector3f[] normals = _makeNormals();
        Color3f[] colors = new Color3f[points.length];
        for (int i = 0; i < points.length; i++) {
            colors[i] = color;
        }
        quads.setNormals(0, normals);
        quads.setColors(0, colors);
        PolygonAttributes attr = new PolygonAttributes();
        attr.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        attr.setCullFace(PolygonAttributes.CULL_NONE);
        Appearance appearance = new Appearance();
        appearance.setPolygonAttributes(attr);
        Shape3D shape = new Shape3D(quads, appearance);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        shape.setCapability(Node.ENABLE_PICK_REPORTING);
        PickTool.setCapabilities(shape, PickTool.INTERSECT_FULL);
        return shape;
    }

    /**
     * Z軸線を作成
     */
    public Shape3D _makeAxisLine(Vector3d jointAxis) {
        Point3f[] points = new Point3f[2];
        
        float[] offset = new float[3];
        
        offset[0] = (upper_[0] - lower_[0]) / 2.0f;
        offset[1] = (upper_[1] - lower_[1]) / 2.0f;
        offset[2] = (upper_[2] - lower_[2]) / 2.0f;
        
        points[0]  = new Point3f(
            (upper_[0] + offset[0]) * (float)jointAxis.x,
            (upper_[1] + offset[1]) * (float)jointAxis.y,
            (upper_[2] + offset[2]) * (float)jointAxis.z
        ); // A

        points[1]  = new Point3f(
            (lower_[0] - offset[0]) * (float)jointAxis.x,
            (lower_[1] - offset[1]) * (float)jointAxis.y,
            (lower_[2] - offset[2]) * (float)jointAxis.z
        ); // B

        LineArray lines = new LineArray(
            points.length,
            LineArray.COLOR_3 | LineArray.COORDINATES | LineArray.NORMALS
        );
        lines.setCoordinates(0, points);                // 座標
        Color3f[] colors = new Color3f[points.length];
        Vector3f[] normals = new Vector3f[points.length];
        Color3f color = new Color3f(0.0f, 0.0f, 1.0f);
        Vector3f normal = new Vector3f(0.0f, 0.0f, 1.0f);
        for (int i = 0; i < points.length; i++) {
            colors[i] = color;
            normals[i] = normal;
        }
        lines.setNormals(0, normals);  // 法線
        lines.setColors(0, colors);    // 色
        lines.setCapability(GeometryArray.ALLOW_COUNT_READ);
        lines.setCapability(GeometryArray.ALLOW_FORMAT_READ);
        lines.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
        LineAttributes lineAttr = new LineAttributes();
        lineAttr.setLineWidth(4.0f);
        Appearance appearance = new Appearance();
        appearance.setLineAttributes(lineAttr);
        Shape3D shape = new Shape3D(lines, appearance);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        //shape.setCapability(Node.ENABLE_PICK_REPORTING);
        return shape;
    }

    /**
     * Switch_Nodeを作成
     */
    public Switch _makeSwitchNode(Shape3D shape) {
        Switch switchNode = new Switch();
        switchNode.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_READ);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_WRITE);
        switchNode.setCapability(Switch.ALLOW_SWITCH_READ);
        switchNode.setCapability(Switch.ALLOW_SWITCH_WRITE);
        switchNode.addChild(shape);
        //switchNode.setUserData(USERDATA_SWITCH);
        //switchNode.setWhichChild(Switch.CHILD_ALL);
        return switchNode;
    }

    // ===== (Thu Apr 11 2002) ===>>
    // Geometryの複製
    private void _cloneGeometry(Node node) {
        // Groupの場合
        if (node instanceof Group) {
            Group group = (Group)node;
            if (group instanceof Primitive) {   // Primitiveである?
                _clonePrimitiveGeometry(node);  // PrimitiveのGeometry複製処理
            } else {
                for(int i = 0; i < group.numChildren(); i ++) { // すべての子ノードについて
                    _cloneGeometry(group.getChild(i));  // 再帰降下
                }
            }
        }
        // Leafの場合
        if (node instanceof Leaf) {
            if (node instanceof Link) { // Linkである?
                Link lk = (Link) node;
                SharedGroup sg = lk.getSharedGroup();   // Linkの先のSharedGroupを取得
                _cloneGeometry(sg); // 再帰降下
            }
        }
    }

    // PrimitiveのGeometryの複製
    private void _clonePrimitiveGeometry(Node node) {
        // Geometry複製処理
        if (node instanceof Shape3D) {  // Shape3Dなら
            Shape3D s3d = (Shape3D)node;
            for(int i = 0; i < s3d.numGeometries(); i ++) { // すべてのGeometryについて
                Geometry g0 = s3d.getGeometry();    // もともとのGeometryを取得
                Geometry g1 = (Geometry)g0.cloneNodeComponent(true);    // Geometryを複製
                s3d.setGeometry(g1,i);  // 複製したGeometryに差し替え
            }
        }
        // ツリー再帰降下処理
        if (node instanceof Group) {    // Groupなら
            Group group = (Group)node;
            for(int i = 0; i < group.numChildren(); i ++) { // すべての子ノードについて
                _clonePrimitiveGeometry(group.getChild(i));  // Geometryの複製
            }
        }
    }
    // <<=== (Thu Apr 11 2002) =====

    //--------------------------------------------------------------------
    // BoundingBoxCreator
    /*
    class BoundingBoxCreator implements TraverseOperation {
        public void operation(
            Node node,
            Node parent
        ) {
    		if (node instanceof JointNode) {
                JointNode jointNode = (JointNode)node;
                TransformGroup tgJoint = jointNode.getTransformGroupRoot();
                _setCapabilities(tgJoint);
            } else if (node instanceof SegmentNode) {
                if (!(parent instanceof JointNode)) return;
                SegmentNode segmentNode = (SegmentNode)node;
                JointNode jointNode = (JointNode)parent;
                Group scene = (Group)segmentNode.getRootNode();
                scene.setCapability(Group.ALLOW_CHILDREN_EXTEND);
                scene.setCapability(Group.ALLOW_CHILDREN_READ);
                // SceneGraphを辿りShape3Dを取得
                init_ = true;            // upper_,lower_を初期化する
                Transform3D tr = new Transform3D();
                tr.setIdentity();
                _calcUpperLower(scene, tr);  // upper_,lower_を計算する

                // バウンディングボックスの作成
                Color3f color = new Color3f(1.0f, 0.0f, 0.0f);
                Switch bbSwitch = _makeSwitchNode(_makeBoundingBox(color));
                scene.addChild(bbSwitch);

                // スイッチノードの参照をTGのユーザーデータ領域のストア
                jointNode.setUserData("boundingBoxSwitch", bbSwitch);

                // 軸の作成
                Vector3d jointAxis = jointNode.getJointAxis();
                if (jointAxis != null) {
                    Switch axisSwitch =
                        _makeSwitchNode(_makeAxisLine(jointAxis));
                    scene.addChild(axisSwitch);
                 
                    jointNode.setUserData("axisLineSwitch", axisSwitch);
                    jointNode.setUserData("jointAxis",jointNode.getJointAxis());
                }
            }
        }
    }
        */
}

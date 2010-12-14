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

import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.sun.j3d.utils.picking.PickTool;

public class SceneGraphModifier {
    //--------------------------------------------------------------------
    // 定数
    public static final int CREATE_BOUNDS = 0;
    public static final int RESIZE_BOUNDS = 1;
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

    /**
     * @brief get hash table assigned to TransformGroup as user data
     * @param tg TransformGroup
     * @return hash table assigned to TransformGroup
     */
    public static Hashtable<String, Object> getHashtableFromTG(TransformGroup tg) {
        if (tg == null) 
			return null; 

        Object userData = tg.getUserData();
        if (userData instanceof Hashtable) 
            return (Hashtable<String, Object>)userData;

        return null;
    }

    /**
     * @brief get GrxLinkItem associated with TransformGroup
     * @param tg TransformGroup
     * @return GrxLinkItem
     */
    public static GrxLinkItem getLinkFromTG(TransformGroup tg){
    	Hashtable<String, Object> htable = getHashtableFromTG(tg);
    	if (htable != null){
    		GrxLinkItem link = (GrxLinkItem)htable.get("linkInfo");
    		return link;
    	}
    	return null;
    }

    /**
     * @brief get GrxModelItem associated with TransformGroup
     * @param tg TransformGroup
     * @return GrxModelItem
     */
    public static GrxModelItem getModelFromTG(TransformGroup tg){
    	Hashtable<String, Object> htable = getHashtableFromTG(tg);
    	if (htable != null){
    		GrxModelItem model = (GrxModelItem)htable.get("object");
    		return model;
    	}
    	return null;
    }
    //--------------------------------------------------------------------
    // 公開メソッド
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
    private void _setSegmentCapabilities(TransformGroup tg) {
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg.setCapability(TransformGroup.ALLOW_LOCAL_TO_VWORLD_READ);
    }

    /**
     * @brief 
     * @param node
     * @param t3dParent
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

    public Point3f[] _makePoints() {
    	return _makePoints(upper_, lower_);
    }

    public static Point3f[] _makePoints(float x, float y, float z){
    	float[] upper = new float[]{x/2,y/2,z/2};
    	float[] lower = new float[]{-x/2,-y/2,-z/2};
    	return _makePoints(upper, lower);
    }
    
    /**
     * @brief make array of vertices of cube
     * @param upper coordinates of upper corner of this cube
     * @param lower coordinates of lower corner of this cube
     * @return array of vertices(24)
     */
    public static Point3f[] _makePoints(float[] upper, float[] lower) {
        Point3f[] points = new Point3f[24];
        points[0]  = new Point3f(upper[0], upper[1], upper[2]); // A
        points[1]  = new Point3f(lower[0], upper[1], upper[2]); // B
        points[2]  = new Point3f(lower[0], lower[1], upper[2]); // C
        points[3]  = new Point3f(upper[0], lower[1], upper[2]); // D
        points[4]  = new Point3f(upper[0], lower[1], lower[2]); // H
        points[5]  = new Point3f(lower[0], lower[1], lower[2]); // G
        points[6]  = new Point3f(lower[0], upper[1], lower[2]); // F
        points[7]  = new Point3f(upper[0], upper[1], lower[2]); // E
        points[8]  = new Point3f(upper[0], upper[1], upper[2]); // A
        points[9]  = new Point3f(upper[0], lower[1], upper[2]); // D
        points[10] = new Point3f(upper[0], lower[1], lower[2]); // H
        points[11] = new Point3f(upper[0], upper[1], lower[2]); // E
        points[12] = new Point3f(lower[0], upper[1], lower[2]); // F
        points[13] = new Point3f(lower[0], lower[1], lower[2]); // G
        points[14] = new Point3f(lower[0], lower[1], upper[2]); // C
        points[15] = new Point3f(lower[0], upper[1], upper[2]); // B
        points[16] = new Point3f(upper[0], lower[1], upper[2]); // C
        points[17] = new Point3f(lower[0], lower[1], upper[2]); // D
        points[18] = new Point3f(lower[0], lower[1], lower[2]); // H
        points[19] = new Point3f(upper[0], lower[1], lower[2]); // G
        points[20] = new Point3f(upper[0], upper[1], lower[2]); // A
        points[21] = new Point3f(lower[0], upper[1], lower[2]); // B
        points[22] = new Point3f(lower[0], upper[1], upper[2]); // F
        points[23] = new Point3f(upper[0], upper[1], upper[2]); // E
        return points;
    }

    private static Vector3f[] _makeNormals() {
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

    public static Shape3D _makeCube(Color3f color, Point3f[] points){
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

    public Shape3D _makeBoundingBox(Color3f color) {
        Point3f[] points = _makePoints();
        return _makeCube(color, points);
    }

    /**
     * @brief make both end points of axis line
     * @param jointAxis joint axis
     * @return array of points(length=2)
     */
    public Point3f[] makeAxisPoints(Vector3d jointAxis){
        Point3f[] points = new Point3f[2];
        
        float upperlen = (float)(
        		upper_[0]*jointAxis.x+
        		upper_[1]*jointAxis.y+
        		upper_[2]*jointAxis.z);
        float lowerlen = (float)(
        		lower_[0]*jointAxis.x+
        		lower_[1]*jointAxis.y+
        		lower_[2]*jointAxis.z);
        float offset = 0.05f;
        
        points[0]  = new Point3f(
            (upperlen + offset) * (float)jointAxis.x,
            (upperlen + offset) * (float)jointAxis.y,
            (upperlen + offset) * (float)jointAxis.z
        ); // A

        points[1]  = new Point3f(
            (lowerlen - offset) * (float)jointAxis.x,
            (lowerlen - offset) * (float)jointAxis.y,
            (lowerlen - offset) * (float)jointAxis.z
        ); // B
        return points;
    }
    /**
     * Z軸線を作成
     */
    public Shape3D _makeAxisLine(Vector3d jointAxis) {
        Point3f[] points = makeAxisPoints(jointAxis);
        
        LineArray lines = new LineArray(
            points.length,
            LineArray.COLOR_3 | LineArray.COORDINATES | LineArray.NORMALS
        );
        lines.setCapability(LineArray.ALLOW_COORDINATE_WRITE);
        lines.setCoordinates(0, points);                // 座標
        Color3f[] colors = new Color3f[points.length];
        Vector3f[] normals = new Vector3f[points.length];
        Color3f color = new Color3f(1.0f, 0.0f, 1.0f);
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
    public static Switch _makeSwitchNode(Shape3D shape) {
        Switch switchNode = _makeSwitchNode();
        switchNode.addChild(shape);
        return switchNode;
    }

    public static Switch _makeSwitchNode() {
        Switch switchNode = new Switch();
        switchNode.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_READ);
        switchNode.setCapability(Switch.ALLOW_CHILDREN_WRITE);
        switchNode.setCapability(Switch.ALLOW_SWITCH_READ);
        switchNode.setCapability(Switch.ALLOW_SWITCH_WRITE);
        //switchNode.setUserData(USERDATA_SWITCH);
        //switchNode.setWhichChild(Switch.CHILD_ALL);
        return switchNode;
    }
}

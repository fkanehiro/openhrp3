/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */

package com.generalrobotix.ui.util;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import com.sun.j3d.utils.geometry.Sphere;

/**
 * @brief functions to make various basic shapes
 */
public class GrxShapeUtil{

	/**
	 * @brief create X(red),Y(green) and Z(blue) axes
	 * @return
	 */
	public static BranchGroup createAxes(){
        Shape3D shape = new Shape3D();
        shape.setPickable(false);
        shape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        try {
        	Appearance app = new Appearance();
        	LineAttributes latt = new LineAttributes();
        	latt.setLineWidth(2.0f);
        	app.setLineAttributes(latt);
        	shape.setAppearance(app);
        	
        	Point3d o = new Point3d(0.0, 0.0, 0.0);
        	Point3d x = new Point3d(0.5, 0.0, 0.0);
        	Point3d y = new Point3d(0.0, 0.5, 0.0);
        	Point3d z = new Point3d(0.0, 0.0, 0.5);
            Point3d[] p3d = {o,x,o,y,o,z};
            LineArray la = new LineArray(p3d.length, LineArray.COLOR_3
                    | LineArray.COORDINATES | LineArray.NORMALS);
            la.setCoordinates(0, p3d);
            Color3f r = new Color3f(1.0f, 0.0f, 0.0f);
            Color3f g = new Color3f(0.0f, 1.0f, 0.0f);
            Color3f b = new Color3f(0.0f, 0.0f, 1.0f);
            Color3f[]  c3f = {r,r,g,g,b,b};
            la.setColors(0, c3f);
            shape.addGeometry(la);
        }catch(Exception ex){
        	ex.printStackTrace();
        }
        TransformGroup tg = new TransformGroup();
        tg.addChild(shape);
        BranchGroup bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_DETACH);
        bg.addChild(tg);
        return bg;
    }


	/**
	 * @brief create ball with switch node
	 * @param radius radius of the ball
	 * @param c color of the ball
	 * @return switch node
	 */
	public static Switch createBall(double radius, Color3f c) {
        Material m = new Material();
        m.setDiffuseColor(c);
        m.setSpecularColor(0.01f, 0.10f, 0.02f);
        m.setLightingEnable(true);
        Appearance app = new Appearance();
        app.setMaterial(m);
        Node sphere = new Sphere((float)radius, Sphere.GENERATE_NORMALS, app);
        sphere.setPickable(false);

        TransformGroup tg = new TransformGroup();
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
}
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.view.vsensor;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;

import javax.swing.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.universe.SimpleUniverse;

import jp.go.aist.hrp.simulator.CameraPOA;
import jp.go.aist.hrp.simulator.ImageData;
import jp.go.aist.hrp.simulator.PixelFormat;
import jp.go.aist.hrp.simulator.CameraPackage.*;

/**
 * Camera
 * @author	Ichitaro Kohara, MSTC
 * @version	1.0(2001.02.22)
 */
public class Camera_impl extends CameraPOA {

	// camera parameter
	private CameraParameter param_;

	private ImageData image_;
	private Raster	raster_;
	
	// screen size
	private int	width_;
	private int	height_;

	// branch-graph of the viewpoint
	private BranchGroup			bgVp_;
	private TransformGroup		tgVp_;
	private Canvas3DI			canvas_;
	private ViewPlatform		vplatform_;
	private View				view_;
	private PhysicalBody		pbody_;
	private PhysicalEnvironment	penv_;
	
	private JFrame	frm_;
	public boolean rendered_ = false;

	// ---------- Constructor ----------

	/**
	 * Constructor
	 * @param	param		camera parameter
	 * @param	offScreen	off-screnn flag
	 */
	public Camera_impl(CameraParameter param, boolean offScreen) {
		param_ = param;
		//
		// raster
		//

		// save screen size
		width_ = param.width;
		height_ = param.height;

		image_ = new ImageData();

		image_.width = param_.width;
		image_.height = param_.height;
		
		image_.octetData = new byte[1];
		image_.longData = new int[1];
		image_.floatData = new float[1];
		
		// camera type
		CameraType cameraType = param.type;

		if (cameraType == CameraType.MONO)
		    image_.format = PixelFormat.GRAY;
		else
		    image_.format = PixelFormat.ARGB;

		// create color infomation for reading color buffer
		// type int, (Alpha:8bit,) R:8bit, G:8bit, B:8bit
		BufferedImage bimageRead = null;
		ImageComponent2D readImage = null;
		if (cameraType == CameraType.COLOR || cameraType == CameraType.COLOR_DEPTH ||
				cameraType == CameraType.MONO || cameraType == CameraType.MONO_DEPTH) {
			bimageRead = new BufferedImage(width_, height_, BufferedImage.TYPE_INT_RGB);
			readImage = new ImageComponent2D(ImageComponent.FORMAT_RGB, bimageRead);
		}

		// create depth information for reading depth buffer
		DepthComponentFloat readDepthFloat = null;
		if (cameraType == CameraType.DEPTH 
				|| cameraType == CameraType.COLOR_DEPTH
				|| cameraType == CameraType.MONO_DEPTH) {
			readDepthFloat = new DepthComponentFloat(width_, height_);
		}

		// create raster
		int rasterType = -1;
		if (cameraType != CameraType.NONE) {
			if (cameraType == CameraType.COLOR 
					|| cameraType == CameraType.MONO) 
				rasterType = Raster.RASTER_COLOR;
			
			else if (cameraType == CameraType.DEPTH)
				rasterType = Raster.RASTER_DEPTH;
			
			else 
				rasterType = Raster.RASTER_COLOR_DEPTH;

			raster_ = new Raster(
				new Point3f(), rasterType,
				0, 0, width_, height_,
				readImage, readDepthFloat
			);
		}

		//
		// create branch-graph elements
		//

		bgVp_ = new BranchGroup();
		bgVp_.setCapability(BranchGroup.ALLOW_DETACH);
		bgVp_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		bgVp_.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		bgVp_.setUserData(param_.defName);

		tgVp_ = new TransformGroup();
		tgVp_.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		tgVp_.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		vplatform_ = new ViewPlatform();

		view_ = new View();
		view_.setFrontClipPolicy(View.VIRTUAL_EYE);
		view_.setBackClipPolicy(View.VIRTUAL_EYE);
		view_.setFrontClipDistance(param.frontClipDistance);
		view_.setBackClipDistance(param.backClipDistance);
		view_.setFieldOfView(param.fieldOfView);

		pbody_ = new PhysicalBody();

		penv_ = new PhysicalEnvironment();

		if (raster_ == null) {
			canvas_ = new Canvas3DI(SimpleUniverse.getPreferredConfiguration());
			canvas_.setSize(width_, height_);
			
		} else {
			if (offScreen) {
				canvas_ = new OffScreenCanvas3D(
					SimpleUniverse.getPreferredConfiguration(),
					raster_,
					width_,
					height_,
					rasterType,
					true
				);
			} else {
				canvas_ = new OnScreenCanvas3D(
					SimpleUniverse.getPreferredConfiguration(),
					raster_,
					width_,
					height_,
					rasterType,
					false
				);
			}
		}

		//
		// construct branch-graph
		//SampleRobotPD.xml

		bgVp_.addChild(tgVp_);
		tgVp_.addChild(vplatform_);
		view_.addCanvas3D(canvas_);
		view_.attachViewPlatform(vplatform_);
		view_.setPhysicalBody(pbody_);
		view_.setPhysicalEnvironment(penv_);

		//bgVp_.compile();

		if (offScreen)	
			return;

		canvas_.stopRenderer();

		//
		// Swing
		//
		
		frm_ = new JFrame(param.sensorName);
		frm_.setSize(width_ + 20, height_ + 30);
		frm_.getContentPane().setLayout(new FlowLayout());
		//frm_.getContentPane().setLayout(new BorderLayout());
		frm_.getContentPane().add(canvas_);
		frm_.pack();
		frm_.setResizable(false);
		frm_.setAlwaysOnTop(true);
		//frm_.setVisible(true);
	}
	
	public void setVisible(boolean b) {
		if (canvas_ instanceof OffScreenCanvas3D)
			((OffScreenCanvas3D)canvas_).cBrowser_.setVisible(b);
		else
			frm_.setVisible(b);
	}

	// ------------ Camera interfece implementation ------------

	/**
	 * Destroy
	 */
	public void destroy() {
		System.out.println("Camera_impl::destroy()");
	    //これらのif(...!=null)は
	    //二重destroy時のエラーを防ぐため
		if(canvas_!=null)
    		canvas_.getView().removeCanvas3D(canvas_);
		
        if(frm_!=null) {
        	frm_.setVisible(false);
			frm_.dispose();
			frm_ = null;
		}

		raster_ = null;

        if(bgVp_!=null) {
        	bgVp_.detach();
			bgVp_ = null;
		}
        
		tgVp_ = null;
		canvas_ = null;
		vplatform_ = null;
		view_ = null;
		pbody_ = null;
		penv_ = null;
	}

	/**
	 * Get camera parameter
	 * @return	camera parameter
	 */
	public CameraParameter getCameraParameter() {
		return param_;
	}
	public void updateView() {
	    if (rendered_) 
	    	return;
	    if (canvas_.isOffScreen()) {
	    	canvas_.renderOffScreenBuffer();
			canvas_.waitForOffScreenRendering();
			
	    } else {
	        if (!frm_.isVisible())
                frm_.setVisible(true);
	    	int cnt = 0;
			canvas_.finished_ = false;
			canvas_.startRenderer();
			try {
				while (canvas_.finished_ == false) {
					Thread.sleep(5);
					if (cnt++ > 20)
						break;
				}
			} catch (InterruptedException ex) { }
			canvas_.stopRenderer();
	    }
	    rendered_ = true;
	}
	/**
	 * Get color buffer
	 * @return	color buffer
	 */
	public int[] getColorBuffer() {
//	  	visionsensor_.updateTransform();
		updateView();
		return canvas_.getColorBuffer();
	}

	/**
	 * Get depgh buffer
	 * @return	depth buffer
	 */
	public float[] getDepthBuffer() {
//	  	visionsensor_.updateTransform();
		updateView();
		return canvas_.getDepthBuffer();
	}

	public ImageData getImageData() {
//	  visionsensor_.updateTransform();

	  updateView();
	  if (param_.type == CameraType.COLOR ||
			  param_.type == CameraType.COLOR_DEPTH){
		  image_.longData = canvas_.getColorBuffer();
	  }
	  
	  if (param_.type == CameraType.MONO ||
			  param_.type == CameraType.MONO_DEPTH){
		  image_.octetData = canvas_.getMonoBuffer();
	  }
	  
	  if (param_.type == CameraType.DEPTH ||
			  param_.type == CameraType.COLOR_DEPTH){
		  image_.floatData = canvas_.getDepthBuffer();
	  }
	  return image_;
	}

	/**
	 * Get the branch-graph of Camera
	 * @return	root BranchGroup of this camera
	 */
	public BranchGroup getBranchGraph() {
		return bgVp_;
	}

	public TransformGroup getTransformGroup() {
		return tgVp_;
	}
}

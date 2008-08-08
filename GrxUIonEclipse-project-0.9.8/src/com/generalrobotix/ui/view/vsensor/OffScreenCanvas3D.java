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

// Java2 SDK
import java.awt.GraphicsConfiguration;

// Java3D
import javax.media.j3d.*;

/**
 * Off screen canvas for OpenHRP
 * NOTE: This class has not been implemented yet.
 * @author	Ichitaro Kohara, MSTC
 * @version	1.0(2001.02.05)
 */
public final class OffScreenCanvas3D
extends Canvas3DI
{
	// debug flag
	private boolean debug_;

	// for debug
	ColorBufferBrowser cBrowser_;

	/**
	 * Constructor
	 *
	 */
	public OffScreenCanvas3D(
		GraphicsConfiguration	graphicsConfiguration,
		Raster					raster,
		int						width,
		int						height,
		int						rasterType,
		boolean			debug
	)
	{
		super(graphicsConfiguration, true);
		//System.out.println("OffScreenCanvas3D::OffScreenCanvas3D()");

		raster_ = raster;
		width_ = width;
		height_ = height;
		rasterType_ = rasterType;
		gc_ = getGraphicsContext3D();
		debug_ = debug;

		if (rasterType_ == Raster.RASTER_DEPTH
			|| rasterType_ == Raster.RASTER_COLOR_DEPTH) {
			depthBuffer_ = new float[width_ * height_];
		}

		if (rasterType_ == Raster.RASTER_COLOR
		    || rasterType_ == Raster.RASTER_COLOR_DEPTH){
		    setOffScreenBuffer(raster_.getImage());
		} else {
		    setOffScreenBuffer(new ImageComponent2D(ImageComponent.FORMAT_RGB,
							    width, height));
		}
		setSize(width, height);
		
		Screen3D s3d = getScreen3D();
		s3d.setSize(width, height);
		s3d.setPhysicalScreenWidth(width);
		s3d.setPhysicalScreenHeight(height);
		if (debug_){
		    // for debug
		    cBrowser_ = new ColorBufferBrowser(width, height,
			 	"color buffer(off-screen)");
		    cBrowser_.setAlwaysOnTop(true);
		    //cBrowser_.setVisible(true);
		}
	}

	/**
	 * This method is called when buffer has swapped
	 */
	public void postSwap()
	{
	    super.postSwap();

	    switch (rasterType_){
	    case Raster.RASTER_COLOR:
			color_ = raster_.getImage().getImage();
			colorBuffer_ = color_.getRGB(0,0,width_, height_, null, 0, width_);
			break;

	    case Raster.RASTER_DEPTH:
			depth_ = (DepthComponentFloat)raster_.getDepthComponent();
			depth_.getDepthData(depthBuffer_);
			break;

	    case Raster.RASTER_COLOR_DEPTH:
			break;
	    }
	    if (debug_) 
			cBrowser_.setColorBuffer(colorBuffer_);
	}
}

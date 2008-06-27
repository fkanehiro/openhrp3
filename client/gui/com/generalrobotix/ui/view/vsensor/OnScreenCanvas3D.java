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
import javax.media.j3d.DepthComponentFloat;
import javax.media.j3d.Raster;

/**
 * Canvas class for rendering
 * @author	Ichitaro Kohara, MSTC
 * @version	1.0(2001.02.22)
 */
public final class OnScreenCanvas3D extends Canvas3DI {
	// for debug
	//private ColorBufferBrowser cBrowser_;

	/**
	 * Constructor
	 * @param	graphicsConfiguration
	 * @param	raster
	 * @param	raster width
	 * @param	raster height
	 * @param	raster type
	 */
	public OnScreenCanvas3D(
		GraphicsConfiguration	graphicsConfiguration,
		Raster					raster,
		int						width,
		int						height,
		int						rasterType,
		boolean					debug
	)
	{
		super(graphicsConfiguration);
		//System.out.println("OnScreenCanvas3D::OnScreenCanvas3D()");

		raster_ = raster;
		width_ = width;
		height_ = height;
		rasterType_ = rasterType;
		gc_ = getGraphicsContext3D();

		if (rasterType_ == Raster.RASTER_DEPTH
			|| rasterType_ == Raster.RASTER_COLOR_DEPTH)
		{
			depthBuffer_ = new float[width_ * height_];
		}

		setSize(width, height);

		/*if (debug_) {
			if (rasterType_ == Raster.RASTER_COLOR
				|| rasterType_ == Raster.RASTER_COLOR_DEPTH)
			{
				cBrowser_ = new ColorBufferBrowser (
					width, height,
					"color buffer(on-screen)"
				);
				cBrowser_.setVisible(true);
			}
		}*/
	}

	/**
	 * update
	 * @param	g
	 */
	public void update(java.awt.Graphics g) {
		paint(g);
	}

	/**
	 * This method is called when buffer has swapped
	 */
	public void postSwap() {
		super.postSwap();
		_readRaster();
		//if (debug_)
			//cBrowser_.setColorBuffer(colorBuffer_);
	}

	/**
	 * Read raster
	 */
	private /*synchronized*/ void _readRaster() {
		if (finished_)
			return;

		gc_.readRaster(raster_);
		switch (rasterType_) {
			case Raster.RASTER_COLOR: // read color buffer
				color_ = raster_.getImage().getImage();
				colorBuffer_ = color_.getRGB(0, 0, width_, height_, null, 0, width_);
				break;

			case Raster.RASTER_DEPTH: // read depth buffer
				depth_ = (DepthComponentFloat)raster_.getDepthComponent();
				depth_.getDepthData(depthBuffer_);
				break;

			case Raster.RASTER_COLOR_DEPTH:
				// read color buffer
				color_ = raster_.getImage().getImage();
				colorBuffer_ = color_.getRGB(0, 0, width_, height_, null, 0, width_);
				// read depth buffer
				depth_ = (DepthComponentFloat)raster_.getDepthComponent();
				depth_.getDepthData(depthBuffer_);
				break;
		}
		finished_ = true;
	}
}

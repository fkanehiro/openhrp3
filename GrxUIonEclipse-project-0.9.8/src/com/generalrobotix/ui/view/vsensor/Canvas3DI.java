// -*- indent-tabs-mode: nil; tab-width: 4; -*-
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

import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;

import javax.media.j3d.DepthComponentFloat;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.Raster;

/**
 * This class defines Canvas which is used in OpenHRP.
 * @author	Ichitaro Kohara, MSTC
 * @version	1.0(2001.02.22)
 */
@SuppressWarnings("serial")
public class Canvas3DI extends javax.media.j3d.Canvas3D {
	// raster object
	protected Raster raster_;
	protected int width_;
	protected int height_;
	protected int rasterType_;
	protected GraphicsContext3D	gc_;

	// color buffer, depth buffer
	int[]	colorBuffer_;
	float[]	depthBuffer_;
	boolean finished_ = false;

	// working space
	protected BufferedImage			color_;
	protected DepthComponentFloat	depth_;

	/**
	 * Constructor
	 * @param	gc
	 */
	public Canvas3DI(GraphicsConfiguration gc) {
		super(gc);
	}

	/**
	 * Constructor
	 * @param	gc
	 * @param	offscr
	 */
	public Canvas3DI(GraphicsConfiguration gc, boolean offscr,
					 Raster raster, int width, int height, int rasterType) {
		super(gc, offscr);
		gc_ = getGraphicsContext3D();
		raster_ = raster;
		width_ = width;
		height_ = height;
		rasterType_ = rasterType;

		if (rasterType_ == Raster.RASTER_DEPTH
			|| rasterType_ == Raster.RASTER_COLOR_DEPTH) {
			depthBuffer_ = new float[width_ * height_];
		}
		if (rasterType_ == Raster.RASTER_COLOR
			|| rasterType_ == Raster.RASTER_COLOR_DEPTH) {
		    colorBuffer_ = new int[width_ * height_]; 
        }
		setSize(width, height);
	}

	/**
	 * Returns color buffer
	 * @return	color buffer
	 */
	public int[] getColorBuffer() {
		return colorBuffer_;
	}

	public byte[] getMonoBuffer() {
		byte[] monoBuffer = new byte[width_*height_];
	  	for (int i=0; i<width_; i++) {
	  		for (int j=0; j<height_; j++) {
	  			monoBuffer[i+j*width_] = 
	  				(byte)(0.587*(0xff&(colorBuffer_[i+j*width_]>>8))
	  				+0.114*(0xff&(colorBuffer_[i+j*width_]))
	  				+0.299*(0xff&(colorBuffer_[i+j*width_]>>16)));
	  		}
	  	}
	  	return monoBuffer;
	}

	/**
	 * Returns depth buffer
	 * @return	depth buffer
	 */
	public float[] getDepthBuffer() {
		return depthBuffer_;
	}
	/**
	 * This method is called when buffer has swapped
	 */
	public void postSwap() {
		super.postSwap();
		_readRaster();
	}

	/**
	 * Read raster
	 */
	private void _readRaster() {
		if (finished_)
			return;

		gc_.readRaster(raster_);
		
		// read color buffer
		if (rasterType_ == Raster.RASTER_COLOR ||
				rasterType_ == Raster.RASTER_COLOR_DEPTH){
			color_ = raster_.getImage().getImage();
			color_.getRGB(0, 0, width_, height_, colorBuffer_, 0, width_);
            if (colorBuffer_.length != width_*height_){
                System.out.println("invalid length of color buffer = "+colorBuffer_.length);
            }
		}

		// read depth buffer
		if (rasterType_ == Raster.RASTER_DEPTH ||
				rasterType_ == Raster.RASTER_COLOR_DEPTH){
			depth_ = (DepthComponentFloat)raster_.getDepthComponent();
			depth_.getDepthData(depthBuffer_);
            if (depthBuffer_.length != width_*height_){
                System.out.println("invalid length of depth buffer = "+depthBuffer_.length);
            }
		}
		finished_ = true;
	}

	public void renderOnce(){
        finished_ = false;
    }
} 

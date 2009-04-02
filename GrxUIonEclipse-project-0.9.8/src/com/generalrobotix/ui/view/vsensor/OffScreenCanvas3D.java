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
@SuppressWarnings("serial")
public final class OffScreenCanvas3D extends Canvas3DI
{
	/**
	 * @brief Constructor
	 * @param graphicsConfiguration
	 * @param raster
	 * @param width
	 * @param height
	 * @param rasterType
	 */
	public OffScreenCanvas3D(
		GraphicsConfiguration	graphicsConfiguration,
		Raster					raster,
		int						width,
		int						height,
		int						rasterType)
	{
		super(graphicsConfiguration, true, raster, width, height, rasterType);
		//System.out.println("OffScreenCanvas3D::OffScreenCanvas3D()");

		if (rasterType_ == Raster.RASTER_COLOR
		    || rasterType_ == Raster.RASTER_COLOR_DEPTH){
		    setOffScreenBuffer(raster_.getImage());
		} else {
		    setOffScreenBuffer(new ImageComponent2D(ImageComponent.FORMAT_RGB,
							    width, height));
		}
		
		Screen3D s3d = getScreen3D();
		s3d.setSize(width, height);
		s3d.setPhysicalScreenWidth(width);
		s3d.setPhysicalScreenHeight(height);
	}

    public void renderOnce(){
        super.renderOnce();
        renderOffScreenBuffer();
        waitForOffScreenRendering();
    }
}

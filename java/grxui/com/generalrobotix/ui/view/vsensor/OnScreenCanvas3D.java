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
import javax.media.j3d.Raster;

/**
 * Canvas class for rendering
 * @author	Ichitaro Kohara, MSTC
 * @version	1.0(2001.02.22)
 */
@SuppressWarnings("serial")
public final class OnScreenCanvas3D extends Canvas3DI {
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
		int						rasterType)
	{
		super(graphicsConfiguration, false, raster, width, height, rasterType);
		//System.out.println("OnScreenCanvas3D::OnScreenCanvas3D()");
		stopRenderer();
	}

    public void renderOnce(){
        super.renderOnce();
        int cnt = 0;
        startRenderer();
        try {
            while (finished_ == false) {
                Thread.sleep(5);
                if (cnt++ > 20)
                    break;
            }
        } catch (InterruptedException ex) { }
        stopRenderer();
    }
}

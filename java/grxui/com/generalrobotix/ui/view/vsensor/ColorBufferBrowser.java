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
import java.awt.Canvas;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;

// Swing
import javax.swing.JFrame;

// Sun
import com.generalrobotix.ui.view.Grx3DView;
import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * Color buffer browser
 * @author	Ichitaro Kohara, MSTC
 * @version	1.0(2001.02.22)
 */
public final class ColorBufferBrowser extends JFrame {
	// canvas for drawing image
	private CanvasDraw canvas_;

	// working space
	private boolean updated_ = false;

	/**
	 * Constructor
	 * @param	width	raster width
	 * @param	height	raster height
	 * @param	title	frame title
	 */
	public ColorBufferBrowser( int width, int height, String title ) {
		super(title);

		canvas_ = new CanvasDraw (
				Grx3DView.graphicsConfiguration,
			width, height
		);
		canvas_.setSize(width, height);
		canvas_.setBackground(java.awt.Color.white);

		getContentPane().setLayout(new FlowLayout());
		getContentPane().add(canvas_);
		pack();
		setResizable(false);
		//setVisible(true);
	}

	/**
	 * Set color buffer
	 * Canvas is redrawn at this timing.
	 * @param	colorBuffer	color buffer
	 */
	public void setColorBuffer(int[] colorBuffer) {
		canvas_.setColorBuffer(colorBuffer);
		updated_ = true;
		canvas_.repaint();
	}

	/**
	 * Inner class
	 * @author	Ichitaro Kohara, MSTC
	 * @date	2001.02.02
	 * @version	1.0
	 */
	final class CanvasDraw extends Canvas {
		// raster size
		private int width_;

		// image source
		private MemoryImageSource	mis_;

		// color model
		private ColorModel cm_ = ColorModel.getRGBdefault();

		// color buffer
		private int[]	colorBuffer_;

		// image
		private Image img_;

		/**
		 * Constructor
		 * @param	gconfig		Graphics configuration
		 * @param	width		raster width
		 * @param	height		raster height
		 */
		public CanvasDraw(java.awt.GraphicsConfiguration gconfig, int width, int height) {
			super(gconfig);

			// raster size
			width_ = width;

			// create image source
			mis_ = new MemoryImageSource (
				width,
				height,
				null,	// color buffer
				0,
				width
			);
		}

		/**
		 * Set color buffer
		 * @param	colorBuffer		color buffer
		 */
		public void setColorBuffer(int[] colorBuffer) {
			colorBuffer_ = colorBuffer;
			mis_.newPixels(colorBuffer, cm_, 0, width_);
		}

		/**
		 * Update image
		 * @param	g	Graphcis
		 */
		public void update(Graphics g) {
			if (updated_)
				paint(g);
			else
				return;
		}

		/**
		 * Paint image
		 * @param	g	Graphcis object
		 */
		public void paint(Graphics g) {
			if (!updated_)
				return;
			updated_ = false;

			if (colorBuffer_ == null)
				return;

			// image
			img_ = createImage(mis_);
			g.drawImage(img_, 0, 0, this);
		}
	}
}

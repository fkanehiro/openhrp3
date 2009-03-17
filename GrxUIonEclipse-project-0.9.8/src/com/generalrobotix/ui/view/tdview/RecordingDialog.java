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
 * RecordingDialog.java
 *
 * @author	Kernel, Inc.
 * @version  1.0 (Sun Dec 09 2001)
 */

package com.generalrobotix.ui.view.tdview;

import java.awt.*;
import javax.swing.*;

import com.generalrobotix.ui.util.FileInput;
import com.generalrobotix.ui.util.ModalDialog;

@SuppressWarnings("serial")
public class RecordingDialog extends ModalDialog {

//	 --------------------------------------------------------------------
    // Instance variables
	private static final Dimension[] imageSize_ =
         new Dimension[] {
             new Dimension(320, 240),
             new Dimension(640, 480),
             new Dimension(800, 600),
         };
	private static final double[] playbackRate_ =
		 new double[] { 4, 2, 1, 0.5, 0.25 };
	private static final String[] playbackRateString_ =
		 new String[] { "x 4", "x 2", "x 1", "x 1/2", "x 1/4" };
	 
    private FileInput fileInput_;
    private JComboBox imSizeCombo_;
    private JComboBox playbackRateCombo_;
    private JTextField frameRateField_;
    public final static String EXTEND_ = "mov";
    public RecordingDialog(Frame owner) {
        super(
            owner,
            "Recording Dialog",
            "Input recording parameters.",
            OK_CANCEL_TYPE
        );

		fileInput_ = new FileInput(new String[] { "mov" }, System.getProperty("user.dir"));
		imSizeCombo_ = new JComboBox(_makeSizeStrings(imageSize_));
        playbackRateCombo_ = new JComboBox(playbackRateString_);
        playbackRateCombo_.setSelectedIndex(2);
        frameRateField_ = new JTextField("10");
        
        addInputComponent("File name", fileInput_, MULTILINE_CAPTION, true);
        addInputComponent("Image size", imSizeCombo_, MULTILINE_CAPTION, true);
        addInputComponent("playrate", playbackRateCombo_, MULTILINE_CAPTION, true);
        addInputComponent("framerate (frame/sec)", frameRateField_, MULTILINE_CAPTION, true);
		 setInputAreaWidth(300);
	 }

	 public String getFileName() {
		 return fileInput_.getFileName();
	 }

	 public Dimension getImageSize() {
	        return new Dimension(imageSize_[imSizeCombo_.getSelectedIndex()]);
	    }

	    public double getPlaybackRate() throws NumberFormatException {
	        return playbackRate_[playbackRateCombo_.getSelectedIndex()];
	    }

	    public int getFrameRate() throws NumberFormatException {
	        return Integer.parseInt(frameRateField_.getText()); 
	    }

	    private String[] _makeSizeStrings(Dimension[] size) {
	        String[] sizeString = new String[size.length];
	        for (int i = 0; i < size.length; i ++) {
	            StringBuffer buf = new StringBuffer();
	            buf.append(size[i].width);
	            buf.append('x');
	            buf.append(size[i].height);
	            sizeString[i] = buf.toString();
	        }
	        return sizeString;
	    }
	 
	}
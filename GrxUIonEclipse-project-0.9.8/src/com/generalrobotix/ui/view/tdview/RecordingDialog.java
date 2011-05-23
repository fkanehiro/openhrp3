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
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.ModalDialog;

@SuppressWarnings("serial") //$NON-NLS-1$
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
		 new String[] { "x 4", "x 2", "x 1", "x 1/2", "x 1/4" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	 
    private FileInput fileInput_;
    private JComboBox imSizeCombo_;
    private JComboBox playbackRateCombo_;
    private JTextField frameRateField_;
    private JTextField startTimeField_;
    private JTextField endTimeField_;
    private double logEndTime_;
    public RecordingDialog(Frame owner, double endTime) {
        super(
            owner,
            MessageBundle.get("RecordingDialog.dialog.title.recording"), //$NON-NLS-1$
            MessageBundle.get("RecordingDialog.dialog.message.recording"), //$NON-NLS-1$
            OK_CANCEL_TYPE
        );

		fileInput_ = new FileInput(new String[] { "mov" }, System.getProperty("user.dir")); //$NON-NLS-1$ //$NON-NLS-2$
		imSizeCombo_ = new JComboBox(_makeSizeStrings(imageSize_));
        playbackRateCombo_ = new JComboBox(playbackRateString_);
        playbackRateCombo_.setSelectedIndex(2);
        frameRateField_ = new JTextField("10"); //$NON-NLS-1$
        startTimeField_ = new JTextField("0.0");
        endTimeField_ = new JTextField(String.format("%.5f", endTime));
        logEndTime_ = endTime;
        
        addInputComponent(MessageBundle.get("RecordingDialog.label.fileName"), fileInput_, MULTILINE_CAPTION, true); //$NON-NLS-1$
        addInputComponent(MessageBundle.get("RecordingDialog.label.imageSize"), imSizeCombo_, MULTILINE_CAPTION, true); //$NON-NLS-1$
        addInputComponent(MessageBundle.get("RecordingDialog.label.playRate"), playbackRateCombo_, MULTILINE_CAPTION, true); //$NON-NLS-1$
        addInputComponent(MessageBundle.get("RecordingDialog.label.frameRate"), frameRateField_, MULTILINE_CAPTION, true); //$NON-NLS-1$
        addInputComponent(MessageBundle.get("RecordingDialog.label.startTime"), startTimeField_, INLINE_CAPTION, true);
        addInputComponent(MessageBundle.get("RecordingDialog.label.endTime"), endTimeField_, INLINE_CAPTION, true);
		setInputAreaWidth(300);
	 }
    
    public double getStartTime(){
    	double startTime;
    	try{
    		startTime = Double.parseDouble(startTimeField_.getText());
    	}catch (NumberFormatException e){
    		startTime = 0.0;
    	}
    	if(startTime < 0.0)
    		startTime = 0.0;
    	return startTime;
    }
    
    public double getEndTime(){
    	double endTime;
    	try{
    		endTime = Double.parseDouble(endTimeField_.getText());
    	}catch (NumberFormatException e){
    		endTime = 0.0;
    	}
    	if(endTime > logEndTime_)
    		endTime = logEndTime_;
    	return endTime;
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
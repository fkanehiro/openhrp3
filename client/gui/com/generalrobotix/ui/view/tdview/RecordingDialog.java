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

public class RecordingDialog extends ModalDialog {

	 //--------------------------------------------------------------------
	 // Instance variables
	 private FileInput fileInput_;
	 private JTextField imageSizeField_;

	 public RecordingDialog(Frame owner) {
		 super(
			owner,	
			"Recording Dialog", 
			"Input recording parameters.",
			 OK_CANCEL_TYPE
		 );

		fileInput_ = new FileInput(new String[] { "mov" }, System.getProperty("user.dir"));
		imageSizeField_ = new JTextField(10);
		imageSizeField_.setText("100");

		 addInputComponent("File name" , fileInput_, MULTILINE_CAPTION, true);
//		   addInputComponent("Image size [%]" , imageSizeField_, MULTILINE_CAPTION, true);
		 setInputAreaWidth(300);
	 }

	 public String getFileName() {
		 return fileInput_.getFileName();
	 }

	 public int getImageSize() throws NumberFormatException {
		 return Integer.parseInt(imageSizeField_.getText());
	 }
}

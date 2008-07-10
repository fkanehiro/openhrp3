/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.util;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

@SuppressWarnings("serial")
public class JTextAreaEx extends JTextArea {
	private int maxRowCount_ = 1000;
	public JTextAreaEx() {
		super();
	}

	public JTextAreaEx(Document doc, String text, int rows, int columns) {
		super(doc, text, rows, columns);
	}

	public JTextAreaEx(Document doc) {
		super(doc);
	}

	public JTextAreaEx(int rows, int columns) {
		super(rows, columns);
	}

	public JTextAreaEx(String text, int rows, int columns) {
		super(text, rows, columns);
	}

	public JTextAreaEx(String text) {
		super(text);
	}
	
	public void append(String str) {
		if (getLineCount() > maxRowCount_) {
			try {
				getDocument().remove(0, getLineEndOffset(0));
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		super.append(str);
	}
	
	public void setMaximumRowCount(int maxrow) {
		maxRowCount_ = maxrow; 
	}
	
	public int getMaximumRowCount() {
		return maxRowCount_;
	}
}

/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/*
 *  GrxTextEditorView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxTextItem;

@SuppressWarnings("serial")
public class GrxTextEditorView extends GrxBaseView {
    public static final String TITLE = "Text Editor";

	private GrxTextItem currentItem_ = null;

	private JLabel  labelFname_  = new JLabel();
	private JLabel 	labelCaretX_ = new JLabel();
	private JLabel 	labelCaretY_ = new JLabel();
	private JTextArea area_ = new JTextArea();
	private JButton save_ = new JButton(
			new ImageIcon(getClass().getResource("/resources/images/save_edit.png")));
	private JButton saveAs_ = new JButton(
			new ImageIcon(getClass().getResource("/resources/images/saveas_edit.png")));
	
	protected UndoAction undoAction_ = new UndoAction();
	protected RedoAction redoAction_ = new RedoAction();

	private static DecimalFormat FORMAT1 = new DecimalFormat("####0");
	
	public GrxTextEditorView(String name, GrxPluginManager manager) {
		super(name, manager);

		isScrollable_ = false;

		JPanel contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		JToolBar bar = new JToolBar();
		contentPane.add(bar, BorderLayout.NORTH);
		contentPane.add(new JScrollPane(area_), BorderLayout.CENTER);
	
		bar.addSeparator();
		bar.setFloatable(false);
		bar.add(save_);
		bar.add(saveAs_);
		bar.addSeparator();
		JPanel pnl = new JPanel();
		pnl.setOpaque(true);
		labelFname_.setOpaque(true);
		pnl.add(labelFname_);
		bar.add(pnl);
		bar.add(labelCaretX_);
		bar.add(new JLabel(":"));
		bar.add(labelCaretY_);
		
		save_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		save_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		save_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.save();
					save_.setEnabled(false);
				}
			}
		});
		
		saveAs_.setPreferredSize(GrxBaseView.getDefaultButtonSize());
		saveAs_.setMaximumSize(GrxBaseView.getDefaultButtonSize());
		saveAs_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.saveAs();
				}
			}
		});
		
		area_.setLineWrap(true);
		area_.setBackground(Color.lightGray);
		area_.setEnabled(false);
		Font font = new Font("Monospaced", Font.TRUETYPE_FONT, 12);
		area_.setFont(font);
		area_.addCaretListener(new javax.swing.event.CaretListener() { 
			public void caretUpdate(javax.swing.event.CaretEvent e) {
				_updateCaretLabel();
			}
		});
		area_.addKeyListener(new java.awt.event.KeyAdapter() { 
			public void keyTyped(java.awt.event.KeyEvent e) {    
				if (currentItem_ != null) {
					currentItem_.setEdited();
					save_.setEnabled(true);
				}
			}
		});	
		
		area_.addFocusListener(new java.awt.event.FocusListener() { 
			public void focusGained(java.awt.event.FocusEvent e) {
				if (currentItem_ == null)
					return;
				if (currentItem_.isModifiedExternally()) {
					int state = JOptionPane.showConfirmDialog(manager_.getFrame(),
						"The file has been modified on the file system.\n" +
						"Do you want to reload the script ?",
						"Reload Script",JOptionPane.YES_NO_OPTION);
					if (state == JOptionPane.YES_OPTION){
						currentItem_.reload();
						area_.setText(currentItem_.getValue());
					}
				}
			}
			
			public void focusLost(FocusEvent e) {
				_syncCurrentItem();
			}
		});
		
		
		area_.getDocument().addUndoableEditListener(new UndoableEditListener() {
				public void undoableEditHappened(UndoableEditEvent e) {
					if (currentItem_ != null) {
						currentItem_.undo_.addEdit(e.getEdit());
						undoAction_.updateUndoState();
						redoAction_.updateRedoState();
					}
				}	
		});
		
		_addBindings();
		_updateCaretLabel();
		_updateTitle();
		
		Dimension d = labelCaretX_.getPreferredSize();
		labelCaretX_.setPreferredSize(d);
		labelCaretX_.setHorizontalAlignment(JLabel.RIGHT);
		labelCaretY_.setPreferredSize(d);
		labelCaretY_.setHorizontalAlignment(JLabel.RIGHT);
	}

	private void _updateTitle() {
		if (currentItem_ != null) 
			labelFname_.setText(currentItem_.getURL(false));
		else 
			labelFname_.setText("NO ITEM SELECTED");
	}
	private void _updateCaretLabel() {
		if (currentItem_ != null) {
			int cpos = area_.getCaretPosition();
			int x=cpos+1,y=1;
			String str = area_.getText().substring(0,cpos);
			int pos = str.indexOf('\n');
			
			while(pos != -1){
				x = cpos-pos;
				pos = str.indexOf('\n',pos+1);
				y++;
			}
			labelCaretX_.setText(FORMAT1.format(x));
			labelCaretY_.setText(FORMAT1.format(y));
		} else {
			labelCaretX_.setText("-----");
			labelCaretY_.setText("-----");
		}
	}
	
	private void _addBindings() {
	  	InputMap inputMap = area_.getInputMap();
	    KeyStroke key = null;

	    //Ctrl-z to undo 
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
	    inputMap.put(key, undoAction_);   
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK);
	    inputMap.put(key, undoAction_);
	    //Ctrl-r to redo 
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK);
	    inputMap.put(key, redoAction_);
	    //Ctrl-p to paste 
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK);
	    inputMap.put(key, DefaultEditorKit.pasteAction);
	        
	    /*// Add a couple of vi key bindings for navigation.
	    // Ctrl-h to go backward one character
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_H, Event.CTRL_MASK);
		inputMap.put(key, DefaultEditorKit.backwardAction);
	    // Ctrl-l to go forward one character
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_L, Event.CTRL_MASK);
	    inputMap.put(key, DefaultEditorKit.forwardAction);
	    // Ctrl-j to go up one line
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK);
	    inputMap.put(key, DefaultEditorKit.upAction);
	    // Ctrl-l to go down one line
	    key = KeyStroke.getKeyStroke(KeyEvent.VK_J, Event.CTRL_MASK);
        inputMap.put(key, DefaultEditorKit.downAction);*/
    }	
    private class UndoAction extends AbstractAction {
        public UndoAction() {
            super("Undo  Ctrl+Z");
            setEnabled(false);
        }

	    public void actionPerformed(ActionEvent e) {
	        try {
	        	if (currentItem_ != null)
	        		currentItem_.undo_.undo();
	        } catch (CannotUndoException ex) {
	            System.out.println("Unable to undo: " + ex);
	            ex.printStackTrace();
	        }
	        updateUndoState();
	        redoAction_.updateRedoState();
	    }

	    protected void updateUndoState() {
	    	if (currentItem_ == null)
	    		return;
	        if (currentItem_.undo_.canUndo()) {
	            setEnabled(true);
	            putValue(Action.NAME, currentItem_.undo_.getUndoPresentationName());
	        } else {
	            setEnabled(false);
	            putValue(Action.NAME, "Undo");
	        }
	    }
	}
	private class RedoAction extends AbstractAction {
	    public RedoAction() {
	        super("Redo  Ctrl+R");
	        setEnabled(false);
	    }

	    public void actionPerformed(ActionEvent e) {
	        try {
	            currentItem_.undo_.redo();
	        } catch (CannotRedoException ex) {
	            System.out.println("Unable to redo: " + ex);
	            ex.printStackTrace();
	        }
	        updateRedoState();
	        undoAction_.updateUndoState();
	    }

	    protected void updateRedoState() {
	        if (currentItem_.undo_.canRedo()) {
	            setEnabled(true);
	            putValue(Action.NAME, currentItem_.undo_.getRedoPresentationName());
	        } else {
	            setEnabled(false);
	            putValue(Action.NAME, "Redo");
	        }
	    }
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		_syncCurrentItem();
		
		currentItem_ = null;
		GrxBaseItem item = manager_.getSelectedItem(GrxTextItem.class, null);
		if (item != null) {
			area_.setText((String)item.getValue());
			// keep currentItem_ null until setValue not to set undo
			currentItem_ = (GrxTextItem) item;
			area_.setCaretPosition(currentItem_.getCaretPositoin());
			area_.setEnabled(true);
			area_.setBackground(Color.white);
			save_.setEnabled(currentItem_.isEdited());
			saveAs_.setEnabled(true);
			undoAction_.updateUndoState();
			redoAction_.updateRedoState();
		} else {
			area_.setText("");
			area_.setEnabled(false);
			area_.setBackground(Color.lightGray);
			save_.setEnabled(false);
			saveAs_.setEnabled(false);
		}
		
		_updateCaretLabel();
		_updateTitle();
	}
	
	private void _syncCurrentItem() {
		if (currentItem_ != null) {
			currentItem_.setValue(area_.getText());
			currentItem_.setCaretPosition(area_.getCaretPosition());
		}
	}
}

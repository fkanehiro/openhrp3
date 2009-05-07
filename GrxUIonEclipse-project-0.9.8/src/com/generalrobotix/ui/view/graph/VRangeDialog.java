/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.view.graph;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.util.MessageBundle;


/**
 * グラフ垂直レンジ設定ダイアログ
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class VRangeDialog extends Dialog {

    // -----------------------------------------------------------------
    // インスタンス変数
    double base_;       // 基準値
    double extent_;     // 幅
    boolean updated_;   // 更新フラグ
    String unit_;       // 単位文字列
    Text minField_;   // 最小値フィールド
    Text maxField_;   // 最大値フィールド
    Label minUnitLabel_;   // 最小値単位ラベル
    Label maxUnitLabel_;   // 最大値単位ラベル
    private Shell shell_;
    private boolean minfirst_;
    private boolean maxfirst_;
    
    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   owner   親フレーム
     */
    public VRangeDialog(Shell shell) {
    	super(shell);
    	shell_ = shell;
    }
    
    protected void configureShell(Shell newShell) {   
        super.configureShell(newShell);
        newShell.setText(MessageBundle.get("dialog.graph.vrange.title"));
    }
    
    protected Control createDialogArea(Composite parent) {
    	Composite composite = (Composite)super.createDialogArea(parent);
    	composite.setLayout(new RowLayout(SWT.VERTICAL));
    	Composite comp0 = new Composite(composite, SWT.NONE);
    	comp0.setLayout(new RowLayout());
    	
    	Label label1 = new Label(comp0, SWT.NONE);
    	label1.setText(MessageBundle.get("dialog.graph.vrange.min"));
    	
    	minField_ = new Text(comp0, SWT.BORDER);
    	minField_.setText(String.format("%10.3f", base_));
    	minfirst_ = true;
    	minField_.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if(minfirst_){
					minField_.setText("");
					minfirst_ = false;
				}
			}

			public void keyReleased(KeyEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}
    		
    	});
    	minField_.setFocus();
    	
    	minUnitLabel_ = new Label(comp0, SWT.NONE);
    	minUnitLabel_.setText(unit_);
    	
    	Composite comp1 = new Composite(composite, SWT.NONE);
    	comp1.setLayout(new RowLayout());
    	
    	Label label3 = new Label(comp1, SWT.NONE);
    	label3.setText(MessageBundle.get("dialog.graph.vrange.max"));
    	maxField_ = new Text(comp1, SWT.BORDER);
    	maxField_.setText(String.format("%10.3f", base_+extent_));
    	maxfirst_ = true;
    	maxField_.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if(maxfirst_){
					maxField_.setText("");
					maxfirst_ = false;
				}
			}

			public void keyReleased(KeyEvent e) {
				// TODO 自動生成されたメソッド・スタブ
				
			}
    		
    	});
    	
    	maxUnitLabel_ = new Label(comp1, SWT.NONE);
    	maxUnitLabel_.setText(unit_);
    	
    	updated_ = false; 
    	return composite;
    }
    
    protected void buttonPressed(int buttonId) {
    	if (buttonId == IDialogConstants.OK_ID) {
    		double min, max;
            try {
                min = Double.parseDouble(minField_.getText());
                max = Double.parseDouble(maxField_.getText());
            } catch (NumberFormatException ex) {
            	 MessageDialog.openError(shell_, 
            			 MessageBundle.get("dialog.graph.vrange.invalidinput.title"),
                         MessageBundle.get("dialog.graph.vrange.invalidinput.message"));               
                 minField_.setFocus();    // フォーカス設定
                return;
            }
            // 入力値チェック
            if (min == max) {
            	MessageDialog.openError(shell_, 
            			MessageBundle.get("dialog.graph.vrange.invalidrange.title"),
            			MessageBundle.get("dialog.graph.vrange.invalidrange.message"));
                 minField_.setFocus();    // フォーカス設定
                return;
            }
            // 値更新
            if (min < max) {
                base_ = min;
                extent_ = max - min;
            } else {
                base_ = max;
                extent_ = min - max;
            }
            updated_ = true; 		
    	}
    	setReturnCode(buttonId);
    	close();
        super.buttonPressed(buttonId);
    }
  
    // -----------------------------------------------------------------
    // メソッド
    
    /**
     * 単位設定
     *
     * @param   unit    単位文字列
     */
    public void setUnit(
        String unit
    ) {
        unit_ = unit;
    }

    /**
     * 基準値設定
     *
     * @param   base    基準値
     */
    public void setBase(
        double base
    ) {
        base_ = base;
    }

    /**
     * 幅設定
     *
     * @param   extent  幅
     */
    public void setExtent(
        double extent
    ) {
        extent_ = extent;
    }

    /**
     * 基準値取得
     *
     * @param   基準値
     */
    public double getBase() {
        return base_;
    }

    /**
     * 幅取得
     *
     * @param   幅
     */
    public double getExtent() {
        return extent_;
    }

    /**
     * 更新フラグ取得
     *
     * @param   更新フラグ
     */
    public boolean isUpdated() {
        return updated_;
    }
}

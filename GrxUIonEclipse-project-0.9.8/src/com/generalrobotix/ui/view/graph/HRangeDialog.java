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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.util.MessageBundle;


/**
 * グラフ水平レンジ設定ダイアログ
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class HRangeDialog extends Dialog {

    // -----------------------------------------------------------------
    // 定数
    // 更新フラグ
    public static final int RANGE_UPDATED = 1;  // レンジ変更
    public static final int POS_UPDATED = 2;    // マーカ位置変更
       
    private static final int MARKER_POS_STEPS = 100;         // マーカ位置段階数

    // -----------------------------------------------------------------
    // インスタンス変数
    int updateFlag_;    // 更新フラグ
    double hRange_;     // レンジ値
    double maxHRange_;  // 最大レンジ
    double minHRange_;  // 最小レンジ
    double markerPos_;  // マーカ位置
    Text hRangeField_;    // レンジ入力フィールド
    Scale markerSlider_;      // マーカ位置設定スライダ
    private boolean first_;
    private Shell shell_;
    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   owner   親フレーム
     */
    public HRangeDialog(Shell shell) {
    	super(shell);
    	shell_ = shell;
    }
    
    protected void configureShell(Shell newShell) {   
        super.configureShell(newShell);
        newShell.setText(MessageBundle.get("dialog.graph.hrange.title"));
    }
        
    protected Control createDialogArea(Composite parent) {
    	Composite composite = (Composite)super.createDialogArea(parent);
    	composite.setLayout(new RowLayout(SWT.VERTICAL));
    	Composite comp0 = new Composite(composite, SWT.NONE);
    	comp0.setLayout(new RowLayout());
    	
    	Label label1 = new Label(comp0, SWT.NONE);
    	label1.setText(MessageBundle.get("dialog.graph.hrange.hrange"));
    	
    	hRangeField_ = new Text(comp0, SWT.BORDER);
    	hRangeField_.setText(String.format("%10.3f", hRange_));
    	first_ = true;
    	hRangeField_.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if(first_){
					hRangeField_.setText("");
					first_ = false;
				}
			}
			public void keyReleased(KeyEvent e) {
			}
    	});
    	hRangeField_.setFocus();
    	
    	Label label2 = new Label(comp0, SWT.NONE);
    	label2.setText(MessageBundle.get("dialog.graph.hrange.unit"));
    	
    	Composite comp1 = new Composite(composite, SWT.NONE);
    	comp1.setLayout(new RowLayout());
    	
    	Label label3 = new Label(comp1, SWT.NONE);
    	label3.setText(MessageBundle.get("dialog.graph.hrange.markerpos"));
    	
    	markerSlider_ = new Scale(comp1, SWT.HORIZONTAL);
    	markerSlider_.setMinimum(0);
    	markerSlider_.setMaximum(MARKER_POS_STEPS);
    	markerSlider_.setIncrement(10);
    	markerSlider_.setSelection((int)(markerPos_ * MARKER_POS_STEPS));
    	
        updateFlag_ = 0;
        return composite;
    }
        
    protected void buttonPressed(int buttonId) {
    	if (buttonId == IDialogConstants.OK_ID) {
    		 double range;
             try {
                 range = Double.parseDouble(hRangeField_.getText());
             } catch (NumberFormatException ex) {
                 // エラー表示
            	 MessageDialog.openError(shell_, 
            			 MessageBundle.get("dialog.graph.hrange.invalidinput.title"),
                         MessageBundle.get("dialog.graph.hrange.invalidinput.message"));
                
                 hRangeField_.setFocus();    // フォーカス設定
                 return;
             }
             // 入力値チェック
             if (range < minHRange_ || range > maxHRange_) {
                 // エラー表示
            	 MessageDialog.openError(shell_, 
                     MessageBundle.get("dialog.graph.hrange.invalidrange.title"),
                     MessageBundle.get("dialog.graph.hrange.invalidrange.message")
                         + "\n(" + minHRange_ + "  -  " + maxHRange_ + ")"    );
                 hRangeField_.setFocus();    // フォーカス設定
                 return;
             }
             double pos = markerSlider_.getSelection() / (double)MARKER_POS_STEPS;
             // 更新チェック
             updateFlag_ = 0;
             if (range != hRange_) { // レンジ更新?
                 hRange_ = range;
                 updateFlag_ += RANGE_UPDATED;
             }
             if (pos != markerPos_) {    // マーカ更新?
                 markerPos_ = pos;
                 updateFlag_ += POS_UPDATED;
             }
    	}
    	setReturnCode(buttonId);
    	close();
        super.buttonPressed(buttonId);
    }
     
    // -----------------------------------------------------------------
    // メソッド
   
    /**
     * 水平レンジ設定
     *
     * @param   hRange  水平レンジ
     */
    public void setHRange(
        double hRange
    ) {
        hRange_ = hRange;
    }

    /**
     * 水平レンジ最大値設定
     *
     * @param   maxHRange   水平レンジ最大値
     */
    public void setMaxHRange(
        double maxHRange
    ) {
        maxHRange_ = maxHRange;
    }

    /**
     * 水平レンジ最小値設定
     *
     * @param   minHRange   水平レンジ最小値
     */
    public void setMinHRange(
        double minHRange
    ) {
        minHRange_ = minHRange;
    }

    /**
     * マーカ位置設定
     *
     * @param   markerPos   マーカ位置
     */
    public void setMarkerPos(
        double markerPos
    ) {
        markerPos_ = markerPos;
    }

    /**
     * 水平レンジ取得
     *
     * @param   水平レンジ
     */
    public double getHRange() {
        return hRange_;
    }

    /**
     * マーカ位置取得
     *
     * @param   マーカ位置
     */
    public double getMarkerPos() {
        return markerPos_;
    }

    /**
     * 更新フラグ取得
     *
     * @param   更新フラグ
     */
    public int getUpdateFlag() {
        return updateFlag_;
    }

}

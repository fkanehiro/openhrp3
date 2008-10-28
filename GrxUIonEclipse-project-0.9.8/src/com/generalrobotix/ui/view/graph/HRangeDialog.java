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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.generalrobotix.ui.util.ErrorDialog;
import com.generalrobotix.ui.util.MessageBundle;

import java.text.DecimalFormat;

/**
 * グラフ水平レンジ設定ダイアログ
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class HRangeDialog extends JDialog {

    // -----------------------------------------------------------------
    // 定数
    // 更新フラグ
    public static final int RANGE_UPDATED = 1;  // レンジ変更
    public static final int POS_UPDATED = 2;    // マーカ位置変更
    // 配置
    private static final int BORDER_GAP = 12;   // 周辺部間隔
    private static final int LABEL_GAP = 12;    // ラベルと内容の最低間隔
    private static final int BUTTON_GAP = 5;    // ボタン間の間隔
    private static final int ITEM_GAP = 11;     // 行の間隔
    private static final int CONTENTS_GAP = 17; // 内容とボタンの間隔
    // その他
    private static final String FORMAT_STRING = "0.000";    // レンジ数値フォーマット文字列
    private static final int MARKER_POS_STEPS = 10;         // マーカ位置段階数

    // -----------------------------------------------------------------
    // インスタンス変数
    int updateFlag_;    // 更新フラグ
    double hRange_;     // レンジ値
    double maxHRange_;  // 最大レンジ
    double minHRange_;  // 最小レンジ
    double markerPos_;  // マーカ位置
    //boolean updated_; // 更新フラグ
    JTextField hRangeField_;    // レンジ入力フィールド
    JSlider markerSlider_;      // マーカ位置設定スライダ
    DecimalFormat rangeFormat_; // レンジ値のフォーマット

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   owner   親フレーム
     */
    public HRangeDialog(Frame owner) {
        super(owner, MessageBundle.get("dialog.graph.hrange.title"), true);

        // ラベル最大長決定
        JLabel label1 = new JLabel(MessageBundle.get("dialog.graph.hrange.hrange"));
        JLabel label2 = new JLabel(MessageBundle.get("dialog.graph.hrange.markerpos"));
        int lwid1 = label1.getMinimumSize().width;
        int lwid2 = label2.getMinimumSize().width;
        int lwidmax = (lwid1 > lwid2 ? lwid1 : lwid2);
        int lgap1 = lwidmax - lwid1 + LABEL_GAP;
        int lgap2 = lwidmax - lwid2 + LABEL_GAP;

        // 1行目(水平レンジ)
        hRangeField_ = new JTextField("", 8);
        hRangeField_.setHorizontalAlignment(JTextField.RIGHT);
        hRangeField_.setPreferredSize(new Dimension(100, 26));
        hRangeField_.setMaximumSize(new Dimension(100, 26));
        hRangeField_.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent evt) {
                    hRangeField_.setSelectionStart(0);
                    hRangeField_.setSelectionEnd(
                        hRangeField_.getText().length()
                    );
                }
            }
        );
        JPanel line1 = new JPanel();
        line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.add(label1);
        line1.add(Box.createHorizontalStrut(lgap1));
        line1.add(hRangeField_);
        line1.add(Box.createHorizontalStrut(5));
        line1.add(new JLabel(MessageBundle.get("dialog.graph.hrange.unit")));
        line1.add(Box.createHorizontalGlue());
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 2行目(マーカ位置)
        markerSlider_ = new JSlider(0, MARKER_POS_STEPS, 0);
        markerSlider_.setPreferredSize(new Dimension(200, 30));
        markerSlider_.setPaintTicks(true);
        markerSlider_.setMajorTickSpacing(1);
        markerSlider_.setSnapToTicks(true);
        JPanel line2 = new JPanel();
        line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.add(label2);
        line2.add(Box.createHorizontalStrut(lgap2));
        line2.add(markerSlider_);
        line2.add(Box.createHorizontalGlue());
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ボタン行
        // OKボタン
        JButton okButton = new JButton(MessageBundle.get("dialog.okButton"));
        okButton.setDefaultCapable(true);
        this.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    // 不正入力チェック
                    double range;
                    try {
                        range = Double.parseDouble(hRangeField_.getText());
                    } catch (NumberFormatException ex) {
                        // エラー表示
                        new ErrorDialog(
                            HRangeDialog.this,
                            MessageBundle.get("dialog.graph.hrange.invalidinput.title"),
                            MessageBundle.get("dialog.graph.hrange.invalidinput.message")
                        ).showModalDialog();
                        hRangeField_.requestFocus();    // フォーカス設定
                        return;
                    }
                    // 入力値チェック
                    if (range < minHRange_ || range > maxHRange_) {
                        // エラー表示
                        new ErrorDialog(
                            HRangeDialog.this,
                            MessageBundle.get("dialog.graph.hrange.invalidrange.title"),
                            MessageBundle.get("dialog.graph.hrange.invalidrange.message")
                                + "\n(" + minHRange_ + "  -  " + maxHRange_ + ")"
                        ).showModalDialog();
                        hRangeField_.requestFocus();    // フォーカス設定
                        return;
                    }
                    double pos = markerSlider_.getValue() / (double)MARKER_POS_STEPS;
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
                    //hRange_ = range;
                    //markerPos_ = markerSlider_.getValue() / (double)MARKER_POS_STEPS;
                    //updated_ = true;
                    HRangeDialog.this.setVisible(false);    // ダイアログ消去
                }
            }
        );
        // キャンセルボタン
        JButton cancelButton = new JButton(MessageBundle.get("dialog.cancelButton"));
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    HRangeDialog.this.setVisible(false);    // ダイアログ消去
                }
            }
        );
        this.addKeyListener(
            new KeyAdapter() {
                public void keyPressed(KeyEvent evt) {
                    if (evt.getID() == KeyEvent.KEY_PRESSED
                        && evt.getKeyCode() == KeyEvent.VK_ESCAPE) {    // エスケープ押下?
                        HRangeDialog.this.setVisible(false);    // ダイアログ消去
                    }
                }
            }
        );
        JPanel bLine = new JPanel();
        bLine.setLayout(new BoxLayout(bLine, BoxLayout.X_AXIS));
        bLine.add(Box.createHorizontalGlue());
        bLine.add(okButton);
        bLine.add(Box.createHorizontalStrut(BUTTON_GAP));
        bLine.add(cancelButton);
        bLine.add(Box.createHorizontalStrut(BORDER_GAP));
        bLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        // パネル構築
        Container pane = getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(Box.createVerticalStrut(BORDER_GAP));
        pane.add(line1);
        pane.add(Box.createVerticalStrut(ITEM_GAP));
        pane.add(line2);
        pane.add(Box.createVerticalStrut(CONTENTS_GAP));
        pane.add(bLine);
        pane.add(Box.createVerticalStrut(BORDER_GAP));

        // その他
        rangeFormat_ = new DecimalFormat(FORMAT_STRING);    // 数値フォーマット生成
        setResizable(false);  // リサイズ不可
    }

    // -----------------------------------------------------------------
    // メソッド
    /**
     * 表示非表示設定
     *
     * @param   visible 表示非表示フラグ
     */
    public void setVisible(
        boolean visible
    ) {
        if (visible) {  // 表示する?
            hRangeField_.setText(rangeFormat_.format(hRange_)); // レンジ設定
            markerSlider_.setValue( // スライダ位置設定
                (int)(markerPos_ * MARKER_POS_STEPS)
            );
            hRangeField_.requestFocus();    // 初期フォーカス設定
            //updated_ = false;
            updateFlag_ = 0;    // 更新フラグクリア
            pack(); // ウィンドウサイズ決定
        }
        super.setVisible(visible);  // 表示設定
    }

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

    /*
    public boolean isUpdated() {
        return updated_;
    }
    */
}

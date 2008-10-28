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
 * グラフ垂直レンジ設定ダイアログ
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class VRangeDialog extends JDialog {

    // -----------------------------------------------------------------
    // 定数
    // 配置
    private static final int BORDER_GAP = 12;   // 周辺部間隔
    private static final int LABEL_GAP = 12;    // ラベルと内容の最低間隔
    private static final int BUTTON_GAP = 5;    // ボタン間の間隔
    private static final int ITEM_GAP = 11;     // 行の間隔
    private static final int CONTENTS_GAP = 17; // 内容とボタンの間隔
    // その他
    private static final String FORMAT_STRING = "0.000";    // レンジ数値フォーマット文字列

    // -----------------------------------------------------------------
    // インスタンス変数
    double base_;       // 基準値
    double extent_;     // 幅
    boolean updated_;   // 更新フラグ
    //String unit_;       // 単位文字列
    JTextField minField_;   // 最小値フィールド
    JTextField maxField_;   // 最大値フィールド
    JLabel minUnitLabel_;   // 最小値単位ラベル
    JLabel maxUnitLabel_;   // 最大値単位ラベル
    DecimalFormat rangeFormat_; // レンジ値のフォーマット

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   owner   親フレーム
     */
    public VRangeDialog(Frame owner) {
        super(owner, MessageBundle.get("dialog.graph.vrange.title"), true);

        // ラベル最大長決定
        JLabel label1 = new JLabel(MessageBundle.get("dialog.graph.vrange.min"));
        JLabel label2 = new JLabel(MessageBundle.get("dialog.graph.vrange.max"));
        int lwid1 = label1.getMinimumSize().width;
        int lwid2 = label2.getMinimumSize().width;
        int lwidmax = (lwid1 > lwid2 ? lwid1 : lwid2);
        int lgap1 = lwidmax - lwid1 + LABEL_GAP;
        int lgap2 = lwidmax - lwid2 + LABEL_GAP;

        // 1行目(最小値)
        minField_ = new JTextField("", 8);
        minField_.setHorizontalAlignment(JTextField.RIGHT);
        minField_.setPreferredSize(new Dimension(100, 26));
        minField_.setMaximumSize(new Dimension(100, 26));
        minField_.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent evt) {
                    minField_.setSelectionStart(0);
                    minField_.setSelectionEnd(
                        minField_.getText().length()
                    );
                }
            }
        );
        minUnitLabel_ = new JLabel("");
        minUnitLabel_.setPreferredSize(new Dimension(50, 26));
        minUnitLabel_.setMaximumSize(new Dimension(50, 26));
        JPanel line1 = new JPanel();
        line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.add(label1);
        line1.add(Box.createHorizontalStrut(lgap1));
        line1.add(minField_);
        line1.add(Box.createHorizontalStrut(5));
        line1.add(minUnitLabel_);
        line1.add(Box.createHorizontalGlue());
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 2行目(最大値)
        maxField_ = new JTextField("", 8);
        maxField_.setHorizontalAlignment(JTextField.RIGHT);
        maxField_.setPreferredSize(new Dimension(100, 26));
        maxField_.setMaximumSize(new Dimension(100, 26));
        maxField_.addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent evt) {
                    maxField_.setSelectionStart(0);
                    maxField_.setSelectionEnd(
                        maxField_.getText().length()
                    );
                }
            }
        );
        maxUnitLabel_ = new JLabel("");
        maxUnitLabel_.setPreferredSize(new Dimension(50, 26));
        maxUnitLabel_.setMaximumSize(new Dimension(50, 26));
        JPanel line2 = new JPanel();
        line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.add(label2);
        line2.add(Box.createHorizontalStrut(lgap2));
        line2.add(maxField_);
        line2.add(Box.createHorizontalStrut(5));
        line2.add(maxUnitLabel_);
        line2.add(Box.createHorizontalGlue());
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ボタン行
        JButton okButton = new JButton(MessageBundle.get("dialog.okButton"));
        okButton.setDefaultCapable(true);
        this.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    // 不正入力チェック
                    double min, max;
                    try {
                        min = Double.parseDouble(minField_.getText());
                        max = Double.parseDouble(maxField_.getText());
                    } catch (NumberFormatException ex) {
                        // エラー表示
/*
                        JOptionPane.showMessageDialog(
                            VRangeDialog.this,
                            MessageBundle.get("dialog.graph.vrange.invalidinput.message"),
                            MessageBundle.get("dialog.graph.vrange.invalidinput.title"),
                            JOptionPane.ERROR_MESSAGE
                        );
*/

                        new ErrorDialog(
                            VRangeDialog.this,
                            MessageBundle.get("dialog.graph.vrange.invalidinput.title"),
                            MessageBundle.get("dialog.graph.vrange.invalidinput.message")
                        ).showModalDialog();

                        minField_.requestFocus();   // フォーカス設定
                        return;
                    }
                    // 入力値チェック
                    if (min == max) {
                        // エラー表示
/*
                        JOptionPane.showMessageDialog(
                            VRangeDialog.this,
                            MessageBundle.get("dialog.graph.vrange.invalidrange.message"),
                            MessageBundle.get("dialog.graph.vrange.invalidrange.title"),
                            JOptionPane.ERROR_MESSAGE
                        );
*/

                        new ErrorDialog(
                            VRangeDialog.this,
                            MessageBundle.get("dialog.graph.vrange.invalidrange.title"),
                            MessageBundle.get("dialog.graph.vrange.invalidrange.message")
                        ).showModalDialog();

                        minField_.requestFocus();   // フォーカス設定
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
                    VRangeDialog.this.setVisible(false);
                }
            }
        );
        // キャンセルボタン
        JButton cancelButton = new JButton(MessageBundle.get("dialog.cancelButton"));
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    VRangeDialog.this.setVisible(false);    // ダイアログ消去
                }
            }
        );
        this.addKeyListener(
            new KeyAdapter() {
                public void keyPressed(KeyEvent evt) {
                    if (evt.getID() == KeyEvent.KEY_PRESSED
                        && evt.getKeyCode() == KeyEvent.VK_ESCAPE) {    // エスケープ押下?
                        VRangeDialog.this.setVisible(false);    // ダイアログ消去
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
            minField_.setText(rangeFormat_.format(base_));              // 最小値設定
            maxField_.setText(rangeFormat_.format(base_ + extent_));    // 最大値設定
            minField_.requestFocus();   // 初期フォーカス設定
            updated_ = false;   // 更新フラグクリア
            pack(); // ウィンドウサイズ決定
        }
        super.setVisible(visible);  // 表示設定
    }

    /**
     * 単位設定
     *
     * @param   unit    単位文字列
     */
    public void setUnit(
        String unit
    ) {
        //unit_ = unit;
        minUnitLabel_.setText(unit);
        maxUnitLabel_.setText(unit);
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

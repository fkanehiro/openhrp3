package com.generalrobotix.ui.view.graph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.generalrobotix.ui.util.MessageBundle;

/**
 * EPS出力ダイアログ
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class EPSDialog extends JDialog {

    // -----------------------------------------------------------------
    // 定数
    // 配置
    private static final int BORDER_GAP = 12;
    private static final int LABEL_GAP = 12;
    private static final int BUTTON_GAP = 5;
    private static final int ITEM_GAP = 11;
    private static final int CONTENTS_GAP = 17;

    // -----------------------------------------------------------------
    // インスタンス変数
    String path_;           // パス
    boolean colorOutput_;   // カラー出力フラグ
    boolean graphOutput_;   // グラフ出力フラグ
    boolean legendOutput_;  // 凡例出力グラフ
    boolean updated_;       // 更新フラグ
    JTextField pathField_;  // パス入力フィールド
    JCheckBox colorCheck_;  // カラーチェックボックス
    JCheckBox graphCheck_;  // グラフ出力チェックボックス
    JCheckBox legendCheck_; // 凡例出力チェックボックス
    JButton okButton_;      // OKボタン
    JFileChooser chooser_;  // ファイル選択ダイアログ

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   owner   親フレーム
     */
    public EPSDialog(Frame owner) {
        super(owner, MessageBundle.get("dialog.graph.eps.title"), true);

        chooser_ = new JFileChooser(System.getProperty("user.dir"));

        // 1行目(パス)
        JLabel outputLabel = new JLabel(MessageBundle.get("dialog.graph.eps.outputto"));
        int labelWidth = outputLabel.getMinimumSize().width;
        pathField_ = new JTextField("", 20);
        pathField_.setPreferredSize(new Dimension(400, 26));
        pathField_.setMaximumSize(new Dimension(400, 26));
        JButton browseButton = new JButton(MessageBundle.get("dialog.graph.eps.browse"));
        browseButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    //int result = chooser_.showSaveDialog(EPSDialog.this);
                    int result = chooser_.showDialog(EPSDialog.this, MessageBundle.get("dialog.okButton"));
                    if (result == JFileChooser.APPROVE_OPTION) {
                        pathField_.setText(
                            chooser_.getSelectedFile().getPath()
                        );
                    }
                }
            }
        );
        JPanel line1 = new JPanel();
        line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.add(outputLabel);
        //line1.add(Box.createHorizontalGlue());
        line1.add(Box.createHorizontalStrut(LABEL_GAP));
        line1.add(pathField_);
        line1.add(Box.createHorizontalStrut(5));
        line1.add(browseButton);
        //line1.add(Box.createHorizontalStrut(7));   // (注)調整箇所
        line1.add(Box.createHorizontalStrut(BORDER_GAP));
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        // チェックボックスリスナ
        ItemListener checkListener = new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                okButton_.setEnabled(
                    graphCheck_.isSelected()
                    || legendCheck_.isSelected()
                );
            }
        };

        // 2行目(グラフチェック)
        colorCheck_ = new JCheckBox(MessageBundle.get("dialog.graph.eps.color"));
        graphCheck_ = new JCheckBox(MessageBundle.get("dialog.graph.eps.graph"));
        graphCheck_.addItemListener(checkListener);
        legendCheck_ = new JCheckBox(MessageBundle.get("dialog.graph.eps.legend"));
        legendCheck_.addItemListener(checkListener);
        JPanel line2 = new JPanel();
        line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
        line2.add(Box.createHorizontalStrut(BORDER_GAP));
        line2.add(Box.createHorizontalStrut(labelWidth));
        line2.add(Box.createHorizontalStrut(LABEL_GAP));
        line2.add(colorCheck_);
        line2.add(Box.createHorizontalStrut(LABEL_GAP));
        line2.add(graphCheck_);
        line2.add(Box.createHorizontalStrut(ITEM_GAP));
        line2.add(legendCheck_);
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ボタン行
        okButton_ = new JButton(MessageBundle.get("dialog.okButton"));
        this.getRootPane().setDefaultButton(okButton_);
        okButton_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    // パスのチェック
                    String path = pathField_.getText().trim();
                    // (不正なパスならエラーダイアログ表示)
                    // 値更新
                    if (!path.equals("") && !path.endsWith(".eps")) {
                        path += ".eps";
                    }
                    path_ = path;
                    colorOutput_ = colorCheck_.isSelected();
                    graphOutput_ = graphCheck_.isSelected();
                    legendOutput_ = legendCheck_.isSelected();
                    updated_ = true;
                    EPSDialog.this.setVisible(false);
                }
            }
        );
        JButton cancelButton = new JButton(MessageBundle.get("dialog.cancelButton"));
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    EPSDialog.this.setVisible(false);
                }
            }
        );
        this.addKeyListener(
            new KeyAdapter() {
                public void keyPressed(KeyEvent evt) {
                    if (evt.getID() == KeyEvent.KEY_PRESSED
                        && evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        EPSDialog.this.setVisible(false);
                    }
                }
            }
        );
        JPanel bLine = new JPanel();
        bLine.setLayout(new BoxLayout(bLine, BoxLayout.X_AXIS));
        bLine.add(Box.createHorizontalGlue());
        bLine.add(okButton_);
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
        if (visible) {
            pathField_.setText(path_);
            colorCheck_.setSelected(colorOutput_);
            graphCheck_.setSelected(graphOutput_);
            legendCheck_.setSelected(legendOutput_);
            okButton_.setEnabled(graphOutput_ || legendOutput_);
            pathField_.requestFocus();   // 初期フォーカス設定
            updated_ = false;
            pack();
        }
        super.setVisible(visible);
    }

    /**
     * パス設定
     *
     * @param   path    パス
     */
    public void setPath(
        String path
    ) {
        path_ = path;
    }

    /**
     * カラー出力フラグ設定
     *
     * @param   color   カラーフラグ
     */
    public void setColorOutput(
        boolean color
    ) {
        colorOutput_ = color;
    }


    /**
     * グラフ出力フラグ設定
     *
     * @param   output  出力フラグ
     */
    public void setGraphOutput(
        boolean output
    ) {
        graphOutput_ = output;
    }

    /**
     * 凡例出力フラグ設定
     *
     * @param   output  出力フラグ
     */
    public void setLegendOutput(
        boolean output
    ) {
        legendOutput_ = output;
    }

    /**
     * パス取得
     *
     * @return  パス
     */
    public String getPath() {
        return path_;
    }

    /**
     * カラー出力フラグ取得
     *
     * @return  カラー出力フラグ
     */
    public boolean isColorOutput() {
        return colorOutput_;
    }

    /**
     * グラフ出力フラグ取得
     *
     * @return  出力フラグ
     */
    public boolean isGraphOutput() {
        return graphOutput_;
    }

    /**
     * 凡例出力フラグ取得
     *
     * @return  出力フラグ
     */
    public boolean isLegendOutput() {
        return legendOutput_;
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

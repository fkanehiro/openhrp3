package com.generalrobotix.ui.view.graph;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.ListIterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.view.graph.LegendInfo;

/**
 * 凡例パネルクラス
 *
 */
	public class LegendPanel extends Canvas implements PaintListener{

    // -----------------------------------------------------------------
    // 定数
    private static final int MARGIN_X = 15;
    private static final int MARGIN_Y = 15;
    private static final int GAP_X = 10;
    private static final int GAP_Y = 5;
    private static final int LEN_LINE = 20;

    // -----------------------------------------------------------------
    // インスタンス変数
    private ArrayList<LegendInfo> legendList_;  // 凡例系列リスト
    private Font font_;             // ラベルフォント
    private Color backColor_;       // 背景色
    private Color labelColor_;      // ラベル色
    private Dimension size_;    // パネルサイズ

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   font        Font    ラベルフォント
     * @param   backColor   Color   背景色
     * @param   labelColor  Color   ラベル色
     */
    public LegendPanel(
    	Composite parent,
        Font font,
        Color backColor,
        Color labelColor
    ) {
    	super(parent, SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED);
        font_ = font;
        backColor_ = backColor;
        labelColor_ = labelColor;
        size_ = new Dimension(0, 0);
        //setPreferredSize(size_);
        legendList_ = new ArrayList<LegendInfo>();
        
        addPaintListener(this);
    }

    /**
     * 凡例追加
     *
     * @param   legend  LegendInfo  凡例情報
     */
    public void addLegend(
        LegendInfo legend
    ) {
        legendList_.add(legend);
        updateSize();
    }

    /**
     * 凡例削除
     *
     * @param   legend  LegendInfo  凡例情報
     */
    public void removeLegend(
        LegendInfo legend
    ) {
        int ind = legendList_.indexOf(legend);
        legendList_.remove(ind);
        updateSize();
    }

    /**
     * ラベルフォント設定
     *
     * @param   font        Font    ラベルフォント
     */
    public void setFont(
        Font font
    ) {
        font_ = font;
    }

    /**
     * 背景色
     *
     * @param   backColor   Color   背景色
     */
    public void setBackColor(
        Color color
    ) {
        backColor_ = color;
    }

    /**
     * ラベル色設定
     *
     * @param   labelColor  Color   ラベル色
     */
    public void setLabelColor(
        Color color
    ) {
        labelColor_ = color;
    }

    /**
     * 描画
     *
     * @param   g   Graphics    グラフィックス
     */
    public void paintControl(PaintEvent e) {
        // 背景
        int width = getSize().x;
        int height = getSize().y;
        e.gc.setBackground(backColor_);
        e.gc.fillRectangle(0, 0, width, height);
        // 凡例
        e.gc.setFont(font_);
        FontMetrics metrics = e.gc.getFontMetrics();    // フォントメトリクス
        int yofs = (int)(metrics.getHeight() / 3.5);    // ラベルYオフセット
        int ygap = metrics.getHeight() + GAP_Y;         // Y間隔
        ListIterator<LegendInfo> li = legendList_.listIterator();
        int ypos = MARGIN_Y;    // Y位置初期化
        while (li.hasNext()) {  // 全凡例をループ
            LegendInfo legend = (LegendInfo)li.next();  // 次の凡例
            e.gc.setForeground(legend.color);   // 凡例線色
            //g.drawLine(MARGIN_X, ypos - 1, MARGIN_X + LEN_LINE, ypos - 1);
            e.gc.drawLine(MARGIN_X, ypos, MARGIN_X + LEN_LINE, ypos);  // 線の描画
            //g.drawLine(MARGIN_X, ypos + 1, MARGIN_X + LEN_LINE, ypos + 1);
            e.gc.setForeground(labelColor_);    // ラベル色
            e.gc.drawString(   // ラベルの描画
                legend.label,
                MARGIN_X + LEN_LINE + GAP_X,
                ypos + yofs
            );
            ypos += ygap;   // Y位置の更新
        }
    }

    /**
     * 必要十分サイズ取得
     *   凡例を表示するのに必要十分なサイズを取得する
     *      (現時点では使用していない)
     *
     */
    public Dimension getMinimalSize() {
        return size_;
    }

    /**
     * サイズ更新
     *   凡例の数や長さに応じてパネルのサイズを決定する
     *
     */
    private void updateSize() {
        //TODO hattori
    	//FontMetrics metrics = this.getgetFontMetrics();    // フォントメトリクス
        int ygap = 0;//metrics.getHeight() + GAP_Y; // Y間隔
        ListIterator<LegendInfo> li = legendList_.listIterator();
        int ysize = MARGIN_Y;   // 高さ
        int max = 0;    // ラベル最大長
        while (li.hasNext()) {  // 全凡例をループ
            LegendInfo legend = (LegendInfo)li.next();
            //TODO hattori
            int len = 0;//metrics.stringWidth(legend.label);    // ラベルの長さを取得
            if (len > max) {    // 最大長?
                max = len;  // 最大長を更新
            }
            if (li.hasNext()) { // 最後の凡例でない?
                ysize += ygap;  // 高さを更新
            }
        }
        ysize += MARGIN_Y;  // 下マージン
        size_.width = MARGIN_X + LEN_LINE + GAP_X + max + MARGIN_X; // 幅更新
        size_.height = ysize;   // 高さ更新
        //System.out.println("ygap = " + ygap);
        //System.out.println("(" + size_.width + ", " + size_.height + ")");
    }
}

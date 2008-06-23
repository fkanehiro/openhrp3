package com.generalrobotix.ui.view.graph;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

/**
 * EPSグラフィックス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class EPSGraphics extends Graphics {

    // -----------------------------------------------------------------
    // 定数
    // ヘッダ等
    private static final String HEADER = "%!PS-Adobe-3.0 EPSF-3.0";
    private static final String BOUNDING_BOX = "%%BoundingBox:";
    private static final String DEF = "/";
    private static final String BIND_DEF = " bind def";
    private static final String EOF = "%%EOF";
    // 色オペレータ
    private static final String SET_COLOR_WHITE     = "setColorWhite";
    private static final String SET_COLOR_LIGHTGRAY = "setColorLightGray";
    private static final String SET_COLOR_GRAY      = "setColorGray";
    private static final String SET_COLOR_DARKGRAY  = "setColorDarkGray";
    private static final String SET_COLOR_BLACK     = "setColorBlack";
    private static final String SET_COLOR_OTHERS    = "setColorOthers";
    private static final String SET_COLOR_GREEN     = "setColorGreen";
    private static final String SET_COLOR_YELLOW    = "setColorYellow";
    private static final String SET_COLOR_PINK      = "setColorPink";
    private static final String SET_COLOR_CYAN      = "setColorCyan";
    private static final String SET_COLOR_MAGENTA   = "setColorMagenta";
    private static final String SET_COLOR_RED       = "setColorRed";
    private static final String SET_COLOR_ORANGE    = "setColorOrange";
    private static final String SET_COLOR_BLUE      = "setColorBlue";
    // 色オペレータ定義(白黒用)
    private static final String DEF_COLOR_WHITE
        = DEF + SET_COLOR_WHITE     + " {0.3 setlinewidth 0 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_LIGHTGRAY
        = DEF + SET_COLOR_LIGHTGRAY + " {0.3 setlinewidth 0.2 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_GRAY
        = DEF + SET_COLOR_GRAY      + " {0.3 setlinewidth 0.5 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_DARKGRAY
        = DEF + SET_COLOR_DARKGRAY  + " {0.3 setlinewidth 0.7 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_BLACK
        = DEF + SET_COLOR_BLACK     + " {0.3 setlinewidth 1 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_OTHERS
        = DEF + SET_COLOR_OTHERS    + " {0.3 setlinewidth 0 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_GREEN
        = DEF + SET_COLOR_GREEN     + " {0.3 setlinewidth 0 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_YELLOW
        = DEF + SET_COLOR_YELLOW    + " {0.3 setlinewidth 0.4 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_PINK
        = DEF + SET_COLOR_PINK      + " {0.3 setlinewidth 0.7 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_CYAN
        = DEF + SET_COLOR_CYAN      + " {0.6 setlinewidth 0.7 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_MAGENTA
        = DEF + SET_COLOR_MAGENTA   + " {0.9 setlinewidth 0 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_RED
        = DEF + SET_COLOR_RED       + " {0.9 setlinewidth 0.7 setgray [] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_ORANGE
        = DEF + SET_COLOR_ORANGE    + " {0.9 setlinewidth 0 setgray [6 2 2 2] 0 setdash}" + BIND_DEF;
    private static final String DEF_COLOR_BLUE
        = DEF + SET_COLOR_BLUE      + " {0.9 setlinewidth 0.7 setgray [6 2 2 2] 0 setdash}" + BIND_DEF;

    // 描画オペレータ
    private static final String DRAW_LINE     = "drawLine";
    private static final String SET_FONT      = "setFont";
    private static final String DRAW_STRING   = "drawString";
    private static final String SET_CLIP      = "setClip";
    private static final String SET_CLIP_NULL = "setClipNull";
    private static final String NEWPATH = "N";
    private static final String MOVETO = "M";
    private static final String LINETO = "L";
    private static final String STROKE = "S";
    // 描画オペレータ定義
    private static final String DEF_DRAW_LINE
        = DEF + DRAW_LINE     + " {newpath moveto lineto stroke}" + BIND_DEF;
    private static final String DEF_SET_FONT
        = DEF + SET_FONT      + " {exch findfont exch scalefont setfont}" + BIND_DEF;
    private static final String DEF_DRAW_STRING
        = DEF + DRAW_STRING   + " {moveto show}" + BIND_DEF;
    private static final String DEF_SET_CLIP
        = DEF + SET_CLIP
        + " {gsave newpath 3 index 3 index moveto dup 0 exch"
        + " rlineto exch 0 rlineto 0 exch sub 0 exch"
        + " rlineto pop pop closepath clip}"
        + BIND_DEF;
    private static final String DEF_SET_CLIP_NULL
        = DEF + SET_CLIP_NULL + " {grestore}" + BIND_DEF;
    private static final String DEF_NEWPATH = DEF + NEWPATH + " {newpath}" + BIND_DEF;
    private static final String DEF_MOVETO  = DEF + MOVETO  + " {moveto}"  + BIND_DEF;
    private static final String DEF_LINETO  = DEF + LINETO  + " {lineto}"  + BIND_DEF;
    private static final String DEF_STROKE  = DEF + STROKE  + " {stroke}"  + BIND_DEF;
    // 紙サイズ
    private static final int PAGE_HEIGHT = 792;
    //private static final int PAGE_WIDTH = 612;

    // -----------------------------------------------------------------
    // インスタンス変数
    private PrintWriter pw;         // プリントライタ
    private ArrayList<Color> colorList_;   // 色一覧
    private ArrayList<String> colorOps_;    // 色オペレータ一覧
    private double scale_;          // スケール
    private boolean inPath_;        // パス継続中フラグ
    private int prevX_;             // 前回X座標
    private int prevY_;             // 前回Y座標
    private int xOfs_;              // X座標オフセット
    private int yOfs_;              // Y座標オフセット

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   writer  ライタ
     * @param   top     バウンディングボックス上座標
     * @param   left    バウンディングボックス左座標
     * @param   width   バウンディングボックス幅
     * @param   heigt   バウンディングボックス高さ
     * @param   color   カラーフラグ
     */
    public EPSGraphics(
        Writer writer,
        int top,
        int left,
        int width,
        int height,
        boolean color
    ) {
        super();
        // 色設定
        colorList_ = new ArrayList<Color>();
        colorList_.add(Color.white);
        colorList_.add(Color.lightGray);
        colorList_.add(Color.gray);
        colorList_.add(Color.darkGray);
        colorList_.add(Color.black);
        colorList_.add(Color.green);
        colorList_.add(Color.yellow);
        colorList_.add(Color.pink);
        colorList_.add(Color.cyan);
        colorList_.add(Color.magenta);
        colorList_.add(Color.red);
        colorList_.add(Color.orange);
        colorList_.add(Color.blue);
        colorOps_ = new ArrayList<String>();
        colorOps_.add(SET_COLOR_WHITE);
        colorOps_.add(SET_COLOR_LIGHTGRAY);
        colorOps_.add(SET_COLOR_GRAY);
        colorOps_.add(SET_COLOR_DARKGRAY);
        colorOps_.add(SET_COLOR_BLACK);
        colorOps_.add(SET_COLOR_GREEN);
        colorOps_.add(SET_COLOR_YELLOW);
        colorOps_.add(SET_COLOR_PINK);
        colorOps_.add(SET_COLOR_CYAN);
        colorOps_.add(SET_COLOR_MAGENTA);
        colorOps_.add(SET_COLOR_RED);
        colorOps_.add(SET_COLOR_ORANGE);
        colorOps_.add(SET_COLOR_BLUE);
        // その他の設定
        scale_ = 1;         // スケールクリア
        inPath_ = false;    // パス継続中でない
        xOfs_ = 0;          // Xオフセットクリア
        yOfs_ = 0;          // Yオフセットクリア
        // ヘッダ出力
        pw = new PrintWriter(writer);   // プリントライタオープン
        _writeHeader(top, left, width, height, color);   // ヘッダ出力
    }

    // -----------------------------------------------------------------
    // メソッド
    /**
     * Xオフセット設定
     *
     * @param   xofs    Xオフセット
     */
    public void setXOffset(int xofs) {
        xOfs_ = xofs;
    }

    /**
     * Yオフセット設定
     *
     * @param   yofs    Yオフセット
     */
    public void setYOffset(int yofs) {
        yOfs_ = yofs;
    }

    /**
     * スケール設定
     *
     * @param   scale   スケール
     */
    public void setScale(double scale) {
        _stroke();
        scale_ = scale;
    }

    /**
     * 線幅設定
     *
     * @param   width   線幅
     */
    public void setLineWidth(double width) {
        _stroke();
        pw.println("" + width + " setlinewidth");
    }

    /**
     * 出力終了
     *
     */
    public void finishOutput() {
        _stroke();           // パスを描画
        pw.println(EOF);    // EOFマーク出力
        pw.close();         // プリントライタクローズ
    }

    // -----------------------------------------------------------------
    // Graphicsのメソッドオーバーライド
    /**
     * 色設定
     *
     * @param   color   色
     */
    public void setColor(Color c) {
        _stroke();
        int ind = colorList_.indexOf(c);
        String col;
        if (ind >= 0) {
            col = (String)colorOps_.get(ind);
        } else {
            col = SET_COLOR_OTHERS;
        }
        pw.println(col);
    }

    /**
     * 矩形塗りつぶし
     *
     * @param   x       左座標
     * @param   y       上座標
     * @param   width   幅
     * @param   height  高さ
     */
    public void fillRect(int x, int y, int width, int height) {
        // 無処理
    }

    /**
     * 線描画
     *
     * @param   x1  始点X座標
     * @param   y1  始点Y座標
     * @param   x2  終点X座標
     * @param   y2  終点Y座標
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        StringBuffer sb;
        if (inPath_) {   // パス継続中?
            if (prevX_ == x1 && prevY_ == y1) { // 始点が前回の終点と一致?
                // x2 y2 lineto
                sb = new StringBuffer();
                sb.append(x2 / scale_ + xOfs_);
                sb.append(' '); sb.append(PAGE_HEIGHT - y2 / scale_ - yOfs_);
                sb.append(' '); sb.append(LINETO);
                pw.println(sb.toString());
            } else {    // 始点が前回の終点と異なる?
                // stroke
                // newpath
                // x1 y1 moveto
                // x2 y2 lineto
                sb = new StringBuffer(STROKE);
                sb.append(' '); sb.append(NEWPATH);
                sb.append(' '); sb.append(x1 / scale_ + xOfs_);
                sb.append(' '); sb.append(PAGE_HEIGHT - y1 / scale_ - yOfs_);
                sb.append(' '); sb.append(MOVETO);
                sb.append('\n'); sb.append(x2 / scale_ + xOfs_);
                sb.append(' '); sb.append(PAGE_HEIGHT - y2 / scale_ - yOfs_);
                sb.append(' '); sb.append(LINETO);
                pw.println(sb.toString());
            }
        } else {    // パス継続中でない?
            // newpath
            // x1 y1 moveto
            // x2 y2 lineto
            sb = new StringBuffer(NEWPATH);
            sb.append(' '); sb.append(x1 / scale_ + xOfs_);
            sb.append(' '); sb.append(PAGE_HEIGHT - y1 / scale_ - yOfs_);
            sb.append(' '); sb.append(MOVETO);
            sb.append('\n'); sb.append(x2 / scale_ + xOfs_);
            sb.append(' '); sb.append(PAGE_HEIGHT - y2 / scale_ - yOfs_);
            sb.append(' '); sb.append(LINETO);
            pw.println(sb.toString());
            inPath_ = true;
        }
        prevX_ = x2; prevY_ = y2;   // 終点を更新

        /* ★この実装は無駄が多いので取りやめ
        StringBuffer sb = new StringBuffer();
        sb.append(x1 / scale_ + xOfs_);
        sb.append(' '); sb.append(PAGE_HEIGHT - y1 / scale_ - yOfs_);
        sb.append(' '); sb.append(x2 / scale_ + xOfs_);
        sb.append(' '); sb.append(PAGE_HEIGHT - y2 / scale_ - yOfs_);
        sb.append(' '); sb.append(DRAW_LINE);
        pw.println(sb.toString());
        */
    }

    /**
     * フォント設定
     *
     * @param   font    フォント
     */
    public void setFont(Font font) {
        _stroke();  // 引きかけの線を引く
        StringBuffer sb = new StringBuffer("/");

        // フォント決定
        //sb.append(font.getPSName()); <--- ★本来はこれで良いはずだが...
        String fname = font.getName();
        String psf;
        if (fname.equals("dialog")) {
            psf = "Helvetica";
        } else if (fname.equals("monospaced")) {
            psf = "Courier";
        } else {
            psf = "Times-Roman";
        }
        sb.append(psf);

        sb.append(' '); sb.append(font.getSize());
        sb.append(' '); sb.append(SET_FONT);
        pw.println(sb.toString());
    }

    /**
     * 文字列描画
     *
     * @param   str 文字列
     * @param   x   X座標
     * @param   y   Y座標
     */
    public void drawString(String str, int x, int y) {
        _stroke();  // 引きかけの線を引く
        StringBuffer sb = new StringBuffer("(");
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '(' || c == ')') {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append(") "); sb.append(x / scale_ + xOfs_);
        sb.append(' '); sb.append(PAGE_HEIGHT - y / scale_ - yOfs_);
        sb.append(' '); sb.append(DRAW_STRING);
        pw.println(sb.toString());
    }

    /**
     * クリップ設定
     *
     * @param   clip    クリップ形状
     */
    public void setClip(Shape clip) {
        _stroke();  // 引きかけの線を引く
        if (clip == null) { // クリップ解除?
            pw.println(SET_CLIP_NULL);  // クリップ解除
        }
    }

    /**
     * 矩形クリップ設定
     *
     * @param   x       左座標
     * @param   y       上座標
     * @param   width   幅
     * @param   height  高さ
     */
    public void setClip(int x, int y, int width, int height) {
        _stroke();  // 引きかけの線を引く
        StringBuffer sb = new StringBuffer();
        sb.append(x / scale_ + xOfs_);
        sb.append(' '); sb.append(PAGE_HEIGHT - (y + height) / scale_ - yOfs_);
        sb.append(' '); sb.append(width / scale_);
        sb.append(' '); sb.append(height / scale_);
        sb.append(' '); sb.append(SET_CLIP);
        pw.println(sb.toString());  // クリップ
    }

    /**
     * XORモードに設定
     *
     * @param   color   色
     */
    public void setXORMode(Color c) {
        // 無処理
    }

    /**
     * ペイントモードに設定
     *
     */
    public void setPaintMode() {
        // 無処理
    }

    // -----------------------------------------------------------------
    // Graphicsのメソッドオーバーライド(未使用部)
    public Graphics create() {
        return null;
    }
    public void translate(int x, int y) { }
    public Color getColor() {
        return null;
    }
    public Font getFont() {
        return null;
    }
    public FontMetrics getFontMetrics(Font f) {
        return null;
    }
    public Rectangle getClipBounds() {
        return null;
    }
    public void clipRect(int x, int y, int width, int height) { }
    public Shape getClip() {
        return null;
    }
    public void copyArea(
        int x, int y, int width, int height, int dx, int dy
    ) { }
    public void clearRect(int x, int y, int width, int height) { }
    public void drawRoundRect(
        int x, int y, int width, int height, int arcWidth, int arcHeight
    ) { }
    public void fillRoundRect(
        int x, int y, int width, int height, int arcWidth, int arcHeight
    ) { }
    public void drawOval(int x, int y, int width, int height) { }
    public void fillOval(int x, int y, int width, int height) { }
    public void drawArc(
        int x, int y, int width, int height, int startAngle, int arcAngle
    ) { }
    public void fillArc(
        int x, int y, int width, int height, int startAngle, int arcAngle
    ) { }
    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) { }
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) { }
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) { }
    public void drawString(
        AttributedCharacterIterator iterator, int x, int y
    ) { }
    public boolean drawImage(
        Image img, int x, int y, ImageObserver observer
    ) {
        return false;
    }
    public boolean drawImage(
        Image img, int x, int y, int width, int height, ImageObserver observer
    ) {
        return false;
    }
    public boolean drawImage(
        Image img, int x, int y, Color bgcolor, ImageObserver observer
    ) {
        return false;
    }
    public boolean drawImage(
        Image img, int x, int y, int width, int height,
        Color bgcolor, ImageObserver observer
    ) {
        return false;
    }
    public boolean drawImage(
        Image img, int dx1, int dy1, int dx2, int dy2,
        int sx1, int sy1, int sx2, int sy2, ImageObserver observer
    ) {
        return false;
    }
    public boolean drawImage(
        Image img, int dx1, int dy1, int dx2, int dy2,
        int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer
    ) {
        return false;
    }
    public void dispose() { }


    // -----------------------------------------------------------------
    // プライベートメソッド
    /**
     * ヘッダ出力
     *
     * @param   top     バウンディングボックス上座標
     * @param   left    バウンディングボックス左座標
     * @param   width   バウンディングボックス幅
     * @param   heigt   バウンディングボックス高さ
     * @param   color   カラーフラグ
     */
    private void _writeHeader(
        int top,
        int left,
        int width,
        int height,
        boolean color
    ) {
        // BoundingBox計算
        int bl = left;
        int bb = PAGE_HEIGHT - (top + height);
        int br = left + width;
        int bt = PAGE_HEIGHT - top;

        // Header
        pw.println(HEADER);
        StringBuffer sb = new StringBuffer(BOUNDING_BOX);
        sb.append(' '); sb.append(bl);
        sb.append(' '); sb.append(bb);
        sb.append(' '); sb.append(br);
        sb.append(' '); sb.append(bt);
        pw.println(sb.toString());
        pw.println();

        // 色オペレータ宣言
        pw.println("% Color definition");
        if (color) {
            for (int i = 0; i < colorList_.size(); i++) {
                Color col = (Color)colorList_.get(i);
                StringBuffer sbuf = new StringBuffer(DEF);
                sbuf.append((String)colorOps_.get(i));
                sbuf.append(" {0.3 setlinewidth 0 setgray [] 0 setdash ");
                sbuf.append((255 - col.getRed()) / 255.0f);
                sbuf.append(' ');
                sbuf.append((255 - col.getGreen()) / 255.0f);
                sbuf.append(' ');
                sbuf.append((255 - col.getBlue()) / 255.0f);
                sbuf.append(" setrgbcolor}");
                sbuf.append(BIND_DEF);
                pw.println(sbuf.toString());
            }
            pw.println(DEF_COLOR_OTHERS);
        } else {
            pw.println(DEF_COLOR_WHITE);
            pw.println(DEF_COLOR_LIGHTGRAY);
            pw.println(DEF_COLOR_GRAY);
            pw.println(DEF_COLOR_DARKGRAY);
            pw.println(DEF_COLOR_BLACK);
            pw.println(DEF_COLOR_OTHERS);
            pw.println(DEF_COLOR_GREEN);
            pw.println(DEF_COLOR_YELLOW);
            pw.println(DEF_COLOR_PINK);
            pw.println(DEF_COLOR_CYAN);
            pw.println(DEF_COLOR_MAGENTA);
            pw.println(DEF_COLOR_RED);
            pw.println(DEF_COLOR_ORANGE);
            pw.println(DEF_COLOR_BLUE);
        }
        pw.println();

        // 描画オペレータ宣言
        pw.println("% Method definition");
        pw.println(DEF_DRAW_LINE);
        pw.println(DEF_SET_FONT);
        pw.println(DEF_DRAW_STRING);
        pw.println(DEF_SET_CLIP);
        pw.println(DEF_SET_CLIP_NULL);
        pw.println(DEF_NEWPATH);
        pw.println(DEF_MOVETO);
        pw.println(DEF_LINETO);
        pw.println(DEF_STROKE);
        pw.println();

        // ヘッダ終了
        pw.println("% end of header");
        pw.println();

        // ★デバグ用(バウンディングボックス描画)
        pw.println("newpath");
        pw.println("" + bl + " " + bb + " moveto");
        pw.println("" + br + " " + bb + " lineto");
        pw.println("" + br + " " + bt + " lineto");
        pw.println("" + bl + " " + bt + " lineto");
        pw.println("closepath");
        pw.println("stroke");
    }

    /**
     * ストローク
     *
     */
    private void _stroke() {
        if (inPath_) {  // パス継続中?
            pw.println(STROKE); // ストローク
            inPath_ = false;    // パス終了
        }
    }
}

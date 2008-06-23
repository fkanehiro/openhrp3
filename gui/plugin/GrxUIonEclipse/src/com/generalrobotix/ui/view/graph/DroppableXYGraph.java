package com.generalrobotix.ui.view.graph;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

/**
 * ドロップ可能折れ線グラフクラス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class DroppableXYGraph
    extends XYLineGraph
    implements DropTargetListener
{
    private Object droppedObject_;  // ドロップされたオブジェクト
    DropTarget dropTarget_;         // ドロップターゲット
    ActionListener actionListener_; // アクションリスナ

    boolean dropSucceeded_;

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   leftMargin      int 左マージン
     * @param   rightMargin     int 右マージン
     * @param   topMargin       int 上マージン
     * @param   bottomMargin    int 下マージン
     */
    public DroppableXYGraph(
        int leftMargin,     // 左マージン
        int rightMargin,    // 右マージン
        int topMargin,      // 上マージン
        int bottomMargin    // 下マージン
    ) {
        super(leftMargin, rightMargin, topMargin, bottomMargin);
        dropTarget_ = new DropTarget(
            this,
            DnDConstants.ACTION_COPY_OR_MOVE,
            this,
            true
        );
        droppedObject_ = null;
        dropSucceeded_ = true;
    }

    // -----------------------------------------------------------------
    // インスタンスメソッド
    /**
     * ドロップ許可不許可設定
     *
     * @param   active  ドロップ許可不許可フラグ
     */
    public void setDropActive(
        boolean active
    ) {
        dropTarget_.setActive(active);
    }

    /**
     * ドロップオブジェクトの取得
     *
     * @return  ドロップされたオブジェクト
     */
    public Object getDroppedObject() {
        return droppedObject_;
    }

    public void setDropSucceeded(boolean flag) {
        dropSucceeded_ = flag;
    }

    // -----------------------------------------------------------------
    // ActionListener登録および削除
    public void addActionListener(ActionListener listener) {
        actionListener_ = AWTEventMulticaster.add(actionListener_, listener);
    }
    public void removeActionListener(ActionListener listener) {
        actionListener_ = AWTEventMulticaster.remove(actionListener_, listener);
    }

    // -----------------------------------------------------------------
    // DropTargetListenerの実装
    /**
     * ドロップされた
     *
     * @param   evt ドロップイベント
     */
    public void drop(DropTargetDropEvent evt) {
        if (
            evt.isDataFlavorSupported(AttributeInfo.dataFlavor)
            && (
                evt.getDropAction()
                & DnDConstants.ACTION_COPY_OR_MOVE
            ) != 0
        ) { // 受け入れ可?
            evt.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);   // ドロップ受け入れ
            try {
                Transferable tr = evt.getTransferable();
                if (tr.isDataFlavorSupported(AttributeInfo.dataFlavor)){
                    droppedObject_ = tr.getTransferData(AttributeInfo.dataFlavor);
                    if(actionListener_ != null) {
                        actionListener_.actionPerformed(
                            new ActionEvent(
                                this,
                                ActionEvent.ACTION_PERFORMED,
                                "Dropped"
                            )
                        );
                    }
                    evt.dropComplete(dropSucceeded_);
                } else {
                    System.err.println("サポートしないフレーバ");
                    evt.dropComplete(false);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("ドロップ失敗");
                evt.dropComplete(false);
            }
        } { // 受け入れ不可?
            evt.rejectDrop();           // ドロップ不許可通知
            evt.dropComplete(false);    // ドロップ失敗通知
        }
    }

    /**
     * ドラッグして入ってきた
     *
     * @param   evt ドラッグイベント
     */
    public void dragEnter(DropTargetDragEvent evt) {
        if (
            evt.isDataFlavorSupported(AttributeInfo.dataFlavor)
            && (evt.getSourceActions() & DnDConstants.ACTION_COPY_OR_MOVE) != 0 // 受け入れ可?
        ) {
            evt.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);   // ドラッグを受け入れる
        } else {
            evt.rejectDrag();   // ドラッグ受け入れ拒否
        }
    }

    public void dragExit(DropTargetEvent evt) {}
    public void dragOver(DropTargetDragEvent evt) {}
    public void dropActionChanged(DropTargetDragEvent evt) {}
}

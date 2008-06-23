/**
 * InvKinemaChangeListener.java
 *
 *   TransformGroup の変更を BehaviorManager に通知する
 *   ために使う Listener Interface
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.tdview;

import java.util.EventListener;

public interface InvKinemaChangeListener extends EventListener
{
    /**
     * コンストラクタ
     *
     * @param   event    イベント
     */
    public void invKinemaChanged(TransformChangeEvent event);
}

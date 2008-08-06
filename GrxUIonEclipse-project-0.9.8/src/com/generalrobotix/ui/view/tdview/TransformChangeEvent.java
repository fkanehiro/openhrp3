/**
 * TransformChangeEvent.java
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.tdview;

import java.util.EventObject;
import javax.media.j3d.*;

public class TransformChangeEvent extends EventObject
{

    /**
     */
    // 内部に保持する TransformGroup
    private TransformGroup tgChanged = null;

    /**
     * コンストラクタ
     * @param  objSource
     * @param  tgChanged
     */
    public TransformChangeEvent(Object objSource,TransformGroup tgChanged)
    {
        super(objSource);
        this.tgChanged = tgChanged;
    }

    /**
     * トランスフォームグループ取得
     * @return  トランスフォームグループ
     */
    public TransformGroup getTransformGroup()
    {
        return tgChanged;
    }
}

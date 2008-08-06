/**
 * SimulationWorld.java
 *
 *  Canvas3Dをaddすることで3D表示が可能であることを示す
 *  インターフェース。
 *  このインターフェースはDNDをサポートすることを前提とする
 *  ため、Serializableインタフェースを継承する。
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.tdview;

import java.io.Serializable;
import javax.media.j3d.*;

public interface ThreeDDrawable extends Serializable {
    /**
     *
     * @param   bg
     */
    public String getName();
    public String getFullName();
    public void attach(BranchGroup bg);

    public void setViewMode(int mode);

    public void setDirection(int dir);

    public void setTransform(Transform3D transform);

    //public void addViewTransformListener(ViewTransformListener listener);

    //public void removeViewTransformListener(ViewTransformListener listener);

    public TransformGroup getTransformGroupRoot();

    public ViewInfo getViewInfo();

/*
    public void changeAttribute(String attrName, String value)
        throws
            NoSuchAttributeException,
            InvalidAttributeException,
            NotEditableAttributeException;
*/
}

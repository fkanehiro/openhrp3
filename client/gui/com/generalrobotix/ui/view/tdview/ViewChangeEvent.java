package com.generalrobotix.ui.view.tdview;

import java.util.EventObject;
import javax.media.j3d.TransformGroup;

class ViewChangeEvent extends EventObject {
    TransformGroup transform_;

    public ViewChangeEvent(Object source, TransformGroup transform) {
        super(source);
        transform_ = transform;
    }

    public TransformGroup getTransformGroup() { return transform_; }
}

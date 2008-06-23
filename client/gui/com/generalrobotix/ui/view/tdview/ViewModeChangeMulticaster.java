package com.generalrobotix.ui.view.tdview;

public class ViewModeChangeMulticaster implements ViewModeChangeListener {
    protected ViewModeChangeListener listener1_, listener2_;

    protected ViewModeChangeMulticaster(
        ViewModeChangeListener listener1,
        ViewModeChangeListener listener2
    ) {
        listener1_ = listener1;
        listener2_ = listener2;
    }

    public void viewModeChanged(int mode) {
        listener1_.viewModeChanged(mode);
        listener2_.viewModeChanged(mode);
    }

    public static ViewModeChangeListener add(
        ViewModeChangeListener listener1,
        ViewModeChangeListener listener2
    ) {
        if (listener1 == null) return listener2;
        if (listener2 == null) return listener1;
        return new ViewModeChangeMulticaster(listener1, listener2);
    }

    public static ViewModeChangeListener remove(
        ViewModeChangeListener listener,
        ViewModeChangeListener old
    ) {
        if (listener == old || listener == null) {
            return null;
        } else if (listener instanceof ViewModeChangeMulticaster) {
            return ((ViewModeChangeMulticaster)listener).remove(old);
        } else {
            return listener;
        }
    }

    protected ViewModeChangeListener remove(ViewModeChangeListener listener) {
        if (listener == listener1_) return listener2_;
        if (listener == listener2_) return listener1_;
        ViewModeChangeListener listener1 = remove(listener1_, listener);
        ViewModeChangeListener listener2 = remove(listener2_, listener);
        if (listener1 == listener1_ && listener2 == listener2_) {
            return this;
        }
        return add(listener1, listener2);
    }
}

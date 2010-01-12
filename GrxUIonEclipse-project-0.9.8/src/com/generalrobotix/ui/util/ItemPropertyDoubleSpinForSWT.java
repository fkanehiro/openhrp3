
package com.generalrobotix.ui.util;

import org.eclipse.swt.widgets.Composite;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.view.graph.SEDoubleTextWithSpinForSWT;


//SEDouble型の値を設定するSWT用スピナーを提供するクラス

public class ItemPropertyDoubleSpinForSWT extends SEDoubleTextWithSpinForSWT{

    private GrxBaseItem item_;
    private String key_;

    public ItemPropertyDoubleSpinForSWT(Composite parent,int style,double min,double max,double step){
        super(parent, style, min, max, step);
    }
    
    protected void updateValue()
    {
        updateProperty(text2value());
    }
    
    public void updateProperty(double v) {
        if (isOk(v)) {
            if (item_ != null && key_ != null){
                item_.setDbl(key_, v);
            }
        }
    }
    
    public void setItem(GrxBaseItem item) {
        item_ = item;
    }
    
    public void setKey(String key){
        key_ = key;
    }
}

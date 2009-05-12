
package com.generalrobotix.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.view.graph.SEDouble;


//SEDouble型の値を設定するSWT用スピナーを提供するクラス

public class ItemPropertyDoubleSpinForSWT extends Composite{

    private SEDouble value_;
    private double max_;
    private double min_;
    private double step_;
    
    private Text text_;
    
    private GrxBaseItem item_;
    private String key_;
    
    public ItemPropertyDoubleSpinForSWT(Composite parent,int style,double min,double max,double step){
        super(parent,style);
        value_ = new SEDouble(0);
        
        max_ = max;
        min_ = min;
        step_ = step;
        
        setLayout(new GridLayout(2,false));
        
        text_ = new Text(this,SWT.SINGLE | SWT.BORDER);
        text_.addFocusListener(new FocusListener(){
        	public void focusLost(FocusEvent e){
        		updateProperty(text2value());
        	}
        	public void focusGained(FocusEvent e){
        	}
        });
        GridData gridData = new GridData();
        gridData.widthHint = 80;
        text_.setLayoutData(gridData);
        
        Composite buttonPane = new Composite(this,SWT.NONE);
        buttonPane.setLayout(new RowLayout(SWT.VERTICAL));
        Button buttonUP = new Button(buttonPane,SWT.ARROW | SWT.UP);
        buttonUP.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                double v = text2value() + step_;
                updateProperty(v);
            }
            
        });
        Button buttonDOWN = new Button(buttonPane,SWT.ARROW | SWT.DOWN);
        buttonDOWN.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                double v = text2value() - step_;
                updateProperty(v);
            }
            
        });
        
        setValue((max+min)/2);
    }
    
    
    public boolean isOk(double v) {
        return (min_ <= v && v<=max_);
    }
    
    public void setEnabled(boolean flag) {
        super.setEnabled(flag);
        Control[] cmps = getChildren();
        for (int i = 0; i < cmps.length; i++) {
            cmps[i].setEnabled(flag);
        }
    }
   
    private double text2value(){
    	String s = text_.getText();
    	double v = 0;
    	try {
            v = new SEDouble(s).doubleValue();
        } catch (Exception e) {
            v = value_.doubleValue();
        }
        return v;
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
    
    public void setValue(double v){
    	value_.setValue(new Double(v));
    	text_.setText(value_.toString());
    }
    
    public double getValue(){
    	return value_.doubleValue();
    }
}

package com.generalrobotix.ui.view.graph;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class SEDoubleTextWithSpinForSWT extends Composite{
    private SEDouble value_;
    private double max_;
    private double min_;
    private double step_;
    private Text text_;
    
    
    public SEDoubleTextWithSpinForSWT (Composite parent,int style,double min,double max,double step) {
        super(parent,style);
        value_ = new SEDouble(0);
        
        max_ = max;
        min_ = min;
        step_ = step;
        
        setLayout(new GridLayout(2,false));
        
        text_ = new Text(this,SWT.BORDER);
        
        text_.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                setValue(text_.getText());
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
                setValue(text_.getText());
                double v = value_.doubleValue() + step_;
                setValue(v);
            }
            
        });
        Button buttonDOWN = new Button(buttonPane,SWT.ARROW | SWT.DOWN);
        buttonDOWN.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                setValue(text_.getText());
                double v = value_.doubleValue() - step_;
                setValue(v);
            }
            
        });

        setValue((max+min)/2);
    }

    
    public SEDouble getValue() {
        setValue(text_.getText());
        return value_;
    }
    
    public void setValue(String s) {
        double v = new SEDouble(s).doubleValue();
        setValue(v);
    }
    
    public void setValue(double v) {
        if (isOk(v)) {
            value_.setValue(new Double(v));
        }

        text_.setText(value_.toString());
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

}

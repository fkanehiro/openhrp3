package com.generalrobotix.ui.view.graph;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Display;

public class SEDoubleTextWithSpinForSWT extends Composite{
    private SEDouble value_;
    private double max_;
    private double min_;
    private double step_;
    
    private Composite parent_;
    private Text text_;
    private Button buttonUP_;
    private Button buttonDOWN_;
    
    private int mouseBtnRepeat_ = 200;
    private boolean isMouseOverBtnUp_ = false;
    private boolean isMouseOverBtnDown_ = false;
    private Runnable incrementRun_;
    private Runnable decrementRun_;
    
    public SEDoubleTextWithSpinForSWT (Composite parent,int style,double min,double max,double step) {
        super(parent,style);
        parent_ = parent;
        value_ = new SEDouble(0);
        
        max_ = max;
        min_ = min;
        step_ = step;
        
        GridLayout gl =new GridLayout(2,false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        setLayout(gl);
        
        text_ = new Text(this,SWT.SINGLE | SWT.BORDER);
        text_.addFocusListener(new FocusListener(){
            public void focusLost(FocusEvent e){
                setValue(text_.getText());
                updateValue();
            }
            public void focusGained(FocusEvent e){
            }
        });
        text_.addVerifyListener(new VerifyListener(){
            public void verifyText(VerifyEvent e){
                // 最大値が無限大のときは無限大文字列は受け取る
                if(max_ == Double.POSITIVE_INFINITY)
                {
                    if(e.text.equals(new SEDouble(Double.POSITIVE_INFINITY).toString()))
                        return;
                }

                for(int i = 0; i < e.text.length(); i++){
                    char c = e.text.charAt(i);
                    if(!(('0' <= c && c <= '9') ||
                         c == '.' ||
                         c == '-' ||
                         c == 'e' || c == 'E'))
                    {
                        e.doit = false;
                    }
                }
            }
        });

        GridData gridData = new GridData();
        //gridData.verticalSpan = 2;
        gridData.widthHint = 80;
        text_.setLayoutData(gridData);
        
        Composite tmp = new Composite(this, SWT.NULL);
        GridLayout glt =new GridLayout(1,false);
        glt.horizontalSpacing = 0;
        glt.verticalSpacing = 0;
        tmp.setLayout(glt); 
        GridData tmpGrid = new GridData();
        tmpGrid.verticalAlignment = SWT.FILL;
        tmp.setLayoutData(tmpGrid);
        
        buttonUP_ = new Button(tmp,SWT.ARROW | SWT.UP);
        buttonUP_.addMouseTrackListener(new MouseTrackListener(){
            public void mouseEnter(MouseEvent e){
                isMouseOverBtnUp_ = true;
            }
            public void mouseExit(MouseEvent e){
                isMouseOverBtnUp_ = false;
            }
            public void mouseHover(MouseEvent e){
            }
        });

        buttonUP_.addMouseMoveListener(new MouseMoveListener(){
            public void mouseMove(MouseEvent e){
                isMouseOverBtnUp_ = (0 <= e.x && e.x < buttonUP_.getSize().x && 0 <= e.y && e.y < buttonUP_.getSize().y);
            }
        });
        buttonUP_.addMouseListener(new MouseListener(){
            public void mouseDoubleClick(MouseEvent e){
            }
            public void mouseDown(MouseEvent e) {
                if(e.button == 1){
                    incrementValue();
                    startIncrementTimer();
                }
            }
            public void mouseUp(MouseEvent e) {
                if(e.button == 1){
                    stopIncrementTimer();
                }
            }
        });
        
        buttonDOWN_ = new Button(tmp,SWT.ARROW | SWT.DOWN);
        buttonDOWN_.addMouseTrackListener(new MouseTrackListener(){
            public void mouseEnter(MouseEvent e){
                isMouseOverBtnDown_ = true;
            }
            public void mouseExit(MouseEvent e){
                isMouseOverBtnDown_ = false;
            }
            public void mouseHover(MouseEvent e){
            }
        });

        buttonDOWN_.addMouseMoveListener(new MouseMoveListener(){
            public void mouseMove(MouseEvent e){
                isMouseOverBtnDown_ = (0 <= e.x && e.x < buttonDOWN_.getSize().x && 0 <= e.y && e.y < buttonDOWN_.getSize().y);
            }
        });
        buttonDOWN_.addMouseListener(new MouseListener(){
            public void mouseDoubleClick(MouseEvent e){
            }
            public void mouseDown(MouseEvent e) {
                if(e.button == 1){
                    decrementValue();
                    startDecrementTimer();
                }
            }
            public void mouseUp(MouseEvent e) {
                if(e.button == 1){
                    stopDecrementTimer();
                }
            }
        });
        
        text_.addControlListener(new ControlListener(){
            public void controlMoved(ControlEvent e) {
            } 
            public void controlResized(ControlEvent e) {
                GridData buttonGrid = new GridData();
                buttonGrid.heightHint= text_.getSize().y/2;
                buttonUP_.setLayoutData(buttonGrid);
                GridData buttonDownGrid = new GridData();
                buttonDownGrid.heightHint= text_.getSize().y/2;
                buttonDOWN_.setLayoutData(buttonDownGrid);
                parent_.layout();
                //layout();
            }
        });
        
        incrementRun_ = new Runnable() {
            public void run() { 
                Display display = text_.getDisplay();
                if (!display.isDisposed())
                {
                    if(isMouseOverBtnUp_)
                        incrementValue();
                    display.timerExec(mouseBtnRepeat_, this);
                }
            }
        };
        decrementRun_ = new Runnable() {
            public void run() { 
                Display display = text_.getDisplay();
                if (!display.isDisposed())
                {
                    if(isMouseOverBtnDown_)
                        decrementValue();
                    display.timerExec(mouseBtnRepeat_, this);
                }
            }
        };
        
        setValue((max+min)/2);
    }


    protected double text2value(){
        String s = text_.getText();
        double v = 0;
        try {
            v = new SEDouble(s).doubleValue();
        } catch (Exception e) {
            v = value_.doubleValue();
        }
        return v;
    }
    
    public SEDouble getValue() {
        return value_;
    }
    public double getValueDouble() {
        return value_.doubleValue();
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

    private void incrementValue(){
        double v = text2value() + step_;
        setValue(v);
        updateValue();
    }
    private void decrementValue(){
        double v = text2value() - step_;
        setValue(v);
        updateValue();
    }
    private void startIncrementTimer(){
        Display display = text_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(mouseBtnRepeat_, incrementRun_);
        }
    }
    private void stopIncrementTimer(){
        Display display = text_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(-1, incrementRun_);
        }
    }
    private void startDecrementTimer(){
        Display display = text_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(mouseBtnRepeat_, decrementRun_);
        }
    }
    private void stopDecrementTimer(){
        Display display = text_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(-1, decrementRun_);
        }
    }

    // 値が変更されたときに呼び出される。
    protected void updateValue(){
    }
}

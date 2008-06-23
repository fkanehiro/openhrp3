/**
 * SpinControl.java
 *
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */
package com.generalrobotix.ui.view.graph;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import javax.swing.plaf.basic.BasicArrowButton;

public class SpinControl extends JPanel {
    private final static int BUTTON_UP =0;
    private final static int BUTTON_DOWN =1;
    private int sleepTime_ = 100;
    private int sleepTimeFirst_  = 400;
    private int sleepMin_  = 6;
    JButton btnUp_;
    JButton btnDown_;
    boolean stopFlag_;
    private java.util.List<SpinListener> listenerList_;

    public static JComponent getComponentWithSpin(
        JComponent component,
        SpinControl spin
    ) {
        JPanel panel = new JPanel() {   
            //オーバーライト
            public void setEnabled(boolean flag) {
                super.setEnabled(flag);
                Component[] cmps = getComponents();
                for(int i = 0; i < cmps.length; i ++) {
                    cmps[i].setEnabled(flag);
                }
            }
        };

        panel.setLayout(new BorderLayout());
        panel.add(BorderLayout.CENTER,component);
        panel.add(BorderLayout.EAST, spin);
        return panel;
    }

    public SpinControl(SpinListener listener) {
        this();
        addSpinListener(listener);
    }

    public SpinControl() {
        listenerList_ = new ArrayList<SpinListener>();
        
        setLayout(new GridLayout(2,1));
        btnUp_ = new BasicArrowButton(BasicArrowButton.NORTH);
        btnDown_ = new BasicArrowButton(BasicArrowButton.SOUTH);
        
        btnUp_.addMouseListener(new MyListener(BUTTON_UP));
        btnDown_.addMouseListener(new MyListener(BUTTON_DOWN));
        add(btnUp_);
        add(btnDown_);
    }

    public void setEnabled(boolean flag) {
        super.setEnabled(flag);
        btnUp_.setEnabled(flag);
        btnDown_.setEnabled(flag);
    }

    public void addSpinListener(SpinListener listener) {
        listenerList_.add(listener);
    }
    
    public void removeSpinListener(SpinListener listener) {
        listenerList_.remove(listener);
    }
    
    
    private class MyListener extends MouseAdapter {
        private int button_;
        private Timer timer_ = null;
        public MyListener(int button) {
            button_ = button;
            timer_ = null;//ダミー
        }

        public void mousePressed(MouseEvent e) {
            if (!isEnabled()) { return; }

            if (e.isAltDown() || e.isMetaDown()) { return; }

            stopFlag_ = false;

            //1回目は、押しただけで起こる。
            _broadCast(button_);
            
            ActionListener listener = new ActionListener() {
                private int allCount_ = 0;
                private int ikiCount_ = 0;

                //1回目に1/2の処理が入るので初期値は２倍
                private int sleep_ = sleepTime_ * 2;
                private int iki_ = 10 / 2;//1/2倍
                public void actionPerformed(ActionEvent e) {
                    if(ikiCount_  % iki_ ==0){
                        sleep_ = sleep_ / 2;
                        if(sleep_ >= sleepMin_){
                            iki_=iki_*2;
                            ikiCount_ = 0 ;
                            timer_.setDelay(sleep_);
                            //timer_.setInitialDelay(sleep_);
                            //timer_.restart();
                            //System.out.println("sleep_ = " + sleep_);
                        }
                    }
                    allCount_++;
                    ikiCount_++;
                    _broadCast(button_);
                }
            };
            timer_ = new Timer(sleepTimeFirst_,listener);
            timer_.setInitialDelay(sleepTimeFirst_);
            timer_.setCoalesce(true);
            timer_.start();
        }

        public void mouseReleased(MouseEvent e) {
            if (timer_ != null) {
                timer_.stop();
            }
        }
    }

    private synchronized void _broadCast(int button) {
        //System.out.print("" + button);
        for (int i = 0; i < listenerList_.size(); i ++) {
            SpinListener listener = (SpinListener)listenerList_.get(i);
            switch (button) {
            case BUTTON_UP:
                listener.up();
                break;
            case BUTTON_DOWN:
                listener.down();
                break;
            }
        }
    }
}

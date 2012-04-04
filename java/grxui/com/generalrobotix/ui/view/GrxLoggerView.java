// -*- indent-tabs-mode: nil; tab-width: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/*
 *  GrxLoggerView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxTimeSeriesItem;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxLoggerView extends GrxBaseView {
    public static final String TITLE = "Logger";

	private GrxTimeSeriesItem currentItem_;

	private static ImageIcon iconPlay_ = new ImageIcon(GrxLoggerView.class.getResource("/resources/images/playback.png"));
	private static ImageIcon iconStop_ = new ImageIcon(GrxLoggerView.class.getResource("/resources/images/pause.png"));
	private static ImageIcon iconFFwd_ = new ImageIcon(GrxLoggerView.class.getResource("/resources/images/fastfwd.png"));
	private static ImageIcon iconFRwd_ = new ImageIcon(GrxLoggerView.class.getResource("/resources/images/fastrwd.png"));
	private static ImageIcon iconSFwd_ = new ImageIcon(GrxLoggerView.class.getResource("/resources/images/slowfwd.png"));
	private static ImageIcon iconSRwd_ = new ImageIcon(GrxLoggerView.class.getResource("/resources/images/slowrwd.png"));
	
	private JButton btnFFwd_ = new JButton(iconFFwd_);
	private JButton btnSFwd_ = new JButton(iconSFwd_);
	private JButton btnPlay_ = new JButton(iconPlay_);
	private JButton btnPause_ = new JButton(iconStop_);
	private JButton btnFRwd_ = new JButton(iconFRwd_);
	private JButton btnSRwd_ = new JButton(iconSRwd_);
	private AbstractButton[] btns_ = new AbstractButton[]{ 
		btnFRwd_, btnSRwd_, 
		btnPause_, btnPlay_, 
		btnSFwd_, btnFFwd_
	};
	private String[] btnToolTips = new String[] {
		"fast-rwd x-2...","slow-rwd x -1/8 (1/2...)",
		"pause", "play", 
		"slow-fwd x 1/8 (1/2...)", "fast-fwd x 2..."
	};
			
	private JTextField tFldTime_ = new JTextField();
	private JLabel  lblPlayRate_ = new JLabel(FORMAT_FAST.format(1.0));
	private JSlider sliderTime_ = new JSlider(0, 0, 0);
	
	private JLabel  lblFrameRateM_ = new JLabel(String.format(FORMAT_FRATE, 10.0));
	private JLabel  lblFrameRate_  = new JLabel("10");
	private JSlider sliderFrameRate_ = new JSlider(1, 50, 10);

	private double current_ = 0;
	private double playRate_ = 1.0;
	private double interval_ = 100;
	private boolean isChanging_ = false;
	private boolean isPlaying_ = false;
    private boolean isControlDisabled_ = false; 
	
	private static DecimalFormat FORMAT_FAST  =  new DecimalFormat(" Play x ##; Play x-##");
	private static DecimalFormat FORMAT_SLOW  =  new DecimalFormat(" Play x 1/##; Play x-1/##");
	private static String FORMAT_FRATE = " FrameRate %2.0f/";
	private static String timeFormat   = "%8.3f"; //"###0.000"
	private static final Font MONO_PLAIN_12 = new Font("Monospaced", Font.PLAIN, 12);
	
	public GrxLoggerView(String name, GrxPluginManager manager) {
		super(name, manager);
		/*
		 * CREATE TOOLBAR 
		 */
		JToolBar toolbar = new JToolBar();
		
		btnPlay_.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				playRate_ = 1.0;
				play();
			}
		});
		
		btnSFwd_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!isPlaying_)
					playRate_ = 1.0/8.0;
				else if (playRate_ < 0)
					playRate_ *= -1;
				else if (1/playRate_ < 64)
					playRate_ *= 0.5;
				play();
			}
		});

		btnFFwd_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!isPlaying_)
					playRate_ = 2.0;
				else if (playRate_ < 0)
					playRate_ *= -1;
				else if (playRate_ < 64)
					playRate_ *= 2.0;
				play();
			}
		});

		btnSRwd_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!isPlaying_)
					playRate_ = -1.0/8.0;
				else if (playRate_ > 0)
					playRate_ *= -1;
				else if (1/playRate_ > -64)
					playRate_ *= 0.5;
				play();
			}
		});

		btnFRwd_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!isPlaying_)
					playRate_ = -2.0;
				else if (playRate_ > 0)
					playRate_ *= -1;
				else if (playRate_ > -64)
					playRate_ *= 2.0;
					
				play();
			}
		});

		btnPause_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				pause();
			}
		});
		
		Dimension dim = new Dimension(90, 27);
		lblPlayRate_.setFont(MONO_PLAIN_12);
		lblPlayRate_.setMaximumSize(dim);
		lblPlayRate_.setPreferredSize(dim);
		lblPlayRate_.setHorizontalAlignment(JLabel.LEFT);

		dim = new Dimension(80, 22);
		tFldTime_.setFont(MONO_PLAIN_12);
		tFldTime_.setText(String.format(timeFormat,0.0));
		tFldTime_.setPreferredSize(dim);
		tFldTime_.setMaximumSize(dim);
		tFldTime_.setMinimumSize(dim);
		tFldTime_.setHorizontalAlignment(JTextField.CENTER);
		tFldTime_.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (currentItem_ == null) 
					return;
				KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
				if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0)) {
					Double targetTime = Double.parseDouble(tFldTime_.getText());
					Double after = 0.0;
					for (int i=0; i<currentItem_.getLogSize() ;i++) {
						Double before = after;
						after = (Double)currentItem_.getTime(i);
						if (before < targetTime && targetTime < after) {
							sliderTime_.setValue(Math.max(0,i));
							break;
						}	
					}
				}
			}
		});
		
		sliderTime_.setPaintTicks(true);
		sliderTime_.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (!isChanging_) {
					isChanging_ = true;
					current_ = sliderTime_.getValue();
				}
			}
		});
		
		dim = new Dimension(100, 20);
		lblFrameRateM_.setFont(MONO_PLAIN_12);
		lblFrameRateM_.setPreferredSize(dim);
		lblFrameRateM_.setMaximumSize(dim);
		lblFrameRateM_.setHorizontalAlignment(JLabel.LEFT);
		
		dim = new Dimension(30, 20);
		lblFrameRate_.setFont(MONO_PLAIN_12);
		lblFrameRate_.setPreferredSize(dim);
		lblFrameRate_.setMaximumSize(dim);
		lblFrameRate_.setHorizontalAlignment(JLabel.LEFT);
		
		dim = new Dimension(100,27);
		sliderFrameRate_.setMajorTickSpacing(10);
		sliderFrameRate_.setMinorTickSpacing(2);
		sliderFrameRate_.setPaintTicks(true);
		sliderFrameRate_.setPaintTrack(true);
		sliderFrameRate_.setPreferredSize(dim);
		sliderFrameRate_.setMaximumSize(dim);
		sliderFrameRate_.setValue((int)(1000.0/manager_.getDelay()));
		sliderFrameRate_.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!sliderFrameRate_.getValueIsAdjusting()) {
					int value = sliderFrameRate_.getValue();
					lblFrameRate_.setText(String.valueOf(value));
					manager_.setDelay(1000/value);
				}
			}
		});
		
		toolbar.addSeparator();
		for (int i=0; i<btns_.length; i++) {
			btns_[i].setPreferredSize(GrxBaseView.getDefaultButtonSize());
			btns_[i].setMaximumSize(GrxBaseView.getDefaultButtonSize());
			btns_[i].setToolTipText(btnToolTips[i]);
			toolbar.add(btns_[i]);
		}
		toolbar.add(lblPlayRate_);
		toolbar.addSeparator();
		toolbar.add(tFldTime_);
		toolbar.add(sliderTime_);
		toolbar.add(lblFrameRateM_);
		toolbar.add(lblFrameRate_);
		toolbar.add(sliderFrameRate_);
		setToolBar(toolbar);
	}

	private double stepTime_ = -1.0;
	public void play() {
		if (!isPlaying_) {
			current_ = sliderTime_.getValue();
			if (current_ == sliderTime_.getMaximum() && playRate_ > 0)
				current_ = 0;
			stepTime_ = currentItem_.getDbl("logTimeStep", -1.0)*1000;
			if (stepTime_ > 0)
				interval_ = Math.ceil((double)(manager_.getDelay())/stepTime_*1.1);
			else 
				interval_ = 1;
			isPlaying_ = true;
			lblPlayRate_.setForeground(Color.green);
		}
		
		if (Math.abs(playRate_)<1.0)
			lblPlayRate_.setText(FORMAT_SLOW.format(1/playRate_));
		else
			lblPlayRate_.setText(FORMAT_FAST.format(playRate_));
	}
	
	
	public void pause() {
		lblPlayRate_.setForeground(Color.black);
		isPlaying_ = false;
	}
	
	public boolean isPlaying() {
		return isPlaying_;
	}
	
	public void restoreProperties() {
		super.restoreProperties();
		
		timeFormat = getStr("timeFormat", "%8.3f");
		GrxDebugUtil.println("LoggerView: timeFormat = \""+timeFormat+"\"\n");
		
		boolean b = isTrue("showFrameRateSlider", true);
		lblFrameRateM_.setVisible(b);
		lblFrameRate_.setVisible(b);
		sliderFrameRate_.setVisible(b);
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		if (!itemList.contains(currentItem_)) {
			currentItem_ = null;
			Iterator<GrxBaseItem> it = itemList.iterator();
			while (it.hasNext()) {
				GrxBaseItem i = it.next();
				if (i instanceof GrxTimeSeriesItem) {
					currentItem_ = (GrxTimeSeriesItem) i;
					break;
				}
			}
			if (currentItem_ == null) {
				sliderTime_.setValue(0);
				sliderTime_.setMaximum(0);
				tFldTime_.setText("NO ITEM");
				setEnabled(false);
				return;
			} else {
				double step = currentItem_.getDbl("logTimeStep", 0.001);
				sliderTime_.setMinorTickSpacing((int)(1.0/step));
				sliderTime_.setMajorTickSpacing((int)(5.0/step));
			}
		}
	}

	public void control(List<GrxBaseItem> itemList) {
	    if (lblFrameRateM_.isVisible())
	    	lblFrameRateM_.setText(String.format(FORMAT_FRATE, 1000/manager_.now));
	    
		if (currentItem_ == null) {
			return;
		}
		
		int sliderMax = sliderTime_.getMaximum();
		int loggerMax = Math.max(currentItem_.getLogSize()-1, 0);
		
		if (sliderMax != loggerMax) {
			isChanging_ = true;
			sliderTime_.setMaximum(loggerMax);
			if (sliderMax == 0 || sliderTime_.getValue() == sliderMax) 
				sliderTime_.setValue(loggerMax);
			sliderMax = loggerMax;
		}
			
		if (isPlaying_) {
			int before = (int) current_;
			current_ += interval_*playRate_;
			int after = (int)current_;
			
			if (sliderMax < after) {
				current_ = sliderMax;
				pause();
			} else if (current_ < 0) {
				current_ = 0;
				pause();
			} /*else {
				Double t1 = currentItem_.getTime(before);
				Double t2 = currentItem_.getTime(after);
				interval_ = Math.pow(manager_.now, 2) * playRate_ * Math.abs(playRate_)/(t2 - t1)*0.001;
			} */
			
			if (before != after) {
				isChanging_ = true;
				sliderTime_.setValue(after);
			}
		}
	
		if (isChanging_) {
			currentItem_.setPosition(sliderTime_.getValue());
			isChanging_ = false;
			setEnabled(sliderMax > 0);
		} else if (currentItem_.getPosition() != sliderTime_.getValue()){
			sliderTime_.setValue(currentItem_.getPosition());
		}
		_updateTimeField();
	}
	
	private void _updateTimeField() {
		Double t = currentItem_.getTime();
		if (t != null) {
			String str = String.format(timeFormat, t);
			if (!tFldTime_.getText().equals(str))
				tFldTime_.setText(str);

		} else if (currentItem_.getLogSize() == 0) {
			tFldTime_.setText("NO DATA");
			setEnabled(false);
		}
	}

	public int getMaximum() {
		return sliderTime_.getMaximum();
	}
	
	public int getCurrentPos() {
		return sliderTime_.getValue();
	}

    public double getPlayRate() {
        return playRate_;
    }
	
	public void setEnabled(boolean b) {
        if (isControlDisabled_) return;
		sliderTime_.setEnabled(b);
		tFldTime_.setEnabled(b);
		lblPlayRate_.setEnabled(b);
		for (int i=0; i<btns_.length; i++)
			btns_[i].setEnabled(b);
	}

    public void disableControl() {
        setEnabled(false);
        isControlDisabled_ = true;
    }

    public void enableControl() {
        isControlDisabled_ = false;
    }
}

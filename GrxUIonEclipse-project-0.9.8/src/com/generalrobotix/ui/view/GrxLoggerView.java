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

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxTimeSeriesItem;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
/**
 * @brief
 */
public class GrxLoggerView extends GrxBaseView {
    public static final String TITLE = "Logger";

	private GrxTimeSeriesItem currentItem_;

	private double current_ = 0;
	private double playRate_ = 1.0;
	private double interval_ = 100;
	private boolean isChanging_ = false;
	private boolean isPlaying_ = false;

	private Scale sliderFrameRate_;
	private Label  lblFrameRate_;

	
	private Scale sliderTime_;

	private Button btnFRwd_;
	private Button btnSRwd_;
	private Button btnPause_;
	private Button btnPlay_;
	private Button btnSFwd_;
	private Button btnFFwd_;
	private Button[] btns_ = new Button[6];

	private String[] btnToolTips = new String[] {
			"fast-rwd x-2...","slow-rwd x -1/8 (1/2...)",
			"pause", "play", 
			"slow-fwd x 1/8 (1/2...)", "fast-fwd x 2..."
		};
	private final static String iconFRwd_ = "fastrwd.png";
	private final static String iconSRwd_ = "slowrwd.png";
	private final static String iconStop_ = "pause.png";
	private final static String iconPlay_ = "playback.png";
	private final static String iconSFwd_ = "slowfwd.png";
	private final static String iconFFwd_ = "fastfwd.png";
	private final static String[] icons_ = { iconFRwd_, iconSRwd_, iconStop_, iconPlay_, iconSFwd_, iconFFwd_ };

	private Label  lblPlayRate_;
	
	private static DecimalFormat FORMAT_FAST  =  new DecimalFormat(" Play x ##; Play x-##");
	private static DecimalFormat FORMAT_SLOW  =  new DecimalFormat(" Play x 1/##; Play x-1/##");
	private static String timeFormat   = "%8.3f"; //"###0.000"

	//private static final Font MONO_PLAIN_12 = new Font("Monospaced", Font.PLAIN, 12);

	/**
	 * @brief
	 * @param name
	 * @param manager
	 * @param vp
	 * @param parent
	 */
	public GrxLoggerView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);

		GridLayout gl = new GridLayout(3,false);
		composite_.setLayout( gl );

		Composite btnComp = new Composite ( composite_, SWT.BORDER);
		GridLayout buttonLayout = new GridLayout(6,false);
		btnComp.setLayout( buttonLayout );

		btnFRwd_ = new Button( btnComp, SWT.NONE );
		btnFRwd_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
				if (!isPlaying_)
					playRate_ = -2.0;
				else if (playRate_ > 0)
					playRate_ *= -1;
				else if (playRate_ > -64)
					playRate_ *= 2.0;
				play();
            }
        });
		
		btnSRwd_ = new Button( btnComp, SWT.NONE );
		btnSRwd_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
				if (!isPlaying_)
					playRate_ = -1.0/8.0;
				else if (playRate_ > 0)
					playRate_ *= -1;
				else if (1/playRate_ > -64)
					playRate_ *= 0.5;
				play();
            }
        });

		btnPause_ = new Button( btnComp, SWT.NONE );
		btnPause_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
        		pause();
            }
        });

		btnPlay_ = new Button( btnComp, SWT.NONE );
		btnPlay_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
				playRate_ = 1.0;
				play();
            }
        });

		btnSFwd_ = new Button( btnComp, SWT.NONE );
		btnSFwd_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
				if (!isPlaying_)
					playRate_ = 1.0/8.0;
				else if (playRate_ < 0)
					playRate_ *= -1;
				else if (1/playRate_ < 64)
					playRate_ *= 0.5;
				play();
            }
        });

		btnFFwd_ = new Button( btnComp, SWT.NONE );
		btnFFwd_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
        		if (!isPlaying_)
					playRate_ = 2.0;
				else if (playRate_ < 0)
					playRate_ *= -1;
				else if (playRate_ < 64)
					playRate_ *= 2.0;
				play();
			}
        });

		btns_[0] = btnFRwd_;
		btns_[1] = btnSRwd_; 
		btns_[2] = btnPause_;
		btns_[3] = btnPlay_;
		btns_[4] = btnSFwd_;
		btns_[5] = btnFFwd_;

		for(int i=0; i<6; i++) {
			btns_[i].setImage( Activator.getDefault().getImage( icons_[i] ) );
			btns_[i].setToolTipText( btnToolTips[i] );
		}

		
		lblFrameRate_ = new Label(composite_, SWT.NONE);
		lblFrameRate_.setText("FPS:"+10);
		
		// フレームレート変更スライダ
		sliderFrameRate_ = new Scale( composite_, SWT.HORIZONTAL );
		sliderFrameRate_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
				int value = sliderFrameRate_.getSelection();
				lblFrameRate_.setText( "FPS:"+String.valueOf(value) );
				manager_.setDelay(1000/value);
            }
		} );
		//sliderFrameRate_.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sliderFrameRate_.setSize(100, 27);
		sliderFrameRate_.setMaximum(50);
		sliderFrameRate_.setMinimum(1);
		sliderFrameRate_.setSelection(10);

		//時間変更スライダ
		sliderTime_ = new Scale( composite_, SWT.HORIZONTAL );
		sliderTime_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
        		if (!isChanging_) {
        			isChanging_ = true;
        			current_ = sliderTime_.getSelection();
        		}
            }
		} );
		//sliderTime_.setMaximum(1);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3; // 三つ分のスペースを使う
		sliderTime_.setLayoutData(gd);
		
		lblPlayRate_ = new Label( composite_, SWT.NONE);
		setScrollMinSize();
	}

	private double stepTime_ = -1.0;
	
	/**
	 * @brief
	 */
	public void play() {
		if (!isPlaying_) {
			//current_ = sliderTime_.getValue();
			current_ = sliderTime_.getSelection();
			if (current_ == sliderTime_.getMaximum() && playRate_ > 0)
				current_ = 0;
			stepTime_ = currentItem_.getDbl("logTimeStep", -1.0)*1000;
			if (stepTime_ > 0)
				interval_ = Math.ceil((double)(manager_.getDelay())/stepTime_*1.1);
			else 
				interval_ = 1;
			isPlaying_ = true;
//			lblPlayRate_.setForeground(Color.green);
		}

		if (Math.abs(playRate_)<1.0)
			lblPlayRate_.setText(FORMAT_SLOW.format(1/playRate_));
		else
			lblPlayRate_.setText(FORMAT_FAST.format(playRate_));
	}
	
	private double prevTime_ = 0.0;
	private double playRateLogTime_ = -1;
	
	/**
	 * @brief
	 * @param intervalMsec
	 */
	public void playLogTime(int intervalMsec) {
		if (!isPlaying_) {
			current_ = sliderTime_.getSelection();
			if (current_ == sliderTime_.getMaximum() && playRate_ > 0)
				current_ = 0;
			
			playRateLogTime_ = (double)intervalMsec/1000.0;
			prevTime_ = 0.0;
			isPlaying_ = true;
		}
	}

	/**
	 * @brief
	 */
	public void pause() {
		isPlaying_ = false;
		playRateLogTime_ = 0;
	}
	
	/**
	 * @brief
	 * @return
	 */
	public boolean isPlaying() {
		return isPlaying_;
	}
	
	/**
	 * @brief
	 */
	public void restoreProperties() {
		super.restoreProperties();
		
		timeFormat = getStr("timeFormat", "%8.3f");
		GrxDebugUtil.println("LoggerView: timeFormat = \""+timeFormat+"\"\n");
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
				sliderTime_.setSelection(0);
				sliderTime_.setMaximum(0);
				setEnabled(false);
				return;
			}
		}
	}

	public void control(List<GrxBaseItem> itemList) {
	    
		if (currentItem_ == null) {
			setEnabled(false);
			return;
		}
		
		int sliderMax = sliderTime_.getMaximum();
		int loggerMax = Math.max(currentItem_.getLogSize()-1, 0);
		
		if (playRateLogTime_ > 0) {
			//for (int p=sliderTime_.getValue();p < sliderMax; p++) {
			for (int p=sliderTime_.getSelection();p < sliderMax; p++) {
				double time = currentItem_.getTime(p);
				if (time - prevTime_ >= playRateLogTime_) {
					prevTime_ = time;
					//sliderTime_.setValue(p);
					sliderTime_.setSelection(p);
					currentItem_.setPosition(p);
					_updateTimeField();
					return;
				}
			}
			pause();
			return;
		} 
		
		if (sliderMax != loggerMax) {
			isChanging_ = true;
			sliderTime_.setMaximum(loggerMax);
//			if (sliderMax == 0 || sliderTime_.getValue() == sliderMax) 
	//			sliderTime_.setValue(loggerMax);
			if (sliderMax == 0 || sliderTime_.getSelection() == sliderMax || sliderTime_.getSelection() == 0 ) 
				sliderTime_.setSelection(loggerMax);
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
				//sliderTime_.setValue(after);
				sliderTime_.setSelection(after);
			}
		}
	
		if (isChanging_) {
			//currentItem_.setPosition(sliderTime_.getValue());
			currentItem_.setPosition(sliderTime_.getSelection());
			isChanging_ = false;
			setEnabled(sliderMax > 0);
//		} else if (currentItem_.getPosition() != sliderTime_.getValue()){
		} else if (currentItem_.getPosition() != sliderTime_.getSelection()){
			//sliderTime_.setValue(currentItem_.getPosition());
			sliderTime_.setSelection(currentItem_.getPosition());
		}
		_updateTimeField();
	}
	
	private void _updateTimeField() {
		Double t = currentItem_.getTime();
		if (t != null) {
//			tFldTime_.setText(String.format(timeFormat, t));
		} else if (currentItem_.getLogSize() == 0){
	//		tFldTime_.setText("NO DATA");
			setEnabled(false);
		}
	}

	public int getMaximum() {
		return sliderTime_.getMaximum();
	}
	
	public int getCurrentPos() {
		//return sliderTime_.getValue();
		return sliderTime_.getSelection();
	}

    public double getPlayRate() {
        return playRate_;
    }
	
	public void setEnabled(boolean b) {
		sliderTime_.setEnabled(b);
//		tFldTime_.setEnabled(b);
		lblPlayRate_.setEnabled(b);
		for (int i=0; i<btns_.length; i++)
			btns_[i].setEnabled(b);
	}
}

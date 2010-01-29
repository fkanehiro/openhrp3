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

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.jface.action.Action;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial") //$NON-NLS-1$
/**
 * @brief
 */
public class GrxLoggerView extends GrxBaseView {
    public static final String TITLE = "Logger"; //$NON-NLS-1$

	private GrxWorldStateItem currentItem_;

	private int current_ = 0;    // current position
	private double playRate_ = 1.0; // playback rate
	private int frameRate_ = maxFrameRate_;
	private boolean isPlaying_ = false;
    private boolean isControlDisabled_ = false; 
    private boolean inSimulation_ = false;
		
    // widgets
	private Scale sliderFrameRate_;
	private Label  lblFrameRate_;
	private Scale sliderTime_;
    

	private Button btnFrameF_;
	private Button btnFrameR_;
	private Button[] btns_ = new Button[2];

	private String[] btnToolTips = new String[] {
			MessageBundle.get("GrxLoggerView.text.oneRwd"), MessageBundle.get("GrxLoggerView.text.onePlay")  //$NON-NLS-1$ //$NON-NLS-2$
		};

	private final static String[] icons_ = { "icon_frame-.png", "icon_frame+.png" };//$NON-NLS-1$ //$NON-NLS-2$

    private Text tFldTime_;
    
    private Action[] toolActs_ = new Action[6];
    private String[] actToolTips_ = new String[] {
        MessageBundle.get("GrxLoggerView.text.fastRwd"),MessageBundle.get("GrxLoggerView.text.slowRwd"), //$NON-NLS-1$ //$NON-NLS-2$
        MessageBundle.get("GrxLoggerView.text.pause"), MessageBundle.get("GrxLoggerView.text.play"), //$NON-NLS-1$ //$NON-NLS-2$
        MessageBundle.get("GrxLoggerView.text.slowFwd"), MessageBundle.get("GrxLoggerView.text.fastFwd") //$NON-NLS-1$ //$NON-NLS-2$
        };
    private final static String[] actIcons_ = {
        "icon_fastrwd.png", //$NON-NLS-1$
        "icon_slowrwd.png", //$NON-NLS-1$
        "icon_pause.png", //$NON-NLS-1$
        "icon_playback.png", //$NON-NLS-1$
        "icon_slowfwd.png", //$NON-NLS-1$
        "icon_fastfwd.png" }; //$NON-NLS-1$
    private final static String[] actDIcons_ = {
        "icond_fastrwd.png", //$NON-NLS-1$
        "icond_slowrwd.png", //$NON-NLS-1$
        "icond_pause.png", //$NON-NLS-1$
        "icond_playback.png", //$NON-NLS-1$
        "icond_slowfwd.png", //$NON-NLS-1$
        "icond_fastfwd.png" }; //$NON-NLS-1$

    private final static int    maxFrameRate_ = 50;
	private Label  lblPlayRate_;
	
	private static DecimalFormat FORMAT_FAST  =  new DecimalFormat("Play x ##;Play x-##"); //$NON-NLS-1$
	private static DecimalFormat FORMAT_SLOW  =  new DecimalFormat("Play x 1/##;Play x-1/##"); //$NON-NLS-1$
	private static String timeFormat   = "%8.3f"; //"###0.000" //$NON-NLS-1$

	//private static final Font MONO_PLAIN_12 = new Font("Monospaced", Font.PLAIN, 12);
	
    private int mouseBtnActionNum = 0;
    private int mouseBtnRepeat_ = 200;
    private int mouseBtnAccelNeed_ = 5;
    private int mouseBtnAccelRepeat_ = 30;
    private boolean isMouseOverBtnR_ = false;
    private boolean isMouseOverBtnF_ = false;
    private Runnable backPosRun_;
    private Runnable forwardPosRun_;
    
	/**
	 * @brief
	 * @param name
	 * @param manager
	 * @param vp
	 * @param parent
	 */
	public GrxLoggerView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);

		GridLayout gl = new GridLayout(6,false);
		gl.marginHeight = 0;
		composite_.setLayout( gl );
		
		// playback rate label
		lblPlayRate_ = new Label( composite_, SWT.NONE);
		lblPlayRate_.setText(MessageBundle.get("GrxLoggerView.label.pause")); //$NON-NLS-1$
		GridData lblGridData = new GridData();
        lblGridData.widthHint = 80;
        lblPlayRate_.setLayoutData(lblGridData);


        // playback controller
		Composite btnComp = new Composite ( composite_, SWT.NONE);
		GridLayout buttonLayout = new GridLayout(2,false);
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		btnComp.setLayout( buttonLayout );

        btnFrameR_ = new Button( btnComp, SWT.NONE );
        
        btnFrameR_.addMouseTrackListener(new MouseTrackListener(){
            public void mouseEnter(MouseEvent e){
                isMouseOverBtnR_ = true;
            }
            public void mouseExit(MouseEvent e){
                isMouseOverBtnR_ = false;
            }
            public void mouseHover(MouseEvent e){
            }
        });

        btnFrameR_.addMouseMoveListener(new MouseMoveListener(){
            public void mouseMove(MouseEvent e){
                isMouseOverBtnR_ = (0 <= e.x && e.x < btnFrameR_.getSize().x && 0 <= e.y && e.y < btnFrameR_.getSize().y);
            }
        });
        btnFrameR_.addMouseListener(new MouseListener(){
            public void mouseDoubleClick(MouseEvent e){
            }
            public void mouseDown(MouseEvent e) {
                if(e.button == 1){
                    backPosition();
                    startBackPosTimer();
                }
            }
            public void mouseUp(MouseEvent e) {
                if(e.button == 1){
                    stopBackPosTimer();
                }
            }
        });

		btnFrameF_ = new Button( btnComp, SWT.NONE );
        
        btnFrameF_.addMouseTrackListener(new MouseTrackListener(){
            public void mouseEnter(MouseEvent e){
                isMouseOverBtnF_ = true;
            }
            public void mouseExit(MouseEvent e){
                isMouseOverBtnF_ = false;
            }
            public void mouseHover(MouseEvent e){
            }
        });

        btnFrameF_.addMouseMoveListener(new MouseMoveListener(){
            public void mouseMove(MouseEvent e){
                isMouseOverBtnF_ = (0 <= e.x && e.x < btnFrameF_.getSize().x && 0 <= e.y && e.y < btnFrameF_.getSize().y);
            }
        });
        btnFrameF_.addMouseListener(new MouseListener(){
            public void mouseDoubleClick(MouseEvent e){
            }
            public void mouseDown(MouseEvent e) {
                if(e.button == 1){
                	forwardPosition();
                    startForwardPosTimer();
                }
            }
            public void mouseUp(MouseEvent e) {
                if(e.button == 1){
                    stopForwardPosTimer();
                }
            }
        });

		btns_[0] = btnFrameR_;
		btns_[1] = btnFrameF_;

		for(int i=0; i<btns_.length; i++) {
			btns_[i].setImage( Activator.getDefault().getImage( icons_[i] ) );
			btns_[i].setToolTipText( btnToolTips[i] );
		}

		// text field to display current time
		tFldTime_ = new Text(composite_, SWT.SINGLE|SWT.BORDER);
        tFldTime_.setText(MessageBundle.get("GrxLoggerView.text.noData")); //$NON-NLS-1$
        GridData textGridData = new GridData();
        textGridData.widthHint = 60;
        tFldTime_.setLayoutData(textGridData);
        tFldTime_.addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.CR) {
            		String str = tFldTime_.getText();
            		try{
            			double tm = Double.parseDouble(str);
            			int newpos = currentItem_.getPositionAt(tm);
            			if (newpos >= 0){
            				currentItem_.setPosition(newpos);
            			}
            		}catch(Exception ex){
            			
            		}
                }
            }
        });

		//時間変更スライダ
		sliderTime_ = new Scale( composite_, SWT.NONE );
		sliderTime_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
            	currentItem_.setPosition(sliderTime_.getSelection());
            }
		} );
		sliderTime_.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_DOWN){
			        if (isPlaying_)
			            pause();
					currentItem_.setPosition(sliderTime_.getSelection());
				}else if (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_UP){
			        if (isPlaying_)
			            pause();
					currentItem_.setPosition(sliderTime_.getSelection());
				}
			}
			public void keyReleased(KeyEvent e) {
			}
		});
		GridData gd = new GridData(SWT.HORIZONTAL|SWT.FILL|GridData.FILL_HORIZONTAL);
		sliderTime_.setLayoutData(gd);
		
		// frame rate
		lblFrameRate_ = new Label(composite_, SWT.NONE);
		lblFrameRate_.setText(MessageBundle.get("GrxLoggerView.label.FPS")+maxFrameRate_); //$NON-NLS-1$
		
		// フレームレート変更スライダ
		sliderFrameRate_ = new Scale( composite_, SWT.HORIZONTAL );
		//sliderFrameRate_.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sliderFrameRate_.setSize(80, 27);
		sliderFrameRate_.setMaximum(maxFrameRate_);
		sliderFrameRate_.setMinimum(1);
		sliderFrameRate_.setSelection(maxFrameRate_);
        sliderFrameRate_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
                frameRate_ = sliderFrameRate_.getSelection();
                lblFrameRate_.setText( MessageBundle.get("GrxLoggerView.label.FPS")+String.valueOf(frameRate_) ); //$NON-NLS-1$
            }
        } );

        Action actFRwd_ = new Action(){
            public void run(){
                if (!isPlaying_)
                    playRate_ = -2.0;
                else if (playRate_ > 0)
                    playRate_ *= -1;
                else if (playRate_ > -64)
                    playRate_ *= 2.0;
                play();
            }
        };
        
        Action actSRwd_ = new Action(){
            public void run(){
                if (!isPlaying_)
                    playRate_ = -1.0/8.0;
                else if (playRate_ > 0)
                    playRate_ *= -1;
                else if (1/playRate_ > -64)
                    playRate_ *= 0.5;
                play();
            }
        };
        Action actPause_ = new Action(){
            public void run(){
                pause();
            }
        };

        Action actPlay_ = new Action(){
            public void run(){
                playRate_ = 1.0;
                play();
            }
        };

        Action actSFwd_ = new Action(){
            public void run(){
                if (!isPlaying_)
                    playRate_ = 1.0/8.0;
                else if (playRate_ < 0)
                    playRate_ *= -1;
                else if (1/playRate_ < 64)
                    playRate_ *= 0.5;
                play();
            }
        };

        Action actFFwd_ = new Action(){
            public void run(){
                if (!isPlaying_)
                    playRate_ = 2.0;
                else if (playRate_ < 0)
                    playRate_ *= -1;
                else if (playRate_ < 64)
                    playRate_ *= 2.0;
                play();
            }
        };

        toolActs_[0] = actFRwd_;
        toolActs_[1] = actSRwd_; 
        toolActs_[2] = actPause_;
        toolActs_[3] = actPlay_;
        toolActs_[4] = actSFwd_;
        toolActs_[5] = actFFwd_;
        IActionBars bars = vp.getViewSite().getActionBars();
        for(int i = 0; i < toolActs_.length; i++) {
            toolActs_[i].setImageDescriptor( Activator.getDefault().getDescriptor(actIcons_[i]) );
            toolActs_[i].setDisabledImageDescriptor(Activator.getDefault().getDescriptor(actDIcons_[i]));
            toolActs_[i].setToolTipText( actToolTips_[i] );
            bars.getToolBarManager().add(toolActs_[i]);
        }

        
        backPosRun_ = new Runnable() {
            public void run() { 
                Display display = btnFrameR_.getDisplay();
                if (!display.isDisposed())
                {
                    if(isMouseOverBtnR_)
                    {
                        mouseBtnActionNum += 1;
                        backPosition();
                    }
                    display.timerExec((mouseBtnActionNum < mouseBtnAccelNeed_ ? mouseBtnRepeat_ : mouseBtnAccelRepeat_), this);
                }
            }
        };
        forwardPosRun_ = new Runnable() {
            public void run() { 
                Display display = btnFrameF_.getDisplay();
                if (!display.isDisposed())
                {
                    if(isMouseOverBtnF_)
                    {
                        mouseBtnActionNum += 1;
                        forwardPosition();
                    }
                    display.timerExec((mouseBtnActionNum < mouseBtnAccelNeed_ ? mouseBtnRepeat_ : mouseBtnAccelRepeat_), this);
                }
            }
        };

		setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
		
		currentItem_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
		if(currentItem_!=null){
			_setTimeSeriesItem(currentItem_);
			currentItem_.addObserver(this);
		}else
			_setTimeSeriesItem(null);
		manager_.registerItemChangeListener(this, GrxWorldStateItem.class);

    }

	private boolean _isAtTheEndAfterPlayback(){
		if (current_ == sliderTime_.getMaximum() && playRate_ > 0){
			return true;
		}else{
			return false;
		}
	}
	
	private void play() {
		if(currentItem_ == null) return;
		if (!isPlaying_) {
			if (_isAtTheEndAfterPlayback()) currentItem_.setPosition(0);
			final double stepTime = currentItem_.getDbl("logTimeStep", -1.0)*1000; //$NON-NLS-1$
			Runnable playRun_ = new Runnable() {
				public void run() {
					if(!isPlaying_) return;
					int sliderMax = currentItem_.getLogSize()-1;
					int interval =  (int)Math.ceil(1000.0d/frameRate_/stepTime*playRate_);
					if(interval==0) interval = (int) Math.signum(playRate_);
					int newpos = current_ + interval;
					boolean _continue = true;
					if ( sliderMax < newpos) {
						newpos = sliderMax;
						if(!inSimulation_)
							_continue = false;
					} else if (newpos < 0) {
						newpos = 0;
						_continue = false;
					} 
					currentItem_.setPosition(newpos);
					if(_continue){
						int sleepTime = (int)(stepTime*interval/playRate_);	
						Display display = composite_.getDisplay();
						if (!display.isDisposed())
							display.timerExec(sleepTime, this);
					}else
						pause();
				}
			};
			isPlaying_ = true;
			Display display = composite_.getDisplay();
			if (!display.isDisposed())
				display.timerExec(1, playRun_);
		}
		if (Math.abs(playRate_)<1.0)
			lblPlayRate_.setText(FORMAT_SLOW.format(1/playRate_));
		else
			lblPlayRate_.setText(FORMAT_FAST.format(playRate_));
	}
	
	/**
	 * @brief pause playback
	 */
	public void pause() {
		isPlaying_ = false;
		lblPlayRate_.setText(MessageBundle.get("GrxLoggerView.label.pause")); //$NON-NLS-1$
	}
	
	/**
	 * @brief check where playing now or not
	 * @return true if playing, false otherwise
	 */
	public boolean isPlaying() {
		return isPlaying_;
	}
	
	/**
	 * @brief
	 */
	public void restoreProperties() {
		super.restoreProperties();
		
		timeFormat = getStr("timeFormat", "%8.3f"); //$NON-NLS-1$ //$NON-NLS-2$
		GrxDebugUtil.println("LoggerView: timeFormat = \""+timeFormat+"\"\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void _setTimeSeriesItem(GrxWorldStateItem item){
		current_ = 0;
		sliderTime_.setSelection(0);
		if (item != null){
			_updateTimeField(item);
			int size = item.getLogSize();
			if(size > 0){
				sliderTimeSetMaximam(item.getLogSize());
				setEnabled(true);
			}else{
				sliderTimeSetMaximam(1);
				setEnabled(false);
			}
		}else{
			sliderTimeSetMaximam(1);
			setEnabled(false);
		}
	}
	
	public void registerItemChange(GrxBaseItem item, int event){
		if(item instanceof GrxWorldStateItem){
			GrxWorldStateItem worldStateItem = (GrxWorldStateItem) item;
	    	switch(event){
	    	case GrxPluginManager.SELECTED_ITEM:
	    		if(currentItem_!=worldStateItem){
	    			_setTimeSeriesItem(worldStateItem);
	    			currentItem_ = worldStateItem;
	    			currentItem_.addObserver(this);
	    		}
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentItem_==worldStateItem){
    				currentItem_.deleteObserver(this);
	    			_setTimeSeriesItem(null);
	    			currentItem_ = null;
	    		}
	    		break;
	    	default:
	    		break;
	    	}
		}
	}
	
	/**
	 * update time field
	 */
	private void _updateTimeField(GrxWorldStateItem item) {
		Double t = item.getTime();
		if (t != null) {
			tFldTime_.setText(String.format(timeFormat, t));
			setEnabled(true);
		} else if (item.getLogSize() == 0){
			tFldTime_.setText(MessageBundle.get("GrxLoggerView.text.noData")); //$NON-NLS-1$
			setEnabled(false);
		}
	}

	/**
	 * get maximum position
	 * @return maximum position
	 */
	public int getMaximum() {
		return sliderTime_.getMaximum();
	}

	/**
	 * get current position of playback
	 * @return current position
	 */
	public int getCurrentPos() {
		return current_;
	}

	public void update(GrxBasePlugin plugin, Object... arg) {
		if(currentItem_!=plugin) return;
		if((String)arg[0]=="PositionChange"){ //$NON-NLS-1$
			int pos = ((Integer)arg[1]).intValue();
			int logSize = currentItem_.getLogSize();
			if ( pos < 0 || logSize < pos){
				return;
			}
			current_ = pos;
			sliderTimeSetMaximam(logSize-1);
			sliderTime_.setSelection(pos);
			_updateTimeField(currentItem_);
		}else if((String)arg[0]=="StartSimulation"){ //$NON-NLS-1$
			inSimulation_ = true; 
			lblPlayRate_.setText(MessageBundle.get("GrxLoggerView.label.live")); //$NON-NLS-1$
			if((Boolean)arg[1])
				disableControl();
			else
				play();
		}else if((String)arg[0]=="StopSimulation"){ //$NON-NLS-1$
			inSimulation_ = false;
			lblPlayRate_.setText(MessageBundle.get("GrxLoggerView.label.pause")); //$NON-NLS-1$
			enableControl();
			pause();
		}else if((String)arg[0]=="ClearLog"){ //$NON-NLS-1$
			_setTimeSeriesItem(currentItem_);
		}
	}

	/**
	 * get playback rate
	 * @return playback rate
	 */
    public double getPlayRate() {
        return playRate_;
    }
	
	private void setEnabled(boolean b) {
        if (isControlDisabled_) return;
		sliderTime_.setEnabled(b);
		tFldTime_.setEnabled(b);
		lblPlayRate_.setEnabled(b);
		for (int i=0; i<btns_.length; i++)
			btns_[i].setEnabled(b);

		for (int i=0; i<toolActs_.length; i++)
			toolActs_[i].setEnabled(b);

	}
	
    public void disableControl() {
        setEnabled(false);
        isControlDisabled_ = true;
    }
    
    public void enableControl() {
        isControlDisabled_ = false;
        setEnabled(true);
    }
    
    public void shutdown() {
        manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
        if(currentItem_!=null)
        	currentItem_.deleteObserver(this);
	}
    
    private void sliderTimeSetMaximam(int value)
    {
        sliderTime_.setMaximum(value);

        if(value < 150)
            sliderTime_.setPageIncrement(5);
        if(value < 1500)
            sliderTime_.setPageIncrement(50);
        else
            sliderTime_.setPageIncrement(500);
    }
    
    private void backPosition(){
        if (isPlaying_)
            pause();
        currentItem_.setPosition(sliderTime_.getSelection()-1);
    }
    private void forwardPosition(){
        if (isPlaying_)
            pause();
        currentItem_.setPosition(sliderTime_.getSelection()+1);
    }
    private void startBackPosTimer(){
        mouseBtnActionNum = 0;

        Display display = btnFrameR_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(mouseBtnRepeat_, backPosRun_);
        }
    }
    private void stopBackPosTimer(){
        mouseBtnActionNum = 0;

        Display display = btnFrameR_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(-1, backPosRun_);
        }
    }
    private void startForwardPosTimer(){
        mouseBtnActionNum = 0;

        Display display = btnFrameF_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(mouseBtnRepeat_, forwardPosRun_);
        }
    }
    private void stopForwardPosTimer(){
        mouseBtnActionNum = 0;

        Display display = btnFrameF_.getDisplay();
        if (!display.isDisposed())
        {
            display.timerExec(-1, forwardPosRun_);
        }
    }
}

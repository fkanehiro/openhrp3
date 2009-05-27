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
 *  GrxTimeSeriesItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.util.ArrayList;


/**
 * series of timed objects
 */
public abstract class GrxTimeSeriesItem extends GrxBaseItem {
	private ArrayList<TValue> log_ = new ArrayList<TValue>();
	private int maxLogSize_ = 1000000;
	private int currentPos_ = -1;
	private boolean bRemoved = false;
    protected int overPos_ = 0;
    protected int changePos_ = -1;
    
	public class TValue {
		private Double time;
        private Object value;
		TValue(Double t, Object v) {
			time = t;
			value = v;
		}
        public Double getTime(){
            return time;
        }
        public Object getValue(){
            return value;
        }
	}

	/**
	 * constructor
	 * @param name name of this item
	 * @param manager plugin manager
	 */
	public GrxTimeSeriesItem(String name, GrxPluginManager manager) {
		super(name, manager);
	}
	
	/**
	 * set position of pointer
	 * @param pos position
	 */
	public void setPosition(Integer pos) {
		if (0 <= pos && pos < log_.size()){
			currentPos_ = pos;
			notifyObservers("PositionChange",pos);
		}
	}

	/**
	 * get current position of pointer
	 * @return position
	 */
	public int getPosition() {
		return currentPos_;
	}
	
	/**
	 * get position of timed object which is nearest to the specified time.
	 * If there is no object in this series -1 is returned. 
	 */
	public int getPositionAt(Double t){
		if (getLogSize() == 0) return -1;
		int pos = 0;
		Double dt = Math.abs(t - getTime(pos));
		for (int i=1; i<getLogSize(); i++){
			Double new_dt = Math.abs(t - getTime(i));
			if (new_dt < dt){
				dt = new_dt;
				pos = i;
			}
		}
		return pos;
	}
	
	/**
	 * set time to current object
	 * @param pos position
	 * @param t time
	 */
	public void setTimeAt(int pos, Double t) {
		if (0 <= pos && pos < log_.size())
			log_.get(pos).time = t;
	}

	/**
	 * get time of current object
	 * @return time
	 */
	public Double getTime() {
		return getTime(currentPos_);
	}
	
	/**
	 * get time of object at specified position
	 * @param pos position of the object
	 * @return time
	 */
	public Double getTime(int pos) {
		if (0 <= pos && pos < log_.size())
			return log_.get(pos).time;
		return null;
	}
	
	/**
	 * add timed object to this series
	 * @param t time
	 * @param val object
	 */
	public void addValue(Double t, Object val) {
		TValue tv = new TValue(t,val);
		log_.add(tv);
		if (maxLogSize_ > 0 && log_.size() > maxLogSize_){
            log_.remove(0);
            bRemoved = true;
        }
	}
	
	/**
	 * get current object
	 * @return current object
	 */
	public Object getValue() {
		return getValue(currentPos_);
	}
	
	/**
	 * get an object at specified position
	 * 
	 * @param pos position
	 * @return object
	 */
	public Object getValue(int pos) {
		if (0 <= pos && pos < log_.size())
			return log_.get(pos).value;
		return null;
	}

	/**
	 * set maximum length of this series
	 * @param maxLogSize maximum length
	 */
	public final void setMaximumLogSize(int maxLogSize) {
		if (maxLogSize > 0) {
			maxLogSize_ = maxLogSize;
		}
	}

	/**
	 * get length of this series
	 * @return length
	 */
	public final int getLogSize() {
		return log_.size();
	}

	/**
	 * clear this series
	 */
	public void clearLog() {
		currentPos_ = -1;
        overPos_ = 0;
        changePos_ = -1;
        bRemoved = false;
		log_.clear();
		System.gc();
	}

    /**
     * get remoded top value in log_ or not
     */
    protected boolean isRemoved() {
        return  bRemoved;
    }
    
    /**
     * get TValue
     */
    protected TValue getObject(int index) {
        return  log_.get(index);
    }
}

/*
 *  GrxTimeSeriesItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.util.Vector;

public abstract class GrxTimeSeriesItem extends GrxBaseItem {
	private Vector<TValue> log_ = new Vector<TValue>();
	private int maxLogSize_ = 1000000;
	private int currentPos_ = -1;
	
	private class TValue {
		Double time;
		Object value;
		TValue(Double t, Object v) {
			time = t;
			value = v;
		}
	}

	public GrxTimeSeriesItem(String name, GrxPluginManager manager) {
		super(name, manager);
	}
	
	public void setPosition(Integer pos) {
		if (0 <= pos && pos < log_.size())
			currentPos_ = pos;
	}

	public int getPosition() {
		return currentPos_;
	}
	
	public void setTimeAt(int pos, Double t) {
		if (0 <= pos && pos < log_.size())
			log_.get(pos).time = t;
	}

	public Double getTime() {
		return getTime(currentPos_);
	}
	
	public Double getTime(int pos) {
		if (0 <= pos && pos < log_.size())
			return log_.get(pos).time;
		return null;
	}
	
	public void addValue(Double t, Object val) {
		TValue tv = new TValue(t,val);
		log_.add(tv);
		if (maxLogSize_ > 0 && log_.size() > maxLogSize_)
			log_.removeElementAt(0);
	}
	
	public Object getValue() {
		return getValue(currentPos_);
	}
	
	public Object getValue(int pos) {
		if (0 <= pos && pos < log_.size())
			return log_.get(pos).value;
		return null;
	}

	public final void setMaximumLogSize(int maxLogSize) {
		if (maxLogSize > 0) {
			maxLogSize_ = maxLogSize;
		}
	}

	public final int getLogSize() {
		return log_.size();
	}

	public void clearLog() {
		currentPos_ = -1;
		log_.clear();
		System.gc();
	}
}

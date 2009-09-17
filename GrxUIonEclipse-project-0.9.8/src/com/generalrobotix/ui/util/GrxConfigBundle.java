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
 *  GrxConfigBundle.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  2004/04/19
 */

package com.generalrobotix.ui.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Properties;

import com.generalrobotix.ui.grxui.Activator;

@SuppressWarnings("serial")
/**
 * @brief
 */
public class GrxConfigBundle extends Properties {	
	/**
	 * @brief constructor
	 */
	public GrxConfigBundle() {
		
	}
	
	/**
	 * @brief construct from a file
	 * @param fname file name
	 * @throws IOException
	 */
	public GrxConfigBundle(String fname) throws IOException{
		load(fname);
	}
	
	/**
	 * @brief load config from a file
	 * @param fname filename
	 * @throws IOException
	 */
	public void load(String fname) throws IOException {
		FileInputStream in = new FileInputStream(fname);
		load(in);
		in.close();
	}
	
	/**
	 * @brief store this config 
	 * @param fname file name
	 * @param comments comments
	 * @throws IOException
	 */
	public void store(String fname,String comments) throws IOException{
		FileOutputStream out = new FileOutputStream(fname);
		store(out,comments);
	}
	
	/**
	 * @brief get value associated to keyword
	 * @param key keyword
	 * @return value associated to keyword. If value can not be found in this config and environment variables, null is returned
	 */
	public final String getStr(String key){	
		String str = null;
		StringBuffer buf = null;
		try {
			str = getProperty(key, System.getenv(key));
			buf = new StringBuffer(str);
		} catch (java.util.MissingResourceException ex){
			GrxDebugUtil.println("ConfigBundle.getStr: missingResouce("+key+")");
			return null;
		} catch (NullPointerException e){
			GrxDebugUtil.printErr("ConfigBundle.getStr: The key '"+key+"' does not exist.");
			return null;
		}
		
		for (int i=0;;i++){
			int index1,index2;
			if ((index1 = buf.indexOf("$(")) == -1)
				break;
			if ((index2 = buf.indexOf(")" ,index1)) != -1){
				// in case there is no environment value,
				// check the property file 
				String keyword = buf.substring(index1+2,index2);
				String prop = System.getProperty(keyword);
				if(prop==null){
					prop = Activator.getDefault().getPreferenceStore().getString(keyword);
					if(prop.equals("")){
						prop = System.getenv(keyword);			
						if (prop == null)
							prop = getStr(keyword,"");
					}
				}
				buf.replace(index1,index2+1,prop);
			}
		}
		return buf.toString();
	}
	
	/**
	 * @brief get value associated to key
	 * @param key keyword
	 * @param defaultVal default return value
	 * @return value associated to key. If it can't be found, default value is returned.
	 */
	public final String getStr(String key, String defaultVal) {
		String ret = null;
		if ((ret= getStr(key)) == null)
			ret = defaultVal;
		return ret;
	}

	/**
	 * @brief get integer value associated to key
	 * @param key keyword
	 * @param defaultVal default value
	 * @return integer value associated to key
	 */
	public final Integer getInt(String key, Integer defaultVal) {
		Integer ret;
		try {
			ret = Integer.parseInt(getStr(key));
		} catch(Exception e){
			ret = defaultVal;	
	   	}
		return ret;
	}
	
	/**
	 * @brief get short value associated to key
	 * @param key keyword
	 * @param defaultVal default value
	 * @return short value associated to key if obtained, defaultVal otherwise
	 */
	public final Short getShort(String key, Short defaultVal) {
		Short ret = getShort(getStr(key));
		if (ret != null){
			return ret;
		}else{
			return defaultVal;
		}
	}

	/**
	 * @breif get Short from String
	 * @param value String
	 * @return Short value if converted successfully, null otherwise
	 */
	public Short getShort(String value){
		Short ret;
		try {
			ret = Short.parseShort(value);
		} catch(Exception e){
			return null;
	   	}
		return ret;
	}

	/**
	 * @brief associate short value to key
	 * @param key keyword
	 * @param value short value
	 */
	public final void setShort(String key, short value) {
		String val = new String();
		val += String.valueOf(value)+" ";
		setProperty(key,val);
	}
	
	/**
	 * @brief get integer array associated to key
	 * @param key keyword
	 * @return integer array associated to key
	 */
	public final int[] getIntAry(String key){
		String s = getStr(key);
		if (s==null)
			return null;
		String[] str = s.split(" ");
		int[] ret = null;
		ret = new int[str.length];
		try{
			for (int i=0;i<str.length;i++)
				ret[i] = Integer.parseInt(str[i]);
		}catch(Exception e){
			return null;
		}
		return ret;
	}
	
	/**
	 * @brief get double value associated to key
	 * @param key keyword
	 * @param defaultVal default value
	 * @return double value associated to key if obtained, defaultVal otherwise
	 */
	public final Double getDbl(String key, Double defaultVal) {
		Double ret = getDbl(getStr(key));
		if (ret != null){
			return ret;
		}else{
			return defaultVal;
		}
	}
	
	/**
	 * @breif get Double from String
	 * @param value string
	 * @return Double value if converted successfully, null otherwise
	 */
	public Double getDbl(String value){
		Double ret;
		try {
			ret = Double.parseDouble(value);
		} catch(Exception e){
			return null;
		}
		return ret;
	}

	/**
	 * @brief get float value associated to key
	 * @param key keyword
	 * @param defaultVal default value
	 * @return float value associated to key
	 */
	public final Float getFlt(String key, Float defaultVal) {
		Float ret;
		try {
			ret = Float.parseFloat(getStr(key));
		} catch(Exception e){
			ret = defaultVal;
		}
		return ret;
	}
	
	public final Float getFlt(String value) {
		Float ret;
		try {
			ret = Float.parseFloat(value);
		} catch(Exception e){
			return null;
		}
		return ret;
	}
	
	/**
	 * @brief get double array associated to key
	 * @param key keyword
	 * @param defaultVal default value
	 * @return double array associated to key
	 */
	public final double[] getDblAry(String key, double[] defaultVal){
		String s = getStr(key);
		double[] ret = getDblAry(s);
		if (ret != null){
			return ret;
		}else{
			return defaultVal;
		}
	}
	
	/**
	 * @brief get array of double from String
	 * @param value string of space separated double values
	 * @return array of double if converted successfully, null otherwise
	 */
	public double[] getDblAry(String value){
		if (value == null) return null;
		String[] str = value.split(" ");
		double[] ret = new double[str.length];
		try {
			for (int i=0;i<str.length;i++)
				ret[i] = Double.parseDouble(str[i]);
		} catch(Exception e){
			return null;
		}
		return ret;
	}
	
	/**
	 * @brief get float array associated to key
	 * @param key keyword
	 * @param defaultVal default value
	 * @return float array associated to key
	 */
	public final float[] getFltAry(String key, float[] defaultVal){
		String s = getStr(key);
		if (s==null)
			return defaultVal;
		String[] str = s.split(" ");
		float[] ret = new float[str.length];
		try {
			for (int i=0;i<str.length;i++)
				ret[i] = Float.parseFloat(str[i]);
		} catch(Exception e){
			return defaultVal;
		}
		
		return ret;
	}
	
	public float[] getFltAry(String value){
		if (value == null) return null;
		String[] str = value.split(" ");
		float[] ret = new float[str.length];
		try {
			for (int i=0;i<str.length;i++)
				ret[i] = Float.parseFloat(str[i]);
		} catch(Exception e){
			return null;
		}
		return ret;
	}
	
	/**
	 * @brief associate double array to key
	 * @param key keyword
	 * @param value double array
	 * @param digits for format
	 */
	public final void setDblAry(String key, double[] value, int digits) {
		String val = new String();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(digits);
		for (int i=0; i<value.length; i++) {
			if (Double.isNaN(value[i])){
				val += "NaN ";
			}else{
				val += nf.format(value[i])+" ";
			}
		}
		setProperty(key,val);
	}
	
	/**
	 * @brief associate double array to key
	 * @param key keyword
	 * @param value double array
	 */
	public final void setDblAry(String key, double[] value) {
		String val = new String();
		for (int i=0; i<value.length; i++) {
			val += String.valueOf(value[i])+" ";
		}
		setProperty(key,val);
	}
	
	/**
	 * @brief associate float value to key
	 * @param key keyword
	 * @param value double value
	 */
	public final void setFlt(String key, float value) {
		String val = new String();
		val += String.valueOf(value)+" ";
		setProperty(key,val);
	}
	
		/**
	 * @brief associate float array to key
	 * @param key keyword
	 * @param value float array
	 */
	public final void setFltAry(String key, float[] value) {
		String val = new String();
		for (int i=0; i<value.length; i++) {
			val += String.valueOf(value[i])+" ";
		}
		setProperty(key,val);
	}
	
	/**
	 * @brief associate double value to key
	 * @param key keyword
	 * @param value double value
	 */
	public final void setDbl(String key, double value) {
		String val = new String();
		val += String.valueOf(value)+" ";
		setProperty(key,val);
	}
	
	/**
	 * @brief associate double value to key
	 * @param key keyword
	 * @param value double value
	 * @param digits digits to format
	 */
	public final void setDbl(String key, double value, int digits) {
		String val = new String();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(digits);
		val += nf.format(value)+" ";
		setProperty(key,val);
	}
	
	/**
	 * @brief associate int value to key
	 * @param key keyword
	 * @param value int value
	 */
	public final void setInt(String key, int value) {
		String val = new String();
		val += String.valueOf(value)+" ";
		setProperty(key,val);
	}
	
	public final void setBool(String key, boolean value){
		if(value)
			setProperty(key,"true");
		else
			setProperty(key,"false");
	}
	
	/**
	 * @brief check whether value associated to key includes a word "true"
	 * @param key keyword
	 * @return true if value associated to key includes a word "true", false otherwise
	 */
	public final boolean isTrue(String key){
		return isTrue(key,false);
	}
	
	/**
	 * @brief check whether value associated to key includes a word "true"
	 * @param key keyword
	 * @param defaultVal default value
	 * @return true if value associated to key includes a word "true", false otherwise
	 */
	public final boolean isTrue(String key,boolean defaultVal){
		Boolean b = new Boolean(defaultVal);
		String str = getStr(key,b.toString()).toLowerCase();
		if (str.indexOf("true")!=-1)
			return true;
		return false;
	}

	/**
	 * @brief check whether value associated to key includes a word "false"
	 * @param key keyword
	 * @return true if value associated to key includes a word "false", false otherwise
	 */
	public final boolean isFalse(String key){
		return isFalse(key,false);
	}

	/**
	 * @brief check whether value associated to key includes a word "false"
	 * @param key keyword
	 * @return true if value associated to key includes a word "false", false otherwise
	 */
	public final boolean isFalse(String key,boolean defaultVal){
		Boolean b = new Boolean(defaultVal);
		String str = getStr(key,b.toString()).toLowerCase();
		if (str.indexOf("false")!=-1)
			return true;
		return false;
	}
}

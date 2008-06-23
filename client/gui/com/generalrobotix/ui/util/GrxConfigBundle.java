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
import java.util.Properties;

@SuppressWarnings("serial")
public class GrxConfigBundle extends Properties {	
	public GrxConfigBundle() {
		
	}
	public GrxConfigBundle(String fname) throws IOException{
		load(fname);
	}
	
	public void load(String fname) throws IOException {
		FileInputStream in = new FileInputStream(fname);
		load(in);
		in.close();
	}
	
	public void store(String fname,String comments) throws IOException{
		FileOutputStream out = new FileOutputStream(fname);
		store(out,comments);
	}
	
//	public void storeToXML(String fname,String comments) throws IOException{
//		FileOutputStream out = new FileOutputStream(fname);
//		properties_.storeToXML(out,comments);
//	}
	
//	public void storeToXML(String fname,String comments,String encoding) throws IOException{
//		FileOutputStream out = new FileOutputStream(fname);
//		properties_.storeToXML(out,comments,encoding);
//	}
	
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
			GrxDebugUtil.printErr("ConfigBundle.getStr: The key '"+key+"' is not exist.");
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
				if (prop == null)
					prop = getStr(keyword,"");				
				buf.replace(index1,index2+1,prop);
			}
		}
		return buf.toString();
	}
	
	public final String getStr(String key, String defaultVal) {
		String ret = null;
		if ((ret= getStr(key)) == null)
			ret = defaultVal;
		return ret;
	}
	
	public final Integer getInt(String key, Integer defaultVal) {
		Integer ret;
		try {
			ret = Integer.parseInt(getStr(key));
		} catch(Exception e){
			ret = defaultVal;	
	   	}
		return ret;
	}
	
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
	
	public final Double getDbl(String key, Double defaultVal) {
		Double ret;
		try {
			ret = Double.parseDouble(getStr(key));
		} catch(Exception e){
			ret = defaultVal;
		}
		return ret;
	}
	
	public final double[] getDblAry(String key, double[] defaultVal){
		String s = getStr(key);
		if (s==null)
			return defaultVal;
		String[] str = s.split(" ");
		double[] ret = new double[str.length];
		try {
			for (int i=0;i<str.length;i++)
				ret[i] = Double.parseDouble(str[i]);
		} catch(Exception e){
			return defaultVal;
		}
		
		return ret;
	}
	
	public final void setDblAry(String key, double[] value) {
		String val = new String();
		for (int i=0; i<value.length; i++) {
			val += String.valueOf(value[i])+" ";
		}
		setProperty(key,val);
	}
	
	public final void setDbl(String key, double value) {
		String val = new String();
		val += String.valueOf(value)+" ";
		setProperty(key,val);
	}
	
	public final boolean isTrue(String key){
		return isTrue(key,false);
	}
	public final boolean isTrue(String key,boolean defaultVal){
		Boolean b = new Boolean(defaultVal);
		String str = getStr(key,b.toString()).toLowerCase();
		if (str.indexOf("true")!=-1)
			return true;
		return false;
	}
	public final boolean isFalse(String key){
		return isFalse(key,false);
	}
	public final boolean isFalse(String key,boolean defaultVal){
		Boolean b = new Boolean(defaultVal);
		String str = getStr(key,b.toString()).toLowerCase();
		if (str.indexOf("false")!=-1)
			return true;
		return false;
	}
}

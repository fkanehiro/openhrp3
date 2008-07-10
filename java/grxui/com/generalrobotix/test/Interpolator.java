/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.test;

import java.io.IOException;

public class Interpolator {
  public static final int METHOD_LINEAR = 0;
  public static final int METHOD_SPLINE = 1;
  private int[]    method_ = null;
  private int      n_  = 0;
  private double[] x_  = null;
  private double[] y_  = null;
  
  private double[] yp_ = null;
  private double[] y2_ = null;

  public Interpolator(double[] x,double[] y, int[] method) {
    n_ = Math.min(x.length, y.length);
    method_ = new int[n_];
    x_  = new double[n_];
    y_  = new double[n_];
    yp_ = new double[n_];
    y2_ = new double[n_];

    int lo = -1;
    for (int i=0; i<n_; i++) {
      x_[i] = x[i];
      y_[i] = y[i];
      if (method != null)
        method_[i] = method[i];
      else 
        method_[i] = METHOD_SPLINE;
      
      if (method_[i] == METHOD_SPLINE) {
        if (lo == -1) {
          yp_[i] = (i == 0) ? 0 : yp_[i-1];
          lo = i;
        } else if (i == n_-1) {
          yp_[i] = 0;
          initSpline(lo,i);
        }
      }
      
      if (method_[i] == METHOD_LINEAR) {
        yp_[i] = (i == n_-1) ? 0:(y[i+1]-y[i]) / (x[i+1]-x[i]);
        if (lo != -1) {
         // System.out.println(lo+":"+yp_[lo]+":"+i+":"+yp_[i]);
          initSpline(lo,i);
          lo = -1;
        }
      }
    }
  }
  
  private void initSpline(int lo, int hi){
    double p,qn,sig,un;
    double[] u = new double[n_];

    if (yp_[lo] > 0.99e30)
      y2_[0] = u[0] = 0.0;
    else {
      y2_[0] = -0.5;
      u[0] = (3.0/(x_[1]-x_[0])) * ((y_[1]-y_[0])/(x_[1]-x_[0])-yp_[lo]);
    }
    
    for (int i=1; i<=n_-2; i++){
      sig = (x_[i]-x_[i-1])/(x_[i+1]-x_[i-1]);
      p = sig * y2_[i-1]+2.0;
      y2_[i] = (sig - 1.0)/p;
      u[i]  = (y_[i+1] - y_[i])/(x_[i+1] - x_[i]) - (y_[i] - y_[i-1])/(x_[i] - x_[i-1]);
      u[i]  = (6.0*u[i]/(x_[i+1] - x_[i-1]) - sig*u[i-1])/p;
    }
  
    if (yp_[hi] > 0.99e30)
      qn = un = 0.0;
    else { 
      qn = 0.5;
      un = (3.0/(x_[n_-1] - x_[n_-2]))*(yp_[hi] - (y_[n_-1] - y_[n_-2])/(x_[n_-1] - x_[n_-2]));
    }
    
    y2_[n_-1] = (un-qn*u[n_-2])/(qn*y2_[n_-2]+1.0);
    
    for (int i=n_-2;i>=0;i--)
      y2_[i] = y2_[i]*y2_[i+1]+u[i];
  }
  
  public double calc(double x) {
    int klo,khi,k;
    double h,b,a;
  
    klo = 0;
    khi = n_-1;
    while (khi-klo > 1) {
      k = (khi+klo) >> 1;
      if (x_[k] > x) 
        khi = k;
      else 
        klo = k;
    }
    h = x_[khi] - x_[klo];
  
    if (h == 0.0)
      h = 10e-10;
    
    if (method_[klo] == METHOD_SPLINE) {
      a = (x_[khi] - x)/h;
      b = (x - x_[klo])/h;

      return a*y_[klo]+b*y_[khi] + ((a*a*a-a)*y2_[klo]+(b*b*b-b)*y2_[khi])*(h*h)/6.0;
    } 
    
    return y_[klo] + (y_[khi] - y_[klo])*(x - x_[klo])/h;
  }
  
  public static void main(String[] args){
    double[] x = new double[]{0.0, 1.0, 2.0};//, 3.0, 4.0};
    double[] y = new double[]{0.0, 0.5, 0.5};//, 0.7, 1.0};
    int[] method = new int[]{METHOD_LINEAR,METHOD_SPLINE,METHOD_SPLINE,METHOD_LINEAR,METHOD_SPLINE};
    //double[] x = new double[]{0.0, 1.0};
    //double[] y = new double[]{0.0, 0.5};
    
    Interpolator s = new Interpolator(x, y, method);
    java.io.File f = new java.io.File("test.csv");
    try {
      java.io.FileWriter fw = new java.io.FileWriter(f);
      int num = 100;
      double last = x[x.length-1];
      for (int i=0;i<num;i++) {
        fw.write(last*i/num + "," + s.calc(last*i/num));
        if (i < x.length)
          fw.write("," + x[i] + "," + y[i]);
        fw.write("\n");
      }
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    for (int i=0;i<s.yp_.length;i++)
      System.out.println(s.yp_[i]+" ");
  } 
}

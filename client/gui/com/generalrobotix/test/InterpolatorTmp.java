package com.generalrobotix.test;

import java.io.IOException;

public class InterpolatorTmp {
  public static final int METHOD_LINEAR = 0;
  public static final int METHOD_SPLINE = 1;
  int method_ = METHOD_SPLINE;
  private int n_ = 0;
  private double[] x_ = null;
  private double[] y_ = null;
  private double[] y2_ = null;
  private double   yp1_ = 0;
  private double   ypn_ = 0;

  public InterpolatorTmp(double[] x,double[] y, int method) {
    n_ = x.length;
    x_  = new double[n_];
    y_  = new double[n_];
    y2_ = new double[n_];

    for (int i=0; i<n_; i++) {
      x_[i] = x[i];
      y_[i] = y[i];
    }
    
    if (method == METHOD_SPLINE)
      initSpline(x, y);
  }
  
  public InterpolatorTmp(double[] x,double[] y, int[] method) {
    n_ = x.length;
    x_  = new double[n_];
    y_  = new double[n_];
    y2_ = new double[n_];

    for (int i=0; i<n_; i++) {
      x_[i] = x[i];
      y_[i] = y[i];
    }
    if (method_ == METHOD_SPLINE)
      initSpline(x, y);
  }
  
  public void initSpline(double[] x,double[] y){
    method_ = METHOD_SPLINE;
    double p,qn,sig,un;
    double[] u = new double[n_];

    if (yp1_ > 0.99e30)
      y2_[0] = u[0] = 0.0;
    else {
      y2_[0] = -0.5;
      u[0] = (3.0/(x_[1]-x_[0])) * ((y_[1]-y_[0])/(x_[1]-x_[0])-yp1_);
    }
    
    for (int i=1; i<=n_-2; i++){
      sig = (x_[i]-x_[i-1])/(x_[i+1]-x_[i-1]);
      p = sig * y2_[i-1]+2.0;
      y2_[i] = (sig - 1.0)/p;
      u[i]  = (y_[i+1] - y_[i])/(x_[i+1] - x_[i]) - (y_[i] - y_[i-1])/(x_[i] - x_[i-1]);
      u[i]  = (6.0*u[i]/(x_[i+1] - x_[i-1]) - sig*u[i-1])/p;
    }
  
    if (ypn_ > 0.99e30)
      qn = un = 0.0;
    else { 
      qn = 0.5;
      un = (3.0/(x_[n_-1] - x_[n_-2]))*(ypn_ - (y_[n_-1] - y_[n_-2])/(x_[n_-1] - x_[n_-2]));
    }
    
    y2_[n_-1] = (un-qn*u[n_-2])/(qn*y2_[n_-2]+1.0);
    
    for (int i=n_-2;i>=0;i--)
      y2_[i] = y2_[i]*y2_[i+1]+u[i];
  }
    
  public void setTerminalGradient(double yp1, double ypn){
    yp1_ = yp1;
    ypn_ = ypn;
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
    
    if (method_ == METHOD_SPLINE) {
      a = (x_[khi] - x)/h;
      b = (x - x_[klo])/h;

      return a*y_[klo]+b*y_[khi] + ((a*a*a-a)*y2_[klo]+(b*b*b-b)*y2_[khi])*(h*h)/6.0;
    } 
    
    return y_[klo] + (y_[khi]-y_[klo])*x/h;
  }
  
  public static void main(String[] args){
    double[] x = new double[]{0,1,2,3,4};
    double[] y = new double[]{0,0.5,2,0.7,0.1};
    
    //double[] x = new double[]{0,1};
    //double[] y = new double[]{0,0.5};
    
    
    InterpolatorTmp s = new InterpolatorTmp(x, y,InterpolatorTmp.METHOD_SPLINE);
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
  } 
}

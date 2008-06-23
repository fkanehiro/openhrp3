/*
 *  GrxKinematicsHRP2m.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

public class GrxKinematicsHRP2m {
  public static final int CHOROMET_RLEG = 0;
  public static final int CHOROMET_LLEG = 1;

  static final double Y0 = 0.019;
  static final double X2 = 0.021;
  static final double Z2 = -0.002;
  static final double Z3 = -0.070;
  static final double Z4 = -0.070;
  static final double DE = 0.0000001;

  static final double q6maxR = Math.toRadians(54.1);
  static final double q6minR = Math.toRadians(-23.1);
  static final double q6maxL = Math.toRadians(23.1);
  static final double q6minL = Math.toRadians(-54.1);
  static final double q5max  = Math.toRadians(60.1);
  static final double q5min  = Math.toRadians(-100.1);

  public static boolean legIK(int whichLeg, double[] P06, double[] R06, double[] q)
  {
    double[] P60 = new double[3];
    double[] R60 = new double[9];
    if (whichLeg == CHOROMET_RLEG)
      P06[1] += Y0;
    else if (whichLeg == CHOROMET_LLEG)
      P06[1] -= Y0;
    
    transposeMat3(R06,R60);
    mulMat3Vec3(R60, P06, P60);
    mulScalVec3( -1, P60, P60);
    q[5]=Math.atan2(P60[1], P60[2]);

    if (whichLeg == CHOROMET_RLEG) {
      if (q[5] < q6minR)
        q[5] = q[5] + Math.PI;
      if (q[5] > q6maxR)
        q[5] = q[5] - Math.PI;
    } else if (whichLeg == CHOROMET_LLEG) {
      if (q[5] < q6minL)
        q[5] = q[5] + Math.PI;
      if (q[5] > q6maxL)
        q[5] = q[5] - Math.PI;
    }

    double c6 = Math.cos(q[5]), s6 = Math.sin(q[5]);
    q[1] = Math.asin( c6 * R06[7] - s6 * R06[8] );

    double[] R12 = new double[9];
    double[] R26 = new double[9];
    double[] R16 = new double[9];
    double[] R01 = new double[9];
    double c2 = Math.cos(q[1]), s2 = Math.sin(q[1]);
    double ss345 = -R06[6] / c2;
    double cc345 = Math.sqrt(1 - Math.pow(ss345,2));
    if ( Math.pow(-s2 * s6 + c2 * cc345 * c6 - R06[8],2) > DE )
      cc345 = -cc345;
    R12[0] = 1; R12[1] =  0; R12[2] =   0;
    R12[3] = 0; R12[4] = c2; R12[5] = -s2;
    R12[6] = 0; R12[7] = s2; R12[8] =  c2;
    R26[0] =  cc345; R26[1] = ss345 * s6; R26[2] = ss345 * c6;
    R26[3] = 0;      R26[4] = c6;         R26[5] = -s6;
    R26[6] = -ss345; R26[7] = cc345 * s6; R26[8] = cc345 * c6;
    mulMat3Mat3(R12, R26, R16);
    transposeMat3(R16,R16);
    mulMat3Mat3(R06, R16, R01);
    q[0] = Math.asin(R01[3]);

    double R02[] = new double[9], P25[] = new double[3];
    double c1 = Math.cos(q[0]), s1 = Math.sin(q[0]), dx, dz;
    R02[0] =  c1; R02[1] = -s1 * c2; R02[2] =   s1 * s2;
    R02[3] =  s1; R02[4] =  c1 * c2; R02[5] =  -c1 * s2;
    R02[6] =   0; R02[7] =  s2;      R02[8] =   c2;
    transposeMat3(R02, R02);
    mulMat3Vec3(R02, P06, P25);
    dx = P25[0] - X2;
    dz = P25[2] - Z2;
    q[3] = Math.acos( (Math.pow(dx,2) + Math.pow(dz,2) - Math.pow(Z3,2) - Math.pow(Z4,2)) / (2 * Z3 * Z4) );
    
    double beta = -Math.atan2(dx,-dz);
    q[2] = beta - q[3] / 2;

    q[4] = Math.atan2(ss345, cc345) - q[2] - q[3];
    if (q[4] < q5min) q[4] = q[4] + 2 * Math.PI;
    if (q[4] > q5max) q[4] = q[4] - 2 * Math.PI;
    
    for (int i=0; i<q.length; i++) {
      if (Double.isNaN(q[i]))
        return false;
    }
    
    return true;
  }
  
  public static void legFK(int whichLeg,double[] th,double[] pos,double[] rot)
  {
    double c[] = new double[6];
    double s[] = new double[6];
    
    for (int i=0; i<6; i++) {
      c[i] = Math.cos( th[i] );
      s[i] = Math.sin( th[i] );
    }
     
    double c34  = Math.cos( th[2] + th[3] );
    double s34  = Math.sin( th[2] + th[3] );
    double c345 = Math.cos( th[2] + th[3] + th[4] );
    double s345 = Math.sin( th[2] + th[3] + th[4] );
    
    rot[0] = c[0] * c345 - s[0] * s[1] * s345;
    rot[1] = c[0] * s345 * s[5] - s[0] * c[1] * c[5] + s[0] * s[1] * c345 * s[5];
    rot[2] = c[0] * s345 * c[5] + s[0] * c[1] * s[5] + s[0] * s[1] * c345 * c[5];
    rot[3] = s[0] * c345 + c[0] * s[1] * s345;
    rot[4] = s[0] * s345 * s[5] + c[0] * c[1] * c[5] - c[0] * s[1] * c345 * s[5];
    rot[5] = s[0] * s345 * c[5] - c[0] * c[1] * s[5] - c[0] * s[1] * c345 * c[5];
    rot[6] =-c[1] * s345;
    rot[7] = s[1] * c[5] + c[1] * c345 * s[5];
    rot[8] =-s[1] * s[5] + c[1] * c345 * c[5];
    
    double a = X2 + Z3 * s[2] + Z4 * s34;
    double b = Z2 + Z3 * c[2] + Z4 * c34;
    
    pos[0] = a * c[0] + b * s[0] * s[1];
    pos[1] = a * s[0] - b * c[0] * s[1];
    if (whichLeg == CHOROMET_RLEG)      
       pos[1] -= Y0;
    else 
       pos[1] += Y0;
    pos[2] = b * c[1];
  }

  private static void mulScalVec3(double iScal, double[] iVec3, double[] oVec3){
    for (int i=0; i<3; i++)
      oVec3[i] = iScal * iVec3[i]; 
  }



  private static void mulMat3Vec3(double[] iMat3,double[] iVec3, double[] oVec3){
    for (int i=0; i<3; i++){
    oVec3[i] = 0;
      for (int j=0; j<3; j++) {
        oVec3[i] += iMat3[i*3+j] * iVec3[j]; 
    }
    }
  }

  private static void  mulMat3Mat3(double[] iMat3a, double[] iMat3b, double[] oMat3){
    for (int i=0; i<3; i++) {
      for (int j=0; j<3; j++) {
      oMat3[i * 3 + j] = 0;
        for (int k=0; k<3; k++)
          oMat3[i * 3 + j] += iMat3a[i * 3 + k]*iMat3b[j + k * 3]; 
    }
    }
  }

  private static void transposeMat3(double[] iMat3, double[]oMat3) {
    double tMat3[] = new double[9];
    for (int i=0;i<9;i++)
      tMat3[i] = iMat3[i];
    oMat3[0] = tMat3[0]; oMat3[1] = tMat3[3]; oMat3[2] = tMat3[6];
    oMat3[3] = tMat3[1]; oMat3[4] = tMat3[4]; oMat3[5] = tMat3[7];
    oMat3[6] = tMat3[2]; oMat3[7] = tMat3[5]; oMat3[8] = tMat3[8];
  }
}

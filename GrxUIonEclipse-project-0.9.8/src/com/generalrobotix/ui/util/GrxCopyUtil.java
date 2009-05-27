/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * GrxCopyUtil.java
 *
 *
 * @author  T.Tawara
 * @version 1.0 (2009/05/18)
 */

package com.generalrobotix.ui.util;

import java.lang.System;

public class GrxCopyUtil {
    public static int[][] copyIntWDim(int[][] src ){
        int [][] ret = new int[src.length][];
        for(int i = 0; i < src.length; ++i){
            ret[i] = new  int[src[i].length];
            GrxCopyUtil.copyDim(src[i], ret[i], src[i].length);
        }
        return ret;
    }

    public static long[][] copyLongWDim(long[][] src ){
        long [][] ret = new long[src.length][];
        for(int i = 0; i < src.length; ++i){
            ret[i] = new  long[src[i].length];
            GrxCopyUtil.copyDim(src[i], ret[i], src[i].length);
        }
        return ret;
    }
    
    public static double[][] copyDoubleWDim(double[][] src ){
        double [][] ret = new double[src.length][];
        for(int i = 0; i < src.length; ++i){
            ret[i] = new  double[src[i].length];
            GrxCopyUtil.copyDim(src[i], ret[i], src[i].length);
        }
        return ret;
    }
    
    public static float[][] copyFloatWDim(float[][] src ){
        float [][] ret = new float[src.length][];
        for(int i = 0; i < src.length; ++i){
            ret[i] = new  float[src[i].length];
            GrxCopyUtil.copyDim(src[i], ret[i], src[i].length);
        }
        return ret;
    }
    
    public static <T> void copyDim(T src, T dest, int length){
        System.arraycopy(src, 0, dest, 0, length);
    }
}

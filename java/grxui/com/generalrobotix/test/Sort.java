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
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;

class Sort {
  public static void main(String[] args){
    try {
      BufferedReader b = new BufferedReader(new FileReader(new File(args[0])));
      ArrayList<String> al = new ArrayList<String>();
      int j=0;
      while(b.ready()) {
        al.add(b.readLine());
        System.out.println(al.get(j++));
      }
      String[] oa = al.toArray(new String[0]);
      Arrays.sort(oa,String.CASE_INSENSITIVE_ORDER);
      for (int i=0; i<oa.length; i++)
        System.out.println(oa[i]);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
};

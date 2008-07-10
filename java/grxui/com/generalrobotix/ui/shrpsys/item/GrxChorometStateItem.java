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
 *  GrxChorometStateItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.item;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxTimeSeriesItem;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxChorometStateItem extends GrxTimeSeriesItem {
	public static String TITLE = "Robot State";

	public RobotState state_ = null;
	private Socket socket_ = null;
	public PrintWriter writer_ = null;
	private InputStreamReader reader_ = null;
	private BufferedReader breader_ = null;
	public String rowData = null;
	private String hostname_ = "hrp3";
	private int port_ = 7777;
	private boolean requesting_ = false;

	public GrxChorometStateItem(String name, GrxPluginManager manager) {
		super(name, manager);
		initSocket();
	}

	public boolean create() {
		return true;
	};

	public void control() {
		if (writer_ == null)
			return;
		state_ = null;

		try {
			if (!breader_.ready() && !requesting_) {
				writer_.println("#request data");
				writer_.flush();
				requesting_ = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			while (breader_.ready()) {
				if (state_ == null) {
					state_ = new RobotState();

					rowData = breader_.readLine() + "\n";
				} else
					rowData += breader_.readLine() + "\n";
			}
			if (state_ != null) {
				requesting_ = false;
				try {
					state_.parse();
				} catch (Exception e) {
					System.out.println("RobotItem: parse error.");
					GrxDebugUtil.println("rowdata:\n" + rowData);
					return;
				}
				/* Collection items = manager_.getSelectedItems(GrxModelItem.class);
				 Iterator it = items.iterator();
				 if (it.hasNext()) {
				 GrxModelItem gitem = (GrxModelItem) it.next();
				 gitem.setJointAngles(state_.angles);
				 }*/
				addValue(state_.run_count*0.02, state_);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initSocket() {
		try {
			hostname_ = System.getProperty("HOST", hostname_);
			System.out.println("host=" + hostname_);
			socket_ = new Socket(hostname_, port_);
			writer_ = new PrintWriter(socket_.getOutputStream());
			reader_ = new InputStreamReader(socket_.getInputStream());
			breader_ = new BufferedReader(reader_);
			requesting_ = false;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Usage:java MessageClient hostname");
		} catch (UnknownHostException e) {
			System.err.println("Host not found.");
		} catch (SocketException e) {
			System.err.println("Socket Error!");
		} catch (IOException e) {
			System.err.println("IO Error!");
		}
	}

	public boolean writeSocket(String com) {
		if (writer_ != null && isSelected()) {
			writer_.println(com);
			writer_.flush();

			return true;
		}

		GrxDebugUtil.println(com);
		return false;
	}

	public boolean isConnected() {
		if (socket_ == null)
			return false;
		return false;// socket_.isConnected();
	}

	public class RobotState {
		public int run_count = 0;
		public double[] angles = null;
		public double[][] force = null;
		public double[][] rate = null;
		public double[][] accel = null;
		public double[][] zmp = null;
		public double[] comAngles = null;
		public double[] comZmp = null;
		public double[] adval = null;
		public int[] dio = null;

		private boolean parse() {
			String[] line = rowData.split("\n");
			for (int i = 0; i < line.length; i++) {
				StringTokenizer st = new StringTokenizer(line[i]);
				if (!st.hasMoreTokens())
					continue;
				String tag = st.nextToken();
				if (tag.equals("#count")) {
					st = new StringTokenizer(line[++i]);
					run_count = Integer.parseInt(st.nextToken());
				} else if (tag.equals("#angle")) {
					//int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					angles = new double[n2];
					st = new StringTokenizer(line[++i]);
					for (int k = 0; k < n2; k++) {
						try {
							angles[k] = Double.parseDouble(st.nextToken());
						} catch (Exception e) {
							angles[k] = Double.NaN;
						}
					}
				} else if (tag.equals("#force")) {
					int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					force = new double[n1][n2];
					zmp = new double[n1][2];
					for (int j = 0; j < n1; j++) {
						st = new StringTokenizer(line[++i]);
						for (int k = 0; k < n2; k++)
							force[j][k] = Double.parseDouble(st.nextToken());
						if (force[j][2] > 1.0) {
							zmp[j][0] = 1000 * -force[j][4] / force[j][2];
							zmp[j][1] = 1000 * force[j][3] / force[j][2];
						} else
							zmp[j][0] = zmp[j][1] = 0.0;
					}
				} else if (tag.equals("#rate")) {
					int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					rate = new double[n1][3];
					for (int j = 0; j < n1; j++) {
						st = new StringTokenizer(line[++i]);
						for (int k = 0; k < n2; k++)
							rate[j][k] = Double.parseDouble(st.nextToken());
					}
				} else if (tag.equals("#accel")) {
					int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					accel = new double[n1][n2];
					for (int j = 0; j < n1; j++) {
						st = new StringTokenizer(line[++i]);
						for (int k = 0; k < n2; k++)
							accel[j][k] = Double.parseDouble(st.nextToken());
					}
				} else if (tag.equals("#adval")) {
					//int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					adval = new double[n2];
					st = new StringTokenizer(line[++i]);
					for (int j = 0; j < n2; j++) {
						adval[j] = Double.parseDouble(st.nextToken());
					}
				}
			}
			return true;
		}
	}
}

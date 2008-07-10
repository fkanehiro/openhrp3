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
 *  GrxSockPortItem.java
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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxSockPortItem extends GrxBaseItem 
		implements GrxPortItem {

	private String sockHost_;
	private int    sockPort_;
	private Socket socket_;
	private BufferedReader reader_;
	private OutputStream   ostrm_;
	private PrintWriter    writer_;
	private boolean isEnabled_ = true;
	
	public static final String ITEM_NAME = "Socket Port";
	
	public GrxSockPortItem(String name, GrxPluginManager manager) {
		super(name, manager);
		setExclusive(true);
	}
	
	public boolean create() {
		return true;
	}
	
	public short open() {
		if (socket_ != null && socket_.isConnected()) 
			return 0;
		
		try {
			sockHost_ = getProperty("sockHost", "hrp3");
			sockPort_ = getInt("sockPort", 7777);
			socket_ = new Socket(sockHost_, sockPort_);
			reader_ = new BufferedReader(new InputStreamReader(socket_.getInputStream()));
			ostrm_    = socket_.getOutputStream();
			writer_ = new PrintWriter(ostrm_);
            return 1;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Usage:java MessageClient hostname");
		} catch (UnknownHostException e) {
			System.err.println("Host not found.");
		} catch (SocketException e) {
			System.err.println("Socket Error!");
		} catch (IOException e) {
			System.err.println("IO Error!");
		}
        socket_ = null;
        return -1;
	}

	public void close() throws IOException {
		reader_.close();
		writer_.close();
		socket_.close();
        socket_ = null;
	}
	
	public boolean ready() throws IOException {
		if (reader_ == null)
			return false;
		return reader_.ready();
	}

	public String readLine() throws IOException {
		if (reader_ == null)
			return null;
		return reader_.readLine();
	}
	
	public int read(char[] ret) throws IOException {
		if (reader_ != null) {
			ret = new char[100];
			return reader_.read(ret);
		}
		return 0;
	}
	
	public synchronized void println(String msg) {
		if (isEnabled_ && writer_ != null) {
			writer_.println(msg);
			writer_.flush();
		}
	}
	
	public synchronized void write(String msg) {
		if (isEnabled_ && writer_ != null)
			writer_.write(msg);
	}

	public synchronized void write(byte[] b) {
		
	}

	public boolean isEnabled() {
		return isEnabled_;
	}

	public void setEnabled(boolean b) {
		isEnabled_ = b;
	}
	
	public boolean isConnected() {
		if (socket_ == null)
			return false;
		return false;// socket_.isConnected();
	}
}

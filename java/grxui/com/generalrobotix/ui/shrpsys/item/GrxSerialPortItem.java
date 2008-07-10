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
 *  GrxSerialPortItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.item;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.swing.JOptionPane;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxSerialPortItem extends GrxBaseItem 
		implements GrxPortItem {//SerialPortEventListener, GrxPortItem{
	
	public static String ITEM_NAME = "SerialPort";

	private CommPortIdentifier portId_;
	private SerialPort port_ = null;
	private int baudRate_ = 57600;
	private int dataBit_ = SerialPort.DATABITS_8;
	private int stopBit_ = SerialPort.STOPBITS_1;
	private int parity_ = SerialPort.PARITY_NONE;
	private int flowCtrl_ = SerialPort.FLOWCONTROL_NONE;
	
	private OutputStream   ostrm_ ;
	private PrintWriter	   writer_;
	private BufferedReader reader_;
	
	private StringBuffer buffer_ = new StringBuffer();
	
	public GrxSerialPortItem(String name, GrxPluginManager manager) {
		super(name, manager);
	}

	public boolean create() {
		return true;
	};

/*	private ArrayList getSerialPortIds() {
		final Enumeration e = CommPortIdentifier.getPortIdentifiers();
		final ArrayList<CommPortIdentifier> result = new ArrayList<CommPortIdentifier>();
		while (e.hasMoreElements()) {
			final CommPortIdentifier id = (CommPortIdentifier) e.nextElement();
			switch (id.getPortType()) {
			case CommPortIdentifier.PORT_SERIAL:
				result.add(id);
				break;
			case CommPortIdentifier.PORT_PARALLEL:
				break;
			default:
				break;
			}
		}
		return result;
	}*/

	public short open() {
		String portName = this.getName();
		try {
			portId_ = CommPortIdentifier.getPortIdentifier(portName);
		} catch (NoSuchPortException e) {
			JOptionPane.showMessageDialog(manager_.getFrame(), 
					portName+" is not a serial port.");
			GrxDebugUtil.printErr("SerialCommunicator.open:", e);
			return -1;
		}

		try {
			port_ = (SerialPort) portId_.open("GrxUI", 1000);
			port_.setSerialPortParams(baudRate_, dataBit_, stopBit_, parity_);
			port_.setFlowControlMode(flowCtrl_);
			InputStream in = port_.getInputStream();
			reader_ = new BufferedReader(new InputStreamReader(in));
			ostrm_ = port_.getOutputStream();
			writer_ = new PrintWriter(ostrm_);
            return 1;
		} catch (PortInUseException e) {
			System.out.println(portName + " is in use.");
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*port_.notifyOnDataAvailable(true);
		try {
			port_.addEventListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}*/
        return -1;
	}

	public void close() {
		port_.close();
	}

	/*public void serialEvent(SerialPortEvent evt) {
		if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				while (reader_.ready())
					buffer_.append(Character.toChars(reader_.read()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}*/

	public boolean ready() throws IOException {
		return reader_.ready();
	}
	
	public String readLine() throws IOException {
		return reader_.readLine();
	}

	public int read(char[] ret) throws IOException {
		if (reader_ != null) {
			ret = new char[100];
			return reader_.read(ret);
		}
		return 0;
	}
	
	public String getText() {
		return buffer_.toString();
	}

	public void println(String msg) {
		
	}

	public void write(String msg) {
			writer_.write(msg);
/*		try {
			out_.write(msg);
			msg += "\n";
			byte[] b = msg.getBytes();
			for (int i = 0; i < b.length; i++) {
				out_.write(b[i]);
				Thread.sleep(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
*/
	}

	public void sendETX() {
		write(new byte[] { 0x03 });
	}

	public void sendEOT() {
		write(new byte[] { 0x04 });
	}

	public void sendSUB() {
		write(new byte[] { 0x1A });
	}
	
	public void write(byte[] b) {
		try {
			ostrm_.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setEnabled(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}
}

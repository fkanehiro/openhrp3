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
 *  GrxWorldStateItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.*;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;

import jp.go.aist.hrp.simulator.*;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxTimeSeriesItem;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.view.graph.*;

@SuppressWarnings("unchecked")
public class GrxWorldStateItem extends GrxTimeSeriesItem {
	public static final String TITLE = "World State";
	public static final String DEFAULT_DIR = "log";
	public static final String FILE_EXTENSION = "log";
	
	private WorldStateEx newStat_ = null;
	private WorldStateEx preStat_ = null;
	private int prePos_ = -1;
	
    public  final LogManager logger_ = new LogManager();
	private float   recDat_[][];
    private String  lastCharName_ = null;
	private boolean initFlag_ = true;
	private boolean useDisk_ = true;
	private boolean storeAllPos_ = true;
	
	private JMenuItem save_ = new JMenuItem("save");
	private JMenuItem saveCSV_ = new JMenuItem("saveAsCSV");
	private JMenuItem clear_ = new JMenuItem("clear");
	
	private AxisAngle4d a4d = new AxisAngle4d();
	private Matrix3d m3d = new Matrix3d();
	private AxisAngle4d a4dg = new AxisAngle4d();
	private Matrix3d m3dg = new Matrix3d();

	private String tempDirBase_;
	private String tempDir_;
	
	public GrxWorldStateItem(String name, GrxPluginManager manager) {
    	super(name, manager);

		tempDirBase_ = System.getProperty("java.io.tmpdir")+File.separator+"grxui-"+System.getProperty("user.name")+File.separator;
		tempDir_ = tempDirBase_+getName();

		save_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
               saveLog();
			}
		});
		
		JMenuItem load = new JMenuItem("load");
		load.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
               loadLog(null);
			}
		});
		
		saveCSV_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
               saveCSV();
			}
		});
		
		clear_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int ans = JOptionPane.showConfirmDialog(manager_.getFrame(), 
					"Are you sure to clear log ?", "Clear Log", 
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
				if (ans == JOptionPane.OK_OPTION)
					clearLog();
			}
		});
		
		setMenuItem(save_);
		setMenuItem(load);
		setMenuItem(saveCSV_);
		setMenuItem(clear_);

		setExclusive(true);
		setIcon(new ImageIcon(getClass().getResource("/resources/images/world.png")));

		logger_.init();
	}
	
	public static String[] getStaticMenu() {
		return new String[]{"create", "load"};
	}

	public boolean create() {
        clearLog();
		return true;
	}

	public boolean load(File f) {
        loadLog(f);
        return true;
    }
	
	public void restoreProperties() {
		super.restoreProperties();
		useDisk_ = isTrue("useDisk", useDisk_);
		storeAllPos_ = isTrue("storeAllPosition", storeAllPos_);
		if (!useDisk_) {
			GrxDebugUtil.println("GrxWorldStateItem: useDisk = false");
			super.setMaximumLogSize(5000);
		} else 
			super.setMaximumLogSize(-1);
	}
	
	public void rename(String newName) {
		File oldDir = new File(tempDir_);
		super.rename(newName);
		File newDir = new File(tempDirBase_+getName());
		if (newDir.isDirectory()) 
			newDir.delete();
		
		if (oldDir.isDirectory())
			oldDir.renameTo(newDir);
		else 
			newDir.mkdir();
		
		tempDir_ = newDir.getPath();
		logger_.setTempDir(tempDir_);
	}
	
	public void clearLog() {
		super.clearLog();
		initFlag_ = true;
		logger_.init();
		newStat_ = null;
		preStat_ = null;
		prePos_ = -1;
		lastCharName_ = null;
		save_.setEnabled(false);
		saveCSV_.setEnabled(false);
	}
	
	// ###### [Changed] CharacterInfo -> BodyInfo
	public void registerCharacter(String cname, BodyInfo binfo) {
		ArrayList<String> logList = new ArrayList<String>();
		logList.add("time");
		logList.add("float");
		
		LinkInfo[] li = binfo.links();
		//ArrayList<Integer> jointList = new ArrayList<Integer>();
		ArrayList<Short> jointList = new ArrayList<Short>();
		ArrayList<SensorInfoLocal> sensList  = new ArrayList<SensorInfoLocal>();
		for (int i=0; i<li.length; i++)	 {
			jointList.add(li[i].jointId);
			
			SensorInfo[] si = li[i].sensors;
			for (int j=0; j<si.length; j++) 
				sensList.add(new SensorInfoLocal(si[j]));
		}
		
		Collections.sort(sensList);
		
		int len = storeAllPos_ ? li.length : 1;
		for (int i=0; i< len; i++) {
			String jname = li[i].name;
			logList.add(jname+".translation");
			logList.add("float[3]");
			logList.add(jname+".rotation");
			logList.add("float[4]");
		}
		
		for (short i=0; i<jointList.size(); i++) {
			int idx = jointList.indexOf(i);
			if (idx >= 0) {
				String jname = li[idx].name;
				logList.add(jname+".angle");
				logList.add("float");
				logList.add(jname+".jointTorque");
				logList.add("float");
			}
		}
		
		for (int i=0; i<sensList.size(); i++) {
			String sname = sensList.get(i).name;
			// ##### [Changed] NewModelLoader.IDL
			//switch(sensList.get(i).type) {
			//case SensorType._FORCE_SENSOR:
			//    logList.add(sname+".force");
			//    logList.add("float[3]");
			//    logList.add(sname+".torque");
			//    logList.add("float[3]");
			//    break;
			//case SensorType._RATE_GYRO:
			//    logList.add(sname+".angularVelocity");
			//    logList.add("float[3]");
			//    break;
			//case SensorType._ACCELERATION_SENSOR:
			//    logList.add(sname+".acceleration");
			//    logList.add("float[3]");
			//    break;
			//default:
			//    break;
			//}
			if(sensList.get(i).type.equals("Force")){
				logList.add(sname+".force");
				logList.add("float[3]");
				logList.add(sname+".torque");
				logList.add("float[3]");
			}else if(sensList.get(i).type.equals("RateGyro")){
			    logList.add(sname+".angularVelocity");
			    logList.add("float[3]");
			}else if(sensList.get(i).type.equals("Acceleration")){
			    logList.add(sname+".acceleration");
			    logList.add("float[3]");
			}else{
			}

			 // ##### [Changed] NewModelLoader.IDL
		}
		
		try {
			logger_.addLogObject(cname, logList.toArray(new String[0]));
		} catch (LogFileFormatException e) {
			e.printStackTrace();
		}
	}

	public void addValue(Double t, Object obj) {
		if (!useDisk_) {
			super.addValue(t, obj);
			return;
		}
			
//System.out.println( "    addValue() : 2.7.0"  );
		if (obj instanceof WorldStateEx) {
			newStat_ = (WorldStateEx) obj;
			_initLog();
			logger_.setTime(new Time((float)newStat_.time));
			
			try {
				Collision[] cols = newStat_.collisions;
				if (cols != null && cols.length > 0) {
					List<CollisionPoint> cdList = new ArrayList<CollisionPoint>();
					for (int i=0; i<cols.length; i++) {
						for (int j=0; j<cols[i].points.length; j++) 
							cdList.add(cols[i].points[j]);
					}
			        CollisionPoint[] cd = cdList.toArray(new CollisionPoint[0]);
			        logger_.putCollisionPointData(cd);
				}
			} catch (IOException e) {
				GrxDebugUtil.printErr("",e);
			}
			
//System.out.println( "    addValue() : 2.7.1"  );
			for (int i=0; i < newStat_.charList.size(); i++) {
				int k = 0;
				recDat_[i][k++] = (float) newStat_.time;
				CharacterStateEx cpos = newStat_.charList.get(i);
				int len = storeAllPos_ ? cpos.position.length : 1;
//System.out.println( "    addValue() : 2.7.2"  );
				for (int j=0; j<len; j++) {
					for (int m=0; m<3; m++)
						recDat_[i][k++] = (float)cpos.position[j].p[m];
					m3d.set(cpos.position[j].R);
					//m3d.transpose();
					a4d.set(m3d);
					recDat_[i][k++] = (float) a4d.x;
					recDat_[i][k++] = (float) a4d.y;
					recDat_[i][k++] = (float) a4d.z;
					recDat_[i][k++] = (float) a4d.angle;
				}
				
//System.out.println( "    addValue() : 2.7.3"  );
				String cname = cpos.characterName;
				SensorState sdata = cpos.sensorState;
//System.out.println( "    addValue() : 2.7.4"  );
				if (sdata != null) {
//System.out.println( "    addValue() : 2.7.5"  );
// ##### [Changed] #####
   //2008-03-06
//System.out.println( "       sdata.q.length = " + sdata.q.length );
					for (int j=0; j<sdata.q.length; j++) {
//System.out.println( "         j = " + j + ", sdata.q[j] = " + sdata.q[j] + ", sdata.u[j] = " + sdata.q[j]  );
						recDat_[i][k++] = (float) sdata.q[j];
						recDat_[i][k++] = (float) sdata.u[j];
					}

//System.out.println( "    addValue() : 2.7.6"  );
					for (int j=0; j<sdata.force.length; j++) {
						for (int m=0; m<sdata.force[j].length; m++) 
							recDat_[i][k++] = (float)sdata.force[j][m];
					}
//System.out.println( "    addValue() : 2.7.7"  );
					for (int j=0; j<sdata.rateGyro.length; j++) {
						for (int m=0; m<sdata.rateGyro[j].length; m++) 
							recDat_[i][k++] = (float)sdata.rateGyro[j][m];
					}
//System.out.println( "    addValue() : 2.7.8"  );
					for (int j=0; j<sdata.accel.length; j++) {
						for (int m=0; m<sdata.accel[j].length; m++) 
							recDat_[i][k++] = (float)sdata.accel[j][m];
					}
				}
				
				try {
					logger_.put(cname, recDat_[i]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			super.addValue(newStat_.time, null);
		}
	}
	private void _initLog() {
		if (!initFlag_)
			return;
		initFlag_ = false;
		save_.setEnabled(true);
		saveCSV_.setEnabled(true);
		SimulationTime stime = new SimulationTime();
		stime.setCurrentTime(newStat_.time);
		stime.setStartTime(newStat_.time);
		
		double val = getDbl("logTimeStep",0.001);
		stime.setTimeStep(val);
		setDbl("logTimeStep",val);
			
		val = getDbl("totalTime", 20.0);
		stime.setTotalTime(val);
		setDbl("totalTime", val);

		File f = new File(tempDir_);
		File pf = f.getParentFile();
		if (!pf.isDirectory())
			pf.mkdir();
		if (!f.isDirectory())
			f.mkdir();
		logger_.setTempDir(f.getPath());
		
		logger_.initCollisionLog(stime);
		try {
			logger_.openAsWrite(stime, 1);
			logger_.openAsRead();
			logger_.openCollisionLogAsWrite();
			logger_.openCollisionLogAsRead();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		recDat_ = new float[logger_.getLogObjectNum()][];
		for (int i=0; i<recDat_.length; i++)
			recDat_[i] = new float[logger_.getDataLength(newStat_.charList.get(i).characterName)];
		preStat_ = newStat_;
	}

	public WorldStateEx getValue() {
		if (!useDisk_)
			return (WorldStateEx)super.getValue();
		
		int pos = getPosition();
		if (pos < 0)
			return null;
		if (pos == getLogSize()-1 && newStat_ != null)
			return newStat_;
		if (pos != prePos_ && preStat_ != null)
			getValue(pos);
		
		return preStat_;
	}

	public WorldStateEx getValue(int pos) {
		if (pos < 0)
			return null;
		
		if (!useDisk_)
			return (WorldStateEx)super.getValue(pos);

		try {        
			preStat_.collisions = new Collision[]{new Collision()};
	        preStat_.collisions[0].points = logger_.getCollisionPointData(pos);
	        
			for (int i=0; i<preStat_.charList.size(); i++) {
				int k=0;
				CharacterStateEx cpos = preStat_.charList.get(i);
				float[] f = logger_.get(cpos.characterName, (long)pos);
				preStat_.time = (double)f[k++];
				for (int j=0; j<cpos.position.length; j++) {
					LinkPosition lpos = cpos.position[j];
					if (storeAllPos_ || j == 0) { 
						for (int m=0; m<3; m++)
							lpos.p[m] = (double)f[k++];
						a4dg.set((double)f[k++], (double)f[k++], (double)f[k++], (double)f[k++]);
						m3dg.set(a4dg);
						lpos.R[0] = m3dg.m00;
						lpos.R[1] = m3dg.m01;
						lpos.R[2] = m3dg.m02;
						lpos.R[3] = m3dg.m10;
						lpos.R[4] = m3dg.m11;
						lpos.R[5] = m3dg.m12;
						lpos.R[6] = m3dg.m20;
						lpos.R[7] = m3dg.m21;
						lpos.R[8] = m3dg.m22;
					} else {
						lpos.p = null;  // to calculate kinema in model
						lpos.R = null;  // to calculate kinema in model
					}
				}
				
				SensorState sdata = cpos.sensorState;
				if (sdata != null) {
					for (int j=0; j<sdata.q.length; j++) {
						sdata.q[j] = (double)f[k++]; 
						sdata.u[j] = (double)f[k++];
					}
					for (int j=0; j<sdata.force.length; j++) {
						for (int m=0; m<sdata.force[j].length; m++) 
							sdata.force[j][m] = (double)f[k++];
					}
					for (int j=0; j<sdata.rateGyro.length; j++) {
						for (int m=0; m<sdata.rateGyro[j].length; m++) 
							sdata.rateGyro[j][m] = (double)f[k++];
					}
					for (int j=0; j<sdata.accel.length; j++) {
						for (int m=0; m<sdata.accel[j].length; m++) 
							sdata.accel[j][m] = (double)f[k++];
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		prePos_ = pos;
		return preStat_;
	}
	
	private void loadLog(final File logFile) {
        save_.setEnabled(false);
		manager_.processingWindow_.setVisible(false);
		manager_.processingWindow_.setTitle("Load worldstate log");
		manager_.processingWindow_.setMessage("Loading log as a file:log/"+getName()+".log ...");
		Thread t = new Thread() {
			public void run() {
				manager_.processingWindow_.setVisible(true);
				_loadLog(logFile);
				manager_.processingWindow_.setVisible(false);
        		saveCSV_.setEnabled(true);
			}
		};
		t.start();
	}

	private void _loadLog(File logFile) {
		try {
			if (logFile == null) 
				logFile = new File("log"+File.separator+getName()+".log");

			if (!logFile.isFile())
				return;

            String fname = logFile.getAbsolutePath();

			File dir = new File(tempDir_);
			if (!dir.isDirectory())
				dir.mkdir();
            clearLog();
			logger_.setTempDir(tempDir_);
			logger_.load(fname, "");
			logger_.openAsRead();
			logger_.openCollisionLogAsRead();
			
            preStat_ = new WorldStateEx();
            Enumeration e = new ZipFile(fname).entries();
            while (e.hasMoreElements()) {
            	String entry = ((ZipEntry)e.nextElement()).getName();
            	if (entry.indexOf(".col") > 0)
            		continue;
            	
            	lastCharName_ = new File(entry).getName().split(".tmp")[0];
            	String[] format = logger_.getDataFormat(lastCharName_);

				// Check log size
            	List<LinkPosition> lposList = new ArrayList<LinkPosition>();
            	int jointCount = 0;
            	int forceCount = 0;
            	int gyroCount = 0;
            	int accelCount = 0;
            	for (int i=0; i<format.length; i++) {
            		String[] str = format[i].split("[.]");
            		if (str.length <= 1) {
            			continue;
            		} else if (str[1].equals("translation")) {
            			LinkPosition lpos = new LinkPosition();
            			lpos.p = new double[3];
            			lpos.R = new double[9];
            			lposList.add(lpos);
            		} else if (str[1].equals("angle")) {
            			if (!storeAllPos_) 
            				lposList.add(new LinkPosition());
            			jointCount++;
            		} else if (str[1].equals("force")) {
            			forceCount++;
            		} else if (str[1].equals("angularVelocity")) {
            			gyroCount++;
            		} else if (str[1].equals("acceleration")) {
            			accelCount++;
                	}
            	}
            	
  				CharacterStateEx cpos = new CharacterStateEx();
  				cpos.characterName = lastCharName_;
  				cpos.position = lposList.toArray(new LinkPosition[0]);
            	if (jointCount+forceCount+accelCount+gyroCount > 1) {
            		SensorState	sdata = new SensorState();
       				sdata.q = new double[jointCount];
       				//sdata.dq = new double[jointCount];
       				sdata.u = new double[jointCount];
      				sdata.force = new double[forceCount][6];
					sdata.rateGyro  = new double[gyroCount][3];
					sdata.accel = new double[accelCount][3];
    				cpos.sensorState = sdata;
            	}
            	preStat_.charList.add(cpos);
            	preStat_.charMap.put(lastCharName_, cpos);
            }
            
            int datLen = logger_.getRecordNum(lastCharName_);
            for (int i=0; i<datLen; i++) 
            	super.addValue(null, null);
            
			Thread.sleep(manager_.getDelay()*2);
            setPosition(0);
		} catch (FileOpenFailException e) {
			e.printStackTrace();
		} catch (LogFileFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void saveLog() {
        save_.setEnabled(false);
		manager_.processingWindow_.setTitle("Save worldstate log");
		manager_.processingWindow_.setMessage("Saving log as log/"+getName()+".log ...");
		Thread t = new Thread() {
			public void run() {
				manager_.processingWindow_.setVisible(true);
				try {
					logger_.closeAsWrite();
					logger_.closeCollisionLogAsWrite();
					logger_.save("log"+File.separator+GrxWorldStateItem.this.getName()+".log", getName()+".prj");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					manager_.processingWindow_.setVisible(false);
				}
			}
		};
		t.start();
	}
	
	private void saveCSV() {
        save_.setEnabled(false);
		manager_.processingWindow_.setTitle("Save log as CSV");
		manager_.processingWindow_.setMessage("Saving each log as log/"+getName()+"/*.csv ...");
		Thread t = new Thread() {
			public void run() {
				try {
					manager_.processingWindow_.setVisible(true);
					String fname = "log"+File.separator+GrxWorldStateItem.this.getName()+".log";
					File f = new File(fname);
					if (!f.isFile()) {
						logger_.closeAsWrite();
						logger_.closeCollisionLogAsWrite();
						logger_.save(fname, getName()+".prj");
					}
					for (int i=0; i<preStat_.charList.size(); i++) {
						String name = preStat_.charList.get(i).characterName;
						fname = "log"+File.separator+GrxWorldStateItem.this.getName()+"_"+name+".csv";
			  			logger_.saveCSV(fname, name);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					manager_.processingWindow_.setVisible(false);
				}
			}
		};
		t.start();
	}
	
	public Double getTime(int pos) {
		if (pos < 0)
			return null;
		Double t = super.getTime(pos);
		if (t == null && lastCharName_ != null) {
			try {
				float[] f = logger_.get(lastCharName_, pos);
				t = (double)f[0];
				setTimeAt(pos, t);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    return t;
	}
	
	public void extendTime(double time) {
		SimulationTime stime = new SimulationTime();
		stime.setCurrentTime(preStat_.time);
		stime.setStartTime(preStat_.time);
	
		double val = getDbl("logTimeStep",0.001);
		stime.setTimeStep(val);
		setDbl("logTimeStep",val);
		
		val = getDbl("totalTime", 20.0) + time;
	    stime.setTotalTime(val);
	    setDbl("totalTime", val);
	    
		logger_.initCollisionLog(stime);
	}
	
	public static class WorldStateEx {
		public double time;
		public Collision[] collisions;
		private List<CharacterStateEx> charList = new ArrayList<CharacterStateEx>();
		private Map<String, CharacterStateEx> charMap = new HashMap<String, CharacterStateEx>();
		
		public WorldStateEx() {}
		
		public WorldStateEx(WorldState wstate) {
			setWorldState(wstate);
		}
		
		public CharacterStateEx get(int idx) {
			return charList.get(idx);
		}
		
		public CharacterStateEx get(String charName) {
			return charMap.get(charName);
		}
		
		private CharacterStateEx _get(String charName) {
			CharacterStateEx c = get(charName);
			if (c == null) {
				c = new CharacterStateEx();
				c.characterName = charName;
				charMap.put(charName, c);
				charList.add(c);
			}
			return c;
		}
		
		public int size() {
			return charList.size();
		}
		
		public String[] characters() {
			String[] chars = new String[charList.size()];
			for (int i=0; i<charList.size(); i++) 
				chars[i] = charList.get(i).characterName;
			return chars;
		}
		
		public void setWorldState(WorldState wstate) {
			time = wstate.time;
			collisions = wstate.collisions;
			for (int i=0; i<wstate.characterPositions.length; i++) {
				_get(wstate.characterPositions[i].characterName).position = 
					wstate.characterPositions[i].linkPositions;
			}
		}
		
		public void setSensorState(String charName, SensorState state) {
			_get(charName).sensorState = state;
		}
		
		public void setTargetState(String charName, double[] targets) {
			_get(charName).targetState = targets;
		}
		
		public void setServoState(String charName, long[] servoStat) {
			_get(charName).servoState = servoStat;
		}
		
		public void setCalibState(String charName, long[] calibStat) {
			_get(charName).calibState = calibStat;
		}
	}
	
	public static class CharacterStateEx {
		public String    characterName;
		public LinkPosition[] position;
		public SensorState sensorState;
		public double[]    targetState;
		public long[]       servoState;
		public long[]       calibState;
	}
	
	private static class SensorInfoLocal implements Comparable {
		String name;
		String type; //#####[Changed] int -> string £õ5¡¦X£ì"¡¦I
		int id;
		public SensorInfoLocal(SensorInfo info) {
			name = info.name;
			// ##### [Changed] NewModelLoader.IDL
			//if( info.type.equals("Force") )
			//{
			    //type = Integer.valueOf(SensorType.FORCE_SENSOR).intValue(); 
			//}
			//else if( info.type.equals("RateGyro") )
			//{
                //type = SensorType.RATE_GYRO; 
			//}
			//else if( info.type.equals("Acceleration") )
			//{
                //type = SensorType.ACCELERATION_SENSOR; 
			//}
			// type = info.type.value();
			type = info.type;
			// ###### [Changed]


			id = info.id;
		}

		public int compareTo(Object o) {
			if (o instanceof SensorInfoLocal) {
				SensorInfoLocal s = (SensorInfoLocal) o;
				//#####[Changed] int -> string
				//if (type != s.type)
				//    return -1;
				//else 
				if (getOrder(type) < getOrder(s.type)) 
				    return -1;
				else {
					if (id < s.id)
					return -1;
				}
			}
			return 1;
		}
		
		private int getOrder(String type) {
			if (type.equals("Force")) 
				return 0;
			else if (type.equals("RateGyro")) 
				return 1;
			else if (type.equals("Acceleration")) 
				return 2;
			else if (type.equals("Vision")) 
				return 3;
			else
				return -1;


		}

	}
} 

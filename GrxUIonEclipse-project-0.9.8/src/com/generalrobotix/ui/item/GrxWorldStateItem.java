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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.Runtime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.vecmath.Matrix3d;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import jp.go.aist.hrp.simulator.*;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.GrxTimeSeriesItem;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.util.AxisAngle4d;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.view.graph.*;
import com.generalrobotix.ui.util.GrxCopyUtil;
import com.generalrobotix.ui.view.GrxLoggerView;

@SuppressWarnings("serial")
public class GrxWorldStateItem extends GrxTimeSeriesItem {
	public static final String TITLE = "World State";
	public static final String DEFAULT_DIR = "log";
	public static final String FILE_EXTENSION = "log";
	
	public static final double DEFAULT_TOTAL_TIME = 20.0;
    private static final int MAX_RAM_BUFFER_SIZE = -1; // 無制限
    private static final int LOAD_LOG_MODITOR_DIM = 32; // プログレスモニター用定数
    private static final long HEAP_MEMORY_TOLERANCE = 4*1024*1024; //残りヒープメモリサイズの許容量
    private static final String OVER_HEAP_LOG_DIR_NAME = "over"; //ヒープメモリを超えたときにログを退避させるディレクトリ
	private static String LOG_DIR;
	
	private WorldStateEx newStat_ = null;
	private WorldStateEx preStat_ = null;
	private int prePos_ = -1;
    
    public  final LogManager logger_ = LogManager.getInstance();
	private float   recDat_[][];
    private String  lastCharName_ = null;
	private boolean initLogFlag_ = false;
	private boolean useDisk_ = true;
	private boolean storeAllPos_ = true;
	
	private Action save_ = new Action(){
        public String getText(){ return "save log"; }
		public void run(){
            if(isRemoved()){
                MessageDialog.openWarning(
                        GrxUIPerspectiveFactory.getCurrentShell(),
                        "Warning! It is not possible to save log.",
                        "Log data is removed.\nPlease increase the bufferSize or set useDisk to true in project file.");
                return;
            }
			_saveLog();
		}
	};
	private Action saveCSV_ = new Action(){
        public String getText(){ return "save log AsCSV"; }
        public void run(){
            if(isRemoved()){
                MessageDialog.openWarning(
                        GrxUIPerspectiveFactory.getCurrentShell(),
                        "Warning! It is not possible to save log.",
                        "Log data is removed.\nPlease increase the bufferSize or set useDisk to true in project file.");
                return;
            }
            _saveCSV();
		}
	};
	private Action clear_ = new Action(){
        public String getText(){ return "clear log"; }
		public void run(){
			if( MessageDialog.openQuestion( null, "Clear Log", "Are you sure to clear log ?" ) )
				clearLog();
		}
	};
	
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
    	
    	Action load = new Action(){
            public String getText(){ return "load log"; }
    		public void run(){
    	        FileDialog fdlg = new FileDialog(GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);
    	        fdlg.setFilterExtensions(new String[]{"*.log"});
    	        final String fPath = fdlg.open();                
    			_loadLog(new File(fPath));
    		}
    	};
		setMenuItem(save_);
		setMenuItem(load);
		setMenuItem(saveCSV_);
		setMenuItem(clear_);

		setExclusive(true);
		setIcon( "world.png" );

		logger_.init();
	}
	
//	public static String[] getStaticMenu() {
//		return new String[]{"create", "load"};
//	}

	public boolean create() {
        clearLog();
        setDbl("totalTime", 20.0);
		setDbl("timeStep", 0.001);
		setDbl("logTimeStep", 0.001);
		setDbl("gravity", 9.8);
		setProperty("method","RUNGE_KUTTA");
		return true;
	}

	public boolean load(File f) {
        _loadLog(f);
        return true;
    }
    
	public void setLogMenus(boolean bAble){
        save_.setEnabled(bAble);
        saveCSV_.setEnabled(bAble);
        clear_.setEnabled(bAble);
    }
    
	public void restoreProperties() {
		super.restoreProperties();
		useDisk_ = isTrue("useDisk", true);
		storeAllPos_ = isTrue("storeAllPosition", storeAllPos_);
        int size = getInt("bufferSize", MAX_RAM_BUFFER_SIZE);
		if ( useDisk_ ) {
			super.setMaximumLogSize(MAX_RAM_BUFFER_SIZE);
		} else {
			GrxDebugUtil.println("GrxWorldStateItem: useDisk = false");
			super.setMaximumLogSize(size);
		}
	}
	
	public void rename(String newName) {
		File oldDir = new File(LOG_DIR+File.separator+getName());
		super.rename(newName);
		File newDir = new File(LOG_DIR+File.separator+getName());
		if (newDir.isDirectory()) 
			newDir.delete();
		
		if (oldDir.isDirectory())
			oldDir.renameTo(newDir);
		else 
			newDir.mkdir();
		
		logger_.setTempDir(newDir.getPath());
	}
	
	public void clearLog() {
		super.clearLog();
        initLogFlag_ = false;
		logger_.init();
		newStat_ = null;
		preStat_ = null;
		prePos_ = -1;
		lastCharName_ = null;
        setLogMenus(false);
		syncExec(new Runnable(){
        	public void run(){
        		notifyObservers("ClearLog");
        	}
        });
	}
	
	@SuppressWarnings("unchecked")
	public void registerCharacter(String cname, BodyInfo binfo) {
		ArrayList<String> logList = new ArrayList<String>();
		logList.add("time");
		logList.add("float");
		
		LinkInfo[] li = binfo.links();
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
		
		for (short i=0; i<jointList.size(); i++) { // short <- its depend on the idl def.
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
			}else if(sensList.get(i).type.equals("Range")){
				logList.add(sname+".range");
				SensorInfo info = sensList.get(i).info;
				int half = (int)(info.specValues[0]/2/info.specValues[1]);// = scanAngle/scanStep
				logList.add("float["+(half*2+1)+"]");
			}else{
			}
		}
		
		logList.add("command");
		logList.add("float["+jointList.size()+"]");
		try {
			logger_.addLogObject(cname, logList.toArray(new String[0]));
		} catch (LogFileFormatException e) {
			e.printStackTrace();
		}
	}

	public void addValue(Double t, Object obj) {
		if ( useDisk_ ) {
            _addValueToLog(t, obj);
        }else {
            if(overPos_ > 0){
                // メモリ＋ファイル記録のログのファイル記録
                _addValueToLog(t, obj);
                ++overPos_;
            } else {
                if( Runtime.getRuntime().freeMemory() < HEAP_MEMORY_TOLERANCE){
                    //メモリからファイルへのログ保存に切替
                    File f = new File(tempDir_);
                    File pf = f.getParentFile();
                    if (!pf.isDirectory()){
                        pf.mkdir();
                    }
                    tempDir_ = tempDirBase_ + getName() + File.separator + OVER_HEAP_LOG_DIR_NAME;
                    changePos_ = super.getLogSize();
                    _addValueToLog(t, obj);
                    ++overPos_;
                } else {
                    super.addValue(t, obj);
                }
            }
        }
    }

    private void _addValueToLog(Double t, Object obj){
        if (obj instanceof WorldStateEx) {
            newStat_ = (WorldStateEx) obj;
            _initLog();
            _toLogFile(logger_);
            super.addValue(newStat_.time, null);
        }
    }

    private void _addValueToLogFromSuperLog(Double t, Object obj, LogManager temp){
        if (obj instanceof WorldStateEx) {
            newStat_ = (WorldStateEx) obj;
            if(temp == logger_){
                _initLog();
            }
            _toLogFile(temp);
        }
    }

    private void _toLogFile(LogManager temp){
        temp.setTime(new Time((float)newStat_.time));
        
        try {
            Collision[] cols = newStat_.collisions;
            if (cols != null && cols.length > 0) {
                List<CollisionPoint> cdList = new ArrayList<CollisionPoint>();
                for (int i=0; i < cols.length; i++) {
                    if( cols[i].points == null){
                        continue;
                    }
                    for (int j=0; j<cols[i].points.length; j++) 
                        cdList.add(cols[i].points[j]);
                }
                CollisionPoint[] cd = cdList.toArray(new CollisionPoint[0]);
                temp.putCollisionPointData(cd);
            }
        } catch (Exception e) {
            GrxDebugUtil.printErr("",e);
        }
        
        for (int i=0; i < newStat_.charList.size(); i++) {
            int k = 0;
            recDat_[i][k++] = (float) newStat_.time;
            CharacterStateEx cpos = newStat_.charList.get(i);
            int len = storeAllPos_ ? cpos.position.length : 1;
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
            
            String cname = cpos.characterName;
            SensorState sdata = cpos.sensorState;
            if (sdata != null) {
                for (int j=0; j<sdata.q.length; j++) {
                    recDat_[i][k++] = (float) sdata.q[j];
                    recDat_[i][k++] = (float) sdata.u[j];
                }
                for (int j=0; j<sdata.force.length; j++) {
                    for (int m=0; m<sdata.force[j].length; m++) 
                        recDat_[i][k++] = (float)sdata.force[j][m];
                }
                for (int j=0; j<sdata.rateGyro.length; j++) {
                    for (int m=0; m<sdata.rateGyro[j].length; m++) 
                        recDat_[i][k++] = (float)sdata.rateGyro[j][m];
                }
                for (int j=0; j<sdata.accel.length; j++) {
                    for (int m=0; m<sdata.accel[j].length; m++) 
                        recDat_[i][k++] = (float)sdata.accel[j][m];
                }
                if (sdata.range != null){
                	for (int j=0; j<sdata.range.length; j++) {
                		for (int m=0; m<sdata.range[j].length; m++) 
                			recDat_[i][k++] = (float)sdata.range[j][m];
                	}
                }
            }
            if (cpos.targetState != null){
            	for (int j=0; j<cpos.targetState.length; j++){
            		recDat_[i][k++]	 = (float)cpos.targetState[j];
            	}
            }
            try {
                temp.put(cname, recDat_[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
	public void init(){
		double val = getDbl("logTimeStep",0.001);
		setDbl("logTimeStep",val);
		val = getDbl("totalTime", DEFAULT_TOTAL_TIME);
		setDbl("totalTime", val);
	}
	
	private void _initLog() {
		if ( initLogFlag_ ){
			return;
		}
        initLogFlag_ = true;
        setLogMenus(true);
		SimulationTime stime = new SimulationTime();
		stime.setCurrentTime(newStat_.time);
		stime.setStartTime(newStat_.time);
		
		double val = getDbl("logTimeStep",0.001);
		stime.setTimeStep(val);
			
		val = getDbl("totalTime", DEFAULT_TOTAL_TIME);
		stime.setTotalTime(val);

		logger_.setTempDir(tempDir_);
		
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
        WorldStateEx ret = null;
        
        int pos = getPosition();
        if(useDisk_){
            if (pos >= 0){
                if (pos == getLogSize()-1 && newStat_ != null){
                    ret = newStat_;
                }
                if (pos != prePos_ && preStat_ != null)
                    ret = getValue(pos);
            }
        } else {
            if( pos > changePos_ && changePos_ >= 0){
                ret = _getValueFromLog( pos - changePos_ );
            } else {
                ret = (WorldStateEx)super.getValue(pos);
            }
        }
		return ret;
	}

	public WorldStateEx getValue(int pos) {
        WorldStateEx ret = null;
		if (pos >= 0){
    		if ( useDisk_ ){
                ret = _getValueFromLog( pos );
            } else {
                if( pos > changePos_ && changePos_ >= 0 ){
                    ret = _getValueFromLog(pos - changePos_);
                } else {
                    ret = (WorldStateEx)super.getValue(pos);
                }
            }
        }
		return ret;
	}
	
    private WorldStateEx _getValueFromLog(int pos){
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
                    if (sdata.range != null){
                    	for (int j=0; j<sdata.range.length; j++) {
                    		for (int m=0; m<sdata.range[j].length; m++) 
                    			sdata.range[j][m] = (double)f[k++];
                    	}
                    }
                }
                if (cpos.targetState != null){
                	for (int j=0; j<cpos.targetState.length; j++){
                		cpos.targetState[j]	= (double)f[k++];
                	}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        prePos_ = pos;
        return preStat_;
    }
    
	private void _loadLog(final File logFile) {
        try {
	        IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    int size = 0;
                    try{
                        ZipFile local = new ZipFile(logFile);
                        size = local.size();
                        local.close();
                    } catch ( IOException ex ){
                        ex.printStackTrace();
                        return;
                    } catch ( Exception ex){
                        ex.printStackTrace();
                        return;
                    }
                    
					monitor.beginTask("Loading log as a file:"+logFile.getName(), size + LOAD_LOG_MODITOR_DIM + 2);
					_loadLog(logFile,monitor);
					monitor.done();
				}
	        };
	        new ProgressMonitorDialog(GrxUIPerspectiveFactory.getCurrentShell()).run(false, true, op);
            setLogMenus(true);
        } catch (InvocationTargetException e) {
        	e.printStackTrace();
        } catch (InterruptedException e) {
        	clearLog();
        }
	}
    
	private void _loadLog(File logFile, IProgressMonitor monitor ) throws InterruptedException{
		try {
			if (logFile == null) 
				logFile = new File("log"+File.separator+getName()+".log");

			if (!logFile.isFile())
				return;

            String fname = logFile.getAbsolutePath();

            clearLog();
            tempDir_ = tempDirBase_ + getName();
			logger_.setTempDir(tempDir_);
			logger_.load(fname, "");
            
			monitor.worked(1);
			final SimulationTime sTime = new SimulationTime();
			logger_.getSimulationTime(sTime);
			syncExec(new Runnable(){
	        	public void run(){
	        		setDbl("totalTime", sTime.getTotalTime());
	    			setDbl("logTimeStep", sTime.getTimeStep());
	        	}
	        });
            
			logger_.openAsRead();
			logger_.openCollisionLogAsRead();
			
            preStat_ = new WorldStateEx();
            Enumeration<? extends ZipEntry> e = new ZipFile(fname).entries();
            while (e.hasMoreElements()) {
                monitor.worked(1);
                if (monitor.isCanceled())
                    throw new InterruptedException();
            	String entry = ((ZipEntry)e.nextElement()).getName();
            	if (entry.indexOf(LogManager.COLLISION_LOG_NAME) > 0 ||
            	    entry.indexOf(LogManager.COLLISION_LOG_DAT_NAME) > 0) {
            	    continue;
                }
                
            	lastCharName_ = new File(entry).getName().split(".tmp")[0];
            	String[] format = logger_.getDataFormat(lastCharName_);
            	List<LinkPosition> lposList = new ArrayList<LinkPosition>();
            	int jointCount = 0;
            	int forceCount = 0;
            	int gyroCount = 0;
            	int accelCount = 0;
            	int rangeCount = 0;
            	
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
                	} else if (str[1].equals("range")){
                		rangeCount++;
                	}
            	}
            	
  				CharacterStateEx cpos = new CharacterStateEx();
  				cpos.characterName = lastCharName_;
  				cpos.position = lposList.toArray(new LinkPosition[0]);
            	if (jointCount+forceCount+accelCount+gyroCount+rangeCount > 1) {
            		SensorState	sdata = new SensorState();
       				sdata.q = new double[jointCount];
       				sdata.u = new double[jointCount];
      				sdata.force = new double[forceCount][6];
					sdata.rateGyro  = new double[gyroCount][3];
					sdata.accel = new double[accelCount][3];
					sdata.range = new double[rangeCount][]; // TODO
    				cpos.sensorState = sdata;
            	}
            	preStat_.charList.add(cpos);
            	preStat_.charMap.put(lastCharName_, cpos);
            }
            
            int datLen = logger_.getRecordNum(lastCharName_);
            
            // プログレス用変数の初期化
            int workdim[] = new int[LOAD_LOG_MODITOR_DIM];
            int workdimCounter = 0;
            int localLength = workdim.length;
            
            for( int i = 0; i < localLength; ++i){
                workdim[i] = datLen * (i + 1) / localLength; 
            }
            
            monitor.worked(1);

            if( !useDisk_ ){
                recDat_ = new float[logger_.getLogObjectNum()][];
                for (int i=0; i<recDat_.length; i++)
                    recDat_[i] = new float[logger_.getDataLength(preStat_.charList.get(i).characterName)];
            }
            
            for (int i=0; i < datLen; i++){
                if ( useDisk_ ) {
                    try{
                        super.addValue( null , null);
                    } catch (Exception ex){
                        ex.printStackTrace();
                        break;
                    }
                } else {
                    //メモリーに展開する場合
                    WorldStateEx worldState = _getValueFromLog(i);
                    
                    if( Runtime.getRuntime().freeMemory() < HEAP_MEMORY_TOLERANCE){
                        //ヒープメモリが足りない場合の処理
                        _createOverLog( worldState.time,
                                        i  == 0 ? worldState.time : getTime(0),
                                        i, datLen - i);
                        break;
                    }
                    
                    try{
                        super.addValue( worldState.time , worldState.clone());
                    } catch (Exception ex){
                        ex.printStackTrace();
                        break;
                    }
                }
                
                //プログレスチェックと処理
                if ( workdim[workdimCounter] < i){
                    workdimCounter ++;
                    monitor.worked(1);
                }
            }
            
            syncExec(new Runnable(){
            	public void run(){
            		setPosition(0);
            	}
            });
            monitor.worked(1);
        } catch (FileOpenFailException e) {
            e.printStackTrace();
        } catch (LogFileFormatException e) {
            e.printStackTrace();
		} catch (IOException e) {
            e.printStackTrace();
        }
	}

	private void _saveLog() {
        FileDialog fdlg = new FileDialog(GrxUIPerspectiveFactory.getCurrentShell(), SWT.SAVE);
        fdlg.setFileName(GrxWorldStateItem.this.getName()+".log");
        fdlg.setFilterExtensions(new String[]{"*.log"});
        final String fPath = fdlg.open();
        if (fPath != null) {
	 		Thread t = new Thread() {
				public void run() {
					try {
                        if( useDisk_ ){
                            // 従来の処理
                            logger_.closeAsWrite();
                            logger_.closeCollisionLogAsWrite();
                            logger_.save(fPath, getName()+".prj");
                        } else {
                            // オンメモリデータをファイルへ
                            LogManager temp = _restoreLogFileFromSuperLog();
                            if(temp != null){
                                temp.save(fPath, getName()+".prj");
                                if(temp != logger_){
                                    temp.closeReads();
                                }
                            }
                        }
					} catch (IOException ex){
                        ex.printStackTrace();
                    } catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};
			t.start();
        }
	}
    
    private void _createOverLog(double currentTime, double startTime, int changePos, int overPos ){
        changePos_ = changePos;
        overPos_ = overPos;

        // サブディレクトリOVER_HEAP_LOG_DIR_NAMEにuseDiskがfalseの時と同様のログを生成
        LogManager temp = LogManager.getTempInstance();
        if( temp == null){
            return;
        }
        String overDir = tempDirBase_ + getName() + File.separator + OVER_HEAP_LOG_DIR_NAME;
        temp.setTempDir( overDir );
        SimulationTime stime = new SimulationTime();
        logger_.getSimulationTime(stime);
        stime.setStartTime(currentTime);
        temp.initCollisionLog(stime);
        try {
            temp.openAsWrite(stime, 1);
            temp.openCollisionLogAsWrite();
            temp.separateLogs(changePos);
            temp.closeAsWrite();
            temp.closeCollisionLogAsWrite();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // loggerをOVER_HEAP_LOG_DIR_NAMEディレクトリ以下のファイルを参照するように変更
        logger_.closeReads();
        logger_.setTempDir(overDir);
        try {
            logger_.openAsRead();
            logger_.openCollisionLogAsRead();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        //overPos部分の時間を登録
        for(int i = 0; i < overPos; ++i){
            try{
                super.addValue( (double)logger_.get(lastCharName_, i)[0], null);
            } catch (Exception ex){
                ex.printStackTrace();
                break;
            }
        }
    }
    
    private LogManager _restoreLogFileFromSuperLog() {
        LogManager ret = null;
        if( changePos_ < 0 ){
            // 全てのログがメモリにある場合
            for(int index = 0; index < getLogSize(); ++index){
                TValue localTValue = getObject(index);
                _addValueToLogFromSuperLog(localTValue.getTime(), localTValue.getValue(), logger_);
            }
            try{
                logger_.closeAsWrite();
                logger_.closeCollisionLogAsWrite();
                ret = logger_;
            } catch (IOException ex){
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            //全てのログがメモリにない場合
            // 書き込みファイルストリームを全て閉じる
            try{
                logger_.closeAsWrite();
                logger_.closeCollisionLogAsWrite();
            } catch (IOException ex){
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
            // サブディレクトリOVER_HEAP_LOG_DIR_NAMEの親ディレクトリにuseDiskがtrueの時と同様のログを生成
            LogManager temp = LogManager.getTempInstance();
            if( temp == null){
                return ret;
            }
            temp.setTempDir( tempDirBase_ + getName() );
            SimulationTime stime = new SimulationTime();
            stime.setCurrentTime(preStat_.time);
            stime.setStartTime( super.getTime(0) );
            
            double localTime = getDbl("logTimeStep",0.001);
            stime.setTimeStep(localTime);
                
            localTime = getDbl("totalTime", DEFAULT_TOTAL_TIME);
            stime.setTotalTime(localTime);
            temp.initCollisionLog(stime);
            try {
                temp.openAsWrite(stime, 1);
                temp.openAsRead();
                temp.openCollisionLogAsWrite();
                temp.openCollisionLogAsRead();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            //メモリ上のログをファイルへ展開
            for(int index = 0; index < getLogSize(); ++index){
                TValue localTValue = getObject(index);
                Object val = localTValue.getValue();
                if( val != null){
                    _addValueToLogFromSuperLog(localTValue.getTime(), val, temp);
                    continue;
                }
                break;
            }
            
            try{
                // 残りのサブディレクトリOVER_HEAP_LOG_DIR_NAMEのログを結合
                temp.jointLogs();
                temp.setTime( new Time( stime.getCurrentTime() ) );
                temp.closeAsWrite();
                temp.closeCollisionLogAsWrite();
                ret = temp;
            } catch (IOException ex){
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ret;
    }
	
	private void _saveCSV() {
        DirectoryDialog ddlg = new DirectoryDialog(GrxUIPerspectiveFactory.getCurrentShell());
        String projectDir = Activator.getDefault().getPreferenceStore().getString("PROJECT_DIR");
        if(projectDir.equals(""))
        	projectDir = System.getenv("PROJECT_DIR");
        if(projectDir!=null)
        	ddlg.setFilterPath(projectDir);
        final String dir = ddlg.open();
        if (dir != null){
            Thread t = new Thread() {
    			public void run() {
                    LogManager temp = null;
                    if( !useDisk_ ){                            
                        // オンメモリデータをファイルへ
                        temp = _restoreLogFileFromSuperLog();
                    }
    				for (int i=0; i<preStat_.charList.size(); i++) {
    					String name = preStat_.charList.get(i).characterName;
                        String fname = dir+File.separator+name+".csv";
    					try {
                            if( useDisk_ ){                            
                                logger_.saveCSV(fname, name);
                            }else{
                                // オンメモリデータをファイルへ
                                if(temp != null){
                                    temp.saveCSV(fname, name);
                                }
                            }
    					} catch (FileOpenFailException e) {
    						e.printStackTrace();
    					}
    				}
                    if(temp != null && temp != logger_){
                        temp.closeReads();
                    }
    			}
    		};
    		t.start();
        }
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
		
	    stime.setTotalTime(time);
	    setDbl("totalTime", time);
	    
		//logger_.initCollisionLog(stime);
	    logger_.extendTime(stime);
	}
	
	public void startSimulation(boolean isSimulatingView){
		if(!isSimulatingView){
			GrxLoggerView view =  (GrxLoggerView)manager_.getView( GrxLoggerView.class );
			if( view == null){
	        	IWorkbench workbench = PlatformUI.getWorkbench();
	    		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	    		IWorkbenchPage page = window.getActivePage();
	    		try {
	    			page.showView("com.generalrobotix.ui.view.GrxLoggerViewPart", null, IWorkbenchPage.VIEW_CREATE);  
	    		} catch (PartInitException e1) {
	    			e1.printStackTrace();
	    		}
	        }
		}
		notifyObservers("StartSimulation", isSimulatingView);
	}
	
	public void stopSimulation(){
		notifyObservers("StopSimulation");
	}
    
    public boolean isUseDsik(){ return useDisk_; }

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
		
		public void setServoState(String charName, int[] servoStat) {
			_get(charName).servoState = servoStat;
		}
		
		public void setPowerState(String charName, double voltage, double current){
			_get(charName).powerState = new double[]{voltage, current};
		}
        
        protected Object clone() throws CloneNotSupportedException{
            WorldStateEx ret = new WorldStateEx();
            ret.time = time;
            if(collisions != null){
                ret.collisions = new Collision[]{new Collision()};
                ret.collisions[0].points = collisions[0].points;
            }
            for(CharacterStateEx    i:charList){
                ret.charList.add((CharacterStateEx)i.clone()); 
            }
            for (String i:charMap.keySet()){
                ret.charMap.put(new String(i), (CharacterStateEx)charMap.get(i).clone());
            }
            return ret;
        }
	}
	
	public static class CharacterStateEx {
		public String    characterName;
		public LinkPosition[] position;
		public SensorState sensorState;
		public double[]    targetState;
		public int[]        servoState;
	        public double[]	    powerState;
        
        protected Object clone() throws CloneNotSupportedException{
            CharacterStateEx ret = new CharacterStateEx();
            ret.characterName = new String( characterName );
            if(position != null){
                ret.position = new LinkPosition[ position.length ];
                for(int i = 0; i < position.length; ++i){
                    ret.position[i] = new LinkPosition();
                    if(position[i].p != null){
                        ret.position[i].p = new double[position[i].p.length];
                        GrxCopyUtil.copyDim(position[i].p, ret.position[i].p, position[i].p.length);
                    }
                    if(position[i].R != null){
                        ret.position[i].R = new double[position[i].R.length];
                        GrxCopyUtil.copyDim(position[i].R, ret.position[i].R, position[i].R.length);
                    }
                }
            }
            if(sensorState != null){
                ret.sensorState = new SensorState();
                if( sensorState.accel != null){
                    ret.sensorState.accel = GrxCopyUtil.copyDoubleWDim(sensorState.accel);
                }
                if(sensorState.force != null){
                    ret.sensorState.force = GrxCopyUtil.copyDoubleWDim(sensorState.force);
                }
                if(sensorState.range != null){
                    ret.sensorState.range = GrxCopyUtil.copyDoubleWDim(sensorState.range);
                }
                if(sensorState.rateGyro != null){
                    ret.sensorState.rateGyro = GrxCopyUtil.copyDoubleWDim(sensorState.rateGyro);
                }
                if(sensorState.dq != null){
                    ret.sensorState.dq = new double[sensorState.dq.length];
                    GrxCopyUtil.copyDim(sensorState.dq, ret.sensorState.dq, sensorState.dq.length);
                }
                if(sensorState.q != null){
                    ret.sensorState.q = new double[sensorState.q.length];
                    GrxCopyUtil.copyDim(sensorState.q, ret.sensorState.q, sensorState.q.length);
                }
                if(sensorState.u != null){
                    ret.sensorState.u = new double[sensorState.u.length];
                    GrxCopyUtil.copyDim(sensorState.u, ret.sensorState.u, sensorState.u.length);
                }
            }
            if(targetState != null){
                ret.targetState = new double[targetState.length];
                GrxCopyUtil.copyDim(targetState, ret.targetState, targetState.length);
            }
            if(servoState != null){
                ret.servoState = new int[servoState.length];
                GrxCopyUtil.copyDim(servoState, ret.servoState, servoState.length);
            }
            return ret;
        }
	}
	
	private static class SensorInfoLocal implements Comparable {
		String name;
		String type; //#####[Changed] int -> string ｕ5・Xｌ"・I
		int id;
		SensorInfo info;
		public SensorInfoLocal(SensorInfo _info) {
			info = _info;
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
			else if (type.equals("Range"))
				return 4;
			else
				return -1;


		}

	}
} 

/*
 *  GrxConfigPane.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  2004/03/19
 */

package com.generalrobotix.ui.util;

import java.util.HashMap;

import org.omg.CORBA.*;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.*;
import org.omg.PortableServer.POA;

public class GrxCorbaUtil {

	private static ORB orb_ = null;
	private static HashMap<String, NamingContext> namingContextList_ = null;

	public static org.omg.CORBA.ORB getORB(String[] argv) {
		if (orb_ == null) {
			java.util.Properties props = null;
			props = System.getProperties();
			//props.put("org.omg.CORBA.ORBClass","com.ooc.OBServer.ORB");
			//props.put("org.omg.CORBA.ORBSingletonClass","com.ooc.CORBA.ORBSingleton");
			orb_ = ORB.init(argv,props);
		}
		return orb_;
	}
  
	public static org.omg.CORBA.ORB getORB() {
		return getORB(null);
	}
  
	public static NamingContext getNamingContext() {   
		try {
			org.omg.CORBA.Object obj = getORB().resolve_initial_references("NameService");
			return NamingContextHelper.narrow(obj);
		} catch (Exception excep) {     
			GrxDebugUtil.printErr("getNamingContext:NG"); 
		}
		return null;
	}

	private static HashMap<String, NamingContext> getNamingContextList() {
		if (namingContextList_ == null)
			namingContextList_ = new HashMap<String, NamingContext>();
		return namingContextList_;
	}

	public static NamingContext getNamingContext(String nsHost, int nsPort) {
		getORB();
		String nameServiceURL = "corbaloc:iiop:"+nsHost+":"+nsPort+"/NameService";
		NamingContext ncxt = getNamingContextList().get(nameServiceURL);
		try {
			if (ncxt == null) {
				org.omg.CORBA.Object obj = orb_.string_to_object(nameServiceURL);
				if (!obj._non_existent()) {
					ncxt = NamingContextHelper.narrow(obj);
					getNamingContextList().put(nameServiceURL,ncxt);
				}
			}
			ncxt._non_existent();
		} catch (Exception excep) {
			ncxt = null;
			namingContextList_.remove(nameServiceURL);
			//GrxDebugUtil.printErr("getNamingContext:NG("+nsHost+","+nsPort+")");
		}
		return ncxt;
	}

	public static org.omg.CORBA.Object getReference(String id, String nsHost, int nsPort) {
		NamingContext namingContext = getNamingContext(nsHost,nsPort);
		org.omg.CORBA.Object obj = null;
		try {
			NameComponent[] nc = new NameComponent[1];
			nc[0] = new NameComponent(id,"");
			obj = namingContext.resolve(nc);
		} catch(Exception excep) {
			obj = null;
			GrxDebugUtil.printErr("getReference:NG("+id+","+nsHost+","+nsPort+")");
		}
		return obj;
	}

/* public static Plugin getPlugin(String id,String nsHost,int nsPort) {
		Plugin p = null;
		try {
			p = jp.go.aist.hrp.simulator.PluginHelper.narrow(getReference(id,nsHost,nsPort));
			p._non_existent();
		} catch (Exception e) {
			DebugUtil.printErr("getPlugin:NG("+id+","+nsHost+","+nsPort+")");
		}
		return p;
	}

	public static PluginManager getPluginManager(String id,String nsHost,int nsPort) {
		PluginManager p = null;
		try {
			p = jp.go.aist.hrp.simulator.PluginManagerHelper.narrow(getReference(id,nsHost,nsPort));
			p._non_existent();
		} catch (Exception e) {
			DebugUtil.printErr("getPluginManager:NG("+id+","+nsHost+","+nsPort+")");
		}
		return p;
	}
*/

	public static org.omg.CORBA.Object getReferenceURL(String id, String nsHost, int nsPort) {
		org.omg.CORBA.Object obj = null;
		try {
			obj = getORB().string_to_object("corbaloc:iiop:"+nsHost+":"+nsPort+"/"+id);
		} catch(Exception e) {
			obj = null;
			GrxDebugUtil.printErr("getReferenceURL:NG"+id+","+nsHost+","+nsPort+")");
		}
		return obj;
	}

	public static String getIOR(org.omg.CORBA.Object obj) {
		return getORB().object_to_string(obj);
	}

	static String getIOR(String id,String nsHost, int nsPort) {
		return getIOR(getReference(id,nsHost,nsPort));
	}

	public static POA getRootPOA() throws InvalidName {
		org.omg.CORBA.Object CORBA_obj = getORB().resolve_initial_references("RootPOA");
		org.omg.PortableServer.POA rootPOA = org.omg.PortableServer.POAHelper.narrow(CORBA_obj);
		return rootPOA;
	}
	public static org.omg.PortableServer.POAManager getRootPOAManager() throws InvalidName {
		org.omg.PortableServer.POAManager manager = getRootPOA().the_POAManager();
		return manager;
	}

	public static boolean isConnected(org.omg.CORBA.Object obj) {
		try {
			obj._non_existent();
		} catch(Exception ex) {
			return false;
		}
		return true;
	}

	public static boolean isConnected(String id,String nsHost, int nsPort) {
		org.omg.CORBA.Object obj = GrxCorbaUtil.getReference(id,nsHost,nsPort);
		return isConnected(obj);
	}

	public static String[] getObjectNameList(String nsHost,int nsPort) {
		int a = 100;
		BindingListHolder bl 	 = new BindingListHolder();
		BindingIteratorHolder bi = new BindingIteratorHolder();
		String[] ret = null;
		try {
			getNamingContext(nsHost,nsPort).list(a,bl,bi);
			ret = new String[bl.value.length];
			for (int i=0;i<bl.value.length;i++)
				ret[i] = bl.value[i].binding_name[0].id;
		} catch(Exception ex) {
			ret = null;
			GrxDebugUtil.println("getObjectList:NG("+nsHost+","+String.valueOf(nsPort)+")");
		}

		return ret;
	}
}

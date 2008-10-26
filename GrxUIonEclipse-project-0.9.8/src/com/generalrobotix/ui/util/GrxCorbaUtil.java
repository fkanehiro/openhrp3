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

/**
 * @brief corba utility functions
 */
public class GrxCorbaUtil {

	private static ORB orb_ = null;
	private static HashMap<String, NamingContext> namingContextList_ = null;
	private static final int DEFAULT_PORT=2809;
	private static final String DEFAULT_HOST="localhost";
	
	/**
	 * @brief initialize and get ORB
	 * @param argv arguments for ORB initialization
	 * @return ORB
	 */
	public static org.omg.CORBA.ORB getORB(String[] argv) {
		if (orb_ == null) {
			java.util.Properties props = null;
			props = System.getProperties();
			orb_ = ORB.init(argv,props);
		}
		return orb_;
	}
  
	/**
	 * @brief initialize and get ORB
	 * @return ORB
	 */
	public static org.omg.CORBA.ORB getORB() {
		return getORB(null);
	}
  
	/**
	 * @brief get port number where naming server is listening
	 * @return port number where naming server is listening
	 */
	public static int nsPort(){
		int nsPort = DEFAULT_PORT;
		try {
			nsPort = Integer.parseInt(System.getenv("NS_PORT"));
		} catch (Exception e) {
			nsPort = DEFAULT_PORT;
		}
		return nsPort;
	}
	
	/**
	 * @brief get hostname where naming server is running
	 * @return hostname where naming server is running
	 */
	public static String nsHost(){
		String nsHost = System.getenv("NS_HOST");
		if (nsHost == null) {
			nsHost = DEFAULT_HOST;
		}
		return nsHost;
	}
	
	/**
	 * @brief get naming context
	 * @return naming context
	 */
	public static NamingContext getNamingContext() {   
		return getNamingContext(nsHost(), nsPort());
	}

	/**
	 * @brief get naming context from name server which is running on the specified hostname and port number
	 * @param nsHost hostname where name server is running
	 * @param nsPort port number where name server is listening
	 * @return naming context
	 */
	public static NamingContext getNamingContext(String nsHost, int nsPort) {
		getORB();
		String nameServiceURL = "corbaloc:iiop:"+nsHost+":"+nsPort+"/NameService";
		NamingContext ncxt = getNamingContextList().get(nameServiceURL);
		try {
			if (ncxt == null) {
				org.omg.CORBA.Object obj = orb_.string_to_object(nameServiceURL);
				ncxt = NamingContextHelper.narrow(obj);
				getNamingContextList().put(nameServiceURL,ncxt);
			}
			ncxt._non_existent();
		} catch (Exception excep) {
			ncxt = null;
			getNamingContextList().remove(nameServiceURL);
		}
		return ncxt;
	}

	/**
	 * @brief get map between naming service location and naming context
	 * @return map
	 */
	private static HashMap<String, NamingContext> getNamingContextList() {
		if (namingContextList_ == null)
			namingContextList_ = new HashMap<String, NamingContext>();
		return namingContextList_;
	}

	/**
	 * @brief get CORBA object which is associated with id
	 * @param id id of the CORBA object
	 * @return CORBA object associated with id
	 */
	public static org.omg.CORBA.Object getReference(String id) {
		return  getReference(id, nsHost(), nsPort());
	}

	/**
	 * @brief get CORBA object which is associated with id
	 * @param id id of the CORBA object
	 * @param nsHost hostname where name server is running
	 * @param nsPort port number where name server is listening
	 * @return CORBA object associated with id
	 */
	public static org.omg.CORBA.Object getReference(String id, String nsHost, int nsPort) {
		NamingContext namingContext = getNamingContext(nsHost, nsPort);
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

	public static String[] getObjectNameList() {
		return _getObjectNameList(getNamingContext());
	}

	public static String[] getObjectNameList(String nsHost, int nsPort) {
		return _getObjectNameList(getNamingContext(nsHost, nsPort));
	}

	private static String[] _getObjectNameList(NamingContext cxt) {
		int a = 100;
		BindingListHolder     bl = new BindingListHolder();
		BindingIteratorHolder bi = new BindingIteratorHolder();
		String[] ret = null;
		try {
			cxt.list(a, bl, bi);
			ret = new String[bl.value.length];
			for (int i=0; i<bl.value.length; i++) {
				ret[i] = bl.value[i].binding_name[0].id;
			}
		} catch(Exception ex) {
			ret = null;
			GrxDebugUtil.printErr("getObjectList:NG", ex);
		}

		return ret;
	}
}

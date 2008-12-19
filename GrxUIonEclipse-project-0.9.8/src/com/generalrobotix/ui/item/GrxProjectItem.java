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
 *  GrxProjectItem.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.item;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.GrxConfigBundle;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.GrxXmlUtil;

@SuppressWarnings({ "unchecked", "serial" })
public class GrxProjectItem extends GrxBaseItem {
	public static final String TITLE = "Project";
	public static final String DEFAULT_DIR = "project";
	public static final String FILE_EXTENSION = "xml";

	public static final int MENU_CREATE=0, MENU_RESTORE=1, MENU_LOAD=2, MENU_SAVE=3, MENU_IMPORT=4;
	private Vector<Action> menu_;
	
    private static final String MODE_TAG = "mode";
    private static final String WINCONF_TAG = "windowconfig";

	private Document doc_;
    private DocumentBuilder builder_;
    private Transformer transformer_;
    
    private Map<String, ModeNodeInfo> modeInfoMap_ = new HashMap<String, ModeNodeInfo>();
    
    private class ModeNodeInfo {
    	Element  root;
    	List     propList;
    	NodeList itemList;
    	NodeList viewList;
    	Element  windowConfig;
    }
    
	public GrxProjectItem(String name, GrxPluginManager manager) {
		super(name, manager);
//		setIcon(manager_.ROBOT_ICON);
		DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
		TransformerFactory tffactory = TransformerFactory.newInstance();
	
		try {
			builder_ = dbfactory.newDocumentBuilder();
			transformer_ = tffactory.newTransformer();
            transformer_.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer_.setOutputProperty(OutputKeys.METHOD, "xml");
            setDefaultDirectory();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		}
	}

	public boolean create() {
		for (int i=0; ; i++) {
			File f = new File(getDefaultDir().getAbsolutePath()+"/"+"newproject"+i+".xml");
			if (!f.isFile()) {
				setName(f.getName().split("[.]")[0]);
				break;
			}
		}
			
		doc_ = builder_.newDocument();
		element_ = doc_.createElement("grxui");
		element_.appendChild(doc_.createTextNode("\n"));
		doc_.appendChild(element_);
		_updateModeInfo();
		return true;
	}

	private void _updateModeInfo() {
		modeInfoMap_.clear();

		NodeList modeList = doc_.getElementsByTagName(MODE_TAG);
		for (int i=0; i<modeList.getLength(); i++) {
            ModeNodeInfo mi = new ModeNodeInfo();
			mi.root = (Element)modeList.item(i);
            modeInfoMap_.put(mi.root.getAttribute("name"), mi);

			// property node 
			NodeList propList = mi.root.getElementsByTagName(PROPERTY_TAG);
			List<Element> elList = new ArrayList<Element>();
			for (int j=0; j<propList.getLength(); j++) {
				if (propList.item(j).getParentNode() == mi.root) 
					elList.add((Element)propList.item(j));
			}
			mi.propList =  elList;

            // item node
			mi.itemList = mi.root.getElementsByTagName(ITEM_TAG);

            // view node
			mi.viewList = mi.root.getElementsByTagName(VIEW_TAG);
			
            // window config element
			NodeList wconfList = mi.root.getElementsByTagName(WINCONF_TAG);
			if (wconfList.getLength() > 0)
				mi.windowConfig =  (Element)wconfList.item(0);
		}
	}

    private ModeNodeInfo _getModeNodeInfo(String mode) {
        ModeNodeInfo mi = modeInfoMap_.get(mode);
        if (mi == null) {
            mi = new ModeNodeInfo();

		    NodeList nodeList = doc_.getElementsByTagName(MODE_TAG);
		    for (int i=0; i<nodeList.getLength(); i++) {
			    Element e = (Element)nodeList.item(i);
			    if (e.getAttribute("name").equals(mode)) {
				    mi.root = e;
                    break;
                }
		    }
		    if (mi.root == null) {
			    mi.root = doc_.createElement(MODE_TAG);
			    mi.root.setAttribute("name", mode);
			    element_.appendChild(doc_.createTextNode(INDENT4));
			    element_.appendChild(mi.root);
			    element_.appendChild(doc_.createTextNode("\n"));
            } 

			_updateModeInfo();
        }

        return mi;
    }
	
	public Element getWindowConfigElement(String mode) {
        return _getModeNodeInfo(mode).windowConfig; 
	}
   
    private Element _createWindowConfigElement(String mode) {
        ModeNodeInfo mi = _getModeNodeInfo(mode);
		mi.windowConfig = doc_.createElement("windowconfig");
		mi.root.appendChild(doc_.createTextNode("\n"+INDENT4+INDENT4));
		mi.root.appendChild(mi.windowConfig);
		mi.root.appendChild(doc_.createTextNode("\n"+INDENT4));
        return mi.windowConfig;
    }

	private void storeMode(String mode) {
		Element modeEl = _getModeNodeInfo(mode).root;
		NodeList list = modeEl.getChildNodes();
		for (int i=list.getLength()-1; i>=0; i--)
			modeEl.removeChild(list.item(i));
		
		modeEl.appendChild(doc_.createTextNode("\n"));
		
		List<GrxBaseItem> itemList = manager_.getActiveItemList();
		for (int i=0; i<itemList.size(); i++) {
			GrxBaseItem item = itemList.get(i);
			modeEl.appendChild(doc_.createTextNode(INDENT4+INDENT4));
			item.setDocument(doc_);
			modeEl.appendChild(item.storeProperties());
			modeEl.appendChild(doc_.createTextNode("\n"));
		}
		
		List<GrxBaseView> viewList = manager_.getActiveViewList();
		for (int i=0; i<viewList.size(); i++) {
			GrxBaseView view = viewList.get(i);
			if (view.propertyNames().hasMoreElements()) {
				modeEl.appendChild(doc_.createTextNode(INDENT4+INDENT4));
				view.setDocument(doc_);
				modeEl.appendChild(view.storeProperties());
				modeEl.appendChild(doc_.createTextNode("\n"));
			}
		}
		
		modeEl.appendChild(doc_.createTextNode(INDENT4));
	}
	
	public Vector<Action> getMenu() {

		if( menu_ == null )
			menu_ = new Vector<Action>();

		// MENU_CREATE=0
		menu_.add( new Action(){
			public String getText(){ return "Create Project"; }
			public void run(){
				boolean ans = MessageDialog.openConfirm( null, "Create New Project", "Before create new Project,\nRemove all items ?" );
				if ( ans )
					manager_.removeAllItems();
				else if ( ans == false )
					return;
				GrxProjectItem.this.create();
			}
		} );
		// MENU_RESTORE=1
		menu_.add( new Action(){
			public String getText(){ return "Restore Project"; }
			public void run(){
				restoreProject();
			}
		} );
		// MENU_LOAD=2
		menu_.add( new Action(){
			public String getText(){ return "Load Project"; }
			public void run(){
				load();
			}
		} );
		// MENU_SAVE=3
		menu_.add( new Action(){
			public String getText(){ return "Save Project"; }
			public void run(){
				saveAs();
			}
		} );
		// MENU_IMPORT=3
		menu_.add( new Action(){
			public String getText(){ return "Import ISE Project"; }
			public void run(){
				importISEProject();
			}
		} );

		return menu_;
	}
	
	public void save(File f) {
		if (f.exists()) {
			if (!f.isFile())
               return;
			boolean ans = MessageDialog.openConfirm( null, "Save Project",
					"Project: "+f.getName()+" is already exist.\n" +
					"Overwrite this file ?" );
			if (ans == false)
				return;
		}
		
		String mode = manager_.getCurrentModeName();
		storeMode(mode);

		setName(f.getName().split("[.]")[0]);
      	setURL(f.getAbsolutePath());
      	
        // manager_.getFrame().update(manager_.getFrame().getGraphics());
      	/*
 		int ans = JOptionPane.showConfirmDialog(
				manager_.getFrame(), 
				"Save current Window Configuration ?",
				"Save Window Config.",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, 
				manager_.ROBOT_ICON);
		if (ans == JOptionPane.CANCEL_OPTION)
			return;
		
		if (ans == JOptionPane.YES_OPTION) {
			Element we = getWindowConfigElement(mode);
            if (we == null) 
               we = _createWindowConfigElement(mode);
			manager_.getFrame().storeConfig(we);
		}
      	*/

      	if (f == null)
			f = new File(getDefaultDir().getAbsolutePath()+"/"+getName()+".xml");
		
		if (!f.getAbsolutePath().endsWith(".xml"))
			f = new File(f.getAbsolutePath()+".xml");
		
	   	try {
	  		DOMSource src = new DOMSource();
	  		src.setNode(doc_);
	  		StreamResult target = new StreamResult();
	  		target.setOutputStream(new FileOutputStream(f));
	  		transformer_.transform(src, target);
	   	} catch (TransformerConfigurationException e) {
	     		e.printStackTrace();
	   	} catch (FileNotFoundException e) {
	     		e.printStackTrace();
	   	} catch (TransformerException e) {
	     		e.printStackTrace();
	   	}
	}
	
	public void saveAs() {
		String path = getURL(true);
		if (path == null)
			path = getDefaultDir().getAbsolutePath()+"/"+getName()+".xml";
		
		File initialFile = new File(path);

		IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		FileDialog fdlg = new FileDialog( window.getShell(), SWT.SAVE);
		String[] exts = { "*.xml" }, extNames={"GrxUI Project"};
		fdlg.setFilterExtensions( exts );
		fdlg.setFilterNames( extNames );

		fdlg.setFilterPath( initialFile.getParent() );
		fdlg.setFileName( initialFile.getName() );
		
		String fPath = fdlg.open();
		if( fPath != null ) {
			File f = new File(fPath);
			save( f );
		}	
	}

	/**
	 * @brief load a project
	 */
	public void load() {
		String path = getURL(true);
		File filterPath = null;
		if (path == null){
			filterPath = getDefaultDir();
		} else {
			File tempFile = new File(path);
			filterPath = tempFile.getParentFile();
		}
		
		
		IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		FileDialog fdlg = new FileDialog( window.getShell(), SWT.OPEN);
		String[] fe = { "*.xml" };
		fdlg.setFilterExtensions( fe );
		if( filterPath != null )
			fdlg.setFilterPath(filterPath.getPath());
		String fPath = fdlg.open();
		if( fPath != null ) {
			File f = new File(fPath);
			load(f);
			//Thread t = new Thread() {
//				public void run() {
					restoreProject();
	//			}
		//	};
			//t.start();
		}
	}

	/**
	 * @brief load a project file
	 * @param f a project file
	 * @return true if loaded successfully, false otherwise
	 */
	public boolean load(File f) {
		System.out.println( "[ProjectItem]@load ProjectFile load "+ f.toString());
		
		if (f == null || !f.isFile())
			return false;
		manager_.removeAllItems();
		setName(f.getName().split("[.]")[0]);

		// set PROJECT_DIR, which is referred to in a project file
		String dir = f.getParent();
		if (dir!=null) {
		    System.setProperty("PROJECT_DIR", dir);
		    System.out.println("PROJECT_DIR is set to "+dir);
		}
		
    	try {
      		doc_ = builder_.parse(f);
      		element_ = doc_.getDocumentElement();
      		_updateModeInfo();
      		file_ = f;
      		setURL(f.getAbsolutePath());
    	} catch (Exception e) {
      		GrxDebugUtil.printErr("project load:",e);
      		file_ = null;
      		return false;
    	}
		
		// register mode
		GrxBaseItem selectedMode = manager_.getSelectedItem(GrxModeInfoItem.class, null);
		NodeList list = doc_.getElementsByTagName(MODE_TAG);
		for (int i=0; i<list.getLength(); i++) {
			Element modeEl = (Element) list.item(i);
			String modeName = modeEl.getAttribute("name");
			if (modeName == null) 
				continue;
			
			GrxBaseItem item = manager_.createItem(GrxModeInfoItem.class, modeName);
			if (item != null)  {
				item.setElement(modeEl);
				if (GrxXmlUtil.getBoolean(modeEl, "select", false)) {
					selectedMode = item;
				} else {
					manager_.setSelectedItem(selectedMode, false);
				}
			}
		}
		
		if (selectedMode != null)
			manager_.setSelectedItem(selectedMode, true);

		return true;
	}
	
	public void restoreProject() {

		String mode = manager_.getCurrentModeName();
		System.out.println("Restore Project (Mode:" +mode+")");

		IRunnableWithProgress runnableProgress = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException {
				String mode = manager_.getCurrentModeName();
				monitor.beginTask("Restore Project (Mode:" +mode+")", 10 );
				restoreProject_work(mode, monitor);
				monitor.done();
			}
		};
		ProgressMonitorDialog progressMonitorDlg = new ProgressMonitorDialog(null);
		try {
			progressMonitorDlg.run(false,false, runnableProgress);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void restoreProject_work(String mode, IProgressMonitor monitor) {
		manager_.restoreProcess();

		monitor.worked(1);

		if (file_ == null || !file_.isFile()) 
			return;
	
		ModeNodeInfo minfo =  modeInfoMap_.get(mode);

        monitor.worked(1);
        
		List propList = minfo.propList;
		if (propList != null) {
			for (int i=0; i<propList.size(); i++) {
				Element propEl = (Element)propList.get(i);
				String key = propEl.getAttribute("name");
				String val = propEl.getAttribute("value");
				setProperty(key, val);
			}
		}
		if (minfo.viewList != null) {
			for (int i = 0; i < minfo.viewList.getLength(); i++)
				_restorePlugin((Element) minfo.viewList.item(i));
		}

		monitor.worked(1);
		
		List<GrxBaseView> vl = manager_.getActiveViewList();
		for (int i=0; i<vl.size(); i++) 
			vl.get(i).restoreProperties();

		monitor.worked(1);
		
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		monitor.worked(1);
		
		if (minfo.itemList != null) {
            List<GrxBaseItem> il = new ArrayList<GrxBaseItem>();
			for (int i = 0; i < minfo.itemList.getLength(); i++) {
				GrxBaseItem p = (GrxBaseItem)_restorePlugin((Element) minfo.itemList.item(i));
                if (p != null)
                     il.add(p);
            }
            
            // for a item that is exclusive selection reselect 
            for (int i=0; i<il.size(); i++) {
			    GrxBaseItem item = il.get(i);
                boolean select = GrxXmlUtil.getBoolean(item.getElement(), "select", false);
			    //manager_.setSelectedItem(item, select);
            }
		}

		monitor.worked(1);
	
//		manager_.processingWindow_.setVisible(false);
	}
	
	private GrxBasePlugin _restorePlugin(Element e) {
		String iname = e.getAttribute("name");
		if (iname == null || iname.length() == 0)
			return null;
		manager_.pluginLoader_.addURL(GrxXmlUtil.expandEnvVal(e.getAttribute("lib")));
		Class cls = manager_.registerPlugin(e.getAttribute("class"));
		if (cls == null)
			return null;
	//TODO: プログレスバーに変更する？いっそいらない？	
//		manager_.processingWindow_.setMessage(
//			"restoring plugin ... \n  " +cls.getSimpleName()+" : "+iname);
		
		GrxBasePlugin plugin = null;
		if (GrxBaseItem.class.isAssignableFrom(cls)) {
			Class<? extends GrxBaseItem> icls = (Class<? extends GrxBaseItem>) cls;
			String url = e.getAttribute("url");
			if (!url.equals(""))
				plugin = manager_.loadItem(icls, iname, url);
			else
				plugin = manager_.createItem(icls, iname);

			plugin = manager_.getItem(icls, iname);
		} else {
			plugin = manager_.getView((Class<? extends GrxBaseView>) cls);
		}
        
        if (plugin != null) {
            plugin.setElement(e);
			plugin.restoreProperties();
        }

        return plugin;
	}
	
	private static final File DEFAULT_ISE_PROJECT_DIR = new File("../ISE/Projects");
	public void importISEProject() {

		FileDialog fDialog = new FileDialog(null,SWT.SAVE);

		String [] exts = {"*.prj"};
		String [] filterNames = {"ISE Project File(*.prj)"};
		fDialog.setFilterExtensions(exts);
		fDialog.setFilterNames(filterNames);
		if( DEFAULT_ISE_PROJECT_DIR != null )
			fDialog.setFilterPath( DEFAULT_ISE_PROJECT_DIR.getPath() );

		String openPath = fDialog.open();
		
		if( openPath != null ) {
			//manager_.processingWindow_.setTitle("Importing ISE Project");
			//manager_.processingWindow_.setMessage(" importing ISE Project ...");
			//manager_.processingWindow_.setVisible(true);
			//Thread t = new Thread() {
				//public void run() {
					//File f = fc.getSelectedFile();
					File f = new File( openPath );
					setName(f.getName().split(".prj")[0]);
					importISEProject(f);
					//manager_.processingWindow_.setVisible(false);
					//fc.setSelectedFile(null);
					storeMode(manager_.getCurrentModeName());
				//}
			//};
			//t.start();
		}
	}
	
	private static String ENVIRONMENT_NODE = "jp.go.aist.hrp.simulator.EnvironmentNode";
	private static String ROBOT_NODE = "jp.go.aist.hrp.simulator.RobotNode";
	private static String COLLISIONPAIR_NODE = "jp.go.aist.hrp.simulator.CollisionPairNode";
	private static String GRAPH_NODE = "jp.go.aist.hrp.simulator.GraphNode";
			
	private static String WORLD_STATE_ITEM = "com.generalrobotix.ui.item.GrxWorldStateItem";
	private static String MODEL_ITEM = "com.generalrobotix.ui.item.GrxModelItem";
	private static String COLLISIONPAIR_ITEM = "com.generalrobotix.ui.item.GrxCollisionPairItem";
	private static String GRAPH_ITEM = "com.generalrobotix.ui.item.GrxGraphItem";
	
	public void importISEProject(File f) {
		manager_.removeAllItems();
		
		GrxConfigBundle prop = null;
		try {
			prop = new GrxConfigBundle(f.getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		setProperty("nsHost", GrxCorbaUtil.nsHost());
		Integer nsPort = new Integer(GrxCorbaUtil.nsPort());
		setProperty("nsPort", nsPort.toString());

		String pname = prop.getStr("Project.name", "");
		Class cls = manager_.registerPlugin(WORLD_STATE_ITEM);
		GrxBaseItem newItem = manager_.createItem((Class <? extends GrxBaseItem>)cls, pname);
		newItem.setDbl("totalTime",   prop.getDbl("Project.totalTime", 20.0));
		newItem.setDbl("timeStep",    prop.getDbl("Project.timeStep", 0.001));
		newItem.setDbl("logTimeStep", prop.getDbl("Project.timeStep", 0.001));
		newItem.setProperty("method", prop.getStr("Project.method"));
		manager_.setSelectedItem(newItem, true);

		for (int i = 0; i < prop.getInt("Project.num_object", 0); i++) {
			String header = "Object" + i + ".";
			String oName = prop.getStr(header + "name");
			String cName = prop.getStr(header + "class");
			if (oName == null || cName == null)
				continue;
			
			if (cName.equals(ENVIRONMENT_NODE) || cName.equals(ROBOT_NODE)) {
				cls = manager_.registerPlugin(MODEL_ITEM);
				try {
					URL url = new URL(prop.getStr(header + "url"));
					newItem = manager_.loadItem((Class<? extends GrxBaseItem>)cls, oName, url.getPath());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (newItem == null)
					continue;

	  			if (cName.equals(ENVIRONMENT_NODE)) {
					newItem.setProperty("isRobot", "false");
	  			} else if (cName.equals(ROBOT_NODE)) {
					newItem.setProperty("isRobot", "true");
	  				Enumeration e = prop.keys();
	  				while (e.hasMoreElements()) {
	  					String key = (String)e.nextElement();
	  					if (key.startsWith(header)) {
	  						String newKey = key.substring(header.length());
	  						if (key.endsWith(".angle")) {
	  							newItem.setDbl(newKey, prop.getDbl(key, 0.0));
	  						} else if (key.endsWith(".mode")) {
								newItem.setProperty(newKey, prop.getStr(key, "Torque"));
	  						} else if (key.endsWith(".translation"))  {
								newItem.setDblAry(newKey, 
									prop.getDblAry(key, new double[]{0.0, 0.0, 0.0}));
	  						} else if (key.endsWith(".rotation"))  {
								newItem.setDblAry(newKey, 
									prop.getDblAry(key, new double[]{0.0, 1.0, 0.0, 0.0}));
	  						}
	  					}
	  				}
	  				
					String controller = prop.getStr(header + "controller");
					controller = controller.replaceFirst("openhrp.", "");
					double controlTime = prop.getDbl(header + "controlTime", 0.001);
					newItem.setProperty("controller", controller);
					newItem.setProperty("controlTime", String.valueOf(controlTime));

					String imageProcessor = prop.getStr(header + "imageProcessor");
					if (imageProcessor != null) {
						double imageProcessTime = prop.getDbl(header + "imageProcessTime", 0.001);
						newItem.setProperty("imageProcessor", imageProcessor);
						newItem.setProperty("imageProcessTime", String.valueOf(imageProcessTime));
					}
				}
	  			
			} else if (cName.equals(COLLISIONPAIR_NODE)) {
				cls = manager_.registerPlugin(COLLISIONPAIR_ITEM); 
				newItem = manager_.createItem((Class<? extends GrxBaseItem>)cls, oName);
				newItem.setProperty("objectName1", prop.getStr(header + "objectName1"));
				newItem.setProperty("jointName1",  prop.getStr(header + "jointName1"));
				newItem.setProperty("objectName2", prop.getStr(header + "objectName2"));
				newItem.setProperty("jointName2",  prop.getStr(header + "jointName2"));
				newItem.setProperty("slidingFriction", prop.getStr(header + "slidingFriction", "0.5"));
				newItem.setProperty("staticFriction",  prop.getStr(header + "staticFriction", "0.5"));
				newItem.setProperty("sprintDamperModel", prop.getStr(header + "springDamplerModel", "false"));
				newItem.setProperty("springConstant", prop.getStr(header + "springConstant", "0.0 0.0 0.0 0.0 0.0 0.0"));
				newItem.setProperty("damperConstant", prop.getStr(header + "damperConstant", "0.0 0.0 0.0 0.0 0.0 0.0"));

			} else if (cName.equals(GRAPH_NODE)) {
				cls = manager_.registerPlugin(GRAPH_ITEM);
				newItem = manager_.getItem((Class<? extends GrxBaseItem>)cls, null);
				if (newItem == null)
					newItem = manager_.createItem((Class<? extends GrxBaseItem>)cls, "GraphList1");
				String items = prop.getStr(header + "dataItems");
				newItem.setProperty(oName + ".dataItems", items);
				String[] str = items.split(",");
				String[] p = { 
					"object", "node", "attr", "index", 
					"numSibling", "legend", "color" 
				};
				for (int j = 0; j < str.length; j++) {
					for (int k = 0; k < p.length; k++) {
						String key = str[j] + "." + p[k];
						String value = prop.getStr(header + key);
						if (value != null)
							newItem.setProperty(oName + "." + key, value);
					}
				}
			}
			newItem.restoreProperties();
			manager_.setSelectedItem(newItem, true);
		}
	}
	
	//
	private void setDefaultDirectory(){
		String dir = java.lang.System.getenv("PROJECT_DIR");
		if( dir != null ){
			setDefaultDirectory( dir );
		}
	}
}

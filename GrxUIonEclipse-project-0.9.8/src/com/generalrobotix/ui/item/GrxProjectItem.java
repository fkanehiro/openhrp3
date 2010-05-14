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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.grxui.PreferenceConstants;
import com.generalrobotix.ui.*;
import com.generalrobotix.ui.view.Grx3DView;
import com.generalrobotix.ui.util.GrxConfigBundle;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.OrderedHashMap;
import com.generalrobotix.ui.util.TwoInputDialog;

@SuppressWarnings({ "unchecked", "serial" }) //$NON-NLS-1$ //$NON-NLS-2$
public class GrxProjectItem extends GrxBaseItem {
	public static final String TITLE = "Project"; //$NON-NLS-1$
	public static final String DEFAULT_DIR = "/"; //$NON-NLS-1$
	public static final String FILE_EXTENSION = "xml"; //$NON-NLS-1$

	public static final int MENU_CREATE=0, MENU_RESTORE=1, MENU_LOAD=2, MENU_SAVE=3, MENU_SAVE_AS=4, MENU_IMPORT=5;
	private Vector<Action> menu_;
	
    private static final String MODE_TAG = "mode"; //$NON-NLS-1$
    private static final String WINCONF_TAG = "perspective"; //$NON-NLS-1$

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
            transformer_.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            transformer_.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
            setDefaultDirectory();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		}
	}

	public boolean create() {
		clear();
		setURL("");
		for (int i=0; ; i++) {
			File f = new File(getDefaultDir().getAbsolutePath()+"/"+"newproject"+i+".xml"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (!f.isFile()) {
				setName(f.getName().split("[.]")[0]); //$NON-NLS-1$
				break;
			}
		}
			
		doc_ = builder_.newDocument();
		element_ = doc_.createElement("grxui"); //$NON-NLS-1$
		element_.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
		doc_.appendChild(element_);
		_updateModeInfo();
		IWorkbenchPage page=null;
 		IWorkbench workbench = PlatformUI.getWorkbench();
        if( workbench != null){
        	IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
     		if (window != null){
     			page = window.getActivePage();
     			if (page != null &&
     			    page.getPerspective().getId().equals(GrxUIPerspectiveFactory.ID+ ".project") )
     	        	page.closePerspective(page.getPerspective(), false, false);
     		}
        }
		return true;
	}

	private void _updateModeInfo() {
		modeInfoMap_.clear();

		NodeList modeList = doc_.getElementsByTagName(MODE_TAG);
		for (int i=0; i<modeList.getLength(); i++) {
            ModeNodeInfo mi = new ModeNodeInfo();
			mi.root = (Element)modeList.item(i);
            modeInfoMap_.put(mi.root.getAttribute("name"), mi); //$NON-NLS-1$

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
			else
				mi.windowConfig = null;
				
		}
	}

    private ModeNodeInfo _getModeNodeInfo(String mode) {
        ModeNodeInfo mi = modeInfoMap_.get(mode);
        if (mi == null) {
            mi = new ModeNodeInfo();

		    NodeList nodeList = doc_.getElementsByTagName(MODE_TAG);
		    for (int i=0; i<nodeList.getLength(); i++) {
			    Element e = (Element)nodeList.item(i);
			    if (e.getAttribute("name").equals(mode)) { //$NON-NLS-1$
				    mi.root = e;
                    break;
                }
		    }
		    if (mi.root == null) {
			    mi.root = doc_.createElement(MODE_TAG);
			    mi.root.setAttribute("name", mode); //$NON-NLS-1$
			    element_.appendChild(doc_.createTextNode(INDENT4));
			    element_.appendChild(mi.root);
			    element_.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
            } 

			_updateModeInfo();
        }

        return mi;
    }
	
    public void setWindowConfigElement(String mode, Element element){
    	_getModeNodeInfo(mode).windowConfig = element;
    }
    
	public Element getWindowConfigElement(String mode) {
        return _getModeNodeInfo(mode).windowConfig; 
	}
   
 	private void storeMode(String mode) {
		Element modeEl = _getModeNodeInfo(mode).root;
		NodeList list = modeEl.getChildNodes();
		for (int i=list.getLength()-1; i>=0; i--)
			modeEl.removeChild(list.item(i));
		
		modeEl.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
		
		List<GrxBaseItem> itemList = manager_.getActiveItemList();
		for (int i=0; i<itemList.size(); i++) {
			GrxBaseItem item = itemList.get(i);
			modeEl.appendChild(doc_.createTextNode(INDENT4+INDENT4));
			item.setDocument(doc_);
			modeEl.appendChild(item.storeProperties());
			modeEl.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
		}
		
		List<GrxBaseView> viewList = manager_.getActiveViewList();
		for (int i=0; i<viewList.size(); i++) {
			GrxBaseView view = viewList.get(i);
			if (view.propertyNames().hasMoreElements()) {
				modeEl.appendChild(doc_.createTextNode(INDENT4+INDENT4));
				view.setDocument(doc_);
				modeEl.appendChild(view.storeProperties());
				modeEl.appendChild(doc_.createTextNode("\n")); //$NON-NLS-1$
			}
		}
		
		modeEl.appendChild(doc_.createTextNode(INDENT4));
	}
	
 	@SuppressWarnings("deprecation")
	private Element storePerspectiveConf(Element element){
 		IWorkbenchPage page=null;
 		IWorkbench workbench = PlatformUI.getWorkbench();
        if( workbench != null){
        	IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
     		if (window != null){
     			page = window.getActivePage();
     			if (!page.getPerspective().getId().contains(GrxUIPerspectiveFactory.ID) ){
     				return null;
     			}
     		}
        }
 		
        IPerspectiveRegistry perspectiveRegistry=workbench.getPerspectiveRegistry();
        IPerspectiveDescriptor orgPd=perspectiveRegistry.findPerspectiveWithId(GrxUIPerspectiveFactory.ID );
   		IPerspectiveDescriptor tempPd=perspectiveRegistry.findPerspectiveWithId(GrxUIPerspectiveFactory.ID + ".project");
   		if(tempPd!=null)
   			perspectiveRegistry.deletePerspective(tempPd);
   		tempPd = perspectiveRegistry.clonePerspective(GrxUIPerspectiveFactory.ID + ".project", getName(), orgPd);
		page.savePerspectiveAs(tempPd);
		page.setPerspective(orgPd);
		page.setPerspective(tempPd);
		IPreferenceStore store = workbench.getPreferenceStore();
		String confString=store.getString(GrxUIPerspectiveFactory.ID +".project"+"_persp");
		Document doc=null;
		try {
			doc = builder_.parse(new InputSource(new StringReader(confString)));
			Element confElement =doc.getDocumentElement();
			Node elementCopy = doc_.importNode(confElement, true);
			element.appendChild(elementCopy);
			return (Element) elementCopy;
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
 	}
 	
	public Vector<Action> getMenu() {

		if( menu_ == null ){
			menu_ = new Vector<Action>();

			// MENU_CREATE=0
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.CreateProject"); } //$NON-NLS-1$
				public void run(){
					boolean ans = MessageDialog.openConfirm( null, MessageBundle.get("GrxProjectItem.dialog.title.createProject"), MessageBundle.get("GrxProjectItem.dialog.message.createProject") ); //$NON-NLS-1$ //$NON-NLS-2$
					if ( ans )
						manager_.removeAllItems();
					else if ( ans == false )
						return;
					create();
				}
			} );
			// MENU_RESTORE=1
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.restoreProject"); } //$NON-NLS-1$
				public void run(){
					restoreProject();
				}
			} );
			// MENU_LOAD=2
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.loadProject"); } //$NON-NLS-1$
				public void run(){
                    if(!checkModifiedModel())
                        return;
                    load();
				}
			} );
			// MENU_SAVE=3
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.saveProject"); } //$NON-NLS-1$
				public void run(){
					save();
				}
			} );
			// MENU_SAVE_AS=4
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.saveProjectAs"); } //$NON-NLS-1$
				public void run(){
					saveAs();
				}
			} );
			// MENU_IMPORT=3
			/*
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.ImportISE"); } //$NON-NLS-1$
				public void run(){
					importISEProject();
				}
			} );
			*/
			menu_.add( new Action(){
				public String getText(){ return MessageBundle.get("GrxProjectItem.menu.addItem"); } //$NON-NLS-1$
				public void run(){
					addItem();
				}
			} );

		}
		
		return menu_;
	}

    private boolean checkModifiedModel()
    {
        List<GrxModelItem> modelList = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        for (int i=0; i<modelList.size(); i++) {
            GrxModelItem model = modelList.get(i);
            if(model.isModified()){
                String mes = MessageBundle.get("GrxProjectItem.dialog.message.changeModel.mes"); //$NON-NLS-1$
                mes = NLS.bind(mes, new String[]{model.getName()});
                
                MessageDialog msgDlg =
                    new MessageDialog(GrxUIPerspectiveFactory.getCurrentShell(),
                    		          MessageBundle.get("GrxProjectItem.dialog.message.changeModel.title"),
                                      null,
                                      mes,
                                      MessageDialog.INFORMATION, 
                                      new String[] {MessageBundle.get("GrxProjectItem.dialog.message.changeModel.btn.save"), 
                                              MessageBundle.get("GrxProjectItem.dialog.message.changeModel.btn.reverse"), 
                                              MessageBundle.get("GrxProjectItem.dialog.message.changeModel.btn.cancel")},
                                      2);
                
                switch(msgDlg.open())
                {
                case 0:
                    if(!model.saveAndLoad())
                        return false;
                    break;
                case 1:
                    if(!model.reload())
                        return false;
                    break;
                case 2:
                default:
                    return false;
                }
            }
        }
        return true;
    }

	public void save(File f) {
		if (f.exists()) {
			if (!f.isFile())
               return;
		}
		
		String mode = manager_.getCurrentModeName();
		storeMode(mode);

		setName(f.getName().split("[.]")[0]); //$NON-NLS-1$
      	setURL(f.getAbsolutePath());
      	
      	if (MessageDialog.openConfirm(null, MessageBundle.get("GrxProjectItem.dialog.savewindow.title"), 
      			MessageBundle.get("GrxProjectItem.dialog.savewindow.message"))) { //$NON-NLS-1$
			Element element = _getModeNodeInfo(mode).root;
			Element windowConfigElement = storePerspectiveConf(element);
			setWindowConfigElement(mode, windowConfigElement);
      	}
		
      	if (f == null)
			f = new File(getDefaultDir().getAbsolutePath()+"/"+getName()+".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (!f.getAbsolutePath().endsWith(".xml")) //$NON-NLS-1$
			f = new File(f.getAbsolutePath()+".xml"); //$NON-NLS-1$
		
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
			path = getDefaultDir().getAbsolutePath()+"/"+getName()+".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		
		File initialFile = new File(path);

		FileDialog fdlg = new FileDialog( GrxUIPerspectiveFactory.getCurrentShell(), SWT.SAVE);
		String[] exts = { "*.xml" }, extNames={"GrxUI Project"}; //$NON-NLS-1$ //$NON-NLS-2$
		fdlg.setFilterExtensions( exts );
		fdlg.setFilterNames( extNames );

		fdlg.setFilterPath( initialFile.getParent() );
		fdlg.setFileName( initialFile.getName() );
		
		String fPath = fdlg.open();
		if( fPath != null ) {
			File f = new File(fPath);
			if (f.exists() && f.isFile()){
				boolean ans = MessageDialog.openConfirm( null, MessageBundle.get("GrxProjectItem.dialog.title.saveProject"), //$NON-NLS-1$
						MessageBundle.get("GrxProjectItem.dialog.message.saveProject0")+f.getName()+MessageBundle.get("GrxProjectItem.dialog.message.saveProject1") + //$NON-NLS-1$ //$NON-NLS-2$
				MessageBundle.get("GrxProjectItem.dialog.message.saveProject2") ); //$NON-NLS-1$
				if (ans == false)
					return;
			}
			save( f );
			setDefaultDirectory(f.getParent());
		}	
	}
	
	public void save(){
		if (getURL(true) == null){
			saveAs();
		}else{
			File f = new File(getURL(true));
			if (f.exists()){
				save(f);
			}else{
				saveAs();
			}
		}
	}

	/**
	 * @brief load a project
	 */
	public void load() {
		FileDialog fdlg = new FileDialog( GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);
		String[] fe = { "*.xml" }; //$NON-NLS-1$
		fdlg.setFilterExtensions( fe );
        fdlg.setFilterPath(getDefaultDir().getAbsolutePath());
		
		String fPath = fdlg.open();
		if( fPath != null ) {
			File f = new File(fPath);
			load(f);
			setDefaultDirectory(f.getParent());
			restoreProject();
		}
	}

	/**
	 * @brief load a project file
	 * @param f a project file
	 * @return true if loaded successfully, false otherwise
	 */
	public boolean load(File f) {
		System.out.println( "[ProjectItem]@load ProjectFile load "+ f.toString()); //$NON-NLS-1$
		
		if (f == null || !f.isFile())
			return false;
		GrxSimulationItem simItem = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
		if(simItem!=null && simItem.isSimulating())
			simItem.stopSimulation();
		
		manager_.removeAllItems();
		manager_.focusedItem(manager_.getProject());
		setName(f.getName().split("[.]")[0]); //$NON-NLS-1$
		
		String dir = f.getParent();
		if(dir != null)
			System.setProperty("CURRENT_DIR", dir);
		
    	try {
      		doc_ = builder_.parse(f);
      		element_ = doc_.getDocumentElement();
      		_updateModeInfo();
      		file_ = f;
      		setURL(f.getAbsolutePath());
    	} catch (Exception e) {
      		GrxDebugUtil.printErr("project load:",e); //$NON-NLS-1$
      		file_ = null;
      		return false;
    	}
		
		// register mode
    	OrderedHashMap modes=(OrderedHashMap)manager_.getItemMap(GrxModeInfoItem.class);
		Iterator<GrxModeInfoItem> it = modes.values().iterator();
		NodeList list = doc_.getElementsByTagName(MODE_TAG);
		for (int i=0; i<list.getLength(); i++) {
			Element modeEl = (Element) list.item(i);
			String modeName = modeEl.getAttribute("name"); //$NON-NLS-1$
			if (modeName == null) 
				continue;
			boolean found=false;
		    while (it.hasNext()) {
		    	GrxModeInfoItem mode = it.next();
		    	if(modeName.equals(mode.getName())){
		    		mode.setElement(modeEl);
		    		found=true;
		    		break;
		    	}
		    }
		    if(!found){
		    	GrxModeInfoItem item = (GrxModeInfoItem)manager_.createItem(GrxModeInfoItem.class, modeName);
		    	if (item != null)  {
		    		item.setElement(modeEl);
		    		if (GrxXmlUtil.getBoolean(modeEl, "select", true)) { //$NON-NLS-1$
		    			manager_.setSelectedItem(item, true);
		    		}
		    	}
		    	manager_.setCurrentMode(item);
			}
		}
		return true;
	}
	
	public void restoreProject() {

		String mode = manager_.getCurrentModeName();
		System.out.println("Restore Project (Mode:" +mode+")"); //$NON-NLS-1$ //$NON-NLS-2$

		IRunnableWithProgress runnableProgress = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InterruptedException {
				String mode = manager_.getCurrentModeName();
				monitor.beginTask("Restore Project (Mode:" +mode+")", 10 ); //$NON-NLS-1$ //$NON-NLS-2$
				restoreProject_work(mode, monitor);
				monitor.done();
			}
		};
		ProgressMonitorDialog progressMonitorDlg = new ProgressMonitorDialog(null);
		try {
			progressMonitorDlg.run(false,false, runnableProgress);
			//ダイアログの影が残ってしまぁE�E��E�題�E対筁E//
			Grx3DView view3d =  (Grx3DView)manager_.getView( Grx3DView.class );
			if(view3d!=null){
				view3d.repaint();
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void restoreProject_work(String mode, IProgressMonitor monitor) {
		//manager_.restoreProcess();

		monitor.worked(1);

		if (file_ == null || !file_.isFile())
		{
			return;
		}
		ModeNodeInfo minfo =  modeInfoMap_.get(mode);

        monitor.worked(1);
        
		List propList = minfo.propList;
		if (propList != null) {
			for (int i=0; i<propList.size(); i++) {
				Element propEl = (Element)propList.get(i);
				String key = propEl.getAttribute("name"); //$NON-NLS-1$
				String val = propEl.getAttribute("value"); //$NON-NLS-1$
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
			if (vl.get(i) != null) vl.get(i).restoreProperties();

		monitor.worked(1);

		if (minfo.itemList != null) {	
			
		//  古いプロジェクトファイルに対応　　//
			//  ModeがSimulationでSimulationItemがない場合は自動的に作成し、WorldStateItemのプロパティをSimulationItemに設定　　//
			if(mode.equals("Simulation") && !containSimulationItem(minfo.itemList)){
				Element simElement = doc_.createElement(ITEM_TAG);
				simElement.setAttribute("class", PreferenceConstants.SIMULATIONITEM);
				simElement.setAttribute("name", "simulationItem");
				simElement.setAttribute("select", "true");
				minfo.root.appendChild(simElement);
				minfo.itemList = minfo.root.getElementsByTagName(ITEM_TAG);
				for (int i=0; i<minfo.itemList.getLength(); i++) {
				    Element itemElement = (Element)minfo.itemList.item(i);
				    if(itemElement.getAttribute("class").equals(PreferenceConstants.WORLDSTATEITEM)){
				    	NodeList props = itemElement.getElementsByTagName(PROPERTY_TAG);
						for (int j = 0; j < props.getLength(); ) {
							Element propEl = (Element) props.item(j);
							String key = propEl.getAttribute("name"); //$NON-NLS-1$
							String val = propEl.getAttribute("value"); //$NON-NLS-1$
							if(!key.equals("logTimeStep")){
								Element element = doc_.createElement(PROPERTY_TAG);
								element.setAttribute("name", key);
								element.setAttribute("value", val);
								simElement.appendChild(element);
								itemElement.removeChild(propEl);
							}else
								j++;
						}
				    }
				}
			}
			
            List<GrxBaseItem> il = new ArrayList<GrxBaseItem>();
			for (int i = 0; i < minfo.itemList.getLength(); i++) {
				GrxBaseItem p = (GrxBaseItem)_restorePlugin((Element) minfo.itemList.item(i));
                if (p != null)
                     il.add(p);
            }
            
            // for a item that is exclusive selection reselect 
            for (int i=0; i<il.size(); i++) {
			    GrxBaseItem item = il.get(i);
                boolean select = GrxXmlUtil.getBoolean(item.getElement(), "select", false); //$NON-NLS-1$
			    //manager_.setSelectedItem(item, select);
            }
		}

		monitor.worked(1);
		
		if (minfo.windowConfig != null) {
			Document doc = builder_.newDocument();
			Node nodeCopy = doc.importNode(minfo.windowConfig, true);
			doc.appendChild(nodeCopy);
			StringWriter sw = new StringWriter();
			try {
				DOMSource src = new DOMSource();
				src.setNode(doc);
				StreamResult target = new StreamResult(sw);
				transformer_.transform(src, target);
			} catch (TransformerConfigurationException e) {
				e.printStackTrace();
			} catch (TransformerException e) {
	     		e.printStackTrace();
			}
	   	
			IWorkbenchPage page=null;
	 		IWorkbench workbench = PlatformUI.getWorkbench();
	        if( workbench != null){
	        	IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	     		if (window != null){
	     			page = window.getActivePage();
	     			if (!page.getPerspective().getId().contains(GrxUIPerspectiveFactory.ID) ){
	     				return;
	     			}
	     		}
	        }
	        if (page.getPerspective().getId().equals(GrxUIPerspectiveFactory.ID+ ".project") )
	        	page.closePerspective(page.getPerspective(), false, false);
	        IPreferenceStore store = workbench.getPreferenceStore();
   			IPerspectiveRegistry perspectiveRegistry=workbench.getPerspectiveRegistry();
   			IPerspectiveDescriptor orgPd=perspectiveRegistry.findPerspectiveWithId(GrxUIPerspectiveFactory.ID );
	   		IPerspectiveDescriptor tempPd=perspectiveRegistry.findPerspectiveWithId(GrxUIPerspectiveFactory.ID + ".project");
	   		if(tempPd!=null)
	   			perspectiveRegistry.deletePerspective(tempPd);
	   		tempPd = perspectiveRegistry.clonePerspective(GrxUIPerspectiveFactory.ID + ".project", getName(), orgPd);
	   		store.setValue(GrxUIPerspectiveFactory.ID + ".project"+"_persp", sw.toString());
	   		page.setPerspective(tempPd);
		}else{
			IWorkbenchPage page=null;
	 		IWorkbench workbench = PlatformUI.getWorkbench();
	        if( workbench != null){
	        	IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
	     		if (window != null){
	     			page = window.getActivePage();
	     			if (!page.getPerspective().getId().contains(GrxUIPerspectiveFactory.ID) ){
	     				return;
	     			}
	     		}
	        }
	        if (page.getPerspective().getId().equals(GrxUIPerspectiveFactory.ID+ ".project") )
	        	page.closePerspective(page.getPerspective(), false, false);
		}
	}
	
	private GrxBasePlugin _restorePlugin(Element e) {
		String iname = e.getAttribute("name"); //$NON-NLS-1$
		if (iname == null || iname.length() == 0)
			return null;
		manager_.pluginLoader_.addURL(GrxXmlUtil.expandEnvVal(e.getAttribute("lib"))); //$NON-NLS-1$
		Class cls = manager_.registerPlugin(e.getAttribute("class")); //$NON-NLS-1$
		if (cls == null)
			return null;
		manager_.getMode().addItemClassList(cls);
		
		GrxBasePlugin plugin = null;
		if (GrxBaseItem.class.isAssignableFrom(cls)) {
			Class<? extends GrxBaseItem> icls = (Class<? extends GrxBaseItem>) cls;
			String url = e.getAttribute("url"); //$NON-NLS-1$
			if (!url.equals("")) //$NON-NLS-1$
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
			if (GrxBaseItem.class.isAssignableFrom(cls)) {
				manager_.itemChange((GrxBaseItem)plugin, GrxPluginManager.ADD_ITEM);
		        manager_.setSelectedItem((GrxBaseItem)plugin, true);
			}
        }

        return plugin;
	}
		
	private boolean containSimulationItem(NodeList list){
		for (int i = 0; i < list.getLength(); i++) {
			if(((Element) list.item(i)).getAttribute("class").equals(PreferenceConstants.SIMULATIONITEM))
				return true;
		}
		return false;
	}
	
	private static final File DEFAULT_ISE_PROJECT_DIR = new File("../ISE/Projects"); //$NON-NLS-1$
	public void importISEProject() {

		FileDialog fDialog = new FileDialog(null,SWT.SAVE);

		String [] exts = {"*.prj"}; //$NON-NLS-1$
		String [] filterNames = {"ISE Project File(*.prj)"}; //$NON-NLS-1$
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
					setName(f.getName().split(".prj")[0]); //$NON-NLS-1$
					importISEProject(f);
					//manager_.processingWindow_.setVisible(false);
					//fc.setSelectedFile(null);
					storeMode(manager_.getCurrentModeName());
				//}
			//};
			//t.start();
		}
	}
	
	private static String ENVIRONMENT_NODE = "jp.go.aist.hrp.simulator.EnvironmentNode"; //$NON-NLS-1$
	private static String ROBOT_NODE = "jp.go.aist.hrp.simulator.RobotNode"; //$NON-NLS-1$
	private static String COLLISIONPAIR_NODE = "jp.go.aist.hrp.simulator.CollisionPairNode"; //$NON-NLS-1$
	private static String GRAPH_NODE = "jp.go.aist.hrp.simulator.GraphNode"; //$NON-NLS-1$
			
	private static String WORLD_STATE_ITEM = "com.generalrobotix.ui.item.GrxWorldStateItem"; //$NON-NLS-1$
	private static String MODEL_ITEM = "com.generalrobotix.ui.item.GrxModelItem"; //$NON-NLS-1$
	private static String COLLISIONPAIR_ITEM = "com.generalrobotix.ui.item.GrxCollisionPairItem"; //$NON-NLS-1$
	private static String GRAPH_ITEM = "com.generalrobotix.ui.item.GrxGraphItem"; //$NON-NLS-1$
	
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

		setProperty("nsHost", GrxCorbaUtil.nsHost()); //$NON-NLS-1$
		Integer nsPort = new Integer(GrxCorbaUtil.nsPort());
		setProperty("nsPort", nsPort.toString()); //$NON-NLS-1$

		String pname = prop.getStr("Project.name", ""); //$NON-NLS-1$ //$NON-NLS-2$
		Class cls = manager_.registerPlugin(WORLD_STATE_ITEM);
		GrxBaseItem newItem = manager_.createItem((Class <? extends GrxBaseItem>)cls, pname);
		newItem.setDbl("totalTime",   prop.getDbl("Project.totalTime", 20.0)); //$NON-NLS-1$ //$NON-NLS-2$
		newItem.setDbl("timeStep",    prop.getDbl("Project.timeStep", 0.001)); //$NON-NLS-1$ //$NON-NLS-2$
		newItem.setDbl("logTimeStep", prop.getDbl("Project.timeStep", 0.001)); //$NON-NLS-1$ //$NON-NLS-2$
		newItem.setProperty("method", prop.getStr("Project.method")); //$NON-NLS-1$ //$NON-NLS-2$
		manager_.itemChange(newItem, GrxPluginManager.ADD_ITEM);
        manager_.setSelectedItem(newItem, true);

		for (int i = 0; i < prop.getInt("Project.num_object", 0); i++) { //$NON-NLS-1$
			String header = "Object" + i + "."; //$NON-NLS-1$ //$NON-NLS-2$
			String oName = prop.getStr(header + "name"); //$NON-NLS-1$
			String cName = prop.getStr(header + "class"); //$NON-NLS-1$
			if (oName == null || cName == null)
				continue;
			
			if (cName.equals(ENVIRONMENT_NODE) || cName.equals(ROBOT_NODE)) {
				cls = manager_.registerPlugin(MODEL_ITEM);
				try {
					URL url = new URL(prop.getStr(header + "url")); //$NON-NLS-1$
					newItem = manager_.loadItem((Class<? extends GrxBaseItem>)cls, oName, url.getPath());
					if(newItem!=null){
						manager_.itemChange(newItem, GrxPluginManager.ADD_ITEM);
						manager_.setSelectedItem(newItem, true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (newItem == null)
					continue;

	  			if (cName.equals(ENVIRONMENT_NODE)) {
					newItem.setProperty("isRobot", "false"); //$NON-NLS-1$ //$NON-NLS-2$
	  			} else if (cName.equals(ROBOT_NODE)) {
					newItem.setProperty("isRobot", "true"); //$NON-NLS-1$ //$NON-NLS-2$
	  				Enumeration e = prop.keys();
	  				while (e.hasMoreElements()) {
	  					String key = (String)e.nextElement();
	  					if (key.startsWith(header)) {
	  						String newKey = key.substring(header.length());
	  						if (key.endsWith(".angle")) { //$NON-NLS-1$
	  							newItem.setDbl(newKey, prop.getDbl(key, 0.0));
	  						} else if (key.endsWith(".mode")) { //$NON-NLS-1$
								newItem.setProperty(newKey, prop.getStr(key, "Torque")); //$NON-NLS-1$
	  						} else if (key.endsWith(".translation"))  { //$NON-NLS-1$
								newItem.setDblAry(newKey, 
									prop.getDblAry(key, new double[]{0.0, 0.0, 0.0}));
	  						} else if (key.endsWith(".rotation"))  { //$NON-NLS-1$
								newItem.setDblAry(newKey, 
									prop.getDblAry(key, new double[]{0.0, 1.0, 0.0, 0.0}));
	  						}
	  					}
	  				}
	  				
					String controller = prop.getStr(header + "controller"); //$NON-NLS-1$
					controller = controller.replaceFirst("openhrp.", ""); //$NON-NLS-1$ //$NON-NLS-2$
					double controlTime = prop.getDbl(header + "controlTime", 0.001); //$NON-NLS-1$
					newItem.setProperty("controller", controller); //$NON-NLS-1$
					newItem.setProperty("controlTime", String.valueOf(controlTime)); //$NON-NLS-1$

					String imageProcessor = prop.getStr(header + "imageProcessor"); //$NON-NLS-1$
					if (imageProcessor != null) {
						double imageProcessTime = prop.getDbl(header + "imageProcessTime", 0.001); //$NON-NLS-1$
						newItem.setProperty("imageProcessor", imageProcessor); //$NON-NLS-1$
						newItem.setProperty("imageProcessTime", String.valueOf(imageProcessTime)); //$NON-NLS-1$
					}
				}
	  			
			} else if (cName.equals(COLLISIONPAIR_NODE)) {
				cls = manager_.registerPlugin(COLLISIONPAIR_ITEM); 
				newItem = manager_.createItem((Class<? extends GrxBaseItem>)cls, oName);
				newItem.setProperty("objectName1", prop.getStr(header + "objectName1")); //$NON-NLS-1$ //$NON-NLS-2$
				newItem.setProperty("jointName1",  prop.getStr(header + "jointName1")); //$NON-NLS-1$ //$NON-NLS-2$
				newItem.setProperty("objectName2", prop.getStr(header + "objectName2")); //$NON-NLS-1$ //$NON-NLS-2$
				newItem.setProperty("jointName2",  prop.getStr(header + "jointName2")); //$NON-NLS-1$ //$NON-NLS-2$
				newItem.setProperty("slidingFriction", prop.getStr(header + "slidingFriction", "0.5")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				newItem.setProperty("staticFriction",  prop.getStr(header + "staticFriction", "0.5")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				newItem.setProperty("cullingThresh",  prop.getStr(header + "cullingThresh", "0.01")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				newItem.setProperty("sprintDamperModel", prop.getStr(header + "springDamplerModel", "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				newItem.setProperty("springConstant", prop.getStr(header + "springConstant", "0.0 0.0 0.0 0.0 0.0 0.0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				newItem.setProperty("damperConstant", prop.getStr(header + "damperConstant", "0.0 0.0 0.0 0.0 0.0 0.0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else if (cName.equals(GRAPH_NODE)) {
				cls = manager_.registerPlugin(GRAPH_ITEM);
				newItem = manager_.getItem((Class<? extends GrxBaseItem>)cls, null);
				if (newItem == null)
					newItem = manager_.createItem((Class<? extends GrxBaseItem>)cls, "GraphList1"); //$NON-NLS-1$
				String items = prop.getStr(header + "dataItems"); //$NON-NLS-1$
				newItem.setProperty(oName + ".dataItems", items); //$NON-NLS-1$
				String[] str = items.split(","); //$NON-NLS-1$
				String[] p = { 
					"object", "node", "attr", "index",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					"numSibling", "legend", "color"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				};
				for (int j = 0; j < str.length; j++) {
					for (int k = 0; k < p.length; k++) {
						String key = str[j] + "." + p[k]; //$NON-NLS-1$
						String value = prop.getStr(header + key);
						if (value != null)
							newItem.setProperty(oName + "." + key, value); //$NON-NLS-1$
					}
				}
			}
			newItem.restoreProperties();
			manager_.setSelectedItem(newItem, true);
		}
	}
	
	//
	private void setDefaultDirectory(){
		String dir = Activator.getDefault().getPreferenceStore().getString("PROJECT_DIR"); //$NON-NLS-1$
        if(dir.equals("")) //$NON-NLS-1$
        	dir = System.getenv("PROJECT_DIR"); //$NON-NLS-1$
		if( dir != null ){
			setDefaultDirectory( dir );
		}
	}
	
	private void addItem(){
		TwoInputDialog dialog = new TwoInputDialog( null, MessageBundle.get("GrxProjectItem.dialog.addItem.title"), MessageBundle.get("GrxProjectItem.dialog.addItem.message0"),
				"", null, MessageBundle.get("GrxProjectItem.dialog.addItem.message1"),""  );
		if(dialog.open()==InputDialog.OK){
			String[] values = dialog.getValues();
			manager_.addPlugin(values[0], values[1]);
		}
	}
}

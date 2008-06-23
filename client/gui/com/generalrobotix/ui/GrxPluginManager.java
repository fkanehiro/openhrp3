/*
 *  GrxPluginManager.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.JTree.DynamicUtilTreeNode;

import org.omg.PortableServer.POA;
import org.w3c.dom.Element;

import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.item.GrxModeInfoItem;
import com.generalrobotix.ui.item.GrxProjectItem;
import com.generalrobotix.ui.view.GrxProcessManagerView;

@SuppressWarnings("unchecked")
public class GrxPluginManager 
{
	// project
	private GrxProjectItem  rcProject_;
	private GrxProjectItem  currentProject_;
	private GrxModeInfoItem currentMode_;
	
	// for managing items
	public  GrxPluginLoader pluginLoader_;
	private HashMap<Class<? extends GrxBasePlugin>, OrderedHashMap> pluginMap_ = new HashMap<Class<? extends GrxBasePlugin>, OrderedHashMap>();
	private List<GrxBaseItem> selectedItemList_ = new ArrayList<GrxBaseItem>();
	private List<GrxBaseView> selectedViewList_ = new ArrayList<GrxBaseView>();
	private boolean isItemSelectionChanged_ = false;
	private boolean isItemModelChanged_ = false;
	private DefaultMutableTreeNode root_ = new DefaultMutableTreeNode(currentProject_);
	public  DefaultTreeModel treeModel_ = new DefaultTreeModel(root_);
    private String homePath_;
	private Map<Class<? extends GrxBasePlugin>, PluginInfo> pinfoMap_ = new HashMap<Class<? extends GrxBasePlugin>, PluginInfo>();
	
	// for cyclic execution
	private javax.swing.Timer timer_;
	private int delay_ = DEFAULT_INTERVAL;// [msec]
	private long prevTime_ = 0; // [msec]
	public  double min, max, now; // [msec]
	private static final int  DEFAULT_INTERVAL = 100; // [msec]
	
	// UI objects
	private GrxUIFrame frame_;
	private JFileChooser fileChooser_ = null;
	public ImageIcon ROBOT_ICON = new ImageIcon(GrxPluginManager.class.getResource("/resources/images/grxrobot.png"));
	public GrxProcessingWindow processingWindow_;
	
	// for CORBA
	public POA poa_;
	public org.omg.CORBA.ORB orb_;
	
	GrxPluginManager() 
	{
		String robotDir = System.getenv("ROBOT_DIR");
		String robot = System.getenv("ROBOT");
		if (robotDir == null && robot != null)
			robotDir = "../../Controller/IOserver/robot/"+robot;
		
		if (robotDir != null && new File(robotDir).isDirectory())
			homePath_ = robotDir+File.separator;
		else
			homePath_ = System.getProperty("user.dir","")+File.separator;

		pluginLoader_ = new GrxPluginLoader("plugin", ClassLoader.getSystemClassLoader());
		registerPlugin(GrxModeInfoItem.class);
		
		// load default plugin settings
	    rcProject_ = new GrxProjectItem("grxuirc", this);
		if (!rcProject_.load(new File(homePath_ + "grxuirc.xml"))) {
			JOptionPane.showMessageDialog(frame_, 
				"Can't find grxuirc.xml.", "Can't Start GrxUI", 
				JOptionPane.ERROR_MESSAGE,  ROBOT_ICON);
			System.exit(0);
		}
	    currentProject_ = new GrxProjectItem("newproject", this);

		// load default project
		String defaultProject = homePath_+System.getProperty("PROJECT", null);
		if (defaultProject == null || !currentProject_.load(new File(GrxXmlUtil.expandEnvVal(defaultProject))))
			currentProject_.create();
		root_.setUserObject(currentProject_);
			
		// setup timer
		timer_ = new javax.swing.Timer(delay_, new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_updateItemSelection();

		        if (isItemModelChanged_)  {
		          treeModel_.reload();
		          isItemModelChanged_ = false;
                }

				_control();
			}
		});
	}
	
	private void _updateItemSelection() {
		if (!isItemSelectionChanged_)
			return;
			
		isItemSelectionChanged_ = false;
		selectedItemList_.clear();
		Enumeration e = root_.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
			Object o = n.getUserObject();
			if (o instanceof GrxBaseItem && ((GrxBaseItem) o).isSelected())
				selectedItemList_.add((GrxBaseItem) o);
		}
		
		for (int i=0; i<selectedViewList_.size(); i++) {
			GrxBaseView view = selectedViewList_.get(i);
			try {
				view.itemSelectionChanged(selectedItemList_);
			} catch (Exception e1) {
				GrxDebugUtil.printErr("Control thread (itemSelectionChanged):"
						+ view.getName() + " got exception.", e1);
			}
		}	
	}

	private void _control() {
		long t = System.currentTimeMillis();
		now = t - prevTime_;
		prevTime_ = t;
		max = Math.max(max, now);
		min = Math.min(min, now);

		for (int i = 0; i < selectedViewList_.size(); i++) {
			GrxBaseView view = selectedViewList_.get(i);
			try {
				switch (view.view_state_) {
				case GrxBaseView.GRX_VIEW_SETUP:
					if (view.setup(selectedItemList_))
						view.view_state_ = GrxBaseView.GRX_VIEW_ACTIVE;
					break;
					
				case GrxBaseView.GRX_VIEW_ACTIVE:
					long prev = System.currentTimeMillis();
					view.control(selectedItemList_);
					t = System.currentTimeMillis();
					view.now = t - prev;
					if (view.max < view.now)
						view.max = view.now;
					if (view.min > view.now)
						view.min = view.now;
					break;
					
				case GrxBaseView.GRX_VIEW_CLEANUP:
					if (view.cleanup(selectedItemList_))
						view.view_state_ = GrxBaseView.GRX_VIEW_SLEEP;
					break;
					
				case GrxBaseView.GRX_VIEW_SLEEP:
					break;
				}
			} catch (final Exception e) {
				GrxDebugUtil.printErr("Control thread :"
						+ view.getName() + " got exception.", e);
			}
		}
	}

	void start() {
		// Start CORBA thread
		new Thread() {
			public void run() {
				try {
					poa_ = GrxCorbaUtil.getRootPOA();
					poa_.the_POAManager().activate();
					GrxDebugUtil.println("Corba Server Ready.");
					orb_ = GrxCorbaUtil.getORB();
					orb_.run();
					orb_.destroy();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		frame_.setVisible(true);

		String defaultMode = System.getProperty("MODE", "");
		GrxModeInfoItem mode = (GrxModeInfoItem)getItem(GrxModeInfoItem.class, defaultMode);
		Map m = pluginMap_.get(GrxModeInfoItem.class);
		GrxModeInfoItem[] modes = (GrxModeInfoItem[])m.values().toArray(new GrxModeInfoItem[0]);
		
		try {
			if (mode == null) {
				int ans = 0;
 				if (modes.length > 1) {
				    GrxBaseItem initialMode = null;
				    for (int i=0; i<modes.length; i++) {
					    if (modes[i].isSelected())
						    initialMode = modes[i];
				    }
					ans = JOptionPane.showOptionDialog(frame_,
						"Select Initial Mode.", "Select Mode",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.INFORMATION_MESSAGE, ROBOT_ICON, 
						modes, initialMode);
				    if (ans < 0)
					    System.exit(0);
				}
				mode = modes[ans];
			} 
			setMode(mode);
			frame_.updateModeButtons(modes, currentMode_);

		} catch (Exception e) {
			GrxDebugUtil.printErr("GrxPluginManager:",e);
		}
	}
	
	void setMode(GrxModeInfoItem mode) {
		if (currentMode_ == mode)
			return;
		
		// prepare change mode
		for (int i=0; i<selectedViewList_.size(); i++) {
			GrxBaseView view = selectedViewList_.get(i);
			view.stop();
			while (!view.isSleeping()) {
				try {
					Thread.sleep(getDelay());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		timer_.stop();
		clearItemSelection();
		
		// update active plugin and create view
		setSelectedItem(mode, true);
		currentMode_ = mode;
		currentMode_.restoreProperties();
		
		// update acrive viewPlugin list
		selectedViewList_.clear();
		for (int i = 0; i < currentMode_.activeViewClassList_.size(); i++) {
			Class cls = currentMode_.activeViewClassList_.get(i);
			GrxBaseView view = getView((Class<? extends GrxBaseView>) cls);
			if (view != null)
				selectedViewList_.add(view);
		}
		
		// update window config
		frame_.updateTab(selectedViewList_);
		frame_.setConfigElement(rcProject_.getWindowConfigElement(currentMode_.getName()));
		Element el = currentProject_.getWindowConfigElement(currentMode_.getName());
		frame_.restoreConfig(el);
		
		currentProject_.restoreProject();

		now = max = min = 0.0;
		for (int i=0; i<selectedViewList_.size(); i++)
			selectedViewList_.get(i).start();
		timer_.start();
	}

	void setFrame(GrxUIFrame frame) {
		frame_ = frame;
		processingWindow_ = new GrxProcessingWindow(frame_, true);
		frame_.setConfigFileName(rcProject_.getURL(true));
	}


	public GrxUIFrame getFrame() {
		return frame_;
	}

	public void setDelay(int msec) {
		delay_ = msec;
		timer_.setDelay(msec);
	}

	public int getDelay() {
		return delay_;
	}	
	
	public Class registerPlugin(String className) {
		Class cls = pluginLoader_.loadClass(className);
		//@SuppressWarnings("unchecked")
		return registerPlugin((Class<? extends GrxBasePlugin>) cls);
	}

	public Class registerPlugin(Class<? extends GrxBasePlugin> cls) {
		if (cls != null && GrxBasePlugin.class.isAssignableFrom(cls)) {
		    if (pluginMap_.get(cls) == null) {
			    pluginMap_.put(cls, new OrderedHashMap());

		    	PluginInfo pi = new PluginInfo();
                pi.title = (String)GrxBasePlugin.getField(cls, "TITLE", cls.getSimpleName());
				pi.lastDir = new File(homePath_+(String)GrxBasePlugin.getField(cls, "DEFAULT_DIR", ""));
				String ext = (String)GrxBasePlugin.getField(cls, "FILE_EXTENSION", null);
				if (ext != null)
					pi.filter = GrxGuiUtil.createFileFilter(ext);
			    pinfoMap_.put(cls, pi);
		    }
		        
		    return cls;
		}
	    return null;
	}

	/*
	public void introducePlugin(Class cls) {
		if (pluginMap_.get(cls) == null)
			pluginMap_.put((Class<? extends GrxBasePlugin>) cls,
					new HashMap<String, GrxBasePlugin>());
	}
	public Class introducePlugin() {
		String className = (String) JOptionPane.showInputDialog(
				frame_, "Input class name to introduce current mode.", "Introduce Plugin", 
				JOptionPane.QUESTION_MESSAGE, 
				ROBOT_ICON, 
				null, "test1");
		Class cls = registerPlugin(className);
		if (cls == null)
			return null;
		if (GrxBaseItem.class.isAssignableFrom(cls)) {
			if (!currentMode_.activeItemClassList_.contains(cls)) {
				currentMode_.activeItemClassList_.add((Class<? extends GrxBaseItem>) cls);
				setVisibleItem(currentMode_.activeItemClassList_);
			}
		} else if (GrxBaseView.class.isAssignableFrom(cls)) {
			if (!currentMode_.activeViewClassList_.contains(cls)) {
				currentMode_.activeViewClassList_.add((Class<? extends GrxBaseView>) cls);
			}
		}
		return cls;
	}
	*/

	public GrxBaseItem createItem(String clsName, String name) {
		Class cls = pluginLoader_.loadClass(clsName);
		if (cls != null)
			return createItem((Class<? extends GrxBaseItem>)cls, name);
		return null;
	}

	public GrxBaseItem createItem(Class<? extends GrxBaseItem> cls, String name) {
		if (name == null) {
			String baseName = "new" + getItemTitle(cls);
			baseName = baseName.toLowerCase().replaceAll(" ", "");
			for (int i = 0;; i++) {
				name = baseName + i;
				if (getItem(cls, name) == null)
					break;
			}
		}
		GrxBaseItem item = (GrxBaseItem) createPlugin(cls, name);
		if (item != null) {
			item.create();
			setSelectedItem(item, true);
			_addItemNode(item);
		}
		return item;
	}

	public GrxBaseItem loadItem(Class<? extends GrxBaseItem> cls, String name, String url) {
		String _url = GrxXmlUtil.expandEnvVal(url);
		if (_url == null)
			return null;
         
		URL u = null;
		File f = null;
		try {
			u = new URL(_url);
			f = new File(u.getFile());
		} catch (Exception e) {
			GrxDebugUtil.printErr("loadItem : in not URL format\n", e);
	        f = new File(_url);
		}
		
		if (!f.isFile())
			return null;

		if (name == null)
			name = f.getName().split("[.]")[0];

		GrxBaseItem item = (GrxBaseItem) createPlugin(cls, name);
		if (item != null) {
			if (item.load(f)) {
				item.setURL(url);
				setSelectedItem(item, true);
				_addItemNode(item);
			} else {
				removeItem(item);
			}
		}

		return item;
	}

	public GrxBaseItem loadItem2(Class<? extends GrxBaseItem> cls, String dir, String[] ext) {
		FileNameLocationDialog dialog = new FileNameLocationDialog(
			frame_, 
			MessageBundle.get("dialog.object.addRobot.title"),
			MessageBundle.get("dialog.object.addRobot.title"),
			ext,dir
		);
		//dialog.setText(prevUrl_);

		if (dialog.showModalDialog() == ModalDialog.OK_BUTTON) {
			String objectName = dialog.getName();
			String location = dialog.getFileName();

			GrxBaseItem item = loadItem(cls, objectName, location);
			if (item != null) 
				item.setProperty("url", location);
			//prevUrl_ = location;
			return item;
		}
		return null;
	}

	public GrxBaseView createView(Class<? extends GrxBaseView> cls, String name) {
        if (name == null)
            name = pinfoMap_.get(cls).title;
		return (GrxBaseView) createPlugin(cls, name);
	}

	private GrxBasePlugin createPlugin(Class<? extends GrxBasePlugin> cls, String name) {
		if (registerPlugin(cls) == null)
			return null;
		try {
			HashMap<String, GrxBasePlugin> map = pluginMap_.get(cls);
			GrxBasePlugin plugin = map.get(name);
			if (plugin != null)
				return null;

			plugin = pluginLoader_.createPlugin(cls, name, this);
			map.put(name, plugin);
			return plugin;
		} catch (Exception e) {
			showExceptionTrace("Couldn't load Class:" + cls.getName(), e);
		}
		return null;
	}

	private void showExceptionTrace(String m, Exception e) {
		GrxDebugUtil.printErr(m, e);

		Throwable cause = e.getCause();
		StackTraceElement[] trace = null;
		String msg = m + "\n\n";
		if (cause != null) {
			msg = cause.toString() + "\n\n";
			trace = cause.getStackTrace();
		} else {
			trace = e.getStackTrace();
		}

		for (int i = 0; i < trace.length; i++) {
			msg += "at " + trace[i].getClassName() + "." + trace[i].getMethodName() + "(";

			if (trace[i].isNativeMethod())
				msg += "(Native Method)\n";
			else if (trace[i].getFileName() == null) 
				msg += "(No source code)\n";
			else
				msg += trace[i].getFileName() + ":" + trace[i].getLineNumber() + ")\n";
		}

		JOptionPane.showMessageDialog(getFrame(), 
				msg, "Exception Occered", JOptionPane.WARNING_MESSAGE, ROBOT_ICON);
	}

	private void _addItemNode(GrxBaseItem item) {
		if (item instanceof GrxModeInfoItem) 
			return;

		DefaultMutableTreeNode node = null;
		for (int i = 0; i < root_.getChildCount(); i++) {
			node = (DefaultMutableTreeNode) root_.getChildAt(i);
			if (node.getUserObject().equals(item.getClass()))
				break;
			node = null;
		}

		if (node == null) {
			node = new DynamicUtilTreeNode(item.getClass(), new Hashtable());
			root_.add(node);
		}

		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(item);
		node.add(newNode);
		
		isItemModelChanged_ = true;
	}

	public void removeItem(GrxBaseItem item) {
		Enumeration e = root_.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
			if (n.getUserObject() == item) {
				try {
					treeModel_.removeNodeFromParent(n);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				break;
			}
		}
		Map m = pluginMap_.get(item.getClass());
		if (m != null) {
			setSelectedItem(item, false);
			m.remove(item.getName());
			isItemModelChanged_ = true;
		}
	}

	public void removeItems(Class<? extends GrxBaseItem> cls) {
		Map<?, ?> m = pluginMap_.get(cls);
		GrxBaseItem[] items = m.values().toArray(new GrxBaseItem[0]);
		for (int i = 0; i < items.length; i++)
			removeItem(items[i]);
	}

	public void removeAllItems() {
		if (currentMode_ == null)
			return;
		for (int i = 0; i < currentMode_.activeItemClassList_.size(); i++)
			removeItems(currentMode_.activeItemClassList_.get(i));
	}

	public void renamePlugin(GrxBasePlugin item, String newName) {
		//@SuppressWarnings("unchecked")
		Map<String, GrxBasePlugin> m = pluginMap_.get(item.getClass());
        if (m.get(newName) == null) {
		    m.remove(item.getName());
		    m.put(newName, item);
		    item.setName(newName);
		    isItemModelChanged_ = true;
        } 
	}

	public void setVisibleItem() {
		root_.removeAllChildren();
		
		ArrayList<Class<? extends GrxBaseItem>> cList = currentMode_.activeItemClassList_;
		for (int i = 0; i < cList.size(); i++) {
			root_.add(new DynamicUtilTreeNode(cList.get(i), new Hashtable()));
			Iterator it = pluginMap_.get(cList.get(i)).values().iterator();
			while (it.hasNext())
				_addItemNode((GrxBaseItem) it.next());
		}
		
		isItemModelChanged_ = true;
	}
	
	public ArrayList<GrxBaseItem> getActiveItemList() {
		ArrayList<Class<? extends GrxBaseItem>> cList = currentMode_.activeItemClassList_;
		ArrayList<GrxBaseItem> iList = new ArrayList<GrxBaseItem>();
		for (int i = 0; i < cList.size(); i++) {
			Iterator it =  pluginMap_.get(cList.get(i)).values().iterator();
			while (it.hasNext()) {
                GrxBaseItem item = (GrxBaseItem)it.next();
				iList.add(item);
            }
		}
		return iList;
	}

	public GrxBaseItem getItem(Class<? extends GrxBaseItem> cls, String name) {
		Iterator it = pluginMap_.get(cls).values().iterator();
		while (it.hasNext()) {
			GrxBaseItem item = (GrxBaseItem)it.next();
			if (item.toString().equals(name) || name == null)
				return item;
		}
		return null;
	}

	public GrxBaseItem getItem(String name) {
		Iterator it = pluginMap_.values().iterator();
		while (it.hasNext()) {
			HashMap m = (HashMap) it.next();
			Object o = m.get(name);
			if (o != null && o instanceof GrxBaseItem)
				return (GrxBaseItem) o;
		}
		return null;
	}

	public Map getItemMap(Class<? extends GrxBaseItem> cls) {
		return pluginMap_.get(cls);
	}

	public GrxBaseItem getSelectedItem(Class<? extends GrxBaseItem> cls, String name) {
		for (int i = 0; i < selectedItemList_.size(); i++) {
			GrxBaseItem item = selectedItemList_.get(i);
			if (cls.isAssignableFrom(item.getClass()) && (name == null || name.equals(item.getName())))
				return item;
		}
		return null;
	}

	public List<GrxBaseItem> getSelectedItemList(Class<? extends GrxBaseItem> cls) {
		ArrayList<GrxBaseItem> list = new ArrayList<GrxBaseItem>();
		for (int i = 0; i < selectedItemList_.size(); i++) {
			GrxBaseItem item = selectedItemList_.get(i);
			if (cls.isInstance(item))
				list.add(item);
		}
		return list;
	}

	public List<GrxBaseView> getActiveViewList() {
		return selectedViewList_;
	}

	public GrxBaseView getView(Class<? extends GrxBaseView> cls) {
		HashMap m = pluginMap_.get(cls);
		if (m != null) {
			Iterator it = m.values().iterator();
			if (it.hasNext())
				return (GrxBaseView) it.next();
		}
		return null;
	}

	public GrxBaseView getView(String name) {
		Iterator it = pluginMap_.values().iterator();
		while (it.hasNext()) {
			HashMap m = (HashMap) it.next();
			Object o = m.get(name);
			if (o != null && o instanceof GrxBaseView)
				return (GrxBaseView) o;
		}
		return null;
	}

	public void setSelectedItem(GrxBaseItem item, boolean select) {
		if (item == null)
			return;
		
		if (select ^ item.isSelected())
			isItemSelectionChanged_ = true;

		if (select && item.isExclusive()) {
			//@SuppressWarnings("unchecked")
			Iterator<GrxBaseItem> it = getItemMap(item.getClass()).values().iterator();
			while (it.hasNext()) {
			    GrxBaseItem	i = it.next();
                if (i != item) 
                    i.setSelected(false);
            }
		}

		item.setSelected(select);
	}

	public void reselectItems() {
		isItemSelectionChanged_ = true;
	}

	public void clearItemSelection() {
		Iterator i = pluginMap_.values().iterator();
		while (i.hasNext()) {
			Iterator j = ((Map) i.next()).values().iterator();
			while (j.hasNext()) {
				GrxBasePlugin p = (GrxBasePlugin)j.next();
				if (p instanceof GrxBaseItem)
					setSelectedItem((GrxBaseItem)p, false);
			}
		}
	}
	
	public String getItemTitle(Class<? extends GrxBasePlugin> cls) {
        return pinfoMap_.get(cls).title;
	}

	private class PluginInfo {
		String title;
		File   lastDir;
		FileFilter filter;
		JMenu  menu;
	}
	
	public JMenu getItemMenu(final Class<? extends GrxBaseItem> cls) {
		final PluginInfo pi = pinfoMap_.get(cls);
		JMenu menu = pi.menu;
		if (menu == null) {
			menu = pi.menu = new JMenu(getItemTitle(cls));
			
			JMenuItem create = new JMenuItem("create");
			menu.add(create);
			create.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					GrxBaseItem item = createItem(cls, null);
				}
			});
			
			JMenuItem load = new JMenuItem("load");
			menu.add(load);
			load.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					JFileChooser fc = getFileChooser();

					fc.setCurrentDirectory(pi.lastDir);
					if (pi.filter != null)
					    fc.setFileFilter(pi.filter);
					
					if (fc.showOpenDialog(getFrame()) == JFileChooser.APPROVE_OPTION) {
						GrxBaseItem item = loadItem(cls, null, fc.getSelectedFile().getAbsolutePath());
						pi.lastDir = fc.getCurrentDirectory();
					}
				}
			});
			
			JMenuItem clear = new JMenuItem("clear");
			menu.add(clear);
			clear.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int ans = JOptionPane.showConfirmDialog(
						getFrame(), 
						"Remove all the items : "+GrxPluginManager.this.getItemTitle(cls)+") ?", "remove items", 
						JOptionPane.OK_CANCEL_OPTION, 
						JOptionPane.QUESTION_MESSAGE, ROBOT_ICON);
					if (ans == JOptionPane.OK_OPTION)
						removeItems(cls);
				}
			});

			try {
				Method m = cls.getMethod("create", (Class[]) null);
				Class c = m.getDeclaringClass();
				create.setEnabled(!(c == GrxBaseItem.class));
				
				m = cls.getMethod("load", File.class);
				c = m.getDeclaringClass();
				load.setEnabled(!(c == GrxBaseItem.class));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return menu;
	}

	void shutdown() {
		Iterator it = pluginMap_.values().iterator();
		for (; it.hasNext();) {
			Iterator it2 = ((Map) it.next()).values().iterator();
			for (; it2.hasNext();)
				((GrxBasePlugin) it2.next()).shutdown();
		}
		
		try {
			GrxCorbaUtil.getORB().shutdown(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}

	public void setProjectProperty(String key, String val) {
		currentProject_.setProperty(key, val);
	}

	public String getProjectProperty(String key) {
		return currentProject_.getProperty(key);
	}
	
	public JFileChooser getFileChooser() {
		if (fileChooser_ == null)
			fileChooser_ = new JFileChooser();
		FileFilter aaf = fileChooser_.getAcceptAllFileFilter();
		FileFilter ff[] = fileChooser_.getChoosableFileFilters();
		for (int i=0; i<ff.length; i++)
			fileChooser_.removeChoosableFileFilter(ff[i]);
		fileChooser_.setFileFilter(aaf);
			
		return fileChooser_;
	}
	
	public String getCurrentModeName() {
		if (currentMode_ == null)
			return null;
		return currentMode_.getName();
	}
	
	public JMenu getProjectMenu() {
		return currentProject_.getMenu();
	}
  
    public String getHomePath() {
        return homePath_;
    }
	
	public void restoreProcess() {
		GrxProcessManagerView pmView = (GrxProcessManagerView) getView(GrxProcessManagerView.class);
		if (pmView == null) 
			return;
		
		processingWindow_.setMessage("loading process manager settings ...");
		processingWindow_.setVisible(true);
		pmView.loadProcessList(rcProject_.getElement());
		pmView.loadProcessList(currentProject_.getElement());
		processingWindow_.setVisible(false);
	}
}

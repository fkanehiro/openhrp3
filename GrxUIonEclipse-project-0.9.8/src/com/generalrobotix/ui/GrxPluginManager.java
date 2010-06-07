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
 *  GrxPluginManager.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.omg.PortableServer.POA;

import org.w3c.dom.Element;

import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.grxui.PreferenceConstants;

import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.GrxPluginLoader;
import com.generalrobotix.ui.util.GrxProcessManager;
import com.generalrobotix.ui.util.GrxXmlUtil;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.OrderedHashMap;
import com.generalrobotix.ui.util.SynchronizedAccessor;
import com.generalrobotix.ui.util.FileUtil;
import com.generalrobotix.ui.item.GrxModeInfoItem;
import com.generalrobotix.ui.item.GrxProjectItem;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.util.GrxServerManager;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @brief プラグイン管理クラス GrxUIの核になるクラス。プラグインのロード等の、初期化を実行する。
 *        プラグインとそのアイテムのマップ（#pluginMap_）、プラグインとその情報のマップ（#pinfoMap_）などを持つ。
 *        各種プラグインはこのクラスへの参照を持ち、必要に応じてこのクラスから情報を取得する事ができる。
 * @see GrxUIFrame
 * @see GrxPluginLoader
 * @see GrxProjectItem
 */
public class GrxPluginManager implements IPropertyChangeListener {
    // project
    private GrxProjectItem currentProject_;
    private GrxModeInfoItem currentMode_;
    private GrxBaseItem focusedItem_ = null;

    // for managing items
    public GrxPluginLoader pluginLoader_;
    public HashMap<Class<? extends GrxBasePlugin>, OrderedHashMap> pluginMap_ = new HashMap<Class<? extends GrxBasePlugin>, OrderedHashMap>(); // プラグインとその生成したアイテムのマップ
    private List<GrxBaseView> selectedViewList_ = new ArrayList<GrxBaseView>();
    private File homePath_;
    private Map<Class<? extends GrxBasePlugin>, PluginInfo> pinfoMap_ = new HashMap<Class<? extends GrxBasePlugin>, PluginInfo>();
    
    private Map<Class<? extends GrxBaseItem>, List<GrxItemChangeListener>> itemChangeListener_ = 
    	new HashMap<Class<? extends GrxBaseItem>, List<GrxItemChangeListener>>(); 
    public static final int ADD_ITEM=0;
    public static final int REMOVE_ITEM=1;
    public static final int SELECTED_ITEM=2;
    public static final int NOTSELECTED_ITEM=3;
    public static final int SETNAME_ITEM=4;
    public static final int FOCUSED_ITEM=5;
    public static final int NOTFOCUSED_ITEM=6;
    public static final int CHANGE_MODE=7;
    
    // for CORBA
    public POA poa_;
    public org.omg.CORBA.ORB orb_;

    /**
     * @brief GrxPluginManagerのコンストラクタ.
     *        まず、プラグインローダ（GrxPluginLoader）のインスタンス#pluginLoader_を作成する。<br>
     *        そして最初に、GrxModeInfoItemをロードする。これは「モード」を管理するアイテムプラグインである。<br>
     *        モードの設定はsetInitialMode関数内で行われるのでそちらを参照。<br>
     *        次にプロジェクトを司るアイテム（GrxProjectItem）を作成する。
     *        Javaのプロパティ「PROJECT」によりデフォルトのプロジェクトが指定されている場合、それをロードする。（なければ生成する。）<br>
     *		  各CORBAサーバーを起動する。<br>
     * @see GrxPluginLoader
     * @see GrxModeInfoItem
     * @see GrxPluginManager#start()
     * @see GrxProjectItem
     */
    public GrxPluginManager() {
        GrxDebugUtil.println("[PM] GrxPluginManager created"); //$NON-NLS-1$
        String dir = Activator.getDefault().getPreferenceStore().getString("PROJECT_DIR"); //$NON-NLS-1$
        if(dir.equals("")) //$NON-NLS-1$
        	dir = System.getenv("PROJECT_DIR"); //$NON-NLS-1$
		if( dir != null ){
			homePath_ = new File(dir);
		}else
			homePath_ = Activator.getDefault().getHomeDir();
        System.out.println("[PM] WORKSPACE PATH=" + ResourcesPlugin.getWorkspace().getRoot().getLocation()); //$NON-NLS-1$

        // TODO: プラグインローダに、プラグインがおいてあるフォルダを指定する方法を検討
        // 1.そもそもプラグイン管理をEclipseでやらせる
        // 2.Eclipseの機能を使ってプラグインのディレクトリを持ってきてもらう
        // 3.とりあえずGrxUIプラグイン自身をロードしたクラスローダを渡しておく <- いまこれ
        pluginLoader_ = new GrxPluginLoader("", GrxPluginManager.class.getClassLoader()); //$NON-NLS-1$
        registerPlugin(GrxModeInfoItem.class);

        File rtcFile = new File( Activator.getDefault().getTempDir(),"rtc.conf");
        if (!rtcFile.exists()){
        	try{
        		FileUtil.resourceToFile(getClass(), "/default_rtc.conf", rtcFile);
        	}catch (IOException ex){
        		ex.printStackTrace();
        	}
        }
        
        versionCheck();
        setInitialMode();
        currentProject_ = new GrxProjectItem("newproject", this); //$NON-NLS-1$
        String defaultProject = System.getProperty("PROJECT", null); //$NON-NLS-1$
        if (defaultProject == null || !currentProject_.load(new File(GrxXmlUtil.expandEnvVal(defaultProject))))
            currentProject_.create();
        
        // サーバの起動　　//
        GrxServerManager serverManager = (GrxServerManager)createItem(GrxServerManager.class, "serverManager");
		if (serverManager != null){
			serverManager.initialize();
			itemChange(serverManager, GrxPluginManager.ADD_ITEM);
		}
		GrxProcessManager processManager = (GrxProcessManager)createItem(GrxProcessManager.class, "processManager");
		if (processManager != null){
			processManager.setProcessList(serverManager);
			itemChange(processManager, GrxPluginManager.ADD_ITEM);
		}
    }

    /**
     * @brief set focused item
     * @param item
     *            focused item
     */
    public void focusedItem(GrxBaseItem item) {
        if (focusedItem_ != item) {
            if (focusedItem_ != null){
                focusedItem_.setFocused(false);
                itemChange(focusedItem_, NOTFOCUSED_ITEM);
            }
            if(item != null){
            	focusedItem_ = item;
            	focusedItem_.setFocused(true);
            	itemChange(focusedItem_, FOCUSED_ITEM);
            }else
            	focusedItem_ = null;
        }
    }

    /**
     * @brief get current focused item
     * @return item
     */
    public GrxBaseItem focusedItem() {
        return focusedItem_;
    }

    /**
     * @brief check if GrxUI perspective is visible or not
     * @return true if visible, false otherwise
     */
    private boolean isPerspectiveVisible() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null)
            return false;
        IWorkbenchPage page = window.getActivePage();
        if (page == null)
            return false;
        IPerspectiveDescriptor pers = page.getPerspective();
        if (!pers.getId().contains(GrxUIPerspectiveFactory.ID))
            return false;
        return true;
    }

    /**
     * @brief update list of views
     * python script から呼ばれた場合、UIスレッド外からの呼び出しとなって、NGなのでsyncexecを使用する。
     */
    private void updateViewList() {
        selectedViewList_.clear();
        
        Display display = Display.getDefault();
        display.syncExec(new Runnable(){
        	public void run(){
        		IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                if (window == null)
                    return;
                IWorkbenchPage page = window.getActivePage();
                if (page == null) {
                    return;
                }
                IPerspectiveDescriptor pers = page.getPerspective();
                // GrxUIパースペクティブが表示されているか？
                if (!pers.getId().contains(GrxUIPerspectiveFactory.ID)) {
                    return;
                }

                for (IViewReference i : page.getViewReferences()) {
                    // 未初期化のビューは初期化する
                    IViewPart v = i.getView(true);
                    if (v != null && GrxBaseViewPart.class.isAssignableFrom(v.getClass())) {
                        GrxBaseView view = ((GrxBaseViewPart) v).getGrxBaseView();
                        selectedViewList_.add(view);
                    }
                }
        	}
        });
       
    }

    /**
     * 全体の処理の開始. 最初に、CORBAのスレッドを開始する。<br>
     * 今のところMODEは”Simulation”のみとし、以下の機能は使っていない。<br>
     * 次にデフォルトのモードをJavaのプロパティ「MODE」から決めてsetMode()を実行する。<br>
     * 「モード」はロードすべきプラグインとその配置のプリセットであり、設定ファイルgrxuirc.xmlにて指定されている。<br>
     * デフォルトのモードが指定されていない場合、最初に現れたモードをデフォルトとして、ダイアログを出してユーザに選択を求める。
     */
    public void start() {
    	// OnlineViewerなどのサーバの登録用
        System.out.println("[PM] START GrxPluginManager"); //$NON-NLS-1$
        new Thread() {
            public void run() {
                try {
                    poa_ = GrxCorbaUtil.getRootPOA();
                    poa_.the_POAManager().activate();
                    GrxDebugUtil.println("Corba Server Ready."); //$NON-NLS-1$
                    orb_ = GrxCorbaUtil.getORB();
                    orb_.run();
                    orb_.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                GrxCorbaUtil.clearOrb();
            }
        }.start();

/*	　　モードの選択は、現在使用していない。
        String defaultMode = System.getProperty("MODE", ""); //$NON-NLS-1$ //$NON-NLS-2$
        GrxModeInfoItem mode = (GrxModeInfoItem) getItem(GrxModeInfoItem.class, defaultMode);
        GrxDebugUtil.println("[PM] current mode=" + mode); //$NON-NLS-1$

        Map<?, ?> m = pluginMap_.get(GrxModeInfoItem.class);
        GrxModeInfoItem[] modes = (GrxModeInfoItem[]) m.values().toArray(new GrxModeInfoItem[0]);

        System.out.println("[PM] try to setMode"); //$NON-NLS-1$

        try {
            if (mode == null) {
                int ans = 0;
                if (modes.length > 1) {
                    String[] modeInfoNames = new String[modes.length];
                    for (int i = 0; i < modes.length; i++)
                        modeInfoNames[i] = modes[i].getName();
                    MessageDialog dlg = new MessageDialog(null, MessageBundle.get("GrxPluginManager.dialog.title.mode"), null, MessageBundle.get("GrxPluginManager.dialog.message.mode"), MessageDialog.NONE, modeInfoNames, 0); //$NON-NLS-1$ //$NON-NLS-2$
                    ans = dlg.open();
                }
                mode = modes[ans];
            }
            setMode(mode);
            // frame_.updateModeButtons(modes, currentMode_);

        } catch (Exception e) {
            GrxDebugUtil.printErr("GrxPluginManager:", e); //$NON-NLS-1$
        }
*/
        
    }

    /**
     * モードを設定する. アクティブなプラグインのリストの更新と、画面の更新を行う。
     */
    /*
    void setMode(GrxModeInfoItem mode) {
        System.out.println("[PM] setMode to " + mode); //$NON-NLS-1$

        if (currentMode_ == mode)
            return;

        // timer_.stop();
        clearItemSelection();

        // update active plugin and create view
        setSelectedItem(mode, true);
        currentMode_ = mode;
        currentMode_.restoreProperties();
        currentProject_.restoreProject();
    }
	*/
    /**
     * @brief get current mode
     * @return current mode
     */
    public GrxModeInfoItem getMode() {
        return currentMode_;
    }

    public void setCurrentMode(GrxModeInfoItem mode){
    	currentMode_ = mode;
    }
    
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public Class<? extends GrxBasePlugin> registerPlugin(String className) {
        Class<?> cls = pluginLoader_.loadClass(className);
        return registerPlugin((Class<? extends GrxBasePlugin>) cls);
    }

    /**
     * エレメントの値をPluginInfoに反映させるためのメソッド
     * 
     * @param el
     * @return
     */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public Class<?> registerPlugin(Element el) {
        Class<?> cls = pluginLoader_.loadClass(el.getAttribute("class")); //$NON-NLS-1$
        Class<? extends GrxBasePlugin> ret = registerPlugin((Class<? extends GrxBasePlugin>) cls);
        if (ret != null) {
            PluginInfo pi = pinfoMap_.get(ret);
            pi.visible = GrxXmlUtil.getBoolean(el, PluginInfo.VISIBLE, true);
            pinfoMap_.put(ret, pi);
        }
        return ret;
    }

    /**
     * プラグインの登録関数。 該当するプラグインが無い場合、nullを返す。
     * 
     * @return プラグインへの参照（GrxBasePluginにキャストして使う）、該当無しの場合はnull
     */
    public Class<? extends GrxBasePlugin> registerPlugin(Class<? extends GrxBasePlugin> cls) {
        if (cls != null && GrxBasePlugin.class.isAssignableFrom(cls)) {
            if (pluginMap_.get(cls) == null) {
                GrxDebugUtil.println("[PM] register " + cls.getName()); //$NON-NLS-1$

                pluginMap_.put(cls, new OrderedHashMap());

                PluginInfo pi = new PluginInfo();
                pi.title = (String) GrxBasePlugin.getField(cls, "TITLE", cls.getSimpleName()); //$NON-NLS-1$
                pi.lastDir = new File(homePath_ + (String) GrxBasePlugin.getField(cls, "DEFAULT_DIR", "")); //$NON-NLS-1$ //$NON-NLS-2$
                String ext = (String) GrxBasePlugin.getField(cls, "FILE_EXTENSION", null); //$NON-NLS-1$
                if (ext != null)
                    pi.filter = "*." + ext;// GrxGuiUtil.createFileFilter(ext); //$NON-NLS-1$
                pinfoMap_.put(cls, pi);
            }

            return cls;
        }
        return null;
    }

    /**
     * @brief アイテムの作成. 指定したアイテムプラグインに、指定したアイテム名で新しいアイテムを作る。
     * @param cls
     *            プラグインのクラス.　GrxXXXItem.classのように指定する。
     * @param name
     *            新しく作成するアイテムの名前。nullの場合アイテムプラグインの指定するデフォルトが使用される。
     * @return アイテムプラグイン。該当するプラグインが無い場合はnullを返す
     */
    public GrxBaseItem createItem(Class<? extends GrxBaseItem> cls, String name) {
        //System.out.println("[PM]@createItem " + name + "(" + cls + ")");
        if (name == null) {
            String baseName = "new" + getItemTitle(cls); //$NON-NLS-1$
            baseName = baseName.toLowerCase().replaceAll(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
            for (int i = 0;; i++) {
                name = baseName + i;
                if (getItem(cls, name) == null)
                    break;
            }
        }
        GrxBaseItem item = (GrxBaseItem) createPlugin(cls, name);
        //GrxDebugUtil.println("[PM]@createItem createPlugin return " + item);
        if (item != null) {
            item.create();
        }
        return item;
    }

    /**
     * @brief
     * @param cls
     * @param name
     * @param url
     * @return
     */
    public GrxBaseItem loadItem(Class<? extends GrxBaseItem> cls, String name, String url) {
        String _url = GrxXmlUtil.expandEnvVal(url);
        if (_url == null)
            return null;

        File f = null;
        try {
            URL u = new URL(_url);
            f = new File(u.getFile());
        } catch (Exception e) {
            //GrxDebugUtil.printErr("loadItem(" + url + ") is not URL format\n", e);
            f = new File(_url);
        }
        try {
			_url = f.getCanonicalPath();
			f = new File(_url);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        if (!f.isFile())
            return null;

        if (name == null){
        	String basename = f.getName().split("[.]")[0]; //$NON-NLS-1$
        	if (getItem(cls, basename) != null){
        		Integer index = 0;
        		do {
        			name = basename + index.toString();
        			index++;
        		}while(getItem(cls, name) != null);
        	}else{
        		name = basename;
        	}
        }
        
        GrxBaseItem item = (GrxBaseItem) createPlugin(cls, name);
        if (item != null) {
            if (item.load(f)) {
                item.setURL(_url);
                item.setDefaultDirectory(f.getParent());
                pinfoMap_.get(cls).lastDir = f.getParentFile();
            } else {
                removeItem(item);
                return null;
            }
        }

        return item;
    }

    /**
     * @brief
     * @param cls
     * @return
     */
    public GrxBaseItem pasteItem(Class<? extends GrxBaseItem> cls, GrxBaseItem item) {
        /*
         * try { HashMap<String, GrxBasePlugin> map = pluginMap_.get(cls);
         * GrxBasePlugin plugin = map.get(item.getName());
         * 
         * plugin = pluginLoader_.createPlugin(cls, name, this);
         * 
         * map.put(name, plugin); return plugin;
         * 
         * } catch (Exception e) { showExceptionTrace("Couldn't load Class:" +
         * cls.getName(), e); }
         */
        return item;
    }

    /**
     * @brief
     * @param cls
     * @param name
     * @return
     */
    public GrxBaseView createView(Class<? extends GrxBaseView> cls, String name) {
        if (name == null)
            name = pinfoMap_.get(cls).title;
        return (GrxBaseView) createPlugin(cls, name);
    }

    /**
     * @brief
     * @param cls
     * @param name
     * @return
     */
    private GrxBasePlugin createPlugin(Class<? extends GrxBasePlugin> cls, String name) {
        if (registerPlugin(cls) == null) {
            GrxDebugUtil.println("[PM]@createPlugin registerPlugin failed"); //$NON-NLS-1$
            return null;
        }
        try {
            GrxBasePlugin plugin = pluginLoader_.createPlugin(cls, name, this);
            if (registerPluginInstance(plugin)) {
                return plugin;
            }
            return plugin;
        } catch (Exception e) {
            showExceptionTrace("Couldn't load Class:" + cls.getName(), e); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * @brief register instance of plugin to this manager
     * @param instance
     *            instance of plugin
     * @return true if registered successfully, false otherwise
     */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public boolean registerPluginInstance(GrxBasePlugin instance) {
        HashMap<String, GrxBasePlugin> map = pluginMap_.get(instance.getClass());
        GrxBasePlugin plugin = map.get(instance.getName());
        if ( plugin != null) {
            GrxDebugUtil.println("[PM]@createPlugin Plugin instance named "+instance.getName()+" is already registered."); //$NON-NLS-1$ //$NON-NLS-2$
            if(plugin instanceof GrxBaseItem){
            	((GrxBaseItem)plugin).delete();
            	map.put(instance.getName(), instance);
            }
            return false;
        }
        map.put(instance.getName(), instance);
        return true;
    }

    /**
     * @brief
     * @param m
     * @param e
     */
    private void showExceptionTrace(String m, Exception e) {
        GrxDebugUtil.printErr(m, e);

        Throwable cause = e.getCause();
        StackTraceElement[] trace = null;
        String msg = m + "\n\n"; //$NON-NLS-1$
        if (cause != null) {
            msg = cause.toString() + "\n\n"; //$NON-NLS-1$
            trace = cause.getStackTrace();
        } else {
            trace = e.getStackTrace();
        }

        for (int i = 0; i < trace.length; i++) {
            msg += "at " + trace[i].getClassName() + "." + trace[i].getMethodName() + "("; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            if (trace[i].isNativeMethod()) {
                msg += "(Native Method)\n"; //$NON-NLS-1$
            } else if (trace[i].getFileName() == null) {
                msg += "(No source code)\n"; //$NON-NLS-1$
            } else {
                msg += trace[i].getFileName() + ":" + trace[i].getLineNumber() + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        MessageDialog.openWarning(null, "Exception Occered", msg); //$NON-NLS-1$
    }

    /**
     * @brief remove item
     * @param item item to be removed
     */
    public void removeItem(GrxBaseItem item) {
        Map<?, ?> m = pluginMap_.get(item.getClass());
        if (m != null) {
            setSelectedItem(item, false);
            m.remove(item.getName());
            itemChange(item, REMOVE_ITEM);
        }
    }

    /**
     * @brief remove all items which are instances of specified class
     * @param cls class
     */
    public void removeItems(Class<? extends GrxBaseItem> cls) {
        Map<?, ?> m = pluginMap_.get(cls);
        GrxBaseItem[] items = m.values().toArray(new GrxBaseItem[0]);
        for (int i = 0; i < items.length; i++)
        	items[i].delete();
    }

    /**
     * @brief remove all items which are instances of active classes in the
     *        current mode
     */
    public void removeAllItems() {
        if (currentMode_ == null)
            return;
        for (int i = 0; i < currentMode_.activeItemClassList_.size(); i++)
            removeItems(currentMode_.activeItemClassList_.get(i));
    }

    /**
     * @brief rename plugin. If new name is already used, name is not changed.
     * @param item
     *            plugin to be renamed
     * @param newName
     *            new name
     * @return true renamed successfully, false otherwise
     */
    public boolean renamePlugin(GrxBasePlugin item, String newName) {
        OrderedHashMap m = pluginMap_.get(item.getClass());
        if (m == null) {
            System.out.println("map for " + item.getClass() + " doesn't exist in pluginMap_"); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        if (m.get(newName) == null) {
            if (!newName.equals(item.getName())) {
                m.remove(item.getName());
                m.put(newName, item);
                item.setName(newName);
            }
            return true;
        } else {
            System.out.println("GrxPluginManager.renamePlugin() : " + newName + " is already used"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return false;
    }

    /**
     * @brief
     * @return
     */
    public ArrayList<GrxBaseItem> getActiveItemList() {
        ArrayList<Class<? extends GrxBaseItem>> cList = currentMode_.activeItemClassList_;
        ArrayList<GrxBaseItem> iList = new ArrayList<GrxBaseItem>();
        for (int i = 0; i < cList.size(); i++) {
            Iterator it = pluginMap_.get(cList.get(i)).values().iterator();
            while (it.hasNext()) {
                GrxBaseItem item = (GrxBaseItem) it.next();
                iList.add(item);
            }
        }
        return iList;
    }

    /**
     * @brief
     * @param cls
     * @param name
     * @return
     */
    public GrxBaseItem getItem(Class<? extends GrxBaseItem> cls, String name) {
        Iterator it = pluginMap_.get(cls).values().iterator();
        while (it.hasNext()) {
            GrxBaseItem item = (GrxBaseItem) it.next();
            if (item.toString().equals(name) || name == null) {
                //GrxDebugUtil.println("[PM] getItem success " + item);
                return item;
            }
        }
        GrxDebugUtil.println("[PM] fault getItem " + cls.getName() + ":" + name); //$NON-NLS-1$ //$NON-NLS-2$
        return null;
    }

    /**
     * @brief
     * @param name
     * @return
     */
    public GrxBaseItem getItem(String name) {
        Iterator<OrderedHashMap> it = pluginMap_.values().iterator();
        while (it.hasNext()) {
            HashMap m = (HashMap) it.next();
            Object o = m.get(name);
            if (o != null && o instanceof GrxBaseItem)
                return (GrxBaseItem) o;
        }
        return null;
    }

    /**
     * @brief
     * @param cls
     * @return
     */
    public Map<?, ?> getItemMap(Class<? extends GrxBaseItem> cls) {
        return pluginMap_.get(cls);
    }

    /**
     * @brief
     * @param cls
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public <T> T getSelectedItem(Class<? extends GrxBaseItem> cls, String name) {
    	Map<String, ? extends GrxBaseItem> oMap = pluginMap_.get(cls);
    	if(oMap==null) return null;
    	if(name != null){
    		GrxBaseItem item = oMap.get(name);
    		if(item.isSelected())
    			return (T)item;
    		else
    			return null;
    	}else{
    		for(GrxBaseItem item : oMap.values()){
    			if(item.isSelected())
    				return (T)item;
    		}
    		return null;
    	}
    }

    /**
     * @brief
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public <T> List<T> getSelectedItemList(Class<? extends GrxBaseItem> cls) {
        ArrayList<T> list = new ArrayList<T>();
        Map<String, ? extends GrxBaseItem> oMap = pluginMap_.get(cls);
        if(oMap==null) return list;
        for(GrxBaseItem item : oMap.values()){
            if (item.isSelected())
                list.add((T)item);
        }
        return list;
    }

    /**
     * @brief
     * @return
     */
    public List<GrxBaseView> getActiveViewList() {
    	updateViewList();
        return selectedViewList_;
    }

    /**
     * @brief
     * @param cls
     * @return
     */
    public GrxBaseView getView(Class<? extends GrxBaseView> cls) {
        updateViewList();
        for (GrxBaseView v : selectedViewList_)
            if (v.getClass() == cls)
                return v;

        return null;
    }

    public synchronized GrxBaseView getView(String name) {
        updateViewList();
        for (GrxBaseView v : selectedViewList_) {
            if (v.getName().equals(name)) {
                return v;
            }
        }
        return null;
    }

    /**
     * @brief select/unselect item
     * @param item
     *            item to be selected/unselected
     * @param select
     *            true to select, false to unselect
     */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	public void setSelectedItem(GrxBaseItem item, boolean select) {
        if (item == null)
            return;

         if (select && item.isExclusive()) {
            for (GrxBaseItem i : (Collection<GrxBaseItem>) getItemMap(item.getClass()).values()) {
                if (i != item) {
                    i.setSelected(false);
                    itemChange(item, NOTSELECTED_ITEM);
                }
            }
        }

        // GrxDebugUtil.println("[PM]@setSelectedItem "+item.getName()+" to "+select+". and now changed? "+isItemSelectionChanged_
        // );

        item.setSelected(select);
        if(select)	itemChange(item, SELECTED_ITEM);
        else		itemChange(item, NOTSELECTED_ITEM);
    }


    /**
     * @brief unselect all items
     */
    public void clearItemSelection() {
        Iterator<OrderedHashMap> i = pluginMap_.values().iterator();
        while (i.hasNext()) {
            Iterator j = (i.next()).values().iterator();
            while (j.hasNext()) {
                GrxBasePlugin p = (GrxBasePlugin) j.next();
                if (p instanceof GrxBaseItem)
                    setSelectedItem((GrxBaseItem) p, false);
            }
        }
    }

    /**
     * @brief get title of item class
     * @param cls
     *            item class
     * @return title
     */
    public String getItemTitle(Class<? extends GrxBasePlugin> cls) {
        return pinfoMap_.get(cls).title;
    }

    /**
     * @brief check an item class is visible or not
     * @param cls
     *            item class
     * @return true if visible, false otherwise
     */
    public boolean isItemVisible(Class<? extends GrxBasePlugin> cls) {
        return pinfoMap_.get(cls).visible;
    }

    /**
     * @brief information of plugin class
     */
    private class PluginInfo {
        static final String VISIBLE = "visible"; //$NON-NLS-1$
        String title;
        File lastDir;
        String filter;
        Vector<Action> menu;
        boolean visible;
    }

    /**
     * @brief
     * @param cls
     * @return
     */
    public Vector<Action> getItemMenu(final Class<? extends GrxBaseItem> cls) {
        final PluginInfo pi = pinfoMap_.get(cls);
        Vector<Action> menu = pi.menu;
        if (menu != null) {
            dynamicChangeMenu(cls, menu);
            return menu;
        }
        menu = pi.menu = new Vector<Action>();

        // menu item : create
        Action create = new Action() {
            public String getText() {
                return MessageBundle.get("GrxPluginManager.menu.create"); //$NON-NLS-1$
            }

            public void run() {
                GrxBaseItem item = createItem(cls, null);
                itemChange(item, GrxPluginManager.ADD_ITEM);
    	        setSelectedItem(item, true);
            }
        };
        menu.add(create);

        // menu item : load
        Action load = new Action() {
            public String getText() {
                return MessageBundle.get("GrxPluginManager.menu.load"); //$NON-NLS-1$
            }

            public void run() {
                FileDialog fdlg = new FileDialog(GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);
                String[] fe = { pi.filter };
                fdlg.setFilterExtensions(fe);
                fdlg.setFilterPath(pi.lastDir.getAbsolutePath());
                String fPath = fdlg.open();
                if (fPath != null) {
                    File f = new File(fPath);
                    GrxBaseItem newItem = loadItem(cls, null, f.getAbsolutePath());
                    if(newItem!=null){
	                    itemChange(newItem, GrxPluginManager.ADD_ITEM);
	                    setSelectedItem(newItem, true);
	                    pi.lastDir = f.getParentFile();
                    }
                }
            }
        };
        menu.add(load);

        // menu item : clear
        Action clear = new Action() {
            public String getText() {
                return MessageBundle.get("GrxPluginManager.menu.clear"); //$NON-NLS-1$
            }

            public void run() {
                String mes = MessageBundle.get("GrxPluginManager.dialog.message.removeItem"); //$NON-NLS-1$
                mes = NLS.bind(mes, new String[]{GrxPluginManager.this.getItemTitle(cls)});
                if (MessageDialog.openConfirm(null, MessageBundle.get("GrxPluginManager.dialog.title.removeItem"), mes)) //$NON-NLS-1$
                    removeItems(cls);
            }
        };
        menu.add(clear);

        // menu item : delete
        Action delete = new Action() {
            public String getText() {
                return MessageBundle.get("GrxPluginManager.menu.delete"); //$NON-NLS-1$
            }

            public void run() {
                String mes = MessageBundle.get("GrxPluginManager.dialog.message.deletePlugin"); //$NON-NLS-1$
                mes = NLS.bind(mes, new String[]{GrxPluginManager.this.getItemTitle(cls)});
                if (MessageDialog.openConfirm(null, MessageBundle.get("GrxPluginManager.dialog.title.deletePlugin"), mes)) //$NON-NLS-1$
                   deletePlugin(cls);
            }
        };
        menu.add(delete);
        
        try {
            Method m = cls.getMethod("create", (Class[]) null); //$NON-NLS-1$
            Class<?> c = m.getDeclaringClass();
            create.setEnabled(!(c == GrxBaseItem.class));

            m = cls.getMethod("load", File.class); //$NON-NLS-1$
            c = m.getDeclaringClass();
            load.setEnabled(!(c == GrxBaseItem.class));
            m = cls.getMethod("paste", String.class); //$NON-NLS-1$
            c = m.getDeclaringClass();
            if (c != GrxBaseItem.class) {
                Action paste = new Action() {
                    public String getText() {
                        return "paste"; //$NON-NLS-1$
                    }

                    public void run() {
                        GrxDebugUtil.println("GrxPluginManager.GrxModelItemClass paste Action"); //$NON-NLS-1$
                        // paste();
                    }
                };
                paste.setEnabled(!isEmptyClipBord());
                menu.add(paste);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // if plugin is user then delete menu is enable
        IPreferenceStore store =Activator.getDefault().getPreferenceStore();
        String itemClassList=store.getString(PreferenceConstants.ITEM+"."+PreferenceConstants.CLASS);
	    String[] itemClass=itemClassList.split(PreferenceConstants.SEPARATOR, -1);
	    boolean found=false;
	    for(int i=0; i<itemClass.length; i++){
	    	if(cls.getName().equals(itemClass[i])){
	    		found=true;
	    		break;
	    	}
	    }
	    if(!found)
	    	delete.setEnabled(true);
	    else
	    	delete.setEnabled(false);
        return menu;
    }
    
    /**
     * @brief
     */
    private void dynamicChangeMenu(final Class<? extends GrxBaseItem> cls, Vector<Action> menu) {
        try {
            Method m = cls.getMethod("paste", String.class); //$NON-NLS-1$
            Class<?> c = m.getDeclaringClass();
            if (c != GrxBaseItem.class) {
                for (Action action : menu) {
                    if (action.getText().equals("paste")) { //$NON-NLS-1$
                        action.setEnabled(!isEmptyClipBord());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief
     */
    private boolean isEmptyClipBord() {
        GrxPluginManager.setClipBordVal();
        return GrxPluginManager.getClipBoardVal().length()==0;
    }

    /**
     * @brief shutdown this manager
     */
    public void shutdown() {
        GrxDebugUtil.println("[PM] shutdown."); //$NON-NLS-1$

        Iterator<OrderedHashMap> it = pluginMap_.values().iterator();
        for (; it.hasNext();) {
            Iterator it2 = (it.next()).values().iterator();
            for (; it2.hasNext();)
                ((GrxBasePlugin) it2.next()).shutdown();
        }
        
        GrxProcessManager.shutDown();
        if(orb_!=null)
        	orb_.shutdown(false);
    }

    /**
     * @brief set property associated to key
     * @param key
     *            keyword
     * @param val
     *            property associated to key
     */
    public void setProjectProperty(String key, String val) {
        currentProject_.setProperty(key, val);
    }

    /**
     * @brief get property of current project associated to key
     * @param key
     * @return property of current project
     */
    public String getProjectProperty(String key) {
        return currentProject_.getProperty(key);
    }

    /**
     * @brief get current model name
     * @return current model name
     */
    public String getCurrentModeName() {
        if (currentMode_ == null)
            return null;
        return currentMode_.getName();
    }

    /**
     * @brief get project menu
     * @return project menu
     */
    public Vector<Action> getProjectMenu() {
        return currentProject_.getMenu();
    }

    /**
     * @brief get home path
     * @return home path
     */
    public File getHomePath() {
        return homePath_;
    }

    /**
     * @brief get project name
     * @return project name
     */
    public String getProjectName() {
        return currentProject_.getName();
    }

    /**
     * @brief get current project
     * @return current project
     */
    public GrxProjectItem getProject() {
        return currentProject_;
    }

    /**
     * @brief Get selected GrxBaseItem List on tree view
     * @return List<GrxBaseItem>
     */
    /*
     * comment out by kanehiro. This class should be independent from any
     * specific view class public List<GrxBaseItem>
     * getSelectedGrxBaseItemList(){ return selectedItemOnTreeViewList_; }
     */

    /**
     * @brief Paste event
     * 
     */
    /*
     * comment out by kanehiro. This class should be independent from any
     * specific view class private void paste(){
     * GrxDebugUtil.println("GrxPluginManager.paste."); for (Object o :
     * selectedItemOnTreeViewList_.toArray() ){ if (
     * GrxModelItem.class.isAssignableFrom( o.getClass() ) ){
     * GrxDebugUtil.println("GrxModelItem 存在確認"); } } }
     */

    /**
     * @brief Get clip board value
     * @return clip board value
     */
    private static String getClipBoardVal() {
        return GrxPluginManager.clipValue_.get();
    }

    /**
     * @brief Set clip board value GrxPluginManager.clipValue_
     */
    private static void setClipBordVal() {
        Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable data = clp.getContents(null);
        String ret = ""; //$NON-NLS-1$
        if (data != null && data.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                ret = (String) data.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                GrxDebugUtil.printErr("GrxPluginManager.setClipBordVal: ", e); //$NON-NLS-1$
            }
        }
        GrxPluginManager.clipValue_.set(ret);
    }

    private static SynchronizedAccessor<String> clipValue_ = new SynchronizedAccessor<String>(""); //$NON-NLS-1$
    
    
    public void registerItemChangeListener(GrxItemChangeListener view, Class<? extends GrxBaseItem> cls){
    	if(itemChangeListener_.get(cls)==null)
    		itemChangeListener_.put(cls, new ArrayList<GrxItemChangeListener>());
    	List<GrxItemChangeListener> list = itemChangeListener_.get(cls);
    	list.add(view);
    }
    	
    public void removeItemChangeListener(GrxItemChangeListener view, Class<? extends GrxBaseItem> cls) {
    	if(itemChangeListener_.get(cls)==null) return;
    	List<GrxItemChangeListener> list = itemChangeListener_.get(cls);
    	list.remove(view);
    }
    
    public void itemChange(GrxBaseItem item, int event){
    	Iterator<Class<? extends GrxBaseItem>> it = itemChangeListener_.keySet().iterator();
    	while(it.hasNext()){
    		Class<? extends GrxBaseItem> cls = it.next();
    		if(cls.isAssignableFrom(item.getClass())){
    			List<GrxItemChangeListener> list = itemChangeListener_.get(cls);
    			Iterator<GrxItemChangeListener> itView = list.iterator();
        		while(itView.hasNext())
        			itView.next().registerItemChange(item, event);
    		}
    	}
    }
    
	private void setInitialMode(){
        IPreferenceStore store =Activator.getDefault().getPreferenceStore();
        String modes = store.getString(PreferenceConstants.MODE);
        String[] mode = modes.split(PreferenceConstants.SEPARATOR, -1);
        String itemIndexs = store.getString(PreferenceConstants.MODE+"."+PreferenceConstants.ITEMINDEX);
        String[] itemIndex = itemIndexs.split(PreferenceConstants.SEPARATOR,-1);
        String itemClassList=store.getString(PreferenceConstants.ITEM+"."+PreferenceConstants.CLASS);
        String[] itemClass=itemClassList.split(PreferenceConstants.SEPARATOR, -1);
        String itemVisibleList=store.getString(PreferenceConstants.ITEM+"."+PreferenceConstants.VISIBLE);
        String[] itemVisible=itemVisibleList.split(PreferenceConstants.SEPARATOR, -1);
        String userItemClassList=store.getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASS);
        String[] userItemClass=userItemClassList.split(PreferenceConstants.SEPARATOR, -1);
        String userItemPathList=store.getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASSPATH);
        String[] userItemPath=userItemPathList.split(PreferenceConstants.SEPARATOR, -1);
        String userItemVisibleList=store.getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.VISIBLE);
        String[] userItemVisible=userItemVisibleList.split(PreferenceConstants.SEPARATOR, -1);
        for(int i=0; i<mode.length; i++){
        	GrxModeInfoItem item = (GrxModeInfoItem)createItem(GrxModeInfoItem.class, mode[i]);
        	String[] index=itemIndex[i].split(",",-1);
	        for(int j=0; j<index.length; j++){
	        	int k=Integer.valueOf(index[j]);
	        	if(k<itemClass.length){
		        	if(pluginLoader_.existClass(itemClass[k])){
			        	Class<? extends GrxBasePlugin> plugin=registerPlugin(itemClass[k]);
			        	if (plugin != null) {
			        		PluginInfo pi = pinfoMap_.get(plugin);
			        		pi.visible = itemVisible[k].equals("true")? true : false;
			        		pinfoMap_.put(plugin, pi);
			        		item.addItemClassList(plugin);
			        	}
		        	}
	        	}
	        }
	        for(int j=0; j<userItemClass.length; j++){
	        	if(!userItemClass[j].equals("")){
		        	pluginLoader_.addURL(userItemPath[j]);
		        	if(pluginLoader_.existClass(userItemClass[j])){
		        		Class<? extends GrxBasePlugin> plugin=registerPlugin(userItemClass[j]);
		        		if (plugin != null) {
		        			PluginInfo pi = pinfoMap_.get(plugin);
		        			pi.visible = userItemVisible[j].equals("true")? true : false;
		        			pinfoMap_.put(plugin, pi);
		        			item.addItemClassList(plugin);
		        		}
		        	}
	        	}
	        }
        }
        store.addPropertyChangeListener(this);
        //	最初のモードを選択		//
        GrxModeInfoItem item = (GrxModeInfoItem)getItem(mode[0]);
        setSelectedItem(item, true);
        currentMode_ = item;
    }
    
    private void versionCheck(){
        IPreferenceStore store =Activator.getDefault().getPreferenceStore();
        String version = store.getString(PreferenceConstants.VERSION);
        if(version.equals("") || !version.equals(PreferenceConstants.CURRENT_VERSION)){
        	//  ヴァージョンが変わった場合の処理を必要に応じて実装する
        	if(version.equals("")){
        		store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.ID);
            	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.COM);
            	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.ARGS);
            	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.AUTOSTART);
            	store.setToDefault(PreferenceConstants.PROCESS+"."+PreferenceConstants.USEORB);
        	}
    		store.setValue(PreferenceConstants.VERSION, PreferenceConstants.CURRENT_VERSION);
        }
    }
    
	@SuppressWarnings("unchecked")
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals("userItemChange")){
			String newItemList = (String) event.getNewValue();
			String[] newItems = newItemList.split(PreferenceConstants.SEPARATOR, -1);
			String oldItemList = (String) event.getOldValue();
			String[] oldItems = oldItemList.split(PreferenceConstants.SEPARATOR, -1);
			IPreferenceStore store =Activator.getDefault().getPreferenceStore();
			String userItemPathList=store.getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.CLASSPATH);
	        String[] userItemPath=userItemPathList.split(PreferenceConstants.SEPARATOR, -1);
	        String userItemVisibleList=store.getString(PreferenceConstants.USERITEM+"."+PreferenceConstants.VISIBLE);
	        String[] userItemVisible=userItemVisibleList.split(PreferenceConstants.SEPARATOR, -1);
			Vector<Integer> delList = new Vector<Integer>();
			for(int i=0; i<oldItems.length; i++){
				boolean flg=false;
				for(String newItem : newItems){
					if(newItem.equals(oldItems[i])){
						flg=true;
						break;
					}
				}
				if(!flg && !oldItems[i].equals(""))
					delList.add(new Integer(i));
				
			}
			
			OrderedHashMap modes=(OrderedHashMap) getItemMap(GrxModeInfoItem.class);
			for(Integer i : delList){
				Class<? extends GrxBasePlugin> itemClass=null;
				try {
					itemClass = (Class<? extends GrxBasePlugin>) Class.forName(oldItems[i.intValue()], false, pluginLoader_);
				} catch (ClassNotFoundException e) {
					continue;
				}
				if(currentMode_.activeItemClassList_.contains(itemClass))
					removeItems((Class<? extends GrxBaseItem>) itemClass);
				Iterator<GrxModeInfoItem> it = modes.values().iterator();
		        while (it.hasNext()) {
		        	GrxModeInfoItem mode = it.next();
		        	if(mode.activeItemClassList_.contains(itemClass))
		        		mode.activeItemClassList_.remove(itemClass);
				}
			}
			for(int i=0; i<newItems.length; i++){
				pluginLoader_.addURL(userItemPath[i]);
				Class<? extends GrxBasePlugin> itemClass=null;
				try {
					itemClass = (Class<? extends GrxBasePlugin>) Class.forName(newItems[i], false, pluginLoader_);
				} catch (ClassNotFoundException e) {
					continue;
				}
				Iterator<GrxModeInfoItem> it = modes.values().iterator();
		        while (it.hasNext()) {
		        	GrxModeInfoItem mode = it.next();
		        	if(mode.activeItemClassList_.contains(itemClass)){
		        		PluginInfo pi = pinfoMap_.get(itemClass);
		        		pi.visible = userItemVisible[i].equals("true")? true : false;
		        		pinfoMap_.put(itemClass, pi);
		        	}else{
		        		if(pluginLoader_.existClass(newItems[i])){
				        	Class<? extends GrxBasePlugin> plugin=registerPlugin(newItems[i]);
				        	if (plugin != null) {
				        		PluginInfo pi = pinfoMap_.get(plugin);
				        		pi.visible = userItemVisible[i].equals("true")? true : false;
				        		pinfoMap_.put(plugin, pi);
				        		mode.addItemClassList(plugin);
				        	}
				        }
					}	
		        }
			}
		}
		else if(event.getProperty().equals(PreferenceConstants.FONT_TABLE)){
	        Activator.getDefault().updateTableFont();
		    List<GrxBaseView> list = getActiveViewList();
	        for (GrxBaseView v : list){
	            v.updateTableFont();
	        }
		}
        else if(event.getProperty().equals(PreferenceConstants.FONT_EDITER)){
            Activator.getDefault().updateEditerFont();
            List<GrxBaseView> list = getActiveViewList();
            for (GrxBaseView v : list){
                v.updateEditerFont();
            }
        }
		itemChange(currentMode_, CHANGE_MODE);
	}
	
	@SuppressWarnings("unchecked")
	public void addPlugin(String className, String classPath){
		pluginLoader_.addURL(classPath);
		Class<? extends GrxBasePlugin> itemClass=null;
		try {
			itemClass = (Class<? extends GrxBasePlugin>) Class.forName(className, false, pluginLoader_);
		} catch (ClassNotFoundException e) {
			return;
		}
		OrderedHashMap modes=(OrderedHashMap) getItemMap(GrxModeInfoItem.class);
		Iterator<GrxModeInfoItem> it = modes.values().iterator();
        while (it.hasNext()) {
        	GrxModeInfoItem mode = it.next();
        	if(!mode.activeItemClassList_.contains(itemClass)){
        		if(pluginLoader_.existClass(className)){
		        	Class<? extends GrxBasePlugin> plugin=registerPlugin(className);
		        	if (plugin != null) {
		        		PluginInfo pi = pinfoMap_.get(plugin);
		        		pi.visible = true;
		        		pinfoMap_.put(plugin, pi);
		        		mode.addItemClassList(plugin);
		        	}
		        }
			}	
        }
        itemChange(currentMode_, CHANGE_MODE);
	}
	
	@SuppressWarnings("unchecked")
	public void deletePlugin(Class<? extends GrxBasePlugin> cls){
		OrderedHashMap modes=(OrderedHashMap) getItemMap(GrxModeInfoItem.class);
		if(currentMode_.activeItemClassList_.contains(cls)){
			removeItems((Class<? extends GrxBaseItem>) cls);
			Iterator<GrxModeInfoItem> it = modes.values().iterator();
	        while (it.hasNext()) {
	        	GrxModeInfoItem mode = it.next();
	        	if(mode.activeItemClassList_.contains(cls))
	        		mode.activeItemClassList_.remove(cls);
			}
		}
	    itemChange(currentMode_, CHANGE_MODE);
	}
	
	public void dispose(){
		IPreferenceStore store =Activator.getDefault().getPreferenceStore();
		store.removePropertyChangeListener(this);
	}
	
	public void loadInitialProject(){
	   	IPreferenceStore store =Activator.getDefault().getPreferenceStore();
        String initProjectFile = store.getString(PreferenceConstants.INITIALPROJECT);
        File f = new File(initProjectFile);
        if(f.exists() && f.isFile())
        	currentProject_.load(f);
	}
	
}

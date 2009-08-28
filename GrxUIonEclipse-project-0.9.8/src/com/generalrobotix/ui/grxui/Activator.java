package com.generalrobotix.ui.grxui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.jar.*;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.util.GrxProcessManager;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin{
    public static final String PLUGIN_ID = "com.generalrobotix.ui.grxui";
    private static Activator   plugin;

    public GrxPluginManager    manager_;
    private ImageRegistry      ireg_     = null;
    private FontRegistry 		freg_ = null;
    private ColorRegistry		creg_ = null;
    private boolean           bStartedGrxUI_ = false; 
    private final static String[] images_ = {"sim_start.png",
    											"sim_stop.png",
    											"fastrwd.png",
    											"slowrwd.png",
    											"pause.png",
    											"playback.png",
    											"slowfwd.png",
    											"fastfwd.png",
    											"frame+.png",
    											"frame-.png",
    											"sim_script_start.png",
    											"sim_script_stop.png",
    											"grxrobot1.png"};
	
    // パースペクティブのイベント初期化、振る舞いを定義するクラス
    class EventInnerClass extends PerspectiveAdapter
            implements IWindowListener, IWorkbenchListener, SynchronousBundleListener, FrameworkListener {
    	
        private IWorkbench      workbench_                  = null;
        private BundleContext   context_                    = null;
        public EventInnerClass(IWorkbench workbench, BundleContext context) {
    	    super();
            workbench_ = workbench;
            context_ = context;
        }

        // Listenerをセット
        public void setEventListner() {
            workbench_.addWindowListener(this);
            workbench_.addWorkbenchListener(this);
            context_.addBundleListener(this);
        }
        
        public void perspectiveOpened(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
            if (GrxUIPerspectiveFactory.ID.equals(perspective.getId())) {
                startGrxUI();
            }
        }

        public void perspectiveClosed(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
            if (GrxUIPerspectiveFactory.ID.equals(perspective.getId())) {
                stopGrxUI();
            }
        }

        public void windowActivated(IWorkbenchWindow window) {}

        public void windowClosed(IWorkbenchWindow window) {
            window.removePerspectiveListener(this);
        }

        public void windowDeactivated(IWorkbenchWindow window) {}

        public void windowOpened(IWorkbenchWindow window) {
            window.addPerspectiveListener(this);
        }

        public void postShutdown(IWorkbench workbench) {
            GrxProcessManager.shutDown();
            try {
                GrxCorbaUtil.getORB().shutdown(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public boolean preShutdown(IWorkbench workbench, boolean forced) {
            try {
                stop(context_);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        }

        public void bundleChanged(BundleEvent event) {
            if (event.getBundle().getSymbolicName().equals(PLUGIN_ID)) {
                doBundleEvnt(event);
            }
        }

        public void frameworkEvent(FrameworkEvent event) {
            if (event.getBundle().getSymbolicName().equals(PLUGIN_ID)) {
                //doFrameworkEvnt(event);
            }
        }

        private void doBundleEvnt(BundleEvent event) {
            switch (event.getType()) {
            case BundleEvent.INSTALLED:
                break;
            case BundleEvent.RESOLVED:
                break;
            case BundleEvent.STARTED:
                if (event.getBundle().getSymbolicName().equals(PLUGIN_ID)) {
                    // GrxUIパースペクティブが既に開いている場合の処理
                    startGrxUI();
                    IWorkbench localWorkbench = PlatformUI.getWorkbench();
                    if (localWorkbench != null) {
                        IWorkbenchWindow localWindow = localWorkbench.getActiveWorkbenchWindow();
                        if (localWindow != null) {
                            localWindow.addPerspectiveListener(this);
                        }
                    }
                }
                break;
            case BundleEvent.STARTING:
                break;
            case BundleEvent.STOPPED:
                break;
            case BundleEvent.STOPPING:
                break;
            case BundleEvent.UNINSTALLED:
                break;
            case BundleEvent.UNRESOLVED:
                break;
            case BundleEvent.UPDATED:
                break;
            }
        }

        /*
        private void doFrameworkEvnt(FrameworkEvent event)
        {
            switch( event.getType() )
            {
            case FrameworkEvent.ERROR:
                GrxDebugUtil.println("[ACTIVATOR.EventInnerClass] FrameworkEvent.ERROR in doFrameworkEvnt:" + event.getThrowable().getMessage());
                break;
            case FrameworkEvent.INFO:
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                break;
            case FrameworkEvent.STARTED:
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                break;
            case FrameworkEvent.WARNING:
                break;
            }
        }
        */
    };

    private EventInnerClass eventInnerClass = null;

    public Activator() {
        System.out.println("[ACTIVATOR] CONSTRUCT");
    }

    /**
     * プラグイン起動処理. プラグインマネージャを作り、処理を開始する。
     */
    public void start(BundleContext context)
            throws Exception {
        super.start(context);
        plugin = this;      

        // デバッグ表示モード
        GrxDebugUtil.setDebugFlag(true);

        File cur = new File(".");
        URL cur_url = cur.toURI().toURL();
        GrxDebugUtil.println("[ACTIVATOR] START in " + cur_url);

        if(PlatformUI.isWorkbenchRunning()){
        	//        	 イベントリスナークラスの登録
            eventInnerClass = new EventInnerClass(PlatformUI.getWorkbench(), context);
            eventInnerClass.setEventListner();
            registryImage();
            registryFont();
            registryColor();
            
        }
    }

    public void registryImage() throws Exception{
    	ireg_ = new ImageRegistry();
    	/*
    	if (getPath().matches(".*\\.jar!.+$")) {
    		GrxDebugUtil.println("registryImage" + getPath());
            // Jarファイル化した場合の ireg_ セット
            URL localUrl = new URL(getPath().substring(0, getPath().length() - 2));
            JarInputStream in = new JarInputStream(localUrl.openStream());
            for (JarEntry i = in.getNextJarEntry(); i != null; i = in.getNextJarEntry()) {
                if (i.getName().matches("resources/images/.*\\.[Pp][Nn][Gg]")) {
                    URL resourceUrl = getClass().getClassLoader().getResources(i.getName()).nextElement();
                    ireg_.put(resourceUrl.getFile(), ImageDescriptor.createFromURL(resourceUrl));
                }
            }
        } else {
            File imgDir = new File(getPath() + "/resources/images");
            GrxDebugUtil.println("[ACTIVATOR] imgDir " + imgDir.getPath());
            File[] fs = imgDir.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith(".png");
                }
            });
            for (File f : fs) {
                ireg_.put(f.getName(), ImageDescriptor.createFromURL(f.toURI().toURL()));
            }
        }
		*/
    	for(int i=0; i<images_.length; i++){
    		URL url = getClass().getResource("/resources/images/"+images_[i]);
    		ireg_.put(images_[i], ImageDescriptor.createFromURL(url));
    	}
        
    }
    
    public void registryFont(){
    	freg_ = new FontRegistry();
    	FontData[] monospaced = {new FontData("monospaced", 10, SWT.NORMAL)};
    	freg_.put("monospaced", monospaced);
    	FontData[] dialog10 = {new FontData("dialog", 10, SWT.NORMAL )};
    	freg_.put("dialog10", dialog10);
    	FontData[] dialog12 = {new FontData("dialog", 12, SWT.NORMAL )};
    	freg_.put("dialog12", dialog12);
 
    }
    
    public void registryColor(){
    	creg_ = new ColorRegistry();
    	RGB focusedColor = new RGB(0,0,100);
    	creg_.put("focusedColor", focusedColor);
    	RGB markerColor = new RGB(255,128,128);
    	creg_.put("markerColor", markerColor);
    }
    
    
    /**
     * プラグインのディレクトリ取得. プラグイン内の画像や設定ファイルを取得するとき使えるかも
     * 
     * @return プラグインのトップディレクトリ
     */
    public static String getPath() {
        URL entry = getDefault().getBundle().getEntry("/");
        String pluginDirectory = "";
        try {
            pluginDirectory = FileLocator.resolve(entry).getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pluginDirectory;
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * nsHsotとnsPortの参照渡し
     * 
     */
    public static void refNSHostPort(StringBuffer nsHost, StringBuffer nsPort) {
        /*
        if (plugin != null) {
            if (plugin.manager_ != null) {
                GrxORBMonitorView omView = (GrxORBMonitorView) plugin.manager_.getView("GrxORBMonitorView");
                if (omView != null) {
                    nsHost.append(omView.getNSHost());
                    nsPort.append(omView.getNSPort());
                }
            }
        }
        */
        if (nsHost.length() == 0){
            nsHost.append(GrxCorbaUtil.nsHost());
        }
        if (nsPort.length() == 0){
            nsPort.append(Integer.toString(GrxCorbaUtil.nsPort()));
        }
    }

    // アイコンの取得
    public Image getImage(String iconName) {
        return ireg_.get(iconName);
    }

    // アイコンデスクリプタの取得
    public ImageDescriptor getDescriptor(String iconName) {
        return ireg_.getDescriptor(iconName);
    }

    public Font getFont(String fontName){
    	return freg_.get(fontName);
    }
    
    public Color getColor(String colorName){
    	return creg_.get(colorName);
    }
    
    public void setColor(String colorName, RGB rgb){
    	creg_.put(colorName, rgb);
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context)
            throws Exception {
        plugin = null;
        super.stop(context);
    }

    // GrxUIパースペクティブが有効になったときの動作
    public void startGrxUI() {
        if( !bStartedGrxUI_ ) {
            
            //暫定処置 Grx3DViewがGUIのみの機能に分離できたらstopGrxUIか
            //manager_.shutdownで処理できるように変更
            GrxProcessManager.shutDown();
            
            bStartedGrxUI_ = true;
            manager_ = new GrxPluginManager();
            manager_.start();
        }
    }

    // GrxUIパースペクティブが無効になったときの動作
    public void stopGrxUI() {
        manager_.shutdown();
        manager_ = null;
        bStartedGrxUI_ = false;
    }
    
    public ImageRegistry getImageRegistry(){
    	return ireg_;
    }
    
    public FontRegistry getFontRegistry(){
    	return freg_;
    }
    
    public ColorRegistry getColorRegistry(){
    	return creg_;
    }
}

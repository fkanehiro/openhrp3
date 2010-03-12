package jp.go.aist.hrp.joystick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    // The plug-in ID
	public static final String PLUGIN_ID = "JoystickPlugin";

	// The shared instance
	private static Activator plugin;

    private static final String  LINUX_HOME_DIR   = System.getenv("HOME") + File.separator ;
    private static final String  WIN_HOME_DIR     = System.getenv("APPDATA") + File.separator;
    private static final String  LINUX_TMP_DIR   = LINUX_HOME_DIR + ".Joystick_aist" + File.separator;
    private static final String  WIN_TMP_DIR     = WIN_HOME_DIR + "Joystick_aist" + File.separator;
    private static final File TMP_DIR = initTempDir();
    
	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	/**
	 * Used to get the path to plug-in directory
	 *
	 * @return absolute path to the top of plug-in directory
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
     * 設定ファイル、ログファイル、ロックファイルなど。
     * OpenHRP固有の目的に使用するファイル群の共通格納先ディレクトリ
     * 
     * @return テンポラリディレクトリ
     */
    public File getTempDir(){
        return TMP_DIR;
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
     * @biref Copy a file from copy source to copy target[destFile]
     *         
     * @param joystickPluginManager  コピー元のリソースを保持する管理するjoystickPluginManager
     * @param srcName                コピー元のリソース名
     * @param destFile               コピー先のFile
     * @throws IOException           何らかの入出力処理例外が発生した場合
     */
    public static void resourceToFile(Class<jp.go.aist.hrp.joystick.Activator> pluginManager, String srcName, File destFile)
    throws IOException {
        InputStream in = pluginManager.getResourceAsStream(srcName.toString());
        OutputStream out = new FileOutputStream(destFile.toString());
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
    }
	
	/**
	 * Create the configuration file
	 * @return Path to the created configuration file
	 */
    private static File createConfigFile() {
        File rtcFile = new File( Activator.getDefault().getTempDir(),"rtc.conf");
        if (!rtcFile.exists()){
        	try{
        		resourceToFile(Activator.class, "/default_rtc.conf", rtcFile);
        	}catch (IOException ex){
        		ex.printStackTrace();
        	}
        }
        return rtcFile;
    }
	
	/**
	 * Searches for configuration file
	 * @return Path to the configuration file
	 */
	public static String getConfigFilePath(){
		String confPath=""; //$NON-NLS-1$
        File defualtRtcFile = new File(Activator.getDefault().getTempDir() + File.separator + "rtc.conf");
        if( defualtRtcFile.isFile() ){
            confPath = defualtRtcFile.getPath(); 
        } else {
        	confPath = createConfigFile().getPath();
        }
    	System.out.println("[Joystick] default Config File path="+confPath); //$NON-NLS-1$
		return confPath;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
    
	static private File initTempDir() {
        File ret = null;
        
        if ( System.getProperty("os.name").equals("Linux") ||
             System.getProperty("os.name").equals("Mac OS X")) {
            ret = new File(LINUX_TMP_DIR);
        } else { //Windows と　仮定
            ret = new File(WIN_TMP_DIR);
        }
        if( !ret.exists() ){
            ret.mkdirs();
        }
        
        return ret;
    }

}

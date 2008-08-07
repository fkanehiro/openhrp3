package com.generalrobotix.ui.grxui;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.GrxDebugUtil;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "com.generalrobotix.ui.grxui";
	private static Activator plugin;
	public GrxPluginManager manager_;

 	private ImageRegistry ireg_ = new ImageRegistry();

	public Activator() {
		System.out.println("[ACTIVATOR] CONSTRUCT");
	}

	/**
	 * �ŏ��̃��プラグイン起動処理. プラグインマネージャを作り、処理を開始する。
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// デバッグ表示モード
		GrxDebugUtil.setDebugFlag(true);

		File cur = new File(".");
		URL cur_url = cur.toURI().toURL();
		GrxDebugUtil.println("[ACTIVATOR] START in " + cur_url );

		// TODO: Jarファイル化した場合の取得方法などについて調査
		File imgDir = new File( getPath()+"/resources/images" );
		File[] fs = imgDir.listFiles( new FileFilter(){
			public boolean accept(File f){
				return f.getName().endsWith( ".png" );
			}
		} );
		for( File f: fs  ){
			ireg_.put( f.getName(), ImageDescriptor.createFromURL(f.toURI().toURL()) );
			//GrxDebugUtil.println("[ACTIVATOR] load image "+f.getName() );
		}

		manager_ = new GrxPluginManager();
		if( ! manager_.initSucceed ) {
			return;
		}
		manager_.start();
	}

	/**
	 * プラグインのディレクトリ取得. プラグイン内の画像や設定ファイルを取得するとき使えるかも
	 * @return プラグインのトップディレクトリ
	 */
	public static String getPath(){
		URL entry = getDefault().getBundle().getEntry("/");
		String pluginDirectory="";
		try {
			pluginDirectory = FileLocator.resolve(entry).getPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pluginDirectory;
	}

	// アイコンの取得
	public Image getImage( String iconName ){
		return ireg_.get( iconName );
	}

	// アイコンデスクリプタの取得
	public ImageDescriptor getDescriptor( String iconName ){
		return ireg_.getDescriptor( iconName );
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		manager_.shutdown();
		
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}

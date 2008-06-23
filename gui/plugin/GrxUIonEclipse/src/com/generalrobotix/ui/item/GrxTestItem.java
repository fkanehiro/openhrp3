/*
 *  GrxTestItem.java
 *
 *  Copyright (C) 2007 s-cubed, Inc.
 *  All Rights Reserved
 *
 *  @author keisuke kobayashi (s-cubed, Inc.)
 */
 
package com.generalrobotix.ui.item;

import java.io.File;
import org.eclipse.jface.action.Action;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxPluginManager;

@SuppressWarnings("serial")
public class GrxTestItem extends GrxBaseItem {
	// GrxPluginManager内で、PluginInfoに保存される。
	// ファイル表示のフィルタに使われたりするので、重要。
	public static final String TITLE = "Test Item"; // ItemViewでの表示に使用される
	public static final String FILE_EXTENSION = "test";
	public static final String DEFAULT_RELATIVE_PATH = "testDir";

	public GrxTestItem(String name, GrxPluginManager manager) {
		super(name, manager);

		// メニューの追加
		Action item = new Action(){
			public String getText(){ return "保存"; }
			public void run(){ save(); }
		};
		setMenuItem(item);
		item = new Action(){
			public String getText(){ return "別名で保存"; }
			public void run(){ saveAs(); }
		};
		setMenuItem(item);

		/* 
		 * 排他的な選択
		 * 同じアイテムプラグインのアイテムは、一つしか選択できないようにする
		 * 他のアイテムプラグインのは選ばれててももちろん平気。
		 */
		setExclusive(true);

		// プロパティをプラグイン内で指定する場合は、以下のようにする。プロジェクトファイルに書いてもいい。
		setProperty("PluginName",getName() );
		setProperty("Name1","Value1");
		setProperty("Name2","Value2");
		setProperty("Name3","Value3");
	}

	public void save(){
		System.out.println("GrxTestItem Save Method!");
	}
	public void saveAs(){
		System.out.println("GrxTestItem SaveAs Method!");
	}
	
	// "create"メニューで実行される関数
	public boolean create() {
		//ファイルの指定
		file_ = new File(getDefaultDir()+File.separator+getName()+"."+getFileExtention());
		/* 保存したファイルの位置の指定
		 * プロジェクトを保存したときに、この値が保存される。
		 * …パスが変わったらアウトじゃね？
		 */
		setURL(file_.getPath());

		setValue("あうあうあー");
		return true;
	}

	/* "load"メニューで実行される関数
	 * "create"のように新しいアイテムを作成するが、こちらは読み込むべきデータを指定される。
	 * この関数は通常"Item View"の"load"メニューから実行されるが、その場合GrxPluginManager#loadItem()内でsetURL()が実行される。
	 * よって、ここではsetURL()を実行する必要は無い。
	 */
	public boolean load(File f) {
		super.load(f);
		setValue("実は読んでないぜ！");
		return true;
	}

	// "rename"メニューで
	public void rename(String newName) {
		// コンファームとか出してみる
		// setName()はこの中で実行されるので実装しなくていい
		super.rename(newName);
		file_ = new File(getDefaultDir()+File.separator+getName()+"."+getFileExtention());
		// ファイル名を変更する場合、いっしょにURL変更を忘れずに
		setURL(file_.getPath());
	}

	// 値の取得に使う。GrxBaseItem#getValue()では返り値はObjectだが、ここではStringにキャストしている。
	public String getValue() {
		return (String)super.getValue();
	}
}

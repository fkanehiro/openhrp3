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
 *  GrxItemView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.util.List;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.IViewerLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerLabel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxModeInfoItem;
import com.generalrobotix.ui.util.OrderedHashMap;

@SuppressWarnings("serial")
/**
 * @brief
 */
public class GrxItemView extends GrxBaseView {

	GrxItemViewPart vp;

	TreeViewer tv;
	MenuManager menuMgr= new MenuManager();

	public GrxItemView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);

		tv = new TreeViewer(composite_);
		tv.setContentProvider( new TreeContentProvider() );
		tv.setLabelProvider( new TreeLabelProvider() );

		Tree t = tv.getTree();

		// ダブルクリックでアイテムの選択状態をトグル
		t.addListener ( SWT.DefaultSelection, new Listener () {
			public void handleEvent (Event event) {
				ISelection selection = tv.getSelection();
				Object o = ((IStructuredSelection) selection).getFirstElement();
				if( GrxBaseItem.class.isAssignableFrom( o.getClass() ) ){
					manager_.setSelectedItem( (GrxBaseItem)o, !((GrxBaseItem)o).isSelected() );
					updateTree();
				}
    		}
    	});

		// 右クリックメニュー
		t.setMenu(menuMgr.createContextMenu( t ));
		t.addListener ( SWT.MenuDetect, new Listener () {
    		public void handleEvent (Event event) {
    			ISelection selection = tv.getSelection();
    			Object o = ((IStructuredSelection) selection).getFirstElement();
                menuMgr.removeAll(); // とりあえずメニュー削除
    			// アイテムのクラス
    			if( Class.class.isAssignableFrom( o.getClass() ) ){
    				if( GrxBaseItem.class.isAssignableFrom( (Class<?>)o ) ) {
    					Vector<Action> menus = manager_.getItemMenu( (Class<? extends GrxBaseItem>) o ); 
    					for( Action a: menus){
    						menuMgr.add(a);
    					}
    				}
    			}
    			// アイテムのインスタンス
    			if( GrxBasePlugin.class.isAssignableFrom( o.getClass() ) ){
        			Vector<Action> menus = ((GrxBasePlugin)o).getMenu(); 
        			for( Action a: menus){
        				menuMgr.add(a);
        			}
    			}
    		}
    	});

		// ツリーの構築
		tv.setInput( manager_ );

        updateTree();
	}

	class TreeContentProvider implements ITreeContentProvider {

		Object[] gets( Object o ) {
			// root(PluginManager) -> プロジェクト名
			if( o instanceof GrxPluginManager ) {
				Object[] os = { ((GrxPluginManager)o).getProjectName() };
				return os;
			}

			// プロジェクト名 -> アイテムのクラスのリスト
			if( o instanceof String ) {
		        GrxModeInfoItem mode = (GrxModeInfoItem)(manager_.getItem( manager_.getCurrentModeName()));
				return mode.activeItemClassList_.toArray();
			}
			
			// アイテムのクラス -> インスタンスのリスト
			if( o instanceof Class ) {
				if( GrxBaseItem.class.isAssignableFrom( (Class<?>)o ) ) {
					OrderedHashMap oMap = manager_.pluginMap_.get( o);
					return oMap.values().toArray();
				}
			}

			// アイテムのインスタンス(の、はず)
			return null;
		}
		public Object[] getChildren(Object parentElement) { return gets(parentElement); }
		public Object getParent(Object element) { return null; }
		public boolean hasChildren(Object element) {
			Object[] os = gets(element);
			return os != null && os.length > 0;
		}
		public Object[] getElements(Object inputElement) { return gets(inputElement); }
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	class TreeLabelProvider extends LabelProvider implements IViewerLabelProvider{

		public String getText(Object object) {
			// アイテムのクラス
			if( object instanceof Class ) {
				if( GrxBaseItem.class.isAssignableFrom( (Class<?>) object ) ) {
					return manager_.getItemTitle( (Class<? extends GrxBaseItem>)object );
				}
			// アイテムのインスタンス
			}else{
				if( GrxBaseItem.class.isAssignableFrom( object.getClass() ) ) {
					return ((GrxBaseItem)object).getName(); 
				}
			}
			// Other
			return object.toString();
		}

		public Image getImage( Object o ){
			if( GrxBaseItem.class.isAssignableFrom( o.getClass() ) ) {
				if( ((GrxBaseItem)o).getIcon() == null )
					return PlatformUI.getWorkbench().getSharedImages().getImage(
							ISharedImages.IMG_OBJ_FILE);
				return ((GrxBaseItem)o).getIcon();
			}
			return PlatformUI.getWorkbench().getSharedImages().getImage(
                    ISharedImages.IMG_OBJ_FOLDER);
		}

		public void updateLabel(ViewerLabel label, Object element) {
			label.setText( getText(element) );
			label.setImage( getImage(element) );
			if( GrxBaseItem.class.isAssignableFrom( element.getClass() ) ) {
				//選択
				if( ((GrxBaseItem)element).isSelected() ) {
					label.setForeground( new Color(null,0,0,0) );
				//非選択
				} else{
					label.setForeground( new Color(null,128,128,128) );
					label.setText( "("+getText(element)+")" );
				}
			}
		}
	}
	
	public void updateTree() {
		tv.refresh();
		tv.expandAll();
	}

	public void itemSelectionChanged( List<GrxBaseItem> itemList )
	{
		updateTree();
	}

	public GrxBasePlugin getFocusedItem() {
		ISelection selection = tv.getSelection();
		if( selection==null )
			return null;
		Object o = ((IStructuredSelection) selection).getFirstElement();
		if( o!=null &&  GrxBasePlugin.class.isAssignableFrom( o.getClass() ) )
			return (GrxBasePlugin) o;
		return null;
	}
}

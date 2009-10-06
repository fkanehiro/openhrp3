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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.IViewerLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerLabel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
import com.generalrobotix.ui.item.GrxHwcItem;
import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModeInfoItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSensorItem;
import com.generalrobotix.ui.util.OrderedHashMap;

@SuppressWarnings("serial")
/**
 * @brief This view shows tree of various items
 */
public class GrxItemView extends GrxBaseView {

	GrxItemViewPart vp;

	TreeViewer tv;
	MenuManager menuMgr= new MenuManager();

	/**
	 * @brief constructor
	 * @param name name of this view
	 * @param manager PluginManager
	 * @param vp
	 * @param parent
	 */
	@SuppressWarnings("unchecked")
	public GrxItemView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) {
		super(name, manager, vp, parent);

		tv = new TreeViewer(composite_);
		tv.setContentProvider( new TreeContentProvider() );
		tv.setLabelProvider( new TreeLabelProvider() );

		Tree t = tv.getTree();
		
		// When an item is left-clicked, the item becomes "current item".
		t.addListener(SWT.Selection, new Listener() {
			public void handleEvent (Event event){
				try{
					ISelection selection = tv.getSelection();
					for (Object o : ((IStructuredSelection) selection).toArray() ){
						if ( GrxBaseItem.class.isAssignableFrom(o.getClass()) ){
							manager_.focusedItem((GrxBaseItem)o);
						}else if(o instanceof String){
							manager_.focusedItem(manager_.getProject());
						}
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		});
		
		// ダブルクリックでアイテムの選択状態をトグル
		t.addListener ( SWT.DefaultSelection, new Listener () {
			public void handleEvent (Event event) {
				ISelection selection = tv.getSelection();
				for (Object o : ((IStructuredSelection) selection).toArray() ){
					if ( GrxBaseItem.class.isAssignableFrom(o.getClass()) ){
						manager_.setSelectedItem( (GrxBaseItem)o, !((GrxBaseItem)o).isSelected() );
					}
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
                
                // project name
                if (o instanceof String){
                	Vector<Action> menus = manager_.getProject().getMenu();
   					for( Action a: menus){
						menuMgr.add(a);
					}                	
                }
                
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
        			Vector<MenuManager> subMenus = ((GrxBasePlugin)o).getSubMenu();
        			for( MenuManager m: subMenus){
        				menuMgr.add(m);
        			}
    			}
    		}
    	});

		// ツリーの構築
		tv.setInput( manager_ );

        updateTree();
        
        manager_.registerItemChangeListener(this, GrxBaseItem.class);
   
        GrxModeInfoItem mode = manager_.getMode();
        Iterator<Class<? extends GrxBaseItem>> it = mode.activeItemClassList_.listIterator();
        while (it.hasNext()){
        	Class<? extends GrxBaseItem> local = (Class<? extends GrxBaseItem>)it.next();
        	if ( manager_.isItemVisible( local ) ){
        		Map<String, ?> map = (Map<String, ?>) manager_.getItemMap(local);
        		Iterator itI = map.values().iterator();
                while(itI.hasNext())
                	((GrxBaseItem)itI.next()).addObserver(this);
        	}
        }
        manager_.getProject().addObserver(this);
	}

	/**
	 * @brief
	 */
	class TreeContentProvider implements ITreeContentProvider {

		Object[] gets( Object o ) {
			// root(PluginManager) -> プロジェクト名
			if( o instanceof GrxPluginManager ) {
				Object[] os = { ((GrxPluginManager)o).getProjectName() };
				return os;
			}

			// プロジェクト名 -> アイテムのクラスのリスト
			if( o instanceof String ) {
		        GrxModeInfoItem mode = manager_.getMode();

		        ArrayList<Class<? extends GrxBaseItem>> localList = new ArrayList<Class<? extends GrxBaseItem>>();
		        Iterator<Class<? extends GrxBaseItem>> it = mode.activeItemClassList_.listIterator();
		        //grxuirc.xmlの属性値grxui.mode.item.visibleを導入してvisibleがtrueのものをふるい分けして表示
		        while (it.hasNext()){
		        	Class<? extends GrxBaseItem> local = (Class<? extends GrxBaseItem>)it.next();
		        	if ( manager_.isItemVisible( local ) ){
			        	localList.add( local );
		        	}
		        }
				return localList.toArray();
			}

			// アイテムのクラス -> インスタンスのリスト
			if( o instanceof Class ) {
				if( GrxBaseItem.class.isAssignableFrom( (Class<?>)o ) ) {
					OrderedHashMap oMap = manager_.pluginMap_.get( o);
					return oMap.values().toArray();
				}
			}
			// GrxModelItem -> ルートのリンクを返す
			if (o instanceof GrxModelItem){
				GrxModelItem model = (GrxModelItem)o;
				if (model.rootLink() != null){
					Object[] os = {((GrxModelItem)o).rootLink()};
					return os;
				}else{
					return null;
				}
			}
			// GrxLinkItem -> 子供のリンク,センサ、形状を返す
			if (o instanceof GrxLinkItem){
				GrxLinkItem link = (GrxLinkItem)o;
				return link.children_.toArray();
			}
			
			// GrxSensorItem -> 子供の形状を返す
			if (o instanceof GrxSensorItem){
				GrxSensorItem sensor = (GrxSensorItem)o;
				return sensor.children_.toArray();
			}
			
			// GrxHwcItem -> 子供の形状を返す
			if (o instanceof GrxHwcItem){
				GrxHwcItem hwc = (GrxHwcItem)o;
				return hwc.children_.toArray();
			}
			
			// その他
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

	/**
	 * @brief
	 */
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
					label.setForeground( Display.getDefault().getSystemColor(SWT.COLOR_BLACK) );
				//非選択
				} else{
					label.setForeground( Display.getDefault().getSystemColor(SWT.COLOR_GRAY) );
					label.setText( "("+getText(element)+")" );
				}
			}
		}
	}

	/**
	 * @brief
	 */
	public void updateTree() {
		tv.refresh();
		tv.expandToLevel(3);
	}
	
	public void update(GrxBasePlugin plugin, Object... arg) {
    	if((String)arg[0]!="PropertyChange")
    		return;
    	updateTree();
    }
	
	public void registerItemChange(GrxBaseItem item, int event){
		switch(event){
	    	case GrxPluginManager.ADD_ITEM:
	    		item.addObserver(this);
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    		item.deleteObserver(this);
	    		break;
	    	default:
	    		break;
    	}
		updateTree();
		if(event==GrxPluginManager.FOCUSED_ITEM){
			List<GrxBasePlugin> l = new ArrayList<GrxBasePlugin>();
			l.add(item);
			tv.setSelection(new StructuredSelection(l), true);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void shutdown(){
		manager_.removeItemChangeListener(this, GrxBaseItem.class);
		
        GrxModeInfoItem mode = manager_.getMode();
        Iterator<Class<? extends GrxBaseItem>> it = mode.activeItemClassList_.listIterator();
        while (it.hasNext()){
        	Class<? extends GrxBaseItem> local = (Class<? extends GrxBaseItem>)it.next();
        	if ( manager_.isItemVisible( local ) ){
        		Map<String, ?> map = (Map<String, ?>) manager_.getItemMap(local);
        		Iterator itI = map.values().iterator();
                while(itI.hasNext())
                	((GrxBaseItem)itI.next()).deleteObserver(this);
        	}
        }
        manager_.getProject().deleteObserver(this);
	}
	
}

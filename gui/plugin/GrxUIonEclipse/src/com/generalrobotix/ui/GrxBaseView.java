/*
 *  GrxBaseView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.awt.Dimension;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;

public class GrxBaseView extends GrxBasePlugin {

	//	private JPanel contentPane_ = null;
//	private JComponent toolBar_ = null;
	private static Dimension defaultButtonSize_ = new Dimension(27, 27);
	public boolean isScrollable_ = true;
	
	public double min, max, now;
	int view_state_ = GRX_VIEW_SLEEP;
	static final int GRX_VIEW_SETUP   = 0;
	static final int GRX_VIEW_ACTIVE  = 1;
	static final int GRX_VIEW_CLEANUP = 2;
	static final int GRX_VIEW_SLEEP   = 3;
    
//    private static Dimension defaultButtonSize_ = new Dimension(27, 27);
    //   private JComponent toolBar_ = null;

	private GrxBaseViewPart vp_;
	private Composite parent_;
    private ScrolledComposite scrollComposite_;
    protected Composite composite_;

	public GrxBaseView( String name, GrxPluginManager manager_, GrxBaseViewPart vp, Composite parent ){
		super( name, manager_ );
		vp_ = vp;
		parent_ = parent;
        
        scrollComposite_ = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        composite_ = new Composite(scrollComposite_, SWT.NONE);
        composite_.setLayout(parent.getLayout());
        scrollComposite_.setExpandHorizontal(true);
        scrollComposite_.setExpandVertical(true);
        scrollComposite_.setContent(composite_);
	}
    

	/*
	public GrxBaseView(String name, GrxPluginManager manager) {
		super(name, manager);
		//menuPath_ = new String[]{"View"};
	}
	*/

	public void setName(String name) {
		super.setName(name);
		//getContentPane().setName(name);
	}
	
	public GrxBaseViewPart getViewPart(){
		return vp_;
	}

	public Composite getParent(){
		return parent_;
	}

	/**
	 * SWTのパーツはここで取得できるコンポジット上に設置する。
	 * @return
	 */
    public Composite getComposite(){
        return composite_;
    }
    
    public void setScrollMinSize(){
        scrollComposite_.setMinSize(composite_.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

	/*
	public final JPanel getContentPane() {
		if (contentPane_ == null) {
			contentPane_ = new JPanel();
			contentPane_.setName(getName());
		}
		return contentPane_;
	};
*/
/*
	public void setToolBar(JComponent toolBar) {
		toolBar_ = toolBar;
	};


	public JComponent getToolBar() {
		return toolBar_;
	};


	public void setToolBarVisible(boolean visible) {
		if (toolBar_ != null)
			toolBar_.isVisible();
	}


	public boolean isToolBarVisible() {
		return toolBar_ != null || toolBar_.isVisible();
	}
*/

	public static Dimension getDefaultButtonSize() {
		return defaultButtonSize_;
	}

	public void start() {
		if (view_state_ == GRX_VIEW_SLEEP)
			view_state_ = GRX_VIEW_SETUP;
	}

	public void stop() {
		if (view_state_ == GRX_VIEW_ACTIVE)
			view_state_ = GRX_VIEW_CLEANUP;
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList){}
	public boolean setup(List<GrxBaseItem> itemList){return true;}
	public void control(List<GrxBaseItem> itemList){}
	public boolean cleanup(List<GrxBaseItem> itemList){return true;}

	public boolean isRunning() {
		return (view_state_ == GRX_VIEW_ACTIVE);
	}

	public boolean isSleeping() {
		return (view_state_ == GRX_VIEW_SLEEP);
	}
    
}

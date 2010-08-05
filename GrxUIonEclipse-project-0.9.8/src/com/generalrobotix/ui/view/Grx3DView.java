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
 *  Grx3DView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.eclipse.jface.dialogs.MessageDialog;

//import com.sun.j3d.utils.pickfast.PickCanvas;
import com.sun.j3d.utils.universe.SimpleUniverse;

import jp.go.aist.hrp.simulator.*;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.util.AxisAngle4d;
import com.generalrobotix.ui.GrxBasePlugin.ValueEditCombo;
import com.generalrobotix.ui.GrxBasePlugin.ValueEditType;
import com.generalrobotix.ui.item.GrxCollisionPairItem;
import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSensorItem;
import com.generalrobotix.ui.item.GrxSimulationItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxWorldStateItem.CharacterStateEx;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.view.tdview.*;
import com.generalrobotix.ui.view.vsensor.Camera_impl;

@SuppressWarnings("serial") //$NON-NLS-1$
public class Grx3DView 
    extends GrxBaseView 
    implements ThreeDDrawable 
{
    public static final String TITLE = "3DView"; //$NON-NLS-1$
    //  java3D 1.4.1のバグ？　対策		
    public static final GraphicsConfiguration graphicsConfiguration = SimpleUniverse.getPreferredConfiguration();
    // items
    private GrxWorldStateItem  currentWorld_ = null;
    private List<GrxModelItem> currentModels_ = new ArrayList<GrxModelItem>();
    private List<GrxCollisionPairItem> currentCollisionPairs_ = new ArrayList<GrxCollisionPairItem>();
    private WorldStateEx currentState_ = null; 
    private GrxSimulationItem simItem_ = null;
    private final static int VIEW=0;
    private final static int EDIT=1;
    private final static int SIMULATION = 2;
    private int viewMode_ = VIEW;
    private double dAngle_ = Math.toRadians(0.1);
 
    // for scene graph
    private static VirtualUniverse universe_;
    private javax.media.j3d.Locale locale_;
    private BranchGroup  bgRoot_;
    private BranchGroup  unclickableBgRoot_;
    
    private BranchGroup rulerBg_;
    private float LineWidth_ = 1.0f;
    private float colprop = 10.0f;
    private float coldiff = 0.1f;
    
    // for view
    private Canvas3D  canvas_;
    private Canvas3D offscreen_;
    private View view_;
    private TransformGroup tgView_;
    private Transform3D t3dViewHome_ = new Transform3D();
    private ViewInfo info_;
    private BehaviorManager behaviorManager_ = new BehaviorManager(manager_);
    private Background backGround_ = new Background(0.0f, 0.0f, 0.0f);
    private double[] default_eye =    new double[]{2.0, 2.0, 0.8};
    private double[] default_lookat = new double[]{0.0, 0.0, 0.8};
    private double[] default_upward = new double[]{0.0, 0.0, 1.0};
    
    // for recording movie
    private RecordingManager recordingMgr_;
    private String lastMovieFileName;
    
    // UI objects
    private ObjectToolBar objectToolBar_ = new ObjectToolBar();
    private ViewToolBar viewToolBar_ = new ViewToolBar(this);
    private JButton btnHomePos_ = new JButton(new ImageIcon(getClass().getResource("/resources/images/home.png"))); //$NON-NLS-1$
    private JToggleButton btnFloor_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/floor.png"))); //$NON-NLS-1$
    private JToggleButton btnCollision_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/collision.png"))); //$NON-NLS-1$
    private JToggleButton btnDistance_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/distance.png"))); //$NON-NLS-1$
    private JToggleButton btnIntersection_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/proximity.png"))); //$NON-NLS-1$
    private JToggleButton btnCoM_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/com.png"))); //$NON-NLS-1$
    private JToggleButton btnCoMonFloor_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/com_z0.png"))); //$NON-NLS-1$
    private JToggleButton btnRec_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/record.png"))); //$NON-NLS-1$
    private JButton btnPlayer_ = new JButton(new ImageIcon(getClass().getResource("/resources/images/movie_player.png"))); //$NON-NLS-1$
    private JButton btnRestore_ = new JButton(new ImageIcon(getClass().getResource("/resources/images/undo.png"))); //$NON-NLS-1$
    private JToggleButton btnBBdisp_ = new JToggleButton(new ImageIcon(getClass().getResource("/resources/images/AABB.png")));
    		
    private JLabel lblMode_ = new JLabel(MessageBundle.get("Grx3DView.label.view")); //$NON-NLS-1$
    private JLabel lblTarget_ = new JLabel(""); //$NON-NLS-1$
    private JLabel lblValue_  = new JLabel(""); //$NON-NLS-1$
    
    private Shape3D collision_;
    private Shape3D distance_;
    private Vector<GrxLinkItem> intersectingLinks_;
    
    private boolean showActualState_ = true;
    
    // for "Linux resize problem"
    Frame frame_;
    Composite comp;
    
    public Grx3DView(String name, GrxPluginManager manager, GrxBaseViewPart vp, Composite parent) 
    {
        super(name, manager, vp, parent);
        isScrollable_ = false;
        
        //----
        // Linuxでリサイズイベントが発行されない問題対策
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=168330

        comp = new Composite( getComposite(), SWT.EMBEDDED);
        frame_ = SWT_AWT.new_Frame( comp );

        comp.addControlListener( new ControlListener() {
            public void controlMoved(ControlEvent e) {}
            public void controlResized(ControlEvent e) {
                frame_.setBounds(0, 0, comp.getSize().x, comp.getSize().y );
            }
        });
        
        //----
        
        //----
        // JCombo等がマウスで開けない問題対策
        // http://www.eclipsezone.com/forums/thread.jspa?messageID=92230432&
        // JPanelでなくAWTのPanelを使う
        Panel contentPane = new Panel();
        
        //----
        
        frame_.add(contentPane);

        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.lightGray);
        //contentPane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        
        lblMode_.setForeground(Color.black);
        lblMode_.setFont(new Font("Monospaced", Font.BOLD, 12)); //$NON-NLS-1$
        lblMode_.setPreferredSize(new Dimension(300, 20));

        lblTarget_.setForeground(Color.white);
        lblTarget_.setFont(new Font("Monospaced", Font.BOLD, 12)); //$NON-NLS-1$
        lblTarget_.setPreferredSize(new Dimension(500, 20));

        lblValue_.setForeground(Color.white);
        lblValue_.setFont(new Font("Monospaced", Font.BOLD, 12)); //$NON-NLS-1$
        lblValue_.setPreferredSize(new Dimension(300, 20));
        
        JPanel southPanel = new JPanel();
		southPanel.setLayout(new GridLayout(1,0));
		southPanel.add( lblMode_ );
		Box clipPanel = Box.createHorizontalBox();
		JLabel clipDistLabel0 = new JLabel(MessageBundle.get("Grx3DView.label.clipDistance0"));
		JLabel clipDistLabel1 = new JLabel(MessageBundle.get("Grx3DView.label.clipDistance1"));
		final TextField frontText = new TextField(6);
		final TextField backText = new TextField(6);
		frontText.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				String str = frontText.getText();
				try{
					info_.frontClipDistance=Double.parseDouble(str);
					view_.setFrontClipDistance(info_.frontClipDistance);
				}catch(NumberFormatException e){
					frontText.setText(String.valueOf(info_.frontClipDistance));
				}	
			}
		});
		backText.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				String str = backText.getText();
				try{
					info_.backClipDistance=Double.parseDouble(str);
					view_.setBackClipDistance(info_.backClipDistance);
				}catch(NumberFormatException e){
					backText.setText(String.valueOf(info_.backClipDistance));
				}
			}
		});
		
		clipPanel.add(clipDistLabel0);
		clipPanel.add(frontText);
		clipPanel.add(clipDistLabel1);
		clipPanel.add(backText);
		southPanel.add(clipPanel);
		
        canvas_ = new Canvas3D(graphicsConfiguration);
        canvas_.setDoubleBufferEnable(true);
        canvas_.addKeyListener(new ModelEditKeyAdapter());  
        _setupSceneGraph();

        frontText.setText(String.valueOf(info_.frontClipDistance));
		backText.setText(String.valueOf(info_.backClipDistance));
        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.setBackground(Color.black);
        contentPane.add(southPanel, BorderLayout.SOUTH);
        mainPane.add(canvas_, BorderLayout.CENTER);
        contentPane.add(mainPane, BorderLayout.CENTER);
        
        _setupToolBars();
        contentPane.add(objectToolBar_, BorderLayout.WEST);
        contentPane.add(viewToolBar_, BorderLayout.NORTH);
        
        collision_ = new Shape3D();
        collision_.setPickable(false);
        collision_.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        try {
            Appearance app = new Appearance();
            LineAttributes latt = new LineAttributes();
            latt.setLineWidth(LineWidth_);
            app.setLineAttributes(latt);
            collision_.setAppearance(app);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        BranchGroup bg = new BranchGroup();
        bg.addChild(collision_);
        bgRoot_.addChild(bg);
        
        distance_ = new Shape3D();
        distance_.setPickable(false);
        distance_.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        try {
            Appearance app = new Appearance();
            LineAttributes latt = new LineAttributes();
            latt.setLineWidth(LineWidth_);
            app.setLineAttributes(latt);
            distance_.setAppearance(app);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        BranchGroup bgDistance = new BranchGroup();
        bgDistance.addChild(distance_);
        bgRoot_.addChild(bgDistance);
        
        intersectingLinks_ = new Vector<GrxLinkItem>();
        
        setScrollMinSize(SWT.DEFAULT,SWT.DEFAULT);
    
        // View が開いたときモデルとWorldStateを取得　変化があればマネジャーに教えてもらう
        currentModels_ = manager_.<GrxModelItem>getSelectedItemList(GrxModelItem.class);
        Iterator<GrxModelItem> it = currentModels_.iterator();
        while(it.hasNext())	{
        	GrxModelItem model = it.next();
        	bgRoot_.addChild(model.bgRoot_);
        	model.addObserver(this);
        }
        updateViewSimulator(0);
        manager_.registerItemChangeListener(this, GrxModelItem.class);
        currentWorld_ = manager_.<GrxWorldStateItem>getSelectedItem(GrxWorldStateItem.class, null);
        if(currentWorld_!=null){
        	currentState_ = currentWorld_.getValue();
        	if (currentState_!=null){
        		updateModels(currentState_);
        		updateViewSimulator(currentState_.time);
            }           
        	currentWorld_.addObserver(this);
        	currentWorld_.addPosObserver(this);
        }
        manager_.registerItemChangeListener(this, GrxWorldStateItem.class);
        currentCollisionPairs_ = manager_.<GrxCollisionPairItem>getSelectedItemList(GrxCollisionPairItem.class);
        manager_.registerItemChangeListener(this, GrxCollisionPairItem.class);
        
        //setup
        behaviorManager_.setThreeDViewer(this);
        behaviorManager_.setViewIndicator(viewToolBar_);
        behaviorManager_.setItem(currentModels_, currentCollisionPairs_);
        behaviorManager_.initDynamicsSimulator();
        behaviorManager_.setOperationMode(BehaviorManager.OPERATION_MODE_NONE);
        behaviorManager_.setViewMode(BehaviorManager.ROOM_VIEW_MODE);
        behaviorManager_.setViewHandlerMode("button_mode_rotation"); //$NON-NLS-1$
        behaviorManager_.replaceWorld(null);
        viewToolBar_.setMode(ViewToolBar.ROOM_MODE);
        viewToolBar_.setOperation(ViewToolBar.ROTATE);
        
        registerCORBA();
        
        simItem_ = manager_.<GrxSimulationItem>getSelectedItem(GrxSimulationItem.class, null);
		if(simItem_!=null){
			simItem_.addObserver(this);
			if(simItem_.isSimulating())
				viewMode_ = SIMULATION;
		}
		manager_.registerItemChangeListener(this, GrxSimulationItem.class);
		
        if(viewMode_==SIMULATION){
        	disableButton();
			objectToolBar_.setMode(ObjectToolBar.DISABLE_MODE);
        }        
    }

    private void _setupSceneGraph() {
        universe_ = new VirtualUniverse();
        locale_ = new javax.media.j3d.Locale(universe_);
        bgRoot_ = new BranchGroup();
        
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        bgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
                
        //locale_.addBranchGraph(_createView());
        bgRoot_.addChild(_createLights());
        bgRoot_.addChild(_createView());
        bgRoot_.compile();
        locale_.addBranchGraph(bgRoot_);
        
        unclickableBgRoot_ = new BranchGroup();
        unclickableBgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        unclickableBgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        unclickableBgRoot_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

        locale_.addBranchGraph( unclickableBgRoot_ );
    }
    
    private BranchGroup _createView() {
        BranchGroup bg = new BranchGroup();
        ViewPlatform platform = new ViewPlatform();
        info_ = new ViewInfo(ViewInfo.VIEW_MODE_ROOM | ViewInfo.FRONT_VIEW, 3.0 );
        view_ = new View();
        view_.setScreenScalePolicy(View.SCALE_EXPLICIT);
        view_.setScreenScale(0.1);
        tgView_ = new TransformGroup();
        
        view_.setPhysicalBody(new PhysicalBody());
        view_.setPhysicalEnvironment(new PhysicalEnvironment());
        view_.setFrontClipPolicy(View.VIRTUAL_EYE);
        view_.setBackClipPolicy(View.VIRTUAL_EYE);
        view_.setFrontClipDistance(info_.frontClipDistance);
        view_.setBackClipDistance(info_.backClipDistance);
        view_.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
        view_.setFieldOfView(Math.PI/4);
        view_.addCanvas3D(canvas_);
        
        tgView_.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        tgView_.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tgView_.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tgView_.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tgView_.setCapability(TransformGroup.ALLOW_LOCAL_TO_VWORLD_READ);
        
        view_.attachViewPlatform(platform);
        tgView_.addChild(platform);
        bg.addChild(tgView_);        
        
        _setViewHomePosition();
        
        return bg;
    }
    
    private BranchGroup _createLights() {
        BranchGroup bg = new BranchGroup();
        DirectionalLight[] light = new DirectionalLight[4];
        TransformGroup[] tg = new TransformGroup[4];
        BoundingSphere bounds =
        new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
       
        //DirectionalLight dlight = new DirectionalLight(new Color3f(1.0f, 1.0f, 1.0f), new Vector3f(-0.8f, -1.2f, -1.5f));
        //dlight.setInfluencingBounds(bounds);
        //tgView_.addChild(dlight);

        light[0] = new DirectionalLight(true,   // lightOn
                new Color3f(0.7f, 0.7f, 0.7f),  // color
                new Vector3f(0.0f, 0.0f, -1.0f) // direction
        );
        
        light[1] = new DirectionalLight(true,   // lightOn
                new Color3f(0.4f, 0.4f, 0.4f),  // color
                new Vector3f(0.0f, 0.0f, -1.0f) // direction
        );
        
        light[2] = new DirectionalLight(true,   // lightOn
                new Color3f(0.7f, 0.7f, 0.7f),  // color
                new Vector3f(0.0f, 0.0f, -1.0f) // direction
        );
        
        light[3] = new DirectionalLight(true,   // lightOn
                new Color3f(0.4f, 0.4f, 0.4f),  // color
                new Vector3f(0.0f, 0.0f, -1.0f) // direction
        );
        
        for (int i = 0; i < 4; i ++) {
            light[i].setInfluencingBounds(bounds);
            tg[i] = new TransformGroup();
            bg.addChild(tg[i]);
            tg[i].addChild(light[i]);
        }

        Transform3D transform = new Transform3D();
        Vector3d pos = new Vector3d();
        AxisAngle4d rot = new AxisAngle4d();
        
        pos.set(10.0, 10.0, 5.0);
        transform.set(pos);
        rot.set(-0.5, 0.5, 0.0, 1.2);
        transform.set(rot);
        tg[0].setTransform(transform);
        
        pos.set(10.0, -10.0, -5.0);
        transform.set(pos);
        rot.set(0.5, 0.5, 0.0, 3.14 - 1.2);
        transform.set(rot);
        tg[1].setTransform(transform);
        
        pos.set(-10.0, -10.0, 5.0);
        transform.set(pos);
        rot.set(0.5, -0.5, 0.0, 1.2);
        transform.set(rot);
        tg[2].setTransform(transform);
        
        pos.set(-10.0, 10.0, -5.0);
        transform.set(pos);
        rot.set(-0.5, -0.5, 0.0, 3.14 - 1.2);
        transform.set(rot);
        tg[3].setTransform(transform);

        // Ambient Light for Alert
        AmbientLight alight = new AmbientLight(new Color3f(1.0f, 1.0f, 1.0f));
        alight.setInfluencingBounds(bounds);
        tg[0].addChild(alight);

        // background
        backGround_.setCapability(Background.ALLOW_COLOR_READ);
        backGround_.setCapability(Background.ALLOW_COLOR_WRITE);
        backGround_.setApplicationBounds(bounds);
        bg.addChild(backGround_);
        
        return bg;
    }

    private void _setupToolBars() {
        btnHomePos_.setToolTipText(MessageBundle.get("Grx3DView.text.goHomeEyePos")); //$NON-NLS-1$
        btnHomePos_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                tgView_.setTransform(t3dViewHome_);
            }
        });
        btnHomePos_.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent arg0) {			
			}
			public void mouseEntered(MouseEvent arg0) {
			}
			public void mouseExited(MouseEvent arg0) {
			}
			public void mousePressed(MouseEvent arg0) {
				if(arg0.getButton()==MouseEvent.BUTTON2 || arg0.getButton()==MouseEvent.BUTTON3){
					PopupMenu popupMenu = new PopupMenu(); 
					final MenuItem menu0 = new MenuItem(MessageBundle.get("Grx3DView.popupmenu.setHomeEyePos"));
					menu0.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent arg0) {
							if(arg0.getSource()==menu0){
								tgView_.getTransform(t3dViewHome_);
								final double[] eyeHomePosition = new double[16];
								t3dViewHome_.get(eyeHomePosition);
								syncExec(new Runnable(){
			                    	public void run(){
			                    		setDblAry("eyeHomePosition", eyeHomePosition, 5); //$NON-NLS-1$ //$NON-NLS-2$
			                    	}
			                    });
							}
						}
					});
					final MenuItem menu1 = new MenuItem(MessageBundle.get("Grx3DView.popupmenu.restoreDefault"));
					menu1.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent arg0) {
							if(arg0.getSource()==menu1){
								_setViewHomePosition();
								final double[] eyeHomePosition = new double[16];
								t3dViewHome_.get(eyeHomePosition);
								syncExec(new Runnable(){
			                    	public void run(){
			                    		setDblAry("eyeHomePosition", eyeHomePosition, 5); //$NON-NLS-1$ //$NON-NLS-2$
			                    	}
			                    });
							}
						}
					});
					popupMenu.add(menu0);
					popupMenu.add(menu1);
					btnHomePos_.add(popupMenu);
					popupMenu.show(btnHomePos_, arg0.getX(), arg0.getY());
				}
			}
			public void mouseReleased(MouseEvent arg0) {
			}	
        });
        
        btnFloor_.setToolTipText(MessageBundle.get("Grx3DView.text,showZPlane")); //$NON-NLS-1$
        btnFloor_.setSelected(false);
        btnFloor_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (btnFloor_.isSelected()) {
                    btnFloor_.setToolTipText(MessageBundle.get("Grx3DView.text.hideZPlane")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showScale", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                    	}
                    });
                    if (bgRoot_.indexOfChild(getRuler()) == -1) 
                        bgRoot_.addChild(getRuler());
                } else {
                    btnFloor_.setToolTipText(MessageBundle.get("Grx3DView.text.showZPlane")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showScale", "false"); //$NON-NLS-1$ //$NON-NLS-2$
                    	}
                    });
                    if (bgRoot_.indexOfChild(getRuler()) != -1)
                        getRuler().detach();
                }
            }
        });
        
        btnCollision_.setToolTipText(MessageBundle.get("Grx3DView.text.showCollision")); //$NON-NLS-1$
        btnCollision_.setSelected(false);
        btnCollision_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (btnCollision_.isSelected()){
                    btnCollision_.setToolTipText(MessageBundle.get("Grx3DView.text.hideCollision")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showCollision", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                    	}
                    });
                    if (viewMode_ == SIMULATION || ( viewMode_ == VIEW && currentState_ != null))
                    	_showCollision(currentState_.collisions);
                    else
                    	_showCollision(behaviorManager_.getCollision());
                }else{
                    btnCollision_.setToolTipText(MessageBundle.get("Grx3DView.text.showCollision")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showCollision", "false"); //$NON-NLS-1$ //$NON-NLS-2$
                    	}
                    });
                    _showCollision(null);
                }
            }
        });

        btnDistance_.setToolTipText(MessageBundle.get("Grx3DView.text.showDistance")); //$NON-NLS-1$
        btnDistance_.setSelected(false);
        btnDistance_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (btnDistance_.isSelected()){
                    btnDistance_.setToolTipText(MessageBundle.get("Grx3DView.text.hideDistance")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showDistance", "true");
                    	}
                    });
                    if (viewMode_ != SIMULATION)
                    	_showDistance(behaviorManager_.getDistance());
                }else {
                    btnDistance_.setToolTipText(MessageBundle.get("Grx3DView.text.showDistance")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showDistance", "false");
                    	}
                    });
                    _showDistance(null);
                }
            }
        });
         
        btnIntersection_.setToolTipText(MessageBundle.get("Grx3DView.text.checkIntersection")); //$NON-NLS-1$
        btnIntersection_.setSelected(false);
         btnIntersection_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (btnIntersection_.isSelected()){
                    btnIntersection_.setToolTipText(MessageBundle.get("Grx3DView.text.nocheckIntersection")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showIntersection", "true");
                    	}
                    });
                    if (viewMode_ != SIMULATION)
                    	_showIntersection(behaviorManager_.getIntersection());
                }else{
                    btnIntersection_.setToolTipText(MessageBundle.get("Grx3DView.text.checkIntersection")); //$NON-NLS-1$
                    syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showIntersection", "false");
                    	}
                    });
                    _showIntersection(null);
                }
            }
        });
        
        btnCoM_.setToolTipText(MessageBundle.get("Grx3DView.text.showCom")); //$NON-NLS-1$
        btnCoM_.setSelected(false);
        btnCoM_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                boolean b = btnCoM_.isSelected();
                for (int i=0; i<currentModels_.size(); i++)
                    currentModels_.get(i).setVisibleCoM(b);
                if(b)
                	syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showCoM", "true");
                    	}
                    });
                else
                	syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showCoM", "false");
                    	}
                    });
            };
        });
        
        btnCoMonFloor_.setToolTipText(MessageBundle.get("Grx3DView.text.showcomFloor")); //$NON-NLS-1$
        btnCoMonFloor_.setSelected(false);
        btnCoMonFloor_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                boolean b = btnCoMonFloor_.isSelected();
                for (int i=0; i<currentModels_.size(); i++)
                    currentModels_.get(i).setVisibleCoMonFloor(b);
                if(b)
                	syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showCoMonFloor", "true");
                    	}
                    });
                else
                	syncExec(new Runnable(){
                    	public void run(){
                    		setProperty("showCoMonFloor", "false");
                    	}
                    }); 
            };
        });
        
        btnRec_.setToolTipText(MessageBundle.get("Grx3DView.text.record")); //$NON-NLS-1$
        btnRec_.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) {    
                if (btnRec_.isSelected()) {
                    if (currentWorld_ != null && currentWorld_.getLogSize() > 0)
                        rec();
                    else 
                        btnRec_.setSelected(false);
                } else  {
                	btnRec_.setSelected(false);
                }
            }
        });
        
        btnPlayer_.setToolTipText(MessageBundle.get("Grx3DView.text.moviePlayer")); //$NON-NLS-1$
        btnPlayer_.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) {
                Display.getDefault().syncExec( new Runnable(){
                    public void run(){
                        new SWTMoviePlayer( getParent().getShell(), lastMovieFileName );
                    }
                });
                
            }
        });
        

        final JButton btnCamera = new JButton("C"); //$NON-NLS-1$
        btnCamera.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                for (int i=0; i<currentModels_.size(); i++) {
                    List<Camera_impl> l = currentModels_.get(i).getCameraSequence();
                    for (int j=0; j<l.size(); j++) {
                        Camera_impl c = l.get(j);
                        c.setVisible(!c.isVisible());
                    }
                }
            }
        });
        
        btnBBdisp_.addActionListener(new ActionListener() { 
            public void actionPerformed(ActionEvent e) {
            	boolean b = btnBBdisp_.isSelected();
            	if(b){
	                List<GrxModelItem> visibleModels = setNumOfAABB();
	                Iterator<GrxModelItem> it = visibleModels.iterator();
	                while(it.hasNext())
	                	it.next().setVisibleAABB(b);
	                if(visibleModels.isEmpty())
	                	btnBBdisp_.setSelected(false);
            	}else
            		for (int i=0; i<currentModels_.size(); i++)
	                    	currentModels_.get(i).setVisibleAABB(b);
            }
        });
        
        viewToolBar_.add(btnHomePos_,0);
        viewToolBar_.add(btnFloor_, 8);
        viewToolBar_.add(btnCollision_, 9);
        viewToolBar_.add(btnDistance_, 10);
        viewToolBar_.add(btnIntersection_, 11);
        viewToolBar_.add(btnCoM_, 12);
        viewToolBar_.add(btnCoMonFloor_, 13);
        viewToolBar_.add(btnRec_);
        viewToolBar_.add(btnPlayer_);
        viewToolBar_.add(btnCamera);
        viewToolBar_.add(btnBBdisp_);

        btnRestore_.setToolTipText(MessageBundle.get("Grx3DView.text.restoreModel")); //$NON-NLS-1$
        btnRestore_.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i=0; i<currentModels_.size(); i++) {
                    final GrxModelItem item = currentModels_.get(i);
                    syncExec(new Runnable(){
                    	public void run(){
                    		item.restoreProperties();
                    	}
                    });
                    
                }
                showOption();
            }
        });
        
        objectToolBar_.add(btnRestore_, 0);
        objectToolBar_.setOrientation(JToolBar.VERTICAL);
        
        JToolBar bars[] = new JToolBar[]{viewToolBar_, objectToolBar_};
        for (int i=0; i<bars.length; i++) {
            JToolBar bar = bars[i];
            bar.setFloatable(false);
            for (int j=0; j<bar.getComponentCount(); j++) {
                Component c = bar.getComponent(j);
                if (c instanceof AbstractButton) {    
                    AbstractButton b = (AbstractButton)c;
                    b.setPreferredSize(GrxBaseView.getDefaultButtonSize());
                    b.setMaximumSize(GrxBaseView.getDefaultButtonSize());
                }
            }    
        }
        
        _registerAction();
    }
    
    public Canvas3D getCanvas3D() {
        return canvas_;
    }
    
    public BranchGroup getBranchGroupRoot() {
        return bgRoot_;
    }
        
    private void _setViewHomePosition() {
        t3dViewHome_.lookAt(
            new Point3d(default_eye), 
            new Point3d(default_lookat), 
            new Vector3d(default_upward));
        t3dViewHome_.invert();
        tgView_.setTransform(t3dViewHome_);
    }
    
    public void restoreProperties(){
    	super.restoreProperties();
    	if(getStr("showScale")==null) propertyChanged("showScale", "true"); 
    	if(getStr("showCollision")==null) propertyChanged("showCollision", "true");
    	if(getStr("showDistance")==null) propertyChanged("showDistance", "false");
    	if(getStr("showIntersection")==null) propertyChanged("showIntersection", "false");
    	if(getStr("showCoM")==null) propertyChanged("showCoM", "false");
    	if(getStr("showCoMonFloor")==null) propertyChanged("showCoMonFloor", "false");
    	if(getStr("view.mode")==null) propertyChanged("view.mode", ViewToolBar.COMBO_SELECT_ROOM);
        if(getStr("showActualState")==null) propertyChanged("showActualState", "true");   
        if(getStr("eyeHomePosition")==null){
        	final double[] eyeHomePosition = new double[16];
        	_setViewHomePosition();
        	t3dViewHome_.get(eyeHomePosition);
        	syncExec(new Runnable(){
            	public void run(){
            		setDblAry("eyeHomePosition", eyeHomePosition, 5); //$NON-NLS-1$ //$NON-NLS-2$
            	}
            });
        	propertyChanged("eyeHomePosiotion", getProperty("eyeHomePosition"));
        }
    }
    
    public void registerItemChange(GrxBaseItem item, int event){
    	if(item instanceof GrxModelItem){
    		GrxModelItem modelItem = (GrxModelItem) item;
	    	switch(event){
	    	case GrxPluginManager.SELECTED_ITEM:
	    		if(!modelItem.bgRoot_.isLive()){
	    			modelItem.setWireFrame(viewToolBar_.isWireFrameSelected());
	                bgRoot_.addChild(modelItem.bgRoot_);
	        		currentModels_.add(modelItem);
	        		behaviorManager_.setItem(currentModels_, currentCollisionPairs_);
	        		if(viewMode_ == VIEW && currentState_!=null){
	        			updateModels(currentState_);
	        			updateViewSimulator(currentState_.time);
	        		}else
	        			updateViewSimulator(0);
	        		if(modelItem.isModified())
	        			optionButtonEnable(false);
	        		showOption();
	        		if(btnBBdisp_.isSelected()){
	        			btnBBdisp_.doClick();
	        		}
	        		modelItem.addObserver(this);
	    		}
	    		break;
	    	case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(modelItem.bgRoot_.isLive()){
	    			modelItem.bgRoot_.detach();
	        		currentModels_.remove(modelItem);
	        		behaviorManager_.setItem(currentModels_, currentCollisionPairs_);
	        		if(modelItem.isModified())
	        			optionButtonEnable(true);
	        		showOption();
	        		modelItem.deleteObserver(this);
	    		}
	    		//if(currentModels_.size()==0)
	    		//	_showCollision(null);
	    		break;
	    	default:
	    		break;
	    	}
    	}else if(item instanceof GrxWorldStateItem){
    		GrxWorldStateItem worldStateItem = (GrxWorldStateItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(currentWorld_!=worldStateItem){
    				currentWorld_ = worldStateItem;
    				currentWorld_.addObserver(this);
                    currentWorld_.addPosObserver(this);
    				currentState_ = currentWorld_.getValue();
    				updatePosition(currentWorld_, currentWorld_.getPosition());
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(currentWorld_==worldStateItem){
	    			currentWorld_.deleteObserver(this);
                    currentWorld_.deletePosObserver(this);
	    			currentWorld_ = null;
	    			currentState_ = null;
	    		}
	    		break;
	    	default:
	    		break;
    		}
    	}else if(item instanceof GrxCollisionPairItem){
    		GrxCollisionPairItem collisionPairItem = (GrxCollisionPairItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(!currentCollisionPairs_.contains(collisionPairItem)){
    				currentCollisionPairs_.add(collisionPairItem);
    				behaviorManager_.setItem(currentModels_, currentCollisionPairs_);
    	    		showOption();
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
    		case GrxPluginManager.NOTSELECTED_ITEM:
    			if(currentCollisionPairs_.contains(collisionPairItem)){
    				currentCollisionPairs_.remove(collisionPairItem);
    				behaviorManager_.setItem(currentModels_, currentCollisionPairs_);
    	    		showOption();
    			}
    			break;
    		}
    	}else if(item instanceof GrxSimulationItem){
    		GrxSimulationItem simItem = (GrxSimulationItem) item;
    		switch(event){
    		case GrxPluginManager.SELECTED_ITEM:
    			if(simItem_!=simItem){
    				simItem_ = simItem;
    				simItem_.addObserver(this);
    			}
    			break;
    		case GrxPluginManager.REMOVE_ITEM:
	    	case GrxPluginManager.NOTSELECTED_ITEM:
	    		if(simItem_==simItem){
	    			simItem_.deleteObserver(this);
	    			simItem_ = null;
	    		}
	    		break;
	    	default:
	    		break;
    		}
    	}
    }
    
    public void update(GrxBasePlugin plugin, Object... arg) {
    	if(simItem_==plugin){
	    	if((String)arg[0]=="StartSimulation"){ //$NON-NLS-1$
				disableButton();
				objectToolBar_.setMode(ObjectToolBar.DISABLE_MODE);
				if((Boolean)arg[1])
					showViewSimulator(true);
				viewMode_ = SIMULATION;
			}else if((String)arg[0]=="StopSimulation"){ //$NON-NLS-1$
				objectToolBar_.setMode(ObjectToolBar.OBJECT_MODE);
				enableButton();
				viewMode_ = VIEW;
			}
    	}else if(currentModels_.contains(plugin)){
    		if((String)arg[0]=="PropertyChange"){
    			if((String)arg[1]=="name" ){
    				behaviorManager_.setItemChange();
    				showOption();
    			}
    			if(((String)arg[1]).contains("translation") || ((String)arg[1]).contains("rotation") || 
    					((String)arg[1]).contains("angle"))
    				showOption();
    		}
    		else if((String)arg[0]=="BodyInfoChange"){
    			behaviorManager_.setItemChange();
    			showOption();
    		}else if((String)arg[0]=="Modified"){
    			optionButtonEnable(false);
    		}else if((String)arg[0]=="ClearModified"){
    			optionButtonEnable(true);
    		}
    	}else if(currentWorld_==plugin){
            if((String)arg[0]=="ClearLog"){ //$NON-NLS-1$
				currentState_ = null;
			}
    	}
    }
    
    public void updatePosition(GrxBasePlugin plugin, Integer arg_pos){
        if(currentWorld_ != plugin)
            return;

        if(viewMode_ == VIEW || viewMode_ == SIMULATION){
            int pos = arg_pos.intValue();
            currentState_ = currentWorld_.getValue(pos);
            if(currentState_!=null){
                _showCollision(currentState_.collisions);
                updateModels(currentState_);
                updateViewSimulator(currentState_.time);
            }
            if(viewMode_ == VIEW)
                showOptionWithoutCollision();
        }
    }

    private void disableButton(){
    	disableOperation();
    	if (btnDistance_.isSelected())
			btnDistance_.doClick();
		if (btnIntersection_.isSelected())
			btnIntersection_.doClick();
		btnDistance_.setEnabled(false);
		btnIntersection_.setEnabled(false);
		btnRestore_.setEnabled(false);
		btnRec_.setEnabled(false);
		btnPlayer_.setEnabled(false);
    }
    
    private void enableButton(){
    	btnDistance_.setEnabled(true);
		btnIntersection_.setEnabled(true);
		btnRestore_.setEnabled(true);
		btnRec_.setEnabled(true);
		btnPlayer_.setEnabled(true);
    }
    
    public void showOption(){
    	if(viewMode_==SIMULATION) return;
    	if (btnCollision_.isSelected()) {
    		_showCollision(behaviorManager_.getCollision());
    		behaviorManager_.setMessageSkip(true);
    	}
    	if (btnDistance_.isSelected()){
    		_showDistance(behaviorManager_.getDistance());
    		behaviorManager_.setMessageSkip(true);
    	}
    	if (btnIntersection_.isSelected()){
    		_showIntersection(behaviorManager_.getIntersection());
    	}
    	behaviorManager_.setMessageSkip(false);
    }
	
    private void showOptionWithoutCollision(){
    	if(viewMode_==SIMULATION) return;
    	if (btnDistance_.isSelected()){
    		_showDistance(behaviorManager_.getDistance());
    		behaviorManager_.setMessageSkip(true);
    	}
    	if (btnIntersection_.isSelected()){
    		_showIntersection(behaviorManager_.getIntersection());
    	}
    	behaviorManager_.setMessageSkip(false);
    }
    
	public void showViewSimulator(boolean b) {
        for (int i=0; i<currentModels_.size(); i++) {
            List<Camera_impl> l = currentModels_.get(i).getCameraSequence();
            for (int j=0; j<l.size(); j++) {
                Camera_impl c = l.get(j);
                c.setVisible(b);
            }
        }
    }
	
    public void updateModels(WorldStateEx state){
        // update models with new WorldState
        for (int i=0; i<currentModels_.size(); i++) {
            GrxModelItem model = currentModels_.get(i);
            CharacterStateEx charStat = state.get(model.getName());
            if (charStat != null) {
                if (charStat.sensorState != null){
                	double[] angles;
                	if (showActualState_) {
                		angles = charStat.sensorState.q;
                	} else {
                		angles = charStat.targetState;
                	}
                    model.setCharacterPos(charStat.position, angles);
                    if (charStat.sensorState.range != null && charStat.sensorState.range.length > 0){
                    	List<GrxSensorItem> sensors = model.getSensors("Range"); //$NON-NLS-1$
                    	for (int j=0; j<sensors.size(); j++){
                    		GrxSensorItem sensor = sensors.get(j);
                    		if (sensor.isVisible() && sensor.id() >= 0 && sensor.id() < charStat.sensorState.range.length){
                    			sensor.updateShapeOfVisibleArea(charStat.sensorState.range[sensor.id()]);
                    		}
                    	}
                    }
                }
                else
                    model.setCharacterPos(charStat.position, null);
            }
        }
    }

    private void rec(){
    	RecordingDialog dialog = new RecordingDialog(frame_);
        if (dialog.showModalDialog() != ModalDialog.OK_BUTTON){
        	btnRec_.setSelected(false);		
        	return;		
        }
    
        Dimension canvasSize = dialog.getImageSize();		
        int framerate=10;		
        try{		
        	framerate = dialog.getFrameRate();		
        }catch(Exception NumberFormatException ){		
        	new ErrorDialog(frame_, MessageBundle.get("Grx3DView.dialog.title.error"), MessageBundle.get("Grx3DView.dialog.message.error") ).showModalDialog();		 //$NON-NLS-1$ //$NON-NLS-2$
        	btnRec_.setSelected(false);		
        	return;		
        }		
        double playbackRate = dialog.getPlaybackRate();
        
        GrxDebugUtil.println("ScreenSize: " + canvasSize.width + "x" + canvasSize.height + " (may be)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        BufferedImage image=new BufferedImage(canvasSize.width,canvasSize.height,BufferedImage.TYPE_INT_ARGB);
		// 参照型で画像を設定
        ImageComponent2D buffer=new ImageComponent2D(ImageComponent.FORMAT_RGBA,image,true,false);
		buffer.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
		
		// オフスクリーンレンダリングの設定
		offscreen_=new Canvas3D(graphicsConfiguration,true);
		offscreen_.setOffScreenBuffer(buffer);	
		view_.addCanvas3D(offscreen_);

		Screen3D screen = canvas_.getScreen3D();
		offscreen_.getScreen3D().setSize(screen.getSize());
		offscreen_.getScreen3D().setPhysicalScreenWidth(screen.getPhysicalScreenWidth());
		offscreen_.getScreen3D().setPhysicalScreenHeight(screen.getPhysicalScreenHeight());
				
        recordingMgr_ = RecordingManager.getInstance();
        recordingMgr_.setImageSize(canvasSize.width , canvasSize.height);
        recordingMgr_.setFrameRate((float)framerate);
        
        String fileName = dialog.getFileName();
        if (new File(fileName).exists()) {
            if (!fileOverwriteDialog(fileName)){
            	btnRec_.setSelected(false);	
                return;
            }
        }

        lastMovieFileName = pathToURL(fileName);
        ComboBoxDialog cmb = new ComboBoxDialog(
            frame_, 
                MessageBundle.get("Grx3DView.dialog.title.videoFormat"),  //$NON-NLS-1$
                MessageBundle.get("Grx3DView.dialog.message.videoFormat"), //$NON-NLS-1$
                recordingMgr_.preRecord(lastMovieFileName, ImageToMovie.QUICKTIME));
        String format__ = (String)cmb.showComboBoxDialog();
        if (format__ == null) {
        	btnRec_.setSelected(false);	
            return;
        }
        
        try {
            if(!recordingMgr_.startRecord(format__)){
            	btnRec_.setSelected(false);	
                return;
            }
        } catch (Exception e) {
            GrxDebugUtil.printErr("Grx3DView.rec():",e); //$NON-NLS-1$
            syncExec(new Runnable(){
				public void run(){
					MessageDialog.openError( comp.getShell(), MessageBundle.get("Grx3DView.dialog.title.error"), MessageBundle.get("Grx3DView.dialog.message.recError")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
            btnRec_.setSelected(false);	
            return;
        }
        
        disableButton();
        viewMode_ = VIEW;
		objectToolBar_.setMode(ObjectToolBar.DISABLE_MODE);
               
        int step = (int)(1000.0/framerate*playbackRate);
        final int recordingStep = Math.max((int)(1000.0/framerate*playbackRate), step);
        
        Thread recThread_ = new Thread() {
			public void run() {
				try {
					int startPosition =0;
					int endPosition = currentWorld_.getLogSize();  
					double playRateLogTime_ = (double)recordingStep/1000.0;
					double prevTime = -recordingStep;
					for (int position=startPosition; position < endPosition; position++) {
						if(!btnRec_.isSelected())break;
						double time = currentWorld_.getTime(position);
						if (time - prevTime >= playRateLogTime_) {
							prevTime = time;
							final int _position = position;
							syncExec(new Runnable(){
								public void run() {
									currentWorld_.setPosition(_position);
								}
							});	
							_doRecording();
						}
					}
					stopRecording();
				} catch (Exception e) {
					syncExec(new Runnable(){
						public void run(){
							MessageDialog.openError( comp.getShell(), MessageBundle.get("Grx3DView.dialog.title.error"), MessageBundle.get("Grx3DView.dialog.message.recException")); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
					stopRecording();
					GrxDebugUtil.printErr("Recording Interrupted by Exception:",e); //$NON-NLS-1$
				}
			}
		};
		
		recThread_.start();
    }
    
    private void stopRecording(){
    	recordingMgr_.endRecord();
		btnRec_.setSelected(false);
		view_.removeCanvas3D(offscreen_);
		syncExec(new Runnable(){
			public void run(){
				MessageDialog.openInformation( comp.getShell(), MessageBundle.get("Grx3DView.dialog.title.Infomation"), MessageBundle.get("Grx3DView.dialog.message.recFinish")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		objectToolBar_.setMode(ObjectToolBar.OBJECT_MODE);
		enableButton();
    }
    
    private boolean ret_;
    private boolean fileOverwriteDialog(final String fileName){
        if  (new File(fileName).isDirectory())
            return false;

        syncExec(new Runnable(){
			public void run(){
				ret_ = MessageDialog.openConfirm( comp.getShell(), MessageBundle.get("Grx3DView.dialog.title.fileExist"), //$NON-NLS-1$
						fileName + " " + MessageBundle.get("Grx3DView.dialog.message.fileExist")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
        if(ret_)
            return true;
        else
        	return false;
    }
    
    private String pathToURL(String path) {
        if (path.startsWith("file:///")) { //$NON-NLS-1$
                    //String filePath = path.substring(8);
            path = path.replace(java.io.File.separatorChar, '/');
            return path;
        }
        if (path.startsWith("http://")) { //$NON-NLS-1$
            return path;
        }
        if (!path.startsWith(java.io.File.separator) && (path.indexOf(':') != 1)) {
            path = System.getProperty("user.dir") + java.io.File.separator + path; //$NON-NLS-1$
        }
        if (path.indexOf(':') == 1) {
            path = path.replace(java.io.File.separatorChar, '/');
            return "file:///" + path; //$NON-NLS-1$
        }
        return "file://" + path; //$NON-NLS-1$
    }
        
    private void _doRecording() {
    	offscreen_.renderOffScreenBuffer();
		offscreen_.waitForOffScreenRendering();
		recordingMgr_.pushImage( offscreen_.getOffScreenBuffer().getImage() );
    }
    
    public void _showCollision(Collision[] collisions) {
        collision_.removeAllGeometries();
        if (collisions == null || collisions.length <= 0 || !btnCollision_.isSelected()) 
            return;
            
        int length = 0;
        for (int i = 0; i < collisions.length; i++) {
            if (collisions[i].points != null)
                length += collisions[i].points.length;
        }
        if (length > 0) {
            CollisionPoint[] cd = new CollisionPoint[length];
            for (int i=0, n=0; i<collisions.length; i++) {
                for (int j=0; j<collisions[i].points.length; j++)
                    cd[n++] = collisions[i].points[j];
            }
            
            Point3d[] p3d = new Point3d[cd.length * 2];
            for (int j=0; j<cd.length; j++) {
                p3d[j*2] = new Point3d(cd[j].position);
                
                Vector3d pole = new Vector3d(cd[j].normal);
                pole.normalize();
                float depth = (float) cd[j].idepth*colprop+coldiff;
                p3d[j*2+1] = new Point3d(
                    cd[j].position[0] + pole.x*depth,
                    cd[j].position[1] + pole.y*depth,
                    cd[j].position[2] + pole.z*depth
                );
            }

            LineArray la = new LineArray(p3d.length, LineArray.COLOR_3
                    | LineArray.COORDINATES | LineArray.NORMALS);
            la.setCoordinates(0, p3d);
            
            Vector3f[] v3f = new Vector3f[p3d.length];
            Color3f[]  c3f =  new Color3f[p3d.length];
            for (int i=0; i<v3f.length; i++) {
                v3f[i] = new Vector3f(0.0f, 0.0f, 1.0f);
                if ((i % 2) == 0) 
                    c3f[i] = new Color3f(0.0f, 0.8f, 0.8f);
                else
                    c3f[i] = new Color3f(0.8f, 0.0f, 0.8f);
            }
            la.setNormals(0, v3f);
            la.setColors(0, c3f);
            collision_.addGeometry(la);
        } else {
            collision_.addGeometry(null);
        }
    }

    private void _showDistance(Distance[] distances) {
        distance_.removeAllGeometries();
        if (distances == null || distances.length <= 0 || !btnDistance_.isSelected()) 
            return;
            
        int length = distances.length;

        if (length > 0) {
            Point3d[] p3d = new Point3d[length * 2];
            for (int j=0; j<length; j++) {
                p3d[j*2] = new Point3d(distances[j].point0);
                p3d[j*2+1] = new Point3d(distances[j].point1);
            }

            LineArray la = new LineArray(p3d.length, LineArray.COLOR_3
                    | LineArray.COORDINATES | LineArray.NORMALS);
            la.setCoordinates(0, p3d);
            
            Vector3f[] v3f = new Vector3f[p3d.length];
            Color3f[]  c3f =  new Color3f[p3d.length];
            for (int i=0; i<v3f.length; i++) {
                v3f[i] = new Vector3f(0.0f, 0.0f, 1.0f);
                c3f[i] = new Color3f(1.0f, 0.0f, 0.0f);
            }
            la.setNormals(0, v3f);
            la.setColors(0, c3f);
            distance_.addGeometry(la);
        } else {
            distance_.addGeometry(null);
        }
    }
    
    @SuppressWarnings("unchecked") //$NON-NLS-1$
	private void _showIntersection(LinkPair[] pairs){
    	if (pairs == null){
    		for (int i=0; i<intersectingLinks_.size(); i++){
            	intersectingLinks_.get(i).restoreColor();
            }
            intersectingLinks_.clear();
    		return;
    	}else{	
	    	Map<String, GrxModelItem> modelMap = (Map<String, GrxModelItem>)manager_.getItemMap(GrxModelItem.class);
	    	Vector<GrxLinkItem> links = new Vector<GrxLinkItem>();
	    	for (int i=0; i<pairs.length; i++){
	    		GrxModelItem m1 = modelMap.get(pairs[i].charName1);
	    		if (m1 != null){
	    			GrxLinkItem l = m1.getLink(pairs[i].linkName1);
	    			if (l != null){
	    				links.add(l);
	    				if (!intersectingLinks_.contains(l)){
	    					l.setColor(java.awt.Color.RED);
	    				}
	    			}
	    		}
	    		GrxModelItem m2 = modelMap.get(pairs[i].charName2);
	    		if (m2 != null){
	    			GrxLinkItem l = m2.getLink(pairs[i].linkName2);
	    			if (l != null) links.add(l);
					if (!intersectingLinks_.contains(l)){
						l.setColor(java.awt.Color.RED);
					}
	    		}
	    	}
	    	for (int i=0; i<intersectingLinks_.size(); i++){
	    		GrxLinkItem l = intersectingLinks_.get(i);
	    		if (!links.contains(l)){
	    			l.restoreColor();
	    		}
	    	}
	    	intersectingLinks_ = links;
    	}
    }

    public void updateViewSimulator(double time) {
        for (int i=0; i<currentModels_.size(); i++) {
            List<Camera_impl> l = currentModels_.get(i).getCameraSequence();
            for (int j=0; j<l.size(); j++) {
                Camera_impl c = l.get(j);
                if (c.isVisible()) c.updateView(time);
            }
        }
    }
    
    public BranchGroup getRuler() {
        if (rulerBg_ == null) {
            rulerBg_ = new BranchGroup();
            rulerBg_.setCapability(BranchGroup.ALLOW_DETACH);
            int n = 40; // number of lines
            Point3d[] p = new Point3d[n * 4];
            double width = n/2.0;
            for (int i=0; i<n; i++) {
                p[2*i]       = new Point3d(-width+i, -width, 0.0);
                p[2*i+1]     = new Point3d(-width+i,  width, 0.0);
                p[2*i+n*2]   = new Point3d(-width, -width+i, 0.0);
                p[2*i+n*2+1] = new Point3d( width, -width+i, 0.0);
            }
            LineArray geometry = new LineArray(p.length,
                    GeometryArray.COORDINATES | GeometryArray.COLOR_3);
            geometry.setCoordinates(0, p);
            for (int i = 0; i < p.length; i++)
                geometry.setColor(i, new Color3f(Color.white));

            Shape3D shape = new Shape3D(geometry);
            shape.setPickable(false);
            rulerBg_.addChild(shape);
            rulerBg_.compile();
        }
        return rulerBg_;
    }
    
    public boolean registerCORBA() {
        NamingContext rootnc = GrxCorbaUtil.getNamingContext();
        
        OnlineViewer_impl olvImpl = new OnlineViewer_impl();
        OnlineViewer olv = olvImpl._this(manager_.orb_);//GrxCorbaUtil.getORB());
        NameComponent[] path1 = {new NameComponent("OnlineViewer", "")}; //$NON-NLS-1$ //$NON-NLS-2$
        ViewSimulator_impl  viewImpl = new ViewSimulator_impl();
        ViewSimulator view = viewImpl._this(manager_.orb_);//GrxCorbaUtil.getORB());
        NameComponent[] path2 = {new NameComponent("ViewSimulator", "")}; //$NON-NLS-1$ //$NON-NLS-2$
        try {
            rootnc.rebind(path1, olv);
            rootnc.rebind(path2, view);
        } catch (Exception ex) {
            GrxDebugUtil.println("3DVIEW : failed to bind to localhost NameService"); //$NON-NLS-1$
            return false;
        }
         
        GrxDebugUtil.println("3DVIEW : successfully bound to localhost NameService"); //$NON-NLS-1$
        return true;
    }
    
    
    public void unregisterCORBA() {
        NamingContext rootnc = GrxCorbaUtil.getNamingContext();
        NameComponent[] path1 = {new NameComponent("OnlineViewer", "")};
        NameComponent[] path2 = {new NameComponent("ViewSimulator", "")};
        try{
            rootnc.unbind(path1);
            rootnc.unbind(path2);
            GrxDebugUtil.println("3DVIEW : successfully unbound to localhost NameService");
        }catch(Exception ex){
            GrxDebugUtil.println("3DVIEW : failed to unbind to localhost NameService");
        }
    }
    
    private class ModelEditKeyAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent arg0) {
        	GrxLinkItem li = null;
        	GrxBaseItem bitem = manager_.focusedItem();
        	if (bitem instanceof GrxLinkItem){
        		li = (GrxLinkItem)bitem;
        	}else{
       	  		arg0.consume();
       	  		return;
       	  	}
        	GrxModelItem item = li.model();
           	  
          	KeyStroke ks = KeyStroke.getKeyStrokeForEvent(arg0);
          	if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_UP,0) ||
          			ks == KeyStroke.getKeyStroke(KeyEvent.VK_K,0)) {
          		int next = li.jointId()-1;
          		if (next >= 0) {
          			for (int j=0; j<item.links_.size(); j++) {
          				if (next == item.links_.get(j).jointId()) {
          					behaviorManager_.setPickTarget(item.links_.get(j).tg_);
          					break;
          				}
          			}
          		}
          	} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0) ||
          			ks == KeyStroke.getKeyStroke(KeyEvent.VK_J,0)) {
          		int next = li.jointId()+1;
  				for (int j=0; j<item.links_.size(); j++) {
      				if (next == item.links_.get(j).jointId()) {
          				behaviorManager_.setPickTarget(item.links_.get(j).tg_);
      					break;
      				}
       			}
          	} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,KeyEvent.SHIFT_MASK) ||
          			ks == KeyStroke.getKeyStroke(KeyEvent.VK_H,KeyEvent.SHIFT_MASK)) {
          		li.jointValue(li.jointValue()-dAngle_);
          		
          		if (li.llimit()[0] < li.ulimit()[0])
          			li.jointValue(Math.max(li.jointValue(), li.llimit()[0]));
          		item.calcForwardKinematics();
        	 	item.setProperty(li.getName()+".angle",String.valueOf(li.jointValue())); //$NON-NLS-1$
        	 	
          	} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,KeyEvent.SHIFT_MASK) ||
          			ks == KeyStroke.getKeyStroke(KeyEvent.VK_L,KeyEvent.SHIFT_MASK)) {
          		li.jointValue(li.jointValue()+dAngle_);
          		if (li.llimit()[0] < li.ulimit()[0])
          			li.jointValue(Math.min(li.jointValue(), li.ulimit()[0]));
        	 	item.calcForwardKinematics();
        	 	item.setProperty(li.getName()+".angle",String.valueOf(li.jointValue())); //$NON-NLS-1$
        	 	
          	} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_H,0) ||
          			ks == KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0)) {
          		li.jointValue(li.jointValue()-dAngle_*20);
          		if (li.llimit()[0] < li.ulimit()[0])
          			li.jointValue(Math.max(li.jointValue(), li.llimit()[0]));
        	 	item.calcForwardKinematics();
        	 	item.setProperty(li.getName()+".angle",String.valueOf(li.jointValue())); //$NON-NLS-1$
        	 	
          	} else if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_L,0) ||
          			ks == KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0)) {
          		li.jointValue(li.jointValue()+dAngle_*20);
          		if (li.llimit()[0] < li.ulimit()[0])
          			li.jointValue(Math.min(li.jointValue(), li.ulimit()[0]));
        	 	item.calcForwardKinematics();
        	 	item.setProperty(li.getName()+".angle",String.valueOf(li.jointValue())); //$NON-NLS-1$
          	}
          	arg0.consume();
        }     
    }
    
    private class ViewSimulator_impl extends ViewSimulatorPOA {
        public void destroy() {
        }

        public void getCameraSequence(CameraSequenceHolder arg0) {
            List<Camera_impl> allList = new ArrayList<Camera_impl>();
            for (int i=0; i<currentModels_.size(); i++) {
                List<Camera_impl> l = currentModels_.get(i).getCameraSequence();
                allList.addAll(l);
            }

            arg0.value = new Camera[allList.size()];
            for (int i=0; i<allList.size(); i++) {
                try {
                    arg0.value[i] = CameraHelper.narrow(manager_.poa_.servant_to_reference(allList.get(i)));
                } catch (ServantNotActive e) {
                    e.printStackTrace();
                } catch (WrongPolicy e) {
                    e.printStackTrace();
                }
            }
        }
        
        public void getCameraSequenceOf(String objectName, CameraSequenceHolder arg1) {
            for (int i=0; i<currentModels_.size(); i++) {
                GrxModelItem item = currentModels_.get(i);
                if (item.getName().equals(objectName)) {
                    List<Camera_impl> l = item.getCameraSequence();
                    arg1.value = new Camera[l.size()];
                    for (int j=0; j<l.size(); j++) {
                        try {
                            arg1.value[j] = CameraHelper.narrow(manager_.poa_.servant_to_reference(l.get(j)));
                        } catch (ServantNotActive e) {
                            e.printStackTrace();
                        } catch (WrongPolicy e) {
                            e.printStackTrace();
                        }
                    }
                    return;
                }
            }
            arg1.value = new Camera[0];
        }

        public void registerCharacter(String name, BodyInfo bInfo) {}
        public void updateScene(WorldState arg0) { }

    }
    
    private class OnlineViewer_impl extends OnlineViewerPOA {
        private double prevTime = 0.0;
        private boolean firstTime_ = true;
        private double logTimeStep_ = 0.0;
        private boolean updateTimer_ = false;
        
        public void clearLog() {
            if (currentWorld_ != null){
            	syncExec(new Runnable(){
            		public void run(){
            			currentWorld_.clearLog();
            		}
            	});
                currentState_=null;
            }
            firstTime_ = true;
        }

        public void load(final String name, String url) {
            System.out.println(name+":"+url); //$NON-NLS-1$
            try {
                URL u = new URL(url);
                final String url_ = u.getFile();
                syncExec(new Runnable(){
                	public void run(){
                		GrxBaseItem newItem = manager_.loadItem(GrxModelItem.class, name, url_);
                		if(newItem!=null){
	                        manager_.itemChange(newItem, GrxPluginManager.ADD_ITEM);
	                        manager_.setSelectedItem(newItem, true);
                		}
                	}
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        
        public boolean getPosture(String name , DblSequenceHolder posture) {
            Object obj = manager_.getItem(GrxModelItem.class, name);
            if (obj != null) {
                GrxModelItem model = (GrxModelItem)obj;
                posture.value =    model.getJointValues();
                return true;
            }
            posture.value = new double[0];
            return false;
        }
        
        public void update(WorldState arg0) {
            final GrxWorldStateItem.WorldStateEx statex = new GrxWorldStateItem.WorldStateEx(arg0);
            if (currentWorld_ == null) {
            	syncExec(new Runnable(){
            		public void run(){
            			GrxWorldStateItem item = (GrxWorldStateItem)manager_.createItem(GrxWorldStateItem.class, null);
            			manager_.itemChange(item, GrxPluginManager.ADD_ITEM);
            			manager_.setSelectedItem(item, true);
            		}
            	});
            }
            if (firstTime_) {
                firstTime_ = false;
                prevTime = 0.0;
                logTimeStep_ = 0.0;
                String[] chars = statex.characters();
                for (int i=0; i<chars.length; i++) {
                    GrxModelItem model = (GrxModelItem)manager_.getItem(GrxModelItem.class, chars[i]);
                    BodyInfo bodyInfo = model.getBodyInfo();
    				if(bodyInfo==null)	return;
    				currentWorld_.registerCharacter(chars[i], bodyInfo);
                }
                syncExec(new Runnable(){
            		public void run(){
	        			if(statex.time > 0){
	        				logTimeStep_ = statex.time;
	        				currentWorld_.setDbl("logTimeStep", logTimeStep_); //$NON-NLS-1$
	        			}
            		}
                }); 
            }
            
            final double stepTime = statex.time - prevTime;
            if(stepTime > 0 && Math.abs(stepTime-logTimeStep_)>10e-9){
            	logTimeStep_ = stepTime;
            	syncExec(new Runnable(){
            		public void run(){
            			currentWorld_.setDbl("logTimeStep", logTimeStep_); //$NON-NLS-1$
            		}
                });
            }
            
            currentWorld_.addValue(statex.time, statex);
            if(!updateTimer_){
	            java.util.Timer timer = new java.util.Timer();
	            timer.schedule(new updateView(),20);
	            updateTimer_ = true;
            }
            prevTime = statex.time;
        }
        
        public void clearData() {}
        public void drawScene(WorldState arg0) {
          update(arg0);
         }
        public void setLineScale(float arg0) {}
        public void setLineWidth(float arg0) {}
    
	    private class updateView extends java.util.TimerTask {
			public void run() {
				updateTimer_ = false;
				final int pos = currentWorld_.getLogSize()-1;
				syncExec(new Runnable(){
	    			public void run(){
	    				currentWorld_.setPosition(pos);
	    			}
	    		});
			}
	     }
    
    }
    
    public void attach(BranchGroup bg) {
        bgRoot_.addChild(bg);
    }

    public void attachUnclickable( BranchGroup bg) {
        unclickableBgRoot_.addChild(bg);
    }
    
    public String getFullName() {
        return getName();
    }

    public TransformGroup getTransformGroupRoot() {
        return tgView_;
    }

    public ViewInfo getViewInfo() {
        return info_;
    }

    public void setDirection(int dir) {
    }

    public void setTransform(Transform3D transform) {
        tgView_.setTransform(transform);
    }

    public void setViewMode(int mode) {
    }
    
    private void _registerAction() {
        GUIAction.ROOM_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                info_.setViewMode(ViewInfo.VIEW_MODE_ROOM);
                view_.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
                behaviorManager_.setViewMode(BehaviorManager.ROOM_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.ROOM_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_ROOM); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.WALK_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                info_.setViewMode(ViewInfo.VIEW_MODE_WALK);
                view_.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
                behaviorManager_.setViewMode(BehaviorManager.WALK_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.WALK_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_WALK); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.FRONT_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (info_.getDirection() != ViewInfo.FRONT_VIEW)
                    info_.setDirection(ViewInfo.FRONT_VIEW);
                info_.setViewMode(ViewInfo.VIEW_MODE_PARALLEL);
                view_.setProjectionPolicy(View.PARALLEL_PROJECTION);
                setTransform(info_.getTransform());
                behaviorManager_.setViewMode(BehaviorManager.PARALLEL_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.PARALLEL_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_FRONT); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.BACK_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (info_.getDirection() != ViewInfo.BACK_VIEW)
                    info_.setDirection(ViewInfo.BACK_VIEW);
                info_.setViewMode(ViewInfo.VIEW_MODE_PARALLEL);
                view_.setProjectionPolicy(View.PARALLEL_PROJECTION);
                setTransform(info_.getTransform());
                behaviorManager_.setViewMode(BehaviorManager.PARALLEL_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.PARALLEL_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_BACK); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.LEFT_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (info_.getDirection() != ViewInfo.LEFT_VIEW)
                    info_.setDirection(ViewInfo.LEFT_VIEW);
                info_.setViewMode(ViewInfo.VIEW_MODE_PARALLEL);
                view_.setProjectionPolicy(View.PARALLEL_PROJECTION);
                setTransform(info_.getTransform());
                behaviorManager_.setViewMode(BehaviorManager.PARALLEL_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.PARALLEL_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_LEFT); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.RIGHT_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (info_.getDirection() != ViewInfo.RIGHT_VIEW)
                    info_.setDirection(ViewInfo.RIGHT_VIEW);
                info_.setViewMode(ViewInfo.VIEW_MODE_PARALLEL);
                view_.setProjectionPolicy(View.PARALLEL_PROJECTION);
                setTransform(info_.getTransform());
                behaviorManager_.setViewMode(BehaviorManager.PARALLEL_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.PARALLEL_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_RIGHT); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.TOP_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (info_.getDirection() != ViewInfo.TOP_VIEW) 
                    info_.setDirection(ViewInfo.TOP_VIEW);
                info_.setViewMode(ViewInfo.VIEW_MODE_PARALLEL);
                view_.setProjectionPolicy(View.PARALLEL_PROJECTION);
                setTransform(info_.getTransform());
                behaviorManager_.setViewMode(BehaviorManager.PARALLEL_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.PARALLEL_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_TOP); //$NON-NLS-1$
                	}
                });
            }
        });

        GUIAction.BOTTOM_VIEW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (info_.getDirection() != ViewInfo.BOTTOM_VIEW)
                    info_.setDirection(ViewInfo.BOTTOM_VIEW);
                info_.setViewMode(ViewInfo.VIEW_MODE_PARALLEL);
                view_.setProjectionPolicy(View.PARALLEL_PROJECTION);
                setTransform(info_.getTransform());
                behaviorManager_.setViewMode(BehaviorManager.PARALLEL_VIEW_MODE);
                viewToolBar_.setMode(ViewToolBar.PARALLEL_MODE);
                syncExec(new Runnable(){
                	public void run(){
                		setProperty("view.mode", ViewToolBar.COMBO_SELECT_BOTTOM); //$NON-NLS-1$
                	}
                });
            }
        });
        GUIAction.VIEW_ZOOM_MODE.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                behaviorManager_.setViewHandlerMode("button_mode_zoom"); //$NON-NLS-1$
                // viewHandlerMode_[currentViewer_] = "button_mode_zoom";
                viewToolBar_.setOperation(ViewToolBar.ZOOM);
                //objectToolBar_.selectNone();
                //lblMode_.setText("[ VIEW ]");
            }
        });

        GUIAction.VIEW_ROTATION_MODE.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                behaviorManager_.setViewHandlerMode("button_mode_rotation"); //$NON-NLS-1$
                // viewHandlerMode_[currentViewer_] = "button_mode_rotation";
                viewToolBar_.setOperation(ViewToolBar.ROTATE);
                //objectToolBar_.selectNone();
                //lblMode_.setText("[ VIEW ]");
            }
        });
        
        GUIAction.VIEW_PAN_MODE.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                behaviorManager_.setViewHandlerMode("button_mode_translation"); //$NON-NLS-1$
                // viewHandlerMode_[currentViewer_] = "button_mode_translation";
                viewToolBar_.setOperation(ViewToolBar.PAN);
                //objectToolBar_.selectNone();
                //lblMode_.setText("[ VIEW ]");
            }
        });
        
        GUIAction.WIRE_FRAME.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Iterator<?> it = manager_.getItemMap(GrxModelItem.class).values().iterator();
                while (it.hasNext()) {
                    ((GrxModelItem)it.next()).setWireFrame(viewToolBar_.isWireFrameSelected());
                }
            }
        });

        GUIAction.BG_COLOR.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Bring up a color chooser
                /*
                Color3f oldColor = new Color3f();
                backGround_.getColor(oldColor);
                Color c = JColorChooser.showDialog(frame_,
                        MessageBundle.get("dialog.bgcolor"), new Color(
                                oldColor.x, oldColor.y, oldColor.z));
                if (c != null) {
                    backGround_.setColor(new Color3f(c));
                }
                */
                
                Display display = Display.getDefault();
                if ( display!=null && !display.isDisposed())
                        display.asyncExec(
                                new Runnable(){
                                    public void run() {
                                        ColorDialog dialog = new ColorDialog( getParent().getShell() );
                                        RGB color = dialog.open();
                                        Color3f c = new Color3f( color.red/255f, color.green/255f, color.blue/255f );
                                        backGround_.setColor(new Color3f(c));
                                    }
                                }
                        );
            }
        });
        

        GUIAction.CAPTURE.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Display display = Display.getDefault();
                if ( display!=null && !display.isDisposed())
                        display.asyncExec(
                                new Runnable(){
                                    public void run() {
                                        FileDialog fdlg = new FileDialog( getParent().getShell(), SWT.SAVE);
                                        String[] fe = { "*.png" }; //$NON-NLS-1$
                                        fdlg.setFilterExtensions( fe );
                                        String fPath = fdlg.open();
                                        if( fPath != null ){
                                            saveScreenShot( new File( fPath ) );
                                        }
                                    }
                                }
                        );
            }
        });

        GUIAction.OBJECT_TRANSLATION.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.OBJECT_TRANSLATION_MODE){
                    behaviorManager_.setOperationMode(BehaviorManager.OBJECT_TRANSLATION_MODE);
                    viewMode_ = EDIT;
                    objectToolBar_.setMode(ObjectToolBar.OBJECT_MODE);
                    lblMode_.setText(MessageBundle.get("Grx3DView.text.editTranslate")); //$NON-NLS-1$
                    showOption();
            	}
            }
        });

        GUIAction.OBJECT_ROTATION.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.OBJECT_ROTATION_MODE){
            		behaviorManager_.setOperationMode(BehaviorManager.OBJECT_ROTATION_MODE);
            		viewMode_ = EDIT;
            		objectToolBar_.setMode(ObjectToolBar.OBJECT_MODE);
            		lblMode_.setText(MessageBundle.get("Grx3DView.text,editRotate")); //$NON-NLS-1$
            		showOption();
            	}
            }
        });
        GUIAction.JOINT_ROTATION.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.JOINT_ROTATION_MODE){
            		behaviorManager_.setOperationMode(BehaviorManager.JOINT_ROTATION_MODE);
            		viewMode_ = EDIT;
            		objectToolBar_.setMode(ObjectToolBar.OBJECT_MODE);
            		lblMode_.setText(MessageBundle.get("Grx3DView.text.editMove")); //$NON-NLS-1$
            		showOption();
            	}
            }
        });

        GUIAction.FITTING_SRC.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.FITTING_FROM_MODE){
	                objectToolBar_.setMode(ObjectToolBar.FITTING_MODE);
	                viewToolBar_.setEnabled(true);
	                behaviorManager_.setOperationMode(BehaviorManager.FITTING_FROM_MODE);
	                viewMode_ = EDIT;
	                lblMode_.setText(MessageBundle.get("Grx3DView.text.editObjectSelect")); //$NON-NLS-1$
	                showOption();
            	}
            }
        });

        GUIAction.FITTING_DEST.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.FITTING_TO_MODE){
	                objectToolBar_.setMode(ObjectToolBar.FITTING_MODE);
	                viewToolBar_.setEnabled(true);
	                behaviorManager_.setOperationMode(BehaviorManager.FITTING_TO_MODE);
	                viewMode_ = EDIT;
	                lblMode_.setText(MessageBundle.get("Grx3DView.text,editObjectDestination")); //$NON-NLS-1$
	                showOption();
            	}
            }
        });

        GUIAction.DO_FIT.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //setModelUpdate(false);
                behaviorManager_.fit();
                objectToolBar_.setMode(ObjectToolBar.FITTING_START_MODE);
                viewToolBar_.setEnabled(true);
                behaviorManager_.setOperationMode(BehaviorManager.FITTING_FROM_MODE);
                viewMode_ = EDIT;
                lblMode_.setText(MessageBundle.get("Grx3DView.text.editObjectSelect")); //$NON-NLS-1$
                showOption();
            }
        });

        GUIAction.INV_KINEMA_FROM.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {  	
            	if (behaviorManager_.getOperationMode() != BehaviorManager.INV_KINEMA_FROM_MODE){
	            	objectToolBar_.setMode(ObjectToolBar.INV_KINEMA_MODE);
	            	behaviorManager_.setOperationMode(BehaviorManager.INV_KINEMA_FROM_MODE);
	            	viewMode_ = EDIT;
	            	lblMode_.setText(MessageBundle.get("Grx3DView.text.IKbaseLink")); //$NON-NLS-1$
	            	showOption();
            	}
            }
        });

        GUIAction.INV_KINEMA_TRANS.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.INV_KINEMA_TRANSLATION_MODE){
	                objectToolBar_.setMode(ObjectToolBar.INV_KINEMA_MODE);
	                behaviorManager_.setOperationMode(BehaviorManager.INV_KINEMA_TRANSLATION_MODE);
	                viewMode_ = EDIT;
	                lblMode_.setText(MessageBundle.get("Grx3DView.text.IKtranslate")); //$NON-NLS-1$
	                showOption();
            	}
            }
        });

        GUIAction.INV_KINEMA_ROT.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (behaviorManager_.getOperationMode() != BehaviorManager.INV_KINEMA_ROTATION_MODE){
	                objectToolBar_.setMode(ObjectToolBar.INV_KINEMA_MODE);
	                behaviorManager_.setOperationMode(BehaviorManager.INV_KINEMA_ROTATION_MODE);
	                viewMode_ = EDIT;
	                lblMode_.setText(MessageBundle.get("Grx3DView.text.IKRotate")); //$NON-NLS-1$
	                showOption();
            	}
            }
        });

        GUIAction.OPERATION_DISABLE.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	disableOperation();
            	viewMode_ = VIEW;
            }
        });
    }
    
    public void disableOperation(){
        //setModelUpdate(true);
    	
        behaviorManager_.setOperationMode(BehaviorManager.OPERATION_MODE_NONE);       
        objectToolBar_.setMode(ObjectToolBar.OBJECT_MODE);
        objectToolBar_.selectNone();
        viewToolBar_.setEnabled(true);
        lblMode_.setText(MessageBundle.get("Grx3DView.text.view")); //$NON-NLS-1$
        
        if(currentState_!=null){
        	syncExec(new Runnable(){
            	public void run(){
            		updateModels(currentState_);
            		_showCollision(currentState_.collisions);
            	}
            });
			updateViewSimulator(currentState_.time);
			showOptionWithoutCollision();
        }
    }

    public void addClickListener( Grx3DViewClickListener listener ){
        behaviorManager_.addClickListener( listener );
    }

    public void removeClickListener( Grx3DViewClickListener listener ){
        behaviorManager_.removeClickListener( listener );
    }

    /*
    private PickCanvas initPickCanvas(int x, int y){
        PickCanvas pickCanvas = new PickCanvas(
                getCanvas3D(),
                getBranchGroupRoot()
            );

        pickCanvas.setShapeLocation( x, y );

        pickCanvas.setMode(PickInfo.NODE);

        PickCone pickCone = (PickCone)pickCanvas.getPickShape();
        Point3d pickOrig = new Point3d();
        Vector3d pickDir = new Vector3d();
        pickCone.getOrigin( pickOrig );
        pickCone.getDirection( pickDir );
        pickCanvas.setShapeRay( pickOrig, pickDir );

        // PickInfoの取得。フラグ設定が重要。
        pickCanvas.setFlags(PickInfo.NODE | PickInfo.CLOSEST_INTERSECTION_POINT |PickInfo.SCENEGRAPHPATH |PickInfo.LOCAL_TO_VWORLD );

        return pickCanvas;
    }
    
    public Point3d getClickPoint(int x, int y){
        PickCanvas canvas = initPickCanvas(x,y);
        PickInfo pickInfo = canvas.pickClosest();
        if (pickInfo == null) {
            GrxDebugUtil.println("[3DView] PickInfo Null.");
        }

        if(pickInfo == null)
            return null;

        //クリック位置の取得、世界座標への変換
        Point3d intersectionPoint = pickInfo.getClosestIntersectionPoint();
        Transform3D ltov = pickInfo.getLocalToVWorld();
        ltov.transform(intersectionPoint);

        if( intersectionPoint == null ){
            GrxDebugUtil.println("[3dView] Not Intersect point.");
        }else{
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(2);
            GrxDebugUtil.println( "CLICK="+"("
                +format.format(intersectionPoint.x)+","
                +format.format(intersectionPoint.y)+","
                +format.format(intersectionPoint.z)+")" );
        }

        return intersectionPoint;
    }

    public TransformGroup getClickNode(int x, int y, int type){
        PickCanvas pickCanvas = initPickCanvas(x,y);
        PickInfo pickInfo = pickCanvas.pickClosest();
        if (pickInfo == null) {
            GrxDebugUtil.println("[3DView] PickInfo Null.");
        }

        //クリックされたノードを取得する
        TransformGroup tg = null;
        try {
            tg = (TransformGroup) pickCanvas.getNode(pickInfo, type );
            if (tg == null)
                GrxDebugUtil.println("[3DView] Node Null.");
        } catch (CapabilityNotSetException ex) {
            ex.printStackTrace();
        }

        return tg;
    }
*/
    
    
    public void saveScreenShot( File file ){
        if ( file == null )
            return;

        //onScreen方式
        Raster raster = null;
        // 読み込み用カラー情報 : type int, Alpha:8bit, R:8bit, G:8bit, B:8bit
        BufferedImage bimageRead = new BufferedImage(canvas_.getWidth(),
                canvas_.getHeight(), BufferedImage.TYPE_INT_RGB);
        ImageComponent2D readImage = new ImageComponent2D(
                ImageComponent.FORMAT_RGB, bimageRead);
        // 読み込み用ラスタ
        raster = new Raster(new Point3f(0.0f, 0.0f, 0.0f),
                Raster.RASTER_COLOR, 0, 0, bimageRead.getWidth(),
                bimageRead.getHeight(), readImage, null
        //readDepthFloat
        //readDepthInt
        );
        //raster.setCapability(Raster.ALLOW_DEPTH_COMPONENT_READ);
        raster.setCapability(Raster.ALLOW_IMAGE_READ);
        canvas_.getGraphicsContext3D().readRaster(raster);
        // カラー情報読み込み
        BufferedImage image = raster.getImage().getImage();
    
        /*
        //offScreen方式
        if(imageChooser_.showSaveDialog(GUIManager.this) == JFileChooser.APPROVE_OPTION ){
            //視点をセット
            recordingMgr_ = RecordingManager.getInstance();   // 録画マネージャ
            recordingMgr_.setView(viewer_[currentViewer_].getDrawable());
            //カメラ
            ImageCamera camera = recordingMgr_.getImageCamera();
            //サイズをセット
            Dimension d = viewer_[currentViewer_].getCanvas3D().getSize();
            camera.setSize(d.width,d.height);
            //イメージ取り出し
            java.awt.image.BufferedImage image = camera.getImage();
        */

        //保存
        try {
            javax.imageio.ImageIO.write(image, "PNG", file); //$NON-NLS-1$
        } catch (IOException ex) {
            // この関数はSWTのEDTから呼ばれるのでSWT.syncExec()とかしなくていいはず
            MessageDialog.openWarning( getParent().getShell(), "", MessageBundle.get("message.ioexception") ); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    public View getView(){
    	return view_;
    }
    
    private boolean xor(boolean a, boolean b){
    	return (a || b) && (!a || !b);
    }
    
    public boolean propertyChanged(String key, String value){
    	if (super.propertyChanged(key, value)){
    		return true;
    	}else {
    		if (key.equals("showScale")){ //$NON-NLS-1$
    			if(xor(btnFloor_.isSelected(), value.equals("true")))
    				btnFloor_.doClick();
    		}else if (key.equals("showCollision")){ //$NON-NLS-1$	
    			if(xor(btnCollision_.isSelected(), value.equals("true")))
    				btnCollision_.doClick();
     		}else if (key.equals("showDistance")){ //$NON-NLS-1$
     			if(xor(btnDistance_.isSelected(), value.equals("true")))
     				btnDistance_.doClick();
    		}else if (key.equals("showIntersection")){ //$NON-NLS-1$
    			if(xor(btnIntersection_.isSelected(), value.equals("true")))
    				btnIntersection_.doClick();
    		}else if (key.equals("showCoM")){ //$NON-NLS-1$	
    			if(xor(btnCoM_.isSelected(), value.equals("true")))
    				btnCoM_.doClick();
    		}else if (key.equals("showCoMonFloor")){ //$NON-NLS-1$	
    			if(xor(btnCoMonFloor_.isSelected(), value.equals("true")))
    				btnCoMonFloor_.doClick();
    		}else if (key.equals("view.mode")){ //$NON-NLS-1$	
    			value = viewToolBar_.selectViewMode(value);
    		}else if (key.equals("showActualState")){ //$NON-NLS-1$	
    			showActualState_ = value.equals("true");
    		}else if (key.equals("eyeHomePosition")){ //$NON-NLS-1$	
    			double[] eyeHomePosition = getDblAry(value);
    			t3dViewHome_.set(eyeHomePosition);
    		    setTransform(t3dViewHome_);
    		}else{
    			return false;
    		}
    		final String key_ = key;
    		final String value_ = value;
    		syncExec(new Runnable(){
            	public void run(){
            		setProperty(key_, value_);
            	}
            });
    		return true;
    	}
    }
   
    // view が閉じられたときの処理
    public void shutdown() {
    	showViewSimulator(false);
    	behaviorManager_.destroyDynamicsSimulator();
    	Iterator<GrxModelItem> it = currentModels_.iterator();
    	while(it.hasNext())	{
    		GrxModelItem model = it.next();
    		model.bgRoot_.detach();
    		model.deleteObserver(this);
    	}
    	manager_.removeItemChangeListener(this, GrxModelItem.class);
    	manager_.removeItemChangeListener(this, GrxWorldStateItem.class);
    	if(currentWorld_!=null)
        {
    		currentWorld_.deleteObserver(this);
    		currentWorld_.deletePosObserver(this);
        }
    	manager_.removeItemChangeListener(this, GrxCollisionPairItem.class);
		if(simItem_!=null)
			simItem_.deleteObserver(this);
		manager_.removeItemChangeListener(this, GrxSimulationItem.class);
	}

    public void repaint(){
    	view_.repaint();
    	objectToolBar_.repaint();
    	viewToolBar_.repaint();
    }
    
    private boolean ans_;
    private List<GrxModelItem> setNumOfAABB(){
    	List<GrxModelItem> ret = new ArrayList<GrxModelItem>();
     	List<GrxModelItem> models = new ArrayList<GrxModelItem>();
    	models.addAll(currentModels_);
    	while(!models.isEmpty()){
    		final GrxModelItem model = models.get(0);
    		final List<GrxModelItem> sameModels = model.getSameUrlModels(); 
	    	boolean flg=false;
	    	for(int i=0; i<model.links_.size(); i++){
	    		if(model.links_.get(i).getStr("NumOfAABB").equals("original data")){
	    			flg = true;
	    			break;
	    		}
	    	}
	    	if(flg){
	    		syncExec(new Runnable(){
	    			public void run(){
	    				ans_ = MessageDialog.openConfirm(comp.getShell(), MessageBundle.get("Grx3DView.dialog.title.confirmation"), model.getName()+" "+MessageBundle.get("Grx3DView.dialog.message.setAABBdepth")); //$NON-NLS-1$ //$NON-NLS-2$
			    		if(ans_){
			    			for(int i=0; i<model.links_.size(); i++){
		    					if(model.links_.get(i).getStr("NumOfAABB").equals("original data")){
		    						model.links_.get(i).setInt("NumOfAABB",1);
		    						model.setProperty(model.links_.get(i).getName()+".NumOfAABB", "1");
		    					}
		    				}
			    			model.makeAABBforSameUrlModels();
			    		}
	    			}
	    		});
	    		if(ans_){
	    			ret.addAll(sameModels);
	    			ret.add(model);
	    		}
	    	}else{
	    		ret.add(model);
	    		ret.addAll(sameModels);
	    	}
	    	models.removeAll(sameModels);
	    	models.remove(model);
    	}
    	return ret;
    }
    
    private boolean btnStateCollision_=true;
    private boolean btnStateDistance_=false;
    private boolean btnStateIntersection_=false;
    private boolean modelModified_ = false;
    private void optionButtonEnable(boolean enable){
    	if(!modelModified_ && enable )
    		return;
    	if(modelModified_ && !enable)
    		return;
    	if(enable){
    		for(GrxModelItem model : currentModels_ ){
    			if(model.isModified())
    				return;
    		}
    	}
    	if(!enable){
    		btnStateCollision_ = btnCollision_.isSelected();
        	btnStateDistance_ = btnDistance_.isSelected();
        	btnStateIntersection_ = btnIntersection_.isSelected();
    		if(btnCollision_.isSelected())
    			btnCollision_.doClick();
    		if(btnDistance_.isSelected())
    			btnDistance_.doClick();
    		if(btnIntersection_.isSelected())
    			btnIntersection_.doClick();
    		modelModified_ = true;
    	}
    	btnCollision_.setEnabled(enable);
    	btnDistance_.setEnabled(enable);
    	btnIntersection_.setEnabled(enable);
    	if(enable){
    		if(btnStateCollision_)
    			btnCollision_.doClick();
    		if(btnStateDistance_)
    			btnDistance_.doClick();
    		if(btnStateIntersection_)
    			btnIntersection_.doClick();
    		modelModified_ = false;
    	}
    }
    
    public ValueEditType GetValueEditType(String key) {
        if(key.equals("showCoM") || key.equals("showCoMonFloor") || key.equals("showDistance") ||
        		key.equals("showIntersection") || key.equals("showCollision") || key.equals("showActualState") ||
        		key.equals("showScale"))
        {
            return new ValueEditCombo(booleanComboItem_);
        }else if(key.equals("view.mode")){
        	return new ValueEditCombo(ViewToolBar.VIEW_MODE);
        }
        return super.GetValueEditType(key);
    }
}

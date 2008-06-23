/*
 *  GrxUIFrame.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import org.w3c.dom.Element;

import com.generalrobotix.ui.util.GrxConfigPane;
import com.generalrobotix.ui.util.GrxDebugUtil;
import com.generalrobotix.ui.item.GrxModeInfoItem;
import com.generalrobotix.ui.view.GrxLoggerView;

@SuppressWarnings("serial")
public class GrxUIFrame extends JFrame {
	private GrxPluginManager manager_ = null;
	private final GrxConfigPane configPane_ = new GrxConfigPane(this);
	
	private final JMenuBar menuBar_ = new JMenuBar();
	private final JPanel[] toolBarPanel_ = new JPanel[2];
	private final JToolBar modeToolBar_ = new JToolBar();
	private JLabel waitLabel = new JLabel("  Loading View Plugins ...", JLabel.CENTER);
	private List<ImageIcon> robotIcons = new ArrayList<ImageIcon>();
	
	private boolean firstTime_ = true;
	private int waitCount = 0;

	public GrxUIFrame(GrxPluginManager manager) {
		manager_ = manager;

		// Frame //
		this.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(
				getClass().getResource("/resources/images/logo.png")));
		this.setSize(800, 600);

		Dimension dispSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((dispSize.width - getSize().width) / 2,
				(dispSize.height - getSize().height - getInsets().top) / 2);
		this.setTitle("GrxUI - Robot Control Interface");
		this.setJMenuBar(menuBar_);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {           
                exitFunction();
            }
        });


		JComponent contentPane = (JComponent) this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		// NorthPanel
		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));
		contentPane.add(northPanel, BorderLayout.NORTH);
		for (int i = 0; i < toolBarPanel_.length; i++) {
			toolBarPanel_[i] = new JPanel();
			toolBarPanel_[i].setLayout(new BoxLayout(toolBarPanel_[i],
					BoxLayout.X_AXIS));
			toolBarPanel_[i].setAlignmentX(JPanel.LEFT_ALIGNMENT);
			northPanel.add(toolBarPanel_[i],0);
		}

		modeToolBar_.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
		modeToolBar_.setVisible(false);
		modeToolBar_.setFloatable(false);
		toolBarPanel_[0].add(modeToolBar_);
		//  toolBarPanel_[0].add(store);
		//  toolBarPanel_[0].add(restore);

		// SouthPanel //
		JPanel statePane = new JPanel();
		statePane.add(new JLabel(""));
		contentPane.add(statePane, BorderLayout.SOUTH);

		waitLabel.setFont(new Font("Monospaced", Font.PLAIN | Font.TRUETYPE_FONT, 14));
		waitLabel.setIcon(manager_.ROBOT_ICON);

		contentPane.add(waitLabel, 0);
		
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		robotIcons.add(new ImageIcon(toolkit.getImage(getClass().getResource("/resources/images/grxrobot.png"))));
		for (int i = 1; i < 14; i++) 
			robotIcons.add(new ImageIcon(toolkit.getImage(getClass().getResource("/resources/images/grxrobot"+i+".png"))));
		
		javax.swing.Timer t = new javax.swing.Timer(800, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!firstTime_)
					((javax.swing.Timer) e.getSource()).stop();
				waitLabel.setIcon(robotIcons.get(waitCount % robotIcons.size()));
				waitCount++;
			}
		});
		t.start();
	}



	public void updateTab(List<GrxBaseView> list) {
		configPane_.clearAllTab();
		_resetMenuBar();
		toolBarPanel_[1].removeAll();
		for (int i = 0; i < list.size(); i++) {
			GrxBaseView view = list.get(i);
			// change tabs
			configPane_.insertTab(view.getContentPane(),
					view.isScrollable_, -1);

			// change menu
			final JMenu menu = view.getMenu();
			final String[] path = view.getMenuPath();
			_addMenu(path, menu);

			// change toolBar
			final JComponent toolbar = view.getToolBar();
			if (toolbar != null) {
				//if (toolbar instanceof JToolBar) 
				//	((JToolBar)toolbar).setFloatable(false);
				if (view instanceof GrxLoggerView) {
					((JToolBar)toolbar).setFloatable(false);
					toolBarPanel_[0].add(toolbar, 0);
				} else {
					toolBarPanel_[1].add(toolbar);
				}
			}
		}
		
		_disableLightWeightPopup(menuBar_);
		
		if (firstTime_) {
			firstTime_ = false;
			this.getContentPane().remove(0);
			this.getContentPane().add(configPane_, BorderLayout.CENTER);
		}
	}
	
	private void _addMenu(String[] path, JMenuItem menu) {
		if (menu != null && path != null) {
			JComponent c = menuBar_;
			JMenu m = null;
			for (int j = 0; j < path.length; j++) {
				m = null;
				for (int k = 0; k < c.getComponentCount(); k++) {
					m = (JMenu) c.getComponent(k);
					if (m.getText().equals(path[j]))
						break;
					m = null;
				}
				if (m == null) {
					m = new JMenu(path[j]);
					if (c == menuBar_)
						c.add(m, Math.max(1, menuBar_.getMenuCount() - 2));
					else 
						c.add(m);
				}
				c = m;
			}
			m.add(menu);
		}
	}
	
	private void _resetMenuBar() {
		menuBar_.removeAll();
		final JMenu fileMenu = new JMenu("File");
		JMenu help = new JMenu("Help");
		menuBar_.add(fileMenu);
		menuBar_.add(configPane_.getMenu());
		menuBar_.add(help);
		
		final JMenuItem exit = new JMenuItem("Exit");
		
		JMenu pmenu = manager_.getProjectMenu();
		String[] path = new String[]{"File"};
		int count = pmenu.getItemCount();
		for (int i=0; i<count ;i++) 
			_addMenu(path, pmenu.getItem(0));
		
		fileMenu.addSeparator();
		fileMenu.add(exit);
		

		exit.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
                exitFunction(); 
			}
		});

		JMenuItem about = new JMenuItem("about");
		help.add(about);
		about.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
				showAboutDialog(GrxUIFrame.this);
			}
		});
	}
	
	private void _disableLightWeightPopup(JComponent comp) {
		Component[] c;
		if (comp instanceof JMenu)
			c = ((JMenu)comp).getMenuComponents();
		else 
			c = comp.getComponents();
		
		for (int j=0; j<c.length; j++) {
			if (c[j] instanceof JMenu) {
				JMenu mc = (JMenu)c[j];
				((JMenu)mc).getPopupMenu().setLightWeightPopupEnabled(false);
				_disableLightWeightPopup(mc);
			}
		}
	}

	class ChangeModeButton extends JToggleButton {
		public ChangeModeButton(final GrxModeInfoItem mode, final Icon icon) {
			super(mode.getName(), icon);
			setMaximumSize(new Dimension(100, 20));
			addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
					Thread t = new Thread(){
						public void run() {
							Component[] cs = modeToolBar_.getComponents();
							for (int i=0; i<cs.length; i++)
								cs[i].setEnabled(false);
							
							manager_.setMode(mode);
							
							for (int i=0; i<cs.length; i++)  {
								if (cs[i] != arg0.getSource())
									cs[i].setEnabled(true);
							}
						}
					};
					t.start();
				}
			});
		}
	}

	public void updateModeButtons(GrxModeInfoItem[] modeList, GrxModeInfoItem mode) {
		modeToolBar_.setVisible(false);
		modeToolBar_.removeAll();
		ButtonGroup modeGroup = new ButtonGroup();
		for (int i = 0; i < modeList.length; i++) {
			GrxModeInfoItem m = modeList[i];
			ChangeModeButton b = new ChangeModeButton(m, m.getIcon());
			if (m == mode) {
				b.setSelected(true);
				b.setEnabled(false);
			}
			modeToolBar_.add(b);
			modeGroup.add(b);
		}
		if (modeList.length > 1)
			modeToolBar_.setVisible(true);
	}
	
	static void showAboutDialog(final JFrame parent) {
		JOptionPane.showMessageDialog(parent,
				  "GrxUI - Robot Control Interface\n"
				+ "for OpenHRP3 RC4\n"
				+ "(c) Copyright General Robotix ,Inc. 2006.  All rights reserved.\n"
				+ "URL http://www.generalrobotix.com/",
				"About Auditor", JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(parent.getClass().getResource("/resources/images/logo.png")));
	}
	
	public void setConfigElement(Element el) {
		configPane_.setElement(el);
	}

	public void setConfigFileName(final String name) {
		configPane_.setStoreFileName(name);
	}
	
	public void storeConfig(Element el) {
		configPane_.storeConfig(el);
	}

	public void restoreConfig(Element el) {
		try {
			if (el == null)
				configPane_.restoreConfig();
			else
				configPane_.restoreConfig(el);
		} catch (final Exception e) {
			GrxDebugUtil.printErr("restoreConfig:", e);
		}
	}
  
    public void exitFunction() {
        int ans = JOptionPane.showConfirmDialog(this,
            "Are you sure exit GrxUI ?",
            "Exit GrxUI", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.QUESTION_MESSAGE, manager_.ROBOT_ICON);
        if (ans == JOptionPane.YES_OPTION)
            manager_.shutdown();
    }
}

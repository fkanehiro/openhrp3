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
 *  GrxConfigPane.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */
package com.generalrobotix.ui.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("serial")
public class GrxConfigPane extends JPanel {
	private static final int WINDOW_MIN_WIDTH = 100;
	private static final int WINDOW_MIN_HEIGHT = 25;
	private static final int GAP = 3;
	private static final String DEFAULT_TAG_NAME = "windowconfig";
	private static final String DEFAULT_STORE_FNAME = ".window.cfg";
	private String storeName_ = DEFAULT_STORE_FNAME;
	private String fname_;
	
	private JFrame owner_;
	private String rootSwapDirection_;
	private ElementPane rootPane_;
	private ElementPane selectedPane_;
	private ElementPane directedPane_;
	private ElementPane focusedPane_;
	private JTabbedPane focusedTabPane_;
	private JMenu menu_;
	private JCheckBoxMenuItem fullScreen = new JCheckBoxMenuItem("full screen");
	
	private Map<String, Component> tablist_ = new HashMap<String, Component>();
	private ArrayList<Component> windowList_ = new ArrayList<Component>();

	private boolean configLock_ = false;
	
	private Document doc_;
	private Element configEl_;
	
	private java.awt.GraphicsDevice screen_ = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

	public GrxConfigPane(JFrame owner) {
		super(new BorderLayout());
		owner_ = owner;
		String[] pos = new String[] { 
				BorderLayout.EAST,  BorderLayout.WEST,
				BorderLayout.SOUTH, BorderLayout.NORTH, };
		
		Dimension d1 = new Dimension(4, 4);
		for (int i = 0; i < 4; i++) {
			JPanel p = new JPanel();
			p.setMaximumSize(d1);
			p.setPreferredSize(d1);
			p.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent evt) {
					JPanel p = (JPanel)evt.getSource();
					BorderLayout bl = (BorderLayout)p.getParent().getLayout();
					rootSwapDirection_ = (String) bl.getConstraints(p);
				}

				public void mouseExited(MouseEvent evt) {
					if (rootSwapDirection_ != null) {
						rootSwapDirection_ = null;
						repaint();
					}
				}
			});
			this.add(p, pos[i]);
		}
		
		rootPane_ = new ElementPane();
		this.add(rootPane_, BorderLayout.CENTER);
		 
		this.addComponentListener(new ComponentAdapter() {
		 	public void componentResized(ComponentEvent arg0) {
		        if (focusedPane_ == null)
                    _moveSeparatorRecursively(rootPane_);
		 	}
		 });
	}
	
	public JMenu getMenu() {
		if (menu_ == null) {
			menu_ = new JMenu("Window");
			final JMenu showView = new JMenu("show view");
			JMenuItem storeConfig = new JMenuItem("store config as default");
			JMenuItem restoreConfig = new JMenuItem("restore config from default");
			
			menu_.add(showView);
			if (screen_.isFullScreenSupported())
				menu_.add(fullScreen);
			menu_.add(storeConfig);
			menu_.add(restoreConfig);
			menu_.getPopupMenu().addPopupMenuListener(new PopupMenuListener(){
				public void popupMenuCanceled(PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					fullScreen.setSelected(screen_.getFullScreenWindow() == owner_);
				}
			});
			fullScreen.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
					if (item.isSelected())
						screen_.setFullScreenWindow(owner_);
					else 
						screen_.setFullScreenWindow(null);
				}
			});
			
			storeConfig.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					storeConfig();
				}
			});
			restoreConfig.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreConfig();
				}
			});
			showView.addMenuListener(new MenuListener() {
				public void menuSelected(MenuEvent arg0) {
					showView.removeAll();
					Iterator it = tablist_.keySet().iterator();
					for (; it.hasNext();) {
						Component c = tablist_.get(it.next());
						Container p = c.getParent();
						boolean b = false;
						if (p == null)
							b = false;
						else if (p instanceof JTabbedPane)
							b = true;
						else {
							for (Container i = p; i != null; i = i.getParent()) {
								if (i instanceof JDialog) {
									b = i.isVisible();
									break;
								}
							}
						}
						JCheckBoxMenuItem item = new JCheckBoxMenuItem(c.getName(), b);
						showView.add(item);
						item.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent arg0) {
								Component c = tablist_.get(arg0.getActionCommand());
								Container p = c.getParent();
								if (p == null) {
									rootPane_.insertTab(c, -1);
								} else if (p instanceof JTabbedPane) {
									p.remove(c);
									if (((JTabbedPane) p).getTabCount() == 0)
										((ElementPane) p.getParent()).removeUselessPane();
								} else {
									for (Container i = p; i != null; i = i.getParent()) {
										if (i instanceof JDialog) {
											i.setVisible(!i.isVisible());
											break;
										}
									}
								}
								rootPane_.setVisible(false);
								rootPane_.setVisible(true);
							}
						});
					}
				}
				public void menuDeselected(MenuEvent arg0) {}
				public void menuCanceled(MenuEvent arg0) {}
			});
		}
		return menu_;
	}
	
	public void setElement(Element configel) {
		configEl_ = configel;
		doc_ = configEl_.getOwnerDocument();
	}
	
	public void setStoreName(String storeName) {
		storeName_ = storeName;
	}
	
	public void setStoreFileName(String fname) {
		fname_ = fname;
	}
	
	public void storeConfig() {
		if (configEl_ != null && doc_ != null) {
			storeConfig(configEl_);
			if (fname_ != null)
				GrxXmlUtil.store(doc_, fname_);
		} else {
			Element configEl = null;
			NodeList configList = GrxXmlUtil.getPropertyElements(DEFAULT_TAG_NAME);
			for (int i = 0; i < configList.getLength(); i++) {
				configEl = (Element) configList.item(i);
				if (configEl.getAttribute("name").equals(storeName_))
					break;
				configEl = null;
			}
			if (configEl == null) {
				configEl = GrxXmlUtil.appendNewElement(null, DEFAULT_TAG_NAME, 1);
				configEl.setAttribute("name", storeName_);
			}
			storeConfig(configEl);
			GrxXmlUtil.store();
		} 
	}
	
	public void storeConfig(Element configEl) {
		if (configEl == null)
			return;
		
		if (focusedPane_ != null)
			focusedPane_.add(focusedTabPane_);
		
		Document doc = configEl.getOwnerDocument();
		Node n = configEl.getFirstChild();
		while (n != null) {
			Node next = n.getNextSibling();
			configEl.removeChild(n);
			n = next;
		}
	
		Dimension size = owner_.getSize();
		Point pos = owner_.getLocation();
		double ratio = roundValue(rootPane_.separatorPos_, 3);

		Element windowEl = GrxXmlUtil.appendNewElement(doc, configEl, "window", 3);
		GrxXmlUtil.setSize(windowEl, size);
		
		GrxXmlUtil.setInteger(windowEl, "x", pos.x);
		GrxXmlUtil.setInteger(windowEl, "y", pos.y);
		GrxXmlUtil.setBoolean(windowEl, "root", true);
		GrxXmlUtil.setBoolean(windowEl, "fullScreen", screen_.getFullScreenWindow() == owner_);

		Element layoutEl = GrxXmlUtil.appendNewElement(doc, windowEl, "layout", 4);
		layoutEl.setAttribute("position", "Center");
		layoutEl.setAttribute("splitratio", String.valueOf(ratio));

		_storeRecursively(doc, rootPane_, layoutEl, 4);

		for (int i = 0; i < windowList_.size(); i++) {
			Component c = windowList_.get(i);
			if (c != null) {
				JDialog jd = (JDialog) c.getParent().getParent().getParent().getParent();
				pos = jd.getLocation();
				windowEl = GrxXmlUtil.appendNewElement(doc, configEl, "window", 3);
				GrxXmlUtil.setSize(windowEl, jd.getSize());
				GrxXmlUtil.setInteger(windowEl, "x", pos.x);
				GrxXmlUtil.setInteger(windowEl, "y", pos.y);
				GrxXmlUtil.setBoolean(windowEl, "root", false);
				Element tabElement = GrxXmlUtil.appendNewElement(doc, windowEl, "tab", 4);
				tabElement.setAttribute("name", c.getName());
			}
		}

		if (focusedPane_ != null)
			GrxConfigPane.this.add(focusedTabPane_);
	}

	private void _storeRecursively(Document doc, ElementPane ep, Node node, int depth) {
		depth++;
		if (ep.tabbedPane_ != null) {
			for (int j = 3; j < ep.tabbedPane_.getComponentCount(); j++) {
				Component c = ep.tabbedPane_.getComponent(j);
				Element tabEl = GrxXmlUtil.appendNewElement(doc, node, "tab", depth);
				tabEl.setAttribute("name", c.getName());
				if (c == ep.tabbedPane_.getSelectedComponent())
					tabEl.setAttribute("select", "true");
			}
		} else {
			double ratio = roundValue(((ElementPane) ep.getComponent(0)).separatorPos_, 3);
			Element layoutEl = GrxXmlUtil.appendNewElement(doc, node,"layout", depth);
			layoutEl.setAttribute("position", "Center");
			layoutEl.setAttribute("splitratio", String.valueOf(ratio));
			_storeRecursively(doc, (ElementPane) ep.getComponent(0), layoutEl, depth);

			if (ep.getChildConstraint() != null) {
				ratio = roundValue(ep.getChildElement().separatorPos_, 3);
				layoutEl = GrxXmlUtil.appendNewElement(doc, node, "layout", depth);
				layoutEl.setAttribute("position", ep.getChildConstraint() .toString());
				layoutEl.setAttribute("splitratio", String.valueOf(ratio));
				_storeRecursively(doc, ep.getChildElement(), layoutEl, depth);
			}
		}
	}

	private double roundValue(double in, int digit) {
		BigDecimal bd = new BigDecimal(String.valueOf(in));
		return bd.setScale(digit, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public void restoreConfig() {
		if (configEl_ == null) {
			NodeList configList = GrxXmlUtil.getPropertyElements(DEFAULT_TAG_NAME);
			for (int i = 0; i < configList.getLength(); i++) {
				configEl_ = (Element) configList.item(i);
				if (configEl_.getAttribute("name").equals(storeName_))
					break;
				configEl_ = null;
			}
		}
		
		restoreConfig(configEl_);
	}
	
	public void restoreConfig(Element configEl) {
		if (configEl == null)
			return;
		
		removeAllTab();
		
		NodeList windowList = configEl.getElementsByTagName("window");
		for (int i = 0; i < windowList.getLength(); i++) {
			Element windowEl = (Element) windowList.item(i);
			int x = GrxXmlUtil.getInteger(windowEl, "x", 0);
			int y = GrxXmlUtil.getInteger(windowEl, "y", 0);
			Dimension d = GrxXmlUtil.getSize(windowEl, new Dimension(800, 600));

			if (GrxXmlUtil.getBoolean(windowEl, "root", false)) {
				Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
				if (d.width + x > screenDim.width) {
					x = screenDim.width - d.width;
					if (x < 0) {
						d.width = screenDim.width;
						x = 0;
					}
				}
				if (d.height + y > screenDim.height) {
					y = screenDim.height - d.height;
					if (y < 0) {
						d.height = screenDim.height;
						y = 0;
					}
				}
				owner_.setVisible(false);
				owner_.setLocation(x, y);
				owner_.setSize(d);
				owner_.setVisible(true);

				ElementPane ep = null;
				ep = _restoreRecursively(null, windowEl);
				ep.setSize(rootPane_.getSize());
				ep.setPreferredSize(rootPane_.getSize());
				remove(rootPane_);
				rootPane_ = ep;
				add(ep);
				_moveSeparatorRecursively(ep);
				//_moveSeparatorRecursively(ep);
			} else {
				NodeList tabList = windowEl.getElementsByTagName("tab");
				if (tabList != null && tabList.getLength() > 0) {
					String name = ((Element) tabList.item(0)).getAttribute("name");
					Component c = tablist_.get(name);
					if (c != null) {
						JDialog jd = createWindow(c);
						jd.setLocation(x, y);
						jd.setSize(d);
						jd.setVisible(true);
					}
				}
			}
			if (GrxXmlUtil.getBoolean(windowEl, "fullScreen", false)) {
				screen_.setFullScreenWindow(owner_);
			}
		}
		if (focusedPane_ != null) {
			focusedPane_.add(focusedTabPane_);
			focusedPane_ = null;
			focusedTabPane_ = null;
			rootPane_.setVisible(true);
		}
	}

	private ElementPane _restoreRecursively(ElementPane ep, Node node) {
		NodeList nList = node.getChildNodes();
		if (nList != null) {
			for (int i = 0; i < nList.getLength(); i++) {
				Node n = nList.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE) {
					_restoreRecursively(ep, n);
					continue;
				}
				Element e = (Element) n;

				if (e.getTagName().equals("layout")) {
					Double ratio = GrxXmlUtil.getDouble(e, "splitratio");
					if (ratio == null)
						ratio = 0.0;
					if (ep == null) {
						ep = new ElementPane();
						ep.separatorPos_ = ratio;
						_restoreRecursively(ep, e);
					} else {
						ElementPane newEp = new ElementPane();
						newEp.separatorPos_ = ratio;
						String str = e.getAttribute("position");
						if (str.equals("Center")) {
							ep.remove(0);
							ep.tabbedPane_ = null;
							ep.add(newEp, BorderLayout.CENTER, 0);
						} else if (str.equals("South")) {
							ep.add(newEp, BorderLayout.SOUTH, 1);
						} else if (str.equals("East")) {
							ep.add(newEp, BorderLayout.EAST, 1);
						} else if (str.equals("West")) {
							ep.add(newEp, BorderLayout.WEST, 1);
						}
						_restoreRecursively(newEp, e);
					}
				} else if (e.getTagName().equals("tab")) {
					Element tabElement = (Element) nList.item(i);
					String tabName = tabElement.getAttribute("name");
					Component c = tablist_.get(tabName);
					if (c != null) {
						ep.insertTab(c, -1);
						String select = tabElement.getAttribute("select");
						if (select.toLowerCase().equals("true"))
							ep.tabbedPane_.setSelectedComponent(c);
					}
				}
			}
		}
		return ep;
	}

	private void _moveSeparatorRecursively(ElementPane ep) {
		String c = (String) ep.getChildConstraint();
		if (c != null) {
			if (c.equals(BorderLayout.EAST) || c.equals(BorderLayout.WEST)) {
				ep.moveSeparatorX((int) (ep.separatorPos_ * ep.getWidth()), false);
			} else if (c.equals(BorderLayout.SOUTH)) {
				ep.moveSeparatorY((int) (ep.separatorPos_ * ep.getHeight()), false);
			}
			_moveSeparatorRecursively(ep.getChildElement());
			_moveSeparatorRecursively((ElementPane) ep.getComponent(0));
		} else {
			Component child = ep.getComponent(0);
			child.setSize(ep.getSize());
			if (child instanceof ElementPane)
				_moveSeparatorRecursively((ElementPane) child);
		}
	}

	public void insertTab(Component c, boolean isScroll, int position) {
		rootPane_.insertTab(c, isScroll, position);
	}

	public void clearAllTab() {
		removeAllTab();
		windowList_.clear();
		tablist_.clear();
		this.remove(rootPane_);
		rootPane_ = new ElementPane();
		this.add(rootPane_, BorderLayout.CENTER);
	}

	private void removeAllTab() {
		Iterator it = this.windowList_.iterator();
		for (; it.hasNext();) {
			Component c = (Component) it.next();
			Container p = c.getParent();
			p.remove(c);
			for (; p != null; p = p.getParent()) {
				if (p instanceof JDialog) {
					p.setVisible(false);
					break;
				}
			}
		}

		it = tablist_.values().iterator();
		for (; it.hasNext();) {
			Component c = (Component) it.next();
			Container p = c.getParent();
			if (p != null)
				p.remove(c);
		}
	}

	public void setConfigLock(boolean lock) {
		configLock_ = lock;
	}

	private JDialog createWindow(Component c) {
		JDialog jd = new JDialog(owner_);
		Container contentPane = jd.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(c);
		windowList_.add(c);
		jd.setTitle(c.getName());
		jd.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		jd.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				Component c = ((JDialog) e.getSource()).getContentPane().getComponent(0);
				rootPane_.insertTab(c, -1);
				windowList_.remove(c);
			}
		});
		jd.pack();

		return jd;
	}

	@SuppressWarnings("serial")
	private class ElementPane extends JPanel {
		JTabbedPane tabbedPane_ = null;
		String prevPos_ = BorderLayout.CENTER;
		double separatorPos_ = 0;
		
		public ElementPane() {
			super();
			initialize();
		}

		public ElementPane(Component c) {
			super();
			initialize();
			if (!(c instanceof JScrollPane)) {
				String name = c.getName();
				c = new JScrollPane(c);
				c.setName(name);
			}
			tabbedPane_.add(c);
		}

		public ElementPane(ElementPane org) {
			super();
			tabbedPane_ = org.tabbedPane_;
			org.remove(org.tabbedPane_);
			org.tabbedPane_ = null;
			initialize();
		}

		Object getConstraint() {
			return ((BorderLayout) getParent().getLayout())
					.getConstraints(ElementPane.this);
		}

		ElementPane getChildElement() {
			if (getComponentCount() > 1)
				return (ElementPane) getComponent(1);
			return null;
		}

		Object getChildConstraint() {
			ElementPane child = getChildElement();
			if (child != null)
				return child.getConstraint();
			return null;
		}

		private void initialize() {
			//setSize(300, 200);
			//setPreferredSize(new Dimension(300,200));
			setLayout(new BorderLayout(GAP, GAP));

			if (tabbedPane_ == null)
				tabbedPane_ = new JTabbedPane(JTabbedPane.TOP,
						JTabbedPane.SCROLL_TAB_LAYOUT);

			add(tabbedPane_, BorderLayout.CENTER);

			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent arg0) {
					Object c = getChildConstraint();
					if (c == null || configLock_)
						return;

					if (c.equals(BorderLayout.EAST) || c.equals(BorderLayout.WEST)) {
						moveSeparatorX(arg0.getX(), true);
					} else if (c.equals(BorderLayout.NORTH) || c.equals(BorderLayout.SOUTH)) {
						moveSeparatorY(arg0.getY(), true);
                    }
				}
			});

			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent arg0) {
					Object c = getChildConstraint();
					if (c == null || configLock_)
						return;
					if (c.equals(BorderLayout.EAST)
							|| c.equals(BorderLayout.WEST))
						setCursor(Cursor
								.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
					else if (c.equals(BorderLayout.NORTH)
							|| c.equals(BorderLayout.SOUTH))
						setCursor(Cursor
								.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
				}

				public void mouseReleased(MouseEvent arg0) {
					if (!configLock_)
						setCursor(Cursor.getDefaultCursor());
				}

				public void mouseExited(MouseEvent arg0) {
					if (!configLock_)
						setCursor(Cursor.getDefaultCursor());
				}
			});

			tabbedPane_.addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent arg0) {
					if (configLock_)
						return;

					String currPos = "None";
					if (directedPane_ != null && selectedPane_ != null) {
						int x = arg0.getX();
						int y = arg0.getY();
						int dx = directedPane_.getWidth();
						int dy = directedPane_.getHeight();
						if (0 < x && x < dx * 0.1 && WINDOW_MIN_HEIGHT < y
								&& y < dy) {
							directedPane_.drawRect(0, 0, dx / 2, dy);
						} else if (dx * 0.9 < x && x < dx
								&& WINDOW_MIN_HEIGHT < y && y < dy) {
							currPos = BorderLayout.EAST;
							directedPane_.drawRect(dx / 2, 0, dx / 2, dy);
						} else if (dy * 0.9 < y && y < dy && 0 < x && x < dx) {
							currPos = BorderLayout.SOUTH;
							directedPane_.drawRect(0, dy / 2, dx, dy);
						} else {
							currPos = BorderLayout.CENTER;
							directedPane_.drawRect(0, 0, dx, dy);
						}
					} else if (GrxConfigPane.this.rootSwapDirection_ != null) {
						int rootDx = rootPane_.getWidth();
						int rootDy = rootPane_.getHeight();
						if (GrxConfigPane.this.rootSwapDirection_
								.equals(BorderLayout.WEST)) {
							currPos = BorderLayout.WEST;
							rootPane_.drawRect(0, 0, rootDx / 4, rootDy);
						} else if (GrxConfigPane.this.rootSwapDirection_
								.equals(BorderLayout.EAST)) {
							currPos = BorderLayout.EAST;
							rootPane_.drawRect(rootDx * 3 / 4, 0, rootDx / 4,
									rootDy);
						} else if (GrxConfigPane.this.rootSwapDirection_
								.equals(BorderLayout.NORTH)) {
							currPos = BorderLayout.NORTH;
							rootPane_.drawRect(0, 0, rootDx, rootDy / 4);
						} else if (GrxConfigPane.this.rootSwapDirection_
								.equals(BorderLayout.SOUTH)) {
							currPos = BorderLayout.SOUTH;
							rootPane_.drawRect(0, rootDy * 3 / 4, rootDx,
									rootDy / 4);
						}
					} else {
						repaint();
						return;
					}
					if (!prevPos_.equals(currPos)) {
						setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
						prevPos_ = currPos;
						repaint();
					}
				}
			});
			tabbedPane_.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent evt) {
					if (evt.getClickCount() == 2 && evt.getY() < 20) {
						focusedTabPane_ = (JTabbedPane) evt.getSource();
						focusedTabPane_.setVisible(false);
						if (focusedPane_ == null) {
							focusedPane_ = (ElementPane) focusedTabPane_.getParent();
							rootPane_.setVisible(false);
							GrxConfigPane.this.add(focusedTabPane_);
							focusedTabPane_.setForeground(java.awt.Color.red);
						} else {
							focusedPane_.add(focusedTabPane_);
							focusedPane_ = null;
                            GrxConfigPane.this.add(rootPane_);
							rootPane_.setVisible(true);
							focusedTabPane_.setForeground(java.awt.Color.black);
						}
						focusedTabPane_.setVisible(true);
					} /*else if (evt.getButton() == MouseEvent.BUTTON3 && 
						evt.getClickCount() == 1 && evt.getY() < 20) {
						JMenu test = new JMenu();
						test.add(new JMenuItem("hide"));
						test.getPopupMenu().show(evt.getComponent(), evt.getX(),evt.getY());
					}*/
				}

				public void mousePressed(MouseEvent arg0) {
					Component c = arg0.getComponent().getParent();
					if (c instanceof ElementPane && arg0.getY() < 20)
						selectedPane_ = directedPane_ = (ElementPane) c;
				}

				public void mouseReleased(MouseEvent arg0) {
					if (selectedPane_ == null || configLock_) {
						directedPane_ = null;
						return;
					}
					setCursor(Cursor.getDefaultCursor());
					if (rootSwapDirection_ != null) {
						ElementPane ep = new ElementPane(selectedPane_
								.getSelectedTab());
						ElementPane newRoot = new ElementPane();
						newRoot.remove(0);
						newRoot.tabbedPane_ = null;

						Dimension dim = rootPane_.getSize();
						newRoot.setPreferredSize(dim);
						newRoot.setSize(dim);
						GrxConfigPane.this.remove(rootPane_);
						GrxConfigPane.this.add(newRoot, BorderLayout.CENTER, 0);

						if (rootSwapDirection_.equals(BorderLayout.NORTH)) {
							newRoot.add(ep, BorderLayout.CENTER, 0);
							newRoot.add(rootPane_, BorderLayout.SOUTH, 1);
							newRoot.moveSeparatorY(dim.height / 4, true);
						} else if (rootSwapDirection_
								.equals(BorderLayout.SOUTH)) {
							newRoot.add(rootPane_, BorderLayout.CENTER, 0);
							newRoot.add(ep, BorderLayout.SOUTH, 1);
							newRoot.moveSeparatorY(dim.height * 3 / 4, true);
						} else if (rootSwapDirection_.equals(BorderLayout.WEST)) {
							newRoot.add(rootPane_, BorderLayout.CENTER, 0);
							newRoot.add(ep, BorderLayout.WEST, 1);
							newRoot.moveSeparatorX(dim.width / 4, true);
						} else if (rootSwapDirection_.equals(BorderLayout.EAST)) {
							newRoot.add(rootPane_, BorderLayout.CENTER, 0);
							newRoot.add(ep, BorderLayout.EAST, 1);
							newRoot.moveSeparatorX(dim.width * 3 / 4, true);
						}
						rootPane_ = newRoot;
					} else if (directedPane_ != null
							&& (selectedPane_ != directedPane_ || selectedPane_.tabbedPane_
									.getTabCount() > 1)) {
						int x = arg0.getX();
						int y = arg0.getY();
						int dx = directedPane_.getWidth();
						int dy = directedPane_.getHeight();
						if (0 < x && x < dx * 0.1 && WINDOW_MIN_HEIGHT < y
								&& y < dy) {
							ElementPane ep = new ElementPane(selectedPane_
									.getSelectedTab());
							Dimension dim = new Dimension(dx / 2, dy);
							ep.setPreferredSize(dim);
							directedPane_.add(ep, BorderLayout.WEST, 1);

							ep = new ElementPane(directedPane_);
							ep.setPreferredSize(dim);
							directedPane_.add(ep, BorderLayout.CENTER, 0);
							directedPane_.separatorPos_ = 0.5;
						} else if (dx * 0.9 < x && x < dx
								&& WINDOW_MIN_HEIGHT < y && y < dy) {
							ElementPane ep = new ElementPane(selectedPane_
									.getSelectedTab());
							Dimension dim = new Dimension(dx / 2, dy);
							ep.setPreferredSize(dim);
							directedPane_.add(ep, BorderLayout.EAST, 1);

							ep = new ElementPane(directedPane_);
							ep.setPreferredSize(dim);
							directedPane_.add(ep, BorderLayout.CENTER, 0);
							directedPane_.separatorPos_ = 0.5;
						} else if (dy * 0.9 < y && y < dy && 0 < x && x < dx) {
							ElementPane ep = new ElementPane(selectedPane_
									.getSelectedTab());
							Dimension dim = new Dimension(dx, dy / 2);
							ep.setPreferredSize(dim);
							directedPane_.add(ep, BorderLayout.SOUTH, 1);

							ep = new ElementPane(directedPane_);
							ep.setPreferredSize(dim);
							directedPane_.add(ep, BorderLayout.CENTER, 0);
							directedPane_.separatorPos_ = 0.5;
						} else {
							if (directedPane_.getSelectedTab() != selectedPane_.getSelectedTab())
								directedPane_.insertTab(selectedPane_.getSelectedTab(), -1);
							else if (y > 20)
								directedPane_.insertTab(selectedPane_.getSelectedTab(), 0);
						}
					} else {
						Point p = MouseInfo.getPointerInfo().getLocation();
						java.awt.Frame f = GrxGuiUtil.getParentFrame(ElementPane.this);
						if (f != null && !f.getBounds().contains(p)) {
							Component c = selectedPane_.getSelectedTab();
							if (c != null)
								createWindow(c).setVisible(true);
						}
					}

					selectedPane_.removeUselessPane();
					repaint();
					selectedPane_ = null;
				}

				public void mouseEntered(MouseEvent arg0) {
					Component c = arg0.getComponent().getParent();
					if (c instanceof ElementPane)
						directedPane_ = (ElementPane) c;
				}

				public void mouseExited(MouseEvent evt) {
					Rectangle r = ((JComponent) evt.getSource()).getBounds();
					if (evt.getX() <= 0 || r.width <= evt.getX()
							|| evt.getY() <= 0 || r.height <= evt.getY()) {
						repaint();
						directedPane_ = null;
					}
				}
			});
		}

		void removeUselessPane() {
			if (tabbedPane_ == null || tabbedPane_.getTabCount() > 0)
				return;

			Container p = this.getParent();
			if (!(p instanceof ElementPane))
				return;
			p.remove(this);
			if (p.getComponentCount() > 0) {
				if (!(p.getComponent(0) instanceof ElementPane))
					return;
				Container gp = p.getParent();
				Object cons = ((BorderLayout) gp.getLayout()).getConstraints(p);
				gp.remove(p);
				ElementPane ep = (ElementPane) p.getComponent(0);
				if (cons.equals("Center"))
					gp.add(ep, cons, 0);
				else
					gp.add(ep, cons);
				if (p == rootPane_)
					rootPane_ = ep;
			} else {
				if (p instanceof ElementPane)
					((ElementPane) p).removeUselessPane();
			}
		}

		boolean moveSeparatorX(int x, boolean updatePos) {
			int width = ElementPane.this.getWidth();
			if (x < WINDOW_MIN_WIDTH)
				x = WINDOW_MIN_WIDTH;
			else if (width - WINDOW_MIN_WIDTH < x)
				x = width - WINDOW_MIN_WIDTH;

			Component[] clist = getComponents();
			if (clist.length < 2) 
                return true;

			for (int i = 0; i < clist.length; i++) {
				if (!(clist[i] instanceof ElementPane)) 
                    continue;

				ElementPane ep = (ElementPane) clist[i];
				Object cons = ep.getChildConstraint();
				if (cons == null) 
                    continue;

				if (cons.equals("WEST")) {
					ep.moveSeparatorX((int) ((width - x - GAP) * ep.separatorPos_), false);
				} else if (cons.equals("EAST")) {
					ep.moveSeparatorX((int) ((x - GAP / 2) * ep.separatorPos_), false);
				} else if (cons.equals("SOUTH")) {
					ep.moveSeparatorY((int) (ElementPane.this.getHeight() * ep.separatorPos_), false);
                }
			}

			Dimension dim = ElementPane.this.getSize();
			this.setVisible(false);
			if (getChildConstraint().equals(BorderLayout.WEST)) {
				dim.width = width - x - GAP;
				clist[0].setPreferredSize(dim);
				clist[0].setSize(dim);
				dim.width = x - GAP / 2;
				clist[1].setPreferredSize(dim);
				clist[1].setSize(dim);
			} else {
				dim.width = x - GAP / 2;
				clist[0].setPreferredSize(dim);
				clist[0].setSize(dim);
				dim.width = width - x - GAP;
				clist[1].setPreferredSize(dim);
				clist[1].setSize(dim);
			}
			this.setVisible(true);
            if (updatePos)
				separatorPos_ = (double) x / width;

			return true;
		}

		int moveSeparatorY(int y, boolean updatePos) {
			int height = ElementPane.this.getHeight();
			if (y < WINDOW_MIN_HEIGHT)
				y = WINDOW_MIN_HEIGHT;
			else if (height - WINDOW_MIN_HEIGHT < y)
				y = height - WINDOW_MIN_HEIGHT;

			Component[] clist = this.getComponents();
			if (clist.length < 2) 
                return ElementPane.this.getHeight();

			for (int i = 0; i < clist.length; i++) {
				if (clist[i] instanceof ElementPane) {
					ElementPane ep = (ElementPane) clist[i];
					Object cons = ep.getChildConstraint();
					if (cons != null && cons.equals("SOUTH"))
						ep.moveSeparatorY((int) (y * ep.separatorPos_), false);
				}
			}

			if (this.getChildConstraint().equals(BorderLayout.SOUTH)) {
				Dimension dim = ElementPane.this.getSize();
				dim.height = y - GAP / 2;
				this.setVisible(false);
				clist[0].setPreferredSize(dim);
				clist[0].setSize(dim);
				dim.height = height - y - GAP;
				clist[1].setPreferredSize(dim);
				clist[1].setSize(dim);
				this.setVisible(true);
                   if (updatePos)
					separatorPos_ = y / (double) height;
			}

			return ElementPane.this.getHeight();
		}

		public void insertTab(Component c, boolean isScroll, int position) {
			if (!(c instanceof JScrollPane)) {
				String name = c.getName();
				if (isScroll) {
					c = new JScrollPane(c,
							JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
							JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				} else {
					c = new JScrollPane(c,
							JScrollPane.VERTICAL_SCROLLBAR_NEVER,
							JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					c.addComponentListener(new ComponentAdapter() {
						public void componentResized(ComponentEvent evt) {
							JScrollPane p = (JScrollPane) evt.getSource();
							Dimension d = p.getSize();
							p.getViewport().setViewSize(d);
							d.height -= (d.height > 2) ? 2 : 0;
							d.width -= (d.width > 2) ? 2 : 0;
							p.getViewport().getView().setPreferredSize(d);
						}
					});
				}
				c.setName(name);
			}
			if (tabbedPane_ != null) {
				int tabCount = tabbedPane_.getTabCount();
				if (position < 0 || tabCount < position)
					position = tabCount;
				tabbedPane_.add(c, position);
        if (position == 0)
				  tabbedPane_.setSelectedComponent(c);
			} else {
				((ElementPane) getComponents()[0]).insertTab(c, isScroll, position);
			}
			if (!tablist_.containsValue(c))
				tablist_.put(c.getName(), c);
		}

		public void insertTab(Component c, int position) {
			insertTab(c, true, position);
		}

		Component getSelectedTab() {
			if (tabbedPane_ != null)
				return tabbedPane_.getSelectedComponent();
			return null;
		}

		private void drawRect(int x, int y, int w, int h) {
			Graphics2D g2 = (Graphics2D) getGraphics();
			BasicStroke b = new BasicStroke(4.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.0F }, 0.0f);
			g2.setStroke(b);
			g2.drawRect(x, y, w, h);
			b = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.0F }, 0.0f);
			g2.setStroke(b);
		}
	}
}

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
 *  GrxSeqEditView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.go.aist.hrp.simulator.*;

import com.generalrobotix.ui.*;
import com.generalrobotix.ui.util.*;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxWorldStateItem;
import com.generalrobotix.ui.item.GrxModelItem.LinkInfoLocal;
import com.generalrobotix.ui.item.GrxWorldStateItem.WorldStateEx;
import com.generalrobotix.ui.shrpsys.item.GrxSockPortItem;
import com.generalrobotix.test.Interpolator;

@SuppressWarnings("serial")
public class GrxSeqEditView extends GrxBaseView {
	private GrxModelItem currentModel_ = null;
	private GrxSockPortItem currentPort_ = null;
	private File currentFile_ = null;
	
	private JMenu menu_ = null;
	private JMenuItem capture_ = new JMenuItem("Capture");
	private JMenuItem wait_    = new JMenuItem("Wait");
	private JMenuItem paste_   = new JMenuItem("Paste");
	private JMenuItem clear_   = new JMenuItem("Clear All");
	private JMenuItem play_    = new JMenuItem("Play");
	private JMenuItem load_    = new JMenuItem("Load");
	private JMenuItem save_    = new JMenuItem("Save");
	private JMenuItem saveAsHrpsys_ = new JMenuItem("Save as hrpsys format");
	private JMenuItem interpolate_ = new JMenuItem("Interpolate");
	
	private JFileChooser fchooser_;
	private JLabel instructionMsg_ = new JLabel("Right Click Here to Start Editing Sequence.");
	private FunctionPanel selectedFunction_;
	private FunctionPanel buffer_;
	private Vector<FunctionPanel> playList_ = new Vector<FunctionPanel>();
	
	private static final String FORMAT1 = "%.1f ";
	private static final KeyStroke KS_LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
	private static final KeyStroke KS_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
	private static final KeyStroke KS_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
	private static final KeyStroke KS_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
	
	private static final String preHrpsysScript = "send seq :joint-angles ";
	private static final String prePythonScript = "seq.sendMsg(':joint-angles ";
	private static final String postHrpsysScript = "\nsend seq :wait-interpolation\n";
	private static final String postPythonScript = "')\n  seq.sendMsg(':wait-interpolation')\n";
	
	public GrxSeqEditView(String name, GrxPluginManager manager) {
		super(name, manager);

		JPanel contentPane = getContentPane();
		contentPane.setLayout(new FlowLayout());
		contentPane.setName("Sequence Editor");
		contentPane.setBackground(Color.white);
		instructionMsg_.setForeground(Color.LIGHT_GRAY);
		instructionMsg_.setFont(new Font("Monospaced", Font.BOLD, 20));
		contentPane.add(instructionMsg_);
		contentPane.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
					paste_.setEnabled(buffer_ != null);
					getMenu().getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	public JMenu getMenu() {
		if (menu_ == null) {
			menu_ = new JMenu("Sequence");
			
			paste_.setEnabled(false);
			menu_.add(capture_);
			menu_.add(wait_);
			menu_.add(paste_);
			menu_.add(clear_);
			menu_.addSeparator();
			menu_.add(play_);
			menu_.addSeparator();
			menu_.add(load_);
			menu_.add(save_);
			menu_.add(saveAsHrpsys_);
			menu_.add(interpolate_);
			
			capture_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					capture(getContentPane().getComponentCount());
				}
			});
			
			wait_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					createWait(getContentPane().getComponentCount());
				}
			});

			paste_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					paste(-1);
				}
			});

			play_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					play(0);
				}
			});

			save_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					saveMotion();
				}
			});

			load_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					loadMotion();
				}
			});

			saveAsHrpsys_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					saveAsHrpsys();
				}
			});

			clear_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					clearAll();
				}
			});
			
			interpolate_.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
				    interpolate();	
				}
			});
		}
		return menu_;
	}
	
	private void capture(int pos) {
		if (currentModel_ != null)
			insert(new PosturePanel(newName(), currentModel_.getJointValues()), pos);
	}
	
	private void createWait(int pos) {
		insert(new WaitPanel(), pos);
	}
	
	private void paste(int pos) {
		if (buffer_ instanceof PosturePanel)
			insert(new PosturePanel((PosturePanel)buffer_), pos);
		else if (buffer_ instanceof WaitPanel)
			insert(new WaitPanel((WaitPanel)buffer_), pos);
	}
	
	private void insert(FunctionPanel p, int pos) {
		if (p != null) {
			Container c = instructionMsg_.getParent();
			if (c != null) {
				c.remove(instructionMsg_);
				pos = 0;
			}
			getContentPane().setVisible(false);
			if (pos < 0)
				pos = getContentPane().getComponentCount();
			getContentPane().add(p, pos);
			getContentPane().setVisible(true);
			getContentPane().scrollRectToVisible(p.getBounds());
			p.setSelected(true);
		}
	}
	
	private void copy() {
		buffer_ = selectedFunction_;
	}

	private void cut() {
		buffer_ = selectedFunction_;
		delete();
	}

	private void delete() {
		Container c = selectedFunction_.getParent();
		c.setVisible(false);
		c.remove(selectedFunction_);
		c.setVisible(true);
	}
	
	private void setEnabled(boolean b) {
		Component[] c = getContentPane().getComponents();
		for (int i=0; i<c.length; i++)
			c[i].setEnabled(b);
	}

	private void play(int index) {
		Component[] comps = getContentPane().getComponents();
		for (int i=index; i < comps.length; i++) {
			if (comps[i] instanceof FunctionPanel)
				playList_.add((FunctionPanel)comps[i]);
		}
	}
	
    private void interpolate() {
		Component[] comps = getContentPane().getComponents();
		int dof = currentModel_.getJointValues().length;
		double[] t = new double[comps.length+1];
		double[][] x = new double[dof][comps.length+1];
		int[] m = new int[comps.length+1];
		t[0] = 0.0;
		
		{
			double[] jv = currentModel_.getJointValues();
		    for (int j=0; j<dof; j++)
		        x[j][0] = jv[j];
			m[0] = Interpolator.METHOD_SPLINE;
		}
		
		for (int i=1; i < comps.length+1; i++) {
			Component c = comps[i-1];
			if (c instanceof PosturePanel) {
				PosturePanel pp = (PosturePanel)c;
                t[i] = t[i-1] + pp.time_;
				for (int j=0;j<dof; j++)
				    x[j][i] = pp.angles_[j];
				m[i] = Interpolator.METHOD_SPLINE;
			} else if (c instanceof FunctionPanel) {
				FunctionPanel fp = (FunctionPanel)c;
				t[i] = t[i-1] + fp.time_;
				for (int j=0;j<dof; j++)
				    x[j][i] = x[j][i-1];
				m[i-1] = Interpolator.METHOD_LINEAR;
				m[i] = Interpolator.METHOD_LINEAR;
			}
		}
		GrxWorldStateItem item = (GrxWorldStateItem)manager_.getSelectedItem(GrxWorldStateItem.class, null);
		Interpolator ips[] = new Interpolator[dof];
		for (int i=0; i<dof ;i++)
		  ips[i] = new Interpolator(t, x[i], m);

		item.clearLog();
		item.registerCharacter(currentModel_.getName(), currentModel_.cInfo_);
		double dt = item.getDbl("logTimeStep", 0.005);
		double totaltime = t[t.length-1];
		for (double d=0; d<=totaltime; d+=dt) {
			double val[] = new double[dof];
			for (int i=0; i<dof; i++)
				val[i] = ips[i].calc(d);

			SensorState ss = new SensorState();
			ss.q = val;
            ss.u = new double[dof];
            for (int i=0; i<dof; i++) 
                ss.u[i] = 0.0;
            ss.force = new double[0][];
            ss.rateGyro = new double[0][];
            ss.accel = new double[0][];

			WorldState ws = new WorldState();
			ws.time = d;
			ws.characterPositions = new CharacterPosition[1];
			ws.characterPositions[0] = new CharacterPosition();
			ws.characterPositions[0].characterName = currentModel_.getName();
			ws.characterPositions[0].linkPositions = new LinkPosition[dof];

			currentModel_.setJointValues(val);
			currentModel_.calcForwardKinematics();
			for (int i=0; i<dof; i++) {
				double[] ret = currentModel_.getTransformArray(currentModel_.lInfo_[i].name);
				ws.characterPositions[0].linkPositions[i] = new LinkPosition();
				ws.characterPositions[0].linkPositions[i].p = new double[3];
				ws.characterPositions[0].linkPositions[i].R = new double[9];
				for (int j=0; j<3; j++)
					ws.characterPositions[0].linkPositions[i].p[j] = ret[j];
				
				for (int j=0; j<9; j++)
					ws.characterPositions[0].linkPositions[i].R[j] = ret[j+3];
			}

			WorldStateEx wsx = new WorldStateEx();
			wsx.time = d;
			wsx.setWorldState(ws);
			wsx.setSensorState(currentModel_.getName(), ss);
			item.addValue(d, wsx);
		}
    }

	private void saveMotion() {
		if (currentFile_ == null)
			currentFile_ = newFile("newpose", "pose");

		if (fchooser_ == null)
			fchooser_ = GrxGuiUtil.createFileChooser("Save Motion", null, "pose");
		fchooser_.setSelectedFile(currentFile_);

		File f = null;
		if (fchooser_.showSaveDialog(manager_.getFrame()) == JFileChooser.APPROVE_OPTION)
			f = fchooser_.getSelectedFile();
		else
			return;

		if (!f.getName().endsWith(".pose"))
			f = new File(f.getName() + ".pose");
		try {
			FileWriter fw = new FileWriter(f);
			Component[] comps = getContentPane().getComponents();
			for (int i=0; i<comps.length; i++) {
				if ((comps[i] instanceof FunctionPanel))
					fw.append(((FunctionPanel)comps[i]).getString4Save());
			}
			fw.close();
			currentFile_ = f;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveAsHrpsys() {
		File f = null;
		if (currentFile_ == null) {
			f = newFile("newpose", "py");
		} else {
			String str = currentFile_.getName();
			if (str.endsWith(".pose")) 
				str = str.substring(0, str.length() - 4);
			f = new File(str+"py");
		}
		
		File dir = new File("script");
		if (!dir.exists())
			dir = null;
		JFileChooser fc = GrxGuiUtil.createFileChooser("Save as hrpsys format", dir, "py");
		fc.setSelectedFile(f);
		if (fc.showSaveDialog(manager_.getFrame()) == JFileChooser.APPROVE_OPTION)
			f = fc.getSelectedFile();
		else
			return;
		try {
			String baseName = f.getName();
			if (baseName.endsWith(".py"))
				baseName = baseName.substring(0, baseName.length() - 3);
			else
				f = new File(f.getAbsolutePath() + ".py");

			FileWriter fw = new FileWriter(f);
			fw.append("def " + baseName + "(seq=None):\n");
			fw.append("  if seq == None:\n");
			fw.append("    return\n");
			Component[] comps = getContentPane().getComponents();
			for (int i=0; i<comps.length; i++) {
				if ((comps[i] instanceof FunctionPanel)) {
					fw.append("  # "+((FunctionPanel)comps[i]).getName()+"\n");
					fw.append(((FunctionPanel)comps[i]).getPythonScript());
				}
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String newName() {
		Component[] comps = getContentPane().getComponents();
		String name = null;
		for (int i=0; ; i++) {
			name = "Posture"+i;
			int j=0;
			for (; j<comps.length; j++) {
				if (name.equals(comps[j].getName()))
					break;
			}
			if (j == comps.length)
				break;
		}
		return name;
	}
	
	private File newFile(String base, String ext) {
		File f = null;
		for (int i = 0;; i++) {
			f = new File(base + i + "." +ext);
			if (!f.exists())
				break;
		}
		return f;
	}
	
	private void loadMotion() {
		if (currentModel_ == null)
			return;
		
		File f = null;
		if (fchooser_ == null)
			fchooser_ = GrxGuiUtil.createFileChooser("Load Motion", new File(""), "pose");
		fchooser_.setSelectedFile(null);
		if (fchooser_.showOpenDialog(manager_.getFrame()) == JFileChooser.APPROVE_OPTION)
			f = fchooser_.getSelectedFile();
		else
			return;

		try {
			BufferedReader  br = new BufferedReader(new FileReader(f));
			StreamTokenizer st = new StreamTokenizer(br);
			st.wordChars('_', '_');
			st.wordChars(':', ':');
			st.eolIsSignificant(true);
			st.whitespaceChars(' ', ' ');
			//st.commentChar('#');
			
			List<FunctionPanel> plist = new ArrayList<FunctionPanel>();
			int dof = currentModel_.getJointValues().length;
			for (; st.ttype != StreamTokenizer.TT_EOF; st.nextToken()) {
				if (st.ttype != StreamTokenizer.TT_WORD)
					continue;
				if (st.sval.equalsIgnoreCase("POSTURE:")) {
					String name = new String();
					while (true) {
						st.nextToken();
						if (st.ttype == StreamTokenizer.TT_EOL)
							break;
						name += st.sval;
					}
					
					double[] values = new double[dof];
					double time = 1.0;
					for (int i=0; i<dof+1; i++) {
						st.nextToken();
						if (st.ttype == StreamTokenizer.TT_NUMBER) {
							if (i < dof)
								values[i] = Math.toRadians(st.nval);
							else 
								time = st.nval;
						} else  {
							br.close();
							return;
						}
					}
					plist.add(new PosturePanel(name, values, time));
				} else if (st.sval.equalsIgnoreCase("WAIT:")) {
					while (st.ttype != StreamTokenizer.TT_EOL) {
						st.nextToken();
					}
					st.nextToken();
					if (st.ttype == StreamTokenizer.TT_NUMBER && st.nval > 0) {
						System.out.print("nval:"+st.nval);
						plist.add(new WaitPanel(st.nval));
					} else {
						while (st.ttype != StreamTokenizer.TT_EOL) {
							st.nextToken();
						}
					}
				}
			}
			br.close();
			currentFile_ = f;
			
			getContentPane().removeAll();
			for (int i=0; i<plist.size(); i++)
				insert(plist.get(i), -1);
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}

	private void clearAll() {
		int ans = JOptionPane.showConfirmDialog(manager_.getFrame(),
			"Remove all postures ?", "",JOptionPane.OK_CANCEL_OPTION);
		if (ans == JOptionPane.OK_OPTION) {
			getContentPane().setVisible(false);
			getContentPane().removeAll();
			getContentPane().setVisible(true);
			currentFile_ = null;
		}
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		//if (!itemList.contains(currentModel_)) {
			currentModel_ = null;
			Iterator<GrxBaseItem> it = itemList.iterator();
			while (it.hasNext()) {
				GrxBaseItem item = it.next();
				if (item instanceof GrxModelItem)
					currentModel_ = (GrxModelItem) item;
				else if (item instanceof GrxSockPortItem)
					currentPort_ = (GrxSockPortItem)item;
			}
		//}
	}
	
	private Date timer_;
	public void control(List<GrxBaseItem> list) {
		if (playList_.size() > 0) {
			if (timer_ == null) {
				setEnabled(false);
				FunctionPanel fp = playList_.get(0);
				JComponent c = getContentPane();
				/*int next = c.getComponentZOrder(fp);
				if (next < c.getComponentCount()) {
					Rectangle r = c.getComponent(next).getBounds();
				    c.scrollRectToVisible(r);
				}*/
				c.scrollRectToVisible(fp.getBounds());
				
				timer_ = new Date();
				timer_.setTime(timer_.getTime() + (long)fp.time_*1000);
				fp.setSelected(true);
				fp.execButton();
			} else if (new Date().after(timer_)) {
				timer_ = null;
				playList_.remove(0);
				if (playList_.size() == 0)
					setEnabled(true);
			}
		}
	}
	
	private interface Function {
		public void exitButton();
		public void enterButton();
		public void execButton();
		public String getHrpsysScript();
		public String getPythonScript();
		public String getString4Save();
	}

	private abstract class FunctionPanel extends JPanel implements Function{
		protected double time_ = 1.0;  // [sec]
		protected JButton  button_ = new JButton();
		protected JSpinner spinner_ = new JSpinner();
		protected Color buttonColor = button_.getBackground();
		
		protected JMenu buttonMenu  = new JMenu();
		protected JMenuItem capture = new JMenuItem("Capture");
		protected JMenuItem rename  = new JMenuItem("Rename");
		protected JMenuItem copy    = new JMenuItem("Copy");
		protected JMenuItem cut     = new JMenuItem("Cut");
		protected JMenuItem paste   = new JMenuItem("Paste");
		protected JMenuItem delete  = new JMenuItem("Delete");
		protected JMenuItem playfrom= new JMenuItem("Play from Here");
		
		private FunctionPanel() {}
		
		public FunctionPanel(String name, double time) {
			setName(name);
			setTime(time);
			
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBackground(Color.white);
			add(button_);
			add(spinner_);
			
			button_.setAlignmentX(JButton.CENTER_ALIGNMENT);

			button_.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent arg0) {
					KeyStroke ks = KeyStroke.getKeyStrokeForEvent(arg0);
					if (ks == KS_UP) {
						spinner_.setValue(spinner_.getNextValue());
					} else if (ks == KS_DOWN) {
						spinner_.setValue(spinner_.getPreviousValue());
					} else {
						FunctionPanel p = FunctionPanel.this;
						Container c = p.getParent();
						int order = c.getComponentZOrder(p);
						if (order > 0 && ks == KS_LEFT) {
							((FunctionPanel) c.getComponent(order-1)).setSelected(true);
						} else if (order<c.getComponentCount()-1 && ks == KS_RIGHT) {
							((FunctionPanel) c.getComponent(order+1)).setSelected(true);
						}
					}
				}
			});

			button_.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					playList_.add(FunctionPanel.this);
				}
			});
			button_.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					enterButton();
				}
				public void mouseExited(MouseEvent e) {
					exitButton();
				}
			});		
			
			spinner_.setModel(new SpinnerNumberModel(
				new Double(time_), new Double(0.19), new Double(1000.0), new Double(0.1)));
			spinner_.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					time_ = ((Number)spinner_.getValue()).doubleValue();
				}
			});
			
			MouseAdapter ma = new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (!javax.swing.SwingUtilities.isRightMouseButton(e)) 
						return;
					FunctionPanel.this.setSelected(true);
					paste.setEnabled(buffer_ != null);
					buttonMenu.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
				}
			};
			button_.addMouseListener(ma);
			spinner_.addMouseListener(ma);
			
			buttonMenu.add(capture);		
			buttonMenu.add(rename);
			buttonMenu.add(copy);
			buttonMenu.add(cut);
			buttonMenu.add(paste);
			buttonMenu.add(delete);
			buttonMenu.addSeparator();
			buttonMenu.add(playfrom);
			
			copy.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					copy();
				}
			});

			cut.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int ans = JOptionPane.showConfirmDialog(getContentPane(),
						"Cut this posture?", "Cut Posture",
						JOptionPane.OK_CANCEL_OPTION);
					if (ans == JOptionPane.OK_OPTION) 
						cut();
				}
			});

			paste.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int ans = JOptionPane.showConfirmDialog(
						FunctionPanel.this, 
						"Paste Posture before " + getName() + " ?",
						"Paste Posture", JOptionPane.OK_CANCEL_OPTION);
					if (ans == JOptionPane.OK_OPTION) 
						paste(getContentPane().getComponentZOrder(FunctionPanel.this));
				}
			});

			delete.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int ans = JOptionPane.showConfirmDialog(getContentPane(),
						"Delete this posture?", "Delete Posture",
						JOptionPane.OK_CANCEL_OPTION);
					if (ans == JOptionPane.OK_OPTION)
						delete();
				}
			});

			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					String name = JOptionPane.showInputDialog(
						getContentPane(), "Input Name", button_.getText());
					if (name != null)
						setName(name);
				}
			});

			capture.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					int ans = JOptionPane.showConfirmDialog(FunctionPanel.this, 
						"Capture Posture before " + button_.getText() + " ?",
						"Capture Posture", JOptionPane.OK_CANCEL_OPTION);
					if (ans == JOptionPane.OK_OPTION) 
						capture(getContentPane().getComponentZOrder(FunctionPanel.this));
				}
			});

			playfrom.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					play(getContentPane().getComponentZOrder(FunctionPanel.this));
				}
			});
		}
		
		public void setName(String name) {
			super.setName(name);
			button_.setText(name);
		}

		private void setTime(double time) {
			if (time > 0) {
				time_ = time;
				spinner_.setValue(new Double(time_));
			}
		}
		
		public void setEnabled(boolean b) {
			button_.setEnabled(b);
			spinner_.setEnabled(b);
		}
		
		protected void setSelected(boolean b) {
			if (b && selectedFunction_ != null)
				selectedFunction_.setSelected(false);
			
			button_.setBackground(b ? Color.yellow : buttonColor);
			button_.requestFocus();
			selectedFunction_ = this;
		}
		
		protected String getTimeString(){
			return String.format(FORMAT1, time_);	
		}
	}
	
	private class PosturePanel extends FunctionPanel {
		private double[] angles_;      // [rad]
		private double[] tmpAng_;      // [rad]
        private boolean  tmpUpdate_;
		
		private JMenuItem update  = new JMenuItem("Update");
		private JMenuItem flipLR  = new JMenuItem("L<->R");
		private JMenuItem copyLtoR= new JMenuItem("L-->R");
		private JMenuItem copyRtoL= new JMenuItem("L<--R");
			
		public PosturePanel(PosturePanel p) {
			this(p.getName(), p.angles_, p.time_);
		}		
		
		public PosturePanel(String name, double[] angles) {
			this(name, angles, 1.0);
		}
		
		public PosturePanel(String name, double[] angles , double time) {
			super(name, time);
			setAngles(angles);

			button_.setToolTipText("change model posture");

			buttonMenu.add(update);
			buttonMenu.add(flipLR);
			buttonMenu.add(copyLtoR);
			buttonMenu.add(copyRtoL);

			update.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					setAngles(currentModel_.getJointValues());
				}
			});

			flipLR.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					copyAtoBbyHead("L","R", true);
				}
			});

			copyLtoR.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					copyAtoBbyHead("L","R", false);
				}
			});

			copyRtoL.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					copyAtoBbyHead("R","L", false);
				}
			});	

		}
		
		public void execButton() {
			if (currentPort_ != null) {
				String com = getHrpsysScript();
				//currentModel_.update_ = false;
				boolean b = currentPort_.isEnabled();
				currentPort_.setEnabled(true);
				currentPort_.println(com);
				currentPort_.setEnabled(b);
				//currentModel_.update_ = true;
			} else {
				currentModel_.setJointValues(angles_);
				currentModel_.calcForwardKinematics();
			}
			
			if (tmpAng_ == null) 
				tmpAng_ = new double[angles_.length];
			for (int i=0; i<tmpAng_.length; i++)
				tmpAng_[i] = angles_[i];
		}
		
		public void enterButton() {
			if (currentPort_ != null)
				currentPort_.setEnabled(false);
			tmpUpdate_ = currentModel_.update_;
			currentModel_.update_ = false;
			if (currentModel_ != null) {
				tmpAng_ = currentModel_.getJointValues();
				currentModel_.setJointValues(angles_);
				currentModel_.calcForwardKinematics();
			}
		}
		
		public void exitButton() {
			if (currentModel_ != null) {
				currentModel_.setJointValues(tmpAng_);
				currentModel_.calcForwardKinematics();
			}
			if (currentPort_ != null)
				currentPort_.setEnabled(true);
			currentModel_.update_ = true;
			//currentModel_.update_ = tmpUpdate_;
		}

		private void setAngles(double[] angles) {
			if (angles_ == null)
				angles_ = new double[angles.length];
			System.arraycopy(angles, 0, angles_, 0, angles_.length);
		}
;
		private void copyAtoBbyHead(String A, String B, boolean swap) {
			LinkInfoLocal[] info = currentModel_.lInfo_;
			for (int i=0; i<info.length; i++) {
				int id1 = info[i].jointId;
				if (id1 < 0)
					continue;
				
				String name = info[i].name;
				if (name.startsWith(A)) { 
					name = B + name.substring(1);
					for (int j=0; j<info.length; j++) {
						int id2 = info[j].jointId;
						if (id2 < 0 || !name.equals(info[j].name)) 
							continue;
						
						double tmp = angles_[id2];
						angles_[id2] = angles_[id1];
						if (swap)
							angles_[id1] = tmp;
						
						double[] axis = info[j].jointAxis;
						if (axis[0] != 0 || axis[2] != 0) {
							if (swap)
								angles_[id1] *= -1;
							angles_[id2] *= -1;
						}
						break;
					}
				} else if (!name.startsWith(B)) {
					double[] axis = info[i].jointAxis;
					if (axis[0] != 0 || axis[2] != 0) 
						angles_[id1] *= -1;
				}
			}
		}
		
		public String getHrpsysScript(){
			return preHrpsysScript + getAngleString() + postHrpsysScript;
		}
		
		public String getPythonScript(){
			return "  "+prePythonScript + getAngleString() + postPythonScript;
		}
		
		public String getString4Save() {
			return "POSTURE: " + getName() + "\n" + getAngleString() + "\n";
		}
		
		private String getAngleString() {
			String com = "";
			for (int i=0; i<angles_.length; i++) {
				double a = Math.toDegrees(angles_[i]);
				com += Math.abs(a) < 0.01 ? "0 " : String.format(FORMAT1, a);
			}
			com += getTimeString();
			return com;
		}
	}
	

	private class WaitPanel extends FunctionPanel {
		public WaitPanel() {
			this(1);
		}
		
		public WaitPanel(WaitPanel p) {
			this(p.time_);
		}
		
		public WaitPanel(double time) {
			super("Wait",time);
			button_.setToolTipText("wait seconds");
			rename.setEnabled(false);
		}
		
		public void execButton() {
			if (currentPort_ != null) {
				String com = getHrpsysScript();
				//currentModel_.update_ = false;
				boolean b = currentPort_.isEnabled();
				currentPort_.setEnabled(true);
				currentPort_.println(com);
				currentPort_.setEnabled(b);
				//currentModel_.update_ = true;
			}

		}
		public void enterButton() {}
		public void exitButton() {}

		public String getHrpsysScript() {
			return "send self :wait "+ getTimeString();
		}

		public String getPythonScript() {
			return "  manager.sendMsg(':wait " + getTimeString() + "')\n";
		}
		
		public String getString4Save() {
			return "WAIT: " + getName() + "\n" + getTimeString() + "\n";
		}
	}
}

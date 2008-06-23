/*
 *  GrxEasyDemoView.java
 *
 *  Copyright (C) 2007 GeneralRobotix, Inc.
 *  All Rights Reserved
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 */

package com.generalrobotix.ui.shrpsys.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.LineBorder;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.shrpsys.item.GrxPortItem;
import com.generalrobotix.ui.util.GrxDebugUtil;

@SuppressWarnings("serial")
public class GrxEasyDemoView extends GrxBaseView {
	private GrxPortItem   currentPort_;
	private ChorometState state_;
	
	//private static final Font FONT_JAPANESE_L = new Font("東風ゴシック", Font.PLAIN, 22);
	private static final Font FONT_JAPANESE_M = new Font("東風ゴシック", Font.PLAIN, 17);
	private static final Font FONT_JAPANESE_S = new Font("東風ゴシック", Font.PLAIN, 14);
	
	private JPanel paneNorth_ = new JPanel();
	private JPanel paneCenter_ = new JPanel();
	private JPanel paneSouth_ = new JPanel();
	
	private JPanel paneDemo1_ = new JPanel();
	private JPanel paneDemo2_ = new JPanel();
	
	private static final JLabel  labelNoMotion_ = new JLabel("編集モードボタンを押して、モーションを編集して下さい。");
	private JButton buttonStart_ = new JButton();
	private JButton buttonEmer_ = new JButton();
	private JButton buttonPlay_ = new JButton();
	private JToggleButton buttonEdit_ = new JToggleButton();
	private JComboBox comboPort_ = new JComboBox();
	private JTextField fieldPortStat_ = new JTextField();
	
	private List<RobotMotion> motionList_ = new ArrayList<RobotMotion>();
	private Map<String, ImageIcon> iconMap_ = new HashMap<String, ImageIcon>();
	private LineBorder borderNormal_ = new LineBorder(Color.gray, 5, false);
	private LineBorder borderSelected_ = new LineBorder(Color.red, 5, false);
	
	private MotionPanel currentMotionPanel_;
	private boolean isRequestingData_ = false;
	private String rowData = "";
	
	private static enum  MODE{BROWSING, EDITTING, PLAYING, PAUSING}; 
	private MODE mode_ = MODE.BROWSING;
	
	public GrxEasyDemoView(String name, GrxPluginManager manager) {
		super(name, manager);
		isScrollable_ = false;
		JPanel contentPane = getContentPane();
		contentPane.setBackground(Color.black);
		
		contentPane.setLayout(new BorderLayout());
		contentPane.add(paneNorth_, BorderLayout.NORTH);
		JScrollPane scPane = new JScrollPane(paneCenter_);
		scPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scPane.setBorder(new LineBorder(Color.cyan, 5, true));
		contentPane.add(scPane, BorderLayout.CENTER);
		//contentPane.add(paneSouth_, BorderLayout.SOUTH);
		
		paneNorth_.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 40));
		paneCenter_.setLayout(new BoxLayout(paneCenter_, BoxLayout.Y_AXIS));
		paneDemo1_.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 40));
		paneDemo2_.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 40));
		paneSouth_.setLayout(new FlowLayout(FlowLayout.RIGHT, 50, 40));
		
		paneNorth_.setBackground(Color.black);
		paneCenter_.setBackground(Color.black);
		paneDemo1_.setBackground(Color.black);
		paneDemo2_.setBackground(Color.black);
		paneSouth_.setBackground(Color.black);
		
		paneNorth_.add(buttonStart_);
		paneNorth_.add(comboPort_);
		paneNorth_.add(fieldPortStat_);
		paneNorth_.add(buttonEmer_);
		paneCenter_.add(paneDemo1_);
		paneCenter_.add(paneDemo2_);
		paneSouth_.add(buttonEdit_);
		
		labelNoMotion_.setForeground(Color.white);
	
		Dimension dim1 = new Dimension(250,80);
		buttonStart_.setText("デモ開始");
		buttonStart_.setPreferredSize(dim1);
		buttonStart_.setFont(FONT_JAPANESE_M);
		buttonStart_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread (){
					public void run() {
						buttonStart_.setEnabled(false);
						GrxPortItem port = (GrxPortItem)comboPort_.getSelectedItem();
						port.open();
						currentPort_ = port;
						int idx = 0;
						if (currentMotionPanel_ != null)
							idx = paneDemo1_.getComponentZOrder(currentMotionPanel_);
						play(idx);
					}
				};
				t.start();
			}
		});
		
		buttonEmer_.setText("緊急停止(サーボOFF)");
		buttonEmer_.setFont(FONT_JAPANESE_M);
		buttonEmer_.setPreferredSize(dim1);
		buttonEmer_.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (currentPort_ != null) {
					currentPort_.println("send robot :servo all off");
				}
			}
		});
		
		comboPort_.setEditable(false);
		comboPort_.setEnabled(false);
		
		fieldPortStat_.setPreferredSize(new Dimension(100,28));
		fieldPortStat_.setHorizontalAlignment(JTextField.CENTER);
		fieldPortStat_.setText("未接続");
		fieldPortStat_.setEditable(false);
		fieldPortStat_.setForeground(Color.red);
		fieldPortStat_.setBackground(Color.yellow);
		fieldPortStat_.setFont(FONT_JAPANESE_M);
		
		Dimension dim2 = new Dimension(150,50);
		buttonEdit_.setText("編集モード");
		buttonEdit_.setPreferredSize(dim2);
		buttonEdit_.setFont(FONT_JAPANESE_S);
		
		buttonPlay_.setText("再生");
		
		String[] icons = {
				"sitting0","robotSit0",
				"sitting","robotSit",
				"standing","robotStand",
				"lying","robotLie",
				"play","playback",
				"pause","pause"
		};
		for (int i=0; i < icons.length/2; i++) {
			URL url = getClass().getResource("/resources/images/"+icons[i*2+1]+".png");
			Toolkit tk = Toolkit.getDefaultToolkit();
			iconMap_.put(icons[i*2], new ImageIcon(tk.getImage(url)));
		}
	}	
	
	public void setMode(MODE m) {
		mode_ = m;
		if (m == MODE.BROWSING) {
			buttonStart_.setEnabled(true);
			
		} else if (m == MODE.EDITTING) {
			buttonStart_.setEnabled(false);
			
		} else if (m == MODE.PLAYING) {
			buttonStart_.setEnabled(false);
			
		} else if (m == MODE.PAUSING) {
			buttonStart_.setEnabled(false);
			
		}
	}
	
	public void restoreProperties() {
		super.restoreProperties();
		updateDemoPane();
	}
	
	public void updateDemoPane() {
		paneDemo1_.removeAll();
		int len = Integer.parseInt(getProperty("motionNumber"));
		for (int i=0; i<len; i++) {
			String header = "motion"+i+".";
			String name    = getProperty(header+"name", "no name");
			String command = getProperty(header+"command","");
			String iState  = getProperty(header+"initialState","-");
			String fState  = getProperty(header+"finalState","-");
			boolean isContinuous = isTrue(header+"isContinuous", false); 
			//int[] motion = getIntAry(header+"motion");
			
			RobotMotion m = new RobotMotion(name, command, iState, fState, isContinuous);
			motionList_.add(m);
		}
		
		int[] indexSeq = getIntAry("playlist0");
			if (indexSeq != null) {
				for (int i=0; i<indexSeq.length; i++)
					paneDemo1_.add(new MotionPanel(motionList_.get(indexSeq[i])));
				MotionPanel lastButton = (MotionPanel)paneDemo1_.getComponent(paneDemo1_.getComponentCount()-1);
				lastButton.labelContinuous_.setVisible(false);
		}
		
		setCurrent((MotionPanel)paneDemo1_.getComponent(0));
	}
	
	public void updateDemoPaneTest() {
		RobotMotion[] testList = new RobotMotion[] {
			new RobotMotion("着席(初期姿勢セット)", "load demo/sitdown" ,  "-", "sitting0", true),
			new RobotMotion("サーボ ON", 	 		"send robot :servo all on", "sitting0", "sitting", true),
			new RobotMotion("起立",	  	  		"load demo/standup" ,  	    "sitting", "standing", true),
			new RobotMotion("待ち (5秒)", 		"send self :wait 5" ,	    "-",   "-", true),
			new RobotMotion("体操１（左右）", 		"load demo/taisou_r",		"standing","standing", true),
			new RobotMotion("体操２（前後）", 		"load demo/taisou_p",		"standing","standing", true),
			new RobotMotion("体操３（上体ひねり）", 	"load demo/taisou_y",	    "standing","standing", true),
			new RobotMotion("寝転び（仰向け）", 	"load demo/stand2up",       "standing", "lying", true),
			new RobotMotion("待ち (5秒)", 		"send self :wait 5",        "-", "-", true),
			new RobotMotion("起き上がり（仰向け）",	"load demo/up2stand","lying", "standing", false),
			new RobotMotion("着席",	  			"load demo/sitdown",  "standing", "sitting", false),
			new RobotMotion("サーボ OFF", 		"send robot :servo all off", "sitting", "sitting0", false),
		};
		
		paneDemo1_.removeAll();
		for (int i=0; i<testList.length; i++) {
			paneDemo1_.add(new MotionPanel(testList[i]));
			motionList_.add(testList[i]);
		}
		MotionPanel lastButton = (MotionPanel)paneDemo1_.getComponent(testList.length-1);
		lastButton.labelContinuous_.setVisible(false);
		
		setCurrent((MotionPanel)paneDemo1_.getComponent(0));
	}

	class RobotMotion {
		String name;
		String command;
		String initialState;
		String finalState;
		boolean isContinuous = false;
		List<RobotMotion> childList = new ArrayList<RobotMotion>();
		
		RobotMotion(String _name, String _com, String _iState, String _fState, boolean isCon) {
			name = _name; 
			command = _com;
			initialState = _iState;
			finalState = _fState;
			isContinuous = isCon;
		}
		
		void addChildMotion(RobotMotion m){
			childList.add(m);
		}
	}
	
	class MotionPanel extends JPanel {
		final Dimension defaultSize_ = new Dimension(240,150); 
		final Dimension defaultSize2_ = new Dimension(200,200); 
		
		RobotMotion motion_;
		
		JPanel paneCommand = new JPanel(new BorderLayout());
		JPanel paneState = new JPanel(new FlowLayout(FlowLayout.CENTER, 10,5));
		JButton button_ = new JButton();
		JLabel  labelState0_ = new JLabel();
		JLabel  labelState1_ = new JLabel();
		JLabel  labelContinuous_ = new JLabel();
		
		public MotionPanel(RobotMotion motion) {
			super(new BorderLayout());
			this.updateMotion(motion);
			this.setBackground(Color.black);
			this.add(paneCommand, BorderLayout.CENTER);
			this.add(labelContinuous_, BorderLayout.EAST);
			paneCommand.add(paneState, BorderLayout.NORTH);
			
			labelState0_.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			labelState1_.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			
			button_.setFont(FONT_JAPANESE_M);
			button_.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					Thread t = new Thread() {
						public void run() {
							int idx = paneDemo1_.getComponentZOrder(MotionPanel.this);
							if (mode_ == MODE.PAUSING && idx > 0)
								play(idx);
							else 
								exec();
						}
					};
					t.start();
				}
			});
			
			paneCommand.setPreferredSize(defaultSize_);
			paneCommand.setAlignmentX(JButton.CENTER_ALIGNMENT);
			paneCommand.add(button_, BorderLayout.CENTER);
			paneCommand.setBorder(borderNormal_);
			
			paneState.setAlignmentX(JPanel.CENTER_ALIGNMENT);
			paneState.setBackground(Color.white);
			paneState.add(labelState0_);
			paneState.add(new JLabel(iconMap_.get("play")));
			paneState.add(labelState1_);
		}
		
		public void updateMotion(RobotMotion motion) {
			setVisible(false);
			motion_ = motion;
			labelState0_.setIcon(iconMap_.get(motion_.initialState));
			labelState1_.setIcon(iconMap_.get(motion_.finalState));
			if (motion_.isContinuous)
				labelContinuous_.setIcon(iconMap_.get("play"));
			else 
				labelContinuous_.setIcon(iconMap_.get("pause"));
				
			button_.setText(motion_.name);
			button_.setToolTipText(motion_.command);
			setVisible(true);
		}
	
		public void exec() {
			if (currentPort_ != null) { 
				reservCommand_ = motion_.command;
				//currentPort_.println(motion_.command);
			}
			//GrxDebugUtil.println(motion_.name+":"+motion_.command);
		}
		
		public void setSelected(boolean b) {
			if (b) {
				paneCommand.setBorder(borderSelected_);
				button_.setIcon(iconMap_.get("play"));
			} else {
				paneCommand.setBorder(borderNormal_);
				button_.setIcon(null);
			}
		}
	}
	
	public void setCurrent(MotionPanel b) {
		if (currentMotionPanel_ != null)
			currentMotionPanel_.setSelected(false);
		currentMotionPanel_ = b;
		currentMotionPanel_.setSelected(true);
		/*Component[] c = paneCenter_.getComponents();
		for (int i=0; i<c.length; i++)
			((MotionPanel)c[i]).setSelected((c[i] == b));*/
	}
	
	public void play(int idx) {
		Component[] c = paneDemo1_.getComponents();
		if (idx == c.length) {
			for (int i=0; i<c.length; i++) {
				MotionPanel cb = (MotionPanel)c[i];
				cb.setVisible(true);
				cb.button_.setEnabled(true);
			}
			setCurrent((MotionPanel)c[0]);
		} else if (-1 < idx && idx < c.length) {
			paneDemo1_.setVisible(false);
			paneDemo2_.setVisible(false);
			for (int i=0; i<c.length; i++) {
				MotionPanel cb = (MotionPanel)c[i];
				if (c.length-3 < idx)
					cb.setVisible(c.length-4 < i);
				else 
					cb.setVisible(idx<=i && i<idx+3);
				cb.button_.setEnabled(false);
			}
			paneDemo1_.setVisible(true);
			
			setMode(MODE.PLAYING);
			
			for (int i = idx; i < c.length; i++) {
				MotionPanel cb = (MotionPanel)c[i];
				setCurrent(cb);
				cb.exec();
				
				while(reservCommand_ == cb.motion_.command || isRequestingData_) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if (i+3 < c.length) {
					cb.setVisible(false);
					c[i+3].setVisible(true);
				} 
				
				if (!cb.motion_.isContinuous && i+1 < c.length) {
					setMode(MODE.PAUSING);
					cb = (MotionPanel)c[i+1];
					setCurrent(cb);
					cb.button_.setEnabled(true);
					paneDemo2_.removeAll();
					for (int j=0; j<motionList_.size(); j++) {
						RobotMotion mp = motionList_.get(j);
						String current = cb.motion_.initialState;
						if (current.equals(mp.initialState) &&
								current.equals(mp.finalState))
							paneDemo2_.add(new MotionPanel(mp));
					}
					paneDemo2_.setVisible(true);
					return;
				}
			}
			
			for (int i=0; i<c.length; i++) {
				MotionPanel cb = (MotionPanel)c[i];
				cb.setVisible(true);
				cb.button_.setEnabled(true);
			}
			setCurrent((MotionPanel)c[0]);
			setMode(MODE.BROWSING);
		}
	}
	
	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		for (int i=0; i<itemList.size(); i++) {
			comboPort_.removeAllItems();
			GrxBaseItem item = itemList.get(i);
			if (item instanceof GrxPortItem) {
				comboPort_.addItem((GrxPortItem)item);
			}
		}
	}

	public boolean setup(List<GrxBaseItem> itemList) {
		currentPort_ = null;
		return true;
	}
	
	private int counter_ = 0;
	private int interval_ = 10;
    private int requestedCounter_ = -1;
    private String reservCommand_ = null;
    
	public void control(List<GrxBaseItem> itemList) {
		if (currentPort_ == null)
			return;
		counter_++;
		try { 
			if (!currentPort_.ready() && !isRequestingData_) {
				if (counter_ % interval_ == 0) {
					currentPort_.println("#request data");
					System.out.println("requestdata");
					isRequestingData_ = true;
					requestedCounter_ = counter_;
				} else if (reservCommand_ != null) {
					currentPort_.println(reservCommand_);
					System.out.println("exec: "+reservCommand_);
					isRequestingData_ = true;
					requestedCounter_ = counter_;
					reservCommand_ = null;
					fieldPortStat_.setText("実行中");
					fieldPortStat_.setForeground(Color.blue);
					fieldPortStat_.setBackground(Color.white);
				}
			}	
			state_ = null;
			while (currentPort_.ready()) {
				String s = currentPort_.readLine();
				if (state_ == null) {
					state_ = new ChorometState();
					rowData = s + "\n";
				} else
					rowData += s + "\n";
			}
				
			if (state_ != null) {
				isRequestingData_ = false;
				try {
					state_.parse(rowData);
					fieldPortStat_.setText("接続中");
					fieldPortStat_.setForeground(Color.blue);
					fieldPortStat_.setBackground(Color.white);
				} catch (Exception e) {
					fieldPortStat_.setText("未接続");
					fieldPortStat_.setForeground(Color.red);
					fieldPortStat_.setBackground(Color.white);
					System.out.println("RobotItem: parse error.");
					GrxDebugUtil.println("rowdata:\n" + rowData);
					return;
				}
//				addValue(state_);
			} 
			
			if (isRequestingData_ && counter_ - requestedCounter_ > interval_) {
				if (!fieldPortStat_.getText().equals("実行中")) {
					fieldPortStat_.setText("未接続");
					fieldPortStat_.setForeground(Color.red);
					fieldPortStat_.setBackground(Color.yellow);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean cleanup(List<GrxBaseItem> itemList) {
		return true;
	}

	public class ChorometState {
		public int run_count = 0;
		public double[] angles = null;
		public double[][] force = null;
		public double[][] rate = null;
		public double[][] accel = null;
		public double[][] zmp = null;
		public double[] comAngles = null;
		public double[] comZmp = null;
		public double[] adval = null;
		public int[] dio = null;
		
		boolean parse(String rowData) {
			String[] line = rowData.split("\n");
			for (int i = 0; i < line.length; i++) {
				StringTokenizer st = new StringTokenizer(line[i]);
				if (!st.hasMoreTokens())
					continue;
				String tag = st.nextToken();
				if (tag.equals("#count")) {
					st = new StringTokenizer(line[++i]);
					run_count = Integer.parseInt(st.nextToken());
				} else if (tag.equals("#angle")) {
					//int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					angles = new double[n2];
					st = new StringTokenizer(line[++i]);
					for (int k = 0; k < n2; k++) {
						try {
							angles[k] = Double.parseDouble(st.nextToken());
						} catch (Exception e) {
							angles[k] = Double.NaN;
						}
					}
				} else if (tag.equals("#force")) {
					int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					force = new double[n1][n2];
					zmp = new double[n1][2];
					for (int j = 0; j < n1; j++) {
						st = new StringTokenizer(line[++i]);
						for (int k = 0; k < n2; k++)
							force[j][k] = Double.parseDouble(st.nextToken());
						if (force[j][2] > 1.0) {
							zmp[j][0] = 1000 * -force[j][4] / force[j][2];
							zmp[j][1] = 1000 * force[j][3] / force[j][2];
						} else
							zmp[j][0] = zmp[j][1] = 0.0;
					}
				} else if (tag.equals("#rate")) {
					int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					rate = new double[n1][3];
					for (int j = 0; j < n1; j++) {
						st = new StringTokenizer(line[++i]);
						for (int k = 0; k < n2; k++)
							rate[j][k] = Double.parseDouble(st.nextToken());
					}
				} else if (tag.equals("#accel")) {
					int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					accel = new double[n1][n2];
					for (int j = 0; j < n1; j++) {
						st = new StringTokenizer(line[++i]);
						for (int k = 0; k < n2; k++)
							accel[j][k] = Double.parseDouble(st.nextToken());
					}
				} else if (tag.equals("#adval")) {
					//int n1 = Integer.parseInt(st.nextToken());
					int n2 = Integer.parseInt(st.nextToken());
					adval = new double[n2];
					st = new StringTokenizer(line[++i]);
					for (int j = 0; j < n2; j++) {
						adval[j] = Double.parseDouble(st.nextToken());
					}
				}
			}
			return true;
		}
	}
}

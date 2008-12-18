/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
/**
 * GraphPanel.java
 *
 * @author Kernel, Inc.
 * @version 1.0  (Sun Sep 23 2001)
 */
package com.generalrobotix.ui.view.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.generalrobotix.ui.GrxBasePlugin;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.util.MessageDialog;

@SuppressWarnings("serial")
public class GraphPanel extends JPanel {
    //--------------------------------------------------------------------
    private static final int BORDER_GAP = 12;
//    private static final int LABEL_GAP = 12;
    private static final int BUTTON_GAP = 5;

    private static final int INITIAL_GRAPH_HEIGHT = 200;
    private static final int INITIAL_GRAPH_WIDTH = 100;

    private static final int MAX_GRAPH_HEIGHT = 500;
    private static final int MIN_GRAPH_HEIGHT = 150;
//    private static final int GRAPH_HEIGHT_STEP = 50;

    private static final int GRAPH_LEFT_MARGIN = 50;
    private static final int GRAPH_RIGHT_MARGIN = 50;
    private static final int GRAPH_TOP_MARGIN = 20;
    private static final int GRAPH_BOTTOM_MARGIN = 30;

    //private static final Color GRAPH_BORDER_COLOR = new Color(0, 0, 100);
    private static final Font GRAPH_LEGEND_FONT = new Font("dialog", Font.PLAIN, 12);

    //--------------------------------------------------------------------
    private DroppableXYGraph[] graph_;
    private GraphElement[] graphElement_;
    public GraphElement currentGraph_;
    private TrendGraphManager trendGraphMgr_;

    private static final LineBorder normalBorder_ = new LineBorder(new Color(204, 204, 204), 2);
//    private static final LineBorder focusedBorder_ = new LineBorder(Color.red, 2);

    //private static final Color normalColor_ = new Color(0, 0, 100);
    //private static final Color focusedColor_ = new Color(0, 40, 0);
    private static final Color normalColor_ = Color.black;
    private static final Color focusedColor_ = new Color(0, 0, 100);


    //private static Color primary2_;
    //private static Color secondary3_;

    private JPanel graphElementBase_;
    private Frame owner_;

    private JSlider heightSlider_;
    private JButton hRangeButton_;
    private JButton vRangeButton_;
    private JButton seriesButton_;
    private JButton epsButton_;
    
    public void setEnabled(boolean b) {
    	heightSlider_.setEnabled(b);
    	hRangeButton_.setEnabled(b);
    	vRangeButton_.setEnabled(b);
    	seriesButton_.setEnabled(b);
    	epsButton_.setEnabled(b);
    }
    
    private HRangeDialog hRangeDialog_;
    private VRangeDialog vRangeDialog_;
    public SeriesDialog seriesDialog_;
    private EPSDialog epsDialog_;

    private JScrollPane graphScrollPane_;

    private int numGraph_;

//    private int mode_;
    private GrxPluginManager manager_;
    
//    private ArrayList<AttributeInfo> addedList_;
    private AttributeInfo[] addedArray_;
     
    //--------------------------------------------------------------------
    //public GraphPanel(GrxPluginManager manager, TrendGraphManager trendGraphMgr,Composite parent) {
    public GraphPanel(GrxPluginManager manager, TrendGraphManager trendGraphMgr, Frame frame) {
        manager_ = manager;
//    	owner_ = SWT_AWT.new_Frame(new Composite(parent,SWT.EMBEDDED));
        owner_ = frame;
        trendGraphMgr_ = trendGraphMgr;
        graphElementBase_ = new JPanel();
        graphElementBase_.setLayout(
            new BoxLayout(graphElementBase_, BoxLayout.Y_AXIS)
        );

        ActionListener focusListener_ = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                DroppableXYGraph graph = (DroppableXYGraph)currentGraph_.getGraph();
                graph.setBorderColor(normalColor_);
                graph.setLegendBackColor(normalColor_);
                currentGraph_.repaint();
                //currentGraph_.setBorder(normalBorder_);
                currentGraph_ = (GraphElement)evt.getSource();
                seriesDialog_.setCurrentGraph(currentGraph_);
                graph = (DroppableXYGraph)currentGraph_.getGraph();
                graph.setBorderColor(focusedColor_);
                graph.setLegendBackColor(focusedColor_);
                currentGraph_.repaint();
                //currentGraph_.setBorder(focusedBorder_);
                heightSlider_.setValue(currentGraph_.getPreferredSize().height);
                updateButtons();
            }
        };

//        DroppableXYGraph lg;
        TrendGraph tg;
//        DataItem di;
//        DataItemInfo dii;
        Dimension dim;

        numGraph_ = trendGraphMgr_.getNumGraph();
        graph_ = new DroppableXYGraph[numGraph_];
        graphElement_ = new GraphElement[numGraph_];
        for (int i = 0; i < numGraph_; i ++) {
            graph_[i] = new DroppableXYGraph(
                GRAPH_LEFT_MARGIN,
                GRAPH_RIGHT_MARGIN,
                GRAPH_TOP_MARGIN,
                GRAPH_BOTTOM_MARGIN
            );
            graph_[i].setBorderColor(normalColor_);
            graph_[i].setLegendBackColor(normalColor_);
            graph_[i].setLegendFont(GRAPH_LEGEND_FONT);

            tg = trendGraphMgr_.getTrendGraph(i);
            tg.setGraph(graph_[i]);

            graphElement_[i] =
                new GraphElement(
                    trendGraphMgr_.getTrendGraph(i),
                    graph_[i],
                    graph_[i].getLegendPanel()
                );

            graphElement_[i].setBorder(normalBorder_);
            graphElement_[i].addActionListener(focusListener_);
            dim = new Dimension(INITIAL_GRAPH_WIDTH, INITIAL_GRAPH_HEIGHT);
            graphElement_[i].setPreferredSize(dim);
            graphElementBase_.add(graphElement_[i]);
        }

        currentGraph_ = graphElement_[0];
        graph_[0].setBorderColor(focusedColor_);
        graph_[0].setLegendBackColor(focusedColor_);

        graphScrollPane_ = new JScrollPane(
            graphElementBase_,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,//.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        graphScrollPane_.getVerticalScrollBar().setUnitIncrement(10);

        heightSlider_ = new JSlider(
            MIN_GRAPH_HEIGHT,
            MAX_GRAPH_HEIGHT,
            currentGraph_.getPreferredSize().height
        );
        heightSlider_.setMinimumSize(new Dimension(100, 22));
        heightSlider_.setMaximumSize(new Dimension(100, 22));
        heightSlider_.setPreferredSize(new Dimension(100, 22));
        //heightSlider_.setPaintTicks(true);
        //heightSlider_.setMajorTickSpacing(GRAPH_HEIGHT_STEP);
        //heightSlider_.setSnapToTicks(false);
        heightSlider_.addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    JSlider s = (JSlider)evt.getSource();
                    graphElementBase_.setVisible(false);
                    Dimension d = currentGraph_.getPreferredSize();//.height = s.getValue();
                    d.height = s.getValue();
                    currentGraph_.setPreferredSize(d);
                    graphElementBase_.setVisible(true);
                }
            }
        );

        hRangeButton_ = new JButton(MessageBundle.get("graph.hrange"));
        hRangeDialog_ = new HRangeDialog(owner_);
        hRangeButton_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    GrxBasePlugin graphItem = manager_.getSelectedItem(GrxGraphItem.class, null);
                    if (graphItem == null)
                    	return;
                    	
                    hRangeDialog_.setMaxHRange(
                        trendGraphMgr_.getTotalTime()
                    );
                    hRangeDialog_.setMinHRange(
                        trendGraphMgr_.getStepTime() * 10
                    );
                    double range = trendGraphMgr_.getTimeRange();
                    double pos = trendGraphMgr_.getMarkerPos();
                    hRangeDialog_.setHRange(range);
                    hRangeDialog_.setMarkerPos(pos);
                    hRangeDialog_.setLocationRelativeTo(GraphPanel.this);
                    hRangeDialog_.setVisible(true);
                    int flag = hRangeDialog_.getUpdateFlag();
                    if (flag != 0) {
                        TrendGraph tg = currentGraph_.getTrendGraph();
                        double hRange = hRangeDialog_.getHRange();
                        double mpos =   hRangeDialog_.getMarkerPos();
                        if ((flag & HRangeDialog.RANGE_UPDATED) != 0
                            && (flag & HRangeDialog.POS_UPDATED) != 0) {
                            tg.setTimeRangeAndPos(hRange, mpos);
                        } else if ((flag & HRangeDialog.RANGE_UPDATED) != 0) {
                            tg.setTimeRange(hRange);
                        } else {
                            tg.setMarkerPos(mpos);
                        }
                        graphItem.setDblAry(currentGraph_.getTrendGraph().getNodeName()+".timeRange", new double[]{hRange, mpos});
                        graphElementBase_.repaint();
                    }
                }
            }
        );

        vRangeButton_ = new JButton(MessageBundle.get("graph.vrange"));
        vRangeDialog_ = new VRangeDialog(owner_);
        vRangeButton_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    TrendGraph tg = currentGraph_.getTrendGraph();
                    vRangeDialog_.setUnit(tg.getUnitLabel());
                    vRangeDialog_.setBase(tg.getBase());
                    vRangeDialog_.setExtent(tg.getExtent());
                    vRangeDialog_.setLocationRelativeTo(GraphPanel.this);
                    vRangeDialog_.setVisible(true);
                    if (vRangeDialog_.isUpdated()) {
                    	double base = vRangeDialog_.getBase();
                        double extent = vRangeDialog_.getExtent();
                        tg.setRange(base, extent);
                        GrxBasePlugin graphItem = manager_.getSelectedItem(GrxGraphItem.class, null);
                        if (graphItem == null)
                        	return;
                        graphItem.setDblAry(currentGraph_.getTrendGraph().getNodeName()+".vRange", new double[]{base, extent});
                        currentGraph_.repaint();
                    }
                }
            }
        );
        seriesButton_ = new JButton(MessageBundle.get("graph.series"));
        //seriesDialog_ = new SeriesDialog(manager_, currentGraph_, parent);
        seriesDialog_ = new SeriesDialog(manager_, currentGraph_, owner_);
        seriesButton_.addActionListener(
            new ActionListener() {
            	public void actionPerformed(ActionEvent evt) {
                    GrxBasePlugin graphItem = manager_.getSelectedItem(GrxGraphItem.class, null);
                    if (graphItem == null)
                    	return;
                    TrendGraph tg = currentGraph_.getTrendGraph();
                    seriesDialog_.setDataItemInfoList(tg.getDataItemInfoList());
                    seriesDialog_.setLocationRelativeTo(GraphPanel.this);
                    seriesDialog_.setVisible(true);
                    if (seriesDialog_.isUpdated()) {
                        DataItemInfo[] dii = seriesDialog_.getDataItemInfoList();
                        for (int i = 0; i < dii.length; i++) {
                            tg.setDataItemInfo(dii[i]);
                        }
                        dii = seriesDialog_.getRemovedList();
                        for (int i = 0; i < dii.length; i++) {
                            tg.removeDataItem(dii[i].dataItem);
                        }
                        
                        addedArray_ = seriesDialog_.getAddedList();
                        for (int i = 0; i < addedArray_.length; i++) {
                        	tg.addDataItem(addedArray_[i]);
                        }
                        
                        String graphName = tg.getNodeName();
                        Enumeration e = graphItem.propertyNames();
                        while (e.hasMoreElements()) {
                        	String key = (String)e.nextElement();
                        	if (key.startsWith(graphName))
                        		graphItem.remove(key);
                        }
                        String dataItems = "";
                        DataItemInfo[] list = tg.getDataItemInfoList();
                        for (int i = 0; i<list.length;i++) {
                        	DataItem di = list[i].dataItem;
                        	String header = tg._getDataItemNodeName(di);
                        	if (i > 0)
                        		dataItems += ",";
                        	dataItems += graphName+"_"+di.object+"_"+di.node+"_"+di.attribute;
                        	if (di.index >= 0)
                        		dataItems += "_"+di.index;
                        	graphItem.setProperty(header+".object",di.object);
                        	graphItem.setProperty(header+".node",di.node);
                        	graphItem.setProperty(header+".attr",di.attribute);
				    		graphItem.setProperty(header+".index", String.valueOf(di.index));
                        }
                        graphItem.setProperty(graphName+".dataItems",dataItems);
                        
                        updateButtons();
                        currentGraph_.repaint();
                    }
                }
            }
        );

        epsButton_ = new JButton(MessageBundle.get("graph.eps"));
        epsDialog_ = new EPSDialog(owner_);
        epsButton_.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    epsDialog_.setColorOutput(true);
                    epsDialog_.setGraphOutput(true);
                    epsDialog_.setLegendOutput(true);
                    epsDialog_.setLocationRelativeTo(GraphPanel.this);
                    epsDialog_.setVisible(true);
                    if (epsDialog_.isUpdated()) {
                        String path = epsDialog_.getPath();
                        boolean cout = epsDialog_.isColorOutput();
                        boolean gout = epsDialog_.isGraphOutput();
                        boolean lout = epsDialog_.isLegendOutput();
                        FileWriter fw = null;

                        if (path.equals("")) {
                            new MessageDialog(
                                owner_,
                                MessageBundle.get("message.filenameempty"),
                                MessageBundle.get("messege.inputfilename")
                            ).showModalDialog();
                            return;
                        }

                        try {
                            fw = new FileWriter(path);
                        } catch (IOException ex) {
                            //ex.printStackTrace();
                            //System.exit(0);
                            new MessageDialog(
                                owner_,
                                MessageBundle.get("messege.fileioerror"),
                                MessageBundle.get("messege.fileioerror")
                            ).showModalDialog();
                            return;
                        }
                        BufferedWriter bw = new BufferedWriter(fw);
                        DroppableXYGraph graph =
                            (DroppableXYGraph)currentGraph_.getGraph();
                        DroppableXYGraph.LegendPanel legend =
                            (DroppableXYGraph.LegendPanel)graph.getLegendPanel();
                        Dimension gsize = graph.getSize();
                        //Dimension lsize = legend.getSize();
                        Dimension lsize = legend.getMinimalSize();
                        int width = 0;
                        int height = 0;
                        int lyofs = 0;
                        if (gout && lout) {
                            width = gsize.width + lsize.width - GRAPH_RIGHT_MARGIN + 10;
                            if (gsize.height > lsize.height) {
                                height = gsize.height;
                                lyofs = (gsize.height - lsize.height) / 2;
                            } else {
                                height = lsize.height;
                            }
                        } else if (gout) {
                            width = gsize.width;
                            height = gsize.height;
                        } else if (lout) {
                            width = lsize.width;
                            height = lsize.height;
                        }
                        EPSGraphics eg = new EPSGraphics(bw, 0, 0, width, height, cout);
                        int xofs = 0;
                        if (gout) {
                            graph.setEPSMode(true);
                            graph.paint(eg);
                            graph.setEPSMode(false);
                            xofs += gsize.width - GRAPH_RIGHT_MARGIN + 10;
                        }
                        if (lout) {
                            eg.setXOffset(xofs);
                            eg.setYOffset(lyofs);
                            legend.paint(eg);
                        }
                        eg.finishOutput();
                    }
                }
            }
        );

        updateButtons();

        JPanel sliderBase = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sliderBase.add(new JLabel(MessageBundle.get("graph.height")));
        sliderBase.add(heightSlider_);
        Box graphControlPanel = Box.createHorizontalBox();
        graphControlPanel.add(Box.createHorizontalStrut(BORDER_GAP));
        graphControlPanel.add(sliderBase);
        graphControlPanel.add(Box.createHorizontalGlue());
        graphControlPanel.add(hRangeButton_);
        graphControlPanel.add(Box.createHorizontalStrut(BUTTON_GAP));
        graphControlPanel.add(vRangeButton_);
        graphControlPanel.add(Box.createHorizontalStrut(BUTTON_GAP));
        graphControlPanel.add(seriesButton_);
        graphControlPanel.add(Box.createHorizontalGlue());
        graphControlPanel.add(epsButton_);
        graphControlPanel.add(Box.createHorizontalStrut(BORDER_GAP));

        this.setLayout(new BorderLayout());
        this.add(graphScrollPane_, BorderLayout.CENTER);
        this.add(graphControlPanel, BorderLayout.SOUTH);

        //mode_ = GUIStatus.EDIT_MODE;

        //PlaybackScheduler.getInstance().addPlaybackStatusListener(this);
    }

    //--------------------------------------------------------------------
    /**
     *
     */
    public void resetFocus() {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    currentGraph_ = graphElement_[0];
                    for (int i = 0; i < numGraph_; i++) {
                        graph_[i].setBorderColor(normalColor_);
                        graph_[i].setLegendBackColor(normalColor_);
                        graphElement_[i].getPreferredSize().height = INITIAL_GRAPH_HEIGHT;
                    }
                    graph_[0].setBorderColor(focusedColor_);
                    graph_[0].setLegendBackColor(focusedColor_);
                    graphScrollPane_.getViewport().setViewPosition(new Point(0, 0));
                    heightSlider_.setValue(INITIAL_GRAPH_HEIGHT);
                    graphElementBase_.setVisible(false);
                    graphElementBase_.setVisible(true);
                    updateButtons();
                }
            }
        );
    }

    //--------------------------------------------------------------------
    /**
     *
     */
    private void updateButtons() {
        boolean enabled = (currentGraph_.getTrendGraph().getNumDataItems() > 0);
        vRangeButton_.setEnabled(enabled);
        seriesButton_.setEnabled(true);
        epsButton_.setEnabled(enabled);
        
        GrxBasePlugin p = manager_.getSelectedItem(GrxGraphItem.class, null);
        setEnabled(p != null);
    }

 /*   public void setMode(int mode) {
        mode_ = mode;
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    boolean enabled =
                        (currentGraph_.getTrendGraph().getNumDataItems() > 0);
                    if (mode_ == GUIStatus.EDIT_MODE) {
                        hRangeButton_.setEnabled(true);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(enabled);
                        epsButton_.setEnabled(enabled);
                    } else if (mode_ == GUIStatus.EXEC_MODE) {
                        hRangeButton_.setEnabled(false);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(false);
                        epsButton_.setEnabled(false);
                    } else if (mode_ == GUIStatus.PLAYBACK_MODE) {
                        hRangeButton_.setEnabled(false);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(false);
                        epsButton_.setEnabled(enabled);
                    }
                }
            }
        );
    }

    //--------------------------------------------------------------------
    // Implementation of PlaybackStatusListener
    public void playbackStatusChanged(int status) {
        final int subMode = status;
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    boolean enabled =
                        (currentGraph_.getTrendGraph().getNumDataItems() > 0);
                    switch (subMode) {
                    case PlaybackStatus.STOPPED:
                        hRangeButton_.setEnabled(true);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(enabled);
                        epsButton_.setEnabled(enabled);
                        break;
                    case PlaybackStatus.PLAYING:
                        hRangeButton_.setEnabled(false);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(false);
                        epsButton_.setEnabled(false);
                        break;
                    case PlaybackStatus.PAUSE:
                        hRangeButton_.setEnabled(true);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(enabled);
                        epsButton_.setEnabled(enabled);
                        break;
                    case PlaybackStatus.RECORDING:
                        hRangeButton_.setEnabled(false);
                        vRangeButton_.setEnabled(enabled);
                        seriesButton_.setEnabled(false);
                        epsButton_.setEnabled(false);
                        break;
                    }
                }
            }
        );
    }*/

    //--------------------------------------------------------------------
/*
    public void timeChanged(Time time) {
        repaint();
    }
*/
}

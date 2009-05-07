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

import java.util.Enumeration;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial")
public class GraphPanel extends Composite {
    //--------------------------------------------------------------------
    private static final int INITIAL_GRAPH_HEIGHT = 200;

    private static final int MAX_GRAPH_HEIGHT = 500;
    private static final int MIN_GRAPH_HEIGHT = 150;

    //--------------------------------------------------------------------
    private GraphElement[] graphElement_;
    public GraphElement currentGraph_;
    private TrendGraphManager trendGraphMgr_;
    private List<GrxModelItem> currentModels_ = null;

    private static final Color normalColor_ = new Color(Display.getDefault(),0, 0, 0);
    private static final Color focusedColor_ = new Color(Display.getDefault(),0, 0, 100);

    private Composite graphElementBase_;
    private Composite comp_;
    
    private Scale heightSlider_;
    private Button hRangeButton_;
    private Button vRangeButton_;
    private Button seriesButton_;
    private Button epsButton_;
    
    public void setEnabled(boolean b) {
    	heightSlider_.setEnabled(b);
    	hRangeButton_.setEnabled(b);
    	vRangeButton_.setEnabled(b);
    	seriesButton_.setEnabled(b);
    	//epsButton_.setEnabled(b);
    }
    
    private HRangeDialog hRangeDialog_;
    private VRangeDialog vRangeDialog_;
    public SeriesDialog seriesDialog_;
    private EPSDialog epsDialog_;

    private ScrolledComposite graphScrollPane_;

    private int numGraph_;
    private GrxPluginManager manager_;
    private DataItemInfo[] addedArray_;
     
     public GraphPanel(GrxPluginManager manager, TrendGraphManager trendGraphMgr, Composite comp) {
        super(comp, SWT.NONE);
        manager_ = manager;
        comp_ = comp;
        trendGraphMgr_ = trendGraphMgr;
      
        setLayout(new GridLayout(1,true));
        graphScrollPane_ = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL| SWT.BORDER);
        graphScrollPane_.setExpandHorizontal(true);
        graphScrollPane_.setExpandVertical(true);
        GridData gridData0 = new GridData();
 		gridData0.horizontalAlignment = GridData.FILL;
 		gridData0.grabExcessHorizontalSpace = true;
 		gridData0.verticalAlignment = GridData.FILL;
 		gridData0.grabExcessVerticalSpace = true;
 		graphScrollPane_.setLayoutData(gridData0);
        Composite graphControlPanel = new Composite(this, SWT.NONE); 
        GridData gridData1 = new GridData();
 		gridData1.horizontalAlignment = GridData.FILL;
 		gridData1.grabExcessHorizontalSpace = true;
        graphControlPanel.setLayoutData(gridData1);
        graphControlPanel.setLayout(new RowLayout());
        graphElementBase_ = new Composite(graphScrollPane_, SWT.NONE);
        graphElementBase_.setLayout(new GridLayout(1,true));
        graphScrollPane_.setContent(graphElementBase_);

        numGraph_ = trendGraphMgr_.getNumGraph();
        graphElement_ = new GraphElement[numGraph_];
        for (int i = 0; i < numGraph_; i ++) {
            graphElement_[i] =
                new GraphElement(
                	this,
                	graphElementBase_,
                    trendGraphMgr_.getTrendGraph(i),INITIAL_GRAPH_HEIGHT
                );
        }
        graphScrollPane_.setMinSize(graphElementBase_.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        currentGraph_ = graphElement_[0];
        graphElement_[0].setBorderColor(focusedColor_);

        Label lb = new Label( graphControlPanel, SWT.NONE);
		lb.setText("Height:");
        heightSlider_ = new Scale(graphControlPanel, SWT.HORIZONTAL);
        heightSlider_.setMaximum(MAX_GRAPH_HEIGHT);
        heightSlider_.setMinimum(MIN_GRAPH_HEIGHT);
        heightSlider_.setSelection(INITIAL_GRAPH_HEIGHT);
        heightSlider_.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e){
             	GridData gridData = (GridData)currentGraph_.getLayoutData();
            	gridData.heightHint = heightSlider_.getSelection();
            	graphScrollPane_.setMinSize(graphElementBase_.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            	graphElementBase_.layout(true);
            }
		} );

        hRangeButton_ = new Button(graphControlPanel,  SWT.PUSH);
        hRangeButton_.setText(MessageBundle.get("graph.hrange"));
        hRangeDialog_ = new HRangeDialog(graphControlPanel.getShell());
        hRangeButton_.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e) {
        		final GrxGraphItem graphItem = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
        		if (graphItem == null)
        			return;
        		hRangeDialog_.setMaxHRange(trendGraphMgr_.getTotalTime());
        		hRangeDialog_.setMinHRange(trendGraphMgr_.getStepTime() * 10);
        		double range = trendGraphMgr_.getTimeRange();
        		double pos = trendGraphMgr_.getMarkerPos();
        		hRangeDialog_.setHRange(range);
        		hRangeDialog_.setMarkerPos(pos);
        		if(hRangeDialog_.open() == IDialogConstants.OK_ID){
        			int flag = hRangeDialog_.getUpdateFlag();
                    if (flag != 0) {
                        TrendGraph tg = currentGraph_.getTrendGraph();
                        final double hRange = hRangeDialog_.getHRange();
                        final double mpos =   hRangeDialog_.getMarkerPos();
                        if ((flag & HRangeDialog.RANGE_UPDATED) != 0
                            && (flag & HRangeDialog.POS_UPDATED) != 0) {
                            tg.setTimeRangeAndPos(hRange, mpos);
                        } else if ((flag & HRangeDialog.RANGE_UPDATED) != 0) {
                            tg.setTimeRange(hRange);
                        } else {
                            tg.setMarkerPos(mpos);
                        }
                        syncExec( new Runnable(){
							public void run() {
								graphItem.setDblAry(currentGraph_.getTrendGraph().getNodeName()+".timeRange", new double[]{hRange, mpos});
							}
                        });
                        for (int i = 0; i < numGraph_; i ++) {
                            graphElement_[i].redraw();
                        }
                    }
                }
        	}
        });

        vRangeButton_ = new Button(graphControlPanel,  SWT.PUSH);
        vRangeButton_.setText(MessageBundle.get("graph.vrange"));
        vRangeDialog_ = new VRangeDialog(graphControlPanel.getShell());
        vRangeButton_.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e) {
                    TrendGraph tg = currentGraph_.getTrendGraph();
                    vRangeDialog_.setUnit(tg.getUnitLabel());
                    vRangeDialog_.setBase(tg.getBase());
                    vRangeDialog_.setExtent(tg.getExtent());
                    if(vRangeDialog_.open() == IDialogConstants.OK_ID){
                    	final double base = vRangeDialog_.getBase();
                        final double extent = vRangeDialog_.getExtent();
                        tg.setRange(base, extent);
                        final GrxGraphItem graphItem = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
                        if (graphItem == null)
                        	return;
                        syncExec( new Runnable(){
							public void run() {
								graphItem.setDblAry(currentGraph_.getTrendGraph().getNodeName()+".vRange", new double[]{base, extent});
							}
                        });
                        currentGraph_.redraw();
                    }
                }
            }
        );

        seriesButton_ = new Button(graphControlPanel,  SWT.TOGGLE);
        seriesButton_.setText(MessageBundle.get("graph.series"));
        
        seriesDialog_ = new SeriesDialog(currentGraph_, graphControlPanel.getShell());
        seriesButton_.addSelectionListener(new SelectionAdapter(){
            	public void widgetSelected(SelectionEvent e) {
                    final GrxGraphItem graphItem = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
                    if (graphItem == null)
                    	return;
                    TrendGraph tg = currentGraph_.getTrendGraph();
                    seriesDialog_.setModelList(currentModels_);
                    seriesDialog_.setDataItemInfoList(tg.getDataItemInfoList());
                    if(seriesDialog_.open() == IDialogConstants.OK_ID){
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
                        
                        final String graphName = tg.getNodeName();
                        Enumeration enume = graphItem.propertyNames();
                        while (enume.hasMoreElements()) {
                        	String key = (String)enume.nextElement();
                        	if (key.startsWith(graphName))
                        		graphItem.remove(key);
                        }
                        String dataItems = "";
                        DataItemInfo[] list = tg.getDataItemInfoList();
                        for (int i = 0; i<list.length;i++) {
                        	final DataItem di = list[i].dataItem;
                        	final String header = tg._getDataItemNodeName(di);
                        	if (i > 0)
                        		dataItems += ",";
                        	dataItems += graphName+"_"+di.object+"_"+di.node+"_"+di.attribute;
                        	if (di.index >= 0)
                        		dataItems += "_"+di.index;
                        	syncExec( new Runnable(){
								public void run() {
									graphItem.setProperty(header+".object",di.object);
									graphItem.setProperty(header+".node",di.node);
									graphItem.setProperty(header+".attr",di.attribute);
									graphItem.setProperty(header+".index", String.valueOf(di.index));
								}
                        	});
                        }
                        final String value = dataItems;
                        syncExec( new Runnable(){
							public void run() {
								graphItem.setProperty(graphName+".dataItems",value);
							}
                        });
                        updateButtons();
                        currentGraph_.redraw();
                    }
                }
            }
        );
        /*
        epsButton_ = new Button(graphControlPanel,  SWT.PUSH);
        epsButton_.setText(MessageBundle.get("graph.eps"));

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
	*/
        setEnabled(false);

    }

    public void setFocuse(GraphElement ge){
    	currentGraph_.setBorderColor(normalColor_);
		currentGraph_.redraw();
		currentGraph_ = ge;
		currentGraph_.setBorderColor(focusedColor_);
		currentGraph_.redraw();
        seriesDialog_.setCurrentGraph(currentGraph_);
        heightSlider_.setSelection(currentGraph_.getSize().y); 
        updateButtons();
    }
    //--------------------------------------------------------------------
    /**
     *
     */
    public void resetFocus() {
    	/*
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    currentGraph_ = graphElement_[0];
                    for (int i = 0; i < numGraph_; i++) {
                    	graphElement_[i].setBorderColor(normalColor_);
                    	graphElement_[i].setLegendBackColor(normalColor_);
                        //graphElement_[i].getPreferredSize().height = INITIAL_GRAPH_HEIGHT;
                    }
                    graphElement_[0].setBorderColor(focusedColor_);
                    graphElement_[0].setLegendBackColor(focusedColor_);
                    //graphScrollPane_.getViewport().setViewPosition(new Point(0, 0));
                    //heightSlider_.setValue(INITIAL_GRAPH_HEIGHT);
                    graphElementBase_.setVisible(false);
                    graphElementBase_.setVisible(true);
                    updateButtons();
                }
            }
        );
        */
    }

    //--------------------------------------------------------------------
    /**
     *
     */
    private void updateButtons() {
        boolean enabled = (currentGraph_.getTrendGraph().getNumDataItems() > 0);
        vRangeButton_.setEnabled(enabled);
        seriesButton_.setEnabled(true);
        //epsButton_.setEnabled(enabled);
        
        GrxGraphItem p = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
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
    private boolean syncExec(Runnable r){
		Display display = comp_.getDisplay();
        if ( display!=null && !display.isDisposed()){
            display.syncExec( r );
            return true;
        }else
        	return false;
	}
    
    public void setModelList(List<GrxModelItem> list){
    	currentModels_ = list;
    }
}

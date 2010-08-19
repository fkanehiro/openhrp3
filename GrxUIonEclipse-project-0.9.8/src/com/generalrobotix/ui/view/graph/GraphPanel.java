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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxGraphItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial") //$NON-NLS-1$
public class GraphPanel extends Composite{
    //--------------------------------------------------------------------
    private GraphElement[] graphElement_;
    public GraphElement currentGraph_;
    private TrendGraphModel trendGraphMgr_;
    private List<GrxModelItem> currentModels_ = null;

    private static final Color normalColor_ = Activator.getDefault().getColor("black");
    private static final Color focusedColor_ = Activator.getDefault().getColor( "focusedColor" ); //$NON-NLS-1$

    private SashForm graphElementBase_;
    
    private Button hRangeButton_;
    private Button vRangeButton_;
    private Button seriesButton_;
    //private Button epsButton_;
    
    public void setEnabled(boolean b) {
    	hRangeButton_.setEnabled(b);
    	vRangeButton_.setEnabled(b);
    	seriesButton_.setEnabled(b);
    	//epsButton_.setEnabled(b);
    }

    public void setEnabledRangeButton(boolean b) {
        hRangeButton_.setEnabled(b);
        vRangeButton_.setEnabled(b);
    }
    
    private HRangeDialog hRangeDialog_;
    private VRangeDialog vRangeDialog_;
    public SeriesDialog seriesDialog_;
    //private EPSDialog epsDialog_;

    private ScrolledComposite graphScrollPane_;

    private int numGraph_;
    private GrxPluginManager manager_;
    private DataItemInfo[] addedArray_;
     
     public GraphPanel(GrxPluginManager manager, TrendGraphModel trendGraphMgr, Composite comp) {
        super(comp, SWT.NONE);
        manager_ = manager;
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
        graphElementBase_ = new SashForm(graphScrollPane_, SWT.VERTICAL);
        graphElementBase_.SASH_WIDTH = 6;
        graphScrollPane_.setContent(graphElementBase_);

        numGraph_ = trendGraphMgr_.getNumGraph();
        graphElement_ = new GraphElement[numGraph_];
        for (int i = 0; i < numGraph_; i ++) {
            graphElement_[i] =
                new GraphElement(
                	this,
                	graphElementBase_,
                    trendGraphMgr_.getTrendGraph(i) );
        }
        graphScrollPane_.setMinSize(graphElementBase_.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        currentGraph_ = graphElement_[0];
        graphElement_[0].setBorderColor(focusedColor_);

        hRangeButton_ = new Button(graphControlPanel,  SWT.PUSH);
        hRangeButton_.setText(MessageBundle.get("GraphPanel.button.hrange")); //$NON-NLS-1$
        hRangeDialog_ = new HRangeDialog(graphControlPanel.getShell());
        hRangeButton_.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e) {
        		GrxGraphItem graphItem = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
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
                        double hRange = hRangeDialog_.getHRange();
                        double mpos =   hRangeDialog_.getMarkerPos();
                        if ((flag & HRangeDialog.RANGE_UPDATED) != 0
                            || (flag & HRangeDialog.POS_UPDATED) != 0) {
                            tg.setTimeRangeAndPos(hRange, mpos);
                        }
                        graphItem.setDblAry("timeRange", new double[]{hRange, mpos}); //$NON-NLS-1$
                        trendGraphMgr_.updateGraph();
                    }
                }
        	}
        });

        vRangeButton_ = new Button(graphControlPanel,  SWT.PUSH);
        vRangeButton_.setText(MessageBundle.get("GraphPanel.button.vrange")); //$NON-NLS-1$
        vRangeDialog_ = new VRangeDialog(graphControlPanel.getShell());
        vRangeButton_.addSelectionListener(new SelectionAdapter(){
        	public void widgetSelected(SelectionEvent e) {
                    TrendGraph tg = currentGraph_.getTrendGraph();
                    vRangeDialog_.setUnit(tg.getUnitLabel());
                    vRangeDialog_.setBase(tg.getBase());
                    vRangeDialog_.setExtent(tg.getExtent());
                    if(vRangeDialog_.open() == IDialogConstants.OK_ID){
                    	double base = vRangeDialog_.getBase();
                        double extent = vRangeDialog_.getExtent();
                        tg.setRange(base, extent);
                        GrxGraphItem graphItem = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
                        if (graphItem == null)
                        	return;
                        graphItem.setDblAry(currentGraph_.getTrendGraph().getNodeName()+".vRange", new double[]{base, extent}); //$NON-NLS-1$
                        redraw(getLocation().x,getLocation().y,getSize().x,getSize().y,true);
                    }
                }
            }
        );

        seriesButton_ = new Button(graphControlPanel,  SWT.PUSH);
        seriesButton_.setText(MessageBundle.get("GraphPanel.button.series")); //$NON-NLS-1$
        
        seriesDialog_ = new SeriesDialog(currentGraph_, graphControlPanel.getShell());
        seriesButton_.addSelectionListener(new SelectionAdapter(){
            	public void widgetSelected(SelectionEvent e) {
                    GrxGraphItem graphItem = manager_.<GrxGraphItem>getSelectedItem(GrxGraphItem.class, null);
                    if (graphItem == null)
                    {
                        if(MessageDialog.openQuestion(
                            null,
                            MessageBundle.get("GraphPanel.dialog.creategraph.title"),
                            MessageBundle.get("GraphPanel.dialog.creategraph.message")))
                        {
                            graphItem = (GrxGraphItem)manager_.createItem(GrxGraphItem.class, null);
                            manager_.itemChange(graphItem, GrxPluginManager.ADD_ITEM);
                            manager_.setSelectedItem(graphItem, true);
                        }
                        else
                            return;
                    }

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
                            tg.removeDataItem(dii[i]);
                        }
                        
                        addedArray_ = seriesDialog_.getAddedList();
                        for (int i = 0; i < addedArray_.length; i++) {
                        	tg.addDataItem(addedArray_[i]);
                        }
                        
                        String graphName = tg.getNodeName();
                        Enumeration<?> enume = graphItem.propertyNames();
                        while (enume.hasMoreElements()) {
                        	String key = (String)enume.nextElement();
                        	if (key.startsWith(graphName))
                        		graphItem.remove(key);
                        }
                        String dataItems = ""; //$NON-NLS-1$
                        DataItemInfo[] list = tg.getDataItemInfoList();
                        for (int i = 0; i<list.length;i++) {
                        	DataItem di = list[i].dataItem;
                        	String header = tg._getDataItemNodeName(di);
                        	if (i > 0)
                        		dataItems += ","; //$NON-NLS-1$
                        	dataItems += graphName+"_"+di.object+"_"+di.node+"_"+di.attribute; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        	if (di.index >= 0)
                        		dataItems += "_"+di.index; //$NON-NLS-1$
                        	graphItem.setProperty(header+".object",di.object); //$NON-NLS-1$
                        	graphItem.setProperty(header+".node",di.node); //$NON-NLS-1$
                        	graphItem.setProperty(header+".attr",di.attribute); //$NON-NLS-1$
                        	graphItem.setProperty(header+".index", String.valueOf(di.index)); //$NON-NLS-1$
                        	graphItem.setProperty(header+".legend", list[i].legend); //$NON-NLS-1$
                        	graphItem.setProperty(header+".color", StringConverter.asString(list[i].color)); //$NON-NLS-1$
                        }
                        graphItem.setProperty(graphName+".dataItems",dataItems); //$NON-NLS-1$
                        updateButtons();
                        trendGraphMgr_.updateGraph();
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
        setEnabledRangeButton(false);
    }

    public void setFocuse(GraphElement ge){
    	currentGraph_.setBorderColor(normalColor_);
		currentGraph_ = ge;
		currentGraph_.setBorderColor(focusedColor_);
		redraw(getLocation().x,getLocation().y,getSize().x,getSize().y,true);
        seriesDialog_.setCurrentGraph(currentGraph_);
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
        setEnabledRangeButton(p != null);
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

    public void setModelList(List<GrxModelItem> list){
    	currentModels_ = list;
    }
    
}

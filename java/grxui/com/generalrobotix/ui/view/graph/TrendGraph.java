/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST) 
 */
package com.generalrobotix.ui.view.graph;

import java.util.*;
import java.awt.*;

/**
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class TrendGraph {

    // -----------------------------------------------------------------
    private static final double MAX_DIV = 5;
    private static final double LOG10 = Math.log(10);

    public static final int SUCCEEDED = 0; 
    public static final int NOT_MATCHED = 1;
    public static final int NOT_SUPPORTED = 2;

    private static final Color[] colorTable_ = {
        Color.green,
        Color.yellow,
        Color.pink,
        Color.cyan,
        Color.magenta,
        Color.red,
        Color.orange,
        Color.blue
    };
    private static final int numColors_;
    static {
        numColors_ = colorTable_.length;
    }
    public Color getGraphColor(int index) {
    	int tmpcolorCounter_ = colorCounter_;
    	tmpcolorCounter_ = tmpcolorCounter_ + index;
    	if (tmpcolorCounter_ >= numColors_) {
            tmpcolorCounter_ = 0;
        }
    	return colorTable_[tmpcolorCounter_];
    }

    private static final HashMap<String, Color> colorMap_ = new HashMap<String, Color>();
    static final HashMap<Color, String> revColorMap_ = new HashMap<Color, String>();
    static {
        colorMap_.put("green",   Color.green);
        colorMap_.put("yellow",  Color.yellow);
        colorMap_.put("pink",    Color.pink);
        colorMap_.put("cyan",    Color.cyan);
        colorMap_.put("magenta", Color.magenta);
        colorMap_.put("red",     Color.red);
        colorMap_.put("orange",  Color.orange);
        colorMap_.put("blue",    Color.blue);

        revColorMap_.put(Color.green,   "green");
        revColorMap_.put(Color.yellow,  "yellow");
        revColorMap_.put(Color.pink,    "pink");
        revColorMap_.put(Color.cyan,    "cyan");
        revColorMap_.put(Color.magenta, "magenta");
        revColorMap_.put(Color.red,     "red");
        revColorMap_.put(Color.orange,  "orange");
        revColorMap_.put(Color.blue,    "blue");
    }

    // -----------------------------------------------------------------
    private int colorCounter_;

    private DataKind dataKind_;
    private XYLineGraph graph_;
    private TrendGraphModel model_;
    private AxisInfo yAxisInfo_;
    private AxisInfo xAxisInfo_;

    private ArrayList<DataItem> dataItemList_;
    private HashMap<String, DataItemInfo> dataItemInfoMap_;
    private HashMap<String, DataSeries> dataSeriesMap_;

//   private DataItemListenerList dataItemListenerList_;

    private String nodeName_;
    //private boolean projectRead_;

//    private DataItemNode[] dataItemNodeArray_;
//    private int dataItemNodeCount_;

    // -----------------------------------------------------------------
    /**
     *
     * @param   graph   XYLineGraph
     * @param   model   TrendGraphModel
     * @param   node    GraphNode
     */
    public TrendGraph(TrendGraphModel model, String node) {
        model_ = model;
        nodeName_ = node;

        xAxisInfo_ = model_.getTimeAxisInfo();
        yAxisInfo_ = new AxisInfo(0, 1);
        yAxisInfo_.unitFont = new Font("dialog", Font.PLAIN, 12);
        yAxisInfo_.unitXOfs = 5;
        yAxisInfo_.unitYOfs = 7;

        dataKind_ = null;

        colorCounter_ = 0;

        dataItemList_ = new ArrayList<DataItem>();
        dataItemInfoMap_ = new HashMap<String, DataItemInfo>();
        dataSeriesMap_ = new HashMap<String, DataSeries>();

 //       dataItemListenerList_ = new DataItemListenerList();

        //projectRead_= false;

//      dataItemNodeArray_ = null;
//      dataItemNodeCount_ = 0;
    }

    // -----------------------------------------------------------------

    public void setGraph(XYLineGraph graph) {
        graph_ = graph;
        graph_.setAxisInfo(
            XYLineGraph.AXIS_BOTTOM,
            xAxisInfo_
        );

        /*
        graph_.setAxisInfo( 
            XYLineGraph.AXIS_LEFT,
            yAxisInfo_
        );
        */
    }

    public XYLineGraph getGraph() {
        return graph_;
    }

    public void repaint() {
        graph_.repaint();
    }

    /**
     * @param   dataKind    String
     * @param   base        double
     * @param   extent      double
     */
    /*
    public void setDataKind(
        String dataKind,
        double base,
        double extent
    ) {
        dataKind_ = GraphProperties.getDataKindFromName(dataKind);
        yAxisInfo_ = new AxisInfo(base, extent);
        _updateDiv();
        yAxisInfo_.unitFont = new Font("dialog", Font.PLAIN, 12);
        yAxisInfo_.unitXOfs = 5;
        yAxisInfo_.unitYOfs = 7;
        yAxisInfo_.unitLabel = dataKind_.unitLabel;
        yAxisInfo_.factor = dataKind_.factor;
        graph_.setAxisInfo(
            XYLineGraph.AXIS_LEFT,
            yAxisInfo_
        );
    }
    */

    /**
     * @param   ai 
     */
    public int addDataItem(
        AttributeInfo ai
    ) {
        //projectRead_ = false;

        DataKind dataKind = GraphProperties.getDataKindFromAttr(ai.fullAttributeName);
        if (dataKind == null) { 
            return NOT_SUPPORTED;
        }
        if (dataKind_ != null) {
            if (!dataKind.equals(dataKind_)) {
                return NOT_MATCHED;
            }
        } else {
            dataKind_ = dataKind;
            //yAxisInfo_ = new AxisInfo(dataKind_.base, dataKind_.extent);
            //yAxisInfo_.unitFont = new Font("dialog", Font.PLAIN, 12);
            //yAxisInfo_.unitXOfs = 5;
            //yAxisInfo_.unitYOfs = 7;
            yAxisInfo_.base = dataKind_.base;
            yAxisInfo_.extent = dataKind_.extent;
            yAxisInfo_.unitLabel = dataKind_.unitLabel;
            yAxisInfo_.factor = dataKind_.factor;
            _updateDiv();
            graph_.setAxisInfo(
                XYLineGraph.AXIS_LEFT,
                yAxisInfo_
            );
//            SEString dk = new SEString(dataKind_.name);
            //System.out.println(nodeName_ + "." + GraphNode.DATA_KIND + "=" + dk.toString());
//            world_.updateAttribute(nodeName_ + "." + GraphNode.DATA_KIND + "=" + dk.toString());
//            SEDoubleArray vr = new SEDoubleArray(new double[]{dataKind_.base, dataKind_.base + dataKind_.extent});
//            world_.updateAttribute(nodeName_ + "." + GraphNode.V_RANGE + "=" + vr.toString());
        }
        int count = (ai.length > 0 ? ai.length : 1); 
        for (int i = 0; i < count; i++) {
            int index = ((ai.length < 1) ? -1 : i);
            DataItem di = new DataItem(
                ai.objectName,
                ai.nodeName,
                ai.attribute,
                index
            );
            Color color = colorTable_[colorCounter_];
            String legend = di.toString();
            DataItemInfo dii = new DataItemInfo(di, color, legend);
            if (_addDataItem(dii)) {
                if (++colorCounter_ >= numColors_) {
                    colorCounter_ = 0;
                }
//                for (int j = 0; j < dataItemListenerList_.size(); j++) {
//                    DataItemListener listener = dataItemListenerList_.getListener(j);
//                    listener.dataItemAdded(dii);
//                }
            }
        }

        return SUCCEEDED;
    }

    /**
     *
     * @param   dataItem    DataItem 
     */
    public void removeDataItem(
        DataItem dataItem
    ) {
        model_.removeDataItem(dataItem, true);
        String key = dataItem.toString();
        DataItemInfo dii = (DataItemInfo)dataItemInfoMap_.remove(key);
        DataSeries ds = (DataSeries)dataSeriesMap_.get(key);
        graph_.removeDataSeries(ds);
        dataSeriesMap_.remove(key);
        int ind = dataItemList_.indexOf(dataItem);
        dataItemList_.remove(ind);
        if (dataItemList_.size() < 1) {
            dataKind_ = null;
            //yAxisInfo_ = null; 
            graph_.setAxisInfo(
                XYLineGraph.AXIS_LEFT,
                null
            );
//            SEString dk = new SEString("");
//            world_.updateAttribute(nodeName_ + "." + GraphNode.DATA_KIND + "=" + dk.toString());
//            SEDoubleArray vr = new SEDoubleArray(new double[]{0, 1});
//            world_.updateAttribute(nodeName_ + "." + GraphNode.V_RANGE + "=" + vr.toString());
        }
//        for (int i = 0; i < dataItemListenerList_.size(); i++) {
//            DataItemListener listener = dataItemListenerList_.getListener(i);
//            listener.dataItemRemoved(dii);
//        }
    }

    /**
     *
     * @return  int 
     */
    public int getNumDataItems() {
        return dataItemList_.size();
    }

    /**
     *
     * @return  DataItemInfo[] 
     */
    public DataItemInfo[] getDataItemInfoList() {
        int size = dataItemList_.size();
        DataItemInfo[] infoArray = new DataItemInfo[size];
        for (int i = 0; i < size; i++) {
            Object key = dataItemList_.get(i);
            infoArray[i] = (DataItemInfo)dataItemInfoMap_.get(key.toString());
        }

        return infoArray;
    }

    /**
     *
     * @return  DataItemInfo[]
     */
   public void setDataItemInfo(
        DataItemInfo dii
    ) {
//        DataSeries ds = (DataSeries)dataSeriesMap_.get(dii.dataItem.toString());
//        Color pc = graph_.getStyle(ds);
//        String pl = graph_.getLegendLabel(ds);
//        String diNodeName = _getDataItemNodeName(dii.dataItem);
        _setDataItemInfo(dii);
 /*       if (!pc.equals(dii.color)) { 
           world_.updateAttribute(
                diNodeName + "." + DataItemNode.COLOR + "=" + (String)revColorMap_.get(dii.color)
            );
        }
        if (!pl.equals(dii.legend)) {
            world_.updateAttribute(
                diNodeName + "." + DataItemNode.LEGEND + "=" + dii.legend
            );
        }*/
    }

    private void _setDataItemInfo(
        DataItemInfo dii
    ) {
        DataSeries ds = (DataSeries)dataSeriesMap_.get(dii.dataItem.toString());
        graph_.setStyle(ds, dii.color);
        graph_.setLegendLabel(ds, dii.legend);
    }

    /**
     *
     * @param   base    double
     * @param   extent  double
     */
    public void setRange(
        double base,
        double extent
    ) {
        _setRange(base, extent);
//        SEDoubleArray vr = new SEDoubleArray(new double[]{base, base + extent});
//        world_.updateAttribute(nodeName_ + "." + GraphNode.V_RANGE + "=" + vr.toString());
    }

    /**
     *
     * @return 
     */
    public String getUnitLabel() {
        return yAxisInfo_.unitLabel;
    }

    /**
     *
     * @return
     */
    public double getBase() {
        return yAxisInfo_.base;
    }

    /**
     *
     * @return  
     */
    public double getExtent() {
        return yAxisInfo_.extent;
    }

    /**
     *
     * @param   timeRange   double
     * @param   markerPos   double
     */
    public void setTimeRangeAndPos(
        double timeRange,
        double markerPos
    ) {
        _setTimeRangeAndPos(timeRange, markerPos);
//        SEDoubleArray tr = new SEDoubleArray(new double[]{timeRange, markerPos});
        //SEDouble hr = new SEDouble(timeRange);
        //SEDouble mp = new SEDouble(markerPos);
//        world_.updateAttribute(nodeName_ + "." + GraphNode.TIME_RANGE + "=" + tr.toString());
    }

    /**
     *
     * @param   timeRange   double
     */
    public void setTimeRange(
        double timeRange
    ) {
        _setTimeRange(timeRange);
//        SEDouble hr = new SEDouble(timeRange);
//        world_.updateAttribute(nodeName_ + "." + GraphNode.H_RANGE + "=" + hr.toString());
    }

    /**
     *
     * @param   markerPos   double 
     */
    public void setMarkerPos(double markerPos) {
        _setMarkerPos(markerPos);
//        SEDouble mp = new SEDouble(markerPos);
 //       world_.updateAttribute(nodeName_ + "." + GraphNode.MARKER_POS + "=" + mp.toString());
    }

    /**
     *
     * @param   listener
     */
//    public void addDataItemListener(DataItemListener listener) {
//        dataItemListenerList_.addListener(listener);
//    }

    /**
     *
     * @param   listener
     */
  /*  public void removeDataItemListener(DataItemListener listener) {
        dataItemListenerList_.removeListener(listener);
    }*/

    // -----------------------------------------------------------------
    // WorldReplaceListener
    /**
     *
     * @param   world   
     */
/*    public void replaceWorld(SimulationWorld world) {
        world_ = world;
        while (dataItemList_.size() > 0) {
            DataItem di = (DataItem)dataItemList_.get(0);
            model_.removeDataItem(di, false);
            String key = di.toString();
            dataItemInfoMap_.remove(key);
            DataSeries ds = (DataSeries)dataSeriesMap_.get(key);
            graph_.removeDataSeries(ds);
            dataSeriesMap_.remove(key);
            dataItemList_.remove(0);
        }

        //projectRead_ = true;

        dataKind_ = null;

        world_.addAttributeListener(this);
        world_.addNodeListener(this);
    }
*/
    // -----------------------------------------------------------------
    // AttributeListener
    /**
     *
     * @param   node
     * @param   attribute
     * @param   value
     */
 /*   public void attributeChanged(
        SimulationNode node,
        String attribute,
        StringExchangeable value
    ) {
        String nodeType = node.getNodeName();
        String name = node.getName();
        if (nodeType.equals(GraphNode.NODE_TYPE)
            && name.equals(nodeName_)
        ) {
            if (attribute.equals(GraphNode.TIME_RANGE)) {
                // timeRange
                //System.out.println("set Time Range: " + value);
                SEDoubleArray tr = (SEDoubleArray)value;
                _setTimeRangeAndPos(tr.doubleValue(0), tr.doubleValue(1));
            } else if (attribute.equals(GraphNode.DATA_KIND)) {
                // dataKind
                _setDataKind(value.toString());
            } else if (attribute.equals(GraphNode.H_RANGE)) {
                // hRange
                _setTimeRange(((SEDouble)value).doubleValue());
                //System.out.println("set HRange: " + value);
            } else if (attribute.equals(GraphNode.MARKER_POS)) {
                // markerPos
                _setMarkerPos(((SEDouble)value).doubleValue());
            } else if (attribute.equals(GraphNode.V_RANGE)) {
                // vRange
                SEDoubleArray vr = (SEDoubleArray)value;
                _setRange(
                    vr.doubleValue(0),
                    vr.doubleValue(1) - vr.doubleValue(0)
                );
            }
        } else if (nodeType.equals(DataItemNode.NODE_TYPE)
            && name.startsWith(nodeName_)
        ) {
            //System.out.println("### data item attribute changed = " + attribute);
            DataItemNode din = (DataItemNode)node;
            if (attribute.equals("color")) {
                String diname = din.getDataItemName();
                if (diname == null) {
                    //System.out.println("color $$$$ Data item name is not decided.");
                    return;
                }
                DataItemInfo dii = (DataItemInfo)dataItemInfoMap_.get(din.getDataItemName());
                if (dii == null) {
                    //System.out.println("color $$$$ Data item info is not found. (" + diname + ")");
                    return;
                }
                dii.color = (Color)colorMap_.get(value.getValue());
                _setDataItemInfo(dii);
				//
                graph_.repaint();
                graph_.getLegendPanel().repaint();
            }
            if (attribute.equals("legend")) {
                String diname = din.getDataItemName();
                if (diname == null) {
                    //System.out.println("legend $$$$ Data item name is not decided.");
                    return;
                }
                DataItemInfo dii = (DataItemInfo)dataItemInfoMap_.get(din.getDataItemName());
                if (dii == null) {
                    //System.out.println("legend $$$$ Data item info is not found. (" + diname + ")");
                    return;
                }
                dii.legend = value.toString();
                _setDataItemInfo(dii);
                graph_.repaint();
                graph_.getLegendPanel().repaint();
            }
        }
    }*/

    // -----------------------------------------------------------------
    /**
     *
     * @param   dataItemInfo    DataItemInfo
     * @return 
     */
    private boolean _addDataItem(
        DataItemInfo dataItemInfo
    ) {
        DataItem dataItem = dataItemInfo.dataItem;
        String key = dataItem.toString();

        DataSeries ds = model_.addDataItem(dataItem);
        if (ds == null) {
            return false;
        }

        dataSeriesMap_.put(key, ds);

        if (dataItemInfoMap_.containsKey(key)) {
            return false;
        }

        dataItemInfoMap_.put(key, dataItemInfo);

        dataItemList_.add(dataItem);

        graph_.addDataSeries(
            ds, xAxisInfo_, yAxisInfo_,
            dataItemInfo.color,
            dataItemInfo.legend
        );

        return true;
    }

    /**
     *
     * @param   dataKind    String
     */
/*  
     private void _setDataKind( String dataKind ) {
        if (dataKind_ != null) {
            if (dataKind.equals(dataKind_.name)) {
                return;
            }
        }
        dataKind_ = GraphProperties.getDataKindFromName(dataKind);
        if (dataKind_ != null) {
            //yAxisInfo_.base = dataKind_.base;
            //yAxisInfo_.extent = dataKind_.extent;
            yAxisInfo_.unitLabel = dataKind_.unitLabel;
            yAxisInfo_.factor = dataKind_.factor;
            _updateDiv();
            graph_.setAxisInfo(
                XYLineGraph.AXIS_LEFT,
                yAxisInfo_
            );
        }
    }
*/
    /**
     *
     * @param   base    double 
     * @param   extent  double 
     */
    private void _setRange(
        double base,
        double extent
    ) {
        yAxisInfo_.base = base;
        yAxisInfo_.extent = extent;
        _updateDiv();
    }

    /**
     * @param   dii 
     * @return 
     */
    String _getDataItemNodeName(DataItem di) {
        StringBuffer sb = new StringBuffer(nodeName_);
        sb.append('.');
        sb.append(nodeName_);
        if (di.object != null) {
            sb.append('_');
            sb.append(di.object);
        }
        sb.append('_');
        sb.append(di.node);
        sb.append('_');
        sb.append(di.attribute);
        if (di.index >= 0) {
            sb.append('_');
            sb.append(di.index);
        }

        return sb.toString();
    }
    
    String getNodeName() {
    	return nodeName_;
    }

    /**
     *
     * @param   timeRange   double
     * @param   markerPos   double
     */
    private void _setTimeRangeAndPos(
        double timeRange,
        double markerPos
    ) {
        model_.setRangeAndPos(timeRange, markerPos);
    }

    /**
     *
     * @param   timeRange   double
     */
    private void _setTimeRange(
        double timeRange
    ) {
        model_.setTimeRange(timeRange);
    }

    /**
     *
     * @param   markerPos   double
     */
    private void _setMarkerPos(double markerPos) {
        model_.setMarkerPos(markerPos);
    }

    /**
     *
     */
    private void _updateDiv() {
        double sMin = yAxisInfo_.extent / MAX_DIV;
        int eMin = (int)Math.floor(Math.log(sMin) / LOG10);
        double step = 0;
        String format = "0";
        int e = eMin;
        boolean found = false;
        while (!found) {
            int m = 1;
            for (int i = 1; i <= 3; i++) {
                step = m * Math.pow(10.0, e);
                if (sMin <= step) { // && step <= sMax
                    if (e < 0) {
                        char[] c = new char[-e + 2];
                        c[0] = '0';
                        c[1] = '.';
                        for (int j = 0; j < -e; j++) {
                            c[j + 2] = '0';
                        }
                        format = new String(c);
                    }
                    found = true;
                    break;
                }
                m += (2 * i - 1);
            }
            e++;
        }
        yAxisInfo_.tickEvery = step;
        yAxisInfo_.labelEvery = step;
        yAxisInfo_.gridEvery = step;
        yAxisInfo_.labelFormat = format;
    }
}

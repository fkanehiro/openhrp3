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
 * TrendGraphManager.java
 *
 * @author  Kernel, Inc.
 * @version  1.0 (Sun Sep 23 2001)
 *
 */

package com.generalrobotix.ui.view.graph;
import com.generalrobotix.ui.item.GrxWorldStateItem;
public class TrendGraphManager {

    //--------------------------------------------------------------------
    //  クラス変数
    private static TrendGraphManager this_;

    //--------------------------------------------------------------------
    //  インスタンス変数
    public TrendGraphModel trendGraphModel_;
    private TrendGraph[] trendGraph_;
   //private int mode_;
    private long totalTime_;

    //--------------------------------------------------------------------
    //  コンストラクタ
    public TrendGraphManager(int numGraph) {
        trendGraphModel_ = new TrendGraphModel();
        //Simulator simulator = Simulator.getInstance();
        //simulator.addWorldReplaceListener(trendGraphModel_);

        trendGraph_ = new TrendGraph[numGraph];
        for (int i = 0; i < trendGraph_.length; i ++) {
            StringBuffer graphName = new StringBuffer("Graph");
            graphName.append(i);
            trendGraph_[i] = new TrendGraph(trendGraphModel_, graphName.toString());
            //simulator.addWorldReplaceListener(trendGraph_[i]);
        }
        
        //simulator.addWorldReplaceListener(this);
    }

    //--------------------------------------------------------------------
    // クラスメソッド
    public static TrendGraphManager createInstance(int numGraph) {
        this_ = new TrendGraphManager(numGraph);
        return this_;
    }

    public static TrendGraphManager getInstance() {
        return this_;
    }

    //--------------------------------------------------------------------
    // パブリックメソッド
    public int getNumGraph() {
        return trendGraph_.length;
    }

    public TrendGraph getTrendGraph(int index) {
        return trendGraph_[index];
    }

    public String getTrendGraphName(int index) {
        if (index < 0 || index >= trendGraph_.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        StringBuffer graphName = new StringBuffer("Graph");
        graphName.append(index);
        return graphName.toString();
    }

    public void setMode(int mode) {
        //mode_ = mode;
        trendGraphModel_.setMode(mode);
    }

    public void setTotalTime(long totalTime) {
        trendGraphModel_.setTotalTime(totalTime);
        //double timeRange = trendGraphModel_.getTimeRange();
        //if (timeRange > (double)totalTime / TrendGraphModel.TIME_SCALE) {
        //    trendGraph_[0].setTimeRange((double)totalTime / TrendGraphModel.TIME_SCALE);
        //}
    }

    public void setCurrentTime(long currentTime) {
        trendGraphModel_.setCurrentTime(currentTime);
    }

    public void setStepTime(long stepTime) {
        trendGraphModel_.setStepTime(stepTime);
    }

    public double getTotalTime() {
        return trendGraphModel_.getTotalTime();
    }

    public double getStepTime() {
        return trendGraphModel_.getStepTime();
    }

    public double getTimeRange() {
        return trendGraphModel_.getTimeRange();
    }

    public double getMarkerPos() {
        return trendGraphModel_.getMarkerPos();
    }

    public void setup(
        long    totalTime,
        long    stepTime,
        long    currentTime,
        double  timeRange,
        double  markerPos
    ) {
        trendGraphModel_.setup(totalTime, stepTime, currentTime, timeRange, markerPos);
    }

    public void setup(
        long    totalTime,
        long    currentTime
    ) {
        trendGraphModel_.setup(totalTime, currentTime);
    }

    public void setup(
        long    totalTime,
        long    stepTime,
        long    currentTime
    ) {
        trendGraphModel_.setup(totalTime, stepTime, currentTime);
    }

    public void reread() {
        trendGraphModel_.reread();
    }

    //---------------------------------------------------------------
    // WorldReplaceListenerの実装
    /**
     * 世界の更新
     *
     * @param   world   世界
     */
    //public void replaceWorld(SimulationWorld world) {
     //   world.addWorldTimeListener(this);
    //}

    // -----------------------------------------------------------------
    // WorldTimeListenerの実装
    /**
     * 時刻変化
     * 
     * @param   time    時刻
     */
    public void worldTimeChanged(Time time) {
        //System.out.println("@@@ TimeChanged @@@ " + time.getUtime());

  //      if (mode_ == GUIManager.GUI_STATUS_EXEC) {
//            trendGraphModel_.shiftCurrentTime();
  //      } else if (mode_ == GUIManager.GUI_STATUS_PLAY) {
  //          trendGraphModel_.setCurrentTime(time.getUtime());
  //      }

        //trendGraphModel_.setTotalTime(totalTime_);
        trendGraphModel_.setDataTermTime(totalTime_);
        trendGraphModel_.setCurrentTime(time.getUtime());

        for (int i = 0; i < trendGraph_.length; i ++) {
            trendGraph_[i].repaint();
        }
    }

    public void simulationTimeChanged(Time time) {
        totalTime_ = time.getUtime();
/*
        setTotalTime(time.getUtime());

        for (int i = 0; i < trendGraph_.length; i ++) {
            trendGraph_[i].repaint();
        }
*/
    }
    
    public void setWorldState(GrxWorldStateItem world) {
    	trendGraphModel_.setWorldState(world);
    }
}

/**
 * SimulationTime.java
 *
 * SimulationTimeクラスは、シミュレーションに使用する時間情報を定義します。
 * ステップ時間はマイクロ秒
 *
 * @author  Kernel Co.,Ltd.
 * @version 1.0 (2001/3/1)
 */

package com.generalrobotix.ui.view.graph;
public class SimulationTime {
    protected Time totalTime_;
    protected Time currentTime_;
    protected Time startTime_;
    protected Time timeStep_;
    protected Time viewUpdateStep_;

    /**
     * コンストラクタ
     *
     * @param   totalTime    合計時間（ミリ秒）
     * @param   timeStep     ステップ時間（マイクロ秒）
     */
    public SimulationTime() {
        totalTime_ = new Time();
        timeStep_ = new Time();
        viewUpdateStep_ = new Time();
        currentTime_ = new Time();
        startTime_ = new Time();
    }

    public SimulationTime(
        Time totalTime, // [msec]
        Time timeStep,   // [usec]
        Time viewUpdateStep   // [usec]
    ) {
        totalTime_ = new Time(totalTime.getDouble());
        timeStep_ = new Time(timeStep.getDouble());
        viewUpdateStep_ = new Time(viewUpdateStep.getDouble());
        currentTime_ = new Time(0,0);
        startTime_ = new Time(0,0);
    }

    public SimulationTime(
        double totalTime, // [msec]
        double timeStep,   // [usec]
        double viewUpdateStep   // [usec]
    ) {
        totalTime_ = new Time(totalTime);
        timeStep_ = new Time(timeStep);
        viewUpdateStep_ = new Time(viewUpdateStep);
        currentTime_ = new Time(0,0);
        startTime_ = new Time(0,0);
    }

    public void set(SimulationTime time) { 
        totalTime_.set(time.totalTime_);
        timeStep_.set(time.timeStep_);
        viewUpdateStep_.set(time.viewUpdateStep_);
        currentTime_.set(time.currentTime_);
        startTime_.set(time.startTime_);
    }

    /**
     * ステップ時間の加算（現在時間更新）
     *   @return   合計時間に達するまでTrue
     */
    public boolean inc() {
        currentTime_.add(timeStep_);
        if (currentTime_.msec_ > totalTime_.msec_) {
            currentTime_.set(totalTime_);
            return false;
        } else if (currentTime_.msec_ == totalTime_.msec_) {
            if (currentTime_.usec_ > totalTime_.usec_) {
                currentTime_.set(totalTime_);
                return false;
            }
        }

        return true;
    }

    /**
     * 開始時間の設定
     *   @return time → 時間（秒）
     */
    public void setStartTime(double time) {
        startTime_.set(time);
    }

    public void setCurrentTime(double time) {
        currentTime_.set(time);
    }

    public void setTotalTime(double time) {
        totalTime_.set(time);
    }

    public void setTimeStep(double time) {
        timeStep_.set(time);
    }

    public void setViewUpdateStep(double time) {
        viewUpdateStep_.set(time);
    }


    /**
     * 開始時間の取得
     *   @return 開始時間（秒）
     */
    public double getStartTime() {
        return startTime_.getDouble();
    }

    /**
     * 現在時間の取得
     *   @return 現在時間（秒）
     */
    public double getCurrentTime() {
        return currentTime_.getDouble();
    }

    /**
     * 合計時間の取得
     *   @return 合計時間（秒）
     */
    public double getTotalTime() {
        return totalTime_.getDouble();
    }

    public double getTimeStep() {
        return timeStep_.getDouble();
    }

    public double getViewUpdateStep() {
        return viewUpdateStep_.getDouble();
    }
}


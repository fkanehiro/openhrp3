package com.generalrobotix.ui.view.graph;

import java.util.*;
import java.awt.*;

/**
 * トレンドグラフモデルクラス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class TrendGraphModel
  //  implements WorldTimeListener, WorldReplaceListener
{
    public  static final double TIME_SCALE = 1000000;   // タイムカウントの倍率(1μsec)
    private static final double MAX_DIV = 10;   // 時間軸の最大分割数
    private static final double LOG10 = Math.log(10);

    private long stepTimeCount_;    // 時間刻み幅(カウント)
    private long totalTimeCount_;   // 総時間(カウント)
    private long currentTimeCount_; // 現在時刻(カウント)
    private long dataTermCount_;    // データの終端の時刻(カウント)

    private double stepTime_;       // 時間刻み幅(秒)
    private double totalTime_;      // 総時間(秒)
    private double currentTime_;    // 現在時刻(秒)

    private double timeRange_;      // 時間レンジ(秒)
    private double markerPos_;      // 現在時刻マーカ位置(0.0から1.0で指定)

    private double baseTime_;       // グラフ左端時刻(秒)

    public int sampleCount_;   // グラフサンプル数
    public long baseCount_;    // データ開始位置

    private AxisInfo timeAxisInfo_; // 時間軸情報

    //private HashMap dataSeriesMap_; // データ系列一覧
    private HashMap<String, ArrayList<String> > dataItemCount_; // データアイテムカウンタ
                                    // (どのデータ系列にいくつのデータアイテムが割り当てられているか)

    private HashMap<String, DataModel> dataModelMap_;   // データモデル一覧
    private DataModel[] dataModelArray_;    // データモデル一覧

    //private DummyDataSource dumSource_; // ★ダミーデータソース
    private LogManager logManager_; // ログマネージャ

    private boolean markerFixed_;   // 現在時刻マーカ固定フラグ
    private double fixedMarkerPos_; // 固定マーカ位置

    private int mode_;  // モード

  //  private SimulationWorld world_;
    /**
     * コンストラクタ
     *
     * @param   logManager  ログマネージャ
     */
    public TrendGraphModel() {
        stepTimeCount_ = 1000;  // 時間刻み幅   ★初期設定ファイルから読む?
        totalTimeCount_ = 10000000;  // 総時間   ★初期設定ファイルから読む?
        currentTimeCount_ = 0;  //1000000;
        stepTime_ = stepTimeCount_ / TIME_SCALE;
        totalTime_ = totalTimeCount_ / TIME_SCALE;
        currentTime_ = currentTimeCount_ / TIME_SCALE;

        timeRange_ = 1;     // 時間レンジ   ★初期設定ファイルから読む?

        fixedMarkerPos_ = 0.8;   // 現在時刻マーカ位置   ★初期設定ファイルから読む?
        markerFixed_ = (timeRange_ < totalTime_);
        if (markerFixed_) { // マーカ固定?
            markerPos_ = fixedMarkerPos_;
            baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
            sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
            baseCount_ = Math.round(baseTime_ / stepTime_); // - 1; // データ開始位置
        } else {
            markerPos_ = currentTime_ / timeRange_;
            baseTime_ = 0;
            sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
            baseCount_ = 0;
        }

        // 軸情報生成
        timeAxisInfo_ = new AxisInfo(baseTime_, timeRange_);
        _updateDiv();    // 軸分割更新
        timeAxisInfo_.min = 0;
        timeAxisInfo_.max = totalTime_;
        timeAxisInfo_.minLimitEnabled = true;
        timeAxisInfo_.maxLimitEnabled = true;
        timeAxisInfo_.unitFont = new Font("dialog", Font.PLAIN, 12);
        timeAxisInfo_.unitXOfs = 12;
        timeAxisInfo_.unitYOfs = 0;
        timeAxisInfo_.unitLabel = "(sec)";
        timeAxisInfo_.markerPos = markerPos_;
        timeAxisInfo_.markerColor = Color.cyan;  //new Color(255, 128, 128);
        timeAxisInfo_.markerVisible = true;

        //dataSeriesMap_ = new HashMap();
        dataItemCount_ = new HashMap<String, ArrayList<String> >();
        dataModelMap_ = new HashMap<String, DataModel>();
        dataModelArray_ = null;

        mode_ = GUIStatus.EDIT_MODE;

      //  logManager_ = LogManager.getInstance();

        // ★ダミーデータソース
        /*
        dumSource_ = new DummyDataSource(10000);
        dumSource_.addDataItem(
            new DataItem("rob1", "LARM_JOINT3", "angle", -1),
            -3.14, 3.14
        );

        dumSource_.addDataItem(
            new DataItem("rob1", "LARM_JOINT3", "absPos", 0),
            -10, 10
        );
        dumSource_.addDataItem(
            new DataItem("rob1", "LARM_JOINT3", "absPos", 1),
            -10, 10
        );
        dumSource_.addDataItem(
            new DataItem("rob1", "LARM_JOINT3", "absPos", 2),
            -10, 10
        );

        dumSource_.addDataItem(
            new DataItem(null, "VLINK1", "contactForce", 0),
            0, 1000
        );
        dumSource_.addDataItem(
            new DataItem(null, "VLINK1", "contactForce", 1),
            0, 1000
        );
        dumSource_.addDataItem(
            new DataItem(null, "VLINK1", "contactForce", 2),
            0, 1000
        );

        dumSource_.addDataItem(
            new DataItem("rob2", "RLEG_JOINT2", "absComPos", 0),
            -10, 10
        );
        dumSource_.addDataItem(
            new DataItem("rob2", "RLEG_JOINT2", "absComPos", 1),
            -10, 10
        );
        dumSource_.addDataItem(
            new DataItem("rob2", "RLEG_JOINT2", "absComPos", 2),
            -10, 10
        );

        dumSource_.createData();
        */
    }

    // -----------------------------------------------------------------
    // メソッド
    /**
     * モードの設定
     *   GUIManager.GUI_STATUS_EDIT:   編集モード
     *   GUIManager.GUI_STATUS_EXEC:   シミュレーション実行モード
     *   GUIManager.GUI_STATUS_PLAY:   再生モード
     * 
     * @param   mode    モード
     */
    public void setMode(int mode) {
        mode_ = mode;
    }

    /**
     * 時間軸情報取得
     *
     * @return  AxisInfo    時間軸情報
     */
    public AxisInfo getTimeAxisInfo() {
        return timeAxisInfo_;
    }

    /**
     * 時間刻み幅設定
     *
     * @param   stepTime    long    時間刻み幅(カウント)
     */
    public void setStepTime(
        long stepTime
    ) {
        stepTimeCount_ = stepTime;
        stepTime_ = stepTimeCount_ / TIME_SCALE;

        sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数(前後2サンプルを追加)
        baseCount_ = Math.round(baseTime_ / stepTime_); //- 1; // データ開始位置

        // 全データ系列の更新
        Iterator itr = dataModelMap_.values().iterator();
        while (itr.hasNext()) {
            DataModel dm = (DataModel)itr.next();
            dm.dataSeries.setSize(sampleCount_);
            dm.dataSeries.setXStep(stepTime_);
        }

        // データを読み直す必要はない(時間刻み幅が設定されるのは編集モードだから)
    }

    /**
     * 総時間設定
     *
     * @param   totalTime   long    総時間(カウント)
     */
    public void setTotalTime(long totalTime) {
        totalTimeCount_ = totalTime;
        totalTime_ = totalTimeCount_ / TIME_SCALE;
        timeAxisInfo_.max = totalTime_; // 軸最大値の更新

        //System.out.println("totalTime="+totalTime);

        if (markerFixed_) {
            markerFixed_ = (timeRange_ < totalTime_);
            if (markerFixed_) { // fixed -> fixed ?
                return;
            } else {    // fixed -> not fixed ?
                markerPos_ = currentTime_ / timeRange_;
                baseTime_ = 0;
                sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
                baseCount_ = 0;
            }
        } else {
            markerFixed_ = (timeRange_ < totalTime_);
            if (markerFixed_) { // non fixed -> fixed ?
                markerPos_ = fixedMarkerPos_;
                baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
                sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
                baseCount_ = Math.round(baseTime_ / stepTime_); // - 1; // データ開始位置
            } else {    // not fixed -> not fixed ?
                markerPos_ = currentTime_ / timeRange_;
                baseTime_ = 0;
                sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
                baseCount_ = 0;
            }
        }

        timeAxisInfo_.base = baseTime_;
        timeAxisInfo_.extent = timeRange_;
        timeAxisInfo_.markerPos = markerPos_;
        _updateDiv();

        // 全データ系列の更新
        Iterator itr = dataModelMap_.values().iterator();
        while (itr.hasNext()) {
            DataModel dm = (DataModel)itr.next();
            dm.dataSeries.setSize(sampleCount_);
            //dm.dataSeries.setXOffset(baseTime_);  ★これではダメ
            dm.dataSeries.setXOffset(baseCount_ * stepTime_);
        }

        // データを読み直す
        //   時間レンジを変更できるのは編集モード、再生モードのみ
        //   再生モードの場合のみデータを読み直す
        if (dataModelArray_ == null) {
            return;
        }
        if (mode_ == GUIStatus.EDIT_MODE) {
            return;
        }
        // ★
        //System.out.println("baseCount_=" + baseCount_);
        logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
    }

    /**
     * 総時間および時間刻み幅設定
     *
     * @param   totalTime   総時間(カウント)
     * @param   stepTime    時間刻み幅(カウント)
     */
    /*
    public void setTotalAndStepTime(
        long totalTime,
        int stepTime
    ) {
    }
    */

    /**
     * 時間レンジおよびマーカ位置設定
     *
     * @param   timeRange   double  時間レンジ(秒)
     * @param   markerPos   double  マーカ位置
     */
    public void setRangeAndPos(
        double timeRange,
        double markerPos
    ) {
        if (fixedMarkerPos_ == markerPos
            && timeRange_ == timeRange) {
            return;
        }
        _setTimeRange(timeRange, markerPos);
    }

    /**
     * 時間レンジ設定
     *
     * @param   timeRange   double  時間レンジ(秒)
     */
    public void setTimeRange(
        double timeRange
    ) {
        if (timeRange_ == timeRange) {
            return;
        }
        _setTimeRange(timeRange, fixedMarkerPos_);
    }

    /**
     * 時間レンジ設定
     *
     * @param   timeRange   double  時間レンジ(秒)
     */
    private void _setTimeRange(
        double timeRange,
        double markerPos
    ) {
        timeRange_ = timeRange;
        fixedMarkerPos_ = markerPos;

        markerFixed_ = (timeRange_ < totalTime_);
        if (markerFixed_) { // マーカ固定?
            markerPos_ = fixedMarkerPos_;
            baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
            sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
            baseCount_ = Math.round(baseTime_ / stepTime_); // - 1; // データ開始位置
        } else {
            markerPos_ = currentTime_ / timeRange_;
            baseTime_ = 0;
            sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
            baseCount_ = 0;
        }
        //System.out.println("*** base= " + (baseTime_ / stepTime_));
        //System.out.println("*** cnt = " + baseCount_);

        timeAxisInfo_.base = baseTime_;
        timeAxisInfo_.extent = timeRange_;
        timeAxisInfo_.markerPos = markerPos_;
        _updateDiv();

        // 全データ系列の更新
        Iterator itr = dataModelMap_.values().iterator();
        while (itr.hasNext()) {
            DataModel dm = (DataModel)itr.next();
            dm.dataSeries.setSize(sampleCount_);
            //dm.dataSeries.setXOffset(baseTime_);  ★これではダメ
            dm.dataSeries.setXOffset(baseCount_ * stepTime_);
        }

        // データを読み直す
        //   時間レンジを変更できるのは編集モード、再生モードのみ
        //   再生モードの場合のみデータを読み直す
        if (dataModelArray_ == null) {
            return;
        }
        if (mode_ == GUIStatus.EDIT_MODE) {
            return;
        }
        // ★
        //System.out.println("baseCount_=" + baseCount_);
        logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
    }

    /**
     * 時間レンジ取得
     *
     * @param   double  時間レンジ(秒)
     */
    public double getTimeRange() {
        return timeRange_;
    }

    /**
     * マーカ位置設定
     *
     * @param   markerPos   double  マーカ位置
     */
    public void setMarkerPos(double markerPos) {
        if (markerPos == fixedMarkerPos_) {
            return;
        }
        fixedMarkerPos_ = markerPos;

        if (!markerFixed_) { // マーカ固定でない?
            return; // なにもしない
        }

        markerPos_ = fixedMarkerPos_;
        baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
        long oldBaseCount = baseCount_;
        baseCount_ = Math.round(baseTime_ / stepTime_); // - 1; // データ開始位置

        timeAxisInfo_.base = baseTime_;
        timeAxisInfo_.markerPos = markerPos_;
        //_updateDiv();

        int diff = (int)(baseCount_ - oldBaseCount);

        // 全データ系列の移動
        Iterator itr = dataModelMap_.values().iterator();
        while (itr.hasNext()) {
            DataSeries ds = ((DataModel)itr.next()).dataSeries;
            ds.shift(diff);
            //System.out.println("shift=" + diff);
        }

        // データを読み直す
        //   マーカ位置を変更できるのは編集モード、再生モードのみ
        //   再生モードの場合のみ差分データを読み出す
        if (diff == 0 || dataModelArray_ == null) {
            return;
        }
        // ★
        if (mode_ == GUIStatus.EDIT_MODE) {
            return;
        }
        if (diff > 0) {
            if (diff >= sampleCount_) {
                logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
            } else {
                logManager_.getData(baseCount_, sampleCount_ - diff, diff, dataModelArray_);
            }
        } else {
            if (-diff >= sampleCount_) {
                logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
            } else {
                logManager_.getData(baseCount_, 0, -diff, dataModelArray_);
            }
        }
    }

    /**
     * マーカ位置取得
     *
     * @param   double  マーカ位置
     */
    public double getMarkerPos() {
        return fixedMarkerPos_;
    }

    /**
     * ステップ時間取得
     *
     * @param   double  ステップ時間
     */
    public double getStepTime() {
        return stepTime_;
    }

    /**
     * トータル時間取得
     *
     * @param   double  トータル時間
     */
    public double getTotalTime() {
        return totalTime_;
    }

    /**
     * 現在時刻設定
     *
     * @param   long    currentTime 現在時刻(カウント)
     */
    public void setCurrentTime(long currentTime) {
        //System.out.println("setCurrentTime(): crrentTime=" + currentTime);

        currentTimeCount_ = currentTime;
        currentTime_ = currentTimeCount_ / TIME_SCALE;

        if (!markerFixed_) {    // マーカ固定でない?
            markerPos_ = currentTime_ / timeRange_;
            timeAxisInfo_.markerPos = markerPos_;
            return;
        }

        baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
        long oldBaseCount = baseCount_;
        baseCount_ = Math.round(baseTime_ / stepTime_); // - 1; // データ開始位置
        timeAxisInfo_.base = baseTime_;

        int diff = (int)(baseCount_ - oldBaseCount);

        // 全データ系列の移動
        Iterator itr = dataModelMap_.values().iterator();
        while (itr.hasNext()) {
            DataSeries ds = ((DataModel)itr.next()).dataSeries;
            ds.shift(diff);
        }

        // データを読み直す
        //   マーカ位置を変更できるのは編集モード、再生モードのみ
        //   再生モードの場合のみ差分データを読み出す
        if (diff == 0 || dataModelArray_ == null) {
            //System.out.println("diff=" + diff);
            return;
        }
        // ★
        if (mode_ == GUIStatus.EDIT_MODE) {
            System.out.println("GUIMODE_EDIT");
            return;
        }
        if (diff > 0) {
            if (diff >= sampleCount_) {
                //System.out.println("getData(): patern 1 in");
                logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
                //System.out.println("getData(): patern 1 out");
            } else {
                //System.out.println("base=" + baseCount_ + ", ofs=" + (sampleCount_ - diff) + ", count=" + diff);
                //System.out.println("getData(): patern 2 in");
                logManager_.getData(baseCount_, sampleCount_ - diff, diff, dataModelArray_);
                //System.out.println("getData(): patern 2 out");
            }
        } else {
            if (-diff >= sampleCount_) {
                //System.out.println("getData(): patern 3 in");
                logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
                //System.out.println("getData(): patern 3 out");
            } else {
                //System.out.println("base=" + baseCount_ + ", ofs=0, count=" + (-diff));
                //System.out.println("getData(): patern 4 in");
                logManager_.getData(baseCount_, 0, -diff, dataModelArray_);
                //System.out.println("getData(): patern 4 out");
            }
        }
    }

    public void setDataTermTime(long dataTermTime) {
        long prevDataTermCount = dataTermCount_;
        dataTermCount_ = dataTermTime / stepTimeCount_;
        if (dataModelArray_ == null) {
            return;
        }
        if (prevDataTermCount < baseCount_) {
            //System.out.println("case1");
            if (dataTermCount_ < baseCount_ + sampleCount_) {
                logManager_.getData(
                    baseCount_,
                    0,
                    (int)(dataTermCount_ - baseCount_), 
                    dataModelArray_
                );
            } else {
                logManager_.getData(
                    baseCount_,
                    0,
                    sampleCount_,
                    dataModelArray_
                );
                dataTermCount_ = baseCount_ + sampleCount_;
            }
        } else if (prevDataTermCount < baseCount_ + sampleCount_) {
            //System.out.println("case2");
            if (dataTermCount_ < baseCount_ + sampleCount_) {
                logManager_.getData(
                    baseCount_,
                    (int)(prevDataTermCount - baseCount_),
                    (int)(dataTermCount_ - prevDataTermCount),
                    dataModelArray_
                );
            } else {
                logManager_.getData(
                    baseCount_,
                    (int)(prevDataTermCount - baseCount_),
                    (int)(baseCount_ + sampleCount_ - prevDataTermCount),
                    dataModelArray_
                );
                dataTermCount_ = baseCount_ + sampleCount_;
            }
        } else {
            //System.out.println("case3: prevDataTermCount=" + prevDataTermCount + " baseCount=" + baseCount_ + " sampleCount=" + sampleCount_);
        }
    }


    /**
     * 現在時刻を1ステップ移動
     *
     */
    public void shiftCurrentTime() {
        // 現在時刻の移動
        //currentTimeCount_ += stepTimeCount_; //★ここで加算するのはおかしい
        //System.out.println("shiftCurrentTime(): currentTimeCount=" + currentTimeCount_);
        currentTime_ = currentTimeCount_ / TIME_SCALE;
//        int setPos;
        if (markerFixed_) { // マーカ固定?
            baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
            baseCount_ = Math.round(baseTime_ / stepTime_); // データ開始位置
            timeAxisInfo_.base = baseTime_;
//            setPos = (int)Math.round(timeRange_ * markerPos_ / stepTime_);  // データ設定位置
        } else {    // マーカ移動?
            markerPos_ = currentTime_ / timeRange_;
            timeAxisInfo_.markerPos = markerPos_;
//            setPos = (int)(currentTimeCount_ / stepTimeCount_);
        }
        // 現在値の取得
//        Iterator itr = dataModelMap_.values().iterator();
        //if (!itr.hasNext()) { System.out.println("dataModelMap is empty!!!"); }
 /*       while (itr.hasNext()) { // 全データモデルをループ
            DataModel dm = (DataModel)itr.next();
            DataItem di = dm.dataItem;
            DataSeries ds = dm.dataSeries;

            StringExchangeable se = world_.getAttributeFromPath(    // 現在のアトリビュートを取得
                di.getAttributePath()
            );
            
            // アトリビュート値取得
            double val;
            if (di.isArray()) {
                if (se instanceof SETranslation) {
                    SETranslation st = (SETranslation)se;
                    if (di.index == 0) {
                        val = st.getX();
                    } else if (di.index == 1) {
                        val = st.getY();
                    } else {
                        val = st.getZ();
                    }
                } else {
                    val = ((SEDoubleArray)se).doubleValue(di.index);
                }
            } else {
                val = ((SEDouble)se).doubleValue();
            }
            //System.out.println("value = " + val);
            ds.set(setPos, val);
            if (markerFixed_) {
                ds.shift(1);
            }
            //ds.addLast(val);    // ★データ系列末尾に追加 <--- これは間違い
        }
*/
        if (currentTimeCount_ < totalTimeCount_) {
            currentTimeCount_ += stepTimeCount_;
            //System.out.println("currentTimeCount=" + currentTimeCount_);
        }
    }

    /**
     * データアイテム追加
     *
     * @param   DataItem    dataItem    データアイテム
     * @return  DataSeries  データ系列
     */
    public DataSeries addDataItem(
        DataItem dataItem
    ) {
        DataSeries ds;
        DataModel dm;

    /*    if (!world_.hasAttributeByPath(dataItem.getAttributePath())) {
            return null;
        }*/

        String key = dataItem.toString();
        ArrayList<String> l = dataItemCount_.get(key);
        if (l == null) {    // 初めてのデータアイテム?
            l = new ArrayList<String>();
            l.add(key);
            dataItemCount_.put(key, l);
            ds = new DataSeries(
                sampleCount_,
                baseCount_ * stepTime_, // baseTime_, ★これではダメ
                stepTime_
            );
            dm = new DataModel(dataItem, ds);
            dataModelMap_.put(key, dm);
            dataModelArray_ = new DataModel[dataModelMap_.size()];
            dataModelMap_.values().toArray(dataModelArray_);
            // データを読み込む
            //   データアイテムを追加できるのは編集モード、再生モードのみ
            //   再生モードの場合のみデータを読み出す
            // ★
// commented by grx
//            if (mode_ != GUIStatus.EDIT_MODE) {
//                logManager_.getData(baseCount_, 0, sampleCount_, new DataModel[]{dm});
//            }

            // アトリビュートフラグの変更
            //System.out.println("###### locked (" + dataItem.getAttributePath() + ")");
       /*     world_.setAttributeFlagFromPath(
                dataItem.getAttributePath(),
                Attribute.RECORD_REQUIRED | Attribute.RECORD_FLAG_LOCKED,   // ★MUST_RECORDなものにRECORD_REQUIREDをセットしても大丈夫か?
                true
            );*/

        } else {
            l.add(key);
            ds = ((DataModel)dataModelMap_.get(key)).dataSeries;
        }

        return ds;
    }

    /**
     * データアイテム削除
     *
     * @param   dataItem    データアイテム
     * @param   setFlag     フラグ設定をするか否か
     */
    public void removeDataItem(
        DataItem dataItem,
        boolean setFlag
    ) {
        String key = dataItem.toString();
        ArrayList l = (ArrayList)dataItemCount_.get(key);   // カウント取得
        l.remove(0);    // カウントを減らす
        if (l.size() <= 0) {    // データ系列に対応するデータアイテムがなくなった?
            dataItemCount_.remove(key); // カウント除去
            dataModelMap_.remove(key); // データ系列除去
            int size = dataModelMap_.size();
            if (size <= 0) {
                dataModelArray_ = null;
            } else {
                dataModelArray_ = new DataModel[size];
                dataModelMap_.values().toArray(dataModelArray_);
            }

            // アトリビュートフラグの変更
            if (!setFlag) { // フラグ変更をしない?
                return; // なにもしない
            }
            String apath = dataItem.getAttributePath(); // アトリビュートパス名の取得
            //if (world_.checkAttributeFlagFromPath(apath, Attribute.MUST_RECORD)) { // 記録必須?
            //    return; // フラグ変更はしない
            //}
            Iterator itr = dataItemCount_.keySet().iterator();  // すべてのデータアイテム名の列挙
//           boolean found = false;  // 発見フラグリセット
            while (itr.hasNext()) { // すべてのデータアイテム名について
                String k = (String)itr.next();
                if (k.startsWith(apath)) {  // 同じアトリビュート?
//                  found = true;   // 発見フラグON
                    break;  // ループ脱出
                }
            }
            //System.out.println("###### unlocked?");
 /*           if (!found) {   // このアトリビュートはもうない?
                //System.out.println("###### unlocked (" + apath + ")");
                world_.setAttributeFlagFromPath(
                    apath,
                    Attribute.RECORD_FLAG_LOCKED,
                    false
                );
            }
*/
        }
    }

    // -----------------------------------------------------------------
    // WorldTimeListenerの実装
    /**
     * 時刻変化
     * 
     * @param   time    時刻
     */
    public void worldTimeChanged(Time time) {
        //System.out.println("@@@ TimeChanged @@@ " + time.getUtime());
        /*
        if (mode_ == GUIManager.GUI_STATUS_EXEC) {
            shiftCurrentTime();
        } else if (mode_ == GUIManager.GUI_STATUS_PLAY) {
            setCurrentTime(time.getUtime());
        }
        */
    }

    // -----------------------------------------------------------------
    // プライベートメソッド
    /**
     * 軸分割更新
     *
     */
    private void _updateDiv() {
        double sMin = timeAxisInfo_.extent / MAX_DIV;
        int eMin = (int)Math.floor(Math.log(sMin) / LOG10);
        double step = 0;
        String format = "0";
        int e = eMin;
        boolean found = false;
        while (!found) {
            int m = 1;
            for (int i = 1; i <= 3; i++) {
                step = m * Math.pow(10.0, e);
                if (sMin <= step) { // && step <= sMax) {
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
        timeAxisInfo_.tickEvery = step;
        timeAxisInfo_.labelEvery = step;
        timeAxisInfo_.gridEvery = step;
        timeAxisInfo_.labelFormat = format;
    }

    // -----------------------------------------------------------------
    // WorldReplaceListenerの実装
    /**
     * 世界の更新
     *
     * @param   world   世界
     */
/*    public void replaceWorld(SimulationWorld world) {
        world_ = world;
        //world_.addTimeListener(this);
    }
*/
    /**
     * 全設定
     *
     * @param   totalTime   総時間(マイクロ秒)
     * @param   stepTime    積分時間(マイクロ秒)
     * @param   currentTime 現在時刻(マイクロ秒)
     * @param   timeRange   時間レンジ(秒)
     * @param   markerPos   マーカ位置
     */
    public void setup(
        long    totalTime,
        long    stepTime,
        long    currentTime,
        double  timeRange,
        double  markerPos
    ) {
        /*
        System.out.println("totalTime=" + totalTime);
        System.out.println("stepTime=" + stepTime);
        System.out.println("currentTime=" + currentTime);
        System.out.println("timeRange=" + timeRange);
        */
        totalTimeCount_ = totalTime;
        stepTimeCount_ = stepTime;
        currentTimeCount_ = currentTime;
        timeRange_ = timeRange;
        fixedMarkerPos_ = markerPos;

        dataTermCount_ = 0L;

        totalTime_ = totalTimeCount_ / TIME_SCALE;
        stepTime_ = stepTimeCount_ / TIME_SCALE;
        currentTime_ = currentTimeCount_ / TIME_SCALE;

        markerFixed_ = (timeRange_ < totalTime_);
        if (markerFixed_) {
            markerPos_ = fixedMarkerPos_;
            baseTime_ = currentTime_ - timeRange_ * markerPos_; // グラフ左端位置
            sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
            baseCount_ = Math.round(baseTime_ / stepTime_); // - 1; // データ開始位置
            //System.out.println("(fixed) sampleCount=" + sampleCount_);
            //System.out.println("(fixed) baseCount=" + baseCount_);
        } else {
            markerPos_ = currentTime_ / timeRange_;
            baseTime_ = 0;
            sampleCount_ = (int)Math.floor(timeRange_ / stepTime_) + 2;  // サンプル数
            baseCount_ = 0;
            //System.out.println("(not fixed) sampleCount=" + sampleCount_);
            //System.out.println("(not fixed) baseCount=" + baseCount_);
        }

        timeAxisInfo_.max = totalTime_; // 軸最大値の更新
        timeAxisInfo_.base = baseTime_;
        timeAxisInfo_.extent = timeRange_;
        timeAxisInfo_.markerPos = markerPos_;
        _updateDiv();

        // 全データ系列の更新
        Iterator itr = dataModelMap_.values().iterator();
        while (itr.hasNext()) {
            DataModel dm = (DataModel)itr.next();
            dm.dataSeries.setSize(sampleCount_);
            dm.dataSeries.setXOffset(baseCount_ * stepTime_);
            dm.dataSeries.setXStep(stepTime_);
        }

//        world_.updateAttribute("Graph0.hRange=" + Double.toString(timeRange_));
//        world_.updateAttribute("Graph0.markerPos=" + Double.toString(fixedMarkerPos_));
    }

    public void setup(
        long    totalTime,
        long    currentTime
    ) {
        setup(
            totalTime,
            stepTimeCount_,
            currentTime,
            (
                timeRange_ >= totalTime / TIME_SCALE
                ? totalTime / TIME_SCALE
                : timeRange_
            ),
            fixedMarkerPos_
        );
    }

    public void setup(
        long    totalTime,
        long    stepTime,
        long    currentTime
    ) {
        setup(
            totalTime,
            stepTime,
            currentTime,
            (
                timeRange_ >= totalTime / TIME_SCALE
                ? totalTime / TIME_SCALE
                : timeRange_
            ),
            fixedMarkerPos_
        );
    }

    public void reread() {
        if (dataModelArray_ == null) {
            return;
        }
        logManager_.getData(baseCount_, 0, sampleCount_, dataModelArray_);
    }
    
    public void setLogManager(LogManager logManager) {
    	logManager_ = logManager;
    }
}

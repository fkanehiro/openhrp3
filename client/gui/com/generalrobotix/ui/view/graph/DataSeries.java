package com.generalrobotix.ui.view.graph;

/**
 * データ系列クラス
 *
 * @author Kernel Inc.
 * @version 1.0 (2001/8/20)
 */
public class DataSeries {

    // -----------------------------------------------------------------
    // インスタンス変数
    private int size_;                  // データ点数
    private double[/*size_*/] data_;    // データのY座標値(Double.NaNはデータの欠落を示す)
    private int headPos_;               // データ先頭添字
    private double xOffset_;            // X座標値オフセット
    private double xStep_;              // X座標値刻み幅

    // -----------------------------------------------------------------
    // コンストラクタ
    /**
     * コンストラクタ
     *
     * @param   size    int     データ点数
     * @param   xOffset double  X座標値オフセット
     * @param   xStep   double  X座標値刻み幅
     */
    public DataSeries(
        int size,
        double xOffset,
        double xStep
    ) {
        size_ = size;
        xOffset_ = xOffset;
        xStep_ = xStep;
        headPos_ = 0;
        data_ = new double[size];
        for (int i = 0; i < size; i++) {
            data_[i] = Double.NaN;
        }
    }

    // -----------------------------------------------------------------
    // インスタンスメソッド
    /**
     * データ点数再設定
     *
     * @param   size    int データ点数
     */
    public void setSize(
        int size
    ) {
        size_ = size;
        headPos_ = 0;
        data_ = new double[size];
        for (int i = 0; i < size; i++) {
            data_[i] = Double.NaN;
        }
    }

    /**
     * X座標値オフセット設定
     *
     * @param   xOffset double  X座標値オフセット
     */
    public void setXOffset(
        double xOffset
    ) {
        xOffset_ = xOffset;
    }

    /**
     * X座標値刻み幅設定
     *
     * @param   xOffset double  X座標値刻み幅
     */
    public void setXStep(
        double xStep
    ) {
        xStep_ = xStep;
    }

    /**
     * サイズ取得
     *
     * @return  int サイズ
     */
    public int getSize() {
        return size_;
    }

    /**
     * X座標値オフセット取得
     *
     * @return  double  X座標値オフセット
     */
    public double getXOffset() {
        return xOffset_;
    }

    /**
     * X座標値刻み幅取得
     *
     * @return  double  X座標値刻み幅
     */
    public double getXStep() {
        return xStep_;
    }

    /**
     * データ配列取得
     *
     * @return  double[]    データ配列
     */
    public double[] getData() {
        return data_;
    }

    /**
     * データ先頭添字
     *
     * @return  int データ先頭添字
     */
    public int getHeadPos() {
        return headPos_;
    }

    /**
     * 先頭データ設定
     *     データ先頭位置からposだけ離れたところからdataを書き込む
     *     データ末尾を超える部分は切り捨てる
     *
     * @param   pos     int         データ設定位置
     * @param   data    double[]    データ
     */
    public void setHead(
        int pos,
        double[] data
    ) {
        // 配列長チェック
        int len = data.length;
        if (len < 1) {  // 空の配列?
            return; // なにもしない
        }

        // 開始位置チェック
        if (pos >= size_) { // 開始位置が不正?
            return; // なにもしない
        }
        int ofs = 0;
        if (pos < 0) {
            if (len <= -pos) {  // 長さが足りない(先頭に届かない)?
                return; // なにもしない
            }
            ofs = -pos; // オフセット設定
            pos = 0;    // 開始位置更新
            len -= ofs; // 配列長更新
        }

        // 配列長チェック
        if (len > size_ - pos) {    // 与えられた配列が長すぎる?
            len = size_ - pos;      // 長さをカット
        }

        // データのコピー
        int former = size_ - headPos_;
        if (pos < former) {    // 開始位置が前半にある?
            int remain = former - pos;
            if (len <= remain) {    // 全て前半に収まる?
                System.arraycopy(
                    data, ofs,
                    data_, headPos_ + pos,
                    len
                );  // 全て前半にコピー
            } else {
                System.arraycopy(
                    data, ofs,
                    data_, headPos_ + pos,
                    remain
                );  // 前半にコピー
                System.arraycopy(
                    data, ofs + remain,
                    data_, 0,
                    len - remain
                );  // 後半にコピー
            }
        } else {    // 開始位置が後半にある?
            System.arraycopy(
                data, ofs,
                data_, pos - former,
                len
            );  // 全て後半にコピー
        }
    }

    /**
     * 末尾データ設定
     *     データ末尾位置からposだけ離れたところからdataを書き込む
     *     データ末尾を超える部分は切り捨てる
     *
     * @param   pos     int         データ設定位置
     * @param   data    double[]    データ
     */
    public void setTail(
        int pos,
        double[] data
    ) {
        setHead(size_ - 1 - pos, data);
    }

    /**
     * データ移動
     *     countだけデータを移動する。
     *     countが正の場合、データ末尾にcountだけNaNを追加する
     *     (データ先頭位置が移動し、先頭データが削除される)
     *     (xOffset += xStep * count)
     *     countが負の場合、データ先頭に-countだけNaNを追加する
     *     (データ先頭位置が移動し、末尾データが削除される)
     *     (xOffset -= xStep * count)
     *
     * @param   count   int データ移動量
     */
    public void shift(
        int count
    ) {
        // 移動量チェック
        if (count == 0) {   // 移動しない?
            return; // なにもしない
        }
        xOffset_ += xStep_ * count; // X座標値オフセット更新
        if (count >= size_ || count <= -size_) {    // 配列長を超える移動?
            for (int i = 0; i < size_; i++) {   // NaNクリア
                data_[i] = Double.NaN;
            }
            return;
        }

        // シフト
        int prevHead = headPos_;            // 古い先頭位置
        int newHead = prevHead + count;     // 新しい先頭位置
        if (count > 0) {    // 正の移動?
            for (int i = prevHead; i < newHead; i++) {  // 新たにできた場所をループ
                data_[i % size_] = Double.NaN;  // NaNクリア
            }
            headPos_ = newHead % size_; // 先頭位置更新
        } else {            // 負の移動?
            int ind;
            for (int i = newHead; i < prevHead; i++) {  // 新たにできた場所をループ
                ind = i % size_;
                if (ind < 0) {
                    ind += size_;
                }
                data_[ind] = Double.NaN;  // NaNクリア
            }
            // 先頭位置更新
            headPos_ = newHead % size_;
            if (headPos_ < 0) {
                headPos_ += size_;
            }
        }
    }

    /**
     * 末尾にデータ追加
     *     1つデータを移動して、valueを書き込む
     *     (xOffset += xStep)
     *
     * @param   count   int データ移動量
     */
    public void addLast(
        double value
    ) {
        xOffset_ += xStep_; // X座標値オフセット更新
        data_[headPos_] = value;    // データ書込み
        headPos_ = (headPos_ + 1) % size_;  // 先頭位置移動
    }

    /**
     * データ設定
     *     先頭からpos離れたところにデータを書き込む
     *
     * @param   pos     データ設定位置
     * @param   value   値
     */
    public void set(
        int pos,
        double value
    ) {
        int setPos = (headPos_ + pos) % size_;
        data_[setPos] = value;
    }
}

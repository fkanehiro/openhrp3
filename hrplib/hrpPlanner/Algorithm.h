// -*- C++ -*-

#ifndef __ALGORITHM_H__
#define __ALGORITHM_H__

#include <map>
#include <vector>
#include <iostream>

#include "Configuration.h"

namespace PathEngine {
    class PathPlanner;
    class Algorithm;
    class Roadmap;

    /**
     * 経路計画アルゴリズム生成関数
     */
    typedef Algorithm* (*AlgorithmNewFunc)(PathPlanner* planner);

    /**
     * 経路計画アルゴリズム削除関数
     */
    typedef void (*AlgorithmDeleteFunc)(Algorithm* algorithm);

    template <class _New>
    Algorithm* AlgorithmCreate(PathPlanner* planner) {
        return new _New(planner);
    }

    template <class _Delete>
    void AlgorithmDelete(Algorithm* algorithm) {
        delete algorithm;
    }

    /**
     * @brief 経路計画アルゴリズム基底クラス
     *
     * 経路計画アルゴリズムを実装する際の基底クラス。
     * 新たな経路計画アルゴリズムを実装する場合、このクラスを継承して以下のメソッドを実装する。
     *  - calcPath()
     */
    class Algorithm {
    protected:
        /**
         * @brief 開始位置
         *
         * setStartConfiguration()によってセットされる。
         */
        Configuration start_;

        /**
         * @brief 終了位置
         *
         * setGoalConfiguration()によってセットされる。
         */
        Configuration goal_;

        /**
         * @brief プロパティ
         *
         * string-stringで名-値関係をなす。
         */
        std::map<std::string, std::string> properties_;

        /**
         * @brief 計算中フラグ
         *
         * calcPath()はこのフラグがfalseとなった場合は計算を中断するように実装する
         */
        bool isRunning_;

        /**
         * @brief 計画された経路
         *
         * 姿勢を並べたベクトルで表される列。ここにセットしたものがgetPath()で読み出される。
         */
        std::vector<Configuration> path_;

        /**
         * @brief 計画経路エンジン
         *
         * コンストラクタによってセットされる。
         * 干渉検出などのインターフェースを提供する。
         */
        PathPlanner* planner_;

        /**
         * @brief ロードマップ
         */
        Roadmap *roadmap_;

        /**
         * @brief デバッグ出力の制御
         */
        bool verbose_;
    public:
        /**
         * @brief コンストラクタ
         * @param planner PathPlannerへのポインタ
         */
        Algorithm(PathPlanner* planner);

        /**
         * @brief デストラクタ
         */
        virtual ~Algorithm();

        void setProperty(const std::string& key, const std::string& value); 

        /**
         * @brief アルゴリズムに対して各種情報を設定する
         * @param properties name-valueの組
         */
        void setProperties(const std::map<std::string, std::string> &properties);

        /**
         * @brief プロパティ一覧を取得する
         * @param names プロパティ名のリスト
         * @param values プロパティ値のリスト
         */
        void getProperties(std::vector<std::string> &names,
                           std::vector<std::string> &values);

        /**
         * @brief 初期位置を設定する
         * @param pos 初期位置
         */
        void setStartConfiguration(const Configuration &pos) {start_ = pos;}

        /**
         * @brief 終了位置を設定する
         * @param pos 終了位置
         */
        void setGoalConfiguration(const Configuration &pos) {goal_ = pos;}

        /**
         * @brief 初期位置と終了位置を直接結べないか検査する
         * @return 結べた場合はtrue, それ以外はfalse
         */
        bool tryDirectConnection();

        /**
         * @brief 経路計画を実行する
         *
         * 以下の条件を満たすように実装する。
         *  - start_からgoal_に到達する経路を計画する
         *  - 計算途中でisRunning_フラグがfalseになった場合は計算を中断する
         *  - 経路を発見した場合はそれをpath_にセットし、計算を終了する
         */
        virtual bool calcPath() = 0;

        /**
         * @brief 計算を止める
         */
        void stopPlanning(){isRunning_ = false;}

        /**
         * @brief 結果を取得する
         * @return 結果の姿勢列
         */
        const std::vector<Configuration>& getPath() {return path_;}

        /**
         * @brief ロードマップを取得する
         * @return ロードマップ
         */
        Roadmap *getRoadmap() { return roadmap_; } 

        /**
         * @brief 経路計画の準備をし、初期位置と目標位置が有効なものであることをチェックする
         * @return 初期位置もしくは目標位置が無効なものであった場合false、それ以外true
         */
        bool preparePlanning();

        /**
         * @brief デバッグ出力の制御
         * @param b trueで出力が有効 
         */
        void verbose(bool b) { verbose_ = b; }
    };
};

#endif // __ALGORITHM_H__

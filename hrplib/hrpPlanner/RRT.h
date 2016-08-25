#ifndef __RRT_H__
#define __RRT_H__

#include <vector>
#include <string>
#include <iostream>
#include <stdio.h>

#include "PathPlanner.h"
#include "Algorithm.h"
#include "Roadmap.h"

namespace PathEngine {

  /**
   * @brief RRTアルゴリズム実装クラス
   */
  class RRT
    : public Algorithm {
  public:
    /**
     * スタートからのツリーとゴールからのツリーが接続できたかの追加チェックを行うユーザ関数。通常のチェックで接続できたとみなされるスタート側のコンフィギュレーションとゴール側のコンフィギュレーションを引数にとり、接続可能である場合にはtrueを、それ以外の場合はfalseを返す。
     */
    typedef boost::function2<bool, const Configuration &, const Configuration &> extraConnectionCheckFunc;

  private:
    /**
     * extend()の結果
     */
    enum ExtendResult {
      Advanced, ///< 指定の位置に近づいた  
      Trapped,  ///< 干渉のため、近づくことができなかった  
      Reached   ///< 指定の位置に到達した  
    };

    /**
     * @brief RRT-connect において、初期位置からのツリー
     */
    RoadmapPtr Tstart_;

    /**
     * @brief RRT-connect において、終了位置からのツリー
     */
    RoadmapPtr Tgoal_;

    /**
     * @brief a tree extened in the last phase
     */
    RoadmapPtr TlastExtended_;

    /**
     * @brief ツリーの交換のために用いる変数。どちらか一方がTstart_をもう一方がTgoal_を指す
     */
    RoadmapPtr Ta_, Tb_;

    /**
     * @brief ランダムな点に向かってツリーを伸ばす。
     * @param tree ツリー
     * @param qRand ランダムな点
     * @param reverse ツリーから点に向かって移動を試みる場合false、点からツリーへ移動を試みる場合はtrue
     * @return 伸ばせなかった場合Trapped, eps_だけ伸ばせた場合Advanced, qRandに到達できた場合Readchedを返す。qRandは伸ばした先の点に書き換えられる
     */
    int extend(RoadmapPtr tree, Configuration& qRand, bool reverse=false);

    /**
     * @brief RRT-connect の connect 関数。伸ばせなくなるまで extend する
     * @param tree ツリー
     * @param qNew 伸ばす方向の点
     * @param ツリーから点に向かって伸ばす場合はfalse、逆はtrue
     * @return qNewにまで到達できなかった場合はTrapped, できた場合はReachedを返す
     */
    int connect(RoadmapPtr tree, const Configuration& qNew, bool reverse=false);

    void swapTrees();

    /**
     * ランダムな点に向けてどれだけのばすか
     */
    double eps_;

    /**
     * ツリーを作成する最大試行回数
     */
    int times_;

    /**
     * スタートからツリーをのばすか否か
     */
    bool extendFromStart_;

    /**
     * ゴールからツリーをのばすか否か
     */
    bool extendFromGoal_;

    /**
     * スタートからのツリーとゴールからのツリーが接続できたかの追加チェックを行うユーザ関数
     */
    extraConnectionCheckFunc extraConnectionCheckFunc_;

  public:
    /**
     * @brief コンストラクタ
     * @param planner パスプランナー
     */
    RRT(PathPlanner* planner);

    /**
     * @brief デストラクタ
     */
    ~RRT();

    /**
     * @brief 親クラスのドキュメントを参照
     */
    bool calcPath();

    /**
     * @brief スタートからツリーをのばすか否かを設定
     * @param b trueでのばす
     */
    void extendFromStart(bool b) { extendFromStart_ = b; }

    /**
     * @brief ゴールからツリーをのばすか否かを設定
     * @param b trueでのばす
     */
    void extendFromGoal(bool b) { extendFromGoal_ = b; }

    /**
     * @brief 計画した経路を抽出し、path_にセットする
     */
    void extractPath();

    /**
     * @brief 計画した経路を抽出し、o_pathにセットする
     */
    void extractPath(std::vector<Configuration>& o_path);

    /**
     * @brief スタートからのツリーを取得する
     * @return スタートからのツリー
     */
    RoadmapPtr getForwardTree() { return Tstart_; }

    /**
     * @brief スタートからのツリーを設定する
     * @param tree スタートからのツリー
     */
    void setForwardTree(RoadmapPtr tree);

    /**
     * @brief ゴールからのツリーを取得する
     * @return ゴールからのツリー
     */
    RoadmapPtr getBackwardTree() { return Tgoal_; }

    /**
     * @brief get a tree extened in the last phase
     * @return a tree extened in the last phase
     */
    RoadmapPtr getLastExtendedTree() { return TlastExtended_; }

    /**
     * @brief ゴールからのツリーを設定する
     * @param tree ゴールからのツリー
     */
    void setBackwardTree(RoadmapPtr tree);

    /**
     * @brief ツリーを伸ばす処理を1回だけ行う
     * @return パスが見つかった場合true
     */
    bool extendOneStep();

    /**
     * @brief サンプリングしたコンフィギュレーションに向かって伸ばす距離を設定
     * @param e サンプリングしたコンフィギュレーションに向かって伸ばす距離
     */
    void epsilon(double e) { eps_ = e; }

    /**
     * @brief スタートからのツリーとゴールからのツリーが接続できたかの追加チェックを行うユーザ関数を設定する
     * @param i_func ユーザ関数
     */
    void setExtraConnectionCheckFunc(extraConnectionCheckFunc i_func);
  };
};


#endif // __RRT_H__

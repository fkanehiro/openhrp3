#ifndef __RRT_H__
#define __RRT_H__

#include <vector>
#include <string>
#include <iostream>
#include <stdio.h>

#include "PathPlanner.h"
#include "Algorithm.h"

namespace PathEngine {
  class Roadmap;

  /**
   * @brief RRTアルゴリズム実装クラス
   */
  class RRT
    : public Algorithm {
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
     * RRT-connect において、初期位置からのツリー
     */
    Roadmap *Ta_;

    /**
     * RRT-connect において、終了位置からのツリー
     */
    Roadmap *Tb_;

    /**
     * @brief ランダムな点に向かってツリーを伸ばす。
     * @param tree ツリー
     * @param qRand ランダムな点
     * @param reverse ツリーから点に向かって移動を試みる場合false、点からツリーへ移動を試みる場合はtrue
     * @return 伸ばせなかった場合Trapped, eps_だけ伸ばせた場合Advanced, qRandに到達できた場合Readchedを返す。qRandは伸ばした先の点に書き換えられる
     */
    int extend(Roadmap *tree, Configuration& qRand, bool reverse=false);

    /**
     * @brief RRT-connect の connect 関数。伸ばせなくなるまで extend する
     * @param tree ツリー
     * @param qNew 伸ばす方向の点
     * @param ツリーから点に向かって伸ばす場合はfalse、逆はtrue
     * @return qNewにまで到達できなかった場合はTrapped, できた場合はReachedを返す
     */
    int connect(Roadmap *tree, const Configuration& qNew, bool reverse=false);

    /**
     * @brief 計画した経路を抽出し、path_にセットする
     */
    void path();

    int extendOneStep();

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
     */
    bool extendFromStart_;

    bool extendFromGoal_;

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

    void extendFromStart(bool b) { extendFromStart_ = b; }

    void extendFromGoal(bool b) { extendFromGoal_ = b; }
  };
};


#endif // __RRT_H__

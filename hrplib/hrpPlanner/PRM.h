//-*- C++ -*-
#ifndef __PRM_H__
#define __PRM_H__

#include <vector>
#include <stack>
#include <queue>
#define _USE_MATH_DEFINES
#include <math.h>

#include "Algorithm.h"
#include "PathPlanner.h"

namespace PathEngine {
  class RoadmapNode;

  /**
   * @brief PRM アルゴリズム実装クラス
   */
  class PRM : public Algorithm {
  private:
    // 近傍ノードの上限
    unsigned long maxNeighbors_;

    // 最大の点数
    unsigned long maxPoints_;

    // ランダムに選んだ点からの選ばれるノードの範囲
    double maxDist_;

    /**
     * @brief ロードマップを生成する
     * @return stopPlanning()によって中断された場合はfalse、それ以外はtrue
     */
    bool buildRoadmap();
    
  public:
    /**
     * @brief コンストラクタ
     * @param planner パスプランナー
     */
    PRM(PathPlanner* planner);

    /**
     * @brief デストラクタ
     */
    virtual ~PRM();

    /**
     * @brief 親クラスのドキュメントを参照
     */
    bool calcPath();
  };
};

#endif // __PRM_H__

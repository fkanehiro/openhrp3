// -*- C++ -*-

#ifndef __OPTIMIZER_H
#define __OPTIMIZER_H

#include <vector>
#include "Position.h"

namespace PathEngine {
  class Optimizer;
  class PathPlanner;

  /**
   * 経路最適化アルゴリズム生成関数
   */
  typedef Optimizer* (*OptimizerNewFunc)(PathPlanner* planner);

  /**
   * 経路最適化アルゴリズム削除関数
   */
  typedef void (*OptimizerDeleteFunc)(Optimizer* optimizer);

  template <class _New>
  Optimizer* OptimizerCreate(PathPlanner* planner) {
    return new _New(planner);
  }

  template <class _Delete>
  void OptimizerDelete(Optimizer* optimizer) {
    delete optimizer;
  }

  /**
   * @brief 経路最適化アルゴリズム実装用の抽象クラス
   *
   * 新たなアルゴリズムを実装する場合はこのクラスを継承し、optimize()を実装する。
   */
  class Optimizer {
  public:
    /**
     * @brief コンストラクタ
     * @param planner PathPlannerへのポインタ
     */
    Optimizer(PathPlanner* planner) : planner_(planner) {}

    /**
     * @brief デストラクタ
     */
    virtual ~Optimizer() {}

    /**
     * @brief 経路を最適化する
     * @param path 元の経路
     * @return 最適化された経路。
     */
    virtual std::vector<Position> optimize(const std::vector<Position> &path)=0;
  protected:
    PathPlanner *planner_;
  };
};

#endif

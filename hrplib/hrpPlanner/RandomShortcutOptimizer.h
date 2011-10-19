// -*- C++ -*-
#ifndef __RANDOM_SHORTCUT_OPTIMIZER_H_
#define __RANDOM_SHORTCUT_OPTIMIZER_H_

#include "Optimizer.h"

namespace PathEngine{
  /**
   * @brief ショートカット可能な経由点を削除して経路を最適化する
   */
  class RandomShortcutOptimizer : public Optimizer
  {
  public:
    /**
     * @brief コンストラクタ
     */
    RandomShortcutOptimizer(PathPlanner *planner) : Optimizer(planner) {}

    /**
     * @brief デストラクタ
     */
    virtual ~RandomShortcutOptimizer() {}

    /**
     * @brief 親クラスのドキュメントを参照
     */
    std::vector<Configuration> optimize(const std::vector<Configuration> &path);
  };
};

#endif

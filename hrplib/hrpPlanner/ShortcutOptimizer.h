// -*- C++ -*-
#ifndef __SHORTCUT_OPTIMIZER_H_
#define __SHORTCUT_OPTIMIZER_H_

#include "Optimizer.h"

namespace PathEngine{
  /**
   * @brief ショートカット可能な経由点を削除して経路を最適化する
   */
  class ShortcutOptimizer : public Optimizer
  {
  public:
    /**
     * @brief コンストラクタ
     */
    ShortcutOptimizer(PathPlanner *planner) : Optimizer(planner) {}

    /**
     * @brief デストラクタ
     */
    virtual ~ShortcutOptimizer() {}

    /**
     * @brief 親クラスのドキュメントを参照
     */
    std::vector<Position> optimize(const std::vector<Position> &path);
  };
};

#endif

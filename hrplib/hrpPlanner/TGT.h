// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#ifndef __TGT_H__
#define __TGT_H__

#include "Mobility.h"
#include "Configuration.h"

namespace PathEngine {
    class PathPlanner;

    /**
     * @brief Turn-Go-Turn移動アルゴリズム実装クラス
     *
     * 最初に終点の方向を向くようにその場で回転し、終点まで真っ直ぐに移動、最後に終了位置での指定の方向を向くためにその場回転をする移動アルゴリズム
     */
    class TGT : public Mobility{
    public:
        /**
         * @brief コンストラクタ
         * @param planner PathPlannerへのポインタ
         */
        TGT(PathPlanner* planner) : Mobility(planner) {}

        /**
         * @brief 親クラスのドキュメントを参照
         */
        Configuration interpolate(const Configuration& from, const Configuration& to, double ratio) const;
      
        /**
         * @brief 親クラスのドキュメントを参照
         */
        double distance(const Configuration& from, const Configuration& to) const;

        /**
         * @brief 親クラスのドキュメントを参照
         */
        bool isReversible() const { return false; }
    };
};

#endif // __TGT_H__

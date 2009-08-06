// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#ifndef __OMNI_WHEEL_H__
#define __OMNI_WHEEL_H__

#include "Mobility.h"
#include "Position.h"

namespace PathEngine {
    class PathPlanner;

    /*
     * @brief
     */
    class OmniWheel : public Mobility {
    public:
        /**
         * @brief コンストラクタ
         * @param planner PathPlannerへのポインタ
         */
        OmniWheel(PathPlanner* planner) : Mobility(planner) {}

        /**
         * @brief 親クラスのドキュメントを参照
         */
        Position interpolate(const Position& from, const Position& to, double ratio) const;
      
        /**
         * @brief 親クラスのドキュメントを参照
         */
        double distance(const Position& from, const Position& to) const;

        /**
         * @brief 親クラスのドキュメントを参照
         */
        bool isReversible() const { return true; }
    };
};
#endif

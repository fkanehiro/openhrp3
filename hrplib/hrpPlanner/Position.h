// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#ifndef __POSITION_H__
#define __POSITION_H__

#include <iostream>
#include <ostream>

#include <math.h>
#ifndef M_PI
#define M_PI 3.14159265358979323846264338327950288
#endif

namespace PathEngine {
    /**
     * @brief 座標クラス
     *
     * 座標 (x, y, theta) を扱うクラス。
     * x[m], y[m], theta[rad]　thetaの値は0以上2*M_PI未満
     */
    class Position {
    private:
        double x_, y_, theta_;
        static double weightX_, weightY_, weightTh_;
        static double maxX_, maxY_, minX_, minY_;
    public:
        Position();
    
        /**
         * @brief コンストラクタ
         * @param x x座標
         * @param y y座標
         * @param theta 回転角
         */
        Position(double x, double y, double theta);

        /**
         * @brief コピーコンストラクタ
         * @param pos コピー元
         */
        Position(const Position& pos);

        friend std::ostream& operator<< (std::ostream& out, const Position& pos) {  return out  << pos.getX() << "  " << pos.getY() << "  " << pos.getTheta();}

        const double getX() const {return x_;}
        const double getY() const {return y_;}
        const double getTheta() const {return theta_;}
        void setX(double x) { x_ = x; } 
        void setY(double y) { y_ = y; } 
        void setTheta(double th) { theta_ = th; } 

#if 0
        /**
         * @brief posとの距離を計算する
         * @param pos 距離をはかりたい Position
         * @return 距離
         */
        double getDistance(const Position &pos) const;
#endif

        /**
         * @brief x,y,thetaのそれぞれが有効な範囲内にあるかどうか検査する
         * @return 全ての要素が有効範囲内にあればtrue、それ以外false
         */
        bool isValid() const;

        /**
         * @brief set weight for X element
         * @param w weight
         */
        static void setWeightX(double w) { weightX_ = w; }

        /**
         * @brief set weight for Y element
         * @param w weight
         */
        static void setWeightY(double w) { weightY_ = w; }

        /**
         * @brief set weight for Theta element
         * @param w weight
         */
        static void setWeightTh(double w) { weightTh_ = w; }

        /**
         * @brief get weight for X element
         * @return weight
         */
        static double getWeightX() { return weightX_; }

        /**
         * @brief get weight for Y element
         * @return weight
         */
        static double getWeightY() { return weightY_; }

        /**
         * @brief get weight for Theta element
         * @return weight
         */
        static double getWeightTh() { return weightTh_; }

        /**
         * @brief set bounds for X element
         * @param min minimum value
         * @param max maximum value
         */
        static void setBoundsX(double min, double max){ minX_ = min; maxX_ = max;}

        /**
         * @brief set bounds for Y element
         * @param min minimum value
         * @param max maximum value
         */
        static void setBoundsY(double min, double max){ minY_ = min; maxY_ = max;}

        /**
         * @brief generate random position
         * @return generated position
         */
        static Position random();
    };

    inline double theta_limit(double theta)
    {
        while (theta >= 2*M_PI) theta -= 2*M_PI;
        while (theta < 0) theta += 2*M_PI;
        return theta;
    }

    inline double theta_diff(double from, double to)
    {
        double diff = to - from;
        if (diff > M_PI){
            return diff - 2*M_PI;
        }else if (diff < -M_PI){
            return diff + 2*M_PI;
        }else{
            return diff;
        }
    }
};
#endif // __POSITION_H__

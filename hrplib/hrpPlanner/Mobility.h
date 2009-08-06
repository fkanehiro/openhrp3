// -*- C++ -*-

#ifndef __MOBILITY_H__
#define __MOBILITY_H__

#include <vector>
#include "Position.h"

namespace PathEngine {

  class PathPlanner;
  class Mobility;

  /**
   * @brief 移動アルゴリズム生成関数
   */
  typedef Mobility* (*MobilityNewFunc)(PathPlanner* planner);

  /**
   * @brief 移動アルゴリズム解放関数
   */
  typedef void (*MobilityDeleteFunc)(Mobility* mobility);

  /**
   * @brief 移動アルゴリズム生成関数生成テンプレート
   */
  template <class _New>
  Mobility* MobilityCreate(PathPlanner* planner) {
    return new _New(planner);
  }

  /**
   * @brief 移動アルゴリズム解放関数生成テンプレート
   */
  template <class _Delete>
  void MobilityDelete(Mobility* mobility) {
    delete mobility;
  }
  
  /**
   * @brief 移動アルゴリズム実装用抽象クラス
   *
   * ロボットの移動アルゴリズムを実装するための抽象クラス。
   * 各々のロボットの移動アルゴリズムを実装する場合、このクラスを継承し、以下のメンバ関数を実装する
   *  - getPath()
   *  - isReversible()
   */
  class Mobility {
  protected:
    /**
     * @brief 計画経路エンジン
     *
     * コンストラクタによってセットされる。
     * 干渉検出などのインターフェースを提供する。
     */
    PathPlanner* planner_;
  public:
    /**
     * @brief コンストラクタ
     * @param planner PathPlannerへのポインタ
     */
    Mobility(PathPlanner* planner) {planner_ = planner;}

    /**
     * @brief デストラクタ
     */
    virtual ~Mobility() {;}

    /**
     * @brief 開始位置から目標地点への移動を補間して生成された姿勢列を取得する。
     *
     * 隣接した二つの姿勢間の距離はinterpolationDistance()で得られる距離以下になるように補間を行う
     * @param from 開始位置
     * @param to 目標位置
     * @return 姿勢列
     */
    virtual std::vector<Position> getPath(const Position &from, const Position &to) const; 

    /**
     * @brief この移動アルゴリズムでA→Bへ移動可能である時に、B→Aが同じ経路で移動可能であるかどうか
     * @return 同じ経路で移動可能である場合true, 必ずしもそうでない場合はfalse
     */
    virtual bool isReversible() const = 0;

    /**
     * @brief fromからtoへ干渉なしに移動可能であるかどうか
     * @return 移動可能であればtrue、そうでなければfalse
     */
    bool isReachable(const Position& from, const Position& to) const;

    /**
     * @brief 
     * @param from
     * @param to
     * @param ratio 
     * @return 
     */
    virtual Position interpolate(const Position& from, const Position& to, double ratio) const = 0;

    /**
     * @brief
     * @param from
     * @param to
     * @return
     */
    virtual double distance(const Position& from, const Position& to) const = 0;
    /**
     * @brief 補間時の隣接する2点間の最大距離を設定する
     * @param d 隣接する2点間の最大距離
     */
    static void interpolationDistance(double d) { interpolationDistance_ = d;}

    /**
     * @brief 補間時の隣接する2点間の最大距離を取得する
     * @return 隣接する2点間の最大距離
     */
    static double interpolationDistance() { return interpolationDistance_;}
  private:
    /**
     * @brief 補間時の隣接する2点間の最大距離
     */
    static double interpolationDistance_;
  };
};
#endif // __MOBILITY_H__

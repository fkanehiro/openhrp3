// -*- C++ -*-
#ifndef __ROADMAP_NODE_H__
#define __ROADMAP_NODE_H__

#include <vector>
#include "Position.h"
#include "exportdef.h"

namespace PathEngine {
  /**
   * @brief ロードマップのノード
   */
  class HRPPLANNER_API RoadmapNode {
  public:
    /**
     * @brief コンストラクタ
     * @param pos このノードの座標
     */
    RoadmapNode(const Position& pos) : pos_(pos) {}

    /**
     * @brief デストラクタ
     */
    ~RoadmapNode() {}

    /**
     * @brief 親ノードの追加
     * @param node 親ノード
     */
    void addParent(RoadmapNode *node) { parents_.push_back(node); }

    /**
     * @brief 子ノードの追加
     * @param node 子ノード
     */
    void addChild(RoadmapNode *node) { children_.push_back(node); }

    /**
     * @brief 位置の取得
     * @return 位置
     */
    const Position& position() const { return pos_; }

    /**
     * @brief 親ノードの取得
     * @param index 親ノードのインデックス
     * @return 不正なインデックスを指定した場合はNULL、それ以外は親ノード
     */
    RoadmapNode *parent(unsigned int index);

    /**
     * @brief 子ノードの取得
     * @param index 子ノードのインデックス
     * @return 不正なインデックスを指定した場合はNULL、それ以外は子ノード
     */
    RoadmapNode *child(unsigned int index);

    /**
     * @brief 親ノードの数を取得
     * @return 親ノードの数
     */
    unsigned int nParents() const { return parents_.size(); }

    /**
     * @brief 子ノードの数を取得
     * @return 子ノードの数
     */
    unsigned int nChildren() const { return children_.size(); }

    /**
     * @brief 探索用フラグを設定する
     * @param flag 探索済みがtrue、未探索がfalse
     */
    void visited(bool flag) { visited_ = flag; }

    /**
     * @brief 探索用フラグの値を取得する
     * @return 探索用フラグの値
     */
    bool visited() const { return visited_; } 
  private:
    /**
     * @brief 親ノードリスト
     */
    std::vector<RoadmapNode*> parents_;

    /**
     * @brief 子ノードリスト
     */
    std::vector<RoadmapNode*> children_;

    /**
     * @brief このノードの座標
     */
    Position pos_;

    /**
     * @brief 探索アルゴリズム用フラグ
     */
    bool visited_;

    friend std::ostream& operator<< (std::ostream& out, const RoadmapNode& r) {  return out << r.pos_ << " - (" << r.children_.size() << ")";}
  };
};

#endif // __ROADMAP_NODE_H__

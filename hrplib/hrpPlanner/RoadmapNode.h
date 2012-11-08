// -*- C++ -*-
#ifndef __ROADMAP_NODE_H__
#define __ROADMAP_NODE_H__

#include <vector>
#include <boost/shared_ptr.hpp>
#include "Configuration.h"
#include "exportdef.h"

namespace PathEngine {
  class RoadmapNode;
  typedef boost::shared_ptr<RoadmapNode> RoadmapNodePtr;

  /**
   * @brief ロードマップのノード
   */
  class HRPPLANNER_API RoadmapNode {
  public:
    /**
     * @brief コンストラクタ
     * @param pos このノードの座標
     */
    RoadmapNode(const Configuration& pos) : pos_(pos) {}

    /**
     * @brief デストラクタ
     */
    ~RoadmapNode() {}

    /**
     * @brief 親ノードの追加
     * @param node 親ノード
     */
    void addParent(RoadmapNodePtr node) { parents_.push_back(node); }

    /**
     * @brief 子ノードの追加
     * @param node 子ノード
     */
    void addChild(RoadmapNodePtr node) { children_.push_back(node); }

    /**
     * @brief 親ノードの削除
     * @param node 親ノード
     * @return 削除できたらtrue
     */
    bool removeParent(RoadmapNodePtr node);

    /**
     * @brief 子ノードの削除
     * @param node 子ノード
     * @return 削除できたらtrue
     */
    bool removeChild(RoadmapNodePtr node);

    /**
     * @brief 位置の取得
     * @return 位置
     */
    Configuration& position() { return pos_; }

    /**
     * @brief 親ノードの取得
     * @param index 親ノードのインデックス
     * @return 不正なインデックスを指定した場合はNULL、それ以外は親ノード
     */
    RoadmapNodePtr parent(unsigned int index);

    /**
     * @brief 子ノードの取得
     * @param index 子ノードのインデックス
     * @return 不正なインデックスを指定した場合はNULL、それ以外は子ノード
     */
    RoadmapNodePtr child(unsigned int index);

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
    std::vector<RoadmapNodePtr> parents_;

    /**
     * @brief 子ノードリスト
     */
    std::vector<RoadmapNodePtr> children_;

    /**
     * @brief このノードの座標
     */
    Configuration pos_;

    /**
     * @brief 探索アルゴリズム用フラグ
     */
    bool visited_;

    friend std::ostream& operator<< (std::ostream& out, const RoadmapNode& r) {  return out << r.pos_ << " - (" << r.children_.size() << ")";}
  };
};

#endif // __ROADMAP_NODE_H__

// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#ifndef __ROADMAP_H__
#define __ROADMAP_H__

#include <vector>
#include <boost/shared_ptr.hpp>
#include "Configuration.h"
#include "RoadmapNode.h"
#include "exportdef.h"

namespace PathEngine{
    class PathPlanner;
    class Roadmap;
    typedef boost::shared_ptr<Roadmap> RoadmapPtr;

    /**
     * @brief ロードマップ
     */
    class HRPPLANNER_API Roadmap{
    public:
        /**
         * @brief コンストラクタ
         */
        Roadmap(PathPlanner *planner) : planner_(planner), m_nEdges(0) {}

        /**
         * @brief デストラクタ
         *
         * ロードマップに登録されているノードは全てdeleteされる
         */
        ~Roadmap();

        /**
         * @brief ノードを追加する
         * @param node 追加されるノード
         */
        void addNode(RoadmapNodePtr node) { nodes_.push_back(node); }

        /**
         * @brief 有向エッジを追加する
         * @param from エッジの始点
         * @param to エッジの終点
         */
        void addEdge(RoadmapNodePtr from, RoadmapNodePtr to);

        /**
         * @brief このロードマップの内容を引数のロードマップに統合する
         *
         * 統合の後、このロードマップの中身は空になる。
         * @param rdmp 統合先のロードマップ
         */
        void integrate(RoadmapPtr rdmp);

        /**
         * @brief ノード数を取得する
         * @return ノード数
         */
        unsigned int nNodes() const { return nodes_.size(); }

        unsigned int nEdges() const { return m_nEdges; }

        /**
         * @brief ノードを取得する
         * @param index ノードのインデックス
         * @return 不正なインデックスが渡された場合はNULL、それ以外はノードを返す
         */
        RoadmapNodePtr node(unsigned int index);

        /**
         * @brief 最も距離の小さいノードを返す
         * @param cfg 距離計算を行う対象となる位置
         * @param node 最も近いノード。ノードが一つもない場合はNULL
         * @param distance 最も近いノードまでの距離
         */
        void findNearestNode(const Configuration& cfg, RoadmapNodePtr &node, double &distance); 

        /**
         * @brief 最後に追加されたノードを取得する
         * @return 最後に追加されたノード。ノードが一つもない場合はNULL
         */
        RoadmapNodePtr lastAddedNode();

        /**
         * @brief 指定されたノードのインデックスを取得する
         * @param node ノード
         * @return ノードのインデックス。ノードがこのロードマップに存在しない場合は-1を返す
         */
        int indexOfNode(RoadmapNodePtr node);

        /**
         * @brief 深さ優先探索
         * @param startNode 初期ノード
         * @param goalNode 終了ノード
         * @return 探索結果を納めたパス
         */
        std::vector<RoadmapNodePtr > DFS(RoadmapNodePtr startNode, RoadmapNodePtr goalNode);

        /**
         * @brief 2つのノードの接続を試み、接続できた場合はエッジを追加する
         * @param from 接続元
         * @param to 接続先
         * @param tryReversed trueの時逆向きの接続も試みる
         */
        void tryConnection(RoadmapNodePtr from,  RoadmapNodePtr to, bool tryReversed=true);

        /**
         * @brief ロードマップをクリアする
         */
        void clear();
    private:
        /**
         * @brief ノードのリスト
         */
        std::vector<RoadmapNodePtr> nodes_;
        PathPlanner *planner_;
        unsigned int m_nEdges;
    };
};

#endif

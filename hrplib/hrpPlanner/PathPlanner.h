//-*- C++ -*-

#ifndef __PATH_PLANNER_H__
#define __PATH_PLANNER_H__

#include <string>
#include <map>
#include <iostream>
#include <sstream>
#include <boost/function.hpp>
#include "hrpUtil/TimeMeasure.h"

#include "exportdef.h"
#include "Algorithm.h"
#include "Configuration.h"
#include "ConfigurationSpace.h"
#include "Mobility.h"
#include "Optimizer.h"
#include "CollisionDetector.h"
#include "hrpCollision/ColdetModelPair.h"
#undef random

#include <hrpCorba/ORBwrap.h>
#include <hrpCorba/ModelLoader.hh>
#include <hrpCorba/CollisionDetector.hh>
#include <hrpCorba/OnlineViewer.hh>

#include <hrpModel/World.h>
#include <hrpModel/ConstraintForceSolver.h>
#include <hrpUtil/TimeMeasure.h>

namespace PathEngine {
    class Algorithm;
    class Mobility;
    class PathPlanner;

    typedef boost::function2<bool, PathPlanner *, const Configuration &> applyConfigFunc;
    typedef boost::shared_ptr<hrp::World<hrp::ConstraintForceSolver> > WorldPtr;
    /**
     * @brief 計画経路エンジン
     *
     * 経路計画を行うプログラムはこのクラスを使用する。干渉検出などを簡易化したメソッドを持つ。
     */
    class HRPPLANNER_API PathPlanner {

    private:
        applyConfigFunc m_applyConfigFunc;
        typedef std::map<const std::string, std::pair<AlgorithmNewFunc, AlgorithmDeleteFunc> > AlgorithmFactory;
        typedef AlgorithmFactory::value_type AlgorithmFactoryValueType;
        /**
         * @brief 経路計画アルゴリズムのファクトリ
         */
        AlgorithmFactory algorithmFactory_;

        typedef std::map<std::string, std::pair<MobilityNewFunc, MobilityDeleteFunc> > MobilityFactory;
        typedef MobilityFactory::value_type MobilityFactoryValueType;
        /**
         * @brief 移動能力のファクトリ
         */
        MobilityFactory mobilityFactory_;

        typedef std::map<std::string, std::pair<OptimizerNewFunc, OptimizerDeleteFunc> > OptimizerFactory;
        typedef OptimizerFactory::value_type OptimizerFactoryValueType;
        /**
         * @brief 経路最適化アルゴリズムのファクトリ
         */
        OptimizerFactory optimizerFactory_;

        /**
         * @brief ModelLoader への参照
         */
        OpenHRP::ModelLoader_var modelLoader_;

        /**
         * @brief デバッグモード時に使用する OnlineViewer への参照
         */
        OpenHRP::OnlineViewer_var onlineViewer_;

        /**
         * @brief 使用する経路計画アルゴリズム名
         */
        std::string algorithmName_;

        /**
         * @brief 使用する経路計画アルゴリズム
         */
        Algorithm* algorithm_;

        /**
         * @brief 使用する移動能力名
         */
        std::string mobilityName_;

        /**
         * @brief 使用する移動能力
         */
        Mobility* mobility_;

        /**
         * @brief コンフィギュレーション空間
         */
        ConfigurationSpace cspace_;

        /**
         * @brief 経路計画の対象とするロボット
         */
        hrp::BodyPtr model_;

        /**
         * @brief デバッグモード
         */
        bool debug_;

        /**
         * @brief デバッグモード時に使用する現在時刻
         */
        double dt_;

        /**
         * @brief CORBAサーバ取得
         *
         * CORBAサーバをネームサーバから取得する
         * @param n サーバ名
         * @param cxt ネーミングコンテキスト
         */
        template<typename X, typename X_ptr>
        X_ptr checkCorbaServer(const std::string &n, CosNaming::NamingContext_var &cxt);

        /**
         * @brief 経路
         */
        std::vector<Configuration> path_;

        /**
         * @brief 干渉チェック呼び出し回数
         */
        unsigned int countCollisionCheck_;

        /**
         * @brief 干渉チェックに使用したクロック数
         */
        TimeMeasure timeCollisionCheck_, timeForwardKinematics_;

        CORBA::ORB_var orb_;

        WorldPtr world_;

        OpenHRP::CollisionDetector_var collisionDetector_;
        OpenHRP::CharacterPositionSequence_var allCharacterPositions_;

        void _setupCharacterData();
        void _updateCharacterPositions();

        bool bboxMode_;

        std::vector<hrp::ColdetModelPair> checkPairs_;
        //< point cloud created by vision or range sensor
        std::vector<hrp::Vector3> pointCloud_; 
        double radius_; ///< radius of spheres assigned to points

        hrp::ColdetModelPair *collidingPair_;

        CollisionDetector *customCollisionDetector_;

        bool defaultCheckCollision();
    public:
        /**
         * @brief 物理世界を取得する
         * @return 物理世界
         */
        WorldPtr world();

        /**
         * @brief ロボットを取得する
         * @return ロボット
         */
        hrp::BodyPtr robot();

        /**
         * @brief コンフィギュレーションベクトルからロボットの姿勢をセットする関数をセットする
         * @param i_func コンフィギュレーションベクトルからロボットの姿勢をセットする関数
         */
        void setApplyConfigFunc(applyConfigFunc i_func);

        /**
         * @brief コンフィギュレーションをセットする
         * @param pos コンフィギュレーション
         */
        bool setConfiguration(const Configuration &pos);

        /**
         * @brief 物理世界の状況を取得する
         * @param wstate 物理世界の状況
         */
        void getWorldState(OpenHRP::WorldState_out wstate);

        /**
         * @brief 干渉チェック対象となるポイントクラウドを設定する
         * @param i_cloud ポイントクラウド
         * @param i_radius ポイントクラウドの各点に割り当てる球の半径
         */
        void setPointCloud(const std::vector<hrp::Vector3>& i_cloud, double i_radius);

        /**
         * @brief コンストラクタ
         * @param dim コンフィギュレーション空間の次元
         * @param world 物理世界
         * @param isDebugMode デバッグモードにするか否か
         */
        PathPlanner(unsigned int dim, 
                    WorldPtr world = WorldPtr(),
                    bool isDebugMode = false);

        /**
         * @brief デストラクタ
         */
        ~PathPlanner();

        /**
         * @brief 経路計画アルゴリズムの登録
         * 
         * @param algorithmName 経路計画アルゴリズム名
         * @param newFunc
         * @param deleteFunc
         */
        void registerAlgorithm(const std::string &algorithmName, AlgorithmNewFunc newFunc, AlgorithmDeleteFunc deleteFunc);

        /**
         * @brief 移動能力の登録
         * 
         * @param mobilityName 移動能力名
         * @param newFunc
         * @param deleteFunc
         */
        void registerMobility(const std::string &mobilityName, MobilityNewFunc newFunc, MobilityDeleteFunc deleteFunc);
    
        /**
         * @brief 経路最適化アルゴリズムの登録
         * 
         * @param optimizerName 経路最適化アルゴリズム名
         * @param newFunc
         * @param deleteFunc
         */
        void registerOptimizer(const std::string &optimizerName, OptimizerNewFunc newFunc, OptimizerDeleteFunc deleteFunc);
    
        /**
         * @brief 初期化。NameServer, ModelLoaderとの接続を行う。
         * @param nameServer CORBAネームサーバの位置をホスト名:ポート番号の形式で指定る。
         */
        void initPlanner(const std::string &nameServer);

        /**
         * @brief キャラクタを動力学シミュレータに登録する。
         * @param name モデル名
         * @param cInfo モデルデータ
         */
        hrp::BodyPtr registerCharacter(const char* name,OpenHRP::BodyInfo_ptr cInfo);

        /**
         * @brief キャラクタを動力学シミュレータに登録する。
         * @param name モデル名
         * @param i_body モデルデータ
         */
        hrp::BodyPtr registerCharacter(const char *name, hrp::BodyPtr i_body);

        /**
         * @brief キャラクタを動力学シミュレータに登録する。
         * @param name モデル名
         * @param url モデルURL
         */
        hrp::BodyPtr registerCharacterByURL(const char* name, const char* url);

        /**
         * @brief 位置を設定する
         *
         * @param character キャラクタ名
         * @param pos 位置姿勢
         */
        void setCharacterPosition(const char* character, 
                                  const OpenHRP::DblSequence& pos);

        /**
         * @brief 衝突検出ペアを設定する
         * @param char1 キャラクタ1
         * @param name1 キャラクタ1のリンク
         * @param char2 キャラクタ2
         * @param name2 キャラクタ2のリンク
         * @param tolerance リンク間の距離がこの値以下になると干渉と見なされる
         */
        void registerIntersectionCheckPair(const char* char1, 
                                           const char* name1, 
                                           const char* char2,
                                           const char* name2,
                                           CORBA::Double tolerance);


        /**
         * @brief 動力学シミュレータを初期化する
         */
        void initSimulation();

        /**
         * @brief 移動能力名の一覧を取得する
         *
         * @return 移動能力名の配列
         */
        void getMobilityNames(std::vector<std::string> &mobilitys);

        /**
         * @brief 経路計画アルゴリズム名の一覧を取得する
         *
         * @return 経路計画アルゴリズム名文字列の配列
         */
        void getAlgorithmNames(std::vector<std::string> &algorithms);

        /**
         * @brief 経路最適化アルゴリズム名の一覧を取得する
         *
         * @return 経路最適化アルゴリズム名文字列の配列
         */
        void getOptimizerNames(std::vector<std::string> &optimizers);

        /**
         * @brief 経路計画アルゴリズムのプロパティ名一覧を取得する
         * @param algorithm 経路計画アルゴリズムの名称
         * @param names プロパティ名文字列の配列
         * @param values プロパティ値文字列の配列
         * @return 取得に成功した場合true、指定したアルゴリズムが見つからなかった場合falseを返す
         */
        bool getProperties(const std::string &algorithm,
                           std::vector<std::string> &names,
                           std::vector<std::string> &values);
  
        /**
         * @brief 移動動作の設計対象にするモデルを設定する
         * @param model モデル名
         */
        void setRobotName(const std::string &model);

      
        /**
         * @brief 使用する経路計画アルゴリズム名を指定する
         * @param algorithm 経路計画アルゴリズム名
         */
        void setAlgorithmName(const std::string &algorithm);

      
        /**
         * @brief 使用する移動能力を設定する
         * @param mobility 移動能力名
         * @return 指定された移動能力が登録されていない場合false、それ以外はtrue
         */
        bool setMobilityName(const std::string &mobility);

        /**
         * @brief アルゴリズムに対して各種情報を設定する
         * @param properties name-valueの組
         */
        void setProperties(const std::map<std::string, std::string> &properties) {algorithm_->setProperties(properties);}
        /**
         * @brief スタート位置を設定する
         * @param pos 初期位置
         */
        void setStartConfiguration(const Configuration &pos) {algorithm_->setStartConfiguration(pos);}

        /**
         * @brief ゴール位置を設定する
         * @param pos 終了位置
         */
        void setGoalConfiguration(const Configuration &pos) {algorithm_->setGoalConfiguration(pos);}

        /**
         * @brief 経路計画を行う
         * @return 計画が正常に終了した場合true、それ以外はfalseを返す
         */
        bool calcPath();

        /**
         * @brief 計算を中止する
         */
        void stopPlanning() {algorithm_->stopPlanning();}
    
        /**
         * @brief ロードマップを取得する
         * @return ロードマップ
         */
        Roadmap *getRoadmap() { return algorithm_->getRoadmap();}

        /**
         * @brief 計画された経路の補間されたものを取得する
         * @return 補間された経路
         */
        std::vector<Configuration> getPath();

        /**
         * @brief 計画された経路を取得する
         * @return 計画された経路
         */
        std::vector<Configuration>& getWayPoints();

        /**
         * @brief 移動能力を取得する
         * @return 移動能力
         */
        Mobility* getMobility() {return mobility_;}

        /**
         * @brief 計画アルゴリズムを取得する
         * @return 計画アルゴリズム
         */
        Algorithm* getAlgorithm() {return algorithm_;}

        /**
         * @brief コンフィギュレーション空間設定を取得する
         * @return コンフィギュレーション空間設定
         */
        ConfigurationSpace* getConfigurationSpace() { return &cspace_; }

        /**
         * @brief 干渉検出を行う
         * @return 干渉している場合true, それ以外false
         */
        bool checkCollision();
        /**
         * @brief 干渉検出を行う
         * @param pos ロボットの位置
         * @return 干渉している場合true, それ以外false
         */
        bool checkCollision(const Configuration &pos);

        /**
         * @brief パスの干渉検出を行う
         * @param path パス
         * @return 一点でも干渉しているとtrue
         */
        bool checkCollision(const std::vector<Configuration> &path);

        /**
         * @brief デバッグモードの変更
         * @param debug デバッグモードのOn/Off
         */
        void setDebug(bool debug) {debug_ = debug;}

        /**
         * @brief 指定した方法で経路を最適化する
         * @param optimizer 最適化法の名称
         * @return 指定した最適化法が見つからなかった場合false、それ以外true
         */
        bool optimize(const std::string& optimizer);

        /**
         * @brief 干渉チェックを呼び出した回数を取得する
         * @return 干渉チェックを呼び出した回数
         */
        unsigned int countCollisionCheck() const { return timeCollisionCheck_.numCalls();}

        /**
         * @brief 干渉チェックに使用した時間[s]を取得する
         * @return 干渉チェックに使用した時間[s]
         */
        double timeCollisionCheck() const;

        double timeForwardKinematics() const;

        void boundingBoxMode(bool mode) { bboxMode_ = mode; } 

        hrp::ColdetModelPair *collidingPair() { return collidingPair_; }

        void setCollisionDetector(CollisionDetector *i_cd){
            customCollisionDetector_ = i_cd; 
        }
    };
};
#endif // __PATH_PLANNER_H__

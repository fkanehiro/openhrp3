#include <math.h>
// planning algorithms
#include "RRT.h"
#include "PRM.h"
// mobilities
#include "TGT.h"
#include "OmniWheel.h"
// optimizers
#include "ShortcutOptimizer.h"
#include "RandomShortcutOptimizer.h"
//
#include "PathPlanner.h"

#include <hrpCorba/OpenHRPCommon.hh>
#include <hrpModel/Body.h>
#include <hrpModel/Link.h>
#include <hrpModel/ModelLoaderUtil.h>

using namespace PathEngine;
using namespace hrp;

//static const bool USE_INTERNAL_COLLISION_DETECTOR = false;
static const bool USE_INTERNAL_COLLISION_DETECTOR = true;

// ----------------------------------------------
// ネームサーバからオブジェクトを取得
// ----------------------------------------------
template<typename X, typename X_ptr>
X_ptr PathPlanner::checkCorbaServer(const std::string &n, CosNaming::NamingContext_var &cxt)
{
    CosNaming::Name ncName;
    ncName.length(1);
    ncName[0].id = CORBA::string_dup(n.c_str());
    ncName[0].kind = CORBA::string_dup("");
    X_ptr srv = NULL;
    try {
        srv = X::_narrow(cxt->resolve(ncName));
    } catch(const CosNaming::NamingContext::NotFound &exc) {
        std::cerr << n << " not found: ";
        switch(exc.why) {
        case CosNaming::NamingContext::missing_node:
            std::cerr << "Missing Node" << std::endl;
        case CosNaming::NamingContext::not_context:
            std::cerr << "Not Context" << std::endl;
            break;
        case CosNaming::NamingContext::not_object:
            std::cerr << "Not Object" << std::endl;
            break;
        }
        return (X_ptr)NULL;
    } catch(CosNaming::NamingContext::CannotProceed &exc) {
        std::cerr << "Resolve " << n << " CannotProceed" << std::endl;
    } catch(CosNaming::NamingContext::AlreadyBound &exc) {
        std::cerr << "Resolve " << n << " InvalidName" << std::endl;
    }
    return srv;
}

bool setConfigurationToBaseXYTheta(PathPlanner *planner, const Configuration& cfg)
{
    Link *baseLink = planner->robot()->rootLink();
    // 目標値設定  
    baseLink->p(0) = cfg.value(0);
    baseLink->p(1) = cfg.value(1);
    Matrix33 R;

    double c = cos(cfg.value(2)), s = sin(cfg.value(2));
    R(0,0) = c; R(0,1) = -s; R(0,2) = 0;
    R(1,0) = s; R(1,1) =  c; R(1,2) = 0;
    R(2,0) = 0; R(2,1) =  0; R(2,2) = 1;
    baseLink->setSegmentAttitude(R);
    planner->robot()->calcForwardKinematics();

    return true;
}

// ----------------------------------------------
// コンストラクタ
// ----------------------------------------------
PathPlanner::PathPlanner(bool isDebugMode) 
    : m_applyConfigFunc(&setConfigurationToBaseXYTheta), algorithm_(NULL), mobility_(NULL), debug_(isDebugMode), collidingPair_(NULL)
{
    if (isDebugMode) {
        std::cerr << "PathPlanner::PathPlanner() : debug mode" << std::endl;
    }

    // planning algorithms
    registerAlgorithm("RRT", AlgorithmCreate<RRT>, AlgorithmDelete<RRT>);
    registerAlgorithm("PRM", AlgorithmCreate<PRM>, AlgorithmDelete<PRM>);

    // mobilities
    registerMobility("TurnGoTurn", MobilityCreate<TGT>, MobilityDelete<TGT>);
    registerMobility("OmniWheel", MobilityCreate<OmniWheel>, MobilityDelete<OmniWheel>);
    //registerMobility("ArcAndOdv", MobilityCreate<ArcAndOdv>, MobilityDelete<ArcAndOdv>);

    // optimizers
    registerOptimizer("Shortcut", 
                      OptimizerCreate<ShortcutOptimizer>,
                      OptimizerDelete<ShortcutOptimizer>);
    registerOptimizer("RandomShortcut", 
                      OptimizerCreate<RandomShortcutOptimizer>,
                      OptimizerDelete<RandomShortcutOptimizer>);
		    
    allCharacterPositions_ = new OpenHRP::CharacterPositionSequence;

    //bboxMode_ = true;
    bboxMode_ = false;

    dt_ = 0.01;
}

// ----------------------------------------------
// デストラクタ
// ----------------------------------------------
PathPlanner::~PathPlanner() {
    if (debug_) {
        std::cerr << "PathPlanner::~PathPlanner()" << std::endl;
    }
    if (algorithm_ != NULL) {
        algorithmFactory_[algorithmName_].second(algorithm_);
    }

    if (mobility_ != NULL) {
        mobilityFactory_[mobilityName_].second(mobility_);
    }
}

void PathPlanner::getOptimizerNames(std::vector<std::string> &optimizers) {
    optimizers.clear();
    OptimizerFactory::iterator it = optimizerFactory_.begin();
    while (it != optimizerFactory_.end()) {
        optimizers.push_back((*it).first);
        it++;
    }
}

void PathPlanner::getMobilityNames(std::vector<std::string> &mobilities) {
    mobilities.clear();
    MobilityFactory::iterator it = mobilityFactory_.begin();
    while (it != mobilityFactory_.end()) {
        mobilities.push_back((*it).first);
        it++;
    }
}

// ----------------------------------------------
// アルゴリズム一覧取得
// ----------------------------------------------
void PathPlanner::getAlgorithmNames(std::vector<std::string> &algorithms) {
    algorithms.clear();
    AlgorithmFactory::iterator it = algorithmFactory_.begin();
    while (it != algorithmFactory_.end()) {
        algorithms.push_back((*it).first);
        it++;
    }
}

// ----------------------------------------------
// プロパティ一覧取得
// ----------------------------------------------
bool PathPlanner::getProperties(const std::string &algorithm,
				std::vector<std::string> &names,
				std::vector<std::string> &values)
{
    if (debug_) {
        std::cerr << "PathPlanner::getPropertyNames(" << algorithm << ")" << std::endl;
    }
    if (algorithmFactory_.count(algorithm) > 0) {
        Algorithm* algorithmInst = algorithmFactory_[algorithm].first(this);
        algorithmInst->getProperties(names, values);
        algorithmFactory_[algorithm].second(algorithmInst);
    }
    else {
        std::cerr << "algorithm not found" << std::endl;
        return false;
    }
    return true;
}

// ----------------------------------------------
// 初期化
// ----------------------------------------------
void PathPlanner::initPlanner(const std::string &nameServer) {

    if (debug_) {
        std::cerr << "PathPlanner::initPlanner(" << nameServer << ")" << std::endl;
    }

    try {
        int ac = 0;
        char* av[1];
        av[0] = NULL;
        orb_ = CORBA::ORB_init(ac, av);
        //
        // Resolve Root POA
        //
        CORBA::Object_var poaObj = orb_ -> resolve_initial_references("RootPOA");
        PortableServer::POA_var rootPOA = PortableServer::POA::_narrow(poaObj);

        //
        // Get a reference to the POA manager
        //
        PortableServer::POAManager_var manager = rootPOA -> the_POAManager();

        std::ostringstream stream;
        stream << "corbaloc:iiop:" << nameServer << "/NameService";

        CosNaming::NamingContext_var cxT;
        CORBA::Object_var	nS = orb_->string_to_object(stream.str().c_str());
        cxT = CosNaming::NamingContext::_narrow(nS);

        if (is_nil(cxT)) {
            std::cerr << "name serivce not found" << std::endl;
        }else{
            std::cout << "NameService OK." << std::endl;
        }

        if (!USE_INTERNAL_COLLISION_DETECTOR){
            std::cerr << "CollisonDetectorFactory ";
            OpenHRP::CollisionDetectorFactory_var cdFactory =
                checkCorbaServer <OpenHRP::CollisionDetectorFactory,
                OpenHRP::CollisionDetectorFactory_var> ("CollisionDetectorFactory", cxT);
            if (CORBA::is_nil(cdFactory)) {
                std::cerr << "not found" << std::endl;
            }else{
                std::cout << "OK." << std::endl;
            }
            try{
                collisionDetector_ = cdFactory->create();
            }catch(...){
                std::cerr << "failed to create CollisionDetector" << std::endl;
            }
        }

        std::cerr << "ModelLoader ";
        modelLoader_ =
            checkCorbaServer <OpenHRP::ModelLoader,
            OpenHRP::ModelLoader_var> ("ModelLoader", cxT);

        if (CORBA::is_nil(modelLoader_)) {
            std::cerr << "not found" << std::endl;
        }else{
            std::cout << "OK." << std::endl;
        }

        if (debug_) {
            onlineViewer_ =
                checkCorbaServer <OpenHRP::OnlineViewer,
                OpenHRP::OnlineViewer_var> ("OnlineViewer", cxT);

            if (CORBA::is_nil(onlineViewer_)) {
                std::cerr << "OnlineViewer not found" << std::endl;
            }
        }
    
    } catch (CORBA::SystemException& ex) {
        std::cerr << ex._rep_id() << std::endl;
        return;
    }

    world_.clearBodies();
    countCollisionCheck_ = 0;
    tickCollisionCheck_ = 0;
    tickForwardKinematics_ = 0;
	checkPairs_.clear();

}

void PathPlanner::setAlgorithmName(const std::string &algorithmName)
{
    // アルゴリズム名とインスタンスをセット
    algorithmName_ = algorithmName;
    if (algorithmFactory_.count(algorithmName) > 0) {
        if (algorithm_ != NULL) {
            algorithmFactory_[algorithmName_].second(algorithm_);
        }
        algorithm_ = algorithmFactory_[algorithmName_].first(this);
    } else {
        std::cerr << "algorithm(" << algorithmName << ") not found" 
                  << std::endl;
    }
}

// ----------------------------------------------
// キャラクタをURLから登録
// ----------------------------------------------
BodyPtr PathPlanner::registerCharacterByURL(const char* name, const char* url){
    if (debug_) {
        std::cerr << "PathPlanner::registerCharacterByURL(" << name << ", " << url << ")" << std::endl;
    }

    if (CORBA::is_nil(modelLoader_)) {
        std::cerr << "nil reference to servers" << std::endl;
        return BodyPtr();
    }

    OpenHRP::BodyInfo_ptr cInfo = modelLoader_->getBodyInfo(url);
    BodyPtr body = registerCharacter(name, cInfo);

    if (debug_) {
        if (CORBA::is_nil(onlineViewer_)) {
            std::cerr << "nil reference to OnlineViewer" << std::endl;
        }
        onlineViewer_->load(name, url);
    }
    return body;
}

// ----------------------------------------------
// 位置の設定
// ----------------------------------------------
void PathPlanner::setCharacterPosition(const char* character,
				       const OpenHRP::DblSequence& pos)
{
    if (debug_){
        std::cerr << "PathPlanner::setCharacterPosition(" << character << ", "
                  << pos[0] << ", " << pos[1] << ", " << pos[2] << ")" 
                  << std::endl;
    }
    BodyPtr body = world_.body(character);
    if (!body) std::cerr << "PathPlanner::setCharacterPosition() : character("
                         << character << ") not found" << std::endl;
    Link* l = body->rootLink();
    l->p(0) = pos[0];
    l->p(1) = pos[1];
    l->p(2) = pos[2];
    Matrix33 R;
    getMatrix33FromRowMajorArray(R, pos.get_buffer(), 3);
    l->setSegmentAttitude(R);
}

void computeBoundingBox(BodyPtr body, double min[3], double max[3])
{
    bool firsttime = true;
    Vector3 v, p;
    Link *l, *root=body->rootLink();
	Matrix33 Rt = (Matrix33)trans(root->R);
    float x, y, z;
    for (int i=0; i<body->numLinks(); i++){
        l = body->link(i);
        for (int j=0; j<l->coldetModel->getNumVertices(); j++){
            l->coldetModel->getVertex(j, x, y, z);
            v[0] = x; v[1] = y; v[2] = z;
            p = Rt*(l->R*v+l->p-root->p);
            if (firsttime){
                for (int k=0; k<3; k++) min[k] = max[k] = p[k];
                firsttime = false;
            }else{
                for (int k=0; k<3; k++){
                    if (p[k] < min[k]) min[k] = p[k];
                    if (p[k] > max[k]) max[k] = p[k];
                }
            }
        }
    }
    std::cout << "bounding box of " << body->name() << ": ("
              << min[0] << ", " << min[1] << ", " << min[2] << ") - ("
              << max[0] << ", " << max[1] << ", " << max[2] << ")" 
              << std::endl;
}

BodyPtr createBoundingBoxBody(BodyPtr body)
{
    double min[3], max[3];
    computeBoundingBox(body, min, max);

    ColdetModelPtr coldetModel(new ColdetModel());
    coldetModel->setNumVertices(8);
    coldetModel->setNumTriangles(12);
    coldetModel->setVertex(0, max[0], max[1], max[2]);
    coldetModel->setVertex(1, min[0], max[1], max[2]);
    coldetModel->setVertex(2, min[0], min[1], max[2]);
    coldetModel->setVertex(3, max[0], min[1], max[2]);
    coldetModel->setVertex(4, max[0], max[1], min[2]);
    coldetModel->setVertex(5, min[0], max[1], min[2]);
    coldetModel->setVertex(6, min[0], min[1], min[2]);
    coldetModel->setVertex(7, max[0], min[1], min[2]);
    coldetModel->setTriangle(0, 0, 1, 2);
    coldetModel->setTriangle(1, 0, 2, 3);
    coldetModel->setTriangle(2, 0, 3, 7);
    coldetModel->setTriangle(3, 0, 7, 4);
    coldetModel->setTriangle(4, 0, 4, 5);
    coldetModel->setTriangle(5, 0, 5, 1);
    coldetModel->setTriangle(6, 3, 2, 6);
    coldetModel->setTriangle(7, 3, 6, 7);
    coldetModel->setTriangle(8, 1, 5, 6);
    coldetModel->setTriangle(9, 1, 6, 2);
    coldetModel->setTriangle(10, 4, 7, 6);
    coldetModel->setTriangle(11, 4, 6, 5);
    coldetModel->build();

    Link *root = new Link();
    root->R = tvmet::identity<Matrix33>();
    root->Rs = tvmet::identity<Matrix33>();
    root->coldetModel = coldetModel;

    BodyPtr bboxBody = new Body();
    bboxBody->setRootLink(root);

    return bboxBody;
}

// ----------------------------------------------
// キャラクタをBodyInfoから登録
// ----------------------------------------------
BodyPtr PathPlanner::registerCharacter(const char* name, OpenHRP::BodyInfo_ptr cInfo) {
    if (debug_) {
        std::cerr << "PathPlanner::registerCharacter(" << name << ", " << cInfo << ")" << std::endl;
    }

    BodyPtr body = new Body();

    if(loadBodyFromBodyInfo(body, cInfo, USE_INTERNAL_COLLISION_DETECTOR)){
        body->setName(name);
        if(debug_){
            //std::cout << "Loaded Model:\n" << *body << std::endl;
        }

        if (bboxMode_ && USE_INTERNAL_COLLISION_DETECTOR){
            body = createBoundingBoxBody(body);
            body->setName(name);
        }

        if(!USE_INTERNAL_COLLISION_DETECTOR){
            collisionDetector_->registerCharacter(name, cInfo);
        }
        world_.addBody(body);
    }
    return body;
}

BodyPtr PathPlanner::registerCharacter(const char *name, BodyPtr i_body)
{
    i_body->setName(name);
    world_.addBody(i_body);
    return i_body;
}




// ----------------------------------------------
// ロボット名を設定
// ----------------------------------------------
void PathPlanner::setRobotName(const std::string &name) {
    if (debug_) {
        std::cerr << "PathPlanner::setRobotName(" << name << ")" << std::endl;
    }

    model_ = world_.body(name);
    if (!model_) {
        std::cerr << "PathPlanner::setRobotName() : robot(" << name << ") not found" << std::endl;
        return;
    }
}

// ----------------------------------------------
// ロボット名を設定
// ----------------------------------------------
bool PathPlanner::setMobilityName(const std::string &mobility)
{
    if (debug_) {
        std::cerr << "PathPlanner::setMobilityName(" << mobility << ")" << std::endl;
    }

    mobilityName_ = mobility;
    if (mobilityFactory_.count(mobilityName_) > 0) {
        if (mobility_ != NULL) {
            mobilityFactory_[mobilityName_].second(mobility_);
        }
        mobility_ = mobilityFactory_[mobilityName_].first(this);
    }
    else {
        std::cerr << "PathPlanner::setMobilityName() : mobility(" << mobility << ") not found" << std::endl;
        return false;
    }
    return true;
}



// ----------------------------------------------
// 衝突チェックペアを登録
// ----------------------------------------------
void PathPlanner::registerIntersectionCheckPair(const char* charName1, 
                                                const char* linkName1, 
                                                const char* charName2,
                                                const char* linkName2,
                                                CORBA::Double tolerance) {
    if (debug_){
        std::cout << "PathPlanner::registerIntersectionCheckPair("
                  << charName1 << ", " << linkName1 << ", " << charName2 
                  << ", " << linkName2
                  << ", " << tolerance << ")" << std::endl;
    }
    int bodyIndex1 = world_.bodyIndex(charName1);
    int bodyIndex2 = world_.bodyIndex(charName2);

    if(bodyIndex1 >= 0 && bodyIndex2 >= 0){

        BodyPtr body1 = world_.body(bodyIndex1);
        BodyPtr body2 = world_.body(bodyIndex2);

        std::string emptyString = "";
        std::vector<Link*> links1;
        if(emptyString == linkName1){
            const LinkTraverse& traverse = body1->linkTraverse();
            links1.resize(traverse.numLinks());
            std::copy(traverse.begin(), traverse.end(), links1.begin());
        } else {
            links1.push_back(body1->link(linkName1));
        }

        std::vector<Link*> links2;
        if(emptyString == linkName2){
            const LinkTraverse& traverse = body2->linkTraverse();
            links2.resize(traverse.numLinks());
            std::copy(traverse.begin(), traverse.end(), links2.begin());
        } else {
            links2.push_back(body2->link(linkName2));
        }

        for(size_t i=0; i < links1.size(); ++i){
            for(size_t j=0; j < links2.size(); ++j){
                Link* link1 = links1[i];
                Link* link2 = links2[j];

                if(link1 && link2 && link1 != link2){
                    if(USE_INTERNAL_COLLISION_DETECTOR){
                        checkPairs_.push_back(ColdetModelPair(link1->coldetModel,
                                                              link2->coldetModel,
                                                              tolerance));
                    }else{
                        OpenHRP::LinkPair_var linkPair = new OpenHRP::LinkPair();
                        linkPair->charName1  = CORBA::string_dup(charName1);
                        linkPair->linkName1 = CORBA::string_dup(link1->name.c_str());
                        linkPair->charName2  = CORBA::string_dup(charName2);
                        linkPair->linkName2 = CORBA::string_dup(link2->name.c_str());
                        linkPair->tolerance = tolerance;
                        collisionDetector_->addCollisionPair(linkPair);
                    }
                }
            }
        }
    }
}
// ----------------------------------------------
// 初期化
// ----------------------------------------------
void PathPlanner::initSimulation () {
    world_.setTimeStep(0.002);
    world_.setCurrentTime(0.0);
    world_.setRungeKuttaMethod();
    world_.enableSensors(true);
  
    int n = world_.numBodies();
    for(int i=0; i < n; ++i){
        world_.body(i)->initializeConfiguration();
    }
  
    _setupCharacterData();
  
    Vector3 g;
    g[0] = g[1] = 0.0; g[2] = 9.8;
    world_.setGravityAcceleration(g);
  
    world_.initialize();
}

bool PathPlanner::setConfiguration(const Configuration &pos)
{
    return (*m_applyConfigFunc)(this, pos);
}

bool PathPlanner::checkCollision (const Configuration &pos) {
#if 0
    if (debug_) {
        std::cerr << "checkCollision(" << pos << ")" << std::endl;
    }
#endif
    if (!setConfiguration(pos)) return true;

    // 干渉チェック
    tick_t t1 = get_tick();
    bool ret = checkIntersection();
    countCollisionCheck_++; 
    tickCollisionCheck_ += get_tick() - t1;

    if (debug_) {
        // 結果を得る
        OpenHRP::WorldState_var state;
        getWorldState(state);

        static double nowTime = 0;
        state->time = nowTime;
        nowTime += dt_;

        onlineViewer_->update(state);
    }
    return ret;
}

void PathPlanner::getWorldState(OpenHRP::WorldState_out wstate)
{
    if(debug_){
        std::cout << "DynamicsSimulator_impl::getWorldState()\n";
    }

    _updateCharacterPositions();

    wstate = new OpenHRP::WorldState;

    wstate->time = world_.currentTime();
    wstate->characterPositions = allCharacterPositions_;

    if(debug_){
        std::cout << "getWorldState - exit" << std::endl;
    }
}

bool PathPlanner::checkIntersection()
{
    tick_t t1 = get_tick();

    collidingPair_ = NULL;

    if (USE_INTERNAL_COLLISION_DETECTOR){
        Link *l;
            for (int j=0; j<model_->numLinks(); j++){
                l = model_->link(j);
                l->coldetModel->setPosition(l->R, l->p);
            }
    }else{
        _updateCharacterPositions();
    }
    tickForwardKinematics_ += get_tick() - t1; 
    if(USE_INTERNAL_COLLISION_DETECTOR){
        for (unsigned int i=0; i<checkPairs_.size(); i++){
            if (checkPairs_[i].tolerance() == 0){
                if (checkPairs_[i].checkCollision()){
                    collidingPair_ = &checkPairs_[i];
                    return true;
                }
            } else{
                if (checkPairs_[i].detectIntersection()) return true;
            } 
        } 
        if (pointCloud_.size() > 0){
            for (int i=0; i<model_->numLinks(); i++){
                Link *l = model_->link(i);
                if (l->coldetModel->checkCollisionWithPointCloud(pointCloud_,
                                                                 radius_)){
                    return true;
                }
            }
        }
        return false;
    }else{
        OpenHRP::LinkPairSequence_var pairs = new OpenHRP::LinkPairSequence;
        collisionDetector_->queryIntersectionForDefinedPairs(false, allCharacterPositions_.in(), pairs);
        
        return pairs->length() > 0;
    }
}

// ----------------------------------------------
// アルゴリズム登録
// ----------------------------------------------
void PathPlanner::registerAlgorithm(const std::string &algorithmName, AlgorithmNewFunc newFunc, AlgorithmDeleteFunc deleteFunc) {
    algorithmFactory_.insert(AlgorithmFactoryValueType(algorithmName, std::make_pair(newFunc, deleteFunc)));
}

void PathPlanner::registerMobility(const std::string &mobilityName, MobilityNewFunc newFunc, MobilityDeleteFunc deleteFunc) {
    mobilityFactory_.insert(MobilityFactoryValueType(mobilityName, std::make_pair(newFunc, deleteFunc)));
}
void PathPlanner::registerOptimizer(const std::string &optimizerName, OptimizerNewFunc newFunc, OptimizerDeleteFunc deleteFunc) {
    optimizerFactory_.insert(OptimizerFactoryValueType(optimizerName, std::make_pair(newFunc, deleteFunc)));
}
bool PathPlanner::checkCollision(const std::vector<Configuration> &path) {
    unsigned int checked = 0;
    unsigned int div = 2;

    std::vector<bool> isVisited;
    for (unsigned int i=0; i<path.size(); i++) {
        isVisited.push_back(false);
    }

 
    while (checked < (path.size()-1) || path.size()/div > 0) {
        int step = path.size()/div;
        for (unsigned int i=step; i<path.size(); i+=step) {
            if (!isVisited[i]) {
                checked++;
                if (checkCollision(path[i])) {
                    return true;
                }
                isVisited[i] = true;
            }
        }
        div++;
    }
    if (checked != path.size()-1) {
        std::cout << "checkCollision() : there are unchecked configurations."
                  << " path.size() = " << path.size() << ", checked = " 
                  << checked << std::endl;
    }
    return checkCollision(path[0]);
}
bool PathPlanner::calcPath()
{
    path_.clear();
    BodyPtr body;
    Link *l;
    for (int i=0; i<world_.numBodies(); i++){
        body = world_.body(i);
        body->calcForwardKinematics();
        for (int j=0; j<body->numLinks(); j++){
            l = body->link(j);
            l->coldetModel->setPosition(l->R, l->p);
        }
    }
    std::cout << "The number of collision check pairs = " << checkPairs_.size() << std::endl;
    if (!algorithm_->preparePlanning()){
        std::cout << "preparePlanning() failed" << std::endl;
        return false;
    }

    if (algorithm_->tryDirectConnection()){
        path_ = algorithm_->getPath();
        std::cout << "connected directly" << std::endl;
        return true;
    }
    std::cout << "failed direct connection" << std::endl;

    if (algorithm_->calcPath()){
        path_ = algorithm_->getPath();
        return true;
    }

    return false;
}

bool PathPlanner::optimize(const std::string& optimizer)
{
    if (optimizerFactory_.count(optimizer) > 0) {
        Optimizer *opt = optimizerFactory_[optimizer].first(this);
        path_ = opt->optimize(path_);
        optimizerFactory_[optimizer].second(opt);
        return true;
    }else{
        return false;
    }
}

std::vector<Configuration>& PathPlanner::getWayPoints()
{
    return path_;
}
std::vector<Configuration> PathPlanner::getPath()
{
    std::vector<Configuration> finalPath;
    if (path_.size() == 0) return finalPath;

    for (unsigned int i=0; i<path_.size()-1; i++) {
        std::vector<Configuration> localPath;
        mobility_->getPath(path_[i], path_[i+1],localPath);
        finalPath.insert(finalPath.end(), localPath.begin(), localPath.end()-1);
    }
    finalPath.push_back(path_[path_.size()-1]);

    return finalPath;
}  

void PathPlanner::_setupCharacterData()
{
    if(debug_){
        std::cout << "PathPlanner::_setupCharacterData()\n";
    }

    int n = world_.numBodies();
    allCharacterPositions_->length(n);

    for(int i=0; i < n; ++i){
        BodyPtr body = world_.body(i);

        int numLinks = body->numLinks();
        OpenHRP::CharacterPosition& characterPosition = allCharacterPositions_[i];
        characterPosition.characterName = CORBA::string_dup(body->name().c_str());
        OpenHRP::LinkPositionSequence& linkPositions = characterPosition.linkPositions;
        linkPositions.length(numLinks);

        if(debug_){
            std::cout << "character[" << i << "], nlinks = " << numLinks << "\n";
        }
    }

    if(debug_){
        std::cout << "_setupCharacterData() - exit" << std::endl;;
    }
}


void PathPlanner::_updateCharacterPositions()
{
    if(debug_){
        std::cout << "PathPlanner::_updateCharacterPositions()\n";
    }

    int n = world_.numBodies();

    {	
#pragma omp parallel for num_threads(3)
        for(int i=0; i < n; ++i){
            BodyPtr body = world_.body(i);
            int numLinks = body->numLinks();
			
            OpenHRP::CharacterPosition& characterPosition = allCharacterPositions_[i];
			
            if(debug_){
                std::cout << "character[" << i << "], nlinks = " << numLinks << "\n";
            }
			
            for(int j=0; j < numLinks; ++j) {
                Link* link = body->link(j);
                OpenHRP::LinkPosition& linkPosition = characterPosition.linkPositions[j];
                setVector3(link->p, linkPosition.p);
                setMatrix33ToRowMajorArray(link->segmentAttitude(), linkPosition.R);
            }
        }
    }

    if(debug_){
        std::cout << "_updateCharacterData() - exit" << std::endl;
    }
}


double PathPlanner::timeCollisionCheck() const
{
    return tickCollisionCheck_/get_cpu_frequency();
}

double PathPlanner::timeForwardKinematics() const
{
    return tickForwardKinematics_/get_cpu_frequency();
}

void PathPlanner::setApplyConfigFunc(applyConfigFunc i_func)
{
    m_applyConfigFunc = i_func;
}

BodyPtr PathPlanner::robot()
{
    return model_;
}

void PathPlanner::setPointCloud(const std::vector<Vector3>& i_cloud, 
                                double i_radius)
{
    pointCloud_ = i_cloud;
    radius_ = i_radius;
}

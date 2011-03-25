// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 8; -*-

#include <stdio.h>
#include <string>
#include <hrpUtil/OnlineViewerUtil.h>
#include <hrpModel/ModelLoaderUtil.h>
#include <hrpCorba/DynamicsSimulator.hh>

using namespace std;
using namespace hrp;
using namespace OpenHRP;

enum {X, Y, Z};
#define deg2rad(x) ( 3.14159265358979 / 180*(x) )
#define DOF 29    // 初期姿勢配列の長さ

// サーバ群の取得
template <typename X, typename X_ptr> 
X_ptr checkCorbaServer(std::string n, CosNaming::NamingContext_var &cxt)
{
    CosNaming::Name ncName;
    ncName.length(1);
    ncName[0].id = CORBA::string_dup(n.c_str());
    ncName[0].kind = CORBA::string_dup("");
    X_ptr srv = NULL;
    try 
    {
        srv = X::_narrow(cxt->resolve(ncName));
    } 
    catch(const CosNaming::NamingContext::NotFound &exc) 
    {
        std::cerr << n << " not found: ";
        switch(exc.why) 
        {
            case CosNaming::NamingContext::missing_node:
                std::cerr << "Missing Node" << std::endl;
                break;
            case CosNaming::NamingContext::not_context:
                std::cerr << "Not Context" << std::endl;
                break;
            case CosNaming::NamingContext::not_object:
                std::cerr << "Not Object" << std::endl;
                break;
        }
        return (X_ptr)NULL;
    } 
    catch(CosNaming::NamingContext::CannotProceed &exc) 
    {
        std::cerr << "Resolve " << n << " CannotProceed" << std::endl;
    } 
    catch(CosNaming::NamingContext::AlreadyBound &exc) 
    {
        std::cerr << "Resolve " << n << " InvalidName" << std::endl;
    }
    return srv;
}


int main(int argc, char* argv[])
{
    int i;
    string url = "file://";
    double ddp = 0.001;
    double timeK = 10.0;

    // -urlでモデルのURLを指定  
    for(i=0; i < argc - 1; i++)
    {
        if( strcmp(argv[i], "-url") == 0)
        {
            url += argv[++i];
        } else if( strcmp(argv[i], "-ddp") == 0)
        {
            ddp = strtod( argv[++i], NULL);
        } else if( strcmp(argv[i], "-timeK") == 0)
        {
            timeK = strtod( argv[++i], NULL);
        }
    }

    string robot_url = url+"sample.wrl";
    string floor_url = url+"floor.wrl";

    const char *ROBOT_URL = robot_url.c_str();
    const char *FLOOR_URL = floor_url.c_str();

    //////////////////////////////////////////////////////////////////////

    // CORBA初期化
    CORBA::ORB_var orb;
    orb = CORBA::ORB_init(argc, argv);

    // ROOT POA
    CORBA::Object_var poaObj = orb -> resolve_initial_references("RootPOA");
    PortableServer::POA_var rootPOA = PortableServer::POA::_narrow(poaObj);

    // POAマネージャへの参照を取得
    PortableServer::POAManager_var manager = rootPOA -> the_POAManager();
    
    // NamingServiceの参照取得
    CosNaming::NamingContext_var cxT;
    {
      CORBA::Object_var    nS = orb->resolve_initial_references("NameService");
      cxT = CosNaming::NamingContext::_narrow(nS);
    }

    /////////////////////////////////////////////////////////////////////////

    // DynamicsSimulatorの取得
    DynamicsSimulatorFactory_var dynamicsSimulatorFactory;
    dynamicsSimulatorFactory = 
        checkCorbaServer <DynamicsSimulatorFactory,
        DynamicsSimulatorFactory_var> ("DynamicsSimulatorFactory", cxT);

    if (CORBA::is_nil(dynamicsSimulatorFactory)) 
    {
        std::cerr << "DynamicsSimulatorFactory not found" << std::endl;
    }

    DynamicsSimulator_var dynamicsSimulator 
        = dynamicsSimulatorFactory->create();

    // モデルの読み込み・登録
    BodyInfo_var robotInfo = loadBodyInfo(ROBOT_URL, argc, argv);
    dynamicsSimulator->registerCharacter("robot", robotInfo);

    // 床の読み込み・登録
    BodyInfo_var floorInfo = loadBodyInfo(FLOOR_URL, argc, argv);
    dynamicsSimulator->registerCharacter("floor", floorInfo);


    /////////////////////////////////////////////////////////////////////////

    // DynamicsSimulatorの初期化
    dynamicsSimulator->init(0.002, 
        DynamicsSimulator::RUNGE_KUTTA, 
        DynamicsSimulator::ENABLE_SENSOR);

    // 重力ベクトルの設定
    DblSequence3 gVector;
    gVector.length(3);
    gVector[0] = gVector[1] = 0;
    gVector[2] = 9.8;
    dynamicsSimulator->setGVector(gVector);
    
    // 関節駆動モードの設定
    dynamicsSimulator->setCharacterAllJointModes(
        "robot", DynamicsSimulator::TORQUE_MODE);

    // 初期姿勢
    double init_pos[] = {0.00E+00, -3.60E-02, 0,  7.85E-02, -4.25E-02,  0.00E+00,
                         1.75E-01, -3.49E-03, 0, -1.57E+00,  0.00E+00,  0.00E+00,
                         0.00E+00,  0.00E+00, -3.60E-02, 0,  7.85E-02, -4.25E-02,
                         0.00E+00,  1.75E-01,  3.49E-03, 0, -1.57E+00,  0.00E+00,
                         0.00E+00,  0.00E+00, 0, 0, 0};

    // 初期姿勢を関節角にセット
    DblSequence q;
    q.length(DOF);
    for (int i=0; i<DOF; i++) 
    {
        q[i] = init_pos[i];
    }
    dynamicsSimulator->setCharacterAllLinkData(
        "robot", DynamicsSimulator::JOINT_VALUE, q);

    // 順運動学計算
    dynamicsSimulator->calcWorldForwardKinematics();

    // 干渉チェックペアの設定－両手 
    DblSequence6 dc, sc;
    dc.length(0);
    sc.length(0);

    dynamicsSimulator->registerCollisionCheckPair(
        "robot",
        "RARM_WRIST_R",
        "robot",
        "LARM_WRIST_R",
        0.5,
        0.5,
        dc,
        sc,
        0.0);

    dynamicsSimulator->initSimulation();

    /////////////////////////////////////////////////////////////////////////

    // OnlineViewerの取得
    OnlineViewer_var onlineViewer = getOnlineViewer(argc, argv);
    // OnlineViewerに対するモデルロード
    try 
    {
        onlineViewer->load("robot", ROBOT_URL);
        onlineViewer->load("floor", FLOOR_URL);
        onlineViewer->setLogName("clap");
        onlineViewer->clearLog();
    } 
    catch (CORBA::SystemException& ex) 
    {
        std::cerr << "Failed to connect GrxUI." << endl;
        return 1;
    }
    /////////////////////////////////////////////////////////////////////////

    // 逆運動学計算の準備 
    double RARM_p[] = {0.197403, -0.210919, 0.93732};
    double RARM_R[] = {0.174891, -0.000607636, -0.984588,
                       0.00348999, 0.999994, 2.77917e-06,
                       0.984582, -0.00343669, 0.174892};

    double LARM_p[] = {0.197403, 0.210919, 0.93732};
    double LARM_R[] = {0.174891, 0.000607636, -0.984588,
                       -0.00348999, 0.999994, -2.77917e-06,
                       0.984582, 0.00343669, 0.174892};
    double dp = 0.0;

    /////////////////////////////////////////////////////////////////////////

    WorldState_var state;        
    while (1) 
    {
        // 逆運動学計算 
        LinkPosition link;
        link.p[0] = RARM_p[0];
        link.p[1] = RARM_p[1] + dp;
        link.p[2] = RARM_p[2];
        for (int i=0; i<9; i++) 
          link.R[i] = RARM_R[i];
        dynamicsSimulator->calcCharacterInverseKinematics(CORBA::string_dup("robot"),
                                  CORBA::string_dup("CHEST"),
                                  CORBA::string_dup("RARM_WRIST_R"),
                                  link);

        link.p[0] = LARM_p[0];
        link.p[1] = LARM_p[1] -  dp;
        link.p[2] = LARM_p[2];
        for (int i=0; i<9; i++) 
          link.R[i] = LARM_R[i];
        dynamicsSimulator->calcCharacterInverseKinematics(CORBA::string_dup("robot"),
                                  CORBA::string_dup("CHEST"),
                                  CORBA::string_dup("LARM_WRIST_R"),
                                  link);                            

        dynamicsSimulator->calcWorldForwardKinematics();
        dp += ddp;
        // OnlineViewer更新 
        try 
        {
            dynamicsSimulator->getWorldState(state);
            state->time = dp*timeK;
            onlineViewer->update(state);
        }
        catch (CORBA::SystemException& ex) 
        {
            std::cerr << "OnlineViewer could not be updated." << endl;
            std::cerr << "ex._rep_id() = " << ex._rep_id() << endl;
            return 1;
        }

        if(state->time >= 200.0){
            std::cout << state->time << endl;
        }

        // 干渉チェック
        dynamicsSimulator->checkCollision(true);

        if (state->collisions.length() > 0) 
        {            
            if (state->collisions[0].points.length() > 0) 
            {
                break;
            }
        }

    }

    return 0;

}

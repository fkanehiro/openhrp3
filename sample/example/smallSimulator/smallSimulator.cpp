#include <hrpUtil/OnlineViewerUtil.h>
#include <hrpModel/ModelLoaderUtil.h>
#include <hrpModel/World.h>
#include <hrpModel/ConstraintForceSolver.h>
#include <hrpModel/Link.h>
#include <hrpUtil/Eigen3d.h>
#include <fstream>

using namespace std;
using namespace hrp;
using namespace OpenHRP;

void  setupCharacterPosition(CharacterPosition &characterPosition, BodyPtr body) 
{
    characterPosition.characterName = CORBA::string_dup(body->name().c_str());
    int numLinks = body->numLinks();
    characterPosition.linkPositions.length(numLinks);
}

void  updateCharacterPosition(CharacterPosition &characterPosition, BodyPtr body) 
{
    for(int j=0; j < body->numLinks(); ++j) {
        LinkPosition &linkPosition = characterPosition.linkPositions[j];
        Link* link = body->link(j);

        setVector3(link->p, linkPosition.p);
        setMatrix33ToRowMajorArray(link->attitude(), linkPosition.R);
    }
}

void getWorldState( WorldBase& world, WorldState& state )
{
    state.time = world.currentTime();
    for (int i=0; i<world.numBodies(); i++){
        updateCharacterPosition(state.characterPositions[i], world.body(i));
    }
}

void initWorldState( WorldBase& world, WorldState& state )
{
    state.characterPositions.length(world.numBodies());
    for (int i=0; i<world.numBodies(); i++){
        setupCharacterPosition(state.characterPositions[i], world.body(i));
    }
}

int main(int argc, char* argv[]) 
{
    double timeStep = 0.001;  // (s)
    double EndTime = 13.0;

    string Model[2];
    double world_gravity = 9.8;  // default gravity acceleration [m/s^2]
    double statFric,slipFric;
    statFric = slipFric = 0.5;   // static/slip friction coefficient 
    double culling_thresh = 0.01;
    bool display = true;

    for (int i=1; i<argc; i++){
        if (strcmp("-ORBconfig", argv[i])==0 || strcmp("-ORBInitRef", argv[i])==0 ){
            argv[++i];  // skip ORB parameter
        }else if (strcmp("-timeStep", argv[i])==0){
            timeStep = atof(argv[++i]);
        }else if (strcmp("-nodisplay", argv[i])==0){
            display = false;
        }
    }

    std::string modelDir = std::string("file://") + OPENHRP_SHARE_DIR + "/sample/model/";
    Model[0] = modelDir + "longfloor.wrl";
    Model[1] = modelDir + "sample.wrl";
        
    for(int j = 0; j < 2; j++){
        cout << "Model: " << Model[j] << endl;
    }

    //================= setup World ======================

    World<ConstraintForceSolver> world;

    world.setTimeStep(timeStep);
    world.setRungeKuttaMethod();
    world.setGravityAcceleration(Vector3(0,0,world_gravity));

    // add bodies
    BodyPtr floor=new Body(), body=new Body();
    loadBodyFromModelLoader(floor, Model[0].c_str(), argc, argv, true);
    int bindex1 = world.addBody(floor);
    loadBodyFromModelLoader(body, Model[1].c_str(), argc, argv, true);
    int bindex2 = world.addBody(body);

    for (int i=0; i<floor->numLinks(); i++){
        Link *link1 = floor->link(i);
        for (int j=i; j<body->numLinks(); j++){
            Link *link2 = body->link(j);
            world.constraintForceSolver.addCollisionCheckLinkPair(
                bindex1, link1, bindex2, link2, 
                statFric, slipFric, culling_thresh, 0.0);
        }
    }

    world.enableSensors(true);

    int nBodies = world.numBodies();
    for(int i=0; i < nBodies; ++i){
        hrp::BodyPtr bodyPtr = world.body(i);
        bodyPtr->initializeConfiguration();
    }
        
    world.initialize();
    world.constraintForceSolver.useBuiltinCollisionDetector(true);

    // initial position and orientation
    Link *root = body->rootLink();
    root->p << 0, 0, 0.7135;
    root->R = Matrix33::Identity();

    double angles[] = {
        0.0, -0.0360373, 0.0, 0.0785047, -0.0424675, 0.0,   //rleg
        0.174533, -0.00349066, 0.0, -1.5708, 0.0, 0.0, 0.0, //rarm
        0.0, -0.0360373, 0.0, 0.0785047, -0.0424675, 0.0,   //lleg
        0.174533, -0.00349066, 0.0, -1.5708, 0.0, 0.0, 0.0, //larm
        0.0, 0.0, 0.0                                       //waist
    };
    for (int i=0; i<29; i++){
        body->joint(i)->q = angles[i];
    }
    body->calcForwardKinematics();

    // ==================  log file   ======================
    static ofstream log_file;
    log_file.open("samplePD.log");
        
    //==================== OnlineViewer (GrxUI) setup ===============
    OnlineViewer_var olv;
    WorldState state;
    if (display){
        olv = getOnlineViewer(argc, argv);
        
        if (CORBA::is_nil( olv )) {
            std::cerr << "OnlineViewer not found" << std::endl;
            return 1;
        }
        try {
            olv->load(floor->name().c_str(), Model[0].c_str());
            olv->load(body->name().c_str(), Model[1].c_str());
            olv->clearLog();
        } catch (CORBA::SystemException& ex) {
            cerr << "Failed to connect GrxUI." << endl;
            return 1;
        }
        initWorldState(world, state);
    }

    // ==================  main loop   ======================
    int i=0;
    int j = 0;
    double time=0.0;
    double controlTime=0.0;
    while ( 1 ) {
        // ================== viewer update ====================
        if (display){
            try {
                getWorldState( world, state );
                olv->update( state );
            } catch (CORBA::SystemException& ex) {
                return 1;
            }
        }
        // ================== simulate one step ==============
        world.constraintForceSolver.clearExternalForces();
        world.calcNextState(state.collisions);
               
        // ================== log data save =====================
        time = world.currentTime();
        log_file << time << " ";
        log_file << root->v[2] << " ";
        log_file << endl;

        if( time > EndTime ) break;
    }

    log_file.close();

    return 0;
}

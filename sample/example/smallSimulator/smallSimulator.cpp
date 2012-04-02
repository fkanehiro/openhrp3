#include <fstream>
#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <rtm/Manager.h>
#include <hrpUtil/OnlineViewerUtil.h>
#include <hrpUtil/Eigen3d.h>
#include <hrpModel/World.h>
#include <hrpModel/ConstraintForceSolver.h>
#include <hrpModel/ModelLoaderUtil.h>
#include "Project.h"
#include "BodyRTC.h"
#include "OnlineViewerUtil.h"
#include "ProjectUtil.h"
#include "OpenRTMUtil.h"

using namespace std;
using namespace hrp;
using namespace OpenHRP;

BodyPtr createBody(const std::string& name, const std::string& url,
                   std::vector<BodyRTCPtr> *bodies,
                   int argc, char *argv[])
{
    RTC::Manager& manager = RTC::Manager::instance();
    std::string args = "BodyRTC?instance_name="+name;
    BodyRTCPtr body = (BodyRTC *)manager.createComponent(args.c_str());
    if (!loadBodyFromModelLoader(body, url.c_str(), argc, argv, true)){
        std::cerr << "failed to load model[" << url << "]" << std::endl;
        manager.deleteComponent(body.get());
        return BodyPtr();
    }else{
        body->createDataPorts();
        bodies->push_back(body);
        return body;
    }
}


int main(int argc, char* argv[]) 
{
    bool display = true;

    for (int i=1; i<argc; i++){
        if (strcmp("-nodisplay", argv[i])==0){
            display = false;
        }
    }

    Project prj;
    if (!prj.parse(argv[1])){
        std::cerr << "failed to parse " << argv[1] << std::endl;
        return 1;
    }

    //================= OpenRTM =========================
    RTC::Manager* manager;
    manager = RTC::Manager::init(argc, argv);
    manager->init(argc, argv);
    BodyRTC::moduleInit(manager);
    manager->activateManager();
    manager->runManager(true);

    //================= setup World ======================
    World<ConstraintForceSolver> world;
    std::vector<BodyRTCPtr> bodies; 
    BodyFactory factory = boost::bind(createBody, _1, _2, &bodies, argc, argv);
    initWorld(prj, factory, world);

    std::vector<ClockReceiver> receivers;
    initRTS(prj, receivers);
    std::cout << "number of receivers:" << receivers.size() << std::endl;

    //==================== OnlineViewer (GrxUI) setup ===============
    OnlineViewer_var olv;
    WorldState state;
    if (display){
        olv = getOnlineViewer(argc, argv);
        
        if (CORBA::is_nil( olv )) {
            std::cerr << "OnlineViewer not found" << std::endl;
            return 1;
        }
        for (std::map<std::string, ModelItem>::iterator it=prj.models().begin();
             it != prj.models().end(); it++){
            try {
                olv->load(it->first.c_str(), it->second.url.c_str());
            } catch (CORBA::SystemException& ex) {
                cerr << "Failed to connect GrxUI." << endl;
                return 1;
            }
        }
        olv->clearLog();

        initWorldState(state, world);
    }

    // ==================  main loop   ======================
    while (world.currentTime() < prj.totalTime()) {
        // ================== viewer update ====================
        if (display){
            try {
                getWorldState(state, world);
                olv->update( state );
            } catch (CORBA::SystemException& ex) {
                return 1;
            }
        }
        // ================== simulate one step ==============
        for (unsigned int i=0; i<bodies.size(); i++){
            bodies[i]->writeDataPorts();
        }

        for (unsigned int i=0; i<bodies.size(); i++){
            bodies[i]->readDataPorts();
        }

        for (unsigned int i=0; i<receivers.size(); i++){
            receivers[i].tick(world.timeStep());
        }

        world.constraintForceSolver.clearExternalForces();
        world.calcNextState(state.collisions);
    }
    manager->shutdown();

    return 0;
}

// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 8; -*-
#include <hrpUtil/OnlineViewerUtil.h>
#include <hrpModel/ModelLoaderUtil.h>
#include <hrpCorba/DynamicsSimulator.hh>
#include <hrpCorba/Controller.hh>
#include <hrpUtil/Eigen3d.h>
#include <fstream>

using namespace std;
using namespace hrp;
using namespace OpenHRP;

template <typename X, typename X_ptr>
X_ptr checkCorbaServer(std::string n, CosNaming::NamingContext_var &cxt)
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

int main(int argc, char* argv[]) 
{
	double timeStep = 0.001;  // (s)
    double controlTimeStep = 0.002;
	double EndTime = 13.0;

	string Model[2], ModelFname[2];
	double world_gravity = 9.8;  // default gravity acceleration [m/s^2]
    double statFric,slipFric;
	statFric = slipFric = 0.5;   // static/slip friction coefficient 
    double culling_thresh = 0.01;

	for (int i=1; i<argc; i++){
        if (strcmp("-ORBconfig", argv[i])==0 || strcmp("-ORBInitRef", argv[i])==0 ){
            argv[++i];	// skip ORB parameter
        }else if (strcmp("-url", argv[i])==0){
            for(int j = 0; j < 2; j++){
                ModelFname[j] = argv[++i];
            }
		}else if (strcmp("-timeStep", argv[i])==0){
			timeStep = atof(argv[++i]);
        }
	}

    for(int j = 0; j < 2; j++){
        Model[j] = "file://" + ModelFname[j];
        cout << "Model: " << Model[j] << endl;
    }

    //================== Model Load ===============================
    BodyInfo_var floor = loadBodyInfo(Model[0].c_str(), argc, argv);
    if(!floor){
        cerr << "ModelLoader: " << Model[0] << " cannot be loaded" << endl;
        return 1;
    }
    BodyInfo_var body = loadBodyInfo(Model[1].c_str(), argc, argv);
    if(!body){
        cerr << "ModelLoader: " << Model[1] << " cannot be loaded" << endl;
        return 1;
    }
    
    //================== CORBA init ===============================
	// initialize CORBA 
	CORBA::ORB_var orb;
	orb = CORBA::ORB_init(argc, argv);

    // ROOT POA
	CORBA::Object_var poaObj = orb -> resolve_initial_references("RootPOA");
	PortableServer::POA_var rootPOA = PortableServer::POA::_narrow(poaObj);
		
	// get reference to POA manager
	PortableServer::POAManager_var manager = rootPOA -> the_POAManager();

    CosNaming::NamingContext_var cxt;
	{
		CORBA::Object_var	nS = orb->resolve_initial_references("NameService");
        cxt = CosNaming::NamingContext::_narrow(nS);
	}

    //==================== OnlineViewer (GrxUI) setup ===============
    OnlineViewer_var olv = getOnlineViewer(argc, argv);
		
    if (CORBA::is_nil( olv )) {
		std::cerr << "OnlineViewer not found" << std::endl;
		return 1;
    }
    try {
        olv->load(floor->name(), Model[0].c_str());
        olv->load(body->name(), Model[1].c_str());
        olv->clearLog();
    } catch (CORBA::SystemException& ex) {
        cerr << "Failed to connect GrxUI." << endl;
        return 1;
    }

	//================= DynamicsSimulator setup ======================
	DynamicsSimulatorFactory_var dynamicsSimulatorFactory;
	dynamicsSimulatorFactory =
		checkCorbaServer <DynamicsSimulatorFactory, DynamicsSimulatorFactory_var> ("DynamicsSimulatorFactory", cxt);
		
	if (CORBA::is_nil(dynamicsSimulatorFactory)) {
		std::cerr << "DynamicsSimulatorFactory not found" << std::endl;
	}
	DynamicsSimulator_var dynamicsSimulator = dynamicsSimulatorFactory->create();

	cout << "** Dynamics server setup ** " << endl;
    cout << "Character  :" << floor->name() << endl;
	dynamicsSimulator->registerCharacter(floor->name(), floor);			    
	cout << "Character  :" << body->name() << endl;
    dynamicsSimulator->registerCharacter(body->name(), body);

    dynamicsSimulator->init(timeStep, DynamicsSimulator::RUNGE_KUTTA, DynamicsSimulator::ENABLE_SENSOR);
    DblSequence3 g;
    g.length(3);
    g[0] = 0.0;
    g[1] = 0.0;
    g[2] = world_gravity;
    dynamicsSimulator->setGVector(g);

    // initial position and orientation
    Vector3  waist_p;
    Matrix33 waist_R;
    waist_p << 0, 0, 0.7135;
	waist_R = Matrix33::Identity();

	DblSequence trans;
	trans.length(12);
	for(int i=0; i<3; i++) trans[i]   = waist_p(i);
	for(int i=0; i<3; i++){
		for(int j=0; j<3; j++) trans[3+3*i+j] = waist_R(i,j);
	}
	dynamicsSimulator->setCharacterLinkData( body->name(), "WAIST", DynamicsSimulator::ABS_TRANSFORM, trans );
    DblSequence angle;
    angle.length(29);
    angle[0] = 0.0;         angle[1] = -0.0360373;  angle[2] = 0.0;         angle[3] = 0.0785047;
    angle[4] = -0.0424675;  angle[5] = 0.0;         angle[6] = 0.174533;    angle[7] = -0.00349066;
    angle[8] = 0.0;         angle[9] = -1.5708;     angle[10] = 0.0;        angle[11] = 0.0;
    angle[12] = 0.0;        angle[13] = 0.0;        angle[14] = -0.0360373; angle[15] = 0.0;
    angle[16] = 0.0785047;  angle[17] = -0.0424675; angle[18] = 0.0;        angle[19] = 0.174533;
    angle[20] = -0.00349066;angle[21] = 0.0;        angle[22] = -1.5708;    angle[23] = 0.0;
    angle[24] = 0.0;        angle[25] = 0.0;        angle[26] = 0.0;        angle[27] = 0.0;
    angle[28] = 0.0;
    dynamicsSimulator->setCharacterAllLinkData( body->name(), DynamicsSimulator::JOINT_VALUE, angle );
    dynamicsSimulator->calcWorldForwardKinematics();

    DblSequence6 K, C;    // spring-damper parameters are not used now
    K.length(0);
    C.length(0);
    dynamicsSimulator->registerCollisionCheckPair(floor->name(),"", body->name() ,"",
		                                            statFric,slipFric,K,C,culling_thresh);
    dynamicsSimulator->initSimulation();
        
    // ==================  Controller setup ==========================
    Controller_var controller;
    controller = checkCorbaServer <Controller, Controller_var> ("SamplePDController", cxt);

    if (CORBA::is_nil(controller)) {
        std::cerr << "Controller not found" << std::endl;
    }

    controller->setModelName(body->name());
    controller->setDynamicsSimulator(dynamicsSimulator);
    controller->initialize();
    controller->setTimeStep(controlTimeStep);
    controller->start();

    // ==================  log file   ======================
	static ofstream log_file;
	log_file.open("samplePD.log");
		
	// ==================  main loop   ======================
    WorldState_var state;
	int i=0;
    int j = 0;
	double time=0.0;
    double controlTime=0.0;
	while ( 1 ) {
        bool control=false;
        if(controlTime <= time){
            control=true;
            j++;
        }

        if(control)
            controller->input();

        i++;
        time = timeStep * i;
		controlTime = controlTimeStep * j;

        if(control)
            controller->control();
               
        // ================== simulate one step ==============
		dynamicsSimulator->stepSimulation();					    
               
        // ================== viewer update ====================
        try {
		    dynamicsSimulator -> getWorldState( state );
			olv->update( state );
		} catch (CORBA::SystemException& ex) {
		    return 1;
        }
	
        // ===================== get robot status ===================
		DblSequence_var waist_pR;  // position and attitude
		DblSequence_var waist_vw;  // linear and angular velocities
		dynamicsSimulator->getCharacterLinkData(body->name(), "WAIST", DynamicsSimulator::ABS_TRANSFORM, waist_pR);
		dynamicsSimulator->getCharacterLinkData(body->name(), "WAIST", DynamicsSimulator::ABS_VELOCITY,  waist_vw);

		// ================== log data save =====================
		log_file << time << " ";
		log_file << waist_vw[2] << " ";
		log_file << endl;

        if(control)
            controller->output();

		if( time > EndTime ) break;

	}

	log_file.close();
	dynamicsSimulator->destroy();

	return 0;
}

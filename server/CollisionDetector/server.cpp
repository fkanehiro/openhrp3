/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/**
 * CollisionDetectorのサーバスタートアップ //
 *
 * @author  K Saito (Kernel. Co.,Ltd.)
 * @version 0.1 (2002/02/22)
 */
#include "CollisionDetector_impl.h"

#ifdef _WIN32
#include "winbase.h"
#else
#include <unistd.h>
#endif /* _WIN32 */

#include <iostream>

using namespace std;

/**
 * サーバスタートアップ //
 *
 * @param   argc
 * @param   argv
 * @return
 */
int main(int argc, char* argv[]) {
    CORBA::ORB_var orb;
    try {
        orb = CORBA::ORB_init(argc, argv);
        //
        // Resolve Root POA
        //
        CORBA::Object_var poaObj = orb -> resolve_initial_references("RootPOA");
        PortableServer::POA_var rootPOA = PortableServer::POA::_narrow(poaObj);

        //
        // Get a reference to the POA manager
        //
        PortableServer::POAManager_var manager = rootPOA -> the_POAManager();

    // ネームサーバへの参照取得 //
    CORBA_Object_var ns;
    try {
      ns = orb -> resolve_initial_references("NameService");
    } catch (const CORBA_ORB::InvalidName&) {
      cerr << argv[0] << ": can't resolve `NameService'" << endl;
      return 1;
    }
    if(CORBA_is_nil(ns)) {
      cerr << argv[0]
	   << ": `NameService' is a nil object reference"
	   << endl;
      return 1;
    }
    
    // ルートネーミングコンテキスト取得 //
    CosNaming_NamingContext_var rootnc = CosNaming_NamingContext::_narrow(ns);
    if(CORBA_is_nil(rootnc)) {
      cerr << argv[0]
	   << ": `NameService' is not a NamingContext object reference"
	   << endl;
      return 1;
    }

    CORBA_Object_var cdFactory;
    CollisionDetectorFactory_impl* cdFactoryImpl = new CollisionDetectorFactory_impl(orb);
    cdFactory = cdFactoryImpl -> _this();
    CosNaming_Name nc;
    nc.length(1);
    nc[0].id = CORBA_string_dup("CollisionDetectorFactory");
    nc[0].kind = CORBA_string_dup("");
    rootnc -> rebind(nc, cdFactory);
    
    // クライアント側からの接続待ち //
    manager -> activate();
    cout << "ready" << endl;
    
    orb -> run();
    } catch (CORBA_SystemException& ex) {
        cerr << ex._rep_id() << endl;
        orb->destroy();
        return 1;
    }
    orb->destroy();
    return 0;
}

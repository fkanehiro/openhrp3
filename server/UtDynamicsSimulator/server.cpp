/** @file DynamicsSimulator/server/server.cpp
 * controlのサーバスタートアップ
 *
 * @author  Ichitaro Kohara (Kernel. Co.,Ltd.)
 * @version 0.1 (2000/03/22)
 */

#include "DynamicsSimulator_impl.h"

#ifdef _WIN32
#include "winbase.h"
#else
#include <unistd.h>
#endif /* _WIN32 */

#include <iostream>

using namespace std;
using namespace OpenHRP;


/**
 * サーバスタートアップ
 *
 * @param   argc
 * @param   argv
 * @return
 */
int main(int argc, char* argv[])
{
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

    CosNaming::NamingContext_var cxT;
    {
      CORBA::Object_var	nS = orb->resolve_initial_references("NameService");
      cxT = CosNaming::NamingContext::_narrow(nS);
    }

    CORBA::Object_var integratorFactory;
    DynamicsSimulatorFactory_impl* integratorFactoryImpl = new DynamicsSimulatorFactory_impl(orb);
    integratorFactory = integratorFactoryImpl -> _this();
    CosNaming::Name nc;
    nc.length(1);
    nc[0].id = CORBA::string_dup("DynamicsSimulatorFactory");
    nc[0].kind = CORBA::string_dup("");
    cxT -> rebind(nc, integratorFactory);

    // クライアント側からの接続待ち
    manager -> activate();
    cout << "ready" << endl;

    orb -> run();
  } catch (CORBA::SystemException& ex) {
    cerr << ex._rep_id() << endl;
    return 1;
  }
  orb->destroy();
  return 0;
}

#include <iostream>
#include <hrpModel/ModelLoaderUtil.h>
#include "VrmlWriter.h"

using namespace std;
using namespace hrp;
using namespace OpenHRP;

int main(int argc, char* argv[])
{
    if (argc < 2){
        cerr << "Usage:" << argv[0] << "URL of the original file" 
             << std::endl;
        return 1;
    }

    CORBA::ORB_var orb;
  
    try {
        orb = CORBA::ORB_init(argc, argv);
        ModelLoader_var ml = getModelLoader(orb);
        BodyInfo_var binfo;
        binfo = ml->getBodyInfo(argv[1]);

        VrmlWriter writer;
        writer.write(binfo, cout);
    }catch(ModelLoader::ModelLoaderException ex){
        std::cerr << ex.description << std::endl;
        return 1;
    }catch (CORBA::SystemException& ex){ 
        cerr << ex._rep_id() << endl;
    }
    return 0;
}

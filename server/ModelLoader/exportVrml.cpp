#include <iostream>
#include <hrpModel/ModelLoaderUtil.h>
#include "VrmlWriter.h"

using namespace std;
using namespace hrp;
using namespace OpenHRP;

int main(int argc, char* argv[])
{
    if (argc < 2){
        cerr << "Usage:" << argv[0] << " <URL of the original file> [--use-inline-shape]"
             << std::endl;
        return 1;
    }

    bool use_inline = false;
    if (argc > 2) {
        for (int i = 2; i < argc; i++) {
            std::string arg (argv[i]);
            if (arg == "--use-inline-shape") {
                use_inline = true;
            }
        }
    }
    CORBA::ORB_var orb;
  
    try {
        orb = CORBA::ORB_init(argc, argv);
        ModelLoader_var ml = getModelLoader(orb);
        BodyInfo_var binfo;
        binfo = ml->getBodyInfo(argv[1]);

        VrmlWriter writer;
        writer.useInlineShape(use_inline);
        writer.write(binfo, cout);
    }catch(ModelLoader::ModelLoaderException ex){
        std::cerr << ex.description << std::endl;
        return 1;
    }catch (CORBA::SystemException& ex){ 
        cerr << ex._rep_id() << endl;
    }
    return 0;
}

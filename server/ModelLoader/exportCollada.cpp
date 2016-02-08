//
// g++ -I. -I../../hrplib -I/usr/include/colladadom
// -I/usr/include/colladadom/1.5 exportCollada.cpp BodyInfo_impl.cpp
// ShapeSetInfo_impl.cpp VrmlUtil.cpp
// ../../hrplib/hrpCorba/ModelLoaderSk.cpp -L../../lib -lhrpModel-3.1
// -lhrpUtil-3.1 -lomniORB4 -lomniDynamic4 -lcollada15dom
// -lboost_system
//
#include <list>
#include <boost/foreach.hpp>
#include <boost/tokenizer.hpp>

#include "ModelLoader_impl.h"

#ifdef _WIN32
#include "winbase.h"
#else
#include <unistd.h>
#endif /* _WIN32 */

#include <iostream>

#include "ColladaWriter.h"
#include "BodyInfo_impl.h"

using namespace std;

int main(int argc, char* argv[])
{
  CORBA::ORB_var orb = CORBA::ORB::_nil();
  
  try 
    {
      orb = CORBA::ORB_init(argc, argv);
      
      CORBA::Object_var obj;
      
      obj = orb->resolve_initial_references("RootPOA");
      PortableServer::POA_var poa = PortableServer::POA::_narrow(obj);
      if(CORBA::is_nil(poa))
	{
	  throw string("error: failed to narrow root POA.");
	}
      
      PortableServer::POAManager_var poaManager = poa->the_POAManager();
      if(CORBA::is_nil(poaManager))
	{
	  throw string("error: failed to narrow root POA manager.");
	}
      
      std::string inFileName, outFileName;
      
      std::list<ManipulatorInfo> listmanipulators;
      
      // get filename
      for(int i=0; i<(argc-1); ++i) 
	{
	  if((strlen(argv[i]) > 1) && (argv[i][0] == '-')) 
	    {
	      
	      if(argv[i][1] == 'i')
		inFileName = std::string(argv[++i]);
	      
	      else if(argv[i][1] == 'o')
		outFileName = std::string(argv[++i]);
	      
	      else if(argv[i][1] == 'a') 
		{
		  std::string armProfStr = std::string(argv[++i]);
		  std::vector<std::string> armVec;
		  boost::char_separator<char> sep1(",");
		  boost::tokenizer< boost::char_separator<char> > armTolkens(armProfStr, sep1);
#if 0
		  BOOST_FOREACH(std::string t, armTolkens)
		    {
		      //armVec.push_back(t);
		    }
#else
		  ManipulatorInfo tmpinfo;
		  boost::tokenizer< boost::char_separator<char> >::iterator
		    armiter(armTolkens.begin());
		  tmpinfo.name = *(armiter++);
		  std::cout << "tmpinfo.name         = " << tmpinfo.name << "\n";
		  tmpinfo.basename = *(armiter++);
		  std::cout << "tmpinfo.basename     = " << tmpinfo.basename << "\n";
		  tmpinfo.effectorname = *(armiter++);
		  std::cout << "tmpinfo.effectorname = " << tmpinfo.effectorname << std::endl;

		  tmpinfo.translation[0] = boost::lexical_cast<double>(*(armiter++));
		  tmpinfo.translation[1] = boost::lexical_cast<double>(*(armiter++));
		  tmpinfo.translation[2] = boost::lexical_cast<double>(*(armiter++));
		  tmpinfo.rotation[0] = boost::lexical_cast<double>(*(armiter++));
		  tmpinfo.rotation[1] = boost::lexical_cast<double>(*(armiter++));
		  tmpinfo.rotation[2] = boost::lexical_cast<double>(*(armiter++));
		  tmpinfo.rotation[3] = boost::lexical_cast<double>(*(armiter++));
		  std::cout << "tmpinfo.translation = " << tmpinfo.translation[0] << ", " << tmpinfo.translation[1] << ", " << tmpinfo.translation[2] << std::endl;
		  std::cout << "tmpinfo.rotation = " << tmpinfo.rotation[0] << ", " << tmpinfo.rotation[1] << ", " << tmpinfo.rotation[2] << ", " << tmpinfo.rotation[3] << std::endl;

		  for(int i=0; armiter != armTolkens.end(); ++i,++armiter)
		    {
		      if(i % 2) 
			{
			  tmpinfo.gripperdir.push_back(*armiter);
			  std::cout << "tmpinfo.gripperdir = " << *armiter;
			}
		      else 
			{
			  tmpinfo.grippernames.push_back(*armiter);
			  std::cout << "tmpinfo.grippernames = " << *armiter;
			}
		      std::cout << std::endl;
		    }
		  listmanipulators.push_back(tmpinfo);
#endif
		}
	    }
	}
      
#if 1
      if((inFileName != "") && (outFileName != "")) 
	{
	  BodyInfo_impl bI(poa);	
	  
	  bI.loadModelFile(inFileName.c_str());
	  
	  std::stringstream sstm;
	  for (int i = 0; i < argc; i++)sstm << argv[i] << " ";

	  ColladaWriter cW(listmanipulators, sstm.str().c_str());
	  cW.Write(&bI);
	  cW.Save(outFileName);
	  std::cout << argv[0] << " " << inFileName << " was successfully exported to " << outFileName << std::endl;
	} 
      else
	{
	  std::cerr << "Usage: " << argv[0] 
		    << "-i <inFileName> " << "-o <outFileName> "
		    << "-a <manipulator_name>,<frame_origin>,<frame_tip>,<px>,<py>,<pz>,<rotX>,<rotY>,<rotZ>,<rotTheta[rad]>,<gripper_joint>,<closing_direction>...\n see http://openrave.org/docs/latest_stable/collada_robot_extensions/"
		    << std::endl;
	}
#endif    
    }
  catch (CORBA::SystemException& ex) 
    {
      cerr << ex._rep_id() << endl;
    }
  catch (const string& error)
    {
      cerr << error << endl;
    }
  
  try 
    {
      orb->destroy();
    }
  catch(...)
    {
      
    }

  return 0;
}

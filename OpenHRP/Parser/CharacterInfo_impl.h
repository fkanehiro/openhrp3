/*! @file
  @author S.NAKAOKA
*/

#ifndef CHARACTERINFO_IMPL_H_INCLUDED
#define CHARACTERINFO_IMPL_H_INCLUDED


#include <string>
#include <ORBwrap.h>

#include "ModelLoader.h"
#include "VrmlNodes.h"
#include "ModelNodeSet.h"
#include "LinkInfo_impl.h"


namespace OpenHRP {

    class CharacterInfo_impl : public POA_OpenHRP::CharacterInfo
	{
		PortableServer::POA_var poa;
		std::vector<LinkInfo_impl* > linkInfoServants;
		
		std::string name_;
		std::string url_;
		StringSequence info_;
		LinkInfoSequence links_;
		
		int readJointNodeSet
		(JointNodeSetPtr jointNodeSet, int& currentIndex, int motherIndex);

		void putMessage(const std::string& message);
		
	public:
		
		CharacterInfo_impl(PortableServer::POA_ptr poa);
		~CharacterInfo_impl();
		void loadModelFile(const std::string& filename);
		
		virtual PortableServer::POA_ptr _default_POA();
		
		virtual char* name();
		virtual char* url();
		virtual StringSequence* info();
		virtual LinkInfoSequence* links();
	};

};


#endif

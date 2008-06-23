/*! @file
  @author S.NAKAOKA
*/

#include "CharacterInfo_impl.h"

#include <iostream>
#include <boost/bind.hpp>


using namespace std;
using namespace boost;
using namespace OpenHRP;


CharacterInfo_impl::CharacterInfo_impl(PortableServer::POA_ptr poa) :
    poa(PortableServer::POA::_duplicate(poa))
{

}


void CharacterInfo_impl::loadModelFile(const std::string& url)
{
    string filename(url);

    static const string fileProtocolHeader1("file:///");
	static const string fileProtocolHeader2("file://");
	static const string fileProtocolHeader3("file:");

    size_t pos = filename.find(fileProtocolHeader1);
    if(pos == 0)
	{
		filename.erase(0, fileProtocolHeader1.size());
    }
	else
	{
	    size_t pos = filename.find(fileProtocolHeader2);
    	if(pos == 0){
			filename.erase(0, fileProtocolHeader2.size());
    	}
		else
		{
			size_t pos = filename.find(fileProtocolHeader3);
    		if(pos == 0){
				filename.erase(0, fileProtocolHeader3.size());
    		}
		}
    }

    ModelNodeSet modelNodeSet;

    modelNodeSet.signalOnStatusMessage.connect(bind(&CharacterInfo_impl::putMessage, this, _1));

    try {
    	modelNodeSet.loadModelFile(filename);
	cout.flush();
    }
    catch(ModelNodeSet::Exception& ex){
	throw ModelLoader::ModelLoaderException(0, ex.message.c_str());
    }

    const string& humanoidName = modelNodeSet.humanoidNode()->defName;
    name_ = CORBA::string_dup(humanoidName.c_str());
    url_ = CORBA::string_dup(url.c_str());

    int numJointNodes = modelNodeSet.numJointNodes();

    links_.length(numJointNodes);
    linkInfoServants.resize(numJointNodes, 0);

    if(numJointNodes > 0){

	int currentIndex = 0;
	JointNodeSetPtr rootJointNodeSet = modelNodeSet.rootJointNodeSet();
	readJointNodeSet(rootJointNodeSet, currentIndex, -1);

	for(size_t i=0; i < linkInfoServants.size(); ++i){
	    LinkInfo_impl* linkInfo = linkInfoServants[i];
	    linkInfo->activate();
	    links_[i] = linkInfo->_this();
	}
    }
}


CharacterInfo_impl::~CharacterInfo_impl()
{
    for(size_t i=0; i < linkInfoServants.size(); ++i){
	LinkInfo_impl* linkInfo = linkInfoServants[i];
	if(linkInfo){
	    linkInfo->deactivate();
	    linkInfo->_remove_ref();
	}
    }
}


PortableServer::POA_ptr CharacterInfo_impl::_default_POA()
{
    return PortableServer::POA::_duplicate(poa);
}


void CharacterInfo_impl::putMessage(const std::string& message)
{
  cout << message;
}


int CharacterInfo_impl::readJointNodeSet
(JointNodeSetPtr jointNodeSet, int& currentIndex, int mother)
{
    int index = currentIndex++;

    LinkInfo_impl* linkInfo = new LinkInfo_impl(poa);
    linkInfoServants[index] = linkInfo;

    linkInfo->mother_ = mother;
    linkInfo->sister_ = -1;
    linkInfo->daughter_ = -1;

    LinkInfo_impl* prevDaugherInfo = 0;
    int numChildren = jointNodeSet->childJointNodeSets.size();
    for(int i=0; i < numChildren; ++i){
	JointNodeSetPtr childJointNodeSet = jointNodeSet->childJointNodeSets[i];

	int daugherIndex = readJointNodeSet(childJointNodeSet, currentIndex, index);

	if(linkInfo->daughter_ == -1){
	    linkInfo->daughter_ = daugherIndex;
	}

	if(prevDaugherInfo){
	    prevDaugherInfo->sister_ = daugherIndex;
	}
	prevDaugherInfo = linkInfoServants[daugherIndex];
    }


    try {
	linkInfo->setJointParameters(jointNodeSet->jointNode);
	linkInfo->setSegmentParameters(jointNodeSet->segmentNode);
	linkInfo->setSensors(jointNodeSet);
    }
    catch(ModelLoader::ModelLoaderException& ex){
	string error = linkInfo->name_.empty() ? "Unnamed JoitNode" : linkInfo->name_;
	error += ": ";
	error += ex.description;
	throw ModelLoader::ModelLoaderException(0, error.c_str());
    }

    return index;
}


char* CharacterInfo_impl::name()
{
    return CORBA::string_dup(name_.c_str());
}

char* CharacterInfo_impl::url()
{
    return CORBA::string_dup(url_.c_str());
}

StringSequence* CharacterInfo_impl::info()
{
    return new StringSequence(info_);
}

LinkInfoSequence* CharacterInfo_impl::links()
{
    return new LinkInfoSequence(links_);
}





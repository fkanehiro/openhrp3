// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-

/*! @file
  @author S.NAKAOKA
*/

#ifndef OPENHRP_MODEL_NODE_SET_H_INCLUDED
#define OPENHRP_MODEL_NODE_SET_H_INCLUDED

#include "ModelParserConfig.h"

#include <vector>
#include <boost/shared_ptr.hpp>
#include <boost/signals.hpp>

#include "VrmlNodes.h"


namespace OpenHRP {

    class VRMLParser;

	class JointNodeSet;
	typedef boost::shared_ptr<JointNodeSet> JointNodeSetPtr;

    class JointNodeSet
    {
    public:
		VrmlProtoInstancePtr jointNode;
		VrmlProtoInstancePtr segmentNode;
		std::vector<JointNodeSetPtr> childJointNodeSets;
		std::vector<VrmlProtoInstancePtr> sensorNodes;
    };
    
    typedef std::vector<JointNodeSetPtr> JointNodeSetArray;
    

    class MODELPARSER_EXPORT  ModelNodeSet
    {
		int numJointNodes_;
		VrmlProtoInstancePtr humanoidNode_;
		JointNodeSetPtr rootJointNodeSet_;
		int messageIndent_;
		bool flgMessageOutput_;

		VrmlProtoPtr protoToCheck;

		void extractHumanoidNode(VRMLParser& parser);

		void throwExceptionOfIllegalField(const std::string& name, VrmlFieldTypeId typeId);
		void requireField(const std::string& name, VrmlFieldTypeId type);
		void checkFieldType(const std::string& name, VrmlFieldTypeId type);
		VrmlVariantField* addField(const std::string& name, VrmlFieldTypeId type);
		void addFloatField(const std::string& name, double defaultValue);
		
		void checkHumanoidProto();
		void checkJointProto();
		void checkSegmentProto();
		void checkSensorProtoCommon();
		void checkHardwareComponentProto();
		void extractJointNodes();
		JointNodeSetPtr addJointNodeSet(VrmlProtoInstancePtr jointNode);
		void extractChildNodes(JointNodeSetPtr jointNodeSet, MFNode& childNodes);

		void putMessage(const std::string& message);
		
    public:

		struct Exception {
			Exception(const std::string& message) : message(message) { }
			std::string message;
		};
		
		ModelNodeSet();

		bool loadModelFile(const std::string& filename);
		
		int numJointNodes() { return numJointNodes_; }
		VrmlProtoInstancePtr humanoidNode() { return humanoidNode_; }
		JointNodeSetPtr rootJointNodeSet() { return rootJointNodeSet_; }

		/**
		   @if jp
		   読み込み進行状況のメッセージを出力するためのシグナル.
		   @note エラー発生時のメッセージはこのシグナルではなく例外によって処理される。
		   @endif
		*/
		boost::signal<void(const std::string& message)> signalOnStatusMessage;

		bool setMessageOutput( bool val ) { return( flgMessageOutput_ = val ); }
    };

	typedef boost::shared_ptr<ModelNodeSet> ModelNodeSetPtr;
};
    


#endif

/*!
  @file BodyInfo_impl.h
  @author S.NAKAOKA
*/

#ifndef BODYINFO_IMPL_H_INCLUDED
#define BODYINFO_IMPL_H_INCLUDED

#include <string>
#include <vector>
#include <ORBwrap.h>

#include "ModelLoader.h"
#include "ModelNodeSet.h"
#include "UniformedShape.h"
#include "VrmlNodes.h"
#include "VrmlFieldCopyUtil.h"

using namespace std;

namespace OpenHRP
{
	class BodyInfo_impl : public POA_OpenHRP::BodyInfo
	{
		PortableServer::POA_var poa;
		
		time_t lastUpdate_;

		std::string name_;
		std::string url_;
		StringSequence info_;
		LinkInfoSequence links_;

		ShapeInfoSequence  shapes_;
		AppearanceInfoSequence appearances_;
		MaterialInfoSequence materials_;
		TextureInfoSequence textures_;
		AllLinkShapeIndices linkShapeIndices_;

		int readJointNodeSet
		(JointNodeSetPtr jointNodeSet, int& currentIndex, int motherIndex);

		void putMessage(const std::string& message);

		void setJointParameters(
			int linkInfoIndex, VrmlProtoInstancePtr jointNode );
		void setSegmentParameters(
			int linkInfoIndex, VrmlProtoInstancePtr segmentNode );
		void setSensors(
			int linkInfoIndex, JointNodeSetPtr jointNodeSet );
		void readSensorNode( int linkInfoIndex, SensorInfo& sensorInfo, VrmlProtoInstancePtr sensorNode );

		void traverseShapeNodes( int index, MFNode& childNodes, matrix44d mTransform );

		bool _calcTransform( VrmlTransformPtr transform, matrix44d& mTransform );

		void _setVertices( ShapeInfo_var&,vector<vector3d>,	matrix44d );
		void _setTriangles(	ShapeInfo_var&, vector<vector3i> );
		void _setNormals( AppearanceInfo_var&, vector<vector3d>, vector<vector3i>, matrix44d );
		void _setShapeInfoType( ShapeInfo_var&, UniformedShape::ShapePrimitiveType );

		long _createMaterialInfo( VrmlMaterialPtr );
		long _createTextureInfo( VrmlTexturePtr ); 

		string _getModelFileDirPath();

	public:
		
		BodyInfo_impl(PortableServer::POA_ptr poa);
		~BodyInfo_impl();

		void loadModelFile(const std::string& filename);

		static string deleteURLScheme( string url );
		string& replace(string& str, const string sb, const string sa);

		void setLastUpdateTime( time_t time ) { lastUpdate_ = time; };
		time_t getLastUpdateTime() { return lastUpdate_; }

		virtual PortableServer::POA_ptr _default_POA();
		
		virtual char* name();
		virtual char* url();
		virtual StringSequence* info();
		virtual LinkInfoSequence* links();

	    virtual AllLinkShapeIndices* linkShapeIndices();
		virtual ShapeInfoSequence* shapes();
		virtual AppearanceInfoSequence* appearances();
		virtual MaterialInfoSequence* materials();
		virtual TextureInfoSequence* textures();

	};

};


#endif

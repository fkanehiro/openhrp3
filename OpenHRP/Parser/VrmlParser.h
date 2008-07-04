/*! @file
  @brief Header file of VRML97 Parser class
  @author S.NAKAOKA
*/

#ifndef VRML_PARSER_H_INCLUDED
#define VRML_PARSER_H_INCLUDED

#include "ModelParserConfig.h"

#include <vector>
#include <string>
#include <boost/shared_ptr.hpp>

#include "VrmlNodes.h"


class EasyScanner;
typedef boost::shared_ptr<EasyScanner> EasyScannerPtr;


namespace OpenHRP {

    /**
       \brief Parser for VRML97 format

       The VrmlParser class reads a VRML97 file and extract its nodes.
    */
    class MODELPARSER_EXPORT  VRMLParser
    {
    public:

	/**
	   Constructor. This version of constructor do 'load' mehtod 
	   after constructing the object.

	   \param filename file name of a target VRML97 file.
	*/
	VRMLParser(const std::string& filename);

	VRMLParser();

	~VRMLParser();

      void setProtoInstanceActualNodeExtractionMode(bool isOn);

	void load(const std::string& filename);

	/**
	   This method returns the top node of the next node tree written in the file.
	*/
	VrmlNodePtr readNode();

	int eliminateUnusedVertices(VrmlNodePtr node);
  
    private:

	struct TSourceInfo{
	    EasyScannerPtr scanner;
	    MFString inlineUrls;
	    VrmlNodeCategory inlineNodeCategory;
	};
	typedef boost::shared_ptr<TSourceInfo> TSourceInfoPtr;

	std::vector<TSourceInfoPtr> sources;
	EasyScannerPtr scanner; // for the current source
	VrmlProtoInstancePtr currentProtoInstance;

      bool protoInstanceActualNodeExtractionMode;

      typedef std::map<VrmlProto*, EasyScannerPtr> ProtoToEntityScannerMap;
      ProtoToEntityScannerMap protoToEntityScannerMap;

	typedef std::map<std::string, VrmlNodePtr> TDefNodeMap;
	typedef std::pair<std::string, VrmlNodePtr> TDefNodePair;
	typedef std::map<std::string, VrmlProtoPtr> TProtoMap;
	typedef std::pair<std::string, VrmlProtoPtr> TProtoPair;

	TProtoMap protoMap;
	TDefNodeMap defNodeMap;

	void init();
	void setSymbols();
	VrmlNodePtr readSpecificNode(VrmlNodeCategory nodeCategory, int symbol);
	VrmlNodePtr readInlineNode(VrmlNodeCategory nodeCategory);
	void newInlineSource(std::string filename);
	VrmlProtoPtr defineProto();
  
	VrmlNodePtr readNode(VrmlNodeCategory nodeCategory);
	VrmlProtoInstancePtr readProtoInstanceNode(const std::string& proto_name, VrmlNodeCategory nodeCategory);
	VrmlNodePtr evalProtoInstance(VrmlProtoInstancePtr proto, VrmlNodeCategory nodeCategory);
	VrmlUnsupportedNodePtr readUnsupportedNode(const std::string& nodeTypeName);
    VrmlUnsupportedNodePtr readScriptNode();	// #####
	VrmlUnsupportedNodePtr readExternProto();	// #####

	VrmlViewpointPtr readViewpointNode();
	VrmlNavigationInfoPtr readNavigationInfoNode();
	VrmlBackgroundPtr readBackgroundNode();
	VrmlGroupPtr readGroupNode();
	VrmlTransformPtr readTransformNode();
	VrmlShapePtr readShapeNode();
	VrmlCylinderSensorPtr readCylinderSensorNode();
	VrmlBoxPtr readBoxNode();
	VrmlConePtr readConeNode();
	VrmlCylinderPtr readCylinderNode();
// #####
	VrmlPointSetPtr			readPointSetNode();
	VrmlPixelTexturePtr		readPixelTextureNode();
	VrmlMovieTexturePtr		readMovieTextureNode();
	VrmlElevationGridPtr	readElevationGridNode();
	VrmlExtrusionPtr		readExtrusionNode();
	VrmlSwitchPtr			readSwitchNode();
	VrmlLODPtr				readLODNode();
	VrmlCollisionPtr		readCollisionNode();
	VrmlAnchorPtr			readAnchorNode();
	VrmlFogPtr				readFogNode();
	VrmlBillboardPtr		readBillboardNode();
	VrmlWorldInfoPtr		readWorldInfoNode();
	VrmlPointLightPtr		readPointLightNode();
	VrmlDirectionalLightPtr	readDirectionalLightNode();
	VrmlSpotLightPtr		readSpotLightNode();
// #####
	VrmlSpherePtr readSphereNode();
	VrmlTextPtr readTextNode();
	VrmlFontStylePtr readFontStyleNode();
	VrmlIndexedLineSetPtr readIndexedLineSetNode();
	VrmlIndexedFaceSetPtr readIndexedFaceSetNode();
	void checkIndexedFaceSet(VrmlIndexedFaceSetPtr node);
	VrmlCoordinatePtr readCoordNode();
	VrmlTextureCoordinatePtr readTextureCoordinateNode();
	VrmlColorPtr readColorNode();
	VrmlAppearancePtr readAppearanceNode();
	VrmlMaterialPtr readMaterialNode();
	VrmlImageTexturePtr readImageTextureNode();
	VrmlTextureTransformPtr readTextureTransformNode();
	VrmlNormalPtr readNormalNode();
  
	VrmlVariantField& readProtoField(VrmlFieldTypeId fieldTypeId);
  
	void readSFInt32(SFInt32& out_value);
	void readSFFloat(SFFloat& out_value);
	void readSFString(SFString& out_value);
	void readMFInt32(MFInt32& out_value);
	void readMFFloat(MFFloat& out_value);
	void readSFColor(SFColor& out_value); 
	void readMFColor(MFColor& out_value); 
	void readMFString(MFString& out_value);
	void readSFVec2f(SFVec2f& out_value);
	void readMFVec2f(MFVec2f& out_value);
	void readSFVec3f(SFVec3f& out_value);
	void readMFVec3f(MFVec3f& out_value);
	void readSFRotation(SFRotation& out_value);
	void readMFRotation(MFRotation& out_value);
	void readSFBool(SFBool& out_value);
	void readSFTime(SFTime& out_value);
	void readMFTime(MFTime& out_value);
	void readSFNode(SFNode& out_node, VrmlNodeCategory nodeCategory);
	SFNode readSFNode(VrmlNodeCategory nodeCategory);
	void readMFNode(MFNode& out_nodes, VrmlNodeCategory nodeCategory);
	void readSFImage( SFImage& out_image );		// #####

	void eliminateUnusedVerticesIter(VrmlNodePtr node, int& numEliminated);
	int eliminateFaceSetUnusedVertices(VrmlIndexedFaceSetPtr faceset);

    };

    typedef boost::shared_ptr<VRMLParser> VRMLParserPtr;
};


#endif

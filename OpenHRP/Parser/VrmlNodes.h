/*! @file
  @brief Header file of VRML node classes
  @author S.Nakaoka
*/

#ifndef VRMLNODES_H_INCLUDED
#define VRMLNODES_H_INCLUDED

#include "ModelParserConfig.h"

#include <vector>
#include <string>
#include <map>
#include <bitset>
#include <typeinfo>
#include <boost/intrusive_ptr.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/array.hpp>


namespace OpenHRP {

    enum VrmlFieldTypeId {
      UNDETERMINED_FIELD_TYPE = 0,
      SFBOOL,
      SFINT32, MFINT32,
      SFFLOAT, MFFLOAT,
      SFVEC2F, MFVEC2F,
      SFVEC3F, MFVEC3F,
      SFROTATION, MFROTATION,
      SFTIME, MFTIME,
      SFCOLOR, MFCOLOR,
      SFSTRING, MFSTRING,
      SFNODE, MFNODE,
	  SFIMAGE,				// #####
    };

    typedef bool        SFBool;
    typedef int         SFInt32;
    typedef double      SFFloat;
    typedef SFFloat     SFTime;
    typedef std::string SFString;

    typedef boost::array<SFFloat, 2> SFVec2f;
    typedef boost::array<SFFloat, 3> SFVec3f;
    typedef boost::array<SFFloat, 4> SFVec4f;

    typedef SFVec3f SFColor;
    typedef SFVec4f SFRotation;

	typedef struct {
		int				width;
		int				height;
		int				numComponents;
		std::vector<unsigned char> pixels;
	} SFImage;


    typedef std::vector<SFInt32>    MFInt32;
    typedef std::vector<SFFloat>    MFFloat;
    typedef std::vector<SFVec2f>    MFVec2f;
    typedef std::vector<SFVec3f>    MFVec3f;
    typedef std::vector<SFVec4f>    MFVec4f;
    typedef std::vector<SFRotation> MFRotation;
    typedef std::vector<SFTime>     MFTime;
    typedef std::vector<SFColor>    MFColor;
    typedef std::vector<SFString>   MFString;


    // see 4.6.3 - 4.6.10 of the VRML97 specification
    enum VrmlNodeCategory {

      ANY_NODE = -1,

      PROTO_DEF_NODE = 0,
      PROTO_INSTANCE_NODE,

      TOP_NODE,
      BINDABLE_NODE,
      GROUPING_NODE,
      CHILD_NODE,

      APPEARANCE_NODE,
      MATERIAL_NODE,
      TEXTURE_NODE,
      TEXTURE_TRANSFORM_NODE,

      SHAPE_NODE,
      GEOMETRY_NODE,
      COORDINATE_NODE,
      COLOR_NODE,
      NORMAL_NODE,
      TEXTURE_COORDINATE_NODE,

      FONT_STYLE_NODE,

      SENSOR_NODE,

      NUM_VRML_NODE_CATEGORIES
    };

    class VrmlNode;

    inline void intrusive_ptr_add_ref(VrmlNode* obj);
    inline void intrusive_ptr_release(VrmlNode* obj);

    //! Abstract base class of all vrml nodes.
     class MODELPARSER_EXPORT VrmlNode
    {
    public:

	VrmlNode();
	virtual ~VrmlNode();

	static const char* getLabelOfFieldType(int type);
	
	std::string defName;

	bool isCategoryOf(VrmlNodeCategory category);

    protected:
	std::bitset<NUM_VRML_NODE_CATEGORIES> categorySet;

    private:
	int refCounter;

	friend void intrusive_ptr_add_ref(VrmlNode* obj);
	friend void intrusive_ptr_release(VrmlNode* obj);
    };

    inline void intrusive_ptr_add_ref(VrmlNode* obj){
	obj->refCounter++;
    }
    
    inline void intrusive_ptr_release(VrmlNode* obj){
	obj->refCounter--;
	if(obj->refCounter <= 0){
	    delete obj;
	}
    }

    typedef boost::intrusive_ptr<VrmlNode> VrmlNodePtr;

    typedef VrmlNodePtr SFNode;
    typedef std::vector<SFNode> MFNode;


    class MODELPARSER_EXPORT  VrmlUnsupportedNode : public VrmlNode
    {
    public:
      VrmlUnsupportedNode(const std::string& nodeTypeName);
      std::string nodeTypeName;
    };
    typedef boost::intrusive_ptr<VrmlUnsupportedNode> VrmlUnsupportedNodePtr;


    //! VRML Viewpoint node
    class MODELPARSER_EXPORT  VrmlViewpoint : public VrmlNode
    {
    public:
	VrmlViewpoint();

	SFFloat fieldOfView;
	SFBool jump;
	SFRotation orientation;
	SFVec3f position;
	SFString description;
    };
    typedef boost::intrusive_ptr<VrmlViewpoint> VrmlViewpointPtr;


    //! VRML NavigationInfo node
    class MODELPARSER_EXPORT  VrmlNavigationInfo : public VrmlNode
    {
    public:
	VrmlNavigationInfo();

	MFFloat avatarSize;
	SFBool headlight;
	SFFloat speed;
	MFString type;
	SFFloat visibilityLimit;
    };
    typedef boost::intrusive_ptr<VrmlNavigationInfo> VrmlNavigationInfoPtr;


    //! VRML Background node
    class MODELPARSER_EXPORT  VrmlBackground : public VrmlNode
    {
    public:
      VrmlBackground();
      
      MFFloat groundAngle;
      MFColor groundColor;
      MFFloat skyAngle;
      MFColor skyColor;
      MFString backUrl;
      MFString bottomUrl;
      MFString frontUrl;
      MFString leftUrl;
      MFString rightUrl;
      MFString topUrl;
    };
    typedef boost::intrusive_ptr<VrmlBackground> VrmlBackgroundPtr;


    //! VRML Group node
    class MODELPARSER_EXPORT  VrmlGroup : public VrmlNode
    {
    public:
	VrmlGroup();

	SFVec3f bboxCenter;    
	SFVec3f bboxSize;  
	MFNode children;
    };
    typedef boost::intrusive_ptr<VrmlGroup> VrmlGroupPtr;


    //! VRML Transform node
    class MODELPARSER_EXPORT  VrmlTransform : public VrmlGroup
    {
    public:
	VrmlTransform();

	SFVec3f center;
	SFRotation rotation;
	SFVec3f scale;
	SFRotation scaleOrientation;
	SFVec3f translation;
    };
    typedef boost::intrusive_ptr<VrmlTransform> VrmlTransformPtr;


    class VrmlAppearance;
    typedef boost::intrusive_ptr<VrmlAppearance> VrmlAppearancePtr;

    class VrmlGeometry;
    typedef boost::intrusive_ptr<VrmlGeometry> VrmlGeometryPtr;


    //! VRML Shape node
    class MODELPARSER_EXPORT  VrmlShape : public VrmlNode
    {
    public:
      VrmlShape();
      VrmlAppearancePtr appearance;
      VrmlGeometryPtr geometry;
    };
    typedef boost::intrusive_ptr<VrmlShape> VrmlShapePtr;


    class VrmlMaterial;
    typedef boost::intrusive_ptr<VrmlMaterial> VrmlMaterialPtr;

    class VrmlTexture;
    typedef boost::intrusive_ptr<VrmlTexture> VrmlTexturePtr;

    class VrmlTextureTransform;
    typedef boost::intrusive_ptr<VrmlTextureTransform> VrmlTextureTransformPtr;


    //! VRML Appearance node
    class MODELPARSER_EXPORT  VrmlAppearance : public VrmlNode
    {
    public:
      VrmlAppearance();
      
      VrmlMaterialPtr material;
      VrmlTexturePtr texture;
      VrmlTextureTransformPtr textureTransform;
    };


    //! VRML Material node
    class MODELPARSER_EXPORT  VrmlMaterial : public VrmlNode
    {
    public:
	VrmlMaterial();

	SFFloat ambientIntensity;
	SFColor diffuseColor;
	SFColor emissiveColor;
	SFFloat shininess;
	SFColor specularColor;
	SFFloat transparency;
    };


    //! Base class of VRML Texture nodes
    class MODELPARSER_EXPORT  VrmlTexture : public VrmlNode
    {
    public:
      VrmlTexture();
    };

    
    //! VRML ImageTexture node
    class MODELPARSER_EXPORT  VrmlImageTexture : public VrmlTexture
    {
    public:
	VrmlImageTexture();

	MFString url;
	SFBool   repeatS;
	SFBool   repeatT;
    };
    typedef boost::intrusive_ptr<VrmlImageTexture> VrmlImageTexturePtr;


    //! VRML TextureTransform node
    class MODELPARSER_EXPORT  VrmlTextureTransform : public VrmlNode
    {
    public:
	VrmlTextureTransform();

	SFVec2f center;
	SFFloat rotation;
	SFVec2f scale;
	SFVec2f translation;
    };

    //! Base class of VRML geometry nodes
    class MODELPARSER_EXPORT  VrmlGeometry : public VrmlNode
    {
    public:
      VrmlGeometry();
    };

    //! VRML Box node
    class MODELPARSER_EXPORT  VrmlBox : public VrmlGeometry
    {
    public:
	VrmlBox();
	SFVec3f size;
    };
    typedef boost::intrusive_ptr<VrmlBox> VrmlBoxPtr;


    //! VRML Cone node
    class MODELPARSER_EXPORT  VrmlCone : public VrmlGeometry
    {
    public:
	VrmlCone();

	SFBool bottom;
	SFFloat bottomRadius;
	SFFloat height;
	SFBool side;
    };
    typedef boost::intrusive_ptr<VrmlCone> VrmlConePtr;


    //! VRML Cylinder node
    class MODELPARSER_EXPORT  VrmlCylinder : public VrmlGeometry
    {
    public:
	VrmlCylinder();

	SFBool bottom;
	SFFloat height;
	SFFloat radius;
	SFBool side;
	SFBool top;
    };
    typedef boost::intrusive_ptr<VrmlCylinder> VrmlCylinderPtr;


    //! VRML Sphere node
    class MODELPARSER_EXPORT  VrmlSphere : public VrmlGeometry
    {
    public:
	VrmlSphere();
	SFFloat radius;
    };
    typedef boost::intrusive_ptr<VrmlSphere> VrmlSpherePtr;


    //! VRML FontStyle node
    class MODELPARSER_EXPORT  VrmlFontStyle : public VrmlNode
    {
    public:
	VrmlFontStyle();

	MFString family;       
	SFBool   horizontal;
	MFString justify;
	SFString language;
	SFBool   leftToRight;
	SFFloat  size;
	SFFloat  spacing;
	SFString style;
	SFBool   topToBottom;
    };
    typedef boost::intrusive_ptr<VrmlFontStyle> VrmlFontStylePtr;


    //! VRML Text node
    class MODELPARSER_EXPORT  VrmlText : public VrmlGeometry
    {
    public:
	VrmlText();

	MFString fstring;
	VrmlFontStylePtr fontStyle;
	MFFloat length;
	SFFloat maxExtent;
    };
    typedef boost::intrusive_ptr<VrmlText> VrmlTextPtr;


    class VrmlColor;
    typedef boost::intrusive_ptr<VrmlColor> VrmlColorPtr;

    class VrmlCoordinate;
    typedef boost::intrusive_ptr<VrmlCoordinate> VrmlCoordinatePtr;

    //! VRML IndexedLineSet node
    class MODELPARSER_EXPORT  VrmlIndexedLineSet : public VrmlGeometry
    {
    public: 
	VrmlIndexedLineSet();

	VrmlColorPtr color;
	VrmlCoordinatePtr coord;
	MFInt32 colorIndex;
	SFBool colorPerVertex;
	MFInt32 coordIndex;
    };
    typedef boost::intrusive_ptr<VrmlIndexedLineSet> VrmlIndexedLineSetPtr;


    class VrmlNormal;
    typedef boost::intrusive_ptr<VrmlNormal> VrmlNormalPtr;

    class VrmlTextureCoordinate;
    typedef boost::intrusive_ptr<VrmlTextureCoordinate> VrmlTextureCoordinatePtr;


    //! VRML IndexedFaseSet node
    class MODELPARSER_EXPORT  VrmlIndexedFaceSet : public VrmlIndexedLineSet
    {
    public:
	VrmlIndexedFaceSet();

	VrmlNormalPtr normal;
	VrmlTextureCoordinatePtr texCoord;
	SFBool ccw;
	SFBool convex;
	SFFloat creaseAngle;
	MFInt32 normalIndex;
	SFBool normalPerVertex;  
	SFBool solid;
	MFInt32 texCoordIndex;
    };
    typedef boost::intrusive_ptr<VrmlIndexedFaceSet> VrmlIndexedFaceSetPtr;


    //! VRML Color node
    class MODELPARSER_EXPORT  VrmlColor : public VrmlNode
    {
    public:
      VrmlColor();
      
      MFColor color;
    };


    //! VRML Coordinate node
    class MODELPARSER_EXPORT  VrmlCoordinate : public VrmlNode
    {
    public:
      VrmlCoordinate();
      MFVec3f point;
    };


    //! VRML TextureCoordinate node
    class MODELPARSER_EXPORT  VrmlTextureCoordinate : public VrmlNode
    {
    public:
      VrmlTextureCoordinate();
      MFVec2f point;
    };


    //! VRML Normal node
    class MODELPARSER_EXPORT  VrmlNormal : public VrmlNode
    {
    public:
      VrmlNormal();
      MFVec3f fvector;
    };


    //! VRML CylinderSensor node
    class MODELPARSER_EXPORT  VrmlCylinderSensor : public VrmlNode
    {
    public:
	VrmlCylinderSensor();

	SFBool  autoOffset;
	SFFloat diskAngle;
	SFBool  enabled;
	SFFloat maxAngle;
	SFFloat minAngle;
	SFFloat offset;
    };
    typedef boost::intrusive_ptr<VrmlCylinderSensor> VrmlCylinderSensorPtr;



	// #####
	//! VRML PointSet node
	class MODELPARSER_EXPORT  VrmlPointSet : public VrmlGeometry
	{
	public:
	VrmlPointSet();

	VrmlCoordinatePtr	coord;
	VrmlColorPtr		color;
	};

	typedef boost::intrusive_ptr<VrmlPointSet> VrmlPointSetPtr;



	// #####
	//! VRML PixelTexture node
	class MODELPARSER_EXPORT  VrmlPixelTexture : public VrmlTexture
	{
	public:
	VrmlPixelTexture();

	SFImage			image;
	SFBool			repeatS;
	SFBool			repeatT;
	};

	typedef boost::intrusive_ptr<VrmlPixelTexture> VrmlPixelTexturePtr;



	// #####
	//! VRML MovieTexture node
	class MODELPARSER_EXPORT  VrmlMovieTexture : public VrmlTexture
	{
	public:
	VrmlMovieTexture();

	MFString		url;
	SFBool			loop;
	SFFloat			speed;
	SFTime			startTime;
	SFTime			stopTime;
	SFBool			repeatS;
	SFBool			repeatT;
	};

	typedef boost::intrusive_ptr<VrmlMovieTexture> VrmlMovieTexturePtr;



	// #####
	//! VRML ElevationGrid node
	class MODELPARSER_EXPORT  VrmlElevationGrid : public VrmlGeometry
	{
	public:
	VrmlElevationGrid();

	SFInt32			xDimension;
	SFInt32			zDimension;
	SFFloat			xSpacing;
	SFFloat			zSpacing;
	MFFloat			height;
	SFBool			ccw;
	SFBool			colorPerVertex;
	SFFloat			creaseAngle;
	SFBool			normalPerVertex;
	SFBool			solid;
	VrmlColorPtr	color;
	VrmlNormalPtr	normal;
	VrmlTextureCoordinatePtr	texCoord;
	};

	typedef boost::intrusive_ptr<VrmlElevationGrid> VrmlElevationGridPtr;



	// #####
	//! VRML Extrusion node
	class MODELPARSER_EXPORT  VrmlExtrusion : public VrmlGeometry
	{
	public:
	VrmlExtrusion();

	MFVec2f			crossSection;
	MFVec3f			spine;
	MFVec2f			scale;
	MFRotation		orientation;
	SFBool			beginCap;
	SFBool			endCap;
	SFBool			solid;
	SFBool			ccw;
	SFBool			convex;
	SFFloat			creaseAngle;
	};

	typedef boost::intrusive_ptr<VrmlExtrusion> VrmlExtrusionPtr;



	// #####
	//! VRML Switch node
	class MODELPARSER_EXPORT  VrmlSwitch : public VrmlNode
	{
	public:
	VrmlSwitch();

	MFNode		choice;
	SFInt32		whichChoice;
	};

	typedef boost::intrusive_ptr<VrmlSwitch> VrmlSwitchPtr;



	// #####
	//! VRML LOD node
	class MODELPARSER_EXPORT  VrmlLOD : public VrmlNode
	{
	public:
	VrmlLOD();

	MFFloat		range;
	SFVec3f		center;
	MFNode		level;
	};

	typedef boost::intrusive_ptr<VrmlLOD> VrmlLODPtr;



	// #####
	//! VRML Collision node
	class MODELPARSER_EXPORT  VrmlCollision : public VrmlNode
	{
	public:
	VrmlCollision();

	SFBool		collide;
	MFNode		children;
	SFNode		proxy;
	SFVec3f		bboxCenter;
	SFVec3f		bboxSize;
	};

	typedef boost::intrusive_ptr<VrmlCollision> VrmlCollisionPtr;



	// #####
	//! VRML Anchor node
	class MODELPARSER_EXPORT  VrmlAnchor : public VrmlNode
	{
	public:
	VrmlAnchor();

	MFNode		children;
	SFString	description;
	MFString	parameter;
	MFString	url;
	SFVec3f		bboxCenter;
	SFVec3f		bboxSize;
	};

	typedef boost::intrusive_ptr<VrmlAnchor> VrmlAnchorPtr;



	// #####
	//! VRML Fog node
	class MODELPARSER_EXPORT  VrmlFog : public VrmlNode
	{
	public:
	VrmlFog();

	SFColor		color;
	SFFloat		visibilityRange;
	SFString	fogType;
	};

	typedef boost::intrusive_ptr<VrmlFog> VrmlFogPtr;



	// #####
	//! VRML Billboard node
	class MODELPARSER_EXPORT  VrmlBillboard : public VrmlNode
	{
	public:
	VrmlBillboard();

	SFVec3f		axisOfRotation;
	MFNode		children;
	SFVec3f		bboxCenter;
	SFVec3f		bboxSize;
	};

	typedef boost::intrusive_ptr<VrmlBillboard> VrmlBillboardPtr;



	// #####
	//! VRML WorldInfo node
	class MODELPARSER_EXPORT  VrmlWorldInfo : public VrmlNode
	{
	public:
	VrmlWorldInfo();

	SFString	title;
	MFString	info;
	};

	typedef boost::intrusive_ptr<VrmlWorldInfo> VrmlWorldInfoPtr;



	// #####
	//! VRML PointLight node
	class MODELPARSER_EXPORT  VrmlPointLight : public VrmlNode
	{
	public:
	VrmlPointLight();

	SFVec3f		location;
	SFBool		on;
	SFFloat		intensity;
	SFColor		color;
	SFFloat		radius;
	SFFloat		ambientIntensity;
	SFVec3f		attenuation;
	};

	typedef boost::intrusive_ptr<VrmlPointLight> VrmlPointLightPtr;



	// #####
	//! VRML DirectionalLight node
	class MODELPARSER_EXPORT  VrmlDirectionalLight : public VrmlNode
	{
	public:
	VrmlDirectionalLight();

	SFVec3f		direction;
	SFBool		on;
	SFFloat		intensity;
	SFColor		color;
	SFFloat		ambientIntensity;
	};

	typedef boost::intrusive_ptr<VrmlDirectionalLight> VrmlDirectionalLightPtr;



	// #####
	//! VRML SpotLight node
	class MODELPARSER_EXPORT  VrmlSpotLight : public VrmlNode
	{
	public:
	VrmlSpotLight();

	SFVec3f		location;
	SFVec3f		direction;
	SFBool		on;
	SFColor		color;
	SFFloat		intensity;
	SFFloat		radius;
	SFFloat		ambientIntensity;
	SFVec3f		attenuation;
	SFFloat		beamWidth;
	SFFloat		cutOffAngle;
	};

	typedef boost::intrusive_ptr<VrmlSpotLight> VrmlSpotLightPtr;



	class MODELPARSER_EXPORT  VrmlVariantField
    {
    private:

	union {
	    SFInt32    sfInt32;
	    SFFloat    sfFloat;
	    SFVec2f    sfVec2f;
	    SFVec3f    sfVec3f;
	    SFRotation sfRotation;
	    SFColor    sfColor;
	    SFBool     sfBool;
	    SFTime     sfTime;
//		SFImage    sfImage;		// #####
	} v;

	void* valueObj; // multi-type field object

	VrmlFieldTypeId typeId_;

	void copy(const VrmlVariantField& org);
	void deleteObj();  

    public:
  
	VrmlVariantField();
	VrmlVariantField(VrmlFieldTypeId typeId);
	VrmlVariantField(const VrmlVariantField& org);
	VrmlVariantField& operator=(const VrmlVariantField& org);

	~VrmlVariantField();

	inline VrmlFieldTypeId typeId() { return typeId_; }
	void setType(VrmlFieldTypeId typeId0);

	inline SFInt32&    sfInt32()    { return v.sfInt32; }
	inline MFInt32&    mfInt32()    { return *((MFInt32*)valueObj); }
	inline SFFloat&    sfFloat()    { return v.sfFloat; }
	inline MFFloat&    mfFloat()    { return *((MFFloat*)valueObj); }
	inline SFTime&     sfTime()     { return v.sfFloat; }
	inline MFTime&     mfTime()     { return *((MFTime*)valueObj); }
	inline SFBool&     sfBool()     { return v.sfBool; }
	inline SFVec2f&    sfVec2f()    { return v.sfVec2f; }
	inline MFVec2f&    mfVec2f()    { return *((MFVec2f*)valueObj); }
	inline SFVec3f&    sfVec3f()    { return v.sfVec3f; }
	inline MFVec3f&    mfVec3f()    { return *((MFVec3f*)valueObj); }
	inline SFRotation& sfRotation() { return v.sfRotation; }
	inline MFRotation& mfRotation() { return *((MFRotation*)valueObj); }
	inline SFString&   sfString()   { return *((SFString*)valueObj); }
	inline MFString&   mfString()   { return *((MFString*)valueObj); }
	inline SFColor&    sfColor()    { return v.sfColor; }
	inline MFColor&    mfColor()    { return *((MFColor*)valueObj); }
	inline SFNode&     sfNode()     { return *((SFNode*)valueObj); }
	inline MFNode&     mfNode()     { return *((MFNode*)valueObj); }
	inline SFImage&    sfImage()    { return *((SFImage*)valueObj); }	// #####

    };

    typedef std::map <std::string, VrmlVariantField> TProtoFieldMap;
    typedef std::pair<std::string, VrmlVariantField> TProtoFieldPair;

    
    //! VRML Proto definition
    class MODELPARSER_EXPORT  VrmlProto : public VrmlNode
    {
    public:
	std::string protoName;
	TProtoFieldMap fields;

	VrmlProto(const std::string& n);

	inline VrmlVariantField* getField(const std::string& fieldName) {
	    TProtoFieldMap::iterator p = fields.find(fieldName);
	    return (p != fields.end()) ? &p->second : 0;
	}

	inline VrmlVariantField* addField(const std::string& fieldName, VrmlFieldTypeId typeId){
	    VrmlVariantField* field = &(fields[fieldName]);
	    field->setType(typeId);
	    return field;
	}

    };
    typedef boost::intrusive_ptr<VrmlProto> VrmlProtoPtr;


    //! VRML node which is instance of VRML Prototype
    class MODELPARSER_EXPORT  VrmlProtoInstance : public VrmlNode
    {
    public:
      VrmlProtoPtr proto;
      TProtoFieldMap fields;
      VrmlNodePtr actualNode;

	VrmlProtoInstance(VrmlProtoPtr proto0);

	inline VrmlVariantField* getField(const std::string& fieldName) {
	    TProtoFieldMap::iterator p = fields.find(fieldName);
	    return (p != fields.end()) ? &p->second : 0;
	} 
		
    };
    typedef boost::intrusive_ptr<VrmlProtoInstance> VrmlProtoInstancePtr;

};

#endif

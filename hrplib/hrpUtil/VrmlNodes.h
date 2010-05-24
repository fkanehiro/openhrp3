/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Shin'ichiro Nakaoka
  @author Ergovision
*/


#ifndef OPENHRP_UTIL_VRMLNODES_H_INCLUDED
#define OPENHRP_UTIL_VRMLNODES_H_INCLUDED

#include "config.h"

#include <vector>
#include <string>
#include <map>
#include <bitset>
#include <typeinfo>
#include <boost/intrusive_ptr.hpp>
#include <boost/array.hpp>


namespace hrp {

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
        SFIMAGE,
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
        INLINE_NODE,

        NUM_VRML_NODE_CATEGORIES
    };

    class VrmlNode;

    inline void intrusive_ptr_add_ref(VrmlNode* obj);
    inline void intrusive_ptr_release(VrmlNode* obj);

    //! Abstract base class of all vrml nodes.
    class HRP_UTIL_EXPORT VrmlNode
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


    class HRP_UTIL_EXPORT  VrmlUnsupportedNode : public VrmlNode
    {
      public:
        VrmlUnsupportedNode(const std::string& nodeTypeName);
        std::string nodeTypeName;
    };
    typedef boost::intrusive_ptr<VrmlUnsupportedNode> VrmlUnsupportedNodePtr;


    //! VRML Viewpoint node
    class HRP_UTIL_EXPORT  VrmlViewpoint : public VrmlNode
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
    class HRP_UTIL_EXPORT  VrmlNavigationInfo : public VrmlNode
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
    class HRP_UTIL_EXPORT  VrmlBackground : public VrmlNode
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


    class HRP_UTIL_EXPORT  AbstractVrmlGroup : public VrmlNode
    {
      public:
	AbstractVrmlGroup();
        
        virtual MFNode getChildren() = 0;
        virtual int countChildren() = 0;
        virtual VrmlNode* getChild(int index) = 0;
        virtual void replaceChild(int childIndex, VrmlNode* childNode) = 0;
        
        void removeChild(int childIndex);
    };
    typedef boost::intrusive_ptr<AbstractVrmlGroup> AbstractVrmlGroupPtr;
    
    
    //! VRML Group node
    class HRP_UTIL_EXPORT VrmlGroup : public AbstractVrmlGroup
    {
      public:
	VrmlGroup();

        virtual MFNode getChildren();
        virtual int countChildren();
        virtual VrmlNode* getChild(int index);
        virtual void replaceChild(int childIndex, VrmlNode* childNode);

	SFVec3f bboxCenter;    
	SFVec3f bboxSize;  
	MFNode children;
    };
    typedef boost::intrusive_ptr<VrmlGroup> VrmlGroupPtr;


    //! VRML Transform node
    class HRP_UTIL_EXPORT  VrmlTransform : public VrmlGroup
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

    //! VRML Inline node
    class HRP_UTIL_EXPORT  VrmlInline : public VrmlGroup
    {
      public:
        VrmlInline();
        MFString urls;
    };
    typedef boost::intrusive_ptr<VrmlInline> VrmlInlinePtr;


    class VrmlAppearance;
    typedef boost::intrusive_ptr<VrmlAppearance> VrmlAppearancePtr;

    class VrmlGeometry;
    typedef boost::intrusive_ptr<VrmlGeometry> VrmlGeometryPtr;


    //! VRML Shape node
    class HRP_UTIL_EXPORT  VrmlShape : public VrmlNode
    {
      public:
        VrmlShape();
        VrmlAppearancePtr appearance;
        SFNode geometry;
    };
    typedef boost::intrusive_ptr<VrmlShape> VrmlShapePtr;


    class VrmlMaterial;
    typedef boost::intrusive_ptr<VrmlMaterial> VrmlMaterialPtr;

    class VrmlTexture;
    typedef boost::intrusive_ptr<VrmlTexture> VrmlTexturePtr;

    class VrmlTextureTransform;
    typedef boost::intrusive_ptr<VrmlTextureTransform> VrmlTextureTransformPtr;


    //! VRML Appearance node
    class HRP_UTIL_EXPORT  VrmlAppearance : public VrmlNode
    {
      public:
        VrmlAppearance();
      
        VrmlMaterialPtr material;
        VrmlTexturePtr texture;
        VrmlTextureTransformPtr textureTransform;
    };


    //! VRML Material node
    class HRP_UTIL_EXPORT  VrmlMaterial : public VrmlNode
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
    class HRP_UTIL_EXPORT  VrmlTexture : public VrmlNode
    {
      public:
        VrmlTexture();
    };

    
    //! VRML ImageTexture node
    class HRP_UTIL_EXPORT  VrmlImageTexture : public VrmlTexture
    {
      public:
	VrmlImageTexture();

	MFString url;
	SFBool   repeatS;
	SFBool   repeatT;
    };
    typedef boost::intrusive_ptr<VrmlImageTexture> VrmlImageTexturePtr;


    //! VRML TextureTransform node
    class HRP_UTIL_EXPORT  VrmlTextureTransform : public VrmlNode
    {
      public:
	VrmlTextureTransform();

	SFVec2f center;
	SFFloat rotation;
	SFVec2f scale;
	SFVec2f translation;
    };

    //! Base class of VRML geometry nodes
    class HRP_UTIL_EXPORT  VrmlGeometry : public VrmlNode
    {
      public:
        VrmlGeometry();
    };

    //! VRML Box node
    class HRP_UTIL_EXPORT  VrmlBox : public VrmlGeometry
    {
      public:
	VrmlBox();
	SFVec3f size;
    };
    typedef boost::intrusive_ptr<VrmlBox> VrmlBoxPtr;


    //! VRML Cone node
    class HRP_UTIL_EXPORT  VrmlCone : public VrmlGeometry
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
    class HRP_UTIL_EXPORT  VrmlCylinder : public VrmlGeometry
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
    class HRP_UTIL_EXPORT  VrmlSphere : public VrmlGeometry
    {
      public:
	VrmlSphere();
	SFFloat radius;
    };
    typedef boost::intrusive_ptr<VrmlSphere> VrmlSpherePtr;


    //! VRML FontStyle node
    class HRP_UTIL_EXPORT  VrmlFontStyle : public VrmlNode
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
    class HRP_UTIL_EXPORT  VrmlText : public VrmlGeometry
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
    class HRP_UTIL_EXPORT  VrmlIndexedLineSet : public VrmlGeometry
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
    class HRP_UTIL_EXPORT  VrmlIndexedFaceSet : public VrmlIndexedLineSet
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
    class HRP_UTIL_EXPORT  VrmlColor : public VrmlNode
    {
      public:
        VrmlColor();
      
        MFColor color;
    };


    //! VRML Coordinate node
    class HRP_UTIL_EXPORT  VrmlCoordinate : public VrmlNode
    {
      public:
        VrmlCoordinate();
        MFVec3f point;
    };


    //! VRML TextureCoordinate node
    class HRP_UTIL_EXPORT  VrmlTextureCoordinate : public VrmlNode
    {
      public:
        VrmlTextureCoordinate();
        MFVec2f point;
    };


    //! VRML Normal node
    class HRP_UTIL_EXPORT  VrmlNormal : public VrmlNode
    {
      public:
        VrmlNormal();
        MFVec3f vector;
    };


    //! VRML CylinderSensor node
    class HRP_UTIL_EXPORT  VrmlCylinderSensor : public VrmlNode
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
    class HRP_UTIL_EXPORT  VrmlPointSet : public VrmlGeometry
    {
      public:
	VrmlPointSet();

	VrmlCoordinatePtr	coord;
	VrmlColorPtr		color;
    };

    typedef boost::intrusive_ptr<VrmlPointSet> VrmlPointSetPtr;



    // #####
    //! VRML PixelTexture node
    class HRP_UTIL_EXPORT  VrmlPixelTexture : public VrmlTexture
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
    class HRP_UTIL_EXPORT  VrmlMovieTexture : public VrmlTexture
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
    class HRP_UTIL_EXPORT  VrmlElevationGrid : public VrmlGeometry
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
    class HRP_UTIL_EXPORT  VrmlExtrusion : public VrmlGeometry
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



    class HRP_UTIL_EXPORT  VrmlSwitch : public AbstractVrmlGroup
    {
      public:
	VrmlSwitch();

        virtual MFNode getChildren();
        virtual int countChildren();
        virtual VrmlNode* getChild(int index);
        virtual void replaceChild(int childIndex, VrmlNode* childNode);

        MFNode	choice;
	SFInt32	whichChoice;
    };

    typedef boost::intrusive_ptr<VrmlSwitch> VrmlSwitchPtr;


    class HRP_UTIL_EXPORT  VrmlLOD : public AbstractVrmlGroup
    {
      public:
	VrmlLOD();

        virtual MFNode getChildren();
        virtual int countChildren();
        virtual VrmlNode* getChild(int index);
        virtual void replaceChild(int childIndex, VrmlNode* childNode);

	MFFloat range;
	SFVec3f center;
	MFNode  level;
    };

    typedef boost::intrusive_ptr<VrmlLOD> VrmlLODPtr;


    class HRP_UTIL_EXPORT VrmlCollision : public VrmlGroup
    {
      public:
	VrmlCollision();
	SFBool collide;
	SFNode proxy;
    };

    typedef boost::intrusive_ptr<VrmlCollision> VrmlCollisionPtr;


    class HRP_UTIL_EXPORT VrmlAnchor : public VrmlGroup
    {
      public:
	VrmlAnchor();
	SFString description;
	MFString parameter;
	MFString url;
    };

    typedef boost::intrusive_ptr<VrmlAnchor> VrmlAnchorPtr;


    class HRP_UTIL_EXPORT VrmlBillboard : public VrmlGroup
    {
      public:
	VrmlBillboard();
	SFVec3f axisOfRotation;
    };

    typedef boost::intrusive_ptr<VrmlBillboard> VrmlBillboardPtr;


    class HRP_UTIL_EXPORT VrmlFog : public VrmlNode
    {
      public:
	VrmlFog();
	SFColor  color;
	SFFloat  visibilityRange;
	SFString fogType;
    };

    typedef boost::intrusive_ptr<VrmlFog> VrmlFogPtr;


    class HRP_UTIL_EXPORT  VrmlWorldInfo : public VrmlNode
    {
      public:
	VrmlWorldInfo();
	SFString title;
	MFString info;
    };

    typedef boost::intrusive_ptr<VrmlWorldInfo> VrmlWorldInfoPtr;


    class HRP_UTIL_EXPORT VrmlPointLight : public VrmlNode
    {
      public:
	VrmlPointLight();
	SFVec3f location;
	SFBool  on;
	SFFloat intensity;
	SFColor color;
	SFFloat radius;
	SFFloat ambientIntensity;
	SFVec3f attenuation;
    };

    typedef boost::intrusive_ptr<VrmlPointLight> VrmlPointLightPtr;


    class HRP_UTIL_EXPORT VrmlDirectionalLight : public VrmlNode
    {
      public:
	VrmlDirectionalLight();
	SFFloat ambientIntensity;
	SFColor color;
	SFVec3f direction;
	SFFloat intensity;
	SFBool  on;
    };

    typedef boost::intrusive_ptr<VrmlDirectionalLight> VrmlDirectionalLightPtr;


    class HRP_UTIL_EXPORT  VrmlSpotLight : public VrmlNode
    {
      public:
	VrmlSpotLight();
	SFVec3f location;
	SFVec3f direction;
	SFBool  on;
	SFColor color;
	SFFloat intensity;
	SFFloat radius;
	SFFloat ambientIntensity;
	SFVec3f attenuation;
	SFFloat beamWidth;
	SFFloat cutOffAngle;
    };

    typedef boost::intrusive_ptr<VrmlSpotLight> VrmlSpotLightPtr;



    class HRP_UTIL_EXPORT  VrmlVariantField
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
    class HRP_UTIL_EXPORT  VrmlProto : public VrmlNode
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
    class HRP_UTIL_EXPORT VrmlProtoInstance : public VrmlNode
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


    /**
       The upper cast operation that supports the situation where the original pointer
       is VrmlProtoInstance and you want to get the actual node,
       the node replaced with the pre-defined node type written in the PROTO definition.
    */
    template<class VrmlNodeType>
    boost::intrusive_ptr<VrmlNodeType> dynamic_node_cast(VrmlNodePtr node) {
        VrmlProtoInstancePtr protoInstance = boost::dynamic_pointer_cast<VrmlProtoInstance>(node);
        if(protoInstance){
            return boost::dynamic_pointer_cast<VrmlNodeType>(protoInstance->actualNode);
        } else {
            return boost::dynamic_pointer_cast<VrmlNodeType>(node);
        }
    }

};

#endif

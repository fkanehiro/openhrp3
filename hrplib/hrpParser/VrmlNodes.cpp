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

#include "VrmlNodes.h"

using namespace hrp;


const char* VrmlNode::getLabelOfFieldType(int type)
{
    switch(type){
    case SFINT32: return "SFInt32";
    case MFINT32: return "MFInt32";
    case SFFLOAT: return "SFFloat";
    case MFFLOAT: return "MFFloat";
    case SFVEC2F: return "SFVec3f";
    case MFVEC2F: return "MFVec2f";
    case SFVEC3F: return "SFVec3f";
    case MFVEC3F: return "MFVec3f";
    case SFROTATION: return "SFRotation";
    case MFROTATION: return "MFRotation";
    case SFTIME: return "SFTime";
    case MFTIME: return "MFTime";
    case SFCOLOR: return "SFColor";
    case MFCOLOR: return "MFColor";
    case SFSTRING: return "SFString";
    case MFSTRING: return "MFString";
    case SFNODE: return "SFNode";
    case MFNODE: return "MFNode";
    case SFBOOL: return "SFBool";
    case SFIMAGE: return "SFImage";
    default: return "Unknown Field Type";
        break;
    }
}


VrmlNode::VrmlNode()
{
    refCounter = 0;
}


VrmlNode::~VrmlNode()
{

}


bool VrmlNode::isCategoryOf(VrmlNodeCategory category)
{
    return (category == ANY_NODE) ? true : categorySet.test(category);
}



VrmlUnsupportedNode::VrmlUnsupportedNode(const std::string& nodeTypeName) :
    nodeTypeName(nodeTypeName)
{
    categorySet.set(TOP_NODE);
    categorySet.set(CHILD_NODE);
}


VrmlViewpoint::VrmlViewpoint()
{
    categorySet.set(TOP_NODE);
    categorySet.set(BINDABLE_NODE);
    categorySet.set(CHILD_NODE);
    
    fieldOfView = 0.785398;
    jump = true;
  
    orientation[0] = orientation[1] = orientation[3] = 0.0;
    orientation[2] = 1.0;

    position[0] = position[1] = 0.0;
    position[2] = 10;
}


VrmlNavigationInfo::VrmlNavigationInfo() :
    avatarSize(3)
{
    categorySet.set(TOP_NODE);
    categorySet.set(BINDABLE_NODE);
    categorySet.set(CHILD_NODE);
  
    avatarSize[0] = 0.25;
    avatarSize[1] = 1.6;
    avatarSize[2] = 0.75;
  
    headlight = true;
    speed = 1.0;
    visibilityLimit = 0.0;

    type.push_back("WALK");
}


VrmlBackground::VrmlBackground()
{
    categorySet.set(TOP_NODE);
    categorySet.set(BINDABLE_NODE);
    categorySet.set(CHILD_NODE);
}


AbstractVrmlGroup::AbstractVrmlGroup()
{
    categorySet.set(TOP_NODE);
    categorySet.set(GROUPING_NODE);
    categorySet.set(CHILD_NODE);
}


void AbstractVrmlGroup::removeChild(int childIndex)
{
    replaceChild(childIndex, 0);
}


VrmlGroup::VrmlGroup()
{
    bboxCenter[0] = bboxCenter[1] = bboxCenter[2] = 0.0;
    bboxSize[0] = bboxSize[1] = bboxSize[2] = -1;
}


int VrmlGroup::countChildren()
{
    return children.size();
}


VrmlNode* VrmlGroup::getChild(int index)
{
    return children[index].get();
}


void VrmlGroup::replaceChild(int childIndex, VrmlNode* childNode)
{
    if(!childNode){
        children.erase(children.begin() + childIndex);
    } else {
        children[childIndex] = childNode;
    }
}


VrmlTransform::VrmlTransform()
{
    center[0] = center[1] = center[2] = 0.0;
    rotation[0] = rotation[1] = rotation[3] = 0.0;
    rotation[2] = 1.0;
    scale[0] = scale[1] = scale[2] = 1.0;
    scaleOrientation[0] = scaleOrientation[1] = scaleOrientation[3] = 0.0;
    scaleOrientation[2] = 1.0;
    translation[0] = translation[1] = translation[2] = 0.0;
}


VrmlShape::VrmlShape()
{
    categorySet.set(TOP_NODE);
    categorySet.set(CHILD_NODE);
    categorySet.set(SHAPE_NODE);
}


VrmlAppearance::VrmlAppearance()
{
    categorySet.set(APPEARANCE_NODE);
}


VrmlMaterial::VrmlMaterial()
{
    categorySet.set(MATERIAL_NODE);
  
    diffuseColor [0] = diffuseColor [1] = diffuseColor [2] = 0.8;
    emissiveColor[0] = emissiveColor[1] = emissiveColor[2] = 0.0;
    specularColor[0] = specularColor[1] = specularColor[2] = 0.0;
    ambientIntensity = 0.2;
    shininess = 0.2;
    transparency = 0.0;
}


VrmlTexture::VrmlTexture()
{
    categorySet.set(TEXTURE_NODE);
}


VrmlImageTexture::VrmlImageTexture()
{
    repeatS = true; 
    repeatT = true; 
}


VrmlTextureTransform::VrmlTextureTransform()
{
    categorySet.set(TEXTURE_TRANSFORM_NODE);
  
    center[0] = center[1] = 0.0;
    scale[0] = scale[1] = 1.0;
    translation[0] = translation[1] = 0.0;
    rotation = 0.0;
}


VrmlGeometry::VrmlGeometry()
{
    categorySet.set(GEOMETRY_NODE);
}


VrmlBox::VrmlBox()
{
    size[0] = size[1] = size[2] = 2.0;
}


VrmlCone::VrmlCone()
{
    bottom = true;
    bottomRadius = 1.0;
    height = 2.0;
    side = true;
}


VrmlCylinder::VrmlCylinder()
{
    height = 2.0;
    radius = 1.0;
    bottom = true;
    side = true;
    top = true;
}


VrmlSphere::VrmlSphere()
{
    radius = 1.0; 
}


VrmlFontStyle::VrmlFontStyle()
{
    categorySet.set(FONT_STYLE_NODE);
  
    family.push_back("SERIF");
    horizontal = true;
    justify.push_back("BEGIN");
    leftToRight = true;
    size = 1.0;
    spacing = 1.0;
    style = "PLAIN";
    topToBottom = true;
}


VrmlText::VrmlText()
{
    maxExtent = 0.0;
}


VrmlIndexedLineSet::VrmlIndexedLineSet()
{
    colorPerVertex = true;
}


VrmlIndexedFaceSet::VrmlIndexedFaceSet()
{
    ccw = true;
    convex = true;
    creaseAngle = 0.0;
    normalPerVertex = true;
    solid = true;
}


VrmlColor::VrmlColor()
{
    categorySet.set(COLOR_NODE);
}


VrmlCoordinate::VrmlCoordinate()
{
    categorySet.set(COORDINATE_NODE);
}


VrmlTextureCoordinate::VrmlTextureCoordinate()
{
    categorySet.set(TEXTURE_COORDINATE_NODE);
}


VrmlNormal::VrmlNormal()
{
    categorySet.set(NORMAL_NODE);
}


VrmlCylinderSensor::VrmlCylinderSensor()
{
    categorySet.set(CHILD_NODE);
    categorySet.set(SENSOR_NODE);
  
    autoOffset = true;
    diskAngle = 0.262;
    enabled = true;
    maxAngle = -1;
    minAngle = 0;
    offset = 0;
}




// ##### VrmlPointSet()
VrmlPointSet::VrmlPointSet()
{
    coord = NULL;
    color = NULL;
}



// ##### VrmlPixelTexture()
VrmlPixelTexture::VrmlPixelTexture()
{
    image.width  = 0;
    image.height = 0;
    image.numComponents = 0;
    image.pixels.clear();
	
    repeatS = true;
    repeatT = true;
}



// ##### VrmlMovieTexture()
VrmlMovieTexture::VrmlMovieTexture()
{
    // url
    loop = false;
    speed = 0;
    startTime = 0;
    stopTime = 0;
    repeatS = true;
    repeatT = true;
}



// ##### VrmlElevationGrid()
VrmlElevationGrid::VrmlElevationGrid()
{
    xDimension = 0;
    zDimension = 0;
    xSpacing = 0.0;
    zSpacing = 0.0;
    // height	// MFFloat
    ccw = true;
    colorPerVertex = true;
    creaseAngle = 0.0;
    normalPerVertex = true;
    solid = true;
    color = NULL;
    normal = NULL;
    texCoord = NULL;
}



// ##### VrmlExtrusion()
VrmlExtrusion::VrmlExtrusion()
{
    // crossSection
    // spine
    beginCap	= true;
    endCap		= true;
    solid		= true;
    ccw			= true;
    convex		= true;
    creaseAngle = 0;
}


VrmlSwitch::VrmlSwitch()
{
    whichChoice = -1;
}


int VrmlSwitch::countChildren()
{
    return choice.size();
}


VrmlNode* VrmlSwitch::getChild(int index)
{
    return choice[index].get();
}


void VrmlSwitch::replaceChild(int childIndex, VrmlNode* childNode)
{
    if(!childNode){
        choice.erase(choice.begin() + childIndex);
        if(whichChoice == childIndex){
            whichChoice = -1;
        } else if(whichChoice > childIndex){
            whichChoice -= 1;
        }
    } else {
        choice[childIndex] = childNode;
    }
}


VrmlLOD::VrmlLOD()
{
    center[0] = center[1] = center[2] = 0.0;
}


int VrmlLOD::countChildren()
{
    return level.size();
}


VrmlNode* VrmlLOD::getChild(int index)
{
    return level[index].get();
}


void VrmlLOD::replaceChild(int childIndex, VrmlNode* childNode)
{
    if(!childNode){
        level.erase(level.begin() + childIndex);
        if(!level.empty()){
            int rangeIndexToRemove = (childIndex > 0) ? (childIndex - 1) : 0;
            range.erase(range.begin() + rangeIndexToRemove);
        }
    } else {
        level[childIndex] = childNode;
    }
}


VrmlCollision::VrmlCollision()
{
    collide = true;
}


VrmlAnchor::VrmlAnchor()
{

}


VrmlBillboard::VrmlBillboard()
{
    axisOfRotation[0] = axisOfRotation[1] = axisOfRotation[3] = 0;
}


VrmlFog::VrmlFog()
{
    color[0] = color[1] = color[3] = 0.0f;
    visibilityRange = 0.0f;
    fogType = "LINEAR";
}


VrmlWorldInfo::VrmlWorldInfo()
{
    categorySet.set(TOP_NODE);
}


VrmlPointLight::VrmlPointLight()
{
    categorySet.set(TOP_NODE);
    categorySet.set(CHILD_NODE);

    location[0] = location[1] = location[2] = 0.0f;
    on = true;
    intensity = 1.0f;
    color[0] = color[1] = color[2] = 1.0f;
    radius = 100.0f;
    ambientIntensity = 0.0f;
    attenuation[0] = 1;
    attenuation[1] = attenuation[2] = 0;
}


VrmlDirectionalLight::VrmlDirectionalLight()
{
    categorySet.set(TOP_NODE);
    categorySet.set(CHILD_NODE);

    ambientIntensity = 0.0f;
    color[0] = color[1] = color[2] = 1.0f;
    direction[0] = direction[1] = 0.0f;
    direction[2] = -1.0f;
    intensity = 1.0f;
    on = true;
}


VrmlSpotLight::VrmlSpotLight()
{
    categorySet.set(TOP_NODE);
    categorySet.set(CHILD_NODE);

    location[0] = location[1] = location[2] = 0.0f;
    direction[0] = direction[1] = 0.0f;
    direction[2] = -1.0f;
    on = true;
    color[0] = color[1] = color[2] = 1.0f;
    intensity = 1.0f;
    radius = 100.0f;
    ambientIntensity = 0.0f;
    attenuation[0] = 1.0f;
    attenuation[1] = attenuation[2] = 0.0f;
    beamWidth = 1.570796f;
    cutOffAngle = 0.785398f;
}



VrmlVariantField::VrmlVariantField(VrmlFieldTypeId initialTypeId)
{
    typeId_ = UNDETERMINED_FIELD_TYPE;
    valueObj = 0;
    setType(initialTypeId);
}

VrmlVariantField::VrmlVariantField()
{
    typeId_ = UNDETERMINED_FIELD_TYPE;
    valueObj = 0;
}

VrmlVariantField::VrmlVariantField(const VrmlVariantField& org)
{
    valueObj = 0;
    copy(org);
}

VrmlVariantField& VrmlVariantField::operator=(const VrmlVariantField& org)
{
    deleteObj();
    copy(org);
    return *this;
}

void VrmlVariantField::copy(const VrmlVariantField& org)
{
    typeId_ = org.typeId_;

    if(!org.valueObj){
        v = org.v;
    } else {
        switch(typeId_){
        case MFINT32:    valueObj = new MFInt32(*((MFInt32*)org.valueObj));       break;
        case MFFLOAT:    valueObj = new MFFloat(*((MFFloat*)org.valueObj));       break;
        case MFVEC2F:    valueObj = new MFVec2f(*((MFVec2f*)org.valueObj));       break;
        case MFVEC3F:    valueObj = new MFVec3f(*((MFVec3f*)org.valueObj));       break;
        case MFROTATION: valueObj = new MFRotation(*((MFRotation*)org.valueObj)); break;
        case MFTIME:     valueObj = new MFTime(*((MFTime*)org.valueObj));         break;
        case MFCOLOR:    valueObj = new MFColor(*((MFColor*)org.valueObj));       break;
        case SFSTRING:   valueObj = new SFString(*((SFString*)org.valueObj));     break;
        case MFSTRING:   valueObj = new MFString(*((MFString*)org.valueObj));     break;
        case SFNODE:     valueObj = new SFNode(*((SFNode*)org.valueObj));         break;
        case MFNODE:     valueObj = new MFNode(*((MFNode*)org.valueObj));         break;
	case SFIMAGE:    valueObj = new SFImage(*((SFImage*)org.valueObj));       break;
        default:
            break;
        }
    } 
}


//! This can be called once 
void VrmlVariantField::setType(VrmlFieldTypeId typeId0)
{
    if(typeId_ == UNDETERMINED_FIELD_TYPE){
        typeId_ = typeId0;

        switch(typeId_){
        case MFINT32:    valueObj = new MFInt32;    break;
        case MFFLOAT:    valueObj = new MFFloat;    break;
        case MFVEC2F:    valueObj = new MFVec2f;    break;
        case MFVEC3F:    valueObj = new MFVec3f;    break;
        case MFROTATION: valueObj = new MFRotation; break;
        case MFTIME:     valueObj = new MFTime;     break;
        case MFCOLOR:    valueObj = new MFColor;    break;
        case SFSTRING:   valueObj = new SFString;   break;
        case MFSTRING:   valueObj = new MFString;   break;
        case SFNODE:     valueObj = new SFNode;     break;
        case MFNODE:     valueObj = new MFNode;     break;
	case SFIMAGE:    valueObj = new SFImage;    break;
        default:
            break;
        } 
    }
}
      

void VrmlVariantField::deleteObj()
{
    if(valueObj){
        switch(typeId_){
        case MFINT32:    delete (MFInt32*)valueObj;    break;
        case MFFLOAT:    delete (MFFloat*)valueObj;    break;
        case MFVEC2F:    delete (MFVec2f*)valueObj;    break;
        case MFVEC3F:    delete (MFVec3f*)valueObj;    break;
        case MFROTATION: delete (MFRotation*)valueObj; break;
        case MFTIME:     delete (MFTime*)valueObj;     break;
        case MFCOLOR:    delete (MFColor*)valueObj;    break;
        case SFSTRING:   delete (SFString*)valueObj;   break;
        case MFSTRING:   delete (MFString*)valueObj;   break;
        case SFNODE:     delete (SFNode*)valueObj;     break;
        case MFNODE:     delete (MFNode*)valueObj;     break;
	case SFIMAGE:    delete (SFImage*)valueObj;    break;
        default:
            break;
        }
        valueObj = 0;
    }
}

    
VrmlVariantField::~VrmlVariantField()
{
    deleteObj();
}


VrmlProto::VrmlProto(const std::string& n) : protoName(n)
{
    categorySet.set(TOP_NODE);
    categorySet.set(PROTO_DEF_NODE);
}


VrmlProtoInstance::VrmlProtoInstance(VrmlProtoPtr proto0) :
    proto(proto0),
    fields(proto0->fields) 
{
    categorySet.set(TOP_NODE);
    categorySet.set(PROTO_INSTANCE_NODE);;
    categorySet.set(CHILD_NODE);
}

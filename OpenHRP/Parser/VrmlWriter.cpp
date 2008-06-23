/*! @file
  @author S.NAKAOKA
*/

#include "VrmlWriter.h"


using namespace std;
using namespace boost;
using namespace OpenHRP;



VrmlWriter::TNodeMethodMap VrmlWriter::nodeMethodMap;


std::ostream& operator<<(std::ostream& out, VrmlWriter::TIndent& indent)
{
  return out << indent.spaces;
}

inline const char* boolstr(bool v)
{
  if(v){
    return "TRUE";
  } else {
    return "FALSE";
  }
}

ostream& operator<<(std::ostream& out, SFVec3f& v)
{
  return out << v[0] << " " << v[1] << " " << v[2];
}


ostream& operator<<(std::ostream& out, SFVec4f& v)
{
  return out << v[0] << " " << v[1] << " " << v[2] << " " << v[3];
}


template <class MFValues> void VrmlWriter::writeMFValues(MFValues values, int numColumn)
{
  out << ++indent << "[\n";
  ++indent;

  out << indent;
  int col = 0;
  int n = values.size();
  for(int i=0; i < n; i++){
    out << values[i] << " ";
    col++;
    if(col == numColumn){
      col = 0;
      out << "\n";
      if(i < n-1){
        out << indent;
      }
    }
  }

  out << --indent << "]\n";
  --indent;
}


void VrmlWriter::writeMFInt32SeparatedByMinusValue(MFInt32& values)
{
  out << ++indent << "[\n";
  ++indent;

  out << indent;
  int n = values.size();
  for(int i=0; i < n; i++){
    out << values[i] << " ";
    if(values[i] < 0){
      out << "\n";
      if(i < n-1){
        out << indent;
      }
    }
  }
  
  out << --indent << "]\n";
  --indent;
}


VrmlWriter::VrmlWriter(std::ostream& out) : out(out)
{
  if(nodeMethodMap.empty()){
    registerNodeMethodMap();
  }

}


void VrmlWriter::registerNodeMethodMap()
{
  registNodeMethod(typeid(VrmlGroup),          &VrmlWriter::writeGroupNode);
  registNodeMethod(typeid(VrmlTransform),      &VrmlWriter::writeTransformNode);
  registNodeMethod(typeid(VrmlShape),          &VrmlWriter::writeShapeNode);
  registNodeMethod(typeid(VrmlIndexedFaceSet), &VrmlWriter::writeIndexedFaceSetNode);
}


VrmlWriterNodeMethod VrmlWriter::getNodeMethod(VrmlNodePtr node)
{
  TNodeMethodMap::iterator p = nodeMethodMap.find(typeid(*node).name()); 
  if(p != nodeMethodMap.end()){
    return p->second; 
  } else {
    return 0; 
  }
}

void VrmlWriter::writeHeader()
{
  out << "#VRML V2.0 utf8\n";
}


bool VrmlWriter::writeNode(VrmlNodePtr node)
{
  indent.clear();
  out << "\n";
  writeNodeIter(node);
  return true;
}


void VrmlWriter::writeNodeIter(VrmlNodePtr node)
{
  VrmlWriterNodeMethod method = getNodeMethod(node);
  if(method){
    (this->*method)(node);
  }
}


void VrmlWriter::beginNode(const char* nodename, VrmlNodePtr node)
{
  out << indent;
  if(node->defName.empty()){
    out << nodename << " {\n";
  } else {
    out << "DEF " << node->defName << " " << nodename << " {\n";
  }
  ++indent;
}


void VrmlWriter::endNode()
{
  out << --indent << "}\n";
}


void VrmlWriter::writeGroupNode(VrmlNodePtr node)
{
  VrmlGroupPtr group = static_pointer_cast<VrmlGroup>(node);

  beginNode("Group", group);
  writeGroupFields(group);
  endNode();
}


void VrmlWriter::writeGroupFields(VrmlGroupPtr group)
{
  if(group->bboxSize[0] >= 0){
    out << indent << "bboxCenter " << group->bboxCenter << "\n";
    out << indent << "bboxSize " << group->bboxSize << "\n";
  }

  if(!group->children.empty()){
    out << indent << "children [\n";
    ++indent;
    for(size_t i=0; i < group->children.size(); i++){
      writeNodeIter(group->children[i]);
    }
    out << --indent << "]\n";
  }
}


void VrmlWriter::writeTransformNode(VrmlNodePtr node)
{
  VrmlTransformPtr trans = static_pointer_cast<VrmlTransform>(node);

  beginNode("Transform", trans);

  out << indent << "center " << trans->center << "\n";
  out << indent << "rotation " << trans->rotation << "\n";
  out << indent << "scale " << trans->scale << "\n";
  out << indent << "scaleOrientation " << trans->scaleOrientation << "\n";
  out << indent << "translation " << trans->translation << "\n";

  writeGroupFields(trans);

  endNode();
}


void VrmlWriter::writeShapeNode(VrmlNodePtr node)
{
  VrmlShapePtr shape = static_pointer_cast<VrmlShape>(node);

  beginNode("Shape", shape);

  if(shape->appearance){
    out << indent << "appearance\n";
    ++indent;
    writeAppearanceNode(shape->appearance);
    --indent;
  }
  if(shape->geometry){
    out << indent << "geometry\n";
    VrmlWriterNodeMethod method = getNodeMethod(shape->geometry);
    if(method){
      ++indent;
      (this->*method)(shape->geometry);
      --indent;
    }
  }

  endNode();
}


void VrmlWriter::writeAppearanceNode(VrmlAppearancePtr appearance)
{
  beginNode("Appearance", appearance);

  if(appearance->material){
    out << indent << "material\n";
    ++indent;
    writeMaterialNode(appearance->material);
    --indent;
  }

  endNode();
}


void VrmlWriter::writeMaterialNode(VrmlMaterialPtr material)
{
  beginNode("Material", material);

  out << indent << "ambientIntensity " << material->ambientIntensity << "\n";
  out << indent << "diffuseColor " << material->diffuseColor << "\n";
  out << indent << "emissiveColor " << material->emissiveColor << "\n";
  out << indent << "shininess " << material->shininess << "\n";
  out << indent << "specularColor " << material->specularColor << "\n";
  out << indent << "transparency " << material->transparency << "\n";

  endNode();
}


void VrmlWriter::writeIndexedFaceSetNode(VrmlNodePtr node)
{
  VrmlIndexedFaceSetPtr faceset = static_pointer_cast<VrmlIndexedFaceSet>(node);

  beginNode("IndexedFaceSet", faceset);

  if(faceset->coord){
    out << indent << "coord\n";
    ++indent;
    writeCoordinateNode(faceset->coord);
    --indent;
  }
  if(!faceset->coordIndex.empty()){
    out << indent << "coordIndex\n";
    writeMFInt32SeparatedByMinusValue(faceset->coordIndex);
  }

  out << indent << "ccw " << boolstr(faceset->ccw) << "\n";
  out << indent << "convex " << boolstr(faceset->convex) << "\n";
  out << indent << "creaseAngle " << faceset->creaseAngle << "\n";
  out << indent << "solid " << boolstr(faceset->solid) << "\n";

  endNode();
}


void VrmlWriter::writeCoordinateNode(VrmlCoordinatePtr coord)
{
  beginNode("Coordinate", coord);

  if(!coord->point.empty()){
    out << indent << "point\n";
    writeMFValues(coord->point, 1);
  }

  endNode();
}

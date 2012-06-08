#include <hrpUtil/Eigen3d.h>
#include "VrmlWriter.h"

using namespace hrp;
using namespace OpenHRP;

void VrmlWriter::write(OpenHRP::BodyInfo_var binfo, std::ostream &ofs)
{
    m_indent = 0;
    writeProtoNodes(ofs);
    writeHumanoidNode(binfo, ofs);
}

void VrmlWriter::indent(std::ostream &ofs)
{
    for (int i=0; i<m_indent; i++) ofs << " ";
}

void VrmlWriter::writeLink(int index, std::ostream &ofs)
{
    LinkInfo &linfo = links[index];
    indent(ofs); ofs << "DEF " << linfo.name << " Joint {" << std::endl;
    m_indent += 2;
    if (linfo.translation[0] || linfo.translation[1] || linfo.translation[2]){
        indent(ofs); ofs << "translation " << linfo.translation[0] << " " 
                         << linfo.translation[1] << " " << linfo.translation[2]
                         << std::endl;
    }
    if (linfo.rotation[3]){
        indent(ofs); ofs << "rotation " << linfo.rotation[0] << " " 
                         << linfo.rotation[1] << " " << linfo.rotation[2] 
                         << " " << linfo.rotation[3] << std::endl;
    }
    if (linfo.jointId != -1){
        indent(ofs); ofs << "jointId " << linfo.jointId << std::endl;
    }
    indent(ofs); ofs << "jointType \"" << linfo.jointType << "\"" << std::endl;
    indent(ofs); ofs << "jointAxis " << linfo.jointAxis[0] << " " 
                     << linfo.jointAxis[1] << " " << linfo.jointAxis[2]
                     << std::endl;
    if (linfo.ulimit.length()){
        indent(ofs); ofs << "ulimit " << linfo.ulimit[0] << std::endl;
    }
    if (linfo.llimit.length()){
        indent(ofs); ofs << "llimit " << linfo.llimit[0] << std::endl;
    }
    if (linfo.uvlimit.length()){
        indent(ofs); ofs << "uvlimit " << linfo.uvlimit[0] << std::endl;
    }
    if (linfo.lvlimit.length()){
        indent(ofs); ofs << "lvlimit " << linfo.lvlimit[0] << std::endl;
    }
    if (linfo.gearRatio != 1){
        indent(ofs); ofs << "gearRatio " << linfo.gearRatio << std::endl;
    }
    if (linfo.rotorInertia){
        indent(ofs); ofs << "rotorInertia " << linfo.rotorInertia << std::endl;
    }
    if (linfo.rotorResistor){
        indent(ofs); ofs << "rotorResistor " << linfo.rotorResistor << std::endl;
    }
    if (linfo.torqueConst != 1){
        indent(ofs); ofs << "torqueConst " << linfo.torqueConst << std::endl;
    }
    if (linfo.encoderPulse != 1){
        indent(ofs); ofs << "encoderPulse " << linfo.encoderPulse << std::endl;
    }
    indent(ofs); ofs << "children [" << std::endl;
    m_indent += 2;
    indent(ofs); ofs << "DEF " << linfo.name << "_s Segment {" << std::endl;
    m_indent += 2;
    indent(ofs); ofs << "mass " << linfo.mass << std::endl;
    indent(ofs); ofs << "centerOfMass " << linfo.centerOfMass[0] << " "
                     << linfo.centerOfMass[1] << " "
                     << linfo.centerOfMass[2] << std::endl;
    indent(ofs); ofs << "momentsOfInertia [";
    for (size_t i=0; i<9; i++) ofs << linfo.inertia[i] << " ";
    ofs << "]" << std::endl;
    indent(ofs); ofs << "children [" << std::endl;
    m_indent +=2;
    TransformedShapeIndexSequence &tsis = linfo.shapeIndices;
    for (size_t i=0; i<tsis.length(); i++){
        writeShape(tsis[i], ofs);
    }
    for (size_t i=0; i<linfo.sensors.length(); i++){
        SensorInfo &si = linfo.sensors[i];
        std::string type = (const char *)si.type;
        indent(ofs); ofs << "DEF " << si.name <<  " ";
        if (type == "Force"){
            ofs << "ForceSensor{" << std::endl;
        }else if (type == "RateGyro"){
            ofs << "Gyro{" << std::endl;
        }else if (type == "Acceleration"){
            ofs << "AccelerationSensor{" << std::endl;
        }else if (type == "Vision"){
            ofs << "VisionSensor{" << std::endl;
        }else if (type == "Range"){
            ofs << "RangeSensor{" << std::endl;
        }else{
            std::cerr << "unknown sensor type(" << type << ")" << std::endl;
        }
        m_indent += 2;
        indent(ofs); ofs << "translation " << si.translation[0] << " " 
                         << si.translation[1] << " " << si.translation[2]
                         << std::endl;
        indent(ofs); ofs << "rotation " << si.rotation[0] << " " 
                         << si.rotation[1] << " " << si.rotation[2] << " "
                         << si.rotation[3] << std::endl;
        indent(ofs); ofs << "sensorId " << si.id << std::endl;
        if (si.shapeIndices.length()){
            indent(ofs); ofs << "children[" << std::endl;
            m_indent += 2;
            for (size_t j=0; j<si.shapeIndices.length(); j++){
                writeShape(si.shapeIndices[j], ofs);
            }
            m_indent -= 2;
            indent(ofs); ofs << "]" << std::endl;
        }
        m_indent -= 2;
        indent(ofs); ofs << "} #Sensor" << std::endl;
    }
    m_indent -=2;
    indent(ofs); ofs << "]" << std::endl;
    m_indent -= 2;
    indent(ofs); ofs << "} #Segment " << linfo.name << "_s" << std::endl;
    for (size_t i=0; i<linfo.childIndices.length(); i++){
        writeLink(linfo.childIndices[i], ofs);
    }
    m_indent -= 2;
    indent(ofs); ofs << "]" << std::endl;
    m_indent -= 2;
    indent(ofs); ofs << "} #Joint" << linfo.name << std::endl;
}

void VrmlWriter::writeShape(TransformedShapeIndex &tsi, std::ostream &ofs)
{
    indent(ofs); ofs << "Transform {" << std::endl;
    m_indent += 2;
    if (tsi.transformMatrix[3] || tsi.transformMatrix[7] || tsi.transformMatrix[11]){
        indent(ofs); ofs << "translation " << tsi.transformMatrix[3] << " "
                         << tsi.transformMatrix[7] << " "
                         << tsi.transformMatrix[11] << " " << std::endl;
    }
    Vector3 xaxis, yaxis, zaxis;
    xaxis << tsi.transformMatrix[0],tsi.transformMatrix[4],tsi.transformMatrix[8];
    yaxis << tsi.transformMatrix[1],tsi.transformMatrix[5],tsi.transformMatrix[9];
    zaxis << tsi.transformMatrix[2],tsi.transformMatrix[6],tsi.transformMatrix[10];
    indent(ofs); ofs << "scale " << xaxis.norm() << " " <<  yaxis.norm() << " " << zaxis.norm() << std::endl;
    xaxis.normalize(); 
    yaxis.normalize(); 
    zaxis.normalize(); 
    Matrix33 R;
    R << xaxis[0], yaxis[0], zaxis[0],
        xaxis[1], yaxis[1], zaxis[1],
        xaxis[2], yaxis[2], zaxis[2];
    Vector3 omega = omegaFromRot(R);
    double th = omega.norm();
    Vector3 axis = omega/th;
    if (th){
        indent(ofs); ofs << "rotation " << axis[0] << " " << axis[1] << " "
                         << axis[2] << " " << th << std::endl;
    }
    indent(ofs); ofs << "children [" << std::endl;
    m_indent += 2;
    indent(ofs); ofs << "Shape {" << std::endl;
    m_indent += 2;
    ShapeInfo &si = shapes[tsi.shapeIndex];
    AppearanceInfo &app = appearances[si.appearanceIndex];
    switch(si.primitiveType){
    case SP_MESH:
        indent(ofs); ofs << "geometry IndexedFaceSet{" << std::endl;
        m_indent += 2;
        indent(ofs); ofs << "coord Coordinate{" << std::endl;
        m_indent += 2;
        indent(ofs); ofs << "point[" << std::endl;
        for (size_t i=0; i<si.vertices.length()/3; i++){
            indent(ofs); ofs << "  " << si.vertices[i*3] << " "
                             << si.vertices[i*3+1] << " " << si.vertices[i*3+2]
                             << std::endl;
        }
        indent(ofs); ofs << "]" << std::endl;
        m_indent -= 2;
        indent(ofs); ofs << "} #Coordinates" << std::endl;
        indent(ofs); ofs << "coordIndex [" << std::endl;
        for (size_t i=0; i<si.triangles.length()/3; i++){
            indent(ofs); ofs << "  " << si.triangles[i*3] << "," 
                             << si.triangles[i*3+1] << "," 
                             << si.triangles[i*3+2] << ", -1," << std::endl;
        }
        indent(ofs); ofs << "]" << std::endl;
        indent(ofs); ofs << "normal Normal{" << std::endl;
        m_indent += 2;
        indent(ofs); ofs << "vector [" << std::endl;
        for (size_t i=0; i<app.normals.length()/3; i++){
            indent(ofs); ofs << "  " << app.normals[i*3] << " " 
                             << app.normals[i*3+1] << " " << app.normals[i*3+2]
                             << "," << std::endl;
        }
        indent(ofs); ofs << "]" << std::endl;
        m_indent -= 2;
        indent(ofs); ofs << "} #Normal" << std::endl;
        indent(ofs); ofs << "normalIndex [" << std::endl;
        for (size_t i=0; i< app.normalIndices.length()/3; i++){
            indent(ofs); ofs << "  " << app.normalIndices[i*3] << ","
                             << app.normalIndices[i*3+1] << ","
                             << app.normalIndices[i*3+2] << ", -1," 
                             << std::endl;
        }
        indent(ofs); ofs << "]" << std::endl;
        indent(ofs); ofs << "normalPerVertex " << (app.normalPerVertex ? "TRUE" : "FALSE") << std::endl;
        indent(ofs); ofs << "solid " << (app.solid ? "TRUE" : "FALSE") << std::endl;
        if (app.creaseAngle){
            indent(ofs); ofs << "creaseAngle " << app.creaseAngle << std::endl;
        }
        if (app.colors.length()){
            indent(ofs); ofs << "color Color{" << std::endl;
            indent(ofs); ofs << "  color " << app.colors[0] << " " << app.colors[1] << " " << app.colors[2] << std::endl;;
            indent(ofs); ofs << "}" << std::endl;
        }
        if (app.colorIndices.length()){
            indent(ofs); ofs << "colorIndex [" << std::endl;
            m_indent += 2;
            for (size_t j=0; j<app.colorIndices.length(); j++){
                ofs << app.colorIndices[j] << ","; 
            }
            ofs << std::endl;
            m_indent -= 2;
            indent(ofs); ofs << "]" << std::endl;
        }
        indent(ofs); ofs << "colorPerVertex " << (app.colorPerVertex ? "TRUE" : "FALSE") << std::endl;
        if (app.textureCoordinate.length()){
            indent(ofs); ofs << "texCoord TextureCoordinate {" << std::endl;
            m_indent += 2;
            indent(ofs); ofs <<"point [" << std::endl;
            m_indent += 2;
            for (size_t j=0; j<app.textureCoordinate.length()/2; j++){
                indent(ofs); ofs << app.textureCoordinate[j*2] << " "
                                 << app.textureCoordinate[j*2+1] << ","
                                 << std::endl;
            }
            m_indent -= 2;
            indent(ofs); ofs << "]" << std::endl;
            m_indent -= 2;
            indent(ofs); ofs << "}" << std::endl;
        }
        if (app.textureCoordIndices.length()){
            indent(ofs); ofs << "texCoordIndex [" << std::endl;
            m_indent += 2;
            for (size_t j=0; j<app.textureCoordIndices.length()/3; j++){
                indent(ofs); ofs << app.textureCoordIndices[j*3] << ","
                                 << app.textureCoordIndices[j*3+1] << ","
                                 << app.textureCoordIndices[j*3+2] << ",-1," << std::endl;
                
            }
            m_indent -= 2;
            indent(ofs); ofs << "]" << std::endl;
        }
        //
        m_indent -= 2;
        indent(ofs); ofs << "} #IndexedFaceSet"  << std::endl;
        break;
    case SP_BOX:
        indent(ofs); ofs << "geometry Box{" << std::endl;
        indent(ofs); ofs << "  size " << si.primitiveParameters[0] << " "
                         << si.primitiveParameters[1] << " " 
                         << si.primitiveParameters[2] << std::endl;
        indent(ofs); ofs << "}" << std::endl;
        break;
    case SP_CYLINDER:
        indent(ofs); ofs << "geometry Cylinder{" << std::endl;
        indent(ofs); ofs << "  radius " << si.primitiveParameters[0] << std::endl;
        indent(ofs); ofs << "  height " << si.primitiveParameters[1] << std::endl;
        indent(ofs); ofs << "  top " << (si.primitiveParameters[2] ? "TRUE" : "FALSE") << std::endl;
        indent(ofs); ofs << "  bottom " << (si.primitiveParameters[3] ? "TRUE" : "FALSE") << std::endl;
        indent(ofs); ofs << "  side " << (si.primitiveParameters[4] ? "TRUE" : "FALSE") << std::endl;
        indent(ofs); ofs << "}" << std::endl;
        break;
    case SP_CONE:
        indent(ofs); ofs << "geometry Cone {" << std::endl;
        indent(ofs); ofs << "  bottomRadius " << si.primitiveParameters[0] << std::endl;
        indent(ofs); ofs << "  height " << si.primitiveParameters[1] << std::endl;
        indent(ofs); ofs << "  bottom " << (si.primitiveParameters[2] ? "TRUE" : "FALSE") << std::endl;
        indent(ofs); ofs << "  side " << (si.primitiveParameters[3] ? "TRUE" : "FALSE") << std::endl;
        indent(ofs); ofs << "}" << std::endl;
        break;
    case SP_SPHERE:
        indent(ofs); ofs << "geometry Sphere {" << std::endl;
        indent(ofs); ofs << "  radius " << si.primitiveParameters[0] << std::endl;
        indent(ofs); ofs << "}" << std::endl;
        break;
    case SP_PLANE:
        indent(ofs); ofs << "geometry Plane{" << std::endl;
        indent(ofs); ofs << "  size " << si.primitiveParameters[0] << " "
                         << si.primitiveParameters[1] << " " 
                         << si.primitiveParameters[2] << std::endl;
        indent(ofs); ofs << "}" << std::endl;
        break;
    default:
        std::cerr << "unknown primitive type:" << si.primitiveType << std::endl;
    }
    if (app.materialIndex >= 0){
        MaterialInfo &mat = materials[app.materialIndex];
        indent(ofs); ofs << "appearance Appearance{" << std::endl;
        m_indent += 2;
        indent(ofs); ofs << "material Material{" << std::endl;
        m_indent += 2;
        indent(ofs); ofs << "ambientIntensity " << mat.ambientIntensity 
                         << std::endl;
        indent(ofs); ofs << "diffuseColor " << mat.diffuseColor[0] << " "
                         << mat.diffuseColor[1] << " " << mat.diffuseColor[2] 
                         << std::endl;
        indent(ofs); ofs << "emissiveColor " << mat.emissiveColor[0] << " "
                         << mat.emissiveColor[1] << " " 
                         << mat.emissiveColor[2] << std::endl;
        indent(ofs); ofs << "shininess " << mat.shininess << std::endl;
        indent(ofs); ofs << "specularColor " << mat.specularColor[0] << " "
                         << mat.specularColor[1] << " " 
                         << mat.specularColor[2] << std::endl;
        indent(ofs); ofs << "transparency " << mat.transparency << std::endl;
        m_indent -= 2;
        indent(ofs); ofs << "} #Material" << std::endl;
        if (app.textureIndex >= 0){
            TextureInfo &ti = textures[app.textureIndex];
            indent(ofs); ofs << "texture ImageTexture{" << std::endl;
            indent(ofs); ofs << "  repeatS " << (ti.repeatS ? "TRUE" : "FALSE")
                             << std::endl;
            indent(ofs); ofs << "  repeatT " << (ti.repeatT ? "TRUE" : "FALSE")
                             << std::endl;
            indent(ofs); ofs << "  url \"" << ti.url << "\"" << std::endl;
            indent(ofs); ofs << "}" << std::endl;
            indent(ofs); ofs << "textureTransform TextureTransform{" << std::endl;
            m_indent += 2;
            // TODO:textransformMatrix
            m_indent -= 2;
            indent(ofs); ofs << "}" << std::endl;
        }
        //
        m_indent -= 2;
        indent(ofs); ofs << "} #Appearance" << std::endl;
    }
    m_indent -= 2;
    indent(ofs); ofs << "} #Shape" << std::endl;
    m_indent -= 2;
    indent(ofs); ofs << "]" << std::endl;
    m_indent -= 2;
    indent(ofs); ofs << "} #Transform" << std::endl;
}
void VrmlWriter::writeHumanoidNode(OpenHRP::BodyInfo_var binfo, 
                                   std::ostream &ofs)
{
    ofs << "DEF " << binfo->name() << " Humanoid {" << std::endl;
    ofs << "  humanoidBody [" << std::endl;
    links = binfo->links();
    shapes = binfo->shapes();
    appearances = binfo->appearances();
    materials = binfo->materials();
    textures = binfo->textures();
    m_indent = 4;
    writeLink(0, ofs);
    ofs << "  ]" << std::endl;
    ofs << "  joints [" << std::endl;
    for (size_t i=0; i<links->length(); i++){
        ofs << "    USE " << links[i].name << "," << std::endl;
    }
    ofs << "  ]" << std::endl;
    ofs << "  segments [" << std::endl;
    for (size_t i=0; i<links->length(); i++){
        ofs << "    USE " << links[i].name << "_s," << std::endl;
    }
    ofs << "  ]" << std::endl;
    ofs << "  name \"" << binfo->name() << "\"" << std::endl;
    ofs << "  info [" << std::endl;
    StringSequence_var info = binfo->info();
    for (size_t i=0; i<info->length(); i++){
        ofs << "    \"" << info[i] << "\"" << std::endl;
    }
    ofs << "  ]" << std::endl;
    ofs << "}" << std::endl;
}

void VrmlWriter::writeProtoNodes(std::ostream &ofs)
{
    ofs << "#VRML V2.0 utf8" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO Joint [" << std::endl;
    ofs << "  exposedField     SFVec3f      center              0 0 0" << std::endl;
    ofs << "  exposedField     MFNode       children            []" << std::endl;
    ofs << "  exposedField     MFFloat      llimit              []" << std::endl;
    ofs << "  exposedField     MFFloat      lvlimit             []" << std::endl;
    ofs << "  exposedField     SFRotation   limitOrientation    0 0 1 0" << std::endl;
    ofs << "  exposedField     SFString     name                \"\"" << std::endl;
    ofs << "  exposedField     SFRotation   rotation            0 0 1 0" << std::endl;
    ofs << "  exposedField     SFVec3f      scale               1 1 1" << std::endl;
    ofs << "  exposedField     SFRotation   scaleOrientation    0 0 1 0" << std::endl;
    ofs << "  exposedField     MFFloat      stiffness           [ 0 0 0 ]" << std::endl;
    ofs << "  exposedField     SFVec3f      translation         0 0 0" << std::endl;
    ofs << "  exposedField     MFFloat      ulimit              []" << std::endl;
    ofs << "  exposedField     MFFloat      uvlimit             []" << std::endl;
    ofs << "  exposedField     SFString     jointType           \"\"" << std::endl;
    ofs << "  exposedField     SFInt32      jointId             -1" << std::endl;
    ofs << "  exposedField     SFVec3f      jointAxis           0 0 1" << std::endl;
    ofs << "" << std::endl;
    ofs << "  exposedField     SFFloat      gearRatio           1" << std::endl;
    ofs << "  exposedField     SFFloat      rotorInertia        0" << std::endl;
    ofs << "  exposedField     SFFloat      rotorResistor       0" << std::endl;
    ofs << "  exposedField     SFFloat      torqueConst         1" << std::endl;
    ofs << "  exposedField     SFFloat      encoderPulse        1" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Transform {" << std::endl;
    ofs << "    center           IS center" << std::endl;
    ofs << "    children         IS children" << std::endl;
    ofs << "    rotation         IS rotation" << std::endl;
    ofs << "    scale            IS scale" << std::endl;
    ofs << "    scaleOrientation IS scaleOrientation" << std::endl;
    ofs << "    translation      IS translation" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO Segment [" << std::endl;
    ofs << "  field           SFVec3f     bboxCenter        0 0 0" << std::endl;
    ofs << "  field           SFVec3f     bboxSize          -1 -1 -1" << std::endl;
    ofs << "  exposedField    SFVec3f     centerOfMass      0 0 0" << std::endl;
    ofs << "  exposedField    MFNode      children          [ ]" << std::endl;
    ofs << "  exposedField    SFNode      coord             NULL" << std::endl;
    ofs << "  exposedField    MFNode      displacers        [ ]" << std::endl;
    ofs << "  exposedField    SFFloat     mass              0 " << std::endl;
    ofs << "  exposedField    MFFloat     momentsOfInertia  [ 0 0 0 0 0 0 0 0 0 ]" << std::endl;
    ofs << "  exposedField    SFString    name              \"\"" << std::endl;
    ofs << "  eventIn         MFNode      addChildren" << std::endl;
    ofs << "  eventIn         MFNode      removeChildren" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Group {" << std::endl;
    ofs << "    addChildren    IS addChildren" << std::endl;
    ofs << "    bboxCenter     IS bboxCenter" << std::endl;
    ofs << "    bboxSize       IS bboxSize" << std::endl;
    ofs << "    children       IS children" << std::endl;
    ofs << "    removeChildren IS removeChildren" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO Humanoid [" << std::endl;
    ofs << "  field           SFVec3f    bboxCenter            0 0 0" << std::endl;
    ofs << "  field           SFVec3f    bboxSize              -1 -1 -1" << std::endl;
    ofs << "  exposedField    SFVec3f    center                0 0 0" << std::endl;
    ofs << "  exposedField    MFNode     humanoidBody          [ ]" << std::endl;
    ofs << "  exposedField    MFString   info                  [ ]" << std::endl;
    ofs << "  exposedField    MFNode     joints                [ ]" << std::endl;
    ofs << "  exposedField    SFString   name                  \"\"" << std::endl;
    ofs << "  exposedField    SFRotation rotation              0 0 1 0" << std::endl;
    ofs << "  exposedField    SFVec3f    scale                 1 1 1" << std::endl;
    ofs << "  exposedField    SFRotation scaleOrientation      0 0 1 0" << std::endl;
    ofs << "  exposedField    MFNode     segments              [ ]" << std::endl;
    ofs << "  exposedField    MFNode     sites                 [ ]" << std::endl;
    ofs << "  exposedField    SFVec3f    translation           0 0 0" << std::endl;
    ofs << "  exposedField    SFString   version               \"1.1\"" << std::endl;
    ofs << "  exposedField    MFNode     viewpoints            [ ]" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Transform {" << std::endl;
    ofs << "    bboxCenter       IS bboxCenter" << std::endl;
    ofs << "    bboxSize         IS bboxSize" << std::endl;
    ofs << "    center           IS center" << std::endl;
    ofs << "    rotation         IS rotation" << std::endl;
    ofs << "    scale            IS scale" << std::endl;
    ofs << "    scaleOrientation IS scaleOrientation" << std::endl;
    ofs << "    translation      IS translation" << std::endl;
    ofs << "    children [" << std::endl;
    ofs << "      Group {" << std::endl;
    ofs << "        children IS viewpoints" << std::endl;
    ofs << "      }" << std::endl;
    ofs << "      Group {" << std::endl;
    ofs << "        children IS humanoidBody " << std::endl;
    ofs << "      }" << std::endl;
    ofs << "    ]" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO VisionSensor [" << std::endl;
    ofs << "  exposedField SFVec3f    translation       0 0 0" << std::endl;
    ofs << "  exposedField SFRotation rotation          0 0 1 0" << std::endl;
    ofs << "  exposedField MFNode     children          [ ]" << std::endl;
    ofs << "  exposedField SFFloat    fieldOfView       0.785398" << std::endl;
    ofs << "  exposedField SFString   name              \"\"" << std::endl;
    ofs << "  exposedField SFFloat    frontClipDistance 0.01" << std::endl;
    ofs << "  exposedField SFFloat    backClipDistance  10.0" << std::endl;
    ofs << "  exposedField SFString   type              \"NONE\"" << std::endl;
    ofs << "  exposedField SFInt32    sensorId          -1" << std::endl;
    ofs << "  exposedField SFInt32    width             320" << std::endl;
    ofs << "  exposedField SFInt32    height            240" << std::endl;
    ofs << "  exposedField SFFloat    frameRate         30" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Transform {" << std::endl;
    ofs << "    rotation         IS rotation" << std::endl;
    ofs << "    translation      IS translation" << std::endl;
    ofs << "    children         IS children" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
    ofs << "" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO ForceSensor [  " << std::endl;
    ofs << "  exposedField SFVec3f    maxForce    -1 -1 -1" << std::endl;
    ofs << "  exposedField SFVec3f    maxTorque   -1 -1 -1" << std::endl;
    ofs << "  exposedField SFVec3f    translation 0 0 0" << std::endl;
    ofs << "  exposedField SFRotation rotation    0 0 1 0" << std::endl;
    ofs << "  exposedField SFInt32    sensorId    -1" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Transform {" << std::endl;
    ofs << "    translation IS translation" << std::endl;
    ofs << "    rotation    IS rotation" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO Gyro [" << std::endl;
    ofs << "  exposedField SFVec3f    maxAngularVelocity -1 -1 -1" << std::endl;
    ofs << "  exposedField SFVec3f    translation        0 0 0" << std::endl;
    ofs << "  exposedField SFRotation rotation           0 0 1 0" << std::endl;
    ofs << "  exposedField SFInt32    sensorId           -1" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Transform {" << std::endl;
    ofs << "    translation IS translation" << std::endl;
    ofs << "    rotation    IS rotation" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
    ofs << "" << std::endl;
    ofs << "PROTO AccelerationSensor [" << std::endl;
    ofs << "  exposedField SFVec3f    maxAcceleration -1 -1 -1" << std::endl;
    ofs << "  exposedField SFVec3f    translation     0 0 0" << std::endl;
    ofs << "  exposedField SFRotation rotation        0 0 1 0" << std::endl;
    ofs << "  exposedField SFInt32    sensorId        -1" << std::endl;
    ofs << "]" << std::endl;
    ofs << "{" << std::endl;
    ofs << "  Transform {" << std::endl;
    ofs << "    translation IS translation" << std::endl;
    ofs << "    rotation    IS rotation" << std::endl;
    ofs << "  }" << std::endl;
    ofs << "}" << std::endl;
}
